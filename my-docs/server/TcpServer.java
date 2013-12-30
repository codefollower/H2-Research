此类由org.h2.tools.Server触发

此类中的方法调用顺序是:

init => start => listen => isRunning

listen和isRunning几乎是同时执行，isRunning用于测试是否在本地连得上TcpServer


//每建立一个新的Session对象时，把它保存到内存数据库management_db_9092的SESSIONS表
stat.execute("CREATE TABLE IF NOT EXISTS SESSIONS(ID INT PRIMARY KEY, URL VARCHAR, USER VARCHAR, CONNECTED TIMESTAMP)");
managementDbAdd = conn.prepareStatement("INSERT INTO SESSIONS VALUES(?, ?, ?, NOW())");
managementDbRemove = conn.prepareStatement("DELETE FROM SESSIONS WHERE ID=?");


server端有一个listen线程，
org.h2.server.TcpServer.listen()

一直在监听客户端连接，
一但收到客户端连接请求就开一个新的线程处理它(并未使用线程池、每连接每线程)

协议如下:
client先发数据:(见: org.h2.engine.SessionRemote.initTransfer(ConnectionInfo, String, String))
int     minClientVersion
int     maxClientVersion
String  db
String  URL
String  userName
byte[]  userPasswordHash
byte[]  filePasswordHash
int     keys length

keys length个
{
	String key
	String value
}

还有两种特殊情况:
在org.h2.server.TcpServerThread.run()中特殊情理，对应if (db == null && originalURL == null)这个语句

SessionRemote.SESSION_CANCEL_STATEMENT
====================================
int     minClientVersion
int     maxClientVersion
String  null
String  null
String  sessionId
int     SessionRemote.SESSION_CANCEL_STATEMENT(13)
int     statement id
====================================
见: org.h2.engine.SessionRemote.cancelStatement(int)

SessionRemote.SESSION_CHECK_KEY
====================================
int     minClientVersion
int     maxClientVersion
String  null
String  null
String  sessionId
int     SessionRemote.SESSION_CHECK_KEY(14)
====================================
见: org.h2.store.FileLock.checkServer()




server响应:

STATUS_OK:
====================================
int     STATUS_OK(1)
int     clientVersion
====================================


STATUS_ERROR: 
====================================
int     STATUS_ERROR(0)
String  SQLState value
String  message
String  sql
int     errorCode 
String  trace
====================================
见org.h2.server.TcpServerThread.sendError(Throwable)



第一个握手包处理完之后，就可以一直使用长连接来发命令了:

命令协议包:

int     operation
每个命令都有自己的格式

具体见: org.h2.server.TcpServerThread.process()

client端发的第一个命令一般是SessionRemote.SESSION_PREPARE_READ_PARAMS/SESSION_PREPARE
具体见: org.h2.command.CommandRemote.prepare(SessionRemote, boolean)


流程:

org.h2.server.TcpServer.listen()
	=> org.h2.server.TcpServerThread.run()
		=> org.h2.engine.Engine.createSession(ConnectionInfo)
		=> org.h2.server.TcpServerThread.process()