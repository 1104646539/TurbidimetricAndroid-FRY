package com.wl.weiqianwllib.network

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.*
import java.util.regex.Pattern
import kotlin.experimental.and

object NetworkUtil {
    private const val IPV4_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$"

    private val IPv4_PATTERN = Pattern.compile(IPV4_REGEX)

    //设置以太网静态IP
    fun setStaticIp(
        context: Context,
        address: String?,
        mask: String,
        gateway: String?,
        dns: String?
    ): Boolean {
        return try {
            //创建EthernetManager实例
            val ethernetManagerClass = Class.forName("android.net.EthernetManager")
            @SuppressLint("WrongConstant") val ethernetManager =
                context.getSystemService("ethernet")
            //设置IpConfiguration
            val staticIpConfiguration = newStaticIpConfiguration(address, gateway, mask, dns)
            val ipConfiguration = newIpConfiguration(staticIpConfiguration)
            //保存IP设置
            saveIpSettings(context, address, gateway, mask, dns)
            //设置Configuration
            val setConfigurationMethod = ethernetManagerClass.getDeclaredMethod(
                "setConfiguration",
                ipConfiguration!!.javaClass
            )
            setConfigurationMethod.invoke(ethernetManager, ipConfiguration)
            true
        } catch (e: Throwable) {
            //Console.error(e);
            e.printStackTrace()
            false
        }
    }

