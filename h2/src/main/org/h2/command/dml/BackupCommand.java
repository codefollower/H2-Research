/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.h2.api.ErrorCode;
import org.h2.command.CommandInterface;
import org.h2.command.Prepared;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.Store;
import org.h2.result.ResultInterface;
import org.h2.store.FileLister;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;

/**
 * This class represents the statement
 * BACKUP
 */
public class BackupCommand extends Prepared {

    private Expression fileNameExpr;

    public BackupCommand(SessionLocal session) {
        super(session);
    }

    public void setFileName(Expression fileName) {
        this.fileNameExpr = fileName;
    }

    @Override
    public long update() {
        String name = fileNameExpr.getValue(session).getString();
        session.getUser().checkAdmin();
        backupTo(name);
        return 0;
    }

    private void backupTo(String fileName) {
        Database db = session.getDatabase();
        if (!db.isPersistent()) {
            throw DbException.get(ErrorCode.DATABASE_IS_NOT_PERSISTENT);
        }
        try {
            Store store = db.getStore();
            store.flush();
            String name = db.getName(); //返回E:/H2/baseDir/mydb
            name = FileUtils.getName(name); //返回mydb(也就是只取简单文件名
            //生成fileName表示的文件，如果已存在则覆盖原有的，也就是文件为空
            try (OutputStream zip = FileUtils.newOutputStream(fileName, false)) {
                ZipOutputStream out = new ZipOutputStream(zip);
                db.flush(); //里面又会对MVStore进行flush
                // synchronize on the database, to avoid concurrent temp file
                // creation / deletion / backup
                String base = FileUtils.getParent(db.getName());
                synchronized (db.getLobSyncObject()) {
                    String prefix = db.getDatabasePath(); //返回E:/H2/baseDir/mydb
                    String dir = FileUtils.getParent(prefix); //返回E:/H2/baseDir
                    dir = FileLister.getDir(dir); //返回E:/H2/baseDir
                    ArrayList<String> fileList = FileLister.getDatabaseFiles(dir, name, true);
                    for (String n : fileList) {
                        if (n.endsWith(Constants.SUFFIX_MV_FILE)) {
                            MVStore s = store.getMvStore();
                            boolean before = s.getReuseSpace();
                            s.setReuseSpace(false);
                            try {
                                InputStream in = store.getInputStream();
                                backupFile(out, base, n, in);
                            } finally {
                                s.setReuseSpace(before);
                            }
                        }
                    }
                }
                out.close();
            }
        } catch (IOException e) {
            throw DbException.convertIOException(e, fileName);
        }
    }

    private static void backupFile(ZipOutputStream out, String base, String fn,
            InputStream in) throws IOException {
        String f = FileUtils.toRealPath(fn); //返回E:/H2/baseDir/mydb.mv.db
        base = FileUtils.toRealPath(base);//返回E:/H2/baseDir
        if (!f.startsWith(base)) {
            throw DbException.getInternalError(f + " does not start with " + base);
        }
        f = f.substring(base.length()); //返回/mydb.mv.db
        f = correctFileName(f); //返回mydb.mv.db
        out.putNextEntry(new ZipEntry(f)); //如果打开backup后的zip文件，里面会有一个以f命名的条目
        IOUtils.copyAndCloseInput(in, out);
        out.closeEntry();
    }

    @Override
    public boolean isTransactional() { //如里BACKUP命令在一个事务中执行，需要手工提交
        return true;
    }

    /**
     * Fix the file name, replacing backslash with slash.
     *
     * @param f the file name
     * @return the corrected file name
     */
    public static String correctFileName(String f) {
        f = f.replace('\\', '/');
        if (f.startsWith("/")) {
            f = f.substring(1);
        }
        return f;
    }

    @Override
    public boolean needRecompile() {
        return false;
    }

    @Override
    public ResultInterface queryMeta() {
        return null;
    }

    @Override
    public int getType() {
        return CommandInterface.BACKUP;
    }

}
