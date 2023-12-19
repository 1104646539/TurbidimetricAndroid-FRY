package com.wl.turbidimetric.view.dialog

import android.content.Context
import android.view.WindowManager
import android.widget.EditText
import com.lxj.xpopup.core.BasePopupView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.isNum
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast

/**
 * 参数设置对话框
 * @property context Context
 * @property etTakeR1 EditText
 * @property etTakeR2 EditText
 * @property etSampling EditText
 * @property etSamplingProbeCleaningTime EditText
 * @property etStirProbeCleaningTime EditText
 * @constructor
 */
class ParamsDialog(val ct: Context) : CustomBtn3Popup(ct, R.layout.dialog_params_settings) {
    /**
     * 取R1量
     */
    var etTakeR1: EditText? = null

    /**
     * 取R2量
     */
    var etTakeR2: EditText? = null

    /**
     * 取样本量
     */
    var etSampling: EditText? = null

    /**
     * 取样针清洗时长
     */
    var etSamplingProbeCleaningTime: EditText? = null

    /**
     * 搅拌针清洗时长
     */
    var etStirProbeCleaningTime: EditText? = null

    /**
     * 搅拌时长
     */
    var etStirDuration: EditText? = null

    /**
     * 第一次检测距离搅拌的间隔时长
     */
    var etTest1DelayTime: EditText? = null
    /**
     * 第二次检测距离搅拌的间隔时长
     */
    var etTest2DelayTime: EditText? = null

    /**
     * 第三次检测距离搅拌的间隔时长
     */
    var etTest3DelayTime: EditText? = null

    /**
     * 第四次检测距离搅拌的间隔时长
     */
    var etTest4DelayTime: EditText? = null


    var takeR1: Int? = null
    var takeR2: Int? = null
    var samplingVolume: Int? = null
    var samplingProbeCleaningTime: Int? = null
    var stirProbeCleaningTime: Int? = null
    var stirDuration: Int? = null
    var test1DelayTime: Long? = null
    var test2DelayTime: Long? = null
    var test3DelayTime: Long? = null
    var test4DelayTime: Long? = null
    fun showDialog(
        takeR1: Int,
        takeR2: Int,
        samplingVolume: Int,
        samplingProbeCleaningTime: Int,
        stirProbeCleaningTime: Int,
        stirDuration: Int,
        test1DelayTime: Long,
        test2DelayTime: Long,
        test3DelayTime: Long,
        test4DelayTime: Long,
        onConfirm: ((takeR1: Int, takeR2: Int, samplingVolume: Int, samplingProbeCleaningTime: Int, stirProbeCleaningTime: Int, stirDuration: Int, test1DelayTime: Long,test2DelayTime: Long, test3DelayTime: Long, test4DelayTime: Long, baseDialog: BasePopupView) -> Unit)?,
        onCancel: onClick?
    ) {
        this.takeR1 = takeR1
        this.takeR2 = takeR2
        this.samplingVolume = samplingVolume
        this.samplingProbeCleaningTime = samplingProbeCleaningTime
        this.stirProbeCleaningTime = stirProbeCleaningTime
        this.stirDuration = stirDuration
        this.test1DelayTime = test1DelayTime
        this.test2DelayTime = test2DelayTime
        this.test3DelayTime = test3DelayTime
        this.test4DelayTime = test4DelayTime
        this.confirmText = "确定"
        this.confirmClick = {
            confirm(onConfirm)
        }
        this.cancelText = "取消"
        this.cancelClick = onCancel
        super.show()

    }

