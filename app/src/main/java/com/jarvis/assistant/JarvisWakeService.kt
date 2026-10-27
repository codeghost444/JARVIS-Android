package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/** Foreground microphone service that repeatedly listens for the spoken wake word. */
class JarvisWakeService : Service() {
    companion object {
        const val ACTION_START = "com.jarvis.assistant.START_WAKE"
        const val ACTION_STOP = "com.jarvis.assistant.STOP_WAKE"
        const val ACTION_WAKE = "com.jarvis.assistant.WAKE_PHRASE"
        const val EXTRA_PHRASE = "wake_phrase"
        private const val CHANNEL_ID = "jarvis_wake_channel"
        private const val NOTIFICATION_ID = 7301
        @Volatile var isRunning: Boolean = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var restartPending = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            else -> {
                try {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    val pm = getSystemService(POWER_SERVICE) as PowerManager
                    if (wakeLock == null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JARVIS:WakeWord").apply { setReferenceCounted(false); acquire() }
                    isRunning = true
                    startRecognition()
                } catch (e: Exception) {
                    Log.e("JARVIS-Wake", "Could not start foreground microphone service", e)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "JARVIS em segundo plano", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Indica que o JARVIS está escutando a palavra de ativação"
                setShowBadge(false)
            })
        }
    }

    private fun buildNotification(): Notification {
        val open = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("J.A.R.V.I.S. ativo")
            .setContentText("Escutando a palavra ‘Jarvis’. Toque para abrir.")
            .setContentIntent(pi).setOngoing(true).build()
    }

    private fun startRecognition() {
        if (!isRunning || listening || restartPending) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("JARVIS-Wake", "Speech recognition unavailable")
            scheduleRestart(5000)
            return
        }
        try {
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { listening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) { listening = false; scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500 else 700) }
                override fun onResults(results: android.os.Bundle?) {
                    listening = false
                    val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    Log.d("JARVIS-Wake", "Heard: $heard")
                    val lower = heard.lowercase(Locale.forLanguageTag("pt-BR"))
                    val idx = lower.indexOf("jarvis")
                    if (idx >= 0) {
                        val command = heard.substring((idx + "jarvis".length).coerceAtMost(heard.length)).trim(' ', ',', '.', '!', '?', ':', ';')
                        val wake = Intent(ACTION_WAKE).setPackage(packageName).putExtra(EXTRA_PHRASE, command)
                        sendBroadcast(wake)
                        // Brief pause reduces the chance of the service transcribing JARVIS's own reply.
                        scheduleRestart(2200)
                    } else scheduleRestart(250)
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            listening = true
            recognizer?.startListening(speechIntent)
        } catch (e: Exception) {
            listening = false
            Log.e("JARVIS-Wake", "Recognition start failed", e)
            scheduleRestart(1800)
        }
    }

    private fun scheduleRestart(delay: Long) {
        if (!isRunning || restartPending) return
        restartPending = true
        handler.postDelayed({ restartPending = false; listening = false; startRecognition() }, delay)
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        wakeLock = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
