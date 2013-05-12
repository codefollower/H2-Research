package my.test.mvstore.bugs;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.junit.Assert;
import org.junit.Test;

public class TestGetMinMax {
	@Test
	public void testGetMinMax() throws Exception {
		MVStore store = MVStore.open(null);
		store.setPageSize(512);

		MVMap<Integer, String> map = store.openMap("MVMapTest");

		int n = 100;
		for (int i = 1; i <= n; i++) {
			map.put(i, "" + i);
		}

		Integer key = 3;

		Assert.assertEquals(4, map.higherKey(key).intValue()); //ok
		Assert.assertEquals(3, map.ceilingKey(key).intValue()); //ok

		Assert.assertEquals(2, map.lowerKey(key).intValue()); //ok
		Assert.assertEquals(3, map.floorKey(key).intValue()); //ok

		key = null;

		Assert.assertEquals(100, map.higherKey(key).intValue()); //failed
		Assert.assertEquals(100, map.ceilingKey(key).intValue()); //failed

		Assert.assertEquals(1, map.lowerKey(key).intValue()); //ok
		Assert.assertEquals(1, map.floorKey(key).intValue()); //ok
	}
}
