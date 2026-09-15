package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AudioStudio", appName)
  }

  @Test
  fun `verify audio format types resolution`() {
    assertEquals(com.example.audio.AudioFormatType.M4A, com.example.audio.AudioFormatType.fromExtension("m4a"))
    assertEquals(com.example.audio.AudioFormatType.WAV, com.example.audio.AudioFormatType.fromExtension("wav"))
    assertEquals(com.example.audio.AudioFormatType.FLAC, com.example.audio.AudioFormatType.fromExtension("flac"))
    assertEquals(com.example.audio.AudioFormatType.OGG, com.example.audio.AudioFormatType.fromExtension("ogg"))
    assertEquals(com.example.audio.AudioFormatType.MP3, com.example.audio.AudioFormatType.fromExtension("mp3"))
  }

  @Test
  fun `verify wav header generation`() {
    val tempFile = java.io.File.createTempFile("test_header", ".wav")
    java.io.FileOutputStream(tempFile).use { fos ->
      com.example.audio.SampleAudioGenerator.writeWavHeader(
        out = fos,
        totalAudioLen = 176400, // 1 segundo de 44.1kHz 16-bit estéreo
        sampleRate = 44100,
        channels = 2
      )
    }
    assertEquals(44L, tempFile.length())
    val bytes = tempFile.readBytes()
    assertEquals('R'.code.toByte(), bytes[0])
    assertEquals('I'.code.toByte(), bytes[1])
    assertEquals('F'.code.toByte(), bytes[2])
    assertEquals('F'.code.toByte(), bytes[3])
    assertEquals('W'.code.toByte(), bytes[8])
    assertEquals('A'.code.toByte(), bytes[9])
    assertEquals('V'.code.toByte(), bytes[10])
    assertEquals('E'.code.toByte(), bytes[11])
    tempFile.delete()
  }

  @Test
  fun `verify conversion result output uri and properties`() {
    val dummyFile = java.io.File("/tmp/dummy_output.m4a")
    val result = com.example.audio.ConversionResult(
      outputFile = dummyFile,
      format = com.example.audio.AudioFormatType.M4A,
      fileSize = 3200000L,
      durationMs = 206000L,
      bitrateKbps = 128,
      sampleRateHz = 44100,
      channelCount = 2
    )
    assertEquals(206000L, result.durationMs)
    assertEquals(128, result.bitrateKbps)
    assertEquals(dummyFile.absolutePath, result.outputFile.absolutePath)
    assertEquals("file:///tmp/dummy_output.m4a", result.outputUri.toString())
  }

  @Test
  fun `verify fidelity ceiling clamps bitrate to original audio`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getDatabase(context)
    val viewModel = com.example.ui.ConverterViewModel(
      context = context,
      conversionDao = database.conversionDao()
    )

    // Simulamos que no hay audio seleccionado: máximo por defecto 320
    assertEquals(320, viewModel.getMaxAllowedBitrate())

    // Forzamos selección de bitrate dentro de los límites
    viewModel.setBitrate(192)
    assertEquals(192, viewModel.selectedBitrateKbps.value)

    // Al sobrepasar 320 sin audio cargado, no excede el límite
    viewModel.setBitrate(500)
    assertEquals(320, viewModel.selectedBitrateKbps.value)
  }
}
