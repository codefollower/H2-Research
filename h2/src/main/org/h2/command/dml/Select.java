/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import org.h2.api.ErrorCode;
import org.h2.api.Trigger;
import org.h2.command.Parser;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.expression.Alias;
import org.h2.expression.Comparison;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.expression.Wildcard;
import org.h2.expression.aggregate.Aggregate;
import org.h2.expression.aggregate.Window;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.LazyResult;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.ResultTarget;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.IndexColumn;
import org.h2.table.JoinBatch;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.table.TableView;
import org.h2.util.ColumnNamer;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueNull;

/**
 * This class represents a simple SELECT statement.
 *
 * For each select statement,
 * visibleColumnCount &lt;= distinctColumnCount &lt;= expressionCount.
 * The expression list count could include ORDER BY and GROUP BY expressions
 * that are not in the select list.
 *
 * The call sequence is init(), mapColumns() if it's a subquery, prepare().
 *
 * @author Thomas Mueller
 * @author Joel Turkel (Group sorted query)
 */
//调用顺序 init=>prepare->query
public class Select extends Query {
//<<<<<<< HEAD
//    private TableFilter topTableFilter;
//    private final ArrayList<TableFilter> filters = New.arrayList(); //包含NestedJoin
//    //所有的非NestedJoin都是top filter
//    //例如"select rownum, * from JoinTest1 CROSS JOIN JoinTest2 CROSS JOIN JoinTest3 CROSS JOIN JoinTest4"
//    //就有4个top filter，依次是JoinTest1、JoinTest2、JoinTest3、JoinTest4
//    private final ArrayList<TableFilter> topFilters = New.arrayList();
//    private ArrayList<Expression> expressions; //select a,b,c from中的a,b,c
//    private Expression[] expressionArray;
//    private Expression having; //having子句
//    private Expression condition; //where子句
//    //visibleColumnCount不包含缺失的order by、GROUP BY、having字段
//    //distinctColumnCount包含缺失的order by字段，但不包含GROUP BY、having字段
//    private int visibleColumnCount, distinctColumnCount;
//    private ArrayList<SelectOrderBy> orderList; //对应order by，一个字段对应一个SelectOrderBy
//    private ArrayList<Expression> group; //对应group by，一个字段对应一个Expression
//    private int[] groupIndex;
//    private boolean[] groupByExpression;
//    private HashMap<Expression, Object> currentGroup;
//=======

    /**
     * The main (top) table filter.
     */
    TableFilter topTableFilter;

    private final ArrayList<TableFilter> filters = Utils.newSmallArrayList();
    private final ArrayList<TableFilter> topFilters = Utils.newSmallArrayList();

    private Expression having;
    private Expression condition;

    /**
     * The visible columns (the ones required in the result).
     */
    int visibleColumnCount;

    /**
     * {@code DISTINCT ON(...)} expressions.
     */
    private Expression[] distinctExpressions;

    private int[] distinctIndexes;

    private int distinctColumnCount;
    private ArrayList<Expression> group;

    /**
     * The indexes of the group-by columns.
     */
    int[] groupIndex;

    /**
     * Whether a column in the expression list is part of a group-by.
     */
    boolean[] groupByExpression;

    SelectGroups groupData;

    private int havingIndex;
    boolean isGroupQuery;
    private boolean isGroupSortedQuery;
    private boolean isWindowQuery;
    private boolean isForUpdate, isForUpdateMvcc;
    private double cost;
    //isQuickAggregateQuery是针对min、max、count三个聚合函数的特别优化
    private boolean isQuickAggregateQuery, isDistinctQuery;
    private boolean isPrepared, checkInit;
    private boolean sortUsingIndex;

    private boolean isGroupWindowStage2;

    private HashMap<String, Window> windows;

    public Select(Session session) {
        super(session);
    }

    @Override
    public boolean isUnion() {
        return false;
    }

    /**
     * Add a table to the query.
     *
     * @param filter the table to add
     * @param isTop if the table can be the first table in the query plan
     */
    public void addTableFilter(TableFilter filter, boolean isTop) {
        // Oracle doesn't check on duplicate aliases
        // String alias = filter.getAlias();
        // if (filterNames.contains(alias)) {
        //     throw Message.getSQLException(
        //         ErrorCode.DUPLICATE_TABLE_ALIAS, alias);
        // }
        // filterNames.add(alias);
        filters.add(filter);
        if (isTop) {
            topFilters.add(filter);
        }
    }

    public ArrayList<TableFilter> getTopFilters() {
        return topFilters;
    }

    public void setExpressions(ArrayList<Expression> expressions) {
        this.expressions = expressions;
    }

    public void setWildcard() {
        expressions = new ArrayList<>(1);
        expressions.add(new Wildcard(null, null));
    }

    /**
     * Called if this query contains aggregate functions.
     */
    public void setGroupQuery() { //有group by或having或聚合函数(包括自定义的聚合函数)时都会调用
        isGroupQuery = true;
    }

    /**
     * Called if this query contains window functions.
     */
    public void setWindowQuery() {
        isWindowQuery = true;
    }

    public void setGroupBy(ArrayList<Expression> group) {
        this.group = group;
    }

    public ArrayList<Expression> getGroupBy() { //未使用
        return group;
    }

    public SelectGroups getGroupDataIfCurrent(boolean window) {
        return groupData != null && (window || groupData.isCurrentGroup()) ? groupData : null;
    }

    @Override
    public void setDistinct() {
        if (distinctExpressions != null) {
            throw DbException.getUnsupportedException("DISTINCT ON together with DISTINCT");
        }
        distinct = true;
    }

    /**
     * Set the distinct expressions.
     */
    public void setDistinct(Expression[] distinctExpressions) {
        if (distinct) {
            throw DbException.getUnsupportedException("DISTINCT ON together with DISTINCT");
        }
        this.distinctExpressions = distinctExpressions;
    }

    @Override
    public void setDistinctIfPossible() {
        if (!isAnyDistinct() && offsetExpr == null && limitExpr == null) {
            distinct = true;
        }
    }

    @Override
    public boolean isAnyDistinct() {
        return distinct || distinctExpressions != null;
    }

    /**
     * Adds a named window definition.
     *
     * @param name name
     * @param window window definition
     * @return true if a new definition was added, false if old definition was replaced
     */
    public boolean addWindow(String name, Window window) {
        if (windows == null) {
            windows = new HashMap<>();
        }
        return windows.put(name, window) == null;
    }

    /**
     * Returns a window with specified name, or null.
     *
     * @param name name of the window
     * @return the window with specified name, or null
     */
    public Window getWindow(String name) {
        return windows != null ? windows.get(name) : null;
    }

    /**
     * Add a condition to the list of conditions.
     *
     * @param cond the condition to add
     */
    public void addCondition(Expression cond) {
        if (condition == null) {
            condition = cond;
        } else {
            condition = new ConditionAndOr(ConditionAndOr.AND, cond, condition);
        }
    }
//<<<<<<< HEAD
//    
//    //当为group by字段建立索引并按此字段排序时就调用此方法 (聚合函数的场景不适用)
//    //如select id,count(id) from mytable where id>2 group by id having id=3 order by id
//    //此方法的代码大多数跟queryGroup类似
//    private void queryGroupSorted(int columnCount, ResultTarget result) {
//        int rowNumber = 0;
//        setCurrentRowNumber(0);
//        currentGroup = null;
//        Value[] previousKeyValues = null;
//        while (topTableFilter.next()) {
//            setCurrentRowNumber(rowNumber + 1);
//            if (condition == null ||
//                    Boolean.TRUE.equals(condition.getBooleanValue(session))) {
//                rowNumber++;
//                Value[] keyValues = new Value[groupIndex.length]; //group by字段的值组成一个数组
//                // update group
//                for (int i = 0; i < groupIndex.length; i++) {
//                    int idx = groupIndex[i];
//                    Expression expr = expressions.get(idx);
//                    keyValues[i] = expr.getValue(session);
//                }
//
//                if (previousKeyValues == null) {
//                    previousKeyValues = keyValues;
//                    currentGroup = New.hashMap();
//                } else if (!Arrays.equals(previousKeyValues, keyValues)) {
//                	//因为是按group by字段排序的，当前后两行的group by字段值不相等时就可以确定是不同组了，这时直接合并
//                    addGroupSortedRow(previousKeyValues, columnCount, result);
//                    previousKeyValues = keyValues;
//                    currentGroup = New.hashMap();
//                }
//                currentGroupRowId++;
//
//                //这里计算非group字段的值
//                for (int i = 0; i < columnCount; i++) {
//                	//groupByExpression字段不可能为null，因为isGroupSortedQuery为true时，getGroupByExpressionCount() > 0
//                    if (groupByExpression == null || !groupByExpression[i]) {
//                        Expression expr = expressions.get(i);
//                        expr.updateAggregate(session);
//                    }
//                }
//            }
//        }
//        //最后一部分也要合并
//        if (previousKeyValues != null) {
//            addGroupSortedRow(previousKeyValues, columnCount, result);
//=======

