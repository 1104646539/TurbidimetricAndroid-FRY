package com.wl.turbidimetric.upload.view

import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseActivity
import com.wl.turbidimetric.databinding.ActivityUploadSettingsBinding
import com.wl.turbidimetric.ex.selectionLast
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.model.TestResultModel
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.upload.hl7.util.ConnectResult
import com.wl.turbidimetric.upload.hl7.util.ConnectStatus
import com.wl.turbidimetric.upload.hl7.util.save
import com.wl.turbidimetric.upload.model.GetPatientCondition
import com.wl.turbidimetric.upload.model.GetPatientType
import com.wl.turbidimetric.upload.model.Patient
import com.wl.turbidimetric.upload.service.OnConnectListener
import com.wl.turbidimetric.upload.service.OnGetPatientCallback
import com.wl.turbidimetric.upload.service.OnUploadCallback
import com.wl.turbidimetric.util.ActivityDataBindingDelegate
import com.wl.turbidimetric.view.dialog.EditUploadResultInfoDialog
import com.wl.turbidimetric.view.dialog.GetTestPatientInfoDialog
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.PatientInfoDialog
import com.wl.turbidimetric.view.dialog.isShow
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.wllib.DateUtil
import com.wl.wllib.LogToFile.i
import com.wl.wllib.isIP
import com.wl.wllib.longStrToLong
import com.wl.wllib.toLong
import com.wl.wllib.toTimeStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal
import java.util.Date

/**
 * 上传设置
 * @property vd ActivityUploadSettingsBinding
 * @property vm BaseViewModel
 */
class UploadSettingsActivity :
    BaseActivity<UploadSettingsViewModel, ActivityUploadSettingsBinding>() {
    override val vd: ActivityUploadSettingsBinding by ActivityDataBindingDelegate(R.layout.activity_upload_settings)
    override val vm: UploadSettingsViewModel by viewModels { UploadSettingsViewModelFactory() }

    /**
     * 获取待检信息
     */
    private val getTestPatientInfoDialog: GetTestPatientInfoDialog by lazy {
        GetTestPatientInfoDialog(this)
    }
    private val waitDialog: HiltDialog by lazy {
        HiltDialog(this)
    }
    private val hiltDialog: HiltDialog by lazy {
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
        vd.nav.setOnBack { finishAfterTransition() }
        vd.nav.setTitle("连接参数设置")
        vd.nav.setRight1("保存配置") {
            saveConfig()
        }
        openUploadChange(vm.openUpload.value)
        vd.swOpenUpload.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.openUpload.value = isChecked
            openUploadChange(isChecked)
        }
        vd.swAutoUpload.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.autoUpload.value = isChecked
        }
        vd.swAutoReconnection.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.isReconnection.value = isChecked
        }
        vd.swTwoway.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.twoway.value = isChecked
            vd.llTimeout.visibility = isChecked.isShow()
        }
        vd.swGetPatient.setOnCheckedChangeListener { buttonView, isChecked ->
            vm.getPatient.value = isChecked

        }
