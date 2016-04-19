package my.test.mvstore.bugs;

import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class TestCursorSkip {
	public static void main(String[] args) {
		MVStore store = MVStore.open(null);
		MVMap<Integer, String> map = store.openMap("MVMapTest");

		int n = 100;
		for (int i = 1; i <= n; i++) {
			map.put(i, "" + i);
		}

		@SuppressWarnings("unchecked")
        Cursor<Integer, ?> c = (Cursor<Integer, ?>)map.keyIterator(50);
		c.skip(40);
		while (c.hasNext()) {
			System.out.println(c.next());
		}
	}
}
