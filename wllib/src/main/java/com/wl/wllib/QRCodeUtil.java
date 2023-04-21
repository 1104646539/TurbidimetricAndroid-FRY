package com.wl.wllib;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 串口控制的二维码模块，
 * 使用usb转串的类实现
 */
public class QRCodeUtil {
    private static final String TAG = "QRCodeUtil";
    /**
     * 扫码超时的时间
     */
    private static final long SCAN_OVERTIME = 1000 * 5;
    /**
     * 扫码失败
     */
    private static final int WHAT_SCAN_FAILED = 1;
    //旧的，不知道型号
//    /**
//     * 打开扫码
//     */
//    private static byte[] sendOpen = new byte[]{0x7E, 0x00, 0x08, 0x01, 0x00, 0x02, 0x01, (byte) 0xAB, (byte) 0xCD};
//    /**
//     * 关闭扫码
//     */
//    private static byte[] sendClose = new byte[]{0x7E, 0x00, 0x08, 0x01, 0x00, 0x02, 0x00, (byte) 0xAB, (byte) 0xCD};
//    /**
//     * 发送打开扫码后返回的
//     */
//    private static byte[] sendOpenResult = new byte[]{0x02, 0x00, 0x00, 0x01, 0x00, 0x33, 0x31};
//    /**
//     * 发送关闭扫码后返回的
//     */
//    private static byte[] sendCloseResult = new byte[]{0x02, 0x00, 0x00, 0x01, 0x00, 0x33, 0x31};

//vm2106 新的
//    /**
//     * 打开扫码
//     */
//    private static byte[] sendOpen = new byte[]{0x5A, 0x00, 0x00, 0x08, 0x53, 0x52, 0x30, 0x33, 0x30, 0x33, 0x30, 0x31, 0x08, (byte) 0xA5};
//    /**
//     * 关闭扫码
//     */
//    private static byte[] sendClose = new byte[]{0x5A, 0x00, 0x00, 0x08, 0x53, 0x52, 0x30, 0x33, 0x30, 0x33, 0x30, 0x30, 0x09, (byte) 0xA5};
//    /**
//     * 发送打开扫码后返回的
//     */
//    private static byte[] sendOpenResult = new byte[]{0x5A, 0x01, 0x00, 0x02, (byte) 0x90, 0x00, (byte) 0x93, (byte) 0xA5};
//    /**
//     * 发送关闭扫码后返回的
//     */
//    private static byte[] sendCloseResult = new byte[]{90, 1, 0, 2, -17, -65, -67, 0, -17, -65, -67, -17, -65, -67};

//vm3290 新的  2021.6.9
    /**
     * 打开扫码
     */
    private static byte[] sendOpen = new byte[]{0x57, 0x00, 0x18, 0x00, 0x55, 0x00};
    /**
     * 关闭扫码
     */
    private static byte[] sendClose = new byte[]{0x57, 0x00, 0x19, 0x00, 0x55, 0x00};
    /**
     * 发送打开扫码后返回的 31 00 00 00 55 00
     */
    private static byte[] sendOpenResult = new byte[]{0x31, 0x00, 0x00, 0x00, 0x55, 0x00};
    /**
     * 发送关闭扫码后返回的
     */
    private static byte[] sendCloseResult = new byte[]{0x31, 0x00, 0x00, 0x00, 0x55, 0x00};


    private UsbToSerialPortUtil qrCodeSerialPortUtil;
    private int baudRate = 9600;
    /**
     * 这次是否已经回应
     * 已经回调了一次成功或失败时，就不在回应，
     * 避免在超时时间刚到，但还没来得及完全关闭摄像头时又扫码成功，导致一次扫码两次回调的问题
     */
    private volatile boolean isResponse = false;

