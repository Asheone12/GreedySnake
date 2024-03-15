package com.muen.greedysnake

import android.view.View
import com.muen.greedysnake.databinding.ActivityMainBinding
import com.muen.greedysnake.rxbus.event.GameOver
import com.muen.greedysnake.rxbus.rxBus
import com.muen.greedysnake.util.BaseActivity
import com.muen.greedysnake.util.WindowUtils

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onCreateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        super.initView()
        WindowUtils.setLightStatusBar(window)
    }

    override fun initListener() {
        super.initListener()
        viewBinding.btnRestart.setOnClickListener {
            val lp = window.attributes
            lp.alpha = 1.0f
            window.attributes = lp
            viewBinding.gameOver.visibility = View.GONE
            viewBinding.gameView.restartGame()
        }
    }

    override fun observerData() {
        super.observerData()
        rxBus<GameOver> {
            val lp = window.attributes
            lp.alpha = 0.7f
            window.attributes = lp
            viewBinding.gameOver.visibility = View.VISIBLE
        }
    }
}