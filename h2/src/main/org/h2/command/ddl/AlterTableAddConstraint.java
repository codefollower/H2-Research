/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import java.util.ArrayList;

import org.h2.api.ErrorCode;
import org.h2.command.CommandInterface;
import org.h2.constraint.Constraint;
import org.h2.constraint.ConstraintActionType;
import org.h2.constraint.ConstraintCheck;
import org.h2.constraint.ConstraintReferential;
import org.h2.constraint.ConstraintUnique;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Right;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.DataType;

/**
 * This class represents the statement
 * ALTER TABLE ADD CONSTRAINT
 */
//AlterTableAlterColumn只有执行alter命令时会产生此类的实例，
//而AlterTableAddConstraint实例在alter和create table命令中都会产生
public class AlterTableAddConstraint extends SchemaCommand {

    private final int type;
    private String constraintName;
    private String tableName;
    private IndexColumn[] indexColumns;
    private ConstraintActionType deleteAction = ConstraintActionType.RESTRICT;
    private ConstraintActionType updateAction = ConstraintActionType.RESTRICT;
    private Schema refSchema;
    private String refTableName;
    private IndexColumn[] refIndexColumns;
    private Expression checkExpression;
    private Index index, refIndex;
    private String comment;
    private boolean checkExisting;
    private boolean primaryKeyHash;
    private boolean ifTableExists;
    private final boolean ifNotExists;
    private final ArrayList<Index> createdIndexes = new ArrayList<>();
    private ConstraintUnique createdUniqueConstraint;

    public AlterTableAddConstraint(Session session, Schema schema, int type, boolean ifNotExists) {
        super(session, schema);
        this.ifNotExists = ifNotExists;
        this.type = type;
    }

    public void setIfTableExists(boolean b) {
        ifTableExists = b;
    }

    private String generateConstraintName(Table table) {
        if (constraintName == null) {
            constraintName = getSchema().getUniqueConstraintName(session, table);
        }
        return constraintName;
    }

