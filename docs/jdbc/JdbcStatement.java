
1.executeUpdate

完成一个executeUpdate操作，client发送三个数据包
(org.h2.jdbc.JdbcPreparedStatement.executeUpdate()只要两个，
因为一个JdbcPreparedStatement实例只对应一个CommandInterface实例，所以只有在关闭JdbcPreparedStatement时才发COMMAND_CLOSE数据包)

executeUpdate流程:

org.h2.jdbc.JdbcStatement.executeUpdate(String)
	=> org.h2.jdbc.JdbcStatement.executeUpdateInternal(String)
		=> org.h2.jdbc.JdbcConnection.translateSQL(String, boolean) (转换JDBC特有的SQL语法)
		=> org.h2.jdbc.JdbcConnection.prepareCommand(String, int) (得到一个org.h2.command.CommandRemote，对应单个SQL)
			=> org.h2.engine.SessionRemote.prepareCommand(String, int)
				=> org.h2.command.CommandRemote
					=> org.h2.command.CommandRemote.prepare(SessionRemote, boolean) (发出一个SESSION_PREPARE_READ_PARAMS数据包)
		=> org.h2.command.CommandRemote.executeUpdate() (发出一个COMMAND_EXECUTE_UPDATE数据包)
		=> org.h2.command.CommandRemote.close() (发出一个COMMAND_CLOSE数据包)



2.executeQuery

完成一个executeQuery操作，client发送三个数据包
(JdbcPreparedStatement同上，也不用发COMMAND_CLOSE数据包，关闭JdbcPreparedStatement时才发)

executeQuery流程:

org.h2.jdbc.JdbcStatement.executeQuery(String)
	=> org.h2.jdbc.JdbcConnection.translateSQL(String, boolean) (转换JDBC特有的SQL语法)
	=> org.h2.jdbc.JdbcConnection.prepareCommand(String, int) (得到一个org.h2.command.CommandRemote，对应单个SQL)
		=> org.h2.engine.SessionRemote.prepareCommand(String, int)
			=> org.h2.command.CommandRemote
				=> org.h2.command.CommandRemote.prepare(SessionRemote, boolean) (发出一个SESSION_PREPARE_READ_PARAMS数据包)
	=> org.h2.command.CommandRemote.executeQuery(int, boolean) (发出一个COMMAND_EXECUTE_QUERY数据包)
	=> org.h2.command.CommandRemote.close() (发出一个COMMAND_CLOSE数据包)
	=> org.h2.jdbc.JdbcResultSet




JdbcStatement和JdbcPreparedStatement都调用同样的org.h2.command.CommandRemote.executeQuery，
但是JdbcStatement的Parameters总是0个，JdbcPreparedStatement是>=0个。























































































































































































































































