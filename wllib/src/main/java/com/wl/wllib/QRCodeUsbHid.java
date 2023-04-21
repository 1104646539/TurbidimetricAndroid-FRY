package com.wl.wllib;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * coding by wang luo 2020/11
 */
public class QRCodeUsbHid {
    private final static String TAG = "QRCodeUsbHid";
    private Context context;
    private UsbManager myUsbManager;
    private UsbDevice myUsbDevice;
    private UsbInterface myInterface;
    private UsbDeviceConnection myDeviceConnection;

    //旭龙物联3030hd
    public static final int VendorID = 1317;
    public static final int ProductID = 42158;
    //万酷6226s
//    public static final int VendorID = 59473;
//    public static final int ProductID = 4097;
    //万酷扫码枪vm2106
//    public static final int VendorID = 9969;
//    public static final int ProductID = 34819;

    private UsbEndpoint epOut;
    private UsbEndpoint epIn;

    private static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private byte[] mybuffer;

    private static QRCodeUsbHid instance = QRCodeHolder.instance;

    public static QRCodeUsbHid getInstance() {
        return instance;
    }

    private static class QRCodeHolder {
        private final static QRCodeUsbHid instance = new QRCodeUsbHid();
    }

    public QRCodeUsbHid() {
        mybuffer = new byte[64];
    }

    public boolean init(Context context) {
        this.context = context;
        myUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (!enumerateDevice()) {
            return false;
        }
        if (!findInterface()) {
            return false;
        }
        if (!openDevice()) {
            return false;
        }
        if (!assignEndpoint()) {
            return false;
        }
        monitorResult();
        return true;
    }

