
package org.dcm4che3.tool.unvscp.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.dcm4che3.media.DicomDirReader;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class DicomDirReaderWrapper extends DicomDirReader implements IDicomReader {
    public DicomDirReaderWrapper(File file) throws IOException {
        super(file);
    }

    protected DicomDirReaderWrapper(File file, String mode) throws IOException {
        super(file, mode);
    }

    @Override
    public URL getURL() {
        return null;
    }
}
