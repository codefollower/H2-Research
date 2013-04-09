/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.table;

import java.util.ArrayList;
import org.h2.command.Prepared;
import org.h2.command.dml.Query;
import org.h2.constant.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.ViewIndex;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.util.IntArray;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.SynchronizedVerifier;
import org.h2.util.Utils;
import org.h2.value.Value;

/**
 * A view is a virtual table that is defined by a query.
 */
/*
         有三种产生TableView的方式:
	1. CREATE VIEW语句

	如: CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view COMMENT IS 'my view'(f1,f2) 
			AS SELECT id,name FROM CreateViewTest


	2. 嵌入在FROM中的临时视图

	如: select id,name from (select id,name from CreateViewTest)


	3. WITH RECURSIVE语句

	如: WITH RECURSIVE my_tmp_table(f1,f2) 
			AS (select id,name from CreateViewTest UNION ALL select 1, 2) 
				select f1, f2 from my_tmp_table
	RECURSIVE这个关键字是可选的


	只有2可以带Parameters，它是通过这个方法调用: org.h2.table.TableView.createTempView(Session, User, String, Query, Query)
	只有3的recursive是true 
 */
public class TableView extends Table {

    private static final long ROW_COUNT_APPROXIMATION = 100;

    private String querySQL;
    private ArrayList<Table> tables;
    private String[] columnNames;
    private Query viewQuery;
    private ViewIndex index;
    private boolean recursive;
    private DbException createException;
    private final SmallLRUCache<IntArray, ViewIndex> indexCache =
            SmallLRUCache.newInstance(Constants.VIEW_INDEX_CACHE_SIZE);
    private long lastModificationCheck;
    private long maxDataModificationId;
    private User owner;
    private Query topQuery;
    private LocalResult recursiveResult;
    private boolean tableExpression;

    public TableView(Schema schema, int id, String name, String querySQL, ArrayList<Parameter> params, String[] columnNames,
            Session session, boolean recursive) {
        super(schema, id, name, false, true);
        init(querySQL, params, columnNames, session, recursive);
    }

    /**
     * Try to replace the SQL statement of the view and re-compile this and all
     * dependent views.
     *
     * @param querySQL the SQL statement
     * @param columnNames the column names
     * @param session the session
     * @param recursive whether this is a recursive view
     * @param force if errors should be ignored
     */
    public void replace(String querySQL, String[] columnNames, Session session, boolean recursive, boolean force) {
        String oldQuerySQL = this.querySQL;
        String[] oldColumnNames = this.columnNames;
        boolean oldRecursive = this.recursive;
        //init里执行了一次initColumnsAndTables，虽然执行了两次initColumnsAndTables，但是init中还建立了ViewIndex
        //所以这里是必须调用init的
        init(querySQL, null, columnNames, session, recursive);
        //recompile里又执行了一次initColumnsAndTables
        DbException e = recompile(session, force);
        
        //如果失败了，按原来的重新来过
        if (e != null) {
            init(oldQuerySQL, null, oldColumnNames, session, oldRecursive);
            recompile(session, true);
            throw e;
        }
    }

    private synchronized void init(String querySQL, ArrayList<Parameter> params,
            String[] columnNames, Session session, boolean recursive) {
        this.querySQL = querySQL;
        this.columnNames = columnNames;
        this.recursive = recursive;
        index = new ViewIndex(this, querySQL, params, recursive);
        SynchronizedVerifier.check(indexCache);
        indexCache.clear();
        initColumnsAndTables(session);
    }

    private static Query compileViewQuery(Session session, String sql) {
        Prepared p = session.prepare(sql);
        if (!(p instanceof Query)) {
            throw DbException.getSyntaxError(sql, 0);
        }
        return (Query) p;
    }

    /**
     * Re-compile the view query and all views that depend on this object.
     *
     * @param session the session
     * @param force if exceptions should be ignored
     * @return the exception if re-compiling this or any dependent view failed
     *         (only when force is disabled)
     */
    public synchronized DbException recompile(Session session, boolean force) {
        try {
            compileViewQuery(session, querySQL);
        } catch (DbException e) {
            if (!force) {
                return e;
            }
        }
        
        //如下:
        //CREATE OR REPLACE FORCE VIEW my_view(f1,f2) AS SELECT id,name FROM CreateViewTest
		//CREATE OR REPLACE FORCE VIEW view1 AS SELECT f1 FROM my_view
		//CREATE OR REPLACE FORCE VIEW view2 AS SELECT f2 FROM my_view
        //view1和view2是建立在my_view这个视图之上，当要recompile my_view时，因为view1和view2依赖了my_view
        //所以要对他们重新recompile，这里getViews()返回view1和view2
        ArrayList<TableView> views = getViews();
        if (views != null) {
            views = New.arrayList(views);
        }
        SynchronizedVerifier.check(indexCache);
        indexCache.clear();
        initColumnsAndTables(session);
        if (views != null) {
            for (TableView v : views) {
                DbException e = v.recompile(session, force);
                if (e != null && !force) {
                    return e;
                }
            }
        }
        return force ? null : createException;
    }

