/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4che3.tool.unvscp.media;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.tool.unvscp.data.DicomAttribute;
import org.dcm4che3.tool.unvscp.data.DicomRecord;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.tool.unvscp.data.CFindWebResponse;
import org.dcm4che3.util.IntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class DicomWebReader implements Closeable, IDicomReader {

    static final Logger LOG = LoggerFactory.getLogger(DicomWebReader.class);

    protected final File file = null;
    protected final URL url;
    protected final RandomAccessFile raf = null;
    protected final DicomInputStream in = null;
    protected final Attributes fmi = null;
    protected final Attributes fsInfo = null;
    protected final IntHashMap<Attributes> cache = new IntHashMap<Attributes>();

    private QRLevel queryRetrieveLevel = QRLevel.STUDY;
    private Collection<DicomRecord> data;
    private Iterator<DicomRecord> patientIterator;
    private Iterator<DicomRecord> studyIterator;
    private Iterator<DicomRecord> seriesIterator;
    private Iterator<DicomRecord> instanceIterator;

    public DicomWebReader(URL url) throws IOException {
        this.url = url;
    }

    public void sendWebRequest(URL url, String username, String password, Attributes keys, QRLevel level, UnvWebClientListener listener, Association as) throws IOException {
        if (level != null) this.queryRetrieveLevel = level;

        UnvWebClient webClient = new UnvWebClient(url.toString());
        webClient.addListener(listener, new DicomClientMetaInfo(as));
        webClient.setBasicParams(username, password, "C-FIND", as);
        webClient.setParam("LEVEL", level.name());
        webClient.setParam("KEYS", DicomAttribute.getAttributesAsList(keys));

        webClient.sendPostRequest();

        CFindWebResponse cfwr = webClient.parseJsonResponse(CFindWebResponse.class);
        this.data = cfwr.getData();

        if (this.data != null) this.patientIterator = this.data.iterator();

    }

    @Override
    public final File getFile(){
        //throw new Exception("Web not File!");
        return null;
    }

    public File toFile(String[] fileIDs){
        return null;
    }

    public final URL getURL() {
        return this.url;
    }

    public final Attributes getFileMetaInformation() {
        return fmi;
    }

    public final Attributes getFileSetInformation() {
        return fsInfo;
    }

    public void close() throws IOException {
        raf.close();
    }

    public String getFileSetUID() {
        //return fmi.getString(Tag.MediaStorageSOPInstanceUID, null);
        return "Mock UID";
    }

    public String getTransferSyntaxUID() {
        return fmi.getString(Tag.TransferSyntaxUID, null);
    }

    public String getFileSetID() {
        //return fsInfo.getString(Tag.FileSetID, null);
        return "Mock ID";
    }

    public File getDescriptorFile() {
        return null;
    }

    public String getDescriptorFileCharacterSet() {
        return fsInfo.getString(
                Tag.SpecificCharacterSetOfFileSetDescriptorFile, null);
    }

    public int getFileSetConsistencyFlag() {
        return fsInfo.getInt(Tag.FileSetConsistencyFlag, 0);
    }

    protected void setFileSetConsistencyFlag(int i) {
        fsInfo.setInt(Tag.FileSetConsistencyFlag, VR.US, i);
    }

    public boolean knownInconsistencies() {
        return getFileSetConsistencyFlag() != 0;
    }

    public int getOffsetOfFirstRootDirectoryRecord() {
        return fsInfo.getInt(
                Tag.OffsetOfTheFirstDirectoryRecordOfTheRootDirectoryEntity, 0);
    }

    protected void setOffsetOfFirstRootDirectoryRecord(int i) {
        fsInfo.setInt(
                Tag.OffsetOfTheFirstDirectoryRecordOfTheRootDirectoryEntity,
                VR.UL, i);
    }

    public int getOffsetOfLastRootDirectoryRecord() {
        return fsInfo.getInt(
                Tag.OffsetOfTheLastDirectoryRecordOfTheRootDirectoryEntity, 0);
    }

    protected void setOffsetOfLastRootDirectoryRecord(int i) {
        fsInfo.setInt(
                Tag.OffsetOfTheLastDirectoryRecordOfTheRootDirectoryEntity,
                VR.UL, i);
    }

    public boolean isEmpty() {
        return getOffsetOfFirstRootDirectoryRecord() == 0;
    }

    public void clearCache() {
        cache.clear();
    }

    public Attributes readFirstRootDirectoryRecord() throws IOException {
        return readRecord(getOffsetOfFirstRootDirectoryRecord());
    }

    public Attributes readLastRootDirectoryRecord() throws IOException {
        return readRecord(getOffsetOfLastRootDirectoryRecord());
    }

    public Attributes readNextDirectoryRecord(Attributes rec)
            throws IOException {
        return readRecord(
                rec.getInt(Tag.OffsetOfTheNextDirectoryRecord, 0));
    }

    public Attributes readLowerDirectoryRecord(Attributes rec)
            throws IOException {
        return readRecord(
                rec.getInt(Tag.OffsetOfReferencedLowerLevelDirectoryEntity, 0));
    }

    protected Attributes findLastLowerDirectoryRecord(Attributes rec)
            throws IOException {
        Attributes lower = readLowerDirectoryRecord(rec);
        if (lower == null)
            return null;

        Attributes next;
        while ((next = readNextDirectoryRecord(lower)) != null)
            lower = next;
        return lower;
    }

    public Attributes findFirstRootDirectoryRecordInUse(boolean ignorePrivate) throws IOException {
        return findRootDirectoryRecord(ignorePrivate, null, false, false);
    }

    public Attributes findRootDirectoryRecord(Attributes keys, boolean ignorePrivate,
            boolean ignoreCaseOfPN, boolean matchNoValue)
            throws IOException {
        return findRecordInUse(getOffsetOfFirstRootDirectoryRecord(), ignorePrivate,
                keys, ignoreCaseOfPN, matchNoValue);
    }

    public Attributes findRootDirectoryRecord(boolean ignorePrivate, Attributes keys,
            boolean ignoreCaseOfPN, boolean matchNoValue) throws IOException {
        return findRootDirectoryRecord(keys, ignorePrivate, ignoreCaseOfPN, matchNoValue);
    }

    public Attributes findNextDirectoryRecordInUse(Attributes rec, boolean ignorePrivate)
            throws IOException {
        return findNextDirectoryRecord(rec, ignorePrivate, null, false, false);
    }

    public Attributes findNextDirectoryRecord(Attributes rec, boolean ignorePrivate,
            Attributes keys, boolean ignoreCaseOfPN, boolean matchNoValue) throws IOException {
        return findRecordInUse(
                rec.getInt(Tag.OffsetOfTheNextDirectoryRecord, 0), ignorePrivate,
                keys, ignoreCaseOfPN, matchNoValue);
    }

    public Attributes findLowerDirectoryRecordInUse(Attributes rec, boolean ignorePrivate)
            throws IOException {
        return findLowerDirectoryRecord(rec, ignorePrivate, null, false, false);
    }

    public Attributes findLowerDirectoryRecord(Attributes rec, boolean ignorePrivate,
            Attributes keys, boolean ignoreCaseOfPN, boolean matchNoValue)
            throws IOException {
        return findRecordInUse(
                rec.getInt(Tag.OffsetOfReferencedLowerLevelDirectoryEntity, 0), ignorePrivate,
                keys, ignoreCaseOfPN, matchNoValue);
    }

    @Override
    public Attributes findPatientRecord(String... ids) throws IOException {
        return this.findNextPatientRecord(null, ids);

        //return findRootDirectoryRecord(false,
        //        pk("PATIENT", Tag.PatientID, VR.LO, ids), false, false);*/
    }

    public Attributes findNextPatientRecord(Attributes patRec, String... ids) throws IOException {
        if (this.patientIterator == null || !this.patientIterator.hasNext()) return null;

        DicomRecord dr = this.patientIterator.next();
/*        this.studyIterator = dr.getNested() == null ? null : dr.getNested().iterator();
        if (this.studyIterator == null && (queryRetrieveLevel == QRLevel.STUDY || queryRetrieveLevel == QRLevel.SERIES || queryRetrieveLevel == QRLevel.IMAGE))
            throw new IOException("Web service did not provide STUDY level");*/

        try {
            return dr.getAttributes();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public Attributes findStudyRecord(Attributes patRec, String... iuids) throws IOException {
        return this.findNextStudyRecord(null, iuids);
        //return findLowerDirectoryRecord(patRec, false,
        //        pk("STUDY", Tag.StudyInstanceUID, VR.UI, iuids),
        //        false, false);
    }

    public Attributes findNextStudyRecord(Attributes studyRec, String... iuids) throws IOException {
        if (this.studyIterator == null || !this.studyIterator.hasNext()) return null;

        DicomRecord dr = this.studyIterator.next();
        this.seriesIterator = dr.getNested() == null ? null : dr.getNested().iterator();
        if (this.seriesIterator == null && (queryRetrieveLevel == QRLevel.SERIES || queryRetrieveLevel == QRLevel.IMAGE))
            throw new IOException("Web service did not provide SERIES level");

        try {
            return dr.getAttributes();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
        //return findNextDirectoryRecord(studyRec, false,
        //        pk("STUDY", Tag.StudyInstanceUID, VR.UI, iuids),
        //        false, false);
    }

    public Attributes findSeriesRecord(Attributes studyRec, String... iuids) throws IOException {
        return this.findNextSeriesRecord(null, iuids);
        //return findLowerDirectoryRecord(studyRec, false,
        //        pk("SERIES", Tag.SeriesInstanceUID, VR.UI, iuids),
        //        false, false);
    }

    public Attributes findNextSeriesRecord(Attributes seriesRec, String... iuids) throws IOException {
        if (this.seriesIterator == null || !this.seriesIterator.hasNext()) return null;

        DicomRecord dr = this.seriesIterator.next();
        this.instanceIterator = dr.getNested() == null ? null : dr.getNested().iterator();
        if (this.instanceIterator == null && queryRetrieveLevel == QRLevel.IMAGE)
            throw new IOException("Web service did not provide IMAGE level");


        try {
            return dr.getAttributes();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
        //return findNextDirectoryRecord(seriesRec, false,
        //        pk("SERIES", Tag.SeriesInstanceUID, VR.UI, iuids),
        //        false, false);
    }

    public Attributes findLowerInstanceRecord(Attributes seriesRec, boolean ignorePrivate, String... iuids) throws IOException {
        return this.findNextInstanceRecord(null, ignorePrivate, iuids);
        //return findLowerDirectoryRecord(seriesRec, ignorePrivate, pk(iuids), false, false);
    }

    public Attributes findNextInstanceRecord(Attributes instRec, boolean ignorePrivate, String... iuids) throws IOException {
        if (this.instanceIterator == null || !this.instanceIterator.hasNext()) return null;

        DicomRecord dr = this.instanceIterator.next();
        //this.seriesIterator = dr.getData().iterator();

        try {
            return dr.getAttributes();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
        //return findNextDirectoryRecord(instRec, ignorePrivate, pk(iuids), false, false);
    }

    public Attributes findRootInstanceRecord(boolean ignorePrivate, String... iuids)
            throws IOException {
        return findRootDirectoryRecord(ignorePrivate, pk(iuids), false, false);
    }

    private Attributes pk(String type, int tag, VR vr, String... ids) {
        Attributes pk = new Attributes(2);
        pk.setString(Tag.DirectoryRecordType, VR.CS, type);
        if (ids != null && ids.length != 0)
            pk.setString(tag, vr, ids);
        return pk;
    }

    private Attributes pk(String... iuids) {
        if (iuids == null || iuids.length == 0)
            return null;

        Attributes pk = new Attributes(1);
        pk.setString(Tag.ReferencedSOPInstanceUIDInFile, VR.UI, iuids);
        return pk;
    }

    private Attributes findRecordInUse(int offset, boolean ignorePrivate, Attributes keys,
            boolean ignoreCaseOfPN, boolean matchNoValue)
            throws IOException {
        while (offset != 0) {
            Attributes item = readRecord(offset);
            if (inUse(item) && !(ignorePrivate && isPrivate(item))
                    && (keys == null || item.matches(keys, ignoreCaseOfPN, matchNoValue)))
                return item;
            offset = item.getInt(Tag.OffsetOfTheNextDirectoryRecord, 0);
        }
        return null;
    }

    private synchronized Attributes readRecord(int offset) throws IOException {
        if (offset == 0)
            return null;

        Attributes item = cache.get(offset);
        if (item == null) {
            long off = offset & 0xffffffffL;
            raf.seek(off);
            in.setPosition(off);
            item = in.readItem();
            cache.put(offset, item);
        }
        return item;
    }

    public static boolean inUse(Attributes rec) {
        return rec.getInt(Tag.RecordInUseFlag, 0) != 0;
    }

    public static boolean isPrivate(Attributes rec) {
        return "PRIVATE".equals(rec.getString(Tag.DirectoryRecordType));
    }

}
