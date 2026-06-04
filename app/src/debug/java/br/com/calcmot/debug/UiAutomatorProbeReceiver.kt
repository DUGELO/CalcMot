package br.com.calcmot.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import java.util.concurrent.TimeUnit

class UiAutomatorProbeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        val filesDir = context.applicationContext.filesDir
        val dumpFile = File(filesDir, "uiautomator-direct-probe.xml")
        val resultFile = File(filesDir, "uiautomator-direct-probe.json")
        dumpFile.delete()

        val startedAt = System.currentTimeMillis()
        val result = runCatching {
            val process = ProcessBuilder(
                "/system/bin/uiautomator",
                "dump",
                "--compressed",
                dumpFile.absolutePath
            )
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            ProbeResult(
                startedAtMillis = startedAt,
                finishedAtMillis = System.currentTimeMillis(),
                finished = finished,
                exitCode = if (finished) process.exitValue() else null,
                output = output,
                dumpExists = dumpFile.exists(),
                dumpLength = dumpFile.takeIf { it.exists() }?.length() ?: 0L,
                error = null
            )
        }.getOrElse { error ->
            ProbeResult(
                startedAtMillis = startedAt,
                finishedAtMillis = System.currentTimeMillis(),
                finished = false,
                exitCode = null,
                output = "",
                dumpExists = dumpFile.exists(),
                dumpLength = dumpFile.takeIf { it.exists() }?.length() ?: 0L,
                error = "${error::class.java.name}: ${error.message.orEmpty()}"
            )
        }

        resultFile.writeText(result.toJson(), Charsets.UTF_8)
    }

    companion object {
        const val ACTION = "br.com.calcmot.DEBUG_UIAUTOMATOR_PROBE"
        private const val TIMEOUT_SECONDS = 12L
    }
}

private data class ProbeResult(
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val finished: Boolean,
    val exitCode: Int?,
    val output: String,
    val dumpExists: Boolean,
    val dumpLength: Long,
    val error: String?
) {
    fun toJson(): String {
        return buildString {
            append("{")
            field("startedAtMillis", startedAtMillis)
            field("finishedAtMillis", finishedAtMillis)
            field("durationMs", finishedAtMillis - startedAtMillis)
            field("finished", finished)
            field("exitCode", exitCode)
            field("output", output)
            field("dumpExists", dumpExists)
            field("dumpLength", dumpLength)
            field("error", error, trailingComma = false)
            append("}")
        }
    }
}

private fun StringBuilder.field(name: String, value: String?, trailingComma: Boolean = true) {
    append("\"").append(name.escapeJson()).append("\":")
    if (value == null) {
        append("null")
    } else {
        append("\"").append(value.escapeJson()).append("\"")
    }
    if (trailingComma) append(",")
}

private fun StringBuilder.field(name: String, value: Int?, trailingComma: Boolean = true) {
    append("\"").append(name.escapeJson()).append("\":")
    if (value == null) append("null") else append(value)
    if (trailingComma) append(",")
}

private fun StringBuilder.field(name: String, value: Long, trailingComma: Boolean = true) {
    append("\"").append(name.escapeJson()).append("\":").append(value)
    if (trailingComma) append(",")
}

private fun StringBuilder.field(name: String, value: Boolean, trailingComma: Boolean = true) {
    append("\"").append(name.escapeJson()).append("\":").append(value)
    if (trailingComma) append(",")
}

private fun String.escapeJson(): String {
    return buildString(length) {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u%04x".format(char.code))
                    } else {
                        append(char)
                    }
                }
            }
        }
    }
}
