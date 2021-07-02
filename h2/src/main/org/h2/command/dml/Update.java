/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.util.HashSet;

import org.h2.api.Trigger;
import org.h2.command.CommandInterface;
import org.h2.command.Prepared;
import org.h2.command.query.AllColumnsForPlan;
import org.h2.engine.DbObject;
import org.h2.engine.Right;
import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultTarget;
import org.h2.result.Row;
import org.h2.table.DataChangeDeltaTable.ResultOption;
import org.h2.table.PlanItem;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueNull;

/**
 * This class represents the statement
 * UPDATE
 */
public final class Update extends FilteredDataChangeStatement {

    private SetClauseList setClauseList;

    private Insert onDuplicateKeyInsert;

    private TableFilter fromTableFilter;

    public Update(SessionLocal session) {
        super(session);
    }

    public void setSetClauseList(SetClauseList setClauseList) {
        this.setClauseList = setClauseList;
    }

    public void setFromTableFilter(TableFilter tableFilter) {
        this.fromTableFilter = tableFilter;
    }

    @Override
    public long update(ResultTarget deltaChangeCollector, ResultOption deltaChangeCollectionMode) {
        targetTableFilter.startQuery(session);
        targetTableFilter.reset();
        Table table = targetTableFilter.getTable();
        try (LocalResult rows = LocalResult.forTable(session, table)) {
            session.getUser().checkTableRight(table, Right.UPDATE);
            table.fire(session, Trigger.UPDATE, true); //调用针对整个update动作的触发器
            //直到事务commit或rollback时才解琐，见org.h2.engine.Session.unlockAll()
            table.lock(session, true, false);
            // get the old rows, compute the new rows
            setCurrentRowNumber(0);
            long count = 0;
            long limitRows = -1;
            if (fetchExpr != null) {
                Value v = fetchExpr.getValue(session);
                if (v == ValueNull.INSTANCE || (limitRows = v.getLong()) < 0) {
                    throw DbException.getInvalidValueException("FETCH", v);
                }
            }
            //第一步先按where条件(如果有的话)取出所有满足条件的所有记录，如果没有where条件就是取全部记录，这些记录不超过limitRows
            while (nextRow(limitRows, count)) {
                Row oldRow = targetTableFilter.get();
                if (table.isRowLockable()) {
                    Row lockedRow = table.lockRow(session, oldRow);
                    if (lockedRow == null) {
                        continue;
                    }
                    if (!oldRow.hasSharedData(lockedRow)) {
                        oldRow = lockedRow;
                        targetTableFilter.set(oldRow);
                        if (condition != null && !condition.getBooleanValue(session)) { 
                            continue;
                        }
                    }
                }
                if (setClauseList.prepareUpdate(table, session, deltaChangeCollector, deltaChangeCollectionMode,
                        rows, oldRow, onDuplicateKeyInsert != null)) {
                    count++;
                }
            }
            doUpdate(this, session, table, rows);
            table.fire(session, Trigger.UPDATE, false);
            return count;
        }
    }

    static void doUpdate(Prepared prepared, SessionLocal session, Table table, LocalResult rows) {
        rows.done();
        // TODO self referencing referential integrity constraints
        // don't work if update is multi-row and 'inversed' the condition!
        // probably need multi-row triggers with 'deleted' and 'inserted'
        // at the same time. anyway good for sql compatibility
        // TODO update in-place (but if the key changes,
        // we need to update all indexes) before row triggers

        // the cached row is already updated - we need the old values
        table.updateRows(prepared, session, rows); //先删除记录，再增加记录
        if (table.fireRow()) {
            for (rows.reset(); rows.next();) {
                Row o = rows.currentRowForTable();
                rows.next();
                Row n = rows.currentRowForTable();
                table.fireAfterRow(session, o, n, false);
            }
        }
    }

    @Override
    public String getPlanSQL(int sqlFlags) {
        StringBuilder builder = new StringBuilder("UPDATE ");
        targetTableFilter.getPlanSQL(builder, false, sqlFlags);
        if (fromTableFilter != null) {
            builder.append("\nFROM ");
            fromTableFilter.getPlanSQL(builder, false, sqlFlags);
        }
        setClauseList.getSQL(builder, sqlFlags);
        appendFilterCondition(builder, sqlFlags);
        return builder.toString();
    }

    @Override
    public void prepare() { //跟org.h2.command.dml.Delete.prepare()一样，只是多了中间的for
        if (fromTableFilter != null) {
            targetTableFilter.addJoin(fromTableFilter, false, null);
        }
        if (condition != null) {
            condition.mapColumns(targetTableFilter, 0, Expression.MAP_INITIAL);
            if (fromTableFilter != null) {
                condition.mapColumns(fromTableFilter, 0, Expression.MAP_INITIAL);
            }
            condition = condition.optimizeCondition(session);
            if (condition != null) {
                //根据where条件建立相关的索引条件，这样可以由where条件中的字段选择合适的索引
                //如为字段name建立了索引，如果是where name>'124'，那么此时就用name的索引。
                condition.createIndexConditions(session, targetTableFilter);
            }
        }
        setClauseList.mapAndOptimize(session, targetTableFilter, fromTableFilter);
        TableFilter[] filters = null;
        if (fromTableFilter == null) {
            filters = new TableFilter[] { targetTableFilter };
        } else {
            filters = new TableFilter[] { targetTableFilter, fromTableFilter };
        }
        PlanItem item = targetTableFilter.getBestPlanItem(session, filters, 0, new AllColumnsForPlan(filters));
        targetTableFilter.setPlanItem(item);
        targetTableFilter.prepare();
    }

    @Override
    public int getType() {
        return CommandInterface.UPDATE;
    }

    @Override
    public String getStatementName() {
        return "UPDATE";
    }

    @Override
    public void collectDependencies(HashSet<DbObject> dependencies) {
        ExpressionVisitor visitor = ExpressionVisitor.getDependenciesVisitor(dependencies);
        if (condition != null) {
            condition.isEverything(visitor);
        }
        setClauseList.isEverything(visitor);
    }

    public Insert getOnDuplicateKeyInsert() {
        return onDuplicateKeyInsert;
    }

    void setOnDuplicateKeyInsert(Insert onDuplicateKeyInsert) {
        this.onDuplicateKeyInsert = onDuplicateKeyInsert;
    }

}
