package com.tsu.redactorapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tsu.redactorapp.databinding.ActivitySplineBinding

@Suppress("DEPRECATION")
class SplineActivity : AppCompatActivity() {
    private lateinit var activitySplineBinding: ActivitySplineBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activitySplineBinding = ActivitySplineBinding.inflate(layoutInflater)
        setContentView(activitySplineBinding.root)
        setListeners()
    }

    private fun setListeners() {
        val imageBack = activitySplineBinding.imageBackSpline
        val splineButton = activitySplineBinding.splineButton
        imageBack.setOnClickListener {
            onBackPressed()
        }
        splineButton.setOnClickListener {
            activitySplineBinding.splineCanvas.performSpline()
        }
   }
}