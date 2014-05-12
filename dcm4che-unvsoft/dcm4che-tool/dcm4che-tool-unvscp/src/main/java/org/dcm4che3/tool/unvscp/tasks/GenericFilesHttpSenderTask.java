
package org.dcm4che3.tool.unvscp.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import org.dcm4che3.tool.unvscp.data.CStoreWebResponse;
import org.dcm4che3.tool.unvscp.data.DicomFileDataPresentation;
import org.dcm4che3.tool.unvscp.media.DicomClientMetaInfo;
import org.dcm4che3.tool.unvscp.media.UnvWebClient;
import org.dcm4che3.tool.unvscp.media.UnvWebClientListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public abstract class GenericFilesHttpSenderTask implements Runnable {
    protected static final Logger LOG = LoggerFactory.getLogger(GenericFilesHttpSenderTask.class);
    private int sleepTime = 5; // sleep time in seconds
    private URL emsowUrl;
    private String emsowUsername, emsowPassword;
    protected File queueDir, tmpDir, badFilesDir;
    protected UnvWebClientListener cnListener;
    private boolean notConcurrent;
    protected Integer compressionLevel;
    private String xzCmd = "xz";
    protected Locale enUSLocale = new Locale("en", "US");

    private UnvWebClient webClient;
    protected List<SenderTaskListener> senderTaskListeners = new ArrayList<SenderTaskListener>();

    public abstract void sendFile(File dcmFile, Integer compressionLevel,
            boolean notConcurrent, UnvWebClientListener cnListener) throws Exception;

    public abstract Properties getFileMeta(File dcmFile);

    public GenericFilesHttpSenderTask(URL emsowUrl, String emsowUsername, String emsowPassword,
            File queueDir, File tmpDir, File badFilesDir, int sleepTime, boolean notConcurrent,
            Integer compressionLevel, UnvWebClientListener cnListener) {

        String osName = System.getProperty("os.name");
        if (osName.matches("(?i).*\\bwin(dows)?\\b.*")) {
            xzCmd = "bin" + File.separator + "xz";
        }

        this.emsowUrl = emsowUrl;
        this.emsowUsername = emsowUsername;
        this.emsowPassword = emsowPassword;
        this.queueDir = queueDir;
        this.tmpDir = tmpDir;
        this.badFilesDir = badFilesDir;
        if (sleepTime != 0) {
            this.sleepTime = sleepTime;
        }
        this.notConcurrent = notConcurrent;
        this.compressionLevel = (compressionLevel != null && compressionLevel > 9) ? (Integer)9 : compressionLevel;
        this.cnListener = cnListener;
    }

    @Override
    public final void run() {
        LOG.info("Launching \"{}\" as a {}, pause duration {} sec",
                new Object[] {
                    Thread.currentThread().getName(),
                    (Thread.currentThread().isDaemon() ? "daemon" : "regular thread"),
                    this.sleepTime
                });

        while (true) {
            File[] dcmFiles = this.selectFiles(this.queueDir);

            Map<File, String> filteredFiles = filterDcmFiles(dcmFiles);

            if (filteredFiles.size() > 0) {
                initUnvWebConnection();
                for (Entry<File, String> fileEntry : filteredFiles.entrySet()) {
                    File f = fileEntry.getKey();
                    String sopInstanceUid = fileEntry.getValue();

                    for (SenderTaskListener stl : senderTaskListeners) {
                        stl.onStartProcessingFile(sopInstanceUid);
                    }

                    if (isFileInUse(f)) {
                        LOG.warn("File {} is in use", f.getName());
                        continue;
                    }

                    String errMsg = null;
                    try {
                        sendFile(f, compressionLevel, this.notConcurrent, this.cnListener);
                    } catch(Exception e) {
                        errMsg = e.toString();
                    }

                    for (SenderTaskListener stl : senderTaskListeners) {
                        stl.onFinishProcessingFile(sopInstanceUid, errMsg);
                    }
                }

                try {
                    closeUnvWebConnection();
                    cleanUp();
                } catch(Exception ignore) {}
            }

            if (filteredFiles.isEmpty()) {
                for (SenderTaskListener stl : senderTaskListeners) {
                    stl.onFinishProcessingBatch();
                }
            }

            try {
                synchronized(this) {
                    this.wait(this.sleepTime * 1000);
                }
            } catch (InterruptedException ie) {}
        }
    }

    protected void initUnvWebConnection() {
        webClient = new UnvWebClient(this.emsowUrl.toString());
        webClient.disableConnectExceptionWrapping();
    }

    protected void closeUnvWebConnection() throws IOException {
        webClient.closeConnection();
    }

    protected File compressFile(File f) throws Exception {
        Process xz = null;
        File compressedFile = null;
        try {
            /*xz =new ProcessBuilder(new String[]{xzCmd, "-V"}).start();
            xz.waitFor();
            int exitCode = xz.exitValue();
            if (exitCode == 0) {
                BufferedReader xzStdOut = new BufferedReader(new InputStreamReader(xz.getInputStream()));
                String versionMsg = "";
                String line;
                for (int i = 0; i < 2 && (line = xzStdOut.readLine()) != null; i++) {
                    versionMsg += (i > 0 ? ", " : "") + line;
                }
                xzStdOut.close();
                logInfo("".equals(versionMsg) ? "version unknown" : versionMsg);
            } else {
                BufferedReader xzStdErr = new BufferedReader(new InputStreamReader(xz.getErrorStream()));
                String firstStdErrLine = xzStdErr.readLine();
                xzStdErr.close();
                throw new RuntimeException("archiver exit code '" + exitCode + "'"
                        + (firstStdErrLine != null ? " (" + firstStdErrLine + ")" : ""));
            }
            xz.destroy();*/

            File xzTempFile = new File(this.tmpDir, UUID.randomUUID().toString() + ".xz");
            xz = new ProcessBuilder(new String[]{
                xzCmd,
                "--compress", "--keep", "--force", "--stdout",
                (this.compressionLevel == null ? "" : "-" + this.compressionLevel),
                f.getAbsolutePath()
            }).redirectOutput(xzTempFile).start();

            xz.waitFor();
            int exitCode = xz.exitValue();
            if (exitCode != 0) {
                BufferedReader xzStdErr = new BufferedReader(new InputStreamReader(xz.getErrorStream()));
                String firstStdErrLine = xzStdErr.readLine();
                xzStdErr.close();
                throw new RuntimeException("archiver exit code '" + exitCode + "'"
                        + (firstStdErrLine != null ? " (" + firstStdErrLine + ")" : ""));
            }
            compressedFile = xzTempFile;
        } catch(Exception e) {
            throw e;
        } finally {
            try { xz.destroy(); } catch(Exception e) {}
        }

        return compressedFile;
    }

    private boolean isFileInUse(File f) {
        boolean result = false;

        if (f.exists()) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(f, "rw");
            } catch (FileNotFoundException fnfe) {
                result = true;
            } finally {
                try {
                    raf.close();
                } catch(Exception e) {}
            }
        }

