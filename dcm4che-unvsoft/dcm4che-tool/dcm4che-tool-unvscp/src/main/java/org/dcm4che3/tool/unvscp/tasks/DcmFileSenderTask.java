
package org.dcm4che3.tool.unvscp.tasks;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.http.conn.HttpHostConnectException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.tool.unvscp.media.UnvWebClientListener;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class DcmFileSenderTask extends GenericFilesHttpSenderTask {
    private Properties params = new Properties();

    private FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isFile(); // Any file with or without extension
        }
    };

    private FileFilter dirFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    public DcmFileSenderTask(URL emsowUrl, String emsowUsername, String emsowPassword,
            File queueDir, File tmpDir, File badFilesDir, int sleepTime, boolean notConcurrent,
            Integer compressionLevel, UnvWebClientListener cnListener, Properties params) {

        super(emsowUrl, emsowUsername, emsowPassword, queueDir, tmpDir, badFilesDir, sleepTime, notConcurrent, compressionLevel, cnListener);

        if (params != null) {
            this.params = params;
        }
    }

    @Override
    public File[] selectFiles(File queueDir) {
        List<File> files = this.listFilesRecursively(queueDir, new ArrayList<File>());
        return files.toArray(new File[files.size()]);
    }

    private List<File> listFilesRecursively(File rootDir, List<File> result) {
        File[] files = rootDir.listFiles(fileFilter);
        result.addAll(Arrays.asList(files));
        File[] childDirs = rootDir.listFiles(dirFilter);
        for (File dir : childDirs) {
            listFilesRecursively(dir, result);
        }
        return result;
    }

    @Override
    public Properties getFileMeta(File dcmFile) {
        params.setProperty("SOP_INSTANCE_UID", "");
        FileInputStream dicomFis = null;
        DicomInputStream dis = null;
        try {
            dicomFis = new FileInputStream(dcmFile);
            dis = new DicomInputStream(dicomFis);
            Attributes attr = dis.readFileMetaInformation();
            dis.close();
            dicomFis.close();

            params.setProperty("SOP_INSTANCE_UID", attr.getString(Tag.MediaStorageSOPInstanceUID, ""));

            String dcmRelPath = dcmFile.getAbsolutePath().replace(queueDir.getAbsolutePath() , "");
            if (dcmRelPath.startsWith(File.separator)) {
                dcmRelPath = dcmRelPath.substring(1);
            }

            LOG.info("{}+0 [{}]: FOUND-FILE {}, UID={}",
                    new String[] {
                        params.getProperty("FROM_AET", "<UNKNOWN>"),
                        Thread.currentThread().getName(),
                        dcmRelPath,
                        params.getProperty("SOP_INSTANCE_UID", "")
                    });
        } catch (IOException ioe) {
            try { dis.close(); } catch(Exception ignore) {}
            try { dicomFis.close(); } catch(Exception ignore) {}
        }
        return params;
    }

    @Override
    public void sendFile(File dcmFile, Integer compressionLevel, boolean notConcurrent, UnvWebClientListener cnListener) {

        Properties params = this.getFileMeta(dcmFile);
        File compressedFile = null;
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
            this.sendHttpUploadRequest(compressedFile == null ? dcmFile : compressedFile, params);
            dcmFile.delete();
        } catch (HttpHostConnectException hhce) { // We are saving the files here to retry sending them later
        } catch (SocketException se) {
        } catch(Exception e) {
            String dcmRelPath = dcmFile.getAbsolutePath().replace(queueDir.getAbsolutePath() , "");
            if (dcmRelPath.startsWith(File.separator)) {
                dcmRelPath = dcmRelPath.substring(1);
            }
            File badDcm = new File(badFilesDir, dcmRelPath);
            badDcm.delete();
            if (!badDcm.getParentFile().exists()) {
                badDcm.getParentFile().mkdirs();
            }
            dcmFile.renameTo(badDcm);
            LOG.error("{}+0 [{}]: BAD-FILE {}, UID={}, details: {}",
                    new String[] {
                        params.getProperty("FROM_AET", "<UNKNOWN>"),
                        Thread.currentThread().getName(),
                        dcmRelPath,
                        params.getProperty("SOP_INSTANCE_UID", ""),
                        e.toString().replaceAll("\\r", "").replaceAll("\\n", "\\\\n")
                    });
        } finally {
            if (compressedFile != null) {
                compressedFile.delete();
            }
        }
    }

    @Override
    public void cleanUp() {
        removeChildDirsRecursive(queueDir);
    }

    private void removeChildDirsRecursive(File dir) {
        if(!dir.equals(queueDir)) {
            dir.delete();
        }
        File[] children = dir.listFiles();
        for (File f : children) {
            if (f.isDirectory()) {
                removeChildDirsRecursive(f);
            }
        }
    }
}
