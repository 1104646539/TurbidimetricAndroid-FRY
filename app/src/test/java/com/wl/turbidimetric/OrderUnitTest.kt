package com.wl.turbidimetric

import com.wl.turbidimetric.model.Item
import com.wl.turbidimetric.model.ReplyState
import org.junit.Test
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class OrderUnitTest {

    val s1 by TextDelegate()
    val s2 by this::s1

    var s3 by TextDelegate2()


    class TextDelegate : ReadOnlyProperty<Any, String> {
        override fun getValue(thisRef: Any, property: KProperty<*>): String {
            return "委托只读属性$thisRef $property"
        }
    }

    class TextDelegate2 : ReadWriteProperty<Any, String> {
        override fun getValue(thisRef: Any, property: KProperty<*>): String {
            return "委托可写属性$thisRef $property"
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
            println("value=$value")
        }
    }

    @Test
    fun delegateTest() {
        println("s1=$s1")
        println("s2=$s2")
        println("s3=$s3")


    }


    fun f(index: Int): ReplyState {
        ReplyState.values().forEach {
            if (it.ordinal == index) {
                return it
            }
        }
        return ReplyState.ORDER
    }

    @Test
    fun testEnum() {
//        val it = Item(SampleState2.None)
//        val it2 = Item(CuvetteState2.None)
//        val ret = it.itemState is SampleState2
//        val ret2 = it.itemState is CuvetteState2
//        val ret3 = it2.itemState is SampleState2
//        val ret4 = it2.itemState is CuvetteState2
//        println("ret=$ret ret2=$ret2 ret3=$ret3 ret4=$ret4")
    }
}
