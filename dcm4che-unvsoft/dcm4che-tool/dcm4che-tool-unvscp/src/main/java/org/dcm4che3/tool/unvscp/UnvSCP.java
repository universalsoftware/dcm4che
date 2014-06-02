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

package org.dcm4che3.tool.unvscp;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.tool.unvscp.data.DicomAttribute;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.media.DicomDirWriter;
import org.dcm4che3.tool.unvscp.media.DicomWebReader;
import org.dcm4che3.tool.unvscp.media.IDicomReader;
import org.dcm4che3.media.RecordFactory;
import org.dcm4che3.media.RecordType;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.AssociationStateException;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.QueryOption;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateAC;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.pdu.RoleSelection;
import org.dcm4che3.net.service.AbstractDicomService;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.BasicCFindSCP;
import org.dcm4che3.net.service.BasicCGetSCP;
import org.dcm4che3.net.service.BasicCMoveSCP;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.BasicRetrieveTask;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4che3.net.service.InstanceLocator;
import org.dcm4che3.net.service.QueryRetrieveLevel;
import org.dcm4che3.net.service.QueryTask;
import org.dcm4che3.net.service.RetrieveTask;
import org.dcm4che3.tool.common.CLIUtils;
import org.dcm4che3.tool.common.FilesetInfo;
import org.dcm4che3.tool.unvscp.data.CAuthWebResponse;
import org.dcm4che3.tool.unvscp.data.CMoveWebResponse;
import org.dcm4che3.tool.unvscp.data.CStoreWebResponse;
import org.dcm4che3.tool.unvscp.data.DicomFileDataPresentation;
import org.dcm4che3.tool.unvscp.data.DicomFileWebData;
import org.dcm4che3.tool.unvscp.data.GenericWebResponse;
import org.dcm4che3.tool.unvscp.gui.ActivityWindow;
import org.dcm4che3.tool.unvscp.media.DicomClientMetaInfo;
import org.dcm4che3.tool.unvscp.media.DicomDirReaderWrapper;
import org.dcm4che3.tool.unvscp.media.DicomDirWriterWrapper;
import org.dcm4che3.tool.unvscp.media.IDicomWriter;
import org.dcm4che3.tool.unvscp.media.UnvWebClient;
import org.dcm4che3.tool.unvscp.media.UnvWebClientListener;
import org.dcm4che3.tool.unvscp.net.pdu.UnvAAssociateRJ;
import org.dcm4che3.tool.unvscp.tasks.AsyncSenderTask;
import org.dcm4che3.tool.unvscp.tasks.DcmFileSenderTask;
import org.dcm4che3.util.AttributesFormat;
import org.dcm4che3.util.SafeClose;
import org.dcm4che3.util.StringUtils;
import org.dcm4che3.util.TagUtils;
import org.dcm4che3.util.UIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com> Pavel Varzinov <varzinov@yandex.ru> Alexander Sirotin <alexander.sirotin@unvsoft.com>
 *
 */
public class UnvSCP implements UnvWebClientListener {

    static final Logger LOG = LoggerFactory.getLogger(UnvSCP.class);
    private static boolean isDynamicAEList = false;

    private static final String[] PATIENT_ROOT_LEVELS = {
        "PATIENT", "STUDY", "SERIES", "IMAGE" };
    private static final String[] STUDY_ROOT_LEVELS = {
        "STUDY", "SERIES", "IMAGE" };
    private static final String[] PATIENT_STUDY_ONLY_LEVELS = {
        "PATIENT", "STUDY" };
    private static ResourceBundle rb =
         ResourceBundle.getBundle("org.dcm4che3.tool.unvscp.messages");

    private final Device device = new Device("unvscp");
    private final ApplicationEntity ae = new ApplicationEntity("*");
    private final Connection conn = new Connection();

    private File storageDir;
    private File dicomDir;
    private boolean isUrl = false;
    private boolean notConcurrent = false;
    private URL dicomUrl, dicomDownloadUrl;
    private String emsowUsername, emsowPassword;
    private AttributesFormat filePathFormat;
    private RecordFactory recFact;
    private String availability;
    private boolean stgCmtOnSameAssoc;
    private boolean sendPendingCGet;
    private int sendPendingCMoveInterval;
    private final FilesetInfo fsInfo = new FilesetInfo();
    private IDicomReader ddReader;
    private IDicomWriter ddWriter;
    private final Map<String, Connection> remoteConnections = Collections.synchronizedMap(new HashMap<String, Connection>());
    private CAuthWebResponse.AERecord emsowServerAeMeta;
    private final Map<String, CAuthWebResponse.AERecord> emsowClientAets = Collections.synchronizedMap(new HashMap<String, CAuthWebResponse.AERecord>());
    private final Set<ExtendedNegotiation> extendedNegotiaions = new LinkedHashSet<ExtendedNegotiation>();
    private final Set<RoleSelection> roleSelections = new LinkedHashSet<RoleSelection>();
    private boolean isPushForUnknownEnabled, isPullForUnknownEnabled;
    private boolean allowUnknown = false;
    private boolean async = false;
    private int sleepTime = 5;
    private File badFilesDir;
    private File uplDir, uplBadFilesDir;
    private File tmpDir;
    private int uplSleepTime = 5;
    private Integer compressionLevel = null; // if null then compression is not used

    private Thread asyncSender, dcmFileSender;

    private ActivityWindow activityWindow;

    private final class CStoreSCPImpl extends BasicCStoreSCP {

        CStoreSCPImpl() {
            super("*");
        }

        @Override
        protected void store(Association as, PresentationContext pc,
                Attributes rq, PDVInputStream data, Attributes rsp)
                throws IOException {
            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
            String tsuid = pc.getTransferSyntax();
            File file = new File(storageDir, iuid);
            try {
                Attributes fmi = as.createFileMetaInformation(iuid, cuid, tsuid);
                storeTo(as, fmi, data, file, iuid);
                Attributes attrs = parse(file);
                File dest = getDestinationFile(attrs);
                renameTo(as, file, dest);
                file = dest;
                if (addDicomDirRecords(as, attrs, fmi, file)) {
                    LOG.info("{}: M-UPDATE {}", as, dicomDir);
                } else {
                    LOG.info("{}: ignore received object", as);
                    deleteFile(as, file, iuid);
                }

            } catch (Exception e) {
                deleteFile(as, file, iuid);
                throw new DicomServiceException(Status.ProcessingFailure, e);
            }
        }
    };

    private final class CStoreWebSCPImpl extends BasicCStoreSCP {
        CStoreWebSCPImpl() {
            super("*");
        }

        private void onAddNewFile(DicomFileDataPresentation dfdp) {
            if (activityWindow != null && dfdp != null) {
                activityWindow.onAddNewFile(
                    dfdp.getSopInstanceUid(),
                    dfdp.getStudyInstanceUid(),
                    dfdp.getStudyDate(),
                    dfdp.getStudyDescription(),
                    dfdp.getPatientName(),
                    dfdp.getPatientBirthDate()
                );
            }
        }

        private void onStartProcessingFile(DicomFileDataPresentation dfdp){
            if (activityWindow != null && dfdp != null) {
                activityWindow.onStartProcessingFile(dfdp.getSopInstanceUid());
            }
        }

        private void onFinishProcessingFile(DicomFileDataPresentation dfdp, String errMsg) {
            if (activityWindow != null && dfdp != null) {
                activityWindow.onFinishProcessingFile(dfdp.getSopInstanceUid(), errMsg);
            }
        }