//        vd.rbRealTime.setOnCheckedChangeListener { buttonView, isChecked ->
//            vm.realTimeGetPatient.value = isChecked
//        }

        vd.etRetryCount.addTextChangedListener {
            vm.retryCount.value = it.toString()
        }
        vd.etTimeoutTime.addTextChangedListener {
            vm.timeout.value = it.toString()
        }
        vd.etUploadInterval.addTextChangedListener {
            vm.uploadInterval.value = it.toString()
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

    override fun onDestroy() {
        super.onDestroy()
        HL7Helper.hl7Log = null
    }

    private fun saveConfig() {
        vm.verifySave().let { ret ->
            if (ret.isNotEmpty()) {
                toast("保存失败,$ret")
                return
            }
        }

        save()
        toast("保存成功")
//        finish()
    }


    private fun save() {
        vm.generateConfig().let { config ->
            config.save()
            SystemGlobal.uploadConfig = config
            if (!config.openUpload) {
                HL7Helper.disconnect()
            }
            EventBus.getDefault().post(EventMsg<String>(EventGlobal.WHAT_UPLOAD_CHANGE))
        }
    }

    private fun openUploadChange(checked: Boolean) {
        vd.swAutoUpload.isEnabled = checked
        vd.swAutoReconnection.isEnabled = checked
        vd.swTwoway.isEnabled = checked
        vd.etRetryCount.isEnabled = checked
        vd.etTimeoutTime.isEnabled = checked
        vd.etSocketIp.isEnabled = checked
        vd.etSocketPort.isEnabled = checked
        vd.etUploadInterval.isEnabled = checked
        vd.swGetPatient.isEnabled = checked
        vd.rbBc.isEnabled = checked
        vd.rbSn.isEnabled = checked
        vd.rbSerial.isEnabled = checked
        vd.btnConnect.isEnabled = checked
        vd.btnSendResult.isEnabled = checked
        vd.btnGetTestPatientInfo.isEnabled = checked

        if (!checked) {
            HL7Helper.disconnect()
        }
    }

    private fun showGetTestPatientInfo() {
        if (!HL7Helper.isConnected()) {
            toast("未连接")
            return
        }
        getTestPatientInfoDialog.showPop(
            this,
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
                                hiltDialog.showPop(
                                    this@UploadSettingsActivity,
                                    isCancelable = true
                                ) {
                                    hiltDialog.showDialog(
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
                            hiltDialog.showPop(this@UploadSettingsActivity, isCancelable = false) {
                                hiltDialog.showDialog(
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

    private val editUploadResultInfoDialog by lazy { EditUploadResultInfoDialog(this) }

    /**
     * 上传结果
     */
    private fun sendResult() {
        if (!HL7Helper.isConnected()) {
            toast("未连接")
            return
        }
        lifecycleScope.launch {
            vm.getProjects().let {
                withContext(Dispatchers.Main) {
                    editUploadResultInfoDialog.showPop(this@UploadSettingsActivity) { dialog ->
                        dialog.showDialog(it,
                            { name: String, age: String, detectionNum: String, barcode: String, abs: String, con: String, deliveryDoctor: String, deliveryDepartment: String, deliveryTime: String, testTime: String, project: ProjectModel, sex: String, result: String, BasePopupView ->
                                HL7Helper.uploadTestResult(
                                    TestResultAndCurveModel(result = TestResultModel(
                                        resultId = 0L,
                                        testResult = result,
                                        concentration = con.toIntOrNull() ?: 0,
                                        absorbances = abs.toBigDecimalOrNull() ?: BigDecimal(0),
                                        name = name,
                                        gender = sex,
                                        age = age,
                                        detectionNum = detectionNum,
                                        sampleBarcode = barcode,
                                        testTime = testTime.toLong(DateUtil.Time1Format),
                                        deliveryTime = deliveryTime.toLong(DateUtil.Time1Format)
                                            .toTimeStr(DateUtil.Time5Format),
                                        deliveryDepartment = deliveryDepartment,
                                        deliveryDoctor = deliveryDoctor,
                                    ), curve = CurveModel().apply {
                                        projectName = project.projectName
                                        projectLjz = project.projectLjz
                                        projectCode = project.projectCode
                                        projectUnit = project.projectUnit
                                    }), object : OnUploadCallback {
                                        override fun onUploadSuccess(msg: String) {
                                            i("onUploadSuccess $msg")
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                hiltDialog.showPop(this@UploadSettingsActivity) { hiltDialog ->
                                                    hiltDialog.showDialog(
                                                        "$msg",
                                                        confirmText = "确定",
                                                        confirmClick = {
                                                            it.dismiss()
                                                        })
                                                }
                                            }
                                        }

                                        override fun onUploadFailed(code: Int, msg: String) {
                                            i("onUploadFailed $code $msg")
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                hiltDialog.showPop(this@UploadSettingsActivity) { hiltDialog ->
                                                    hiltDialog.showDialog(
                                                        "$msg code=$code",
                                                        confirmText = "确定",
                                                        confirmClick = {
                                                            it.dismiss()
                                                        })
                                                }
                                            }
                                        }
                                    })
                                return@showDialog
                            }, {
                                it.dismiss()
                            }
                        )
                    }

                }
            }
        }

//        HL7Helper.uploadTestResult(
//            TestResultAndCurveModel(result = TestResultModel(
//                resultId = 0L,
//                testResult = "阴性",
//                concentration = 66,
//                absorbances = "121120".toBigDecimal(),
//                name = "张三",
//                gender = "男",
//                age = "30",
//                detectionNum = vm.getDetectionNumInc(),
//                sampleBarcode = "ABCD",
//                testTime = Date().time,
//                deliveryTime = "20220202020202",
//                deliveryDepartment = "体检科",
//                deliveryDoctor = "w医生",
//            ), curve = CurveModel().apply {
//                projectName = "便潜血"
//                projectLjz = 100
//                projectCode = "FOB"
//                projectUnit = "ul"
//            }), object : OnUploadCallback {
//                override fun onUploadSuccess(msg: String) {
//                    i("onUploadSuccess $msg")
//                }
//
//                override fun onUploadFailed(code: Int, msg: String) {
//                    i("onUploadFailed $code $msg")
//                }
//            })
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
                SystemGlobal.connectStatus = connectStatus
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
            return "重发间隔必须大于等于5000"
        }
        val uploadInterval = vm.uploadInterval.value.toIntOrNull() ?: -1
        if (uploadInterval < 100) {
            return "上传间隔必须大于等于100"
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
            vm.openUpload.collectLatest {
                vd.swOpenUpload.isChecked = it
            }
        }
        lifecycleScope.launch {
            vm.autoUpload.collectLatest {
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
            vm.uploadInterval.collectLatest {
                vd.etUploadInterval.setText(it)
                vd.etUploadInterval.selectionLast()
            }
        }
        lifecycleScope.launch {
            vm.twoway.collectLatest {
                vd.llTimeout.visibility = it.isShow()
                vd.swTwoway.isChecked = it

                vd.llGetPatient.visibility = it.isShow()
                vd.llRealTime.visibility = (it && vm.getPatient.value).isShow()
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
//                vd.llRealTime.visibility =
//                    if (it && vm.getPatient.value) View.VISIBLE else View.GONE
//                vd.rbRealTime.isChecked = it
//                vd.rbBeforeTesting.isChecked = !it
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
                vd.llRealTime.visibility = (it && vm.twoway.value).isShow()
            }
        }

    }

}


