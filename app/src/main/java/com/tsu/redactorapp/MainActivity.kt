package com.tsu.redactorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.tsu.redactorapp.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {

    companion object {
        const val KEY_IMAGE_URI = "imageUri"
    }

    private  lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { imageUri ->
                Intent(applicationContext, EditImageActivity::class.java).also { editImageIntent ->
                    editImageIntent.putExtra(KEY_IMAGE_URI, imageUri)
                    startActivity(editImageIntent)
                }
            }
        }
    }

    private fun setListeners() {
        binding.buttonFilters.setOnClickListener {
            val pickerIntent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(pickerIntent)
        }
        binding.buttonSplines.setOnClickListener {
            val intent = Intent(this, SplineActivity::class.java)
            startActivity(intent)
        }
    }
}
