package com.tsu.redactorapp

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Scalar
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream

class RecognitionFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var root: View
    private var originalBitmap: Bitmap? = null
    private var exportBitmap: Bitmap? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_face_recognition, container, false)
        val activity: EditImageActivity? = activity as EditImageActivity?
        originalBitmap = activity?.getBitMap()!!
        exportBitmap = originalBitmap
        imageView = root.findViewById(R.id.imageViewPreview)
        imageView.setImageBitmap(originalBitmap)
        val buttonRecognition: Button = root.findViewById(R.id.recognizeButton)
        setListeners()

        buttonRecognition.setOnClickListener {
            GlobalScope.launch {
                detectFaces()
            }
        }
        return root
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

     private suspend fun detectFaces() {
        if (originalBitmap == null) {
            return
        }

        coroutineScope.coroutineContext.cancelChildren()

        coroutineScope.launch {
            val mat = Mat()
            Utils.bitmapToMat(originalBitmap, mat)
            val resultMat = faceRecognition(mat, requireContext())

            val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(resultMat, resultBitmap)
            exportBitmap = resultBitmap
            imageView.setImageBitmap(resultBitmap)

            Snackbar.make(root , "Recognized", Snackbar.LENGTH_SHORT).show()
        }
    }

    private suspend fun faceRecognition(input: Mat, context: Context): Mat = withContext(Dispatchers.IO) {
        val cascadeFilesOpenCv = listOf(
            "haarcascade_frontalface_alt2.xml",
            "haarcascade_frontalface_alt.xml",
            "haarcascade_frontalface_alt_tree.xml",
        )

        val cascadeFiles = cascadeFilesOpenCv.map { fileName ->
            val cascadeFileDir = context.getExternalFilesDir(null)
            val cascadeFile = File(cascadeFileDir, fileName)

            if (!cascadeFile.exists()) {
                context.resources.openRawResource(context.resources.getIdentifier(fileName.removeSuffix(".xml"), "raw", context.packageName)).use { inputStream ->
                    FileOutputStream(cascadeFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            cascadeFile
        }

        val faces = MatOfRect()
        for (cascadeFile in cascadeFiles) {
            val faceCascade = CascadeClassifier(cascadeFile.absolutePath)
            if (faceCascade.empty()) {
                Log.e("OpenCV", "Failed to load ${cascadeFile.absolutePath}")
                continue
            }

            val tempFaces = MatOfRect()
            faceCascade.detectMultiScale(input, tempFaces)
            faces.push_back(tempFaces)
        }

        val faceBorders = faces.toArray()
        val mask = Mat.zeros(input.size(), input.type())

        faceBorders.forEach { rect ->
            val faceRegion = mask.submat(rect)
            faceRegion.setTo(Scalar(255.0, 255.0, 255.0))
        }

        val filteredImage = input.clone()
        gammaFilter(filteredImage, mask, 1.0, 20.0)

        filteredImage.copyTo(input, mask)

        return@withContext input
    }

    fun gammaFilter(image: Mat, mask: Mat, alpha: Double, beta: Double) {
        for (i in 0 until image.rows()) {
            for (j in 0 until image.cols()) {
                if (mask.get(i, j)[0] > 0) {
                    val pixel = image.get(i, j)
                    val newPixel = DoubleArray(pixel.size)

                    for (k in pixel.indices) {
                        newPixel[k] = (pixel[k] * alpha + beta).coerceIn(0.0, 255.0)
                    }

                    newPixel[0] = (newPixel[0] * 1.2).coerceIn(0.0, 255.0)
                    newPixel[1] = (newPixel[1] * 0.8).coerceIn(0.0, 255.0)
                    newPixel[2] = (newPixel[2] * 0.8).coerceIn(0.0, 255.0)

                    image.put(i, j, *newPixel)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setListeners() {
        val imageBack = root.findViewById<AppCompatImageView>(R.id.imageBackRecognition)
        val imageSave = root.findViewById<AppCompatImageView>(R.id.imageSaveRecognition)
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
        fun newInstance() = RecognitionFragment()
    }
}