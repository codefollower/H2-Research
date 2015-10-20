///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package my.test.mvstore;
//
//import java.util.Set;
//
//import org.h2.mvstore.Cursor;
//import org.h2.mvstore.MVMap;
//import org.h2.mvstore.MVStore;
//import org.h2.store.fs.FileUtils;
//
//public class MVMapTest {
//    public static void p(Object o) {
//        System.out.println(o);
//    }
//
//    public static void p() {
//        System.out.println();
//    }
//
//    public static void main(String[] args) {
//        // testKeyIterator();
//        // testRemove();
//        testOpenVersion();
//    }
//
//    public static void testOpenVersion() {
//        String fileName = null;
//        fileName = "E:/H2/baseDir/MVStoreTest333";
//        FileUtils.deleteRecursive(fileName, true);
//
//        MVStore.Builder builder = new MVStore.Builder();
//        // builder.writeDelay(100).fileName(fileName);
//
//        MVStore store = builder.open();
//        // store.setPageSize(512);
//
//        MVMap<Integer, String> map = store.openMap("MVMapTest");
//
//        // store.incrementVersion();
//        // store.incrementVersion();
//
//        // p(map.openVersion(3).getRoot());
//
//        // p(map.openVersion(4).getRoot());
//
//        p(map.size());
//        p(map.getRoot());
//        p(map.openVersion(0).getRoot());
//
//        map.put(1, "1");
//
//        p(map.openVersion(0).getRoot());
//
//        store.incrementVersion();
//        store.incrementVersion();
//
//        map.put(2, "2");
//        store.incrementVersion();
//
//        map.put(3, "3");
//        store.incrementVersion();
//        map.put(4, "4");
//
//        p(map.openVersion(0).getRoot());
//        p(map.openVersion(1).getRoot());
//        p(map.openVersion(2).getRoot());
//        p(map.openVersion(3).getRoot());
//        p(map.openVersion(4).getRoot());
//        store.commit();
//        store.close(); // 老的版本在close后就不能打开了，自动清掉
//
//    }
//
//    static MVStore getMVStore(String fileName) {
//        MVStore.Builder builder = new MVStore.Builder();
//        builder.writeDelay(0).fileName(fileName);
//
//        MVStore store = builder.open();
//        store.setPageSize(512);
//
//        return store;
//    }
//
//    public static void testRemove() {
//        MVStore store = MVStore.open(null);
//        store.setPageSize(512);
//
//        MVMap<Integer, String> map = store.openMap("MVMapTest");
//
//        int n = 10;
//        for (int i = 1; i <= n; i++) {
//            map.put(i, "Hello" + i);
//        }
//
//        p(map.getRoot());
//
//        // map.clear();
//        // map.removeMap();
//
//        map.remove(1);
//        map.remove(2);
//        map.remove(3);
//
//        for (int i = 1; i <= n; i++) {
//            map.remove(i);
//        }
//
//        p(map.getRoot());
//
//        map.put(1, "a");
//
//        p(map.getRoot());
//
//    }
//
//    public static void testKeyIterator() {
//        MVStore store = MVStore.open(null);
//        store.setPageSize(512);
//
//        MVMap<Integer, String> map = store.openMap("MVMapTest");
//
//        int n = 100;
//        for (int i = 1; i <= n; i++) {
//            map.put(i, "Hello" + i);
//        }
//
//        p(map.getRoot());
//
//        Integer key = 3;
//        key = null;
//
//        key = -2;
//
//        key = 101;
//
//        p(map.higherKey(key));
//        p(map.ceilingKey(key));
//
//        p(map.lowerKey(key));
//        p(map.floorKey(key));
//
//        p();
//        Cursor<Integer> c = map.keyIterator(50);
//        // c.skip(40);
//        // c.skip(30);
//        c.skip(6);
//        while (c.hasNext()) {
//            p(c.next());
//        }
//
//        Set<Integer> set = map.keySet();
//        for (Integer k : set) {
//            p(k);
//        }
//
//        // for (Integer k : map.keyIterator(98)) {
//        // p(k);
//        // }
//    }
//
//    public static void main2(String[] args) {
//        // String fileName = null;
//        // fileName = "E:/H2/baseDir/MVStoreTest333";
//        // MVStore store = MVStore.open(fileName);
//        MVStore store = MVStore.open(null);
//        store.setPageSize(512);
//
//        // store.incrementVersion();
//        // store.incrementVersion();
//
//        MVMap<Integer, String> map = store.openMap("MVMapTest");
//
//        p(map.toString());
//        p(map.asString("test"));
//
//        // map.put(1, null);
//
//        map.renameMap("MVMapTest2");
//
//        map.put(1, "1");
//        store.incrementVersion();
//        store.incrementVersion();
//
//        map.put(2, "2");
//        store.incrementVersion();
//        store.incrementVersion();
//
//        map.put(3, "3");
//
//        p(map.firstKey());
//        p(map.lastKey());
//
//        p(store.getCurrentVersion());
//        // map.openVersion(-1);
//
//        p(map.getVersion());
//
//        int n = 100;
//        for (int i = 0; i < n; i++) {
//            map.put(i, "Hello" + i);
//
//            if (i % 20 == 0)
//                store.incrementVersion();
//        }
//
//        p(map.firstKey());
//        p(map.lastKey());
//
//        map.openVersion(1);
//
//        p(map.getRoot());
//
//        p(map.openVersion(0).getRoot());
//
//        p(map.openVersion(1).getRoot());
//
//        p(map.openVersion(2).getRoot());
//
//        p(map.getKey(10));
//
//        map.remove(0);
//        map.remove(1);
//        map.remove(2);
//
//        for (int i = 0; i < 18; i++) {
//            map.remove(i);
//        }
//
//        p(map.getRoot());
//
//        // add some data
//        map.put(1, "Hello");
//        map.put(2, "World");
//        map.put(3, "33");
//        map.put(2, "4422");
//        map.put(5, "55");
//        map.put(6, "66");
//
//        p(map.ceilingKey(6));
//        p(map.floorKey(2));
//
//        p(map.getKeyIndex(2));
//
//        p(map.getName());
//        p(map.getSize());
//
//        p(map.higherKey(2));
//
//        map.renameMap("MVMapTest22");
//        p(map.getName());
//
//        // get the current version, for later use
//        long oldVersion = store.getCurrentVersion();
//
//        // from now on, the old version is read-only
//        store.incrementVersion();
//
//        p(map.asString("test"));
//
//        // more changes, in the new version
//        // changes can be rolled back if required
//        // changes always go into "head" (the newest version)
//        map.put(1, "Hi");
//        map.remove(2);
//
//        p(map.asString("test"));
//
//        MVMap<Integer, String> map2 = store.openMap("MVMapTest2");
//        p(map2.asString("test"));
//
//        // map.openVersion(-1);
//
//        // access the old data (before incrementVersion)
//        MVMap<Integer, String> oldMap = map.openVersion(oldVersion);
//
//        // oldMap.removeMap();
//
//        // map.openVersion(0);
//
//        map2.removeMap();
//        map.openVersion(2);
//
//        // mark the changes as committed
//        store.commit();
//
//        // print the old version (can be done
//        // concurrently with further modifications)
//        // this will print "Hello" and "World":
//        p(oldMap.get(1));
//        p(oldMap.get(2));
//        oldMap.close();
//
//        // print the newest version ("Hi")
//        p(map.get(1));
//
//        p(map.get(2));
//
//        // close the store - this doesn't write to disk
//        store.close();
//    }
// }
