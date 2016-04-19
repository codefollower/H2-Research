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

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.Page;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.ObjectDataType;

public class LockFreeMVMap2<K, V> extends MVMap<K, V> implements Callable<Void> {

    private static Merger merger;
    static {
        merger = new Merger();
        merger.start();
    }

    public static void stopMerger() {
        merger.stopMerger();
    }

    private static class ValueHolder<V> {
        final V value;
        //final byte tag;

        ValueHolder(V value, int tag) {
            this.value = value;
            //this.tag = (byte) tag;
        }

        ValueHolder(V value) {
            this(value, 0);
        }
    }

    private volatile ConcurrentSkipListMap<K, V> current = new ConcurrentSkipListMap<K, V>();
    private volatile ConcurrentSkipListMap<K, ValueHolder<V>> current2 = new ConcurrentSkipListMap<K, ValueHolder<V>>();
    private volatile ConcurrentSkipListMap<K, V> merging;
    private volatile ConcurrentSkipListMap<K, ValueHolder<V>> merging2 = new ConcurrentSkipListMap<K, ValueHolder<V>>();
    private volatile ConcurrentSkipListSet<Object> removed;

    public LockFreeMVMap2(DataType keyType, DataType valueType) {
        super(keyType, valueType);

        merger.addMap(this);
    }

    @Override
    public V put(K key, V value) {
        current2.put(key, new ValueHolder<V>(value));

        return current.put(key, value);
    }

    @Override
    public V get(Object key) {
        V v = current.get(key);
        if (v != null)
            return v;

        ValueHolder<V> vh = current2.get(key);
        if (vh != null) {
            if (vh.value == null)
                return null;
            return vh.value;
        }

        if (merging != null) {
            v = merging.get(key);
            if (v != null)
                return v;
        }

        v = super.get(key);
        if (v != null && removed.contains(key))
            return null;

        return v;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        V v = current.remove(key);
        if (v != null)
            return v;

        ValueHolder<V> vh = current2.remove(key);
        if (vh != null) {
            if (vh.value == null)
                return null;

            return vh.value;
        }

        if (merging != null) {
            v = merging.remove(key);
            if (v != null)
                return v;
        }

        v = super.get(key);
        if (v != null) {
            removed.add(key);

            current2.put((K) key, new ValueHolder<V>(null));
        }
        return v;
    }

    @Override
    public Void call() throws Exception {
        beginMerge();
        return null;
    }

    private void beginMerge() {
        merging = current;
        current = new ConcurrentSkipListMap<K, V>();

        merging2 = current2;
        current2 = new ConcurrentSkipListMap<K, ValueHolder<V>>();

        merge();

        merging = null;
        merging2 = null;
    }

    private void merge() {
        beforeWrite();

        long v = writeVersion;
        Page p;
        Object key;
        Object value;

        for (Entry<K, V> e : merging.entrySet()) {
            p = root.copy(v);
            key = e.getKey();
            value = e.getValue();
            p = splitRootIfNeeded(p, v);
            put(p, v, key, value);
            newRoot(p);
        }

        for (Entry<K, ValueHolder<V>> e : merging2.entrySet()) {
            key = e.getKey();
            value = e.getValue().value;

            p = root.copy(v);

            if (value != null) {
                p = splitRootIfNeeded(p, v);
                put(p, v, key, value);
            } else {
                remove(p, v, key);
                if (!p.isLeaf() && p.getTotalCount() == 0) {
                    p.removePage();
                    //p = Page.createEmpty(this, p.getVersion());
                }
            }

            newRoot(p);
        }

        for (Object k : removed) {
            p = root.copy(v);
            remove(p, v, k);
            if (!p.isLeaf() && p.getTotalCount() == 0) {
                p.removePage();
                //p = Page.createEmpty(this, p.getVersion());
            }
            newRoot(p);

            removed.remove(k);
        }
    }

    public static class Merger extends Thread {
        private static final ExecutorService executorService = Executors.newCachedThreadPool();

        private volatile boolean isRunning;
        private final ArrayList<LockFreeMVMap2<?, ?>> maps = new ArrayList<LockFreeMVMap2<?, ?>>();
        private final ArrayList<Future<Void>> futures = new ArrayList<Future<Void>>();

        public synchronized void addMap(LockFreeMVMap2<?, ?> map) {
            maps.add(map);
        }

        public Merger() {
            super("BTree-Merger");
        }

        @Override
        public void run() {
            isRunning = true;
            long millis = 5 * 60 * 1000;
            while (isRunning) {
                try {
                    sleep(millis);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                for (LockFreeMVMap2<?, ?> map : maps) {
                    futures.add(executorService.submit(map));
                }

                for (Future<Void> f : futures) {
                    try {
                        f.get();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }

                futures.clear();
            }
        }

        public void stopMerger() {
            isRunning = false;
            interrupt();
            try {
                join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

            executorService.shutdown();
        }
    }

    /**
     * A builder for this class.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public static class Builder<K, V> implements MapBuilder<LockFreeMVMap2<K, V>, K, V> {

        protected DataType keyType;
        protected DataType valueType;

        /**
         * Create a new builder with the default key and value data types.
         */
        public Builder() {
            // ignore
        }

        /**
         * Set the key data type.
         *
         * @param keyType the key type
         * @return this
         */
        public Builder<K, V> keyType(DataType keyType) {
            this.keyType = keyType;
            return this;
        }

        /**
         * Set the key data type.
         *
         * @param valueType the key type
         * @return this
         */
        public Builder<K, V> valueType(DataType valueType) {
            this.valueType = valueType;
            return this;
        }

        @Override
        public LockFreeMVMap2<K, V> create() {
            if (keyType == null) {
                keyType = new ObjectDataType();
            }
            if (valueType == null) {
                valueType = new ObjectDataType();
            }
            return new LockFreeMVMap2<K, V>(keyType, valueType);
        }
    }
}