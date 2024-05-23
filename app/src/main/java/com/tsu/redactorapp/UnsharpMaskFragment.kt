package com.tsu.redactorapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
@Suppress("DEPRECATION")
class UnsharpMaskFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var root: View
    private var originalBitmap: Bitmap? = null
    private var exportBitmap: Bitmap? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_unsharpmask, container, false)
        val activity: EditImageActivity? = activity as EditImageActivity?
        originalBitmap = activity?.getBitMap()!!
        exportBitmap = originalBitmap
        imageView = root.findViewById(R.id.imageViewPreview)
        imageView.setImageBitmap(originalBitmap)

        val buttonUnSharpMask: Button = root.findViewById(R.id.maskButton)

        buttonUnSharpMask.setOnClickListener {
            GlobalScope.launch {
                unsharpMask(15, 200);
            }
        }
        setListeners()
        return root
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main) // CoroutineScope tied to the Main dispatcher

    private suspend fun unsharpMask(radius: Int, amount: Int) {
        if (originalBitmap == null) {
            return
        }

        // Cancel any existing coroutine to avoid overlapping
        coroutineScope.coroutineContext.cancelChildren()

        coroutineScope.launch {
            originalBitmap?.let { bitmap ->
                val width = bitmap.width
                val height = bitmap.height
                val blurredBitmap = GaussianBlur(bitmap, radius)
                val unsharpMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val pixels = IntArray(width * height)
                val blurredPixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                blurredBitmap.getPixels(blurredPixels, 0, width, 0, 0, width, height)

                val amountOver = amount / 256.0

                for (i in pixels.indices) {
                    val pixel = pixels[i]
                    val blurredPixel = blurredPixels[i]

                    val diffRed = ((pixel shr 16 and 0xFF) - (blurredPixel shr 16 and 0xFF)) * amountOver
                    val diffGreen = ((pixel shr 8 and 0xFF) - (blurredPixel shr 8 and 0xFF)) * amountOver
                    val diffBlue = ((pixel and 0xFF) - (blurredPixel and 0xFF)) * amountOver

                    val newRed = ((pixel shr 16 and 0xFF) + diffRed).coerceIn(0.0, 255.0).toInt()
                    val newGreen = ((pixel shr 8 and 0xFF) + diffGreen).coerceIn(0.0, 255.0).toInt()
                    val newBlue = ((pixel and 0xFF) + diffBlue).coerceIn(0.0, 255.0).toInt()

                    pixels[i] = (0xFF shl 24) or (newRed shl 16) or (newGreen shl 8) or newBlue
                }

                unsharpMaskBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

                exportBitmap = unsharpMaskBitmap

                imageView.setImageBitmap(unsharpMaskBitmap)
            }
            Snackbar.make(root , "Masked", Snackbar.LENGTH_SHORT).show()
        }
    }
    private fun GaussianBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val weights = calculateGaussianWeights(radius)
        val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val finalBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        // по горизонтали размытие
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                var red = 0.0
                var green = 0.0
                var blue = 0.0
                var weightSum = 0.0

                for (k in -radius..radius) {
                    val newX = (x + k).coerceIn(0, bitmap.width - 1)
                    val pixel = bitmap.getPixel(newX, y)
                    val weight = weights[k + radius][0]
                    red += Color.red(pixel) * weight
                    green += Color.green(pixel) * weight
                    blue += Color.blue(pixel) * weight
                    weightSum += weight
                }

                val newRed = (red / weightSum).toInt().coerceIn(0, 255)
                val newGreen = (green / weightSum).toInt().coerceIn(0, 255)
                val newBlue = (blue / weightSum).toInt().coerceIn(0, 255)
                tempBitmap.setPixel(x, y, Color.rgb(newRed, newGreen, newBlue))
            }
        }

        // по вертикали размытие
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                var red = 0.0
                var green = 0.0
                var blue = 0.0
                var weightSum = 0.0

                for (k in -radius..radius) {
                    val newY = (y + k).coerceIn(0, bitmap.height - 1)
                    val pixel = tempBitmap.getPixel(x, newY)
                    val weight = weights[0][k + radius]
                    red += Color.red(pixel) * weight
                    green += Color.green(pixel) * weight
                    blue += Color.blue(pixel) * weight
                    weightSum += weight
                }

                val newRed = (red / weightSum).toInt().coerceIn(0, 255)
                val newGreen = (green / weightSum).toInt().coerceIn(0, 255)
                val newBlue = (blue / weightSum).toInt().coerceIn(0, 255)
                finalBitmap.setPixel(x, y, Color.rgb(newRed, newGreen, newBlue))
            }
        }

        return finalBitmap
    }
    private fun calculateGaussianWeights(radius: Int): Array<DoubleArray> {
        val sigma = radius / 3.0
        val constant = 1 / (2 * Math.PI * sigma * sigma)
        val twoSigmaSquare = 2 * sigma * sigma
        val size = 2 * radius + 1
        val weights = Array(size) { DoubleArray(size) }
        var totalWeight = 0.0

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val distance = i * i + j * j
                val weight = constant * Math.exp(-distance / twoSigmaSquare)
                weights[i + radius][j + radius] = weight
                totalWeight += weight
            }
        }

        // нормализуем веса
        for (i in 0 until size) {
            for (j in 0 until size) {
                weights[i][j] /= totalWeight
            }
        }
        return weights
    }

    @Suppress("DEPRECATION")
    private fun setListeners() {
        val imageBack = root.findViewById<AppCompatImageView>(R.id.imageBackMask)
        val imageSave = root.findViewById<AppCompatImageView>(R.id.imageSaveMask)
        imageBack?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
        imageSave?.setOnClickListener {
            val activity: EditImageActivity? = activity as EditImageActivity?
            exportBitmap?.let { it1 -> activity!!.setBitMap(it1) }
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancel the CoroutineScope when the activity or fragment is destroyed
    }
    companion object {
        @JvmStatic
        fun newInstance() = UnsharpMaskFragment()
    }
}