package com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer

import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoVisualizerPreset

/**
 * Bridges the "Visualizer Studio Pro" picker (used on the Now Playing screen and
 * now on the MP3→MP4 export screen) to the [VideoVisualizerPreset] enum that the
 * export renderer ([com.salmanlaghari.pulsemusicplayerai.core.service.VisualizerFrameRenderer])
 * actually draws.
 *
 * Both enums expose the exact same categorised library (Radial/Bars/Particles/
 * Ambient/Fluid/3D/Symmetric), so the user sees one consistent preset list in
 * both places. Every [VisualizerPreset] maps to a distinct, renderer-supported
 * [VideoVisualizerPreset], guaranteeing the preset the user picks in the export
 * flow is the one rendered into the final MP4 (no silent subsetting).
 */
private val PRESET_MAP: Map<VisualizerPreset, VideoVisualizerPreset> = mapOf(
    VisualizerPreset.CIRCULAR_BARS to VideoVisualizerPreset.CIRCULAR_SPECTRUM,
    VisualizerPreset.CONCENTRIC_RINGS to VideoVisualizerPreset.CONCENTRIC_DOTS,
    VisualizerPreset.LINEAR_BARS to VideoVisualizerPreset.LINEAR_BARS,
    VisualizerPreset.PARTICLE_ORB to VideoVisualizerPreset.PARTICLE_ORB,
    VisualizerPreset.STARFIELD to VideoVisualizerPreset.STARFIELD,
    VisualizerPreset.FLUID_WAVE to VideoVisualizerPreset.SMOOTH_WAVE,
    VisualizerPreset.ISOMETRIC_GRID to VideoVisualizerPreset.ISOMETRIC_GRID,
    VisualizerPreset.FLOATING_BUBBLES to VideoVisualizerPreset.GALAXY_CLOUD,
    VisualizerPreset.KALEIDOSCOPE to VideoVisualizerPreset.KALEIDOSCOPE,
    VisualizerPreset.DOUBLE_HELIX to VideoVisualizerPreset.DOUBLE_HELIX,
    VisualizerPreset.TUNNEL_WARP to VideoVisualizerPreset.TUNNEL_WARP,
    VisualizerPreset.HEARTBEAT_PULSAR to VideoVisualizerPreset.PULSE_LINE,
    VisualizerPreset.FLAME_SPECTRUM to VideoVisualizerPreset.FLAME_SPECTRUM,
    VisualizerPreset.MATRIX_RAIN to VideoVisualizerPreset.STEP_WAVE,
    VisualizerPreset.SOUND_RIBBON to VideoVisualizerPreset.RIBBON_WAVE,
    VisualizerPreset.NEON_BARS to VideoVisualizerPreset.NEON_BARS,
    VisualizerPreset.GALAXY_RING to VideoVisualizerPreset.GALAXY_RING,
    VisualizerPreset.AURORA_FLOW to VideoVisualizerPreset.ORBIT_PARTICLES,
    VisualizerPreset.FIRE_SPECTRUM to VideoVisualizerPreset.FIREWORKS,
    VisualizerPreset.WATER_WAVES to VideoVisualizerPreset.FILLED_WAVE,
    VisualizerPreset.PLASMA_WAVE to VideoVisualizerPreset.MULTI_WAVE,
    VisualizerPreset.FREQUENCY_LINES to VideoVisualizerPreset.FREQUENCY_LINES,
    VisualizerPreset.COLOR_BURST to VideoVisualizerPreset.COLOR_BURST,
    VisualizerPreset.SPIRAL_GALAXY to VideoVisualizerPreset.SPIRAL_GALAXY,
    VisualizerPreset.RAINBOW_RING to VideoVisualizerPreset.RAINBOW_RING,
    VisualizerPreset.LASER_BEAMS to VideoVisualizerPreset.LASER_BEAMS,
    VisualizerPreset.CRYSTAL_MESH to VideoVisualizerPreset.CRYSTAL_MESH,
    VisualizerPreset.SMOKE_TRAILS to VideoVisualizerPreset.CROSS_WAVE,
    VisualizerPreset.CYBER_GRID to VideoVisualizerPreset.CYBER_GRID,
    VisualizerPreset.INFINITY_LOOP to VideoVisualizerPreset.INFINITY_LOOP,
    VisualizerPreset.RETRO_GRID to VideoVisualizerPreset.RETRO_GRID,
    VisualizerPreset.LIGHTNING_BOLT to VideoVisualizerPreset.WHEEL_SPECTRUM,
    VisualizerPreset.ORBITAL_RINGS to VideoVisualizerPreset.ORBITAL_SR,
    VisualizerPreset.MIRROR_SYMMETRY to VideoVisualizerPreset.MIRRORED_WAVE,
    VisualizerPreset.HEXAGON_MESH to VideoVisualizerPreset.HEXAGON_MESH,
    VisualizerPreset.ENERGY_SHIELD to VideoVisualizerPreset.PULSE_RING,
    VisualizerPreset.DIAMOND_GLOW to VideoVisualizerPreset.DIAMOND_GLOW,
    VisualizerPreset.METEOR_SHOWER to VideoVisualizerPreset.METEOR_SHOWER,
    VisualizerPreset.SNOWFALL to VideoVisualizerPreset.SNOWFALL,
    VisualizerPreset.FIREWORKS to VideoVisualizerPreset.FIREWORKS,
    VisualizerPreset.OCEAN_TIDES to VideoVisualizerPreset.WAVEFORM,
    VisualizerPreset.SPECTRUM_X to VideoVisualizerPreset.EXTREME_SPECTRUM_X,
    VisualizerPreset.ULTRA_BASS to VideoVisualizerPreset.FUTURE_PULSE,
    VisualizerPreset.AURORA_X to VideoVisualizerPreset.QUANTUM_CLOUD,
    VisualizerPreset.GLASS_RING to VideoVisualizerPreset.RADIAL_DOTS,
    VisualizerPreset.PLASMA_X to VideoVisualizerPreset.GLOW_BARS,
    VisualizerPreset.CYBER_WAVE to VideoVisualizerPreset.DUAL_WAVE,
    VisualizerPreset.INFINITY_BARS to VideoVisualizerPreset.INFINITY_BARS,
    VisualizerPreset.FUTURE_PULSE to VideoVisualizerPreset.RAINBOW_BARS,
    VisualizerPreset.DIGITAL_STORM to VideoVisualizerPreset.ECHO_WAVE,
    VisualizerPreset.PRISM to VideoVisualizerPreset.PRISM,
    VisualizerPreset.QUANTUM to VideoVisualizerPreset.QUANTUM_CLOUD
)

fun VisualizerPreset.toVideoPreset(): VideoVisualizerPreset {
    return PRESET_MAP[this] ?: VideoVisualizerPreset.SPECTRUM_BARS
}
