package com.wl.turbidimetric.settings.network

import androidx.lifecycle.MutableLiveData
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.weiqianwllib.network.NetworkUtil

class NetworkViewModel : BaseViewModel() {
    val ip = MutableLiveData("")
    val gateway = MutableLiveData("")
    val netmask = MutableLiveData("")
    val dns1 = MutableLiveData("")
    val mac = MutableLiveData("")


    init {

    }



    fun verifyConfig(): String {
        if (ip.value.isNullOrEmpty()) {
            return "ip地址未输入"
        }
        if (gateway.value.isNullOrEmpty()) {
            return "网关未输入"
        }
        if (netmask.value.isNullOrEmpty()) {
            return "掩码未输入"
        }
        if (dns1.value.isNullOrEmpty()) {
            return "dns1未输入"
        }

        if (!NetworkUtil.isIP(ip.value ?: "")) {
            return "ip地址格式错误"
        }
        if (!NetworkUtil.isIP(gateway.value ?: "")) {
            return "网关格式错误"
        }
        if (!NetworkUtil.isIP(netmask.value ?: "")) {
            return "掩码格式错误"
        }
        if (!NetworkUtil.isIP(dns1.value ?: "")) {
            return "dns格式错误"
        }
        return ""
    }

    fun changeConfig(
        ip: String,
        gateway: String,
        netmask: String,
        dns1: String,
    ) {
        this.ip.value = ip
        this.gateway.value = gateway
        this.netmask.value = netmask
        this.dns1.value = dns1

    }
}
