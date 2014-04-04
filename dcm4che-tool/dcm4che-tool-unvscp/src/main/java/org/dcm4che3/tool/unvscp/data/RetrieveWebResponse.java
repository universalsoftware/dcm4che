/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.data;

import org.dcm4che3.net.service.InstanceLocator;

/**
 *
 * @author Alexander
 */
public class RetrieveWebResponse extends GenericWebResponse{

    private String iuid;
    private String uri;

    public RetrieveWebResponse(String success, String msg, InstanceLocator inst) {
        super(success, msg);
        this.iuid = inst.iuid;
        this.uri = inst.uri;
    }
}
