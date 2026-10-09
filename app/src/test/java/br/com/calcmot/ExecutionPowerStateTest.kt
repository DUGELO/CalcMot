package br.com.calcmot

import org.junit.Assert.*
import org.junit.Test

class ExecutionPowerStateTest {
    @Test fun dozeExemptionDoesNotOverrideExplicitRestriction() {
        val state = ExecutionPowerState.Snapshot(dozeExempt = true, backgroundRestricted = true, powerSave = false)
        assertTrue(state.identifiedRestriction())
        assertTrue(state.optimizationLabel().contains("OEM não verificado"))
    }
    @Test fun chargingDoesNotProveBackgroundIsAllowed() {
        assertTrue(ExecutionPowerState.Snapshot(charging = true, backgroundRestricted = true).identifiedRestriction())
    }
    @Test fun unknownIsNotClassifiedAsRestrictionOrUnrestricted() {
        val state = ExecutionPowerState.Snapshot()
        assertFalse(state.identifiedRestriction())
        assertEquals("Otimização desconhecida", state.optimizationLabel())
    }
    @Test fun powerSaverAndDozeAreIndependent() {
        assertTrue(ExecutionPowerState.Snapshot(dozeExempt = true, powerSave = true).identifiedRestriction())
        assertTrue(ExecutionPowerState.Snapshot(dozeExempt = false, powerSave = false).identifiedRestriction())
    }
}
