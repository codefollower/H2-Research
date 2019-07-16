/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.h2.api.ErrorCode;
import org.h2.command.Parser;
import org.h2.command.Prepared;
import org.h2.command.dml.AllColumnsForPlan;
import org.h2.command.dml.Query;
import org.h2.command.dml.SelectUnion;
import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.expression.Parameter;
import org.h2.expression.condition.Comparison;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.JoinBatch;
import org.h2.table.TableFilter;
import org.h2.table.TableView;
import org.h2.util.IntArray;
import org.h2.value.Value;

/**
 * This object represents a virtual index for a query.
 * Actually it only represents a prepared SELECT statement.
 */
public class ViewIndex extends BaseIndex implements SpatialIndex {

    private static final long MAX_AGE_NANOS =
            TimeUnit.MILLISECONDS.toNanos(Constants.VIEW_COST_CACHE_MAX_AGE);

    private final TableView view;
    private final String querySQL;
    private final ArrayList<Parameter> originalParameters;
    private boolean recursive;
    private final int[] indexMasks;
    private Query query;
    private final Session createSession;

    /**
     * The time in nanoseconds when this index (and its cost) was calculated.
     */
    private final long evaluatedAt;

    /**
     * Constructor for the original index in {@link TableView}.
     *
     * @param view the table view
     * @param querySQL the query SQL
     * @param originalParameters the original parameters
     * @param recursive if the view is recursive
     */
    public ViewIndex(TableView view, String querySQL,
            ArrayList<Parameter> originalParameters, boolean recursive) {
        super(view, 0, null, null, IndexType.createNonUnique(false));
        this.view = view;
        this.querySQL = querySQL;
        this.originalParameters = originalParameters;
        this.recursive = recursive;
        columns = new Column[0];
        this.createSession = null;
        this.indexMasks = null;
        // this is a main index of TableView, it does not need eviction time
        // stamp
        evaluatedAt = Long.MIN_VALUE;
    }

    /**
     * Constructor for plan item generation. Over this index the query will be
     * executed.
     *
     * @param view the table view
     * @param index the view index
     * @param session the session
     * @param masks the masks
     * @param filters table filters
     * @param filter current filter
     * @param sortOrder sort order
     */
    public ViewIndex(TableView view, ViewIndex index, Session session,
            int[] masks, TableFilter[] filters, int filter, SortOrder sortOrder) {
        super(view, 0, null, null, IndexType.createNonUnique(false));
        this.view = view;
        this.querySQL = index.querySQL;
        this.originalParameters = index.originalParameters;
        this.recursive = index.recursive;
        this.indexMasks = masks;
        this.createSession = session;
        columns = new Column[0];
        if (!recursive) {
            query = getQuery(session, masks, filters, filter, sortOrder);
        }
        // we don't need eviction for recursive views since we can't calculate
        // their cost if it is a sub-query we don't need eviction as well
        // because the whole ViewIndex cache is getting dropped in
        // Session.prepareLocal
        evaluatedAt = recursive || view.getTopQuery() != null ? Long.MAX_VALUE : System.nanoTime();
    }

    @Override
    public IndexLookupBatch createLookupBatch(TableFilter[] filters, int filter) {
        if (recursive) {
            // we do not support batching for recursive queries
            return null;
        }
        return JoinBatch.createViewIndexLookupBatch(this);
    }

    public Session getSession() {
        return createSession;
    }

    public boolean isExpired() {
        assert evaluatedAt != Long.MIN_VALUE : "must not be called for main index of TableView";
        return !recursive && view.getTopQuery() == null &&
                System.nanoTime() - evaluatedAt > MAX_AGE_NANOS;
    }

    @Override
    public String getPlanSQL() {
        return query == null ? null : query.getPlanSQL(false);
    }

    @Override
    public void close(Session session) {
        // nothing to do
    }

    @Override
    public void add(Session session, Row row) {
        throw DbException.getUnsupportedException("VIEW");
    }

