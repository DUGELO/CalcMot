package br.com.calcmot.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseIdentityAuthErrorTest {
    @Test fun otherGoogleOwnerIsCollisionEvenWhenSdkUsesInvalidCredentials() {
        assertEquals(IdentityFailure.COLLISION, invalidCredentialFailure("ERROR_USER_MISMATCH"))
    }

    @Test fun invalidOrExpiredSessionStillRequiresRecovery() {
        assertEquals(IdentityFailure.SESSION_EXPIRED, invalidCredentialFailure("ERROR_INVALID_CREDENTIAL"))
        assertEquals(IdentityFailure.SESSION_EXPIRED, invalidCredentialFailure("ERROR_INVALID_USER_TOKEN"))
    }
}
