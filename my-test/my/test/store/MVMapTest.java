/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package my.test.store;

import java.io.File;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreTool;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.type.StringDataType;

public class MVMapTest {
    public static void main(String[] args) {
        new MVMapTest().run();
    }

    public static void p(Object o) {
        System.out.println(o);
    }

    public static void p() {
        System.out.println();
    }

    MVStore store;
    MVMap<String, String> map;
    MVRTreeMap<String> rmap;
    String fileName = "./data/test/mvstore/MVMapTest.mv.db";

    void initMVStore() {
        new File(fileName).getParentFile().mkdirs();
        MVStore.Builder builder = new MVStore.Builder();
        builder.fileName(fileName);
        builder.compress();
        builder.autoCommitDisabled();
        builder.pageSplitSize(1024);
        store = builder.open();
    }

    void run() {
        testMap();
        // testMapTool();

        // testCompact();
    }

    void testMapTool() {
        // String[] args = { "-info", fileName };
        String[] args = { "-dump", fileName, "-compact", fileName };
        MVStoreTool.main(args);
    }

    void testMap() {
        initMVStore();
        try {
            openMap();

            // store.removeMap(map);

            // openMap();

            // testPut();
            // testGet();

            testOpenVersion();

        } finally {
            // LockFreeMVMap.stopMerger();
            store.close();
        }
    }

    void testOpenVersion() {
        map.put("10", "a");
        long version = store.commit();
        map.put("10", "b");
        version = store.commit();

        MVMap<String, String> m = map.openVersion(version);
        p(m.get("10"));

        m = map.openVersion(version - 1);
        p(m.get("10"));

        m = map.openVersion(version - 2);
        p(m.get("10"));
    }

    void testCompact() {
        initMVStore();

        int targetFillRate = 80;
        int write = 100;
        // store.compact(targetFillRate, write);
        store.compactMoveChunks(targetFillRate, write);
    }

    void openMap() {
        MVMap.Builder<String, String> builder = new MVMap.Builder<String, String>();
        builder.keyType(StringDataType.INSTANCE);
        builder.valueType(StringDataType.INSTANCE);

        MVRTreeMap.Builder<String> rbuilder = new MVRTreeMap.Builder<String>();
        rbuilder.dimensions(2);
        rbuilder.valueType(StringDataType.INSTANCE);
        // rmap = store.openMap("rtest", rbuilder);

        map = store.openMap("test", builder);
    }

    void testPut() {
        for (int i = 10; i < 100; i++) {
            // map.put("" + i, null);
            map.put("" + i, "value" + i);
            // rmap.put(new SpatialKey(i, i * 1.0F, i * 2.0F), "value" + i);
        }

        long version = 0;
        version = store.commit();
        p(version);

        // for (int i = 100; i < 200; i++) {
        // map.put("" + i, "value" + i);
        // }

        map.put("" + 10, "value" + 1000);
        map.put("" + 30, "value" + 3000);

        p(map.get("10"));

        version = store.commit();
        p(version);

        store.rollbackTo(version - 1);
        p(map.get("10"));

        map.remove("10");
        p(map.size());
    }

    void testGet() {
        p(map.get("50"));
        p(map.get("50"));
        String key = map.firstKey();
        p(key);

        key = map.lastKey();
        p(key);

        key = map.getKey(20);
        p(key);

        // for (String k : map.keyList())
        // p(k);

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
    }
}