    @Override
    public void remove(Session session, Row row) {
        throw DbException.getUnsupportedException("VIEW");
    }

//<<<<<<< HEAD
//    /**
//     * A calculated cost value.
//     */
//    static class CostElement {
//
//        /**
//         * The time in milliseconds when this cost was calculated.
//         */
//        long evaluatedAt;
//
//        /**
//         * The cost.
//         */
//        double cost;
//    }
//    
//    //在view对应的select语句中加上view中的字段条件
//    //例如:sql = "select * from my_view where f2 > 'b1'";
//    //实际是SELECT ID, NAME FROM CreateViewTest WHERE NAME >= ?1
//    //masks的数组元素是一个view中包含的所有列，如果某一列不是查询条件，那么对应的masks[列id]这个数组元素就是0
//    
//    //此方法不影响此类的任何字段，只是为了计算cost
//    @Override
//    public synchronized double getCost(Session session, int[] masks, TableFilter filter, SortOrder sortOrder) {
//        if (recursive) { //对应WITH RECURSIVE开头之类的语句，见my.test.command.ddl.CreateViewTest
//            return 1000;
//        }
//        IntArray masksArray = new IntArray(masks == null ?
//                Utils.EMPTY_INT_ARRAY : masks);
//        SynchronizedVerifier.check(costCache);
//        CostElement cachedCost = costCache.get(masksArray);
//        if (cachedCost != null) {
//            long time = System.currentTimeMillis();
//            if (time < cachedCost.evaluatedAt + Constants.VIEW_COST_CACHE_MAX_AGE) {
//                return cachedCost.cost;
//            }
//        }
//        //例如:sql = "select * from my_view where f2 > 'b1'";
//        //CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2)AS SELECT id,name FROM CreateViewTest
//        //这里masks就对应f2 > 'b1'
//        //相当于把f2 > 'b1'这个条件加到SELECT id,name FROM CreateViewTest中，
//        //变成SELECT id,name FROM CreateViewTest where name > 'b1'";
//        //实际是SELECT ID, NAME FROM CreateViewTest WHERE NAME >= ?1
//        Query q = (Query) session.prepare(querySQL, true);
//        if (masks != null) {
//            IntArray paramIndex = new IntArray();
//            for (int i = 0; i < masks.length; i++) {
//                int mask = masks[i];
//                if (mask == 0) {
//                    continue;
//                }
//                paramIndex.add(i);
//            }
//            int len = paramIndex.size();
//            for (int i = 0; i < len; i++) {
//                int idx = paramIndex.get(i);
//                int mask = masks[idx];
//                int nextParamIndex = q.getParameters().size() + view.getParameterOffset();
//                if ((mask & IndexCondition.EQUALITY) != 0) {
//                    Parameter param = new Parameter(nextParamIndex);
//                    q.addGlobalCondition(param, idx, Comparison.EQUAL_NULL_SAFE);
//                } else if ((mask & IndexCondition.SPATIAL_INTERSECTS) != 0) {
//                    Parameter param = new Parameter(nextParamIndex);
//                    q.addGlobalCondition(param, idx, Comparison.SPATIAL_INTERSECTS);
//                } else {
//                    if ((mask & IndexCondition.START) != 0) {
//                    	//例如:sql = "select * from my_view where f2 > 'b1'";
//                    	//实际是SELECT ID, NAME FROM CreateViewTest WHERE NAME >= ?1
//                    	//在org.h2.index.IndexCondition.getMask(ArrayList<IndexCondition>)那里把
//                    	//BIGGER_EQUAL、BIGGER都当成了START，而这里统一转成BIGGER_EQUAL，当view要过滤记录时再按大于过滤
//                        Parameter param = new Parameter(nextParamIndex);
//                        q.addGlobalCondition(param, idx, Comparison.BIGGER_EQUAL);
//                    }
//                    if ((mask & IndexCondition.END) != 0) {
//                        Parameter param = new Parameter(nextParamIndex);
//                        q.addGlobalCondition(param, idx, Comparison.SMALLER_EQUAL);
//                    }
//                }
//            }
//            //实际是SELECT ID, NAME FROM CreateViewTest WHERE NAME >= ?1
//            String sql = q.getPlanSQL();
//            q = (Query) session.prepare(sql, true);
//        }
//        double cost = q.getCost();
//        cachedCost = new CostElement();
//        cachedCost.evaluatedAt = System.currentTimeMillis();
//        cachedCost.cost = cost;
//        costCache.put(masksArray, cachedCost);
//        return cost;
//=======
    @Override
    public double getCost(Session session, int[] masks,
            TableFilter[] filters, int filter, SortOrder sortOrder,
            AllColumnsForPlan allColumnsSet) {
        return recursive ? 1000 : query.getCost();
    }

