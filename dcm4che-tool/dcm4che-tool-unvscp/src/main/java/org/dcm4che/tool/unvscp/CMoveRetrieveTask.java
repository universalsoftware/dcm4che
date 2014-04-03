package org.dcm4che.tool.unvscp;


import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.dcm4che.data.Attributes;
import org.dcm4che.io.DicomInputStream;
import org.dcm4che.net.Association;
import org.dcm4che.net.DataWriter;
import org.dcm4che.net.InputStreamDataWriter;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.InstanceLocator;
import org.dcm4che.tool.unvscp.data.GenericWebResponse;
import org.dcm4che.tool.unvscp.media.UnvWebClientListener;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
class CMoveRetrieveTask extends GenericRetrieveTask {
    public CMoveRetrieveTask(Service service, Association as,
            PresentationContext pc, Attributes rq, List<InstanceLocator> matches,
            URL downloadUrl, String username, String password, UnvWebClientListener listener) {
        super(service, as, pc, rq, matches, downloadUrl, username, password, listener);
    }

    @Override
    protected DataWriter createDataWriter(InstanceLocator inst, String tsuid) throws IOException {

        DicomInputStream in = retrieveFileStreamFromWeb(inst, tsuid);

        in.readFileMetaInformation();
        DataWriter dw =  new InputStreamDataWriter(in);
        this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new GenericWebResponse("1", "Parsing of file completed"));
        return dw;
    }

}
