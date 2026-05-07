package com.henryuide.pruebacoffe.view

import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.henryuide.pruebacoffe.view.onboarding.OnBoardingActivity
import com.henryuide.pruebacoffe.data.PreferencesProvider
import com.henryuide.pruebacoffe.extensions.goToActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        Handler()
            .postDelayed({
                val onBanding = PreferencesProvider.getSecondsRemaining(this)
                if (onBanding)
                    goToActivity<LiveObjectDetectionActivity>()
                else
                    goToActivity<OnBoardingActivity>()
            }, 3000)
    }
}