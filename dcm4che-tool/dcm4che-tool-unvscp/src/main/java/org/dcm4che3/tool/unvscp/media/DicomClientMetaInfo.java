/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.media;

import java.util.Properties;
import org.dcm4che3.net.Association;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class DicomClientMetaInfo {
    private Association as;
    private Properties extraData;

    public DicomClientMetaInfo(Association as) {
        this.as = as;
    }

    public DicomClientMetaInfo(Properties extraData) {
        this.extraData = extraData;
    }

    public DicomClientMetaInfo(Association as, Properties extraData) {
        this.as = as;
        this.extraData = extraData;
    }

    public Association getAssoc() {
        return this.as;
    }

    public Properties getExtraData() {
        return this.extraData;
    }
}
