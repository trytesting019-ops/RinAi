
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

    // Sejarah mesej untuk konteks model.
    private val chatMessages = JSONArray()

    private val systemPrompt =
        "Nama awak Rin. Awak pembantu AI tempatan yang mesra, " +
        "lembut dan prihatin. Panggil pengguna sayang. " +
        "Jawab dalam Bahasa Melayu Malaysia secara semula jadi. " +
        "Gunakan mesej terdahulu untuk memahami konteks. " +
        "Jangan mendakwa mempunyai pengalaman atau perasaan " +
        "manusia sebenar."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadMessages()

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
                chatMessages.clear()
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
            "Mod Offline — Qwen dengan ingatan konteks"
        }
    }

    private fun loadMessages() {
        chatMessages.clear()

        val saved = prefs.getString("chat_context", null)
        if (!saved.isNullOrBlank()) {
            try {
                val array = JSONArray(saved)
                val start = maxOf(0, array.length() - 12)

                for (i in start until array.length()) {
                    chatMessages.put(array.getJSONObject(i))
                }
            } catch (_: Exception) {
                chatMessages.clear()
            }
        }
    }

    private fun saveMessages() {
        prefs.edit()
            .putString("chat_context", chatMessages.toString())
            .apply()
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        if (!sendButton.isEnabled) return

        if (mode == "Online") {
            transcript.append(
                "\n\nSayang: $message\n" +
                "Rin: Mod Online belum disambungkan."
            )
            input.text.clear()
            saveHistory()
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            return
        }

        chatMessages.put(
            JSONObject()
                .put("role", "user")
                .put("content", message)
        )

        // Hadkan sejarah kepada 12 mesej terakhir.
        while (chatMessages.length() > 12) {
            chatMessages.remove(0)
        }

        transcript.append(
            "\n\nSayang: $message\nRin: Sedang berfikir... ❤️"
        )
        input.text.clear()
        sendButton.isEnabled = false
        saveMessages()
        saveHistory()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        // Salin konteks semasa supaya ia tidak berubah ketika permintaan berjalan.
        val contextSnapshot = JSONArray(chatMessages.toString())

        executor.execute {
            val reply = askLocalModel(contextSnapshot)

            runOnUiThread {
                val current = transcript.text.toString()
                transcript.text = current.replace(
                    "Rin: Sedang berfikir... ❤️",
                    "Rin: $reply"
                )

                // Simpan jawapan model sebagai konteks assistant.
                chatMessages.put(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", reply)
                )

                while (chatMessages.length() > 12) {
                    chatMessages.remove(0)
                }

                saveMessages()
                saveHistory()
                sendButton.isEnabled = true
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun askLocalModel(context: JSONArray): String {
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
                    .put("content", systemPrompt)
            )

            for (i in 0 until context.length()) {
                messages.put(context.getJSONObject(i))
            }

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
                val content = JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                if (content.isBlank()) {
                    "Rin belum dapat menghasilkan jawapan, sayang."
                } else {
                    content
                }
            }
        } catch (e: Exception) {
            "Rin tak dapat sambung ke model offline. " +
            "Pastikan llama-server berjalan dalam Termux. " +
            "Ralat: ${e.javaClass.simpleName}"
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
