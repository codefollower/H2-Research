/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import org.h2.table.IndexColumn;
import org.h2.table.Table;

/**
 * A page store index.
 */
public abstract class PageIndex extends BaseIndex {

    /**
     * The root page of this index.
     */
	//每次增加记录时都会按rootPageId找出第一个页面，然后从此页面比较row key，在适当位置插入此行，
	//这个适当位置有很多种情况，通常最开始时rootPageId指向的是一个PageBtreeLeaf，所以记录会放在PageBtreeLeaf中，
	//当记当越来越多，一个PageBtreeLeaf放不下时，会把此PageBtreeLeaf切割，一分为二，再新建一个PageBtreeNode作为它们俩的父结点，
	//此时rootPageId就代表此父结点的页面id，同时此PageBtreeNode会记下一个分隔key，用来识别它下面的字节点哪些记录在哪个子节点，
	//当下次再增加新记录时，会按此记录的row key与分隔key比较，确定此新记录放到哪个子节点中，如果子节点又是一个PageBtreeNode，
	//那么执行同样的操作，直到找到一个PageBtreeLeaf为止
    protected int rootPageId;
    
    //只在org.h2.index.PageDataLeaf中用到，
    //如insert into IndexTestTable(id, name, address) SORTED values(...)
    private boolean sortedInsertMode; 

    /**
     * Initialize the page store index.
     *
     * @param newTable the table
     * @param id the object id
     * @param name the index name
     * @param newIndexColumns the columns that are indexed or null if this is
     *            not yet known
     * @param newIndexType the index type
     */
    protected PageIndex(Table newTable, int id, String name, IndexColumn[] newIndexColumns, IndexType newIndexType) {
        super(newTable, id, name, newIndexColumns, newIndexType);
    }

    /**
     * Get the root page of this index.
     *
     * @return the root page id
     */
    public int getRootPageId() {
        return rootPageId;
    }

    /**
     * Write back the row count if it has changed.
     */
    public abstract void writeRowCount(); //只有PageDataNode、PageBtreeNode才存rowCount

    @Override
    public void setSortedInsertMode(boolean sortedInsertMode) {
        this.sortedInsertMode = sortedInsertMode;
    }

    boolean isSortedInsertMode() {
        return sortedInsertMode;
    }

}
