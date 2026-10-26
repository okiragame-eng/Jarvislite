package com.example.jarvislite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 200, 48, 48)
        }
        val btn = Button(this).apply { text = "🎤 Сказать команду" }
        statusText = TextView(this).apply {
            text = "Нажми кнопку и говори..."
            textSize = 18f
            setPadding(0, 48, 0, 0)
        }
        layout.addView(btn)
        layout.addView(statusText)
        setContentView(layout)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        } else {
            statusText.text = "На этом устройстве нет офлайн-распознавания речи"
        }

        btn.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            speechRecognizer.startListening(intent)
        }

        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { statusText.text = "Слушаю..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { statusText.text = "Обрабатываю..." }
            override fun onError(error: Int) {
                statusText.text = "Ошибка распознавания (код $error). Попробуй ещё раз."
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                statusText.text = "Ты сказал: \"$text\""
                handleCommand(text.lowercase())
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleCommand(cmd: String) {
        when {
            cmd.contains("ютуб") || cmd.contains("youtube") -> {
                val query = cmd.substringAfter("ютуб").substringAfter("youtube").trim()
                openYouTube(query)
            }
            cmd.contains("ватсап") || cmd.contains("whatsapp") -> {
                val msg = cmd.substringAfter("ватсап").substringAfter("whatsapp").trim()
                shareTo("com.whatsapp", msg)
            }
            cmd.contains("телеграм") || cmd.contains("telegram") -> {
                val msg = cmd.substringAfter("телеграм").substringAfter("telegram").trim()
                shareTo("org.telegram.messenger", msg)
            }
            cmd.contains("открой") -> {
                val appName = cmd.substringAfter("открой").trim()
                openAppByName(appName)
            }
            else -> {
                statusText.text = "Не понял команду: \"$cmd\"\nПопробуй: \"ютуб котики\", \"ватсап привет\", \"открой камера\""
            }
        }
    }

    private fun openYouTube(query: String) {
        val url = if (query.isBlank()) "https://www.youtube.com"
        else "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        statusText.text = "Открываю YouTube: $query"
    }

    private fun shareTo(packageName: String, text: String) {
        if (text.isBlank()) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) startActivity(intent)
            else Toast.makeText(this, "Приложение не найдено", Toast.LENGTH_SHORT).show()
            return
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(packageName)
        }
        try {
            startActivity(sendIntent)
            statusText.text = "Текст готов: \"$text\" — выбери контакт и отправь"
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(fallback, "Отправить через..."))
        }
    }

    private fun openAppByName(name: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val target = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().lowercase().contains(name)
        }
        if (target != null) {
            val intent = pm.getLaunchIntentForPackage(target.packageName)
            if (intent != null) {
                startActivity(intent)
                statusText.text = "Открываю: ${pm.getApplicationLabel(target)}"
                return
            }
        }
        statusText.text = "Приложение \"$name\" не найдено"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
    }
}
