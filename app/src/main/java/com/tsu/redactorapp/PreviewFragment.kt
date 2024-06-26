package com.tsu.redactorapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.MaskableFrameLayout
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.tsu.redactorapp.adaptive.ImageAdapter
import com.tsu.redactorapp.models.ImageItem

class PreviewFragment : Fragment(), OnItemClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)

    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FilterFragment()
        setListeners()
        view.findViewById<AppCompatImageView>(R.id.imageBack)
        val activity: EditImageActivity? = activity as EditImageActivity?
        val image = activity?.getBitMap()
        val imagePreview = view.findViewById<AppCompatImageView>(R.id.imagePreview)
        imagePreview.setImageBitmap(image)
        imagePreview.visibility = View.VISIBLE
        val imageRV = view.findViewById<RecyclerView>(R.id.imageRecyclerViewPreview)

        imageRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())

        val imageList = arrayListOf(
            ImageItem(R.drawable.rotate_button),
            ImageItem(R.drawable.filters_button),
            ImageItem(R.drawable.resize_button),
            ImageItem(R.drawable.retouch_button),
            ImageItem(R.drawable.face_button),
            ImageItem(R.drawable.mask_button)
        )

        val imageAdapter = ImageAdapter(this)
        imageRV.adapter = imageAdapter
        imageAdapter.submitList(imageList)

    }


    @SuppressLint("WrongViewCast")
    override fun onItemClick(position: Int) {
        val imageAdapter = ImageAdapter(this)
        val imageRV = view?.findViewById<RecyclerView>(R.id.imageRecyclerViewPreview)
        val currentView =
            imageRV?.findViewHolderForAdapterPosition(position)?.itemView?.findViewById<MaskableFrameLayout>(
                R.id.itemContainer
            )
        currentView?.transitionName = "shared_element_two"
        when (position) {
            0 -> currentView?.let {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView2, RotationFragment.newInstance())?.commit()
            }

            1 -> currentView?.let {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView2, FilterFragment.newInstance())?.commit()
            }

            2 -> currentView?.let {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView2, ScaleFragment.newInstance())?.commit()
            }

            3 -> currentView?.let {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView2, RetouchingFragment.newInstance())?.commit()
            }

            4 -> currentView?.let {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView2, RecognitionFragment.newInstance())
                    ?.commit()
            }

            5 -> currentView?.let {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView2, UnsharpMaskFragment.newInstance())
                    ?.commit()
            }

        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PreviewFragment()
    }

    @Suppress("DEPRECATION")
    private fun setListeners() {
        val imageBack = view?.findViewById<AppCompatImageView>(R.id.imageBack)
        val imageSave = view?.findViewById<AppCompatImageView>(R.id.imageSave)
        imageBack?.setOnClickListener {
            activity?.onBackPressed()
        }
        imageSave?.setOnClickListener {
            val activity: EditImageActivity? = activity as EditImageActivity?
            activity!!.saveImageToGallery(activity.findViewById(android.R.id.content))
        }
    }
}