    @Override
    public Cursor find(Session session, SearchRow first, SearchRow last) {
        return find(session, first, last, null);
    }

    @Override
    public Cursor findByGeometry(TableFilter filter, SearchRow first,
            SearchRow last, SearchRow intersection) {
        return find(filter.getSession(), first, last, intersection);
    }

    private static Query prepareSubQuery(String sql, Session session, int[] masks,
            TableFilter[] filters, int filter, SortOrder sortOrder) {
        Prepared p;
        session.pushSubQueryInfo(masks, filters, filter, sortOrder);
        try {
            p = session.prepare(sql, true, true);
        } finally {
            session.popSubQueryInfo();
        }
        return (Query) p;
    }

    private Cursor findRecursive(SearchRow first, SearchRow last) {
        assert recursive;
//<<<<<<< HEAD
//        // 如 WITH RECURSIVE my_tmp_table(f1,f2)
//        // AS(select id,name from CreateViewTest UNION ALL select 1, 2) select f1, f2 from my_tmp_table
//        // 不过有bug
//        ResultInterface recResult = view.getRecursiveResult();
//        if (recResult != null) {
//            recResult.reset();
//            return new ViewCursor(this, recResult, first, last);
//=======
        ResultInterface recursiveResult = view.getRecursiveResult();
        if (recursiveResult != null) {
            recursiveResult.reset();
            return new ViewCursor(this, recursiveResult, first, last);
        }
        if (query == null) {
            Parser parser = new Parser(createSession);
            parser.setRightsChecked(true);
            parser.setSuppliedParameterList(originalParameters);
            query = (Query) parser.prepare(querySQL);
            query.setNeverLazy(true);
        }
        if (!query.isUnion()) {
            throw DbException.get(ErrorCode.SYNTAX_ERROR_2,
                    "recursive queries without UNION");
        }
        SelectUnion union = (SelectUnion) query;
        Query left = union.getLeft();
        left.setNeverLazy(true);
        // to ensure the last result is not closed
        left.disableCache();
        ResultInterface resultInterface = left.query(0);
        LocalResult localResult = union.getEmptyResult();
        // ensure it is not written to disk,
        // because it is not closed normally
        localResult.setMaxMemoryRows(Integer.MAX_VALUE);
        while (resultInterface.next()) {
            Value[] cr = resultInterface.currentRow();
            localResult.addRow(cr);
        }
        Query right = union.getRight();
        right.setNeverLazy(true);
        resultInterface.reset();
        view.setRecursiveResult(resultInterface);
        // to ensure the last result is not closed
        right.disableCache();
        while (true) {
//<<<<<<< HEAD
//            // 如 WITH RECURSIVE my_tmp_table(f1,f2)
//            // AS(select id,name from CreateViewTest UNION ALL select 1, 2) select f1, f2 from my_tmp_table
//            // 不过有bug
//            // 这里会一直是死循环，因为right.query(0)不会返回一个空结果集
//            r = right.query(0);
//            if (!r.hasNext()) {
//=======
            resultInterface = right.query(0);
            if (!resultInterface.hasNext()) {
                break;
            }
            while (resultInterface.next()) {
                Value[] cr = resultInterface.currentRow();
                localResult.addRow(cr);
            }
//<<<<<<< HEAD
//            r.reset();
//            view.setRecursiveResult(r);
//            // 我加上的
//            // 避免死循环，因为此时union all的右边子句不是当前view
//            if (!right.getTables().contains(view)) {
//                break;
//            }
//=======
            resultInterface.reset();
            view.setRecursiveResult(resultInterface);
        }
        view.setRecursiveResult(null);
        localResult.done();
        return new ViewCursor(this, localResult, first, last);
    }

