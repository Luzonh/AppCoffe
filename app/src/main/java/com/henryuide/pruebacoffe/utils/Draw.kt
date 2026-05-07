package com.henryuide.pruebacoffe.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class Draw(
    context: Context?,
    var react: Rect,
    var text: String
) : View(context) {

    lateinit var paint: Paint
    lateinit var textPaint: Paint

    init {
        init()
    }

    private fun init() {
        paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE

        textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        println("Draw - left: ${react.left.toFloat()}\ntop: ${react.top.toFloat()}\nright: ${react.right.toFloat()}\nbottom: ${react.bottom.toFloat()}\n")
        println("Text - X: ${react.centerX().toFloat()}\nY: ${react.centerY().toFloat()}\n")

        canvas.drawRect(
            react.left.toFloat(),
            react.top.toFloat(),
            react.right.toFloat(),
            react.bottom.toFloat(),
            paint
        )
        canvas.drawText(text, react.centerX().toFloat(), react.centerY().toFloat(), textPaint)
    }
}