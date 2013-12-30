/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.h2.constant.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.db.TransactionStore.Transaction;
import org.h2.mvstore.db.TransactionStore.TransactionMap;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

/**
 * A table stored in a MVStore.
 */
//每个MVTable只对应一个MVPrimaryIndex的实例，MVPrimaryIndex用来存放表的所有记录
//MVPrimaryIndex这个名字起得有点误导性，它的作用跟PageDataIndex一样，
//并不是完全对应primary key，只有当primary key只有一个字段，并且此字段是byte、short、int、long类型时，
//且最初的MVPrimaryIndex还没有加入记录，才通过MVDelegateIndex指向MVPrimaryIndex
//其它的primary key都是用MVSecondaryIndex实现
public class MVPrimaryIndex extends BaseIndex {

    private final MVTable mvTable;
    private final String mapName;
    private TransactionMap<Value, Value> dataMap;
    private long lastKey;
    private int mainIndexColumn = -1;

    public MVPrimaryIndex(Database db, MVTable table, int id, IndexColumn[] columns,
                IndexType indexType) {
        this.mvTable = table;
        //也可建立与table.getName() + "_DATA"同名的索引，但是不会冲突，
        //因为内部做了特殊处理，MVPrimaryIndex是不可见的，也不放入Meta表中
        initBaseIndex(table, id, table.getName() + "_DATA", columns, indexType);
        int[] sortTypes = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            sortTypes[i] = SortOrder.ASCENDING; //默认都用升序,而MVSecondaryIndex用字段原有的排序方式
        }
        //用于序列化时, key都是long，所以用不到db.getCompareMode(), db, sortTypes(数组类型用到)
        ValueDataType keyType = new ValueDataType(
                null, null, null);
        ValueDataType valueType = new ValueDataType(
                db.getCompareMode(), db, sortTypes);
        //这个mapBuilder传给openMap时仅仅是为了传递keyType、valueType，并没调用create()
        mapName = "table." + getId();
        MVMap.Builder<Value, Value> mapBuilder = new MVMap.Builder<Value, Value>().
                keyType(keyType).
                valueType(valueType);
        //mvTable.getTransaction(null)会创建一个新的Transaction对象，
        //不过创建新的Transaction对象并不会对以后的事务有什么影响，这个新的Transaction对象是临时的，并不需要commit或rollback
        //此后调用getMap(Session session)时都会由此dataMap生成一个新的MVMap
        dataMap = mvTable.getTransaction(null).openMap(mapName, mapBuilder);
        Value k = dataMap.lastKey();
        lastKey = k == null ? 0 : k.getLong();
    }

    @Override
    public String getCreateSQL() {
        return null; //每个表默认都有个MVPrimaryIndex，并不需要显示创建，所以也不需要Create SQL
    }

    @Override
    public String getPlanSQL() {
        return table.getSQL() + ".tableScan";
    }

    public void setMainIndexColumn(int mainIndexColumn) { //跟org.h2.index.PageDataIndex.mainIndexColumn一样
        this.mainIndexColumn = mainIndexColumn;
    }

    public int getMainIndexColumn() {
        return mainIndexColumn;
    }

    @Override
    public void close(Session session) {
        // ok
    }

    @Override
    public void add(Session session, Row row) {        
        if (mainIndexColumn == -1) { //没有mainIndexColumn时自动生成记录唯一行key
        	//TODO 没看到哪里把rowKey设为非0值
            if (row.getKey() == 0) {
                row.setKey(++lastKey); //当更新记录时，会删除原来的记录，再插入新记录，但是新记录的rowKey是0，不会沿用原来的rowKey
            }
        } else { //否则使用mainIndexColumn的值
            long c = row.getValue(mainIndexColumn).getLong(); //因为主键列不会为null，所以这里肯定能取到非null值
            row.setKey(c);
        }

        if (mvTable.getContainsLargeObject()) {
            for (int i = 0, len = row.getColumnCount(); i < len; i++) {
                Value v = row.getValue(i);
                Value v2 = v.link(database, getId());
                if (v2.isLinked()) {
                    session.unlinkAtCommitStop(v2);
                }
                if (v != v2) {
                    row.setValue(i, v2);
                }
            }
        }

        TransactionMap<Value, Value> map = getMap(session);
        Value key = ValueLong.get(row.getKey());
        Value old = map.getLatest(key);
        if (old != null) {
            String sql = "PRIMARY KEY ON " + table.getSQL();
            if (mainIndexColumn >= 0 && mainIndexColumn < indexColumns.length) {
                sql +=  "(" + indexColumns[mainIndexColumn].getSQL() + ")";
            }
            DbException e = DbException.get(ErrorCode.DUPLICATE_KEY_1, sql);
            e.setSource(this);
            throw e;
        }
        try {
            map.put(key, ValueArray.get(row.getValueList()));
        } catch (IllegalStateException e) {
            throw DbException.get(ErrorCode.CONCURRENT_UPDATE_1, table.getName());
        }
        lastKey = Math.max(lastKey, row.getKey());
    }

    @Override
    public void remove(Session session, Row row) {
        if (mvTable.getContainsLargeObject()) {
            for (int i = 0, len = row.getColumnCount(); i < len; i++) {
                Value v = row.getValue(i);
                if (v.isLinked()) {
                    session.unlinkAtCommit(v);
                }
            }
        }
        TransactionMap<Value, Value> map = getMap(session);
        try {
            Value old = map.remove(ValueLong.get(row.getKey()));
            if (old == null) {
                throw DbException.get(ErrorCode.ROW_NOT_FOUND_WHEN_DELETING_1,
                        getSQL() + ": " + row.getKey());
            }
        } catch (IllegalStateException e) {
            throw DbException.get(ErrorCode.CONCURRENT_UPDATE_1, table.getName());
        }
    }

    @Override
    public Cursor find(Session session, SearchRow first, SearchRow last) {
        long min, max;
        //当mainIndexColumn < 0时说明没有设PRIMARY KEY，此时用自动生成的key，
        //所以最小值不能精确指定，用Long.MIN_VALUE更合适
        if (first == null || mainIndexColumn < 0) {
            min = Long.MIN_VALUE;
        } else { //只有first != && mainIndexColumn >= 0时才转到else分枝
            Value v = first.getValue(mainIndexColumn);
            if (v == null) {
                min = 0;
            } else {
                min = v.getLong(); //不管PRIMARY KEY是byte、short、int都转到long
            }
        }
        //同上
        if (last == null || mainIndexColumn < 0) {
            max = Long.MAX_VALUE;
        } else {
            Value v = last.getValue(mainIndexColumn);
            if (v == null) {
                max = Long.MAX_VALUE;
            } else {
                max = v.getLong();
            }
        }
        TransactionMap<Value, Value> map = getMap(session);
        //map.keyIterator(ValueLong.get(min))得到的是一个从min开始的key列表，不是值
        return new MVStoreCursor(session, map.keyIterator(
                ValueLong.get(min)), max);
    }

    @Override
    public MVTable getTable() {
        return mvTable;
    }

    @Override
    public Row getRow(Session session, long key) {
        TransactionMap<Value, Value> map = getMap(session);
        Value v = map.get(ValueLong.get(key));
        ValueArray array = (ValueArray) v;
        Row row = new Row(array.getList(), 0);
        row.setKey(key);
        return row;
    }

    @Override
    public double getCost(Session session, int[] masks, SortOrder sortOrder) { //相当于全表扫描，所以代价很大
        try {
            long cost = 10 * (dataMap.map.getSize() + Constants.COST_ROW_OFFSET);
            return cost;
        } catch (IllegalStateException e) {
            throw DbException.get(ErrorCode.OBJECT_CLOSED);
        }
    }

    @Override
    public int getColumnIndex(Column col) {
        // can not use this index - use the delegate index instead
        return -1;
    }


    @Override
    public void remove(Session session) {
        TransactionMap<Value, Value> map = getMap(session);
        if (!map.isClosed()) {
            map.removeMap();
        }
    }

    @Override
    public void truncate(Session session) {
        TransactionMap<Value, Value> map = getMap(session);
        if (mvTable.getContainsLargeObject()) {
            database.getLobStorage().removeAllForTable(table.getId());
        }
        map.clear();
    }

    @Override
    public boolean canGetFirstOrLast() {
        return true;
    }

    @Override
    public Cursor findFirstOrLast(Session session, boolean first) {
        TransactionMap<Value, Value> map = getMap(session);
        Value v = first ? map.firstKey() : map.lastKey();
        if (v == null) {
            return new MVStoreCursor(session, Collections.<Value>emptyList().iterator(), 0);
        }
        long key = v.getLong();
        MVStoreCursor cursor = new MVStoreCursor(session,
                Arrays.asList((Value) ValueLong.get(key)).iterator(), key);
        cursor.next();
        return cursor;
    }

    @Override
    public boolean needRebuild() {
        return false;
    }

    @Override
    public long getRowCount(Session session) {
        TransactionMap<Value, Value> map = getMap(session);
        return map.getSize();
    }

    @Override
    public long getRowCountApproximation() {
        try {
            return dataMap.map.getSize();
        } catch (IllegalStateException e) {
            throw DbException.get(ErrorCode.OBJECT_CLOSED);
        }
    }

    @Override
    public long getDiskSpaceUsed() {
        // TODO estimate disk space usage
        return 0;
    }

    public String getMapName() {
        return mapName;
    }

    @Override
    public void checkRename() {
        // ok
    }

    /**
     * Get the key from the row.
     *
     * @param row the row
     * @param ifEmpty the value to use if the row is empty
     * @param ifNull the value to use if the column is NULL
     * @return the key
     */
    //见MVDelegateIndex.find(Session, SearchRow, SearchRow)的注释
    long getKey(SearchRow row, long ifEmpty, long ifNull) {
        if (row == null) {
            return ifEmpty;
        }
        Value v = row.getValue(mainIndexColumn);
        if (v == null) {
            throw DbException.throwInternalError(row.toString());
        } else if (v == ValueNull.INSTANCE) {
            return ifNull;
        }
        return v.getLong();
    }

    /**
     * Search for a specific row or a set of rows.
     *
     * @param session the session
     * @param first the key of the first row
     * @param last the key of the last row
     * @return the cursor
     */
    Cursor find(Session session, long first, long last) {
        TransactionMap<Value, Value> map = getMap(session);
        return new MVStoreCursor(session, map.keyIterator(ValueLong.get(first)), last);
    }

    @Override
    public boolean isRowIdIndex() {
        return true;
    }

    /**
     * Get the map to store the data.
     *
     * @param session the session
     * @return the map
     */
    //只有在renameTable方法中使用getMap(null)，
    //此时得到最原始的TransactionMap，其他情况都是得到原始TransactionMap的一个快照
    TransactionMap<Value, Value> getMap(Session session) {
        if (session == null) {
            return dataMap;
        }
        Transaction t = mvTable.getTransaction(session); //会使用Session中的Transaction对象
        return dataMap.getInstance(t, Long.MAX_VALUE); //会得到一个dataMap，但是并不会对dataMap进行复制
    }

    /**
     * A cursor.
     */
    class MVStoreCursor implements Cursor {

        private final Session session;
        private final Iterator<Value> it;
        private final long last;
        private ValueLong current;
        private Row row;

        //it是一个key列表，不是值
        public MVStoreCursor(Session session, Iterator<Value> it, long last) {
            this.session = session;
            this.it = it;
            this.last = last;
        }

        @Override
        public Row get() {
            if (row == null) {
                if (current != null) {
                    row = getRow(session, current.getLong()); //按key从map中取值
                }
            }
            return row;
        }

        @Override
        public SearchRow getSearchRow() {
            return get();
        }

        @Override
        public boolean next() {
            current = (ValueLong) it.next();
            if (current != null && current.getLong() > last) {
                current = null;
            }
            row = null;
            return current != null;
        }

        @Override
        public boolean previous() {
            // TODO previous
            return false;
        }

    }

}
