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
import android.widget.Toolbar
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.slider.Slider
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
        var angle = 0f

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                angle = progress.toFloat() * 360 / 100
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
        val width = originalBitmap.width
        val height = originalBitmap.height
        val pixels = IntArray(width * height)
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val radians = angle * (Math.PI / 180)
        val cosTheta = cos(radians)
        val sinTheta = sin(radians)

        val newPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val centerX = width / 2f
                val centerY = height / 2f

                val newX = cosTheta * (x - centerX) - sinTheta * (y - centerY) + centerX
                val newY = sinTheta * (x - centerX) + cosTheta * (y - centerY) + centerY

                val roundedX = newX.roundToInt()
                val roundedY = newY.roundToInt()

                if (roundedX in 0 until width && roundedY in 0 until height) {
                    val index = roundedY * width + roundedX
                    newPixels[y * width + x] = pixels[index]
                } else {
                    newPixels[y * width + x] = Color.TRANSPARENT
                }
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        rotatedBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)

        // Отображаем повернутое изображение на rotatedImageView
        imageView.setImageBitmap(rotatedBitmap)
    }
}