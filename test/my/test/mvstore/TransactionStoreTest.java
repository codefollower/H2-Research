package my.test.mvstore;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.TransactionStore;
import org.h2.mvstore.db.TransactionStore.Transaction;
import org.h2.mvstore.db.TransactionStore.TransactionMap;

public class TransactionStoreTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MVStore s = MVStore.open(null);
		TransactionStore ts = new TransactionStore(s);
		Transaction tx;
		TransactionMap<String, String> m;
		long startUpdate;
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
		m = tx.openMap("test", version);
		System.out.println(m.trySet("1", "Hello", true));
		System.out.println(m.trySet("2", "World", true));
		// not seen yet (within the same statement)
		System.out.println(m.get("1"));
		System.out.println(m.get("2"));

		// start of statement
		startUpdate = tx.setSavepoint();
		version = s.getCurrentVersion();
		// now we see the newest version
		m = tx.openMap("test", version);
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
		m = tx.openMap("test", version);
		// select * from test
		System.out.println(m.get("1"));
		System.out.println("Hello=" + m.get("2"));
		System.out.println("World=" + m.get("3"));

		// start of statement
		startUpdate = tx.setSavepoint();
		version = s.getCurrentVersion();
		m = tx.openMap("test", version);
		// update test set id = 1
		// should fail: duplicate key
		System.out.println(m.trySet("2", null, true));
		System.out.println(m.trySet("1", "Hello", true));
		System.out.println(m.trySet("3", null, true));
		System.out.println(m.trySet("1", "World", true));
		tx.rollbackToSavepoint(startUpdate);

		version = s.getCurrentVersion();
		m = tx.openMap("test", version);
		System.out.println(m.get("1"));
		System.out.println("Hello=" + m.get("2"));
		System.out.println("World=" + m.get("3"));

		tx.commit();

		ts.close();
		s.close();

	}

}
