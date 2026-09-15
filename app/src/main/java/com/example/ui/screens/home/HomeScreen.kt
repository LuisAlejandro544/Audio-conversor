package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConverterViewModel
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioOrange
import com.example.ui.theme.AudioPurple
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Modelo de datos para las tarjetas de herramientas de edición y conversión.
 *
 * @param id Identificador único para pruebas y navegación.
 * @param title Nombre visible de la herramienta.
 * @param icon Ícono representativo en formato vectorial.
 * @param accentColor Color temático distintivo.
 * @param isLocked Indica si la herramienta está bloqueada con candado (en desarrollo).
 * @param isPrimaryFeature Si está activa y redirige al flujo de conversión principal.
 * @param description Resumen para el usuario de lo que hace o hará la función.
 */
data class AudioToolItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isLocked: Boolean = false,
    val isPrimaryFeature: Boolean = false,
    val description: String = ""
)

/**
 * Pantalla principal (Hub) de AudioStudio.
 *
 * Diseñada con una arquitectura visual limpia, intuitiva y moderna inspirada en AudioLab.
 * Las herramientas en fase de desarrollo se encuentran claramente identificadas con candado,
 * mientras que las herramientas operativas permiten el acceso directo e inmediato al flujo de audio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ConverterViewModel,
    onNavigateToSelectAudio: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyList.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedLockedTool by remember { mutableStateOf<AudioToolItem?>(null) }
    var selectedBottomNavIndex by remember { mutableStateOf(0) }

    // Herramientas usadas recientemente (carrusel superior horizontal 100% operativas)
    val recentTools = remember {
        listOf(
            AudioToolItem(
                id = "tool_convert",
                title = "Convertir\nFormato",
                icon = Icons.Default.Transform,
                accentColor = AudioCyan,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Convierte archivos entre formatos MP3, WAV, AAC, FLAC, OPUS y OGG."
            ),
            AudioToolItem(
                id = "tool_compress",
                title = "Comprimir\nAudio",
                icon = Icons.Default.FolderZip,
                accentColor = AudioOrange,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Reduce el tamaño en MB optimizando la tasa de bits y el códec de compresión."
            ),
            AudioToolItem(
                id = "tool_gain",
                title = "Volumen &\nGanancia",
                icon = Icons.Default.VolumeUp,
                accentColor = AudioAmber,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Aumenta o reduce el volumen (25% a 200%) con limitador de picos integrado."
            ),
            AudioToolItem(
                id = "tool_resample",
                title = "Remuestreo\nde Frecuencia",
                icon = Icons.Default.Speed,
                accentColor = AudioGreen,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Cambia la frecuencia de muestreo desde 8.000 Hz hasta 96.000 Hz."
            ),
            AudioToolItem(
                id = "tool_channels",
                title = "Canales\nMono / Estéreo",
                icon = Icons.Default.AltRoute,
                accentColor = AudioPurple,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Convierte pistas a sonido monoaural o estéreo de dos canales equilibrados."
            )
        )
    }

    // Cuadrícula completa de herramientas (12 herramientas en cuadrícula 4x3)
    // Las herramientas sin lógica completa están bloqueadas con candado según solicitud.
    val allAudioTools = remember {
        listOf(
            AudioToolItem(
                id = "grid_convert",
                title = "Convertir",
                icon = Icons.Default.Transform,
                accentColor = AudioCyan,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Conversor de audio versátil con soporte para MP3, WAV, AAC, FLAC, OPUS y OGG."
            ),
            AudioToolItem(
                id = "grid_compress",
                title = "Comprimir",
                icon = Icons.Default.FolderZip,
                accentColor = AudioOrange,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Optimización de tasa de bits para reducir el tamaño de tus archivos de audio."
            ),
            AudioToolItem(
                id = "grid_volume",
                title = "Volumen",
                icon = Icons.Default.VolumeUp,
                accentColor = AudioAmber,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Amplificación de ganancia con limitador suave para evitar distorsiones."
            ),
            AudioToolItem(
                id = "grid_resample",
                title = "Remuestreo",
                icon = Icons.Default.Speed,
                accentColor = AudioGreen,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Conversión de frecuencia de muestreo de 8.000 Hz hasta 96.000 Hz."
            ),
            AudioToolItem(
                id = "grid_channels",
                title = "Canales",
                icon = Icons.Default.AltRoute,
                accentColor = AudioPurple,
                isLocked = false,
                isPrimaryFeature = true,
                description = "Conversión de balance entre pistas mono (1 canal) y estéreo (2 canales)."
            ),
            AudioToolItem(
                id = "grid_cut",
                title = "Recorte",
                icon = Icons.Default.ContentCut,
                accentColor = Color(0xFFFF5252),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Herramienta de recorte de fragmentos de audio por marcas de tiempo milimétricas."
            ),
            AudioToolItem(
                id = "grid_merge",
                title = "Fusión",
                icon = Icons.Default.CallMerge,
                accentColor = Color(0xFF448AFF),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Une múltiples archivos de audio de forma secuencial en una única pista continua."
            ),
            AudioToolItem(
                id = "grid_mix",
                title = "Mezclador",
                icon = Icons.Default.Tune,
                accentColor = Color(0xFF69F0AE),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Superpone y mezcla varias fuentes de audio simultáneamente con niveles individuales."
            ),
            AudioToolItem(
                id = "grid_tags",
                title = "Etiquetas ID3",
                icon = Icons.Default.Label,
                accentColor = Color(0xFFFFD740),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Inspecciona y edita metadatos de canciones: artista, título, álbum y carátula."
            ),
            AudioToolItem(
                id = "grid_record",
                title = "Grabadora",
                icon = Icons.Default.Mic,
                accentColor = Color(0xFFFF4081),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Grabación de audio de alta fidelidad directamente desde el micrófono de tu teléfono."
            ),
            AudioToolItem(
                id = "grid_8d",
                title = "Audio 8D",
                icon = Icons.Default.Headphones,
                accentColor = Color(0xFF7C4DFF),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Efecto envolvente binaural 8D con paneo rotacional dinámico para auriculares."
            ),
            AudioToolItem(
                id = "grid_silence",
                title = "Silencios",
                icon = Icons.Default.VolumeOff,
                accentColor = Color(0xFF00E5FF),
                isLocked = true,
                isPrimaryFeature = false,
                description = "Detección inteligente y remoción de pausas o silencios largos en grabaciones de voz."
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(AudioCyan, AudioOrange))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Logo",
                                tint = StudioBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AudioStudio",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Suite de Edición & Conversión",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                },
                actions = {
                    // Botón de Historial con badge de registros guardados en Room
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("nav_history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (history.isNotEmpty()) {
                                    Badge(
                                        containerColor = AudioOrange,
                                        contentColor = Color.White
                                    ) {
                                        Text(history.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Historial",
                                tint = AudioCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface
                )
            )
        },
        bottomBar = {
            // Barra de navegación inferior amigable y limpia (3 accesos esenciales)
            NavigationBar(
                containerColor = StudioSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 0,
                    onClick = { selectedBottomNavIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = { Text("Inicio", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioBackground,
                        indicatorColor = AudioCyan,
                        selectedTextColor = AudioCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedBottomNavIndex == 1,
                    onClick = {
                        selectedBottomNavIndex = 1
                        onNavigateToHistory()
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (history.isNotEmpty()) {
                                    Badge(containerColor = AudioOrange) {
                                        Text(history.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Historial"
                            )
                        }
                    },
                    label = { Text("Historial", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioBackground,
                        indicatorColor = AudioCyan,
                        selectedTextColor = AudioCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedBottomNavIndex == 2,
                    onClick = {
                        selectedBottomNavIndex = 2
                        showSettingsDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    },
                    label = { Text("Ajustes", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioBackground,
                        indicatorColor = AudioCyan,
                        selectedTextColor = AudioCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        },
        floatingActionButton = {
            // Botón flotante para seleccionar archivo y convertir inmediatamente
            FloatingActionButton(
                onClick = onNavigateToSelectAudio,
                containerColor = AudioCyan,
                contentColor = StudioBackground,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_pick_audio")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Convertir Audio",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // SECCIÓN 1: Herramientas Usadas Recientemente (Carrusel horizontal superior)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Herramientas usadas recientemente",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fijado",
                            tint = AudioCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentTools) { tool ->
                            RecentToolCard(
                                tool = tool,
                                onClick = {
                                    if (tool.isLocked) {
                                        selectedLockedTool = tool
                                    } else {
                                        onNavigateToSelectAudio()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // SECCIÓN 2: Cuadrícula completa de Herramientas de Edición de Audio (con candados)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Herramientas de edición de audio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                        ) {
                            Text(
                                text = "${allAudioTools.count { !it.isLocked }} activas",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AudioGreen,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Cuadrícula 4 columnas con espaciado limpio
                    val rows = allAudioTools.chunked(4)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rows.forEach { rowTools ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowTools.forEach { tool ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        GridToolCard(
                                            tool = tool,
                                            onClick = {
                                                if (tool.isLocked) {
                                                    selectedLockedTool = tool
                                                } else {
                                                    onNavigateToSelectAudio()
                                                }
                                            }
                                        )
                                    }
                                }
                                // Rellenar celdas vacías si la última fila tiene menos de 4 columnas
                                repeat(4 - rowTools.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN 3: Acceso directo rápido para seleccionar archivo y convertir
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FastConvertCard(onSelectClick = onNavigateToSelectAudio)
                }
            }
        }
    }

    // Diálogo informativo para herramientas bloqueadas con candado
    selectedLockedTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { selectedLockedTool = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tool.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = tool.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tool.title.replace("\n", " "),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = StudioBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = null,
                                tint = tool.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tool.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = "Esta herramienta está en fase de desarrollo y estará disponible en una próxima actualización.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )

                    Text(
                        text = "Puedes usar inmediatamente las herramientas activas: Conversión de formatos, Compresión de audio, Control de ganancia/volumen, Remuestreo de frecuencia y División estéreo/mono.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedLockedTool = null
                        onNavigateToSelectAudio()
                    }
                ) {
                    Text("Usar Herramientas Activas", color = AudioCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLockedTool = null }) {
                    Text("Cerrar", color = TextSecondary)
                }
            },
            containerColor = StudioSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Diálogo de Ajustes e Información General
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = AudioCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Acerca de AudioStudio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "AudioStudio v2.1",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "• Compatible con Android 8.0+ (Oreo y superior).\n• 100% Offline: Todo el procesamiento se ejecuta localmente en tu teléfono.\n• Sin anuncios ni límites de tiempo.\n• Escala de texto estable para cualquier configuración de pantalla móvil.\n• Diseñado para distribución directa y tiendas de terceros como Uptodown.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Aceptar", color = AudioCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = StudioSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Tarjeta para las herramientas del carrusel reciente (superior).
 */
