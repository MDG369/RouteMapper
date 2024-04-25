package com.example.routemapper

import android.graphics.*

import android.graphics.drawable.Drawable

class LineDrawable(private var points: List<PointF>) : Drawable() {
    private val mPaint: Paint = Paint()

    init {
        mPaint.setStrokeWidth(3f)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // Clear the canvas

        val b: Rect = bounds
        mPaint.setColor(-0x10000)

        if (points.isEmpty()) {
            return;
        }

        var xPrev = (b.width()/ 2).toFloat()
        var yPrev = (b.height() / 2).toFloat()

        for ((index, point) in points.withIndex()) {

            val xDist = b.width() * point.x
            val yDist = b.width() * point.y

            canvas.drawLine(xPrev,
                yPrev, xPrev + xDist, yPrev + yDist, mPaint)

            xPrev += xDist
            yPrev += yDist
        }
    }

    override fun onLevelChange(level: Int): Boolean {
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}