/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

//import org.h2.engine.SysProperties;

/**
 * The base class for wrapping / delegating file systems such as
 * the split file system.
 */
public abstract class FilePathWrapper extends FilePath {

    private FilePath base; //实际上就是FilePathDisk

    @Override
    public FilePathWrapper getPath(String path) {
        return create(path, unwrap(path));
    }

    /**
     * Create a wrapped path instance for the given base path.
     *
     * @param base the base path
     * @return the wrapped path
     */
    public FilePathWrapper wrap(FilePath base) {
        return base == null ? null : create(getPrefix() + base.name, base);
    }

    @Override
    public FilePath unwrap() {
        return unwrap(name);
    }

    private FilePathWrapper create(String path, FilePath base) {
        try {
//<<<<<<< HEAD
//            FilePathWrapper p = getClass().newInstance();
//            p.name = translateFileName(path); //我加上的
//            //p.name = path;
//=======
            FilePathWrapper p = getClass().getDeclaredConstructor().newInstance();
            p.name = path;
            p.base = base;
            return p;
        } catch (Exception e) {
            throw new IllegalArgumentException("Path: " + path, e);
        }
    }

//	private String translateFileName(String fileName) {  //我加上的
//        fileName = fileName.replace('\\', '/');
//        String prefix = getScheme() + ":";
//        if (fileName.startsWith(prefix)) {
//            fileName = fileName.substring(prefix.length());
//        }
//		return prefix + expandUserHomeDirectory(fileName);
//	}

//	private String expandUserHomeDirectory(String fileName) {  //我加上的
//		if (fileName.startsWith("~") && (fileName.length() == 1 || fileName.startsWith("~/"))) {
//			String userDir = SysProperties.USER_HOME;
//			fileName = userDir + fileName.substring(1);
//		}
//		return fileName;
//	}

    protected String getPrefix() {
        return getScheme() + ":";
    }

    /**
     * Get the base path for the given wrapped path.
     *
     * @param path the path including the scheme prefix
     * @return the base file path
     */
    protected FilePath unwrap(String path) {
        return FilePath.get(path.substring(getScheme().length() + 1)); //去掉模式前缀，当成FilePathDisk
    }

    protected FilePath getBase() {
        return base;
    }

    @Override
    public boolean canWrite() {
        return base.canWrite();
    }

    @Override
    public void createDirectory() {
        base.createDirectory();
    }

    @Override
    public boolean createFile() {
        return base.createFile();
    }

    @Override
    public void delete() {
        base.delete();
    }

    @Override
    public boolean exists() {
        return base.exists();
    }

    @Override
    public FilePath getParent() {
        return wrap(base.getParent());
    }

    @Override
    public boolean isAbsolute() {
        return base.isAbsolute();
    }

    @Override
    public boolean isDirectory() {
        return base.isDirectory();
    }

    @Override
    public long lastModified() {
        return base.lastModified();
    }

    @Override
    public FilePath toRealPath() {
        return wrap(base.toRealPath());
    }

    @Override
    public List<FilePath> newDirectoryStream() {
        List<FilePath> list = base.newDirectoryStream();
        for (int i = 0, len = list.size(); i < len; i++) {
            list.set(i, wrap(list.get(i)));
        }
        return list;
    }

    @Override
    public void moveTo(FilePath newName, boolean atomicReplace) {
        base.moveTo(((FilePathWrapper) newName).base, atomicReplace);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return base.newInputStream();
    }

    @Override
    public OutputStream newOutputStream(boolean append) throws IOException {
        return base.newOutputStream(append);
    }

    @Override
    public FileChannel open(String mode) throws IOException {
        return base.open(mode);
    }

    @Override
    public boolean setReadOnly() {
        return base.setReadOnly();
    }

    @Override
    public long size() {
        return base.size();
    }

    @Override
    public FilePath createTempFile(String suffix, boolean inTempDir) throws IOException {
        return wrap(base.createTempFile(suffix, inTempDir));
    }

}
