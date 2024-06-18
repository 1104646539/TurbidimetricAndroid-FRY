package com.wl.turbidimetric.upload.hl7.util;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Arrays;

import ca.uhn.hl7v2.llp.LLPException;

public class HL7Reader {
    private static final String TAG = "HL7Reader";
    protected Charset charset;
    private static final char END_OF_BLOCK = '\u001c';
    private static final char START_OF_BLOCK = '\u000b';
    private static final char CARRIAGE_RETURN = (char) 13;

    public HL7Reader(Charset charset) {
        this.charset = charset;
    }

    public String getMessage(InputStream in) throws IOException, LLPException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (true) {
            int c = in.read();
            if (c == -1) {
                Log.d(TAG, "getMessage: c=" + c);
                return null;
            } else if (c == END_OF_BLOCK) {
                return new String(out.toByteArray(), this.charset);
            } else if (c == START_OF_BLOCK) {
                out = new ByteArrayOutputStream();
            } else {
                out.write(c);
            }
        }
    }

    protected String toString(byte[] data) {
        return asString(data, this.charset);
    }

    protected static String asString(byte[] data, Charset charset) {
        return new String(data, charset);
    }
}
