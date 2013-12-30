package my.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.h2.util.IOUtils;

public class GBK2UTF8 {
	public static void main(String[] args) throws Exception {
		File from = new File(args[0]);
		File to = new File(args[1]);
		if (!to.exists())
			to.mkdirs();
		listFilesRecursive(from, from, to);
	}

	public static void listFilesRecursive(File file, File from, File to) throws Exception {
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory())
				listFilesRecursive(f, from, to);
			else if (f.getName().toLowerCase().endsWith(".java")) {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "GBK"));
				String fileName = f.getCanonicalPath().substring(from.getCanonicalPath().length());
				f = new File(to.getCanonicalPath(), fileName);
				if (!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				System.out.println(f);
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
				IOUtils.copyAndCloseInput(in, out, Integer.MAX_VALUE);
				IOUtils.closeSilently(in);
				IOUtils.closeSilently(out);
			}
		}
	}
}
