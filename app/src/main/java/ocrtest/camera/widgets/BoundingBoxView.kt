package ocrtest.camera.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * View to control bounding box
 */
class BoundingBoxView(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        var boxStartX: Float = 0f,
        var boxStartY: Float = 0f,
        var boxEndX: Float = 0f,
        var boxEndY: Float = 0f)
    : View(context, attrs, defStyleAttr) {

    constructor(context: Context?): this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?): this(context, attrs, 0)

    override fun onFinishInflate() {
        super.onFinishInflate()

        this.setOnTouchListener(object: OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.actionMasked?.equals(MotionEvent.ACTION_DOWN) == true) {
                    boxStartX = event?.x
                    boxEndX = event?.x
                    boxStartY = event?.y
                    boxEndY = event?.y
                } else if (event?.actionMasked?.equals(MotionEvent.ACTION_MOVE) == true) {
                    boxEndX = event?.x
                    boxEndY = event?.y
                }
                invalidate()
                return true
            }
        })
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val rect = Rect(
                Math.min(boxStartX, boxEndX).toInt(),
                Math.min(boxStartY, boxEndY).toInt(),
                Math.max(boxStartX, boxEndX).toInt(),
                Math.max(boxStartY, boxEndY).toInt())
        val fillPaint = Paint()
        fillPaint.color = 0x55FF0000
        canvas?.drawRect(rect, fillPaint)
    }
}