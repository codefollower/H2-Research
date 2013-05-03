/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import org.h2.constant.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.DbObject;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.schema.SchemaObjectBase;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.MathUtils;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

/**
 * Most index implementations extend the base index.
 */
public abstract class BaseIndex extends SchemaObjectBase implements Index {

    protected IndexColumn[] indexColumns;
    protected Column[] columns;
    protected int[] columnIds;
    protected Table table;
    protected IndexType indexType;
    protected boolean isMultiVersion;

    /**
     * Initialize the base index.
     *
     * @param newTable the table
     * @param id the object id
     * @param name the index name
     * @param newIndexColumns the columns that are indexed or null if this is
     *            not yet known
     * @param newIndexType the index type
     */
    protected void initBaseIndex(Table newTable, int id, String name,
            IndexColumn[] newIndexColumns, IndexType newIndexType) {
        initSchemaObjectBase(newTable.getSchema(), id, name, Trace.INDEX);
        this.indexType = newIndexType;
        this.table = newTable;
        if (newIndexColumns != null) {
            this.indexColumns = newIndexColumns;
            columns = new Column[newIndexColumns.length];
            int len = columns.length;
            columnIds = new int[len];
            for (int i = 0; i < len; i++) {
                Column col = newIndexColumns[i].column;
                columns[i] = col;
                columnIds[i] = col.getColumnId();
            }
        }
    }

    public String getDropSQL() {
        return null;
    }

    /**
     * Create a duplicate key exception with a message that contains the index
     * name.
     *
     * @return the exception
     */
    protected DbException getDuplicateKeyException() {
        String sql = getName() + " ON " + table.getSQL() + "(" + getColumnListSQL() + ")";
        DbException e = DbException.get(ErrorCode.DUPLICATE_KEY_1, sql);
        e.setSource(this);
        return e;
    }

    public String getPlanSQL() {
        return getSQL();
    }

    public void removeChildrenAndResources(Session session) {
        table.removeIndex(this);
        remove(session);
        database.removeMeta(session, getId());
    }

    public boolean canFindNext() {
        return false;
    }


    public Cursor find(TableFilter filter, SearchRow first, SearchRow last) {
        return find(filter.getSession(), first, last);
    }

    /**
     * Find a row or a list of rows that is larger and create a cursor to
     * iterate over the result. The base implementation doesn't support this feature.
     *
     * @param session the session
     * @param higherThan the lower limit (excluding)
     * @param last the last row, or null for no limit
     * @return the cursor
     * @throws DbException always
     */
    public Cursor findNext(Session session, SearchRow higherThan, SearchRow last) {
        throw DbException.throwInternalError();
    }