    @Override
    public int update() {
        try {
            return tryUpdate();
        } catch (DbException e) {
            try {
                if (createdUniqueConstraint != null) {
                    Index index = createdUniqueConstraint.getIndex();
                    session.getDatabase().removeSchemaObject(session, createdUniqueConstraint);
                    createdIndexes.remove(index);
                }
                for (Index index : createdIndexes) {
                    session.getDatabase().removeSchemaObject(session, index);
                }
            } catch (Throwable ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            getSchema().freeUniqueName(constraintName);
        }
    }

    /**
     * Try to execute the statement.
     *
     * @return the update count
     */
    private int tryUpdate() {
        if (!transactional) { //在org.h2.command.ddl.CreateTable.update()中设置transactional
            session.commit(true); //如果是非事务的，那么就得自动提交
        }
        Database db = session.getDatabase();
        Table table = getSchema().findTableOrView(session, tableName);
        if (table == null) {
            if (ifTableExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1, tableName);
        }
        if (constraintName != null && getSchema().findConstraint(session, constraintName) != null) {
            if (ifNotExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.CONSTRAINT_ALREADY_EXISTS_1,
                    constraintName);
        }
        session.getUser().checkRight(table, Right.ALL);
        db.lockMeta(session);
        table.lock(session, true, true);
        Constraint constraint;
        //四种类型:PRIMARY_KEY、UNIQUE、CHECK、REFERENTIAL
        //其中PRIMARY_KEY、UNIQUE共用一个ConstraintUnique类
        switch (type) {
        case CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY: {
            IndexColumn.mapColumns(indexColumns, table);
            //虽然像这样ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 PRIMARY KEY HASH(f1,f2) INDEX myindex
            //指定了myindex做为索引，但是其实是无用的，虽然此时index不为null，
            //但是下面这行代码又把之前index变量的值覆盖了
            index = table.findPrimaryKey();
            ArrayList<Constraint> constraints = table.getConstraints();
            for (int i = 0; constraints != null && i < constraints.size(); i++) {
                Constraint c = constraints.get(i);
                if (Constraint.Type.PRIMARY_KEY == c.getConstraintType()) {
                    throw DbException.get(ErrorCode.SECOND_PRIMARY_KEY);
                }
            }
            if (index != null) {
                // if there is an index, it must match with the one declared
                // we don't test ascending / descending
                IndexColumn[] pkCols = index.getIndexColumns();
                if (pkCols.length != indexColumns.length) {
                    throw DbException.get(ErrorCode.SECOND_PRIMARY_KEY);
                }
                for (int i = 0; i < pkCols.length; i++) {
                    if (pkCols[i].column != indexColumns[i].column) {
                        throw DbException.get(ErrorCode.SECOND_PRIMARY_KEY);
                    }
                }
//<<<<<<< HEAD
//            }
//            if (index == null) {
//                IndexType indexType = IndexType.createPrimaryKey(table.isPersistIndexes(), primaryKeyHash);
//                String indexName = table.getSchema().getUniqueIndexName(session, table, Constants.PREFIX_PRIMARY_KEY);
//                int id = getObjectId(); //会得到新的对象id，作为索引id
//=======
            } else {
                IndexType indexType = IndexType.createPrimaryKey(
                        table.isPersistIndexes(), primaryKeyHash);
                String indexName = table.getSchema().getUniqueIndexName(
                        session, table, Constants.PREFIX_PRIMARY_KEY);
                int indexId = session.getDatabase().allocateObjectId(); 
                try {
                    index = table.addIndex(session, indexName, indexId,
                            indexColumns, indexType, true, null);
                } finally {
                    getSchema().freeUniqueName(indexName);
                }
            }
            index.getIndexType().setBelongsToConstraint(true);
            int id = getObjectId(); //又会得到另一个新的对象id，作为约束对象id
            String name = generateConstraintName(table);
            ConstraintUnique pk = new ConstraintUnique(getSchema(),
                    id, name, table, true);
            pk.setColumns(indexColumns);
            pk.setIndex(index, true);
            constraint = pk;
            break;
        }
        case CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_UNIQUE:
            IndexColumn.mapColumns(indexColumns, table);
//<<<<<<< HEAD
//            boolean isOwner = false; //使用已存在的索引并且不显示加INDEX子句时isOwner为false，其他情况下为true
//            
//            //如果没有为约束字段指定索引，那么会自动为这些约束字段创建索引，此索引归属于约束对象，并不属于表，但是也加到表中
//            //假设在f1和f2上建立了索引，并且是唯一索引，现在indexColumns列多加了一列f3，
//            //因为f1和f2能保证唯一了，所以直接用此索引当成当前唯一约束的索引即可
//            if (index != null && canUseUniqueIndex(index, table, indexColumns)) {
//                isOwner = true;
//                index.getIndexType().setBelongsToConstraint(true);
//            } else {
//            	//如果定义约束时没有指定index，但又能从当前表中得到一个满足indexColumns的唯一索引，
//            	//此时isOwner就是false了，也就是说返回的索引不归属约束，而是归属表
//                index = getUniqueIndex(table, indexColumns);
//                if (index == null) {
//                    index = createIndex(table, indexColumns, true); //用indexColumns创建一个唯一索引
//                    isOwner = true;
//                }
//            }
//            int id = getObjectId();
//            String name = generateConstraintName(table);
//            ConstraintUnique unique = new ConstraintUnique(getSchema(), id,
//                    name, table, false);
//            unique.setColumns(indexColumns);
//            unique.setIndex(index, isOwner);
//            constraint = unique;
//=======
            constraint = createUniqueConstraint(table, index, indexColumns, false);
            break;
        case CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_CHECK: {
            int id = getObjectId();
            String name = generateConstraintName(table);
            ConstraintCheck check = new ConstraintCheck(getSchema(), id, name, table);
            TableFilter filter = new TableFilter(session, table, null, false, null, 0, null);
            checkExpression.mapColumns(filter, 0, Expression.MAP_INITIAL);
            checkExpression = checkExpression.optimize(session);
            check.setExpression(checkExpression);
            check.setTableFilter(filter);
            constraint = check;
            if (checkExisting) {
                check.checkExistingData(session);
            }
            break;
        }
        case CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_REFERENTIAL: {
            Table refTable = refSchema.resolveTableOrView(session, refTableName);
            if (refTable == null) {
                throw DbException.get(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1, refTableName);
            }
            session.getUser().checkRight(refTable, Right.ALL);
            if (!refTable.canReference()) {
                StringBuilder builder = new StringBuilder("Reference ");
                refTable.getSQL(builder, false);
                throw DbException.getUnsupportedException(builder.toString());
            }
            boolean isOwner = false;
            IndexColumn.mapColumns(indexColumns, table);
            
            //只要index不是Scan索引(即getCreateSQL()不为null)，且index所在表就是table，且索引字段包含所有的indexColumns
            if (refIndexColumns == null) {
                refIndexColumns = refTable.getPrimaryKey().getIndexColumns();
            } else {
                IndexColumn.mapColumns(refIndexColumns, refTable);
            }
            int columnCount = indexColumns.length;
            if (refIndexColumns.length != columnCount) {
                throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH);
            }
            for (IndexColumn indexColumn : refIndexColumns) {
                Column column = indexColumn.column;
                if (column.getGenerated()) {
                    switch (deleteAction) {
                    case SET_DEFAULT:
                    case SET_NULL:
                        throw DbException.get(ErrorCode.GENERATED_COLUMN_CANNOT_BE_UPDATABLE_BY_CONSTRAINT_2,
                                column.getSQLWithTable(new StringBuilder(), false).toString(),
                                "ON DELETE " + deleteAction.getSqlName());
                    default:
                        // All other actions are allowed
                    }
                    switch (updateAction) {
                    case CASCADE:
                    case SET_DEFAULT:
                    case SET_NULL:
                        throw DbException.get(ErrorCode.GENERATED_COLUMN_CANNOT_BE_UPDATABLE_BY_CONSTRAINT_2,
                                column.getSQLWithTable(new StringBuilder(), false).toString(),
                                "ON UPDATE " + updateAction.getSqlName());
                    default:
                        // All other actions are allowed
                    }
                }
            }
            for (int i = 0; i < columnCount; i++) {
                Column column1 = indexColumns[i].column, column2 = refIndexColumns[i].column;
                if (!DataType.areStableComparable(column1.getType(), column2.getType())) {
                    throw DbException.get(ErrorCode.UNCOMPARABLE_REFERENCED_COLUMN_2, column1.getCreateSQL(),
                            column2.getCreateSQL());
                }
            }
            if (index != null && canUseIndex(index, table, indexColumns, false)) {
                isOwner = true;
                index.getIndexType().setBelongsToConstraint(true);
            } else {
                index = getIndex(table, indexColumns, false);
                if (index == null) {
                    index = createIndex(table, indexColumns, false);
                    isOwner = true;
                }
            }
//<<<<<<< HEAD
//            if (refIndexColumns == null) { //当主表存在主键时，引用表可以不指定主表的主键字段
//                Index refIdx = refTable.getPrimaryKey();
//                refIndexColumns = refIdx.getIndexColumns();
//            } else {
//                IndexColumn.mapColumns(refIndexColumns, refTable);
//            }
//            if (refIndexColumns.length != indexColumns.length) {
//                throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH);
//            }
//            boolean isRefOwner = false;
//            //refIndex.getTable() == refTable是多于的，canUseIndex已经判断了
//            if (refIndex != null && refIndex.getTable() == refTable &&
//                    canUseIndex(refIndex, refTable, refIndexColumns, false)) {
//                isRefOwner = true;
//                refIndex.getIndexType().setBelongsToConstraint(true);
//            } else {
//                refIndex = null;
//            }
//            if (refIndex == null) {
//                refIndex = getIndex(refTable, refIndexColumns, false);
//                if (refIndex == null) {
//                    refIndex = createIndex(refTable, refIndexColumns, true); //注意: 为引用字段建立了唯一索引
//                    isRefOwner = true;
//                }
//            }
//=======
//>>>>>>> c39744852e76bb33dd714d90c9bf0bbb9aab31f9
            int id = getObjectId();
            String name = generateConstraintName(table);
            ConstraintReferential refConstraint = new ConstraintReferential(getSchema(),
                    id, name, table);
            refConstraint.setColumns(indexColumns);
            refConstraint.setIndex(index, isOwner);
            refConstraint.setRefTable(refTable);
            refConstraint.setRefColumns(refIndexColumns);
            ConstraintUnique unique = getUniqueConstraint(refTable, refIndexColumns);
            if (unique == null) {
                unique = createUniqueConstraint(refTable, refIndex, refIndexColumns, true);
                addConstraintToTable(db, refTable, unique);
                createdUniqueConstraint = unique;
            }
            refConstraint.setRefConstraint(unique);
            if (checkExisting) {
                refConstraint.checkExistingData(session);
            }
            refTable.addConstraint(refConstraint);
            refConstraint.setDeleteAction(deleteAction);
            refConstraint.setUpdateAction(updateAction);
            constraint = refConstraint;
            break;
        }
        default:
            throw DbException.throwInternalError("type=" + type);
        }
        // parent relationship is already set with addConstraint
        constraint.setComment(comment);
        addConstraintToTable(db, table, constraint);
        return 0;
    }

