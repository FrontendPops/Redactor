package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.ceil


@Suppress("DEPRECATION")
class FilterFragment : Fragment(), OnItemClickListener {
    lateinit var image: Bitmap
    lateinit var filteredImage : Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity: EditImageActivity? = activity as EditImageActivity?
        image = activity?.getBitMap()!!
        image.let { Bitmap.createBitmap(it) }
        val imagePreview = view.findViewById<AppCompatImageView>(com.tsu.redactorapp.R.id.imagePreview)
        setFilterListeners(imagePreview)
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

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("WrongViewCast")
    override fun onItemClick(position: Int) {
        ImageAdapter(this)
        val imagePreview = view?.findViewById<AppCompatImageView>(com.tsu.redactorapp.R.id.imagePreview)
        val imageRV = view?.findViewById<RecyclerView>(R.id.imageRecyclerViewFilters)
        val currentView = imageRV?.findViewHolderForAdapterPosition(position)?.itemView?.findViewById<MaskableFrameLayout>(R.id.itemContainer)
        when(position)
        {
            0 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(applyBlackAndWhiteFilter(image))
                }
            }
            1 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(applyContrast(image, 1.5f))
                }
            }
            2 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(applyBlur(image, 5))
                }
            }
            3 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(addNoise(image, 20))
                }
            }
            4 -> currentView?.let {
                GlobalScope.async {
                    imagePreview!!.setImageBitmap(applyInvertion(image))
                }
            }


        }
    }

    private suspend fun applyInvertion(bitmap: Bitmap): Bitmap {
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
    private fun applyBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blurredPixels = IntArray(width * height)

        val numCoroutines = Runtime.getRuntime().availableProcessors()
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

            jobs.forEach { it.join() }

            blurredBitmap.setPixels(blurredPixels, 0, width, 0, 0, width, height)
        }

        return blurredBitmap
    }
    private suspend fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        return withContext(Dispatchers.Default) {
            val contrastValue = contrast.coerceIn(0f, 10f)
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

                pixels[i] = Color.argb(alpha, red, green, blue)
            }

            contrastBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            contrastBitmap
        }
    }
    private suspend fun applyBlackAndWhiteFilter(bitmap: Bitmap): Bitmap {
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

    private suspend fun addNoise(bitmap: Bitmap, intensity: Int): Bitmap {
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

    companion object {
        @JvmStatic
        fun newInstance() = FilterFragment()
    }

    private fun setFilterListeners(imagePreview: AppCompatImageView) {
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
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView2, PreviewFragment.newInstance()).commit()
        }
    }
}