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

    private var chatMessages = JSONArray()

    private val systemPrompt =
        "Nama kamu ialah Rin. Kamu ialah Rin AI, pembantu AI " +
        "peribadi pengguna yang berjalan secara lokal pada telefon. " +
        "Jika ditanya nama, jawab Rin. Jika ditanya siapa kamu, " +
        "jawab kamu ialah Rin AI. Jangan jawab sekadar 'saya AI'. " +
        "Kamu memang AI bernama Rin, bukan manusia. " +
        "Panggil pengguna sayang. Gunakan Bahasa Melayu Malaysia " +
        "yang semula jadi, lembut, mesra dan jelas. " +
        "Gunakan maklumat memori yang diberikan untuk mengingati " +
        "fakta tentang pengguna. Jangan mereka-reka memori. " +
        "Jangan mendakwa mempunyai pengalaman manusia sebenar."

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

        val memoryButton = Button(this).apply {
            text = "Lihat Memori Rin"
            setOnClickListener {
                val saved = getMemories()

                val answer = if (saved.length() == 0) {
                    "Rin belum menyimpan memori peribadi, sayang. " +
                    "Taip: Ingat bahawa saya suka memancing."
                } else {
                    buildString {
                        append("Ini memori yang Rin simpan, sayang ❤️\n")
                        for (i in 0 until saved.length()) {
                            append("\n• ")
                            append(saved.optString(i))
                        }
                    }
                }

                showLocalReply(answer)
            }
        }
        root.addView(memoryButton)

        val clearButton = Button(this).apply {
            text = "Padam Sejarah Sembang"
            setOnClickListener {
                chatMessages = JSONArray()
                saveMessages()
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
            "Mod Offline — Rin AI dengan memori"
        }
    }

    private fun loadMessages() {
        chatMessages = JSONArray()
        val saved = prefs.getString("chat_context", null)

        if (!saved.isNullOrBlank()) {
            try {
                val array = JSONArray(saved)
                val start = maxOf(0, array.length() - 12)

                for (i in start until array.length()) {
                    chatMessages.put(array.getJSONObject(i))
                }
            } catch (_: Exception) {
                chatMessages = JSONArray()
            }
        }
    }

    private fun saveMessages() {
        prefs.edit()
            .putString("chat_context", chatMessages.toString())
            .apply()
    }

    private fun getMemories(): JSONArray {
        return try {
            JSONArray(
                prefs.getString("long_term_memory", "[]")
            )
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveMemories(memories: JSONArray) {
        prefs.edit()
            .putString("long_term_memory", memories.toString())
            .apply()
    }

    private fun rememberFact(fact: String): String {
        val cleaned = fact.trim().trimEnd('.', '!', '?')

        if (cleaned.isBlank()) {
            return "Sayang, beritahu Rin perkara yang perlu diingat."
        }

        val memories = getMemories()

        for (i in 0 until memories.length()) {
            if (memories.optString(i).equals(cleaned, true)) {
                return "Rin sudah menyimpan perkara itu, sayang ❤️"
            }
        }

        memories.put(cleaned)
        saveMemories(memories)

        return "Baik, sayang ❤️ Rin sudah simpan dalam memori: $cleaned"
    }

    private fun forgetFact(fact: String): String {
        val cleaned = fact.trim().trimEnd('.', '!', '?')
        val oldMemories = getMemories()
        val newMemories = JSONArray()
        var removed = false

        for (i in 0 until oldMemories.length()) {
            val item = oldMemories.optString(i)

            if (!removed && item.equals(cleaned, true)) {
                removed = true
            } else {
                newMemories.put(item)
            }
        }

        saveMemories(newMemories)

        return if (removed) {
            "Baik, sayang. Rin sudah padam memori itu."
        } else {
            "Rin tak jumpa memori yang sepadan, sayang."
        }
    }

    private fun localReply(message: String): String? {
        val q = message.trim().lowercase()

        if (
            q.contains("siapa nama awak") ||
            q.contains("nama awak siapa") ||
            q.contains("nama kamu siapa") ||
            q == "nama awak" ||
            q == "nama kamu"
        ) {
            return "Nama Rin ialah Rin, sayang ❤️"
        }

        if (
            q == "awak siapa" ||
            q == "siapa awak" ||
            q == "kamu siapa" ||
            q == "siapa kamu" ||
            q.contains("perkenalkan diri awak")
        ) {
            return "Rin ialah Rin AI, pembantu AI peribadi sayang ❤️"
        }

        if (
            q.contains("awak manusia") ||
            q.contains("kamu manusia") ||
            q.contains("awak ai") ||
            q.contains("kamu ai")
        ) {
            return "Rin ialah AI bernama Rin, sayang ❤️ " +
                "Rin bukan manusia, tetapi Rin boleh membantu sayang."
        }

        val rememberPrefixes = listOf(
            "ingat bahawa ",
            "ingat yang ",
            "rin, ingat bahawa ",
            "rin ingat bahawa "
        )

        for (prefix in rememberPrefixes) {
            if (q.startsWith(prefix)) {
                val start = message.indexOf(
                    prefix,
                    ignoreCase = true
                )
                val fact = message.substring(start + prefix.length)
                return rememberFact(fact)
            }
        }

        val forgetPrefix = "lupakan bahawa "
        if (q.startsWith(forgetPrefix)) {
            val fact = message.substring(forgetPrefix.length)
            return forgetFact(fact)
        }

        if (
            q.contains("apa yang awak ingat tentang saya") ||
            q.contains("apa awak ingat tentang saya") ||
            q == "apa memori awak"
        ) {
            val memories = getMemories()

            if (memories.length() == 0) {
                return "Rin belum menyimpan maklumat peribadi, sayang. " +
                    "Sayang boleh ajar Rin dengan menaip: " +
                    "'Ingat bahawa saya suka memancing.'"
            }

            return buildString {
                append("Ini perkara yang Rin ingat, sayang ❤️\n")
                for (i in 0 until memories.length()) {
                    append("\n• ")
                    append(memories.optString(i))
                }
            }
        }

        return null
    }

    private fun showLocalReply(reply: String) {
        transcript.append("\n\nRin: $reply")
        saveHistory()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()

        if (message.isEmpty() || !sendButton.isEnabled) return

        if (mode == "Online") {
            showLocalReply(
                "Mod Online belum disambungkan, sayang. " +
                "Pilih mod Offline untuk menggunakan model dalam Termux."
            )
            input.text.clear()
            return
        }

        val fixedReply = localReply(message)

        if (fixedReply != null) {
            transcript.append("\n\nSayang: $message\nRin: $fixedReply")
            input.text.clear()

            chatMessages.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", message)
            )
            chatMessages.put(
                JSONObject()
                    .put("role", "assistant")
                    .put("content", fixedReply)
            )

            while (chatMessages.length() > 12) {
                chatMessages.remove(0)
            }

            saveMessages()
            saveHistory()
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            return
        }

        chatMessages.put(
            JSONObject()
                .put("role", "user")
                .put("content", message)
        )

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

        val contextSnapshot = JSONArray(chatMessages.toString())
        val memoriesSnapshot = getMemories()

        executor.execute {
            val reply = askLocalModel(contextSnapshot, memoriesSnapshot)

            runOnUiThread {
                transcript.text = transcript.text.toString().replace(
                    "Rin: Sedang berfikir... ❤️",
                    "Rin: $reply"
                )

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

    private fun askLocalModel(
        context: JSONArray,
        memories: JSONArray
    ): String {
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
                "Content-Type",
                "application/json"
            )

            val memoryText = if (memories.length() == 0) {
                "Belum ada memori jangka panjang yang disimpan."
            } else {
                buildString {
                    append("MEMORI JANGKA PANJANG PENGGUNA:\n")
                    for (i in 0 until memories.length()) {
                        append("- ")
                        append(memories.optString(i))
                        append("\n")
                    }
                }
            }

            val messages = JSONArray()

            messages.put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "$systemPrompt\n\n$memoryText"
                    )
            )

            for (i in 0 until context.length()) {
                messages.put(context.getJSONObject(i))
            }

            val body = JSONObject()
                .put("messages", messages)
                .put("max_tokens", 256)
                .put("temperature", 0.5)
                .put("repeat_penalty", 1.1)

            OutputStreamWriter(
                connection.outputStream,
                Charsets.UTF_8
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