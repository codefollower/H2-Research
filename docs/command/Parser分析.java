此类的public方法只有下面这些:

static类:

org.h2.command.Parser.isKeyword(String, boolean)
org.h2.command.Parser.quoteIdentifier(String)

非static类:
org.h2.command.Parser.Parser(Session)
org.h2.command.Parser.getSession()
org.h2.command.Parser.parseExpression(String)
org.h2.command.Parser.prepare(String)
org.h2.command.Parser.prepareCommand(String)
org.h2.command.Parser.setRightsChecked(boolean)

包级别的方法只有一个:
org.h2.command.Parser.parse(String) //除此类外，只看到在org.h2.command.CommandContainer.recompileIfRequired()中使用


org.h2.command.Parser.parse(String) 是起点

由下面两者触发

org.h2.command.Parser.parse(String)

	<= org.h2.command.Parser.prepare(String) (得到一个Prepared)
		<= org.h2.engine.Session.prepare(String, boolean) 在起动时调用，用于非远程客户端发起的调用，org.h2.engine.MetaRecord.execute(Database, Session, DatabaseEventListener)

	(得到一个CommandContainer，CommandContainer有Prepared，Prepared也有对CommandContainer的引用)
	<= org.h2.command.Parser.prepareCommand(String)
		<= org.h2.engine.Session.prepareLocal(String) 远程客户端发起的调用，见org.h2.server.TcpServerThread.process()

上面两者都会调用Prepared.prepare()


流程分析:
org.h2.command.Parser.parse(String)
	=> org.h2.command.Parser.parse(String, boolean) 只在parse(String)中调用
		=> org.h2.command.Parser.initialize(String)
		=> org.h2.command.Parser.read()
		=> org.h2.command.Parser.parsePrepared() 核心在这里

    /**
     * Parse the statement, but don't prepare it for execution.
     *
     * @param sql the SQL statement to parse
     * @return the prepared object
     */
    Prepared parse(String sql) {
        Prepared p;
        try {
            // first, try the fast variant
        	//大多数情况下SQL都是正确的，所以这里做了些优化: 默认不使用expectedList，当出现错误时捕获DbException，
			//如果是语法错误那么再解析一次，并用expectedList记录SQL在语法层面缺了哪些东西，
			//如果是非语法错误，则把SQL关联到异常，直接抛出异常。
        	//如果不起用这个优化，那么因为频繁调用readIf->addExpected会导致expectedList变得很大
            p = parse(sql, false);
        } catch (DbException e) {
            if (e.getErrorCode() == ErrorCode.SYNTAX_ERROR_1) {
                // now, get the detailed exception
                p = parse(sql, true);
            } else {
                throw e.addSQL(sql);
            }
        }
        p.setPrepareAlways(recompileAlways);
        p.setParameterList(parameters);
        return p;
    }

    private Prepared parse(String sql, boolean withExpectedList) {
        initialize(sql);
        if (withExpectedList) {
            expectedList = New.arrayList();
        } else {
            expectedList = null;
        }
        parameters = New.arrayList();
        currentSelect = null;
        currentPrepared = null;
        createView = null;
        recompileAlways = false;
        indexedParameterList = null;
        read();
        return parsePrepared();
    }


org.h2.command.Parser.initialize(String)
提前分析sql语句中的每个字符，比如把注释替换成空格



