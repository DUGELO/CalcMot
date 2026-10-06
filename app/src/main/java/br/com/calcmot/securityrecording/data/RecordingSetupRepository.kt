package br.com.calcmot.securityrecording.data

import android.content.Context
import android.util.AtomicFile
import br.com.calcmot.securityrecording.domain.RecordingLens
import br.com.calcmot.securityrecording.domain.RecordingSetup
import br.com.calcmot.securityrecording.domain.RecordingRetention
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RecordingPreferences(
    val lens: RecordingLens = RecordingLens.FRONT,
    val framed: Boolean = false,
    val framedOrientation: Int? = null,
    val segmentMinutes: Int = 10,
    val retentionDays: Int = 1,
    val disclosureVersion: Int = 0,
    val requestedPermissions: Set<String> = emptySet()
)

/** Private, atomic and excluded by the security-recording backup subtree. */
class RecordingSetupRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "security-recording/setup.properties"))
    suspend fun read(): RecordingPreferences = withContext(Dispatchers.IO) { mutex.withLock { readUnlocked() } }
    suspend fun update(change: (RecordingPreferences) -> RecordingPreferences): RecordingPreferences = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = change(readUnlocked())
            require(next.segmentMinutes in RecordingSetup.SEGMENT_DURATION_OPTIONS_MINUTES)
            require(next.retentionDays in RecordingRetention.options)
            val values = Properties().apply {
                next.framedOrientation?.let { setProperty("framedOrientation", it.toString()) }
                setProperty("lens", next.lens.name); setProperty("framed", next.framed.toString())
                setProperty("segmentMinutes", next.segmentMinutes.toString()); setProperty("retentionDays", next.retentionDays.toString())
                setProperty("disclosureVersion", next.disclosureVersion.toString())
                setProperty("requestedPermissions", next.requestedPermissions.sorted().joinToString(","))
            }
            file.baseFile.parentFile?.let { check(it.isDirectory || it.mkdirs()) }
            val output = file.startWrite()
            try { values.store(output, null); file.finishWrite(output) }
            catch (failure: Exception) { file.failWrite(output); throw failure }
            next
        }
    }
    private fun readUnlocked(): RecordingPreferences {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return RecordingPreferences()
        val values = Properties().apply { file.openRead().use(::load) }
        return RecordingPreferences(
            lens = RecordingLens.entries.firstOrNull { it.name == values.getProperty("lens") } ?: RecordingLens.FRONT,
            framed = values.getProperty("framed").toBoolean(),
            framedOrientation = values.getProperty("framedOrientation")?.toIntOrNull(),
            segmentMinutes = values.getProperty("segmentMinutes")?.toIntOrNull()?.takeIf { it in RecordingSetup.SEGMENT_DURATION_OPTIONS_MINUTES } ?: 10,
            retentionDays = values.getProperty("retentionDays")?.toIntOrNull()?.takeIf { it in RecordingRetention.options } ?: 1,
            disclosureVersion = values.getProperty("disclosureVersion")?.toIntOrNull() ?: 0,
            requestedPermissions = values.getProperty("requestedPermissions", "").split(',').filter { it.isNotBlank() }.toSet()
        )
    }
    companion object { const val DISCLOSURE_VERSION = 1; private val mutex = Mutex() }
}
