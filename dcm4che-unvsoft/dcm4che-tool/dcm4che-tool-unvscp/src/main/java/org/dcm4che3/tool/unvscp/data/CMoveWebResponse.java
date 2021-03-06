/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.data;

import java.net.URL;
import java.util.Collection;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class CMoveWebResponse extends GenericWebResponse {
    private URL url;
    private Collection<DicomFileWebData> data;

    public URL getUrl() {
        return this.url;
    }

    @Override
    public Collection<DicomFileWebData> getData() {
        return this.data;
    }
}
