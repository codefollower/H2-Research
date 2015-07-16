H2数据库的版本: 1.3.169

server端有一个listen线程，
org.h2.server.TcpServer.listen()

此线程一直在监听客户端连接，
一但收到客户端连接请求就开一个新的线程处理它(并未使用线程池、每连接每线程)


协议共有18条命令(从0到17)


1. 初始化阶段(或握手阶段)

client先发数据:
int     minClientVersion
int     maxClientVersion
String  db               //数据库名
String  URL
String  userName
byte[]  userPasswordHash //32字节，使用SHA256算法对 userName + "@" + userPassword 进行hash，作为用户保存到数据库中的密码
byte[]  filePasswordHash //32字节，使用SHA256算法对 "file@" + filePassword 进行hash，用来加密数据库文件
int     keys length //url中的key-value参数个数

keys length个
{
	String key
	String value
}

当前支持的协议版本是6到15

minClientVersion和maxClientVersion用来告诉server端当前client能支持的最小和最大协议版本是多少，
根据这两个参数，server端会选择一个合适的协议版本与client通信，
选择合适的协议版本的方法如下:
6<=minClientVersion<=12，
如果maxClientVersion>=12，那么就用12，否则使用minClientVersion


server响应: STATUS_OK 或 STATUS_ERROR

STATUS_OK:
------------------------------------
int     STATUS_OK(1)
int     clientVersion
------------------------------------


STATUS_ERROR: 
------------------------------------
int     STATUS_ERROR(0)
String  SQLState value
String  message
String  sql
int     errorCode 
String  trace
------------------------------------
见org.h2.server.TcpServerThread.sendError(Throwable)


2. sessionId设置

client自己生成一个长度为64个字符的sessionId，然后传给server，
client给server发送的是一个SESSION_SET_ID数据包

sessionId在server端会在内存中保存，当client调用org.h2.jdbc.JdbcStatement.cancel()时才用到

------------------------------------
int     SESSION_SET_ID(12)
String  sessionId
------------------------------------

server响应: 只有STATUS_OK
------------------------------------
int     STATUS_OK(1)
boolean autoCommit //从协议版本15开始支持
------------------------------------


上面的1和2在client和server之间每次建立一个新的connection(或称为session)时都会固定发送，
(见: org.h2.engine.SessionRemote.initTransfer(ConnectionInfo, String, String))




3. prepare阶段


接下来按执行SQL语句的类别分成两类: QUERY和UPDATE

执行SQL之前，需要先进行prepare，
client给server发送的是一个SESSION_PREPARE_READ_PARAMS或SESSION_PREPARE数据包，两者格式几本上一样，
SESSION_PREPARE_READ_PARAMS需要server响应的数据包中包含SQL语句中的参数元数据，SESSION_PREPARE则不需要，
SESSION_PREPARE用在第二次对同一个SQL进行prepare时。

SESSION_PREPARE_READ_PARAMS
------------------------------------
int     SESSION_PREPARE_READ_PARAMS(11)
int     id(执行当前sql时为此操作生成的一个计数器)
String  sql
------------------------------------

server响应:
------------------------------------
int     STATUS_OK 或 STATUS_OK_STATE_CHANGED
boolean isQuery
boolean readonly
int     sql parameter size
Parameter MetaData[]
------------------------------------

SESSION_PREPARE
------------------------------------
int     SESSION_PREPARE(0)
int     id(执行当前sql时为此操作生成的一个计数器)
String  sql
------------------------------------

server收到上面两个包后，会按id把对应的SQL缓存起来，
server响应:
------------------------------------
int     STATUS_OK 或 STATUS_OK_STATE_CHANGED
boolean isQuery
boolean readonly
int     sql parameter size
------------------------------------

相关源代码见: org.h2.command.CommandRemote.prepare(SessionRemote, boolean)



4. update

当进行java.sql.Statement.executeUpdate之类的操作时通常会触发update

