/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.pagestore.db;

import java.util.ArrayDeque;
import java.util.ArrayList;
import org.h2.api.DatabaseEventListener;
import org.h2.api.ErrorCode;
import org.h2.command.ddl.CreateTableData;
import org.h2.engine.Constants;
import org.h2.engine.SessionLocal;
import org.h2.engine.SysProperties;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.util.Utils;
import org.h2.value.CompareMode;

/**
 * A table store in a PageStore.
 */
public class PageStoreTable extends RegularTable {

    private Index scanIndex; //要么是PageDataIndex，要么是ScanIndex
    private long rowCount;

    /**
     * The queue of sessions waiting to lock the table. It is a FIFO queue to
     * prevent starvation, since Java's synchronized locking is biased.
     */
    private final ArrayDeque<SessionLocal> waitingSessions = new ArrayDeque<>();
    private final Trace traceLock;
    private final ArrayList<Index> indexes = Utils.newSmallArrayList();
    private long lastModificationId; //在addRow、commit、removeRow、truncate时改变
    private final PageDataIndex mainIndex; //这个字段用来选主索引列时有用
    private int changesSinceAnalyze;
    private int nextAnalyze;

    public PageStoreTable(CreateTableData data) {
        super(data);
        nextAnalyze = database.getSettings().analyzeAuto;
        // 如果database.isPersistent()是false，说明是内存数据库
        // 如果data.persistData是false，说明是内存临时表
        // 当是内存数据库时，不管data.persistData是false还是true都使用ScanIndex(也就是内存索引)
        if (data.persistData && database.isPersistent()) {
            mainIndex = new PageDataIndex(this, data.id,
                    IndexColumn.wrap(getColumns()),
                    IndexType.createScan(data.persistData),
                    data.create, data.session);
            scanIndex = mainIndex;
        } else {
            mainIndex = null;
            scanIndex = new ScanIndex(this, data.id,
                    IndexColumn.wrap(getColumns()), IndexType.createScan(data.persistData));
        }
        indexes.add(scanIndex);
        traceLock = database.getTrace(Trace.LOCK); //在起用debug时，其实就是用来输出debug日志，在lock/unlock时记录debug日志
    }

    @Override
    public void close(SessionLocal session) {
        for (Index index : indexes) {
            index.close(session);
        }
    }

    //从辅助索引那里过来的，索引那里记录了主表记录的key，按key获取主表的完整记录
    @Override
    public Row getRow(SessionLocal session, long key) {
        return scanIndex.getRow(session, key);
    }

    @Override
    public void addRow(SessionLocal session, Row row) {
        lastModificationId = database.getNextModificationDataId();
        int i = 0;
        try {
            //truncate、removeRow一样，都是从最后一个索引开始, 而addRow、commit是从第一个开始
            for (int size = indexes.size(); i < size; i++) {
                Index index = indexes.get(i);
                index.add(session, row);
                checkRowCount(session, index, 1);
            }
            rowCount++;
        } catch (Throwable e) {
            try {
                //因为第i个index抛异常了，所以就没必要从i开始了，直接--i
                while (--i >= 0) {
                    Index index = indexes.get(i);
                    index.remove(session, row);
                    checkRowCount(session, index, 0); //rowCount+0等于index.getRowCoun
                }
            } catch (DbException e2) {
                // this could happen, for example on failure in the storage
                // but if that is not the case it means there is something wrong
                // with the database
                trace.error(e2, "could not undo operation");
                throw e2;
            }
            throw DbException.convert(e);
        }
        analyzeIfRequired(session);
    }

    private void checkRowCount(SessionLocal session, Index index, int offset) {
        if (SysProperties.CHECK) {
            if (!(index instanceof PageDelegateIndex)) {
                long rc = index.getRowCount(session);
                if (rc != rowCount + offset) {
                    throw DbException.getInternalError("rowCount expected " + (rowCount + offset) + " got " + rc + ' '
                            + getName() + '.' + index.getName());
                }
            }
        }
    }

    @Override
    public Index getScanIndex(SessionLocal session) {
        return indexes.get(0); //ScanIndex总是在最前面
    }

    @Override
    public Index getUniqueIndex() {
        for (Index idx : indexes) {
            if (idx.getIndexType().isUnique()) {
                return idx;
            }
        }
        return null;
    }

    @Override
    public ArrayList<Index> getIndexes() {
        return indexes;
    }

