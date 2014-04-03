
package org.dcm4che.tool.unvscp.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class CAuthWebResponse extends GenericWebResponse {
    public final class AERecord {
        private String pacsae_title;
        private String pacsae_host;
        private String pacsae_port;
        private String pacsae_type; // [client|server] client by default
        private String pacsae_pull;
        private String pacsae_push;
        private String pacsae_routes; // a string containing aets devided by comas
        private String pacsae_allow_unknown;

        public String getAET() { return this.pacsae_title; }
        public String getHost() { return this.pacsae_host; }
        public String getPort() { return this.pacsae_port; }
        public boolean isServer() {
            return (pacsae_type != null && pacsae_type.matches("^(?i)server$"));
        }
        public boolean isPullEnabled() {
            return CAuthWebResponse.parseBoolean(pacsae_pull);
        }
        public boolean isPushEnabled() {
            return CAuthWebResponse.parseBoolean(pacsae_push);
        }
        public List<String> getClients() {
            return (pacsae_routes == null || "".equals(pacsae_routes.trim())) ? null : Arrays.asList(pacsae_routes.split(","));
        }
        public boolean isUnknownAllowed() {
            return CAuthWebResponse.parseBoolean(pacsae_allow_unknown);
        }
    };

    private Collection<AERecord> aelist;
    private String pacsae_pull_for_unknown;
    private String pacsae_push_for_unknown;

    @Override
    public Collection<AERecord> getData() {
        return this.aelist;
    }

    public boolean isPullForUnknownEnabled() {
        return this.parseBoolean(pacsae_pull_for_unknown);
    }

    public boolean isPushForUnknownEnabled() {
        return this.parseBoolean(pacsae_push_for_unknown);
    }
}
