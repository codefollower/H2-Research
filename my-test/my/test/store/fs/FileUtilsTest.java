package my.test.store.fs;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.h2.store.fs.FileUtils;

public class FileUtilsTest {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//test("file");
		//test("nio");
		//test("nioMapped");
		//test("memFS");
		//test("split");
		//new File("E:\\H2\\tmp\\FileStoreTest\\my.txt").toURL();
		testFilePathZip();
	}
	public static void testFilePathZip() throws Exception {
		System.setProperty("user.home", "E:/H2/lib");
		//FileUtils.newInputStream("zip:~/servlet-api-2.4.jar"); //不能直接打开jar
		//FileUtils.newInputStream("zip:~/servlet-api-2.4.jar!/javax/servlet/Filter.class "); //最后不能有空格
		//感叹号后面有没有/都是可以的
		//FileUtils.newInputStream("zip:~/servlet-api-2.4.jar!javax/servlet/Filter.class");
		FileUtils.newInputStream("zip:~/servlet-api-2.4.jar!/javax/servlet/Filter.class");
		
		
		p(FileUtils.exists("zip:~/servlet-api-2.4.jar"));
		p(FileUtils.exists("zip:~/servlet-api-2.4.jar!javax"));
		p(FileUtils.unwrap("zip:~/servlet-api-2.4.jar!javax"));
	}
	public static void main2(String[] args) throws Exception {
		String fileName = null;
		String mode = null;
		mode = "rw";
		fileName = "E:\\H2\\tmp\\FileStoreTest\\my.txt";
		//name = "file:E:\\H2\\tmp\\FileStoreTest\\my.txt";
		//name = "cache:E:\\H2\\tmp\\FileStoreTest\\my.txt";

		fileName = "memFS:E:\\H2\\tmp\\FileStoreTest\\my.txt";

		System.setProperty("user.home", "E:/H2/tmp/FileStoreTest");
		fileName = "~";
		fileName = "file:~/my.txt";

		FileChannel fc = FileUtils.open(fileName, mode);
		ByteBuffer src = ByteBuffer.allocate(1024);
		src.put("ddddddddd".getBytes());
		src.flip();
		fc.write(src);
		fc.close();

		p(FileUtils.exists(fileName));
		FileUtils.createDirectory("~/abc");
		p(FileUtils.exists("~/abc"));
		
		FileUtils.createFile("~/a.txt");
		p(FileUtils.exists("~/a.txt"));
		FileUtils.delete("~/a.txt");
		p(FileUtils.exists("~/a.txt"));
		FileUtils.delete("file:~/my2.txt");
		FileUtils.move(fileName, "file:~/my2.txt");
		FileUtils.delete("file:~/my2.txt");
		FileUtils.createFile("~/a22.txt");
		p(FileUtils.newDirectoryStream("~"));
		
		p(new File(".").getCanonicalFile());
		
		FileUtils.newInputStream("classpath:my/test/store/fs/FileUtilsTest.class");
		
		p(FileUtils.createTempFile("file:~/my3", "log", true));
	}
	
	public static void test(String scheme) throws Exception {
		String fileName = null;
		String mode = null;
		mode = "rw";
		fileName = "E:\\H2\\tmp\\FileStoreTest\\my.txt";
		//name = "file:E:\\H2\\tmp\\FileStoreTest\\my.txt";
		//name = "cache:E:\\H2\\tmp\\FileStoreTest\\my.txt";

		fileName = "memFS:E:\\H2\\tmp\\FileStoreTest\\my.txt";

		System.setProperty("user.home", "E:/H2/tmp/FileStoreTest");
		fileName = "~";
		fileName = scheme+":~/my.txt";

		FileChannel fc = FileUtils.open(fileName, mode);
		ByteBuffer src = ByteBuffer.allocate(1024);
		src.put("ddddddddd".getBytes());
		src.flip();
		fc.write(src);
		fc.close();

		p(FileUtils.exists(fileName));
		FileUtils.createDirectory("~/abc");
		p(FileUtils.exists("~/abc"));
		
		FileUtils.createFile("~/a.txt");
		p(FileUtils.exists("~/a.txt"));
		FileUtils.delete("~/a.txt");
		p(FileUtils.exists("~/a.txt"));
		FileUtils.delete(scheme+":~/my2.txt");
		FileUtils.move(fileName, scheme+":~/my2.txt");
		FileUtils.delete(scheme+":~/my2.txt");
		FileUtils.createFile("~/a22.txt");
		p(FileUtils.newDirectoryStream("~"));
		
		p(new File(".").getCanonicalFile());
		
		FileUtils.newInputStream("classpath:my/test/store/fs/FileUtilsTest.class");
		
		p(FileUtils.createTempFile(scheme+":~/my3", "log", true));
	}
	
	static void p(Object v) {
		System.out.println(v.toString());
	}

}
