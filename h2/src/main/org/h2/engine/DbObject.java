/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import java.util.ArrayList;
import org.h2.table.Table;

/**
 * A database object such as a table, an index, or a user.
 */
//15个字段，16个方法
public interface DbObject {
	
	//15种数据库对象(从0到14)，
	/*
    Schema掌管7个能带Schema前缀的模式数据库对象
    private final HashMap<String, Table> tablesAndViews;
    private final HashMap<String, Index> indexes;
    private final HashMap<String, Sequence> sequences;
    private final HashMap<String, TriggerObject> triggers;
    private final HashMap<String, Constraint> constraints;
    private final HashMap<String, Constant> constants;
    private final HashMap<String, FunctionAlias> functions;

          而Database类自身掌管了其他8个数据库对象，这些对象因为不是模式数据库对象，所以不能带Schema前缀
    private final HashMap<String, Role> roles = New.hashMap();
    private final HashMap<String, User> users = New.hashMap();
    private final HashMap<String, Setting> settings = New.hashMap();
    private final HashMap<String, Schema> schemas = New.hashMap();
    private final HashMap<String, Right> rights = New.hashMap();
    private final HashMap<String, UserDataType> userDataTypes = New.hashMap();
    private final HashMap<String, UserAggregate> aggregates = New.hashMap();
    private final HashMap<String, Comment> comments = New.hashMap();
    */

    /**
     * The object is of the type table or view.
     */
    int TABLE_OR_VIEW = 0;

    /**
     * This object is an index.
     */
    int INDEX = 1;

    /**
     * This object is a user.
     */
    int USER = 2;

    /**
     * This object is a sequence.
     */
    int SEQUENCE = 3;

    /**
     * This object is a trigger.
     */
    int TRIGGER = 4;

    /**
     * This object is a constraint (check constraint, unique constraint, or
     * referential constraint).
     */
    int CONSTRAINT = 5;

    /**
     * This object is a setting.
     */
    int SETTING = 6;

    /**
     * This object is a role.
     */
    int ROLE = 7;

    /**
     * This object is a right.
     */
    int RIGHT = 8;

    /**
     * This object is an alias for a Java function.
     */
    int FUNCTION_ALIAS = 9;

    /**
     * This object is a schema.
     */
    int SCHEMA = 10;

    /**
     * This object is a constant.
     */
    int CONSTANT = 11;

    /**
     * This object is a domain.
     */
    int DOMAIN = 12;

    /**
     * This object is a comment.
     */
    int COMMENT = 13;

    /**
     * This object is a user-defined aggregate function.
     */
    int AGGREGATE = 14;

    /**
     * This object is a synonym.
     */
    int SYNONYM = 15;

    /**
     * Get the SQL name of this object (may be quoted).
     *
     * @param alwaysQuote quote all identifiers
     * @return the SQL name
     */
    String getSQL(boolean alwaysQuote);

    /**
     * Appends the SQL name of this object (may be quoted) to the specified
     * builder.
     *
     * @param builder
     *            string builder
     * @param alwaysQuote quote all identifiers
     * @return the specified string builder
     */
    StringBuilder getSQL(StringBuilder builder, boolean alwaysQuote);

    /**
     * Get the list of dependent children (for tables, this includes indexes and
     * so on).
     *
     * @return the list of children
     */
    ArrayList<DbObject> getChildren(); //只有Table和User有Children

    /**
     * Get the database.
     *
     * @return the database
     */
    Database getDatabase();

    /**
     * Get the unique object id.
     *
     * @return the object id
     */
    int getId();

    /**
     * Get the name.
     *
     * @return the name
     */
    String getName();

    /**
     * Build a SQL statement to re-create the object, or to create a copy of the
     * object with a different name or referencing a different table
     *
     * @param table the new table
     * @param quotedName the quoted name
     * @return the SQL statement
     */
    String getCreateSQLForCopy(Table table, String quotedName); //用在AlterTableAlterColumn

    /**
     * Construct the original CREATE ... SQL statement for this object.
     *
     * @return the SQL statement
     */
    String getCreateSQL();

    /**
     * Construct a DROP ... SQL statement for this object.
     *
     * @return the SQL statement
     */
    String getDropSQL(); //只看到org.h2.command.dml.ScriptCommand中有使用

    /**
     * Get the object type.
     *
     * @return the object type
     */
    int getType();

    /**
     * Delete all dependent children objects and resources of this object.
     *
     * @param session the session
     */
    void removeChildrenAndResources(Session session);

    /**
     * Check if renaming is allowed. Does nothing when allowed.
     */
    void checkRename();

    /**
     * Rename the object.
     *
     * @param newName the new name
     */
    void rename(String newName);

    /**
     * Check if this object is temporary (for example, a temporary table).
     *
     * @return true if is temporary
     */
    boolean isTemporary();

    /**
     * Tell this object that it is temporary or not.
     *
     * @param temporary the new value
     */
    void setTemporary(boolean temporary);

    /**
     * Change the comment of this object.
     *
     * @param comment the new comment, or null for no comment
     */
    void setComment(String comment);

    /**
     * Get the current comment of this object.
     *
     * @return the comment, or null if not set
     */
    String getComment();

}
