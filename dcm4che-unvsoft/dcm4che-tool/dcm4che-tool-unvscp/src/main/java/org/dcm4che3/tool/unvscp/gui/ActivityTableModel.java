
package org.dcm4che3.tool.unvscp.gui;

import java.util.Arrays;
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
        "Date of Service", "Patient Name", "Date of Birth", "Study Desc.", "Images", "Status", "Progress", "Study UID", "SOP Instance UIDs", "Show Summary", "Processed"
    };
    private static final Class[] columnClasses = {
        Date.class, String.class, Date.class, String.class, Integer.class, Object.class, Object.class, String.class, Map.class, Boolean.class, Boolean.class
    };

    public static class NoSuchColumnNameException extends RuntimeException {
        private final String columnName;
        public NoSuchColumnNameException(String columnName) {
            if (columnName == null) {
                throw new NullPointerException();
            }
            this.columnName = columnName;
        }
        @Override
        public String getMessage() {
            return "Column \"" + columnName + "\" does not exist in ActivityTableModel";
        }
    }

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

    public int getColumnByName(String name) {
        return Arrays.asList(columnNames).indexOf(name);
    }

    public void setValueAt(Object aValue, int row, String columnName) {
        int col;
        if ((col = getColumnByName(columnName)) > -1) {
            setValueAt(aValue, row, col);
        } else {
            throw new NoSuchColumnNameException(columnName);
        }
    }

    public Object getValueAt(int row, String columnName) {
        int col;
        if ((col = getColumnByName(columnName)) > -1) {
            return getValueAt(row, col);
        } else {
            throw new NoSuchColumnNameException(columnName);
        }
    }

    public synchronized void insertUpdate(String sopInstanceUid, String studyInstanceUid,
            Date studyDate, String studyDescription,
            String patientName, Date patientDob) {

        Map<String, Object[]> sopInstanceUidMap;
        int rowToUpdate;
        if ((rowToUpdate = findRow(new Object[]{null, null, null, null, null, null, null, studyInstanceUid})) > -1) {
            setValueAt(studyDate, rowToUpdate, "Date of Service");
            setValueAt(patientName, rowToUpdate, "Patient Name");
            setValueAt(patientDob, rowToUpdate, "Date of Birth");
            setValueAt(studyDescription, rowToUpdate, "Study Desc.");
            sopInstanceUidMap = (Map<String, Object[]>)getValueAt(rowToUpdate, "SOP Instance UIDs");
            if (!sopInstanceUidMap.containsKey(sopInstanceUid)) {
                setValueAt((Integer)getValueAt(rowToUpdate, "Images") + 1, rowToUpdate, "Images");
                sopInstanceUidMap.put(sopInstanceUid, new Object[]{false, null});
            } else {
                Object[] fileStatus = sopInstanceUidMap.get(sopInstanceUid);
                fileStatus[0] = false; // false + errMsg==null => new file; false + errMsg=="" => failue
                fileStatus[1] = null;  // err msg
            }
            /* Column "Status" is used to render the status */
            /* Column "Progress" is used to render the progress */
            /* Column "Study UID" is not updated cause it contains the same studyInstanceUid */
            setValueAt(false, rowToUpdate, "Show Summary");
            this.fireTableRowsUpdated(rowToUpdate, rowToUpdate);
        } else {
            sopInstanceUidMap = new HashMap<String, Object[]>();
            sopInstanceUidMap.put(sopInstanceUid, new Object[]{false, null}); // File status and status text
            Object[] record = new Object[]{
                studyDate, patientName, patientDob, studyDescription, 1, null, null, studyInstanceUid, sopInstanceUidMap, false, false
            };
            insertRow(0, record);
        }
    }

    public synchronized void markGroupAsProcessed(String sopInstanceUid) {
        int row = findRowBySopInstanceUid(sopInstanceUid);
        if (row > -1) {
            setValueAt(true, row, "Processed");
        }
        this.fireTableRowsUpdated(row, row);
    }

    public synchronized boolean isPending(int rowIndex) {
        return !(Boolean)getValueAt(rowIndex, "Processed");
    }

    public synchronized void transferProcessUpdate(String sopInstanceUid, String errMsg) {
        for (int row = 0; row < getRowCount(); row++) {
            Map<String, Object[]> sopInstanceUidMap = (Map<String, Object[]>)getValueAt(row, "SOP Instance UIDs");
            Object[] fileStatus = sopInstanceUidMap.get(sopInstanceUid);
            if (fileStatus == null) continue;

            fileStatus[0] = errMsg == null;
            fileStatus[1] = errMsg;

            this.fireTableRowsUpdated(row, row);
            break;
        }
    }

    private int findRowBySopInstanceUid(String sopInstanceUid) {
        for (int row = 0; row < getRowCount(); row++) {
            Map<String, Object[]> sopInstanceUidMap = (Map<String, Object[]>)getValueAt(row, "SOP Instance UIDs");
            Object[] fileStatus = sopInstanceUidMap.get(sopInstanceUid);
            if (fileStatus != null) {
                return row;
            }
        }
        return -1;
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
        return (Integer)getValueAt(rowIndex, "Images");
    }

    public synchronized int getNumberOfSentFiles(int rowIndex) {
        int res = 0;

        Map<String, Object[]> sopInstances = (Map<String, Object[]>)getValueAt(rowIndex, "SOP Instance UIDs");
        for (Object[] sopInstance : sopInstances.values()) {
            res += ((Boolean)sopInstance[0] && sopInstance[1] == null) ? 1 : 0;
        }
        return res;
    }

    public synchronized int getNumberOfBadFiles(int rowIndex) {
        int res = 0;

        Map<String, Object[]> sopInstances = (Map<String, Object[]>)getValueAt(rowIndex, "SOP Instance UIDs");
        for (Object[] sopInstance : sopInstances.values()) {
            res += (!(Boolean)sopInstance[0] && sopInstance[1] != null) ? 1 : 0;
        }
        return res;
    }

    public synchronized void showSummary() {
        int row;
        for (row = 0; row < getRowCount(); row++) {
            setValueAt(true, row, "Show Summary");
        }
        if (getRowCount() > 0) {
            this.fireTableRowsUpdated(0, row);
        }
    }

    public synchronized boolean getShowSummary(int rowIndex) {
        return (Boolean)getValueAt(rowIndex, "Show Summary");
    }

    public synchronized String[] getErrorsInfo(int rowIndex) {
        /*HashMap<String, Integer> res = new HashMap<String, Integer>();
        Map<String, Object[]> sopInstances = (Map<String, Object[]>)getValueAt(rowIndex, "SOP Instance UIDs");
        for (Object[] sopInstance : sopInstances.values()) {
            if (sopInstance[1] != null) {
                if (res.containsKey(sopInstance[1])) {

                } else {
                    res.put(sopInstance[1], 1);
                }
            }
            res += (!(Boolean)sopInstance[0] && sopInstance[1] != null) ? 1 : 0;
        }*/
        return new String[]{"Test1", "Test2"};
    }

    public synchronized boolean isSendingInProgress() {
        for (int rowIndex = 0; rowIndex < getRowCount(); rowIndex++) {
            Map<String, Object[]> sopInstances = (Map<String, Object[]>)getValueAt(rowIndex, "SOP Instance UIDs");
            for (Object[] sopInstance : sopInstances.values()) {
                if (!(Boolean)sopInstance[0] && sopInstance[1] == null) { return true; }
            }
        }
        return false;
    }
}
