package br.com.calcmot

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class RuntimeRetirementTest {
    @Test fun cancelledNativeWorkerMustFinishBeforeReplacement() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val owner = SupervisorJob()
        val scope = CoroutineScope(owner + dispatcher)
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        scope.launch { entered.complete(Unit); release.await() }
        entered.await()
        owner.cancel()
        try {
            assertFalse(owner.isCompleted)
            var replacementPublished = false
            val failure = runCatching {
                RuntimeRetirement.await(owner, 50)
                replacementPublished = true
            }.exceptionOrNull()
            assertTrue(failure is TimeoutCancellationException)
            assertFalse(replacementPublished)
            release.countDown()
            RuntimeRetirement.await(owner, 2_000)
            assertTrue(owner.isCompleted)
        } finally { release.countDown(); owner.join(); dispatcher.close() }
    }
    @Test fun unbindCancelsAwaitWithoutPublishingReplacement() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val owner = SupervisorJob()
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        CoroutineScope(owner + dispatcher).launch { entered.complete(Unit); release.await() }
        entered.await()
        owner.cancel()
        var replacementPublished = false
        val awaiting = launch(start = CoroutineStart.UNDISPATCHED) {
            RuntimeRetirement.await(owner, 2_000)
            replacementPublished = true
        }
        try {
            awaiting.cancelAndJoin()
            assertFalse(replacementPublished)
        } finally { release.countDown(); owner.join(); dispatcher.close() }
    }

    @Test fun unfinishedInitializerIsDrainedEvenWhenServiceOwnerAlreadyCompleted() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val initializer = CoroutineScope(SupervisorJob() + dispatcher).launch { entered.complete(Unit); release.await() }
        entered.await()
        initializer.cancel()
        val completedServiceOwner = Job().apply { complete() }
        try {
            val failure = runCatching {
                RuntimeRetirement.awaitAll(listOf(completedServiceOwner, initializer), 50)
            }.exceptionOrNull()
            assertTrue(failure is TimeoutCancellationException)
            assertFalse(initializer.isCompleted)
            release.countDown()
            RuntimeRetirement.awaitAll(listOf(completedServiceOwner, initializer), 2_000)
            assertTrue(initializer.isCompleted)
        } finally { release.countDown(); initializer.join(); dispatcher.close() }
    }

    @Test fun recreatedServiceCannotBypassRetiredWorkFromDestroyedInstance() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val oldOwner = SupervisorJob()
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        CoroutineScope(oldOwner + dispatcher).launch { entered.complete(Unit); release.await() }
        entered.await()
        RuntimeRetirement.retain(listOf(oldOwner))
        oldOwner.cancel()
        val newOwner = Job().apply { complete() }
        try {
            assertTrue(runCatching { RuntimeRetirement.await(newOwner, 50) }.exceptionOrNull() is TimeoutCancellationException)
            release.countDown()
            RuntimeRetirement.await(newOwner, 2_000)
            assertTrue(oldOwner.isCompleted)
        } finally { release.countDown(); oldOwner.join(); dispatcher.close() }
    }
}