    fun confirm(onConfirm: ((takeR1: Int, takeR2: Int, samplingVolume: Int, samplingProbeCleaningTime: Int, stirProbeCleaningTime: Int, stirDuration: Int, test1DelayTime: Long,test2DelayTime: Long, test3DelayTime: Long, test4DelayTime: Long, baseDialog: BasePopupView) -> Unit)?) {
        val takeR1 = etTakeR1?.text.toString()
        val takeR2 = etTakeR2?.text.toString()
        val sampling = etSampling?.text.toString()
        val samplingProbeCleaningTime = etSamplingProbeCleaningTime?.text.toString()
        val stirProbeCleaningTime = etStirProbeCleaningTime?.text.toString()
        val stirDuration = etStirDuration?.text.toString()
        val test1DelayTime = etTest1DelayTime?.text.toString()
        val test2DelayTime = etTest2DelayTime?.text.toString()
        val test3DelayTime = etTest3DelayTime?.text.toString()
        val test4DelayTime = etTest4DelayTime?.text.toString()

        if (takeR1.trim().isNullOrEmpty() || takeR2.trim().isNullOrEmpty() || sampling.trim()
                .isNullOrEmpty() || samplingProbeCleaningTime.trim()
                .isNullOrEmpty() || stirProbeCleaningTime.trim()
                .isNullOrEmpty() || stirDuration.trim()
                .isNullOrEmpty() || test1DelayTime.trim()
                .isNullOrEmpty() || test2DelayTime.trim()
                .isNullOrEmpty() || test3DelayTime.trim()
                .isNullOrEmpty() || test4DelayTime.trim()
                .isNullOrEmpty()
        ) {
            toast("请输入数字")
            return
        }
        if (!takeR1.isNum() || !takeR2.isNum() || !sampling.isNum()) {
            toast("请输入数字")
            return
        }
        onConfirm?.invoke(
            takeR1.toIntOrNull() ?: 0,
            takeR2.toIntOrNull() ?: 0,
            sampling.toIntOrNull() ?: 0,
            samplingProbeCleaningTime.toIntOrNull() ?: 0,
            stirProbeCleaningTime.toIntOrNull() ?: 0,
            stirDuration.toIntOrNull() ?: 0,
            test1DelayTime.toLongOrNull() ?: 0L,
            test2DelayTime.toLongOrNull() ?: 0L,
            test3DelayTime.toLongOrNull() ?: 0,
            test4DelayTime.toLongOrNull() ?: 0,
            this
        )
    }

    override fun initDialogView() {
        etTakeR1 = findViewById(R.id.et_take_r1)
        etTakeR2 = findViewById(R.id.et_take_r2)
        etSampling = findViewById(R.id.et_sampling)
        etSamplingProbeCleaningTime = findViewById(R.id.et_sampling_probe_cleaning_time)
        etStirProbeCleaningTime = findViewById(R.id.et_stir_probe_cleaning_time)
        etStirDuration = findViewById(R.id.et_stir_time)
        etTest1DelayTime = findViewById(R.id.et_test1_delay_time)
        etTest2DelayTime = findViewById(R.id.et_test2_delay_time)
        etTest3DelayTime = findViewById(R.id.et_test3_delay_time)
        etTest4DelayTime = findViewById(R.id.et_test4_delay_time)
    }

    override fun setContent() {
        super.setContent()
        etTakeR1?.setText(takeR1.toString())
        etTakeR2?.setText(takeR2.toString())
        etSampling?.setText(samplingVolume.toString())
        etSamplingProbeCleaningTime?.setText(samplingProbeCleaningTime.toString())
        etStirProbeCleaningTime?.setText(stirProbeCleaningTime.toString())
        etStirDuration?.setText(stirDuration.toString())
        etTest1DelayTime?.setText(test1DelayTime.toString())
        etTest2DelayTime?.setText(test2DelayTime.toString())
        etTest3DelayTime?.setText(test3DelayTime.toString())
        etTest4DelayTime?.setText(test4DelayTime.toString())

        etTakeR1?.selectionLast()
        etTakeR2?.selectionLast()
        etSampling?.selectionLast()
        etSamplingProbeCleaningTime?.selectionLast()
        etStirProbeCleaningTime?.selectionLast()
        etStirDuration?.selectionLast()
        etTest1DelayTime?.selectionLast()
        etTest2DelayTime?.selectionLast()
        etTest3DelayTime?.selectionLast()
        etTest4DelayTime?.selectionLast()

    }

    override fun getResId(): Int {
        return 0
    }

    override fun showIcon(): Boolean {
        return false
    }

}