    public QRCodeUtil(Activity activity) {
        qrCodeSerialPortUtil = new UsbToSerialPortUtil(activity, UsbToSerialPortUtil.VENDOR_PL2303, (byte) 0x0A, baudRate, new UsbToSerialPortUtil.OnUsbToSerialPortListener() {
            @Override
            public void onInitSuccess(UsbDevice usbDevice) {
                Log.d(TAG, "扫码串口初始化成功");
            }

            @Override
            public void onInitFailed(String msg) {
                Log.d(TAG, "扫码串口初始化失败 " + msg);
            }
        });
        qrCodeSerialPortUtil.setOnParseReadData(new UsbToSerialPortUtil.OnParseReadData() {
            @Override
            public void onParse(byte[] data, String temp) {
//                Log.d(TAG, "onParse data=" + data + " length=" + data.length + " temp=" + temp);
                if (temp == null) {
//                    ToastUtil.showToast("onParse temp=" + temp);
                }
                String str = "data :";
                for (int i = 0; i < data.length; i++) {
                    str += "i=" + i + " " + data[i];
                }
                Log.d(TAG, "data : str=" + str);

                ArrayList<Byte> sourceList = new ArrayList<Byte>();
                for (int i = 0; i < data.length; i++) {
                    sourceList.add(data[i]);
                }
                ArrayList<Byte> target1 = new ArrayList<Byte>();
                for (int i = 0; i < sendCloseResult.length; i++) {
                    target1.add(sendCloseResult[i]);
                }
                int index = -1;
                int tcount = 0;
                boolean isAgain = false;

//
                //因为发送关闭和开启扫码扫码模块都会回复响应码，但是响应码没有结束符，所以在接受到扫码成功后去统一删除掉所有响应码
                /** 清除响应码 start **/
                do {
                    index = -1;
                    tcount = 0;
                    isAgain = false;
                    for (int i = 0; i < sourceList.size(); i++) {
                        if (tcount == target1.size()) {
                            isAgain = true;
                            break;
                        }

                        if (sourceList.get(i).equals(target1.get(tcount))) {
                            if (tcount == 0) {
                                index = i;
                            }
                            tcount++;

                            if (tcount == target1.size()) {
                                isAgain = true;
                                break;
                            }
                        } else if (tcount != 0) {
                            tcount = 0;
                            index = -1;
                        }
                    }
//                    Log.d(TAG, "index=" + index + " tcount=" + tcount);
                    if (index == -1) {
                        isAgain = false;
                    } else {
                        Iterator<Byte> ibSource = sourceList.iterator();
                        int ibi = 0;
                        while (ibSource.hasNext()) {
                            ibi++;
                            ibSource.next();
                            if (ibi >= index && ibi <= index + target1.size()) {
                                ibSource.remove();
//                                Log.d(TAG, "index=" + index + " tcount=" + tcount +"删除" );
                            }
                        }

                        for (int i = 0; i < sourceList.size(); i++) {
//                            Log.d(TAG, "删除后的 i=" + i + " data[i]=" + sourceList.get(i));
                        }
                    }
                }
                while (isAgain);
                byte[] tempb = new byte[sourceList.size()];
                for (int i = 0; i < tempb.length; i++) {
                    tempb[i] = sourceList.get(i);
                }
                /** 清除响应码 end **/
                temp = new String(tempb);
                temp = temp.replaceAll("\r", "");
                temp = temp.replaceAll("\n", "");

                Log.d(TAG, "onParse data=" + data + " length=" + data.length + " temp=" + temp);

                handler.removeMessages(WHAT_SCAN_FAILED);
                if (onQRCodeListener != null) {
                    if (!isResponse) {
                        isResponse = true;
                        onQRCodeListener.onSuccess(temp, step);
                    }
                }
            }
        });

    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == WHAT_SCAN_FAILED) {
                sendCloseScan(step);
                if (onQRCodeListener != null) {
                    if (!isResponse) {
                        isResponse = true;
                        onQRCodeListener.onFailed(step);
                    }
                }
            }
        }
    };
    int step = -1;

    /**
     * 开始扫码
     *
     * @param step 扫码时，对应的移动步数
     */
    public void sendOpenScan(int step) {
        this.step = step;
        if (qrCodeSerialPortUtil == null) {
            return;
        }
        isResponse = false;
        qrCodeSerialPortUtil.write(sendOpen);
        handler.sendEmptyMessageDelayed(WHAT_SCAN_FAILED, SCAN_OVERTIME);
    }

    public void sendCloseScan(int step) {
        this.step = step;
        if (qrCodeSerialPortUtil == null) {
            return;
        }
        qrCodeSerialPortUtil.write(sendClose);
    }

    public static boolean isSendResult(byte[] result) {
        if (Arrays.equals(result, sendOpenResult)) {
            return true;
        }
        if (Arrays.equals(result, sendCloseResult)) {
            return true;
        }
        return false;
    }

    OnQRCodeListener onQRCodeListener;

    public void setOnQRCodeListener(OnQRCodeListener onQRCodeListener) {
        this.onQRCodeListener = onQRCodeListener;
    }

    public interface OnQRCodeListener {
        void onSuccess(String str, int step);

        void onFailed(int step);

    }
}
