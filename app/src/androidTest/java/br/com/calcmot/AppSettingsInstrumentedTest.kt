package br.com.calcmot

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppSettingsInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearSettings() {
        context.getSharedPreferences(PREFS_NAME, 0).edit().clear().commit()
    }

    @After
    fun restoreSettings() {
        context.getSharedPreferences(PREFS_NAME, 0).edit().clear().commit()
    }

    @Test
    fun outlinedOverlayIsTheDefaultWithoutOverridingSavedChoice() {
        assertEquals(OverlayThemePreference.OUTLINED, AppSettings.getOverlayTheme(context))

        AppSettings.setOverlayTheme(context, OverlayThemePreference.SOLID)

        assertEquals(OverlayThemePreference.SOLID, AppSettings.getOverlayTheme(context))
    }

    private companion object {
        const val PREFS_NAME = "calcmot_settings"
    }
}
