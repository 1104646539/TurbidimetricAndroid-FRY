package com.wl.turbidimetric.model

import com.wl.turbidimetric.R

/**
 * 比色皿当前的状态
 */
//enum class CuvetteState(state: String) {
//    None("未知"),//啥也没干
//    Skip("跳过"),//跳过
//    CuvetteNotEmpty("比色皿非空"),//比色皿非空
//    DripSample("已加样"),//已经加样
//    TakeReagentFailed("取试剂失败"),//取试剂失败
//    DripReagent("已加试剂"),//已经加过试剂
//    Stir("已搅拌"),//已经搅拌
//    Test1("已检测1次"),//检测过第一次
//    Test2("已检测2次"),//检测过第二次
//    Test3("已检测3次"),//检测过第三次
//    Test4("已检测4次")//检测过第四次
//}

/**
 * 仪器检测模式
 */
enum class MachineTestModel {
    Auto,//全自动模式 。指自动识别样本是否存在，扫码等
    Manual,//手动模式。指手动输入样本数量
    ManualSampling;//手动加样模式。指手动输入样本数量
}

///**
// * 样本当前的状态
// */
//enum class SampleState {
//    None,//啥也没干
//    NONEXISTENT,//确定没有任何样本
//    Exist,//确定存在
//    ScanSuccess,//扫码成功
//    ScanFailed,//扫码失败
//    Pierced,//刺破完成
//    Squeezing,//挤压完成
//    SamplingFailed,//取样失败
//    Sampling,//取样完成
//
//}

interface ItemState {
    var state: String
    var color: Int
    var soildWidth: Int
    var soildColor: Int
}

enum class SampleState : ItemState {
    None {
        override var state: String = "未知"
        override var color: Int = R.color.shelf_none
        override var soildWidth: Int = 3
        override var soildColor: Int = R.color.shelf_solid
    },
    NONEXISTENT {
        override var state: String = "不存在"
        override var color: Int = R.color.shelf_none
        override var soildWidth: Int = 3
        override var soildColor: Int = R.color.shelf_solid
    },
    Exist {
        override var state: String = "存在"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    ScanSuccess {
        override var state: String = "扫码成功"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    ScanFailed {
        override var state: String = "扫码失败"
        override var color: Int = R.color.shelf_error
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Pierced {
        override var state: String = "已刺破"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Squeezing {
        override var state: String = "已挤压"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    SamplingFailed {
        override var state: String = "取样失败"
        override var color: Int = R.color.shelf_error
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Sampling {
        override var state: String = "取样完成"
        override var color: Int = R.color.shelf_sample_finish
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
}

enum class CuvetteState : ItemState {
    None {
        override var state: String = "未知"
        override var color: Int = R.color.shelf_none
        override var soildWidth: Int = 3
        override var soildColor: Int = R.color.shelf_solid
    },
    Skip {
        override var state: String = "跳过"
        override var color: Int = R.color.shelf_none
        override var soildWidth: Int = 3
        override var soildColor: Int = R.color.shelf_solid
    },
    CuvetteNotEmpty {
        override var state: String = "比色皿非空"
        override var color: Int = R.color.shelf_error
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    DripSample {
        override var state: String = "已加样"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    TakeReagentFailed {
        override var state: String = "取试剂失败"
        override var color: Int = R.color.shelf_error
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    DripReagent {
        override var state: String = "已加试剂"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Stir {
        override var state: String = "已搅拌"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Test1 {
        override var state: String = "检测第一次"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Test2 {
        override var state: String = "检测第二次"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Test3 {
        override var state: String = "检测第三次"
        override var color: Int = R.color.shelf_sampling
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
    Test4 {
        override var state: String = "检测完成"
        override var color: Int = R.color.shelf_test
        override var soildWidth: Int = -1
        override var soildColor: Int = 0
    },
}

data class Item(
    var state: ItemState,
    var testResult: TestResultModel? = null,
    var id: String? = null,
    var sampleType: SampleType? = null
)


/**
 * 检测状态
 * @property state Int
 * @constructor
 */
enum class TestState(val state: Int) {
    None(0),//等待开始自检
    GetMachineState(10),//自检中
    NotGetMachineState(11),//自检失败后未重新自检
    RunningError(50),//在运行时出现错误
    Normal(100),//正常的，自检成功后，等待开始
    GetState(101),//获取状态中
    DripDiluentVolume(102),//加稀释液中
    DripStandardVolume(103),//加标准品中
    MoveSample(104),//移动已混匀的
    DripSample(105),//取样加样中
    DripReagent(106),//取试剂加试剂中
    Test1(107),//检测过第一次
    Test2(108),//检测过第二次
    Test3(109),//检测过第三次。
    Test4(110),//检测过第四次
    TestFinish(111); //正在执行结束流程

    /**
     * 判断是否是在运行中
     */
    fun isTestRunning(): Boolean {
        return None != this && TestFinish != this && Normal != this
    }

    /**
     * 仪器是否可正常运行
     *
     * @return Boolean
     */
    fun machineStateNormal(): Boolean {
        return this.isNotPrepare()
    }

    /**
     * 是否正在运行
     * @return Boolean
     */
    fun isRunning(): Boolean {
        return this.state > Normal.state
    }

    /**
     * 是否未准备好
     * @return Boolean
     */
    fun isNotPrepare(): Boolean {
        return this.state < Normal.state
    }

    /**
     * 是否在运行中报错
     * @return Boolean
     */
    fun isRunningError(): Boolean {
        return this.state == RunningError.state
    }
}

/**
 * 检测类型
 */
enum class TestType {
    None,//无
    Debug,//调试
    Test,//检测
    MatchingArgs,//标曲
    Repeatability;//重复性测试

    fun isTest(): Boolean {
        return this == Test
    }

    fun isMatchingArgs(): Boolean {
        return this == MatchingArgs
    }

    fun isRepeatability(): Boolean {
        return this == Repeatability
    }

    fun isDebug(): Boolean {
        return this == Debug
    }
}

/**
 * 检测到的采便管的类型
 */
enum class SampleType(val state: String) {
    NONEXISTENT("不存在"), SAMPLE("样本管"), CUVETTE("比色杯")
}
