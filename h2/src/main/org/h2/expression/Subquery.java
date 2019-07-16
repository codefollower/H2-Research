/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.util.ArrayList;
import java.util.Arrays;
import org.h2.api.ErrorCode;
import org.h2.command.dml.Query;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.h2.value.ValueRow;

/**
 * A query returning a single value.
 * Subqueries are used inside other statements.
 */
public class Subquery extends Expression {

    private final Query query;
    private Expression expression; //query的select字段列表，如果有多列，那么是一个ExpressionList

    public Subquery(Query query) {
        this.query = query;
    }
    
    //子查询不能多于1个列
	//sql = "delete from ConditionInSelectTest where id in(select id,name from ConditionInSelectTest where id=3)";
	//sql = "delete from ConditionInSelectTest where id in(select id from ConditionInSelectTest where id>2)";

	//sql = "delete from ConditionInSelectTest where id > ALL(select id from ConditionInSelectTest where id>10)";
	//ANY和SOME一样
	//sql = "delete from ConditionInSelectTest where id > ANY(select id from ConditionInSelectTest where id>1)";
	//sql = "delete from ConditionInSelectTest where id > SOME(select id from ConditionInSelectTest where id>10)";
	
	//严格来说这种sql才算Subquery，上面的in，ALL，ANY，SOME都只是普通的select
	//Subquery包含的行数不能大于1，而in，ALL，ANY，SOME没限制，
	//想一下也理解，比如id> (select id from ConditionInSelectTest where id>1)如果这个Subquery大于1行，那么id就不知道和谁比较
	//sql = "delete from ConditionInSelectTest where id > (select id from ConditionInSelectTest where id>1)";
    //但是Subquery可以有多例
	//sql = "delete from ConditionInSelectTest where id > (select id, name from ConditionInSelectTest where id=1 and name='a1')";
    @Override
    public Value getValue(Session session) {
        query.setSession(session);
        //getValue虽然在主查询有多条记录的情况下都会被调用，但是query内部是有缓存的，只是一个浅拷贝，所以对性能影响不大
        try (ResultInterface result = query.query(2)) {
            Value v;
            if (!result.next()) {
                v = ValueNull.INSTANCE;
            } else {
//<<<<<<< HEAD
//                Value[] values = result.currentRow();
//                if (result.getVisibleColumnCount() == 1) {
//                    v = values[0];
//                } else {
//                	//对于id > (select id, name from ConditionInSelectTest where id=1 and name='a1')
//                	//此时由id, name得到一个ValueArray
//                	//但进行比较时，左边的id也会转换成一个String数组，最后与ValueArray比较
//                	//所以如果子查询只需要一个列时，就不应该多写一个列，这样性能更高。
//                    v = ValueArray.get(values);
//                }
//=======
                v = readRow(result);
                if (result.hasNext()) {
                    throw DbException.get(ErrorCode.SCALAR_SUBQUERY_CONTAINS_MORE_THAN_ONE_ROW);
                }
            }
            return v;
//        } finally {
//          //对于org.h2.result.LocalResult只有external不为null时才把closed设为true
//        	//当在org.h2.command.dml.Query.query(int)判断org.h2.result.LocalResult.isClosed()时因为closed为false
//        	//所以这个close方法并没效果。
//            result.close();
        }
    }

    /**
     * Evaluates and returns all rows of the subquery.
     *
     * @param session
     *            the session
     * @return values in all rows
     */
    public ArrayList<Value> getAllRows(Session session) {
        ArrayList<Value> list = new ArrayList<>();
        query.setSession(session);
        try (ResultInterface result = query.query(Integer.MAX_VALUE)) {
            while (result.next()) {
                list.add(readRow(result));
            }
        }
        return list;
    }

    private static Value readRow(ResultInterface result) {
        Value[] values = result.currentRow();
        int visible = result.getVisibleColumnCount();
        return visible == 1 ? values[0]
                : ValueRow.get(visible == values.length ? values : Arrays.copyOf(values, visible));
    }

    @Override
    public TypeInfo getType() {
        return getExpression().getType();
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level, int state) {
        query.mapColumns(resolver, level + 1);
    }

    @Override
    public Expression optimize(Session session) {
        session.optimizeQueryExpression(query);
        return this;
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        query.setEvaluatable(tableFilter, b);
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder, boolean alwaysQuote) {
        return builder.append('(').append(query.getPlanSQL(alwaysQuote)).append(')');
    }

    @Override
    public void updateAggregate(Session session, int stage) {
        query.updateAggregate(session, stage);
    }

    private Expression getExpression() {
        if (expression == null) {
            ArrayList<Expression> expressions = query.getExpressions();
            int columnCount = query.getColumnCount();
            if (columnCount == 1) {
                expression = expressions.get(0);
            } else {
                Expression[] list = new Expression[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    list[i] = expressions.get(i);
                }
                expression = new ExpressionList(list, false);
            }
        }
        return expression;
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        return query.isEverything(visitor);
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public int getCost() {
        return query.getCostAsExpression();
    }

    @Override
    public Expression[] getExpressionColumns(Session session) {
        return getExpression().getExpressionColumns(session);
    }
}
