/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command;

import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.constant.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.ResultInterface;
import org.h2.util.MathUtils;

/**
 * Represents a SQL statement. This object is only used on the server side.
 */
public abstract class Command implements CommandInterface {

    /**
     * The session.
     */
    protected final Session session;

    /**
     * The last start time.
     */
    protected long startTime;

    /**
     * The trace module.
     */
    private final Trace trace;

    /**
     * If this query was canceled.
     */
    private volatile boolean cancel;

    private final String sql;

    private boolean canReuse;

    Command(Parser parser, String sql) {
        this.session = parser.getSession();
        this.sql = sql;
        trace = session.getDatabase().getTrace(Trace.COMMAND);
    }

    /**
     * Check if this command is transactional.
     * If it is not, then it forces the current transaction to commit.
     *
     * @return true if it is
     */
    public abstract boolean isTransactional();

    /**
     * Check if this command is a query.
     *
     * @return true if it is
     */
    public abstract boolean isQuery();

    /**
     * Get the list of parameters.
     *
     * @return the list of parameters
     */
    public abstract ArrayList<? extends ParameterInterface> getParameters();

    /**
     * Check if this command is read only.
     *
     * @return true if it is
     */
    public abstract boolean isReadOnly();

    /**
     * Get an empty result set containing the meta data.
     *
     * @return an empty result set
     */
    public abstract ResultInterface queryMeta(); //就是结果集元数据，对应于java.sql.ResultSetMetaData

    /**
     * Execute an updating statement, if this is possible.
     *
     * @return the update count
     * @throws DbException if the command is not an updating statement
     */
    public int update() { //子类要实现这个方法
        throw DbException.get(ErrorCode.METHOD_NOT_ALLOWED_FOR_QUERY);
    }

    /**
     * Execute a query statement, if this is possible.
     *
     * @param maxrows the maximum number of rows returned
     * @return the local result set
     * @throws DbException if the command is not a query
     */
    public ResultInterface query(int maxrows) { //子类要实现这个方法
        throw DbException.get(ErrorCode.METHOD_ONLY_ALLOWED_FOR_QUERY);
    }

    public final ResultInterface getMetaData() {
        return queryMeta();
    }

    /**
     * Start the stopwatch.
     */
    void start() {
        if (trace.isInfoEnabled()) {
            startTime = System.currentTimeMillis();
        }
    }

    void setProgress(int state) {
        session.getDatabase().setProgress(state, sql, 0, 0);
    }

    /**
     * Check if this command has been canceled, and throw an exception if yes.
     *
     * @throws DbException if the statement has been canceled
     */
    protected void checkCanceled() {
        if (cancel) {
            cancel = false;
            throw DbException.get(ErrorCode.STATEMENT_WAS_CANCELED);
        }
    }

    private void stop() {
        session.closeTemporaryResults();
        session.setCurrentCommand(null);
        //DDL的isTransactional默认都是false，相当于每执行完一条DDL都默认提交事务
        if (!isTransactional()) {
            session.commit(true);
        } else if (session.getAutoCommit()) { //如果是自动提交模式，那么执行完一条SQL时由系统自动提交，非自动提交模式由应用负责提交
            session.commit(false);
        } else if (session.getDatabase().isMultiThreaded()) {
            Database db = session.getDatabase();
            if (db != null) {
                if (db.getLockMode() == Constants.LOCK_MODE_READ_COMMITTED) {
                    session.unlockReadLocks();
                }
            }
        }
        if (trace.isInfoEnabled() && startTime > 0) {
            long time = System.currentTimeMillis() - startTime;
            if (time > Constants.SLOW_QUERY_LIMIT_MS) { //如果一条sql的执行时间小于100毫秒，记下它
                //trace.info("slow query: {0} ms", time);
            	trace.info("slow query: {0} ms, sql: {1}", time, sql); //我加上的
            }
        }
    }

