package br.com.calcmot

import org.junit.Assert.*
import org.junit.Test

class SingleFlightQueryTest {
    @Test fun blockedQueryDoesNotQueueMoreQueries() {
        val gate = SingleFlightQuery()
        assertTrue(gate.begin())
        repeat(100) { assertFalse(gate.begin()) }
        gate.end()
        assertTrue(gate.begin())
    }
    @Test fun concurrentCallersCanStartOnlyOneQuery() {
        val gate = SingleFlightQuery()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val requests = (1..20).map { pool.submit<Boolean> { gate.begin() } }
            assertEquals(1, requests.count { it.get(2, java.util.concurrent.TimeUnit.SECONDS) })
        } finally { pool.shutdownNow() }
    }
}
