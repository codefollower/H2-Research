new Error().printStackTrace();

org.h2.command.dml.Insert.update()
	=> org.h2.command.dml.Insert.insertRows()


	org.h2.command.dml.Insert.addRow(Expression[])
	org.h2.command.dml.Insert.prepare()
	org.h2.command.dml.Insert.update()
	org.h2.command.dml.Insert.insertRows()

	=> org.h2.table.RegularTable.addRow(Session, Row)

假设:
CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255))

对于insert sql
INSERT INTO TEST(ID, NAME) VALUES(3000, 'aaa')

第1步
//解析insert sql，得到列名，如：[ID, NAME]
org.h2.command.dml.Insert.setColumns(Column[])
java.lang.Error
	at org.h2.command.dml.Insert.setColumns(Insert.java:62)
	at org.h2.command.Parser.parseInsert(Parser.java:968)
	at org.h2.command.Parser.parsePrepared(Parser.java:375)
	at org.h2.command.Parser.parse(Parser.java:279)
	at org.h2.command.Parser.parse(Parser.java:251)
	at org.h2.command.Parser.prepareCommand(Parser.java:217)
	at org.h2.engine.Session.prepareLocal(Session.java:415)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:253)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)

第2步
//解析insert sql，得到列值，如：[3000, 'aaa']
org.h2.command.dml.Insert.addRow(Expression[])
java.lang.Error
	at org.h2.command.dml.Insert.addRow(Insert.java:75)
	at org.h2.command.Parser.parseInsert(Parser.java:993)
	at org.h2.command.Parser.parsePrepared(Parser.java:375)
	at org.h2.command.Parser.parse(Parser.java:279)
	at org.h2.command.Parser.parse(Parser.java:251)
	at org.h2.command.Parser.prepareCommand(Parser.java:217)
	at org.h2.engine.Session.prepareLocal(Session.java:415)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:253)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)

第3步
org.h2.command.dml.Insert.prepare()
java.lang.Error
	at org.h2.command.dml.Insert.prepare(Insert.java:215)
	at org.h2.command.Parser.prepareCommand(Parser.java:218)
	at org.h2.engine.Session.prepareLocal(Session.java:415)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:253)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)

第4步
org.h2.command.dml.Insert.update()
java.lang.Error
	at org.h2.command.dml.Insert.update(Insert.java:79)
	at org.h2.command.CommandContainer.update(CommandContainer.java:75)
	at org.h2.command.Command.executeUpdate(Command.java:230)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:328)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)


org.h2.table.RegularTable.addRow(Session, Row)
org.h2.index.PageDataIndex.add(Session, Row)
org.h2.index.PageDataIndex.addTry(Session, Row)

org.h2.index.PageDataLeaf.addRowTry(Row)

在PageDataLeaf中的org.h2.index.PageData.keys 存放key
在org.h2.index.PageDataLeaf.rows中存放多行数据

每个PageDataLeaf被放入org.h2.store.PageStore.cache中
在org.h2.store.PageStore.writeBack()中把每个PageDataLeaf的内容写回硬盘。

java.lang.Error
	at org.h2.util.CacheLRU.addToFront(CacheLRU.java:225)
	at org.h2.util.CacheLRU.update(CacheLRU.java:127)
	at org.h2.store.PageStore.update(PageStore.java:1059)
	at org.h2.index.PageDataLeaf.addRowTry(PageDataLeaf.java:206)
	at org.h2.index.PageDataIndex.addTry(PageDataIndex.java:167)
	at org.h2.index.PageDataIndex.add(PageDataIndex.java:130)
	at org.h2.table.RegularTable.addRow(RegularTable.java:121)
	at org.h2.command.dml.Insert.insertRows(Insert.java:124)
	at org.h2.command.dml.Insert.update(Insert.java:84)
	at org.h2.command.CommandContainer.update(CommandContainer.java:75)
	at org.h2.command.Command.executeUpdate(Command.java:230)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:328)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)

java.lang.Exception
	at org.h2.index.PageDataNode.create(PageDataNode.java:66)
	at org.h2.index.PageDataIndex.addTry(PageDataIndex.java:181)
	at org.h2.index.PageDataIndex.add(PageDataIndex.java:130)
	at org.h2.table.RegularTable.addRow(RegularTable.java:121)
	at org.h2.command.dml.Insert.insertRows(Insert.java:124)
	at org.h2.command.dml.Insert.update(Insert.java:84)
	at org.h2.command.CommandContainer.update(CommandContainer.java:75)
	at org.h2.command.Command.executeUpdate(Command.java:230)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:328)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Unknown Source)


java.lang.Error
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



第一次建完表后会首选创建一个root PageDataLeaf
见org.h2.index.PageDataIndex.PageDataIndex(RegularTable, int, IndexColumn[], IndexType, boolean, Session)
		if (create) {
            rootPageId = store.allocatePage();
            store.addMeta(this, session);
            PageDataLeaf root = PageDataLeaf.create(this, rootPageId, PageData.ROOT);
            store.update(root);

建立PageDataLeaf时会先写头，此时checksum还没有，还是0
PageDataLeaf放入org.h2.store.PageStore.cache


