接口: org.h2.server.Service
	=> org.h2.server.TcpServer
	=> org.h2.server.web.WebServer
	=> org.h2.server.pg.PgServer

可以在org.h2.tools.Server类中同时启动上面三个服务

由org.h2.tools.Server类按下面的顺序调用org.h2.server.Service的文法:
init(String...) => start() => listen()

org.h2.tools.Server类继承自抽象类org.h2.util.Tool


main Server -> tcp Server(web/pg Server)

tcp Server(web/pg Server)的ShutdownHandler指向tcp Server


Server运行流程:

如果什么参数都不加，那么会同时启动tcp、web、pg、browser
org.h2.tools.Server.main(String...)
	=> org.h2.tools.Server.runTool(String...)