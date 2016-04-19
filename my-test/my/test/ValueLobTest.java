/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.test;

import java.io.File;

import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.store.DataHandler;
import org.h2.store.fs.FileUtils;
import org.h2.util.MathUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.Utils;
import org.h2.value.Value;
import org.h2.value.ValueLob;

public class ValueLobTest {

    public static void main2(String[] args) {

        System.setProperty("h2.lobInDatabase", "false");
        System.setProperty("h2.lobClientMaxSizeMemory", "1024");
        System.setProperty("java.io.tmpdir", "E:\\H2\\tmp");
        System.setProperty("h2.lobFilesPerDirectory", "3");

        // MyDataHandler handler = new MyDataHandler();
        // int length = 2049;
        // byte[] bytes = new byte[length];
        // for (int i = 0; i < length; i++)
        // bytes[i] = (byte) i;
        //
        // ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        //
        // BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        //
        // ValueLob.createSmallLob(Value.BLOB, bytes);
        // ValueLob.createSmallLob(Value.CLOB, bytes);
        // ValueLob.createBlob(in, length, handler);
        // ValueLob.createClob(reader, length, handler);

        String p = getFileNamePrefix("a", 1);
        System.out.println(p);

        p = getFileNamePrefix("a", 2);
        System.out.println(p);

        p = getFileNamePrefix("a", 3);
        System.out.println(p);

        p = getFileNamePrefix("a", 5);
        System.out.println(p);

        p = getFileNamePrefix("a", 10);
        System.out.println(p);

        p = getFileNamePrefix("a", 27);
        System.out.println(p);
    }

    public static void main(String[] args) {

        System.setProperty("h2.lobInDatabase", "false");
        System.setProperty("h2.lobClientMaxSizeMemory", "1024");
        System.setProperty("java.io.tmpdir", "E:\\H2\\tmp");

        // 要在初始SysProperties之前设置，因为LOB_FILES_PER_DIRECTORY是一个static final字段
        System.setProperty("h2.lobFilesPerDirectory", "5");

        MyDataHandler handler = new MyDataHandler();

        String path = handler.getDatabasePath();
        if ((path != null) && (path.length() == 0)) {
            path = new File(Utils.getProperty("java.io.tmpdir", "."), SysProperties.PREFIX_TEMP_FILE).getAbsolutePath();
        }
        // System.setProperty("h2.lobFilesPerDirectory", "5"); //在这里设置不起作用了，SysProperties类中已设成256

        // FileUtils.deleteRecursive("E:\\H2\\tmp\\h2.temp.lobs.db",false);
        for (int i = 0; i < 1; i++) {
            int objectId = getNewObjectId(handler);
            String fileName = getFileNamePrefix(path, objectId) + Constants.SUFFIX_TEMP_FILE;

            System.out.println(fileName);

            // handler.openFile(fileName, "rw", false);
        }

        ValueLob.openLinked(Value.BLOB, handler, 1, 2, 10, true);
    }

    static String getFileNamePrefix(String path, int objectId) {
        String name;
        int f = objectId % SysProperties.LOB_FILES_PER_DIRECTORY;
        if (f > 0) {
            name = SysProperties.FILE_SEPARATOR + objectId;
        } else {
            name = "";
        }
        objectId /= SysProperties.LOB_FILES_PER_DIRECTORY;
        while (objectId > 0) {
            f = objectId % SysProperties.LOB_FILES_PER_DIRECTORY;
            name = SysProperties.FILE_SEPARATOR + f + Constants.SUFFIX_LOBS_DIRECTORY + name;
            objectId /= SysProperties.LOB_FILES_PER_DIRECTORY;
        }
        name = FileUtils.toRealPath(path + Constants.SUFFIX_LOBS_DIRECTORY + name);
        return name;
    }

    static int dirCounter;

    static int getNewObjectId(DataHandler h) {
        String path = h.getDatabasePath();
        if ((path != null) && (path.length() == 0)) {
            path = new File(Utils.getProperty("java.io.tmpdir", "."), SysProperties.PREFIX_TEMP_FILE).getAbsolutePath();
        }
        int newId = 0;
        int lobsPerDir = SysProperties.LOB_FILES_PER_DIRECTORY;
        while (true) {
            String dir = getFileNamePrefix(path, newId);
            // System.out.println(dir);
            System.out.println(newId);
            String[] list = getFileList(h, dir);
            int fileCount = 0;
            boolean[] used = new boolean[lobsPerDir];
            for (String name : list) {
                if (name.endsWith(Constants.SUFFIX_DB_FILE)) {
                    name = FileUtils.getName(name);
                    String n = name.substring(0, name.indexOf('.'));
                    int id;
                    try {
                        id = Integer.parseInt(n);
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                    if (id > 0) {
                        fileCount++;
                        used[id % lobsPerDir] = true;
                    }
                }
            }
            int fileId = -1;
            if (fileCount < lobsPerDir) {
                for (int i = 1; i < lobsPerDir; i++) {
                    if (!used[i]) {
                        fileId = i;
                        break;
                    }
                }
            }
            if (fileId > 0) {
                newId += fileId;
                invalidateFileList(h, dir);
                break;
            }
            if (newId > Integer.MAX_VALUE / lobsPerDir) {
                // this directory path is full: start from zero
                newId = 0;
                dirCounter = MathUtils.randomInt(lobsPerDir - 1) * lobsPerDir;
            } else {
                // calculate the directory
                // start with 1 (otherwise we don't know the number of
                // directories)
                // it doesn't really matter what directory is used, it might as
                // well be random
                // (but that would generate more directories):
                // int dirId = RandomUtils.nextInt(lobsPerDir - 1) + 1;
                int dirId = (dirCounter++ / (lobsPerDir - 1)) + 1;
                newId = newId * lobsPerDir;
                newId += dirId * lobsPerDir;
            }
        }
        return newId;
    }

    static String[] getFileList(DataHandler h, String dir) {
        SmallLRUCache<String, String[]> cache = h.getLobFileListCache();
        String[] list;
        if (cache == null) {
            list = FileUtils.newDirectoryStream(dir).toArray(new String[0]);
        } else {
            synchronized (cache) {
                list = cache.get(dir);
                if (list == null) {
                    list = FileUtils.newDirectoryStream(dir).toArray(new String[0]);
                    cache.put(dir, list);
                }
            }
        }
        return list;
    }

    static void invalidateFileList(DataHandler h, String dir) {
        SmallLRUCache<String, String[]> cache = h.getLobFileListCache();
        if (cache != null) {
            synchronized (cache) {
                cache.remove(dir);
            }
        }
    }
}
