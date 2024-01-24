package com.wl.turbidimetric.main.splash

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.databinding.FragmentSplashBinding

class SplashFragment :
    BaseFragment<BaseViewModel, FragmentSplashBinding>(R.layout.fragment_splash) {
    override val vm: BaseViewModel by viewModels()

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {

    }

}
