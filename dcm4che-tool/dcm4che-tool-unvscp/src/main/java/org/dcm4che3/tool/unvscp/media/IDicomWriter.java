
package org.dcm4che3.tool.unvscp.media;

import java.io.File;
import java.io.IOException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomEncodingOptions;
import org.dcm4che3.media.DicomDirWriter;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public interface IDicomWriter extends IDicomReader {
    public DicomDirWriter getDicomDirWriter();

    public DicomEncodingOptions getEncodingOptions();
    public void setEncodingOptions(DicomEncodingOptions encOpts);
    //@Override
    public void close() throws IOException;
    public String[] toFileIDs(File f);

    // Synchronized
    public Attributes addRootDirectoryRecord(Attributes rec) throws IOException;
    public Attributes addLowerDirectoryRecord(Attributes parentRec, Attributes rec)
            throws IOException;
    public Attributes findOrAddPatientRecord(Attributes rec) throws IOException;
    public Attributes findOrAddStudyRecord(Attributes patRec, Attributes rec)
            throws IOException;
    public Attributes findOrAddSeriesRecord(Attributes studyRec, Attributes rec)
            throws IOException;
    public boolean deleteRecord(Attributes rec) throws IOException;
    public void rollback() throws IOException;
    public void commit() throws IOException;
    public int purge() throws IOException;

}
