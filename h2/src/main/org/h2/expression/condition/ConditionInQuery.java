/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression.condition;

import org.h2.command.query.Query;
import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.index.IndexCondition;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.DataType;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;
import org.h2.value.ValueRow;

/**
 * An IN() condition with a subquery, as in WHERE ID IN(SELECT ...)
 */
public final class ConditionInQuery extends PredicateWithSubquery {

    private Expression left;
    private final boolean not;
    private final boolean whenOperand;
    private final boolean all;
    private final int compareType;

    public ConditionInQuery(Expression left, boolean not, boolean whenOperand, Query query, boolean all,
            int compareType) {
        super(query);
        this.left = left;
        this.not = not;
        this.whenOperand = whenOperand;
        /*
         * Need to do it now because other methods may be invoked in different
         * order.
         */
        query.setRandomAccessResult(true);
        query.setNeverLazy(true);
        query.setDistinctIfPossible();
        this.all = all;
        this.compareType = compareType;
    }

    @Override
    public Value getValue(SessionLocal session) {
        return getValue(session, left.getValue(session));
    }

    @Override
    public boolean getWhenValue(SessionLocal session, Value left) {
        if (!whenOperand) {
            return super.getWhenValue(session, left);
        }
        return getValue(session, left).getBoolean();
    }

    private Value getValue(SessionLocal session, Value left) {
        query.setSession(session);
//<<<<<<< HEAD
//        // We need a LocalResult
//        query.setNeverLazy(true);
//        // 子查询没有记录时，如果是ALL类型的子查询，那么认为条件为true，
//        // 否则为false，
//        // 假设表中字段id的值是1到6，这条语句
//        // delete from ConditionInSelectTest where id > ALL(select id from ConditionInSelectTest where id>10)
//        // 里面的子查询没有值，所以where id<ALL()时都为true，实际上是删除所有记录
//        // 如果改成ANY，那么什么记录都不删
//        query.setDistinctIfPossible();
//=======
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
        LocalResult rows = (LocalResult) query.query(0);
        if (!rows.hasNext()) {
//<<<<<<< HEAD
//            return ValueBoolean.get(all);
//        } else if (l.containsNull()) { //如果left是null，那么返回null
//            return ValueNull.INSTANCE;
//=======
            return ValueBoolean.get(not ^ all);
        }
        if ((compareType & ~1) == Comparison.EQUAL_NULL_SAFE) {
            return getNullSafeValueSlow(session, rows, left);
        }
        if (left.containsNull()) {
            return ValueNull.INSTANCE;
        }
        if (all || compareType != Comparison.EQUAL || !session.getDatabase().getSettings().optimizeInSelect) {
            return getValueSlow(session, rows, left);
        }
        // 下面代码是处理非all，且是EQUAL或EQUAL_NULL_SAFE的情况

        // 获得结果集中第一列的类型
        int columnCount = query.getColumnCount();
        if (columnCount != 1) {
            Value[] leftValue = left.convertToAnyRow().getList();
            if (columnCount == leftValue.length && rows.containsDistinct(leftValue)) {
                return ValueBoolean.get(!not);
            }
        } else {
            TypeInfo colType = rows.getColumnType(0);
            if (colType.getValueType() == Value.NULL) {
                return ValueNull.INSTANCE;
            }
            if (left.getValueType() == Value.ROW) {
                left = ((ValueRow) left).getList()[0];
            }
//<<<<<<< HEAD
//            //把left的值转成结果集中第一列的类型，然后判断结果集中是否包含它，返回true
//            l = l.convertTo(colType, session, null);
//            if (rows.containsDistinct(new Value[] { l })) {
//                return ValueBoolean.TRUE;
//=======
            if (rows.containsDistinct(new Value[] { left })) {
                return ValueBoolean.get(!not);
            }
        }
        //结果集中不包含left且有null，那么返回null
        if (rows.containsNull()) {
            return ValueNull.INSTANCE;
        }
        return ValueBoolean.get(not);
    }

    private Value getValueSlow(SessionLocal session, ResultInterface rows, Value l) {
        // this only returns the correct result if the result has at least one
        // row, and if l is not null
        boolean simple = l.getValueType() != Value.ROW && query.getColumnCount() == 1;
        boolean hasNull = false;
        ValueBoolean searched = ValueBoolean.get(!all);
        while (rows.next()) {
            Value[] currentRow = rows.currentRow();
            Value cmp = Comparison.compare(session, l, simple ? currentRow[0] : ValueRow.get(currentRow),
                    compareType);
            if (cmp == ValueNull.INSTANCE) {
                hasNull = true;
            } else if (cmp == searched) {
                return ValueBoolean.get(not == all);
            }
        }
        if (hasNull) {
            return ValueNull.INSTANCE;
        }
        return ValueBoolean.get(not ^ all);
    }

