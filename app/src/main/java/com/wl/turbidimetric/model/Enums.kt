package com.wl.turbidimetric.model

/**
 * 比色皿当前的状态
 */
enum class CuvetteState(state: String) {
    None("未知"),//啥也没干
    Skip("跳过"),//跳过
    DripSample("已加样"),//已经加样
    DripReagent("已加试剂"),//已经加过试剂
    Stir("已搅拌"),//已经搅拌
    Test1("已检测1次"),//检测过第一次
    Test2("已检测2次"),//检测过第二次
    Test3("已检测3次"),//检测过第三次
    Test4("已检测4次")//检测过第四次
}

/**
 * 仪器检测模式
 */
enum class MachineTestModel {
    Auto,//全自动模式 。指自动识别样本是否存在，扫码等
    Manual;//手动模式。指手动输入样本数量
}

/**
 * 样本当前的状态
 */
enum class SampleState {
    None,//啥也没干
    Exist,//确定存在
    ScanSuccess,//扫码成功
    ScanFailed,//扫码失败
    Pierced,//刺破完成
    Squeezing,//挤压完成
    Sampling,//取样完成

}

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
    Test3(109),//检测过第三次
    Test4(110),//检测过第四次
    TestFinish(111); //正在执行结束流程

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
    Test,//检测
    MatchingArgs,//标曲
    Repeatability,//重复性测试
}

