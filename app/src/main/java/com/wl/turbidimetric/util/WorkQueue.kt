package com.wl.turbidimetric.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class WorkQueue<T>(var intervalTime: Long = 30000L, var scope: CoroutineScope) {
    val queue: BlockingQueue<T> = LinkedBlockingQueue()
    var onWorkStart: ((t: T) -> Unit)? = null

    /**
     * allowNext == true 可以继续消费下一个
     * allowNext == false 不可以继续消费下一个，等待
     */
    private var allowNext = true

    init {
        startWork()
    }


    fun working() {
        allowNext = false
    }

    fun finishedWork() {
        allowNext = true
    }

    private fun startWork() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                if (allowNext) {
                    val t = queue.take()
                    onWorkStart?.invoke(t)
                }
                Thread.sleep(intervalTime)
            }
        }
    }

    fun addWork(work: T) {
        queue.add(work)
    }

    fun clear() {
        queue.clear()
    }

    fun dispose() {
        scope.cancel()
    }
}
