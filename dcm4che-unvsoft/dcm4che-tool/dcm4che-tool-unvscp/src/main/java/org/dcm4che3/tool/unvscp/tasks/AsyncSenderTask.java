
package org.dcm4che3.tool.unvscp.tasks;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import org.apache.http.conn.HttpHostConnectException;
import org.dcm4che3.tool.unvscp.data.CStoreWebResponse;
import org.dcm4che3.tool.unvscp.media.DicomClientMetaInfo;
import org.dcm4che3.tool.unvscp.media.UnvWebClientListener;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class AsyncSenderTask extends GenericFilesHttpSenderTask {
    private FileFilter fileFilter = new FileFilter(){
        @Override
        public boolean accept(File file) {
            return file.isFile() && file.getName().matches("^.+\\.2\\.(?i)(dcm)$");
        }
    };

    public AsyncSenderTask(URL emsowUrl, String emsowUsername, String emsowPassword,
            File queueDir, File tmpDir, File badFilesDir, int sleepTime, boolean notConcurrent,
            Integer compressionLevel, UnvWebClientListener cnListener) {

        super(emsowUrl, emsowUsername, emsowPassword, queueDir, tmpDir, badFilesDir, sleepTime, notConcurrent, compressionLevel, cnListener);
    }

    @Override
    public File[] selectFiles(File queueDir) {
        return queueDir.listFiles(this.fileFilter);
    }

    @Override
    public Properties getFileMeta(File dcmFile) {
        File metaFile = new File(dcmFile.getParentFile(), dcmFile.getName().substring(0, dcmFile.getName().lastIndexOf(".2.dcm")) + ".1.meta");
        Properties params;

        try {
            FileInputStream fis = new FileInputStream(metaFile);
            params = new Properties();
            params.load(fis);
            params.setProperty("META_FILE_PATH", metaFile.getAbsolutePath());
            fis.close();
        } catch (IOException ioe) {
            params = null;
        }

        if (params == null) {
            CStoreWebResponse.ExtraMessage msg = new CStoreWebResponse.ExtraMessage();
            msg.setSuccess("0");
            msg.setDebug("0");
            msg.setDebugLevel("1");
            msg.setMsg("Meta file for " + dcmFile.getName() + " was not found. Dcm file will be skipped");
            cnListener.logMessage(new DicomClientMetaInfo(null, null), "C-STORE", msg, "EMSOW-DIALOG");
            LOG.error("<UNKNOWN>+0 [{}]: BAD-FILE {}, details: {}",
                    new String[] {
                        Thread.currentThread().getName(),
                        dcmFile.getName(),
                        "Meta file was not found, dcm file will be skipped"
                    });
            File badDcm = new File(badFilesDir, dcmFile.getName());
            badDcm.setLastModified(new Date().getTime());
            dcmFile.renameTo(badDcm);
        } else {
            LOG.info("{}+0 [{}]: FOUND-FILE {}, UID={}",
                new String[] {
                    params.getProperty("FROM_AET", "<UNKNOWN>"),
                    Thread.currentThread().getName(),
                    dcmFile.getName(),
                    params.getProperty("SOP_INSTANCE_UID", "")
                }
            );
        }

        return params;
    }

    @Override
    public void sendFile(File dcmFile, Integer compressionLevel, boolean notConcurrent, UnvWebClientListener cnListener) {

        Properties params = this.getFileMeta(dcmFile);
        if (dcmFile == null || params == null) {
            return;
        }

        File compressedFile = null;
        File metaFile = new File(params.getProperty("META_FILE_PATH"));
        try {
            if (this.compressionLevel != null) {
                LOG.info("{}+0 [{}]: COMPRESSION-BEGIN UID={}",
                    new String[] {
                        params.getProperty("FROM_AET", "<UNKNOWN>"),
                        Thread.currentThread().getName(),
                        params.getProperty("SOP_INSTANCE_UID", "")
                    }
                );
                Date startTime = new Date();
                compressedFile = this.compressFile(dcmFile);
                float compressionTime = (float)(new Date().getTime() - startTime.getTime()) / 1000;
                LOG.info("{}+0 [{}]: COMPRESSION-END UID={}, time={}",
                    new String[] {
                        params.getProperty("FROM_AET", "<UNKNOWN>"),
                        Thread.currentThread().getName(),
                        params.getProperty("SOP_INSTANCE_UID", ""),
                        String.format(this.enUSLocale, "%.3f(s)", compressionTime)
                    }
                );
            }
            params.remove("META_FILE_PATH");
            this.sendHttpUploadRequest(compressedFile == null ? dcmFile : compressedFile, params);

            metaFile.delete();
            dcmFile.delete();
        } catch (HttpHostConnectException hhce) { // We are saving the files here to retry sending them later
        } catch (SocketException se) {
        } catch (Exception e) {
            File badMeta = new File(badFilesDir, metaFile.getName());
            File badDcm = new File(badFilesDir, dcmFile.getName());
            badMeta.delete();
            badDcm.delete();
            metaFile.renameTo(badMeta);
            dcmFile.renameTo(badDcm);
            Date lastMod = new Date();
            badMeta.setLastModified(lastMod.getTime());
            badDcm.setLastModified(lastMod.getTime());
            LOG.error("{}+0 [{}]: BAD-FILE {}, UID={}, details: {}",
                    new String[] {
                        params.getProperty("FROM_AET", "<UNKNOWN>"),
                        Thread.currentThread().getName(),
                        dcmFile.getName(),
                        params.getProperty("SOP_INSTANCE_UID", ""),
                        e.toString().replaceAll("\\r", "").replaceAll("\\n", "\\\\n")
                    });
        } finally {
            if (compressedFile != null) {
                compressedFile.delete();
            }
        }
    }
}
