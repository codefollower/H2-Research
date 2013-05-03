package my.test.mvstore;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.TransactionStore;
import org.h2.mvstore.db.TransactionStore.Transaction;
import org.h2.mvstore.db.TransactionStore.TransactionMap;

//import org.h2.store.fs.FileUtils;

public class TransactionStoreTest {

	public static void main(String[] args) {
		String fileName = null;
		fileName = "E:/H2/baseDir/TransactionStoreTest";
		MVStore s = null;
		//FileUtils.deleteRecursive(fileName, true);

		s = MVStore.open(fileName);
		TransactionStore ts = new TransactionStore(s);
		Transaction tx;
		TransactionMap<String, String> m;
		long startUpdate;
		@SuppressWarnings("unused")
		long version;

		tx = ts.begin();

		// start of statement
		// create table test
		startUpdate = tx.setSavepoint();
		tx.openMap("test");

		// start of statement
		// insert into test(id, name) values(1, 'Hello'), (2, 'World')
		startUpdate = tx.setSavepoint();
		version = s.getCurrentVersion();
		m = tx.openMap("test");

		int n = 100;
		for (int i = 0; i < n; i++) {
			m.put(i + "", "Hello" + i);

			if (i % 20 == 0)
				s.incrementVersion();
		}

		long savepoint = tx.setSavepoint();
		m.put(2 + "", "Hello22");

		System.out.println(m.get("2"));

		m.setSavepoint(savepoint);

		System.out.println(m.get("2"));

		System.out.println(m.trySet("1", "Hello", true));
		System.out.println(m.trySet("2", "World", true));
		// not seen yet (within the same statement)
		System.out.println(m.get("1"));
		System.out.println(m.get("2"));

		// start of statement
		startUpdate = tx.setSavepoint();
		version = s.getCurrentVersion();
		// now we see the newest version
		m = tx.openMap("test");
		System.out.println("Hello=" + m.get("1"));
		System.out.println("World=" + m.get("2"));
		// update test set primaryKey = primaryKey + 1
		// (this is usually a tricky cases)
		System.out.println("Hello=" + m.get("1"));
		System.out.println(m.trySet("1", null, true));
		System.out.println(m.trySet("2", "Hello", true));
		System.out.println("World=" + m.get("2"));
		// already updated by this statement, so it has no effect
		// but still returns true because it was changed by this transaction
		System.out.println(m.trySet("2", null, true));

		System.out.println(m.trySet("3", "World", true));
		// not seen within this statement
		System.out.println("Hello=" + m.get("1"));
		System.out.println("World=" + m.get("2"));
		System.out.println(m.get("3"));

		// start of statement
		startUpdate = tx.setSavepoint();
		version = s.getCurrentVersion();
		m = tx.openMap("test");
		// select * from test
		System.out.println(m.get("1"));
		System.out.println("Hello=" + m.get("2"));
		System.out.println("World=" + m.get("3"));

		// start of statement
		startUpdate = tx.setSavepoint();
		version = s.getCurrentVersion();
		m = tx.openMap("test");
		// update test set id = 1
		// should fail: duplicate key
		System.out.println(m.trySet("2", null, true));
		System.out.println(m.trySet("1", "Hello", true));
		System.out.println(m.trySet("3", null, true));
		System.out.println(m.trySet("1", "World", true));
		tx.rollbackToSavepoint(startUpdate);

		version = s.getCurrentVersion();
		m = tx.openMap("test");
		System.out.println(m.get("1"));
		System.out.println("Hello=" + m.get("2"));
		System.out.println("World=" + m.get("3"));

		tx.commit();

		ts.close();
		s.close();

	}

}