COMMAND_EXECUTE_UPDATE
------------------------------------
int     COMMAND_EXECUTE_UPDATE(3)
int     id(对应prepare阶段生成的id)
int     sql parameter size
Parameter[]
------------------------------------

server响应:
------------------------------------
int     STATUS_OK 或 STATUS_OK_STATE_CHANGED
int     updateCount
boolean autoCommit
------------------------------------

相关源代码见: org.h2.command.CommandRemote.executeUpdate()



5. query

(TODO 需要举例完善)

当进行java.sql.Statement.executeQuery之类的操作时通常会触发query

COMMAND_EXECUTE_QUERY
------------------------------------
int     COMMAND_EXECUTE_QUERY(2)
int     id(对应prepare阶段生成的id)
int     objectId(跟id类似，实际上就是一个递增计数器，在server端缓存查询结果集时，这个objectId就是结果集的缓存key)
int     maxRows
int     fetchSize
int     sql parameter size
Parameter[]
------------------------------------

server响应:
------------------------------------
int     STATUS_OK 或 STATUS_OK_STATE_CHANGED
int     columnCount
int     rowCount
ResultColumn[] 结果集中列的元数据
	ResultColumn {
		String  Alias
		String  SchemaName
		String  TableName
		String  ColumnName
		int     ColumnType
		long    ColumnPrecision
		int     ColumnScale
		int     DisplaySize
		boolean isAutoIncrement
		int     Nullable
	}
row result {
	boolean hasNext
	如果hasNext==true
	字段值，...
}
------------------------------------

相关源代码见: org.h2.command.CommandRemote.executeQuery(int, boolean)





6. 其他命令

6.1 获取结果集元数据


COMMAND_GET_META_DATA
------------------------------------
int     COMMAND_GET_META_DATA(10)
int     id(对应prepare阶段生成的id)
int     objectId(跟id类似，实际上就是一个递增计数器，在server端缓存元数据结果集时，这个objectId就是结果集的缓存key)
------------------------------------

server响应:
------------------------------------
int     STATUS_OK
int     columnCount
int     rowCount (固定是0)
ResultColumn[] 结果集中列的元数据
	ResultColumn {
		String  Alias
		String  SchemaName
		String  TableName
		String  ColumnName
		int     ColumnType
		long    ColumnPrecision
		int     ColumnScale
		int     DisplaySize
		boolean isAutoIncrement
		int     Nullable
	}
}
------------------------------------

相关源代码见: org.h2.command.CommandRemote.getMetaData()




6.2 COMMAND_CLOSE

COMMAND_CLOSE
------------------------------------
int     COMMAND_CLOSE(4)
int     id(对应prepare阶段生成的id)
------------------------------------

server清除对应id的缓存，无响应包

相关源代码见: org.h2.command.CommandRemote.close()



6.3 RESULT_FETCH_ROWS

RESULT_FETCH_ROWS
------------------------------------
int     RESULT_FETCH_ROWS(5)
int     id(对应Query阶段生成的id)
int     fetchSize
------------------------------------


server响应:
------------------------------------
int     STATUS_OK
int     columnCount
row result {
	boolean hasNext
	如果hasNext==true
	字段值，...
}
------------------------------------

相关源代码见: org.h2.result.ResultRemote.fetchRows(boolean)


6.4 RESULT_CLOSE

RESULT_CLOSE
------------------------------------
int     RESULT_CLOSE(7)
int     id(对应Query阶段生成的id)
------------------------------------


server清除对应id的缓存，无响应包

相关源代码见: org.h2.result.ResultRemote.sendClose()



6.5 RESULT_RESET

在进行java.sql.ResultSet的first、beforeFirst、absolute会触发此操作
RESULT_RESET
------------------------------------
int     RESULT_RESET(6)
int     id(对应Query阶段生成的id)
------------------------------------


server端的Result对象被reset，无响应包

相关源代码见: org.h2.result.ResultRemote.reset()



6.6 COMMAND_COMMIT

