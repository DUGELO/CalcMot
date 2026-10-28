package br.com.calcmot.telemetry

import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException

/** Place under Context.noBackupFilesDir. Single writer: CoreAnalysisRecorder, never an offer callback. */
internal class AndroidCoreAnalysisStore(directory: File) : CoreAnalysisStore {
    private val file = AtomicFile(File(directory, "core-analysis-v1.json"))

    override fun read(): CoreAnalysisState {
        // Absence is distinct from an unreadable/corrupt file: do not reset on any other error.
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) {
            return CoreAnalysisState()
        }
        return CoreAnalysisCodec.decode(file.openRead().use { input ->
            val bytes = input.readBytesBounded()
            bytes.toString(Charsets.UTF_8)
        })
    }

    override fun write(state: CoreAnalysisState) {
        val bytes = CoreAnalysisCodec.encode(state).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES)
        file.baseFile.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) throw IOException("Local metrics unavailable") }
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
    }

    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val result = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count == -1) break
            if (result.size() + count > MAX_BYTES) throw IOException("Invalid local metrics size")
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }

    private companion object { const val MAX_BYTES = 1_048_576 }
}

internal object CoreAnalysisCodec {
    private val gson = Gson()
    fun encode(state: CoreAnalysisState): String = gson.toJson(state.also { it.validate() })
    fun decode(json: String): CoreAnalysisState {
        val root = JsonParser.parseString(json).asJsonObject
        require(root.keySet() == setOf("version", "receipts", "days", "firstValues", "aliases"))
        return requireNotNull(gson.fromJson(root, CoreAnalysisState::class.java)).also { it.validate() }
    }
}
