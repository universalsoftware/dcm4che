/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che.tool.unvscp.data;

import java.util.Collection;

/**
 *
 * @author Pavel Varzinov
 */
public class GenericWebResponse {
    private String success;
    private String msg;
    private String debugLevel = "0"; // 0=normal mode; 1=debug mode; 2=warning

    public GenericWebResponse() {}

    public GenericWebResponse(String success, String msg) {
        this.success = success;
        this.msg = msg;
    }

    public GenericWebResponse(String success, String msg, int debugLevel) {
        this.success = success;
        this.msg = msg;
        this.debugLevel = "" + debugLevel;
    }

    public static boolean parseBoolean(String strVal) {
        return (strVal != null && strVal.matches("^[\\s]*([+-]?[0]*[1-9]{1}[\\d]*|(?i)true|(?i)yes)[\\s]*$"));
    }

    public boolean getSuccess() {
        return parseBoolean(success);
    }

    public int getSuccessAsInt() {
        if (this.getSuccess()) return 1;
        else return 0;
    }

    public String getSuccessAsString() {
        return this.success;
    }

    public String getErrorMessage() {
        return this.msg;
    }

    public int getDebugLevel() {
        try {
            return Integer.parseInt(this.debugLevel);
        } catch (Exception e) {
            return 0;
        }
    }

    public Collection<? extends Object> getData(){
        return null;
    }
}
