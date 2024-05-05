package com.tsu.redactorapp

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.fragment.app.Fragment

class FilterFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: EditImageActivity? = activity as EditImageActivity?
        val image = activity?.getBitMap()
        val imagePreview = view.findViewById<AppCompatImageView>(R.id.imagePreview)
        imagePreview.setImageBitmap(image)
        imagePreview.visibility = View.VISIBLE
    }

    fun applyBlackAndWhiteFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)

                // Extracting RGB components
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                // Converting to grayscale using luminance formula
                val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

                // Creating new pixel value with grayscale
                val newPixel = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray

                // Setting pixel in result bitmap
                resultBitmap.setPixel(x, y, newPixel)
            }
        }

        return resultBitmap
    }

    companion object {
        @JvmStatic
        fun newInstance() = FilterFragment()
    }
}