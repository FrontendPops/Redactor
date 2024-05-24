package com.tsu.redactorapp

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    ): View {
        root = inflater.inflate(R.layout.fragment_unsharpmask, container, false)
        val activity: EditImageActivity? = activity as EditImageActivity?
        originalBitmap = activity?.getBitMap()!!
        exportBitmap = originalBitmap
        imageView = root.findViewById(R.id.imageViewPreview)
        imageView.setImageBitmap(originalBitmap)
        val buttonUnSharpMask: Button = root.findViewById(R.id.maskButton)
        setListeners()

        buttonUnSharpMask.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                unsharpMask(15, 200)
            }
        }
        return root
    }

    private val coroutineScope =
        CoroutineScope(Dispatchers.Main)

    private suspend fun unsharpMask(radius: Int, amount: Int) {
        if (originalBitmap == null) {
            return
        }

        withContext(Dispatchers.Default) {
            val blurredBitmap = GaussianBlur(originalBitmap!!, radius)

            val amountOver = amount / 256.0

            val pixels = IntArray(originalBitmap!!.width * originalBitmap!!.height)
            val blurredPixels = IntArray(originalBitmap!!.width * originalBitmap!!.height)

            originalBitmap!!.getPixels(
                pixels,
                0,
                originalBitmap!!.width,
                0,
                0,
                originalBitmap!!.width,
                originalBitmap!!.height
            )
            blurredBitmap.getPixels(
                blurredPixels,
                0,
                originalBitmap!!.width,
                0,
                0,
                originalBitmap!!.width,
                originalBitmap!!.height
            )

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val blurredPixel = blurredPixels[i]

                val diffRed =
                    ((pixel shr 16 and 0xFF) - (blurredPixel shr 16 and 0xFF)) * amountOver
                val diffGreen =
                    ((pixel shr 8 and 0xFF) - (blurredPixel shr 8 and 0xFF)) * amountOver
                val diffBlue = ((pixel and 0xFF) - (blurredPixel and 0xFF)) * amountOver

                val newRed = ((pixel shr 16 and 0xFF) + diffRed).coerceIn(0.0, 255.0).toInt()
                val newGreen = ((pixel shr 8 and 0xFF) + diffGreen).coerceIn(0.0, 255.0).toInt()
                val newBlue = ((pixel and 0xFF) + diffBlue).coerceIn(0.0, 255.0).toInt()

                pixels[i] = (0xFF shl 24) or (newRed shl 16) or (newGreen shl 8) or newBlue
            }

            val unsharpMaskBitmap = Bitmap.createBitmap(
                originalBitmap!!.width,
                originalBitmap!!.height,
                Bitmap.Config.ARGB_8888
            )
            unsharpMaskBitmap.setPixels(
                pixels,
                0,
                originalBitmap!!.width,
                0,
                0,
                originalBitmap!!.width,
                originalBitmap!!.height
            )

            exportBitmap = unsharpMaskBitmap
            withContext(Dispatchers.Main) {
                imageView.setImageBitmap(unsharpMaskBitmap)
                Snackbar.make(root, "Masked", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun GaussianBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val weights = gaussianWeights(radius)
        val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val finalBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

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

    private fun gaussianWeights(radius: Int): Array<DoubleArray> {
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
        coroutineScope.cancel()
    }

    companion object {
        @JvmStatic
        fun newInstance() = UnsharpMaskFragment()
    }
}