package com.wl.turbidimetric.settings.network

import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.viewModels
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentNetworkBinding
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.view.dialog.HiltDialog
import com.wl.turbidimetric.view.dialog.showPop
import com.wl.weiqianwllib.network.NetworkUtil


class NetworkFragment :
    BaseFragment<NetworkViewModel, FragmentNetworkBinding>(R.layout.fragment_network) {
    override val vm: NetworkViewModel by viewModels()
    private val hiltDialog by lazy { HiltDialog(requireContext()) }
    override fun initViewModel() {
        vd.model = vm
    }

    override fun init(savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    private fun initView() {
        listenerView()
        listenerData()
    }

    private fun listenerData() {
        vm.mac.observe(this) {
            vd.tvMac.text = "MAC：${it}"
        }
    }

    private fun listenerView() {
        vd.btnChange.setOnClickListener {
            vm.verifyConfig().let { msg ->
                if (msg.isNotEmpty()) {
                    hiltDialog.showPop(requireContext()) { dialog ->
                        dialog.showDialog(msg, "确定", {
                            it.dismiss()
                        })
                    }
                    return@setOnClickListener
                }
                setConfig(
                    vm.ip.value ?: "",
                    vm.gateway.value ?: "",
                    vm.netmask.value ?: "",
                    vm.dns1.value ?: "",
                )
            }
        }

        vd.btnGetIp.setOnClickListener {
            val ip = NetworkUtil.getLocalIp()
            toast("ip=$ip")
        }
        updateMac()
        vd.tvMac.setOnClickListener {
            updateMac()
        }


    }

    private fun updateMac() {
        vm.mac.value = NetworkUtil.getMacAddress()
    }


    private fun initData() {
        val ip = Settings.Global.getString(requireContext().contentResolver, "ethernet_static_ip")?:""
        val mask =
            Settings.Global.getString(requireContext().contentResolver, "ethernet_static_mask")?:""
        val gateway =
            Settings.Global.getString(requireContext().contentResolver, "ethernet_static_gateway")?:""
        val dns1 =
            Settings.Global.getString(requireContext().contentResolver, "ethernet_static_dns1")?:""
        vm.changeConfig(ip, mask, gateway, dns1)
    }

    fun setConfig(
        ip: String, gateway: String, netmask: String, dns1: String
    ) {
        NetworkUtil.setStaticIp(requireContext(), ip, gateway, netmask, dns1)
    }

    companion object {
        @JvmStatic
        fun newInstance() = NetworkFragment()
    }
}