        @Override
        protected void store(Association as, PresentationContext pc, Attributes rq,
            PDVInputStream data, Attributes rsp) throws IOException {

            String serviceCommand = "C-STORE";
            checkEmsowPermissions(as, serviceCommand);

            String iuid  = rq.getString(Tag.AffectedSOPInstanceUID);
            String cuid  = rq.getString(Tag.AffectedSOPClassUID);
            String tsuid = pc.getTransferSyntax();
            Attributes fmi = as.createFileMetaInformation(iuid, cuid, tsuid);

            String fileName = java.util.UUID.randomUUID().toString();
            if (UnvSCP.this.async) {
                fileName = (new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS-")).format(new Date()) + fileName;
            }

            File file = new File(UnvSCP.this.storageDir, fileName);

            FileOutputStream fos = null;
            File metaFile = null;
            try {
                storeTo(as, fmi, data, file, iuid);

                DicomFileDataPresentation dicomFileData = null;
                try {
                    dicomFileData = new DicomFileDataPresentation(file);
                } catch (IOException ioe) {}
                onAddNewFile(dicomFileData);

                if(UnvSCP.this.async) {
                    Properties metaData = UnvWebClient.getAssocParams(as);
                    if (metaData != null) {
                        metaData.setProperty("SOP_INSTANCE_UID", iuid);
                        metaFile = new File(UnvSCP.this.storageDir, file.getName() + ".1.meta");
                        fos = new FileOutputStream(metaFile);
                        metaData.store(fos, null);
                        fos.close();
                    } else {
                        LOG.warn("{}: M-WRITE-BEGIN metafile was not created due to empty Association data, UID={}", new Object[]{as, iuid});
                    }
                    file.renameTo(new File(file.getParentFile(), file.getName() + ".2.dcm"));
                    Thread asyncSender = UnvSCP.this.asyncSender;
                    if (asyncSender != null && asyncSender.isAlive()) {
                        synchronized(asyncSender) {
                            asyncSender.notify();
                        }
                    }
                } else {
                    onStartProcessingFile(dicomFileData);
                    try {
                        // We rename the file because it must have extension ".dcm"
                        File dcmFile = new File(file.getParentFile(), file.getName() + ".dcm");
                        if (file.renameTo(dcmFile)) {
                            file = dcmFile;
                        }
                        this.doWebCStore(as, serviceCommand, iuid, file);
                        onFinishProcessingFile(dicomFileData, null);
                    } catch(IOException ioe) {
                        onFinishProcessingFile(dicomFileData, ioe.getMessage());
                        throw ioe;
                    }
                }
            } catch(Exception e) {
                try { fos.close(); } catch(Exception ignore) {}
                try { metaFile.delete(); } catch(Exception ignore) {}
                throw new DicomServiceException(Status.OutOfResources, e.getMessage());
            } finally {
                if (file.exists()) {
                    deleteFile(as, file, iuid);
                }
            }
        }

        private void doWebCStore (Association as, String serviceCommand, String iuid, File file) throws IOException {
            DicomClientMetaInfo metaInfo = new DicomClientMetaInfo(as);
            UnvWebClient webClient = new UnvWebClient(UnvSCP.this.dicomUrl.toString());
            webClient.addListener(UnvSCP.this, metaInfo);
            webClient.setBasicParams(UnvSCP.this.emsowUsername, UnvSCP.this.emsowPassword, serviceCommand, as);
            webClient.setParam("SOP_INSTANCE_UID", iuid);
            webClient.attachFile("file", file);

            if (UnvSCP.this.notConcurrent) {
                synchronized(UnvSCP.this) {
                    webClient.uploadFiles();
                }
            } else {
                webClient.uploadFiles();
            }
            CStoreWebResponse cswr = webClient.parseJsonResponse(CStoreWebResponse.class);

            Collection<CStoreWebResponse.ExtraMessage> result = cswr.getData();

            if(result != null) {
                for(CStoreWebResponse.ExtraMessage rec : result) {
                    if (rec.getSuccess() == null) {
                        rec.setSuccess("1");
                    }
                    if (rec.getDebug() == null) {
                        rec.setDebug("0");
                    }
                    if (rec.getDebugLevel() == null) {
                        rec.setDebugLevel("0");
                    }
                    logMessage(metaInfo, serviceCommand, rec, "EMSOW-DIALOG");
                }
            }
        }

    }

    private final class StgCmtSCPImpl extends AbstractDicomService {

        public StgCmtSCPImpl() {
            super(UID.StorageCommitmentPushModelSOPClass);
        }

        @Override
        public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                Attributes rq, Attributes actionInfo) throws IOException {
            if (dimse != Dimse.N_ACTION_RQ)
                throw new DicomServiceException(Status.UnrecognizedOperation);

            int actionTypeID = rq.getInt(Tag.ActionTypeID, 0);
            if (actionTypeID != 1)
                throw new DicomServiceException(Status.NoSuchActionType)
                            .setActionTypeID(actionTypeID);

            Attributes rsp = Commands.mkNActionRSP(rq, Status.Success);
            String callingAET = as.getCallingAET();
            String calledAET = as.getCalledAET();
            Connection remoteConnection = getRemoteConnection(callingAET);
            if (remoteConnection == null)
                throw new DicomServiceException(Status.ProcessingFailure,
                        "Unknown Calling AET: " + callingAET);
            Attributes eventInfo =
                    calculateStorageCommitmentResult(calledAET, actionInfo);
            try {
                as.writeDimseRSP(pc, rsp, null);
                device.execute(new SendStgCmtResult(as, eventInfo,
                        stgCmtOnSameAssoc, remoteConnection));
            } catch (AssociationStateException e) {
                LOG.warn("{} << N-ACTION-RSP failed: {}", as, e.getMessage());
            }
        }

    }

    private final class CFindSCPImpl extends BasicCFindSCP {

        private final String[] qrLevels;
        private final QueryRetrieveLevel rootLevel;

        public CFindSCPImpl(String sopClass, String... qrLevels) {
            super(sopClass);
            this.qrLevels = qrLevels;
            this.rootLevel = QueryRetrieveLevel.valueOf(qrLevels[0]);
        }

        @Override
        protected QueryTask calculateMatches(Association as, PresentationContext pc,
                Attributes rq, Attributes keys) throws DicomServiceException {
            checkEmsowPermissions(as, "C-FIND");
            QueryRetrieveLevel level = QueryRetrieveLevel.valueOf(keys, qrLevels);
            level.validateQueryKeys(keys, rootLevel,
                    rootLevel == QueryRetrieveLevel.IMAGE || relational(as, rq));
            IDicomReader ddr = getDicomDirReader();

            try {
                if (isUrl) {
                    ((DicomWebReader)ddr).sendWebRequest(
                        UnvSCP.this.dicomUrl, UnvSCP.this.emsowUsername, UnvSCP.this.emsowPassword,
                        keys, IDicomReader.QRLevel.getEnum(level.name()), UnvSCP.this, as
                    );
                }
            } catch(UnknownHostException uhe) {
                throw new DicomServiceException(Status.UnableToProcess, "Unknown Host: " + uhe.getMessage());
            } catch(IOException ioe) {
                throw new DicomServiceException(Status.UnableToProcess, ioe.getMessage());
            }

            String availability = getInstanceAvailability();
            return new PatientQueryTask(as, pc, rq, keys, ddr, availability);
        }

        private boolean relational(Association as, Attributes rq) {
            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
            return QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
        }
    }

    private final class CGetSCPImpl extends BasicCGetSCP {

        private final String[] qrLevels;
        private final boolean withoutBulkData;
        private final QueryRetrieveLevel rootLevel;

        public CGetSCPImpl(String sopClass, String... qrLevels) {
            super(sopClass);
            this.qrLevels = qrLevels;
            this.withoutBulkData = qrLevels.length == 0;
            this.rootLevel = withoutBulkData
                    ? QueryRetrieveLevel.IMAGE
                    : QueryRetrieveLevel.valueOf(qrLevels[0]);
        }

        @Override
        protected RetrieveTask calculateMatches(Association as, PresentationContext pc,
                Attributes rq, Attributes keys) throws DicomServiceException {
            checkEmsowPermissions(as, "C-GET");
            QueryRetrieveLevel level = withoutBulkData
                    ? QueryRetrieveLevel.IMAGE
                    : QueryRetrieveLevel.valueOf(keys, qrLevels);
            level.validateRetrieveKeys(keys, rootLevel, relational(as, rq));
            List<InstanceLocator> matches = UnvSCP.this.calculateMatches(keys, as, "C-GET");
            CGetRetrieveTask retrieveTask = new CGetRetrieveTask(as, pc, rq, matches,
                    UnvSCP.this.dicomDownloadUrl, UnvSCP.this.emsowUsername, UnvSCP.this.emsowPassword, UnvSCP.this, withoutBulkData);
            retrieveTask.setSendPendingRSP(isSendPendingCGet());
            return retrieveTask;
        }

        private boolean relational(Association as, Attributes rq) {
            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
            return QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
        }

    }

    private final class CMoveSCPImpl extends BasicCMoveSCP {

        private final String[] qrLevels;
        private final QueryRetrieveLevel rootLevel;

        public CMoveSCPImpl(String sopClass, String... qrLevels) {
            super(sopClass);
            this.qrLevels = qrLevels;
            this.rootLevel = QueryRetrieveLevel.valueOf(qrLevels[0]);
        }

