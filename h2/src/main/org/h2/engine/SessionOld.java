///*
// * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
// * and the EPL 1.0 (https://h2database.com/html/license.html).
// * Initial Developer: H2 Group
// */
//package org.h2.engine;
//
//import java.util.ArrayList;
//
//import org.h2.command.CommandInterface;
//import org.h2.jdbc.meta.DatabaseMeta;
//import org.h2.message.Trace;
//import org.h2.result.ResultInterface;
//import org.h2.store.DataHandler;
//import org.h2.util.NetworkConnectionInfo;
//import org.h2.util.TimeZoneProvider;
//import org.h2.util.Utils;
//import org.h2.value.ValueLob;
//
///**
// * A local or remote session. A session represents a database connection.
// */
//public abstract class Session implements CastDataProvider, AutoCloseable {
//
//    /**
//     * Static settings.
//     */
//    public static final class StaticSettings {
//
//        /**
//         * Whether unquoted identifiers are converted to upper case.
//         */
//        public final boolean databaseToUpper;
//
//        /**
//         * Whether unquoted identifiers are converted to lower case.
//         */
//        public final boolean databaseToLower;
//
//        /**
//         * Whether all identifiers are case insensitive.
//         */
//        public final boolean caseInsensitiveIdentifiers;
//
//        /**
//         * Creates new instance of static settings.
//         *
//         * @param databaseToUpper
//         *            whether unquoted identifiers are converted to upper case
//         * @param databaseToLower
//         *            whether unquoted identifiers are converted to lower case
//         * @param caseInsensitiveIdentifiers
//         *            whether all identifiers are case insensitive
//         */
//        public StaticSettings(boolean databaseToUpper, boolean databaseToLower, boolean caseInsensitiveIdentifiers) {
//            this.databaseToUpper = databaseToUpper;
//            this.databaseToLower = databaseToLower;
//            this.caseInsensitiveIdentifiers = caseInsensitiveIdentifiers;
//        }
//
//    }
//
//    /**
//     * Dynamic settings.
//     */
//    public static final class DynamicSettings {
//
//        /**
//         * The database mode.
//         */
//        public final Mode mode;
//
//        /**
//         * The current time zone.
//         */
//        public final TimeZoneProvider timeZone;
//
//        /**
//         * Creates new instance of dynamic settings.
//         *
//         * @param mode
//         *            the database mode
//         * @param timeZone
//         *            the current time zone
//         */
//        public DynamicSettings(Mode mode, TimeZoneProvider timeZone) {
//            this.mode = mode;
//            this.timeZone = timeZone;
//        }
//
//    }
//
//    private ArrayList<String> sessionState;
//
//    boolean sessionStateChanged;
//
//    private boolean sessionStateUpdating;
//
//    volatile StaticSettings staticSettings;
//
//    Session() {
//    }
//
//    /**
//     * Get the list of the cluster servers for this session.
//     *
//     * @return A list of "ip:port" strings for the cluster servers in this
//     *         session.
//     */
//    public abstract ArrayList<String> getClusterServers();
//
//    /**
//     * Parse a command and prepare it for execution.
//     *
//     * @param sql the SQL statement
//     * @param fetchSize the number of rows to fetch in one step
//     * @return the prepared command
//     */
//    public abstract CommandInterface prepareCommand(String sql, int fetchSize);
//
//    /**
//     * Roll back pending transactions and close the session.
//     */
//    @Override
//    public abstract void close();
//
//    /**
//     * Get the trace object
//     *
//     * @return the trace object
//     */
//    public abstract Trace getTrace();
//
//    /**
//     * Check if close was called.
//     *
//     * @return if the session has been closed
//     */
//    public abstract boolean isClosed();
//
//    /**
//     * Get the data handler object.
//     *
//     * @return the data handler
//     */
//    public abstract DataHandler getDataHandler();
//
//    /**
//     * Check whether this session has a pending transaction.
//     *
//     * @return true if it has
//     */
//    public abstract boolean hasPendingTransaction();
//
//    /**
//     * Cancel the current or next command (called when closing a connection).
//     */
//    public abstract void cancel();
//
//    /**
//     * Returns the TCP protocol version of remote connection, or the latest
//     * supported TCP protocol version for local session.
//     *
//     * @return the TCP protocol version
//     */
//    public abstract int getClientVersion();
//
//    /**
//     * Check if this session is in auto-commit mode.
//     *
//     * @return true if the session is in auto-commit mode
//     */
//    public abstract boolean getAutoCommit();
//
//    /**
//     * Set the auto-commit mode. This call doesn't commit the current
//     * transaction.
//     *
//     * @param autoCommit the new value
//     */
//    public abstract void setAutoCommit(boolean autoCommit);
//
//    /**
//     * Add a temporary LOB, which is closed when the session commits.
//     *
//     * @param v the value
//     * @return the specified value
//     */
//    public abstract ValueLob addTemporaryLob(ValueLob v);
//
//    /**
//     * Check if this session is remote or embedded.
//     *
//     * @return true if this session is remote
//     */
//    public abstract boolean isRemote();
//
//    /**
//     * Set current schema.
//     *
//     * @param schema the schema name
//     */
//<<<<<<< HEAD
//    void removeLocalTempTableConstraint(Constraint constraint) {
//        if (localTempTableConstraints != null) {
//            localTempTableConstraints.remove(constraint.getName());
//            synchronized (database) {
//                constraint.removeChildrenAndResources(this);
//            }
//        }
//    }
//
//    @Override
//    public boolean getAutoCommit() {
//        return autoCommit;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    @Override
//    public void setAutoCommit(boolean b) {
//        autoCommit = b;
//    }
//
//    public int getLockTimeout() {
//        return lockTimeout;
//    }
//
//    public void setLockTimeout(int lockTimeout) {
//        this.lockTimeout = lockTimeout;
//        if (transaction != null) {
//            transaction.setTimeoutMillis(lockTimeout);
//        }
//    }
//
//    //4个prepare方法要么生成CommandInterface(Command)要么生成Prepared
//    //并且都调用org.h2.command.Prepared.prepare()方法
//    @Override
//    public synchronized CommandInterface prepareCommand(String sql,
//            int fetchSize) {
//        return prepareLocal(sql);
//    }
//=======
//    public abstract void setCurrentSchemaName(String schema);
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
//
//    /**
//     * Get current schema.
//     *
//     * @return the current schema name
//     */
//<<<<<<< HEAD
//    //
//    public Prepared prepare(String sql) {
//        return prepare(sql, false, false);
//    }
//=======
//    public abstract String getCurrentSchemaName();
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
//
//    /**
//     * Returns is this session supports generated keys.
//     *
//     * @return {@code true} if generated keys are supported, {@code false} if only
//     *         {@code SCOPE_IDENTITY()} is supported
//     */
//<<<<<<< HEAD
//    //rightsChecked参数用来传到org.h2.table.TableFilter的构造函数中，用于检查否有Right.SELECT权限
//    //如果是true，那么就不检查了
//    public Prepared prepare(String sql, boolean rightsChecked, boolean literalsChecked) {
//        Parser parser = new Parser(this);
//        parser.setRightsChecked(rightsChecked);
//        parser.setLiteralsChecked(literalsChecked);
//        return parser.prepare(sql);
//    }
//=======
//    public abstract boolean isSupportsGeneratedKeys();
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
//
//    /**
//     * Sets the network connection information if possible.
//     *
//     * @param networkConnectionInfo the network connection information
//     */
//<<<<<<< HEAD
//    public Command prepareLocal(String sql) {
//        if (isClosed()) {
//            throw DbException.get(ErrorCode.CONNECTION_BROKEN_1,
//                    "session closed");
//        }
//        Command command;
//        //如果允许重用那么不再做SQL解析
//        if (queryCacheSize > 0) {
//            if (queryCache == null) {
//                queryCache = SmallLRUCache.newInstance(queryCacheSize);
//                modificationMetaID = database.getModificationMetaId();
//            } else {
//                long newModificationMetaID = database.getModificationMetaId();
//                if (newModificationMetaID != modificationMetaID) {
//                    queryCache.clear();
//                    modificationMetaID = newModificationMetaID;
//                }
//                command = queryCache.get(sql);
//                //命令关闭后才可重用
//                if (command != null && command.canReuse()) {
//                    command.reuse();
//                    return command;
//                }
//            }
//        }
//        Parser parser = new Parser(this);
//        try {
//            command = parser.prepareCommand(sql);
//        } finally {
//            // we can't reuse sub-query indexes, so just drop the whole cache
//            subQueryIndexCache = null;
//        }
//        if (queryCache != null) {
//            //只有DML并且是下面的这几类可缓存:
//            //Insert、Delete、Update、Merge、TransactionCommand这5个无条件可缓存
//            //Call这个当产生的结果不是结果集时可缓存，否则不可缓存
//            //Select这个当不是isForUpdate时可缓存，否则不可缓存
//            if (command.isCacheable()) {
//                queryCache.put(sql, command);
//            }
//        }
//        return command;
//    }
//=======
//    public abstract void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo);
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
//
//    /**
//     * Returns the isolation level.
//     *
//     * @return the isolation level
//     */
//    public abstract IsolationLevel getIsolationLevel();
//
//    /**
//     * Sets the isolation level.
//     *
//     * @param isolationLevel the isolation level to set
//     */
//    public abstract void setIsolationLevel(IsolationLevel isolationLevel);
//
//    /**
//     * Returns static settings. These settings cannot be changed during
//     * lifecycle of session.
//     *
//     * @return static settings
//     */
//    public abstract StaticSettings getStaticSettings();
//
//    /**
//     * Returns dynamic settings. These settings can be changed during lifecycle
//     * of session.
//     *
//     * @return dynamic settings
//     */
//    public abstract DynamicSettings getDynamicSettings();
//
//    /**
//     * Returns database meta information.
//     *
//     * @return database meta information
//     */
//<<<<<<< HEAD
//    public void rollbackTo(Savepoint savepoint) { //"SET UNDO_LOG 0"禁用撤消日志，此时所有rollback都无效
//        int index = savepoint == null ? 0 : savepoint.logIndex;
//        if (undoLog != null) {
//            while (undoLog.size() > index) {
//                UndoLogRecord entry = undoLog.getLast();
//                entry.undo(this);
//                undoLog.removeLast();
//            }
//        }
//        //这里的rollback是session级的，
//        //而org.h2.mvstore.db.TransactionStore.rollbackTo(Transaction, long, long)是表级的，在MVTable的addRow和removeRow调用
//        //事务有可能更新了多个表，只要其中之一提前 rollback了，此时就需要rollback其他的
//        if (transaction != null) {
//            markUsedTablesAsUpdated();
//            if (savepoint == null) {
//                transaction.rollback();
//                transaction = null;
//            } else {
//                transaction.rollbackToSavepoint(savepoint.transactionSavepoint);
//            }
//        }
//        if (savepoints != null) {
//            String[] names = savepoints.keySet().toArray(new String[0]);
//            for (String name : names) {
//                Savepoint sp = savepoints.get(name);
//                int savepointIndex = sp.logIndex;
//                if (savepointIndex > index) {
//                    savepoints.remove(name);
//                }
//            }
//        }
//
//        // Because cache may have captured query result (in Query.lastResult),
//        // which is based on data from uncommitted transaction.,
//        // It is not valid after rollback, therefore cache has to be cleared.
//        if (queryCache != null) {
//            queryCache.clear();
//        }
//    }
//
//    @Override
//    public boolean hasPendingTransaction() {
//        return undoLog != null && undoLog.size() > 0;
//    }
//=======
//    public abstract DatabaseMeta getDatabaseMeta();
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
//
//    /**
//     * Returns whether INFORMATION_SCHEMA contains old-style tables.
//     *
//     * @return whether INFORMATION_SCHEMA contains old-style tables
//     */
//    public abstract boolean isOldInformationSchema();
//
//    /**
//     * Re-create the session state using the stored sessionState list.
//     */
//    void recreateSessionState() {
//        if (sessionState != null && !sessionState.isEmpty()) {
//            sessionStateUpdating = true;
//            try {
//                for (String sql : sessionState) {
//                    CommandInterface ci = prepareCommand(sql, Integer.MAX_VALUE);
//                    ci.executeUpdate(null);
//                }
//            } finally {
//                sessionStateUpdating = false;
//                sessionStateChanged = false;
//            }
//        }
//    }
//
//    /**
//     * Read the session state if necessary.
//     */
//    public void readSessionState() {
//        if (!sessionStateChanged || sessionStateUpdating) {
//            return;
//        }
//<<<<<<< HEAD
//        if (undoLogEnabled) { //undoLogEnabled默认是true
//            UndoLogRecord log = new UndoLogRecord(table, operation, row);
//            // called _after_ the row was inserted successfully into the table,
//            // otherwise rollback will try to rollback a not-inserted row
//            if (SysProperties.CHECK) {
//                int lockMode = database.getLockMode();
//                if (lockMode != Constants.LOCK_MODE_OFF &&
////<<<<<<< HEAD
////                        !database.isMultiVersion()) {
////                    //当要记撤消日志时，除了TABLE_LINK、EXTERNAL_TABLE_ENGINE这两种类型的表外，其他表类型必须先锁表
////                    //例如调用org.h2.table.Table.lock(Session, boolean, boolean)
////=======
//                        !database.isMVStore()) {
//                    TableType tableType = log.getTable().getTableType();
//                    if (!locks.contains(log.getTable())
//                            && TableType.TABLE_LINK != tableType
//                            && TableType.EXTERNAL_TABLE_ENGINE != tableType) {
//                        throw DbException.throwInternalError(String.valueOf(tableType));
//                    }
//                }
//            }
////<<<<<<< HEAD
////            undoLog.add(log);
////        } else {
////            if (database.isMultiVersion()) {
////              //调用SET UNDO_LOG 0可禁用撤消日志
////                // see also UndoLogRecord.commit
////                ArrayList<Index> indexes = table.getIndexes();
////                for (int i = 0, size = indexes.size(); i < size; i++) {
////                    Index index = indexes.get(i);
////                    index.commit(operation, row);
////                }
////                row.commit(); //把Row中的sessionId设为0
////=======
//            if (undoLog == null) {
//                undoLog = new UndoLog(database);
//            }
//            undoLog.add(log);
//        }
//    }
//
//    /**
//     * Unlock just this table.
//     *
//     * @param t the table to unlock
//     */
//    void unlock(Table t) {
//        locks.remove(t);
//    }
//
//    private void unlockAll() {
//        if (undoLog != null && undoLog.size() > 0) {
//            throw DbException.throwInternalError();
//        }
//        if (!locks.isEmpty()) {
//            Table[] array = locks.toArray(new Table[0]);
//            for (Table t : array) {
//                if (t != null) {
//                    t.unlock(this);
//                }
//            }
//            locks.clear();
//        }
//        database.unlockMetaDebug(this);
//        savepoints = null;
//        sessionStateChanged = true;
//    }
//
//    private void cleanTempTables(boolean closeSession) {
//        if (localTempTables != null && localTempTables.size() > 0) {
//            if (database.isMVStore()) {
//                _cleanTempTables(closeSession);
//            } else {
//                synchronized (database) {
//                    _cleanTempTables(closeSession);
//                }
//            }
//        }
//    }
//
//    private void _cleanTempTables(boolean closeSession) {
//        Iterator<Table> it = localTempTables.values().iterator();
//        while (it.hasNext()) {
//            Table table = it.next();
//            if (closeSession || table.getOnCommitDrop()) {
//                modificationId++;
//                table.setModified();
//                it.remove();
//                // Exception thrown in org.h2.engine.Database.removeMeta
//                // if line below is missing with TestDeadlock
//                database.lockMeta(this);
//                table.removeChildrenAndResources(this);
//                if (closeSession) {
//                    // need to commit, otherwise recovery might
//                    // ignore the table removal
//                    database.commit(this);
//                }
//            } else if (table.getOnCommitTruncate()) {
//                table.truncate(this);
//            }
//        }
//    }
//
//    public Random getRandom() {
//        if (random == null) {
//            random = new Random();
//        }
//        return random;
//    }
//
//    @Override
//    public Trace getTrace() {
//        if (trace != null && !isClosed()) {
//            return trace;
//        }
//        String traceModuleName = "jdbc[" + id + "]";
//        if (isClosed()) {
//            return new TraceSystem(null).getTrace(traceModuleName);
//        }
//        trace = database.getTraceSystem().getTrace(traceModuleName);
//        return trace;
//    }
//
//    /**
//     * Sets the current value of the sequence and last identity value for this
//     * session.
//     *
//     * @param sequence
//     *            the sequence
//     * @param value
//     *            the current value of the sequence
//     */
//    public void setCurrentValueFor(Sequence sequence, Value value) {
//        WeakHashMap<Sequence, Value> currentValueFor = this.currentValueFor;
//        if (currentValueFor == null) {
//            this.currentValueFor = currentValueFor = new WeakHashMap<>();
//        }
//        currentValueFor.put(sequence, value);
//        setLastIdentity(value);
//    }
//
//    /**
//     * Returns the current value of the sequence in this session.
//     *
//     * @param sequence
//     *            the sequence
//     * @return the current value of the sequence in this session
//     * @throws DbException
//     *             if current value is not defined
//     */
//    public Value getCurrentValueFor(Sequence sequence) {
//        WeakHashMap<Sequence, Value> currentValueFor = this.currentValueFor;
//        if (currentValueFor != null) {
//            Value value = currentValueFor.get(sequence);
//            if (value != null) {
//                return value;
//            }
//        }
//        throw DbException.get(ErrorCode.CURRENT_SEQUENCE_VALUE_IS_NOT_DEFINED_IN_SESSION_1, sequence.getSQL(false));
//    }
//
//    public void setLastIdentity(Value last) {
//        this.lastIdentity = last;
//        this.lastScopeIdentity = last;
//    }
//
//    public Value getLastIdentity() {
//        return lastIdentity;
//    }
//
//    public void setLastScopeIdentity(Value last) {
//        this.lastScopeIdentity = last;
//    }
//
//    public Value getLastScopeIdentity() {
//        return lastScopeIdentity;
//    }
//
//    public void setLastTriggerIdentity(Value last) {
//        this.lastTriggerIdentity = last;
//    }
//
//    public Value getLastTriggerIdentity() {
//        return lastTriggerIdentity;
//    }
//
//    /**
//     * Called when a log entry for this session is added. The session keeps
//     * track of the first entry in the transaction log that is not yet
//     * committed.
//     *
//     * @param logId the transaction log id
//     * @param pos the position of the log entry in the transaction log
//     */
//    //在org.h2.store.PageLog.logAddOrRemoveRow(Session, int, Row, boolean)和logTruncate时调用
//    public void addLogPos(int logId, int pos) {
//        if (firstUncommittedLog == Session.LOG_WRITTEN) {
//            firstUncommittedLog = logId;
//            firstUncommittedPos = pos;
//        }
//    }
//
//    public int getFirstUncommittedLog() {
//        return firstUncommittedLog;
//    }
//
//    /**
//     * This method is called after the transaction log has written the commit
//     * entry for this session.
//     */
//    void setAllCommitted() {
//        firstUncommittedLog = Session.LOG_WRITTEN;
//        firstUncommittedPos = Session.LOG_WRITTEN;
//    }
//
//    /**
//     * Whether the session contains any uncommitted changes.
//     *
//     * @return true if yes
//     */
//    public boolean containsUncommitted() {
//        if (database.getStore() != null) {
//            return transaction != null && transaction.hasChanges();
//        }
//        return firstUncommittedLog != Session.LOG_WRITTEN;
//    }
//
//    /**
//     * Create a savepoint that is linked to the current log position.
//     *
//     * @param name the savepoint name
//     */
//    public void addSavepoint(String name) {
//        if (savepoints == null) {
//            savepoints = database.newStringMap();
//        }
//        savepoints.put(name, setSavepoint());
//    }
//
//    /**
//     * Undo all operations back to the log position of the given savepoint.
//     *
//     * @param name the savepoint name
//     */
//    public void rollbackToSavepoint(String name) { //"SET UNDO_LOG 0"禁用撤消日志，此时所有rollback都无效
//        checkCommitRollback();
//        currentTransactionName = null;
//        transactionStart = null;
//        if (savepoints == null) {
//            throw DbException.get(ErrorCode.SAVEPOINT_IS_INVALID_1, name);
//        }
//        Savepoint savepoint = savepoints.get(name);
//        if (savepoint == null) {
//            throw DbException.get(ErrorCode.SAVEPOINT_IS_INVALID_1, name);
//        }
//        rollbackTo(savepoint);
//    }
//
//    /**
//     * Prepare the given transaction.
//     *
//     * @param transactionName the name of the transaction
//     */
//    public void prepareCommit(String transactionName) {
//        if (containsUncommitted()) {
//            // need to commit even if rollback is not possible (create/drop
//            // table and so on)
//            database.prepareCommit(this, transactionName);
//        }
//        currentTransactionName = transactionName;
//    }
//
//    /**
//     * Commit or roll back the given transaction.
//     *
//     * @param transactionName the name of the transaction
//     * @param commit true for commit, false for rollback
//     */
//    public void setPreparedTransaction(String transactionName, boolean commit) {
//        if (currentTransactionName != null &&
//                currentTransactionName.equals(transactionName)) {
//            if (commit) {
//                commit(false);
//            } else {
//                rollback();
//            }
//        } else {
//            ArrayList<InDoubtTransaction> list = database
//                    .getInDoubtTransactions();
//            int state = commit ? InDoubtTransaction.COMMIT
//                    : InDoubtTransaction.ROLLBACK;
//            boolean found = false;
//            if (list != null) {
//                for (InDoubtTransaction p: list) {
//                    if (p.getTransactionName().equals(transactionName)) {
//                        p.setState(state);
//                        found = true;
//                        break;
//                    }
//                }
//            }
//            if (!found) {
//                throw DbException.get(ErrorCode.TRANSACTION_NOT_FOUND_1,
//                        transactionName);
//            }
//        }
//    }
//
//    @Override
//    public boolean isClosed() {
//        return state.get() == State.CLOSED;
//    }
//
//    public boolean isOpen() {
//        State current = state.get();
//        checkSuspended(current);
//        return current != State.CLOSED;
//    }
//
//    public void setThrottle(int throttle) {
//        this.throttleNs = TimeUnit.MILLISECONDS.toNanos(throttle);
//    }
//
//    /**
//     * Wait for some time if this session is throttled (slowed down).
//     */
//    public void throttle() {
//        if (currentCommandStart == null) {
//            currentCommandStart = DateTimeUtils.currentTimestamp(timeZone);
//        }
//        if (throttleNs == 0) {
//            return;
//        }
//        long time = System.nanoTime();
//        if (lastThrottle + TimeUnit.MILLISECONDS.toNanos(Constants.THROTTLE_DELAY) > time) {
//            return;
//        }
//        lastThrottle = time + throttleNs;
//        State prevState = transitionToState(State.THROTTLED, false);
//        try {
//            Thread.sleep(TimeUnit.NANOSECONDS.toMillis(throttleNs));
//        } catch (InterruptedException ignore) {
//        } finally {
//            transitionToState(prevState, false);
//        }
//    }
//
//    /**
//     * Set the current command of this session. This is done just before
//     * executing the statement.
//     *
//     * @param command the command
//     */
//    private void setCurrentCommand(Command command) {
//        State targetState = command == null ? State.SLEEP : State.RUNNING;
//        transitionToState(targetState, true);
//        if (isOpen()) {
//            currentCommand = command;
//            if (command != null) {
//                if (queryTimeout > 0) {
//                    currentCommandStart = DateTimeUtils.currentTimestamp(timeZone);
//                    long now = System.nanoTime();
//                    cancelAtNs = now + TimeUnit.MILLISECONDS.toNanos(queryTimeout);
//                } else {
//                    currentCommandStart = null;
//                }
//            }
//        }
//    }
//
//    private State transitionToState(State targetState, boolean checkSuspended) {
//        State currentState;
//        while((currentState = state.get()) != State.CLOSED &&
//                (!checkSuspended || checkSuspended(currentState)) &&
//                !state.compareAndSet(currentState, targetState)) {/**/}
//        return currentState;
//    }
//
//    private boolean checkSuspended(State currentState) {
//        if (currentState == State.SUSPENDED) {
//            close();
//            throw DbException.get(ErrorCode.DATABASE_IS_IN_EXCLUSIVE_MODE);
//        }
//        return true;
//    }
//
//    /**
//     * Check if the current transaction is canceled by calling
//     * Statement.cancel() or because a session timeout was set and expired.
//     *
//     * @throws DbException if the transaction is canceled
//     */
//    public void checkCanceled() {
//        throttle();
//        if (cancelAtNs == 0) {
//            return;
//        }
//        long time = System.nanoTime();
//        if (time >= cancelAtNs) {
//            cancelAtNs = 0;
//            throw DbException.get(ErrorCode.STATEMENT_WAS_CANCELED);
//        }
//    }
//
//    /**
//     * Get the cancel time.
//     *
//     * @return the time or 0 if not set
//     */
//    public long getCancel() {
//        return cancelAtNs;
//    }
//
//    public Command getCurrentCommand() {
//        return currentCommand;
//    }
//
//    public ValueTimestampTimeZone getCurrentCommandStart() {
//        if (currentCommandStart == null) {
//            currentCommandStart = DateTimeUtils.currentTimestamp(timeZone);
//        }
//        return currentCommandStart;
//    }
//
//    public boolean getAllowLiterals() {
//        return allowLiterals;
//    }
//
//    public void setAllowLiterals(boolean b) {
//        this.allowLiterals = b;
//    }
//
//    public void setCurrentSchema(Schema schema) {
//        modificationId++;
//        if (queryCache != null) {
//            queryCache.clear();
//        }
//        this.currentSchemaName = schema.getName();
//    }
//
//    @Override
//    public String getCurrentSchemaName() {
//        return currentSchemaName;
//    }
//
//    @Override
//    public void setCurrentSchemaName(String schemaName) {
//        Schema schema = database.getSchema(schemaName);
//        setCurrentSchema(schema);
//    }
//
//    /**
//     * Create an internal connection. This connection is used when initializing
//     * triggers, and when calling user defined functions.
//     *
//     * @param columnList if the url should be 'jdbc:columnlist:connection'
//     * @return the internal connection
//     */
//    public JdbcConnection createConnection(boolean columnList) {
//        String url;
//        if (columnList) {
//            url = Constants.CONN_URL_COLUMNLIST;
//        } else {
//            url = Constants.CONN_URL_INTERNAL;
//        }
//        return new JdbcConnection(this, getUser().getName(), url);
//    }
//
//    @Override
//    public DataHandler getDataHandler() {
//        return database;
//    }
//
//    /**
//     * Remember that the given LOB value must be removed at commit.
//     *
//     * @param v the value
//     */
//    public void removeAtCommit(Value v) {
//        final String key = v.toString();
//        if (!v.isLinkedToTable()) {
//            throw DbException.throwInternalError(key);
//        }
//        if (removeLobMap == null) {
//            removeLobMap = new HashMap<>();
//        }
//        removeLobMap.put(key, v);
//    }
//
//    /**
//     * Do not remove this LOB value at commit any longer.
//     *
//     * @param v the value
//     */
//    public void removeAtCommitStop(Value v) {
//        if (removeLobMap != null) {
//            removeLobMap.remove(v.toString());
//        }
//    }
//
//    /**
//     * Get the next system generated identifiers. The identifier returned does
//     * not occur within the given SQL statement.
//     *
//     * @param sql the SQL statement
//     * @return the new identifier
//     */
//    public String getNextSystemIdentifier(String sql) {
//        String identifier;
//        do {
//            identifier = SYSTEM_IDENTIFIER_PREFIX + systemIdentifier++;
//        } while (sql.contains(identifier));
//        return identifier;
//    }
//
//    /**
//     * Add a procedure to this session.
//     *
//     * @param procedure the procedure to add
//     */
//    public void addProcedure(Procedure procedure) {
//        if (procedures == null) {
//            procedures = database.newStringMap();
//        }
//        procedures.put(procedure.getName(), procedure);
//    }
//
//    /**
//     * Remove a procedure from this session.
//     *
//     * @param name the name of the procedure to remove
//     */
//    public void removeProcedure(String name) {
//        if (procedures != null) {
//            procedures.remove(name);
//        }
//    }
//
//    /**
//     * Get the procedure with the given name, or null
//     * if none exists.
//     *
//     * @param name the procedure name
//     * @return the procedure or null
//     */
//    public Procedure getProcedure(String name) {
//        if (procedures == null) {
//            return null;
//        }
//        return procedures.get(name);
//    }
//
//    public void setSchemaSearchPath(String[] schemas) {
//        modificationId++;
//        this.schemaSearchPath = schemas;
//    }
//
//    public String[] getSchemaSearchPath() {
//        return schemaSearchPath;
//    }
//
//    @Override
//    public int hashCode() {
//        return serialId;
//    }
//
//    @Override
//    public String toString() {
//        return "#" + serialId + " (user: " + (user == null ? "<null>" : user.getName()) + ", " + state.get() + ")";
//    }
//
//    public void setUndoLogEnabled(boolean b) {
//        this.undoLogEnabled = b;
//    }
//
//    public void setRedoLogBinary(boolean b) {
//        this.redoLogBinary = b;
//    }
//
//    public boolean isUndoLogEnabled() {
//        return undoLogEnabled;
//    }
//
//    /**
//     * Begin a transaction.
//     */
//    public void begin() {
//        autoCommitAtTransactionEnd = true;
//        autoCommit = false;
//    }
//
//    public ValueTimestampTimeZone getSessionStart() {
//        return sessionStart;
//    }
//
//    public ValueTimestampTimeZone getTransactionStart() {
//        if (transactionStart == null) {
//            transactionStart = DateTimeUtils.currentTimestamp(timeZone);
//        }
//        return transactionStart;
//    }
//
//    public Set<Table> getLocks() {
//        /*
//         * This implementation needs to be lock-free.
//         */
//        if (database.getLockMode() == Constants.LOCK_MODE_OFF || locks.isEmpty()) {
//            return Collections.emptySet();
//        }
//        /*
//         * Do not use ArrayList.toArray(T[]) here, its implementation is not
//         * thread-safe.
//         */
//        Object[] array = locks.toArray();
//        /*
//         * The returned array may contain null elements and may contain
//         * duplicates due to concurrent remove().
//         */
//        switch (array.length) {
//        case 1: {
//            Object table = array[0];
//            if (table != null) {
//                return Collections.singleton((Table) table);
//            }
//        }
//        //$FALL-THROUGH$
//        case 0:
//            return Collections.emptySet();
//        default: {
//            HashSet<Table> set = new HashSet<>();
//            for (Object table : array) {
//                if (table != null) {
//                    set.add((Table) table);
//                }
//            }
//            return set;
//        }
//        }
//    }
//
//    /**
//     * Wait if the exclusive mode has been enabled for another session. This
//     * method returns as soon as the exclusive mode has been disabled.
//     */
//    public void waitIfExclusiveModeEnabled() {
//        transitionToState(State.RUNNING, true);
//        // Even in exclusive mode, we have to let the LOB session proceed, or we
//        // will get deadlocks.
//        if (database.getLobSession() == this) {
//            return;
//        }
//        while (isOpen()) {
//            Session exclusive = database.getExclusiveSession();
//            if (exclusive == null || exclusive == this) {
//                break;
//            }
//            if (Thread.holdsLock(exclusive)) {
//                // if another connection is used within the connection
//                break;
//            }
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                // ignore
//            }
//        }
//    }
//
//    /**
//     * Get the view cache for this session. There are two caches: the subquery
//     * cache (which is only use for a single query, has no bounds, and is
//     * cleared after use), and the cache for regular views.
//     *
//     * @param subQuery true to get the subquery cache
//     * @return the view cache
//     */
//    public Map<Object, ViewIndex> getViewIndexCache(boolean subQuery) {
//        if (subQuery) {
//            // for sub-queries we don't need to use LRU because the cache should
//            // not grow too large for a single query (we drop the whole cache in
//            // the end of prepareLocal)
//            if (subQueryIndexCache == null) {
//                subQueryIndexCache = new HashMap<>();
//            }
//            return subQueryIndexCache;
//        }
//        SmallLRUCache<Object, ViewIndex> cache = viewIndexCache;
//        if (cache == null) {
//            viewIndexCache = cache = SmallLRUCache.newInstance(Constants.VIEW_INDEX_CACHE_SIZE);
//        }
//        return cache;
//    }
//
//    /**
//     * Remember the result set and close it as soon as the transaction is
//     * committed (if it needs to be closed). This is done to delete temporary
//     * files as soon as possible, and free object ids of temporary tables.
//     *
//     * @param result the temporary result set
//     */
//    public void addTemporaryResult(ResultInterface result) {
//        if (!result.needToClose()) {
//            return;
//        }
//        if (temporaryResults == null) {
//            temporaryResults = new HashSet<>();
//        }
//        if (temporaryResults.size() < 100) {
//            // reference at most 100 result sets to avoid memory problems
//            temporaryResults.add(result);
//        }
//    }
//
//    private void closeTemporaryResults() {
//        if (temporaryResults != null) {
//            for (ResultInterface result : temporaryResults) {
//                result.close();
//            }
//            temporaryResults = null;
//        }
//    }
//
//    public void setQueryTimeout(int queryTimeout) {
//        int max = database.getSettings().maxQueryTimeout;
//        if (max != 0 && (max < queryTimeout || queryTimeout == 0)) {
//            // the value must be at most max
//            queryTimeout = max;
//        }
//        this.queryTimeout = queryTimeout;
//        // must reset the cancel at here,
//        // otherwise it is still used
//        this.cancelAtNs = 0;
//    }
//
//    public int getQueryTimeout() {
//        return queryTimeout;
//    }
//
//    /**
//     * Set the table this session is waiting for, and the thread that is
//     * waiting.
//     *
//     * @param waitForLock the table
//     * @param waitForLockThread the current thread (the one that is waiting)
//     */
//    public void setWaitForLock(Table waitForLock, Thread waitForLockThread) {
//        this.waitForLock = waitForLock;
//        this.waitForLockThread = waitForLockThread;
//    }
//
//    public Table getWaitForLock() {
//        return waitForLock;
//    }
//
//    public Thread getWaitForLockThread() {
//        return waitForLockThread;
//    }
//
//    public int getModificationId() {
//        return modificationId;
//    }
//
//    public Value getTransactionId() {
//        if (database.getStore() != null) {
//            if (transaction == null || !transaction.hasChanges()) {
//                return ValueNull.INSTANCE;
//            }
//            return ValueString.get(Long.toString(getTransaction().getSequenceNum()));
//        }
//        if (!database.isPersistent()) {
//            return ValueNull.INSTANCE;
//        }
//        if (undoLog == null || undoLog.size() == 0) {
//            return ValueNull.INSTANCE;
//        }
//        return ValueString.get(firstUncommittedLog + "-" + firstUncommittedPos +
//                "-" + id);
//    }
//
//    /**
//     * Get the next object id.
//     *
//     * @return the next object id
//     */
//    public int nextObjectId() {
//        return objectId++;
//    }
//
//    public boolean isRedoLogBinaryEnabled() {
//        return redoLogBinary;
//    }
//
//    /**
//     * Get the transaction to use for this session.
//     *
//     * @return the transaction
//     */
//    public Transaction getTransaction() {
//        if (transaction == null) {
//            MVTableEngine.Store store = database.getStore();
//            if (store != null) {
//                if (store.getMvStore().isClosed()) {
//                    Throwable backgroundException = database.getBackgroundException();
//                    database.shutdownImmediately();
//                    throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, backgroundException);
//                }
//                transaction = store.getTransactionStore().begin(this, this.lockTimeout, id);
//                transaction.setIsolationLevel(isolationLevel);
//            }
//            startStatement = -1;
//        }
//        return transaction;
//    }
//
//    private long getStatementSavepoint() {
//        if (startStatement == -1) {
//            startStatement = getTransaction().setSavepoint();
//        }
//        return startStatement;
//    }
//
//    /**
//     * Start a new statement within a transaction.
//     * @param command about to be started
//     */
//    @SuppressWarnings("incomplete-switch")
//    public void startStatementWithinTransaction(Command command) {
//        Transaction transaction = getTransaction();
//        if (transaction != null) {
//            HashSet<MVMap<Object,VersionedValue<Object>>> maps = new HashSet<>();
//            if (command != null) {
//                Set<DbObject> dependencies = command.getDependencies();
//                switch (transaction.getIsolationLevel()) {
//                case SNAPSHOT:
//                case SERIALIZABLE:
//                    if (!transaction.hasStatementDependencies()) {
//                        for (Table table : database.getAllTablesAndViews(false)) {
//                            if (table instanceof MVTable) {
//                                addTableToDependencies((MVTable)table, maps);
//                            }
//                        }
//                        break;
//                    }
//                    //$FALL-THROUGH$
//                case READ_COMMITTED:
//                case READ_UNCOMMITTED:
//                    for (DbObject dependency : dependencies) {
//                        if (dependency instanceof MVTable) {
//                            addTableToDependencies((MVTable)dependency, maps);
//                        }
//                    }
//                    break;
//                case REPEATABLE_READ:
//                    HashSet<MVTable> processed = new HashSet<>();
//                    for (DbObject dependency : dependencies) {
//                        if (dependency instanceof MVTable) {
//                            addTableToDependencies((MVTable)dependency, maps, processed);
//                        }
//                    }
//                    break;
//                }
//            }
//            transaction.markStatementStart(maps);
//        }
//        startStatement = -1;
//        if (command != null) {
//            setCurrentCommand(command);
//        }
//    }
//
//    @SuppressWarnings({"unchecked", "rawtypes"})
//    private static void addTableToDependencies(MVTable table, HashSet<MVMap<Object,VersionedValue<Object>>> maps) {
//        for (Index index : table.getIndexes()) {
//            if (index instanceof MVIndex) {
//                maps.add(((MVIndex) index).getMVMap());
//            }
//        }
//    }
//
//    private static void addTableToDependencies(MVTable table, HashSet<MVMap<Object,VersionedValue<Object>>> maps,
//            HashSet<MVTable> processed) {
//        if (!processed.add(table)) {
//            return;
//        }
//        addTableToDependencies(table, maps);
//        ArrayList<Constraint> constraints = table.getConstraints();
//        if (constraints != null) {
//            for (Constraint constraint : constraints) {
//                Table ref = constraint.getTable();
//                if (ref != table && ref instanceof MVTable) {
//                    addTableToDependencies((MVTable) ref, maps, processed);
//                }
//            }
//        }
//    }
//
//    /**
//     * Mark the statement as completed. This also close all temporary result
//     * set, and deletes all temporary files held by the result sets.
//     */
//    public void endStatement() {
//        setCurrentCommand(null);
//        if (transaction != null) {
//            transaction.markStatementEnd();
//        }
//        startStatement = -1;
//        closeTemporaryResults();
//    }
//
//    /**
//     * Clear the view cache for this session.
//     */
//    public void clearViewIndexCache() {
//        viewIndexCache = null;
//    }
//
//    @Override
//    public void addTemporaryLob(Value v) {
//        if (!DataType.isLargeObject(v.getValueType())) {
//            return;
//        }
//        if (v.getTableId() == LobStorageFrontend.TABLE_RESULT
//                || v.getTableId() == LobStorageFrontend.TABLE_TEMP) {
//            if (temporaryResultLobs == null) {
//                temporaryResultLobs = new LinkedList<>();
//            }
//            temporaryResultLobs.add(new TimeoutValue(v));
//        } else {
//            if (temporaryLobs == null) {
//                temporaryLobs = new ArrayList<>();
//            }
//            temporaryLobs.add(v);
//        }
//    }
//
//    @Override
//    public boolean isRemote() {
//        return false;
//    }
//
//    /**
//     * Mark that the given table needs to be analyzed on commit.
//     *
//     * @param table the table
//     */
//    public void markTableForAnalyze(Table table) {
//        if (tablesToAnalyze == null) {
//            tablesToAnalyze = new HashSet<>();
//        }
//        tablesToAnalyze.add(table);
//    }
//
//    public State getState() {
//        return getBlockingSessionId() != 0 ? State.BLOCKED : state.get();
//    }
//
//    public int getBlockingSessionId() {
//        return transaction == null ? 0 : transaction.getBlockerId();
//    }
//
//    @Override
//    public void onRollback(MVMap<Object, VersionedValue<Object>> map, Object key,
//                            VersionedValue<Object> existingValue,
//                            VersionedValue<Object> restoredValue) {
//        // Here we are relying on the fact that map which backs table's primary index
//        // has the same name as the table itself
//        MVTableEngine.Store store = database.getStore();
//        if(store != null) {
//            MVTable table = store.getTable(map.getName());
//            if (table != null) {
//                long recKey = (Long)key;
//                Row oldRow = getRowFromVersionedValue(table, recKey, existingValue);
//                Row newRow = getRowFromVersionedValue(table, recKey, restoredValue);
//                table.fireAfterRow(this, oldRow, newRow, true);
//
//                if (table.getContainsLargeObject()) {
//                    if (oldRow != null) {
//                        for (int i = 0, len = oldRow.getColumnCount(); i < len; i++) {
//                            Value v = oldRow.getValue(i);
//                            if (v.isLinkedToTable()) {
//                                removeAtCommit(v);
//                            }
//                        }
//                    }
//                    if (newRow != null) {
//                        for (int i = 0, len = newRow.getColumnCount(); i < len; i++) {
//                            Value v = newRow.getValue(i);
//                            if (v.isLinkedToTable()) {
//                                removeAtCommitStop(v);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private static Row getRowFromVersionedValue(MVTable table, long recKey,
//                                                VersionedValue<Object> versionedValue) {
//        Object value = versionedValue == null ? null : versionedValue.getCurrentValue();
//        if (value == null) {
//            return null;
//        }
//        Row result;
//        if(value instanceof Row) {
//            result = (Row) value;
//            assert result.getKey() == recKey : result.getKey() + " != " + recKey;
//        } else {
//            result = table.createRow(((ValueArray) value).getList(), 0, recKey);
//        }
//        return result;
//    }
//
//
//    /**
//     * Represents a savepoint (a position in a transaction to where one can roll
//     * back to).
//     */
//    public static class Savepoint {
//
//        /**
//         * The undo log index.
//         */
//        int logIndex;
//
//        /**
//         * The transaction savepoint id.
//         */
//        long transactionSavepoint;
//    }
//
//    /**
//     * An object with a timeout.
//     */
//    public static class TimeoutValue {
//
//        /**
//         * The time when this object was created.
//         */
//        final long created = System.nanoTime();
//
//        /**
//         * The value.
//         */
//        final Value value;
//
//        TimeoutValue(Value v) {
//            this.value = v;
//        }
//
//    }
//
//    public ColumnNamerConfiguration getColumnNamerConfiguration() {
//        return columnNamerConfiguration;
//    }
//
//    public void setColumnNamerConfiguration(ColumnNamerConfiguration columnNamerConfiguration) {
//        this.columnNamerConfiguration = columnNamerConfiguration;
//    }
//
//    @Override
//    public boolean isSupportsGeneratedKeys() {
//        return true;
//    }
//
//    /**
//     * Returns the network connection information, or {@code null}.
//     *
//     * @return the network connection information, or {@code null}
//     */
//    public NetworkConnectionInfo getNetworkConnectionInfo() {
//        return networkConnectionInfo;
//    }
//
//    @Override
//    public void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo) {
//        this.networkConnectionInfo = networkConnectionInfo;
//    }
//
//    @Override
//    public ValueTimestampTimeZone currentTimestamp() {
//        return database.getMode().dateTimeValueWithinTransaction ? getTransactionStart() : getCurrentCommandStart();
//    }
//
//    @Override
//    public Mode getMode() {
//        return database.getMode();
//    }
//
//    @Override
//    public IsolationLevel getIsolationLevel() {
//        if (database.isMVStore()) {
//            return isolationLevel;
//        } else {
//            return IsolationLevel.fromLockMode(database.getLockMode());
//        }
//    }
//
//    @Override
//    public void setIsolationLevel(IsolationLevel isolationLevel) {
//        commit(false);
//        if (database.isMVStore()) {
//            this.isolationLevel = isolationLevel;
//        } else {
//            int lockMode = isolationLevel.getLockMode();
//            org.h2.command.dml.Set set = new org.h2.command.dml.Set(this, SetTypes.LOCK_MODE);
//            set.setInt(lockMode);
//            synchronized (database) {
//                set.update();
//            }
//        }
//    }
//
//    /**
//     * Gets bit set of non-keywords.
//     *
//     * @return set of non-keywords, or {@code null}
//     */
//    public BitSet getNonKeywords() {
//        return nonKeywords;
//    }
//
//    /**
//     * Sets bit set of non-keywords.
//     *
//     * @param nonKeywords set of non-keywords, or {@code null}
//     */
//    public void setNonKeywords(BitSet nonKeywords) {
//        this.nonKeywords = nonKeywords;
//    }
//
//    @Override
//    public StaticSettings getStaticSettings() {
//        StaticSettings settings = staticSettings;
//        if (settings == null) {
//            DbSettings dbSettings = database.getSettings();
//            staticSettings = settings = new StaticSettings(dbSettings.databaseToUpper, dbSettings.databaseToLower,
//                    dbSettings.caseInsensitiveIdentifiers);
//        }
//        return settings;
//    }
//
//    @Override
//    public DynamicSettings getDynamicSettings() {
//        return new DynamicSettings(database.getMode(), timeZone);
//    }
//
//    @Override
//    public TimeZoneProvider currentTimeZone() {
//        return timeZone;
//    }
//
//    /**
//     * Sets current time zone.
//     *
//     * @param timeZone time zone
//     */
//    public void setTimeZone(TimeZoneProvider timeZone) {
//        if (!timeZone.equals(this.timeZone)) {
//            this.timeZone = timeZone;
//            ValueTimestampTimeZone ts = transactionStart;
//            if (ts != null) {
//                transactionStart = moveTimestamp(ts, timeZone);
//            }
//            ts = currentCommandStart;
//            if (ts != null) {
//                currentCommandStart = moveTimestamp(ts, timeZone);
//            }
//            modificationId++;
//        }
//    }
//
//    private static ValueTimestampTimeZone moveTimestamp(ValueTimestampTimeZone timestamp, TimeZoneProvider timeZone) {
//        long dateValue = timestamp.getDateValue();
//        long timeNanos = timestamp.getTimeNanos();
//        int offsetSeconds = timestamp.getTimeZoneOffsetSeconds();
//        return DateTimeUtils.timestampTimeZoneAtOffset(dateValue, timeNanos, offsetSeconds,
//                timeZone.getTimeZoneOffsetUTC(DateTimeUtils.getEpochSeconds(dateValue, timeNanos, offsetSeconds)));
//    }
//
//    /**
//     * Check if two values are equal with the current comparison mode.
//     *
//     * @param a the first value
//     * @param b the second value
//     * @return true if both objects are equal
//     */
//    public boolean areEqual(Value a, Value b) {
//        // can not use equals because ValueDecimal 0.0 is not equal to 0.00.
//        return a.compareTo(b, this, database.getCompareMode()) == 0;
//    }
//
//    /**
//     * Compare two values with the current comparison mode. The values may have
//     * different data types including NULL.
//     *
//     * @param a the first value
//     * @param b the second value
//     * @return 0 if both values are equal, -1 if the first value is smaller, and
//     *         1 otherwise
//     */
//    public int compare(Value a, Value b) {
//        return a.compareTo(b, this, database.getCompareMode());
//    }
//
//    /**
//     * Compare two values with the current comparison mode. The values may have
//     * different data types including NULL.
//     *
//     * @param a the first value
//     * @param b the second value
//     * @param forEquality perform only check for equality (= or &lt;&gt;)
//     * @return 0 if both values are equal, -1 if the first value is smaller, 1
//     *         if the second value is larger, {@link Integer#MIN_VALUE} if order
//     *         is not defined due to NULL comparison
//     */
//    public int compareWithNull(Value a, Value b, boolean forEquality) {
//        return a.compareWithNull(b, forEquality, this, database.getCompareMode());
//    }
//
//    /**
//     * Compare two values with the current comparison mode. The values must be
//     * of the same type.
//     *
//     * @param a the first value
//     * @param b the second value
//     * @return 0 if both values are equal, -1 if the first value is smaller, and
//     *         1 otherwise
//     */
//    public int compareTypeSafe(Value a, Value b) {
//        return a.compareTypeSafe(b, database.getCompareMode(), this);
//=======
//        sessionStateChanged = false;
//        sessionState = Utils.newSmallArrayList();
//        CommandInterface ci = prepareCommand(!isOldInformationSchema()
//                ? "SELECT STATE_COMMAND FROM INFORMATION_SCHEMA.SESSION_STATE"
//                : "SELECT SQL FROM INFORMATION_SCHEMA.SESSION_STATE", Integer.MAX_VALUE);
//        ResultInterface result = ci.executeQuery(0, false);
//        while (result.next()) {
//            sessionState.add(result.currentRow()[0].getString());
//        }
//>>>>>>> 5a91cf068195d0e613d30e0e7202a0b05f87f253
//    }
//
//}
