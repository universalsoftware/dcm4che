
package org.dcm4che3.tool.unvscp.tasks;

import java.util.Date;
import java.util.EventListener;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public interface SenderTaskListener extends EventListener {
    void onAddNewFile(
        String sopInstanceUid,
        String studyInstanceUid,
        Date studyDate,
        String studyDescription,
        String patientName,
        Date patientDob
    );
    
    void onProcessFile(String sopInstanceUid, String errMsg);
}
