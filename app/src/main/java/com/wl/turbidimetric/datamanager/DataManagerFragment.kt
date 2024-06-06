package com.wl.turbidimetric.datamanager

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.app.PrinterState
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentDataManagerBinding
import com.wl.turbidimetric.ex.calcShowTestResult
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.SystemGlobal
import com.wl.turbidimetric.main.MainActivity
import com.wl.turbidimetric.model.ConditionModel
import com.wl.turbidimetric.model.TestResultAndCurveModel
import com.wl.turbidimetric.print.PrintUtil
import com.wl.turbidimetric.upload.hl7.HL7Helper
import com.wl.turbidimetric.util.ExportExcelHelper
import com.wl.turbidimetric.util.ExportReportHelper
import com.wl.turbidimetric.util.PrintHelper
import com.wl.turbidimetric.util.PrintSDKHelper
import com.wl.turbidimetric.view.dialog.*
import com.wl.wllib.LogToFile.i
import com.wl.wllib.LogToFile.u
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*


/**
 * 数据管理
 */
class DataManagerFragment :
    BaseFragment<DataManagerViewModel, FragmentDataManagerBinding>(R.layout.fragment_data_manager) {
    init {
        i("init create")
    }

    override val vm: DataManagerViewModel by viewModels {
        DataManagerViewModelFactory()
    }

    val adapter: DataManagerAdapter by lazy {
        DataManagerAdapter()
    }

    /**
     * 筛选对话框
     */
    val conditionDialog by lazy {
        ConditionDialog(requireContext())
    }

    /**
     * 删除对话框
     */
    val deleteDialog by lazy {
        HiltDialog(requireContext())
    }

    /**
     * 等待任务对话框
     */
    val waitDialog by lazy {
        HiltDialog(requireContext())
    }

    override fun init(savedInstanceState: Bundle?) {
        initView()
        listener()
//        test()
    }

    private fun initView() {
        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.itemAnimator = DefaultItemAnimator()
        val itemDividerMode = DividerItemDecoration(
            requireContext(), android.widget.LinearLayout.VERTICAL
        ).apply { setDrawable(resources.getDrawable(R.drawable.item_hori_divider)!!) }
        vd.rv.addItemDecoration(
            itemDividerMode
        )

        val loadStateAdapter = adapter.withLoadStateFooter(DataManagerLoadStateAdapter())
        vd.rv.adapter = loadStateAdapter
    }

    fun test() {
//        lifecycleScope.launch {
//            val testdatas = createTestData()
//            vm.add(testdatas)
//        }
    }

//    private fun createPrintData(): List<List<String>> {
//        return createTestData().map {
//            mutableListOf<String>().apply {
//                add("${it.concentration}")
//                add("${it.testResult}")
//                add("${it.name}")
//                add("${it.gender}")
//                add("${it.age}")
//                add("${it.detectionNum}")
//                add("${it.testOriginalValue1}")
//                add("${it.testOriginalValue2}")
//                add("${it.testOriginalValue3}")
//                add("${it.testOriginalValue4}")
//                add("${it.testValue1}")
//                add("${it.testValue2}")
//                add("${it.testValue3}")
//                add("${it.testValue4}")
//                add("${it.testTime}")
//                add("${it.absorbances}")
//            }
//        }
//    }
//
//    private fun createTestData(): List<TestResultModel> {
//        return mutableListOf<TestResultModel>().apply {
//            for (i in 0..100) {
//                val dr = TestResultModel(
//                    testResult = (i % 2 == 0).PD("阳性", "阴性"),
//                    concentration = 66 + i,
//                    absorbances = "121120".toBigDecimal(),
//                    name = (i % 2 == 0).PD("张三", "李四"),
//                    gender = (i % 2 == 0).PD("男", "女"),
//                    age = (i % 90).toString(),
//                    detectionNum = LocalData.getDetectionNumInc(),
//                    testOriginalValue1 = 52111,
//                    testOriginalValue2 = 52112,
//                    testOriginalValue3 = 52113,
//                    testOriginalValue4 = 52114,
//                    testValue1 = "52.31".toBigDecimal(),
//                    testValue2 = "52.32".toBigDecimal(),
//                    testValue3 = "52.33".toBigDecimal(),
//                    testValue4 = "52.34".toBigDecimal(),
//                    testTime = Date().time,
//                    deliveryTime = "",
//                    deliveryDepartment = "",
//                    deliveryDoctor = "",
//                    sampleBarcode = "ABCD$i",
//                    curveOwnerId = 0
//                )
//                add(dr)
//            }
//        }
//    }


    private fun listener() {
        listenerView()
        listenerData()
    }

    private fun listenerView() {

        vd.btnCondition.setOnClickListener {
            u("数据筛选")
            showConditionDialog()
        }

        vd.btnExportExcel.setOnClickListener {
            u("导出Excel")
            exportExcelAll()
        }
        vd.btnUpload.setOnClickListener {
            upload()
        }
        lifecycleScope.launch {
            vm.resultSize.collectLatest {
                vd.tvCount.text = "(${it}条)"
            }
        }

        adapter.onLongClick = { id ->
            u("详情$id")
            if (id > 0) {
                lifecycleScope.launch {
                    vm.showDetails(id)
                }
            } else {
                toast("ID错误")
            }
        }

        vd.btnPrint.setOnClickListener {
            u("打印")
            print()
        }
        vd.btnDelete.setOnClickListener {
            u("删除")
            delete()
        }
        vd.btnExportPdf.setOnClickListener {
            u("导出报告")
            exportReport()
        }
        vd.btnPrintPdf.setOnClickListener {
            u("打印报告")
            printReport()
        }
        adapter.onSelectChange = { pos, selected ->

        }
        lifecycleScope.launch {
            vm.dialogUiState.collectLatest { state ->
                when (state) {
                    DataManagerUiState.None -> {
                    }

                    is DataManagerUiState.ResultDetailsDialog -> {
                        resultDialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog(state.model, SystemGlobal.isDebugMode) { result ->
                                lifecycleScope.launch {
                                    if (SystemGlobal.isDebugMode) {
                                        val newResult = calcShowTestResult(
                                            result.result.concentration,
                                            result.curve?.projectLjz ?: 0
                                        )
                                        result.result.testResult = newResult
                                    }
                                    val ret = vm.update(result)
                                    toast("更新${if (ret > 0) "成功" else "失败"}")
                                }
                                true
                            }
                        }
                    }

                    is DataManagerUiState.DeleteDialog -> {
                        deleteDialog.showPop(requireContext(), isCancelable = false) {
                            it.showDialog("确定要删除数据吗?", "确定", {
                                val results = getSelectData()
                                it.dismiss()
                                if (!results.isNullOrEmpty()) {
                                    lifecycleScope.launch {
                                        vm.clickDeleteDialogConfirm(results.map { it.result })
                                    }
                                }
                            }, "取消", {
                                it.dismiss()
                            }, showIcon = true, iconId = ICON_HINT)
                        }
                    }
                }
            }
        }
    }

    private val mCurrentPosition = FloatArray(2)

    private fun addCart() {
        val targetView = (requireActivity() as MainActivity).getTopPrint() ?: return
        //   一、创造出执行动画的主题---imageview
        //代码new一个imageview，图片资源是上面的imageview的图片
        // (这个图片就是执行动画的图片，从开始位置出发，经过一个抛物线（贝塞尔曲线），移动到购物车里)
        val goods = ImageView(requireContext())
        goods.setImageResource(R.drawable.icon_report)
        val params = RelativeLayout.LayoutParams(60, 60)
        vd.clRoot.addView(goods, params)

//    二、计算动画开始/结束点的坐标的准备工作
        //得到父布局的起始点坐标（用于辅助计算动画开始/结束时的点的坐标）
        val parentLocation = IntArray(2)
        vd.clRoot.getLocationInWindow(parentLocation)

        //得到商品图片的坐标（用于计算动画开始的坐标）
        val startLoc = IntArray(2)
        vd.btnPrintPdf.getLocationInWindow(startLoc)

        //得到购物车图片的坐标(用于计算动画结束后的坐标)
        val endLoc = IntArray(2)
        targetView.getLocationInWindow(endLoc)


//    三、正式开始计算动画开始/结束的坐标
        //开始掉落的商品的起始点：商品起始点-父布局起始点+该商品图片的一半
        val startX = (startLoc[0] - parentLocation[0] + vd.btnPrintPdf.width / 2).toFloat()
        val startY = (startLoc[1] - parentLocation[1] + vd.btnPrintPdf.height / 2).toFloat()

        //商品掉落后的终点坐标：购物车起始点-父布局起始点+购物车图片的1/5
        val toX: Float = (endLoc[0] - parentLocation[0] + targetView.width / 5).toFloat()
        val toY = (endLoc[1] - parentLocation[1]).toFloat()

//    四、计算中间动画的插值坐标（贝塞尔曲线）（其实就是用贝塞尔曲线来完成起终点的过程）
        //开始绘制贝塞尔曲线
        val path = Path()
        //移动到起始点（贝塞尔曲线的起点）
        path.moveTo(startX, startY)
        //使用二次萨贝尔曲线：注意第一个起始坐标越大，贝塞尔曲线的横向距离就会越大，一般按照下面的式子取即可
        path.quadTo((startX + toX) / 2, startY, toX, toY)
        //mPathMeasure用来计算贝塞尔曲线的曲线长度和贝塞尔曲线中间插值的坐标，
        // 如果是true，path会形成一个闭环
        val mPathMeasure = PathMeasure(path, false)

        //★★★属性动画实现（从0到贝塞尔曲线的长度之间进行插值计算，获取中间过程的距离值）
        val valueAnimator = ValueAnimator.ofFloat(0f, mPathMeasure.getLength())
        valueAnimator.setDuration(600)
        // 插值器
        valueAnimator.interpolator = android.view.animation.AccelerateInterpolator()
        valueAnimator.addUpdateListener { animation -> // 当插值计算进行时，获取中间的每个值，
            // 这里这个值是中间过程中的曲线长度（下面根据这个值来得出中间点的坐标值）
            val value = animation.animatedValue as Float
            // ★★★★★获取当前点坐标封装到mCurrentPosition
            // boolean getPosTan(float distance, float[] pos, float[] tan) ：
            // 传入一个距离distance(0<=distance<=getLength())，然后会计算当前距
            // 离的坐标点和切线，pos会自动填充上坐标，这个方法很重要。
            mPathMeasure.getPosTan(value, mCurrentPosition, null) //mCurrentPosition此时就是中间距离点的坐标值
            // 移动的商品图片（动画图片）的坐标设置为该中间点的坐标
            goods.translationX = mCurrentPosition[0]
            goods.translationY = mCurrentPosition[1]
        }
        //   五、 开始执行动画
        valueAnimator.start()

//   六、动画结束后的处理
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            //当动画结束后：
            override fun onAnimationEnd(animation: Animator) {
                // 购物车的数量加1
//                i++
//                count.setText(java.lang.String.valueOf(i))
                // 把移动的图片imageview从父布局里移除
                vd.clRoot.removeView(goods)
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun printReport() {
        val data = getSelectData()
        if (data.isNullOrEmpty()) {
            waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
                dialog.showDialog("未选择数据", "确定", { dialog -> dialog.dismiss() })
            }
            return
        }
        waitDialog.showPop(requireContext(), isCancelable = true) { dialog ->
            if (PrintSDKHelper.printerState == PrinterState.Success) {
                ((requireActivity()) as MainActivity).addPrintWorkAnim(vd.btnPrintPdf) {
                    PrintHelper.addPrintWork(data, "xxx医院", false)
                }
            } else if (PrintSDKHelper.printerState == PrinterState.None || PrintSDKHelper.printerState == PrinterState.InitSdkFailed) {
                dialog.showDialog("打印未初始化")
            } else if (PrintSDKHelper.printerState == PrinterState.NotInstallApk) {
                dialog.showDialog("打印程序未安装")
            } else if (PrintSDKHelper.printerState == PrinterState.NotPrinter) {
                dialog.showDialog("未设置打印机，请先设置打印机", "选择打印机", {
                    PrintSDKHelper.showSetupPrinterUi()
                    it.dismiss()
                }, "取消", { it.dismiss() })
            }
        }


//        waitDialog.showPop(requireContext(), isCancelable = true) { dialog ->
//            if (PrintSDKHelper.printerState == PrinterState.Success) {
//                PrintHelper.addPrintWork(data, "xxx医院", false)
//                dialog.showDialog("已加入打印队列，请等待打印")
//            } else if (PrintSDKHelper.printerState == PrinterState.None || PrintSDKHelper.printerState == PrinterState.InitSdkFailed) {
//                dialog.showDialog("打印未初始化")
//            } else if (PrintSDKHelper.printerState == PrinterState.NotInstallApk) {
//                dialog.showDialog("打印程序未安装")
//            } else if (PrintSDKHelper.printerState == PrinterState.NotPrinter) {
//                dialog.showDialog("未设置打印机，请先设置打印机", "选择打印机", {
//                    PrintSDKHelper.showSetupPrinterUi()
//                    it.dismiss()
//                }, "取消", { it.dismiss() })
//            }
//        }
    }

    private fun exportReport() {
        val data = getSelectData()
        waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
            dialog.showDialog("正在导出数据，请等待……")

            ExportReportHelper.exportReport(
                requireContext(),
                data,
                "XX人民医院",
                lifecycleScope,
                false,
                { count, successCount, failedCount ->
                    i("导出报告完成，本次导出总数${count}条,成功${successCount}条,失败${failedCount}条")
                    dialog.showDialog(
                        "导出报告完成，本次导出总数${count}条,成功${successCount}条,失败${failedCount}条",
                        "确定",
                        {
                            it.dismiss()
                        })
                }, { err ->
                    i("导出报告失败 $err")
                    dialog.showDialog("导出报告失败,$err", "确定", {
                        it.dismiss()
                    })
                }
            )
        }
    }

    private fun delete() {
        vm.deleteResult()
    }


    /**
     * 上传
     */
    private fun upload() {
        val results = getSelectData()
        var verifyRet: String? = null
        //验证上传数据
        if (verifyUploadData(results).also { verifyRet = it } != null) {
//            toast("$verifyRet")
            waitDialog.showPop(requireContext()) { dialog ->
                dialog.showDialog("$verifyRet", "确定", {
                    it.dismiss()
                })
            }
            return
        }
        //批量上传
        waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
            dialog.showDialog("请等待……", confirmText = "", confirmClick = {})

            lifecycleScope.launch(Dispatchers.IO) {
                if (HL7Helper.isConnected()) {
                    HL7Helper.uploadTestResult(results) { count, success, failed ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            dialog.showDialog(
                                "上传结束，本次上传共${count}条，成功${success}条,失败${failed}条",
                                confirmText = "我知道了",
                                confirmClick = { d ->
                                    d.dismiss()
                                })
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
//                        dialog.dismiss()
                        dialog.showDialog(
                            "上传未连接",
                            confirmText = "确定",
                            confirmClick = { dialog -> dialog.dismiss() })
                    }
                    i("上传未连接")
                }
            }
        }
    }

    /**
     * 验证上传的数据
     * @param results List<TestResultModel>?
     * @return String?
     */
    private fun verifyUploadData(results: List<TestResultAndCurveModel>?): String? {

        if (results.isNullOrEmpty()) {
            return "请选择数据"
        }

        if (appVm.testState.isTestRunning() && SystemGlobal.uploadConfig.autoUpload) {
            return "请等待检测结束后上传"
        }

        if (results.any {
                it.result.curveOwnerId <= 0
            }) {
            return "没有检测项目"
        }

        if (results.any {
                it.result.testResult.isNullOrEmpty()
            }) {
            return "未检测的数据"
        }
        return null
    }

    private fun listenerData() {

        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadState ->
                if (loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && adapter.itemCount < 1) {
                    vd.rv.isVisible = false
                    vd.empty.isVisible = true
                } else {
                    vd.rv.isVisible = true
                    vd.empty.isVisible = false
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            vm.conditionModel.collectLatest {
                queryData(it)
            }
        }
        lifecycleScope.launch {
            SystemGlobal.obDebugMode.collectLatest {
                vd.btnDelete.visibility = it.isShow()
            }
        }
    }

    private fun print() {
        val results = getSelectData()
        if (results.isNullOrEmpty()) {
//            toast("请选择数据")
            waitDialog.showPop(requireContext()) { dialog ->
                dialog.showDialog("请选择数据", "确定", {
                    it.dismiss()
                })
            }
            return
        }

        PrintUtil.printTest(results)
    }

    private fun exportExcelSelected() {
        exportExcel(false)
    }


    private fun exportExcelAll() {
        exportExcel(true)
    }

    /**
     * 导出数据到U盘 （excel格式.xls）
     * @param exportAll Boolean
     * @param dialog HiltDialog
     * @param items List<TestResultModel>?
     */
    private fun exportExcel(exportAll: Boolean) {
        waitDialog.showPop(requireContext(), isCancelable = false) { dialog ->
            //step1、 显示等待对话框
            dialog.showDialog("正在导出,请等待……", confirmText = "", confirmClick = {})
            lifecycleScope.launch(Dispatchers.IO) {
                //step2、 获取数据
                val data = if (exportAll) {
                    val condition = vm.conditionModel.value
                    vm.getFilterAll(condition)
                } else {
                    getSelectData()
                }
                //step3、 导出 等待结果
                val err = exportExcelVerify(data)
                if (err.isEmpty()) {
                    ExportExcelHelper.export(
                        requireContext(),
                        data,
                        { msg ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                dialog.showDialog("导出成功,文件保存在 $msg", "确定", { d ->
                                    d.dismiss()
                                })
                            }
                        },
                        { it ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                dialog.showDialog("导出失败,$it", "确定", { d ->
                                    d.dismiss()
                                })
                            }
                        })
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.showDialog("导出失败,$err", "确定", { d ->
                            d.dismiss()
                        })
                    }
                }
            }
        }
    }

    /**
     * 验证
     */
    private fun exportExcelVerify(items: List<TestResultAndCurveModel>?): String {
        return if (items.isNullOrEmpty()) {
            "请选择数据"
        } else if (items.size > 4000) {
            "一次操作不能大于4000条，当前操作了${items.size}条"
        } else {
            ""
        }
    }


    private fun getSelectData(): List<TestResultAndCurveModel> {
        return adapter.getSelectedItems()
    }

    /**
     * 显示筛选对话框
     */
    private fun showConditionDialog() {
        conditionDialog.showPop(requireContext(), isCancelable = false) {
            it.showDialog({ conditionModel ->
                lifecycleScope.launch {
                    vm.conditionChange(conditionModel)
                    it.dismiss()
                }
                i("conditionModel=$conditionModel")
            }, {
                it.dismiss()
            })
        }
    }

    var datasJob: Job? = null
    private suspend fun queryData(condition: ConditionModel) {
        datasJob?.cancelAndJoin()
        datasJob = lifecycleScope.launch {
            vm.item(condition).collectLatest {
                i("---监听到了变化---condition=$condition")
                vm.conditionChange(condition)
                adapter.submitData(lifecycle, it)
                //刷新后移动到顶部，只在没有移动的时候移动
                withContext(Dispatchers.Main) {
                    if (vd.rv.scrollState == RecyclerView.SCROLL_STATE_IDLE)
                        vd.rv.scrollToPosition(0)
                }
            }
        }
    }


    private val resultDialog by lazy {
        ResultDetailsDialog(requireContext())
    }

    val TAG = "DataManagerFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        Log.d(TAG, "hidden=$hidden｝")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }


    companion object {
        @JvmStatic
        fun newInstance() = DataManagerFragment()
    }

    override fun initViewModel() {
    }


}
