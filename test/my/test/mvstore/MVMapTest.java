package my.test.mvstore;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class MVMapTest {
	public static void main(String[] args) {
		MVStore store = MVStore.open(null);

		MVMap<Integer, String> map = store.openMap("MVMapTest");
		System.out.println(map.asString("test"));

		// add some data
		map.put(1, "Hello");
		map.put(2, "World");
		map.put(3, "33");
		map.put(2, "4422");
		map.put(5, "55");
		map.put(6, "66");
		
		System.out.println(map.ceilingKey(6));
		System.out.println(map.floorKey(2));
		
		System.out.println(map.getKeyIndex(2));
		
		System.out.println(map.getName());
		System.out.println(map.getSize());
		
		System.out.println(map.higherKey(2));
		
		map.renameMap("MVMapTest22");
		System.out.println(map.getName());

		// get the current version, for later use
		long oldVersion = store.getCurrentVersion();

		// from now on, the old version is read-only
		store.incrementVersion();

		System.out.println(map.asString("test"));

		// more changes, in the new version
		// changes can be rolled back if required
		// changes always go into "head" (the newest version)
		map.put(1, "Hi");
		map.remove(2);

		System.out.println(map.asString("test"));

		MVMap<Integer, String> map2 = store.openMap("MVMapTest2");
		System.out.println(map2.asString("test"));

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
