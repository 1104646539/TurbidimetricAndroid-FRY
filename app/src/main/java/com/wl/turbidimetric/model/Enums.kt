package com.wl.turbidimetric.model

/**
 * 比色皿当前的状态
 */
enum class CuvetteState {
    None,//啥也没干
    Skip,//跳过
    DripSample,//已经加样
    DripReagent,//已经加过试剂
    Stir,//已经搅拌
    Test1,//检测过第一次
    Test2,//检测过第二次
    Test3,//检测过第三次
    Test4//检测过第四次
}

/**
 * 采便管当前的状态
 */
enum class ShitTubeState {
    None,//啥也没干
    Exist,//确定存在
    ScanSuccess,//扫码成功
    ScanFailed,//扫码成功
    Pierced,//刺破完成
    Sampling,//取样完成
}

/**
 * 检测状态
 */
enum class TestState {
    None,//等待开始
    DripSample,//取样加样中
    DripReagent,//取试剂加试剂中
    Test1,//检测过第一次
    Test2,//检测过第二次
    Test3,//检测过第三次
    Test4,//检测过第四次
    TestFinish //正在执行结束流程
}

/**
 * 拟合参数流程的状态
 */
enum class MatchingArgState {
    None,//等待开始
    GetState,//获取状态中
    DripDiluentVolume,//加稀释液中
    DripStandardVolume,//加标准品中
    MoveSample,//移动已混匀的
    DripReagent,//加试剂中
    Test1,//检测第一次
    Test2,//检测第二次
    Test3,//检测第三次
    Test4,//检测第四次
    Finish//拟合结束
}

