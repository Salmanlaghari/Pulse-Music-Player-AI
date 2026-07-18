package com.salmanlaghari.pulsemusicplayerai.presentation.templatestudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerPreset
import com.salmanlaghari.pulsemusicplayerai.utils.UserTier
import org.json.JSONObject
import java.io.File

data class VisualizerTemplate(
    val id: String,
    val name: String,
    val visualizerType: String = "CIRCULAR_BARS",
    val primaryColorHex: String = "#7C4DFF",
    val secondaryColorHex: String = "#00E5FF",
    val gradientEnabled: Boolean = true,
    val bgStyle: String = "Album Art",
    val rotateArtwork: Boolean = true,
    val blurRadius: Float = 15f,
    val glowRadius: Float = 10f,
    val particlesCount: Int = 100,
    val beatSensitivity: Float = 1.0f,
    val animSpeed: Float = 1.0f,
    val rotationAngle: Float = 0f,
    val textStyle: String = "Modern Bold",
    val lyricsStyle: String = "Neon Highlight",
    val fontName: String = "Default",
    val opacity: Float = 0.85f,
    val borderThickness: Float = 2.0f
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("visualizerType", visualizerType)
        json.put("primaryColorHex", primaryColorHex)
        json.put("secondaryColorHex", secondaryColorHex)
        json.put("gradientEnabled", gradientEnabled)
        json.put("bgStyle", bgStyle)
        json.put("rotateArtwork", rotateArtwork)
        json.put("blurRadius", blurRadius.toDouble())
        json.put("glowRadius", glowRadius.toDouble())
        json.put("particlesCount", particlesCount)
        json.put("beatSensitivity", beatSensitivity.toDouble())
        json.put("animSpeed", animSpeed.toDouble())
        json.put("rotationAngle", rotationAngle.toDouble())
        json.put("textStyle", textStyle)
        json.put("lyricsStyle", lyricsStyle)
        json.put("fontName", fontName)
        json.put("opacity", opacity.toDouble())
        json.put("borderThickness", borderThickness.toDouble())
        return json
    }

    companion object {
        fun fromJson(jsonStr: String): VisualizerTemplate {
            val json = JSONObject(jsonStr)
            return VisualizerTemplate(
                id = json.optString("id", System.currentTimeMillis().toString()),
                name = json.optString("name", "Unnamed Template"),
                visualizerType = json.optString("visualizerType", "CIRCULAR_BARS"),
                primaryColorHex = json.optString("primaryColorHex", "#7C4DFF"),
                secondaryColorHex = json.optString("secondaryColorHex", "#00E5FF"),
                gradientEnabled = json.optBoolean("gradientEnabled", true),
                bgStyle = json.optString("bgStyle", "Album Art"),
                rotateArtwork = json.optBoolean("rotateArtwork", true),
                blurRadius = json.optDouble("blurRadius", 15.0).toFloat(),
                glowRadius = json.optDouble("glowRadius", 10.0).toFloat(),
                particlesCount = json.optInt("particlesCount", 100),
                beatSensitivity = json.optDouble("beatSensitivity", 1.0).toFloat(),
                animSpeed = json.optDouble("animSpeed", 1.0).toFloat(),
                rotationAngle = json.optDouble("rotationAngle", 0.0).toFloat(),
                textStyle = json.optString("textStyle", "Modern Bold"),
                lyricsStyle = json.optString("lyricsStyle", "Neon Highlight"),
                fontName = json.optString("fontName", "Default"),
                opacity = json.optDouble("opacity", 0.85).toFloat(),
                borderThickness = json.optDouble("borderThickness", 2.0).toFloat()
            )
        }
    }
}

class TemplateManager(private val context: Context) {
    private val templatesDir = File(context.filesDir, "templates").apply { mkdirs() }

    init {
        if (getAllTemplates().isEmpty()) {
            saveTemplate(
                VisualizerTemplate(
                    id = "default_pulse",
                    name = "Pulse Premium Default",
                    visualizerType = "CIRCULAR_BARS",
                    primaryColorHex = "#7C4DFF",
                    secondaryColorHex = "#00E5FF"
                )
            )
        }
    }

