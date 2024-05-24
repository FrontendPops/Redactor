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
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class RetouchingFragment : Fragment() {
    private var radius = 25
    private var bitmap: Bitmap? = null
    private var x = 0
    private var y = 0
    private var isBrushSizeFixed = false
    private var saturationCoefficient = 1.0f
    private var processedBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_retouching, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as? EditImageActivity
        val brushSideTextView = view.findViewById<AppCompatTextView>(R.id.textView3)
        val coefficientTextView = view.findViewById<AppCompatTextView>(R.id.textView4)
        bitmap = activity?.getBitMap()
        val imagePreview = view.findViewById<AppCompatImageView>(R.id.imageView2)
        imagePreview.setImageBitmap(bitmap)
        imagePreview.visibility = View.VISIBLE

        val seekBarBrush = view.findViewById<SeekBar>(R.id.seekBarBrush)
        seekBarBrush.max = 50
        seekBarBrush.progress = radius

        seekBarBrush.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isBrushSizeFixed) {
                    radius = progress
                    brushSideTextView.text = radius.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekBarCoefficient = view.findViewById<SeekBar>(R.id.seekBarCoefficient)
        seekBarCoefficient.max = 20
        seekBarCoefficient.progress = (saturationCoefficient * 10).toInt()

        seekBarCoefficient.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                saturationCoefficient = progress / 10.0f
                coefficientTextView.text = saturationCoefficient.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        imagePreview.setOnTouchListener { _, motionEvent ->
            handleTouch(imagePreview, motionEvent)
            true
        }

        setOnClickListeners()
    }

    private fun handleTouch(imageView: ImageView, motionEvent: MotionEvent) {
        val imageDrawable = imageView.drawable as BitmapDrawable
        bitmap = imageDrawable.bitmap

        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val coords = getPointerCoords(imageView, motionEvent)
                x = coords.x.toInt()
                y = coords.y.toInt()

                if (x in 0 until bitmap!!.width && y in 0 until bitmap!!.height) {
                    CoroutineScope(Dispatchers.Main).launch {
                        processedBitmap = withContext(Dispatchers.Default) {
                            val bmp = processImage(bitmap!!, x, y, radius)
                            applyColorSaturation(bmp, x, y, radius, saturationCoefficient)
                        }
                        imageView.setImageBitmap(processedBitmap)
                    }
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
        val origW = drawable?.intrinsicWidth ?: 0
        val origH = drawable?.intrinsicHeight ?: 0

        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)

        val left = (view.width - actW) / 2
        val top = (view.height - actH) / 2

        coords.x = (e.x - left) / scaleX
        coords.y = (e.y - top) / scaleY

        return coords
    }

    private fun processImage(bitmap: Bitmap, x: Int, y: Int, radius: Int): Bitmap {
        val processedBitmap = bitmap.copy(bitmap.config, true)

        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        var count = 0

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val distance = sqrt((i * i + j * j).toDouble())
                if (distance <= radius) {
                    val pixelX = x + i
                    val pixelY = y + j
                    if (pixelX >= 0 && pixelX < processedBitmap.width && pixelY >= 0 && pixelY < processedBitmap.height) {
                        val color = processedBitmap.getPixel(pixelX, pixelY)
                        redSum += Color.red(color)
                        greenSum += Color.green(color)
                        blueSum += Color.blue(color)
                        count++
                    }
                }
            }
        }

        val avgRed = redSum / count
        val avgGreen = greenSum / count
        val avgBlue = blueSum / count

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val distance = sqrt((i * i + j * j).toDouble())
                if (distance <= radius) {
                    val pixelX = x + i
                    val pixelY = y + j
                    if (pixelX >= 0 && pixelX < processedBitmap.width && pixelY >= 0 && pixelY < processedBitmap.height) {
                        val weight = 1 - (distance / radius)
                        val newRed = (avgRed * weight + Color.red(processedBitmap.getPixel(pixelX, pixelY)) * (1 - weight)).toInt()
                        val newGreen = (avgGreen * weight + Color.green(processedBitmap.getPixel(pixelX, pixelY)) * (1 - weight)).toInt()
                        val newBlue = (avgBlue * weight + Color.blue(processedBitmap.getPixel(pixelX, pixelY)) * (1 - weight)).toInt()
                        processedBitmap.setPixel(pixelX, pixelY, Color.rgb(newRed, newGreen, newBlue))
                    }
                }
            }
        }

        return processedBitmap
    }

    private fun applyColorSaturation(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int, saturationCoefficient: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val processedBitmap = bitmap.copy(bitmap.config, true)

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val distance = sqrt((i * i + j * j).toDouble())
                if (distance <= radius) {
                    val pixelX = centerX + i
                    val pixelY = centerY + j
                    if (pixelX in 0 until width && pixelY in 0 until height) {
                        val pixel = bitmap.getPixel(pixelX, pixelY)
                        val colorRed = Color.red(pixel)
                        val colorGreen = Color.green(pixel)
                        val colorBlue = Color.blue(pixel)
                        val alpha = Color.alpha(pixel)

                        val hsl = translateRgbToHsl(colorRed, colorGreen, colorBlue)
                        hsl[1] = (hsl[1] * saturationCoefficient).coerceIn(0f, 1f)

                        val newColor = translateHslToRgb(hsl[0], hsl[1], hsl[2])
                        processedBitmap.setPixel(pixelX, pixelY, Color.argb(alpha, Color.red(newColor), Color.green(newColor), Color.blue(newColor)))
                    }
                }
            }
        }

        return processedBitmap
    }

    private fun translateRgbToHsl(red: Int, green: Int, blue: Int): FloatArray {
        val rf = red / 255f
        val gf = green / 255f
        val bf = blue / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val lightnessOfColor = (max + min) / 2f
        val saturationOfColor: Float
        val hueOfColor: Float

        if (delta == 0f) {
            saturationOfColor = 0f
            hueOfColor = 0f
        } else {
            saturationOfColor = if (lightnessOfColor < 0.5f) {
                delta / (max + min)
            } else {
                delta / (2f - max - min)
            }

            hueOfColor = when (max) {
                rf -> (gf - bf) / delta + (if (gf < bf) 6 else 0)
                gf -> (bf - rf) / delta + 2
                bf -> (rf - gf) / delta + 4
                else -> 0f
            } / 6f
        }

        return floatArrayOf(hueOfColor, saturationOfColor, lightnessOfColor)
    }

    private fun translateHslToRgb(hue: Float, saturation: Float, lightness: Float): Int {
        val redColor: Float
        val greenColor: Float
        val blueColor: Float

        if (saturation == 0f) {
            redColor = lightness
            greenColor = lightness
            blueColor = lightness
        } else {
            val q = if (lightness < 0.5f) {
                lightness * (1f + saturation)
            } else {
                lightness + saturation - lightness * saturation
            }
            val p = 2f * lightness - q
            redColor = translateToRgb(p, q, hue + 1f / 3f)
            greenColor = translateToRgb(p, q, hue)
            blueColor = translateToRgb(p, q, hue - 1f / 3f)
        }

        return Color.rgb((redColor * 255).toInt(), (greenColor * 255).toInt(), (blueColor * 255).toInt())
    }

    private fun translateToRgb(p: Float, q: Float, t: Float): Float {
        var newT = t
        if (newT < 0) newT += 1f
        if (newT > 1) newT -= 1f
        return when {
            newT < 1f / 6f -> p + (q - p) * 6f * newT
            newT < 1f / 2f -> q
            newT < 2f / 3f -> p + (q - p) * (2f / 3f - newT) * 6f
            else -> p
        }
    }

    private fun setOnClickListeners() {
        val buttonBack = view?.findViewById<AppCompatImageView>(R.id.imageBack)
        val buttonApply = view?.findViewById<AppCompatImageView>(R.id.imageSave)
        buttonBack?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
        buttonApply?.setOnClickListener {
            val activity: EditImageActivity? = activity as EditImageActivity?
            processedBitmap?.let {
                activity?.setBitMap(it)
            }
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, PreviewFragment.newInstance())?.commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RetouchingFragment()
    }
}