package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingListen = false
    private var textToSpeech: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private var pendingText: String? = null
    private var pendingRate = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(2, 6, 13)
        window.navigationBarColor = Color.rgb(2, 6, 13)
        initializeTts()
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(2, 6, 13))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val audio = request.resources.filter { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }.toTypedArray()
                        if (audio.isNotEmpty() && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) request.grant(audio) else request.deny()
                    }
                }
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.e("JARVIS-Web", "${message.message()} -- ${message.sourceId()}:${message.lineNumber()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    Log.e("JARVIS-Web", "WebView error $errorCode: $description ($failingUrl)")
                    if (failingUrl.startsWith("file:")) runOnUiThread { Toast.makeText(this@MainActivity, "Erro ao carregar a interface JARVIS", Toast.LENGTH_LONG).show() }
                }
            }
            addJavascriptInterface(NativeSpeechBridge(), "AndroidSpeech")
            addJavascriptInterface(NativeTtsBridge(), "AndroidTTS")
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech
                val lang = tts?.setLanguage(Locale.forLanguageTag("pt-BR"))
                ttsReady = lang != null && lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { sendToPage("window.jarvisNativeTtsState('speaking')") }
                    override fun onDone(utteranceId: String?) { sendToPage("window.jarvisNativeTtsState('idle')") }
                    @Deprecated("Deprecated in Android API")
                    override fun onError(utteranceId: String?) { sendToPage("window.jarvisNativeTtsState('error')") }
                    override fun onError(utteranceId: String?, errorCode: Int) { sendToPage("window.jarvisNativeTtsState('error')") }
                })
                pendingText?.let { text -> speakNative(text, pendingRate); pendingText = null }
            } else {
                ttsReady = false
                sendToPage("window.jarvisNativeTtsState('error')")
            }
        }
    }

    private inner class NativeTtsBridge {
        @JavascriptInterface fun speak(text: String, rate: Double) {
            runOnUiThread {
                pendingRate = rate.toFloat().coerceIn(0.5f, 1.8f)
                if (ttsReady) speakNative(text, pendingRate) else pendingText = text
            }
        }
        @JavascriptInterface fun stop() { runOnUiThread { textToSpeech?.stop(); sendToPage("window.jarvisNativeTtsState('idle')") } }
        @JavascriptInterface fun isReady(): Boolean = ttsReady
    }

    private fun speakNative(text: String, rate: Float) {
        val tts = textToSpeech ?: return
        tts.setSpeechRate(rate)
        val id = "jarvis-${UUID.randomUUID()}"
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
        if (result == TextToSpeech.ERROR) sendToPage("window.jarvisNativeTtsState('error')")
    }

    private inner class NativeSpeechBridge {
        @JavascriptInterface fun startListening() {
            runOnUiThread {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    pendingListen = true
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
                    return@runOnUiThread
                }
                beginNativeRecognition()
            }
        }
        @JavascriptInterface fun stopListening() { runOnUiThread { try { speechRecognizer?.cancel() } catch (_: Exception) {} } }
    }

    private fun beginNativeRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { sendToPage("window.jarvisNativeError('unavailable')"); return }
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { sendToPage("window.jarvisNativeState('listening')") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { sendToPage("window.jarvisNativeState('idle')") }
            override fun onError(error: Int) { sendToPage("window.jarvisNativeError('error')") }
            override fun onResults(results: Bundle?) {
                val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                sendToPage("window.jarvisNativeResult(${JSONObject.quote(phrase)})")
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun sendToPage(js: String) {
        runOnUiThread { if (::webView.isInitialized) webView.evaluateJavascript(js, null) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && pendingListen) {
            pendingListen = false
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) beginNativeRecognition() else sendToPage("window.jarvisNativeError('not-allowed')")
        }
    }

    override fun onDestroy() {
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        try { textToSpeech?.stop(); textToSpeech?.shutdown() } catch (_: Exception) {}
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated API retained for compatibility")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
