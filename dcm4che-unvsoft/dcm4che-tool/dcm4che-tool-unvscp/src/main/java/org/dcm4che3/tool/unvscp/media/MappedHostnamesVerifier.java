
package org.dcm4che3.tool.unvscp.media;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class MappedHostnamesVerifier implements X509HostnameVerifier{
    X509HostnameVerifier defaultVerifier = SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;
    private Properties hostMap = new Properties();
    private List<String> domainWhiteList = new ArrayList<String>();

    public MappedHostnamesVerifier() {}

    public MappedHostnamesVerifier(List<String> domainWhiteList) {
        if (domainWhiteList != null) {
            this.domainWhiteList = domainWhiteList;
        }
    }

    public void addMappedHost(String mappedHost, String defaultHost) {
        hostMap.setProperty(mappedHost, defaultHost);
    }

    @Override
    public boolean verify(String host, SSLSession ssls) {
        return defaultVerifier.verify(host, ssls);
    }

    @Override
    public void verify(String host, SSLSocket ssl) throws IOException {
        String principalName = ssl.getSession().getPeerPrincipal().getName();
        Pattern pattern = Pattern.compile("CN=[\\w\\.\\-]+");
        Matcher matcher = pattern.matcher(principalName);
        if (matcher.find()) {

            String commonName = matcher.group().substring(3); // the default certificate host name (the common name CN)
            if(commonName != null) {
                // We check hostName mapping first
                String strictlyMatchedHost = null, regexMatchedHost = null;
                for (Entry<Object, Object> hostMapEntry : this.hostMap.entrySet()) {
                    String key = (String)hostMapEntry.getKey();
                    if (host.equalsIgnoreCase(key)) {
                        strictlyMatchedHost = this.hostMap.getProperty(key);
                        break;
                    }
                    if (host.matches(key.replaceAll("\\*", "[\\\\w]+"))) {
                        regexMatchedHost = this.hostMap.getProperty(key);
                    }
                }
                String hostSubstitute = strictlyMatchedHost != null ? strictlyMatchedHost
                        : (regexMatchedHost != null ? regexMatchedHost : null);
                if (hostSubstitute != null && commonName.matches(hostSubstitute.replaceAll("\\*", "[\\\\w]+"))) {
                    host = commonName;
                }

                for (String s : this.domainWhiteList) {
                    if (commonName.matches(s.replaceAll("\\*", "[\\\\w]+"))) {
                        host = commonName;
                        break;
                    }
                }
            }
        }
        defaultVerifier.verify(host, ssl);
    }

    @Override
    public void verify(String host, X509Certificate cert) throws SSLException {
        defaultVerifier.verify(host, cert);
    }

    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        defaultVerifier.verify(host, cns, subjectAlts);
    }
}
