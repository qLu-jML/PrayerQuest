package com.prayerquest.app.ui.gratitude

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Thin wrapper around [SpeechRecognizer] scoped to the Gratitude Speed
 * Round (DD §3.6). Each call to [start] runs one recognition pass; when
 * the user stops speaking (or hits the max-silence window), we hand the
 * single best transcription to [onFinalResult] so the Speed Round can
 * write a new GratitudeEntry.
 *
 * Key behaviour choices:
 *   * Held-to-speak, not continuous: the UI calls [start] on ACTION_DOWN
 *     and [cancel] on ACTION_UP. On a natural pause the platform fires
 *     `onResults` and we commit; holding past that just starts the next
 *     pass on the next press.
 *   * Partial results ARE surfaced via [onPartialResult] so the chip-
 *     counter UI can show the in-progress transcript, but we commit only
 *     on final results to avoid double-writes.
 *   * Errors are routed to [onError] with the mapped [SpeechRecognizer]
 *     error code so the caller can show a short toast without crashing
 *     the round.
 */
class SpeedRoundVoiceRecognizer(
    private val context: Context,
    private val onPartialResult: (String) -> Unit = {},
    private val onFinalResult: (String) -> Unit,
    private val onError: (Int) -> Unit = {}
) {

    private var recognizer: SpeechRecognizer? = null

    /**
     * True when a recognition pass is currently in flight. Backed by
     * `mutableStateOf` so Compose-side reads (e.g. the VoiceMicRow's tint
     * animation) recompose when the flag flips — a plain `var` would be
     * invisible to the Compose runtime and the mic would never show its
     * "listening" state to the user.
     */
    var isListening: Boolean by mutableStateOf(false)
        private set

    /**
     * Whether on-device recognition is available on this device. Callers
     * should hide the mic button if this returns false instead of opening
     * the recognizer and immediately erroring out.
     */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Starts a single recognition pass. Safe to call even while a previous
     * pass is in flight — we reuse the existing recognizer and restart.
     */
    fun start() {
        if (!isAvailable()) {
            onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            return
        }
        val rec = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(buildListener())
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // EXTRA_LANGUAGE expects a BCP-47 tag (a String). Passing the
            // Locale object compiles (it's Serializable) but the platform
            // silently ignores it, so we hand over the language tag explicitly.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Short silence windows — the user pauses between items.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
        }
        try {
            rec.cancel()
            rec.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            onError(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    /**
     * Cancels the current recognition pass without committing. Used when
     * the Speed Round's timer runs out or the user releases the mic
     * before a result is ready.
     */
    fun cancel() {
        recognizer?.cancel()
        isListening = false
    }

    /**
     * Releases all native resources. MUST be called from the screen's
     * DisposableEffect cleanup — SpeechRecognizer holds a bound service
     * and a leaked instance will keep the mic indicator on.
     */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        isListening = false
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val best = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (best.isNotBlank()) onPartialResult(best)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val best = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (best.isNotBlank()) onFinalResult(best)
        }

        override fun onError(error: Int) {
            isListening = false
            // NO_MATCH / SPEECH_TIMEOUT aren't really "errors" from the
            // user's POV — they just didn't talk. Swallow those so the UI
            // doesn't keep popping up an error toast when the user is
            // tapping chips instead of speaking.
            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            ) return
            // Explicit outer-class qualifier — otherwise `onError(error)` in
            // this override resolves to the member function itself and
            // recurses until StackOverflowError. The outer-class property is
            // the constructor-parameter lambda the caller actually wants.
            this@SpeedRoundVoiceRecognizer.onError(error)
        }
    }
}
