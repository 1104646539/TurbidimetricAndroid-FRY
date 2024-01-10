package com.wl.turbidimetric.upload.view

import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ActivityUploadSettingsBinding
import com.wl.turbidimetric.datastore.LocalData
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.upload.service.OnUploadCallback
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.view.dialog.GetTestPatientInfoDialog
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.PatientInfoDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.LogToFile.i
import com.wl.wllib.isIP
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.view.dialog.isShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

/**
 * 上传设置
 * @property vd ActivityUploadSettingsBinding
 * @property vm BaseViewModel
 */
class UploadSettingsActivity :
    BaseActivity<UploadSettingsViewModel, ActivityUploadSettingsBinding>() {
    override val vd: ActivityUploadSettingsBinding by ActivityDataBindingDelegate(R.layout.activity_upload_settings)
    override val vm: UploadSettingsViewModel by viewModels()

    /**
     * 获取待检信息
     */
    private val getTestPatientInfoDialog: GetTestPatientInfoDialog by lazy {
        GetTestPatientInfoDialog(this)
    }
    private val waitDialog: HiltDialog by lazy {
        HiltDialog(this)
    }
    private val dialog: HiltDialog by lazy {
        HiltDialog(this)
    }

    /**
     * 待检信息列表
     */
    private val patientInfoDialog: PatientInfoDialog by lazy {
        PatientInfoDialog(this)
    }

    override fun init() {
        listener()
    }

    private fun listener() {
        listenerViewModel()
        listenerView()
    }

    private fun listenerView() {
        //监听view改变viewModel的值

        vd.swAutoUpload.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.autUpload.value = isChecked
        }
        vd.swAutoReconnection.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.isReconnection.value = isChecked
        }
        vd.swTwoway.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.twoway.value = isChecked
            vd.llTimeout.visibility = isChecked.isShow()
        }
//        vd.swGetPatient.setOnCheckedChangeListener { buttonView, isChecked ->
//            vm.getPatient.value = isChecked
//        }
        //        vd.rbRealTime.setOnCheckedChangeListener { buttonView, isChecked ->