    /**
     * Calculate the cost for the given mask as if this index was a typical
     * b-tree range index. This is the estimated cost required to search one
     * row, and then iterate over the given number of rows.
     *
     * @param masks the search mask
     * @param rowCount the number of rows in the index
     * @param sortOrder the sort order
     * @return the estimated cost
     */
    protected long getCostRangeIndex(int[] masks, long rowCount, SortOrder sortOrder) { //无子类覆盖
        rowCount += Constants.COST_ROW_OFFSET;
        long cost = rowCount;
        long rows = rowCount;
        int totalSelectivity = 0;
        if (masks == null) {
            return cost;
        }
        for (int i = 0, len = columns.length; i < len; i++) {
            Column column = columns[i];
            int index = column.getColumnId();
            int mask = masks[index];
            //代价比较:
            //EQUALITY < RANGE < END < START
            //如果索引字段列表的第一个字段在Where中是RANGE、START、END，那么索引字段列表中的其他字段就不需要再计算cost了，
            //如果是EQUALITY，则还可以继续计算cost，rows变量的值会变小，cost也会变小
            if ((mask & IndexCondition.EQUALITY) == IndexCondition.EQUALITY) {
            	//索引字段列表中的最后一个在where当中是EQUALITY，确此索引是唯一索引时，cost直接是3
            	//因为如果最后一个索引字段是EQUALITY，说明前面的字段全是EQUALITY，
            	//如果是唯一索引则rowCount / distinctRows是1，所以rows = Math.max(rowCount / distinctRows, 1)=1
            	//所以cost = 2 + rows = 3
                if (i == columns.length - 1 && getIndexType().isUnique()) {
                    cost = 3;
                    break;
                }
                totalSelectivity = 100 - ((100 - totalSelectivity) * (100 - column.getSelectivity()) / 100);
                long distinctRows = rowCount * totalSelectivity / 100; //totalSelectivity变大时distinctRows变大
                if (distinctRows <= 0) {
                    distinctRows = 1;
                }
                rows = Math.max(rowCount / distinctRows, 1); //distinctRows变大，则rowCount / distinctRows变小，rows也变小
                cost = 2 + rows; //rows也变小，所以cost也变小
            } else if ((mask & IndexCondition.RANGE) == IndexCondition.RANGE) { //见TableFilter.getBestPlanItem中的注释
                cost = 2 + rows / 4;
                break;
            } else if ((mask & IndexCondition.START) == IndexCondition.START) {
                cost = 2 + rows / 3;
                break;
            } else if ((mask & IndexCondition.END) == IndexCondition.END) { //"<="的代价要小于">="
                cost = rows / 3;
                break;
            } else {
                break;
            }
        }
        // if the ORDER BY clause matches the ordering of this index,
        // it will be cheaper than another index, so adjust the cost accordingly
        if (sortOrder != null) {
            int[] columnIndexes = new int[ indexColumns.length ];
            int[] columnSortTypes = new int[ indexColumns.length ];
            for (int i = 0, len = indexColumns.length; i < len; i++) {
                columnIndexes[i] = indexColumns[i].column.getColumnId();
                columnSortTypes[i] = indexColumns[i].sortType;
            }
            boolean sortOrderMatches = true;
            int[] sortOrderIndexes = sortOrder.getIndexes();
            //sortOrderIndexes = sortOrder.getColumnIndexes();
            int coveringCount = 0;
            for (int i = 0, len = sortOrderIndexes.length; i < len; i++) {
                if (i >= columnIndexes.length) {
                    // we can still use this index if we are sorting by more than it's columns
                    break;
                }
                //这样的比较其实是无效的，columnIndexes[i]得到的是列id，
                //但是sortOrderIndexes[i]得到的是select字段列表中的位置，并不是列id
                if (columnIndexes[i] != sortOrderIndexes[i] || columnSortTypes[i] != sortOrder.getSortTypes()[i]) {
                    sortOrderMatches = false;
                    break;
                }
                coveringCount++;
            }
            if (sortOrderMatches) {
                // "coveringCount" makes sure that when we have two
                // or more covering indexes, we choose the one
                // that covers more
                cost -= coveringCount;
            }
        }
        return cost;
    }

