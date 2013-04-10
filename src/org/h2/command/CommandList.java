/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command;

import java.util.ArrayList;
import org.h2.expression.ParameterInterface;
import org.h2.result.ResultInterface;

/**
 * Represents a list of SQL statements.
 */
class CommandList extends Command {

    private final Command command;
    private final String remaining;

    CommandList(Parser parser, String sql, Command c, String remaining) {
        super(parser, sql);
        this.command = c;
        this.remaining = remaining;
    }

    public ArrayList<? extends ParameterInterface> getParameters() {
        return command.getParameters();
    }
    
    //sql = "select id,name from ParserTest;select id,name from ParserTest;select id,name from ParserTest";
    //那么先执行第一个，此时remainingCommand是CommandList(后面两个select)，然后继续CommandList.query，
    //此时remainingCommand是CommandContainer
    //总之路径是这样CommandList->CommandList->CommandContainer
    //注意session.prepareLocal里是会重新new新的Parser
    private void executeRemaining() {
        Command remainingCommand = session.prepareLocal(remaining);
        if (remainingCommand.isQuery()) {
            remainingCommand.query(0);
        } else {
            remainingCommand.update();
        }
    }

    public int update() {
        int updateCount = command.executeUpdate();
        executeRemaining();
        return updateCount;
    }

    public ResultInterface query(int maxrows) {
        ResultInterface result = command.query(maxrows);
        executeRemaining();
        return result;
    }

    public boolean isQuery() {
        return command.isQuery();
    }

    public boolean isTransactional() {
        return true;
    }

    public boolean isReadOnly() {
        return false;
    }

    public ResultInterface queryMeta() {
        return command.queryMeta();
    }

    public int getCommandType() {
        return command.getCommandType();
    }

}
