package net.guselnikov.logicblox.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import com.amrdeveloper.codeview.CodeView

class SelectableLineCodeView(c: Context, attrs: AttributeSet? = null): CodeView(c, attrs) {

    var highlightLineNumber: Int? = null
    private val highlightedLineBounds: Rect = Rect()
    private val highlightedLinePaint = Paint().also {
        it.setColor(0x40602020)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        try {
            val highlightedLine = highlightLineNumber ?: return

            getLineBounds(highlightedLine, highlightedLineBounds)
            canvas.drawRect(highlightedLineBounds, highlightedLinePaint)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}