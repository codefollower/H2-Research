/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore.db;

import org.h2.engine.Session;
import org.h2.index.BaseIndex;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;

/**
 * An index that delegates indexing to another index.
 */
//如果创建一个只有一个字段的primary key索引，在适当的时候并不需要构造一个新的MVSecondaryIndex实例，
//这时用这个类来表示一个假的primary key，委派给org.h2.mvstore.db.MVTable.primaryIndex即可
//此类只负责查询类的方法转发，更新类的方法直接忽略，因为在MVPrimaryIndex时己做了。
public class MVDelegateIndex extends BaseIndex {

    private final MVPrimaryIndex mainIndex;

    public MVDelegateIndex(MVTable table, int id, String name,
            MVPrimaryIndex mainIndex,
            IndexType indexType) {
    	//只有一列
        IndexColumn[] cols = IndexColumn.wrap(new Column[] { table.getColumn(mainIndex.getMainIndexColumn())});
        this.initBaseIndex(table, id, name, cols, indexType);
        this.mainIndex = mainIndex;
        if (id < 0) {
            throw DbException.throwInternalError("" + name);
        }
    }

    public void add(Session session, Row row) {
        // nothing to do
    }

    public boolean canGetFirstOrLast() {
        return true;
    }

    public void close(Session session) {
        // nothing to do
    }

    public Cursor find(Session session, SearchRow first, SearchRow last) {
        long min = mainIndex.getKey(first, Long.MIN_VALUE, Long.MIN_VALUE);
        // ifNull is MIN_VALUE as well, because the column is never NULL
        // so avoid returning all rows (returning one row is OK)
        //如果last是null，就相当于没有last限制，所以用Long.MAX_VALUE
        //如果last中的key值是ValueNull.INSTANCE，显然是不合理的，设为Long.MIN_VALUE，什么值都不返回
        long max = mainIndex.getKey(last, Long.MAX_VALUE, Long.MIN_VALUE);
        return mainIndex.find(session, min, max);
    }

    public Cursor findFirstOrLast(Session session, boolean first) {
        return mainIndex.findFirstOrLast(session, first);
    }

    public int getColumnIndex(Column col) {
        if (col.getColumnId() == mainIndex.getMainIndexColumn()) {
            return 0;
        }
        return -1;
    }

    //跟MVSecondaryIndex的一样，因为本身就相当于一个primary key的MVSecondaryIndex
    public double getCost(Session session, int[] masks, SortOrder sortOrder) {
        return 10 * getCostRangeIndex(masks, mainIndex.getRowCount(session), sortOrder);
    }

    public boolean needRebuild() {
        return false;
    }

    public void remove(Session session, Row row) {
        // nothing to do
    }

    public void remove(Session session) {
        mainIndex.setMainIndexColumn(-1);
    }

    public void truncate(Session session) {
        // nothing to do
    }

    public void checkRename() {
        // ok
    }

    public long getRowCount(Session session) {
        return mainIndex.getRowCount(session);
    }

    public long getRowCountApproximation() {
        return mainIndex.getRowCountApproximation();
    }

    public long getDiskSpaceUsed() {
        return 0;
    }

}
