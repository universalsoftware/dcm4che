
package org.dcm4che3.tool.unvscp.data;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class DicomFileDataPresentation {
    private String sopInstanceUid, studyInstanceUid, studyDescription, patientName;
    private Date studyDate, patientBirthDate;

    public DicomFileDataPresentation(File dicomFile) throws IOException {
        DicomInputStream dis = null;
        Attributes attribs = null;
        try {
            dis = new DicomInputStream(dicomFile);
            attribs = dis.readDataset(-1, Tag.PixelData);
            dis.close();
        } catch (IOException ioe) {
            try {
                dis.close();
            } catch(Exception ignore) {}
            throw ioe;
        }
        if (attribs != null) {
            sopInstanceUid = attribs.getString(Tag.SOPInstanceUID);
            studyInstanceUid = attribs.getString(Tag.StudyInstanceUID);
            studyDescription = attribs.getString(Tag.StudyDescription);
            patientName = attribs.getString(Tag.PatientName);
            studyDate = attribs.getDate(Tag.StudyDate);
            patientBirthDate = attribs.getDate(Tag.PatientBirthDate);
        }
    }

    public String getSopInstanceUid() {
        return sopInstanceUid;
    }

    public String getStudyInstanceUid() {
        return studyInstanceUid;
    }
    public String getStudyDescription() {
        return studyDescription;
    }
    public String getPatientName() {
        if (patientName != null) {
            return patientName.replaceFirst("\\^", ", ").replaceAll("\\^", " ");
        } else {
            return null;
        }
    }
    public Date getStudyDate() {
        return studyDate;
    }
    public Date getPatientBirthDate() {
        return patientBirthDate;
    }
}