    private void initColumnsAndTables(Session session) {
        Column[] cols;
        removeViewFromTables();
        try {
            Query query = compileViewQuery(session, querySQL); //重新对select语句进行解析和prepare
            this.querySQL = query.getPlanSQL();
            tables = New.arrayList(query.getTables());
            ArrayList<Expression> expressions = query.getExpressions();
            ArrayList<Column> list = New.arrayList();
            
    		//select字段个数比view字段多的情况，多出来的按select字段原来的算
    		//这里实际是f1、name
    		//sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1) " //
    		//		+ "AS SELECT id,name FROM CreateViewTest";

    		//select字段个数比view字段少的情况，view中少的字段被忽略
    		//这里实际是f1，而f2被忽略了，也不提示错误
    		//sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
    		//		+ "AS SELECT id FROM CreateViewTest";

    		//不管加不加FORCE，跟上面也一样
    		//sql = "CREATE OR REPLACE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
    		//		+ "AS SELECT id FROM CreateViewTest";
            
            //expressions.size有可能大于query.getColumnCount()，因为query.getColumnCount()不包含group by等额外加进来的表达式
            for (int i = 0, count = query.getColumnCount(); i < count; i++) {
                Expression expr = expressions.get(i);
                String name = null;
                //如CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view AS SELECT id,name FROM CreateViewTest
                //没有为视图指定字段的时候，用select字段列表中的名字，此时columnNames==null
                if (columnNames != null && columnNames.length > i) {
                    name = columnNames[i];
                }
                if (name == null) {
                    name = expr.getAlias();
                }
                int type = expr.getType();
                long precision = expr.getPrecision();
                int scale = expr.getScale();
                int displaySize = expr.getDisplaySize();
                Column col = new Column(name, type, precision, scale, displaySize);
                col.setTable(this, i);
                list.add(col);
            }
            cols = new Column[list.size()];
            list.toArray(cols);
            createException = null;
            viewQuery = query;
        } catch (DbException e) {
            e.addSQL(getCreateSQL());
            createException = e;
            // if it can't be compiled, then it's a 'zero column table'
            // this avoids problems when creating the view when opening the
            // database
            tables = New.arrayList();
            cols = new Column[0];
            if (recursive && columnNames != null) {
                cols = new Column[columnNames.length];
                for (int i = 0; i < columnNames.length; i++) {
                    cols[i] = new Column(columnNames[i], Value.STRING);
                }
                index.setRecursive(true);
                createException = null;
            }
        }
        setColumns(cols);
        if (getId() != 0) {
            addViewToTables();
        }
    }

    /**
     * Check if this view is currently invalid.
     *
     * @return true if it is
     */
    public boolean isInvalid() {
        return createException != null;
    }

    public synchronized PlanItem getBestPlanItem(Session session, int[] masks, SortOrder sortOrder) {
        PlanItem item = new PlanItem();
        item.cost = index.getCost(session, masks, sortOrder);
        IntArray masksArray = new IntArray(masks == null ? Utils.EMPTY_INT_ARRAY : masks);
        SynchronizedVerifier.check(indexCache);
        ViewIndex i2 = indexCache.get(masksArray);
        if (i2 == null || i2.getSession() != session) {
            i2 = new ViewIndex(this, index, session, masks);
            indexCache.put(masksArray, i2);
        }
        item.setIndex(i2);
        return item;
    }

    public String getDropSQL() {
        return "DROP VIEW IF EXISTS " + getSQL() + " CASCADE";
    }

    public String getCreateSQLForCopy(Table table, String quotedName) {
        return getCreateSQL(false, true, quotedName);
    }


    public String getCreateSQL() {
        return getCreateSQL(false, true);
    }

    /**
     * Generate "CREATE" SQL statement for the view.
     *
     * @param orReplace if true, then include the OR REPLACE clause
     * @param force if true, then include the FORCE clause
     * @return the SQL statement
     */
    public String getCreateSQL(boolean orReplace, boolean force) {
        return getCreateSQL(orReplace, force, getSQL());
    }

    private String getCreateSQL(boolean orReplace, boolean force, String quotedName) {
        StatementBuilder buff = new StatementBuilder("CREATE ");
        if (orReplace) {
            buff.append("OR REPLACE ");
        }
        if (force) {
            buff.append("FORCE ");
        }
        buff.append("VIEW ");
        buff.append(quotedName);
        if (comment != null) {
            buff.append(" COMMENT ").append(StringUtils.quoteStringSQL(comment));
        }
        if (columns != null && columns.length > 0) {
            buff.append('(');
            for (Column c : columns) {
                buff.appendExceptFirst(", ");
                buff.append(c.getSQL());
            }
            buff.append(')');
        } else if (columnNames != null) {
            buff.append('(');
            for (String n : columnNames) {
                buff.appendExceptFirst(", ");
                buff.append(n);
            }
            buff.append(')');
        }
        return buff.append(" AS\n").append(querySQL).toString();
    }

