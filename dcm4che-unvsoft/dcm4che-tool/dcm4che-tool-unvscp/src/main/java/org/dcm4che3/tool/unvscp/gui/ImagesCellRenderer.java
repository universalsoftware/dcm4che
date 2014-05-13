
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

        setValue(numberOfSentFiles + "/" + totalNumberOfFiles);
        setToolTipText("<html>Total: <b>" + totalNumberOfFiles);
        return this;
    }
}