//            vm.realTimeGetPatient.value = isChecked
//        }

        vd.etRetryCount.addTextChangedListener {
            vm.retryCount.value = it.toString()
        }
        vd.etTimeoutTime.addTextChangedListener {
            vm.timeout.value = it.toString()
        }
        vd.etSocketIp.addTextChangedListener {
            vm.ip.value = it.toString()
        }
        vd.etSocketPort.addTextChangedListener {
            vm.port.value = it.toString()
        }

        vd.rbBc.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                vm.getPatientType.value = GetPatientType.BC
            }
        }
        vd.rbSn.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                vm.getPatientType.value = GetPatientType.SN
            }
        }

        vd.rbSerial.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.serialPort.value = isChecked
        }

        vd.btnConnect.setOnClickListener {
            connect()
        }
        vd.btnSendResult.setOnClickListener {
            sendResult()
        }
        vd.llBack.setOnClickListener {
            finish()
        }
        vd.btnGetTestPatientInfo.setOnClickListener {
            showGetTestPatientInfo()
        }

        vd.btnClear.setOnClickListener {
            vd.tvLog.text = ""
        }
        HL7Helper.hl7Log = {
            vd.tvLog.post {
                vd.tvLog.append("$it\n")
                vd.svLog.fullScroll(View.FOCUS_DOWN)
                i("listenerView: $it\n")
            }
        }

    }

    private fun showGetTestPatientInfo() {
        getTestPatientInfoDialog.showPop(
            this,
            width = 600,
        ) { tpiDialog ->
            tpiDialog.show { condition1, condition2, type ->
                tpiDialog.dismiss()
                startGetTestPatientInfo(condition1, condition2, type)
            }
        }

    }

    private fun startGetTestPatientInfo(
        condition1: String,
        condition2: String,
        type: GetPatientType
    ) {
        waitDialog.showPop(this) { hilt ->
            hilt.showDialog("正在获取信息，请等待……")
            HL7Helper.getPatientInfo(
                GetPatientCondition(condition1, condition2, type),
                object : OnGetPatientCallback {
                    override fun onGetPatientSuccess(patients: List<Patient>?) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            hilt.dismiss()
                            if (patients.isNullOrEmpty()) {
                                dialog.showPop(this@UploadSettingsActivity, isCancelable = true) {
                                    dialog.showDialog(
                                        msg = "没有待检信息",
                                        confirmText = "我知道了",
                                        confirmClick = {
                                            it.dismiss()

                                        },
                                    )
                                }
                            } else {
                                patientInfoDialog.showPop(
                                    this@UploadSettingsActivity,
                                    width = 1000,
                                    isCancelable = false
                                ) { pi ->
                                    pi.showPatient(patients, {
                                        toast("点击确定")
                                        pi.dismiss()
                                    }, {
                                        toast("点击取消")
                                        pi.dismiss()
                                    })
                                }
                            }
                        }
                    }

                    override fun onGetPatientFailed(code: Int, msg: String) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            hilt.dismiss()
                            dialog.showPop(this@UploadSettingsActivity, isCancelable = false) {
                                dialog.showDialog(
                                    msg = "$code $msg",
                                    confirmText = "我知道了",
                                    confirmClick = {
                                        it.dismiss()
                                    },
                                )
                            }
                        }
                    }
                })
        }
    }

    /**
     * 上传结果
     */
    private fun sendResult() {
        HL7Helper.uploadTestResult(
            TestResultAndCurveModel(result = TestResultModel(
                resultId = 0L,
                testResult = "阴性",
                concentration = 66,
                absorbances = "121120".toBigDecimal(),
                name = "张三",
                gender = "男",
                age = "30",
                detectionNum = LocalData.getDetectionNumInc(),
                sampleBarcode = "ABCD",
                testTime = Date().time,
                deliveryTime = "20220202020202",
                deliveryDepartment = "体检科",
                deliveryDoctor = "w医生",
            ), curve = CurveModel().apply {
                projectName = "便潜血"
                projectLjz = 100
                projectCode = "FOB"
                projectUnit = "ul"
            }), object : OnUploadCallback {
                override fun onUploadSuccess(msg: String) {
                    i("onUploadSuccess $msg")
                }

                override fun onUploadFailed(code: Int, msg: String) {
                    i("onUploadFailed $code $msg")
                }
            })
    }

    private fun connect() {
        var err = ""
        if (verifyConnectConfig().also { err = it }.isNotEmpty()) {
            toast("$err")
            return
        }
        toast("上传配置已保存")
        HL7Helper.connect(vm.generateConfig(), object : OnConnectListener {
            override fun onConnectResult(connectResult: ConnectResult) {
                i("onConnectResult connectResult=$connectResult")
            }

            override fun onConnectStatusChange(connectStatus: ConnectStatus) {
                i("onConnectStatusChange connectStatus=$connectStatus")
            }
        })

    }

    private fun verifyConnectConfig(): String {
        if (vm.retryCount.value.isNullOrEmpty()) {
            return "请输入重发次数"
        }
        if (vm.timeout.value.isNullOrEmpty()) {
            return "请输入超时时间"
        }
        val retryCount = vm.retryCount.value.toIntOrNull() ?: -1
        if (retryCount < 1) {
            return "重发次数必须大于等于1"
        }
        val timeout = vm.timeout.value.toIntOrNull() ?: -1
        if (timeout < 5000) {
            return "重发次数必须大于等于5000"
        }
        //网口才需要判断
        if (!vm.serialPort.value) {
            if (vm.ip.value.isNullOrEmpty()) {
                return "请输入IP"
            }
            if (vm.port.value.isNullOrEmpty()) {
                return "请输入端口"
            }
            val ip = vm.ip.value
            if (!isIP(ip)) {
                return "IP格式不正确，格式应为x.x.x.x"
            }
            val port = vm.port.value.toIntOrNull() ?: -1
            if (port < 2000 && port < 25000) {
                return "端口号必须大于2000并且小于25000"
            }
        }

        return ""
    }

    private fun listenerViewModel() {
        //监听viewModel的值改变view
        lifecycleScope.launch {
            vm.autUpload.collectLatest {
                vd.swAutoUpload.isChecked = it
            }
        }
        lifecycleScope.launch {
            vm.ip.collectLatest {
                vd.etSocketIp.setText(it)
                vd.etSocketIp.selectionLast()
            }
        }
        lifecycleScope.launch {
            vm.twoway.collectLatest {
                vd.llTimeout.visibility = it.isShow()
                vd.swTwoway.isChecked = it
            }
        }
        lifecycleScope.launch {
            vm.port.collectLatest {
                vd.etSocketPort.setText(it)
                vd.etSocketPort.selectionLast()
            }
        }
        lifecycleScope.launch {
            vm.timeout.collectLatest {
                vd.etTimeoutTime.setText(it)
                vd.etTimeoutTime.selectionLast()
            }
        }
        lifecycleScope.launch {
            vm.retryCount.collectLatest {
                vd.etRetryCount.setText(it)
                vd.etRetryCount.selectionLast()
            }
        }
        lifecycleScope.launch {
            vm.serialPortBaudRate.collectLatest {
                if (it == "9600") {
                    vd.rbBaudRate9600.isChecked = true
                    vd.rbBaudRate115200.isChecked = false
                } else {
                    vd.rbBaudRate9600.isChecked = false
                    vd.rbBaudRate115200.isChecked = true
                }
            }
        }
        lifecycleScope.launch {
            vm.serialPort.collectLatest {
                vd.rbSerial.isChecked = it
                vd.rbSocket.isChecked = !it

                vd.llSerial.visibility = if (it) View.VISIBLE else View.GONE
                vd.llSocket.visibility = if (!it) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            vm.isReconnection.collectLatest {
                vd.swAutoReconnection.isChecked = it
            }
        }
        lifecycleScope.launch {
            vm.realTimeGetPatient.collectLatest {
                vd.llRealTime.visibility =
                    if (it && vm.getPatient.value) View.VISIBLE else View.GONE
                vd.rbRealTime.isChecked = it
                vd.rbBeforeTesting.isChecked = !it
            }
        }
        lifecycleScope.launch {
            vm.getPatientType.collectLatest {
                vd.rbBc.isChecked = it == GetPatientType.BC
                vd.rbSn.isChecked = it == GetPatientType.SN
            }
        }
        lifecycleScope.launch {
            SystemGlobal.obConnectStatus.collectLatest {
                vd.tvConnectStatus.text = it.msg
            }
        }
        lifecycleScope.launch {
            vm.getPatient.collectLatest {
                vd.swGetPatient.isChecked = it
                vd.llGetPatient1.visibility = if (it) View.VISIBLE else View.GONE
                vd.llRealTime.visibility =
                    if (it && vm.realTimeGetPatient.value) View.VISIBLE else View.GONE
            }
        }

    }

}