        @Override
        protected RetrieveTask calculateMatches(Association as, PresentationContext pc,
                final Attributes rq, Attributes keys) throws DicomServiceException {
            checkEmsowPermissions(as, "C-MOVE");
            QueryRetrieveLevel level = QueryRetrieveLevel.valueOf(keys, qrLevels);
            level.validateRetrieveKeys(keys, rootLevel, relational(as, rq));
            String dest = rq.getString(Tag.MoveDestination);

            //If MoveDestination AET is not on the list then client's remote host and port 104 are used
            Connection cn = getRemoteConnection(dest);
            if (cn == null && !UnvSCP.this.allowUnknown) {
                throw new DicomServiceException(Status.MoveDestinationUnknown,
                        "Move destination is not configured for this AET: " + dest);
            } else {
                String defaultHost = (
                    cn == null && UnvSCP.this.allowUnknown
                    || cn.getHostname() == null
                    || "".equals(cn.getHostname().trim())
                    || "0.0.0.0".equals(cn.getHostname().trim())
                )
                    ? as.getSocket().getInetAddress().getHostAddress()
                    : cn.getHostname();

                int defaultPort = (
                    cn == null && UnvSCP.this.allowUnknown
                    || cn.getPort() == 0
                )
                    ? 104
                    : cn.getPort();

                cn = UnvSCP.createAllowedConnection(defaultHost + ":" + defaultPort);
            }
            final Connection remote = cn;

            List<InstanceLocator> matches = UnvSCP.this.calculateMatches(keys, as, "C-MOVE");
            BasicRetrieveTask retrieveTask = new CMoveRetrieveTask(
                    BasicRetrieveTask.Service.C_MOVE, as, pc, rq, matches,
                    UnvSCP.this.dicomDownloadUrl, UnvSCP.this.emsowUsername, UnvSCP.this.emsowPassword, UnvSCP.this) {

                @Override
                protected Association getStoreAssociation() throws DicomServiceException {
                    try {
                        return as.getApplicationEntity().connect(
                                as.getConnection(), remote, makeAAssociateRQ());
                    } catch (IOException e) {
                        LOG.info("{} << ERROR: {} {}:{}", new String[]{this.as.toString(), e.getMessage(), remote.getHostname(), remote.getPort() + ""});
                        throw new DicomServiceException(Status.UnableToPerformSubOperations, e);
                    } catch (InterruptedException e) {
                        LOG.info("{} << ERROR: {} {}:{}", new String[]{this.as.toString(), e.getMessage(), remote.getHostname(), remote.getPort() + ""});
                        throw new DicomServiceException(Status.UnableToPerformSubOperations, e);
                    } catch (IncompatibleConnectionException e) {
                        LOG.info("{} << ERROR: {} {}:{}", new String[]{this.as.toString(), e.getMessage(), remote.getHostname(), remote.getPort() + ""});
                        throw new DicomServiceException(Status.UnableToPerformSubOperations, e);
                    } catch (GeneralSecurityException e) {
                        LOG.info("{} << ERROR: {} {}:{}", new String[]{this.as.toString(), e.getMessage(), remote.getHostname(), remote.getPort() + ""});
                        throw new DicomServiceException(Status.UnableToPerformSubOperations, e);
                    }
                }

            };
            retrieveTask.setSendPendingRSPInterval(getSendPendingCMoveInterval());
            return retrieveTask;
        }

        private boolean relational(Association as, Attributes rq) {
            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
            return QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
        }
    }

    public UnvSCP(boolean isUrl) throws IOException {
        this.isUrl = isUrl;
        device.addConnection(conn);
        device.addApplicationEntity(ae);
        ae.setAssociationAcceptor(true);
        ae.addConnection(conn);
        device.setDimseRQHandler(createServiceRegistry());
    }

    public UnvSCP() throws IOException {
        this(false);
    }

    private void checkEmsowPermissions(Association as, String serviceCommand) throws DicomServiceException {
        if(serviceCommand == null) {
            throw new DicomServiceException(Status.UnableToProcess, "serviceCommand cannot be empty");
        }
        boolean isPush = serviceCommand.equalsIgnoreCase("C-STORE");
        boolean isPull = serviceCommand.matches("^((?i)C-GET|(?i)C-MOVE|(?i)C-FIND)$");

        String calledAet;
        String callingAet = as.getCallingAET();
        CAuthWebResponse.AERecord calledAeMeta, callingAeMeta;
        boolean isPullForUnknownEnabled, isPushForUnknownEnabled;
        synchronized(UnvSCP.this.emsowClientAets) {
            calledAeMeta = UnvSCP.this.emsowServerAeMeta;
            callingAeMeta = UnvSCP.this.emsowClientAets.get(callingAet);
            isPullForUnknownEnabled = UnvSCP.this.isPullForUnknownEnabled;
            isPushForUnknownEnabled = UnvSCP.this.isPushForUnknownEnabled;
        }

        if(calledAeMeta == null) { // impossible in normal case
            calledAet = UnvWebClient.getCalledAetOverride(as.getCalledAET());
            throw new DicomServiceException(
                Status.UnableToProcess, "Server AET '" + calledAet + "' is missing in PACS Clients Module"
            );
        }
        calledAet = calledAeMeta.getAET();
        if (isPush && !calledAeMeta.isPushEnabled()) {
            throw new DicomServiceException(
                Status.UnableToProcess, "Pushing any data upon server with AET '" + calledAet + "' is not allowed"
            );
        }
        if (isPull && !calledAeMeta.isPullEnabled()) {
            throw new DicomServiceException(
                Status.UnableToProcess, "Pulling any data from server with AET '" + calledAet + "' is not allowed"
            );
        }

        if (callingAeMeta == null) {
            if (!calledAeMeta.isUnknownAllowed()) {
                throw new DicomServiceException(
                    Status.UnableToProcess, "Client AET '" + callingAet + "' is missing in PACS Clients Module"
                );
            }
            if (isPush && !isPushForUnknownEnabled) {
                throw new DicomServiceException(
                    Status.UnableToProcess, "Pushing any data from unknown clients is not allowed"
                );
            }
            if (isPull && !isPullForUnknownEnabled) {
                throw new DicomServiceException(
                    Status.UnableToProcess, "Pulling any data onto unknown clients is not allowed"
                );
            }
        } else {
            List<String> AllowedClients = calledAeMeta.getClients();
            boolean isClientAllowed = calledAeMeta.isUnknownAllowed()
                || AllowedClients == null || AllowedClients.contains(callingAeMeta.getID());
            if (isPush && !(callingAeMeta.isPushEnabled() && isClientAllowed)) {
                throw new DicomServiceException(
                    Status.UnableToProcess, "Pushing any data from client with AET '" + callingAet + "' is not allowed"
                );
            }
            if (isPull && !(callingAeMeta.isPullEnabled() && isClientAllowed)) {
                throw new DicomServiceException(
                    Status.UnableToProcess, "Pulling any data onto client with AET '" + callingAet + "' is not allowed"
                );
            }
        }
    }

    private void storeTo(Association as, Attributes fmi,
            PDVInputStream data, File file, String iuid) throws IOException  {
        LOG.info("{}: M-WRITE-BEGIN {}, UID={}", new Object[]{as, file, iuid});
        file.getParentFile().mkdirs();
        DicomOutputStream out = new DicomOutputStream(file);
        try {
            out.writeFileMetaInformation(fmi);
            data.copyTo(out);
        } finally {
            SafeClose.close(out);
        }
        LOG.info("{}: M-WRITE-END {}, UID={}", new Object[]{as, file, iuid});
    }

    private File getDestinationFile(Attributes attrs) {
        File file = new File(storageDir, filePathFormat.format(attrs));
        while (file.exists())
            file = new File(file.getParentFile(),
                    TagUtils.toHexString(new Random().nextInt()));
        return file;
    }

    private static void renameTo(Association as, File from, File dest)
            throws IOException {
        LOG.info("{}: M-RENAME {}", new Object[]{ as, from, dest });
        dest.getParentFile().mkdirs();
        if (!from.renameTo(dest))
            throw new IOException("Failed to rename " + from + " to " + dest);
    }

    private static Attributes parse(File file) throws IOException {
        DicomInputStream in = new DicomInputStream(file);
        try {
            in.setIncludeBulkData(IncludeBulkData.NO);
            return in.readDataset(-1, Tag.PixelData);
        } finally {
            SafeClose.close(in);
        }
    }


    private static void deleteFile(Association as, File file, String iuid) {
        if (file.delete())
            LOG.info("{}: M-DELETE {}, UID={}", new Object[]{as, file, iuid});
        else
            LOG.warn("{}: M-DELETE {} failed! UID={}", new Object[]{as, file, iuid});
    }