    //创建StaticIpConfiguration
    private fun newStaticIpConfiguration(
        address: String?,
        gateway: String?,
        mask: String,
        dns: String?
    ): Any? {
        //创建StaticIpConfiguration
        var staticIpConfiguration: Any? = null
        try {
            val clazz = Class.forName("android.net.StaticIpConfiguration")
            staticIpConfiguration = clazz.newInstance()
            //设置IP
            val addressField = clazz.getField("ipAddress")
            addressField[staticIpConfiguration] = newLinkAddress(address, mask)
            //设置网关
            val gatewayField = clazz.getField("gateway")
            gatewayField[staticIpConfiguration] = InetAddress.getByName(gateway)
            //设置子网掩码
            val domainField = clazz.getField("domains")
            domainField[staticIpConfiguration] = mask
            //设置DNS
            val dnsField = clazz.getField("dnsServers")
            val dnsList: MutableList<InetAddress> =
                dnsField[staticIpConfiguration] as MutableList<InetAddress>
            dnsList.add(InetAddress.getByName(dns))
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
        return staticIpConfiguration
    }


    //创建LinkAddress
    private fun newLinkAddress(address: String?, mask: String): Any? {
        var linkAddressClass: Class<*>? = null
        var constructor: Constructor<*>? = null
        return try {
            linkAddressClass = Class.forName("android.net.LinkAddress")
            constructor = linkAddressClass.getDeclaredConstructor(
                InetAddress::class.java,
                Int::class.javaPrimitiveType
            )
            constructor.newInstance(InetAddress.getByName(address), getPrefixLength(mask))
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            false
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            false
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            false
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            false
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            false
        } catch (e: InstantiationException) {
            e.printStackTrace()
            false
        }
    }

    //根据子网掩码，自动计算前缀长度
    private fun getPrefixLength(mask: String): Int {
        val strs = mask.split("\\.").toTypedArray()
        var count = 0
        for (str in strs) if (str == "255") ++count
        return count * 8
    }

    //创建IpConfiguration
    private fun newIpConfiguration(staticIpConfiguration: Any?): Any? {
        //创建IpConfiguration
        var ipConfiguration: Any? = null
        try {
            val clazz = Class.forName("android.net.IpConfiguration")
            ipConfiguration = clazz.newInstance()
            //设置staticIpConfiguration
            val staticIpConfigurationField = clazz.getField("staticIpConfiguration")
            staticIpConfigurationField[ipConfiguration] = staticIpConfiguration
            val ipConfigurationEnum = getIpConfigurationEnumMap(clazz)
            //设置ipAssignment
            val ipAssignment = clazz.getField("ipAssignment")
            ipAssignment[ipConfiguration] = ipConfigurationEnum["IpAssignment.STATIC"]
            //设置proxySettings
            val proxySettings = clazz.getField("proxySettings")
            proxySettings[ipConfiguration] = ipConfigurationEnum["ProxySettings.STATIC"]
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
        return ipConfiguration
    }

    //获取IpConfiguration类中的所有枚举常量
    private fun getIpConfigurationEnumMap(ipConfigurationClass: Class<*>): Map<String?, Any?> {
        val enumMap: MutableMap<String?, Any?> = hashMapOf()
        val classes = ipConfigurationClass.declaredClasses
        for (clazz in classes) {
            val enumConstants = clazz.enumConstants ?: continue
            for (constant in enumConstants) enumMap[clazz.simpleName + "." + constant.toString()] =
                constant
        }
        return enumMap
    }

    //保存IP设置
    private fun saveIpSettings(
        context: Context,
        address: String?,
        gateway: String?,
        mask: String?,
        dns: String?
    ) {
        val contentResolver = context.contentResolver
        Settings.Global.putString(contentResolver, "ethernet_static_ip", address)
        Settings.Global.putString(contentResolver, "ethernet_static_mask", mask)
        Settings.Global.putString(contentResolver, "ethernet_static_gateway", gateway)
        Settings.Global.putString(contentResolver, "ethernet_static_dns1", dns)
    }

    fun getLocalIp(): String? {
        var ipaddress = ""
        try {
            val en = NetworkInterface
                .getNetworkInterfaces()
            // 遍历所用的网络接口
            while (en.hasMoreElements()) {
                // 得到每一个网络接口绑定的所有ip
                val nif = en.nextElement()
                val inet = nif.inetAddresses
                // 遍历每一个接口绑定的所有ip
                while (inet.hasMoreElements()) {
                    val ip = inet.nextElement()
                    if (!ip.isLoopbackAddress) {
                        ipaddress = ip.hostAddress
                        if (isValidIPV4ByCustomRegex(ipaddress)) {
                            return ipaddress
                        }
                        //                        return ipaddress;
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return ipaddress
    }


    fun isValidIPV4ByCustomRegex(ip: String?): Boolean {
        if (ip == null || ip.trim { it <= ' ' }.isEmpty()) {
            return false
        }
        if (!IPv4_PATTERN.matcher(ip).matches()) {
            return false
        }
        val parts = ip.split("\\.").toTypedArray()
        try {
            for (segment in parts) {
                if (segment.toInt() > 255 ||
                    segment.length > 1 && segment.startsWith("0")
                ) {
                    return false
                }
            }
        } catch (e: NumberFormatException) {
            return false
        }
        return true
    }

    fun getMacAddress(): String? {
        var strMacAddr: String? = null
        try {
            val ip = getLocalInetAddress()
            val b = NetworkInterface.getByInetAddress(ip)
                .hardwareAddress
            val buffer = StringBuffer()
            for (i in b.indices) {
                if (i != 0) {
                    buffer.append(':')
                }
                val str = Integer.toHexString((b[i]).toInt() and 0xFF)
                buffer.append(if (str.length == 1) "0$str" else str)
            }
            strMacAddr = buffer.toString().uppercase(Locale.getDefault())
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return strMacAddr
    }

    /**
     * 获取移动设备本地IP
     *
     * @return
     */
    private fun getLocalInetAddress(): InetAddress? {
        var ip: InetAddress? = null
        try {
            //列举
            val en_netInterface: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
            while (en_netInterface.hasMoreElements()) { //是否还有元素
                val ni = en_netInterface.nextElement() as NetworkInterface //得到下一个元素
                val en_ip: Enumeration<*> = ni.inetAddresses //得到一个ip地址的列举
                while (en_ip.hasMoreElements()) {
                    ip = en_ip.nextElement() as InetAddress
                    ip =
                        if (!ip.isLoopbackAddress && ip!!.hostAddress.indexOf(":") == -1) break else null
                }
                if (ip != null) {
                    break
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return ip
    }
}
