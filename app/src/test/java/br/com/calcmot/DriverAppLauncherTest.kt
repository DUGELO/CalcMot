package br.com.calcmot

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriverAppLauncherTest {

    @Test
    fun `resolve returns only the requested driver app`() {
        val resolved = DriverAppLauncher.resolve(
            installedApps = listOf(
                installed(DriverApp.NINETY_NINE, "com.app99.driver"),
                installed(DriverApp.UBER, "com.ubercab.driver")
            ),
            driverApp = DriverApp.UBER
        )

        assertEquals(DriverApp.UBER, resolved?.driverApp)
        assertEquals("com.ubercab.driver", resolved?.packageName)
    }

    @Test
    fun `resolve does not fall back to 99 when uber is requested but missing`() {
        val resolved = DriverAppLauncher.resolve(
            installedApps = listOf(installed(DriverApp.NINETY_NINE, "com.app99.driver")),
            driverApp = DriverApp.UBER
        )

        assertNull(resolved)
    }

    @Test
    fun `resolve opens 99 only when 99 is explicitly requested`() {
        val resolved = DriverAppLauncher.resolve(
            installedApps = listOf(installed(DriverApp.NINETY_NINE, "com.app99.driver")),
            driverApp = DriverApp.NINETY_NINE
        )

        assertEquals(DriverApp.NINETY_NINE, resolved?.driverApp)
        assertEquals("com.app99.driver", resolved?.packageName)
    }

    private fun installed(driverApp: DriverApp, packageName: String): InstalledDriverApp {
        return InstalledDriverApp(
            driverApp = driverApp,
            packageName = packageName,
            launchIntent = Intent("br.com.calcmot.TEST_LAUNCH")
        )
    }
}
