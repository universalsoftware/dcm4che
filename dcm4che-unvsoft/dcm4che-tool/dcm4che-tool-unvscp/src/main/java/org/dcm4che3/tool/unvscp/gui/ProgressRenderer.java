
package org.dcm4che3.tool.unvscp.gui;

import java.awt.Component;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class ProgressRenderer extends JProgressBar implements TableCellRenderer {

    public ProgressRenderer() {
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){

        setMaximum(((ActivityTableModel)table.getModel()).getTotalNumberOfFiles(row));
        setValue(((ActivityTableModel)table.getModel()).getNumberOfSentFiles(row));

        return this;
    }
}
