/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import java.util.ArrayList;

import org.h2.api.ErrorCode;
import org.h2.command.dml.AllColumnsForPlan;
import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.TableFilter;
import org.h2.util.Utils;

/**
 * The scan index is not really an 'index' in the strict sense, because it can
 * not be used for direct lookup. It can only be used to iterate over all rows
 * of a table. Each regular table has one such object, even if no primary key or
 * indexes are defined.
 */
//建表时加 NOT PERSISTENT可以测试此类，
//如: create LOCAL TEMPORARY table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null) NOT PERSISTENT
//数据放在内存中，不存到硬盘
//此类只能通过建表时触发，不能通过建索引触发
//即：只在RegularTable(CreateTableData)中使用，不能通过RegularTable.addIndex触发
//org.h2.index.MultiVersionIndex不会嵌套ScanIndex
//因为MultiVersionIndex只在RegularTable.addIndex中使用，而ScanIndex不能通过RegularTable.addIndex触发
public class ScanIndex extends BaseIndex {
    private long firstFree = -1;
    private ArrayList<Row> rows = Utils.newSmallArrayList();
    private final RegularTable tableData;
    private long rowCount;
//<<<<<<< HEAD
//    
//    //跟PageDataIndex一样，id都是表id
//    public ScanIndex(RegularTable table, int id, IndexColumn[] columns, IndexType indexType) {
//        initBaseIndex(table, id, table.getName() + "_DATA", columns, indexType);
//        if (database.isMultiVersion()) {
//            sessionRowCount = New.hashMap();
//        } else {
//            sessionRowCount = null;
//        }
//=======

    public ScanIndex(RegularTable table, int id, IndexColumn[] columns,
            IndexType indexType) {
        super(table, id, table.getName() + "_DATA", columns, indexType);
        tableData = table;
    }

    @Override
    public void remove(Session session) {
        truncate(session);
    }

    @Override
    public void truncate(Session session) {
        rows = Utils.newSmallArrayList();
        firstFree = -1;
        if (tableData.getContainsLargeObject() && tableData.isPersistData()) {
            database.getLobStorage().removeAllForTable(table.getId());
        }
        tableData.setRowCount(0);
        rowCount = 0;
    }

    @Override
    public String getCreateSQL() {
        return null;
    }

    @Override
    public void close(Session session) {
        // nothing to do
    }

    @Override
    public Row getRow(Session session, long key) {
        return rows.get((int) key);
    }

    @Override
    public void add(Session session, Row row) {
        // in-memory
        if (firstFree == -1) {
            int key = rows.size();
            row.setKey(key);
            rows.add(row);
        } else {
            long key = firstFree; //使用上次删除的记录的位置
            Row free = rows.get((int) key);
            firstFree = free.getKey();
            row.setKey(key);
            rows.set((int) key, row);
        }
        row.setDeleted(false);
//<<<<<<< HEAD
//        if (database.isMultiVersion()) {
//            if (delta == null) {
//                delta = New.hashSet();
//            }
//            boolean wasDeleted = delta.remove(row); //Row类没有实现hashCode方法，默认采用Objetc.hashCode方法
//            if (!wasDeleted) {
//                delta.add(row);
//            }
//            incrementRowCount(session.getId(), 1);
//        }
//=======
//>>>>>>> d9a7cf0dcb563abb69ed313f35cdebfebe544674
        rowCount++;
    }
    
    //通过Connection.setAutoCommit(false)禁用自动提交事务，就不会每insert或delete一条记录就调用此方法，
    //这样delta中就会有记录了，否则每次调用此方法清除row
    @Override
    public void remove(Session session, Row row) {
        // in-memory
        if (rowCount == 1) {
            rows = Utils.newSmallArrayList();
            firstFree = -1;
        } else {
            Row free = session.createRow(null, 1);
            free.setKey(firstFree);
            long key = row.getKey();
            if (rows.size() <= key) {
                throw DbException.get(ErrorCode.ROW_NOT_FOUND_WHEN_DELETING_1,
                        rows.size() + ": " + key);
            }
            rows.set((int) key, free);
            firstFree = key; //如果连续删两次以上，能通过firstFree和free.key串接成一个未使用的空行列表，这样在add时可以一个个使用
        }
        rowCount--;
    }

    @Override
    public Cursor find(Session session, SearchRow first, SearchRow last) {
        return new ScanCursor(this);
    }

    @Override
    public double getCost(Session session, int[] masks,
            TableFilter[] filters, int filter, SortOrder sortOrder,
            AllColumnsForPlan allColumnsSet) {
        return tableData.getRowCountApproximation() + Constants.COST_ROW_OFFSET;
    }

    @Override
    public long getRowCount(Session session) {
        return rowCount;
    }

    /**
     * Get the next row that is stored after this row.
     *
     * @param row the current row or null to start the scan
     * @return the next row or null if there are no more rows
     */
    Row getNextRow(Row row) {
        long key;
        if (row == null) {
            key = -1;
        } else {
            key = row.getKey();
        }
        while (true) {
            key++;
            if (key >= rows.size()) {
                return null;
            }
            row = rows.get((int) key);
            if (!row.isEmpty()) {
                return row;
            }
        }
    }

    @Override
    public int getColumnIndex(Column col) {
        // the scan index cannot use any columns
        return -1;
    }

    @Override
    public boolean isFirstColumn(Column column) {
        return false;
    }

    @Override
    public void checkRename() {
        throw DbException.getUnsupportedException("SCAN");
    }

    @Override
    public boolean needRebuild() {
        return false;
    }

    @Override
    public boolean canGetFirstOrLast() {
        return false;
    }

    @Override
    public Cursor findFirstOrLast(Session session, boolean first) {
        throw DbException.getUnsupportedException("SCAN");
    }

    @Override
    public long getRowCountApproximation() {
        return rowCount;
    }

    @Override
    public long getDiskSpaceUsed() {
        return 0;
    }

    @Override
    public String getPlanSQL() {
        return table.getSQL() + ".tableScan";
    }

}
