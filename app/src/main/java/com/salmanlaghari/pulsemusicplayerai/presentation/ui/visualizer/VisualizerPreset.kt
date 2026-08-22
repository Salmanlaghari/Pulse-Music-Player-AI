package com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer

enum class VisualizerPreset(val displayName: String, val category: String, val description: String) {
    // ---------- RADIAL (15) ----------
    CIRCULAR_RADIAL_BARS("Circular Radial Bars", "Radial", "Bars arranged in a radial layout extending outward with simulated frequency."),
    CONCENTRIC("Concentric", "Radial", "Multiple concentric wave rings expanding and pulsating on simulated beats."),
    RADIAL_WAVE("Radial Wave", "Radial", "Dots radiating outward from the centre with wave motion."),
    SPIRAL_PULSE("Spiral Pulse", "Radial", "A multi-arm spiral of glowing cosmic dust pulsing with beats."),
    ROTATING_RINGS("Rotating Rings", "Radial", "Spoked wheel of spectrum bars rotating smoothly."),
    CLOCKWISE_SPIN("Clockwise Spin", "Radial", "Planetary rings revolving dynamically with custom visual tilt."),
    COUNTER_SPIN("Counter Spin", "Radial", "Particles orbiting the centre on elliptical paths in reverse."),
    PULSING_CORE("Pulsing Core", "Radial", "A single ring scaling with the acoustic beat."),
    EXPANDING_CIRCLES("Expanding Circles", "Radial", "An advanced circular telemetry pulse indicator ring."),
    GALAXY_SPIN("Galaxy Spin", "Radial", "A rotating ring of particles orbiting on bass drops."),
    DOUBLE_HELIX("Double Helix", "Radial", "Two intertwined rotating waves forming a glowing double helix."),
    ORBITING_DOTS("Orbiting Dots", "Radial", "Dots arranged in concentric rings, orbiting smoothly."),
    SOLAR_SYSTEM("Solar System", "Radial", "Planetary rings revolving dynamically with custom visual tilt."),
    VORTEX("Vortex", "Radial", "Concentric squares zooming outward into a tunnel."),
    MANDALA_ROTATE("Mandala Rotate", "Radial", "Symmetric vectors mirrored around a rotating origin."),

    // ---------- BARS (15) ----------
    VERTICAL_BARS("Vertical Bars", "Bars", "Clean equalizer bars standing on a bottom baseline."),
    HORIZONTAL_BARS("Horizontal Bars", "Bars", "Bars growing outward from the centre line."),
    MIRROR_BARS("Mirror Bars", "Bars", "Spectrum bars mirrored around the horizontal centre line."),
    CENTERED_BARS("Centered Bars", "Bars", "Classic equalizer bars rising from a baseline."),
    SIDE_BARS("Side Bars", "Bars", "A high-intensity dual-ended spectrum cross expanding on beats."),
    WAVE_BARS("Wave Bars", "Bars", "A smooth filled waveform area resembling bars."),
    STEP_BARS("Step Bars", "Bars", "An 8-bit stepped digital waveform."),
    RAINBOW_BARS("Rainbow Bars", "Bars", "Full-spectrum coloured bars on a baseline."),
    GLOW_BARS("Glow Bars", "Bars", "Bottom bars with a heavy neon glow."),
    BARS_3D("3D Bars", "Bars", "Bold thick equalizer bars."),
    STACKED_BARS("Stacked Bars", "Bars", "Bars expanding from the centre horizon to both sides."),
    BOUNCING_BARS("Bouncing Bars", "Bars", "Bottom bars topped with bright peak caps."),
    EQUALIZER_CLASSIC("Equalizer Classic", "Bars", "Classic equalizer bars rising from a baseline."),
    SPECTRUM_FLAT("Spectrum Flat", "Bars", "Clean equalizer bars standing on a bottom baseline."),
    PEAK_METER("Peak Meter", "Bars", "Bottom bars topped with bright peak caps."),

