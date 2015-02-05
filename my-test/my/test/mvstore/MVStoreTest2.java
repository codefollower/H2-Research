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

import java.util.Map;

import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStore.Builder;
import org.h2.mvstore.MVStoreTool;
import org.h2.mvstore.type.StringDataType;

public class MVStoreTest2 {
    public static void main(String[] args) {
        new MVStoreTest2().run();
    }

    public static void p(Object o) {
        System.out.println(o);
    }

    public static void p() {
        System.out.println();
    }

    MVStore store;
    MVMap<String, String> map;
    String fileName = "./target/mvstore-test/MVStoreTest1.mv.db";

    void initMVStore() {
        Builder builder = new Builder();
        builder.fileName(fileName);
        builder.compress();
        builder.autoCommitDisabled();
        builder.pageSplitSize(1024);
        store = builder.open();
    }

    void openMap() {
        MVMap.Builder<String, String> builder = new MVMap.Builder<String, String>();
        builder.keyType(new StringDataType());
        builder.valueType(new StringDataType());

        map = store.openMap("mvtest", builder);
    }

    void run() {
        //testMVStoreTool();

        initMVStore();
        openMap();
        printMeta();

        //testMVStore();
        //testPut();
        //testBatchPut();
        //testVersion();
        //testGet();

        //testRemove();

        testCompact();

        store.close();
    }

    void testMVStoreTool() {

        //MVStoreTool.dump(fileName, true);

        //MVStoreTool.dump(fileName, false);

        MVStoreTool.info(fileName);
    }

    void testMVStore() {
        for (String mapName : store.getMapNames())
            p(mapName);

        store.rollbackTo(3);
    }

    void printMeta() {
        MVMap<String, String> meta = store.getMetaMap();
        for (Map.Entry<String, String> e : meta.entrySet())
            p(e.getKey() + "=" + e.getValue());
        p(meta);

        for (String k : meta.keySet())
            p(k);

        p("meta.size=" + meta.size());
    }

    void testCompact() {
        //store.compactMoveChunks();
        //store.compact(100, Integer.MAX_VALUE);
        store.compact(100, 300);
    }

    void testGet() {

        String key = map.firstKey();
        p(key);

        key = map.lastKey();
        p(key);

        key = map.getKey(20);
        p(key);

        //        for (String k : map.keyList())
        //            p(k);

        long index = map.getKeyIndex("30");
        p(index);

        index = map.getKeyIndex("300");
        p(index);

        key = map.higherKey("30");
        p(key);
        key = map.ceilingKey("30");
        p(key);

        key = map.floorKey("30");
        p(key);
        key = map.lowerKey("30");
        p(key);

        Cursor<String, String> cursor = map.cursor("4900");
        while (cursor.hasNext()) {
            key = cursor.next();
            p(key);
        }

        p(map.getName());
    }

    void testRemove() {
        map.put("10", "value_10");
        map.put("30", "value_30");
        map.put("20", "value_20");

        map.remove("20");
        map.remove("10");
        map.remove("30");

        for (int i = 10; i < 30; i++) {
            map.put("" + i, "value" + i);
        }

        for (int i = 10; i < 30; i++) {
            map.remove("" + i);
        }
    }

    void testPut() {
        p(map.getKey(3999));
        //map.put("1", null);

        map.put("10", "value_10");

        map.put("30", "value_30");

        map.put("20", "value_20");

        map.put("20", "value_20_1");

        for (int i = 10; i < 100; i++) {
            map.put("" + i, "value" + i);
        }

        store.commit();

        for (int i = 100; i < 200; i++) {
            map.put("" + i, "value" + i);
        }
    }

    void testBatchPut() {
        //map.put("1", null);

        //        for (int i = 10; i < 100; i++) {
        //            map.put("" + i, "value" + i);
        //        }
        //        store.commit();

        for (int i = 1000; i < 3000; i++) {
            map.put("" + i, "value" + i);
        }

        //        store.commit();
        //        store.sync();
        //
        //        map.put("1", "abc");
        //        store.commit();
        //        store.sync();

        for (int i = 3000; i < 5000; i++) {
            map.put("" + i, "value" + i);
        }
        //store.commit();

        p(map.getKey(3999));

        //        for (int i = 100; i < 200; i++) {
        //            map.put("" + i, "value" + i);
        //        }

        //        store.commit();
        //        store.commit();

    }

    void testVersion() {
        long version1 = map.getVersion();
        p(version1);
        map.put("1", "abc");
        version1 = map.getVersion();
        p(version1);

        map.put("2", "cdf");

        map.put("1", "def");
        p(map.get("1"));

        store.commit();

        version1 = map.getVersion();
        p(version1);

        long version2 = 0;

        for (int i = 0; i < 100; i++) {
            if (i == 50)
                version2 = map.getVersion();
            map.put("" + i, "def" + i);
        }

        MVMap<String, String> map2 = map.openVersion(version2);
        p(map2.get("50"));

        map.put("1001", "def");

        MVMap<String, String> map3 = map.openVersion(version1);
        p(map3.get("1"));

        try {
            map3.put("1", "1");
        } catch (Exception e) {
            p(e);
        }

        store.close();
    }
}
