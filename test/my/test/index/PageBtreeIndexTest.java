package my.test.index;

import my.test.TestBase;

public class PageBtreeIndexTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new PageBtreeIndexTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("PAGE_SIZE", "2048");
		
		//prop.setProperty("mode", "Derby");
		//prop.setProperty("mode", "oracle");
	}

	@Override
	public void startInternal() throws Exception {
		//find();
		//findFirstOrLast();

		//split();
		//remove();
		containsNullAndAllowMultipleNull();
	}
	public void containsNullAndAllowMultipleNull() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int, name varchar(500), b boolean)");
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id DESC, name ASC) ");
		stmt.executeUpdate("CREATE Unique INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id, name) ");

		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(5, 'a2', false)");
//		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(30, null, true)");
//		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(30, null, true)");
//		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(null, null, true)");
//		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(null, null, true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(40, 'a2', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(70, 'b3', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(70, 'b3', true)");
		
	}
	public void find() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int, name varchar(500), b boolean)");
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id DESC, name ASC) ");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id, name) ");

		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(40, 'a2', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(70, 'b3', true)");
		
		//stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(20, 'b1', true)");
		
		stmt.executeUpdate("delete from IndexTestTable where id=20");

		sql = "select * from IndexTestTable where id> 20 AND name='a2'";
		sql = "select * from IndexTestTable where id= 40 AND name='a2'";
		executeQuery();
	}

	public void findFirstOrLast() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id)");

		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(40, 'a2', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(70, 'b3', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(null, 'b3', true)");

		sql = "select min(id) from IndexTestTable";
		executeQuery();

		sql = "select max(id) from IndexTestTable";
		executeQuery();
	}

	public void split() throws Exception {
		//		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		//		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int, name varchar(500), address varchar(500))");
		//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id, name)");
		//
		//		long ii = 1000000001L;
		//		int count = 100;
		//		count = 40;
		//		for (int i = 1; i <= count; i++) {
		//			//stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + i + ", '" + s + "abcdef1234')");
		//			//stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
		//			stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
		//			ii++;
		//		}

		sql = "select * from IndexTestTable where id> 20 AND name='a2'";

		sql = "select * from IndexTestTable where id=28";
		executeQuery();

		sql = "select * from IndexTestTable where id=28";
	}

	public void remove() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int, name varchar(500), address varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id, name)");

		long ii = 1000000001L;
		int count = 100;
		count = 2;
		for (int i = 1; i <= count; i++) {
			//stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + i + ", '" + s + "abcdef1234')");
			//stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
			stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
			ii++;
		}

		stmt.executeUpdate("delete from IndexTestTable where id=1");
		sql = "select * from IndexTestTable where id> 20 AND name='a2'";

		sql = "select * from IndexTestTable where id=28";
		sql = "select * from IndexTestTable";
		executeQuery();
	}

	//@Override
	public void startInternal2() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTestTableIndex ON IndexTestTable(id, name)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into IndexTestTable(id, name, b) values(70, 'b3', true)");

		sql = "select * from IndexTestTable where name='1234567890'";
		executeQuery();

		//stmt.executeUpdate("create TEMPORARY table IF NOT EXISTS IndexTestTable(id int, name varchar(500),CONSTRAINT myindex INDEX (name)) TRANSACTIONAL");
		//stmt.executeUpdate("create table IF NOT EXISTS IndexTestTable(id int, name varchar(500) not null)");

		//stmt.executeUpdate("create LOCAL TEMPORARY table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null)");
		//stmt.executeUpdate("CREATE UNIQUE HASH INDEX idx_name ON IndexTestTable(name)");
		//stmt.executeUpdate("CREATE PRIMARY KEY HASH idx_name ON IndexTestTable(id)");

		//stmt.executeUpdate("create table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null)");
		//stmt.executeUpdate("CREATE primary key idx_name ON IndexTestTable(id)");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + 120 + ", '"+110 +"abcdef1234')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + 110 + ", '"+110 +"abcdef1234')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + 130 + ", '"+110 +"abcdef1234')");

		//测试TreeIndex
		//stmt.executeUpdate("create LOCAL TEMPORARY table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null) NOT PERSISTENT");
		//stmt.executeUpdate("CREATE index idx_name ON IndexTestTable(id)");

		//测试PageBtreeIndex
		//找断点条件indexName.equalsIgnoreCase("idx_name"); getName().equalsIgnoreCase("idx_name");
		//index.getName().equalsIgnoreCase("idx_name");
		stmt.executeUpdate("create table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null, address varchar(500) not null)");
		stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON IndexTestTable(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON IndexTestTable(name desc)");
		long ii = 1000000001L;
		for (int i = 1; i <= 100; i++) {
			//stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + i + ", '" + s + "abcdef1234')");
			//stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
			stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
			ii++;
		}

		//		stmt.executeUpdate("delete from IndexTestTable where id=2");
		//		stmt.executeUpdate("delete from IndexTestTable where id>300");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");
		//
		//		stmt.executeUpdate("update IndexTestTable set name='1234567890' where id>10");

		sql = "select name,id from IndexTestTable where name='1234567890'";
		executeQuery();

		stmt.executeUpdate("TRUNCATE TABLE IndexTestTable");
		stmt.executeUpdate("drop index IF EXISTS idx_name");

	}
}