    /**
     * Set the query parameters.
     *
     * @param session the session
     * @param first the lower bound
     * @param last the upper bound
     * @param intersection the intersection
     */
    public void setupQueryParameters(Session session, SearchRow first, SearchRow last,
            SearchRow intersection) {
        ArrayList<Parameter> paramList = query.getParameters();
        if (originalParameters != null) {
            for (Parameter orig : originalParameters) {
                int idx = orig.getIndex();
                Value value = orig.getValue(session);
                setParameter(paramList, idx, value);
            }
        }
        int len;
        if (first != null) {
            len = first.getColumnCount();
        } else if (last != null) {
            len = last.getColumnCount();
        } else if (intersection != null) {
            len = intersection.getColumnCount();
        } else {
            len = 0;
        }
        //view中已给select加了外部条件，所以多了Parameter，这里就是给这些Parameter赋值
        int idx = view.getParameterOffset(originalParameters);
        for (int i = 0; i < len; i++) {
            int mask = indexMasks[i];
            if ((mask & IndexCondition.EQUALITY) != 0) {
                setParameter(paramList, idx++, first.getValue(i));
            }
            if ((mask & IndexCondition.START) != 0) {
                setParameter(paramList, idx++, first.getValue(i));
            }
            if ((mask & IndexCondition.END) != 0) {
                setParameter(paramList, idx++, last.getValue(i));
            }
            if ((mask & IndexCondition.SPATIAL_INTERSECTS) != 0) {
                setParameter(paramList, idx++, intersection.getValue(i));
            }
        }
    }

    private Cursor find(Session session, SearchRow first, SearchRow last,
            SearchRow intersection) {
        if (recursive) {
            return findRecursive(first, last);
        }
        setupQueryParameters(session, first, last, intersection);
        ResultInterface result = query.query(0);
        return new ViewCursor(this, result, first, last);
    }

    private static void setParameter(ArrayList<Parameter> paramList, int x,
            Value v) {
        if (x >= paramList.size()) {
            // the parameter may be optimized away as in
            // select * from (select null as x) where x=1;
            return;
        }
        Parameter param = paramList.get(x);
        param.setValue(v);
    }

    public Query getQuery() {
        return query;
    }

