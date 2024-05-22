package com.tsu.redactorapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*

class retouchingFragment : Fragment() {
    private val radius = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_retouching, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: EditImageActivity? = activity as EditImageActivity?
        val image = activity?.getBitMap()
        val imagePreview = view.findViewById<AppCompatImageView>(R.id.imageView2)
        imagePreview.setImageBitmap(image)
        imagePreview.visibility = View.VISIBLE

        imagePreview.setOnTouchListener { _, motionEvent ->
            handleTouch(imagePreview, motionEvent)
            true
        }

        //val buttonBrushSize: Button = view.findViewById(R.id.buttonSizeBrush)
        //buttonBrushSize.setOnClickListener {
        //    showPopupMenu(it)
        //}
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.popup_menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.size_small -> {
                    Toast.makeText(requireContext(), "Small selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.size_medium -> {
                    Toast.makeText(requireContext(), "Medium selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.size_large -> {
                    Toast.makeText(requireContext(), "Large selected", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun handleTouch(imageView: ImageView, motionEvent: MotionEvent) {
        val imageDrawable = imageView.drawable as BitmapDrawable
        val bitmap: Bitmap = imageDrawable.bitmap

        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                val coords = getPointerCoords(imageView, motionEvent)
                val x = coords.x.toInt()
                val y = coords.y.toInt()

                // Запуск обработки изображения в фоновом потоке
                CoroutineScope(Dispatchers.Main).launch {
                    val processedBitmap = withContext(Dispatchers.Default) {
                        processImage(bitmap, x, y)
                    }
                    imageView.setImageBitmap(processedBitmap)
                }
            }
        }
    }

    private fun getPointerCoords(view: ImageView, e: MotionEvent): PointF {
        val coords = PointF()
        val imageMatrix = view.imageMatrix

        val values = FloatArray(9)
        imageMatrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]

        val drawable = view.drawable
        val origW = drawable.intrinsicWidth
        val origH = drawable.intrinsicHeight

        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)

        val left = (view.width - actW) / 2
        val top = (view.height - actH) / 2

        coords.x = (e.x - left) / scaleX
        coords.y = (e.y - top) / scaleY

        return coords
    }

    private fun processImage(bitmap: Bitmap, x: Int, y: Int): Bitmap {
        val radius = 5 // Уменьшение радиуса обработки для увеличения скорости

        val colors = mutableListOf<Int>()
        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val pixelX = x + i
                val pixelY = y + j
                if (pixelX >= 0 && pixelX < bitmap.width && pixelY >= 0 && pixelY < bitmap.height) {
                    colors.add(bitmap.getPixel(pixelX, pixelY))
                }
            }
        }

        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        for (color in colors) {
            redSum += Color.red(color)
            greenSum += Color.green(color)
            blueSum += Color.blue(color)
        }
        val avgRed = redSum / colors.size
        val avgGreen = greenSum / colors.size
        val avgBlue = blueSum / colors.size

        val filteredRed = (avgRed * 2).coerceAtMost(255)
        val filteredGreen = (avgGreen * 2).coerceAtMost(255)
        val filteredBlue = (avgBlue * 2).coerceAtMost(255)

        val processedBitmap = bitmap.copy(bitmap.config, true) // Создаем копию Bitmap для изменения в фоновом потоке

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val pixelX = x + i
                val pixelY = y + j
                if (pixelX >= 0 && pixelX < processedBitmap.width && pixelY >= 0 && pixelY < processedBitmap.height) {
                    processedBitmap.setPixel(pixelX, pixelY, Color.rgb(filteredRed, filteredGreen, filteredBlue))
                }
            }
        }
        return processedBitmap
    }

    companion object {
        @JvmStatic
        fun newInstance() = retouchingFragment()
    }
}