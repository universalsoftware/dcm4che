/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che.tool.unvscp.data;

import java.util.Collection;

/**
 *
 * @author Pavel Varzinov
 */
public class CFindWebResponse extends GenericWebResponse {
    private Collection<DicomRecord> data;

    @Override
    public Collection<DicomRecord> getData() {
        return this.data;
    }

}
