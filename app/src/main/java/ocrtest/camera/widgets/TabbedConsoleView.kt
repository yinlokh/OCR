package ocrtest.camera.widgets

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.*
import ocrtest.camera.R

/**
 * Widget to act as a shitty pager
 */
class TabbedConsoleView(context: Context, pages: List<String>) : FrameLayout(context, null, 0) {

    var buttonContainer : LinearLayout? = null
    var textContainer : ScrollView? = null
    var textViews : HashMap<String, TextView> = HashMap()
    var buttons : HashMap<String, Button> = HashMap()
    var previouslySelected : String = ""

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.tabbed_console, this, true)
        buttonContainer = findViewById(R.id.buttonContainer)
        textContainer = findViewById(R.id.textContainer)

        for (page in pages) {
            addPage(page)
        }

        textContainer?.addView(textViews.get(pages.get(0)))
        buttons.get(pages.get(0))?.isEnabled = false
        previouslySelected = pages.get(0)
    }

    fun addPage(name: String) {
        val textView = TextView(context)
        textView.textSize = 18f
        textViews.put(name, textView)

        val button = Button(context)
        buttons.put(name, button)
        button.setText(name)
        button.setOnClickListener{view ->
            textContainer?.removeAllViews()
            textContainer?.addView(textView)
            buttons.get(previouslySelected)?.isEnabled = true
            button.isEnabled = false
            previouslySelected = name
        }
        buttonContainer?.addView(button)
    }

    fun getTextView(page : String) : TextView? {
        return textViews.get(page)
    }

    fun markPageLoaded(page : String) {
        buttons.get(page)?.alpha=1f
    }

    fun clearLoadStates() {
        buttons.values.forEach { it -> it.alpha=0.2f }
        textViews.values.forEach { it -> it.setText("") }
    }
}