    @Override
    public Index addIndex(SessionLocal session, String indexName, int indexId, IndexColumn[] cols, IndexType indexType,
            boolean create, String indexComment) {
        if (indexType.isSpatial()) {
            throw DbException.getUnsupportedException("MV_STORE=FALSE && SPATIAL INDEX");
        }
        cols = prepareColumns(database, cols, indexType);
        //如果是LOCAL TEMPORARY表，则只放到session的LocalTempTableIndex中，否则加到database的meta表
        boolean isSessionTemporary = isTemporary() && !isGlobalTemporary();
        if (!isSessionTemporary) {
            database.lockMeta(session);
        }
        Index index;
        // PrimaryKey索引，并且只有一个字段，并且此字段是byte、short、int、long类型才能作为mainIndexColumn
        // 且最初的mainIndex还没有加入记录
        if (isPersistIndexes() && indexType.isPersistent()) {
            int mainIndexColumn;
            if (database.isStarting() &&
                    database.getPageStore().getRootPageId(indexId) != 0) {
                mainIndexColumn = -1;
            } else if (!database.isStarting() && mainIndex.getRowCount(session) != 0
                    || mainIndex.getMainIndexColumn() != -1) {
                mainIndexColumn = -1;
            } else {
                mainIndexColumn = getMainIndexColumn(indexType, cols);
            }
            if (mainIndexColumn != -1) {
                mainIndex.setMainIndexColumn(mainIndexColumn);
                // PageDelegateIndex在增加行时什么都不做，
                // 满足这个条件的索引: "PrimaryKey索引，并且只有一个字段，并且此字段是byte、short、int、long类型"
                // 实际上在增加行时只有最初的PageDataIndex起作用，
                // PageDelegateIndex只在查询时有作用
                index = new PageDelegateIndex(this, indexId, indexName,
                        indexType, mainIndex, create, session);
            } else {
                index = new PageBtreeIndex(this, indexId, indexName, cols,
                        indexType, create, session);
            }
        } else {
            // hash索引最多只有一列
            if (indexType.isHash()) {
                if (cols.length != 1) {
                    throw DbException.getUnsupportedException(
                            "hash indexes may index only one column");
                }
                if (indexType.isUnique()) {
                    index = new HashIndex(this, indexId, indexName, cols,
                            indexType);
                } else {
                    index = new NonUniqueHashIndex(this, indexId, indexName,
                            cols, indexType);
                }
            } else {
                index = new TreeIndex(this, indexId, indexName, cols, indexType);
            }
        }
        if (index.needRebuild() && rowCount > 0) { //从ScanIndex中读出原始记录，新建或重建索引
            try {
                Index scan = getScanIndex(session);
                long remaining = scan.getRowCount(session);
                long total = remaining;
                Cursor cursor = scan.find(session, null, null);
                long i = 0;
                int bufferSize = (int) Math.min(rowCount, database.getMaxMemoryRows());
                ArrayList<Row> buffer = new ArrayList<>(bufferSize);
                String n = getName() + ":" + index.getName();
                while (cursor.next()) {
                    database.setProgress(DatabaseEventListener.STATE_CREATE_INDEX, n, i++, total);
                    Row row = cursor.get();
                    buffer.add(row);
                    if (buffer.size() >= bufferSize) {
                        addRowsToIndex(session, buffer, index);
                    }
                    remaining--;
                }
                addRowsToIndex(session, buffer, index);
                if (remaining != 0) {
                    throw DbException.getInternalError("rowcount remaining=" + remaining + ' ' + getName());
                }
            } catch (DbException e) {
                getSchema().freeUniqueName(indexName);
                try {
                    index.remove(session);
                } catch (DbException e2) {
                    // this could happen, for example on failure in the storage
                    // but if that is not the case it means
                    // there is something wrong with the database
                    trace.error(e2, "could not remove index");
                    throw e2;
                }
                throw e;
            }
        }
        index.setTemporary(isTemporary());
        if (index.getCreateSQL() != null) {
            index.setComment(indexComment);
            if (isSessionTemporary) {
                session.addLocalTempTableIndex(index);
            } else {
                database.addSchemaObject(session, index);
            }
        }
        indexes.add(index);
        setModified();
        return index;
    }

    @Override
    public long getRowCount(SessionLocal session) {
        return rowCount;
    }

