package com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer

import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoVisualizerPreset

private val PRESET_MAP: Map<VisualizerPreset, VideoVisualizerPreset> = mapOf(
    VisualizerPreset.CIRCULAR_RADIAL_BARS to VideoVisualizerPreset.CIRCULAR_SPECTRUM,
    VisualizerPreset.CONCENTRIC to VideoVisualizerPreset.CONCENTRIC_DOTS,
    VisualizerPreset.RADIAL_WAVE to VideoVisualizerPreset.CONCENTRIC_DOTS,
    VisualizerPreset.SPIRAL_PULSE to VideoVisualizerPreset.SPIRAL_GALAXY,
    VisualizerPreset.ROTATING_RINGS to VideoVisualizerPreset.CIRCULAR_SPECTRUM,
    VisualizerPreset.CLOCKWISE_SPIN to VideoVisualizerPreset.ORBITAL_SR,
    VisualizerPreset.COUNTER_SPIN to VideoVisualizerPreset.ORBIT_PARTICLES,
    VisualizerPreset.PULSING_CORE to VideoVisualizerPreset.PULSE_RING,
    VisualizerPreset.EXPANDING_CIRCLES to VideoVisualizerPreset.FUTURE_PULSE,
    VisualizerPreset.GALAXY_SPIN to VideoVisualizerPreset.GALAXY_RING,
    VisualizerPreset.DOUBLE_HELIX to VideoVisualizerPreset.DOUBLE_HELIX,
    VisualizerPreset.ORBITING_DOTS to VideoVisualizerPreset.CONCENTRIC_DOTS,
    VisualizerPreset.SOLAR_SYSTEM to VideoVisualizerPreset.ORBITAL_SR,
    VisualizerPreset.VORTEX to VideoVisualizerPreset.TUNNEL_WARP,
    VisualizerPreset.MANDALA_ROTATE to VideoVisualizerPreset.KALEIDOSCOPE,

    VisualizerPreset.VERTICAL_BARS to VideoVisualizerPreset.SPECTRUM_BARS,
    VisualizerPreset.HORIZONTAL_BARS to VideoVisualizerPreset.SPECTRUM_BARS,
    VisualizerPreset.MIRROR_BARS to VideoVisualizerPreset.MIRROR_BARS,
    VisualizerPreset.CENTERED_BARS to VideoVisualizerPreset.SPECTRUM_BARS,
    VisualizerPreset.SIDE_BARS to VideoVisualizerPreset.EXTREME_SPECTRUM_X,
    VisualizerPreset.WAVE_BARS to VideoVisualizerPreset.SMOOTH_WAVE,
    VisualizerPreset.STEP_BARS to VideoVisualizerPreset.STEP_WAVE,
    VisualizerPreset.RAINBOW_BARS to VideoVisualizerPreset.RAINBOW_BARS,
    VisualizerPreset.GLOW_BARS to VideoVisualizerPreset.NEON_BARS,
    VisualizerPreset.BARS_3D to VideoVisualizerPreset.THICK_BARS,
    VisualizerPreset.STACKED_BARS to VideoVisualizerPreset.INFINITY_BARS,
    VisualizerPreset.BOUNCING_BARS to VideoVisualizerPreset.PEAK_BARS,
    VisualizerPreset.EQUALIZER_CLASSIC to VideoVisualizerPreset.SPECTRUM_BARS,
    VisualizerPreset.SPECTRUM_FLAT to VideoVisualizerPreset.SPECTRUM_BARS,
    VisualizerPreset.PEAK_METER to VideoVisualizerPreset.PEAK_BARS,

    VisualizerPreset.DOTS_PULSE to VideoVisualizerPreset.LINE_SPECTRUM,
    VisualizerPreset.FLYING_PARTICLES to VideoVisualizerPreset.PARTICLE_ORB,
    VisualizerPreset.RAIN_DROPS to VideoVisualizerPreset.METEOR_SHOWER,
    VisualizerPreset.CONFETTI to VideoVisualizerPreset.FIREWORKS,
    VisualizerPreset.SPARKLES to VideoVisualizerPreset.COLOR_BURST,
    VisualizerPreset.NEBULA_FLOW to VideoVisualizerPreset.GALAXY_CLOUD,
    VisualizerPreset.STAR_FIELD to VideoVisualizerPreset.STARFIELD,
    VisualizerPreset.SMOKE_RINGS to VideoVisualizerPreset.CROSS_WAVE,
    VisualizerPreset.FIRE_SPARKS to VideoVisualizerPreset.FIREWORKS,
    VisualizerPreset.SNOW_FALL to VideoVisualizerPreset.SNOWFALL,

    VisualizerPreset.SOFT_PULSE to VideoVisualizerPreset.PULSE_LINE,
    VisualizerPreset.BREATHING_GLOW to VideoVisualizerPreset.PULSE_RING,
    VisualizerPreset.GRADIENT_SHIFT to VideoVisualizerPreset.RAINBOW_RING,
    VisualizerPreset.COLOR_WAVE to VideoVisualizerPreset.MULTI_WAVE,
    VisualizerPreset.SLOW_FADE to VideoVisualizerPreset.SMOOTH_WAVE,
    VisualizerPreset.DREAM_FLOW to VideoVisualizerPreset.SMOOTH_WAVE,
    VisualizerPreset.CALM_RIPPLE to VideoVisualizerPreset.FILLED_WAVE,
    VisualizerPreset.NIGHT_SKY to VideoVisualizerPreset.STEP_WAVE,
    VisualizerPreset.AURORA to VideoVisualizerPreset.SMOOTH_WAVE,
    VisualizerPreset.DEEP_SPACE to VideoVisualizerPreset.STARFIELD
)

fun VisualizerPreset.toVideoPreset(): VideoVisualizerPreset {
    return PRESET_MAP[this] ?: VideoVisualizerPreset.SPECTRUM_BARS
}
