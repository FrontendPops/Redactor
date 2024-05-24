package com.tsu.redactorapp

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tsu.redactorapp.databinding.ActivityFiltersBinding
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EditImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFiltersBinding
    lateinit var sharedBitmap : Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getParcelableExtra(MainActivity.KEY_IMAGE_URI, Uri::class.java)?.let { imageUri ->
            val inputStream = contentResolver.openInputStream(imageUri)
            sharedBitmap = BitmapFactory.decodeStream(inputStream)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView2, PreviewFragment.newInstance()).commit()

    }

    fun getBitMap(): Bitmap {
        return sharedBitmap
    }
    fun setBitMap(newBitmap : Bitmap) {
        sharedBitmap = newBitmap
    }

    fun saveImageToGallery(view: View) {
        val context = view.context
        val filename = createUniqueFileName("my_image", "png")
        val relativeLocation = Environment.DIRECTORY_PICTURES

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativeLocation)
        }

        val resolver = context.contentResolver
        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            try {
                resolver.openOutputStream(imageUri).use { outputStream ->
                    if (outputStream != null) {
                        sharedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        Snackbar.make(view, "Image saved", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(view, "Failed to save image", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Snackbar.make(view, "Failed to save image", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(view, "Failed to create file", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun createUniqueFileName(baseName: String, extension: String): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "$baseName$timeStamp.$extension"
    }
}