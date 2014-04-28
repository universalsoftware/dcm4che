
package org.dcm4che3.tool.unvscp.gui;

import java.awt.Component;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class IconCellRenderer extends DefaultTableCellRenderer {
    private final ImageIcon warningIcon;
    private final ImageIcon checkedIcon;

    public IconCellRenderer() {
        URL warningIconResource = getClass().getClassLoader().getResource("org/dcm4che3/tool/unvscp/icons/warning.png");
        warningIcon = warningIconResource == null ? null : new ImageIcon(warningIconResource);
        URL checkedIconResource = getClass().getClassLoader().getResource("org/dcm4che3/tool/unvscp/icons/check.gif");
        checkedIcon = checkedIconResource == null ? null : new ImageIcon(checkedIconResource);
        this.setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        boolean showSummary = ((ActivityTableModel)table.getModel()).getShowSummary(row);
        if (showSummary) {
            int numberOfBadFiles = ((ActivityTableModel)table.getModel()).getNumberOfBadFiles(row);
            if (numberOfBadFiles > 0) {
                this.setIcon(warningIcon);
            } else {
                this.setIcon(checkedIcon);
            }
        } else {
            this.setIcon(null);
        }
        return this;
    }
}
