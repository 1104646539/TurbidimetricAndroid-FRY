package com.wl.turbidimetric.util

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class WorkQueue<T>(var intervalTime: Long = 30000L) {
    val queue: BlockingQueue<T> = LinkedBlockingQueue()
    var onWorkStart: ((t: T) -> Unit)? = null

    init {
        startWork()
    }

    private fun startWork() {
        thread {
            while (true) {
                val t = queue.take()
                onWorkStart?.invoke(t)
                Thread.sleep(intervalTime)
            }
        }
    }

    fun addWork(work: T) {
        queue.add(work)
    }


}
