/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import org.h2.command.Parser;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.TypeInfo;
import org.h2.value.Value;

/**
 * A user-defined variable, for example: @ID.
 */
public class Variable extends Expression {
    // 对于如下语句:
    // SET @topVariableName=3
    // select @topVariableName
    // select @nullVariableName //不存在的变量名，此时值为ValueNull.INSTANCE
    // 字段name和lastValue分别是
    // topVariableName, 3
    // nullVariableName, ValueNull.INSTANCE
    private final String name;
    private Value lastValue;
    
    //只看到在org.h2.command.Parser.readTerm()中使用
    public Variable(Session session, String name) {
        this.name = name;
        lastValue = session.getVariable(name);
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder, boolean alwaysQuote) {
        builder.append('@');
        // 调用这个方法而不是直接调用StringUtils.quoteIdentifier性能更好，因为大多数情况就是一个普通的标识符，没有什么特殊的，
        // 这时就不必要再重新构造一个加引号的字符串
        return Parser.quoteIdentifier(builder, name, alwaysQuote);
    }

    @Override
    public TypeInfo getType() {
        return lastValue.getType();
    }

    @Override
    public Value getValue(Session session) {
        lastValue = session.getVariable(name);
        return lastValue;
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        switch (visitor.getType()) {
        case ExpressionVisitor.EVALUATABLE:
            // the value will be evaluated at execute time
        case ExpressionVisitor.SET_MAX_DATA_MODIFICATION_ID:
            // it is checked independently if the value is the same as the last
            // time
        case ExpressionVisitor.OPTIMIZABLE_AGGREGATE:
        case ExpressionVisitor.READONLY:
        case ExpressionVisitor.INDEPENDENT:
        case ExpressionVisitor.NOT_FROM_RESOLVER:
        case ExpressionVisitor.QUERY_COMPARABLE:
        case ExpressionVisitor.GET_DEPENDENCIES:
        case ExpressionVisitor.GET_COLUMNS1:
        case ExpressionVisitor.GET_COLUMNS2:
            return true;
        case ExpressionVisitor.DETERMINISTIC:
        	//因为变量本身就是可变的，所以并不确定，
        	//当要判断当前表达式(即变量)是否满足ExpressionVisitor.DETERMINISTIC(确定性)时显然返回false
            return false;
        default:
            throw DbException.throwInternalError("type="+visitor.getType());
        }
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level, int state) {
        // nothing to do
    }

    @Override
    public Expression optimize(Session session) {
        return this;
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean value) {
        // nothing to do
    }

    @Override
    public void updateAggregate(Session session, int stage) {
        // nothing to do
    }

    public String getName() {
        return name;
    }

}
