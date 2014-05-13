
package org.dcm4che3.tool.unvscp.gui;

import java.text.SimpleDateFormat;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class DateCellRenderer extends DefaultTableCellRenderer {
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/YYYY");

    @Override
    public void setValue(Object value) {
        setText(value == null ? null : formatter.format(value));
    }
}
