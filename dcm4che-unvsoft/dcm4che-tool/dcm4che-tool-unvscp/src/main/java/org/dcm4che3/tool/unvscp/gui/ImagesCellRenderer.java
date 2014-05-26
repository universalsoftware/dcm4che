
package org.dcm4che3.tool.unvscp.gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class ImagesCellRenderer extends DefaultTableCellRenderer {
    public ImagesCellRenderer() {
        this.setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        int numberOfSentFiles = ((ActivityTableModel)table.getModel()).getNumberOfSentFiles(row);
        int totalNumberOfFiles = ((ActivityTableModel)table.getModel()).getTotalNumberOfFiles(row);

        String renderValue = "<html><table style=\"border:none;border-spacing:0;border-collapse:collapse;margin:0;\"><tr>"
                + "<td style=\"padding:0;\"><div style=\"width:25px;text-align:right;\">" + numberOfSentFiles + "</div></td>"
                + "<td style=\"padding:0;\"><div>/</div></td>"
                + "<td style=\"padding:0;\"><div style=\"width:25px;\">" + totalNumberOfFiles + "</div></td>";
        setValue(renderValue);
        setToolTipText("<html>Total: <b>" + totalNumberOfFiles);
        return this;
    }
}
