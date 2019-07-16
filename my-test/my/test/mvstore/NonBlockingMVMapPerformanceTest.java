package my.test.mvstore;

import java.util.ArrayList;
import java.util.Collections;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

//以单元测试的方式运行会比通过main方法运行得出稍微慢一些的测试结果，
//这可能是因为单元测试额外启动了一个ReaderThread占用了一些资源
public class NonBlockingMVMapPerformanceTest {

    public static void main(String[] args) throws Exception {
        new NonBlockingMVMapPerformanceTest().run();
    }

    static int threadsCount = 4; // Runtime.getRuntime().availableProcessors() * 4;
    static int count = 500000 * 1;// 50000;

    int[] randomKeys = getRandomKeys();
    MVMap<Integer, String> map;

    public void run() {
        // MVStore.Builder builder = new MVStore.Builder();
        MVStore store = MVStore.open(null);
        map = store.openMap("NonBlockingMVMapPerformanceTest");
        // singleThreadWrite();
        int loop = 20;
        for (int i = 1; i <= loop; i++) {
            map.clear();

            //singleThreadRandomWrite();
            // singleThreadSerialWrite();

            // singleThreadRandomRead();
            // singleThreadSerialRead();

             multiThreadsRandomWrite(i);
            // multiThreadsSerialWrite(i);

             //multiThreadsRandomRead(i);
            // multiThreadsSerialRead(i);
        }
    }

    int[] getRandomKeys() {
        ArrayList<Integer> list = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            list.add(i);
        }
        Collections.shuffle(list);
        int[] keys = new int[count];
        for (int i = 0; i < count; i++) {
            keys[i] = list.get(i);
        }
        return keys;
    }

    void singleThreadSerialWrite() {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            map.put(i, "valueaaa");
        }
        long t2 = System.currentTimeMillis();
        System.out.println("single-thread serial write time: " + (t2 - t1) + " ms, count: " + count);
    }

    void singleThreadRandomWrite() {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            map.put(randomKeys[i], "valueaaa");
        }
        long t2 = System.currentTimeMillis();
        System.out.println("single-thread random write time: " + (t2 - t1) + " ms, count: " + count);
    }

    void singleThreadSerialRead() {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            map.get(i);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("single-thread serial read time: " + (t2 - t1) + " ms, count: " + count);
    }

    void singleThreadRandomRead() {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            map.get(randomKeys[i]);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("single-thread random read time: " + (t2 - t1) + " ms, count: " + count);
    }

    class MyThread extends Thread {
        int start;
        int end;
        boolean read;
        boolean random;

        long readTime;
        long writeTime;
        String timeStr;

        MyThread(int start, int end, boolean read, boolean random) {
            super("MyThread-" + start);
            this.start = start;
            this.end = end;
            this.read = read;
            this.random = random;
        }

        void write() throws Exception {
            for (int i = start; i < end; i++) {
                int key;
                if (random)
                    key = randomKeys[i];
                else
                    key = i;
                String value = "value-";// "value-" + key;
                map.put(key, value);
            }
        }

        void read() throws Exception {
            for (int i = start; i < end; i++) {
                if (random)
                    map.get(randomKeys[i]);
                else
                    map.get(i);
            }
        }

        @Override
        public void run() {
            try {
                long t1 = System.currentTimeMillis();
                if (read) {
                    read();
                } else {
                    write();
                }
                long t2 = System.currentTimeMillis();
                if (read) {
                    readTime = t2 - t1;
                } else {
                    writeTime = t2 - t1;
                }
                timeStr = (getName() + (random ? " random " : " serial ") + (read ? "read" : "write") + " end, time: "
                        + (t2 - t1) + " ms, count: " + (end - start));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void multiThreadsSerialRead(int loop) {
        multiThreads(loop, true, false);
    }

    void multiThreadsRandomRead(int loop) {
        multiThreads(loop, true, true);
    }

    void multiThreadsSerialWrite(int loop) {
        multiThreads(loop, false, false);
    }

    void multiThreadsRandomWrite(int loop) {
        multiThreads(loop, false, true);
    }

    void multiThreads(int loop, boolean read, boolean random) {
        int avg = count / threadsCount;
        MyThread[] threads = new MyThread[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            int start = i * avg;
            int end = (i + 1) * avg;
            if (i == threadsCount - 1)
                end = count;
            threads[i] = new MyThread(start, end, read, random);
        }

        for (int i = 0; i < threadsCount; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threadsCount; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long timeSum = 0;
        if (read) {
            for (int i = 0; i < threadsCount; i++) {
                timeSum += threads[i].readTime;
            }
        } else {
            for (int i = 0; i < threadsCount; i++) {
                timeSum += threads[i].writeTime;
            }
        }
        System.out.println();
        System.out.println("loop: " + loop + ", threads: " + threadsCount + ", count: " + count);
        System.out.println("==========================================================");
        for (int i = 0; i < threadsCount; i++) {
            System.out.println(threads[i].timeStr);
        }
        System.out.println("multi-threads" + (random ? " random " : " serial ") + (read ? "read" : "write")
                + " time, sum: " + timeSum + " ms, avg: " + (timeSum / threadsCount) + " ms");
        System.out.println("==========================================================");
    }
}
