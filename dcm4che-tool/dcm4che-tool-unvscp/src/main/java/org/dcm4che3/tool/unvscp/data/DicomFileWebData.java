/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcm4che3.tool.unvscp.data;

/**
 *
 * @author Pavel Varzinov
 */
public class DicomFileWebData {
    private int pacsfile_id;
    private String pacsfile_name;
    private String pacsfile_media_storage_sop_class_uid;
    private String pacsfile_media_storage_sop_instance_uid;
    private String pacsfile_transfer_syntax_uid;

    public int getFileId() {
        return this.pacsfile_id;
    }

    public String getFileName() {
        return this.pacsfile_name == null ? "" : this.pacsfile_name;
    }

    public String getClassUID() {
        return this.pacsfile_media_storage_sop_class_uid == null ? "" : this.pacsfile_media_storage_sop_class_uid;
    }

    public String getInstanceUID() {
        return this.pacsfile_media_storage_sop_instance_uid == null ? "" : this.pacsfile_media_storage_sop_instance_uid;
    }

    public String getTransferSyntaxUID() {
        return this.pacsfile_transfer_syntax_uid == null ? "" : this.pacsfile_transfer_syntax_uid;
    }

    public String toString() {
        return this.pacsfile_name + "; " + this.pacsfile_media_storage_sop_class_uid + "; " + this.pacsfile_media_storage_sop_instance_uid;
    }
}
