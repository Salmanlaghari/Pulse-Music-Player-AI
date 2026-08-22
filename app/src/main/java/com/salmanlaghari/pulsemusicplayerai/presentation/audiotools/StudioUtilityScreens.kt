package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import android.net.Uri
import android.app.Activity
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerPreset
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.toVideoPreset
import com.salmanlaghari.pulsemusicplayerai.data.premium.PremiumUnlockStore
import com.salmanlaghari.pulsemusicplayerai.data.premium.PremiumFeature
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.BorderStroke
import com.salmanlaghari.pulsemusicplayerai.domain.model.AudioFormat
import com.salmanlaghari.pulsemusicplayerai.domain.model.BackgroundFit
import com.salmanlaghari.pulsemusicplayerai.domain.model.CompressionPreset
import com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoAspectRatio
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoBackgroundStyle
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoEffect
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoResolution
import com.salmanlaghari.pulsemusicplayerai.domain.model.BuiltInBackgroundTracks
import com.salmanlaghari.pulsemusicplayerai.domain.model.VisualizerVideoConfig
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ==========================================
// PROFESSIONAL MP3 → MP4 STUDIO DATA MODELS
// ==========================================

enum class StudioVisualizerPreset(val displayName: String, val description: String, val mappedPreset: VisualizerPreset) {
    // ---------- BARS (15) ----------
    SPECTRUM_BARS("Spectrum Bars", "Classic equalizer bars rising from baseline", VisualizerPreset.VERTICAL_BARS),
    MIRROR_BARS("Mirror Bars", "Spectrum bars mirrored around the horizontal centre line", VisualizerPreset.MIRROR_BARS),
    NEON_BARS("Neon Bars", "Vibrant neon bars pulsing from top and bottom baselines", VisualizerPreset.NEON_BARS),
    FLAME_SPECTRUM("Flame Spectrum", "Rising red-to-yellow bars resembling flames", VisualizerPreset.FLAME_SPECTRUM),
    INFINITY_BARS("Infinity Bars", "Bars expanding from the centre horizon to both sides", VisualizerPreset.INFINITY_BARS),
    EXTREME_SPECTRUM_X("Extreme Spectrum X", "Dual-ended spectrum cross expanding on beats", VisualizerPreset.EXTREME_SPECTRUM_X),
    LINEAR_BARS("Linear Bars", "Clean equalizer bars standing on a bottom baseline", VisualizerPreset.LINEAR_BARS),
    DUAL_ENDED_BARS("Dual Ended Bars", "Bars growing outward from the centre line", VisualizerPreset.DUAL_ENDED_BARS),
    RAINBOW_BARS("Rainbow Bars", "Full-spectrum coloured bars on a baseline", VisualizerPreset.RAINBOW_BARS),
    GLOW_BARS("Glow Bars", "Bottom bars with a heavy neon glow", VisualizerPreset.GLOW_BARS),
    PEAK_BARS("Peak Bars", "Bottom bars topped with bright peak caps", VisualizerPreset.PEAK_BARS),
    THICK_BARS("Thick Bars", "Bold thick equalizer bars", VisualizerPreset.THICK_BARS),
    HORIZONTAL_BARS("Horizontal Bars", "Bars growing outward from the centre line", VisualizerPreset.HORIZONTAL_BARS),
    CENTERED_BARS("Centered Bars", "Classic equalizer bars rising from a baseline", VisualizerPreset.CENTERED_BARS),
    SIDE_BARS("Side Bars", "A high-intensity dual-ended spectrum cross expanding on beats", VisualizerPreset.SIDE_BARS),

    // ---------- CIRCULAR (15) ----------
    CIRCULAR_SPECTRUM("Circular Spectrum", "Spectrum bars radiating outward around a centre ring", VisualizerPreset.CIRCULAR_RADIAL_BARS),
    RAINBOW_RING("Rainbow Ring", "A full-spectrum ring of dots pulsating radially", VisualizerPreset.RAINBOW_RING),
    GALAXY_RING("Galaxy Ring", "A rotating ring of particles orbiting on bass drops", VisualizerPreset.GALAXY_SPIN),
    SPIRAL_GALAXY("Spiral Galaxy", "A multi-arm spiral of glowing cosmic dust", VisualizerPreset.SPIRAL_PULSE),
    RADIAL_DOTS("Radial Dots", "Dots radiating outward from the centre", VisualizerPreset.RADIAL_WAVE),
    FUTURE_PULSE("Future Pulse", "An advanced circular telemetry pulse indicator ring", VisualizerPreset.EXPANDING_CIRCLES),
    ORBITAL_SR("Orbital Ring", "Planetary rings revolving with custom tilt", VisualizerPreset.CLOCKWISE_SPIN),
    PULSE_RING("Pulse Ring", "A single ring scaling with the acoustic beat", VisualizerPreset.PULSING_CORE),
    CONCENTRIC_DOTS("Concentric Dots", "Dots arranged in concentric rings", VisualizerPreset.CONCENTRIC),
    WHEEL_SPECTRUM("Wheel Spectrum", "Spoked wheel of spectrum bars", VisualizerPreset.ROTATING_RINGS),
    WAVE_BARS("Wave Bars", "A smooth filled waveform area resembling bars", VisualizerPreset.WAVE_BARS),
    STEP_BARS("Step Bars", "An 8-bit stepped digital waveform", VisualizerPreset.STEP_BARS),
    BARS_3D("3D Bars", "Bold thick equalizer bars", VisualizerPreset.BARS_3D),
    STACKED_BARS("Stacked Bars", "Bars expanding from the centre horizon to both sides", VisualizerPreset.STACKED_BARS),
    BOUNCING_BARS("Bouncing Bars", "Bottom bars topped with bright peak caps", VisualizerPreset.PEAK_BARS),
    SPECTRUM_FLAT("Spectrum Flat", "Clean equalizer bars standing on a bottom baseline", VisualizerPreset.SPECTRUM_FLAT),
    PEAK_METER("Peak Meter", "Bottom bars topped with bright peak caps", VisualizerPreset.PEAK_BARS),

    // ---------- WAVE (10) ----------
    WAVEFORM("Waveform", "The raw audio waveform drawn as a continuous line", VisualizerPreset.WAVEFORM),
    DUAL_WAVE("Dual Wave", "Two mirrored waveform lines", VisualizerPreset.DUAL_WAVE),
    MULTI_WAVE("Multi Wave", "Several layered waveform lines", VisualizerPreset.MULTI_WAVE),
    MIRRORED_WAVE("Mirrored Wave", "Left-to-right perfectly mirrored wave", VisualizerPreset.MIRRORED_WAVE),
    FILLED_WAVE("Filled Wave", "A waveform filled with colour underneath", VisualizerPreset.FILLED_WAVE),
    RIBBON_WAVE("Sound Ribbon", "A smooth bezier ribbon weaving across the screen", VisualizerPreset.RIBBON_WAVE),
    STEP_WAVE("Step Wave", "An 8-bit stepped digital waveform", VisualizerPreset.STEP_WAVE),
    SMOOTH_WAVE("Smooth Wave", "A smooth filled waveform area", VisualizerPreset.SMOOTH_WAVE),
    CROSS_WAVE("Cross Wave", "Two crossing waveform lines", VisualizerPreset.CROSS_WAVE),
    ECHO_WAVE("Echo Wave", "Trailing echo waveform lines", VisualizerPreset.ECHO_WAVE),

