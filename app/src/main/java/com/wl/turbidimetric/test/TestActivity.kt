package com.wl.turbidimetric.test

import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ActivityTestBinding
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.TestState
import com.wl.turbidimetric.test.repeatablitylity.RepeatabilityFragment

class TestActivity : BaseActivity<BaseViewModel, ActivityTestBinding>() {
    override val vd: ActivityTestBinding by ActivityDataBindingDelegate(R.layout.activity_test)
    override val vm: BaseViewModel by viewModels()


    override fun init() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "测试页面"
        supportActionBar?.show()

        val flag = intent.getStringExtra(TestActivity.flag)
        var fragment: Fragment? = null
        if (flag == flag_Repeatability) {
            fragment = RepeatabilityFragment.newInstance()
        }


        fragment?.let {
            supportFragmentManager.beginTransaction()
                .add(R.id.ll_root, it, flag_Repeatability).commitNowAllowingStateLoss()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (SystemGlobal.testState != TestState.Normal) {
                toast("正在检测，请稍后……")
                return false
            }
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        val flag_Repeatability = "repeatability"
        val flag = "flag"
    }
}