    /**
     * 开始监听返回的数据
     */
    private void monitorResult() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(runnable);
    }

    int count;
    byte[] byteContent = new byte[1024];
    /**
     * 数据位开始的下标
     */
    int byteStartIndex = 2;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "开始监听");
            if (epIn == null) {
                Log.d(TAG, "监听失败");
                return;
            }
            Log.d(TAG, "正在监听");
            while (true) {
                //传出结果
                final byte[] reByte = new byte[64];
                int re2 = myDeviceConnection.bulkTransfer(epIn, reByte, reByte.length, 2000);
//                Log.i(TAG, "re2=" + re2 + "\n" + bytesToHexString(reByte));
                if (re2 == -1) {
                    continue;
                }

                int curLength = reByte[1];
                if (curLength <= 0) {
                    continue;
                }
                //先添加到数组内
                byte[] temp = Arrays.copyOfRange(reByte, byteStartIndex, curLength + byteStartIndex);
                LogToFile.d(TAG, "runnable" + " temp=" + new String(temp) + " temp.length=" + temp.length + " reByte[63]=" + reByte[63]);
                System.arraycopy(temp, 0, byteContent, count, temp.length);
                count += temp.length;
                //已经是最后一条了
                if (reByte[63] == 0) {
                    //取出信息后清空
                    byte[] temp2 = Arrays.copyOfRange(byteContent, 0, count);
                    count = 0;
                    if (onQRCodeListeners != null && !onQRCodeListeners.isEmpty()) {
                        onQRCodeListener = onQRCodeListeners.get(0);
                        if (onQRCodeListener != null) {
                            String code = new String(temp2).trim();
//                        LogToFile.d(TAG, "runnable2" + " temp2=" + code + " temp.length=" + temp2.length + "count=" + count + "code.length=" + code.length() + " reByte[63]=" + reByte[63]);
                            onQRCodeListener.onQRCodeListener(temp2, code);
                        }
                    }
                }
            }
        }
    };

    //获取权限，打开设备
    private boolean openDevice() {
        if (myInterface != null) {
            UsbDeviceConnection conn = null;
            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限，可以查阅相关资料
            if (myUsbManager.hasPermission(myUsbDevice)) {
                conn = myUsbManager.openDevice(myUsbDevice);
            } else {
                Log.d(TAG, "未获得权限");
                return false;
            }

            if (conn == null) {
                return false;
            }

            if (conn.claimInterface(myInterface, true)) {
                myDeviceConnection = conn; // 到此你的android设备已经连上HID设备
                Log.d(TAG, "打开设备成功");
                return true;
            } else {
                conn.close();
            }
        }
        return false;
    }

    /**
     * 枚举设备
     */
    private boolean enumerateDevice() {
        if (myUsbManager == null) {
            return false;
        }

        HashMap<String, UsbDevice> deviceList = myUsbManager.getDeviceList();
        if (!deviceList.isEmpty()) { // deviceList不为空
            StringBuffer sb = new StringBuffer();
            for (UsbDevice device : deviceList.values()) {
                sb.append(device.toString());
                sb.append("\n");
                // 输出设备信息
                Log.d(TAG, "DeviceInfo: " + device.toString());

                // 枚举到设备
                if (device.getVendorId() == VendorID
                        && device.getProductId() == ProductID) {
                    myUsbDevice = device;
                    Log.d(TAG, "枚举设备成功");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 分配端点，IN | OUT，即输入输出；此处我直接用1为OUT端点，0为IN，当然你也可以通过判断
     */
    private boolean assignEndpoint() {
        if (myInterface.getEndpoint(1) != null) {
            epOut = myInterface.getEndpoint(1);
        } else {
            Log.d(TAG, "assignEndpoint ==null");
            return false;
        }
        if (myInterface.getEndpoint(0) != null) {
            epIn = myInterface.getEndpoint(0);
        } else {
            Log.d(TAG, "assignEndpoint ==null");
            return false;
        }
        return true;
    }

    /**
     * 找设备接口
     */
    private boolean findInterface() {
        if (myUsbDevice != null) {
            Log.d(TAG, "interfaceCounts : " + myUsbDevice.getInterfaceCount());
            Log.d(TAG, "interfaceType : " + myUsbDevice.getInterface(0).getEndpoint(0).getType());
            for (int i = 0; i < myUsbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = myUsbDevice.getInterface(i);
                // 根据手上的设备做一些判断，其实这些信息都可以在枚举到设备时打印出来
//                Log.d(TAG, i+"  interfaceCounts2222 : " + intf.getInterfaceClass());3
//                UsbEndpoint[mAddress=129,mAttributes=3,mMaxPacketSize=8,mInterval=4]
                Log.d(TAG, i + "  getInterfaceClass : " + intf.getInterfaceClass());
                Log.d(TAG, i + "  getInterfaceSubclass : " + intf.getInterfaceSubclass());//得到该接口的子类 0
                Log.d(TAG, i + "  getInterfaceProtocol : " + intf.getInterfaceProtocol()); //协议类型 1
                if (intf.getInterfaceClass() == 3
                        && intf.getInterfaceSubclass() == 0
                        && intf.getInterfaceProtocol() == 0) {
                    myInterface = intf;
                    Log.d(TAG, "找到我的设备接口1");
                    return true;
                }
            }
        }
        return false;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    OnQRCodeListener onQRCodeListener;

//    /**
//     * 注册 onQRCodeListener 为当前的回调，并保存上一次的回调
//     *
//     * @param onQRCodeListener
//     */
//    public void setOnQRCodeListener(OnQRCodeListener onQRCodeListener) {
//        this.onQRCodeListenerOld = this.onQRCodeListener;
//        this.onQRCodeListener = onQRCodeListener;
//    }
//
//    /**
//     * 恢复上一次注册的回调
//     */
//    public void resetListener() {
//        this.onQRCodeListener = this.onQRCodeListenerOld;
//    }

    public interface OnQRCodeListener {
        void onQRCodeListener(byte[] bytes, String str);
    }

    /**
     * 收到信息时，只让第一个接收数据
     * 哪个组件需要数据时，调用addQRCodeListener注册，当不需要时调用removeQRCodeListener移除，这样一个数据只有一个回调能处理
     */
    List<OnQRCodeListener> onQRCodeListeners = new ArrayList<>();

    public void addQRCodeListener(OnQRCodeListener onQRCodeListener) {
        if (onQRCodeListener == null) {
            return;
        }
        onQRCodeListeners.add(0, onQRCodeListener);
    }

    public void removeQRCodeListener(OnQRCodeListener onQRCodeListener) {
        if (onQRCodeListener == null) {
            return;
        }
        if (!onQRCodeListeners.isEmpty()) {
            onQRCodeListeners.remove(onQRCodeListener);
        }
    }
}