    fun getAllTemplates(): List<VisualizerTemplate> {
        val files = templatesDir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                VisualizerTemplate.fromJson(file.readText())
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name }
    }

    fun saveTemplate(template: VisualizerTemplate) {
        val file = File(templatesDir, "${template.id}.json")
        file.writeText(template.toJsonObject().toString(4))
    }

    fun deleteTemplate(id: String) {
        File(templatesDir, "$id.json").delete()
    }

    fun duplicateTemplate(template: VisualizerTemplate): VisualizerTemplate {
        val duplicated = template.copy(
            id = System.currentTimeMillis().toString(),
            name = "${template.name} (Copy)"
        )
        saveTemplate(duplicated)
        return duplicated
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateStudioScreen(
    musicViewModel: MusicViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val templateManager = remember { TemplateManager(context) }
    val activeTier by musicViewModel.activeTier.collectAsState(initial = UserTier.GUEST)

    var templatesList by remember { mutableStateOf(emptyList<VisualizerTemplate>()) }
    var currentEditingTemplate by remember { mutableStateOf<VisualizerTemplate?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showPremiumUpgrade by remember { mutableStateOf(false) }

    val reloadTemplates = {
        templatesList = templateManager.getAllTemplates()
    }

    LaunchedEffect(Unit) {
        reloadTemplates()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().readText()
                    val imported = VisualizerTemplate.fromJson(content)
                    val finalTemplate = imported.copy(id = System.currentTimeMillis().toString())
                    templateManager.saveTemplate(finalTemplate)
                    reloadTemplates()
                    Toast.makeText(context, "Template imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: Invalid template file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showPremiumUpgrade) {
        com.salmanlaghari.pulsemusicplayerai.presentation.ui.PremiumUpgradeDialog(
            viewModel = musicViewModel,
            onDismiss = { showPremiumUpgrade = false }
        )
    }

    if (currentEditingTemplate != null || isCreatingNew) {
        val template = currentEditingTemplate ?: VisualizerTemplate(
            id = System.currentTimeMillis().toString(),
            name = "New Custom Template"
        )

        var templateName by remember { mutableStateOf(template.name) }
        var visualizerType by remember { mutableStateOf(template.visualizerType) }
        var primaryColorHex by remember { mutableStateOf(template.primaryColorHex) }
        var secondaryColorHex by remember { mutableStateOf(template.secondaryColorHex) }
        var gradientEnabled by remember { mutableStateOf(template.gradientEnabled) }
        var bgStyle by remember { mutableStateOf(template.bgStyle) }
        var rotateArtwork by remember { mutableStateOf(template.rotateArtwork) }
        var blurRadius by remember { mutableStateOf(template.blurRadius) }
        var glowRadius by remember { mutableStateOf(template.glowRadius) }
        var particlesCount by remember { mutableStateOf(template.particlesCount) }
        var beatSensitivity by remember { mutableStateOf(template.beatSensitivity) }
        var animSpeed by remember { mutableStateOf(template.animSpeed) }
        var rotationAngle by remember { mutableStateOf(template.rotationAngle) }
        var textStyle by remember { mutableStateOf(template.textStyle) }
        var lyricsStyle by remember { mutableStateOf(template.lyricsStyle) }
        var fontName by remember { mutableStateOf(template.fontName) }
        var opacity by remember { mutableStateOf(template.opacity) }
        var borderThickness by remember { mutableStateOf(template.borderThickness) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090F))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        currentEditingTemplate = null
                        isCreatingNew = false
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Template Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                }

                Button(
                    onClick = {
                        if (templateName.isBlank()) {
                            Toast.makeText(context, "Please enter a template name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val saved = VisualizerTemplate(
                            id = template.id,
                            name = templateName,
                            visualizerType = visualizerType,
                            primaryColorHex = primaryColorHex,
                            secondaryColorHex = secondaryColorHex,
                            gradientEnabled = gradientEnabled,
                            bgStyle = bgStyle,
                            rotateArtwork = rotateArtwork,
                            blurRadius = blurRadius,
                            glowRadius = glowRadius,
                            particlesCount = particlesCount,
                            beatSensitivity = beatSensitivity,
                            animSpeed = animSpeed,
                            rotationAngle = rotationAngle,
                            textStyle = textStyle,
                            lyricsStyle = lyricsStyle,
                            fontName = fontName,
                            opacity = opacity,
                            borderThickness = borderThickness
                        )
                        templateManager.saveTemplate(saved)
                        currentEditingTemplate = null
                        isCreatingNew = false
                        reloadTemplates()
                        Toast.makeText(context, "Template saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121220)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("General Properties", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Visualizer Type", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CIRCULAR_BARS", "LINEAR_BARS", "PARTICLE_ORB").forEach { type ->
                            val isSel = visualizerType == type
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { visualizerType = type },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF1E1E30)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(type.replace("_", " "), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121220)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Aesthetic Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Primary Color Hex: $primaryColorHex", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("#7C4DFF", "#00E5FF", "#FF007F", "#E040FB", "#00E676").forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .border(
                                        if (primaryColorHex == color) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent),
                                        CircleShape
                                    )
                                    .clickable { primaryColorHex = color }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Secondary Color Hex: $secondaryColorHex", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("#00E5FF", "#7C4DFF", "#00E676", "#FFD700", "#FF3D00").forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .border(
                                        if (secondaryColorHex == color) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent),
                                        CircleShape
                                    )
                                    .clickable { secondaryColorHex = color }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Neon Glow Radius: ${glowRadius.toInt()}dp", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Slider(
                        value = glowRadius,
                        onValueChange = { glowRadius = it },
                        valueRange = 0f..30f,
                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Beat Sensitivity: ${String.format("%.1f", beatSensitivity)}x", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Slider(
                        value = beatSensitivity,
                        onValueChange = { beatSensitivity = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Component Opacity: ${String.format("%.0f", opacity * 100)}%", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090F))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Template Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        val isLocked = activeTier != UserTier.PREMIUM
                        if (isLocked) showPremiumUpgrade = true else importLauncher.launch("application/json")
                    }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import Template", tint = MaterialTheme.colorScheme.secondary)
                    }

                    Button(
                        onClick = {
                            val isLocked = activeTier != UserTier.PREMIUM
                            if (isLocked) {
                                showPremiumUpgrade = true
                            } else {
                                isCreatingNew = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Customize and manage exclusive visualizer skins. Duplicate, tweak, and export customized configuration files.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(templatesList) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121220)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(template.primaryColorHex)).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(template.primaryColorHex)))
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(template.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${template.visualizerType.replace("_", " ")} | Glow ${template.glowRadius.toInt()}dp | Sens ${String.format("%.1f", template.beatSensitivity)}x",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    val isLocked = activeTier != UserTier.PREMIUM
                                    if (isLocked) showPremiumUpgrade = true else currentEditingTemplate = template
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }

                                IconButton(onClick = {
                                    val isLocked = activeTier != UserTier.PREMIUM
                                    if (isLocked) {
                                        showPremiumUpgrade = true
                                    } else {
                                        templateManager.duplicateTemplate(template)
                                        reloadTemplates()
                                        Toast.makeText(context, "Template duplicated!", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }

                                IconButton(onClick = {
                                    val isLocked = activeTier != UserTier.PREMIUM
                                    if (isLocked) {
                                        showPremiumUpgrade = true
                                    } else {
                                        try {
                                            val exportIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Pulse Template - ${template.name}")
                                                putExtra(Intent.EXTRA_TEXT, template.toJsonObject().toString(4))
                                            }
                                            context.startActivity(Intent.createChooser(exportIntent, "Export Template Via"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Export", tint = Color.Green, modifier = Modifier.size(20.dp))
                                }

                                if (template.id != "default_pulse") {
                                    IconButton(onClick = {
                                        val isLocked = activeTier != UserTier.PREMIUM
                                        if (isLocked) {
                                            showPremiumUpgrade = true
                                        } else {
                                            templateManager.deleteTemplate(template.id)
                                            reloadTemplates()
                                            Toast.makeText(context, "Template deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
