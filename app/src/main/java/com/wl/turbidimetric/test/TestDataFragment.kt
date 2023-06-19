package com.wl.turbidimetric.test

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.FragmentTestDataBinding
import com.wl.turbidimetric.test.cmd.CmdProxy
import com.wl.turbidimetric.test.cmd.MoveSampleShelfCmd
import com.wl.wwanandroid.base.BaseFragment

class TestDataFragment :
    BaseFragment<TestDataViewModel, FragmentTestDataBinding>(R.layout.fragment_test_data) {
    override val vm: TestDataViewModel by viewModels()

    val proxy: CmdProxy by lazy {
        CmdProxy()
    }

    override fun initViewModel() {
    }

    override fun init(savedInstanceState: Bundle?) {
        vd.btnMoveSampleShelf.setOnClickListener {
            proxy.cmd = MoveSampleShelfCmd(0)
            proxy.exec()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            TestDataFragment()
    }
}
