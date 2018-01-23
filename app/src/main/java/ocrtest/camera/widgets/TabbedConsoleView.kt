package ocrtest.camera.widgets

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import ocrtest.camera.R
import java.util.*

/**
 * Widget to act as a shitty pager
 */
class TabbedConsoleView(context: Context, pages: List<String>) : FrameLayout(context, null, 0) {

    var buttonContainer : LinearLayout? = null
    var textContainer : ScrollView? = null
    var textViews : HashMap<String, TextView> = HashMap()
    var buttons : HashMap<String, Button> = HashMap()
    var previouslySelected : String = ""
    var scrolled = false
    var autoScrollObservable : PublishSubject<Object> = PublishSubject.create()

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
        autoScrollObservable.observeOn(AndroidSchedulers.mainThread())
                .filter{ !scrolled }
                .subscribe { textContainer?.fullScroll(View.FOCUS_DOWN) }
    }

    fun addPage(name: String) {
        val textView = TextView(context)
        textView.textSize = 16f
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
            autoScrollObservable.onNext(Object())
        }

        textView?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (textContainer?.getChildAt(0)?.equals(textView) ?: false) {
                    autoScrollObservable.onNext(Object())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

        })
        textContainer?.setOnTouchListener { v, event ->
            scrolled = true
            false
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
        scrolled = false
        buttons.values.forEach { it -> it.alpha=0.2f }
        textViews.values.forEach { it -> it.setText("") }
    }
}