    public void checkRename() {
        // ok
    }

    public void lock(Session session, boolean exclusive, boolean force) {
        // exclusive lock means: the view will be dropped
    }

    public void close(Session session) {
        // nothing to do
    }

    public void unlock(Session s) {
        // nothing to do
    }

    public boolean isLockedExclusively() {
        return false;
    }

    public Index addIndex(Session session, String indexName, int indexId, IndexColumn[] cols, IndexType indexType,
            boolean create, String indexComment) {
        throw DbException.getUnsupportedException("VIEW");
    }

    public void removeRow(Session session, Row row) {
        throw DbException.getUnsupportedException("VIEW");
    }

    public void addRow(Session session, Row row) {
        throw DbException.getUnsupportedException("VIEW");
    }

    public void checkSupportAlter() {
        throw DbException.getUnsupportedException("VIEW");
    }

    public void truncate(Session session) {
        throw DbException.getUnsupportedException("VIEW");
    }

    public long getRowCount(Session session) {
        throw DbException.throwInternalError();
    }

    public boolean canGetRowCount() {
        // TODO view: could get the row count, but not that easy
        return false;
    }

    public boolean canDrop() {
        return true;
    }

    public String getTableType() {
        return Table.VIEW;
    }

    public void removeChildrenAndResources(Session session) {
        removeViewFromTables();
        super.removeChildrenAndResources(session);
        database.removeMeta(session, getId());
        querySQL = null;
        index = null;
        invalidate();
    }

    public String getSQL() {
        if (isTemporary()) {
            return "(\n" + StringUtils.indent(querySQL) + ")";
        }
        return super.getSQL();
    }

    public String getQuery() {
        return querySQL;
    }

    public Index getScanIndex(Session session) {
        if (createException != null) {
            String msg = createException.getMessage();
            throw DbException.get(ErrorCode.VIEW_IS_INVALID_2, createException, getSQL(), msg);
        }
        PlanItem item = getBestPlanItem(session, null, null);
        return item.getIndex();
    }

    public ArrayList<Index> getIndexes() {
        return null;
    }

    public long getMaxDataModificationId() {
        if (createException != null) {
            return Long.MAX_VALUE;
        }
        if (viewQuery == null) {
            return Long.MAX_VALUE;
        }
        // if nothing was modified in the database since the last check, and the
        // last is known, then we don't need to check again
        // this speeds up nested views
        long dbMod = database.getModificationDataId();
        if (dbMod > lastModificationCheck && maxDataModificationId <= dbMod) {
            maxDataModificationId = viewQuery.getMaxDataModificationId();
            lastModificationCheck = dbMod;
        }
        return maxDataModificationId;
    }

    public Index getUniqueIndex() {
        return null;
    }

    private void removeViewFromTables() {
        if (tables != null) {
            for (Table t : tables) {
                t.removeView(this);
            }
            tables.clear();
        }
    }

    private void addViewToTables() {
        for (Table t : tables) {
            t.addView(this);
        }
    }

    private void setOwner(User owner) {
        this.owner = owner;
    }

    public User getOwner() {
        return owner;
    }

    /**
     * Create a temporary view out of the given query.
     *
     * @param session the session
     * @param owner the owner of the query
     * @param name the view name
     * @param query the query
     * @param topQuery the top level query
     * @return the view table
     */
    public static TableView createTempView(Session session, User owner, String name, Query query, Query topQuery) {
        Schema mainSchema = session.getDatabase().getSchema(Constants.SCHEMA_MAIN);
        String querySQL = query.getPlanSQL();
        TableView v = new TableView(mainSchema, 0, name, querySQL, query.getParameters(), null, session,
                false);
        if (v.createException != null) {
            throw v.createException;
        }
        v.setTopQuery(topQuery);
        v.setOwner(owner);
        v.setTemporary(true);
        return v;
    }

    private void setTopQuery(Query topQuery) {
        this.topQuery = topQuery;
    }

    public long getRowCountApproximation() {
        return ROW_COUNT_APPROXIMATION;
    }

    public long getDiskSpaceUsed() {
        return 0;
    }

    public int getParameterOffset() {
        return topQuery == null ? 0 : topQuery.getParameters().size();
    }

    public boolean isDeterministic() {
        if (recursive || viewQuery == null) {
            return false;
        }
        return viewQuery.isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR);
    }

    public void setRecursiveResult(LocalResult value) {
        if (recursiveResult != null) {
            recursiveResult.close();
        }
        this.recursiveResult = value;
    }

    public ResultInterface getRecursiveResult() {
        return recursiveResult;
    }

    public void setTableExpression(boolean tableExpression) {
        this.tableExpression = tableExpression;
    }

    public boolean isTableExpression() {
        return tableExpression;
    }

}
