
package org.dcm4che3.tool.unvscp;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomStreamException;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicRetrieveTask;
import org.dcm4che3.net.service.InstanceLocator;
import org.dcm4che3.tool.unvscp.data.GenericWebResponse;
import org.dcm4che3.tool.unvscp.data.RetrieveWebResponse;
import org.dcm4che3.tool.unvscp.media.DicomClientMetaInfo;
import org.dcm4che3.tool.unvscp.media.UnvWebClient;
import org.dcm4che3.tool.unvscp.media.UnvWebClientListener;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class GenericRetrieveTask extends BasicRetrieveTask {
    protected String serviceCommand = "";
    private URL downloadUrl;
    private String username, password;
    protected UnvWebClientListener webClientListener;
    protected DicomClientMetaInfo clientMetaInfo;

    public GenericRetrieveTask(Service service, Association as,
            PresentationContext pc, Attributes rq, List<InstanceLocator> matches,
            URL downloadUrl, String username, String password, UnvWebClientListener listener) {

        super(service, as, pc, rq, matches);

        if (Service.C_MOVE.equals(service)) {
            this.serviceCommand = "C-MOVE";
        }
        if (Service.C_GET.equals(service)) {
            this.serviceCommand = "C-GET";
        }

        this.downloadUrl = downloadUrl;
        this.username = username;
        this.password = password;
        this.webClientListener = listener;
        this.clientMetaInfo = new DicomClientMetaInfo(as);
    }

    @Override
    protected void cstore(Association storeas, InstanceLocator inst)
            throws IOException, InterruptedException {
        try {
            super.cstore(storeas, inst);
        } catch (DicomStreamException dse) {
            this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new RetrieveWebResponse("0", "Suboperation failed: " + dse.getMessage(), inst), "DICOM-CSTORE-ERROR [DicomStreamException]");
            throw dse;
        } catch (IOException ioe) {
            this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new RetrieveWebResponse("0", "Suboperation failed: " + ioe.getMessage(), inst), "DICOM-CSTORE-ERROR [IOException]");
            throw ioe;
        } catch (InterruptedException ie) {
            this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new RetrieveWebResponse("0", "Suboperation failed: " + ie.getMessage(), inst), "DICOM-CSTORE-ERROR [InterruptedException]");
            throw ie;
        } catch (RuntimeException re) {
            this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new RetrieveWebResponse("0", "Suboperation failed: " + re.getMessage(), inst), "DICOM-CSTORE-ERROR [RuntimeException]"); //storeas.getRemoteAET()
            throw re;
        }
    }

    protected DicomInputStream retrieveFileStreamFromWeb(InstanceLocator inst, String tsuid) throws IOException {
        if ("".equals(inst.tsuid)) {
            this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new GenericWebResponse("0", "Empty TransferSyntaxUID for file " + inst.uri + " - retrieving aborted"));
            throw new IOException("Empty TransferSyntaxUID for file " + inst.uri + " - retrieving aborted");
        }
        this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new GenericWebResponse("1", "Retrieving file from EMSOW: " + inst.uri));

        UnvWebClient webClient = new UnvWebClient(this.downloadUrl.toString());
        webClient.addListener(this.webClientListener, this.clientMetaInfo);
        webClient.setBasicParams(this.username, this.password, this.serviceCommand, this.as); //this.serviceCommand contains "C-MOVE" or "C-GET"
        webClient.setParam("pacsfile_name", inst.uri);

        webClient.sendPostRequest();

        GenericWebResponse gwr = new GenericWebResponse("1", "Parsing of file: name=" + inst.uri + "; MediaStorageSOPClassUID=" + inst.cuid + "; MediaStorageSOPInstanceUID=" + inst.iuid + "; TransferSyntaxUID=" + inst.tsuid);
        this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, gwr);

        return new DicomInputStream(webClient.getResponseInputStream());
    }
}
