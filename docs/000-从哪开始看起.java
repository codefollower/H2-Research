先看server目录中的文档

org.h2.tools.Server是主入口


三种server:  tcp、pg、web

org.h2.server.Service
	=> org.h2.dev.ftp.server.FtpServer
	=> org.h2.server.pg.PgServer
	=> org.h2.server.TcpServer
	=> org.h2.server.web.WebServer


