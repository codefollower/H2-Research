package my.test.mvstore.bugs;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.store.fs.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestMVStoreDataLoss {
	static MVStore getMVStore(String fileName) {
		MVStore.Builder builder = new MVStore.Builder();
		builder.writeDelay(100).fileName(fileName);

		MVStore store = builder.open();
		store.setPageSize(512);

		return store;
	}

	@Test
	public void testDataLoss() throws Exception {
		String fileName = "E:/H2/baseDir/testDataLoss";
		FileUtils.deleteRecursive(fileName, true);

		MVStore store = getMVStore(fileName);

		MVMap<Integer, String> map = store.openMap("MVMapTest");

		map.put(1, "1");
		map.put(2, "2");
		map.put(3, "3");

		Assert.assertEquals(3, map.size()); //ok

		store.commit();
		//store.store();
		store.close();

		store = getMVStore(fileName);
		map = store.openMap("MVMapTest");

		Assert.assertEquals(3, map.size()); //failed
	}
}
