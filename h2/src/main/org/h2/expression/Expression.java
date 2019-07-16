/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.util.List;

import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.result.ResultInterface;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueCollectionBase;

/**
 * An expression is a operation, a value, or a function in a query.
 */
//12个抽象方法
public abstract class Expression {

    /**
     * Initial state for {@link #mapColumns(ColumnResolver, int, int)}.
     */
    public static final int MAP_INITIAL = 0;

    /**
     * State for expressions inside a window function for
     * {@link #mapColumns(ColumnResolver, int, int)}.
     */
    public static final int MAP_IN_WINDOW = 1;

    /**
     * State for expressions inside an aggregate for
     * {@link #mapColumns(ColumnResolver, int, int)}.
     */
    public static final int MAP_IN_AGGREGATE = 2;

    private boolean addedToFilter;

    /**
     * Get the SQL snippet for a list of expressions.
     *
     * @param builder the builder to append the SQL to
     * @param expressions the list of expressions
     * @param alwaysQuote quote all identifiers
     */
    public static void writeExpressions(StringBuilder builder, List<? extends Expression> expressions,
            boolean alwaysQuote) {
        for (int i = 0, length = expressions.size(); i < length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            expressions.get(i).getSQL(builder, alwaysQuote);
        }
    }

    /**
     * Get the SQL snippet for an array of expressions.
     *
     * @param builder the builder to append the SQL to
     * @param expressions the list of expressions
     * @param alwaysQuote quote all identifiers
     */
    public static void writeExpressions(StringBuilder builder, Expression[] expressions, boolean alwaysQuote) {
        for (int i = 0, length = expressions.length; i < length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Expression e = expressions[i];
            if (e == null) {
                builder.append("DEFAULT");
            } else {
                e.getSQL(builder, alwaysQuote);
            }
        }
    }

    /**
     * Return the resulting value for the current row.
     *
     * @param session the session
     * @return the result
     */
    public abstract Value getValue(Session session);

    /**
     * Returns the data type. The data type may not be known before the
     * optimization phase.
     *
     * @return the data type
     */
    public abstract TypeInfo getType();

    /**
     * Map the columns of the resolver to expression columns.
     *
     * @param resolver the column resolver
     * @param level the subquery nesting level
     * @param state current state for nesting checks, initial value is
     *              {@link #MAP_INITIAL}
     */
    //只有ConditionExists、ConditionInSelect、Subquery这三个子类中对level加1
    public abstract void mapColumns(ColumnResolver resolver, int level, int state);

    /**
     * Try to optimize the expression.
     *
     * @param session the session
     * @return the optimized expression
     */
    public abstract Expression optimize(Session session);

    /**
     * Tell the expression columns whether the table filter can return values
     * now. This is used when optimizing the query.
     *
     * @param tableFilter the table filter
     * @param value true if the table filter can return value
     */
    //这个方法的目的就是给org.h2.expression.ExpressionColumn中的evaluatable字段赋值
    //除ExpressionColumn外其他子类的实现都是充当一个传递者的角色
    public abstract void setEvaluatable(TableFilter tableFilter, boolean value);

    /**
     * Get the SQL statement of this expression.
     * This may not always be the original SQL statement,
     * specially after optimization.
     *
     * @param alwaysQuote quote all identifiers
     * @return the SQL statement
     */
    public String getSQL(boolean alwaysQuote) {
        return getSQL(new StringBuilder(), alwaysQuote).toString();
    }

    /**
     * Appends the SQL statement of this expression to the specified builder.
     * This may not always be the original SQL statement, specially after
     * optimization.
     *
     * @param builder
     *            string builder
     * @param alwaysQuote quote all identifiers
     * @return the specified string builder
     */
    public abstract StringBuilder getSQL(StringBuilder builder, boolean alwaysQuote);