    // ---------- PARTICLES (10) ----------
    PARTICLE_BEAT("Particle Beat", "Particles pushed outward by detected beats", VisualizerPreset.PARTICLE_BEAT),
    PARTICLE_ORB("Particle Orb", "A central glowing orb surrounded by orbiting particles", VisualizerPreset.PARTICLE_ORB),
    STARFIELD("Starfield", "Stars moving outward from the centre on audio", VisualizerPreset.STAR_FIELD),
    QUANTUM_CLOUD("Quantum Cloud", "A cloud of high-velocity subatomic particles", VisualizerPreset.NEBULA_FLOW),
    FIREWORKS("Fireworks", "Explosive firework bursts on beats", VisualizerPreset.FIREWORKS),
    METEOR_SHOWER("Meteor Shower", "Diagonal meteor streaks flashing on transients", VisualizerPreset.RAIN_DROPS),
    SNOWFALL("Snowfall", "Delicate snowflakes drifting on the beat", VisualizerPreset.SNOW_FALL),
    COLOR_BURST("Color Burst", "Explosive colour particle bursts from centre", VisualizerPreset.SPARKLES),
    ORBIT_PARTICLES("Orbit Particles", "Particles orbiting the centre on elliptical paths", VisualizerPreset.ORBITING_DOTS),
    GALAXY_CLOUD("Galaxy Cloud", "A rotating cloud of glowing particles", VisualizerPreset.SMOKE_RINGS),

    // ---------- GEOMETRIC (12) ----------
    HEXAGON_MESH("Hexagon Mesh", "A structural grid of glowing hexagons", VisualizerPreset.HEXAGON_MESH),
    CRYSTAL_MESH("Crystal Mesh", "Interconnected glowing crystal nodes", VisualizerPreset.CRYSTAL_MESH),
    ISOMETRIC_GRID("Isometric Grid", "Cubes in isometric projection scaling with frequency", VisualizerPreset.ISOMETRIC_GRID),
    DOUBLE_HELIX("Double Helix", "Two intertwined rotating waves forming a helix", VisualizerPreset.DOUBLE_HELIX),
    KALEIDOSCOPE("Kaleidoscope", "Symmetric vectors mirrored around a rotating origin", VisualizerPreset.KALEIDOSCOPE),
    PRISM("Prism", "Light refraction vectors splitting into rainbow colours", VisualizerPreset.PRISM),
    DIAMOND_GLOW("Diamond Glow", "Nested glowing diamonds scaling with beats", VisualizerPreset.DIAMOND_GLOW),
    LASER_BEAMS("Laser Beams", "Sharp laser rays projecting from the centre", VisualizerPreset.LASER_BEAMS),
    INFINITY_LOOP("Infinity Loop", "An overlaid rotating infinity loop", VisualizerPreset.MANDALA_ROTATE),
    FREQUENCY_LINES("Frequency Lines", "Clean overlaid sine-wave indicator bands", VisualizerPreset.FREQUENCY_LINES),
    TUNNEL_WARP("Tunnel Warp", "Concentric squares zooming outward into a tunnel", VisualizerPreset.TUNNEL_WARP),
    VORTEX("Vortex", "Concentric squares zooming outward into a tunnel", VisualizerPreset.VORTEX),

    // ---------- MINIMAL (8) ----------
    LINE_SPECTRUM("Line Spectrum", "Thin spectrum lines on a clean baseline", VisualizerPreset.LINE_SPECTRUM),
    DOT_SPECTRUM("Dot Spectrum", "Dots rising on a baseline", VisualizerPreset.DOT_SPECTRUM),
    PULSE_LINE("Pulse Line", "A single horizontal pulse line", VisualizerPreset.PULSE_LINE),
    MINIMAL_BARS("Minimal Bars", "Thin minimal equalizer bars", VisualizerPreset.MINIMAL_BARS),
    EQUALIZER_DOTS("Equalizer Dots", "Vertical columns of equializer dots", VisualizerPreset.EQUALIZER_DOTS),
    TICK_SPECTRUM("Tick Spectrum", "Spectrum drawn as vertical ticks", VisualizerPreset.TICK_SPECTRUM),
    SOFT_PULSE("Soft Pulse", "A single horizontal pulse line", VisualizerPreset.SOFT_PULSE),
    BREATHING_GLOW("Breathing Glow", "A single ring scaling with the acoustic beat", VisualizerPreset.BREATHING_GLOW),

    // ---------- AMBIENT (10) ----------
    GRADIENT_SHIFT("Gradient Shift", "Rich, multi-layered cosmic aurora bands waving slowly", VisualizerPreset.GRADIENT_SHIFT),
    COLOR_WAVE("Color Wave", "High-frequency plasma lines morphing with high-velocity speed", VisualizerPreset.COLOR_WAVE),
    SLOW_FADE("Slow Fade", "A smooth filled waveform area", VisualizerPreset.SLOW_FADE),
    DREAM_FLOW("Dream Flow", "Ambient waves of organic coloured glow mimicking Northern Lights", VisualizerPreset.DREAM_FLOW),
    CALM_RIPPLE("Calm Ripple", "Concentric water ripples echoing outward with organic damping", VisualizerPreset.CALM_RIPPLE),
    NIGHT_SKY("Night Sky", "Digital rain columns falling with speed and brightness driven by audio", VisualizerPreset.NIGHT_SKY),
    AURORA("Aurora", "Ambient waves of organic coloured glow mimicking Northern Lights", VisualizerPreset.AURORA),
    DEEP_SPACE("Deep Space", "Stars moving outward from the centre on audio", VisualizerPreset.DEEP_SPACE)
}

data class LiveBackgroundPreset(
    val id: String,
    val displayName: String,
    val category: String,
    val gradient: List<Color>,
    val isPremium: Boolean = false,
    val resolutionLabel: String = "SD"
)

data class EffectType(
    val id: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

data class ColorTheme(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val gradient: List<Color>
)

data class QuickPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val visualizer: StudioVisualizerPreset,
    val background: LiveBackgroundPreset,
    val effect: EffectType? = null,
    val theme: ColorTheme
)