@Composable
private fun RecentToolCard(
    tool: AudioToolItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(115.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
            .testTag("recent_tool_${tool.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Fila superior con estado de fijado o candado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tool.accentColor)
                )
            }

            // Ícono central con contenedor circular translúcido
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tool.accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Título inferior con tipografía fija
            Text(
                text = tool.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 13.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tarjeta individual para la cuadrícula 4x de herramientas.
 * Muestra un candado visible y estética atenuada si la herramienta aún no tiene su lógica lista.
 */
@Composable
private fun GridToolCard(
    tool: AudioToolItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (tool.isLocked) StudioCardBorder.copy(alpha = 0.5f) else StudioCardBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag(tool.id),
        colors = CardDefaults.cardColors(
            containerColor = if (tool.isLocked) StudioSurfaceVariant.copy(alpha = 0.6f) else StudioSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Insignia de candado en la esquina superior derecha si está bloqueada
            if (tool.isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(StudioBackground.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = TextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .then(if (tool.isLocked) Modifier.alpha(0.65f) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (tool.isLocked) tool.accentColor.copy(alpha = 0.1f)
                            else tool.accentColor.copy(alpha = 0.16f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = if (tool.isLocked) tool.accentColor.copy(alpha = 0.7f) else tool.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (tool.isLocked) FontWeight.Normal else FontWeight.Medium,
                        color = if (tool.isLocked) TextSecondary else TextPrimary,
                        lineHeight = 12.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tarjeta de conversión rápida para seleccionar audio real desde el almacenamiento.
 */
@Composable
private fun FastConvertCard(
    onSelectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, AudioCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .testTag("fast_convert_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AudioCyan.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = AudioCyan,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Seleccionar archivo de audio",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Abre cualquier canción o nota de voz de tu dispositivo",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            IconButton(
                onClick = onSelectClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AudioCyan)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Elegir Audio",
                    tint = StudioBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
