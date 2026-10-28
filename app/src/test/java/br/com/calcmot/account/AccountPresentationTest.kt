package br.com.calcmot.account

import br.com.calcmot.entitlement.*
import org.junit.Assert.*
import org.junit.Test

class AccountPresentationTest {
    @Test fun accountTasksAreTypedOrderedAndAbsentWithoutASession() {
        assertTrue(accountActions(AccountState()).isEmpty())
        val actions = accountActions(AccountState(signedIn = true, busy = true))
        assertEquals(listOf(AccountActionId.ACCESS, AccountActionId.SUPPORT, AccountActionId.SIGN_OUT), actions.map { it.id })
        assertEquals(actions.size, actions.map { it.id }.distinct().size)
        assertEquals(AccountActionSection.SESSION, actions.last().section)
        assertFalse(actions.last().enabled)
        assertTrue(actions.take(2).all { it.enabled })
    }
    @Test fun validResponseWithoutHistoricalGrantIsInformationNotAPendingTask() {
        val view = accessPresentation(AccountState(signedIn = true,
            access = EntitlementState(status = EntitlementStatus.READY, revision = 1)))
        assertEquals(AccessKind.NO_CONFIRMED_GRANT, view.kind)
        assertEquals("Acesso nesta conta", view.title)
        assertFalse(view.title.contains("ainda"))
        assertTrue(view.description.contains("Não há um direito antigo confirmado nesta conta"))
        assertTrue(view.description.contains("continuar usando"))
    }
    @Test fun authenticatedSessionDoesNotAlwaysMeanGoogle() {
        assertEquals("Conta Google conectada", accountSessionLabel(setOf("google.com")))
        assertEquals("Conta conectada", accountSessionLabel(setOf("synthetic-provider")))
        assertEquals("Identidade ainda não vinculada", accountSessionLabel(emptySet()))
    }
    @Test fun errorsNeverBecomeAValidResponseWithoutGrant() {
        for (status in listOf(EntitlementStatus.OFFLINE, EntitlementStatus.UNAUTHORIZED,
            EntitlementStatus.BACKEND_UNAVAILABLE, EntitlementStatus.INCONSISTENT)) {
            val result = accessPresentation(AccountState(signedIn = true, access = EntitlementState(status = status)))
            assertEquals(AccessKind.UNAVAILABLE, result.kind)
            assertFalse(result.description.contains("Não encontramos"))
        }
    }
    @Test fun notYetCheckedDiffersFromValidSnapshotWithoutLegacy() {
        assertEquals(AccessKind.AWAITING, accessPresentation(AccountState(signedIn = true)).kind)
        assertEquals(AccessKind.NO_CONFIRMED_GRANT, accessPresentation(AccountState(signedIn = true,
            access = EntitlementState(status = EntitlementStatus.READY, revision = 1))).kind)
    }
    @Test fun permanentCoreWinsOverTemporaryFailureWithoutBecomingPremium() {
        val state = AccountState(signedIn = true, access = EntitlementState(status = EntitlementStatus.OFFLINE, legacyCore = true))
        assertEquals(AccessKind.LEGACY, accessPresentation(state).kind)
        assertFalse(state.access.proAllowed)
    }
    @Test fun favorableClaimDoesNotDisplayGrantBeforeEntitlementConfirmation() {
        val claim = LegacyRequestView("synthetic-approved", "GRANTED")
        val state = AccountState(signedIn = true, request = claim)
        assertEquals("Aguardando confirmação do acesso", reviewTitle(claim, state))
        assertNotEquals(AccessKind.LEGACY, accessPresentation(state).kind)
        assertEquals("Direito concedido", reviewTitle(claim, state.copy(access = EntitlementState(legacyCore = true))))
    }
    @Test fun signedOutNeverPresentsForeignLegacyOrTemporaryRights() {
        assertEquals(AccessKind.SIGNED_OUT, accessPresentation(AccountState(access = EntitlementState(legacyCore = true, proAllowed = true))).kind)
    }
    @Test fun pendingReviewIsNotAGrantAndPhaseAloneDoesNotActivatePremium() {
        assertEquals(AccessKind.REVIEW, accessPresentation(AccountState(signedIn = true,
            request = LegacyRequestView("synthetic-pending", "UNDER_REVIEW"))).kind)
        assertEquals(AccessKind.AWAITING, accessPresentation(AccountState(signedIn = true,
            access = EntitlementState(billingPhase = "PAID_PERIOD"))).kind)
    }
    @Test fun recoveryFeedbackKeepsTheErrorWithoutRepeatingTheCoreReminder() {
        for (status in listOf(EntitlementStatus.OFFLINE, EntitlementStatus.UNAUTHORIZED,
            EntitlementStatus.BACKEND_UNAVAILABLE, EntitlementStatus.INCONSISTENT, EntitlementStatus.STORAGE_UNAVAILABLE)) {
            val state = AccountState(signedIn = true, access = EntitlementState(status = status))
            val message = requireNotNull(accountMessage(state, includeCoreReminder = false))
            assertFalse(message.contains("Core"))
            assertFalse(message.contains("Não encontramos"))
            assertFalse(message.contains("concedido"))
        }
        assertTrue(accountMessage(AccountState(signedIn = true,
            access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)))!!.contains("Core atual continua disponível"))
    }
    @Test fun recoveryLoginFeedbackDoesNotDuplicateTheAvailabilityFooter() {
        for (notice in listOf(AccountNotice.CANCELED, AccountNotice.CONFIGURATION, AccountNotice.LOGGED_OUT)) {
            val message = requireNotNull(accountMessage(AccountState(notice = notice), includeCoreReminder = false))
            assertFalse(message.contains("Core"))
            assertFalse(message.contains("continuar usando"))
        }
    }
}