    /**
     * Execute a query and return the result.
     * This method prepares everything and calls {@link #query(int)} finally.
     *
     * @param maxrows the maximum number of rows to return
     * @param scrollable if the result set must be scrollable (ignored)
     * @return the result set
     */
    public ResultInterface executeQuery(int maxrows, boolean scrollable) {
        startTime = 0;
        long start = 0;
        Database database = session.getDatabase();
        //也跟executeUpdate()的情型一样，就算是查询也不例外
        Object sync = database.isMultiThreaded() ? (Object) session : (Object) database;
        session.waitIfExclusiveModeEnabled();
        boolean writing = !isReadOnly();
        if (writing) {
            while (!database.beforeWriting()) {
                // wait
            }
        }
        synchronized (sync) {
            session.setCurrentCommand(this);
            try {
                while (true) {
                    database.checkPowerOff();
                    try {
                        return query(maxrows);
                    } catch (DbException e) {
                        start = filterConcurrentUpdate(e, start);
                    } catch (Throwable e) {
                        throw DbException.convert(e);
                    }
                }
            } catch (DbException e) {
                e.addSQL(sql);
                database.exceptionThrown(e.getSQLException(), sql);
                throw e;
            } finally {
                stop();
                if (writing) {
                    database.afterWriting();
                }
            }
        }
    }

    public int executeUpdate() {
        long start = 0;
        Database database = session.getDatabase();
        //默认一个数据库只允许一个线程更新，通过SET MULTI_THREADED 1可变成多线程的，
        //这样同步对象是session，即不同的session之间可以并发使用数据库，但是同一个session内部是只允许一个线程。
        //通过使用database作为同步对象就相当于数据库是单线程的
        Object sync = database.isMultiThreaded() ? (Object) session : (Object) database;
        session.waitIfExclusiveModeEnabled();
        boolean callStop = true;
        boolean writing = !isReadOnly();
        if (writing) {
            while (!database.beforeWriting()) {
                // wait
            }
        }
        synchronized (sync) {
            int rollback = session.getUndoLogPos(); //记下日志位置，以便失败时回退
            session.setCurrentCommand(this);
            try {
                while (true) {
                    database.checkPowerOff();
                    try {
                        return update();
                    } catch (DbException e) {
                        start = filterConcurrentUpdate(e, start);
                    } catch (Throwable e) {
                        throw DbException.convert(e);
                    }
                }
            } catch (DbException e) {
                e = e.addSQL(sql);
                SQLException s = e.getSQLException();
                database.exceptionThrown(s, sql);
                database.checkPowerOff();
                if (s.getErrorCode() == ErrorCode.DEADLOCK_1) {
                    session.rollback();
                } else if (s.getErrorCode() == ErrorCode.OUT_OF_MEMORY) {
                    // there is a serious problem:
                    // the transaction may be applied partially
                    // in this case we need to panic:
                    // close the database
                    callStop = false;
                    database.shutdownImmediately();
                    throw e;
                } else {
                    session.rollbackTo(rollback, false);
                }
                throw e;
            } finally {
                try {
                    if (callStop) {
                        stop();
                    }
                } finally {
                    if (writing) {
                        database.afterWriting();
                    }
                }
            }
        }
    }

    private long filterConcurrentUpdate(DbException e, long start) {
        if (e.getErrorCode() != ErrorCode.CONCURRENT_UPDATE_1) {
            throw e;
        }
        long now = System.nanoTime() / 1000000;
        if (start != 0 && now - start > session.getLockTimeout()) {
            throw DbException.get(ErrorCode.LOCK_TIMEOUT_1, e.getCause(), "");
        }
        Database database = session.getDatabase();
        int sleep = 1 + MathUtils.randomInt(10);
        while (true) {
            try {
                if (database.isMultiThreaded()) {
                    Thread.sleep(sleep);
                } else {
                    database.wait(sleep);
                }
            } catch (InterruptedException e1) {
                // ignore
            }
            long slept = System.nanoTime() / 1000000 - now;
            if (slept >= sleep) {
                break;
            }
        }
        return start == 0 ? now : start;
    }

    public void close() { //命令关闭后才可重用
        canReuse = true;
    }

    public void cancel() {
        this.cancel = true;
    }

    public String toString() {
        return sql + Trace.formatParams(getParameters());
    }

    public boolean isCacheable() { //子类CommandContainer覆盖了
        return false;
    }

    /**
     * Whether the command is already closed (in which case it can be re-used).
     *
     * @return true if it can be re-used
     */
    public boolean canReuse() {
        return canReuse;
    }

    /**
     * The command is now re-used, therefore reset the canReuse flag, and the
     * parameter values.
     */
    public void reuse() {
        canReuse = false;
        ArrayList<? extends ParameterInterface> parameters = getParameters();
        for (int i = 0, size = parameters.size(); i < size; i++) {
            ParameterInterface param = parameters.get(i);
            param.setValue(null, true); //置null并关闭之前的值，关闭这个操作只是针对lob值的情况，见org.h2.value.Value.close()
        }
    }

}
