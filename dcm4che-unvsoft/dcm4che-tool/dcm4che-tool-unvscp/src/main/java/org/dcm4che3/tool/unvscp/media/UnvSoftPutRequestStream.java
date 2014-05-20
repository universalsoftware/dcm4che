/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dcm4che3.tool.unvscp.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
class UnvSoftPutRequestStream extends InputStream {
    public static final String VERSION = "UPT0";

    byte[] header;
    List<UnvSoftParam> unvSoftParams;
    int headerPos = 0, paramsPos = 0;

    public UnvSoftPutRequestStream(Map<String, String> textHeaders, Map<String, File> files) {
        unvSoftParams = new ArrayList<UnvSoftParam>();
        for(Map.Entry<String, String> e : textHeaders.entrySet()) {
            unvSoftParams.add(new UnvSoftParam(e.getKey(), e.getValue()));
        }
        for(Map.Entry<String, File> e : files.entrySet()) {
            unvSoftParams.add(new UnvSoftParam(e.getKey() + "_FILE_NAME", e.getValue().getName()));
            unvSoftParams.add(new UnvSoftParam(e.getKey(), e.getValue()));
        }

        // 4 b. - protocol version; 4 b. - number of params
        header = new byte[4 + 4];

        System.arraycopy(VERSION.getBytes(Charset.forName("utf8")), 0, header, 0, 4);
        System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(unvSoftParams.size()).array(), 0, header, 4, 4);
    }

    @Override
    public int read() throws IOException {
        if (headerPos < header.length) {
            return header[headerPos++] & 255;
        }

        int value = -1;
        while (paramsPos < unvSoftParams.size() && (value = unvSoftParams.get(paramsPos).read()) == -1) {
            paramsPos++;
        }

        return value;
    }

    public static class UnvSoftParam {
        private static final int UTF8_TEXT = 0;
        private static final int BINARY_DATA = 1;

        private final byte[] meta;
        private final byte[] data;
        private final FileInputStream fis;
        private final int fileSize;
        private int metaPos, dataPos;

        public UnvSoftParam(String name, String value) {
            meta = createParamMeta(name, UTF8_TEXT);
            byte[] valueInBytes = value.getBytes(Charset.forName("utf8"));
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.length()).array(), 0, meta, 8, 4);
            data = valueInBytes;
            fis = null;
            fileSize = 0;
        }

        public UnvSoftParam(String name, File file) {
            meta = createParamMeta(name, BINARY_DATA);
            fileSize = (int)file.length();
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fileSize).array(), 0, meta, 8, 4);
            data = null;
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(file);
            } catch(FileNotFoundException fnfe) {
                fileInputStream = null;
            }
            fis = fileInputStream;
        }

        private byte[] createParamMeta(String paramName, int paramType) {
            byte[] nameInBytes = paramName.getBytes(Charset.forName("utf8"));

            // 4 b. - param type; 4 b. - param name length; 4 b. - param value length
            byte[] paramMeta = new byte[4 + 4 + 4 + nameInBytes.length];

            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(paramType).array(), 0, paramMeta, 0, 4);
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(paramName.length()).array(), 0, paramMeta, 4, 4);
            Arrays.fill(paramMeta, 8, 12, (byte)0);
            System.arraycopy(nameInBytes, 0, paramMeta, 12, nameInBytes.length);

            return paramMeta;
        }

        public int read() {
            if(meta != null && metaPos < meta.length) {
                return meta[metaPos++] & 255;
            } else if (data != null && dataPos < data.length) {
                return data[dataPos++] & 255;
            } else if (fis != null) {
                try {
                    return fis.read();
                } catch(IOException ioe) {
                    return -1;
                }
            } else {
                return -1;
            }
        }

        public int getLength() {
            return (meta == null ? 0 : meta.length) + (data == null ? 0 : data.length) + fileSize;
        }

        @Override
        public String toString() {
            int paramType = ByteBuffer.wrap(meta, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int nameLength = ByteBuffer.wrap(meta, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            String name = new String(meta, 12, nameLength, Charset.forName("utf8"));
            String value = data == null ? "[INPUT STREAM: " + (fis == null ? "null" : "SIZE=" + fileSize) + "]" : new String(data, Charset.forName("utf8"));
            return (paramType == 0 ? "UTF8_TEXT" : (paramType == 1 ? "BINARY_DATA" : "UNKNOWN")) + ": " + name + "=" + value;
        }
    }
}