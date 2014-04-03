/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che.tool.unvscp.data;

import java.util.Collection;

public class CStoreWebResponse extends GenericWebResponse{

    public static final class ExtraMessage {
        private String success;
        private String msg;
        private String debug;
        private String debugLevel;
        private String type;

        public String getDebug() {
            return debug;
        }

        public void setDebug(String debug) {
            this.debug = debug;
        }

        public String getDebugLevel() {
            return debugLevel;
        }

        public void setDebugLevel(String debugLevel) {
            this.debugLevel = debugLevel;
        }

        public String getType() {
            return type;
        }

        public String getSuccess() {
            return success;
        }

        public void setSuccess(String success) {
            this.success = success;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

    }

    private Collection<ExtraMessage> extraMsg;

    @Override
    public Collection<ExtraMessage> getData(){
        return extraMsg;
    }
}