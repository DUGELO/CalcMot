package br.com.calcmot.entitlement

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class EntitlementCodecTest {
    private fun invalid(raw: String) {
        try { EntitlementCodec.decode(raw); fail("inconsistent snapshot accepted") }
        catch (_: IllegalArgumentException) { }
        catch (_: IllegalStateException) { }
        catch (_: com.google.gson.JsonParseException) { }
        catch (_: java.io.IOException) { }
    }
    @Test fun acceptsS01AndRejectsUnknownMissingFieldsAndUnsafeNumbers() {
        val raw = EntitlementRepositoryTest.fixture()
        assertTrue(EntitlementCodec.decode(raw).legacyCore)
        invalid(raw.replace("\"revision\":7", "\"revision\":9007199254740992"))
        invalid(raw.replace("\"revision\":7", "\"revision\":7.5"))
        invalid(raw.replace("\"revision\":7", "\"revision\":\"7\""))
        invalid(raw.replace("\"revision\":7,", ""))
        invalid(raw.replace("\"revision\":7", "\"revision\":7,\"extra\":true"))
        invalid(raw + " {}")
    }
    @Test fun rejectsFakePremiumAndImpossibleSourceDates() {
        val raw = EntitlementRepositoryTest.fixture()
        invalid(raw.replace("\"proAllowed\":false", "\"proAllowed\":true"))
        invalid(raw.replace("2026-10-14T11:00:00Z", "2026-02-30T11:00:00Z"))
        invalid(raw.replace("2026-10-14T11:00:00Z", "2099-10-14T11:00:00Z"))
        invalid(raw.replace("\"proFeatureSet\":[]", "\"proFeatureSet\":[\"PRO_INSIGHTS_WEEKLY\"]"))
        invalid(EntitlementRepositoryTest.fixture(play = true).replace("\"validUntil\":\"2026-10-14T12:01:00Z\"", "\"validUntil\":null"))
    }
    @Test fun acceptsFractionalUtcAndRejectsAmbiguousTimeZones() {
        val raw = EntitlementRepositoryTest.fixture()
        val fractional = raw.replace("2026-10-14T11:00:00Z", "2026-10-14T11:00:00.123456789Z")
        assertEquals(123L, EntitlementCodec.decode(fractional).legacy.grantedAt!! % 1000)
        invalid(raw.replace("2026-10-14T11:00:00Z", "2026-10-14T11:00:00-03:00"))
    }
    @Test fun promoCannotStartBeforeServerPeriodAndNeverRequiresLegacy() {
        val root = JsonParser.parseString(EntitlementRepositoryTest.fixture(legacy = false)).asJsonObject
        root.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-14T11:00:00Z")
            addProperty("validUntil", "2026-10-14T13:00:00Z")
        }
        root.addProperty("proAllowed", true)
        assertFalse(EntitlementCodec.decode(root.toString()).legacyCore)
        root.getAsJsonObject("promo").addProperty("startAt", "2026-10-14T14:00:00Z")
        invalid(root.toString())
    }
}
