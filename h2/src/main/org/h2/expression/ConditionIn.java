/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

/**
 * An 'in' condition with a list of values, as in WHERE NAME IN(...)
 */
public class ConditionIn extends Condition {

    private final Database database;
    private Expression left;
    private final ArrayList<Expression> valueList;
    private int queryLevel; //没看到用处

    /**
     * Create a new IN(..) condition.
     *
     * @param database the database
     * @param left the expression before IN
     * @param values the value list (at least one element)
     */
    public ConditionIn(Database database, Expression left,
            ArrayList<Expression> values) {
        this.database = database;
        this.left = left;
        this.valueList = values;
    }

    @Override
    public Value getValue(Session session) { //每行记录都要调用此方法判断一下
        Value l = left.getValue(session); //如果left是字段，这里就会取这个字段在当前记录中的字段值
        if (l == ValueNull.INSTANCE) {
            return l;
        }
        boolean result = false;
        boolean hasNull = false;
        //然后在这个for中，把valueList中的值转换成left的类型，并与left比较，如果相等，那么就返回true了
        for (Expression e : valueList) {
            Value r = e.getValue(session);
            if (r == ValueNull.INSTANCE) {
                hasNull = true;
            } else {
                result = Comparison.compareNotNull(database, l, r, Comparison.EQUAL);
                if (result) {
                    break;
                }
            }
        }
        //valueList中有null值，并且没有值满足left，那么返回null
        //如delete from ConditionInTest where id in(30,40,null)，假设id在表中的值没有30、40, 此时就返回null
        if (!result && hasNull) {
            return ValueNull.INSTANCE;
        }
        return ValueBoolean.get(result);
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level, int state) {
        left.mapColumns(resolver, level, state);
        for (Expression e : valueList) {
            e.mapColumns(resolver, level, state);
        }
        this.queryLevel = Math.max(level, this.queryLevel); //没看到用处
    }

    @Override
    public Expression optimize(Session session) {
        left = left.optimize(session);
        boolean constant = left.isConstant();
        //如delete from ConditionInTest where null in(1,2)
        //此时left是个constant，返回null，什么都没删除
        if (constant && left == ValueExpression.getNull()) {
            return left;
        }
        boolean allValuesConstant = true;
        boolean allValuesNull = true;
        int size = valueList.size();
        for (int i = 0; i < size; i++) {
            Expression e = valueList.get(i);
            e = e.optimize(session);
            if (e.isConstant() && e.getValue(session) != ValueNull.INSTANCE) {
                allValuesNull = false;
            }
            if (allValuesConstant && !e.isConstant()) {
                allValuesConstant = false;
            }
            if (left instanceof ExpressionColumn && e instanceof Parameter) {
                ((Parameter) e)
                        .setColumn(((ExpressionColumn) left).getColumn());
            }
            valueList.set(i, e);
        }
        //如delete from ConditionInTest where 2 in(1,2)
        //直接返回true值
        if (constant && allValuesConstant) {
            return ValueExpression.get(getValue(session));
        }
        //如delete from ConditionInTest where id in(2)
        //转换成delete from ConditionInTest where id = 2
        if (size == 1) {
            Expression right = valueList.get(0);
            Expression expr = new Comparison(session, Comparison.EQUAL, left, right);
            expr = expr.optimize(session);
            return expr;
        }

        //如select count(*) from ConditionInTest where id in(1,2)
        //left是id，不是常量，但是valueList全是常量且是相同的类型

        if (allValuesConstant && !allValuesNull) {
            int leftType = left.getType();
            if (leftType == Value.UNKNOWN) {
                return this;
            }
            if (leftType == Value.ENUM && !(left instanceof ExpressionColumn)) {
                return this;
            }
            Expression expr = new ConditionInConstantSet(session, left, valueList);
            expr = expr.optimize(session);
            return expr;
        }
        return this;
    }

    @Override
    public void createIndexConditions(Session session, TableFilter filter) {
        if (!(left instanceof ExpressionColumn)) {
            return;
        }
        ExpressionColumn l = (ExpressionColumn) left;
        if (filter != l.getTableFilter()) {
            return;
        }
        if (session.getDatabase().getSettings().optimizeInList) {
            ExpressionVisitor visitor = ExpressionVisitor.getNotFromResolverVisitor(filter);
            for (Expression e : valueList) {
                if (!e.isEverything(visitor)) {
                    return;
                }
            }
            filter.addIndexCondition(IndexCondition.getInList(l, valueList));
        }
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        left.setEvaluatable(tableFilter, b);
        for (Expression e : valueList) {
            e.setEvaluatable(tableFilter, b);
        }
    }

    @Override
    public String getSQL() {
        StatementBuilder buff = new StatementBuilder("(");
        buff.append(left.getSQL()).append(" IN(");
        for (Expression e : valueList) {
            buff.appendExceptFirst(", ");
            buff.append(e.getSQL());
        }
        return buff.append("))").toString();
    }

    @Override
    public void updateAggregate(Session session, int stage) {
        left.updateAggregate(session, stage);
        for (Expression e : valueList) {
            e.updateAggregate(session, stage);
        }
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        if (!left.isEverything(visitor)) {
            return false;
        }
        return areAllValues(visitor);
    }

    private boolean areAllValues(ExpressionVisitor visitor) {
        for (Expression e : valueList) {
            if (!e.isEverything(visitor)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getCost() {
        int cost = left.getCost();
        for (Expression e : valueList) {
            cost += e.getCost();
        }
        return cost;
    }

    /**
     * Add an additional element if possible. Example: given two conditions
     * A IN(1, 2) OR A=3, the constant 3 is added: A IN(1, 2, 3).
     *
     * @param other the second condition
     * @return null if the condition was not added, or the new condition
     */
    Expression getAdditional(Comparison other) {
        Expression add = other.getIfEquals(left);
        if (add != null) {
            valueList.add(add);
            return this;
        }
        return null;
    }
}
