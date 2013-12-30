package my.test.message;

import org.h2.message.Trace;
import org.h2.message.TraceSystem;

public class TraceSystemTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// System.setProperty("user.home", "E:/H2/tmp/FileStoreTest");
		// FileUtils.createFile("~/a.txt");

		TraceSystem ts = new TraceSystem("E:/H2/tmp/TraceSystemTest/trace.txt");
		ts.setLevelFile(TraceSystem.DEBUG);
		ts.setLevelSystemOut(TraceSystem.DEBUG);
		Trace t = ts.getTrace(org.h2.message.Trace.JDBC);
		t = ts.getTrace(org.h2.message.Trace.FILE_LOCK);

		t.debug("aaaa");
		t.error(new Error(), "bbb");

	}

}