    // 目的是为了对indexColumns赋值，indexColumns另有它用
    // 比如在org.h2.command.dml.Select.prepare()中就有应用(cost = preparePlan那行代码之后)
    private Query getQuery(Session session, int[] masks,
            TableFilter[] filters, int filter, SortOrder sortOrder) {
        Query q = prepareSubQuery(querySQL, session, masks, filters, filter, sortOrder);
        if (masks == null) {
            return q;
        }
        //比如AS SELECT top 2 id,name FROM CreateViewTest order by id
        //limitExpr和sort都不为空，此时不允许加全局条件到select中
        if (!q.allowGlobalConditions()) {
            return q;
        }
        int firstIndexParam = view.getParameterOffset(originalParameters);
        // the column index of each parameter
        // (for example: paramColumnIndex {0, 0} mean
        // param[0] is column 0, and param[1] is also column 0)
        IntArray paramColumnIndex = new IntArray();
        int indexColumnCount = 0;
        for (int i = 0; i < masks.length; i++) {
            int mask = masks[i];
            if (mask == 0) {
                continue;
            }
            indexColumnCount++;
//<<<<<<< HEAD
//            paramIndex.add(i);
//            //为1的bit个数，比如mask=3时，就是0011，所以bitCount=2
//            //mask=6时，就是0110，也就是RANGE = START | END
//            //如select * from my_view where f2 between 'b1' and 'b2'
//            if (Integer.bitCount(mask) > 1) {
//                // two parameters for range queries: >= x AND <= y
//                paramIndex.add(i);
//            }
//        }
//        int len = paramIndex.size(); //paramIndex中放的是列id
//=======
            // the number of parameters depends on the mask;
            // for range queries it is 2: >= x AND <= y
            // but bitMask could also be 7 (=, and <=, and >=)
            int bitCount = Integer.bitCount(mask);
            for (int j = 0; j < bitCount; j++) {
                paramColumnIndex.add(i);
            }
        }
        int len = paramColumnIndex.size();
        ArrayList<Column> columnList = new ArrayList<>(len);
        for (int i = 0; i < len;) {
            int idx = paramColumnIndex.get(i);
            columnList.add(table.getColumn(idx));
            int mask = masks[idx];
            if ((mask & IndexCondition.EQUALITY) != 0) {
                Parameter param = new Parameter(firstIndexParam + i);
                q.addGlobalCondition(param, idx, Comparison.EQUAL_NULL_SAFE);
                i++;
            }
            if ((mask & IndexCondition.START) != 0) {
                Parameter param = new Parameter(firstIndexParam + i);
                q.addGlobalCondition(param, idx, Comparison.BIGGER_EQUAL);
                i++;
            }
            if ((mask & IndexCondition.END) != 0) {
                Parameter param = new Parameter(firstIndexParam + i);
                q.addGlobalCondition(param, idx, Comparison.SMALLER_EQUAL);
                i++;
            }
            if ((mask & IndexCondition.SPATIAL_INTERSECTS) != 0) {
                Parameter param = new Parameter(firstIndexParam + i);
                q.addGlobalCondition(param, idx, Comparison.SPATIAL_INTERSECTS);
                i++;
            }
        }
        columns = columnList.toArray(new Column[0]);

        // reconstruct the index columns from the masks
        this.indexColumns = new IndexColumn[indexColumnCount];
        this.columnIds = new int[indexColumnCount];
        //type从0到1，也就是循环两次运行子循环
        //当type为0时，只取where条件中的"等于"关系表达式
        //当type为1时，只取where条件中的除"等于"关系表达式之上的表达式
        //如select * from my_view where f1=2 and f2 between 'b1' and 'b2'
        //当type为0时，只取f1=2
        //当type为1时，只取f2 between 'b1' and 'b2'
        for (int type = 0, indexColumnId = 0; type < 2; type++) {
            for (int i = 0; i < masks.length; i++) {
                int mask = masks[i];
                if (mask == 0) {
                    continue;
                }
                if (type == 0) {
                    if ((mask & IndexCondition.EQUALITY) == 0) {
                        // the first columns need to be equality conditions
                        continue;
                    }
                } else {
                    if ((mask & IndexCondition.EQUALITY) != 0) {
                        // after that only range conditions
                        continue;
                    }
                }
                IndexColumn c = new IndexColumn();
                c.column = table.getColumn(i);
                indexColumns[indexColumnId] = c;
                columnIds[indexColumnId] = c.column.getColumnId();
                indexColumnId++;
            }
        }

        String sql = q.getPlanSQL(true);
        q = prepareSubQuery(sql, session, masks, filters, filter, sortOrder);
        return q;
    }

    @Override
    public void remove(Session session) {
        throw DbException.getUnsupportedException("VIEW");
    }

    @Override
    public void truncate(Session session) {
        throw DbException.getUnsupportedException("VIEW");
    }

    @Override
    public void checkRename() {
        throw DbException.getUnsupportedException("VIEW");
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
        throw DbException.getUnsupportedException("VIEW");
    }

    public void setRecursive(boolean value) {
        this.recursive = value;
    }

    @Override
    public long getRowCount(Session session) {
        return 0;
    }

    @Override
    public long getRowCountApproximation() {
        return 0;
    }

    @Override
    public long getDiskSpaceUsed() {
        return 0;
    }

    public boolean isRecursive() {
        return recursive;
    }
}
