package com.tsu.redactorapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class RecognitionFragment : Fragment() {

    private lateinit var imageView: ImageView
    private var originalBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null

    private lateinit var getImageFromGallery: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_face_recognition, container, false)

        imageView = root.findViewById(R.id.imageView2)
        val buttonRecognition: Button = root.findViewById(R.id.button2)
        val buttonLoadImage: Button = root.findViewById(R.id.button)
        val scaleFactors: EditText = root.findViewById(R.id.scaleFactorText)

        buttonLoadImage.setOnClickListener {
            openGalleryForImage()
        }

        buttonRecognition.setOnClickListener {
            detectFaces()
        }

        registerGetImageFromGallery()

        return root
    }

    private fun registerGetImageFromGallery() {
        getImageFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedImage = result.data?.data
                imageView.setImageURI(selectedImage)
                originalBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, selectedImage)
            }
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageFromGallery.launch(intent)
    }

    private fun detectFaces() {
        if (originalBitmap == null) {
            Toast.makeText(requireContext(), "Загрузи изображение", Toast.LENGTH_SHORT).show()
            return
        }

        val mat = Mat()
        Utils.bitmapToMat(originalBitmap, mat)
        val resultMat = faceRecognition(mat, requireContext())

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)

        imageView.setImageBitmap(resultBitmap)
        saveImageToGallery(resultBitmap)
    }

    private fun faceRecognition(input: Mat, context: Context): Mat {
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
                Log.e("OpenCV", "Ошибка загрузки ${cascadeFile.absolutePath}")
                continue
            }

            val tempFaces = MatOfRect()
            faceCascade.detectMultiScale(input, tempFaces)
            faces.push_back(tempFaces)
        }

        val faceBorders = faces.toArray()
        faceBorders.forEach { rect ->
            val faceBorder = input.submat(rect)
            gammaFilter(faceBorder, 1.0, 20.0)
        }

        return input
    }

    fun gammaFilter(image: Mat, alpha: Double, beta: Double) {
        for (i in 0 until image.rows()) {
            for (j in 0 until image.cols()) {
                val pixel = image.get(i, j)
                val newPixel = DoubleArray(pixel.size)

                for (k in pixel.indices) {
                    newPixel[k] = pixel[k] * alpha + beta
                    newPixel[k] = newPixel[k].coerceIn(0.0, 255.0)
                }

                image.put(i, j, *newPixel)
            }
        }
    }


    private fun saveImageToGallery(bitmap: Bitmap) {
        val savedImageURL = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver,
            bitmap,
            "Новое изображение",
            "Сделано алгоритмом"
        )
        if (savedImageURL != null) {
            Toast.makeText(requireContext(), "Изображение сохранено в галерею", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Изображение не сохранено в галерею", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RecognitionFragment()
    }
}