    private ConstraintUnique createUniqueConstraint(Table table, Index index, IndexColumn[] indexColumns,
            boolean forForeignKey) {
        boolean isOwner = false;
        if (index != null && canUseIndex(index, table, indexColumns, true)) {
            isOwner = true;
            index.getIndexType().setBelongsToConstraint(true);
        } else {
            index = getIndex(table, indexColumns, true);
            if (index == null) {
                index = createIndex(table, indexColumns, true);
                isOwner = true;
            }
        }
        int id;
        String name;
        if (forForeignKey) {
            id = session.getDatabase().allocateObjectId();
            name = getSchema().getUniqueConstraintName(session, table);
        } else {
            id = getObjectId();
            name = generateConstraintName(table);
        }
        ConstraintUnique unique = new ConstraintUnique(getSchema(), id, name, table, false);
        unique.setColumns(indexColumns);
        unique.setIndex(index, isOwner);
        return unique;
    }

    private void addConstraintToTable(Database db, Table table, Constraint constraint) {
        if (table.isTemporary() && !table.isGlobalTemporary()) {
            session.addLocalTempTableConstraint(constraint);
        } else {
            db.addSchemaObject(session, constraint);
        }
        table.addConstraint(constraint);
    }

    private Index createIndex(Table t, IndexColumn[] cols, boolean unique) {
        int indexId = session.getDatabase().allocateObjectId();//得到新的对象id
        IndexType indexType;
        if (unique) {
            // for unique constraints
            indexType = IndexType.createUnique(t.isPersistIndexes(), false);
        } else {
            // constraints
            indexType = IndexType.createNonUnique(t.isPersistIndexes());
        }
        indexType.setBelongsToConstraint(true);
        String prefix = constraintName == null ? "CONSTRAINT" : constraintName;
        String indexName = t.getSchema().getUniqueIndexName(session, t,
                prefix + "_INDEX_");
        try {
            Index index = t.addIndex(session, indexName, indexId, cols,
                    indexType, true, null);
            createdIndexes.add(index);
            return index;
        } finally {
            getSchema().freeUniqueName(indexName);
        }
    }

