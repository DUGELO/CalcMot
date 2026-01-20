package com.example.calcmot

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.calcmot.accessibility.UberAccessibilityService
import com.example.calcmot.model.TripData
import com.example.calcmot.overlay.IOverlayManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Implementação "fake" do OverlayManager para ser usada em testes, eliminando a necessidade do Mockito.
 */
class FakeOverlayManager : IOverlayManager {
    var showOverlayCalled = false
    var hideOverlayCalled = false
    var removeOverlayCalled = false
    var lastTripData: TripData? = null

    override fun showOverlay(data: TripData) {
        showOverlayCalled = true
        lastTripData = data
    }

    override fun hideOverlay() {
        hideOverlayCalled = true
    }

    override fun removeOverlay() {
        removeOverlayCalled = true
    }
}

@RunWith(AndroidJUnit4::class)
class AccessibilityToOverlayIntegrationTest {

    private lateinit var service: UberAccessibilityService
    private lateinit var fakeOverlayManager: FakeOverlayManager

    @Before
    fun setup() {
        service = UberAccessibilityService()
        fakeOverlayManager = FakeOverlayManager()
        service.overlayManager = fakeOverlayManager
    }

    @Test
    fun processBitmap_comImagemValida_chamaShowOverlayComTripData() = runBlocking {
        // Contexto
        val context = InstrumentationRegistry.getInstrumentation().context
        val testImage = context.assets.open("uber_offer.png").use { BitmapFactory.decodeStream(it) }
        assertNotNull("A imagem de teste 'uber_offer.png' não foi encontrada nos assets", testImage)

        // Ação
        service.processBitmap(testImage)

        // Pausa para processamento assíncrono
        Thread.sleep(3000) // Aumentado para garantir o processamento completo do OCR

        // Verificação
        assertTrue("O método showOverlay deveria ter sido chamado", fakeOverlayManager.showOverlayCalled)
        assertNotNull("O TripData não deveria ser nulo", fakeOverlayManager.lastTripData)
        assertTrue("O valor da corrida deveria ser maior que zero", (fakeOverlayManager.lastTripData?.valor ?: 0.0) > 0.0)
        assertTrue("A distância deveria ser maior que zero", (fakeOverlayManager.lastTripData?.distanciaKm ?: 0.0) > 0.0)
    }
}