package com.example.calcmot.overlay

import com.example.calcmot.model.TripData

/**
 * Define o contrato para o gerenciador do overlay.
 * Usar uma interface facilita a criação de mocks em testes.
 */
interface IOverlayManager {
    fun showOverlay(data: TripData)
    fun hideOverlay()
    fun removeOverlay()
}