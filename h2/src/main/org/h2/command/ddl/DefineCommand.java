/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.result.ResultInterface;

/**
 * This class represents a non-transaction statement, for example a CREATE or
 * DROP.
 */
public abstract class DefineCommand extends Prepared {

    /**
     * The transactional behavior. The default is disabled, meaning the command
     * commits an open transaction.
     */
    protected boolean transactional;

    /**
     * Create a new command for the given session.
     *
     * @param session the session
     */
    DefineCommand(Session session) {
        super(session);
    }

    @Override
    public boolean isReadOnly() { //无子类覆盖
        return false;
    }

    @Override
    public ResultInterface queryMeta() { //无子类覆盖

        return null;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    //在org.h2.command.Command.stop()调用org.h2.command.CommandContainer.isTransactional()
    //然后调用此方法，默认是false，相当于此DDL不在一个事务中，每次执行此DDL时都默认自动提交事务
    @Override
    public boolean isTransactional() { 
        return transactional;
    }

}
