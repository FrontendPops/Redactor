package com.tsu.redactorapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class RotationFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rotation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: EditImageActivity? = activity as EditImageActivity?
        val image = activity?.getBitMap()
        val imagePreview = view.findViewById<AppCompatImageView>(R.id.imageView2)
        val rotatedImageView = view.findViewById<AppCompatImageView>(R.id.imageView3)
        imagePreview.setImageBitmap(image)
        imagePreview.visibility = View.VISIBLE

        val rotateButtonLeft = view.findViewById<Button>(R.id.buttonLeft)
        rotateButtonLeft.setOnClickListener {
            rotatedImageView.visibility = View.INVISIBLE
            imagePreview.visibility = View.VISIBLE
            rotatitonLeft(imagePreview)
        }

        val rotateButtonRight = view.findViewById<Button>(R.id.buttonRight)
        rotateButtonRight.setOnClickListener {
            rotatedImageView.visibility = View.INVISIBLE
            imagePreview.visibility = View.VISIBLE
            rotatitonRight(imagePreview)
        }

        val seekBar = view.findViewById<SeekBar>(R.id.seekBarForImage)
        //var angle = 0f

        var previousProgress = seekBar.progress

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val delta = progress - previousProgress
                val angle: Float = if (delta >= 0) {
                    // Ползунок движется вправо
                    (progress - 50).toFloat() * 360f / 50f
                } else {
                    // Ползунок движется влево
                    360 - (50 - progress).toFloat() * 360f / 50f
                }
                previousProgress = progress
                imagePreview.visibility = View.INVISIBLE
                rotatedImageView.visibility = View.VISIBLE
                rotationAnyAngle(rotatedImageView, image!!, angle)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = RotationFragment()
    }

    fun rotatitonLeft(imagePreview: AppCompatImageView) {
        val bitmapDrawable = imagePreview.drawable
        val bitmap = (bitmapDrawable as BitmapDrawable).bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // углы после поворота
        val radians = Math.PI / 2
        val cosTheta = cos(radians)
        val sinTheta = sin(radians)
        val newWidth = (height * abs(sinTheta) + width * abs(cosTheta)).toInt()
        val newHeight = (height * abs(cosTheta) + width * abs(sinTheta)).toInt()

        // центр нового изображения
        val centerX = newWidth / 2f
        val centerY = newHeight / 2f

        val newPixels = IntArray(newWidth * newHeight)

        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                // исходные координаты пикселя после поворота
                val newX = cosTheta * (centerX - x) + sinTheta * (centerY - y) + width / 2
                val newY = -sinTheta * (centerX - x) + cosTheta * (centerY - y) + height / 2

                // Округление исходных координат
                val roundedX = newX.roundToInt()
                val roundedY = newY.roundToInt()

                if (roundedX in 0 until width && roundedY in 0 until height) {
                    val index = roundedY * width + roundedX
                    newPixels[y * newWidth + x] = pixels[index]
                }
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        rotatedBitmap.setPixels(newPixels, 0, newWidth, 0, 0, newWidth, newHeight)
        imagePreview.setImageBitmap(rotatedBitmap)
    }

    fun rotatitonRight(imagePreview: AppCompatImageView) {
        val bitmapDrawable = imagePreview.drawable
        val bitmap = (bitmapDrawable as BitmapDrawable).bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val radians = Math.PI / 2
        val cosTheta = cos(radians)
        val sinTheta = sin(radians)
        val newWidth = (height * abs(sinTheta) + width * abs(cosTheta)).toInt()
        val newHeight = (height * abs(cosTheta) + width * abs(sinTheta)).toInt()

        val centerX = newWidth / 2f
        val centerY = newHeight / 2f

        val newPixels = IntArray(newWidth * newHeight)

        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                val newX = cosTheta * (x - centerX) + sinTheta * (y - centerY) + width / 2
                val newY = -sinTheta * (x - centerX) + cosTheta * (y - centerY) + height / 2

                val roundedX = newX.roundToInt()
                val roundedY = newY.roundToInt()

                if (roundedX in 0 until width && roundedY in 0 until height) {
                    val index = roundedY * width + roundedX
                    newPixels[y * newWidth + x] = pixels[index]
                }
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        rotatedBitmap.setPixels(newPixels, 0, newWidth, 0, 0, newWidth, newHeight)
        imagePreview.setImageBitmap(rotatedBitmap)
    }

    fun rotationAnyAngle(imageView: AppCompatImageView, originalBitmap: Bitmap, angle: Float) {
        val imageViewWidth = imageView.width.toFloat()
        val imageViewHeight = imageView.height.toFloat()

        val width = originalBitmap.width
        val height = originalBitmap.height
        val pixels = IntArray(width * height)
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val radians = Math.toRadians(angle.toDouble())
        val cosTheta = Math.cos(radians)
        val sinTheta = Math.sin(radians)

        val centerX = width / 2f
        val centerY = height / 2f

        val newWidth = (abs(cosTheta) * width + abs(sinTheta) * height).toFloat()
        val newHeight = (abs(sinTheta) * width + abs(cosTheta) * height).toFloat()

        val newPixels = IntArray(imageViewWidth.toInt() * imageViewHeight.toInt()) { Color.TRANSPARENT }

        for (y in 0 until imageViewHeight.toInt()) {
            for (x in 0 until imageViewWidth.toInt()) {
                val newX = x - imageViewWidth / 2
                val newY = y - imageViewHeight / 2

                val rotatedX = cosTheta * newX - sinTheta * newY + centerX
                val rotatedY = sinTheta * newX + cosTheta * newY + centerY

                val originalX = rotatedX.toInt().coerceIn(0, width - 1)
                val originalY = rotatedY.toInt().coerceIn(0, height - 1)

                val destX = x
                val destY = y

                newPixels[destY * imageViewWidth.toInt() + destX] = pixels[originalY * width + originalX]
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(imageViewWidth.toInt(), imageViewHeight.toInt(), Bitmap.Config.ARGB_8888)
        rotatedBitmap.setPixels(newPixels, 0, imageViewWidth.toInt(), 0, 0, imageViewWidth.toInt(), imageViewHeight.toInt())

        imageView.setImageBitmap(rotatedBitmap)
    }
}