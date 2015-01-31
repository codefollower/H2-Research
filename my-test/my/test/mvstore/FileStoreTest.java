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

import java.nio.ByteBuffer;

import my.test.TestBase;

import org.h2.mvstore.FileStore;
import org.h2.mvstore.WriteBuffer;

public class FileStoreTest extends TestBase {

    public static void main(String[] args) {
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

    void run() {
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
}
