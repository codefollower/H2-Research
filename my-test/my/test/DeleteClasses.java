package my.test;

import java.io.File;

public class DeleteClasses {
    public static void main(String[] args) throws Exception {
        File from = new File(args[0]);
        listFilesRecursive(from);
    }

    public static void listFilesRecursive(File file) throws Exception {
        File[] files = file.listFiles();
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (f.isDirectory())
                listFilesRecursive(f);
            else if (name.endsWith(".class")) {
                System.out.println(f);
                f.delete();
            }
        }
    }
}
