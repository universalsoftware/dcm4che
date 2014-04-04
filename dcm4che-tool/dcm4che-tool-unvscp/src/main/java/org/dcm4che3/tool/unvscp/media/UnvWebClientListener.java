/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.media;

import java.util.EventListener;

/**
 *
 * @author Pavel Varzinov
 */
public interface UnvWebClientListener extends EventListener {
    void logMessage(DicomClientMetaInfo metaInfo, String command, Object message, String dialog);
    void logMessage(DicomClientMetaInfo metaInfo, String command, Object message);
    void logMultilineMessage(DicomClientMetaInfo metaInfo, String command, String header, String body, String footer);
}
