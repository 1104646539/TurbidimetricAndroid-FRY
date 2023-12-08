package com.wl.mvvm_demo

import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.matchingargs.MatchingArgsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
}
