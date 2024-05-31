package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.wl.turbidimetric.R

class TopNavView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet) {
    private var root: View? = null
    private var ivLogo: ImageView? = null
    private var ivStateMachine: ImageView? = null
    private var ivStateUpload: ImageView? = null
    private var ivStateStorage: ImageView? = null
    private var ivStatePrinter: ImageView? = null
    private var llShutdown: View? = null
    private var tvTime: TextView? = null
    private var tvPrintNum: TextView? = null

    init {
        root = LayoutInflater.from(context).inflate(R.layout.layout_main_top_nav, this, true)
        initView()
        listenerView()

    }

    private fun listenerView() {
        ivLogo?.setOnClickListener { }
    }

    fun setTime(timeStr: String) {
        tvTime?.text = timeStr
    }

    fun setShutdownListener(onClick: OnClickListener) {
        llShutdown?.setOnClickListener(onClick)
    }

    fun setStateMachineSrc(@DrawableRes id: Int) {
        ivStateMachine?.setImageResource(id)
    }

    fun setStateUploadSrc(@DrawableRes id: Int) {
        ivStateUpload?.setImageResource(id)
    }

    fun setStateStorageSrc(@DrawableRes id: Int) {
        ivStateStorage?.setImageResource(id)
    }

    fun setStatePrinterSrc(@DrawableRes id: Int) {
        ivStatePrinter?.setImageResource(id)
    }

    fun setPrintNum(text: Int) {
        if (text <= 0) {
//            tvPrintNum?.visibility = View.GONE
            tvPrintNum?.setText("")
        } else {
            tvPrintNum?.setText("$text")
        }
    }

    fun setPrintNumVisibility(visibility: Int) {
        tvPrintNum?.visibility = visibility
    }

    fun getStatePrinter(): View? {
        return ivStatePrinter
    }
    fun getStateMachine(): View? {
        return ivStateMachine
    }

    fun getStateUpload(): View? {
        return ivStateUpload
    }

    fun getStateStorage(): View? {
        return ivStateStorage
    }

    private fun initView() {
        root?.apply {
            ivLogo = findViewById(R.id.iv_logo)
            ivStateMachine = findViewById(R.id.iv_state_machine)
            ivStateUpload = findViewById(R.id.iv_state_upload)
            ivStateStorage = findViewById(R.id.iv_state_storage)
            ivStatePrinter = findViewById(R.id.iv_state_printer)
            llShutdown = findViewById(R.id.ll_shutdown)
            tvTime = findViewById(R.id.tv_time)
            tvPrintNum = findViewById(R.id.tv_print_num)
        }
    }

    fun setMachineClick(onClick: (View) -> Unit) {
        ivStateMachine?.setOnClickListener {
            onClick.invoke(it)
        }
    }

    fun setUploadClick(onClick: (View) -> Unit) {
        ivStateUpload?.setOnClickListener {
            onClick.invoke(it)
        }
    }

    fun setStorageClick(onClick: (View) -> Unit) {
        ivStateStorage?.setOnClickListener {
            onClick.invoke(it)
        }
    }

}