    private DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());

        if (isUrl)
            serviceRegistry.addDicomService(new CStoreWebSCPImpl());
        else
            serviceRegistry.addDicomService(new CStoreSCPImpl());

        serviceRegistry.addDicomService(new StgCmtSCPImpl());
        serviceRegistry.addDicomService(
                new CFindSCPImpl(
                        UID.PatientRootQueryRetrieveInformationModelFIND,
                        PATIENT_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CFindSCPImpl(
                        UID.StudyRootQueryRetrieveInformationModelFIND,
                        STUDY_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CFindSCPImpl(
                        UID.PatientStudyOnlyQueryRetrieveInformationModelFINDRetired,
                        PATIENT_STUDY_ONLY_LEVELS));
        serviceRegistry.addDicomService(
                new CFindSCPImpl(
                        UID.ModalityWorklistInformationModelFIND,
                        PATIENT_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CGetSCPImpl(
                        UID.PatientRootQueryRetrieveInformationModelGET,
                        PATIENT_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CGetSCPImpl(
                        UID.StudyRootQueryRetrieveInformationModelGET,
                        STUDY_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CGetSCPImpl(
                        UID.PatientStudyOnlyQueryRetrieveInformationModelGETRetired,
                        PATIENT_STUDY_ONLY_LEVELS));
        serviceRegistry.addDicomService(
                new CGetSCPImpl(
                        UID.CompositeInstanceRetrieveWithoutBulkDataGET));
        serviceRegistry.addDicomService(
                new CMoveSCPImpl(
                        UID.PatientRootQueryRetrieveInformationModelMOVE,
                        PATIENT_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CMoveSCPImpl(
                        UID.StudyRootQueryRetrieveInformationModelMOVE,
                        STUDY_ROOT_LEVELS));
        serviceRegistry.addDicomService(
                new CMoveSCPImpl(
                        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired,
                        PATIENT_STUDY_ONLY_LEVELS));
        return serviceRegistry ;
    }

    public final Device getDevice() {
        return device;
    }

    public final void setDicomDirectory(File dicomDir) {
        File storageDir = dicomDir.getParentFile();
        if (storageDir.mkdirs())
            System.out.println("M-WRITE " + storageDir);
        this.storageDir = storageDir;
        this.dicomDir = dicomDir;
    }

    public final void setDicomDirectory(URL dicomUrl, String username, String password, File workDir, File tmpDir, Boolean notConcurrent) {
        this.storageDir = workDir;
        this.tmpDir = tmpDir;
        this.dicomUrl = dicomUrl;
        this.emsowUsername = username;
        this.emsowPassword = password;
        this.notConcurrent = notConcurrent;
    }

    public final File getStorageDirectory() {
        return storageDir;
    }

    public final AttributesFormat getFilePathFormat() {
        return filePathFormat;
    }

    public void setFilePathFormat(String pattern) {
        this.filePathFormat = new AttributesFormat(pattern);
    }

    public final File getDicomDirectory() {
        return dicomDir;
    }

    public final URL getDicomURL() {
        return dicomUrl;
    }

    public boolean isWriteable() {
        if (!isUrl){
            return storageDir.canWrite();
        }else{
            return true;
        }
    }

    public final void setInstanceAvailability(String availability) {
        this.availability = availability;
    }

    public final String getInstanceAvailability() {
        return availability;
    }

    public boolean isStgCmtOnSameAssoc() {
        return stgCmtOnSameAssoc;
    }

    public void setStgCmtOnSameAssoc(boolean stgCmtOnSameAssoc) {
        this.stgCmtOnSameAssoc = stgCmtOnSameAssoc;
    }

    public final void setSendPendingCGet(boolean sendPendingCGet) {
        this.sendPendingCGet = sendPendingCGet;
    }

    public final boolean isSendPendingCGet() {
        return sendPendingCGet;
    }

    public final void setSendPendingCMoveInterval(int sendPendingCMoveInterval) {
        this.sendPendingCMoveInterval = sendPendingCMoveInterval;
    }

    public final int getSendPendingCMoveInterval() {
        return sendPendingCMoveInterval;
    }

    public final void setRecordFactory(RecordFactory recFact) {
        this.recFact = recFact;
    }

    public final RecordFactory getRecordFactory() {
        return recFact;
    }

    private static CommandLine parseComandLine(String[] args)
            throws ParseException {
        Options opts = new Options();
        CLIUtils.addFilesetInfoOptions(opts);
        CLIUtils.addBindServerOption(opts);
        CLIUtils.addConnectTimeoutOption(opts);
        CLIUtils.addAcceptTimeoutOption(opts);
        CLIUtils.addAEOptions(opts);
        CLIUtils.addCommonOptions(opts);
        CLIUtils.addResponseTimeoutOption(opts);
        addDicomDirOption(opts);
        addTransferCapabilityOptions(opts);
        addInstanceAvailabilityOption(opts);
        addStgCmtOptions(opts);
        addSendingPendingOptions(opts);
        addRemoteConnectionsOption(opts);
        addLogOptions(opts);
        addAsyncOptions(opts);
        addUploadDirOptions(opts);
        addCompressionOptions(opts);
        addPushOptions(opts);
        addBridgeOptions(opts);
        addSessionOptions(opts);
        return CLIUtils.parseComandLine(args, opts, rb, UnvSCP.class);
    }

    @SuppressWarnings("static-access")
    private static void addLogOptions(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("log-dir")
                .withDescription(rb.getString("log-dir"))
                .withLongOpt("log-dir")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("store-log")
                .withDescription(rb.getString("store-log"))
                .withLongOpt("store-log")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addAsyncOptions(Options opts) {
        opts.addOption(null, "async", false, rb.getString("asynchronous-mode"));
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("bad-files-dir")
                .withDescription(rb.getString("bad-files-dir"))
                .withLongOpt("bad-files-dir")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("interval")
                .withDescription(rb.getString("async-sleep-interval"))
                .withLongOpt("async-sleep-interval")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addUploadDirOptions(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("upl-dir")
                .withDescription(rb.getString("upl-dir"))
                .withLongOpt("upl-dir")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("upl-bad-files-dir")
                .withDescription(rb.getString("upl-bad-files-dir"))
                .withLongOpt("upl-bad-files-dir")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("upl-sleep-interval")
                .withDescription(rb.getString("upl-sleep-interval"))
                .withLongOpt("upl-sleep-interval")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("[aet[@ip]:]port")
                .withDescription(rb.getString("source-override"))
                .withLongOpt("source-override")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("days amount")
                .withDescription(rb.getString("store-upl-failures"))
                .withLongOpt("store-upl-failures")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addCompressionOptions(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("[0..9]")
                .withDescription(rb.getString("compression"))
                .withLongOpt("compression")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addPushOptions(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("PUT|POST")
                .withDescription(rb.getString("push-http-method"))
                .withLongOpt("push-http-method")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addInstanceAvailabilityOption(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("code")
                .withDescription(rb.getString("availability"))
                .withLongOpt("availability")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addBridgeOptions(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("[aet[@ip]]")
                .withDescription(rb.getString("destination-override"))
                .withLongOpt("destination-override")
                .create());
        opts.addOption(null, "gui", false, rb.getString("visual-interface"));
    }

    @SuppressWarnings("static-access")
    private static void addSessionOptions(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("str")
                .withDescription(rb.getString("emsow-session-name"))
                .withLongOpt("emsow-session-name")
                .create());
    }

    private static void addStgCmtOptions(Options opts) {
        opts.addOption(null, "stgcmt-same-assoc", false, rb.getString("stgcmt-same-assoc"));
    }

    @SuppressWarnings("static-access")
    private static void addSendingPendingOptions(Options opts) {
        opts.addOption(null, "pending-cget", false, rb.getString("pending-cget"));
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("s")
                .withDescription(rb.getString("pending-cmove"))
                .withLongOpt("pending-cmove")
                .create());
   }

    @SuppressWarnings("static-access")
    private static void addDicomDirOption(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file")
                .withDescription(rb.getString("dicomdir"))
                .withLongOpt("dicomdir")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("pattern")
                .withDescription(rb.getString("filepath"))
                .withLongOpt("filepath")
                .create(null));
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("emsow-url")
                .withDescription(rb.getString("emsow-url"))
                .withLongOpt("emsow-url")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("emsow-username")
                .withDescription(rb.getString("emsow-username"))
                .withLongOpt("emsow-username")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("emsow-password")
                .withDescription(rb.getString("emsow-password"))
                .withLongOpt("emsow-password")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("work-dir")
                .withDescription(rb.getString("work-dir"))
                .withLongOpt("work-dir")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file")
                .withDescription(rb.getString("tmp-dir"))
                .withLongOpt("tmp-dir")
                .create());
        opts.addOption(null, "not-concurrent", false, rb.getString("not-concurrent"));
    }

    @SuppressWarnings("static-access")
    private static void addTransferCapabilityOptions(Options opts) {
        opts.addOption(null, "all-storage", false, rb.getString("all-storage"));
        opts.addOption(null, "no-storage", false, rb.getString("no-storage"));
        opts.addOption(null, "no-query", false, rb.getString("no-query"));
        opts.addOption(null, "no-retrieve", false, rb.getString("no-retrieve"));
        opts.addOption(null, "relational", false, rb.getString("relational"));
        opts.addOption(null, "allow-unknown", false, rb.getString("allow-unknown"));
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file|url")
                .withDescription(rb.getString("storage-sop-classes"))
                .withLongOpt("storage-sop-classes")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file|url")
                .withDescription(rb.getString("query-sop-classes"))
                .withLongOpt("query-sop-classes")
                .create());
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file|url")
                .withDescription(rb.getString("retrieve-sop-classes"))
                .withLongOpt("retrieve-sop-classes")
                .create());
    }

    @SuppressWarnings("static-access")
    private static void addRemoteConnectionsOption(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("file|url")
                .withDescription(rb.getString("ae-config"))
                .withLongOpt("ae-config")
                .create());
     }

    public static void main(String[] args) {
        try {
            CommandLine cl = parseComandLine(args);
            final UnvSCP main = new UnvSCP(!cl.hasOption("dicomdir"));
            CLIUtils.configure(main.fsInfo, cl);
            CLIUtils.configureBindServer(main.conn, main.ae, cl);
            CLIUtils.configure(main.conn, cl);
            configureDicomFileSet(main, cl);
            configureLog(main, cl);
            configureAsyncMode(main, cl);
            configureManualUploading(main, cl);
            configureCompression(main, cl);
            configurePushMethod(main, cl);
            configureTransferCapability(main, cl);
            configureRoleSelectionsAndExtendedNegotiations(main, cl);
            configureInstanceAvailability(main, cl);
            configureStgCmt(main, cl);
            configureSendPending(main, cl);
            configureDestinationOverride(main, cl);
            configureSession(main, cl);
            configureRemoteConnections(main, cl);
            configureVisualInterface(main, cl);
            ExecutorService executorService = Executors.newCachedThreadPool();
            ScheduledExecutorService scheduledExecutorService =
                    Executors.newSingleThreadScheduledExecutor();
            main.device.setScheduledExecutor(scheduledExecutorService);
            main.device.setExecutor(executorService);
            main.device.bindConnections();

            main.device.setAssociationHandler(new AssociationHandler(){
                @Override
                protected AAssociateAC negotiate(Association as, AAssociateRQ rq) throws IOException {
                    //as.getCallingAET(); as.getRemoteAET(); // show the same result

                    LOG.info(
                        "{} >> A-ASSOCIATE-RQ {}",
                        "Association" + as.toString().replaceFirst("\\b[^+^-]*", ""),
                        (rq != null ? rq.getCallingAET() : "")
                    );

                    try {
                        UnvWebClient webClient = new UnvWebClient(main.dicomUrl.toString());
                        webClient.addListener(main, new DicomClientMetaInfo(as));
                        webClient.setBasicParams(main.emsowUsername, main.emsowPassword, "EMSOW-AUTH", as);

                        if (main.notConcurrent) {
                            synchronized(main) {
                                webClient.sendPostRequest();
                            }
                        } else {
                            webClient.sendPostRequest();
                        }

                        CAuthWebResponse cawr = webClient.parseJsonResponse(CAuthWebResponse.class);
                        Collection<CAuthWebResponse.AERecord> ae_client_list = cawr.getClientsData();
                        if (ae_client_list != null) {
                            synchronized(main.emsowClientAets) {
                                main.emsowServerAeMeta = cawr.getServerData();
                                main.emsowClientAets.clear();
                                for (CAuthWebResponse.AERecord aerec : ae_client_list) {
                                    main.emsowClientAets.put(aerec.getAET(), aerec);
                                }
                                main.isPullForUnknownEnabled = cawr.isPullForUnknownEnabled();
                                main.isPushForUnknownEnabled = cawr.isPushForUnknownEnabled();
                            }
                            if(UnvSCP.isDynamicAEList) {
                                synchronized(main.remoteConnections) {
                                    main.remoteConnections.clear();
                                    for (CAuthWebResponse.AERecord aerec : ae_client_list) {
                                        String host = (aerec.getHost() == null) ? "" : aerec.getHost();
                                        String port = (aerec.getHost() == null || "".equals(aerec.getPort().trim())) ? "104" : aerec.getPort();
                                        main.addRemoteConnection(aerec.getAET(), UnvSCP.createAllowedConnection(host + ":" + port));
                                    }
                                }
                            }
                        }
                    } catch(IOException ioe) {
                        throw new UnvAAssociateRJ(UnvAAssociateRJ.RESULT_REJECTED_PERMANENT,
                                                  UnvAAssociateRJ.SOURCE_SERVICE_USER,
                                                  UnvAAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED,
                                                  ioe.getMessage());
                    } catch(Exception e) {
                        throw new UnvAAssociateRJ(UnvAAssociateRJ.RESULT_REJECTED_PERMANENT,
                                                  UnvAAssociateRJ.SOURCE_SERVICE_USER,
                                                  UnvAAssociateRJ.REASON_USER_DEFINED,
                                                  e.getMessage());
                    }

                    AAssociateAC ac = super.negotiate(as, rq);
                    for (RoleSelection rs : main.roleSelections) {
                        ac.addRoleSelection(rs);
                    }
                    for (ExtendedNegotiation extNeg : main.extendedNegotiaions) {
                        ac.addExtendedNegotiation(extNeg);
                    }
                    return ac;
                }
            });

            if (main.async) {
                AsyncSenderTask asyncSenderTask = new AsyncSenderTask(
                    main.dicomUrl, main.emsowUsername, main.emsowPassword,
                    main.storageDir, main.tmpDir, main.badFilesDir, main.sleepTime,
                    main.notConcurrent, main.compressionLevel, main
                );
                asyncSenderTask.addSenderTaskListener(main.activityWindow);
                main.asyncSender = new Thread(asyncSenderTask, "ASYNC");
                main.asyncSender.setDaemon(true);
                main.asyncSender.start();
            }

            if (main.uplDir != null && main.uplBadFilesDir != null) {
                Properties params = UnvWebClient.getUploadParams(main.ae, main.conn);
                DcmFileSenderTask dcmFileSenderTask = new DcmFileSenderTask(
                    main.dicomUrl, main.emsowUsername, main.emsowPassword,
                    main.uplDir, main.tmpDir, main.uplBadFilesDir, main.uplSleepTime,
                    main.notConcurrent, main.compressionLevel, main, params
                );
                dcmFileSenderTask.addSenderTaskListener(main.activityWindow);
                main.dcmFileSender = new Thread(dcmFileSenderTask, "UPL");
                main.dcmFileSender.setDaemon(true);
                main.dcmFileSender.start();
            }
        } catch (ParseException e) {
            System.err.println("unvscp: " + e.getMessage());
            System.err.println(rb.getString("try"));
            System.exit(2);
        } catch (Exception e) {
            System.err.println("unvscp: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void configureAsyncMode(UnvSCP main, CommandLine cl) throws ParseException {
        if (cl.hasOption("async")) {
            if (!cl.hasOption("bad-files-dir")) {
                throw new MissingOptionException(rb.getString("missing-bad-files-dir"));
            }

            File badFilesDir = new File(cl.getOptionValue("bad-files-dir"));
            if (!badFilesDir.exists()) {
                throw new ParseException("bad-files-dir '" + badFilesDir.getPath()
                        + "' doesn't exist. Create the directory or change option --bad-files-dir");
            }
            if (!badFilesDir.isDirectory()) {
                throw new ParseException("bad-files-dir '" + badFilesDir.getPath() + "' is not a directory. Change option --bad-files-dir");
            }
            if (!badFilesDir.canRead()) {
                throw new ParseException("Can not read from bad-files-dir '" + badFilesDir.getPath() + "'. Check access permissions or change option --bad-files-dir");
            }
            if (!badFilesDir.canWrite()) {
                throw new ParseException("Can not write to bad-files-dir '" + badFilesDir.getPath() + "'. Check access permissions or change option --bad-files-dir");
            }

            main.async = true;
            main.badFilesDir = badFilesDir;
            try {
                main.sleepTime = Integer.parseInt(cl.getOptionValue("async-sleep-interval", "5"));
            } catch(NumberFormatException nfe) {
                throw new ParseException("\"" + cl.getOptionValue("async-sleep-interval") + "\" is not a valid integer value for --async-sleep-interval");
            }
        }
    }

    private static void configureManualUploading(UnvSCP main, CommandLine cl) throws ParseException {
        if (cl.hasOption("upl-dir")) {
            if (!cl.hasOption("upl-bad-files-dir")) {
                throw new MissingOptionException(rb.getString("missing-upl-bad-files"));
            }

            File uplDir = new File(cl.getOptionValue("upl-dir"));
            if (!uplDir.exists()) {
                throw new ParseException("upl-dir '" + uplDir.getPath()
                        + "' doesn't exist. Create the directory or change option --upl-dir");
            }
            if (!uplDir.isDirectory()) {
                throw new ParseException("upl-dir '" + uplDir.getPath() + "' is not a directory. Change option --upl-dir");
            }
            if (!uplDir.canRead()) {
                throw new ParseException("Can not read from upl-dir '" + uplDir.getPath() + "'. Check access permissions or change option --upl-dir");
            }
            if (!uplDir.canWrite()) {
                throw new ParseException("Can not write to upl-dir '" + uplDir.getPath() + "'. Check access permissions or change option --upl-dir");
            }

            File uplBadFilesDir = new File(cl.getOptionValue("upl-bad-files-dir"));
            if (!uplBadFilesDir.exists()) {
                throw new ParseException("upl-bad-files-dir '" + uplBadFilesDir.getPath()
                        + "' doesn't exist. Create the directory or change option --upl-bad-files-dir");
            }
            if (!uplBadFilesDir.isDirectory()) {
                throw new ParseException("upl-bad-files-dir '" + uplBadFilesDir.getPath() + "' is not a directory. Change option --upl-bad-files-dir");
            }
            if (!uplBadFilesDir.canRead()) {
                throw new ParseException("Can not read from upl-bad-files-dir '" + uplBadFilesDir.getPath() + "'. Check access permissions or change option --upl-bad-files-dir");
            }
            if (!uplBadFilesDir.canWrite()) {
                throw new ParseException("Can not write to upl-bad-files-dir '" + uplBadFilesDir.getPath() + "'. Check access permissions or change option --upl-bad-files-dir");
            }

            main.uplDir = uplDir;
            main.uplBadFilesDir = uplBadFilesDir;
            try {
                main.uplSleepTime = Integer.parseInt(cl.getOptionValue("upl-sleep-interval", "5"));
            } catch(NumberFormatException nfe) {
                throw new ParseException("\"" + cl.getOptionValue("upl-sleep-interval") + "\" is not a valid integer value for --upl-sleep-interval");
            }

            if (cl.hasOption("source-override")) {
                UnvWebClient.setSourceOverride(cl.getOptionValue("source-override"));
            }

            if (cl.hasOption("store-upl-failures")) {
                final Integer days;
                try {
                    days = Integer.parseInt(cl.getOptionValue("store-upl-failures"));
                } catch (NumberFormatException nfe) {
                    throw new ParseException("--store-upl-failures can not accept \"" + cl.getOptionValue("store-upl-failures") + "\" (only integer values are allowed)");
                }

                FileFilter filter = new FileFilter(){
                    private Date theDate = new Date();
                    {
                        Calendar c = Calendar.getInstance();
                        c.setTime(theDate);
                        c.add(Calendar.DATE, -1 * Math.abs(days));
                        theDate = c.getTime();
                    }

                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() || file.lastModified() < this.theDate.getTime();
                    }
                };

                doRecursiveCleanUp(uplBadFilesDir, true, filter);
            }
        }
    }

    public static void doRecursiveCleanUp(File dir, boolean saveRoot, FileFilter filter) {
        File[] children = dir.listFiles(filter);
        for (File child : children) {
            if (child.isDirectory()) {
                doRecursiveCleanUp(child, false, filter);
            } else {
                if (child.delete()) {
                    LOG.info("Old file {} has been removed", child.getName());
                } else {
                    LOG.warn("Can not delete file {}. The file may be in use or the system settings do not allow this operation", child.getName());
                }
            }
        }
        if (!saveRoot) {
            dir.delete();
        }
    }

    private static void configureCompression(UnvSCP main, CommandLine cl) throws ParseException {
        if (cl.hasOption("compression")) {
            try {
                Integer level = Integer.parseInt(cl.getOptionValue("compression"));
                main.compressionLevel = (level == 0 ? null : level);
            } catch (NumberFormatException nfe) {
                throw new ParseException("--compression can not accept \"" + cl.getOptionValue("compression") + "\" (only integer values are allowed)");
            }
        }
    }

    private static void configurePushMethod(UnvSCP main, CommandLine cl) throws ParseException {
        String method = null;
        if (cl.hasOption("push-http-method") && !"".equals(method = cl.getOptionValue("push-http-method"))) {
            if (HttpPost.METHOD_NAME.equalsIgnoreCase(method)) {
                UnvWebClient.setUploadingFilesHttpMethod(HttpPost.METHOD_NAME);
            } else if (HttpPut.METHOD_NAME.equalsIgnoreCase(method)) {
                UnvWebClient.setUploadingFilesHttpMethod(HttpPut.METHOD_NAME);
            } else {
                throw new ParseException("--push-http-method accepts only POST|PUT. Method \"" + method + "\" is not supported.");
            }
        } else {
            UnvWebClient.setUploadingFilesHttpMethod(HttpPut.METHOD_NAME);
        }

        LOG.info("Using http method \"{}\" for uploading files", UnvWebClient.getUploadingFilesHttpMethod());
    }

    private static void configureLog(UnvSCP main, CommandLine cl) throws ParseException, FileNotFoundException, IOException {
        File logDir = null;
        if (cl.hasOption("log-dir")) {
            logDir = new File(cl.getOptionValue("log-dir"));
            if (!logDir.exists()) {
                throw new ParseException("log-dir '" + logDir.getPath()
                        + "' doesn't exist. Create the directory or change option --log-dir");
            }
            if (!logDir.isDirectory()) {
                throw new ParseException("log-dir '" + logDir.getPath() + "' is not a directory. Change option --log-dir");
            }
            if (!logDir.canRead()) {
                throw new ParseException("Can not read from log-dir '" + logDir.getPath() + "'. Check access permissions or change option --log-dir");
            }
            if (!logDir.canWrite()) {
                throw new ParseException("Can not write to log-dir '" + logDir.getPath() + "'. Check access permissions or change option --log-dir");
            }

            Properties log4jProps = new Properties();
            InputStream configStream = main.getClass().getResourceAsStream( "/log4j.properties");
            log4jProps.load(configStream);
            configStream.close();

            log4jProps.setProperty("log4j.rootLogger", "INFO, stdout, file");
            //log4jProps.setProperty("log4j.appender.file", "org.apache.log4j.DailyRollingFileAppender");
            log4jProps.setProperty("log4j.appender.file", "org.apache.log4j.rolling.RollingFileAppender");
            log4jProps.setProperty("log4j.appender.file.File", new File(logDir, "latest.log").getPath());
            log4jProps.setProperty("log4j.appender.file.ImmediateFlush", "true");
            log4jProps.setProperty("log4j.appender.file.Append", "true");
            //log4jProps.setProperty("log4j.appender.file.DatePattern", "'-'yyyy-MM-dd-mm-ss'.log'");
            //log4jProps.setProperty("log4j.appender.file.MaxBackupIndex", "7");
            //log4jProps.setProperty("log4j.appender.file.triggeringPolicy", "org.apache.log4j.rolling.TimeBasedRollingPolicy");
            //log4jProps.setProperty("log4j.appender.file.triggeringPolicy.FileNamePattern", new File(logDir, "%d{yyyy-MM-dd-mm-ss}.log").getPath());
            log4jProps.setProperty("log4j.appender.file.RollingPolicy", "org.apache.log4j.rolling.TimeBasedRollingPolicy");
            log4jProps.setProperty("log4j.appender.file.RollingPolicy.FileNamePattern", new File(logDir, "%d{yyyy-MM-dd}.log").getPath());
            log4jProps.setProperty("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
            log4jProps.setProperty("log4j.appender.file.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");

            LogManager.resetConfiguration();
            PropertyConfigurator.configure(log4jProps);

        } else {
            if (cl.hasOption("store-log")) {
                throw new MissingOptionException(rb.getString("missing-log-dir"));
            }
        }

        if (cl.hasOption("store-log")) {
            final Integer days;
            try {
                days = Integer.parseInt(cl.getOptionValue("store-log"));
            } catch (NumberFormatException nfe) {
                throw new ParseException("--store-log can not accept \"" + cl.getOptionValue("store-log") + "\" (only integer values are allowed)");
            }

            // Temporary Solution for Log clean up
            FileFilter filter = new FileFilter(){
                private Date theDate = new Date();
                {
                    Calendar c = Calendar.getInstance();
                    c.setTime(theDate);
                    c.add(Calendar.DATE, -1 * Math.abs(days));
                    theDate = c.getTime();
                }

                @Override
                public boolean accept(File file) {
                    return file.isFile() && file.lastModified() < this.theDate.getTime()
                            && file.getName().matches("^\\d\\d\\d\\d-\\d\\d-\\d\\d\\.(?i)(log)$");
                }
            };

            File[] logFilesToRemove = logDir.listFiles(filter);
            if (logFilesToRemove.length > 0) {
                LOG.info("Cleaning up the log dir (There are {} old log files to remove)", logFilesToRemove.length);
            }
            for (File f : logFilesToRemove) {
                if (!f.delete()) {
                    LOG.warn("Can not delete log file {}. The file may be in use or the system settings do not allow this operation", f.getName());
                }
            }
        }
    }

    private static void configureDicomFileSet(UnvSCP main, CommandLine cl)
            throws ParseException, MalformedURLException, IOException {
        if (!cl.hasOption("dicomdir") && !cl.hasOption("emsow-url")) {
            throw new MissingOptionException(rb.getString("missing-dicomdir"));
        }
        if (cl.hasOption("dicomdir")){
            main.isUrl = false;
            main.setDicomDirectory(new File(cl.getOptionValue("dicomdir")));
        } else { //if emsow-url is specified
            if (!cl.hasOption("emsow-username") || !cl.hasOption("emsow-password")
                   || !cl.hasOption("work-dir") || !cl.hasOption("tmp-dir")) {
                throw new MissingOptionException(rb.getString("missing-emsow"));
            }
            main.isUrl = true;
            main.setDicomDirectory(
                new URL(cl.getOptionValue("emsow-url")),
                cl.getOptionValue("emsow-username"),
                cl.getOptionValue("emsow-password"),
                new File(cl.getOptionValue("work-dir")),
                new File(cl.getOptionValue("tmp-dir")),
                cl.hasOption("not-concurrent")
            );
        }
        main.setFilePathFormat(cl.getOptionValue("filepath",
            "DICOM/{0020000D,hash}/{0020000E,hash}/{00080018,hash}"));
        main.setRecordFactory(new RecordFactory());

        main.allowUnknown = cl.hasOption("allow-unknown");
    }

    private static void configureInstanceAvailability(UnvSCP main, CommandLine cl) {
        main.setInstanceAvailability(cl.getOptionValue("availability"));
    }

    private static void configureStgCmt(UnvSCP main, CommandLine cl) {
        main.setStgCmtOnSameAssoc(cl.hasOption("stgcmt-same-assoc"));
    }

    private static void configureSendPending(UnvSCP main, CommandLine cl) {
        main.setSendPendingCGet(cl.hasOption("pending-cget"));
        if (cl.hasOption("pending-cmove"))
                main.setSendPendingCMoveInterval(
                        Integer.parseInt(cl.getOptionValue("pending-cmove")));
    }

    private static void configureTransferCapability(UnvSCP main, CommandLine cl)
            throws IOException {
        ApplicationEntity ae = main.ae;
        EnumSet<QueryOption> queryOptions = cl.hasOption("relational")
                ? EnumSet.of(QueryOption.RELATIONAL)
                : EnumSet.noneOf(QueryOption.class);
        boolean storage = !cl.hasOption("no-storage") && main.isWriteable();
        if (storage && cl.hasOption("all-storage")) {
            TransferCapability tc = new TransferCapability(null,
                    "*",
                    TransferCapability.Role.SCP,
                    "*");
            tc.setQueryOptions(queryOptions);
            ae.addTransferCapability(tc);
        } else {
            ae.addTransferCapability(
                    new TransferCapability(null,
                            UID.VerificationSOPClass,
                            TransferCapability.Role.SCP,
                            UID.ImplicitVRLittleEndian));
            Properties storageSOPClasses = CLIUtils.loadProperties(
                    cl.getOptionValue("storage-sop-classes",
                            "resource:storage-sop-classes.properties"),
                    null);
            if (storage)
                addTransferCapabilities(ae, storageSOPClasses,
                        TransferCapability.Role.SCP, null);
            if (!cl.hasOption("no-retrieve")) {
                addTransferCapabilities(ae, storageSOPClasses,
                        TransferCapability.Role.SCU, null);
                Properties p = CLIUtils.loadProperties(
                        cl.getOptionValue("retrieve-sop-classes",
                                "resource:retrieve-sop-classes.properties"),
                        null);
                addTransferCapabilities(ae, p, TransferCapability.Role.SCP, queryOptions);
            }
            if (!cl.hasOption("no-query")) {
                Properties p = CLIUtils.loadProperties(
                        cl.getOptionValue("query-sop-classes",
                                "resource:query-sop-classes.properties"),
                        null);
                addTransferCapabilities(ae, p, TransferCapability.Role.SCP, queryOptions);
            }
        }
        if (storage && !main.isUrl)
            main.openDicomDir();
        else
            main.openDicomDirForReadOnly();
     }

    /**
     * Since we store the meta data in the db we should use the relational (not hierarchical) model
     * to check for missing attributes in the association request
     * for retrieving images by C-GET/C-MOVE or querying by C-FIND.<br>
     * Key "<b>--relational</b>" now has effect. Always use it when launching UnvSCP. <br><br>
     * Config file <b>storage-sop-classes.properties</b> is now also used for creating role selections
     * that enable to transfer specific sop classes via C-GET
     */
    private static void configureRoleSelectionsAndExtendedNegotiations(UnvSCP main, CommandLine cl) {
        for (TransferCapability tc : main.ae.getTransferCapabilitiesWithRole(TransferCapability.Role.SCP)) {
            EnumSet<QueryOption> queryOpts = tc.getQueryOptions();
            /**
             * if queryOpts == null then we are dealing with storage sop classes for which we have to create role selections,
             * otherwise it's about query or retrieve sop classes for which we have to create extended negotiations containing the queryOpts
             */
            if (queryOpts == null) {
                main.roleSelections.add(new RoleSelection(tc.getSopClass(), true, true));
            } else {
                if (!queryOpts.isEmpty()) {
                    main.extendedNegotiaions.add(
                        new ExtendedNegotiation(tc.getSopClass(), QueryOption.toExtendedNegotiationInformation(queryOpts))
                    );
                }
            }
        }
    }

    private static void configureDestinationOverride(UnvSCP main, CommandLine cl)
        throws ParseException {

        if (cl.hasOption("destination-override")) {
            /* delete?
            if (cl.getOptionValue("destination-override") == null) {
                throw new ParseException("--destination-override cannot be empty");
            }
            */

            if (!cl.hasOption("log-dir")
                || !cl.hasOption("store-log")) {
                throw new MissingOptionException(rb.getString("missing-bridge"));
            }
            UnvWebClient.setDestinationOverride(cl.getOptionValue("destination-override"));
        }
    }

    private static void configureSession(UnvSCP main, CommandLine cl) {
        setEmsowSessionName(cl);
    }

    public static void setEmsowSessionName(CommandLine cl) {
        if (!cl.hasOption("emsow-session-name") || "".equals(cl.getOptionValue("emsow-session-name").trim())) {
            UnvWebClient.setSessionName("bridge");
        } else {
            UnvWebClient.setSessionName(cl.getOptionValue("emsow-session-name"));
        }
    }

    private static void configureVisualInterface(UnvSCP main, CommandLine cl) {
        if (cl.hasOption("gui")) {
            main.activityWindow = ActivityWindow.launch();
        }
    }

    private static void addTransferCapabilities(ApplicationEntity ae,
            Properties p, TransferCapability.Role role,
            EnumSet<QueryOption> queryOptions) {
        for (String cuid : p.stringPropertyNames()) {
            String ts = p.getProperty(cuid);
            TransferCapability tc = new TransferCapability(null, cuid, role,
                    ts.equals("*")
                        ? new String[] { "*" }
                        : toUIDs(StringUtils.split(ts, ',')));
            tc.setQueryOptions(queryOptions);
            ae.addTransferCapability(tc);
        }
    }

    private static String[] toUIDs(String[] names) {
        String[] uids = new String[names.length];
        for (int i = 0; i < uids.length; i++)
            uids[i] = UID.forName(names[i].trim());
        return uids ;
    }

    private static void configureRemoteConnections(UnvSCP main, CommandLine cl)
            throws Exception {
        String file = cl.getOptionValue("ae-config");
        if (file == null) {
            file = "resource:ae.properties";
            UnvSCP.isDynamicAEList = true;
        }
        Properties aeConfig = CLIUtils.loadProperties(file, null);
        for (Map.Entry<Object, Object> entry : aeConfig.entrySet()) {
            String aet = (String) entry.getKey();
            String value = (String) entry.getValue();
            try {
                main.addRemoteConnection(aet, UnvSCP.createAllowedConnection(value));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid entry in " + file + ": " + aet + "=" + value);
            }
        }
    }

    /**
     *
     * @param value a String in the following Format: hostname:port[:cipher1[:...]]
     * <p><b>An example:</b><br>localhost:2762[:SSL_RSA_WITH_NULL_SHA[:TLS_RSA_WITH_AES_128_CBC_SHA[:TLS_RSA_WITH_3DES_EDE_CBC_SHA]]]</p>
     * @return a new Connection that should be later included in the list of allowed connections for C-MOVE operation
     */
    private static Connection createAllowedConnection(String value) {
        String[] hostPortCiphers = StringUtils.split(value, ':');
        String[] ciphers = new String[hostPortCiphers.length-2];
        System.arraycopy(hostPortCiphers, 2, ciphers, 0, ciphers.length);
        Connection remote = new Connection();
        remote.setHostname(hostPortCiphers[0]);
        remote.setPort(Integer.parseInt(hostPortCiphers[1]));
        remote.setTlsCipherSuites(ciphers);
        return remote;
    }

    final IDicomReader getDicomDirReader() {
         return ddReader;
    }

    final DicomDirWriter getDicomDirWriter() {
         return ddWriter.getDicomDirWriter();
    }

    private void openDicomDir() throws IOException {
        if (!dicomDir.exists())
            DicomDirWriter.createEmptyDirectory(dicomDir,
                    UIDUtils.createUIDIfNull(fsInfo.getFilesetUID()),
                    fsInfo.getFilesetID(),
                    fsInfo.getDescriptorFile(),
                    fsInfo.getDescriptorFileCharset());
        ddReader = ddWriter = DicomDirWriterWrapper.open(dicomDir);
    }

    private void openDicomDirForReadOnly() throws IOException {
        if (isUrl){
            ddReader = new DicomWebReader(dicomUrl);
        } else {
            ddReader = new DicomDirReaderWrapper(dicomDir);
        }
    }

    public void addRemoteConnection(String aet, Connection remote) {
        remoteConnections.put(aet, remote);
    }

    Connection getRemoteConnection(String dest) {
        return remoteConnections.get(dest);
    }

    @Override
    public synchronized void logMessage(DicomClientMetaInfo metaInfo, String command, Object message, String dialog) {
        Association as = metaInfo.getAssoc();
        Properties extraData = metaInfo.getExtraData();
        String threadName = !"main".equals(Thread.currentThread().getName()) ? " [" + Thread.currentThread().getName() + "]" : "";
        String assocData = (as != null ? as.toString() : (extraData != null ? extraData.getProperty("FROM_AET", "<UNKNOWN>") : "<UNKNOWN>") + "+0");
        LOG.info("{}{}: {} {} >> {}", new String[]{assocData, threadName, dialog, command, new Gson().toJson(message)});
    }

    @Override
    public synchronized void logMessage(DicomClientMetaInfo metaInfo, String command, Object message) {
        logMessage(metaInfo, command, message, "EMSOW-DIALOG");
    }

    @Override
    public synchronized void logMultilineMessage(DicomClientMetaInfo metaInfo, String command, String header, String body, String footer) {
        Association as = metaInfo.getAssoc();
        Properties extraData = metaInfo.getExtraData();
        String threadName = !"main".equals(Thread.currentThread().getName()) ? " [" + Thread.currentThread().getName() + "]" : "";
        String assocData = (as != null ? as.toString() : (extraData != null ? extraData.getProperty("FROM_AET", "<UNKNOWN>") : "<UNKNOWN>") + "+0");
        String multilineMsg = "=============== " + assocData + threadName + " " + header + " ===============\n"
                + (body.length() > 1000 ? body.substring(0, 1000) + " ... /TRUNCATED/ ...\n" : body + "\n")
                + "================ " + assocData + threadName + " " + footer + " ================";
                System.out.println(multilineMsg);
    }

    public List<InstanceLocator> calculateMatches(Attributes keys, Association as, String command) throws DicomServiceException {
        /*String[] patIDs = keys.getStrings(Tag.PatientID);
        String[] studyIUIDs = keys.getStrings(Tag.StudyInstanceUID);
        String[] seriesIUIDs = keys.getStrings(Tag.SeriesInstanceUID);
        String[] sopIUIDs = keys.getStrings(Tag.SOPInstanceUID);*/

        try {
            DicomClientMetaInfo metaInfo = new DicomClientMetaInfo(as);
            logMessage(metaInfo, command, new GenericWebResponse("1", "Retrieving file list from EMSOW..."));
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "COUNT");
            List<InstanceLocator> list = new ArrayList<InstanceLocator>();

            UnvWebClient webClient = new UnvWebClient(this.dicomUrl.toString());
            webClient.addListener(this, metaInfo);
            webClient.setBasicParams(this.emsowUsername, this.emsowPassword, command, as);
            webClient.setParam("KEYS", DicomAttribute.getAttributesAsList(keys));

            webClient.sendPostRequest();

            CMoveWebResponse cmwr = webClient.parseJsonResponse(CMoveWebResponse.class);

            this.dicomDownloadUrl = cmwr.getUrl();
            Collection<DicomFileWebData> data = cmwr.getData();
            boolean hasEmptyTsuidOnList = false;
            if (data != null) {
                for(DicomFileWebData d : data) {
                    String tsuid = d.getTransferSyntaxUID();
                    list.add(new InstanceLocator(d.getClassUID(), d.getInstanceUID(), tsuid, d.getFileName()));
                    hasEmptyTsuidOnList = hasEmptyTsuidOnList || (tsuid == null || "".equals(tsuid));
                }
            }

            if (hasEmptyTsuidOnList) {
                logMessage(metaInfo, command, new GenericWebResponse("0", "Some of the files have empty TransferSyntaxUID. They will not be retrieved"));
            }

            return list;

        } catch (IOException e) {
            throw new DicomServiceException(Status.UnableToCalculateNumberOfMatches, e);
        }
    }

    public Attributes calculateStorageCommitmentResult(String calledAET,
            Attributes actionInfo) throws DicomServiceException {
        Sequence requestSeq = actionInfo.getSequence(Tag.ReferencedSOPSequence);
        int size = requestSeq.size();
        String[] sopIUIDs = new String[size];
        Attributes eventInfo = new Attributes(6);
        eventInfo.setString(Tag.RetrieveAETitle, VR.AE, calledAET);
        eventInfo.setString(Tag.StorageMediaFileSetID, VR.SH, ddReader.getFileSetID());
        eventInfo.setString(Tag.StorageMediaFileSetUID, VR.SH, ddReader.getFileSetUID());
        eventInfo.setString(Tag.TransactionUID, VR.UI, actionInfo.getString(Tag.TransactionUID));
        Sequence successSeq = eventInfo.newSequence(Tag.ReferencedSOPSequence, size);
        Sequence failedSeq = eventInfo.newSequence(Tag.FailedSOPSequence, size);
        LinkedHashMap<String, String> map =
                new LinkedHashMap<String, String>(size * 4 / 3);
        for (int i = 0; i < sopIUIDs.length; i++) {
            Attributes item = requestSeq.get(i);
            map.put(sopIUIDs[i] = item.getString(Tag.ReferencedSOPInstanceUID),
                    item.getString(Tag.ReferencedSOPClassUID));
        }
        IDicomReader ddr = ddReader;
        try {
            Attributes patRec = ddr.findPatientRecord();
            while (patRec != null) {
                Attributes studyRec = ddr.findStudyRecord(patRec);
                while (studyRec != null) {
                    Attributes seriesRec = ddr.findSeriesRecord(studyRec);
                    while (seriesRec != null) {
                        Attributes instRec = ddr.findLowerInstanceRecord(seriesRec, true, sopIUIDs);
                        while (instRec != null) {
                            String iuid = instRec.getString(Tag.ReferencedSOPInstanceUIDInFile);
                            String cuid = map.remove(iuid);
                            if (cuid.equals(instRec.getString(Tag.ReferencedSOPClassUIDInFile)))
                                successSeq.add(refSOP(iuid, cuid, Status.Success));
                            else
                                failedSeq.add(refSOP(iuid, cuid, Status.ClassInstanceConflict));
                            instRec = ddr.findNextInstanceRecord(instRec, true, sopIUIDs);
                        }
                        seriesRec = ddr.findNextSeriesRecord(seriesRec);
                    }
                    studyRec = ddr.findNextStudyRecord(studyRec);
                }
                patRec = ddr.findNextPatientRecord(patRec);
            }
        } catch (IOException e) {
            LOG.info("Failed to M-READ " + dicomDir, e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            failedSeq.add(refSOP(entry.getKey(), entry.getValue(), Status.NoSuchObjectInstance));
        }
        if (failedSeq.isEmpty())
            eventInfo.remove(Tag.FailedSOPSequence);
        return eventInfo;
    }

    boolean addDicomDirRecords(Association as, Attributes ds, Attributes fmi,
            File f) throws IOException {
        DicomDirWriter ddWriter = getDicomDirWriter();
        RecordFactory recFact = getRecordFactory();
        String pid = ds.getString(Tag.PatientID, null);
        String styuid = ds.getString(Tag.StudyInstanceUID, null);
        String seruid = ds.getString(Tag.SeriesInstanceUID, null);
        String iuid = fmi.getString(Tag.MediaStorageSOPInstanceUID, null);
        if (pid == null)
            ds.setString(Tag.PatientID, VR.LO, pid = styuid);

        Attributes patRec = ddWriter.findPatientRecord(pid);
        if (patRec == null) {
            patRec = recFact.createRecord(RecordType.PATIENT, null,
                    ds, null, null);
            ddWriter.addRootDirectoryRecord(patRec);
        }
        Attributes studyRec = ddWriter.findStudyRecord(patRec, styuid);
        if (studyRec == null) {
            studyRec = recFact.createRecord(RecordType.STUDY, null,
                    ds, null, null);
            ddWriter.addLowerDirectoryRecord(patRec, studyRec);
        }
        Attributes seriesRec = ddWriter.findSeriesRecord(studyRec, seruid);
        if (seriesRec == null) {
            seriesRec = recFact.createRecord(RecordType.SERIES, null,
                    ds, null, null);
            ddWriter.addLowerDirectoryRecord(studyRec, seriesRec);
        }
        Attributes instRec =
                ddWriter.findLowerInstanceRecord(seriesRec, false, iuid);
        if (instRec != null)
            return false;

        instRec = recFact.createRecord(ds, fmi, ddWriter.toFileIDs(f));
        ddWriter.addLowerDirectoryRecord(seriesRec, instRec);
        ddWriter.commit();
        return true;
    }

    private static Attributes refSOP(String iuid, String cuid, int failureReason) {
        Attributes attrs = new Attributes(3);
        attrs.setString(Tag.ReferencedSOPClassUID, VR.UI, cuid);
        attrs.setString(Tag.ReferencedSOPInstanceUID, VR.UI, iuid);
        if (failureReason != Status.Success)
            attrs.setInt(Tag.FailureReason, VR.US, failureReason);
        return attrs ;
    }

}
