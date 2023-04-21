package com.wl.turbidimetric.util

import android.app.Activity
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ActivityDataBindingDelegate<out VD : ViewDataBinding>(@LayoutRes val layoutId: Int) :
    ReadOnlyProperty<Activity, VD> {
    private var viewBinding: VD? = null;
    override fun getValue(thisRef: Activity, property: KProperty<*>): VD =
        viewBinding ?: DataBindingUtil.setContentView<VD>(thisRef, layoutId).also {
            viewBinding = it
        }
}
