/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import java.util.ArrayList;

import org.h2.engine.Session;
import org.h2.schema.Schema;
import org.h2.table.Column;

/**
 * The data required to create a table.
 */
//总共13个字段，都是默认值(null、false、0等)，只有columns字段不为null
public class CreateTableData {

    /**
     * The schema.
     */
    public Schema schema;

    /**
     * The table name.
     */
    public String tableName;

    /**
     * The object id.
     */
    public int id;

    /**
     * The column list.
     */
    public ArrayList<Column> columns = new ArrayList<>();

    /**
     * Whether this is a temporary table.
     */
    public boolean temporary;

    /**
     * Whether the table is global temporary.
     */
    public boolean globalTemporary;

    /**
     * Whether the indexes should be persisted.
     */
    public boolean persistIndexes; //在CreateTable类的构造函数中默认设为true

    /**
     * Whether the data should be persisted.
     */
    public boolean persistData; //在CreateTable类的构造函数中默认设为true

    /**
     * Whether to create a new table.
     */
    public boolean create;

    /**
     * The session.
     */
    public Session session;

    /**
     * The table engine to use for creating the table.
     */
    public String tableEngine;

    /**
     * The table engine params to use for creating the table.
     */
    public ArrayList<String> tableEngineParams;

    /**
     * The table is hidden.
     */
    public boolean isHidden;
    
	public String toString() { // 我加上的
		return "CreateTableData[id=" + id + ", tableName=" + tableName
				+ ", create=" + create + ", persistIndexes=" + persistIndexes
				+ ", persistData=" + persistData + "]";
	}

}
