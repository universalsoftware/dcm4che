
package org.dcm4che3.tool.unvscp.data;

import java.util.Collection;
import java.util.Iterator;
import org.dcm4che3.data.Attributes;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class DicomRecord {
    private Collection<DicomAttribute> attributes;
    private Collection<DicomRecord> nested;

    public Attributes getAttributes() {
        Attributes attr = (attributes != null && attributes.size() > 0 ? new Attributes() : null);
        if (attr == null) return null;

        Iterator<DicomAttribute> attributesIterator = attributes.iterator();
        while (attributesIterator.hasNext()) {
            DicomAttribute da = attributesIterator.next();
            attr.setString(da.getTag(), da.getVr(), da.getValue());
        }

        return attr;
    }

    public Collection<DicomRecord> getNested() {
        return this.nested;
    }
}
