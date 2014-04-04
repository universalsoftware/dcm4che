
package org.dcm4che3.tool.unvscp.media;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.dcm4che3.media.DicomDirWriter;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public abstract class DicomDirWriterWrapper implements IDicomWriter {
    private DicomDirWriterWrapper(){}

    private static class DicomDirWriterHandler implements InvocationHandler {
        private DicomDirWriter ddw;
        private Method[] ddwMethods = DicomDirWriter.class.getMethods();

        public DicomDirWriterHandler(DicomDirWriter ddw) {
            this.ddw = ddw;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getDicomDirWriter".equals(method.getName())) {
                return this.ddw;
            } else {
                for (Method ddwMethod : ddwMethods) {
                    if (ddwMethod.equals(method)) {
                        return ddwMethod.invoke(ddw, args);
                    }
                }
                return null;
            }
        }
    }

    public static IDicomWriter open(File file) throws IOException {
        InvocationHandler iHandler = new DicomDirWriterHandler(DicomDirWriter.open(file));

        IDicomWriter wrapperInstance = (IDicomWriter) Proxy.newProxyInstance(
            IDicomWriter.class.getClassLoader(),
            new Class[] {IDicomWriter.class},
            iHandler
        );

        return wrapperInstance;
    }

}
