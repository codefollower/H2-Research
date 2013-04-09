层次图: 由大到小

RegularTable => PageDataIndex => PageDataNode => PageDataLeaf

最初是一个root PageDataLeaf，当PageDataLeaf很大时会分裂，变成PageDataNode和两个子PageDataLeaf

一个PageDataLeaf页可以放多行记录，先放到内容中保存在PageDataLeaf的rows字段中


添加新行的过程：

先写到内存：
org.h2.table.RegularTable.addRow(Session, Row)
	=> org.h2.index.PageDataIndex.add(Session, Row)
		=> org.h2.index.PageDataIndex.addTry(Session, Row)
			=> org.h2.index.PageDataLeaf.addRowTry(Row)
				=> rows = insert(rows, entryCount, x, row);

再写回硬盘:
	at org.h2.index.PageDataLeaf.write(PageDataLeaf.java:456)
	at org.h2.store.PageStore.writeBack(PageStore.java:1009)
	at org.h2.store.PageStore.writeBack(PageStore.java:412)
	at org.h2.store.PageStore.checkpoint(PageStore.java:430)
	at org.h2.engine.Database.closeOpenFilesAndUnlock(Database.java:1196)
	at org.h2.engine.Database.close(Database.java:1149)
	at org.h2.engine.Database.removeSession(Database.java:1028)
	at org.h2.engine.Session.close(Session.java:563)
	at org.h2.server.TcpServerThread.closeSession(TcpServerThread.java:175)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:270)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)



在org.h2.engine.Database.addMeta(Session, DbObject)中存表元数据
( /* key:26 */ 14, 0, 0, STRINGDECODE('CREATE CACHED TABLE PUBLIC.TEST(\n    ID INT NOT NULL,\n    NAME VARCHAR(255)\n)'))