    private Value getNullSafeValueSlow(SessionLocal session, ResultInterface rows, Value l) {
        boolean simple = l.getValueType() != Value.ROW && query.getColumnCount() == 1;
        boolean searched = all == (compareType == Comparison.NOT_EQUAL_NULL_SAFE);
        while (rows.next()) {
            Value[] currentRow = rows.currentRow();
            if (session.areEqual(l, simple ? currentRow[0] : ValueRow.get(currentRow)) == searched) {
                return ValueBoolean.get(not == all);
            }
        }
        return ValueBoolean.get(not ^ all);
    }

    @Override
    public boolean isWhenConditionOperand() {
        return whenOperand;
    }

    @Override
    public Expression getNotIfPossible(SessionLocal session) {
        if (whenOperand) {
            return null;
        }
        return new ConditionInQuery(left, !not, false, query, all, compareType);
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level, int state) {
        left.mapColumns(resolver, level, state);
        super.mapColumns(resolver, level, state);
    }

    @Override
    public Expression optimize(SessionLocal session) {
        super.optimize(session);
        left = left.optimize(session);
//<<<<<<< HEAD
//        return super.optimize(session);
//
////早期的版本ConditionInSelect有下面这个判断
////      //如where id in(select id,name from ConditionInSelectTest where id=3)
////      //org.h2.jdbc.JdbcSQLException: Subquery is not a single column query
////      //子查询不能多于1个列
////      session.optimizeQueryExpression(query);
////      if (query.getColumnCount() != 1) {
////          throw DbException.get(ErrorCode.SUBQUERY_IS_NOT_SINGLE_COLUMN);
////      }
//=======
        TypeInfo.checkComparable(left.getType(), query.getRowDataType());
        return this;
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        left.setEvaluatable(tableFilter, b);
        super.setEvaluatable(tableFilter, b);
    }

    @Override
    public boolean needParentheses() {
        return true;
    }

    @Override
    public StringBuilder getUnenclosedSQL(StringBuilder builder, int sqlFlags) {
        boolean outerNot = not && (all || compareType != Comparison.EQUAL);
        if (outerNot) {
            builder.append("NOT (");
        }
        left.getSQL(builder, sqlFlags, AUTO_PARENTHESES);
        getWhenSQL(builder, sqlFlags);
        if (outerNot) {
            builder.append(')');
        }
        return builder;
    }

    @Override
    public StringBuilder getWhenSQL(StringBuilder builder, int sqlFlags) {
        if (all) {
            builder.append(Comparison.COMPARE_TYPES[compareType]).append(" ALL");
        } else if (compareType == Comparison.EQUAL) {
            if (not) {
                builder.append(" NOT");
            }
            builder.append(" IN");
        } else {
            builder.append(' ').append(Comparison.COMPARE_TYPES[compareType]).append(" ANY");
        }
        return super.getUnenclosedSQL(builder, sqlFlags);
    }

    @Override
    public void updateAggregate(SessionLocal session, int stage) {
        left.updateAggregate(session, stage);
        super.updateAggregate(session, stage);
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        return left.isEverything(visitor) && super.isEverything(visitor);
    }

    @Override
    public int getCost() {
        return left.getCost() + super.getCost();
    }

    @Override
    public void createIndexConditions(SessionLocal session, TableFilter filter) {
        if (!session.getDatabase().getSettings().optimizeInList) {
            return;
        }
        if (not || compareType != Comparison.EQUAL) {
            return;
        }
        if (query.getColumnCount() != 1) {
            return;
        }
        int leftType = left.getType().getValueType();
        if (!DataType.hasTotalOrdering(leftType)
                && leftType != query.getExpressions().get(0).getType().getValueType()) {
            return;
        }
        if (!(left instanceof ExpressionColumn)) {
            return;
        }
        ExpressionColumn l = (ExpressionColumn) left;
        if (filter != l.getTableFilter()) {
            return;
        }
        // query中的columnResolver与filter不是同一个实例时就把query加入索引条件
        // 也就是说如果query中的columnResolver与filter是同一个实例，
        // 就相当于filter自己找自己了，会出现死循环
        ExpressionVisitor visitor = ExpressionVisitor.getNotFromResolverVisitor(filter);
        if (!query.isEverything(visitor)) {
            return;
        }
        filter.addIndexCondition(IndexCondition.getInQuery(l, query));
    }

}
