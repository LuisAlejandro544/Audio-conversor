package com.example.ui.screens.history

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ConversionEntity
import com.example.ui.ConverterViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioOrange
import com.example.ui.theme.AudioRed
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla que muestra el historial persistido en Room de todos los audios convertidos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ConverterViewModel,
    onNavigateBack: () -> Unit,
    onStartNewConversion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyList.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var currentlyPlayingItem by remember { mutableStateOf<ConversionEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de Conversiones",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Vaciar historial",
                                tint = AudioRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        }
    ) { innerPadding ->
        if (history.isEmpty()) {
            EmptyHistoryView(
                onStartClick = onStartNewConversion,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Reproductor activo si el usuario seleccionó un elemento de la lista
                currentlyPlayingItem?.let { item ->
                    val file = File(item.convertedFilePath)
                    if (file.exists()) {
                        item {
                            AudioPlayerCard(
                                playerController = viewModel.playerController,
                                audioUri = Uri.fromFile(file),
                                title = item.convertedFileName,
                                subtitle = "${item.convertedFormat} • ${item.targetBitrateKbps} kbps"
                            )
                        }
                    }
                }

                items(history, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onPlayClick = {
                            currentlyPlayingItem = item
                            val file = File(item.convertedFilePath)
                            if (file.exists()) {
                                viewModel.playerController.loadAndPlay(Uri.fromFile(file))
                            }
                        },
                        onShareClick = {
                            val file = File(item.convertedFilePath)
                            if (file.exists()) {
                                viewModel.shareResult(file)
                            }
                        },
                        onDeleteClick = {
                            if (currentlyPlayingItem?.id == item.id) {
                                viewModel.playerController.stop()
                                currentlyPlayingItem = null
                            }
                            viewModel.deleteHistoryItem(item)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Diálogo para confirmar borrado completo
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("¿Vaciar historial?", color = TextPrimary) },
            text = { Text("Se eliminarán los registros y archivos generados de la lista.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.playerController.stop()
                        currentlyPlayingItem = null
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Vaciar", color = AudioRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

/**
 * Tarjeta individual de una conversión en el historial.
 */
@Composable
private fun HistoryItemCard(
    item: ConversionEntity,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val fileExists = File(item.convertedFilePath).exists()
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(item.timestamp))

    val diffBytes = item.originalSizeBytes - item.convertedSizeBytes
    val percentReduced = if (item.originalSizeBytes > 0) {
        ((diffBytes.toDouble() / item.originalSizeBytes) * 100).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AudioCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.convertedFormat,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AudioCyan
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.convertedFileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (percentReduced > 0) {
                    Surface(
                        color = AudioGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "-$percentReduced%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AudioGreen
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Datos técnicos y fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatBytes(item.convertedSizeBytes)} • ${item.targetBitrateKbps} kbps",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones de acción rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fileExists) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = AudioCyan
                        )
                    }

                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = AudioRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Vista mostrada cuando aún no se han realizado conversiones.
 */
@Composable
private fun EmptyHistoryView(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(StudioSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = AudioCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sin conversiones previas",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Text(
            text = "Los audios que conviertas y sus estadísticas de ahorro aparecerán aquí.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(containerColor = AudioCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = StudioBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar Primera Conversión", color = StudioBackground, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.2f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}
