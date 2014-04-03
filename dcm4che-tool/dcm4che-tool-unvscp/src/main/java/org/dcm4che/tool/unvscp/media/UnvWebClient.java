
package org.dcm4che.tool.unvscp.media;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Association;
import org.dcm4che.net.Connection;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class UnvWebClient extends GenericWebClient {

    protected static String hostOverride = "";
    protected static String hostNameOverride = "";
    protected static int portOverride;
    protected static String aetOverride = "";

    protected static String srcHostOverride = "";
    protected static String srcHostNameOverride = "";
    protected static int srcPortOverride;
    protected static String srcAetOverride = "";

    protected static String sessionName;

    public UnvWebClient(String uri) {
        super(uri);
    }

    public UnvWebClient(String uri, Properties params) {
        super(uri, params);
    }

    public static void setSessionName(String sessionName) {
        UnvWebClient.sessionName = sessionName;
    }

    public static Properties getAssocParams(Association as) {
        if (as != null) {
            Properties p = new Properties();
            p.setProperty("FROM_HOST",      as.getSocket().getInetAddress().getHostAddress());
            p.setProperty("FROM_HOST_NAME", as.getSocket().getInetAddress().getHostName());
            p.setProperty("FROM_PORT",      as.getSocket().getPort() + "");
            p.setProperty("FROM_AET",       as.getCallingAET());
            p.setProperty("TO_HOST",        getOverride(as.getSocket().getLocalAddress().getHostAddress(), UnvWebClient.hostOverride));
            p.setProperty("TO_HOST_NAME",   getOverride(as.getSocket().getLocalAddress().getHostName(), UnvWebClient.hostNameOverride));
            p.setProperty("TO_PORT",        getOverride(as.getSocket().getLocalPort() + "", UnvWebClient.portOverride + ""));
            p.setProperty("TO_AET",         getOverride(as.getCalledAET(), UnvWebClient.aetOverride));
            return p;
        } else {
            return null;
        }
    }

    public static Properties getUploadParams(ApplicationEntity ae, Connection conn) {
        if (ae != null && conn != null) {
            Properties p = new Properties();
            p.setProperty("FROM_HOST",      getOverride(conn.getServer().getInetAddress().getHostAddress(), UnvWebClient.srcHostOverride));
            p.setProperty("FROM_HOST_NAME", getOverride(conn.getServer().getInetAddress().getHostName(), UnvWebClient.srcHostNameOverride));
            p.setProperty("FROM_PORT",      getOverride(conn.getServer().getLocalPort() + "", UnvWebClient.srcPortOverride + ""));
            p.setProperty("FROM_AET",       getOverride(ae.getAETitle(), UnvWebClient.srcAetOverride));
            p.setProperty("TO_HOST",        getOverride(conn.getServer().getInetAddress().getHostAddress(), UnvWebClient.hostOverride));
            p.setProperty("TO_HOST_NAME",   getOverride(conn.getServer().getInetAddress().getHostName(), UnvWebClient.hostNameOverride));
            p.setProperty("TO_PORT",        getOverride(conn.getServer().getLocalPort() + "", UnvWebClient.portOverride + ""));
            p.setProperty("TO_AET",         getOverride(ae.getAETitle(), UnvWebClient.aetOverride));
            return p;
        } else {
            return null;
        }
    }

    public static String getCalledAetOverride(String aeTitle) {
        return getOverride(aeTitle, UnvWebClient.aetOverride);
    }

    public static String getCallingAetOverride(String aeTitle) {
        return getOverride(aeTitle, UnvWebClient.srcAetOverride);
    }

    public void setBasicParams(String username, String password, String serviceCommand, Association as) {
        if (username != null) this.setParam("EMSOW_INSTANT_LOGIN", username);
        if (password != null) this.setParam("EMSOW_INSTANT_PASS", password);
        Properties assocParams = UnvWebClient.getAssocParams(as);
        if (assocParams != null) {
            Set<Entry<Object, Object>> entries = assocParams.entrySet();
            for (Entry entry : entries) {
                this.setParam((String)entry.getKey(), (String)entry.getValue());
            }
        }
        if (serviceCommand != null) this.setParam("COMMAND", serviceCommand);

        this.setCookie("EMSOW_SESSION_NAME", UnvWebClient.sessionName);
    }

    protected static String getOverride(String value, String overrideValue){
        return overrideValue == null || "".equals(overrideValue) || "0".equals(overrideValue)
                ? value
                : overrideValue;
    }

    private static String[] split(String s, char delim, int defPos) {
        String[] s2 = new String[2];
        int pos = s.indexOf(delim);
        if (pos != -1) {
            s2[0] = s.substring(0, pos);
            s2[1] = s.substring(pos + 1);
        } else {
            s2[defPos] = s;
        }
        return s2;
    }


    private static String[] setOverride(String override) {
        String   aeAtHostPort     = override;
        String[] aeAtHostAndPort  = split(aeAtHostPort, ':', 0);
        String[] aeHost           = split(aeAtHostAndPort[0], '@', 0);

        // result[0] - AET; result[1] - HOST; result[2] - HOST NAME; result[3] - PORT
        String[] result = {"", "", "", ""};

        try {
            result[3] = aeAtHostAndPort[1];
        } catch(Exception e) {}

        try {
            result[0]  = aeHost[0];
        } catch(Exception e) {}

        try {
            String ipPattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                +"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
            Pattern pattern = Pattern.compile(ipPattern);
            Matcher matcher = pattern.matcher(aeHost[1]);

            if (matcher.matches()) {
                result[1] = aeHost[1];
            } else {
                result[2] = aeHost[1];
            }
        } catch(Exception e) {}

        return result;
    }

    public static void setSourceOverride(String sourceOverride) {
        String[] overridenValues = UnvWebClient.setOverride(sourceOverride);
        UnvWebClient.srcAetOverride = overridenValues[0];
        UnvWebClient.srcHostOverride = overridenValues[1];
        UnvWebClient.srcHostNameOverride = overridenValues[2];
        try {
            UnvWebClient.srcPortOverride = Integer.parseInt(overridenValues[3]);
        } catch(Exception e) {}
    }

    public static void setDestinationOverride(String destinationOverride) {
        String[] overridenValues = UnvWebClient.setOverride(destinationOverride);
        UnvWebClient.aetOverride = overridenValues[0];
        UnvWebClient.hostOverride = overridenValues[1];
        UnvWebClient.hostNameOverride = overridenValues[2];
        try {
            UnvWebClient.portOverride = Integer.parseInt(overridenValues[3]);
        } catch(Exception e) {}
    }
}
