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

        val filename = "my_image.png"
        val resolver = contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)

        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { fos ->
                    sharedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    Snackbar.make(view, "Image saved", Snackbar.LENGTH_SHORT).show()

                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}