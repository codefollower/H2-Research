/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.constraint;

import java.util.HashSet;
import org.h2.api.ErrorCode;
import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.StringUtils;
import org.h2.value.Value;

/**
 * A check constraint.
 */
public class ConstraintCheck extends Constraint {

    private TableFilter filter;
    private Expression expr;

    public ConstraintCheck(Schema schema, int id, String name, Table table) {
        super(schema, id, name, table);
    }

    @Override
    public Type getConstraintType() {
        return Constraint.Type.CHECK;
    }

    public void setTableFilter(TableFilter filter) {
        this.filter = filter;
    }

    public void setExpression(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String getCreateSQLForCopy(Table forTable, String quotedName) {
        StringBuilder buff = new StringBuilder("ALTER TABLE ");
        forTable.getSQL(buff, DEFAULT_SQL_FLAGS).append(" ADD CONSTRAINT ");
        if (forTable.isHidden()) {
            buff.append("IF NOT EXISTS ");
        }
        buff.append(quotedName);
        if (comment != null) {
            buff.append(" COMMENT ");
            StringUtils.quoteStringSQL(buff, comment);
        }
        buff.append(" CHECK");
        expr.getEnclosedSQL(buff, DEFAULT_SQL_FLAGS).append(" NOCHECK");
        return buff.toString();
    }

    private String getShortDescription() {
        StringBuilder builder = new StringBuilder().append(getName()).append(": ");
        expr.getTraceSQL();
        return builder.toString();
    }

    @Override
    public String  getCreateSQLWithoutIndexes() {
        return getCreateSQL();
    }

    @Override
    public String getCreateSQL() {
        return getCreateSQLForCopy(table, getSQL(DEFAULT_SQL_FLAGS));
    }

    @Override
    public void removeChildrenAndResources(SessionLocal session) {
        table.removeConstraint(this);
        database.removeMeta(session, getId());
        filter = null;
        expr = null;
        table = null;
        invalidate();
    }
    
    //只用于insert和update
    @Override
    public void checkRow(SessionLocal session, Table t, Row oldRow, Row newRow) {
        if (newRow == null) {
            return;
        }
        boolean b;
        try {
            Value v;
            synchronized (this) {
                filter.set(newRow); //为了在expr.getValue能取到当前newRow
                v = expr.getValue(session);
            }
            // Both TRUE and NULL are ok
            b = v.isFalse();
        } catch (DbException ex) {
            throw DbException.get(ErrorCode.CHECK_CONSTRAINT_INVALID, ex, getShortDescription());
        }
        if (b) {
            throw DbException.get(ErrorCode.CHECK_CONSTRAINT_VIOLATED_1, getShortDescription());
        }
    }

    @Override
    public boolean usesIndex(Index index) {
        return false;
    }

    @Override
    public void setIndexOwner(Index index) {
        throw DbException.getInternalError(toString());
    }

    @Override
    public HashSet<Column> getReferencedColumns(Table table) {
        HashSet<Column> columns = new HashSet<>();
        expr.isEverything(ExpressionVisitor.getColumnsVisitor(columns, table));
        return columns;
    }

    @Override
    public Expression getExpression() {
        return expr;
    }

    @Override
    public boolean isBefore() {
        return true;
    }

    //通常是在构建约束对象之后马上根据CHECK和NOCHECK调用与不调用
    @Override
    public void checkExistingData(SessionLocal session) { //比如用于alter时
        if (session.getDatabase().isStarting()) {
            // don't check at startup
            return;
        }
        //用NOT，意思就是说只要找到一个反例就与约束冲突了
        //比如，如果是CHECK f1 not null，
        //如果此时表中的f1字段存在null值，那么这个约束就创建失败
        StringBuilder builder = new StringBuilder().append("SELECT NULL FROM ");
        filter.getTable().getSQL(builder, DEFAULT_SQL_FLAGS).append(" WHERE NOT ");
        expr.getSQL(builder, DEFAULT_SQL_FLAGS, Expression.AUTO_PARENTHESES);
        String sql = builder.toString();
        ResultInterface r = session.prepare(sql).query(1);
        if (r.next()) {
            throw DbException.get(ErrorCode.CHECK_CONSTRAINT_VIOLATED_1, getName());
        }
    }

    @Override
    public void rebuild() {
        // nothing to do
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        return expr.isEverything(visitor);
    }

}
