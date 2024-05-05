package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.tsu.redactorapp.adaptive.ImageAdapter
import com.tsu.redactorapp.models.ImageItem

class PreviewFragment : Fragment(), OnItemClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)

    }
    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        val imageBack = view.findViewById<AppCompatImageView>(R.id.imageBack)
        val activity: EditImageActivity? = activity as EditImageActivity?
        val image = activity?.getBitMap()
        val imagePreview = view.findViewById<AppCompatImageView>(R.id.imagePreview)
        imagePreview.setImageBitmap(image)
        imagePreview.visibility = View.VISIBLE
        val imageRV = view.findViewById<RecyclerView>(R.id.imageRecyclerView)

        imageRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())

        val imageList = arrayListOf(
            ImageItem(R.drawable.rotate_button),
            ImageItem(R.drawable.filters_button),
            ImageItem(R.drawable.resize_button),
            ImageItem(R.drawable.retouch_button),
            ImageItem(R.drawable.face_button)
        )


        val imageAdapter = ImageAdapter(this)
        imageRV.adapter = imageAdapter
        imageAdapter.submitList(imageList)
    }


    @SuppressLint("WrongViewCast")
    override fun onItemClick(position: Int) {
        when(position)
        {
            1 -> activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView2, FilterFragment.newInstance())?.commit()
        }
    }
    companion object {
        @JvmStatic
        fun newInstance() =PreviewFragment()
    }

    @Suppress("DEPRECATION")
    private fun setListeners() {
        val imageBack = view?.findViewById<AppCompatImageView>(R.id.imageBack)
        imageBack?.setOnClickListener {
            activity?.onBackPressed()
        }
    }
}