/*        Process plsof = null;
        BufferedReader reader = null;
        try {
            // If you want to use piping/redirecting the output then you have to launch the shell since only the shell understands < | >
            //plsof = new ProcessBuilder(new String[]{"/bin/sh", "-c",  "lsof -F a " + f.getAbsolutePath() + " | grep -c ^a[\\-wu]"}).start();

            plsof = new ProcessBuilder(new String[]{"lsof", "-F", "a", f.getAbsolutePath()}).start();

            reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
            String line;
            while((line = reader.readLine()) != null) {
                if(line.matches("^a[\\-wu]")) {
                    result = true;
                    break;
                }
            }
        } catch(Exception e) {
        } finally {
            try { reader.close(); } catch(Exception e) {}
            try { plsof.destroy(); } catch(Exception e) {}
        }
*/
        return result;
    }

    public File[] selectFiles(File queueDir) {
        return queueDir.listFiles();
    }

    protected CStoreWebResponse sendHttpUploadRequest(File file, Properties params) throws IOException {
        webClient.clearParams();
        webClient.setBasicParams(this.emsowUsername, this.emsowPassword, "C-STORE", null);
        webClient.setParams(params);

        DicomClientMetaInfo metaInfo = new DicomClientMetaInfo(params);

        if (this.cnListener != null) {
            webClient.addListener(this.cnListener, metaInfo);
        }
        webClient.attachFile("file", file);

        if (this.notConcurrent) {
            //We synchronize to cnListener because cnListener is the main class (UnvSCP)
            synchronized (this.cnListener) {
                webClient.uploadFiles();
            }
        } else {
            webClient.uploadFiles();
        }
        CStoreWebResponse cswr = webClient.parseJsonResponse(CStoreWebResponse.class);

        Collection<CStoreWebResponse.ExtraMessage> data = cswr.getData();
        if(data != null) {
            for(CStoreWebResponse.ExtraMessage rec : data) {
                if (rec.getSuccess() == null) {
                    rec.setSuccess("1");
                }
                if (rec.getDebug() == null) {
                    rec.setDebug("0");
                }
                if (rec.getDebugLevel() == null) {
                    rec.setDebugLevel("0");
                }
                this.cnListener.logMessage(metaInfo, "C-STORE", rec, "EMSOW-DIALOG");
            }
        }

        return cswr;
    }

    public void cleanUp(){}

    public void addSenderTaskListener(SenderTaskListener stl) {
        if (stl != null) {
            senderTaskListeners.add(stl);
        }
    }

    private Map<File, String> filterDcmFiles(File[] dcmFiles) {
        Map<File, String> res = new LinkedHashMap<File, String>();
        for (File f : dcmFiles) {
            try {
                DicomFileDataPresentation dfdp = new DicomFileDataPresentation(f);
                res.put(f, dfdp.getSopInstanceUid());
                for (SenderTaskListener stl : senderTaskListeners) {
                    stl.onAddNewFile(
                        dfdp.getSopInstanceUid(),
                        dfdp.getStudyInstanceUid(),
                        dfdp.getStudyDate(),
                        dfdp.getStudyDescription(),
                        dfdp.getPatientName(),
                        dfdp.getPatientBirthDate()
                    );
                }
            } catch (IOException ioe) {}
        }
        return res;
    }
}
