
package my.rinai.offline

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(20, 24, 35))
        }

        val title = TextView(this).apply {
            text = "Rin AI"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val transcript = TextView(this).apply {
            text = "Rin: Hai sayang ❤️\nRin sedia membantu."
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(8, 24, 8, 24)
        }

        val scroll = ScrollView(this)
        scroll.addView(transcript)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        val input = EditText(this).apply {
            hint = "Tulis mesej..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        root.addView(input)

        val send = Button(this).apply {
            text = "Hantar"
            setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    transcript.append("\n\nSayang: $message\nRin: Mesej diterima, sayang ❤️")
                    input.text.clear()
                    scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
        root.addView(send)

        setContentView(root)
    }
}
