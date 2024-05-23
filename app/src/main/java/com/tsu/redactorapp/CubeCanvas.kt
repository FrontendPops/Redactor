package com.tsu.redactorapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class DiceView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLUE
        strokeWidth = 5f
        textSize = 40f
        isAntiAlias = true
    }

    private var angleX = 0f
    private var angleY = 0f
    private var angleZ = 0f
    private var previousX = 0f
    private var previousY = 0f

    init {
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCube(canvas)
    }

    private fun drawCube(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val size = 300f

        val vertices = arrayOf(
            floatArrayOf(-size, -size, -size),
            floatArrayOf(size, -size, -size),
            floatArrayOf(size, size, -size),
            floatArrayOf(-size, size, -size),
            floatArrayOf(-size, -size, size),
            floatArrayOf(size, -size, size),
            floatArrayOf(size, size, size),
            floatArrayOf(-size, size, size)
        )

        vertices.forEach { vertex ->
            rotateX(vertex, angleX)
            rotateY(vertex, angleY)
            rotateZ(vertex, angleZ)
        }

        val projectedVertices = vertices.map { project(it, centerX, centerY) }

        val edges = arrayOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )

        edges.forEach { (start, end) ->
            val (startX, startY) = projectedVertices[start]
            val (endX, endY) = projectedVertices[end]
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        val faceCenters = arrayOf(
            floatArrayOf(0f, 0f, -size),
            floatArrayOf(0f, 0f, size),
            floatArrayOf(0f, -size, 0f),
            floatArrayOf(0f, size, 0f),
            floatArrayOf(-size, 0f, 0f),
            floatArrayOf(size, 0f, 0f)
        )

        faceCenters.forEachIndexed { index, center ->
            rotateX(center, angleX)
            rotateY(center, angleY)
            rotateZ(center, angleZ)
            val (x, y) = project(center, centerX, centerY)
            canvas.drawText((index + 1).toString(), x, y, paint)
        }
    }

    private fun project(vertex: FloatArray, centerX: Float, centerY: Float): Pair<Float, Float> {
        val (x, y, z) = vertex
        val factor = 500 / (z + 800)
        val projectedX = x * factor + centerX
        val projectedY = y * factor + centerY
        return projectedX to projectedY
    }

    private fun rotateX(vertex: FloatArray, angle: Float) {
        val (y, z) = vertex[1] to vertex[2]
        vertex[1] = y * cos(angle) - z * sin(angle)
        vertex[2] = y * sin(angle) + z * cos(angle)
    }

    private fun rotateY(vertex: FloatArray, angle: Float) {
        val (x, z) = vertex[0] to vertex[2]
        vertex[0] = x * cos(angle) + z * sin(angle)
        vertex[2] = -x * sin(angle) + z * cos(angle)
    }

    private fun rotateZ(vertex: FloatArray, angle: Float) {
        val (x, y) = vertex[0] to vertex[1]
        vertex[0] = x * cos(angle) - y * sin(angle)
        vertex[1] = x * sin(angle) + y * cos(angle)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                val dy = event.y - previousY
                angleY += -dx * 0.01f
                angleX += dy * 0.01f
                previousX = event.x
                previousY = event.y
                invalidate()
            }
        }
        return true
    }
}