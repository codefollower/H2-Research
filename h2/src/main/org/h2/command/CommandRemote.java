/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command;

import java.io.IOException;
import java.util.ArrayList;

import org.h2.engine.Constants;
import org.h2.engine.GeneratedKeysMode;
import org.h2.engine.SessionRemote;
import org.h2.engine.SysProperties;
import org.h2.expression.ParameterInterface;
import org.h2.expression.ParameterRemote;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.ResultInterface;
import org.h2.result.ResultRemote;
import org.h2.result.ResultWithGeneratedKeys;
import org.h2.util.Utils;
import org.h2.value.Transfer;
import org.h2.value.Value;
import org.h2.value.ValueNull;

/**
 * Represents the client-side part of a SQL statement.
 * This class is not used in embedded mode.
 */
public class CommandRemote implements CommandInterface {

    private final ArrayList<Transfer> transferList;
    private final ArrayList<ParameterInterface> parameters;
    private final Trace trace;
    private final String sql;
    private final int fetchSize;
    private SessionRemote session;
    private int id;
    private boolean isQuery;
    private int cmdType = UNKNOWN;
    private boolean readonly;
    private final int created;

    public CommandRemote(SessionRemote session,
            ArrayList<Transfer> transferList, String sql, int fetchSize) {
        this.transferList = transferList;
        trace = session.getTrace();
        this.sql = sql;
        parameters = Utils.newSmallArrayList();
        prepare(session, true);
        // set session late because prepare might fail - in this case we don't
        // need to close the object
        this.session = session;
        this.fetchSize = fetchSize;
        created = session.getLastReconnect();
    }

    @Override
    public void stop() {
        // Must never be called, because remote result is not lazy.
        throw DbException.throwInternalError();
    }

    private void prepare(SessionRemote s, boolean createParams) {
    	//虽然getNextId内部是nextId++;
    	//但是org.h2.engine.SessionRemote.prepareCommand(String, int)是synchronized的，
    	//
    	//另外org.h2.command.CommandRemote.prepareIfRequired()也在synchronized (session)中调用
    	//所以这里没有并发问题
    	
        id = s.getNextId(); //这个id会发往server，用于缓存server对应的command。
        for (int i = 0, count = 0; i < transferList.size(); i++) {
            try {
                Transfer transfer = transferList.get(i);

                boolean v16 = s.getClientVersion() >= Constants.TCP_PROTOCOL_VERSION_16;

                if (createParams) {
                    s.traceOperation(v16 ? "SESSION_PREPARE_READ_PARAMS2"
                            : "SESSION_PREPARE_READ_PARAMS", id);
                    transfer.writeInt(
                            v16 ? SessionRemote.SESSION_PREPARE_READ_PARAMS2
                                    : SessionRemote.SESSION_PREPARE_READ_PARAMS)
                            .writeInt(id).writeString(sql);
                } else {
                    s.traceOperation("SESSION_PREPARE", id);
                    transfer.writeInt(SessionRemote.SESSION_PREPARE).
                        writeInt(id).writeString(sql);
                }
                s.done(transfer);
                isQuery = transfer.readBoolean();
                readonly = transfer.readBoolean();

                cmdType = v16 && createParams ? transfer.readInt() : UNKNOWN;

                int paramCount = transfer.readInt();
                if (createParams) {
                    parameters.clear();
                    //prepare阶段每个ParameterRemote只有类型还没有值，会在接下来通过getParameters()传给JdbcPreparedStatement
                    //然后在JdbcPreparedStatement中设置，如果在executeQuery和executeUpdate中还没有为这些参数设置值，
                    //那么调用checkParameters时会抛异常
                    for (int j = 0; j < paramCount; j++) {
                        ParameterRemote p = new ParameterRemote(j);
                        p.readMetaData(transfer);
                        parameters.add(p);
                    }
                }
            } catch (IOException e) {
                s.removeServer(e, i--, ++count);
            }
        }
    }

    @Override
    public boolean isQuery() {
        return isQuery;
    }

    @Override
    public ArrayList<ParameterInterface> getParameters() {
        return parameters;
    }

    private void prepareIfRequired() {
        if (session.getLastReconnect() != created) {
            // in this case we need to prepare again in every case
            id = Integer.MIN_VALUE;
        }
        session.checkClosed();
        if (id <= session.getCurrentId() - SysProperties.SERVER_CACHED_OBJECTS) {
            // object is too old - we need to prepare again
            prepare(session, false);
        }
    }
    