在集群环境下会自动提交模式会变成false，当执行操作完后，client端会自动发此命令给所有server，通知他们提交.
COMMAND_COMMIT
------------------------------------
int     COMMAND_COMMIT(8)
------------------------------------


server响应:
------------------------------------
int     STATUS_OK 或 STATUS_OK_STATE_CHANGED
------------------------------------

相关源代码见: org.h2.engine.SessionRemote.autoCommitIfCluster()




6.7 CHANGE_ID

通常在server端每个session的cache大小都是有限制的，默认是64个，当结果集的id很小(也就意味着很旧了)
那么需要从当前的SessionRemote得到一个新的id，然后通知server用这个新id与原来cache中的结果集重新映射一次.

CHANGE_ID
------------------------------------
int     CHANGE_ID(9)
int     旧ID
int     新ID
------------------------------------


server响应:
------------------------------------
server端无响应包
------------------------------------

相关源代码见: org.h2.result.ResultRemote.remapIfOld()




6.8 SESSION_SET_AUTOCOMMIT

改变autoCommit模式

SESSION_SET_AUTOCOMMIT
------------------------------------
int     SESSION_SET_AUTOCOMMIT(15)
boolean true or false
------------------------------------


server响应:
------------------------------------
int     STATUS_OK
------------------------------------

相关源代码见: org.h2.engine.SessionRemote.setAutoCommitSend(boolean)



6.9 SESSION_UNDO_LOG_POS

关闭连接时，看看server端是否还有撤消日志，如果有，则执行rollback操作

SESSION_UNDO_LOG_POS
------------------------------------
int     SESSION_UNDO_LOG_POS(16)
------------------------------------


server响应:
------------------------------------
int     STATUS_OK
int     Undo Log Pos
------------------------------------

相关源代码见: org.h2.engine.SessionRemote.getUndoLogPos()



6.10 LOB_READ

TODO



7 misc

还有两种特殊情况:
在org.h2.server.TcpServerThread.run()中特殊情理，对应if (db == null && originalURL == null)这个语句

SessionRemote.SESSION_CANCEL_STATEMENT
------------------------------------
int     minClientVersion
int     maxClientVersion
String  null
String  null
String  sessionId
int     SessionRemote.SESSION_CANCEL_STATEMENT(13)
int     statement id
------------------------------------
见: org.h2.engine.SessionRemote.cancelStatement(int)


server响应:
------------------------------------
不需要响应
------------------------------------



SessionRemote.SESSION_CHECK_KEY
------------------------------------
int     minClientVersion
int     maxClientVersion
String  null
String  null
String  sessionId
int     SessionRemote.SESSION_CHECK_KEY(14)
------------------------------------
见: org.h2.store.FileLock.checkServer()




server响应:

STATUS_OK:
------------------------------------
int     STATUS_OK(1)
int     clientVersion
------------------------------------


STATUS_ERROR: 
------------------------------------
int     STATUS_ERROR(0)
String  SQLState value
String  message
String  sql
int     errorCode 
String  trace
------------------------------------
见org.h2.server.TcpServerThread.sendError(Throwable)



第一个握手包处理完之后，就可以一直使用长连接来发命令了:

命令协议包:

int     operation
每个命令都有自己的格式

具体见: org.h2.server.TcpServerThread.process()

client端发的第一个命令一般是SessionRemote.SESSION_PREPARE_READ_PARAMS/SESSION_PREPARE
具体见: org.h2.command.CommandRemote.prepare(SessionRemote, boolean)




三种类型的close命令，按级别由小到大分别是:
RESULT_CLOSE   通知server关闭结果集，并在session的cache中删除结果集缓存
COMMAND_CLOSE  通知server关闭SQL命令，并在session的cache中删除命令缓存
SESSION_CLOSE  通知server关闭session，停掉线程，删除与session相关的所有资源(这条命令会释放大量资源)




流程:

org.h2.server.TcpServer.listen()
	=> org.h2.server.TcpServerThread.run()
		=> org.h2.engine.Engine.createSession(ConnectionInfo)
		=> org.h2.server.TcpServerThread.process()