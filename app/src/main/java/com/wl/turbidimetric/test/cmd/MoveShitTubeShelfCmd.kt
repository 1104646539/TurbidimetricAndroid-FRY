package com.wl.turbidimetric.test.cmd

import com.wl.turbidimetric.model.*
import com.wl.turbidimetric.util.SerialPortUtil

class MoveSampleShelfCmd(private val pos: Int) : CmdIF() {
    override fun exec() {
        super.exec()
        SerialPortUtil.moveSampleShelf(pos)
    }

    override fun readDataTempModel(reply: ReplyModel<TempModel>) {


    }

    override fun readDataSqueezing(reply: ReplyModel<SqueezingModel>) {

    }
}
