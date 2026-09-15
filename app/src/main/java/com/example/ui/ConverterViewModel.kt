package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioConverterEngine
import com.example.audio.AudioExportHelper
import com.example.audio.AudioFormatType
import com.example.audio.AudioInfo
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioPlayerController
import com.example.audio.ConversionConfig
import com.example.audio.ConversionResult
import com.example.data.local.AppDatabase
import com.example.data.local.ConversionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel principal para el flujo de conversión de audio y gestión de estados.
 *
 * Responsabilidades:
 * - Mantiene el estado de la pista seleccionada y de los parámetros de configuración.
 * - Orquesta la ejecución del motor de conversión en segundo plano.
 * - Registra las conversiones exitosas en la base de datos Room.
 * - Ofrece control de preescucha mediante AudioPlayerController.
 */
class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val conversionDao = database.conversionDao()
    private val converterEngine = AudioConverterEngine(context)

    // Controlador de audio para reproducir pistas originales o convertidas
    val playerController = AudioPlayerController(context, viewModelScope)

    // Historial reactivo de conversiones
    val historyList: StateFlow<List<ConversionEntity>> = conversionDao.getAllConversions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado del audio origen seleccionado
    private val _selectedAudio = MutableStateFlow<AudioInfo?>(null)
    val selectedAudio: StateFlow<AudioInfo?> = _selectedAudio.asStateFlow()

    // Parámetros de configuración para la conversión
    private val _selectedFormat = MutableStateFlow(AudioFormatType.M4A)
    val selectedFormat: StateFlow<AudioFormatType> = _selectedFormat.asStateFlow()

    private val _selectedBitrateKbps = MutableStateFlow(128)
    val selectedBitrateKbps: StateFlow<Int> = _selectedBitrateKbps.asStateFlow()

    private val _selectedSampleRateHz = MutableStateFlow(0) // 0 = Mantener original
    val selectedSampleRateHz: StateFlow<Int> = _selectedSampleRateHz.asStateFlow()

    private val _selectedChannels = MutableStateFlow(0) // 0 = Mantener original, 1 = Mono, 2 = Estéreo
    val selectedChannels: StateFlow<Int> = _selectedChannels.asStateFlow()

    private val _volumeGainFactor = MutableStateFlow(1.0f) // 1.0 = 100%
    val volumeGainFactor: StateFlow<Float> = _volumeGainFactor.asStateFlow()

    private val _customFileName = MutableStateFlow("")
    val customFileName: StateFlow<String> = _customFileName.asStateFlow()

    // Estado de la ejecución de conversión
    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _conversionProgress = MutableStateFlow(0)
    val conversionProgress: StateFlow<Int> = _conversionProgress.asStateFlow()

    private val _conversionStatusText = MutableStateFlow("Listo")
    val conversionStatusText: StateFlow<String> = _conversionStatusText.asStateFlow()

    private val _lastConversionResult = MutableStateFlow<ConversionResult?>(null)
    val lastConversionResult: StateFlow<ConversionResult?> = _lastConversionResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _exportSuccessMessage = MutableStateFlow<String?>(null)
    val exportSuccessMessage: StateFlow<String?> = _exportSuccessMessage.asStateFlow()

    /**
     * Carga y procesa un archivo de audio seleccionado por el usuario vía Uri.
     */
    fun selectAudio(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _errorMessage.value = null
            val info = AudioMetadataReader.readAudioInfo(context, uri)
            if (info != null) {
                _selectedAudio.value = info
                val baseName = info.fileName.substringBeforeLast('.')
                _customFileName.value = "${baseName}_convertido"
                // Ajustar bitrate sugerido a un valor razonable
                _selectedBitrateKbps.value = if (info.bitrateKbps in 64..320) info.bitrateKbps else 128
            } else {
                _errorMessage.value = "No se pudieron leer los metadatos del archivo de audio."
            }
        }
    }

    fun setFormat(format: AudioFormatType) {
        _selectedFormat.value = format
        if (format == AudioFormatType.WAV || format == AudioFormatType.FLAC) {
            _selectedBitrateKbps.value = format.defaultBitrateKbps
        } else if (_selectedBitrateKbps.value > 320 || _selectedBitrateKbps.value < 64) {
            _selectedBitrateKbps.value = 128
        }
    }

    fun setBitrate(bitrateKbps: Int) {
        _selectedBitrateKbps.value = bitrateKbps
    }

    fun setSampleRate(sampleRateHz: Int) {
        _selectedSampleRateHz.value = sampleRateHz
    }

    fun setChannels(channels: Int) {
        _selectedChannels.value = channels
    }

    fun setVolumeGain(gain: Float) {
        _selectedVolumeGain(gain)
    }

    private fun _selectedVolumeGain(gain: Float) {
        _volumeGainFactor.value = gain
    }

    fun setCustomFileName(name: String) {
        _customFileName.value = name
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearExportMessage() {
        _exportSuccessMessage.value = null
    }

    /**
     * Inicia el proceso de conversión de audio con los parámetros seleccionados.
     */
    fun startConversion(onSuccess: (ConversionResult) -> Unit) {
        val source = _selectedAudio.value ?: return
        if (_isConverting.value) return

        playerController.stop()
        _isConverting.value = true
        _conversionProgress.value = 0
        _conversionStatusText.value = "Iniciando proceso..."
        _errorMessage.value = null

        val config = ConversionConfig(
            sourceUri = source.uri,
            sourceInfo = source,
            targetFormat = _selectedFormat.value,
            targetBitrateKbps = _selectedBitrateKbps.value,
            targetSampleRateHz = _selectedSampleRateHz.value,
            targetChannels = _selectedChannels.value,
            volumeGainFactor = _volumeGainFactor.value,
            customOutputFileName = _customFileName.value
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = converterEngine.convertAudio(config) { percent, status ->
                    _conversionProgress.value = percent
                    _conversionStatusText.value = status
                }

                _lastConversionResult.value = result

                // Guardar registro en base de datos local Room
                val entity = ConversionEntity(
                    originalFileName = source.fileName,
                    originalFormat = source.format,
                    originalSizeBytes = source.sizeBytes,
                    originalBitrateKbps = source.bitrateKbps,
                    convertedFileName = result.outputFile.name,
                    convertedFilePath = result.outputFile.absolutePath,
                    convertedFormat = result.format.extension.uppercase(),
                    convertedSizeBytes = result.fileSize,
                    targetBitrateKbps = result.bitrateKbps,
                    targetSampleRateHz = result.sampleRateHz,
                    targetChannels = result.channelCount,
                    durationMs = result.durationMs
                )
                conversionDao.insertConversion(entity)

                withContext(Dispatchers.Main) {
                    _isConverting.value = false
                    onSuccess(result)
                }
            } catch (e: InterruptedException) {
                withContext(Dispatchers.Main) {
                    _isConverting.value = false
                    _conversionStatusText.value = "Conversión cancelada por el usuario"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isConverting.value = false
                    _errorMessage.value = "Fallo en la conversión: ${e.localizedMessage ?: "Error desconocido"}"
                }
            }
        }
    }

    fun cancelConversion() {
        converterEngine.cancel()
        _isConverting.value = false
    }

    fun saveResultToDeviceMusic(result: ConversionResult) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = AudioExportHelper.saveToMusicDirectory(
                context = context,
                sourceFile = result.outputFile,
                title = result.outputFile.nameWithoutExtension
            )
            withContext(Dispatchers.Main) {
                if (uri != null) {
                    _exportSuccessMessage.value = "Audio disponible en la carpeta pública ConvertX/Converter."
                } else {
                    _errorMessage.value = "No se pudo exportar el audio a la carpeta pública ConvertX/Converter."
                }
            }
        }
    }

    fun shareResult(file: File) {
        AudioExportHelper.shareAudioFile(context, file)
    }

    fun deleteHistoryItem(item: ConversionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Eliminar archivo físico si aún existe
            try {
                val f = File(item.convertedFilePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
            conversionDao.deleteConversion(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            conversionDao.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
