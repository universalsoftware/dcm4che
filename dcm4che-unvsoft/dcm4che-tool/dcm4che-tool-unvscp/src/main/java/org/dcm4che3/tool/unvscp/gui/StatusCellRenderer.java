
package org.dcm4che3.tool.unvscp.gui;

import java.awt.Component;
import java.net.URL;
import java.util.Arrays;
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

        int numberOfSentFiles = ((ActivityTableModel)table.getModel()).getNumberOfSentFiles(row);
        int totalNumberOfFiles = ((ActivityTableModel)table.getModel()).getTotalNumberOfFiles(row);
        if (((ActivityTableModel)table.getModel()).isPending(row)) {
            this.setIcon(pendingIcon);
            this.setValue("pending");
        } else {
            if (numberOfSentFiles == totalNumberOfFiles) {
                this.setIcon(successIcon);
                this.setValue("<html><span style=\"color:" + (isSelected ? "#90ee90" : "#008000") + ";\">success");
            } else {
                int numberOfBadFiles = ((ActivityTableModel)table.getModel()).getNumberOfBadFiles(row);
                if (numberOfBadFiles > 0) {
                    this.setIcon(failureIcon);
                    this.setValue("<html><span style=\"color:" + (isSelected ? "#f08080" : "#a52a2a") + ";\">failure");
                    /*String[] errorsInfo = ((ActivityTableModel)table.getModel()).getErrorsInfo(row);
                    if (errorsInfo != null && errorsInfo.length > 0) {
                        String toolTipText = "<html>";
                        for (int i = 0; i < errorsInfo.length; i++) {
                            toolTipText += (i > 0 ? "<br>" : "") + errorsInfo[i];
                        }
                        this.setToolTipText(toolTipText);
                    }*/
                } else {
                    this.setIcon(sendingIcon);
                    this.setValue("<html><span style=\"color:" + (isSelected ? "#87cefa" : "#005ba7") + ";\">sending");
                }
            }
        }

        return this;
    }
}