    public Expression getCondition() {
        return condition;
    }

    private LazyResult queryGroupSorted(int columnCount, ResultTarget result, long offset, boolean quickOffset) {
        LazyResultGroupSorted lazyResult = new LazyResultGroupSorted(expressionArray, columnCount);
        skipOffset(lazyResult, offset, quickOffset);
        if (result == null) {
            return lazyResult;
        }
        while (lazyResult.next()) {
            result.addRow(lazyResult.currentRow());
        }
        return null;
    }

    /**
     * Create a row with the current values, for queries with group-sort.
     *
     * @param keyValues the key values
     * @param columnCount the number of columns
     * @return the row
     */
    Value[] createGroupSortedRow(Value[] keyValues, int columnCount) {
        Value[] row = new Value[columnCount];
        //先填充group by字段
        //groupIndex字段是不会为null的
        for (int j = 0; groupIndex != null && j < groupIndex.length; j++) {
            row[groupIndex[j]] = keyValues[j];
        }
        //再填充非group by字段
        for (int j = 0; j < columnCount; j++) {
            //groupByExpression字段不可能为null，因为isGroupSortedQuery为true时，getGroupByExpressionCount() > 0
            if (groupByExpression != null && groupByExpression[j]) {
                continue;
            }
            Expression expr = expressions.get(j);
            row[j] = expr.getValue(session);
        }
        if (isHavingNullOrFalse(row)) {
            return null;
        }
        row = keepOnlyDistinct(row, columnCount);
        return row;
    }
    
    //例如:select id,count(id) from mytable where id>2 group by id having id=3 order by id
    //having id=3也加到expressions中了，此时columnCount是3，但是distinctColumnCount是2，所以要去掉一列
    private Value[] keepOnlyDistinct(Value[] row, int columnCount) {
        if (columnCount == distinctColumnCount) {
            return row;
        }
        // remove columns so that 'distinct' can filter duplicate rows
        return Arrays.copyOf(row, distinctColumnCount);
    }

    private boolean isHavingNullOrFalse(Value[] row) {
        return havingIndex >= 0 && !row[havingIndex].getBoolean();
    }

    private Index getGroupSortedIndex() {
        //这两条件，只要判断一个就行了，因为在init时，两者要么都为null，要么都不为null
        //别外，getGroupSortedIndex()只有在getGroupByExpressionCount() > 0时才调用，
        //并且getGroupByExpressionCount()中已经判断过groupByExpression == null了，
        //所以这个if是多于的
        if (groupIndex == null || groupByExpression == null) {
            return null;
        }
        ArrayList<Index> indexes = topTableFilter.getTable().getIndexes();
        if (indexes != null) {
            for (Index index : indexes) {
                if (index.getIndexType().isScan()) {
                    continue;
                }
                if (index.getIndexType().isHash()) {
                    // does not allow scanning entries
                    continue;
                }
                if (isGroupSortedIndex(topTableFilter, index)) {
                    return index;
                }
            }
        }
        return null;
    }
    //用于group的索引跟order by索引不一样，用于group的索引的索引字段顺序不需要和group字段顺序一样，而order by需要一样
    private boolean isGroupSortedIndex(TableFilter tableFilter, Index index) {
        // check that all the GROUP BY expressions are part of the index
        Column[] indexColumns = index.getColumns();
        // also check that the first columns in the index are grouped
        boolean[] grouped = new boolean[indexColumns.length];
        outerLoop:
        for (int i = 0, size = expressions.size(); i < size; i++) {
            if (!groupByExpression[i]) {
                continue;
            }
            Expression expr = expressions.get(i).getNonAliasExpression();
            if (!(expr instanceof ExpressionColumn)) { //例如group by name, 2
                return false;
            }
            //看看group by字段是否是索引字段
            ExpressionColumn exprCol = (ExpressionColumn) expr;
            for (int j = 0; j < indexColumns.length; ++j) {
                if (tableFilter == exprCol.getTableFilter()) {
                    if (indexColumns[j].equals(exprCol.getColumn())) {
                        grouped[j] = true;
                        continue outerLoop;
                    }
                }
            }
            // We didn't find a matching index column
            // for one group by expression
            //只要其中一个group by字段在index字段中找不到，那么就认为此index不适合用于group索引
            //如果当前group by字段在index字段中找到了，会在上面的内部for循环中通过continue outerLoop转到外层的for，i增加，
            //不会转到这里
            return false;
        }
        // check that the first columns in the index are grouped
        // good: index(a, b, c); group by b, a
        // bad: index(a, b, c); group by a, c
        //group by字段列表的前后两个字段在index中要同时出现，并且是紧挨着的(不分前后)
        //index(a, b, c); group by a, c就不行，因为"group by a, c"中的a和c虽然都是索引字段，但是在index中不是紧挨着的
        //group by字段必须是索引字段列表的前缀
        //group by b, c也返回false
        for (int i = 1; i < grouped.length; i++) {
            if (!grouped[i - 1] && grouped[i]) {
                return false;
            }
        }
        return true;
    }

    private int getGroupByExpressionCount() {
        if (groupByExpression == null) {
            return 0;
        }
        int count = 0;
        for (boolean b : groupByExpression) {
            if (b) {
                ++count; //只要找到一个其实就可以退出了
            }
        }
        return count;
    }

    boolean isConditionMet() {
        return condition == null || condition.getBooleanValue(session);
    }

    private void queryWindow(int columnCount, LocalResult result, long offset, boolean quickOffset) {
        initGroupData(columnCount);
        try {
            gatherGroup(columnCount, Aggregate.STAGE_WINDOW);
            processGroupResult(columnCount, result, offset, quickOffset);
        } finally {
            groupData.reset();
        }
    }

//<<<<<<< HEAD
//    //看这方法的代码时要时刻想到聚合函数、group by、having都有可能触发它
//    private void queryGroup(int columnCount, LocalResult result) {
//        //key是ValueArray类型，如果当前有group by，则key对应group by字段值组成的数组，
//        //否则当前sql只有普通的聚合函数，此时key对应默认的ValueArray.get(new Value[0])
//        ValueHashMap<HashMap<Expression, Object>> groups =
//                ValueHashMap.newInstance();
//=======
    private void queryGroupWindow(int columnCount, LocalResult result, long offset, boolean quickOffset) {
        initGroupData(columnCount);
        try {
            gatherGroup(columnCount, Aggregate.STAGE_GROUP);
            while (groupData.next() != null) {
                updateAgg(columnCount, Aggregate.STAGE_WINDOW);
            }
            groupData.done();
            try {
                isGroupWindowStage2 = true;
                processGroupResult(columnCount, result, offset, quickOffset);
            } finally {
                isGroupWindowStage2 = false;
            }
        } finally {
            groupData.reset();
        }
    }

    private void queryGroup(int columnCount, LocalResult result, long offset, boolean quickOffset) {
        initGroupData(columnCount);
        try {
            gatherGroup(columnCount, Aggregate.STAGE_GROUP);
            processGroupResult(columnCount, result, offset, quickOffset);
        } finally {
            groupData.reset();
        }
    }

    private void initGroupData(int columnCount) {
        if (groupData == null) {
            groupData = SelectGroups.getInstance(session, expressions, isGroupQuery, groupIndex);
        } else {
            updateAgg(columnCount, Aggregate.STAGE_RESET);
        }
        groupData.reset();
    }

