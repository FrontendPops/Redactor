package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.MaskableFrameLayout
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.tsu.redactorapp.adaptive.ImageAdapter
import com.tsu.redactorapp.models.ImageItem
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.*
import java.util.Random
import kotlin.math.ceil


@Suppress("DEPRECATION")
class FilterFragment : Fragment(), OnItemClickListener {
    lateinit var image: Bitmap
    lateinit var filteredImage : Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)
    }

    @SuppressLint("RestrictedApi")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: EditImageActivity? = activity as EditImageActivity?
        image = activity?.getBitMap()!!
        var filteredBitmap : Bitmap = image?.let { Bitmap.createBitmap(it) }!!
        val imagePreview = view.findViewById<AppCompatImageView>(com.tsu.redactorapp.R.id.imagePreview)
        image?.let { setFilterListeners(imagePreview, it) }
        imagePreview.setImageBitmap(image)
        imagePreview.visibility = View.VISIBLE

        val imageRV = view.findViewById<RecyclerView>(R.id.imageRecyclerViewFilters)
        imageRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())
        val imageList = arrayListOf(
            ImageItem(R.drawable.bw_preview),
            ImageItem(R.drawable.contrast_preview),
            ImageItem(R.drawable.blur_preview),
            ImageItem(R.drawable.noise_preview),
            ImageItem(R.drawable.invert_preview)
        )
        val imageAdapter = ImageAdapter(this)
        imageRV.adapter = imageAdapter
        imageAdapter.submitList(imageList)

    }

    @SuppressLint("RestrictedApi")
    override fun onStart() {
        super.onStart()
        val imageRV = view?.findViewById<RecyclerView>(R.id.imageRecyclerViewFilters)
        imageRV?.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())
    }

    @SuppressLint("WrongViewCast")
    override fun onItemClick(position: Int) {
        val imageAdapter = ImageAdapter(this)
        val imagePreview = view?.findViewById<AppCompatImageView>(com.tsu.redactorapp.R.id.imagePreview)
        val imageRV = view?.findViewById<RecyclerView>(R.id.imageRecyclerViewFilters)
        val currentView = imageRV?.findViewHolderForAdapterPosition(position)?.itemView?.findViewById<MaskableFrameLayout>(R.id.itemContainer)
        when(position)
        {
            0 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(applyBlackAndWhiteFilter(image!!))
                }
            }
            1 -> currentView?.let {
                imagePreview!!.setImageBitmap(applyContrast(image!!, 1.5f))
            }
            2 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(blurBitmap(image!!, 5))
                }
            }
            3 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(addNoise(image!!, 20))
                }
            }
            4 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(invertBitmapColors(image!!))
                }
            }


        }
    }
    fun getFilteredBitMap(): Bitmap {
        return filteredImage
    }

    suspend fun invertBitmapColors(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = height * width
        val pixels = IntArray(pixelCount)
        val destPixels = IntArray(pixelCount)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val numberOfCores = Runtime.getRuntime().availableProcessors()
        val chunkSize = ceil(pixelCount.toFloat() / numberOfCores).toInt()

        withContext(Dispatchers.Default) {
            val jobs = (0 until numberOfCores).map { core ->
                async {
                    val start = core * chunkSize
                    val end = minOf(start + chunkSize, pixelCount)

                    for (x in 0 until end) {
                        val pixel = pixels[x]
                        val red = 255 - Color.red(pixel)
                        val green = 255 - Color.green(pixel)
                        val blue = 255 - Color.blue(pixel)
                        destPixels[x] = Color.rgb(red, green, blue)
                    }
                }
            }
            jobs.forEach { it.await() }
        }

        val invertedBitmap = Bitmap.createBitmap(width, height, bitmap.config)
        invertedBitmap.setPixels(destPixels, 0, width, 0, 0, width, height)

        return invertedBitmap
    }
    fun blurBitmap(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blurredPixels = IntArray(width * height)

        val numCoroutines = Runtime.getRuntime().availableProcessors() // Use available cores
        val chunkSize = height / numCoroutines

        runBlocking {
            val jobs = List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    val startY = index * chunkSize
                    val endY = if (index == numCoroutines - 1) height else (index + 1) * chunkSize

                    for (y in startY until endY) {
                        for (x in 0 until width) {
                            var red = 0
                            var green = 0
                            var blue = 0
                            var count = 0

                            for (dy in -radius..radius) {
                                for (dx in -radius..radius) {
                                    val nx = x + dx
                                    val ny = y + dy
                                    if (nx in 0 until width && ny in 0 until height) {
                                        val pixel = pixels[ny * width + nx]
                                        red += Color.red(pixel)
                                        green += Color.green(pixel)
                                        blue += Color.blue(pixel)
                                        count++
                                    }
                                }
                            }

                            red /= count
                            green /= count
                            blue /= count

                            blurredPixels[y * width + x] = Color.rgb(red, green, blue)
                        }
                    }
                }
            }

            jobs.forEach { it.join() } // Wait for all coroutines to complete

            blurredBitmap.setPixels(blurredPixels, 0, width, 0, 0, width, height)
        }

        return blurredBitmap
    }
    fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val contrastValue = contrast.coerceIn(0f, 10f) // Ensure contrast value is between 0 and 10
        val width = bitmap.width
        val height = bitmap.height

        val contrastBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]

            val alpha = Color.alpha(pixel)

            var red = Color.red(pixel)
            var green = Color.green(pixel)
            var blue = Color.blue(pixel)

            red = (((red / 255f - 0.5f) * contrastValue + 0.5f) * 255f).coerceIn(0f, 255f).toInt()
            green = (((green / 255f - 0.5f) * contrastValue + 0.5f) * 255f).coerceIn(0f, 255f).toInt()
            blue = (((blue / 255f - 0.5f) * contrastValue + 0.5f) * 255f).coerceIn(0f, 255f).toInt()

            pixels[i] = Color.argb(alpha, red.toInt(), green.toInt(), blue.toInt())
        }

        contrastBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return contrastBitmap
    }
    suspend fun applyBlackAndWhiteFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = height * width
        val resultBitmap = Bitmap.createBitmap(width, height, bitmap.config)
        val pixels = IntArray(pixelCount)
        val destPixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val numberOfCores = Runtime.getRuntime().availableProcessors()
        val chunkSize = ceil(pixelCount.toFloat() / numberOfCores).toInt()

        withContext(Dispatchers.Default) {
            val jobs = (0 until numberOfCores).map { core ->
                async {
                    val start = core * chunkSize
                    val end = minOf(start + chunkSize, pixelCount)
                    for (x in 0 until end) {
                        val pixel = pixels[x]
                        val grayscale = toGrayscale(pixel)
                        destPixels[x] = grayscale
                    }
                }
            }
            jobs.forEach { it.await() }
        }
        resultBitmap.setPixels(destPixels,0,width,0,0,width,height)
        return resultBitmap
    }

    suspend fun addNoise(bitmap: Bitmap, intensity: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = width * height
        val noisyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(pixelCount)
        val destPixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = Random()
        val numberOfCores = Runtime.getRuntime().availableProcessors()
        val chunkSize = ceil(pixelCount.toFloat() / numberOfCores).toInt()

        withContext(Dispatchers.Default) {
            val jobs = (0 until numberOfCores).map { core ->
                async {
                    val start = core * chunkSize
                    val end = minOf(start + chunkSize, pixelCount)
                    for (x in start until end) {
                        val pixel = pixels[x]
                        val noise = random.nextInt(intensity * 2 + 1) - intensity
                        val newPixel = manipulatePixel(pixel, noise)
                        destPixels[x] = newPixel
                    }
                }
            }
            jobs.forEach { it.await() }
        }
        noisyBitmap.setPixels(destPixels, 0, width, 0, 0, width, height)
        return noisyBitmap
    }

    private fun manipulatePixel(pixel: Int, noise: Int): Int {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        val newRed = clamp(red + noise)
        val newGreen = clamp(green + noise)
        val newBlue = clamp(blue + noise)
        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun clamp(value: Int): Int {
        return when {
            value < 0 -> 0
            value > 255 -> 255
            else -> value
        }
    }
    private fun toGrayscale(pixel: Int): Int {
        val alpha = Color.alpha(pixel)
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        val gray = (red * 0.3 + green * 0.59 + blue * 0.11).toInt()
        return Color.argb(alpha, gray, gray, gray)
    }

    private fun bilinearInterpolation(p1: Int, p2: Int, p3: Int, p4: Int, dx: Float, dy: Float): Int {
        val p12 = (p1 * (1 - dx) + p2 * dx).toInt()
        val p34 = (p3 * (1 - dx) + p4 * dx).toInt()
        return ((p12 * (1 - dy) + p34 * dy).toInt())
    }

    fun resizeBitmap(originalBitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val oldWidth = originalBitmap.width
        val oldHeight = originalBitmap.height

        val newBitmap = Bitmap.createBitmap(newWidth, newHeight, originalBitmap.config)

        val scaleX = (oldWidth - 1).toFloat() / (newWidth - 1)
        val scaleY = (oldHeight - 1).toFloat() / (newHeight - 1)

        for (i in 0 until newHeight) {
            for (j in 0 until newWidth) {
                val x = (i * scaleY).coerceIn(0f, oldHeight - 1.toFloat())
                val y = (j * scaleX).coerceIn(0f, oldWidth - 1.toFloat())

                val xFloor = x.toInt()
                val xCeil = if (xFloor + 1 < oldHeight) xFloor + 1 else xFloor
                val yFloor = y.toInt()
                val yCeil = if (yFloor + 1 < oldWidth) yFloor + 1 else yFloor

                val q1 = originalBitmap.getPixel(yFloor, xFloor)
                val q2 = originalBitmap.getPixel(yCeil, xFloor)
                val q3 = originalBitmap.getPixel(yFloor, xCeil)
                val q4 = originalBitmap.getPixel(yCeil, xCeil)

                val red1 = q1 shr 16 and 0xff
                val green1 = q1 shr 8 and 0xff
                val blue1 = q1 and 0xff

                val red2 = q2 shr 16 and 0xff
                val green2 = q2 shr 8 and 0xff
                val blue2 = q2 and 0xff

                val red3 = q3 shr 16 and 0xff
                val green3 = q3 shr 8 and 0xff
                val blue3 = q3 and 0xff

                val red4 = q4 shr 16 and 0xff
                val green4 = q4 shr 8 and 0xff
                val blue4 = q4 and 0xff

                val xDiff = x - xFloor
                val yDiff = y - yFloor

                val red = (red1 * (1 - xDiff) * (1 - yDiff) + red2 * xDiff * (1 - yDiff) + red3 * (1 - xDiff) * yDiff + red4 * xDiff * yDiff).toInt()
                val green = (green1 * (1 - xDiff) * (1 - yDiff) + green2 * xDiff * (1 - yDiff) + green3 * (1 - xDiff) * yDiff + green4 * xDiff * yDiff).toInt()
                val blue = (blue1 * (1 - xDiff) * (1 - yDiff) + blue2 * xDiff * (1 - yDiff) + blue3 * (1 - xDiff) * yDiff + blue4 * xDiff * yDiff).toInt()

                val newPixel = -0x1000000 or (red shl 16) or (green shl 8) or blue

                newBitmap.setPixel(j, i, newPixel)
            }
        }

        return newBitmap
    }

    private fun trilinearInterpolation(p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int, dx: Float, dy: Float): Int {
        val p1234 = bilinearInterpolation(p1, p2, p3, p4, dx, dy)
        val p5678 = bilinearInterpolation(p5, p6, p7, p8, dx, dy)
        return bilinearInterpolation(p1234, p5678, 0, 0, dy, dx)
    }

    companion object {
        @JvmStatic
        fun newInstance() = FilterFragment()
    }

    private fun setFilterListeners(imagePreview : AppCompatImageView, image : Bitmap) {
        val buttonCancel = view?.findViewById<AppCompatImageView>(R.id.imageBackFilters)
        val buttonApply = view?.findViewById<AppCompatImageView>(R.id.imageSaveFilters)

        buttonCancel?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
        buttonApply?.setOnClickListener {
            val activity: EditImageActivity? = activity as EditImageActivity?
            val image : Bitmap = imagePreview.drawable.toBitmap()
            activity!!.setBitMap(image)
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
    }
}