package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.tsu.redactorapp.adaptive.ImageAdapter
import com.tsu.redactorapp.databinding.ActivityFiltersBinding
import com.tsu.redactorapp.models.ImageItem


class EditImageActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityFiltersBinding

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        displayImagePreview()
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

    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0f,  // fromXDelta
            0f,  // toXDelta
            view.height.toFloat(),  // fromYDelta
            0f
        ) // toYDelta
        animate.setDuration(500)
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    // slide the view from its current position to below itself
    private fun slideDown(view: View) {
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
        slideDown(imageRV)

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