// Professional MP3 → MP4 Studio Data Instances
private val STUDIO_VISUALIZERS = StudioVisualizerPreset.values()
private val LIVE_BACKGROUNDS = listOf(
    LiveBackgroundPreset("neon_galaxy", "Neon Galaxy", "LIVE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("purple_galaxy", "Purple Galaxy", "LIVE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF2979FF), Color(0xFF05060A))),
    LiveBackgroundPreset("cyber_city", "Cyber City", "LIVE", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0B0D14))),
    LiveBackgroundPreset("space_tunnel", "Space Tunnel", "LIVE", listOf(Color(0xFF05060A), Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("aurora", "Aurora", "LIVE", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("particle_universe", "Particle Universe", "LIVE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFFFF2BD6), Color(0xFF05060A))),
    LiveBackgroundPreset("cosmic_dust", "Cosmic Dust", "LIVE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("neon_waves", "Neon Waves", "LIVE", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF05060A))),
    LiveBackgroundPreset("energy_flow", "Energy Flow", "LIVE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFFFF00A8), Color(0xFF05060A))),
    LiveBackgroundPreset("light_tunnel", "Light Tunnel", "LIVE", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFFFFFFFF), Color(0xFF05060A))),
    LiveBackgroundPreset("cyber_grid", "Cyber Grid", "LIVE", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0B0D14))),
    LiveBackgroundPreset("star_field", "Star Field", "LIVE", listOf(Color(0xFF05060A), Color(0xFFFFFFFF), Color(0xFF7C4DFF), Color(0xFF05060A))),
    LiveBackgroundPreset("abstract_liquid", "Abstract Liquid", "LIVE", listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFFFF2BD6), Color(0xFF7C4DFF))),
    LiveBackgroundPreset("purple_smoke", "Purple Smoke", "LIVE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF10131D), Color(0xFF05060A))),
    LiveBackgroundPreset("blue_smoke", "Blue Smoke", "LIVE", listOf(Color(0xFF05060A), Color(0xFF2979FF), Color(0xFF10131D), Color(0xFF05060A))),
    LiveBackgroundPreset("rainbow_energy", "Rainbow Energy", "LIVE", listOf(Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF00A8))),
    LiveBackgroundPreset("digital_matrix", "Digital Matrix", "LIVE", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF0B0D14), Color(0xFF00E5FF))),
    LiveBackgroundPreset("music_stage", "Music Stage", "LIVE", listOf(Color(0xFF10131D), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF10131D))),
    LiveBackgroundPreset("futuristic_tunnel", "Futuristic Tunnel", "LIVE", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF05060A))),
    LiveBackgroundPreset("cosmic_explosion", "Cosmic Explosion", "LIVE", listOf(Color(0xFF05060A), Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF05060A)))
)
private val CINEMATIC_4K_BACKGROUNDS = listOf(
    LiveBackgroundPreset("4k_galaxy", "4K Galaxy", "4K", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "4K"),
    LiveBackgroundPreset("4k_neon", "4K Neon", "4K", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "4K"),
    LiveBackgroundPreset("4k_cyber", "4K Cyber", "4K", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0B0D14)), isPremium = true, resolutionLabel = "4K"),
    LiveBackgroundPreset("4k_aurora", "4K Aurora", "4K", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "4K"),
    LiveBackgroundPreset("4k_space", "4K Space", "4K", listOf(Color(0xFF05060A), Color(0xFF2979FF), Color(0xFF7C4DFF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "4K")
)
private val CINEMATIC_8K_BACKGROUNDS = listOf(
    LiveBackgroundPreset("8k_space", "8K Space", "8K", listOf(Color(0xFF05060A), Color(0xFF2979FF), Color(0xFF7C4DFF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "8K"),
    LiveBackgroundPreset("8k_galaxy", "8K Galaxy", "8K", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "8K"),
    LiveBackgroundPreset("8k_cosmic", "8K Cosmic", "8K", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "8K"),
    LiveBackgroundPreset("8k_abstract", "8K Abstract", "8K", listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFFFF2BD6), Color(0xFF7C4DFF)), isPremium = true, resolutionLabel = "8K"),
    LiveBackgroundPreset("8k_neon_energy", "8K Neon Energy", "8K", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "8K"),
    LiveBackgroundPreset("8k_futuristic", "8K Futuristic", "8K", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF05060A)), isPremium = true, resolutionLabel = "8K")
)
private val ABSTRACT_BACKGROUNDS = listOf(
    LiveBackgroundPreset("abstract_1", "Abstract Flow", "ABSTRACT", listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFFFF2BD6), Color(0xFF7C4DFF))),
    LiveBackgroundPreset("abstract_2", "Liquid Metal", "ABSTRACT", listOf(Color(0xFF10131D), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF10131D))),
    LiveBackgroundPreset("abstract_3", "Neon Silk", "ABSTRACT", listOf(Color(0xFF05060A), Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("abstract_4", "Chrome Wave", "ABSTRACT", listOf(Color(0xFF181C2A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF181C2A)))
)
private val SPACE_BACKGROUNDS = listOf(
    LiveBackgroundPreset("space_1", "Deep Space", "SPACE", listOf(Color(0xFF05060A), Color(0xFF2979FF), Color(0xFF7C4DFF), Color(0xFF05060A))),
    LiveBackgroundPreset("space_2", "Nebula", "SPACE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFFFF2BD6), Color(0xFF05060A))),
    LiveBackgroundPreset("space_3", "Star Cluster", "SPACE", listOf(Color(0xFF05060A), Color(0xFFFFFFFF), Color(0xFF7C4DFF), Color(0xFF05060A))),
    LiveBackgroundPreset("space_4", "Black Hole", "SPACE", listOf(Color(0xFF05060A), Color(0xFF000000), Color(0xFF7C4DFF), Color(0xFF05060A)))
)
private val NEON_BACKGROUNDS = listOf(
    LiveBackgroundPreset("neon_1", "Neon City", "NEON", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0B0D14))),
    LiveBackgroundPreset("neon_2", "Neon Streets", "NEON", listOf(Color(0xFF05060A), Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("neon_3", "Neon Rain", "NEON", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF05060A))),
    LiveBackgroundPreset("neon_4", "Neon Grid", "NEON", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0B0D14)))
)
private val FUTURE_BACKGROUNDS = listOf(
    LiveBackgroundPreset("future_1", "Future Tech", "FUTURE", listOf(Color(0xFF0B0D14), Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF0B0D14))),
    LiveBackgroundPreset("future_2", "Cyber Future", "FUTURE", listOf(Color(0xFF05060A), Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF05060A))),
    LiveBackgroundPreset("future_3", "Digital Future", "FUTURE", listOf(Color(0xFF10131D), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF10131D))),
    LiveBackgroundPreset("future_4", "AI Core", "FUTURE", listOf(Color(0xFF05060A), Color(0xFF00E5FF), Color(0xFFFFFFFF), Color(0xFF05060A)))
)

private val STUDIO_EFFECTS = listOf(
    EffectType("glow", "Glow", Icons.Default.GraphicEq, "Add neon glow to visualizer"),
    EffectType("bloom", "Bloom", Icons.Default.Speed, "Soft bloom light effect"),
    EffectType("particles", "Particles", Icons.Default.Star, "Floating particle system"),
    EffectType("starfield", "Starfield", Icons.Default.Star, "Animated star background"),
    EffectType("light_rays", "Light Rays", Icons.Default.SwapHoriz, "Volumetric light rays"),
    EffectType("lens_flare", "Lens Flare", Icons.Default.Speed, "Cinematic lens flare"),
    EffectType("smoke", "Smoke", Icons.Default.GraphicEq, "Volumetric smoke overlay"),
    EffectType("sparks", "Sparks", Icons.Default.Star, "Electric spark particles"),
    EffectType("eq_glow", "Equalizer Glow", Icons.Default.GraphicEq, "Glow bars reacting to audio"),
    EffectType("vignette", "Vignette", Icons.Default.ScreenLockPortrait, "Dark edges focus effect"),
    EffectType("film_grain", "Film Grain", Icons.Default.Timer, "Classic film grain texture"),
    EffectType("motion_blur", "Motion Blur", Icons.Default.Speed, "Directional motion blur"),
    EffectType("rgb_shift", "RGB Shift", Icons.Default.SwapHoriz, "Chromatic aberration"),
    EffectType("neon_edge", "Neon Edge", Icons.Default.GraphicEq, "Neon edge glow detection")
)