    // ---------- PARTICLES (10) ----------
    DOTS_PULSE("Dots Pulse", "Particles", "Thin spectrum lines on a clean baseline."),
    FLYING_PARTICLES("Flying Particles", "Particles", "A central glowing orb surrounded by orbiting particles."),
    RAIN_DROPS("Rain Drops", "Particles", "Diagonal meteor streaks flashing on transients."),
    CONFETTI("Confetti", "Particles", "Explosive firework bursts on beats."),
    SPARKLES("Sparkles", "Particles", "Explosive colour particle bursts from centre."),
    NEBULA_FLOW("Nebula Flow", "Particles", "A cloud of high-velocity subatomic particles."),
    STAR_FIELD("Star Field", "Particles", "Stars moving outward from the centre on audio."),
    SMOKE_RINGS("Smoke Rings", "Particles", "A rotating cloud of glowing particles."),
    FIRE_SPARKS("Fire Sparks", "Particles", "Explosive firework bursts on beats."),
    SNOW_FALL("Snow Fall", "Particles", "Delicate snowflakes drifting on the beat."),

    // ---------- AMBIENT (10) ----------
    SOFT_PULSE("Soft Pulse", "Ambient", "A single horizontal pulse line."),
    BREATHING_GLOW("Breathing Glow", "Ambient", "A single ring scaling with the acoustic beat."),
    GRADIENT_SHIFT("Gradient Shift", "Ambient", "Rich, multi-layered cosmic aurora bands waving slowly."),
    COLOR_WAVE("Color Wave", "Ambient", "High-frequency plasma lines morphing with high-velocity speed."),
    SLOW_FADE("Slow Fade", "Ambient", "A smooth filled waveform area."),
    DREAM_FLOW("Dream Flow", "Ambient", "Ambient waves of organic coloured glow mimicking Northern Lights."),
    CALM_RIPPLE("Calm Ripple", "Ambient", "Concentric water ripples echoing outward with organic damping."),
    NIGHT_SKY("Night Sky", "Ambient", "Digital rain columns falling with speed and brightness driven by audio."),
    AURORA("Aurora", "Ambient", "Ambient waves of organic coloured glow mimicking Northern Lights."),
    DEEP_SPACE("Deep Space", "Ambient", "Stars moving outward from the centre on audio.");

    /** Compatibility aliases used by the studio's richer preset catalog. */
    companion object {
        val NEON_BARS = GLOW_BARS
        val FLAME_SPECTRUM = VERTICAL_BARS
        val INFINITY_BARS = STACKED_BARS
        val EXTREME_SPECTRUM_X = SIDE_BARS
        val LINEAR_BARS = VERTICAL_BARS
        val DUAL_ENDED_BARS = SIDE_BARS
        val PEAK_BARS = PEAK_METER

        val RAINBOW_RING = CIRCULAR_RADIAL_BARS
        val GALAXY_RING = GALAXY_SPIN
        val SPIRAL_GALAXY = SPIRAL_PULSE

        val WAVEFORM = WAVE_BARS
        val DUAL_WAVE = COLOR_WAVE
        val MULTI_WAVE = COLOR_WAVE
        val MIRRORED_WAVE = WAVE_BARS
        val FILLED_WAVE = DREAM_FLOW
        val RIBBON_WAVE = DREAM_FLOW
        val STEP_WAVE = STEP_BARS
        val SMOOTH_WAVE = DREAM_FLOW
        val CROSS_WAVE = COLOR_WAVE
        val ECHO_WAVE = DREAM_FLOW

        val PARTICLE_BEAT = DOTS_PULSE
        val PARTICLE_ORB = FLYING_PARTICLES
        val FIREWORKS = CONFETTI

        val HEXAGON_MESH = MANDALA_ROTATE
        val CRYSTAL_MESH = CONCENTRIC
        val ISOMETRIC_GRID = STACKED_BARS
        val KALEIDOSCOPE = MANDALA_ROTATE
        val PRISM = RAINBOW_BARS
        val DIAMOND_GLOW = CONCENTRIC
        val LASER_BEAMS = RADIAL_WAVE
        val FREQUENCY_LINES = COLOR_WAVE
        val TUNNEL_WARP = VORTEX

        val LINE_SPECTRUM = DOTS_PULSE
        val DOT_SPECTRUM = DOTS_PULSE
        val PULSE_LINE = SOFT_PULSE
        val MINIMAL_BARS = VERTICAL_BARS
        val EQUALIZER_DOTS = DOTS_PULSE
        val TICK_SPECTRUM = DOTS_PULSE
    }
}
