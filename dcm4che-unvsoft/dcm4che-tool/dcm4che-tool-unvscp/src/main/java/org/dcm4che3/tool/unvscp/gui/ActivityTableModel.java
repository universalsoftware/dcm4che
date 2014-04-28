
package org.dcm4che3.tool.unvscp.gui;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class ActivityTableModel extends DefaultTableModel {
    private static final String[] columnNames = {
        "Date", "Patient name", "Date of birth", "Study", "Images", "Status", "Study UID", "SOP Instance UIDs", "Show Summary"
    };
    private static final Class[] columnClasses = {
        Date.class, String.class, Date.class, String.class, Integer.class, Object.class, String.class, Map.class, Boolean.class
    };

    public ActivityTableModel() {
        super(columnNames, 0);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    public synchronized void insertUpdate(String sopInstanceUid, String studyInstanceUid,
            Date studyDate, String studyDescription,
            String patientName, Date patientDob) {

        Map<String, Object[]> sopInstanceUidMap;
        int rowToUpdate;
        if ((rowToUpdate = findRow(new Object[]{null, null, null, null, null, null, studyInstanceUid})) > -1) {
            setValueAt(studyDate, rowToUpdate, 0);
            setValueAt(patientName, rowToUpdate, 1);
            setValueAt(patientDob, rowToUpdate, 2);
            setValueAt(studyDescription, rowToUpdate, 3);
            sopInstanceUidMap = (Map<String, Object[]>)getValueAt(rowToUpdate, 7);
            if (!sopInstanceUidMap.containsKey(sopInstanceUid)) {
                setValueAt((Integer)getValueAt(rowToUpdate, 4) + 1, rowToUpdate, 4);
                sopInstanceUidMap.put(sopInstanceUid, new Object[]{false, null});
            } else {
                Object[] fileStatus = sopInstanceUidMap.get(sopInstanceUid);
                fileStatus[0] = false;
                fileStatus[1] = null;
            }
            /* Column 5 is used to render the progress */
            /* Column 6 is not updated cause it contains the same studyInstanceUid */
            setValueAt(false, rowToUpdate, 8);
            this.fireTableRowsUpdated(rowToUpdate, rowToUpdate);
        } else {
            sopInstanceUidMap = new HashMap<String, Object[]>();
            sopInstanceUidMap.put(sopInstanceUid, new Object[]{false, ""}); // File status and status text
            Object[] record = new Object[]{
                studyDate, patientName, patientDob, studyDescription, 1, null, studyInstanceUid, sopInstanceUidMap, false
            };
            addRow(record);
        }
    }

    public synchronized void transferProcessUpdate(String sopInstanceUid, String errMsg) {
        for (int row = 0; row < getRowCount(); row++) {
            Map<String, Object[]> sopInstanceUidMap = (Map<String, Object[]>)getValueAt(row, 7);
            Object[] fileStatus = sopInstanceUidMap.get(sopInstanceUid);
            if (fileStatus == null) continue;

            fileStatus[0] = errMsg == null;
            fileStatus[1] = errMsg;

            this.fireTableRowsUpdated(row, row);
            break;
        }
    }

    public int findRow(Object[] searchData) {
        int resultRow = -1;
        for (int row = 0; row < getRowCount(); row++) {
            int matchCount = 0;
            int col;
            for (col = 0; col < getColumnCount() && col < searchData.length; col++) {
                Object cellValue = getValueAt(row, col);
                if (searchData[col] == null || searchData[col].equals(cellValue)) {
                    matchCount++;
                }
            }
            if (matchCount == col) {
                resultRow = row;
                break;
            }
        }
        return resultRow;
    }

    public synchronized int getTotalNumberOfFiles(int rowIndex) {
        return (Integer)getValueAt(rowIndex, 4);
    }

    public synchronized int getNumberOfSentFiles(int rowIndex) {
        int res = 0;

        Map<String, Object[]> sopInstances = (Map<String, Object[]>)getValueAt(rowIndex, 7);
        for (Object[] sopInstance : sopInstances.values()) {
            res += (Boolean)sopInstance[0] ? 1 : 0;
        }
        return res;
    }

    public synchronized int getNumberOfBadFiles(int rowIndex) {
        return getTotalNumberOfFiles(rowIndex) - getNumberOfSentFiles(rowIndex);
    }

    public synchronized void showSummary() {
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(true, row, 8);
            this.fireTableRowsUpdated(row, row);
        }
    }

    public synchronized boolean getShowSummary(int rowIndex) {
        return (Boolean)getValueAt(rowIndex, 8);
    }
}