    public int compareRows(SearchRow rowData, SearchRow compare) { //只比较索引字段，并不一定是所有字段
        if (rowData == compare) {
            return 0;
        }
        for (int i = 0, len = indexColumns.length; i < len; i++) {
            int index = columnIds[i];
            Value v = compare.getValue(index);
            if (v == null) { //只要compare中有null值就认为无法比较，直接认为rowData和compare相等(通常在查询时在where中再比较)
                // can't compare further
                return 0;
            }
            int c = compareValues(rowData.getValue(index), v, indexColumns[i].sortType);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /**
     * Check if one of the columns is NULL and multiple rows with NULL are
     * allowed using the current compatibility mode for unique indexes. Note:
     * NULL behavior is complicated in SQL.
     *
     * @param newRow the row to check
     * @return true if one of the columns is null and multiple nulls in unique
     *         indexes are allowed
     */
    protected boolean containsNullAndAllowMultipleNull(SearchRow newRow) {
        Mode mode = database.getMode();
        //1. 对于唯一索引，必须完全唯一，适用于Derby/HSQLDB/MSSQLServer
        if (mode.uniqueIndexSingleNull) {
        	//不允许出现:
        	//(x, null)
        	//(x, null)
        	//也不允许出现:
        	//(null, null)
        	//(null, null)
            return false;
        } else if (mode.uniqueIndexSingleNullExceptAllColumnsAreNull) {
        	//2. 对于唯一索引，索引记录可以全为null，适用于Oracle
        	
        	//不允许出现:
        	//(x, null)
        	//(x, null)
        	//但是允许出现:
        	//(null, null)
        	//(null, null)
            for (int index : columnIds) {
                Value v = newRow.getValue(index);
                if (v != ValueNull.INSTANCE) {
                    return false;
                }
            }
            return true;
        }
        //3. 对于唯一索引，只要一个为null，就是合法的，适用于REGULAR(即H2)/DB2/MySQL/PostgreSQL
        
        //即允许出现:
    	//(x, null)
    	//(x, null)
    	//也允许出现:
    	//(null, null)
    	//(null, null)
        
        //也就是说，只要相同的两条索引记录包含null即可
        for (int index : columnIds) {
            Value v = newRow.getValue(index);
            if (v == ValueNull.INSTANCE) {
                return true;
            }
        }
        
        //4. 对于唯一索引，没有null时是不允许出现两条相同的索引记录的
        return false;
    }

    /**
     * Compare the positions of two rows.
     *
     * @param rowData the first row
     * @param compare the second row
     * @return 0 if both rows are equal, -1 if the first row is smaller, otherwise 1
     */
    int compareKeys(SearchRow rowData, SearchRow compare) {
        long k1 = rowData.getKey();
        long k2 = compare.getKey();
        if (k1 == k2) {
            if (isMultiVersion) {
                int v1 = rowData.getVersion();
                int v2 = compare.getVersion();
                return MathUtils.compareInt(v2, v1);
            }
            return 0;
        }
        return k1 > k2 ? 1 : -1;
    }

    private int compareValues(Value a, Value b, int sortType) {
        if (a == b) {
            return 0;
        }
        boolean aNull = a == null, bNull = b == null;
        if (aNull || bNull) {
            return SortOrder.compareNull(aNull, sortType);
        }
        int comp = table.compareTypeSave(a, b);
        if ((sortType & SortOrder.DESCENDING) != 0) { //降序时，把比较结果反过来
            comp = -comp;
        }
        return comp;
    }

    public int getColumnIndex(Column col) { //并不是返回列id，而是索引字段列表中的位置
        for (int i = 0, len = columns.length; i < len; i++) {
            if (columns[i].equals(col)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the list of columns as a string.
     *
     * @return the list of columns
     */
    private String getColumnListSQL() {
        StatementBuilder buff = new StatementBuilder();
        for (IndexColumn c : indexColumns) {
            buff.appendExceptFirst(", ");
            buff.append(c.getSQL());
        }
        return buff.toString();
    }

    public String getCreateSQLForCopy(Table targetTable, String quotedName) {
        StringBuilder buff = new StringBuilder("CREATE ");
        buff.append(indexType.getSQL());
        buff.append(' ');
        if (table.isHidden()) {
            buff.append("IF NOT EXISTS ");
        }
        buff.append(quotedName);
        buff.append(" ON ").append(targetTable.getSQL());
        if (comment != null) {
            buff.append(" COMMENT ").append(StringUtils.quoteStringSQL(comment));
        }
        buff.append('(').append(getColumnListSQL()).append(')');
        return buff.toString();
    }

    public String getCreateSQL() {
        return getCreateSQLForCopy(table, getSQL());
    }

    public IndexColumn[] getIndexColumns() {
        return indexColumns;
    }

    public Column[] getColumns() {
        return columns;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public int getType() {
        return DbObject.INDEX;
    }

    public Table getTable() {
        return table;
    }

    public void commit(int operation, Row row) {
        // nothing to do
    }

    void setMultiVersion(boolean multiVersion) {
        this.isMultiVersion = multiVersion;
    }

    public Row getRow(Session session, long key) {
        throw DbException.getUnsupportedException(toString());
    }

    public boolean isHidden() {
        return table.isHidden();
    }

    public boolean isRowIdIndex() { //只有org.h2.mvstore.db.MVPrimaryIndex和org.h2.index.PageDataIndex返回true
        return false;
    }

    public boolean canScan() {
        return true;
    }

    public void setSortedInsertMode(boolean sortedInsertMode) { //只有org.h2.index.PageIndex覆盖
        // ignore
    }

}
