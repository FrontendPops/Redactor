package com.tsu.redactorapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class SplineCanvas (
    context: Context,
    attrs: AttributeSet): View(context, attrs) {
        init {

        }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(splinePath, splinePaint)
        canvas.drawPath(path, splinePaint)
        for (point in points) {
            canvas.drawCircle(point.x, point.y, 10f, pointPaint)
        }
    }
    private val pointPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 8f
        color = android.graphics.Color.RED
        style = Paint.Style.FILL
    }
    private val splinePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 5f
        color = android.graphics.Color.BLUE
        style = Paint.Style.STROKE
    }

    private val points = mutableListOf<PointF>()
    private val splinePath = Path()
    private val path = Path()
    private val touchTolerance = 40f
    private var selectedPoint: PointF? = null
    private var lastTouchDownTime: Long = 0
    private val doubleClickDelay: Long = 300
    private var isSplined: Boolean = false
    override fun onTouchEvent(event: MotionEvent): Boolean {
        splinePath.reset()
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                var possiblePoint: PointF? = null
                possiblePoint = points.find { isPointTouched(it, event.x, event.y) }
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTouchDownTime <= doubleClickDelay) {
                    // Double-click detected
                    points.find { isPointTouched(it, x, y) }?.let { selectedPoint ->
                        points.remove(selectedPoint)
                        if (isSplined) {
                            performSpline()
                        } else {
                            connectPoints()
                        }
                        invalidate()
                    }
                }
                else {
                    if (possiblePoint != null)
                    {
                        selectedPoint = possiblePoint
                    }
                    else
                    {
                        points.add(PointF(x, y))
                        connectPoints()
                        invalidate()
                    }
                }
                lastTouchDownTime = currentTime

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                selectedPoint?.let {
                    it.x = x
                    it.y = y
                    if (isSplined) {
                        performSpline()
                    } else {
                        connectPoints()
                    }
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                selectedPoint = null
                if (isSplined) {
                    performSpline()
                } else {
                    connectPoints()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isPointTouched(point: PointF, x: Float, y: Float): Boolean {
        val dx = point.x - x
        val dy = point.y - y
        val distance = sqrt(dx.pow(2) + dy.pow(2))
        return distance <= touchTolerance
    }

    fun performSpline() {
        isSplined = true
        path.reset()
        splinePath.reset()
        if (points.size > 2) {
            val firstPoint = points[0]
            splinePath.moveTo(firstPoint.x, firstPoint.y)

            val tension = 1f

            for (i in 0 until points.size - 1) {
                val p0 = if (i - 1 >= 0) points[i - 1] else firstPoint
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i + 2 < points.size) points[i + 2] else p2

                for (t in 0..100) {
                    val tFloat = t / 100.0f
                    val a0 = -tension * tFloat + 2 * tension * tFloat * tFloat - tension * tFloat * tFloat * tFloat
                    val a1 = 1 + (tension - 3) * tFloat * tFloat + (2 - tension) * tFloat * tFloat * tFloat
                    val a2 = tFloat + (3 - 2 * tension) * tFloat * tFloat - (2 - tension) * tFloat * tFloat * tFloat
                    val a3 = -tension * tFloat * tFloat + tension * tFloat * tFloat * tFloat

                    val x = a0 * p0.x + a1 * p1.x + a2 * p2.x + a3 * p3.x
                    val y = a0 * p0.y + a1 * p1.y + a2 * p2.y + a3 * p3.y
                    splinePath.lineTo(x, y)
                }
            }
        }
        invalidate()
    }
    private fun connectPoints() {
        path.reset()
        isSplined = false
        if (points.size >= 2) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
    }


}