    public void setDeleteAction(ConstraintActionType action) {
        this.deleteAction = action;
    }

    public void setUpdateAction(ConstraintActionType action) {
        this.updateAction = action;
    }

    private static ConstraintUnique getUniqueConstraint(Table t, IndexColumn[] cols) {
        ArrayList<Constraint> constraints = t.getConstraints();
        if (constraints != null) {
            for (Constraint constraint : constraints) {
                if (constraint.getTable() == t) {
                    Constraint.Type constraintType = constraint.getConstraintType();
                    if (constraintType == Constraint.Type.PRIMARY_KEY || constraintType == Constraint.Type.UNIQUE) {
                        if (canUseIndex(constraint.getIndex(), t, cols, true)) {
                            return (ConstraintUnique) constraint;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Index getIndex(Table t, IndexColumn[] cols, boolean unique) {
        ArrayList<Index> indexes = t.getIndexes();
        if (indexes != null) {
            for (Index idx : indexes) {
                if (canUseIndex(idx, t, cols, unique)) {
                    return idx;
                }
            }
        }
        return null;
    }

    private static boolean canUseIndex(Index index, Table table, IndexColumn[] cols, boolean unique) {
        if (index.getTable() != table //
                || (unique ? !index.getIndexType().isUnique() : index.getCreateSQL() == null) //
                || index.getColumns().length != cols.length) {
            return false;
        }
//<<<<<<< HEAD
//        Column[] indexCols = idx.getColumns();
//        HashSet<Column> indexColsSet = new HashSet<>();
//        Collections.addAll(indexColsSet, indexCols);
//        HashSet<Column> colsSet = new HashSet<>();
//        for (IndexColumn c : cols) {
//            colsSet.add(c.column);
//        }
////<<<<<<< HEAD
////        //索引列要比约束列要少，索引列必须出现在所有约束列中
////        for (Column c : indexCols) {
////            // all columns of the index must be part of the list,
////            // but not all columns of the list need to be part of the index
////            if (!set.contains(c)) {
////                return false;
////            }
////        }
////        return true;
////=======
//        return colsSet.equals(indexColsSet);
//    }
//
//    private static boolean canUseIndex(Index existingIndex, Table table,
//            IndexColumn[] cols, boolean moreColumnsOk) {
//        if (existingIndex.getTable() != table || existingIndex.getCreateSQL() == null) {
//            // can't use the scan index or index of another table
//            return false;
//        }
//        Column[] indexCols = existingIndex.getColumns();
//
//        if (moreColumnsOk) {
//            if (indexCols.length < cols.length) {
//=======
        for (IndexColumn col : cols) {
            // all columns of the list must be part of the index
            if (index.getColumnIndex(col.column) < 0) {
                return false;
            }
        }
        return true;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public int getType() {
        return type;
    }

    public void setCheckExpression(Expression expression) {
        this.checkExpression = expression;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setIndexColumns(IndexColumn[] indexColumns) {
        this.indexColumns = indexColumns;
    }

    public IndexColumn[] getIndexColumns() {
        return indexColumns;
    }

    /**
     * Set the referenced table.
     *
     * @param refSchema the schema
     * @param ref the table name
     */
    public void setRefTableName(Schema refSchema, String ref) {
        this.refSchema = refSchema;
        this.refTableName = ref;
    }

    public void setRefIndexColumns(IndexColumn[] indexColumns) {
        this.refIndexColumns = indexColumns;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    public void setRefIndex(Index refIndex) {
        this.refIndex = refIndex;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCheckExisting(boolean b) {
        this.checkExisting = b;
    }

    public void setPrimaryKeyHash(boolean b) {
        this.primaryKeyHash = b;
    }

}