private val STUDIO_COLOR_THEMES = listOf(
    ColorTheme("cyber_purple", "Cyber Purple", Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFFFF2BD6), listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFFFF2BD6))),
    ColorTheme("electric_blue", "Electric Blue", Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF7C4DFF), listOf(Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF7C4DFF))),
    ColorTheme("neon_pink", "Neon Pink", Color(0xFFFF2BD6), Color(0xFF7C4DFF), Color(0xFF00E5FF), listOf(Color(0xFFFF2BD6), Color(0xFF7C4DFF), Color(0xFF00E5FF))),
    ColorTheme("magenta_dream", "Magenta Dream", Color(0xFFFF00A8), Color(0xFF7C4DFF), Color(0xFFFF2BD6), listOf(Color(0xFFFF00A8), Color(0xFF7C4DFF), Color(0xFFFF2BD6))),
    ColorTheme("aurora", "Aurora", Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF00E5FF), listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF00E5FF))),
    ColorTheme("rainbow", "Rainbow", Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF7C4DFF), listOf(Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF00A8))),
    ColorTheme("midnight", "Midnight", Color(0xFF7C4DFF), Color(0xFF2979FF), Color(0xFF7C4DFF), listOf(Color(0xFF7C4DFF), Color(0xFF2979FF), Color(0xFF7C4DFF))),
    ColorTheme("solar_flare", "Solar Flare", Color(0xFFFF2BD6), Color(0xFFFF00A8), Color(0xFFFF2BD6), listOf(Color(0xFFFF2BD6), Color(0xFFFF00A8), Color(0xFFFF2BD6))),
    ColorTheme("ocean", "Ocean", Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF7C4DFF), listOf(Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF7C4DFF))),
    ColorTheme("fire_energy", "Fire Energy", Color(0xFFFF2BD6), Color(0xFFFF00A8), Color(0xFFFF2BD6), listOf(Color(0xFFFF2BD6), Color(0xFFFF00A8), Color(0xFFFF2BD6)))
)

private val QUICK_PRESETS = listOf(
    QuickPreset("cyber_night", "Cyber Night", "Visualizer + Cyber Background + Purple Glow", StudioVisualizerPreset.STEP_BARS, LIVE_BACKGROUNDS.find { it.id == "cyber_city" }!!, STUDIO_EFFECTS.find { it.id == "glow" }!!, STUDIO_COLOR_THEMES.find { it.id == "cyber_purple" }!!),
    QuickPreset("galaxy_dream", "Galaxy Dream", "Circular Visualizer + Galaxy + Stars", StudioVisualizerPreset.GALAXY_RING, LIVE_BACKGROUNDS.find { it.id == "neon_galaxy" }!!, STUDIO_EFFECTS.find { it.id == "starfield" }!!, STUDIO_COLOR_THEMES.find { it.id == "electric_blue" }!!),
    QuickPreset("neon_pulse", "Neon Pulse", "Neon Ring + Neon Waves + Glow", StudioVisualizerPreset.CIRCULAR_SPECTRUM, LIVE_BACKGROUNDS.find { it.id == "neon_waves" }!!, STUDIO_EFFECTS.find { it.id == "glow" }!!, STUDIO_COLOR_THEMES.find { it.id == "neon_pink" }!!),
    QuickPreset("aurora", "Aurora", "Aurora Background + Radial Visualizer", StudioVisualizerPreset.AURORA, LIVE_BACKGROUNDS.find { it.id == "aurora" }!!, STUDIO_EFFECTS.find { it.id == "particles" }!!, STUDIO_COLOR_THEMES.find { it.id == "aurora" }!!),
    QuickPreset("future_energy", "Future Energy", "Energy Ring + Futuristic Background + Light Rays", StudioVisualizerPreset.VORTEX, LIVE_BACKGROUNDS.find { it.id == "futuristic_tunnel" }!!, STUDIO_EFFECTS.find { it.id == "light_rays" }!!, STUDIO_COLOR_THEMES.find { it.id == "cyber_purple" }!!),
    QuickPreset("cinema", "Cinema", "Minimal Visualizer + Cinematic Background", StudioVisualizerPreset.SPECTRUM_BARS, LIVE_BACKGROUNDS.find { it.id == "space_tunnel" }!!, STUDIO_EFFECTS.find { it.id == "vignette" }!!, STUDIO_COLOR_THEMES.find { it.id == "midnight" }!!)
)

// ==========================================
// PROFESSIONAL MP3 → MP4 STUDIO COMPONENTS
// ==========================================

