
package org.dcm4che3.tool.unvscp.gui;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Pavel Varzinov <varzinov@yandex.ru>
 */
public class ActivityTableModel extends DefaultTableModel {
    public static final int PENDING = 0, SENDING = 1, SUCCESS = 2, FAILURE = 3;

    private static final String[] columnNames = {
        "Date of Service", "Patient Name", "Date of Birth", "Study Desc.", "Images", "Status", "Progress", "Study UID", "SOP Instance UIDs", "Show Summary"
    };
    private static final Class[] columnClasses = {
        Date.class, String.class, Date.class, String.class, Integer.class, Object.class, Object.class, String.class, Map.class, Boolean.class
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
            if (getStatus(rowToUpdate) != SENDING) {
                setValueAt(PENDING, rowToUpdate, "Status");
            }
            /* Column "Progress" is used to render the progress */
            /* Column "Study UID" is not updated cause it contains the same studyInstanceUid */
            setValueAt(false, rowToUpdate, "Show Summary");
            this.fireTableRowsUpdated(rowToUpdate, rowToUpdate);
        } else {
            sopInstanceUidMap = new HashMap<String, Object[]>();
            sopInstanceUidMap.put(sopInstanceUid, new Object[]{false, null}); // File status and status text
            Object[] record = new Object[]{
                studyDate, patientName, patientDob, studyDescription, 1, PENDING, null, studyInstanceUid, sopInstanceUidMap, false
            };
            insertRow(0, record);
        }
    }

    public synchronized int getStatus(int rowIndex) {
        Object value = getValueAt(rowIndex, "Status");
        int status = (value == null) ? PENDING : (Integer)value;
        return status;
    }

    public synchronized void markGroupAsSending(String sopInstanceUid) {
        int row = findRowBySopInstanceUid(sopInstanceUid);
        if (row > -1 && getStatus(row) == PENDING) {
            setValueAt(SENDING, row, "Status");
        }
        this.fireTableRowsUpdated(row, row);
    }

    public synchronized void transferProcessUpdate(String sopInstanceUid, String errMsg) {
        for (int row = 0; row < getRowCount(); row++) {
            Map<String, Object[]> sopInstanceUidMap = (Map<String, Object[]>)getValueAt(row, "SOP Instance UIDs");
            Object[] fileStatus = sopInstanceUidMap.get(sopInstanceUid);
            if (fileStatus == null) continue;

            fileStatus[0] = errMsg == null;
            fileStatus[1] = errMsg;
            if (errMsg != null) {
                setValueAt(FAILURE, row, "Status");
            }

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
            if (getTotalNumberOfFiles(row) == getNumberOfSentFiles(row) && getNumberOfBadFiles(row) == 0) {
                setValueAt(SUCCESS, row, "Status");
            }
        }
        if (getRowCount() > 0) {
            this.fireTableRowsUpdated(0, row);
        }
    }

    public synchronized boolean getShowSummary(int rowIndex) {
        return (Boolean)getValueAt(rowIndex, "Show Summary");
    }

    public synchronized LinkedHashMap<String, Integer> getErrorsInfo(int rowIndex) {
        LinkedHashMap<String, Integer> res = null;

        Map<String, Object[]> sopInstances = (Map<String, Object[]>)getValueAt(rowIndex, "SOP Instance UIDs");
        for (Object[] sopInstance : sopInstances.values()) {
            String errMsgText = (String)sopInstance[1];
            if (errMsgText != null) {
                if (res == null) {
                    res = new LinkedHashMap<String, Integer>();
                }
                if (res.containsKey(errMsgText)) {
                    res.put(errMsgText, res.get(errMsgText) + 1);
                } else {
                    res.put(errMsgText, 1);
                }
            }
        }
        return res;
    }

    public synchronized boolean isSendingInProgress() {
        for (int rowIndex = 0; rowIndex < getRowCount(); rowIndex++) {
            if (getStatus(rowIndex) == SENDING) { return true; }
        }
        return false;
    }
}