    //这个并不是对应java.sql.ResultSetMetaData，而是在org.h2.jdbc.JdbcPreparedStatement.getMetaData()调用
    //等价于ResultSet.getMetaData()，只不过PreparedStatement.getMetaData()不需要事先执行查询
    @Override
    public ResultInterface getMetaData() {
        synchronized (session) {
            if (!isQuery) {
                return null;
            }
            int objectId = session.getNextId();
            ResultRemote result = null;
            for (int i = 0, count = 0; i < transferList.size(); i++) {
                prepareIfRequired();
                Transfer transfer = transferList.get(i);
                try {
                    session.traceOperation("COMMAND_GET_META_DATA", id);
                    //这里得到的ResultRemote也会在server端按objectId缓存一个org.h2.result.LocalResult
                    //但是这个ResultRemote在org.h2.jdbc.JdbcPreparedStatement.getMetaData()中被封装到JdbcResultSetMetaData后
                    //没有机会调用ResultRemote.close来释放server端的相关东西了
                    //这个影响有多大?不过server端的cache是有限制的，并且按session来配置，所以不至于造成OOM
                    //==========================
                    //上面理解错了，ResultRemote的构造函数中会触发ResultRemote.fetchRows(boolean)，
                    //因为rowCount是0，所以当下就调用sendClose了
                    transfer.writeInt(SessionRemote.COMMAND_GET_META_DATA).writeInt(id).writeInt(objectId);
                    session.done(transfer);
                    int columnCount = transfer.readInt();
                    result = new ResultRemote(session, transfer, objectId,
                            columnCount, Integer.MAX_VALUE);
                    break;
                } catch (IOException e) {
                    session.removeServer(e, i--, ++count);
                }
            }
            session.autoCommitIfCluster();
            return result;
        }
    }

    @Override
    public ResultInterface executeQuery(int maxRows, boolean scrollable) {
        checkParameters();
        synchronized (session) {
            int objectId = session.getNextId();
            ResultRemote result = null;
            for (int i = 0, count = 0; i < transferList.size(); i++) {
                prepareIfRequired();
                Transfer transfer = transferList.get(i);
                try {
                    session.traceOperation("COMMAND_EXECUTE_QUERY", id);
                    transfer.writeInt(SessionRemote.COMMAND_EXECUTE_QUERY).
                        writeInt(id).writeInt(objectId).writeInt(maxRows);
                    int fetch;
                    if (session.isClustered() || scrollable) {
                        fetch = Integer.MAX_VALUE;
                    } else {
                        fetch = fetchSize;
                    }
                    transfer.writeInt(fetch);
                    //如果是JdbcStatement，没有参数，JdbcPreparedStatement才有
                    sendParameters(transfer);
                    session.done(transfer);
                    int columnCount = transfer.readInt();
                    if (result != null) {
                        result.close();
                        result = null;
                    }
                    result = new ResultRemote(session, transfer, objectId, columnCount, fetch);
                    //对于只读查询只需要从一台server上获得结果就可以了，不需要每台server都查询一次
                    //select ... for update并不能使readonly为false，
                    //相反，在where中加上一些NotDeterministic的函数，例如RAND、RANDOM等反而能使readonly为false
                    //见org.h2.expression.Function.addFunctionNotDeterministic(String, int, int, int)
                    //例子: select * from SessionRemoteTest where id>? and b=? and id<RAND()
                    if (readonly) {
                        break;
                    }
                } catch (IOException e) {
                    session.removeServer(e, i--, ++count);
                }
            }
            session.autoCommitIfCluster();
            session.readSessionState();
            return result;
        }
    }
    
