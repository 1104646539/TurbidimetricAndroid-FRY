package com.wl.weiqianwllib.upan

enum class StorageState(val stateName: String) {
    NONE("U盘不存在"), INSERTED("U盘已插入,请等待挂载"), EXIST("U盘可使用"), UNAUTHORIZED("U盘未授权");

    /**
     * U盘已可使用
     * @return Boolean
     */
    fun isExist(): Boolean {
        return this == EXIST
    }
}
