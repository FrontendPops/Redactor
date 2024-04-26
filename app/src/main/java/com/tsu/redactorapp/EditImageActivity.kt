package com.tsu.redactorapp

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tsu.redactorapp.databinding.ActivityFiltersBinding

class EditImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFiltersBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        displayImagePreview()
    }

 //   @Suppress("DEPRECATION")
    private fun displayImagePreview () {
        intent.getParcelableExtra(MainActivity.KEY_IMAGE_URI, Uri::class.java)?.let { imageUri ->
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitMap = BitmapFactory.decodeStream(inputStream)
            binding.imagePreview.setImageBitmap(bitMap)
            binding.imagePreview.visibility = View.VISIBLE
        }
    }
    @Suppress("DEPRECATION")
    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressed()
        }
    }
}