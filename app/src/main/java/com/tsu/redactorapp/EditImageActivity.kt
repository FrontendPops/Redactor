package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.tsu.redactorapp.adaptive.ImageAdapter
import com.tsu.redactorapp.databinding.ActivityFiltersBinding
import com.tsu.redactorapp.models.ImageItem


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


}