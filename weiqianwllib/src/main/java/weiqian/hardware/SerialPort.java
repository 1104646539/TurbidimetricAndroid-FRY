package weiqian.hardware;


public class SerialPort {

    private int mFd;

    public SerialPort() {
        mFd = -1;
    }

    public void open(String path, int baud, int databits, String parity, int stopbits) {
        mFd = HardwareControl.OpenSerialPort(path, baud, databits, parity, stopbits);
    }

    public boolean isOpen() {
        return mFd > 0;
    }

    public void close() {
        HardwareControl.CloseSerialPort(mFd);
    }

    public int read(byte[] buff, int count) {
        return HardwareControl.ReadSerialPort(mFd, buff, count);
    }

    public int write(byte[] buff, int count) {
        return HardwareControl.WriteSerialPort(mFd, buff, count);
    }

    public int readTimeout(byte[] buff, int count, int timeout) {
        return HardwareControl.ReadSerialPortTimeout(mFd, buff, count, timeout);
    }

    public String read() {
        byte[] buff = new byte[64];
        read(buff, 64);
        return buff.toString();
    }

    public void write(String buff) {
        write(buff.getBytes(), buff.length());
    }
}
