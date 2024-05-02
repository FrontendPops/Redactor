package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.tsu.redactorapp.adaptive.ImageAdapter
import com.tsu.redactorapp.databinding.ActivityFiltersBinding
import com.tsu.redactorapp.models.ImageItem


class EditImageActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityFiltersBinding
    lateinit var sharedBitmap : Bitmap
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        intent.getParcelableExtra(MainActivity.KEY_IMAGE_URI, Uri::class.java)?.let { imageUri ->
            val inputStream = contentResolver.openInputStream(imageUri)
            sharedBitmap = BitmapFactory.decodeStream(inputStream)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView2, PreviewFragment.newInstance()).commit()
        val imageRV = findViewById<RecyclerView>(R.id.imageRecyclerView)

        imageRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())

        val imageList = arrayListOf(
            ImageItem(R.drawable.rotate_button),
            ImageItem(R.drawable.filters_button),
            ImageItem(R.drawable.resize_button),
            ImageItem(R.drawable.retouch_button),
            ImageItem(R.drawable.rotate_button),
            ImageItem(R.drawable.rotate_button)
        )

        val imageAdapter = ImageAdapter(this)
        imageRV.adapter = imageAdapter
        imageAdapter.submitList(imageList)
    }

    fun getBitMap(): Bitmap {
        return sharedBitmap
    }

    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0f,  // fromXDelta
            0f,  // toXDelta
            0f,  // fromYDelta
            -
            view.height.toFloat()
        ) // toYDelta
        animate.setDuration(100)
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    // slide the view from its current position to below itself
    private fun slideDown(view: View) {
        view.visibility = View.INVISIBLE
        val delta = view.height.toFloat() + 70
        val animate = TranslateAnimation(
            0f,  // fromXDelta
            0f,  // toXDelta
            0f,  // fromYDelta
            delta.toFloat()
        ) // toYDelta
        animate.setDuration(100)
        animate.fillAfter = true
        view.startAnimation(animate)
    }
    override fun onItemClick(position: Int) {
        val imageRV = findViewById<RecyclerView>(R.id.imageRecyclerView)
        val backButton = findViewById<AppCompatImageView>(R.id.imageBack)
        val saveButton = findViewById<AppCompatImageView>(R.id.imageSave)
        slideDown(imageRV)
        slideUp(backButton)
        slideUp(saveButton)
        when(position)
        {
            1 -> supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView2, FilterFragment.newInstance()).commit()
        }
    }

    @Suppress("DEPRECATION")
    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressed()
        }
    }
}