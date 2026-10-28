package br.com.calcmot.account

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket
import java.util.concurrent.TimeUnit
import androidx.test.platform.app.InstrumentationRegistry
import br.com.calcmot.AppSettings
import br.com.calcmot.ReadingPipelineRuntime
import android.provider.Settings
import android.os.Bundle
import br.com.calcmot.BuildConfig
import br.com.calcmot.entitlement.HttpsEntitlementRemote
import br.com.calcmot.entitlement.EntitlementCodec
import kotlinx.coroutines.runBlocking
import br.com.calcmot.entitlement.*
import br.com.calcmot.identity.FirebaseIdentityAuth
import android.app.Application
import com.google.firebase.FirebaseApp
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.io.IOException

/** Explicit pilot only: token travels over local USB/ADB socket, never UI/log/file/intent. */
@RunWith(AndroidJUnit4::class)
class OperatorSessionHandoffDeviceTest {
    @Test fun realSignedSessionCannotUsePublicAdminOrChooseAnotherOwner() {
        val user=FirebaseAuth.getInstance().currentUser ?: error("Voluntary login required")
        val token=Tasks.await(user.getIdToken(false),15,TimeUnit.SECONDS).token!!
        fun request(path:String,credential:String):Pair<Int,String> {
            val connection=URL(BuildConfig.ACCOUNT_API_BASE_URL.trimEnd('/')+path).openConnection() as HttpsURLConnection
            try {
                connection.instanceFollowRedirects=false; connection.connectTimeout=10000; connection.readTimeout=10000
                connection.setRequestProperty("Authorization","Bearer $credential")
                val code=connection.responseCode
                return code to if(code==200)connection.inputStream.bufferedReader().use { it.readText() } else ""
            } finally { connection.disconnect() }
        }
        val own=request("/v1/me/entitlement?uid=synthetic-other-uid&project=synthetic-other-project",token)
        assertEquals(200,own.first); assertTrue(EntitlementCodec.decode(own.second).legacyCore)
        assertEquals(403,request("/admin/legacy/issue",token).first)
        assertEquals(401,request("/v1/me/entitlement","synthetic-invalid-token").first)
    }
    @Test fun persistedRealGrantSurvivesControlled503AndOffline() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName=="br.com.calcmot.identitydev")
        val app=context.applicationContext as Application
        val auth=FirebaseIdentityAuth(FirebaseAuth.getInstance())
        val owner=EntitlementOwner(FirebaseApp.getInstance().options.projectId!!,auth.currentSession()!!.uid)
        val store=AndroidEntitlementStore(app)
        val original=store.read(owner) ?: error("Open real account access first to persist the authorized DEV grant")
        assertTrue(original.snapshot.legacyCore && !original.snapshot.proAllowed)
        // Real device Keystore/cache + real session; only the failure is injected, explicitly.
        var offline=false
        val remote=object:EntitlementRemote { override suspend fun get(token:String):EntitlementHttpResponse {
            if(offline)throw IOException("Controlled offline test")
            return EntitlementHttpResponse(503,"")
        } }
        val repo=EntitlementRepository(owner.project,auth,remote,store,AndroidEntitlementClock(app),OfflinePolicy.APPROVED_V1)
        try {
            repo.restore(); assertTrue(repo.current().legacyCore)
            repo.refresh(); assertEquals(EntitlementStatus.BACKEND_UNAVAILABLE,repo.current().status)
            assertTrue(repo.current().legacyCore && repo.current().currentFreeCorePreserved)
            offline=true; repo.refresh(); assertEquals(EntitlementStatus.OFFLINE,repo.current().status)
            assertTrue(repo.current().legacyCore && !repo.current().proAllowed)
            assertEquals(original,store.read(owner))
        } finally { repo.close() }
    }
    @Test fun queryRealDevEntitlementWithoutLoggingIdentityOrToken() = runBlocking {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName=="br.com.calcmot.identitydev")
        val expected=InstrumentationRegistry.getArguments().getString("expectLegacy","true").toBoolean()
        val user=FirebaseAuth.getInstance().currentUser ?: error("Voluntary Google login required")
        val token=Tasks.await(user.getIdToken(false),15,TimeUnit.SECONDS).token!!
        val remote=HttpsEntitlementRemote(BuildConfig.ACCOUNT_API_BASE_URL)
        repeat(6) { sample ->
            val start=System.nanoTime()
            val response=remote.get(token)
            val metrics=Bundle().apply {
                putInt("sample",sample+1); putInt("httpStatus",response.code)
                putLong("roundTripMs",(System.nanoTime()-start)/1_000_000)
            }
            if(expected) {
                assertEquals(200,response.code)
                val snapshot=EntitlementCodec.decode(response.body)
                assertTrue(snapshot.legacyCore && snapshot.coreAllowed && !snapshot.proAllowed)
                assertEquals("calcmot-core-3.5-vc12",snapshot.legacy.baseline)
                metrics.putBoolean("legacyCore",snapshot.legacyCore)
                metrics.putBoolean("proAllowed",snapshot.proAllowed)
                metrics.putLong("revision",snapshot.revision)
            } else {
                // No source or snapshot is deliberately not a fabricated entitlement.
                assertEquals(503,response.code)
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(2,metrics)
        }
    }
    @Test fun prepareStandaloneDevPilotWithNoMonitoringService() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName=="br.com.calcmot.identitydev")
        check(Settings.Secure.getString(context.contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(context.packageName)!=true)
        check(!ReadingPipelineRuntime.snapshot.value.serviceConnected)
        val preferences=context.getSharedPreferences("s06_pilot_fixture",0)
        if(!preferences.contains("previous_monitoring")) preferences.edit()
            .putBoolean("previous_monitoring",AppSettings.isMonitoringEnabled(context)).commit()
        AppSettings.setMonitoringEnabled(context,false)
        // Instrumentation may terminate the process before apply() flushes.
        check(context.getSharedPreferences("calcmot_settings",0).edit().putBoolean("monitoring_enabled",false).commit())
        assertTrue(!AppSettings.isMonitoringEnabled(context))
    }
    @Test fun handoffFreshOwnerSessionToPrivateLocalConsole() {
        val user=FirebaseAuth.getInstance().currentUser ?: error("Operator must sign in voluntarily first")
        val result=Tasks.await(user.getIdToken(true),15,TimeUnit.SECONDS)
        Socket("127.0.0.1",8967).use { socket ->
            socket.soTimeout=15000
            socket.getOutputStream().write((JSONObject().put("token",result.token).toString()+"\n").toByteArray())
            val reply=socket.getInputStream().bufferedReader().readLine()
            val expected=InstrumentationRegistry.getArguments().getString("expectAccepted","true").toBoolean()
            assertEquals("Private operator authorization result",expected,JSONObject(reply).getBoolean("accepted"))
        }
    }
}
