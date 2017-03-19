package my.test;

import java.sql.SQLException;
import java.util.ArrayList;

public class MyServer {
	public static void main(String[] args) throws SQLException {
		// System.setProperty("DATABASE_TO_UPPER", "false");
		System.setProperty("h2.lobInDatabase", "false");
		System.setProperty("h2.lobClientMaxSizeMemory", "1024");
		System.setProperty("java.io.tmpdir", "./target/mytest/tmp");
		System.setProperty("h2.baseDir", "./target/mytest");
		//System.setProperty("h2.check2", "true");
		ArrayList<String> list = new ArrayList<String>();
		// list.add("-tcp");
		// //list.add("-tool");
		// org.h2.tools.Server.main(list.toArray(new String[list.size()]));
		//
		//list.add("-tcp");
		// list.add("-tcpPort");
		// list.add("9092");

		// 测试org.h2.server.TcpServer.checkKeyAndGetDatabaseName(String)
		// list.add("-key");
		// list.add("mydb");
		// list.add("mydatabase");

		//list.add("-pg");
		list.add("-tcp");
		list.add("-web");
		//list.add("-ifExists");
		org.h2.tools.Server.main(list.toArray(new String[list.size()]));
	}
}

