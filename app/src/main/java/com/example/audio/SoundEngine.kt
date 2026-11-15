package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var musicJob: Job? = null
  private var engineJob: Job? = null

  @Volatile var musicEnabled: Boolean = true
  @Volatile var soundEnabled: Boolean = true
  @Volatile var musicVolume: Float = 0.75f
  @Volatile var soundVolume: Float = 0.85f
  @Volatile var vibrationEnabled: Boolean = true
  @Volatile private var isAppActive: Boolean = true

  @Volatile private var engineRunning: Boolean = false
  @Volatile private var engineRpm: Float = 0.15f
  @Volatile private var engineThrottle: Float = 0f

  private val sampleRate = 22050

  private var coinSamples: ShortArray? = null
  private var fuelSamples: ShortArray? = null
  private var clickSamples: ShortArray? = null
  private var jumpSamples: ShortArray? = null
  private var landSamples: ShortArray? = null
  private var crashSamples: ShortArray? = null
  private var stuntSamples: ShortArray? = null
  private var upgradeSamples: ShortArray? = null
  private var startSamples: ShortArray? = null

  init {
    scope.launch {
      coinSamples = synthArpeggio(floatArrayOf(987.77f, 1318.51f), 0.12f, 0.35f)
      fuelSamples = synthSweep(320f, 760f, 0.24f, 0.38f)
      clickSamples = synthTone(640f, 0.045f, 0.22f, decay = 35f)
      jumpSamples = synthSweep(220f, 520f, 0.18f, 0.28f)
      landSamples = synthNoiseBurst(0.11f, 0.32f, lowPass = true)
      crashSamples = synthNoiseBurst(0.35f, 0.55f, lowPass = false)
      stuntSamples = synthArpeggio(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f), 0.28f, 0.40f)
      upgradeSamples = synthArpeggio(floatArrayOf(587.33f, 880f, 1174.66f), 0.20f, 0.35f)
      startSamples = synthSweep(110f, 260f, 0.30f, 0.35f)
    }
  }

  fun updateSettings(
    music: Boolean,
    sound: Boolean,
    vibration: Boolean,
    musicVol: Float = musicVolume,
    soundVol: Float = soundVolume,
  ) {
    musicEnabled = music
    soundEnabled = sound
    vibrationEnabled = vibration
    musicVolume = musicVol.coerceIn(0f, 1f)
    soundVolume = soundVol.coerceIn(0f, 1f)
    if (!musicEnabled || musicVolume <= 0.01f) {
      stopMusic()
    } else if (isAppActive && musicJob == null) {
      startMusic()
    }
    if (!soundEnabled || soundVolume <= 0.01f) {
      engineRunning = false
    }
  }

  fun onResume() {
    isAppActive = true
    if (musicEnabled && musicVolume > 0.01f && musicJob == null) {
      startMusic()
    }
  }

  fun onPause() {
    isAppActive = false
    stopMusic()
    stopEngine()
  }

  fun startMusic() {
    if (!musicEnabled || musicVolume <= 0.01f || !isAppActive || musicJob != null) return
    musicJob =
      scope.launch {
        var track: AudioTrack? = null
        try {
          val bufferSamples = 2048
          val minBuf =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
              )
              .coerceAtLeast(bufferSamples * 4)
          track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(minBuf)
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()
          track.play()

          // Upbeat energetic Indian-pentatonic inspired arcade melody loop
          val melody =
            floatArrayOf(
              392.00f,
              440.00f,
              523.25f,
              587.33f,
              659.25f,
              587.33f,
              523.25f,
              440.00f,
              392.00f,
              523.25f,
              659.25f,
              783.99f,
              659.25f,
              587.33f,
              523.25f,
              440.00f,
            )
          val bass =
            floatArrayOf(
              196.00f,
              196.00f,
              261.63f,
              261.63f,
              220.00f,
              220.00f,
              196.00f,
              246.94f,
            )

          val samplesPerStep = (sampleRate * 0.21f).toInt()
          val chunk = ShortArray(1024)
          var totalSampleIdx = 0L
          var phaseMel = 0.0
          var phaseBass = 0.0

          while (isActive && musicEnabled && isAppActive) {
            val mVol = musicVolume
            for (i in chunk.indices) {
              val stepIdx = ((totalSampleIdx / samplesPerStep) % melody.size).toInt()
              val bassIdx = ((totalSampleIdx / (samplesPerStep * 2)) % bass.size).toInt()
              val posInStep = (totalSampleIdx % samplesPerStep).toFloat() / samplesPerStep
              val envMel = exp(-posInStep * 4.5f)
              val envBass = exp(-((totalSampleIdx % (samplesPerStep * 2)).toFloat() / (samplesPerStep * 2)) * 2.5f)

              val fMel = melody[stepIdx]
              val fBass = bass[bassIdx]
              phaseMel += 2.0 * PI * fMel / sampleRate
              phaseBass += 2.0 * PI * fBass / sampleRate

              val sMel = sin(phaseMel) * 0.5 + sin(phaseMel * 2.0) * 0.15
              val sBass = sin(phaseBass) * 0.6
              val mixed = ((sMel * envMel * 0.12) + (sBass * envBass * 0.10)) * mVol * 32767.0
              chunk[i] = mixed.toInt().coerceIn(-32767, 32767).toShort()
              totalSampleIdx++
            }
            track.write(chunk, 0, chunk.size)
          }
        } catch (_: Throwable) {
        } finally {
          runCatching {
            track?.stop()
            track?.release()
          }
          musicJob = null
        }
      }
  }

  fun stopMusic() {
    musicJob?.cancel()
    musicJob = null
  }

  fun startEngine() {
    if (!soundEnabled || !isAppActive || engineRunning) return
    engineRunning = true
    playOneShot(startSamples)
    engineJob?.cancel()
    engineJob =
      scope.launch {
        var track: AudioTrack? = null
        try {
          val minBuf =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
              )
              .coerceAtLeast(2048)
          track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(minBuf)
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()
          track.play()

          val chunk = ShortArray(768)
          var phase = 0.0
          while (isActive && engineRunning && soundEnabled && isAppActive) {
            val freq = 46f + engineRpm * 92f
            val vol = (0.06f + engineThrottle * 0.07f) * soundVolume
            for (i in chunk.indices) {
              phase += 2.0 * PI * freq / sampleRate
              val purr =
                sin(phase) * 0.55 + sin(phase * 2.0) * 0.3 + sin(phase * 3.0) * 0.15
              chunk[i] = (purr * vol * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            track.write(chunk, 0, chunk.size)
          }
        } catch (_: Throwable) {
        } finally {
          runCatching {
            track?.stop()
            track?.release()
          }
        }
      }
  }

  fun updateEngine(rpm: Float, throttle: Float) {
    engineRpm = rpm.coerceIn(0f, 1.4f)
    engineThrottle = throttle.coerceIn(0f, 1f)
  }

  fun stopEngine() {
    engineRunning = false
    engineJob?.cancel()
    engineJob = null
  }

  fun playClick() = playOneShot(clickSamples)

  fun playCoin() = playOneShot(coinSamples)

  fun playFuel() {
    playOneShot(fuelSamples)
    vibrate(35L)
  }

  fun playJump() = playOneShot(jumpSamples)

  fun playLand(intensity: Float = 1f) {
    if (intensity > 0.35f) playOneShot(landSamples)
  }

  fun playCrash() {
    playOneShot(crashSamples)
    vibrate(140L)
  }

  fun playStunt() {
    playOneShot(stuntSamples)
    vibrate(55L)
  }

  fun playUpgrade() {
    playOneShot(upgradeSamples)
    vibrate(30L)
  }

  fun vibrate(durationMs: Long) {
    if (!vibrationEnabled) return
    try {
      val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vm?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
          )
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(durationMs)
        }
      }
    } catch (_: Throwable) {
    }
  }

  private fun playOneShot(samples: ShortArray?) {
    if (!soundEnabled || soundVolume <= 0.01f || !isAppActive || samples == null) return
    val vol = soundVolume
    scope.launch {
      var track: AudioTrack? = null
      try {
        val scaled = ShortArray(samples.size) { idx -> (samples[idx] * vol).toInt().toShort() }
        track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(scaled.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(scaled, 0, scaled.size)
        track.play()
        delay((scaled.size * 1000L / sampleRate) + 40L)
      } catch (_: Throwable) {
      } finally {
        runCatching {
          track?.stop()
          track?.release()
        }
      }
    }
  }

  private fun synthTone(freq: Float, durationSec: Float, amp: Float, decay: Float): ShortArray {
    val n = (sampleRate * durationSec).toInt().coerceAtLeast(1)
    return ShortArray(n) { i ->
      val t = i.toFloat() / sampleRate
      val env = exp(-t * decay)
      (sin(2.0 * PI * freq * t) * env * amp * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun synthArpeggio(freqs: FloatArray, totalDur: Float, amp: Float): ShortArray {
    val n = (sampleRate * totalDur).toInt().coerceAtLeast(1)
    val perNote = (n / freqs.size).coerceAtLeast(1)
    return ShortArray(n) { i ->
      val idx = (i / perNote).coerceAtMost(freqs.lastIndex)
      val localT = (i % perNote).toFloat() / sampleRate
      val env = exp(-localT * 18f)
      val f = freqs[idx]
      (sin(2.0 * PI * f * i / sampleRate) * env * amp * 32767.0)
        .toInt()
        .coerceIn(-32767, 32767)
        .toShort()
    }
  }

  private fun synthSweep(fStart: Float, fEnd: Float, dur: Float, amp: Float): ShortArray {
    val n = (sampleRate * dur).toInt().coerceAtLeast(1)
    var phase = 0.0
    return ShortArray(n) { i ->
      val p = i.toFloat() / n
      val freq = fStart + (fEnd - fStart) * p
      phase += 2.0 * PI * freq / sampleRate
      val env = sin(PI * p).toFloat()
      (sin(phase) * env * amp * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun synthNoiseBurst(dur: Float, amp: Float, lowPass: Boolean): ShortArray {
    val n = (sampleRate * dur).toInt().coerceAtLeast(1)
    var last = 0f
    return ShortArray(n) { i ->
      val p = i.toFloat() / n
      val env = (1f - p) * (1f - p)
      val raw = (Random.nextFloat() * 2f - 1f)
      val filtered =
        if (lowPass) {
          last = last * 0.82f + raw * 0.18f
          last
        } else {
          last = last * 0.45f + raw * 0.55f
          last
        }
      (filtered * env * amp * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  fun release() {
    stopMusic()
    stopEngine()
  }
}
