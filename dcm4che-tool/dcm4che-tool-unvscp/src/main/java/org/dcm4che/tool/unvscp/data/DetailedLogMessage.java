/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che.tool.unvscp.data;

import java.util.HashMap;

/**
 *
 * @author Pavel Varzinov
 */
public class DetailedLogMessage extends GenericWebResponse {
    private String uri;
    HashMap<String, Object> params;
    public DetailedLogMessage(String success, String msg, String uri, HashMap<String, Object> params) {
        super(success, msg);
        this.uri = uri;
        this.params = params;
    }
}