@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun VisualizerCard(
    preset: StudioVisualizerPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (selected) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            else listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = preset.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BackgroundPreviewCard(
    preset: LiveBackgroundPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors = preset.gradient))
            )
            if (preset.isPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = preset.resolutionLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = preset.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
                Text(
                    text = preset.resolutionLabel,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EffectChip(
    effect: EffectType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = effect.icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = effect.displayName,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ThemeChip(
    theme: ColorTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = theme.gradient))
            )
            Text(
                text = theme.displayName,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: QuickPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(colors = preset.theme.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FormatSelector(
    selected: VideoAspectRatio,
    onSelect: (VideoAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VideoAspectRatio.values().forEach { ratio ->
            val isSelected = selected == ratio
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSelect(ratio) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = ratio.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun QualitySelector(
    selected: VideoResolution,
    onSelect: (VideoResolution) -> Unit,
    premiumStore: PremiumUnlockStore,
    onRequestUnlock: (String, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VideoResolution.values().forEach { res ->
            val isLockedPremium = when (res) {
                VideoResolution.FHD_1080 -> !premiumStore.isUnlocked(PremiumFeature.EXPORT_1080P).collectAsState(initial = false).value
                VideoResolution.RES_4K -> !premiumStore.isUnlocked(PremiumFeature.EXPORT_4K).collectAsState(initial = false).value
                VideoResolution.RES_8K -> !premiumStore.isUnlocked(PremiumFeature.EXPORT_8K).collectAsState(initial = false).value
                else -> false
            }
            val isSelected = selected == res && !isLockedPremium
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable {
                        if (isLockedPremium) {
                            val feature = when (res) {
                                VideoResolution.FHD_1080 -> PremiumFeature.EXPORT_1080P
                                VideoResolution.RES_4K -> PremiumFeature.EXPORT_4K
                                VideoResolution.RES_8K -> PremiumFeature.EXPORT_8K
                                else -> PremiumFeature.EXPORT_1080P
                            }
                            val label = when (res) {
                                VideoResolution.FHD_1080 -> "1080p Export"
                                VideoResolution.RES_4K -> "4K Export"
                                VideoResolution.RES_8K -> "8K Export"
                                else -> "Export"
                            }
                            onRequestUnlock(feature, label) { onSelect(res) }
                        } else {
                            onSelect(res)
                        }
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = res.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (isLockedPremium) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 1. AUDIO CONVERTER SCREEN ---

@Composable
fun ConverterToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var targetFormat by remember { mutableStateOf(AudioFormat.M4A) }
    var outputFileName by remember { mutableStateOf("Converted_Audio_Track") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Audio Converter Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio File to Convert", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Target Audio Format", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Only formats the Android platform can genuinely encode are
                // offered, so no export is ever mislabelled.
                val formats = AudioFormat.values().filter { it.encodable }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { format ->
                        val isSelected = targetFormat == format
                        Card(
                            modifier = Modifier.weight(1f).height(50.dp).clickable { targetFormat = format },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(format.name, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Text(
                        "Conversion re-encodes the audio for real (decode to PCM, then " +
                            "encode). WAV is written as uncompressed PCM; AAC/M4A is encoded " +
                            "with MediaCodec into an MP4 container.\n\n" +
                            "MP3, FLAC and OGG are not listed because Android has no built-in " +
                            "encoder for them — only decoders. Offering them would mean writing " +
                            "an AAC stream under a false extension.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert Audio File", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another file", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.convertAudio(uri, outputFileName, targetFormat)
                        }
                    }
                ) {
                    Text("Export", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Helper time formatter for preview screen
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

// Helper bytes size formatter for preview screen
private fun formatBytes(bytes: Long): String {
    val sizeMb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(java.util.Locale.getDefault(), "%.2f MB", sizeMb)
}

@Composable
fun VideoPreviewEditScreen(
    file: com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile,
    viewModel: AudioStudioViewModel,
    onNavigateBackToStudio: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Initialize ExoPlayer to play the newly generated MP4 video file
    val videoUri = Uri.parse(file.uriString)
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onNavigateBackToStudio()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preview & Edit Video", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Professional ExoPlayer View container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Metadata details panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Render details:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Filename: ${file.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Duration: ${formatTime(file.duration)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Text("Filesize: ${formatBytes(file.size)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Premium action buttons row: Save, Share, Discard/Re-render
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Discard/Re-render
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deleteExport(file)
                    onNavigateBackToStudio()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Discard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }

            // Share
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.shareExport(file)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1.2f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Share", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            // Save to Device
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToLibrary()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.5f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save to Device", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// --- 5. PREMIUM VIDEO STUDIO COMPONENT ---

enum class VideoStudioType(val displayName: String, val description: String) {
    MP3_TO_MP4("MP3 → MP4 Visualizer", "Standard spectrum visualizer video from MP3 files."),
    MP3_HD("MP3-HD Video", "Convert MP3 to HD video with album art and visualizer — instant export."),
    ALBUM_ART("Album Art Video", "Overlay rotating or static album artwork in the center of the video."),
    LYRICS("Lyrics Video", "Incorporate synced text/lrc lyric sheets with beautiful backdrop flows."),
    WAVEFORM("Waveform Video", "Horizontal linear fluid waveforms drawing across the screen."),
    SPECTRUM("Spectrum Video", "Traditional dual-ended spectrogram bars responsive to sines."),
    NEON("Neon Video", "High-frequency neon colors and glowing elements outlining the waveform."),
    CIRCULAR("Circular Visualizer Video", "Dynamic concentric circular rings scaling with transients."),
    AUDIO_STATUS("Audio Status Video", "Compact, highly stylized status video overlay with track metadata."),
    STORY_9_16("Story Video (9:16)", "Portrait mode video layout customized for Instagram/TikTok stories."),
    YOUTUBE_16_9("YouTube Landscape (16:9)", "Cinema landscape layout with wide screen visualization ratios.")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    type: VideoStudioType,
    viewModel: AudioStudioViewModel,
    premiumStore: PremiumUnlockStore,
    onRequestUnlock: (String, String, () -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val haptic = LocalHapticFeedback.current
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val spectrum by viewModel.spectrumTrack.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // ---- Config state. Every one of these fields is consumed by the exporter. ----
    // The preset picker uses the SAME "Visualizer Studio Pro" library
    // (com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerPreset)
    // as the Now Playing screen, so the user sees one consistent, categorised
    // preset list everywhere. It is converted to the renderer's
    // VideoVisualizerPreset when the config is built.
    var preset by remember {
        mutableStateOf(
            when (type) {
                VideoStudioType.WAVEFORM -> VisualizerPreset.DREAM_FLOW
                VideoStudioType.SPECTRUM -> VisualizerPreset.VERTICAL_BARS
                VideoStudioType.CIRCULAR -> VisualizerPreset.CIRCULAR_RADIAL_BARS
                VideoStudioType.NEON -> VisualizerPreset.GLOW_BARS
                VideoStudioType.ALBUM_ART -> VisualizerPreset.GALAXY_SPIN
                else -> VisualizerPreset.CIRCULAR_RADIAL_BARS
            }
        )
    }
    var aspectRatio by remember {
        mutableStateOf(
            when (type) {
                VideoStudioType.STORY_9_16 -> VideoAspectRatio.RATIO_9_16
                VideoStudioType.AUDIO_STATUS -> VideoAspectRatio.RATIO_1_1
                else -> VideoAspectRatio.RATIO_16_9
            }
        )
    }
    var resolution by remember { mutableStateOf(VideoResolution.HD_720) }
    var fps by remember { mutableStateOf(30) }
    var bgStyle by remember { mutableStateOf(VideoBackgroundStyle.DARK_GRADIENT) }
    var bgFit by remember { mutableStateOf(BackgroundFit.CROP) }
    var bgImageUri by remember { mutableStateOf<String?>(null) }
    var bgDim by remember { mutableStateOf(0.35f) }
    var titleText by remember { mutableStateOf("") }
    var artistText by remember { mutableStateOf("") }
    // Generic title overlay is OFF by default — it was an unwanted artifact burned
    // onto the preview and exported MP4. Users can still opt in via the switch.
    var showText by remember { mutableStateOf(false) }
    var glow by remember { mutableStateOf(true) }
    // Built-in background music (layered UNDER the source audio).
    var bgTrackResName by remember { mutableStateOf<String?>(null) }
    var bgTrackVolume by remember { mutableStateOf(0.35f) }
    // The Pulse logo watermark is now permanently burned into every exported
    // MP4 — there is no longer a user toggle, so no state is needed here.
    var trimStartMs by remember { mutableStateOf(0L) }
    var trimEndMs by remember { mutableStateOf(0L) }
    var outputFileName by remember { mutableStateOf("Pulse_${type.name}_Video") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    // Selection state for the four export sections — declared before [config]
    // so the single source-of-truth config below can read the current values.
    /** ARGB colour ints of the chosen ANIMATION BACKGROUND gradient (null = none). */
    var bgGradient by remember { mutableStateOf<List<Int>?>(null) }
    var selectedEffectId by remember { mutableStateOf<String?>(null) }
    var selectedThemeId by remember { mutableStateOf<String?>(null) }

    // ── VISUALIZER CROP & POSITION EDITOR state ──
    // Declared BEFORE [config] so the single source-of-truth object can read
    // every value. Each slider/button below updates one of these, and the
    // preview + exporter both consume them via [config].
    var vizScale by remember { mutableStateOf(1.0f) }
    var vizScaleX by remember { mutableStateOf(1.0f) }
    var vizScaleY by remember { mutableStateOf(1.0f) }
    var vizOffsetX by remember { mutableStateOf(0.0f) }
    var vizOffsetY by remember { mutableStateOf(0.0f) }
    var vizRotation by remember { mutableStateOf(0.0f) }
    var vizPosY by remember { mutableStateOf(0.6f) }

    val selectedTheme = STUDIO_COLOR_THEMES.firstOrNull { it.id == selectedThemeId }
    val config = VisualizerVideoConfig(
        preset = preset.toVideoPreset(),
        aspectRatio = aspectRatio,
        resolution = resolution,
        fps = fps,
        startMs = trimStartMs,
        endMs = trimEndMs,
        title = titleText,
        artist = artistText,
        showText = showText,
        backgroundImageUri = bgImageUri,
        backgroundFit = bgFit,
        backgroundStyle = bgStyle,
        backgroundGradient = bgGradient,
        backgroundDim = bgDim,
        visualizerScale = vizScale,
        visualizerScaleX = vizScaleX,
        visualizerScaleY = vizScaleY,
        visualizerOffsetX = vizOffsetX,
        visualizerOffsetY = vizOffsetY,
        visualizerRotation = vizRotation,
        visualizerPositionY = vizPosY,
        glow = glow,
        backgroundTrackResName = bgTrackResName,
        backgroundTrackVolume = bgTrackVolume,
        backgroundMood = BuiltInBackgroundTracks.resolve(bgTrackResName)?.mood,
        effect = VideoEffect.fromId(selectedEffectId),
        themePrimary = selectedTheme?.primary?.toArgb(),
        themeSecondary = selectedTheme?.secondary?.toArgb(),
        themeTertiary = selectedTheme?.tertiary?.toArgb(),
        outputName = outputFileName
    )

    var expandedVisualizer by remember { mutableStateOf(true) }
    var expandedBackground by remember { mutableStateOf(false) }
    var expandedEffects by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) }
    var expandedPresets by remember { mutableStateOf(false) }
    var expandedFormat by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }
    var expandedCrop by remember { mutableStateOf(false) }
    var expandedAdvanced by remember { mutableStateOf(false) }
    var selectedBgCategory by remember { mutableStateOf("LIVE") }
    var selectedBackgroundId by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importError = null
            viewModel.selectFiles(listOf(uri))
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // A custom photo from device storage becomes the static background and
            // takes precedence over any preset gradient. GetContent("image/*")
            // uses the system picker with scoped URI access, so no
            // READ_MEDIA_IMAGES permission is required on Android 13+.
            bgImageUri = uri.toString()
            bgStyle = VideoBackgroundStyle.DARK_GRADIENT
            bgGradient = null
            selectedBackgroundId = null
        }
    }

    val sourceUri = selectedFiles.firstOrNull()

    // ---- Real preview playback of the selected audio ----
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build()
    }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // Load the picked file into the preview player and kick off the real
    // spectrum analysis that drives the visualizer.
    LaunchedEffect(sourceUri) {
        positionMs = 0L
        isPreviewPlaying = false
        importError = null
        if (sourceUri != null) {
            try {
                exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(sourceUri))
                exoPlayer.prepare()
                viewModel.analyzeForPreview(sourceUri, fps)
            } catch (e: Exception) {
                importError = "Couldn't load this audio file. Try a different file or check that the file isn't corrupted. (${e.message})"
                exoPlayer.clearMediaItems()
                viewModel.clearPreviewAnalysis()
            }
        } else {
            exoPlayer.clearMediaItems()
            viewModel.clearPreviewAnalysis()
        }
    }

    // Drive the live preview clock from the real player position.
    LaunchedEffect(isPreviewPlaying, sourceUri) {
        while (isPreviewPlaying) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val d = exoPlayer.duration
            if (d > 0) durationMs = d
            kotlinx.coroutines.delay(33L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // CLEAN TOP BAR: The back arrow and settings gear icon were removed —
        // they were covering the screen. The top of this screen now shows ONLY
        // the video preview area, with no back arrow, no gear icon, no text,
        // and no other objects there.

        if (sourceUri == null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio Track (MP3, WAV, M4A)", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(type.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {

                // ---------------- LIVE PREVIEW (aspect-ratio-safe) ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    BoxWithConstraints {
                        val previewHeight = when (aspectRatio) {
                            VideoAspectRatio.RATIO_16_9 -> (maxWidth * 9 / 16).coerceAtMost(280.dp)
                            VideoAspectRatio.RATIO_9_16 -> (maxWidth * 16 / 9).coerceAtMost(280.dp)
                            VideoAspectRatio.RATIO_1_1 -> maxWidth.coerceAtMost(280.dp)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(previewHeight)) {
                            LiveVisualizerPreview(
                                config = config,
                                spectrum = spectrum,
                                positionMs = positionMs + trimStartMs,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ---------------- TRANSPORT CONTROLS ----------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isPreviewPlaying) {
                                exoPlayer.pause()
                                isPreviewPlaying = false
                            } else {
                                exoPlayer.play()
                                isPreviewPlaying = true
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewPlaying) "Pause preview" else "Play preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(formatTime(positionMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = positionMs.toFloat(),
                        onValueChange = {
                            positionMs = it.toLong()
                            exoPlayer.seekTo(it.toLong())
                        },
                        valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatTime(durationMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ---------------- SCROLLABLE SETTINGS (single LazyColumn, no nested scroll) ----------------
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        if (importError != null) {
                            Text(
                                importError!!,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (isAnalyzing) {
                            Text(
                                "Analysing real audio spectrum… $analysisProgress%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = analysisProgress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (spectrum == null) {
                            // Only shown when analysis could not produce ANY data
                            // (real decode failed AND synthetic fallback failed) —
                            // the preview now falls back to a synthetic energy
                            // curve so visualizers always animate.
                            Text(
                                "Audio analysis unavailable for this file — preview cannot react to it.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            // Real spectrum (or synthetic fallback) is loaded —
                            // every visualizer now reacts to bass, beats, and
                            // frequencies in real time.
                            Text(
                                "Audio spectrum loaded — visualizers react live",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ---- LIVE VISUALIZER ----
                    item {
                        CollapsibleSection(title = "LIVE VISUALIZER", expanded = expandedVisualizer, onToggle = { expandedVisualizer = !expandedVisualizer }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 2
                            ) {
                                STUDIO_VISUALIZERS.forEach { viz ->
                                    VisualizerCard(
                                        preset = viz,
                                        selected = preset.toVideoPreset() == viz.mappedPreset.toVideoPreset(),
                                        onClick = { preset = viz.mappedPreset },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- ANIMATION BACKGROUNDS ----
                    item {
                        CollapsibleSection(title = "ANIMATION BACKGROUNDS", expanded = expandedBackground, onToggle = { expandedBackground = !expandedBackground }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("LIVE", "4K", "8K", "ABSTRACT", "SPACE", "NEON", "FUTURE").forEach { cat ->
                                    val isSelected = selectedBgCategory == cat
                                    Card(
                                        modifier = Modifier.height(32.dp).clickable { selectedBgCategory = cat },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                            Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val backgroundsForCategory = when (selectedBgCategory) {
                                "4K" -> CINEMATIC_4K_BACKGROUNDS
                                "8K" -> CINEMATIC_8K_BACKGROUNDS
                                "ABSTRACT" -> ABSTRACT_BACKGROUNDS
                                "SPACE" -> SPACE_BACKGROUNDS
                                "NEON" -> NEON_BACKGROUNDS
                                "FUTURE" -> FUTURE_BACKGROUNDS
                                else -> LIVE_BACKGROUNDS
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 2
                            ) {
                                backgroundsForCategory.forEach { bg ->
                                    BackgroundPreviewCard(
                                        preset = bg,
                                        selected = selectedBackgroundId == bg.id,
                                        onClick = {
                                            selectedBackgroundId = bg.id
                                            bgGradient = bg.gradient.map { it.toArgb() }
                                            bgImageUri = null
                                            when (bg.category) {
                                                "4K" -> resolution = VideoResolution.RES_4K
                                                "8K" -> resolution = VideoResolution.RES_8K
                                                else -> resolution = VideoResolution.HD_720
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // ---- Pick Background Image from device storage ----
                            // Restored feature: opens the Android system image
                            // picker (ActivityResultContracts.PickVisualMedia) so the
                            // user can choose any photo as the static background. The
                            // picked URI is scoped (no READ_MEDIA_IMAGES permission
                            // required on Android 13+) and applied to the export.
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Custom Background",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (bgImageUri == null) "Pick Background Image" else "Change Image",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (bgImageUri != null) {
                                    Button(
                                        onClick = {
                                            bgImageUri = null
                                            bgGradient = null
                                            selectedBackgroundId = null
                                        },
                                        modifier = Modifier.height(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                    ) {
                                        Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (bgImageUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Background Fit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BackgroundFit.values().forEach { f ->
                                        val isSel = bgFit == f
                                        Card(
                                            modifier = Modifier.weight(1f).height(40.dp).clickable { bgFit = f },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(f.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Background Dim (${(bgDim * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Slider(value = bgDim, onValueChange = { bgDim = it }, valueRange = 0f..0.9f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- VIDEO EFFECTS ----
                    item {
                        CollapsibleSection(title = "VIDEO EFFECTS", expanded = expandedEffects, onToggle = { expandedEffects = !expandedEffects }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                STUDIO_EFFECTS.forEach { effect ->
                                    EffectChip(
                                        effect = effect,
                                        selected = selectedEffectId == effect.id,
                                        onClick = {
                                            // Toggle the selected effect; only one
                                            // effect may be active at a time and it is
                                            // carried to the export via config.effect.
                                            selectedEffectId = if (selectedEffectId == effect.id) null else effect.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- COLOR THEME ----
                    item {
                        CollapsibleSection(title = "COLOR THEME", expanded = expandedTheme, onToggle = { expandedTheme = !expandedTheme }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                STUDIO_COLOR_THEMES.forEach { theme ->
                                    ThemeChip(
                                        theme = theme,
                                        selected = selectedThemeId == theme.id,
                                        onClick = {
                                            // Toggle the colour theme; it overrides the
                                            // visualizer's accent/secondary colours via
                                            // config.themePrimary/Secondary and changes
                                            // the exported video's colour scheme.
                                            selectedThemeId = if (selectedThemeId == theme.id) null else theme.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- QUICK PRESETS ----
                    item {
                        CollapsibleSection(title = "QUICK PRESETS", expanded = expandedPresets, onToggle = { expandedPresets = !expandedPresets }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                QUICK_PRESETS.forEach { preset ->
                                    PresetCard(preset = preset, selected = false, onClick = { })
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- VIDEO FORMAT ----
                    item {
                        CollapsibleSection(title = "VIDEO FORMAT", expanded = expandedFormat, onToggle = { expandedFormat = !expandedFormat }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FormatSelector(selected = aspectRatio, onSelect = { aspectRatio = it })
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- EXPORT QUALITY ----
                    item {
                        CollapsibleSection(title = "EXPORT QUALITY", expanded = expandedQuality, onToggle = { expandedQuality = !expandedQuality }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            QualitySelector(selected = resolution, onSelect = { resolution = it }, premiumStore = premiumStore, onRequestUnlock = onRequestUnlock)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Frame Rate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(24, 30, 60).forEach { f ->
                                    val isSelected = fps == f
                                    Card(
                                        modifier = Modifier.height(36.dp).clickable { fps = f; viewModel.analyzeForPreview(sourceUri, f) },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                            Text("$f fps", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- VISUALIZER CROP & POSITION EDITOR ----
                    // NEW FEATURE: every control here is wired to the config that
                    // drives both the live preview and the exported MP4, so moving
                    // / resizing / rotating the visualizer updates the video in
                    // real time. No placeholders — full backend logic below.
                    item {
                        CollapsibleSection(title = "VISUALIZER CROP & POSITION", expanded = expandedCrop, onToggle = { expandedCrop = !expandedCrop }) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // ── MOVE ──
                            Text("Move Visualizer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val moveBtnW = Modifier.weight(1f).height(36.dp)
                                Button(onClick = { vizOffsetY = (vizOffsetY - 0.15f).coerceIn(-1f, 1f) }, modifier = moveBtnW, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Up", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                                Button(onClick = { vizOffsetY = (vizOffsetY + 0.15f).coerceIn(-1f, 1f) }, modifier = moveBtnW, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Down", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                                Button(onClick = { vizOffsetX = (vizOffsetX - 0.15f).coerceIn(-1f, 1f) }, modifier = moveBtnW, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Left", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                                Button(onClick = { vizOffsetX = (vizOffsetX + 0.15f).coerceIn(-1f, 1f) }, modifier = moveBtnW, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Right", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { vizOffsetX = 0f; vizOffsetY = 0f },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Center", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Offset X: ${String.format(java.util.Locale.getDefault(), "%+.2f", vizOffsetX)}   Offset Y: ${String.format(java.util.Locale.getDefault(), "%+.2f", vizOffsetY)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── CROP (width / height / scale) ──
                            Text("Crop Width (${String.format(java.util.Locale.getDefault(), "%.2f", vizScaleX)}x)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = vizScaleX, onValueChange = { vizScaleX = it }, valueRange = 0.4f..1.6f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                            Text("Crop Height (${String.format(java.util.Locale.getDefault(), "%.2f", vizScaleY)}x)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = vizScaleY, onValueChange = { vizScaleY = it }, valueRange = 0.4f..1.6f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                            Text("Master Scale (${String.format(java.util.Locale.getDefault(), "%.2f", vizScale)}x)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = vizScale, onValueChange = { vizScale = it }, valueRange = 0.4f..1.6f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))

                            Spacer(modifier = Modifier.height(8.dp))

                            // ── ROTATION ──
                            Text("Rotation (${vizRotation.toInt()}°)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = vizRotation, onValueChange = { vizRotation = it }, valueRange = 0f..360f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))

                            Spacer(modifier = Modifier.height(8.dp))

                            // ── RESET ──
                            Button(
                                onClick = { vizScale = 1.0f; vizScaleX = 1.0f; vizScaleY = 1.0f; vizOffsetX = 0f; vizOffsetY = 0f; vizRotation = 0f; vizPosY = 0.6f },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            ) {
                                Text("Reset All Crop & Position", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ---- ADVANCED SETTINGS (restored — every control below is wired
                    // to the config that drives the live preview and the export) ----
                    item {
                        CollapsibleSection(title = "ADVANCED SETTINGS", expanded = expandedAdvanced, onToggle = { expandedAdvanced = !expandedAdvanced }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Show Title / Artist Overlay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                androidx.compose.material3.Switch(checked = showText, onCheckedChange = { showText = it })
                            }
                            if (showText) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = titleText, onValueChange = { titleText = it }, label = { Text("Title text") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = artistText, onValueChange = { artistText = it }, label = { Text("Artist text") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Glow Effect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                androidx.compose.material3.Switch(checked = glow, onCheckedChange = { glow = it })
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Visualizer Vertical Position (${(vizPosY * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(value = vizPosY, onValueChange = { vizPosY = it }, valueRange = 0.15f..0.9f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                            if (durationMs > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Trim Start (${formatTime(trimStartMs)})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Slider(value = trimStartMs.toFloat(), onValueChange = { trimStartMs = it.toLong() }, valueRange = 0f..durationMs.toFloat(), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                                Text(if (trimEndMs > trimStartMs) "Trim End (${formatTime(trimEndMs)})" else "Trim End (end of track)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Slider(value = trimEndMs.toFloat(), onValueChange = { trimEndMs = it.toLong() }, valueRange = 0f..durationMs.toFloat(), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // ---------------- ACTION BUTTONS ----------------
                // All three buttons share ONE consistent size (48.dp tall, equal
                // weight) so they line up as a single professional row. Each
                // button's text is forced onto a single line and centered inside
                // the button — previously the Preview/Export icons+text overflowed
                // the 44.dp button and the label rendered below the button face.
                val actionBtnHeight = 48.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.analyzeForPreview(sourceUri, fps)
                        },
                        modifier = Modifier.weight(1f).height(actionBtnHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            if (isPreviewPlaying) {
                                exoPlayer.pause()
                                isPreviewPlaying = false
                            } else {
                                exoPlayer.play()
                                isPreviewPlaying = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(actionBtnHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPreviewPlaying) "Pause" else "Preview", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        enabled = spectrum != null && !isAnalyzing,
                        modifier = Modifier.weight(1f).height(actionBtnHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export MP4", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another audio file", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        exoPlayer.pause()
                        isPreviewPlaying = false
                        val exportConfig = config.copy(outputName = outputFileName)
                        // The Pulse watermark is now permanently burned into every
                        // exported MP4 (no toggle), so export directly.
                        if (sourceUri != null) {
                            viewModel.exportVisualizerVideo(sourceUri, exportConfig)
                        }
                    }
                ) {
                    Text("Export", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // NOTE: The export progress HUD (single progress indicator + one Cancel
    // button) is owned by the parent AudioToolsScreen so that this screen does
    // not render a duplicate, stacked progress box during export.
}

// --- 2. AUDIO EXTRACTOR SCREEN ---

@Composable
fun ExtractorToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var outputFormat by remember { mutableStateOf("MP3") }
    var outputFileName by remember { mutableStateOf("Extracted_Video_Track") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Extract Audio from Video", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("video/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Video File (MP4, MKV, AVI, MOV)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Video"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Output Audio Format", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("MP3", "AAC").forEach { format ->
                        val isSelected = outputFormat == format
                        Card(
                            modifier = Modifier.weight(1f).height(55.dp).clickable { outputFormat = format },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(format, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Audio Stream", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another video", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.extractAudio(uri, outputFileName, outputFormat)
                        }
                    }
                ) {
                    Text("Extract Track", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 3. AUDIO COMPRESSOR SCREEN ---

@Composable
fun CompressorToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var selectedPreset by remember { mutableStateOf(CompressionPreset.MEDIUM) }
    var outputFileName by remember { mutableStateOf("Compressed_Audio_File") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Acoustic Compressor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio to Compress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Compression Level Presets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    Pair(CompressionPreset.LOW, "Low Compression (Highest Audio Fidelity, Largest File Size)"),
                    Pair(CompressionPreset.MEDIUM, "Medium Compression (Standard Balance of File Size & Clarity)"),
                    Pair(CompressionPreset.HIGH, "High Compression (Ultra Small File Size, Lower Bitrate)")
                ).forEach { (preset, desc) ->
                    val isSelected = selectedPreset == preset
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedPreset = preset },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(preset.name, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compress File Size", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another track", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.compressAudio(uri, outputFileName, selectedPreset)
                        }
                    }
                ) {
                    Text("Compress", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 4. SPEED & PITCH CHANGER SCREEN ---

@Composable
fun SpeedPitchToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    var selectedPitch by remember { mutableStateOf(1.0f) }
    var outputFileName by remember { mutableStateOf("Resampled_Audio") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Speed & Pitch Lab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio to Modify", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Speed Selector (0.5x, 0.75x, 1.25x, 1.5x, 2.0x)
                Text("Select Target Speed Export", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    speeds.take(3).forEach { speed ->
                        val isSelected = selectedSpeed == speed
                        Card(
                            modifier = Modifier.weight(1f).height(45.dp).clickable { selectedSpeed = speed },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${speed}x", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    speeds.takeLast(3).forEach { speed ->
                        val isSelected = selectedSpeed == speed
                        Card(
                            modifier = Modifier.weight(1f).height(45.dp).clickable { selectedSpeed = speed },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${speed}x", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Pitch Selector Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Target Pitch Adjustment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(String.format("%.2fx", selectedPitch), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = selectedPitch,
                    onValueChange = { selectedPitch = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export with Speed & Pitch", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another track", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.changeSpeedAndPitch(uri, outputFileName, selectedSpeed, selectedPitch)
                        }
                    }
                ) {
                    Text("Export Resampled", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * "Visualizer Studio Pro" preset picker, shared styling with the Now Playing
 * screen. Exposes the SAME categorised [VisualizerPreset] library (search +
 * category chips + grid) so the MP3→MP4 export flow offers every preset the
 * user can pick for live playback. The chosen [VisualizerPreset] is converted to
 * the renderer's [com.salmanlaghari.pulsemusicplayerai.domain.model.VideoVisualizerPreset]
 * when the export config is built, so the exact selected look is rendered into
 * the final MP4.
 */
@Composable
fun VisualizerStudioProPicker(
    selected: VisualizerPreset,
    onSelect: (VisualizerPreset) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }

    val categories = remember {
        listOf("All") + VisualizerPreset.values().map { it.category }.distinct()
    }
    val filtered = remember(query, category) {
        VisualizerPreset.values().filter { p ->
            val matchesCat = category == "All" || p.category == category
            val q = query.trim()
            val matchesQuery = q.isBlank() ||
                    p.displayName.contains(q, ignoreCase = true) ||
                    p.category.contains(q, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Visualizer Studio Pro",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search presets…", fontSize = 12.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category chips (horizontal scroll)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSelected = category == cat
                Card(
                    modifier = Modifier.height(34.dp).clickable { category = cat },
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset options — horizontal scroll rows of large tappable cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filtered) { p ->
                val isSelected = selected == p
                Card(
                    modifier = Modifier
                        .height(56.dp)
                        .clickable { onSelect(p) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            p.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single selectable option used by [HorizontalChipRow]. Mirrors the premium,
 * tactile chip rows found in studio apps (CapCut / InShot): horizontally
 * scrollable, with the active option highlighted in the accent colour.
 */
private data class ChipOption(
    val label: String,
    val selected: Boolean,
    val locked: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Premium horizontal-scroll chip row for a setting category (Resolution, Frame
 * Rate, Background, Aspect Ratio, …). Replaces the old stacked, equal-width
 * cards so the settings screen scrolls horizontally per group instead of
 * stacking everything vertically — matching the CapCut/InShot studio feel.
 */
@Composable
private fun HorizontalChipRow(options: List<ChipOption>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(options) { opt ->
            val containerColor = when {
                opt.locked -> MaterialTheme.colorScheme.surfaceVariant
                opt.selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = when {
                opt.locked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                opt.selected -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }
            Card(
                modifier = Modifier
                    .height(42.dp)
                    .clickable(enabled = !opt.locked) { opt.onClick() },
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = if (opt.selected && !opt.locked)
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        opt.label,
                        fontSize = 12.sp,
                        fontWeight = if (opt.selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1
                    )
                    if (opt.locked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