    /**
     * Appends the SQL statement of this expression to the specified builder.
     * This may not always be the original SQL statement, specially after
     * optimization. Enclosing '(' and ')' are removed.
     *
     * @param builder
     *            string builder
     * @param alwaysQuote
     *            quote all identifiers
     * @return the specified string builder
     */
    public StringBuilder getUnenclosedSQL(StringBuilder builder, boolean alwaysQuote) {
        int first = builder.length();
        int last = getSQL(builder, alwaysQuote).length() - 1;
        if (last > first && builder.charAt(first) == '(' && builder.charAt(last) == ')') {
            builder.setLength(last);
            builder.deleteCharAt(first);
        }
        return builder;
    }

    /**
     * Update an aggregate value. This method is called at statement execution
     * time. It is usually called once for each row, but if the expression is
     * used multiple times (for example in the column list, and as part of the
     * HAVING expression) it is called multiple times - the row counter needs to
     * be used to make sure the internal state is only updated once.
     *
     * @param session the session
     * @param stage select stage
     */
    //用于聚合、分组中
    //在org.h2.command.dml.Select.queryGroup(int, LocalResult)
    //和org.h2.command.dml.Select.queryGroupSorted(int, ResultTarget)有用到
    //在遍历记录的过程中执行，在getValue之前调用
    //对Aggregate、JavaAggregate、ExpressionColumn比较有用
    public abstract void updateAggregate(Session session, int stage);

    /**
     * Check if this expression and all sub-expressions can fulfill a criteria.
     * If any part returns false, the result is false.
     *
     * @param visitor the visitor
     * @return if the criteria can be fulfilled
     */
    public abstract boolean isEverything(ExpressionVisitor visitor);

    /**
     * Estimate the cost to process the expression.
     * Used when optimizing the query, to calculate the query plan
     * with the lowest estimated cost.
     *
     * @return the estimated cost
     */
    public abstract int getCost();

    /**
     * If it is possible, return the negated expression. This is used
     * to optimize NOT expressions: NOT ID>10 can be converted to
     * ID&lt;=10. Returns null if negating is not possible.
     *
     * @param session the session
     * @return the negated expression, or null
     */
    public Expression getNotIfPossible(Session session) {
        // by default it is not possible
        return null;
    }

    /**
     * Check if this expression will always return the same value.
     *
     * @return if the expression is constant
     */
    public boolean isConstant() { //只有ValueExpression、Parameter覆盖了此方法
        return false;
    }

    /**
     * Check if this expression will always return the NULL value.
     *
     * @return if the expression is constant NULL value
     */
    public boolean isNullConstant() {
        return false;
    }

    /**
     * Is the value of a parameter set.
     *
     * @return true if set
     */
    public boolean isValueSet() { //只有ValueExpression、Parameter覆盖了此方法
        return false;
    }

    /**
     * Check if this is an auto-increment column.
     *
     * @return true if it is an auto-increment column
     */
    public boolean isAutoIncrement() { //只有子类Alias和ExpressionColumn覆盖了此方法
        return false;
    }

    /**
     * Get the value in form of a boolean expression.
     * Returns true or false.
     * In this database, everything can be a condition.
     *
     * @param session the session
     * @return the result
     */
    public boolean getBooleanValue(Session session) { //没有字类覆盖
        return getValue(session).getBoolean();
    }

    /**
     * Create index conditions if possible and attach them to the table filter.
     *
     * @param session the session
     * @param filter the table filter
     */
    //这8种类型的表达式能建立索引条件
    //CompareLike、Comparison、
    //ConditionAndOr、ConditionIn、ConditionInConstantSet、ConditionInSelect、
    //ExpressionColumn、ValueExpression
    public void createIndexConditions(Session session, TableFilter filter) {
        // default is do nothing
    }

    /**
     * Get the column name or alias name of this expression.
     *
     * @return the column name
     */
    public String getColumnName() { //只有子类Alias和ExpressionColumn覆盖了此方法
        return getAlias();
    }