    private void gatherGroup(int columnCount, int stage) {
        int rowNumber = 0;
        setCurrentRowNumber(0);
        int sampleSize = getSampleSizeValue(session);
        while (topTableFilter.next()) {
            setCurrentRowNumber(rowNumber + 1);
            if (isConditionMet()) {
                rowNumber++;
//<<<<<<< HEAD
//                //聚合函数的情形
//                if (groupIndex == null) { //如select count(id) from mytable where id>0时groupIndex=null
//                    key = defaultGroup;
//                } else { //group by、having的情形
//                    //避免在ExpressionColumn.getValue中取到旧值
//                    //例如SELECT id/3 AS A, COUNT(*) FROM mytable GROUP BY A HAVING A>=0
//                    currentGroup = null; //我加上的
//                    
//                	//按当前行，抽取group by字段列表的值，组合成一个key
//                	//例如group by id,name，那么先按id的字段下标从当前行中取出值，放到keyValues[0]中，
//                	//然后取出name字段的值放到keyValues[1]中。
//                    Value[] keyValues = new Value[groupIndex.length];
//                    // update group
//                    for (int i = 0; i < groupIndex.length; i++) {
//                        int idx = groupIndex[i];
//                        Expression expr = expressions.get(idx);
//                        keyValues[i] = expr.getValue(session);
//                    }
//                    key = ValueArray.get(keyValues);
//                }
//                HashMap<Expression, Object> values = groups.get(key);
//                if (values == null) {
//                    values = new HashMap<Expression, Object>();
//                    groups.put(key, values);
//                }
//                currentGroup = values;
//                currentGroupRowId++;
//                int len = columnCount;
//                //如果是聚合函数的场景，那么select表达式列表部分不能出现字段
//                //如果是group by、having的场景，那么select表达式列表部分只允许出现group by字段
//                for (int i = 0; i < len; i++) {
//                	//当是聚合函数时groupByExpression为null，group by、having的情形groupByExpression不为null
//                	//select id,count(id) from mytable where id>0时是聚合函数，但是加入id字段是错误的，
//                	//从常识理解来看字段和聚合函数放在一起有歧义，不知道该怎么显式结果，
//                	//所以会报错: Column "ID" must be in the GROUP BY list
//                	//如果变成这样select id,count(id) from mytable where id>0 group by id
//                	//那么语义就很明确了：以id分组，然后统计每组的行数。
//                	//这样显示结果时，
//                	//1  2
//                	//2  4
//                	//3  5
//                	//就表示id是1的有两行，id是2的有4行，id是3的有5行
//                    if (groupByExpression == null || !groupByExpression[i]) {
//                        Expression expr = expressions.get(i);
//                        expr.updateAggregate(session);
//                    }
//                }
//=======
                groupData.nextSource();
                updateAgg(columnCount, stage);
                if (sampleSize > 0 && rowNumber >= sampleSize) {
                    break;
                }
            }
        }
//<<<<<<< HEAD
//        //只有聚会函数，但是没有记录(可能是表本身没有记录，或没有满足条件的记录)
//        //例如假设id最大为10,用此语句测试select count(id) from mytable where id>1000
//        if (groupIndex == null && groups.size() == 0) {
//            groups.put(defaultGroup, new HashMap<Expression, Object>());
//=======
        groupData.done();
    }

    void updateAgg(int columnCount, int stage) {
        for (int i = 0; i < columnCount; i++) {
            if (groupByExpression == null || !groupByExpression[i]) {
                Expression expr = expressions.get(i);
                expr.updateAggregate(session, stage);
            }
        }
    }

    private void processGroupResult(int columnCount, LocalResult result, long offset, boolean quickOffset) {
        for (ValueArray currentGroupsKey; (currentGroupsKey = groupData.next()) != null;) {
            Value[] keyValues = currentGroupsKey.getList();
            Value[] row = new Value[columnCount];
            //先填充group by字段
            for (int j = 0; groupIndex != null && j < groupIndex.length; j++) {
                row[groupIndex[j]] = keyValues[j];
            }
            //再填充非group by字段
            for (int j = 0; j < columnCount; j++) {
                if (groupByExpression != null && groupByExpression[j]) {
                    continue;
                }
                Expression expr = expressions.get(j);
                row[j] = expr.getValue(session);
            }
            //根据having条件过滤，having条件也会加入expressions中，
            //所以在上面row[j] = expr.getValue(session)时已经算好了true或false
            if (isHavingNullOrFalse(row)) {
                continue;
            }
            if (quickOffset && offset > 0) {
                offset--;
                continue;
            }
            row = keepOnlyDistinct(row, columnCount);
            result.addRow(row);
        }
    }

