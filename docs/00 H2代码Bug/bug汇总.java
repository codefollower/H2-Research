1. DECODE 函数没有返回第一个匹配的值，与文档描述不符合

文档: 
Returns the first matching value. NULL is considered to match NULL. 
If no match was found, then NULL or the last parameter (if the parameter count is even) is returned. 
This function is provided for Oracle compatibility (see there for details).

Example:

CALL DECODE(RAND()>0.5, 0, 'Red', 1, 'Black');


重现:
SELECT DECODE(0, 0, 'v1', 0, 'v2', 1, 'v3', 1, 'v4')

正确应返回v1
但是返回了v2

修复:
org.h2.expression.Function.getSimpleValue(Session, Value, Expression[], Value[])

		case DECODE: {
            int index = -1;
            for (int i = 1; i < args.length - 1; i += 2) {
                if (database.areEqual(v0, getNullOrValue(session, args, values, i))) {
                    index = i + 1;
					break; //在这里加上这一行
                }
            }
            if (index < 0 && args.length % 2 == 0) {
                index = args.length - 1;
            }
            Value v = index < 0 ? ValueNull.INSTANCE : getNullOrValue(session, args, values, index);
            result = v.convertTo(dataType);
            break;
        }



2. 
sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int NULL_TO_DEFAULT SEQUENCE myseq) ";
stmt.executeUpdate(sql);
stmt.executeUpdate("insert into mytable(f1) values(1)");
stmt.executeUpdate("insert into mytable(f1) values(2)");

Exception in thread "main" org.h2.jdbc.JdbcSQLException: General error: "java.lang.NullPointerException"; SQL statement:
insert into mytable(f1) values(1) [50000-170]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:329)
	at org.h2.message.DbException.get(DbException.java:158)
	at org.h2.message.DbException.convert(DbException.java:281)
	at org.h2.command.Command.executeUpdate(Command.java:240)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:350)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:158)
	at java.lang.Thread.run(Thread.java:662)
Caused by: java.lang.NullPointerException
	at org.h2.table.Column.validateConvertUpdateSequence(Column.java:276)
	at org.h2.table.Table.validateConvertUpdateSequence(Table.java:698)
	at org.h2.command.dml.Insert.insertRows(Insert.java:120)
	at org.h2.command.dml.Insert.update(Insert.java:84)
	at org.h2.command.CommandContainer.update(CommandContainer.java:76)
	at org.h2.command.Command.executeUpdate(Command.java:236)
	... 3 more

	at org.h2.engine.SessionRemote.done(SessionRemote.java:592)
	at org.h2.command.CommandRemote.executeUpdate(CommandRemote.java:207)
	at org.h2.jdbc.JdbcStatement.executeUpdateInternal(JdbcStatement.java:131)
	at org.h2.jdbc.JdbcStatement.executeUpdate(JdbcStatement.java:116)
	at my.test.command.ddl.CreateTableTest.parseColumnForTable(CreateTableTest.java:79)
	at my.test.command.ddl.CreateTableTest.startInternal(CreateTableTest.java:24)
	at my.test.TestBase.start(TestBase.java:57)
	at my.test.command.ddl.CreateTableTest.main(CreateTableTest.java:7)
