
package org.dcm4che3.tool.unvscp.gui;

import java.awt.Component;
import java.awt.Rectangle;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class ProgressCellRenderer extends DefaultTableCellRenderer {
    private final JProgressBar progress;

    public ProgressCellRenderer() {
        progress = new JProgressBar();
        progress.setStringPainted(true);
        this.add(progress);
    }

    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        boolean showSummary = ((ActivityTableModel)table.getModel()).getShowSummary(row);
        int numberOfBadFiles = ((ActivityTableModel)table.getModel()).getNumberOfBadFiles(row);
        if (showSummary && numberOfBadFiles > 0) {
            setText(((ActivityTableModel)table.getModel()).getNumberOfBadFiles(row) + " were moved to the bad files directory.");
            progress.setVisible(false);
        } else {
            setText(null);
            progress.setMaximum(((ActivityTableModel)table.getModel()).getTotalNumberOfFiles(row));
            progress.setValue(((ActivityTableModel)table.getModel()).getNumberOfSentFiles(row));
            Rectangle cellRect = table.getCellRect(row, column, false);
            progress.setSize(cellRect.width, cellRect.height);
            progress.setVisible(true);
        }

        return this;
    }
}
