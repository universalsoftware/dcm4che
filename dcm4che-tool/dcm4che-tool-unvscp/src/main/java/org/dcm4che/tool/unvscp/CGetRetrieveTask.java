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

package org.dcm4che.tool.unvscp;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.io.DicomInputStream;
import org.dcm4che.io.DicomInputStream.IncludeBulkData;
import org.dcm4che.net.Association;
import org.dcm4che.net.DataWriter;
import org.dcm4che.net.DataWriterAdapter;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.BasicRetrieveTask;
import org.dcm4che.net.service.InstanceLocator;
import org.dcm4che.tool.unvscp.data.GenericWebResponse;
import org.dcm4che.tool.unvscp.media.UnvWebClientListener;
import org.dcm4che.util.SafeClose;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com> Pavel Varzinov <varzinov@yandex.ru>
 */
class CGetRetrieveTask extends GenericRetrieveTask {

    private final boolean withoutBulkData;

    public CGetRetrieveTask(Association as, PresentationContext pc, Attributes rq, List<InstanceLocator> matches,
            URL downloadUrl, String username, String password, UnvWebClientListener listener, boolean withoutBulkData) {
        super(BasicRetrieveTask.Service.C_GET, as, pc, rq, matches, downloadUrl, username, password, listener);
        this.withoutBulkData = withoutBulkData;
    }

    @Override
    protected DataWriter createDataWriter(InstanceLocator inst, String tsuid)
            throws IOException {
        Attributes attrs;

        DicomInputStream in = retrieveFileStreamFromWeb(inst, tsuid);
        try {
            if (withoutBulkData) {
                in.setIncludeBulkData(IncludeBulkData.NO);
                attrs = in.readDataset(-1, Tag.PixelData);
            } else {
                in.setIncludeBulkData(IncludeBulkData.URI);
                attrs = in.readDataset(-1, -1);
            }
        } finally {
            SafeClose.close(in);
        }

        DataWriterAdapter dwa = new DataWriterAdapter(attrs);
        this.webClientListener.logMessage(this.clientMetaInfo, this.serviceCommand, new GenericWebResponse("1", "Parsing of file completed"));
        return dwa;
    }

 }
