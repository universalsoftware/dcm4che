
package org.dcm4che3.tool.unvscp.media;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.tool.unvscp.data.DetailedLogMessage;
import org.dcm4che3.tool.unvscp.data.GenericWebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class GenericWebClient {
    protected static final Logger LOG = LoggerFactory.getLogger(GenericWebClient.class);

    private static String uploadingFilesHttpMethod = HttpPost.METHOD_NAME;
    public static void setUploadingFilesHttpMethod(String method) {
        if (HttpPost.METHOD_NAME.equalsIgnoreCase(method)) {
            uploadingFilesHttpMethod = HttpPost.METHOD_NAME;
        } else if (HttpPut.METHOD_NAME.equalsIgnoreCase(method)) {
            uploadingFilesHttpMethod = HttpPut.METHOD_NAME;
        } else {
            throw new RuntimeException("\"" + (method == null ? "null" : method) + "\" requests are not supported by this application");
        }
    }
    public static String getUploadingFilesHttpMethod() {
        return uploadingFilesHttpMethod;
    }

    private CloseableHttpClient httpClient;
    private CookieStore basicCookieStore = new BasicCookieStore();

    private String uri;
    private HashMap<String, Object> params = new HashMap<String, Object>();
    private HashMap<UnvWebClientListener, DicomClientMetaInfo> listeners = new HashMap<UnvWebClientListener, DicomClientMetaInfo>();
    private HashMap<String, File> files = new HashMap<String, File>();

    private String responseBody;
    private InputStream responseInputStream;
    private String command;

    private int connectionAttemptCount = 1;

    private boolean wrapConnectExceptions = true;

    public GenericWebClient(String uri) {
        this(uri, null);
    }

    public GenericWebClient(String uri, Properties params) {
        this.uri = uri;
        setParams(params);

        /* Using of a customized host name verifier
        List<String> domainWhiteList;
        MappedHostnamesVerifier mhv = new MappedHostnamesVerifier(GenericWebClient.domainWhiteList);
        SSLContext sslContext = SSLContexts.createDefault();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, mhv);
        */

        httpClient = HttpClients.custom()/*.setSSLSocketFactory(csf)*/.setDefaultCookieStore(basicCookieStore).build();
    }

    public void closeConnection() throws IOException {
        httpClient.close();
    }

    public void setConnectionAttemptCount(int count) {
        this.connectionAttemptCount = (count < 1) ? 1 : count;
    }

    public void disableConnectExceptionWrapping() {
        this.wrapConnectExceptions = false;
    }

    public void setParams(Properties params) {
        if (params != null) {
            Set<String> keys = params.stringPropertyNames();
            for(String key : keys) {
                setParam(key, params.getProperty(key));
            }
        }
    }

    //TODO check if params are replaced
    public void setParam(String key, Object value) {
        if (key == null) throw new NullPointerException("key cannot be null in setParam");
        this.params.put(key, value);
        if ("COMMAND".equalsIgnoreCase(key)) this.command = value.toString();
    }

    public void clearParams() {
        this.params.clear();
    }

    //TODO make sure to replace values of existing cookies
    public void setCookie(String name, String value) {
        try {
            URI uriInstance = new URI(uri);
            BasicClientCookie cookie = new BasicClientCookie(name , value);
            cookie.setDomain(uriInstance.getHost());
            cookie.setPath(uriInstance.getPath());
            basicCookieStore.addCookie(cookie);
        } catch(URISyntaxException use) {}
    }

    public void attachFile(String key, File file) {
        if (key == null) throw new NullPointerException("key cannot be null in attachFile");
        files.put(key, file);
    }

    private void sendRequest(String uri, HttpEntity entity) throws IOException {
        sendRequest(uri, entity, HttpPost.METHOD_NAME);
    }

    private void sendRequest(String uri, HttpEntity entity, String methodName) throws IOException {

        this.responseBody = null;
        if (this.responseInputStream != null) {
            try {
                this.responseInputStream.close();
            } catch(IOException ioe) {
            } finally {
                this.responseInputStream = null;
            }
        }

        HttpEntityEnclosingRequestBase httpRequest;
        if (HttpPost.METHOD_NAME.equalsIgnoreCase(methodName)) {
            httpRequest = new HttpPost(uri);
        } else if (HttpPut.METHOD_NAME.equalsIgnoreCase(methodName)) {
            httpRequest = new HttpPut(uri);
        } else {
            throw new RuntimeException("\"" + methodName + "\" requests are not supported by this application");
        }

        httpRequest.setEntity(entity);
        notifyListeners(new DetailedLogMessage("1", "Sending " + httpRequest.getMethod() + " request", uri, params));
        HttpResponse response = this.executeHttpRequest(httpClient, httpRequest);

        int statusCode = response.getStatusLine().getStatusCode();
        Header location = response.getLastHeader("Location");

        if (statusCode >= 300 && statusCode < 400 && location != null) {
            notifyListeners(new GenericWebResponse("1", "Response received: " + response.getStatusLine(), 1));
            notifyListeners(new GenericWebResponse("1", "Request redirected to " + location.getValue(), 1));

            response.getEntity().getContent().close(); // We use this instead of httpPost.reset() to keep the connection alive
            if (HttpPost.METHOD_NAME.equalsIgnoreCase(methodName)) {
                httpRequest = new HttpPost(location.getValue());
            } else if (HttpPut.METHOD_NAME.equalsIgnoreCase(methodName)) {
                httpRequest = new HttpPut(location.getValue());
            } else {
                throw new RuntimeException("\"" + methodName + "\" requests are not supported by this application");
            }

            httpRequest.setEntity(entity);
            response = this.executeHttpRequest(httpClient, httpRequest);

            statusCode = response.getStatusLine().getStatusCode();
        }

        if (statusCode >= 200 && statusCode < 300) {
            notifyListeners(new GenericWebResponse("1", "Response received: " + response.getStatusLine(), 1));
        } else {
            String responseText = EntityUtils.toString(response.getEntity());
            responseText = responseText != null ? responseText : "";
            responseText = responseText.length() > 200 ? responseText.substring(0, 200) : responseText;
            throw new DicomServiceException(Status.UnableToProcess, "Unacceptable response: " + response.getStatusLine() + "; " + responseText);
        }

        HttpEntity responseEntity = response.getEntity();

        String contentType = responseEntity.getContentType().getValue();
        long contentLength = responseEntity.getContentLength();
        notifyListeners(new GenericWebResponse("1", "Response content type: " + contentType + "; Response content length: " + contentLength, 1));
        if ("application/octet-stream".equalsIgnoreCase(contentType)
            || "application/download".equalsIgnoreCase(contentType)
            || "application/dicom".equalsIgnoreCase(contentType)) {
            // TODO Check if
            this.responseInputStream = responseEntity.getContent();
        } else {
            this.responseBody = EntityUtils.toString(responseEntity);
        }

        if (this.responseBody != null) notifyListeners("WEB-RESPONSE BODY BEGIN", this.responseBody, "WEB-RESPONSE BODY END");
    }

    private HttpResponse executeHttpRequest(HttpClient httpClient, HttpEntityEnclosingRequestBase httpRequest) throws IOException {
        HttpResponse response = null;
        for (int i = this.connectionAttemptCount; i > 0; i--) {
            try {
                response = httpClient.execute(httpRequest);
                break;
            } catch(IOException ioe) {
                if ((ioe instanceof HttpHostConnectException || ioe instanceof SocketException) && i > 1) {
                    notifyListeners(new GenericWebResponse("0", ioe.toString()));
                    // Sleep interval may be inserted here
                    int attemptNumber = this.connectionAttemptCount - i + 2;
                    notifyListeners(new DetailedLogMessage("1", "Sending " + httpRequest.getMethod() + " request (attempt " + attemptNumber + ")",
                            httpRequest.getURI().toString(), params));
                } else {
                    throw ioe;
                }
            }
        }
        return response;
    }

    public void sendPostRequest() throws IOException {
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();

            Set<Entry<String, Object>> paramSet = this.params.entrySet();
            for (Entry<String, Object> param : paramSet) {
                Object value = param.getValue();
                if (value instanceof String || value instanceof Number || value instanceof Character || value instanceof Boolean) {
                    params.add(new BasicNameValuePair(param.getKey(), value.toString()));
                } else {
                    params.add(new BasicNameValuePair(param.getKey(), new Gson().toJson(value)));
                }
            }

            sendRequest(this.uri, new UrlEncodedFormEntity(params));
        } catch(DicomServiceException dse) {
            notifyListeners(new GenericWebResponse("0", dse.toString()));
            throw dse;
        } catch(IOException ioe) {
            notifyListeners(new GenericWebResponse("0", ioe.toString()));
            if ((ioe instanceof HttpHostConnectException || ioe instanceof SocketException) && !this.wrapConnectExceptions) {
                throw ioe;
            } else {
                throw new DicomServiceException(Status.UnableToProcess, ioe.toString());
            }
        } catch (Exception e) {
            notifyListeners(new GenericWebResponse("0", e.toString()));
            throw new DicomServiceException(Status.UnableToProcess, e.toString());
        }
    }

    public void uploadFiles() throws IOException {
        uploadFiles(uploadingFilesHttpMethod);
    }

    public void uploadFiles(String methodName) throws IOException {
        try {
            HttpEntity httpEntity = null;
            if (HttpPut.METHOD_NAME.equalsIgnoreCase(methodName)) {
                httpEntity = prepareUnvSoftFileStreamEntity();
            } else if (HttpPost.METHOD_NAME.equalsIgnoreCase(methodName)) {
                httpEntity = prepareMultipartFileEntity();
            }
            sendRequest(this.uri, httpEntity, methodName);
        } catch(DicomServiceException dse) {
            notifyListeners(new GenericWebResponse("0", dse.toString()));
            throw dse;
        } catch(IOException ioe) {
            notifyListeners(new GenericWebResponse("0", ioe.toString()));
            if ((ioe instanceof HttpHostConnectException || ioe instanceof SocketException) && !this.wrapConnectExceptions) {
                throw ioe;
            } else {
                throw new DicomServiceException(Status.UnableToProcess, ioe.toString());
            }
        } catch(Exception e) {
            notifyListeners(new GenericWebResponse("0", e.toString()));
            throw new DicomServiceException(Status.UnableToProcess, e.toString());
        }
    }

    private HttpEntity prepareUnvSoftFileStreamEntity() {
        if (this.files.isEmpty()) {
            throw new RuntimeException("No files for uploading");
        }

        UnvSoftFileStreamEntityBuilder usBuilder = UnvSoftFileStreamEntityBuilder.create();

        Set<Entry<String, Object>> paramSet = params.entrySet();
        for (Entry<String, Object> param : paramSet) {
            Object value = param.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Character || value instanceof Boolean) {
                usBuilder.addTextHeader(param.getKey(), value.toString());
            } else {
                usBuilder.addTextHeader(param.getKey(), new Gson().toJson(value));
            }
        }

        Set<Entry<String, File>> fileSet = files.entrySet();
        for(Entry<String, File> attachment : fileSet) {
            usBuilder.addFileParam(attachment.getKey(), attachment.getValue());
        }

        return usBuilder.build();
    }

    private HttpEntity prepareMultipartFileEntity() {
        if (this.files.isEmpty()) {
            throw new RuntimeException("No files for uploading");
        }

        MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        Set<Entry<String, Object>> paramSet = params.entrySet();
        for (Entry<String, Object> param : paramSet) {
            Object value = param.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Character || value instanceof Boolean) {
                meBuilder.addTextBody(param.getKey(), value.toString());
            } else {
                meBuilder.addTextBody(param.getKey(), new Gson().toJson(value));
            }
        }

        Set<Entry<String, File>> fileSet = files.entrySet();
        for(Entry<String, File> attachment : fileSet) {
            meBuilder.addBinaryBody(attachment.getKey(), attachment.getValue());
        }

        return meBuilder.build();
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public InputStream getResponseInputStream() {
         return this.responseInputStream;
    }

    public <T extends GenericWebResponse> T parseJsonResponse(Class<T> classOfT) throws IOException {
        try {
            notifyListeners(new GenericWebResponse("1", "Trying to parse JSON...", 1));
            T result = new Gson().fromJson(this.responseBody, classOfT);

            String successStateText = (result.getSuccessAsString() == null ? "success is null" : "success=" + result.getSuccessAsString());

            String msg = result.getErrorMessage();
            if (msg != null) {
                msg = msg.trim();
            }
            boolean isEmptyMsg = (msg == null || "".equals(msg));

            String msgStateText = isEmptyMsg ? "msg is empty" : "msg is not empty";

            String dataStateText = "";
            if (result.getData() != null) {
                dataStateText = "data contains " + result.getData().size() + " records";
            } else {
                dataStateText = "data is null";
            }
            notifyListeners(new GenericWebResponse("1", "JSON response successfully parsed: " + successStateText + ", "
                                                   + msgStateText + ", " + dataStateText, 1));

            if (!result.getSuccess()) {
                String err = isEmptyMsg ? "Error is not specified by msg param" : msg;
                notifyListeners(new GenericWebResponse("0", err));
                throw new DicomServiceException(Status.UnableToProcess, err);
            } else {
                if (!isEmptyMsg) {
                    notifyListeners(new GenericWebResponse("1", msg, result.getDebugLevel()));
                }
            }
            return result;
        } catch(JsonSyntaxException jse) {
            notifyListeners(new GenericWebResponse("0", "Wrong JSON or probably not a JSON"));
            throw new DicomServiceException(Status.UnableToProcess, "Wrong JSON or probably not a JSON");
        } catch(RuntimeException re) {
            notifyListeners(new GenericWebResponse("0", re.toString()));
            throw new DicomServiceException(Status.UnableToProcess, re.toString());
        }
    }

    public void addListener(UnvWebClientListener l, DicomClientMetaInfo metaInfo) {
        if (l == null) {
            throw new NullPointerException("UnvWebClientListener cannot be null in addListener");
        }
        if (metaInfo == null) {
            throw new NullPointerException("DicomClientMetaInfo cannot be null in addListener");
        }
        listeners.put(l, metaInfo);
    }

    public void removeListener(UnvWebClientListener l) {
        listeners.remove(l);
    }

    private void notifyListeners(GenericWebResponse message) {
        Set<Entry<UnvWebClientListener, DicomClientMetaInfo>> listenerSet = listeners.entrySet();
        for(Entry<UnvWebClientListener, DicomClientMetaInfo> l : listenerSet) {
            l.getKey().logMessage(l.getValue(), this.command, message);
        }
    }

    private void notifyListeners (String header, String body, String footer) {
        Set<Entry<UnvWebClientListener, DicomClientMetaInfo>> listenerSet = listeners.entrySet();
        for(Entry<UnvWebClientListener, DicomClientMetaInfo> l : listenerSet) {
            l.getKey().logMultilineMessage(l.getValue(), this.command, header, body, footer);
        }
    }
}
