package com.wl.turbidimetric.upload.hl7.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import ca.uhn.hl7v2.llp.CharSetUtil;
import ca.uhn.hl7v2.llp.LLPException;

public  class HL7Write {
    protected Charset charset;

    public HL7Write(Charset charset) {
        this.charset = charset;
    }

    public void putMessage(String message, OutputStream out) throws IOException, LLPException {
        byte[] bytes = this.toByteArray(message);
        byte[] outBytes = new byte[bytes.length + 3];
        outBytes[0] = 11;
        System.arraycopy(bytes, 0, outBytes, 1, bytes.length);
        outBytes[outBytes.length - 2] = 28;
        outBytes[outBytes.length - 1] = 13;
        out.write(outBytes);
        out.flush();
    }

    protected byte[] toByteArray(String message) {
        return asByteArray(message, this.charset, false);
    }

    protected static byte[] asByteArray(String message, Charset charset, boolean omitBOM) {
        byte[] b = message.getBytes(charset);
        return omitBOM ? CharSetUtil.withoutBOM(b) : b;
    }
}
