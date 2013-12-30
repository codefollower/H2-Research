package my.test;

import java.sql.SQLException;
import java.util.ArrayList;

public class MyServer3 {
	public static void main(String[] args) throws SQLException {
		System.setProperty("h2.lobInDatabase", "false");
		System.setProperty("h2.lobClientMaxSizeMemory", "1024");
		System.setProperty("java.io.tmpdir", "E:\\H2\\tmp");
		
		ArrayList<String> list = new ArrayList<String>();
//		list.add("-tcp");
//		//list.add("-tool");
//		org.h2.tools.Server.main(list.toArray(new String[list.size()]));
//		
		list.add("-tcp");
		list.add("-tcpPort");
		list.add("9093");
		org.h2.tools.Server.main(list.toArray(new String[list.size()]));
	}
}

/*
 * 
 * public static void main(String[] args) throws SQLException {
 * ArrayList<String> list = new ArrayList<String>(); //list.add("-?");
 * list.add("-tcp"); //list.add("-web-showUsageAndThrowUnsupportedOption");
 * 
 * //list.add("-throwUnsupportedOption");
 * 
 * //list.add("-tcpShutdown");list.add("tcp:localhost");
 * 
 * org.h2.tools.Server.main(list.toArray(new String[list.size()]));
 * 
 * } public static void main2(String[] args) throws SQLException {
 * ArrayList<String> list = new ArrayList<String>(); //list.add("-?");
 * list.add("-tcpShutdown"); list.add("tcp://192.168.1.106:9092");
 * //list.add("-web-showUsageAndThrowUnsupportedOption");
 * 
 * //list.add("-throwUnsupportedOption");
 * 
 * //list.add("-tcpShutdown");list.add("tcp:localhost");
 * 
 * org.h2.tools.Server.main(list.toArray(new String[list.size()]));
 * 
 * }
 */
