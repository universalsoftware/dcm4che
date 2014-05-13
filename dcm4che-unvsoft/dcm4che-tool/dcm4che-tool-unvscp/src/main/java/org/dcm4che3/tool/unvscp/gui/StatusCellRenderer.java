
package org.dcm4che3.tool.unvscp.gui;

import java.awt.Component;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class StatusCellRenderer extends DefaultTableCellRenderer {
    private final ImageIcon pendingIcon;
    private final ImageIcon sendingIcon;
    private final ImageIcon failureIcon;
    private final ImageIcon successIcon;

    public StatusCellRenderer() {
        URL pendingIconResource = getClass().getClassLoader().getResource("org/dcm4che3/tool/unvscp/icons/clock.png");
        pendingIcon = pendingIconResource == null ? null : new ImageIcon(pendingIconResource);
        URL sendingIconResource = getClass().getClassLoader().getResource("org/dcm4che3/tool/unvscp/icons/processing.png");
        sendingIcon = sendingIconResource == null ? null : new ImageIcon(sendingIconResource);
        URL failureIconResource = getClass().getClassLoader().getResource("org/dcm4che3/tool/unvscp/icons/warning.png");
        failureIcon = failureIconResource == null ? null : new ImageIcon(failureIconResource);
        URL successIconResource = getClass().getClassLoader().getResource("org/dcm4che3/tool/unvscp/icons/tick.png");
        successIcon = successIconResource == null ? null : new ImageIcon(successIconResource);
    }

    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Integer status = ((ActivityTableModel)table.getModel()).getStatus(row);
        switch(status) {
            case ActivityTableModel.PENDING:
                this.setIcon(pendingIcon);
                this.setValue("pending");
                this.setToolTipText(null);
                break;
            case ActivityTableModel.SENDING:
                this.setIcon(sendingIcon);
                this.setValue("<html><span style=\"color:" + (isSelected ? "#87cefa" : "#005ba7") + ";\">sending");
                this.setToolTipText(null);
                break;
            case ActivityTableModel.SUCCESS:
                this.setIcon(successIcon);
                this.setValue("<html><span style=\"color:" + (isSelected ? "#90ee90" : "#008000") + ";\">success");
                this.setToolTipText(null);
                break;
            case ActivityTableModel.FAILURE:
                this.setIcon(failureIcon);
                this.setValue("<html><span style=\"color:" + (isSelected ? "#f08080" : "#a52a2a") + ";\">failure");
                LinkedHashMap<String, Integer> errorsInfo = ((ActivityTableModel)table.getModel()).getErrorsInfo(row);
                if (errorsInfo != null && errorsInfo.size() > 0) {
                    String toolTipText = "<html><table style=\"border:none;border-spacing:0;border-collapse:collapse;margin:0;\">";
                    for (Entry<String, Integer> e : errorsInfo.entrySet()) {
                        toolTipText += "<tr><th style=\"text-align:right;padding:0 3px 0 0;\">" + e.getValue() + " file" + (e.getValue() != 1 ? "s" : "")
                                + ":</th><td style=\"color:#a52a2a;padding:0;\">" + e.getKey() + "</td></tr>";
                    }
                    this.setToolTipText(toolTipText);
                } else {
                    this.setToolTipText(null);
                }
                break;
            default:
                this.setIcon(null);
                this.setValue(null);
                this.setToolTipText(null);
        }

        return this;
    }
}
