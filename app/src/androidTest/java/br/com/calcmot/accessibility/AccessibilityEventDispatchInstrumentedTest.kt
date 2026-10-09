package br.com.calcmot.accessibility

import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityEventDispatchInstrumentedTest {
    @Test
    fun callbackReturnsAndMainLooperRespondsWhileReaderIsBlocked() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val enteredRead = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val mainResponded = CountDownLatch(1)
        val queue = AccessibilityEventQueue<AccessibilityEvent> { it.recycle() }
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        lateinit var service: UberAccessibilityService
        instrumentation.runOnMainSync {
            service = UberAccessibilityService()
            // Isolate dispatch from service connection/real offers; the consumer simulates
            // a stalled remote accessibility read without stalling the Android main looper.
            val field = UberAccessibilityService::class.java.getDeclaredField("accessibilityEvents")
            field.isAccessible = true
            field.set(service, queue)
        }
        fun submit(packageName: String, text: String) {
            instrumentation.runOnMainSync {
                val original = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                original.packageName = packageName
                original.text.add(text)
                service.onAccessibilityEvent(original)
                original.recycle()
            }
        }
        try {
            submit("com.ubercab.driver", "original payload")
            val reader = launch(dispatcher) {
                val pending = queue.receive()
                try {
                    assertFalse(Looper.myLooper() == Looper.getMainLooper())
                    // The callback's framework event has already been recycled.
                    assertEquals("original payload", pending.value.text.single().toString())
                    enteredRead.countDown()
                    check(releaseRead.await(10, TimeUnit.SECONDS))
                    assertFalse(queue.isCurrent(pending))
                } finally {
                    queue.release(pending)
                }
            }
            assertTrue(enteredRead.await(5, TimeUnit.SECONDS))
            submit("com.example.otherapp", "new foreground")
            Handler(Looper.getMainLooper()).post { mainResponded.countDown() }
            assertTrue("Main looper must respond before the blocked read finishes", mainResponded.await(2, TimeUnit.SECONDS))
            assertEquals(1L, releaseRead.count)
            assertFalse(queue.isContextSettled)
            releaseRead.countDown()
            reader.join()
        } finally {
            releaseRead.countDown()
            queue.cancel()
            dispatcher.close()
            // This service was intentionally never connected to the framework.
            for (name in listOf("serviceScope", "mainScope")) {
                val field = UberAccessibilityService::class.java.getDeclaredField(name)
                field.isAccessible = true
                (field.get(service) as CoroutineScope).cancel()
            }
            val field = UberAccessibilityService::class.java.getDeclaredField("captureDispatcher")
            field.isAccessible = true
            (field.get(service) as ExecutorCoroutineDispatcher).close()
        }
    }
}
