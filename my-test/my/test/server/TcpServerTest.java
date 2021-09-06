package my.test.server;

import org.h2.server.TcpServer;

import my.test.TestBase;

public class TcpServerTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new TcpServerTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		//建立了一个自定义的函数STOP_SERVER，
        //通过类似这样CALL STOP_SERVER(9092, '', 0)就能关闭H2数据库
        //调用的是org.h2.server.TcpServer.stopServer(int, String, int)方法
        //不过因为函数STOP_SERVER是在内存数据库的，所以通过TCP远程调用是不行的，
        //要在Client端手工再建立同样的函数，见: org.h2.server.TcpServer.initManagementDb()
		stmt.executeUpdate("CREATE ALIAS IF NOT EXISTS STOP_SERVER FOR \"" //
				+ TcpServer.class.getName() + ".stopServer\"");

		//SHUTDOWN_NORMAL = 0;
		//SHUTDOWN_FORCE = 1;

		//正常关闭H2数据库
		stmt.executeUpdate("CALL STOP_SERVER(9092, 'aaa', 0)");

		//强制关闭H2数据库
		//stmt.executeUpdate("CALL STOP_SERVER(9092, '', 1)");
	}

}
