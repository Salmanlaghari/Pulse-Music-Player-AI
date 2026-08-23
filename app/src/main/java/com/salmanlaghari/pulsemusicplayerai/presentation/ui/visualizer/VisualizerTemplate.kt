package com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * AVEE-style visualizer template descriptor.
 *
 * A template describes a visualizer style in a data-driven way so that
 * new skins can be added without touching the core rendering code.
 */
data class VisualizerTemplate(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val primaryColor: Color = Color(0xFFFF00FF),
    val secondaryColor: Color = Color(0xFF00FFFF),
    val tertiaryColor: Color = Color(0xFFFFFF00),
    val particleCount: Int = 60,
    val ringCount: Int = 5,
    val barCount: Int = 64,
    val mirror: Boolean = false,
    val rotationSpeed: Float = 1f,
    val glowIntensity: Float = 0.6f
)

object VisualizerTemplateRegistry {
    private val templates = mutableListOf<VisualizerTemplate>()

    fun register(template: VisualizerTemplate) {
        templates.add(template)
    }

    fun getAll(): List<VisualizerTemplate> = templates.toList()

    fun getById(id: String): VisualizerTemplate? = templates.find { it.id == id }

    init {
        register(
            VisualizerTemplate(
                id = "radial_pulse",
                name = "Radial Pulse",
                category = "Template",
                description = "AVEE-style radial pulse with expanding rings and centre orb.",
                primaryColor = Color(0xFFFF00FF),
                secondaryColor = Color(0xFF00FFFF),
                particleCount = 80,
                ringCount = 8,
                glowIntensity = 0.8f
            )
        )
        register(
            VisualizerTemplate(
                id = "particle_burst",
                name = "Particle Burst",
                category = "Template",
                description = "AVEE-style particle burst with physics-based particles on beats.",
                primaryColor = Color(0xFFFF5500),
                secondaryColor = Color(0xFFFFDD00),
                particleCount = 120,
                ringCount = 4,
                glowIntensity = 0.9f
            )
        )
        register(
            VisualizerTemplate(
                id = "spectrum_rings",
                name = "Spectrum Rings",
                category = "Template",
                description = "AVEE-style concentric spectrum rings with frequency-reactive radii.",
                primaryColor = Color(0xFF00FFAA),
                secondaryColor = Color(0xFF00AAFF),
                ringCount = 12,
                barCount = 128,
                glowIntensity = 0.7f
            )
        )
        register(
            VisualizerTemplate(
                id = "kaleidoscope_wave",
                name = "Kaleidoscope Wave",
                category = "Template",
                description = "AVEE-style kaleidoscope with mirrored waveform segments.",
                primaryColor = Color(0xFFAA00FF),
                secondaryColor = Color(0xFFFF00AA),
                tertiaryColor = Color(0xFF00FFAA),
                particleCount = 90,
                ringCount = 6,
                mirror = true,
                rotationSpeed = 0.8f,
                glowIntensity = 0.75f
            )
        )
    }
}
