
package my.rinai.offline

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var scroll: ScrollView
    private lateinit var sendButton: Button

    private var mode = "Offline"
    private val executor = Executors.newSingleThreadExecutor()

    private val prefs by lazy {
        getSharedPreferences("rin_ai_data", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.rgb(20, 24, 35))
        }

        val title = TextView(this).apply {
            text = "Rin AI Hybrid ❤️"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 12)
        }
        root.addView(title)

        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 12)
        }
        root.addView(status)

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val onlineButton = Button(this).apply {
            text = "Online"
            setOnClickListener {
                mode = "Online"
                updateStatus()
            }
        }

        val offlineButton = Button(this).apply {
            text = "Offline"
            setOnClickListener {
                mode = "Offline"
                updateStatus()
            }
        }

        modeRow.addView(
            onlineButton,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        )
        modeRow.addView(
            offlineButton,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        )
        root.addView(modeRow)

        transcript = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(8, 16, 8, 16)
            text = prefs.getString(
                "chat_history",
                "Rin: Hai sayang ❤️\nRin AI sedia."
            )
        }

        scroll = ScrollView(this)
        scroll.addView(transcript)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        input = EditText(this).apply {
            hint = "Tulis mesej..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(false)
        }
        root.addView(input)

        sendButton = Button(this).apply {
            text = "Hantar"
            setOnClickListener { sendMessage() }
        }
        root.addView(sendButton)

        val clearButton = Button(this).apply {
            text = "Padam Sejarah Sembang"
            setOnClickListener {
                transcript.text = "Rin: Hai sayang ❤️"
                saveHistory()
            }
        }
        root.addView(clearButton)

        setContentView(root)
        updateStatus()
    }

    private fun updateStatus() {
        status.text = if (mode == "Online") {
            "Mod Online belum disambungkan"
        } else {
            "Mod Offline — Qwen melalui Termux"
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        transcript.append("\n\nSayang: $message\n")
        transcript.append("Rin: Sedang berfikir... ❤️")
        input.text.clear()
        sendButton.isEnabled = false
        saveHistory()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        if (mode == "Online") {
            transcript.append(
                "\nMod Online belum disambungkan, sayang."
            )
            sendButton.isEnabled = true
            saveHistory()
            return
        }

        executor.execute {
            val reply = askLocalModel(message)

            runOnUiThread {
                val current = transcript.text.toString()
                val updated = current.replace(
                    "Rin: Sedang berfikir... ❤️",
                    "Rin: $reply"
                )
                transcript.text = updated
                sendButton.isEnabled = true
                saveHistory()
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun askLocalModel(message: String): String {
        var connection: HttpURLConnection? = null

        return try {
            connection = URL(
                "http://127.0.0.1:8080/v1/chat/completions"
            ).openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 120000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type", "application/json"
            )

            val messages = JSONArray()
            messages.put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "Nama awak Rin. Awak pembantu AI tempatan " +
                        "yang mesra, lembut dan prihatin. " +
                        "Panggil pengguna sayang. Jawab dalam " +
                        "Bahasa Melayu Malaysia secara semula jadi. " +
                        "Jangan mendakwa mempunyai pengalaman " +
                        "atau perasaan manusia sebenar."
                    )
            )
            messages.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", message)
            )

            val body = JSONObject()
                .put("messages", messages)
                .put("max_tokens", 256)
                .put("temperature", 0.7)

            OutputStreamWriter(
                connection.outputStream, Charsets.UTF_8
            ).use {
                it.write(body.toString())
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream.bufferedReader().use {
                it.readText()
            }

            if (code !in 200..299) {
                "Rin tak dapat hubungi model. Ralat HTTP $code."
            } else {
                JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        } catch (e: Exception) {
            "Rin tak dapat sambung ke model offline. " +
            "Pastikan llama-server sedang berjalan dalam Termux, " +
            "sayang. ❤️ (${e.javaClass.simpleName})"
        } finally {
            connection?.disconnect()
        }
    }

    private fun saveHistory() {
        prefs.edit()
            .putString("chat_history", transcript.text.toString())
            .apply()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