    @Override
    public void removeRow(SessionLocal session, Row row) {
        lastModificationId = database.getNextModificationDataId();
        int i = indexes.size() - 1;
        try {
            for (; i >= 0; i--) {
                Index index = indexes.get(i);
                index.remove(session, row);
                checkRowCount(session, index, -1);
            }
            rowCount--;
        } catch (Throwable e) {
            try {
                while (++i < indexes.size()) { //truncate、removeRow一样，都是从最后一个索引开始, 而addRow、commit是从第一个开始
                    Index index = indexes.get(i);
                    index.add(session, row);
                    checkRowCount(session, index, 0);
                }
            } catch (DbException e2) {
                // this could happen, for example on failure in the storage
                // but if that is not the case it means there is something wrong
                // with the database
                trace.error(e2, "could not undo operation");
                throw e2;
            }
            throw DbException.convert(e);
        }
        analyzeIfRequired(session);
    }

    @Override
    public long truncate(SessionLocal session) {
        lastModificationId = database.getNextModificationDataId();
        long result = rowCount;
        //truncate、removeRow一样，都是从最后一个索引开始, 而addRow、commit是从第一个开始
        for (int i = indexes.size() - 1; i >= 0; i--) {
            Index index = indexes.get(i);
            index.truncate(session);
        }
        rowCount = 0;
        changesSinceAnalyze = 0;
        return result;
    }

    //默认插入2000行时，为每一字段调用SELECTIVITY聚合函数，然后修改字段的SELECTIVITY值，重新更新一下此表的meta信息
    private void analyzeIfRequired(SessionLocal session) {
        if (nextAnalyze == 0 || nextAnalyze > changesSinceAnalyze++) {
            return;
        }
        changesSinceAnalyze = 0;
        int n = 2 * nextAnalyze; //每算完一次就翻倍，比如第一次是2000行算一次，下次是4000，再下次是8000...
        if (n > 0) {
            nextAnalyze = n;
        }
        session.markTableForAnalyze(this);
    }

    // 直到事务commit或rollback时才解琐，见org.h2.engine.Session.unlockAll()
    // Select没有锁表
    // 比如DDL相关的SQL通常把force设为true，此时不管MVCC，META表都是把force设为true，
    // Select的isForUpdate变种在非MVCC下也把force设为true，
    // Insert、Update之类的才设为false
    @Override
    public boolean lock(SessionLocal session, boolean exclusive,
            boolean forceLockEvenInMvcc) { //琐粒度太大，每insert一行都琐表
        int lockMode = database.getLockMode();
        if (lockMode == Constants.LOCK_MODE_OFF) { //禁用锁
            return lockExclusiveSession != null;
        }
        if (lockExclusiveSession == session) {
            return true;
        }
        if (!exclusive && lockSharedSessions.containsKey(session)) {
            return true;
        }
        synchronized (database) {
            if (!exclusive && lockSharedSessions.contains(session)) {
                return true;
            }
            session.setWaitForLock(this, Thread.currentThread());
            waitingSessions.addLast(session);
            try {
                doLock1(session, lockMode, exclusive);
            } finally {
                session.setWaitForLock(null, null);
                waitingSessions.remove(session);
            }
        }
        return false;
    }

