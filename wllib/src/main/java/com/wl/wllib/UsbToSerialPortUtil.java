package com.wl.wllib;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * usb转串的工具类
 */
public class UsbToSerialPortUtil implements UsbSerialInterface.UsbReadCallback {
    public final static int VENDOR_ID_CH340 = 6790;
    public final static int VENDOR_FT = 1027;
    public final static int VENDOR_PL2303 = 1659;
    public final static int VENDOR_8746 = 8746;
    private Context context;
    private String TAG = UsbToSerialPortUtil.class.getSimpleName();
    private int vendorId = -1;
    private StringBuilder strBuffer = new StringBuilder();
    private byte endByte;
    private char endChar;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private UsbManager manager;
    /**
     * 是否连接
     */
    private boolean connect = false;

    public boolean isConnect() {
        return connect;
    }

    public UsbToSerialPortUtil(Context context, int vendorId, byte endByte, char endChar, int baudRate, int dataBits, int stopBits, int parity_none, OnUsbToSerialPortListener onUsbToSerialPortListener) {
        this.onUsbToSerialPortListener = onUsbToSerialPortListener;
        this.context = context;
        this.vendorId = vendorId;
        this.endByte = endByte;
        this.endChar = endChar;

        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> usbDevices = manager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            Iterator<String> iterator = usbDevices.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                UsbDevice usbDevice = usbDevices.get(key);

                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%s:%s)-%b class:%X:%X name:%s ",
                        usbDevice.getVendorId(), usbDevice.getProductId(),
                        UsbSerialDevice.isSupported(usbDevice),
                        usbDevice.getDeviceClass(), usbDevice.getDeviceSubclass(),
                        usbDevice.getDeviceName()));

                if (UsbSerialDevice.isSupported(usbDevice)) {
                    if (usbDevice.getVendorId() == vendorId) {
                        device = usbDevice;
                        if (!manager.hasPermission(usbDevice)) {
                            if (onUsbToSerialPortListener != null) {
                                onUsbToSerialPortListener.onInitFailed("串口未开启，未获取权限");
                            }
                            Log.d(TAG, "!serialPort.open()");
                            return;
                        }
                        connection = manager.openDevice(usbDevice);
                        Log.d(TAG, "findSerialPortDevice 找到了设备device=" + device.toString());
                        break;
                    }
                }
            }
            //
            if (device != null) {
                serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                if (serialPort != null) {
                    //初始化成功
                    if (serialPort.open()) {
                        serialPort.setBaudRate(baudRate);
                        serialPort.setDataBits(dataBits);
                        serialPort.setStopBits(stopBits);
                        serialPort.setParity(parity_none);
                        serialPort.read(this);
                        connect = true;
                        if (onUsbToSerialPortListener != null) {
                            onUsbToSerialPortListener.onInitSuccess(device);
                        }
                    } else {
                        connect = false;
                        if (onUsbToSerialPortListener != null) {
                            onUsbToSerialPortListener.onInitFailed("串口未开启 !serialPort.open()");
                        }
                        Log.d(TAG, "!serialPort.open()");
                    }
                } else {
                    connect = false;
                    if (onUsbToSerialPortListener != null) {
                        onUsbToSerialPortListener.onInitFailed("串口未开启 serialPort==null");
                    }
                    Log.d(TAG, "serialPort==null");
                }
            } else {
                connect = false;
                if (onUsbToSerialPortListener != null) {
                    onUsbToSerialPortListener.onInitFailed("串口未开启 device==null");
                }
                Log.d(TAG, "device==null");
            }
        } else {
            connect = false;
            if (onUsbToSerialPortListener != null) {
                onUsbToSerialPortListener.onInitFailed("串口未开启 devices.isEmpty");
            }
            Log.d(TAG, "devices.isEmpty");
        }
    }

    public UsbToSerialPortUtil(Context context, int vendorId, byte endByte, char endChar, int baudRate, OnUsbToSerialPortListener onUsbToSerialPortListener) {
        this(context, vendorId, endByte, endChar, baudRate, UsbSerialInterface.DATA_BITS_8, UsbSerialInterface.STOP_BITS_1, UsbSerialInterface.PARITY_NONE, onUsbToSerialPortListener);
    }

    public UsbToSerialPortUtil(Context context, int vendorId, char endChar, int baudRate, OnUsbToSerialPortListener onUsbToSerialPortListener) {
        this(context, vendorId, (byte) 0x00, endChar, baudRate, UsbSerialInterface.DATA_BITS_8, UsbSerialInterface.STOP_BITS_1, UsbSerialInterface.PARITY_NONE, onUsbToSerialPortListener);
    }

    public UsbToSerialPortUtil(Context context, int vendorId, byte endByte, int baudRate, OnUsbToSerialPortListener onUsbToSerialPortListener) {
        this(context, vendorId, endByte, '\u0000', baudRate, UsbSerialInterface.DATA_BITS_8, UsbSerialInterface.STOP_BITS_1, UsbSerialInterface.PARITY_NONE, onUsbToSerialPortListener);
    }


    OnParseReadData onParseReadData;

    public void setOnParseReadData(OnParseReadData onParseReadData) {
        this.onParseReadData = onParseReadData;
    }

    ArrayList<Byte> bytes = new ArrayList<>();
    int length = 0;

    @Override
    public void onReceivedData(byte[] data) {
        if (data == null) {
            return;
        }
//        for (int i = 0; i < data.length; i++) {
//            Log.d(TAG, "i=" + i + "data=" + data[i]);
//        }
        if (endChar == '\uFFFF') {
//            Log.d(TAG, "onReceivedData ="+new String(data));
            parse(data, null);
            return;
        }
        //以char \n为结束符
        if (endChar != '\u0000') {
            String str = new String(data);
//        Log.d(TAG, "str=" + str);
            strBuffer.append(str);
            for (int i = 0; i < strBuffer.length(); i++) {
//                Log.d(TAG, "i=" + i + "strBuffer.charAt(i)=" + strBuffer.charAt(i));
                if (strBuffer.charAt(i) == endChar) {
                    String temp = strBuffer.substring(0, i + 1);
                    if (temp.length() > 1) {
                        parse(temp.getBytes(), temp);
                    }
                    strBuffer.delete(0, i + 1);
//                    Log.d(TAG, "temp=" + temp + " strBuffer=" + strBuffer.toString() + " len=" + strBuffer.length());
                    return;
                }
            }
            //以byte 0x0A为结束符
        } else {
            //1、先将新接收到的数组加入缓存
            for (int i = 0; i < data.length; i++) {
                bytes.add(data[i]);
            }
            int length = bytes.size();
            //2、判断缓存内是否有结束符，有就截取结束符之前的数组分发，同时清理缓存内已分发的数组，i=0可以使得如果两次的响应一起接收到都可以识别
            for (int i = 0; i < length; i++) {
                if (i >= bytes.size()) {
                    return;
                }
//                Log.d(TAG, "i=" + i + "data=" + bytes.get(i));
                if (bytes.get(i) == endByte) {
                    byte[] parseBytes = new byte[i + 1];
                    for (int j = 0; j < parseBytes.length; j++) {
//                        Log.d(TAG, "i=" + i + "bytes data=" + bytes.get(i) + "j=" + j + "bytes data=" + bytes.get(j));
                        parseBytes[j] = bytes.get(j);
                    }
                    if (bytes.size() > i + 1) {
                        ArrayList<Byte> tb = new ArrayList<>();
                        tb.addAll(bytes.subList(i + 1, length));
                        for (int j = 0; j < tb.size(); j++) {
//                            Log.d(TAG, "j=" + j + "bytes data=" + tb.get(j));
                        }
                        bytes = tb;
                        i = 0;
                    } else {
                        bytes.clear();
                    }
                    parse(parseBytes, "");
                }
            }
        }
    }


    public interface OnParseReadData {
        void onParse(byte[] data, String temp);
    }

    OnUsbToSerialPortListener onUsbToSerialPortListener;

    public void setOnUsbToSerialPortListener(OnUsbToSerialPortListener onUsbToSerialPortListener) {
        this.onUsbToSerialPortListener = onUsbToSerialPortListener;
    }

    public interface OnUsbToSerialPortListener {
        void onInitSuccess(UsbDevice usbDevice);

        void onInitFailed(String msg);
    }

    private void parse(byte[] data, String temp) {
//        Log.d(TAG, "parse data=" + data + " temp=" + temp);
        if (onParseReadData != null) {
            onParseReadData.onParse(data, temp);
        }
    }

    public void write(byte[] bytes) {
        if (bytes != null) {
            if (serialPort != null) {
                serialPort.write(bytes);
            }
        }
    }

}
