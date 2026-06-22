package br.com.calcmot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverAppPackagePolicyTest {

    @Test
    fun `driver packages include approved uber and 99 variants`() {
        listOf(
            "com.ubercab.driver",
            "com.ubercab",
            "br.com.taxis99",
            "com.app99.driver"
        ).forEach { packageName ->
            assertEquals(PackageDecision.DRIVER_APP, DriverAppPackagePolicy.classify(packageName))
            assertTrue(DriverAppPackagePolicy.isDriverPackage(packageName))
            assertTrue(DriverAppPackagePolicy.isAllowedDriverPackage(packageName))
        }
    }

    @Test
    fun `driver app identity is resolved without mixing uber and 99`() {
        assertEquals(DriverApp.UBER, DriverAppPackagePolicy.driverAppForPackage("com.ubercab.driver"))
        assertEquals(DriverApp.UBER, DriverAppPackagePolicy.driverAppForPackage("com.ubercab"))
        assertEquals(DriverApp.NINETY_NINE, DriverAppPackagePolicy.driverAppForPackage("com.app99.driver"))
        assertEquals(DriverApp.NINETY_NINE, DriverAppPackagePolicy.driverAppForPackage("br.com.taxis99"))
        assertEquals(DriverApp.UNKNOWN, DriverAppPackagePolicy.driverAppForPackage("com.example.notes"))
    }

    @Test
    fun `own package is not a driver package`() {
        assertEquals(PackageDecision.OWN_APP, DriverAppPackagePolicy.classify("br.com.calcmot"))
        assertFalse(DriverAppPackagePolicy.isDriverPackage("br.com.calcmot"))
        assertFalse(DriverAppPackagePolicy.isAllowedDriverPackage("br.com.calcmot"))
        assertFalse(DriverAppPackagePolicy.isCaptureBlockedUserApp("br.com.calcmot"))
    }

    @Test
    fun `unknown package values are ignored rather than treated as blocked user apps`() {
        listOf(null, "", "   ", "unknown", "UNKNOWN").forEach { packageName ->
            assertEquals(PackageDecision.UNKNOWN, DriverAppPackagePolicy.classify(packageName))
            assertFalse(DriverAppPackagePolicy.isDriverPackage(packageName))
            assertFalse(DriverAppPackagePolicy.isCaptureBlockedUserApp(packageName))
        }
    }

    @Test
    fun `transient system packages are ignored for overlay blocking`() {
        listOf(
            "android",
            "com.android.systemui",
            "com.samsung.android.app.smartcapture"
        ).forEach { packageName ->
            assertEquals(PackageDecision.TRANSIENT_SYSTEM, DriverAppPackagePolicy.classify(packageName))
            assertFalse(DriverAppPackagePolicy.isDriverPackage(packageName))
            assertFalse(DriverAppPackagePolicy.isCaptureBlockedUserApp(packageName))
        }
    }

    @Test
    fun `blocked user apps block capture and force overlay hidden`() {
        listOf(
            "com.nu.production",
            "com.android.settings",
            "com.android.chrome",
            "com.whatsapp",
            "com.google.android.apps.photos",
            "com.sec.android.gallery3d",
            "br.com.digio.uber",
            "br.com.itau",
            "com.example.notes"
        ).forEach { packageName ->
            assertEquals(PackageDecision.BLOCKED_USER_APP, DriverAppPackagePolicy.classify(packageName))
            assertTrue(DriverAppPackagePolicy.isCaptureBlockedUserApp(packageName))
            assertTrue(DriverAppPackagePolicy.isCriticalUserApp(packageName))
            assertTrue(DriverAppPackagePolicy.isBlockedUserApp(packageName))
            assertFalse(DriverAppPackagePolicy.isDriverPackage(packageName))
        }
    }
}
