package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class ScaleFragment : Fragment() {
    private lateinit var imageView: ImageView
    private lateinit var root: View
    private var originalBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_scale, container, false)
        val activity: EditImageActivity? = activity as EditImageActivity?
        originalBitmap = activity?.getBitMap()!!
        imageView = root.findViewById(R.id.imageViewPreview)
        imageView.setImageBitmap(originalBitmap)
        val buttonScaleImage: Button = root.findViewById(R.id.scaleButton)
        val scaleFactors: EditText = root.findViewById(R.id.scaleFactorText)
        setListeners()

        buttonScaleImage.setOnClickListener {
            val scaleFactorText = scaleFactors.text.toString()
            if (scaleFactorText.isNotEmpty()) {
                val scaleFactor: Float? = scaleFactorText.toFloatOrNull()

                if (scaleFactor!! > 1f) {
                    CoroutineScope(Dispatchers.Main).launch {
                        scaleImage(scaleFactor)
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        scaleImageTrilinear(scaleFactor)
                    }
                }

            }
        }
        return root
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun scaleImage(scaleFactor: Float) {
        originalBitmap?.let { bitmap ->
            withContext(Dispatchers.Default) {
                scaledBitmap = scaleBitmap(bitmap, scaleFactor)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(scaledBitmap)
                }
            }
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, scaleFactor: Float): Bitmap {
        val width = (bitmap.width * scaleFactor).toInt()
        val height = (bitmap.height * scaleFactor).toInt()
        val scaledBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val scaleX = bitmap.width.toFloat() / width
        val scaleY = bitmap.height.toFloat() / height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val extraX = x * scaleX
                val extraY = y * scaleY

                val x1 = extraX.toInt()
                val y1 = extraY.toInt()
                val x2 = (x1 + 1).coerceAtMost(bitmap.width - 1)
                val y2 = (y1 + 1).coerceAtMost(bitmap.height - 1)

                val pixel1 = bitmap.getPixel(x1, y1)
                val pixel2 = bitmap.getPixel(x2, y1)
                val pixel3 = bitmap.getPixel(x1, y2)
                val pixel4 = bitmap.getPixel(x2, y2)

                val dx = extraX - x1
                val dy = extraY - y1

                val red = bilinearInterpolation(
                    pixel1 shr 16 and 0xFF,
                    pixel2 shr 16 and 0xFF,
                    pixel3 shr 16 and 0xFF,
                    pixel4 shr 16 and 0xFF,
                    dx,
                    dy
                )
                val green = bilinearInterpolation(
                    pixel1 shr 8 and 0xFF,
                    pixel2 shr 8 and 0xFF,
                    pixel3 shr 8 and 0xFF,
                    pixel4 shr 8 and 0xFF,
                    dx,
                    dy
                )
                val blue = bilinearInterpolation(
                    pixel1 and 0xFF,
                    pixel2 and 0xFF,
                    pixel3 and 0xFF,
                    pixel4 and 0xFF,
                    dx,
                    dy
                )

                val newColor = Color.argb(255, red, green, blue)
                scaledBitmap.setPixel(x, y, newColor)
            }
        }
        Snackbar.make(root, "Scaled", Snackbar.LENGTH_SHORT).show()
        return scaledBitmap
    }

    private fun bilinearInterpolation(
        pixel1: Int,
        pixel2: Int,
        pixel3: Int,
        pixel4: Int,
        dx: Float,
        dy: Float
    ): Int {
        val pixels1 = (pixel1 * (1 - dx) + pixel2 * dx).toInt()
        val pixels2 = (pixel3 * (1 - dx) + pixel4 * dx).toInt()
        return ((pixels1 * (1 - dy) + pixels2 * dy).toInt())
    }

    private fun scaleImageTrilinear(scaleFactor: Float) {
        originalBitmap?.let { bitmap ->
            CoroutineScope(Dispatchers.IO).launch {
                val width = bitmap.width
                val height = bitmap.height
                val scaledWidth = (width * scaleFactor).toInt()
                val scaledHeight = (height * scaleFactor).toInt()
                val scaledBitmapTemp =
                    Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

                val scaleX = width.toFloat() / scaledWidth
                val scaleY = height.toFloat() / scaledHeight

                val gaussianRadius = when {
                    width * scaleFactor <= 100 || height * scaleFactor <= 100 -> 10
                    width * scaleFactor <= 200 || height * scaleFactor <= 200 -> 3
                    else -> 1
                }

                val blurredBitmap = applyGaussianBlur(bitmap, gaussianRadius, gaussianRadius)

                val pixels = IntArray(scaledWidth * scaledHeight)

                withContext(Dispatchers.Default) {
                    for (x in 0 until scaledWidth) {
                        for (y in 0 until scaledHeight) {
                            val extraX = x * scaleX
                            val extraY = y * scaleY

                            val x1 = extraX.toInt()
                            val y1 = extraY.toInt()
                            val x2 = (x1 + 1).coerceAtMost(width - 1)
                            val y2 = (y1 + 1).coerceAtMost(height - 1)

                            val dx1 = extraX - x1
                            val dy1 = extraY - y1
                            val dx2 = 1.0f - dx1
                            val dy2 = 1.0f - dy1

                            val pixel1 = blurredBitmap.getPixel(x1, y1)
                            val pixel2 = blurredBitmap.getPixel(x2, y1)
                            val pixel3 = blurredBitmap.getPixel(x1, y2)
                            val pixel4 = blurredBitmap.getPixel(x2, y2)

                            val pixel5 = if (y1 > 0) blurredBitmap.getPixel(x1, y1 - 1) else pixel1
                            val pixel6 =
                                if (x2 < width - 1) blurredBitmap.getPixel(x2 + 1, y1) else pixel2
                            val pixel7 =
                                if (y2 < height - 1) blurredBitmap.getPixel(x1, y2 + 1) else pixel3
                            val pixel8 =
                                if (x2 < width - 1 && y2 < height - 1) blurredBitmap.getPixel(
                                    x2 + 1,
                                    y2 + 1
                                ) else pixel4

                            val red = trilinearInterpolation(
                                pixel1 shr 16 and 0xFF,
                                pixel2 shr 16 and 0xFF,
                                pixel3 shr 16 and 0xFF,
                                pixel4 shr 16 and 0xFF,
                                pixel5 shr 16 and 0xFF,
                                pixel6 shr 16 and 0xFF,
                                pixel7 shr 16 and 0xFF,
                                pixel8 shr 16 and 0xFF,
                                dx1,
                                dy1,
                                dx2,
                                dy2
                            )
                            val green = trilinearInterpolation(
                                pixel1 shr 8 and 0xFF,
                                pixel2 shr 8 and 0xFF,
                                pixel3 shr 8 and 0xFF,
                                pixel4 shr 8 and 0xFF,
                                pixel5 shr 8 and 0xFF,
                                pixel6 shr 8 and 0xFF,
                                pixel7 shr 8 and 0xFF,
                                pixel8 shr 8 and 0xFF,
                                dx1,
                                dy1,
                                dx2,
                                dy2
                            )
                            val blue = trilinearInterpolation(
                                pixel1 and 0xFF, pixel2 and 0xFF, pixel3 and 0xFF, pixel4 and 0xFF,
                                pixel5 and 0xFF, pixel6 and 0xFF, pixel7 and 0xFF, pixel8 and 0xFF,
                                dx1, dy1, dx2, dy2
                            )

                            pixels[x + y * scaledWidth] = Color.argb(255, red, green, blue)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    scaledBitmapTemp.setPixels(
                        pixels,
                        0,
                        scaledWidth,
                        0,
                        0,
                        scaledWidth,
                        scaledHeight
                    )
                    scaledBitmap = scaledBitmapTemp
                    imageView.setImageBitmap(scaledBitmap)
                    Snackbar.make(root, "Scaled", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun trilinearInterpolation(
        pixel1: Int, pixel2: Int, pixel3: Int, pixel4: Int,
        pixel5: Int, pixel6: Int, pixel7: Int, pixel8: Int,
        dx1: Float, dy1: Float, dx2: Float, dy2: Float
    ): Int {
        val pixels1 = linearInterpolation(pixel1, pixel2, dx1)
        val pixels2 = linearInterpolation(pixel5, pixel6, dx1)
        val pixels3 = linearInterpolation(pixels1, pixels2, dy1)

        val pixels4 = linearInterpolation(pixel3, pixel4, dx1)
        val pixels5 = linearInterpolation(pixel7, pixel8, dx1)
        val pixels6 = linearInterpolation(pixels4, pixels5, dy1)

        return linearInterpolation(pixels3, pixels6, dx2)
    }

    private fun linearInterpolation(pixel1: Int, pixel2: Int, ratio: Float): Int {
        return (pixel1 * (1 - ratio) + pixel2 * ratio).toInt()
    }

    private fun applyGaussianBlur(
        bitmap: Bitmap,
        horizontalRadius: Int,
        verticalRadius: Int
    ): Bitmap {
        val horizontalRadiuses = (horizontalRadius * 3)
        val verticalRadiuses = (verticalRadius * 2)
        val horizontalWeights = gaussianWeights(horizontalRadiuses)
        val verticalWeights = gaussianWeights(verticalRadiuses)
        val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val finalBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                var red = 0.0
                var green = 0.0
                var blue = 0.0
                var weightSum = 0.0

                for (k in -horizontalRadius..horizontalRadius) {
                    val newX = (x + k).coerceIn(0, bitmap.width - 1)
                    val pixel = bitmap.getPixel(newX, y)
                    val weight = horizontalWeights[k + horizontalRadius][0]
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

                for (k in -verticalRadius..verticalRadius) {
                    val newY = (y + k).coerceIn(0, bitmap.height - 1)
                    val pixel = tempBitmap.getPixel(x, newY)
                    val weight = verticalWeights[0][k + verticalRadius]
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
        val size = 2 * radius + 1
        val weights = Array(size) { DoubleArray(size) }
        val sigma = radius / 3.0
        val twoSigma = 2 * sigma * sigma
        var totalWeight = 0.0

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val distance = i * i + j * j
                val weight = Math.exp(-distance / twoSigma) / (Math.PI * twoSigma)
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
        val imageBack = root.findViewById<AppCompatImageView>(R.id.imageBackScale)
        val imageSave = root.findViewById<AppCompatImageView>(R.id.imageSaveScale)
        imageBack?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
        imageSave?.setOnClickListener {
            val activity: EditImageActivity? = activity as EditImageActivity?
            scaledBitmap?.let { it1 -> activity!!.setBitMap(it1) }
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScaleFragment()
    }
}