    /**
     * Get the schema name, or null
     *
     * @return the schema name
     */
    public String getSchemaName() { //只有子类Wildcard和ExpressionColumn覆盖了此方法
        return null;
    }

    /**
     * Get the table name, or null
     *
     * @return the table name
     */
    public String getTableName() { //只有子类Alias和ExpressionColumn覆盖了此方法
        return null;
    }

    /**
     * Check whether this expression is a column and can store NULL.
     *
     * @return whether NULL is allowed
     */
    public int getNullable() { //只有子类Alias和ExpressionColumn覆盖了此方法
        return Column.NULLABLE_UNKNOWN;
    }

    /**
     * Get the table alias name or null
     * if this expression does not represent a column.
     *
     * @return the table alias name
     */
    public String getTableAlias() { //只有子类Wildcard覆盖了此方法
        return null;
    }

    /**
     * Get the alias name of a column or SQL expression
     * if it is not an aliased expression.
     *
     * @return the alias name
     */

//    /**
//     * Only returns true if the expression is a wildcard.
//     *
//     * @return if this expression is a wildcard
//     */
//    public boolean isWildcard() { //只有子类org.h2.expression.Wildcard覆盖了此方法并且返回为true
//        return false;

    public String getAlias() { //只有子类Alias和ExpressionColumn覆盖了此方法
        return getUnenclosedSQL(new StringBuilder(), false).toString();
    }

    /**
     * Returns the main expression, skipping aliases.
     *
     * @return the expression
     */
    public Expression getNonAliasExpression() { //只有子类org.h2.expression.Alias覆盖了此方法
        return this;
    }

    /**
     * Add conditions to a table filter if they can be evaluated.
     *
     * @param filter the table filter
     */
    public void addFilterConditions(TableFilter filter) {
        if (!addedToFilter && isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)) {
            filter.addFilterCondition(this, false);
            addedToFilter = true;
        }
    }

    /**
     * Convert this expression to a String.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return getSQL(false);
    }

    /**
     * If this expression consists of column expressions it should return them.
     *
     * @param session the session
     * @return array of expression columns if applicable, null otherwise
     */
    public Expression[] getExpressionColumns(Session session) {
        return null;
    }

    /**
     * Extracts expression columns from ValueArray
     *
     * @param session the current session
     * @param value the value to extract columns from
     * @return array of expression columns
     */
    protected static Expression[] getExpressionColumns(Session session, ValueCollectionBase value) {
        Value[] list = value.getList();
        ExpressionColumn[] expr = new ExpressionColumn[list.length];
        for (int i = 0, len = list.length; i < len; i++) {
            Value v = list[i];
            Column col = new Column("C" + (i + 1), v.getType());
            expr[i] = new ExpressionColumn(session.getDatabase(), col);
        }
        return expr;
    }

    /**
     * Extracts expression columns from the given result set.
     *
     * @param session the session
     * @param result the result
     * @return an array of expression columns
     */
    public static Expression[] getExpressionColumns(Session session, ResultInterface result) {
        int columnCount = result.getVisibleColumnCount();
        Expression[] expressions = new Expression[columnCount];
        Database db = session == null ? null : session.getDatabase();
        for (int i = 0; i < columnCount; i++) {
            String name = result.getColumnName(i);
            TypeInfo type = result.getColumnType(i);
            Column col = new Column(name, type);
            Expression expr = new ExpressionColumn(db, col);
            expressions[i] = expr;
        }
        return expressions;
    }

    /**
     * Returns count of subexpressions.
     *
     * @return count of subexpressions
     */
    public int getSubexpressionCount() {
        return 0;
    }

    /**
     * Returns subexpression with specified index.
     *
     * @param index 0-based index
     * @return subexpression with specified index, may be null
     * @throws IndexOutOfBoundsException if specified index is not valid
     */
    public Expression getSubexpression(int index) {
        throw new IndexOutOfBoundsException();
    }

}