    //注意: transferList.size大于1时，说明是集群环境，但是并不是XA，也就是说可能有一台server更新成功了，可以允许另一台更新不成功
    @Override
    public ResultWithGeneratedKeys executeUpdate(Object generatedKeysRequest) {
        checkParameters();
        boolean supportsGeneratedKeys = session.isSupportsGeneratedKeys();
        int generatedKeysMode = GeneratedKeysMode.valueOf(generatedKeysRequest);
        boolean readGeneratedKeys = supportsGeneratedKeys && generatedKeysMode != GeneratedKeysMode.NONE;
        int objectId = readGeneratedKeys ? session.getNextId() : 0;
        synchronized (session) {
            int updateCount = 0;
            ResultRemote generatedKeys = null;
            boolean autoCommit = false;
            for (int i = 0, count = 0; i < transferList.size(); i++) {
                prepareIfRequired();
                Transfer transfer = transferList.get(i);
                try {
                    session.traceOperation("COMMAND_EXECUTE_UPDATE", id);
                    transfer.writeInt(SessionRemote.COMMAND_EXECUTE_UPDATE).writeInt(id);
                    //如果是JdbcStatement，没有参数，JdbcPreparedStatement才有
                    sendParameters(transfer);
                    if (supportsGeneratedKeys) {
                        transfer.writeInt(generatedKeysMode);
                        switch (generatedKeysMode) {
                        case GeneratedKeysMode.COLUMN_NUMBERS: {
                            int[] keys = (int[]) generatedKeysRequest;
                            transfer.writeInt(keys.length);
                            for (int key : keys) {
                                transfer.writeInt(key);
                            }
                            break;
                        }
                        case GeneratedKeysMode.COLUMN_NAMES: {
                            String[] keys = (String[]) generatedKeysRequest;
                            transfer.writeInt(keys.length);
                            for (String key : keys) {
                                transfer.writeString(key);
                            }
                            break;
                        }
                        }
                    }
                    session.done(transfer);
                    updateCount = transfer.readInt();
                    autoCommit = transfer.readBoolean();
                    if (readGeneratedKeys) {
                        int columnCount = transfer.readInt();
                        if (generatedKeys != null) {
                            generatedKeys.close();
                            generatedKeys = null;
                        }
                        generatedKeys = new ResultRemote(session, transfer, objectId, columnCount, Integer.MAX_VALUE);
                    }
                } catch (IOException e) {
                	//只有所有server都出错时才尝试重连(不过要取决于各种参数)
                    session.removeServer(e, i--, ++count);
                }
            }
//<<<<<<< HEAD
//            session.setAutoCommitFromServer(autoCommit); //如果是集群环境，设为false
//            session.autoCommitIfCluster(); //如果是集群环境，通知所有server提交事务
//            session.readSessionState();//当session状态发生改变时，提取INFORMATION_SCHEMA.SESSION_STATE信息，下次可重建session
//            return updateCount;
//=======
            session.setAutoCommitFromServer(autoCommit);
            session.autoCommitIfCluster();
            session.readSessionState();
            if (generatedKeys != null) {
                return new ResultWithGeneratedKeys.WithKeys(updateCount, generatedKeys);
            }
            return ResultWithGeneratedKeys.of(updateCount);
        }
    }

    private void checkParameters() {
        if (cmdType != EXPLAIN) {
            for (ParameterInterface p : parameters) {
                p.checkSet();
            }
        }
    }

    private void sendParameters(Transfer transfer) throws IOException {
        int len = parameters.size();
        transfer.writeInt(len);
        for (ParameterInterface p : parameters) {
            Value pVal = p.getParamValue();

            if (pVal == null && cmdType == EXPLAIN) {
                pVal = ValueNull.INSTANCE;
            }

            transfer.writeValue(pVal);
        }
    }

    @Override
    public void close() {
        if (session == null || session.isClosed()) {
            return;
        }
        synchronized (session) {
            session.traceOperation("COMMAND_CLOSE", id);
            for (Transfer transfer : transferList) {
                try {
                    //transfer没有立刻flush输出流，可能是考虑到COMMAND_CLOSE命令不需要server端响应，
                    //也考虑到不立刻flush输出流能一定程度上提高调用close()方法的性能，
                    //延迟到下一次执行query、update或关闭session时再一并flush出去
                    transfer.writeInt(SessionRemote.COMMAND_CLOSE).writeInt(id);
                } catch (IOException e) {
                    trace.error(e, "close");
                }
            }
        }
        session = null;
        try {
            for (ParameterInterface p : parameters) {
                Value v = p.getParamValue();
                if (v != null) {
                    v.remove(); //只对ValueLob、ValueLobDb有用
                }
            }
        } catch (DbException e) {
            trace.error(e, "close");
        }
        parameters.clear();
    }

    /**
     * Cancel this current statement.
     */
    @Override
    public void cancel() {
        session.cancelStatement(id);
    }

    @Override
    public String toString() {
        return sql + Trace.formatParams(getParameters());
    }

    @Override
    public int getCommandType() {
        return cmdType;
    }

}
