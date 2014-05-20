
package org.dcm4che3.tool.unvscp.media;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.entity.InputStreamEntity;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class UnvSoftFileStreamEntityBuilder {
    Map<String, File> files = new LinkedHashMap<String, File>();
    Map<String, String> textHeaders = new LinkedHashMap<String, String>();

    public static UnvSoftFileStreamEntityBuilder create() {
        return new UnvSoftFileStreamEntityBuilder();
    }

    private UnvSoftFileStreamEntityBuilder() {}

    public UnvSoftFileStreamEntityBuilder addTextHeader(String name, String text) {
        if (name == null)  {
            throw new NullPointerException();
        }
        if (text == null) {
            text = "";
        }
        textHeaders.put(name, text);
        return this;
    }

    public UnvSoftFileStreamEntityBuilder addFileParam(String name, File file) {
        if (name == null || file == null) {
            throw new NullPointerException();
        }
        files.put(name, file);
        return this;
    }

    public HttpEntity build() {
        return new InputStreamEntity(new UnvSoftPutRequestStream(textHeaders, files));
    }

}
