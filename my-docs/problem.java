org.h2.server.TcpServer.listen()    -> for(;;) select()

	=> org.h2.server.TcpServerThread.run()  -> Thread.start()
  
		=> org.h2.engine.Engine.createSession(ConnectionInfo)  -> INSERT INTO SESSIONS VALUES(?, ?, ?, NOW())
    
		=> org.h2.server.TcpServerThread.process()   -> switch(cmd) case do protocol
    
    数据存储？ 事务？ 查询优化？
    
