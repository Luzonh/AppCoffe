package com.henryuide.pruebacoffe.view.onboarding

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.henryuide.pruebacoffe.R
import com.henryuide.pruebacoffe.data.PreferencesProvider
import com.henryuide.pruebacoffe.databinding.ActivityOnBoardingBinding
import com.henryuide.pruebacoffe.extensions.goToActivity
import com.henryuide.pruebacoffe.view.LiveObjectDetectionActivity

class OnBoardingActivity : AppCompatActivity(), ViewPagerAdapter.OnItemSelected {
    private lateinit var boardList: List<Board>
    private lateinit var binding: ActivityOnBoardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        else
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

        boardList = listOf(
            Board(
                background = R.color.white,
                //image = R.raw.first,
                image = R.drawable.uidelogo,
                title = getString(R.string.onboarding_protect),
                description = getString(R.string.onboarding_tip_one)
            ),

        )

        val adapter = ViewPagerAdapter(boardList, this)
        binding.viewPager.adapter = adapter
        binding.viewPager.layoutDirection = ViewPager2.LAYOUT_DIRECTION_LTR
    }

    override fun onClickListener(position: Int) {
        if (position == (boardList.size - 1)) {
            PreferencesProvider.setSecondsRemaining(this, true)
            goToActivity<LiveObjectDetectionActivity>()
        } else {
            binding.viewPager.setCurrentItem((position + 1), true)
        }
    }


}