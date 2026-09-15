package com.example.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFormatType
import com.example.ui.ConverterViewModel
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioOrange
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Pantalla de configuración técnica para la conversión y reducción de bitrate de audio.
 *
 * Permite ajustar con precisión:
 * 1. Formato de salida (M4A, WAV, FLAC, OGG, MP3)
 * 2. Tasa de bits / Bireraje (64k, 96k, 128k, 192k, 256k, 320k bps)
 * 3. Frecuencia de muestreo (Hz)
 * 4. Configuración de canales (Mono / Estéreo)
 * 5. Ganancia de volumen (50% a 200%)
 * 6. Nombre de archivo personalizado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertConfigScreen(
    viewModel: ConverterViewModel,
    onNavigateBack: () -> Unit,
    onStartConversion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAudio by viewModel.selectedAudio.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedBitrate by viewModel.selectedBitrateKbps.collectAsState()
    val selectedSampleRate by viewModel.selectedSampleRateHz.collectAsState()
    val selectedChannels by viewModel.selectedChannels.collectAsState()
    val volumeGain by viewModel.volumeGainFactor.collectAsState()
    val customFileName by viewModel.customFileName.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes de Conversión",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("config_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Resumen de archivo de origen
            selectedAudio?.let { audio ->
                SourceAudioBanner(audio = audio)
            }

            // 2. Selección de Formato Destino
            FormatSelectionSection(
                currentFormat = selectedFormat,
                onFormatSelected = { viewModel.setFormat(it) }
            )

            // 3. Reducción de Bitrate / Bireraje (solo para formatos comprimibles)
            val maxAllowedBitrate = viewModel.getMaxAllowedBitrate()
            val maxAllowedChannels = viewModel.getMaxAllowedChannels()
            val maxAllowedSampleRate = viewModel.getMaxAllowedSampleRate()

            if (!selectedFormat.isLossless) {
                BitrateConfigSection(
                    currentBitrate = selectedBitrate,
                    maxAllowedBitrate = maxAllowedBitrate,
                    onBitrateChanged = { viewModel.setBitrate(it) }
                )
            } else {
                LosslessNotice(format = selectedFormat)
            }

            // 4. Canales de Audio (Mono vs Estéreo)
            ChannelsConfigSection(
                selectedChannels = selectedChannels,
                maxAllowedChannels = maxAllowedChannels,
                onChannelsChanged = { viewModel.setChannels(it) }
            )

            // 5. Frecuencia de Muestreo (Sample Rate Hz)
            SampleRateConfigSection(
                selectedSampleRate = selectedSampleRate,
                maxAllowedSampleRate = maxAllowedSampleRate,
                onSampleRateChanged = { viewModel.setSampleRate(it) }
            )

            // 6. Ganancia de Volumen
            VolumeGainSection(
                gain = volumeGain,
                onGainChanged = { viewModel.setVolumeGain(it) }
            )

            // 7. Nombre del archivo resultante
            OutlinedTextField(
                value = customFileName,
                onValueChange = { viewModel.setCustomFileName(it) },
                label = { Text("Nombre del archivo de salida") },
                suffix = { Text(".${selectedFormat.extension}", color = AudioCyan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("output_file_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AudioCyan,
                    unfocusedBorderColor = StudioCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = AudioCyan,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Botón de inicio de conversión
            Button(
                onClick = onStartConversion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("execute_conversion_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = StudioBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Conversión",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Banner superior con información resumida de la pista origen.
 */
@Composable
private fun SourceAudioBanner(audio: com.example.audio.AudioInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = StudioSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StudioBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = AudioCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audio.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${audio.format} • ${audio.bitrateKbps} kbps • ${audio.getFormattedSize()} • ${audio.getFormattedDuration()}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

/**
 * Selector interactivo de formatos destino.
 */
@Composable
private fun FormatSelectionSection(
    currentFormat: AudioFormatType,
    onFormatSelected: (AudioFormatType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "1. Formato de Destino",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioFormatType.values().forEach { format ->
                val isSelected = currentFormat == format
                val borderColor = if (isSelected) AudioCyan else StudioCardBorder
                val bgColor = if (isSelected) StudioSurfaceVariant else StudioSurface

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onFormatSelected(format) }
                        .testTag("format_chip_${format.extension}"),
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = format.extension.uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AudioCyan else TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (format.isLossless) "Lossless" else "Compresión",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) AudioOrange else TextMuted,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }

        // Descripción del formato activo
        Text(
            text = currentFormat.description,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/**
 * Sección para ajuste de bitrate con presets y slider acotados a la calidad original.
 */
@Composable
private fun BitrateConfigSection(
    currentBitrate: Int,
    maxAllowedBitrate: Int,
    onBitrateChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Tasa de Bits (Bitrate)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Surface(
                    color = AudioOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$currentBitrate kbps",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AudioOrange
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Protección de calidad activa: Máximo permitido de $maxAllowedBitrate kbps (origen). No se permite upsampling para evitar inflar el peso sin ganancia sonora real.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Chips de valores comunes recomendados
            val commonBitrates = listOf(64, 96, 128, 192, 256, 320)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                commonBitrates.forEach { br ->
                    val isExceeded = br > maxAllowedBitrate
                    val selected = currentBitrate == br && !isExceeded
                    val isClickable = !isExceeded

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isClickable) {
                                    Modifier.clickable { onBitrateChanged(br) }
                                } else {
                                    Modifier
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    selected -> AudioOrange
                                    isExceeded -> StudioCardBorder.copy(alpha = 0.3f)
                                    else -> StudioCardBorder
                                },
                                RoundedCornerShape(8.dp)
                            ),
                        color = when {
                            selected -> AudioOrange.copy(alpha = 0.2f)
                            isExceeded -> StudioBackground.copy(alpha = 0.4f)
                            else -> StudioBackground
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isExceeded) "$br 🔒" else "$br",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        selected -> AudioOrange
                                        isExceeded -> TextMuted.copy(alpha = 0.35f)
                                        else -> TextPrimary
                                    },
                                    fontSize = if (isExceeded) 10.sp else 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rango de slider limitado por la tasa de bits del archivo original
            val sliderMin = 64f.coerceAtMost(maxAllowedBitrate.toFloat())
            val sliderMax = maxAllowedBitrate.toFloat().coerceAtLeast(sliderMin)

            Slider(
                value = currentBitrate.toFloat().coerceIn(sliderMin, sliderMax),
                onValueChange = { onBitrateChanged(it.toInt().coerceAtMost(maxAllowedBitrate)) },
                valueRange = sliderMin..sliderMax,
                steps = if (sliderMax > sliderMin) {
                    ((sliderMax - sliderMin) / 32f).toInt().coerceAtLeast(0)
                } else 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bitrate_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = AudioOrange,
                    activeTrackColor = AudioOrange,
                    inactiveTrackColor = StudioCardBorder
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${sliderMin.toInt()} kbps (Mín)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                Text("Tope: $maxAllowedBitrate kbps (Original)", style = MaterialTheme.typography.labelSmall.copy(color = AudioOrange, fontWeight = FontWeight.Bold))
            }
        }
    }
}

/**
 * Mensaje para formatos sin compresión (WAV/FLAC).
 */
@Composable
private fun LosslessNotice(format: AudioFormatType) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = StudioSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AudioCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${format.displayName} es un formato sin pérdida. Mantendrá la máxima fidelidad sonora del audio original.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}

/**
 * Sección para elegir canales de audio (Mono vs Estéreo).
 */
@Composable
private fun ChannelsConfigSection(
    selectedChannels: Int,
    maxAllowedChannels: Int,
    onChannelsChanged: (Int) -> Unit
) {
    val isStereoAllowed = maxAllowedChannels >= 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "3. Canales de Audio",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChannelOption(
                    title = "Original",
                    subtitle = "Mantener pistas",
                    selected = selectedChannels == 0,
                    enabled = true,
                    onClick = { onChannelsChanged(0) },
                    modifier = Modifier.weight(1f)
                )
                ChannelOption(
                    title = if (isStereoAllowed) "Estéreo" else "Estéreo 🔒",
                    subtitle = if (isStereoAllowed) "2 Canales (L/R)" else "No disp. (origen Mono)",
                    selected = selectedChannels == 2 && isStereoAllowed,
                    enabled = isStereoAllowed,
                    onClick = { onChannelsChanged(2) },
                    modifier = Modifier.weight(1f)
                )
                ChannelOption(
                    title = "Mono",
                    subtitle = "-50% espacio",
                    selected = selectedChannels == 1,
                    enabled = true,
                    onClick = { onChannelsChanged(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChannelOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (enabled) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .border(
                1.dp,
                when {
                    selected && enabled -> AudioCyan
                    !enabled -> StudioCardBorder.copy(alpha = 0.3f)
                    else -> StudioCardBorder
                },
                RoundedCornerShape(10.dp)
            ),
        color = when {
            selected && enabled -> AudioCyan.copy(alpha = 0.15f)
            !enabled -> StudioBackground.copy(alpha = 0.4f)
            else -> StudioBackground
        },
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = when {
                        selected && enabled -> AudioCyan
                        !enabled -> TextMuted.copy(alpha = 0.35f)
                        else -> TextPrimary
                    }
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (enabled) TextMuted else TextMuted.copy(alpha = 0.35f),
                    fontSize = 9.sp
                )
            )
        }
    }
}

/**
 * Frecuencia de muestreo (Hz) con protección contra upsampling destructivo.
 */
@Composable
private fun SampleRateConfigSection(
    selectedSampleRate: Int,
    maxAllowedSampleRate: Int,
    onSampleRateChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "4. Frecuencia de Muestreo (Sample Rate)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Tope de fidelidad: ${String.format(Locale.US, "%.1f", maxAllowedSampleRate / 1000f)} kHz (frecuencia nativa). No se permite upsampling artificial.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            val rates = listOf(
                0 to "Original",
                48000 to "48.0 kHz",
                44100 to "44.1 kHz",
                32000 to "32.0 kHz",
                22050 to "22.0 kHz"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rates.forEach { (rate, label) ->
                    val isExceeded = rate > 0 && rate > maxAllowedSampleRate
                    val selected = selectedSampleRate == rate && !isExceeded
                    val isClickable = !isExceeded

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isClickable) {
                                    Modifier.clickable { onSampleRateChanged(rate) }
                                } else {
                                    Modifier
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    selected -> AudioGreen
                                    isExceeded -> StudioCardBorder.copy(alpha = 0.3f)
                                    else -> StudioCardBorder
                                },
                                RoundedCornerShape(8.dp)
                            ),
                        color = when {
                            selected -> AudioGreen.copy(alpha = 0.15f)
                            isExceeded -> StudioBackground.copy(alpha = 0.4f)
                            else -> StudioBackground
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isExceeded) "$label 🔒" else label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        selected -> AudioGreen
                                        isExceeded -> TextMuted.copy(alpha = 0.35f)
                                        else -> TextSecondary
                                    },
                                    fontSize = if (isExceeded) 9.sp else 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Control de ganancia / volumen del audio procesado.
 */
@Composable
private fun VolumeGainSection(
    gain: Float,
    onGainChanged: (Float) -> Unit
) {
    val percentage = (gain * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = AudioCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5. Ganancia de Volumen",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    color = AudioCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AudioCyan
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Slider(
                value = gain,
                onValueChange = onGainChanged,
                valueRange = 0.5f..2.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("volume_gain_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = AudioCyan,
                    activeTrackColor = AudioCyan,
                    inactiveTrackColor = StudioCardBorder
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("50% (Atenuar)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                Text("100% (Normal)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                Text("200% (Amplificar)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
            }
        }
    }
}
