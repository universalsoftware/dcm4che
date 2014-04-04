/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.dcm4che3.data.Attributes;

/**
 *
 * @author Alexander
 */
public interface IDicomReader {

    public static enum QRLevel {
        PATIENT, STUDY, SERIES, IMAGE;

        public static QRLevel getEnum(String name) {
            try {
                return valueOf(name);
            } catch(IllegalArgumentException iae) {
                return null;
            }
        }
    }

    public File getFile();
    public URL getURL();

    public Attributes getFileMetaInformation();
    public Attributes getFileSetInformation();

    public String getFileSetUID();
    public String getTransferSyntaxUID();
    public String getFileSetID();
    public File getDescriptorFile();
    public File toFile(String[] fileIDs);
    public String getDescriptorFileCharacterSet();
    public int getFileSetConsistencyFlag();
//    protected void setFileSetConsistencyFlag(int i);
    public boolean knownInconsistencies();
    public int getOffsetOfFirstRootDirectoryRecord();
//    protected void setOffsetOfFirstRootDirectoryRecord(int i);
    public int getOffsetOfLastRootDirectoryRecord();
//    protected void setOffsetOfLastRootDirectoryRecord(int i);
    public boolean isEmpty();
    public void clearCache();
    public Attributes readFirstRootDirectoryRecord() throws IOException;
    public Attributes readLastRootDirectoryRecord() throws IOException;

    public Attributes readNextDirectoryRecord(Attributes rec)
            throws IOException;

    public Attributes readLowerDirectoryRecord(Attributes rec)
            throws IOException;
/*
    protected Attributes findLastLowerDirectoryRecord(Attributes rec)
            throws IOException;
*/
    public Attributes findFirstRootDirectoryRecordInUse(boolean ignorePrivate) throws IOException;

    public Attributes findRootDirectoryRecord(Attributes keys, boolean ignorePrivate,
            boolean ignoreCaseOfPN, boolean matchNoValue)
            throws IOException;

    public Attributes findRootDirectoryRecord(boolean ignorePrivate, Attributes keys,
            boolean ignoreCaseOfPN, boolean matchNoValue) throws IOException;

    public Attributes findNextDirectoryRecordInUse(Attributes rec, boolean ignorePrivate)
            throws IOException;

    public Attributes findNextDirectoryRecord(Attributes rec, boolean ignorePrivate,
            Attributes keys, boolean ignoreCaseOfPN, boolean matchNoValue) throws IOException;

    public Attributes findLowerDirectoryRecordInUse(Attributes rec, boolean ignorePrivate)
            throws IOException;

    public Attributes findLowerDirectoryRecord(Attributes rec, boolean ignorePrivate,
            Attributes keys, boolean ignoreCaseOfPN, boolean matchNoValue)
            throws IOException;

    public Attributes findPatientRecord(String... ids) throws IOException;

    public Attributes findNextPatientRecord(Attributes patRec, String... ids) throws IOException;

    public Attributes findStudyRecord(Attributes patRec, String... iuids)
            throws IOException;

    public Attributes findNextStudyRecord(Attributes studyRec, String... iuids)
            throws IOException;

    public Attributes findSeriesRecord(Attributes studyRec, String... iuids)
            throws IOException;

    public Attributes findNextSeriesRecord(Attributes seriesRec, String... iuids)
            throws IOException;

    public Attributes findLowerInstanceRecord(Attributes seriesRec, boolean ignorePrivate,
            String... iuids) throws IOException;

    public Attributes findNextInstanceRecord(Attributes instRec, boolean ignorePrivate,
            String... iuids) throws IOException;

    public Attributes findRootInstanceRecord(boolean ignorePrivate, String... iuids)
            throws IOException;
/*
    private Attributes pk(String type, int tag, VR vr, String... ids);

    private Attributes pk(String... iuids);

    private Attributes findRecordInUse(int offset, boolean ignorePrivate, Attributes keys,
            boolean ignoreCaseOfPN, boolean matchNoValue)
            throws IOException;

    private synchronized Attributes readRecord(int offset) throws IOException;
*/
/*
    public static boolean inUse(Attributes rec);
    public static boolean isPrivate(Attributes rec);
*/
}