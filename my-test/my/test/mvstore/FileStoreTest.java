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
package my.test.mvstore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import my.test.TestBase;

import org.h2.mvstore.FileStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.cache.FilePathCache;
import org.h2.store.fs.FilePath;
import org.h2.store.fs.encrypt.FilePathEncrypt; 

public class FileStoreTest extends TestBase {

    public static void main(String[] args) throws Exception {
        //Package p = FileStoreTest.class.getPackage();
        //p(p.getName());
        new FileStoreTest().run();
    }

    String fileName = "./target/mvstore-test/FileStoreTest1.mv.db";

    WriteBuffer wb = new WriteBuffer();

    void put(String str) {
        wb.putStringData(str, str.length());
    }

    void put(int i) {
        wb.putInt(i);
    }

    void run() throws Exception {
        //testFileStore();
        testFilePath();
    }

    void testFileStore() {
        FileStore fs = new FileStore();

        try {
            char[] encryptionKey = null;
            //encryptionKey = "my".toCharArray();
            fs.open(fileName, false, encryptionKey);

            put("abc");

            ByteBuffer buff = wb.getBuffer();
            buff.flip();
            long pos = fs.size();
            p("size=" + pos);
            fs.writeFully(pos, buff);

        } finally {
            fs.close();
        }
    }

    void testFilePath() throws IOException {
        //测FilePathDisk
        FilePath fp;
        //fp = FilePath.get(fileName);

        String fileName = this.fileName;
        //测FilePathNio
        fileName = "nio:" + this.fileName;

        //测FilePathNio
        fileName = "cache:encrypt:mypassword:nio:" + this.fileName;
        FilePath.register(new FilePathCache());
        FilePathEncrypt.register();
        fp = FilePath.get(fileName);

        String name = fp.getName();
        p(name);
        p(fp.getScheme());

        p(fp.size());

        //fp.moveTo(FilePath.get(fileName + "new"), true);

        for (FilePath f : fp.getParent().newDirectoryStream())
            p(f.getName());

        p(fp.toRealPath());

        try {
            p(fp.createTempFile(".my", true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileChannel ch = null;
        try {
            ch = fp.open("rw");

            //for (int i = 0; i < 3000; i++)
            //put("abc");

            ByteBuffer buff = wb.getBuffer();
            buff.flip();
            long pos = ch.size();
            p("size=" + pos);
            ch.write(buff);

            buff.clear();

            ch.read(buff, 0);
            buff.clear();
            ch.read(buff, 0);
        } finally {
            if (ch != null)
                ch.close();
        }
    }
}