    private void doLock1(SessionLocal session, int lockMode, boolean exclusive) {
        traceLock(session, exclusive, "requesting for");
        // don't get the current time unless necessary
        long max = 0;
        boolean checkDeadlock = false;
        while (true) {
            // if I'm the next one in the queue
            if (waitingSessions.getFirst() == session) {
                if (doLock2(session, lockMode, exclusive)) {
                    return;
                }
            }
            if (checkDeadlock) {
                ArrayList<SessionLocal> sessions = checkDeadlock(session, null, null);
                if (sessions != null) {
                    throw DbException.get(ErrorCode.DEADLOCK_1,
                            getDeadlockDetails(sessions, exclusive));
                }
            } else {
                // check for deadlocks from now on
                checkDeadlock = true;
            }
            long now = System.nanoTime();
            if (max == 0L) {
                // try at least one more time
                max = Utils.nanoTimePlusMillis(now, session.getLockTimeout());
            } else if (now - max >= 0L) {
                traceLock(session, exclusive, "timeout after " + session.getLockTimeout());
                throw DbException.get(ErrorCode.LOCK_TIMEOUT_1, getName());
            }
            try {
                traceLock(session, exclusive, "waiting for");
                if (database.getLockMode() == Constants.LOCK_MODE_TABLE_GC) {
                    for (int i = 0; i < 20; i++) {
                        long free = Runtime.getRuntime().freeMemory();
                        System.gc();
                        long free2 = Runtime.getRuntime().freeMemory();
                        if (free == free2) {
                            break;
                        }
                    }
                }
                // don't wait too long so that deadlocks are detected early
                long sleep = Math.min(Constants.DEADLOCK_CHECK, (max - now) / 1_000_000);
                if (sleep == 0) {
                    sleep = 1;
                }
                database.wait(sleep);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private boolean doLock2(SessionLocal session, int lockMode, boolean exclusive) {
        if (exclusive) {
            if (lockExclusiveSession == null) {
                if (lockSharedSessions.isEmpty()) {
                    traceLock(session, exclusive, "added for");
                    session.registerTableAsLocked(this);
                    lockExclusiveSession = session;
                    return true;
                } else if (lockSharedSessions.size() == 1 &&
                        lockSharedSessions.containsKey(session)) {
                    //如果前面有一个读锁，并且是相同的session，那么insert之类的操作不须等待
                    traceLock(session, exclusive, "add (upgraded) for ");
                    lockExclusiveSession = session;
                    return true;
                }
            }
        } else {
            if (lockExclusiveSession == null) {
                // 如果lockExclusive不为null，说明前面有一个排它锁，不管当前操作是查询还是更新，都必须等待，
                // 如果lockExclusive为null，那么当前操作可顺利进行
                if (lockMode == Constants.LOCK_MODE_READ_COMMITTED) {
                    // PageStore is single-threaded, no lock is required
                    return true;
                }
                if (!lockSharedSessions.containsKey(session)) {
                    traceLock(session, exclusive, "ok");
                    session.registerTableAsLocked(this);
                    lockSharedSessions.put(session, session);
                }
                return true;
            }
        }
        return false;
    }

    private void traceLock(SessionLocal session, boolean exclusive, String s) {
        if (traceLock.isDebugEnabled()) {
            traceLock.debug("{0} {1} {2} {3}", session.getId(),
                    exclusive ? "exclusive write lock" : "shared read lock", s, getName());
        }
    }

    @Override
    public void unlock(SessionLocal s) {
        if (database != null) {
            traceLock(s, lockExclusiveSession == s, "unlock");
            if (lockExclusiveSession == s) {
                lockSharedSessions.remove(s);
                lockExclusiveSession = null;
            }
            synchronized (database) {
                if (!lockSharedSessions.isEmpty()) {
                    lockSharedSessions.remove(s);
                }
                if (!waitingSessions.isEmpty()) {
                    database.notifyAll();
                }
            }
        }
    }

    /**
     * Set the row count of this table.
     *
     * @param count the row count
     */
    public void setRowCount(long count) {
        this.rowCount = count;
    }

    @Override
    public void removeChildrenAndResources(SessionLocal session) {
        if (containsLargeObject) {
            // unfortunately, the data is gone on rollback
            truncate(session);
            database.getLobStorage().removeAllForTable(getId());
            database.lockMeta(session);
        }
        // 里面删除约束时会把属于约束的索引也删了，此时indexes会变化
        // 在org.h2.index.BaseIndex.removeChildrenAndResources(Session)中删除
        super.removeChildrenAndResources(session);
        // go backwards because database.removeIndex will call table.removeIndex
        while (indexes.size() > 1) {
            Index index = indexes.get(1);
            if (index.getName() != null) {
                database.removeSchemaObject(session, index);
            }
            // needed for session temporary indexes
            indexes.remove(index);
        }
        scanIndex.remove(session);
        database.removeMeta(session, getId());
        scanIndex = null;
        lockExclusiveSession = null;
        lockSharedSessions.clear();
        invalidate();
    }

    @Override
    public long getMaxDataModificationId() {
        return lastModificationId;
    }

    @Override
    public long getRowCountApproximation(SessionLocal session) {
        return scanIndex.getRowCountApproximation(session); //ScanIndex和PageDataIndex都是返回rowCount
    }

    @Override
    public long getDiskSpaceUsed() { //用于DISK_SPACE_USED函数
        // 如果是ScanIndex，因为ScanIndex只是个内存索引，所以返回0，
        // 如果么是PageDataIndex，那么就是leaf节点个数*pageSize。
        return scanIndex.getDiskSpaceUsed();
    }

    public void setCompareMode(CompareMode compareMode) {
        this.compareMode = compareMode;
    }

    @Override
    public int getMainIndexColumn() {
        return mainIndex != null ? mainIndex.getMainIndexColumn() : SearchRow.ROWID_INDEX;
    }

}
