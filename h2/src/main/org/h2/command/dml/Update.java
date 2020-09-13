/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
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
import org.h2.result.ResultInterface;
import org.h2.result.ResultTarget;
import org.h2.result.Row;
import org.h2.result.RowList;
import org.h2.table.PlanItem;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.table.DataChangeDeltaTable.ResultOption;
import org.h2.value.Value;
import org.h2.value.ValueNull;

/**
 * This class represents the statement
 * UPDATE
 */
public final class Update extends DataChangeStatement {

    private Expression condition;
    private TableFilter targetTableFilter;// target of update

    /** The limit expression as specified in the LIMIT clause. */
    private Expression limitExpr;

    private SetClauseList setClauseList;

    private Insert onDuplicateKeyInsert;

    public Update(SessionLocal session) {
        super(session);
    }

    @Override
    public Table getTable() {
        return targetTableFilter.getTable();
    }

    public void setTableFilter(TableFilter tableFilter) {
        this.targetTableFilter = tableFilter;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Expression getCondition( ) {
        return this.condition;
    }

    public void setSetClauseList(SetClauseList setClauseList) {
        this.setClauseList = setClauseList;
    }

    @Override
    public long update(ResultTarget deltaChangeCollector, ResultOption deltaChangeCollectionMode) {
        targetTableFilter.startQuery(session);
        targetTableFilter.reset();
        Table table = targetTableFilter.getTable();
        try (RowList rows = new RowList(session, table)) {
            session.getUser().checkRight(table, Right.UPDATE);
            //调用针对整个update动作的触发器
            table.fire(session, Trigger.UPDATE, true);
            //直到事务commit或rollback时才解琐，见org.h2.engine.Session.unlockAll()
            table.lock(session, true, false);
            // get the old rows, compute the new rows
            setCurrentRowNumber(0);
            long count = 0;
            int limitRows = -1;
            if (limitExpr != null) {
                Value v = limitExpr.getValue(session);
                if (v != ValueNull.INSTANCE) {
                    limitRows = v.getInt();
                }
            }
            //第一步先按where条件(如果有的话)取出所有满足条件的所有记录，如果没有where条件就是取全部记录，这些记录不超过limitRows
            while (targetTableFilter.next()) {
                setCurrentRowNumber(count+1);
                if (limitRows >= 0 && count >= limitRows) {
                    break;
                }
                if (condition == null || condition.getBooleanValue(session)) {
                    Row oldRow = targetTableFilter.get();
                    if (table.isMVStore()) {
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
//<<<<<<< HEAD
//                    Row newRow = table.getTemplateRow();
//                    //以原表中的所有字段来遍历，而不是以update中的字段
//                    boolean setOnUpdate = false;
//                    for (int i = 0; i < columnCount; i++) {
//                        Column column = columns[i];
//                        Expression newExpr = setClauseMap.get(column);
//                        Value newValue;
//                        if (newExpr == null) { //说明不是更新字段，直接用原来的值
//                            if (column.getOnUpdateExpression() != null) {
//                                setOnUpdate = true;
//                            }
////<<<<<<< HEAD
////                            newValue = oldRow.getValue(i);
////                        } else if (newExpr == ValueExpression.getDefault()) { //是更新字段，但是取默认值
////                            newValue = table.getDefaultValue(session, column);
////                        } else { //是更新字段，并且取更新值
////=======
//                            newValue = column.getGenerated() ? null : oldRow.getValue(i);
//                        } else if (newExpr == ValueExpression.DEFAULT) {
//                            newValue = null;
//                        } else {
//                            newValue = newExpr.getValue(session);
//                        }
//                        newRow.setValue(i, newValue);
//                    }
//                    long key = oldRow.getKey();
//                    newRow.setKey(key);
//                    table.validateConvertUpdateSequence(session, newRow); //验证新记录(包括字段约束检查)
//                    if (setOnUpdate || updateToCurrentValuesReturnsZero) {
//                        setOnUpdate = false;
//                        for (int i = 0; i < columnCount; i++) {
//                            // Use equals here to detect changes from numeric 0 to 0.0 and similar
//                            if (!Objects.equals(oldRow.getValue(i), newRow.getValue(i))) {
//                                setOnUpdate = true;
//                                break;
//                            }
//                        }
//                        if (setOnUpdate) {
//                            for (int i = 0; i < columnCount; i++) {
//                                Column column = columns[i];
//                                if (setClauseMap.get(column) == null) {
//                                    Expression onUpdate = column.getOnUpdateExpression();
//                                    if (onUpdate != null) {
//                                        newRow.setValue(i, onUpdate.getValue(session));
//                                    }
//                                }
//                            }
//                            // Convert on update expressions and reevaluate
//                            // generated columns
//                            table.validateConvertUpdateSequence(session, newRow);
//                        } else if (updateToCurrentValuesReturnsZero) {
//                            count--;
//                        }
//                    }
//                    if (deltaChangeCollectionMode == ResultOption.OLD) {
//                        deltaChangeCollector.addRow(oldRow.getValueList());
//                    } else if (deltaChangeCollectionMode == ResultOption.NEW) {
//                        deltaChangeCollector.addRow(newRow.getValueList().clone());
//                    }
//                    if (!table.fireRow() || !table.fireBeforeRow(session, oldRow, newRow)) { //调用针对行级别的触发器 
//                        rows.add(oldRow);
//                        rows.add(newRow);
//                        if (updatedKeysCollector != null) {
//                            updatedKeysCollector.add(key);
//                        }
//                    }
//                    if (deltaChangeCollectionMode == ResultOption.FINAL) {
//                        deltaChangeCollector.addRow(newRow.getValueList());
//                    }
//                    count++;
//                }
//            }
//            // TODO self referencing referential integrity constraints
//            // don't work if update is multi-row and 'inversed' the condition!
//            // probably need multi-row triggers with 'deleted' and 'inserted'
//            // at the same time. anyway good for sql compatibility
//            // TODO update in-place (but if the key changes,
//            // we need to update all indexes) before row triggers
//
//            // the cached row is already updated - we need the old values
//            //第二步更新记录(先删除记录，再增加记录)
//            table.updateRows(this, session, rows);
//            if (table.fireRow()) {
//                for (rows.reset(); rows.hasNext();) {
//                    Row o = rows.next();
//                    Row n = rows.next();
//                    table.fireAfterRow(session, o, n, false);
//=======
                    if (setClauseList.prepareUpdate(table, session, deltaChangeCollector, deltaChangeCollectionMode,
                            rows, oldRow, onDuplicateKeyInsert != null)) {
                        count++;
                    }
                }
            }
            doUpdate(this, session, table, rows);
            table.fire(session, Trigger.UPDATE, false);
            return count;
        }
    }

    static void doUpdate(Prepared prepared, SessionLocal session, Table table, RowList rows) {
        // TODO self referencing referential integrity constraints
        // don't work if update is multi-row and 'inversed' the condition!
        // probably need multi-row triggers with 'deleted' and 'inserted'
        // at the same time. anyway good for sql compatibility
        // TODO update in-place (but if the key changes,
        // we need to update all indexes) before row triggers

        // the cached row is already updated - we need the old values
        table.updateRows(prepared, session, rows);
        if (table.fireRow()) {
            for (rows.reset(); rows.hasNext();) {
                Row o = rows.next();
                Row n = rows.next();
                table.fireAfterRow(session, o, n, false);
            }
        }
    }

    @Override
    public String getPlanSQL(int sqlFlags) {
        StringBuilder builder = new StringBuilder("UPDATE ");
        targetTableFilter.getPlanSQL(builder, false, sqlFlags);
        setClauseList.getSQL(builder, sqlFlags);
        if (condition != null) {
            builder.append("\nWHERE ");
            condition.getUnenclosedSQL(builder, sqlFlags);
        }

        if (limitExpr != null) {
            builder.append("\nLIMIT ");
            limitExpr.getUnenclosedSQL(builder, sqlFlags);
        }
        return builder.toString();
    }

    @Override
    public void prepare() { //跟org.h2.command.dml.Delete.prepare()一样，只是多了中间的for
        if (condition != null) {
            condition.mapColumns(targetTableFilter, 0, Expression.MAP_INITIAL);
//<<<<<<< HEAD
//            condition = condition.optimize(session);
//            //根据where条件建立相关的索引条件，这样可以由where条件中的字段选择合适的索引
//            //如为字段name建立了索引，如果是where name>'124'，那么此时就用name的索引。
//            condition.createIndexConditions(session, targetTableFilter);
//        }
//        for (Entry<Column, Expression> entry : setClauseMap.entrySet()) {
//            Expression e = entry.getValue();
//            e.mapColumns(targetTableFilter, 0, Expression.MAP_INITIAL);
//            if (sourceTableFilter!=null){
//                e.mapColumns(sourceTableFilter, 0, Expression.MAP_INITIAL);
//=======
            condition = condition.optimizeCondition(session);
            if (condition != null) {
                condition.createIndexConditions(session, targetTableFilter);
            }
        }
        setClauseList.mapAndOptimize(session, targetTableFilter, null);
        TableFilter[] filters = new TableFilter[] { targetTableFilter };
        PlanItem item = targetTableFilter.getBestPlanItem(session, filters, 0, new AllColumnsForPlan(filters));
        targetTableFilter.setPlanItem(item);
        targetTableFilter.prepare();
    }

    @Override
    public boolean isTransactional() {
        return true;
    }

    @Override
    public ResultInterface queryMeta() {
        return null;
    }

    @Override
    public int getType() {
        return CommandInterface.UPDATE;
    }

    @Override
    public String getStatementName() {
        return "UPDATE";
    }

    public void setLimit(Expression limit) {
        this.limitExpr = limit;
    }

    @Override
    public boolean isCacheable() {
        return true;
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
