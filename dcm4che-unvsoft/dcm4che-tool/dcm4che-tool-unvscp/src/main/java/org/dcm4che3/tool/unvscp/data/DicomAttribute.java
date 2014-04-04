/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.data;

import java.util.ArrayList;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;

/**
 *
 * @author Pavel Varzinov
 */
public class DicomAttribute {
    private String tag;
    private String vr;
    private String value;

    public void set(int tag, VR vr, String value) {
        this.tag = tag + "";
        if (vr != null) this.vr = vr.name();
        this.value = value;
    }

    public int getTag(){
        try {
            return Integer.parseInt(tag);
        } catch (NumberFormatException nfe) {
            throw new NumberFormatException("Invalid int \"" + tag + "\" for \"tag\" parameter in the attributes");
        }
    }

    public VR getVr() {
        return VR.valueOf(this.vr);
    }

    public String getValue() {
        return this.value;
    }

    public static List<DicomAttribute> getAttributesAsList(Attributes a) {
        if (a == null) return null;

        int[] tags = a.tags();
        List<DicomAttribute> attribs = new ArrayList<DicomAttribute>();
        for (int i = 0; i < tags.length; i++) {
            DicomAttribute da = new DicomAttribute();
            da.set(tags[i], a.getVR(tags[i]), a.getString(tags[i]));
            attribs.add(da);
        }
        return attribs;
    }
}
