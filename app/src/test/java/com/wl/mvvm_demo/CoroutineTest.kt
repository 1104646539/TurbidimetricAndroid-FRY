package com.wl.mvvm_demo

import kotlinx.coroutines.*
import org.junit.Test

class CoroutineTest {

    @Test
    fun cTest() {

    }

    private suspend fun s2() {
        println("s2 start")
//        Thread.sleep(2000)
        println("s2 end")
    }

    private suspend fun s1(): String {
        println("s1 start")
//        delay(2000)
        println("s1 end")
        return "s1"
    }

    suspend fun s3() = coroutineScope {
        val s1Re = async { s1() }
        s1Re.await()
        println("s1Re.await after")
    }

}
