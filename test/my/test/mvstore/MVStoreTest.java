package my.test.mvstore;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.store.fs.FileUtils;

public class MVStoreTest {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new MVStoreTest().run();
	}

	void run() {
		storeTest();
		//rollbackToTest();
	}
	
	void storeTest() {
		MVStore store = getMVStore();

		MVMap<Integer, String> map = store.openMap("data");
		MVMap<Integer, String> map2 = store.openMap("data2");

		System.out.println(store.getCurrentVersion());

		int n = 100;
		for (int i = 0; i < n; i++) {
			map.put(i, "Hello" + i);
			map2.put(i, "Hello" + i);
		}

		long oldVersion = store.getCurrentVersion();

		System.out.println(map.getSize());

		store.incrementVersion();

		map.put(101, "Hello101");
		map.put(102, "Hello102");

		store.rollbackTo(0);
		store.rollbackTo(oldVersion + 1);

		store.commit();

		map = store.openMap("data");
		System.out.println(map.getSize());

		store.close();
	}

	void rollbackToTest() {
		MVStore store = getMVStore();

		MVMap<Integer, String> map = store.openMap("data");
		MVMap<Integer, String> map2 = store.openMap("data2");

		System.out.println(store.getCurrentVersion());

		int n = 100;
		for (int i = 0; i < n; i++) {
			map.put(i, "Hello" + i);
			map2.put(i, "Hello" + i);
		}

		long oldVersion = store.getCurrentVersion();

		System.out.println(map.getSize());

		store.incrementVersion();

		map.put(101, "Hello101");
		map.put(102, "Hello102");

		store.rollbackTo(0);
		store.rollbackTo(oldVersion + 1);

		store.commit();

		map = store.openMap("data");
		System.out.println(map.getSize());

		store.close();
	}

	MVStore getMVStore() {
		MVStore.Builder builder = new MVStore.Builder();
		builder.cacheSize(10).compressData().readOnly().writeDelay(2000);

		String str = builder.toString();

		//System.out.println(str);
		builder = MVStore.Builder.fromString(str);

		// open the store (in-memory if fileName is null)
		String fileName = null;
		fileName = "E:/H2/baseDir/MVStoreTest333";
		MVStore store = null;
		FileUtils.deleteRecursive(fileName, true);

		//store = MVStore.open(fileName);

		builder = new MVStore.Builder();
		builder.writeDelay(0).fileName(fileName);

		store = builder.open();

		return store;
	}

	public static void main2(String[] args) {
		MVStore.Builder builder = new MVStore.Builder();
		builder.cacheSize(10).compressData().readOnly().writeDelay(2000);

		String str = builder.toString();

		System.out.println(str);
		builder = MVStore.Builder.fromString(str);

		// open the store (in-memory if fileName is null)
		String fileName = null;
		fileName = "E:/H2/baseDir/MVStoreTest333";
		MVStore store = null;
		FileUtils.deleteRecursive(fileName, true);

		//store = MVStore.open(fileName);

		builder = new MVStore.Builder();
		builder.writeDelay(0).fileName(fileName);

		store = builder.open();

		System.out.println(store.getRetainVersion());
		System.out.println(store.getRetentionTime());
		System.out.println(store.getReuseSpace());
		System.out.println(store.getStoreVersion());
		System.out.println(store.getUnsavedPageCount());

		// create/get the map named "data"
		MVMap<Integer, String> map = store.openMap("data");

		int n = 100;
		for (int i = 0; i < n; i++) {
			map.put(i, "Hello" + i);

			if (i % 20 == 0)
				store.incrementVersion();
		}

		store.commit();

		// add some data
		map.put(1, "Hello");
		map.put(2, "World");

		// get the current version, for later use
		long oldVersion = store.getCurrentVersion();

		// from now on, the old version is read-only
		store.incrementVersion();

		// more changes, in the new version
		// changes can be rolled back if required
		// changes always go into "head" (the newest version)
		map.put(1, "Hi");
		map.remove(2);

		// access the old data (before incrementVersion)
		MVMap<Integer, String> oldMap = map.openVersion(oldVersion);

		// mark the changes as committed
		store.commit();

		// print the old version (can be done
		// concurrently with further modifications)
		// this will print "Hello" and "World":
		System.out.println(oldMap.get(1));
		System.out.println(oldMap.get(2));
		oldMap.close();

		// print the newest version ("Hi")
		System.out.println(map.get(1));

		System.out.println(map.get(2));

		// close the store - this doesn't write to disk
		store.close();
	}

}
