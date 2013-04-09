
在H2中，每个数据库的表记录和表元数据放在一个以"h2.db"结尾的文件中，
比如"test.h2.db"就是存放"test"这个数据库的文件


"h2.db"文件的格式:

如果"h2.db"文件不需要加密，下列三个字段都相同，都是"-- H2 0.5/B -- \n"(16个字节)
此文为了简单起见，不考虑加密的情况

字节数  用意
========================
16      magic
16      salt
16      magic
(这三项的写入和读取在org.h2.store.FileStore.init())

接下来是每个Page

StaticHeader
=================
4       pageSize 默认是2048(2K)
1       WRITE_VERSION 常量3
1       READ_VERSION 常量3
1994    保留
(这4项的写入在org.h2.store.PageStore.writeStaticHeader())
(以上7项刚好是2k)

VariableHeader
=================
4       CRC
8       writeCount
4       logKey
4       logFirstTrunkPage
4       logFirstDataPage
2024    保留(补够2k)

4       CRC
8       writeCount
4       logKey
4       logFirstTrunkPage
4       logFirstDataPage
2024    保留(补够2k)
(上面两者相同，后者只有在前者无效时(比如CRC错了)才有用)
(这12项的写入在org.h2.store.PageStore.writeVariableHeader())

第一个PageFreeList的pageId是3

第4个page也就是pageId=4的页放metaIndex

java.lang.Error
	at org.h2.store.PageStore.allocatePage(PageStore.java:1113)
	at org.h2.store.PageStore.update(PageStore.java:1062)
	at org.h2.index.PageDataIndex.getPage(PageDataIndex.java:231)
	at org.h2.index.PageDataIndex.<init>(PageDataIndex.java:82)
	at org.h2.table.RegularTable.<init>(RegularTable.java:86)
	at org.h2.store.PageStore.openMetaIndex(PageStore.java:1570)
	at org.h2.store.PageStore.openNew(PageStore.java:306)
	at org.h2.store.PageStore.open(PageStore.java:288)
	at org.h2.engine.Database.getPageStore(Database.java:2123)
	at org.h2.engine.Database.open(Database.java:582)
	at org.h2.engine.Database.openDatabase(Database.java:222)
	at org.h2.engine.Database.<init>(Database.java:217)
	at org.h2.engine.Engine.openSession(Engine.java:56)
	at org.h2.engine.Engine.openSession(Engine.java:159)
	at org.h2.engine.Engine.createSessionAndValidate(Engine.java:138)
	at org.h2.engine.Engine.createSession(Engine.java:121)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:136)
	at java.lang.Thread.run(Thread.java:662)


第5个page也就是pageId=5的页放PageStreamTrunk

然后再预分配PageStreamTrunk.getPagesAddressed(pageSize)个页给PageStreamData

先写PageStreamTrunk
java.lang.Error
	at org.h2.store.PageStore.writePage(PageStore.java:1337)
	at org.h2.store.PageStreamTrunk.write(PageStreamTrunk.java:140)
	at org.h2.store.PageOutputStream.initNextData(PageOutputStream.java:100)
	at org.h2.store.PageOutputStream.reserve(PageOutputStream.java:78)
	at org.h2.store.PageLog.openForWriting(PageLog.java:186)
	at org.h2.store.PageStore.openNew(PageStore.java:308)
	at org.h2.store.PageStore.open(PageStore.java:288)
	at org.h2.engine.Database.getPageStore(Database.java:2123)
	at org.h2.engine.Database.open(Database.java:582)
	at org.h2.engine.Database.openDatabase(Database.java:222)
	at org.h2.engine.Database.<init>(Database.java:217)
	at org.h2.engine.Engine.openSession(Engine.java:56)
	at org.h2.engine.Engine.openSession(Engine.java:159)
	at org.h2.engine.Engine.createSessionAndValidate(Engine.java:138)
	at org.h2.engine.Engine.createSession(Engine.java:121)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:136)
	at java.lang.Thread.run(Thread.java:662)
