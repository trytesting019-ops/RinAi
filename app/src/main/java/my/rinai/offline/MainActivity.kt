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

class MainActivity : Activity() {

private lateinit var transcript: TextView
private lateinit var input: EditText
private lateinit var status: TextView
private lateinit var scroll: ScrollView

private var mode = "Offline"

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
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    modeRow.addView(
        offlineButton,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    root.addView(modeRow)

    transcript = TextView(this).apply {
        textSize = 17f
        setTextColor(Color.WHITE)
        setPadding(8, 16, 8, 16)
        text = prefs.getString(
            "chat_history",
            "Rin: Hai sayang ❤️\nRin AI Hybrid sedia untuk disediakan."
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

    val sendButton = Button(this).apply {
        text = "Hantar"
        setOnClickListener {
            sendMessage()
        }
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
        "Mod Online — sambungan AI belum disediakan"
    } else {
        "Mod Offline — model AI belum dipasang"
    }
}

private fun sendMessage() {
    val message = input.text.toString().trim()
    if (message.isEmpty()) return

    transcript.append("\n\nSayang: $message\n")

    val reply = if (mode == "Online") {
        "Mod Online belum disambungkan kepada penyedia AI, sayang. ❤️"
    } else {
        "Model AI Offline belum dipasang. Kita akan sediakan enjin AI tempatan selepas ini, sayang. ❤️"
    }

    transcript.append("Rin: $reply")
    input.text.clear()
    saveHistory()

    scroll.post {
        scroll.fullScroll(View.FOCUS_DOWN)
    }
}

private fun saveHistory() {
    prefs.edit()
        .putString("chat_history", transcript.text.toString())
        .apply()
}

}