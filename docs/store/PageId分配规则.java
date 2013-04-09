pageId: 0
========================
文件头
---------------------
16      magic
16      salt  (如果不使用加密，这16字节就是magic，如果使用了加密，那么是随机生成的)
16      magic (如果使用了加密，那么这16字节是对magic加密后的值)

StaticHeader
---------------------
4       pageSize 默认是2048(2K)
1       WRITE_VERSION 常量3
1       READ_VERSION 常量3
1994    保留
(这4项的写入在org.h2.store.PageStore.writeStaticHeader())
(以上7项刚好是2k)


pageId: 1
========================
VariableHeader
---------------------
4       CRC
8       writeCount
4       logKey
4       logFirstTrunkPage
4       logFirstDataPage
2024    保留(补够2k)

pageId: 2 (是pageId 1的冗余，当1发生错误时会使用2)
========================
VariableHeader
---------------------
4       CRC
8       writeCount
4       logKey
4       logFirstTrunkPage
4       logFirstDataPage
2024    保留(补够2k)

page 1和2是可变的，随着数据的不断写入，前5个字段会不断的更新


pageId: 3
========================
PageFreeList

pageId: 4
========================
metaIndex

pageId: 5
========================
PageStreamTrunk





每个PageFreeList有自己的pageId，

PageFreeList也是一种page，

PageFreeList的pageId由此公式计算: 
pageId = PAGE_ID_FREE_LIST_ROOT + i * freeListPagesPerList
       = 3 + i * freeListPagesPerList
       = 3 + i * ((pageSize - DATA_START) * 8)
       = 3 + i * ((pageSize - DATA_START) * 8)
	   = 3 + i * ((pageSize - 3) * 8)
i>=0 



假设一个page的size是128字节，那么减去头三个字节后就是剩下的字节数，再乘以8以表示bit位数
一个bit就能代表一个pageId，
那么一个PageFreeList能表示的page个数是:
(128 - 3) * 8 = 1000


假设PageFreeList自己的pageId是3，那么从它分配出来的pageId范围是
3+1 到 3+1000-1

下一个PageFreeList的pageId从1003开始
PageFreeList: pageId = 3
PageFreeList: pageId = 1003
PageFreeList: pageId = 2003
PageFreeList: pageId = 3003
PageFreeList: pageId = 4003
PageFreeList: pageId = 5003
PageFreeList: pageId = 6003
PageFreeList: pageId = 7003
PageFreeList: pageId = 8003
PageFreeList: pageId = 9003
PageFreeList: pageId = 10003
PageFreeList: pageId = 11003
PageFreeList: pageId = 12003
PageFreeList: pageId = 13003
PageFreeList: pageId = 14003
PageFreeList: pageId = 15003
PageFreeList: pageId = 16003



第一个PageFreeList的pageId是4，分配给metaIndex
java.lang.Error
	at org.h2.store.PageFreeList.allocate(PageFreeList.java:138)
	at org.h2.store.PageStore.allocatePage(PageStore.java:1125)
	at org.h2.store.PageStore.update(PageStore.java:1064)
	at org.h2.index.PageDataIndex.getPage(PageDataIndex.java:233)
	at org.h2.index.PageDataIndex.<init>(PageDataIndex.java:84)
	at org.h2.table.RegularTable.<init>(RegularTable.java:86)
	at org.h2.store.PageStore.openMetaIndex(PageStore.java:1585)
	at org.h2.store.PageStore.openNew(PageStore.java:308)
	at org.h2.store.PageStore.open(PageStore.java:290)
	at org.h2.engine.Database.getPageStore(Database.java:2129)
	at org.h2.engine.Database.open(Database.java:582)
	at org.h2.engine.Database.openDatabase(Database.java:221)
	at org.h2.engine.Database.<init>(Database.java:216)
	at org.h2.engine.Engine.openSession(Engine.java:59)
	at org.h2.engine.Engine.openSession(Engine.java:167)
	at org.h2.engine.Engine.createSessionAndValidate(Engine.java:145)
	at org.h2.engine.Engine.createSession(Engine.java:127)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:136)
	at java.lang.Thread.run(Thread.java:662)