    /**
     * Get the index that matches the ORDER BY list, if one exists. This is to
     * avoid running a separate ORDER BY if an index can be used. This is
     * specially important for large result sets, if only the first few rows are
     * important (LIMIT is used)
     *
     * @return the index if one is found
     */
    private Index getSortIndex() {
        if (sort == null) {
            return null;
        }
//<<<<<<< HEAD
//        ArrayList<Column> sortColumns = New.arrayList();
//        //生成orderby字段列表
//=======
        ArrayList<Column> sortColumns = Utils.newSmallArrayList();
        for (int idx : sort.getQueryColumnIndexes()) {
            if (idx < 0 || idx >= expressions.size()) {
                throw DbException.getInvalidValueException("ORDER BY", idx + 1);
            }
            Expression expr = expressions.get(idx);
            expr = expr.getNonAliasExpression();
            if (expr.isConstant()) {
                continue;
            }
            if (!(expr instanceof ExpressionColumn)) {
                return null;
            }
            ExpressionColumn exprCol = (ExpressionColumn) expr;
            if (exprCol.getTableFilter() != topTableFilter) {
                return null;
            }
            sortColumns.add(exprCol.getColumn());
        }
//<<<<<<< HEAD
//        Column[] sortCols = sortColumns.toArray(new Column[sortColumns.size()]);
//        int[] sortTypes = sort.getSortTypes();
//        //如果没有orderby字段直接用scan index
//=======
        Column[] sortCols = sortColumns.toArray(new Column[0]);
        if (sortCols.length == 0) {
            // sort just on constants - can use scan index
            return topTableFilter.getTable().getScanIndex(session);
        }
        ArrayList<Index> list = topTableFilter.getTable().getIndexes();
        if (list != null) {
//<<<<<<< HEAD
//        	//循环遍历当前表的所有索引，对比每个索引的字段是否是orderby字段(可能会有多个)和排序类型是否一样，
//        	//如果都一样，那么返回此索引
//            for (int i = 0, size = list.size(); i < size; i++) {
//                Index index = list.get(i);
//=======
            int[] sortTypes = sort.getSortTypesWithNullPosition();
            for (Index index : list) {
                if (index.getCreateSQL() == null) {
                    // can't use the scan index
                    continue;
                }
                if (index.getIndexType().isHash()) {
                    continue;
                }
                IndexColumn[] indexCols = index.getIndexColumns();
                if (indexCols.length < sortCols.length) {
                    continue;
                }
                boolean ok = true;
                for (int j = 0; j < sortCols.length; j++) {
                    // the index and the sort order must start
                    // with the exact same columns
                    IndexColumn idxCol = indexCols[j];
                    Column sortCol = sortCols[j];

                    //如果是多字段索引，只有当这个索引的第一个字段与order by字段相同时才使用些索引
                    if (idxCol.column != sortCol) {
                        ok = false;
                        break;
                    }
                    if (SortOrder.addExplicitNullPosition(idxCol.sortType) != sortTypes[j]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return index;
                }
            }
        }
        //按_ROWID_排序的情况
        if (sortCols.length == 1 && sortCols[0].getColumnId() == -1) {
            // special case: order by _ROWID_
            Index index = topTableFilter.getTable().getScanIndex(session);
            if (index.isRowIdIndex()) {
                return index;
            }
        }
        return null;
    }

//<<<<<<< HEAD
//    //对于select distinct name from mytable, 直接走name的B-tree索引就可以得到name例的值了，不用找PageData索引
//    private void queryDistinct(ResultTarget result, long limitRows) {
//        // limitRows must be long, otherwise we get an int overflow
//        // if limitRows is at or near Integer.MAX_VALUE
//        // limitRows is never 0 here
//        if (limitRows > 0 && offsetExpr != null) {
//            int offset = offsetExpr.getValue(session).getInt();
//            if (offset > 0) {
//                limitRows += offset;
//=======
    private void queryDistinct(ResultTarget result, long offset, long limitRows, boolean withTies,
            boolean quickOffset) {
        if (limitRows > 0 && offset > 0) {
            limitRows += offset;
            if (limitRows < 0) {
                // Overflow
                limitRows = Long.MAX_VALUE;
            }
        }
        int rowNumber = 0;
        setCurrentRowNumber(0);
        Index index = topTableFilter.getIndex();
        SearchRow first = null;
        int columnIndex = index.getColumns()[0].getColumnId();
        int sampleSize = getSampleSizeValue(session);
        if (!quickOffset) {
            offset = 0;
        }
        while (true) {
            setCurrentRowNumber(++rowNumber);
            Cursor cursor = index.findNext(session, first, null);
            if (!cursor.next()) {
                break;
            }
            SearchRow found = cursor.getSearchRow();
            Value value = found.getValue(columnIndex);
            if (first == null) {
                first = topTableFilter.getTable().getTemplateSimpleRow(true);
            }
            first.setValue(columnIndex, value);
            if (offset > 0) {
                offset--;
                continue;
            }
            Value[] row = { value };
            result.addRow(row);
            if ((sort == null || sortUsingIndex) && limitRows > 0 &&
                    rowNumber >= limitRows && !withTies) {
                break;
            }
            if (sampleSize > 0 && rowNumber >= sampleSize) {
                break;
            }
        }
    }

//<<<<<<< HEAD
//    private LazyResult queryFlat(int columnCount, ResultTarget result, long limitRows) {
//        // limitRows must be long, otherwise we get an int overflow
//        // if limitRows is at or near Integer.MAX_VALUE
//        // limitRows is never 0 here
//    	//并不会按offset先跳过前面的行数，而是limitRows加上offset，读够limitRows+offset行，然后这从result中跳
//    	//因为可能需要排序，offset是相对于最后的结果来说的，而不是排序前的结果
//        if (limitRows > 0 && offsetExpr != null) {
//            int offset = offsetExpr.getValue(session).getInt();
//            if (offset > 0) {
//                limitRows += offset;
//=======
    private LazyResult queryFlat(int columnCount, ResultTarget result, long offset, long limitRows, boolean withTies,
            boolean quickOffset) {
        if (limitRows > 0 && offset > 0 && !quickOffset) {
            limitRows += offset;
            if (limitRows < 0) {
                // Overflow
                limitRows = Long.MAX_VALUE;
            }
        }
        ArrayList<Row> forUpdateRows = this.isForUpdateMvcc ? Utils.<Row>newSmallArrayList() : null;
        int sampleSize = getSampleSizeValue(session);
//<<<<<<< HEAD
//        while (topTableFilter.next()) {
//            setCurrentRowNumber(rowNumber + 1);
//            if (condition == null ||
//                    Boolean.TRUE.equals(condition.getBooleanValue(session))) {
//                Value[] row = new Value[columnCount];
//                for (int i = 0; i < columnCount; i++) {
//                    Expression expr = expressions.get(i);
//                    //触发:
//                    //org.h2.expression.ExpressionColumn.getValue(Session)
//                    //org.h2.table.TableFilter.getValue(Column)
//                    row[i] = expr.getValue(session);
//                }
//                if (isForUpdateMvcc) {
//                    topTableFilter.lockRowAdd(forUpdateRows);
//                }
//                result.addRow(row);
//                rowNumber++;
//                //如果sort为null，说明不用排序，只要读够那么多行就可以了
//                //同样的，如果sortUsingIndex为true，那么说明当前是按索引先取的，索引是有序的，所以只要读够那么多行也可以了
//                if ((sort == null || sortUsingIndex) && limitRows > 0 && result.getRowCount() >= limitRows) {
//                    break;
//                }
//                if (sampleSize > 0 && rowNumber >= sampleSize) {
//                    break;
//                }
//=======
        LazyResultQueryFlat lazyResult = new LazyResultQueryFlat(expressionArray,
                sampleSize, columnCount);
        skipOffset(lazyResult, offset, quickOffset);
        if (result == null) {
            return lazyResult;
        }
        if (limitRows < 0 || sort != null && !sortUsingIndex || withTies && !quickOffset) {
            limitRows = Long.MAX_VALUE;
        }
        Value[] row = null;
        while (result.getRowCount() < limitRows && lazyResult.next()) {
            if (forUpdateRows != null) {
                topTableFilter.lockRowAdd(forUpdateRows);
            }
            row = lazyResult.currentRow();
            result.addRow(row);
        }
        if (limitRows != Long.MAX_VALUE && withTies && sort != null && row != null) {
            Value[] expected = row;
            while (lazyResult.next()) {
                row = lazyResult.currentRow();
                if (sort.compare(expected, row) != 0) {
                    break;
                }
                if (forUpdateRows != null) {
                    topTableFilter.lockRowAdd(forUpdateRows);
                }
                result.addRow(row);
            }
            result.limitsWereApplied();
        }
        if (forUpdateRows != null) {
            topTableFilter.lockRows(forUpdateRows);
        }
        return null;
    }

    private static void skipOffset(LazyResultSelect lazyResult, long offset, boolean quickOffset) {
        if (quickOffset) {
            while (offset > 0 && lazyResult.next()) {
                offset--;
            }
        }
    }

    private void queryQuick(int columnCount, ResultTarget result, boolean skipResult) {
        Value[] row = new Value[columnCount];
        for (int i = 0; i < columnCount; i++) {
            Expression expr = expressions.get(i);
            row[i] = expr.getValue(session);
        }
        if (!skipResult) {
            result.addRow(row);
        }
    }

    @Override
    public ResultInterface queryMeta() {
        LocalResult result = session.getDatabase().getResultFactory().create(session, expressionArray,
                visibleColumnCount);
        result.done();
        return result;
    }

    //执行insert into t select时target不为null
    @Override
    protected ResultInterface queryWithoutCache(int maxRows, ResultTarget target) {
        int limitRows = maxRows == 0 ? -1 : maxRows;
        if (limitExpr != null) {
            Value v = limitExpr.getValue(session);
            int l = v == ValueNull.INSTANCE ? -1 : v.getInt();
            if (limitRows < 0) {
                limitRows = l;
            } else if (l >= 0) {
                limitRows = Math.min(l, limitRows);
            }
        }
        boolean fetchPercent = this.fetchPercent;
        if (fetchPercent) {
            // Need to check it row, because negative limit has special treatment later
            if (limitRows < 0 || limitRows > 100) {
                throw DbException.getInvalidValueException("FETCH PERCENT", limitRows);
            }
            // 0 PERCENT means 0
            if (limitRows == 0) {
                fetchPercent = false;
            }
        }
        long offset;
        if (offsetExpr != null) {
            offset = offsetExpr.getValue(session).getLong();
            if (offset < 0) {
                offset = 0;
            }
        } else {
            offset = 0;
        }
        boolean lazy = session.isLazyQueryExecution() &&
                target == null && !isForUpdate && !isQuickAggregateQuery &&
                limitRows != 0 && !fetchPercent && !withTies && offset == 0 && isReadOnly();
        int columnCount = expressions.size();
        LocalResult result = null;
        if (!lazy && (target == null ||
                !session.getDatabase().getSettings().optimizeInsertFromSelect)) {
            result = createLocalResult(result);
        }
//<<<<<<< HEAD
//        //就算target不为null，如果满足下面的特殊条件还是必须建立LocalResult
//        if (sort != null && (!sortUsingIndex || distinct)) {
//=======
        // Do not add rows before OFFSET to result if possible
        boolean quickOffset = !fetchPercent;
        if (sort != null && (!sortUsingIndex || isAnyDistinct() || withTies)) {
            result = createLocalResult(result);
            result.setSortOrder(sort);
            if (!sortUsingIndex) {
                quickOffset = false;
            }
        }
        if (distinct) {
            if (!isDistinctQuery) {
                quickOffset = false;
                result = createLocalResult(result);
                result.setDistinct();
            }
        } else if (distinctExpressions != null) {
            quickOffset = false;
            result = createLocalResult(result);
            result.setDistinct(distinctIndexes);
        }
        if (isWindowQuery || isGroupQuery && !isGroupSortedQuery) {
            result = createLocalResult(result);
        }
        if (!lazy && (limitRows >= 0 || offset > 0)) {
            result = createLocalResult(result);
        }
        topTableFilter.startQuery(session);
        topTableFilter.reset();
        boolean exclusive = isForUpdate && !isForUpdateMvcc; //见setForUpdate(boolean)
        if (isForUpdateMvcc) {
            if (isGroupQuery) {
                throw DbException.getUnsupportedException(
                        "MVCC=TRUE && FOR UPDATE && GROUP");
            } else if (isAnyDistinct()) {
                throw DbException.getUnsupportedException(
                        "MVCC=TRUE && FOR UPDATE && DISTINCT");
            } else if (isQuickAggregateQuery) {
                throw DbException.getUnsupportedException(
                        "MVCC=TRUE && FOR UPDATE && AGGREGATE");
            } else if (topTableFilter.getJoin() != null) {
                throw DbException.getUnsupportedException(
                        "MVCC=TRUE && FOR UPDATE && JOIN");
            }
        }
        topTableFilter.lock(session, exclusive, exclusive);
        ResultTarget to = result != null ? result : target;
        lazy &= to == null;
        LazyResult lazyResult = null;
        //如果行数限制是0，那么什么也不做
        if (limitRows != 0) {
            // Cannot apply limit now if percent is specified
            int limit = fetchPercent ? -1 : limitRows;
            try {
                if (isQuickAggregateQuery) {
                    queryQuick(columnCount, to, quickOffset && offset > 0);
                } else if (isWindowQuery) {
                    if (isGroupQuery) {
                        queryGroupWindow(columnCount, result, offset, quickOffset);
                    } else {
                        queryWindow(columnCount, result, offset, quickOffset);
                    }
                } else if (isGroupQuery) {
                    if (isGroupSortedQuery) {
                        lazyResult = queryGroupSorted(columnCount, to, offset, quickOffset);
                    } else {
//<<<<<<< HEAD
//                        //isGroupQuery为true且isGroupSortedQuery为false时，result总是为null的，此时用to也是一样的
//                        queryGroup(columnCount, result);
//=======
                        queryGroup(columnCount, result, offset, quickOffset);
                    }
                } else if (isDistinctQuery) {
                    queryDistinct(to, offset, limit, withTies, quickOffset);
                } else {
                    lazyResult = queryFlat(columnCount, to, offset, limit, withTies, quickOffset);
                }
                if (quickOffset) {
                    offset = 0;
                }
            } finally {
                if (!lazy) {
                    resetJoinBatchAfterQuery();
                }
            }
        }
        assert lazy == (lazyResult != null): lazy;
        if (lazyResult != null) {
            if (limitRows > 0) {
                lazyResult.setLimit(limitRows);
            }
            if (randomAccessResult) {
                return convertToDistinct(lazyResult);
            } else {
                return lazyResult;
            }
        }
        if (offset != 0) {
            if (offset > Integer.MAX_VALUE) {
                throw DbException.getInvalidValueException("OFFSET", offset);
            }
            result.setOffset((int) offset);
        }
        if (limitRows >= 0) {
            result.setLimit(limitRows);
            result.setFetchPercent(fetchPercent);
            result.setWithTies(withTies);
        }
        if (result != null) {
            result.done();
            if (randomAccessResult && !distinct) {
                result = convertToDistinct(result);
            }
            if (target != null) {
                while (result.next()) {
                    target.addRow(result.currentRow());
                }
                result.close();
                return null;
            }
            return result;
        }
        return null;
    }

    /**
     * Reset the batch-join after the query result is closed.
     */
    void resetJoinBatchAfterQuery() {
        JoinBatch jb = getJoinBatch();
        if (jb != null) {
            jb.reset(false);
        }
    }

    private LocalResult createLocalResult(LocalResult old) {
        return old != null ? old : session.getDatabase().getResultFactory().create(session, expressionArray,
                visibleColumnCount);
    }
//<<<<<<< HEAD
//    
//    //把"select *"或"select t.*"转成"select 表的所有字段"
//    //也就是把单个Wildcard展开成多个ExpressionColumn
//=======

    private LocalResult convertToDistinct(ResultInterface result) {
        LocalResult distinctResult = session.getDatabase().getResultFactory().create(session,
            expressionArray, visibleColumnCount);
        distinctResult.setDistinct();
        result.reset();
        while (result.next()) {
            distinctResult.addRow(result.currentRow());
        }
        result.close();
        distinctResult.done();
        return distinctResult;
    }

    private void expandColumnList() {
        Database db = session.getDatabase();

        // the expressions may change within the loop
        for (int i = 0; i < expressions.size(); i++) {
            Expression expr = expressions.get(i);
            //select表达式中可以同时出现*和字段名
            //如select id, * from mytable as t where id>199
            //当expr是id时是一个ExpressionColumn
            //当expr是*时是一个Wildcard
            //只有子类org.h2.expression.Wildcard覆盖了isWildcard方法并且返回为true
            if (!expr.isWildcard()) {
                continue;
            }
            String schemaName = expr.getSchemaName();
            //ExpressionColumn类没有覆盖getTableAlias方法，所以哪怕这样 public.t.id引用列，也不会返回t
            //不过这里的代码没问题，因为运行到这里时expr肯定是Wildcard，Wildcard有覆盖getTableAlias方法,
            //这些注释只是顺便提一下getTableAlias在超类和不同子类中的实现差别
            String tableAlias = expr.getTableAlias();
            
            //如"select *"
            if (tableAlias == null) {
                expressions.remove(i);
                //有可能是多表join，这时一个*就会先扩展成多表的字段
                for (TableFilter filter : filters) {
                    i = expandColumnList(filter, i);
                }
                i--; //expandColumnList里多加了1，所以要减一
                
            } else { //如"select t.*"

            	//如select public.t.* from mytable as t where id>199
            	//其中public是schemaName
            	//t是tableAlias
                TableFilter filter = null;
                for (TableFilter f : filters) {
                    //select mytable.* from mytable as t这种用法是错的，MySQL也报错
                    //必须这样select t.* from mytable as t或者select mytable.* from mytable
                    if (db.equalsIdentifiers(tableAlias, f.getTableAlias())) {
                        if (schemaName == null ||
                                db.equalsIdentifiers(schemaName,
                                        f.getSchemaName())) {
                            filter = f;
                            break;
                        }
                    }
                }
                if (filter == null) {
                    throw DbException.get(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1,
                            tableAlias);
                }
                expressions.remove(i);

                // expandColumnList里多加了1，所以要减一
                // 所以下次实际是展开后的下一个元素开始
                // 比如select public.t.id, *, name from mytable as t where id>199
                // 展开后是select public.t.id, [id, name], name from mytable as t where id>199
                // 下次就从最后一个name开始
                i = expandColumnList(filter, i);
                i--;  
            }
        }
    }

    //这个方法已经能够处理没有字段的表
    private int expandColumnList(TableFilter filter, int index) {
        Table t = filter.getTable();
        String alias = filter.getTableAlias();
        Column[] columns = t.getColumns();                
        // 原先是select * from natural_join_test_table1 natural join natural_join_test_table2
        // AGE2没有忽略，只有NATURAL_JOIN_TEST_TABLE2的id和name被忽略了，因为他们是Natural Join列
        // [NATURAL_JOIN_TEST_TABLE1.ID, NATURAL_JOIN_TEST_TABLE1.NAME,
        // NATURAL_JOIN_TEST_TABLE1.AGE1, NATURAL_JOIN_TEST_TABLE2.AGE2]
        for (Column c : columns) { 
            if (!c.getVisible()) {
                continue;
            }
            // 跳过Natural Join列，
            // 右边的表对应的TableFilter有Natural Join列，而左边没有
            if (filter.isNaturalJoinColumn(c)) {
                continue;
            }
            String name = filter.getDerivedColumnName(c);
            ExpressionColumn ec = new ExpressionColumn(
                    session.getDatabase(), null, alias, name != null ? name : c.getName());
            expressions.add(index++, ec);
        }
        return index;
    }

    @Override
    public void init() {
    	//expressions字段会动态加入*扩展后的字段、缺失的order by、GROUP BY字段，还有having表达式
        if (SysProperties.CHECK && checkInit) {
            DbException.throwInternalError();
        }
        expandColumnList();
        visibleColumnCount = expressions.size(); //visibleColumnCount不包含缺失的order by、GROUP BY字段，还有having表达式
        ArrayList<String> expressionSQL;
//<<<<<<< HEAD
//        if (orderList != null || group != null) { //只有order by和group by里的字段会引用select表达式中名称
//            expressionSQL = New.arrayList();
//=======
        if (distinctExpressions != null || orderList != null || group != null) {
            expressionSQL = new ArrayList<>(visibleColumnCount);
            for (int i = 0; i < visibleColumnCount; i++) {
                Expression expr = expressions.get(i);
                //例如: select name as n, id from mytable order id
                //expressionSQL是[name, id]而不是[n, id], 不使用org.h2.expression.Alias的别名
                expr = expr.getNonAliasExpression();
                String sql = expr.getSQL();
                expressionSQL.add(sql);
            }
        } else {
            expressionSQL = null;
        }
        if (distinctExpressions != null) {
            BitSet set = new BitSet();
            for (Expression e : distinctExpressions) {
                set.set(initExpression(session, expressions, expressionSQL, e, visibleColumnCount, false,
                        filters));
            }
            int idx = 0, cnt = set.cardinality();
            distinctIndexes = new int[cnt];
            for (int i = 0; i < cnt; i++) {
                idx = set.nextSetBit(idx);
                distinctIndexes[i] = idx;
                idx++;
            }
        }
        if (orderList != null) {
//<<<<<<< HEAD
//        	//在select中加distinct时distinct变量为true
//        	//此时如果order by子句中的字段在select字段列表中不存在，那么就认为是错误
//        	//比如select distinct name from mytable order by id desc是错的
//        	//错误提示:  org.h2.jdbc.JdbcSQLException: Order by expression "ID" must be in the result list in this case; 
//        	//这样就没问题select name from mytable order by id desc
//        	//会自动加order by中的字段到select字段列表中
//            initOrder(session, expressions, expressionSQL, orderList, visibleColumnCount, distinct, filters);
//=======
            initOrder(session, expressions, expressionSQL, orderList,
                    visibleColumnCount, isAnyDistinct(), filters);
        }
        distinctColumnCount = expressions.size();
        if (having != null) {
            expressions.add(having);
            havingIndex = expressions.size() - 1;
            having = null;
        } else {
            havingIndex = -1;
        }

        if (withTies && !hasOrder()) {
            throw DbException.get(ErrorCode.WITH_TIES_WITHOUT_ORDER_BY);
        }

        Database db = session.getDatabase();

        // first the select list (visible columns),
        // then 'ORDER BY' expressions,
        // then 'HAVING' expressions,
        // and 'GROUP BY' expressions at the end
        
        //为groupIndex和groupByExpression两个字段赋值，
        //groupIndex记录了GROUP BY子句中的字段在select字段列表中的位置索引(从0开始计数)
        //groupByExpression数组的大小跟select字段列表一样，类似于一个bitmap，用来记录select字段列表中的哪些字段是GROUP BY字段
        //如果GROUP BY子句中的字段不在select字段列表中，那么会把它加到select字段列表
        if (group != null) {
            int size = group.size();
            int expSize = expressionSQL.size();
            groupIndex = new int[size];
            for (int i = 0; i < size; i++) {
                Expression expr = group.get(i);
                String sql = expr.getSQL();
                int found = -1;
                for (int j = 0; j < expSize; j++) {
                    String s2 = expressionSQL.get(j);
                    if (db.equalsIdentifiers(s2, sql)) {
                        found = j;
                        break;
                    }
                }
                if (found < 0) {
                    // special case: GROUP BY a column alias
                    for (int j = 0; j < expSize; j++) {
                        Expression e = expressions.get(j);
                        if (db.equalsIdentifiers(sql, e.getAlias())) {
                            found = j;
                            break;
                        }
                        sql = expr.getAlias();
                        if (db.equalsIdentifiers(sql, e.getAlias())) {
                            found = j;
                            break;
                        }
                    }
                }
                if (found < 0) {
                    int index = expressions.size();
                    groupIndex[i] = index;
                    expressions.add(expr);
                } else {
                    groupIndex[i] = found;
                }
            }
            groupByExpression = new boolean[expressions.size()];
            for (int gi : groupIndex) {
                groupByExpression[gi] = true;
            }
            group = null;
        }
        // map columns in select list and condition
        for (TableFilter f : filters) {
            mapColumns(f, 0);
        }
//<<<<<<< HEAD
//        // 我注释掉了，改进办法见ExpressionColumn
//        // if (havingIndex >= 0) { //在对expressions进行mapColumns时map过了
//        // Expression expr = expressions.get(havingIndex);
//        // SelectListColumnResolver res = new SelectListColumnResolver(this);
//        // 处理having中出现列别名的场景
//        // //当having不在visibleColumn的字段列表中时，mapColumns什么都不做
//        // expr.mapColumns(res, 0);
//        // }
//
//=======
        if (havingIndex >= 0) {
            Expression expr = expressions.get(havingIndex);
            SelectListColumnResolver res = new SelectListColumnResolver(this);
            expr.mapColumns(res, 0, Expression.MAP_INITIAL);
        }
        checkInit = true;
    }

    @Override
    public void prepare() {
        if (isPrepared) {
            // sometimes a subquery is prepared twice (CREATE TABLE AS SELECT)
            return;
        }
        if (SysProperties.CHECK && !checkInit) {
            DbException.throwInternalError("not initialized");
        }
        //得到一个综合的SortOrder实例后清除无用的orderList
        if (orderList != null) {
            sort = prepareOrder(orderList, expressions.size());
            orderList = null;
        }
//<<<<<<< HEAD
//        //init中已经事先mapColumns了
//=======
        ColumnNamer columnNamer = new ColumnNamer(session);
        for (int i = 0; i < expressions.size(); i++) {
            Expression e = expressions.get(i);
            String proposedColumnName = e.getAlias();
            String columnName = columnNamer.getColumnName(e, i, proposedColumnName);
            // if the name changed, create an alias
            if (!columnName.equals(proposedColumnName)) {
                e = new Alias(e, columnName, true);
            }
            expressions.set(i, e.optimize(session));
        }
        if (condition != null) {
            condition = condition.optimize(session);
            for (TableFilter f : filters) {
                // outer joins: must not add index conditions such as
                // "c is null" - example:
                // create table parent(p int primary key) as select 1;
                // create table child(c int primary key, pc int);
                // insert into child values(2, 1);
                // select p, c from parent
                // left outer join child on p = pc where c is null;
                if (!f.isJoinOuter() && !f.isJoinOuterIndirect()) {
                	//只要是正常的带有where条件的都会调用
                	//建立索引条件
                	//如: select name from mytable where id=3
                	//另外，像:SELECT rownum, * FROM JoinTest1 LEFT OUTER JOIN JoinTest2 WHERE id2=90
                	//因为f=JoinTest2是outer连接，所以也不把条件加到JoinTest2这个TableFilter中
                	//同时因为id2不是JoinTest1的字段，所以也不加入JoinTest1这个TableFilter中
                    condition.createIndexConditions(session, f);
                }
            }
        }
        
        //这里是针对min、max、count三个聚合函数的特别优化，见org.h2.expression.Aggregate.getValue(Session)
        //有group by或having或聚合函数时isGroupQuery=true
        //同时满足下面5个条件时isQuickAggregateQuery为true
        //isGroupQuery=true、groupIndex=null(即没有group by子句)、没有having、单表、无where
        //测试下面代码用select count(id) from mytable
        if (isGroupQuery && groupIndex == null && havingIndex < 0 && filters.size() == 1) {
            if (condition == null) {
                Table t = filters.get(0).getTable();
                //返回OPTIMIZABLE_MIN_MAX_COUNT_ALL类型
                ExpressionVisitor optimizable = ExpressionVisitor.getOptimizableVisitor(t);
                //isEverything里再判断所有的select字段表达式对于OPTIMIZABLE_MIN_MAX_COUNT_ALL是否可优化
                //可参考org.h2.expression.Aggregate.isEverything(ExpressionVisitor)
                isQuickAggregateQuery = isEverything(optimizable);
            }
//<<<<<<< HEAD
//        } 
//
//        // 这一步里头会为topTableFilter选择最合适的索引，只不过下面3个if如果碰到特殊情况再调整索引
//        cost = preparePlan(session.isParsingView());
//
//        // 以下三个if语句是用来选择不同的index
//
//        // 用select distinct name from mytable测试下面的代码，
//        // 为name建立UNIQUE(name,...)(name是第一个字段)，建表时为name字段加SELECTIVITY 10
//        // 这样，就不会用其他索引，而是用UNIQUE索引
//=======
        }
        cost = preparePlan(session.isParsingCreateView());
        if (distinct && session.getDatabase().getSettings().optimizeDistinct &&
                !isGroupQuery && filters.size() == 1 &&
                expressions.size() == 1 && condition == null) {
            Expression expr = expressions.get(0);
            expr = expr.getNonAliasExpression();
            if (expr instanceof ExpressionColumn) {
                Column column = ((ExpressionColumn) expr).getColumn();
                int selectivity = column.getSelectivity();
                Index columnIndex = topTableFilter.getTable().
                        getIndexForColumn(column, false, true);
                if (columnIndex != null &&
                        selectivity != Constants.SELECTIVITY_DEFAULT &&
                        selectivity < 20) {
                    // the first column must be ascending
                    boolean ascending = columnIndex.
                            getIndexColumns()[0].sortType == SortOrder.ASCENDING;
                    Index current = topTableFilter.getIndex();
                    // if another index is faster
                    if (columnIndex.canFindNext() && ascending &&
                            (current == null ||
                            current.getIndexType().isScan() ||
                            columnIndex == current)) {
                        IndexType type = columnIndex.getIndexType();
                        // hash indexes don't work, and unique single column
                        // indexes don't work
                        if (!type.isHash() && (!type.isUnique() ||
                                columnIndex.getColumns().length > 1)) {
                            topTableFilter.setIndex(columnIndex);
                            isDistinctQuery = true;
                        }
                    }
                }
            }
        }
        //有order by，但是没有聚合函数，也没有group by的情型
        //!isQuickAggregateQuery这个条件是多余的，因为isGroupQuery为false时，isQuickAggregateQuery必定是false
        //所以改成if (sort != null && !isGroupQuery)就足够了
        if (sort != null && !isQuickAggregateQuery && !isGroupQuery) {
            Index index = getSortIndex();
            Index current = topTableFilter.getIndex();
            if (index != null && current != null) {
                if (current.getIndexType().isScan() || current == index) {
                    topTableFilter.setIndex(index);
                    if (!topTableFilter.hasInComparisons()) {
                        // in(select ...) and in(1,2,3) may return the key in
                        // another order
                        sortUsingIndex = true;
                    }
                //见my.test.command.dml.SelectTest.getSortIndex()中的测试
                } else if (index.getIndexColumns() != null
                        && index.getIndexColumns().length >= current
                                .getIndexColumns().length) {
                    IndexColumn[] sortColumns = index.getIndexColumns();
                    IndexColumn[] currentColumns = current.getIndexColumns();
                    boolean swapIndex = false;
                    for (int i = 0; i < currentColumns.length; i++) {
                        if (sortColumns[i].column != currentColumns[i].column) {
                            swapIndex = false;
                            break;
                        }
                        //调用getSortIndex后肯定确认它返回的索引的顺序与order by的排序一样，如果最初选择的索引不一样，那么就替换
                        if (sortColumns[i].sortType != currentColumns[i].sortType) {
                            swapIndex = true;
                        }
                    }
                    if (swapIndex) {
                        topTableFilter.setIndex(index);
                        sortUsingIndex = true;
                    }
                }
            }
        }
        //没有快速聚合函数，有group by的情型
        //其实不需要调用getGroupByExpressionCount()的，
        //只要判断groupByExpression!=null即可，此时getGroupByExpressionCount()必定>0
        //因为在调用init时，如果groupByExpression不为null，必定包含true值的
        if (!isQuickAggregateQuery && isGroupQuery && getGroupByExpressionCount() > 0) {
            Index index = getGroupSortedIndex();
            Index current = topTableFilter.getIndex();
            if (index != null && current != null && (current.getIndexType().isScan() ||
                    current == index)) {
                topTableFilter.setIndex(index);
                isGroupSortedQuery = true;
            }
        }
        expressionArray = expressions.toArray(new Expression[0]);
        isPrepared = true;
    }

    @Override
    public void prepareJoinBatch() {
        ArrayList<TableFilter> list = new ArrayList<>();
        TableFilter f = getTopTableFilter();
        do {
            if (f.getNestedJoin() != null) {
                // we do not support batching with nested joins
                return;
            }
            list.add(f);
            f = f.getJoin();
        } while (f != null);
        TableFilter[] fs = list.toArray(new TableFilter[0]);
        // prepare join batch
        JoinBatch jb = null;
        for (int i = fs.length - 1; i >= 0; i--) {
            jb = fs[i].prepareJoinBatch(jb, fs, i);
        }
    }

    public JoinBatch getJoinBatch() {
        return getTopTableFilter().getJoinBatch();
    }

    @Override
    public double getCost() {
        return cost;
    }

    @Override
    public HashSet<Table> getTables() {
        HashSet<Table> set = new HashSet<>();
        for (TableFilter filter : filters) {
            set.add(filter.getTable());
        }
        return set;
    }

    @Override
    public void fireBeforeSelectTriggers() {
        for (TableFilter filter : filters) {
            filter.getTable().fire(session, Trigger.SELECT, true);
        }
    }

    //注意，只关心top层的TableFilter
    private double preparePlan(boolean parse) {
        TableFilter[] topArray = topFilters.toArray(new TableFilter[0]);
        for (TableFilter t : topArray) {
            t.setFullCondition(condition);
        }

        Optimizer optimizer = new Optimizer(topArray, condition, session);
        optimizer.optimize(parse);
        topTableFilter = optimizer.getTopFilter();
        double planCost = optimizer.getCost();

        setEvaluatableRecursive(topTableFilter);

        if (!parse) {
            topTableFilter.prepare();
        }
        return planCost;
    }

    private void setEvaluatableRecursive(TableFilter f) {
        for (; f != null; f = f.getJoin()) {
            f.setEvaluatable(f, true);
            if (condition != null) {
                condition.setEvaluatable(f, true);
            }
            TableFilter n = f.getNestedJoin();
            if (n != null) {
                setEvaluatableRecursive(n);
            }
            Expression on = f.getJoinCondition();
            if (on != null) {
                if (!on.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)) {
                    // need to check that all added are bound to a table
                    on = on.optimize(session);
                    if (!f.isJoinOuter() && !f.isJoinOuterIndirect()) {
                        f.removeJoinCondition();
                        addCondition(on);
                    }
                }
            }
            on = f.getFilterCondition();
            if (on != null) {
                if (!on.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)) {
                    f.removeFilterCondition();
                    addCondition(on);
                }
            }
            // this is only important for subqueries, so they know
            // the result columns are evaluatable
            for (Expression e : expressions) {
                e.setEvaluatable(f, true);
            }
        }
    }

    @Override
    public String getPlanSQL() {
        // can not use the field sqlStatement because the parameter
        // indexes may be incorrect: ? may be in fact ?2 for a subquery
        // but indexes may be set manually as well
        Expression[] exprList = expressions.toArray(new Expression[0]);
        StatementBuilder buff = new StatementBuilder();
        for (TableFilter f : topFilters) {
            Table t = f.getTable();
            TableView tableView = t.isView() ? (TableView) t : null;
            if (tableView != null && tableView.isRecursive() && tableView.isTableExpression()) {

                if (!tableView.isTemporary()) {
                    // skip the generation of plan SQL for this already recursive persistent CTEs,
                    // since using a with statement will re-create the common table expression
                    // views.
                } else {
                    buff.append("WITH RECURSIVE ")
                            .append(t.getSchema().getSQL()).append('.').append(Parser.quoteIdentifier(t.getName()))
                            .append('(');
                    buff.resetCount();
                    for (Column c : t.getColumns()) {
                        buff.appendExceptFirst(",");
                        buff.append(c.getName());
                    }
                    buff.append(") AS ").append(t.getSQL()).append('\n');
                }
            }
        }
        buff.resetCount();
        buff.append("SELECT");
        if (isAnyDistinct()) {
            buff.append(" DISTINCT");
            if (distinctExpressions != null) {
                buff.append(" ON(");
                for (Expression distinctExpression: distinctExpressions) {
                    buff.appendExceptFirst(", ");
                    buff.append(distinctExpression.getSQL());
                }
                buff.append(')');
                buff.resetCount();
            }
        }
        for (int i = 0; i < visibleColumnCount; i++) {
            buff.appendExceptFirst(",");
            buff.append('\n');
            buff.append(StringUtils.indent(exprList[i].getSQL(), 4, false));
        }
        buff.append("\nFROM ");
        TableFilter filter = topTableFilter;
        if (filter != null) {
            buff.resetCount();
            int i = 0;
            do {
                buff.appendExceptFirst("\n");
                buff.append(filter.getPlanSQL(i++ > 0));
                filter = filter.getJoin();
            } while (filter != null);
        } else {
            buff.resetCount();
            int i = 0;
            for (TableFilter f : topFilters) {
                do {
                    buff.appendExceptFirst("\n");
                    buff.append(f.getPlanSQL(i++ > 0));
                    f = f.getJoin();
                } while (f != null);
            }
        }
        if (condition != null) {
            buff.append("\nWHERE ").append(
                    StringUtils.unEnclose(condition.getSQL()));
        }
        if (groupIndex != null) {
            buff.append("\nGROUP BY ");
            buff.resetCount();
            for (int gi : groupIndex) {
                Expression g = exprList[gi];
                g = g.getNonAliasExpression();
                buff.appendExceptFirst(", ");
                buff.append(StringUtils.unEnclose(g.getSQL()));
            }
        }
        if (group != null) {
            buff.append("\nGROUP BY ");
            buff.resetCount();
            for (Expression g : group) {
                buff.appendExceptFirst(", ");
                buff.append(StringUtils.unEnclose(g.getSQL()));
            }
        }
        if (having != null) {
            // could be set in addGlobalCondition
            // in this case the query is not run directly, just getPlanSQL is
            // called
            Expression h = having;
            buff.append("\nHAVING ").append(
                    StringUtils.unEnclose(h.getSQL()));
        } else if (havingIndex >= 0) {
            Expression h = exprList[havingIndex];
            buff.append("\nHAVING ").append(
                    StringUtils.unEnclose(h.getSQL()));
        }
        if (sort != null) {
            buff.append("\nORDER BY ").append(
                    sort.getSQL(exprList, visibleColumnCount));
        }
        if (orderList != null) {
            buff.append("\nORDER BY ");
            buff.resetCount();
            for (SelectOrderBy o : orderList) {
                buff.appendExceptFirst(", ");
                buff.append(StringUtils.unEnclose(o.getSQL()));
            }
        }
        appendLimitToSQL(buff.builder());
        if (sampleSizeExpr != null) {
            buff.append("\nSAMPLE_SIZE ").append(
                    StringUtils.unEnclose(sampleSizeExpr.getSQL()));
        }
        if (isForUpdate) {
            buff.append("\nFOR UPDATE");
        }
        if (isQuickAggregateQuery) {
            buff.append("\n/* direct lookup */");
        }
        if (isDistinctQuery) {
            buff.append("\n/* distinct */");
        }
        if (sortUsingIndex) {
            buff.append("\n/* index sorted */");
        }
        if (isGroupQuery) {
            if (isGroupSortedQuery) {
                buff.append("\n/* group sorted */");
            }
        }
        // buff.append("\n/* cost: " + cost + " */");
        return buff.toString();
    }

    public void setHaving(Expression having) {
        this.having = having;
    }

    public Expression getHaving() { //未使用
        return having;
    }

    @Override
    public int getColumnCount() {
        return visibleColumnCount;
    }

    public TableFilter getTopTableFilter() {
        return topTableFilter;
    }

    @Override
    public void setForUpdate(boolean b) {
        this.isForUpdate = b;
        if (session.getDatabase().getSettings().selectForUpdateMvcc &&
                session.getDatabase().isMVStore()) {
            isForUpdateMvcc = b;
        }
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level) {
        for (Expression e : expressions) {
//<<<<<<< HEAD
//            // 像这样sql =
//            // "select id, name from natural_join_test_table1, natural_join_test_table2";
//            // //如果natural_join_test_table1和natural_join_test_table2有相同的id,name
//            // //那么在第一次org.h2.expression.ExpressionColumn.mapColumn(ColumnResolver,
//            // Column, int)时
//            // //columnResolver=null，此时columnResolver设为natural_join_test_table1
//            // //当第二次mapColumn时，因为id这个ExpressionColumn的columnResolver已经设过了，所以报错:
//            // //Ambiguous column name "ID";
//            e.mapColumns(resolver, level);
//=======
            e.mapColumns(resolver, level, Expression.MAP_INITIAL);
        }
        if (condition != null) {
            condition.mapColumns(resolver, level, Expression.MAP_INITIAL);
        }
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        for (Expression e : expressions) {
            e.setEvaluatable(tableFilter, b);
        }
        if (condition != null) {
            condition.setEvaluatable(tableFilter, b);
        }
    }

    /**
     * Check if this is an aggregate query with direct lookup, for example a
     * query of the type SELECT COUNT(*) FROM TEST or
     * SELECT MAX(ID) FROM TEST.
     *
     * @return true if a direct lookup is possible
     */
    public boolean isQuickAggregateQuery() {
        return isQuickAggregateQuery;
    }

    /**
     * Checks if this query is a group query.
     *
     * @return whether this query is a group query.
     */
    public boolean isGroupQuery() {
        return isGroupQuery;
    }

    /**
     * Checks if this query contains window functions.
     *
     * @return whether this query contains window functions
     */
    public boolean isWindowQuery() {
        return isWindowQuery;
    }

    /**
     * Checks if window stage of group window query is performed. If true,
     * column resolver may not be used.
     *
     * @return true if window stage of group window query is performed
     */
    public boolean isGroupWindowStage2() {
        return isGroupWindowStage2;
    }

    @Override
    public void addGlobalCondition(Parameter param, int columnId,
            int comparisonType) {
        addParameter(param);
        Expression comp;
        Expression col = expressions.get(columnId);
        col = col.getNonAliasExpression();
        if (col.isEverything(ExpressionVisitor.QUERY_COMPARABLE_VISITOR)) {
            comp = new Comparison(session, comparisonType, col, param);
        } else {
            // this condition will always evaluate to true, but need to
            // add the parameter, so it can be set later
            comp = new Comparison(session, Comparison.EQUAL_NULL_SAFE, param, param);
        }
        comp = comp.optimize(session);
        boolean addToCondition = true;
        if (isGroupQuery) {
            addToCondition = false;
            for (int i = 0; groupIndex != null && i < groupIndex.length; i++) {
                if (groupIndex[i] == columnId) {
                    addToCondition = true;
                    break;
                }
            }
            if (!addToCondition) {
                if (havingIndex >= 0) {
                    having = expressions.get(havingIndex);
                }
                if (having == null) {
                    having = comp;
                } else {
                    having = new ConditionAndOr(ConditionAndOr.AND, having, comp);
                }
            }
        }
        if (addToCondition) {
            if (condition == null) {
                condition = comp;
            } else {
                condition = new ConditionAndOr(ConditionAndOr.AND, condition, comp);
            }
        }
    }

    @Override
    public void updateAggregate(Session s, int stage) {
        for (Expression e : expressions) {
            e.updateAggregate(s, stage);
        }
        if (condition != null) {
            condition.updateAggregate(s, stage);
        }
        if (having != null) {
            having.updateAggregate(s, stage);
        }
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        switch (visitor.getType()) {
        case ExpressionVisitor.DETERMINISTIC: {
            if (isForUpdate) {
                return false;
            }
            for (TableFilter f : filters) {
                if (!f.getTable().isDeterministic()) {
                    return false;
                }
            }
            break;
        }
        case ExpressionVisitor.SET_MAX_DATA_MODIFICATION_ID: {
            for (TableFilter f : filters) {
                long m = f.getTable().getMaxDataModificationId();
                visitor.addDataModificationId(m);
            }
            break;
        }
        case ExpressionVisitor.EVALUATABLE: {
            if (!session.getDatabase().getSettings().optimizeEvaluatableSubqueries) {
                return false;
            }
            break;
        }
        case ExpressionVisitor.GET_DEPENDENCIES: {
            for (TableFilter f : filters) {
                Table table = f.getTable();
                visitor.addDependency(table);
                table.addDependencies(visitor.getDependencies());
            }
            break;
        }
        default:
        }
        ExpressionVisitor v2 = visitor.incrementQueryLevel(1);
        for (Expression e : expressions) {
            if (!e.isEverything(v2)) {
                return false;
            }
        }
        if (condition != null && !condition.isEverything(v2)) {
            return false;
        }
        if (having != null && !having.isEverything(v2)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return isEverything(ExpressionVisitor.READONLY_VISITOR);
    }

    @Override
    public boolean isCacheable() {
        return !isForUpdate;
    }

    @Override
    public boolean allowGlobalConditions() {
//<<<<<<< HEAD
//        if (offsetExpr == null && (limitExpr == null || sort == null)) {
//            return true;
//        }
//        return false;
//	}
//	
//	public String toString() { //我加上的
//        return getPlanSQL();
//=======
        return offsetExpr == null && (limitExpr == null || sort == null);
    }

    public SortOrder getSortOrder() {
        return sort;
    }

    /**
     * Lazy execution for this select.
     */
    private abstract class LazyResultSelect extends LazyResult {

        int rowNumber;
        int columnCount;

        LazyResultSelect(Expression[] expressions, int columnCount) {
            super(expressions);
            this.columnCount = columnCount;
            setCurrentRowNumber(0);
        }

        @Override
        public final int getVisibleColumnCount() {
            return visibleColumnCount;
        }

        @Override
        public void close() {
            if (!isClosed()) {
                super.close();
                resetJoinBatchAfterQuery();
            }
        }

        @Override
        public void reset() {
            super.reset();
            resetJoinBatchAfterQuery();
            topTableFilter.reset();
            setCurrentRowNumber(0);
            rowNumber = 0;
        }
    }

    /**
     * Lazy execution for a flat query.
     */
    private final class LazyResultQueryFlat extends LazyResultSelect {

        int sampleSize;

        LazyResultQueryFlat(Expression[] expressions, int sampleSize, int columnCount) {
            super(expressions, columnCount);
            this.sampleSize = sampleSize;
        }

        @Override
        protected Value[] fetchNextRow() {
            while ((sampleSize <= 0 || rowNumber < sampleSize) &&
                    topTableFilter.next()) {
                setCurrentRowNumber(rowNumber + 1);
                if (isConditionMet()) {
                    ++rowNumber;
                    Value[] row = new Value[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        Expression expr = expressions.get(i);
                        row[i] = expr.getValue(getSession());
                    }
                    return row;
                }
            }
            return null;
        }
    }

    /**
     * Lazy execution for a group sorted query.
     */
    private final class LazyResultGroupSorted extends LazyResultSelect {

        private Value[] previousKeyValues;

        LazyResultGroupSorted(Expression[] expressions, int columnCount) {
            super(expressions, columnCount);
            if (groupData == null) {
                groupData = SelectGroups.getInstance(getSession(), Select.this.expressions, isGroupQuery, groupIndex);
            } else {
                // TODO is this branch possible?
                updateAgg(columnCount, Aggregate.STAGE_RESET);
                groupData.resetLazy();
            }
        }

        @Override
        public void reset() {
            super.reset();
            groupData.resetLazy();
            previousKeyValues = null;
        }

        @Override
        protected Value[] fetchNextRow() {
            while (topTableFilter.next()) {
                setCurrentRowNumber(rowNumber + 1);
                if (isConditionMet()) {
                    rowNumber++;
                    Value[] keyValues = new Value[groupIndex.length];
                    // update group
                    for (int i = 0; i < groupIndex.length; i++) {
                        int idx = groupIndex[i];
                        Expression expr = expressions.get(idx);
                        keyValues[i] = expr.getValue(getSession());
                    }

                    Value[] row = null;
                    if (previousKeyValues == null) {
                        previousKeyValues = keyValues;
                        groupData.nextLazyGroup();
                    } else if (!Arrays.equals(previousKeyValues, keyValues)) {
                        row = createGroupSortedRow(previousKeyValues, columnCount);
                        previousKeyValues = keyValues;
                        groupData.nextLazyGroup();
                    }
                    groupData.nextLazyRow();
                    updateAgg(columnCount, Aggregate.STAGE_GROUP);
                    if (row != null) {
                        return row;
                    }
                }
            }
            Value[] row = null;
            if (previousKeyValues != null) {
                row = createGroupSortedRow(previousKeyValues, columnCount);
                previousKeyValues = null;
            }
            return row;
        }
    }
}
