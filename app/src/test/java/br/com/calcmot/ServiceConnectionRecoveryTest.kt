package br.com.calcmot

import org.junit.Assert.*
import org.junit.Test

class ServiceConnectionRecoveryTest {
    @Test fun disconnectedServiceCannotRecoverItself() {
        val recovery = ServiceConnectionRecovery()
        assertFalse(recovery.beginAttempt())
        recovery.connected()
        assertTrue(recovery.beginAttempt())
        recovery.disconnected()
        assertFalse(recovery.canRetry())
        assertFalse(recovery.beginAttempt())
    }
    @Test fun initializationFailureHasOnlyTwoRetries() {
        val recovery = ServiceConnectionRecovery()
        recovery.connected()
        repeat(3) { assertTrue(recovery.beginAttempt()) }
        assertFalse(recovery.canRetry())
        assertFalse(recovery.beginAttempt())
        recovery.connected()
        assertTrue(recovery.beginAttempt())
    }
    @Test fun concurrentResetRequestsCannotExceedBudget() {
        val budget = WatchdogRecoveryBudget()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val results = (1..20).map { executor.submit<Boolean> { budget.allow(0) } }
            assertEquals(3, results.count { it.get(2, java.util.concurrent.TimeUnit.SECONDS) })
        } finally { executor.shutdownNow() }
    }
    @Test fun watchdogStopsRepeatingUntilNewSuccessfulRead() {
        val budget = WatchdogRecoveryBudget()
        repeat(3) { assertTrue(budget.allow(100)) }
        assertFalse(budget.allow(100))
        assertTrue(budget.allow(200))
        repeat(2) { assertTrue(budget.allow(200)) }
        assertFalse(budget.allow(200))
        budget.reset()
        assertTrue(budget.allow(200))
    }
}
