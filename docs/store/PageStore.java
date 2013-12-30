PageStore的实例在org.h2.engine.Database.getPageStore()中生成

然后按下面的流程调用:
org.h2.store.PageStore.open()
	=> org.h2.store.PageStore.openNew() //数据库文件不存在
	
或	=> org.h2.store.PageStore.openExisting() //数据库文件已存在
		=> org.h2.engine.Database.openFile(String, String, boolean)
			=> org.h2.store.FileStore.open(DataHandler, String, String, String, byte[])
				=> org.h2.store.FileStore.FileStore(DataHandler, String, String)

			=> org.h2.store.FileStore.init()


PAGE_INDEX表
CREATE CACHED TABLE "".PAGE_INDEX(
    ID INTEGER,
    TYPE INTEGER,
    PARENT INTEGER,
    HEAD INTEGER,
    OPTIONS VARCHAR,
    COLUMNS VARCHAR
)
    private void openMetaIndex() {
        CreateTableData data = new CreateTableData();
        ArrayList<Column> cols = data.columns;
        cols.add(new Column("ID", Value.INT)); //index id
        cols.add(new Column("TYPE", Value.INT)); //对应META_TYPE_DATA_INDEX和META_TYPE_BTREE_INDEX
        cols.add(new Column("PARENT", Value.INT)); //table id
        cols.add(new Column("HEAD", Value.INT)); //RootPageId
        cols.add(new Column("OPTIONS", Value.STRING)); //CompareMode name，Strength，临时表，d(表示是PageDelegateIndex)
        cols.add(new Column("COLUMNS", Value.STRING)); //列id/sortType
        metaSchema = new Schema(database, 0, "", null, true);
        data.schema = metaSchema;
        data.tableName = "PAGE_INDEX";
        data.id = META_TABLE_ID; //id是-1
        data.temporary = false;
        data.persistData = true;
        data.persistIndexes = true;
        data.create = false;
        data.session = systemSession;
        metaTable = new RegularTable(data);
        metaIndex = (PageDataIndex) metaTable.getScanIndex(
                systemSession);
        metaObjects.clear();
        metaObjects.put(-1, metaIndex);
    }

//内存数据库不会生成PageStore实例

//每个Page的size默认是2k
//一个PageStore的 实例就代表一个".h2.db"文件


最先开始调用org.h2.store.PageStore.allocatePage()的是在client端发起的非内存数据库访问时
在org.h2.engine.Database.open的meta = mainSchema.createTable(data)那里
-------------------------------------------------------------
java.lang.Error
	at org.h2.index.PageDataIndex.<init>(PageDataIndex.java:78)
	at org.h2.table.RegularTable.<init>(RegularTable.java:86)
	at org.h2.schema.Schema.createTable(Schema.java:556)
	at org.h2.engine.Database.open(Database.java:622)
	at org.h2.engine.Database.openDatabase(Database.java:221)
	at org.h2.engine.Database.<init>(Database.java:216)
	at org.h2.engine.Engine.openSession(Engine.java:59)
	at org.h2.engine.Engine.openSession(Engine.java:167)
	at org.h2.engine.Engine.createSessionAndValidate(Engine.java:145)
	at org.h2.engine.Engine.createSession(Engine.java:127)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:136)
	at java.lang.Thread.run(Thread.java:662)


最早调用org.h2.store.PageStore.getFreeList(int)是在这:
-------------------------------------------------------------
java.lang.Error
	at org.h2.store.PageStore.getFreeList(PageStore.java:1084)
	at org.h2.store.PageStore.getFreeListForPage(PageStore.java:1079)
	at org.h2.store.PageStore.allocatePage(PageStore.java:1124)
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



org.h2.store.PageStore.addMeta(PageIndex, Session)
( /* key:17 */ 16, 1, 15, 70, 'OFF,0,,', '1')

从org.h2.engine.Database.getPageStore()开始触发PageStore的初始化

	//fileName = E:/H2/test.h2.db
	//accessMode = rw
	//cacheSizeDefault = 16384 (默认16K)，可通过CACHE_SIZE参数设置
    public PageStore(Database database, String fileName, String accessMode, int cacheSizeDefault) {
        this.fileName = fileName;
        this.accessMode = accessMode;
        this.database = database;
        trace = database.getTrace(Trace.PAGE_STORE);
        // if (fileName.endsWith("X.h2.db"))
        // trace.setLevel(TraceSystem.DEBUG);
        String cacheType = database.getCacheType(); //默认LRU
        this.cache = CacheLRU.getCache(this, cacheType, cacheSizeDefault);
        systemSession = new Session(database, null, 0);
    }

	//org.h2.store.PageFreeList.getPagesAddressed(int)
    public static int getPagesAddressed(int pageSize) { //2048 (2K)
        return (pageSize - DATA_START) * 8; //cacheSizeDefault是16K，所以大概能存8个page
    }

org.h2.store.PageStore.open()
	=> org.h2.store.PageStore.openExisting()
		=> org.h2.engine.Database.openFile(String, String, boolean)
			=> org.h2.store.FileStore.open(DataHandler, String, String, String, byte[])
				=> org.h2.store.FileStore.FileStore(DataHandler, String, String)

			=> org.h2.store.FileStore.init()


    public void init() {
        int len = Constants.FILE_BLOCK_SIZE;
        byte[] salt;
        byte[] magic = HEADER.getBytes(); //HEADER_LENGTH = 48
        if (length() < HEADER_LENGTH) { //第一次建立*.h2.db文件时length为0
            // write unencrypted
            checkedWriting = false;
			//写入三个"-- H2 0.5/B -- \n"，每个16字节
            writeDirect(magic, 0, len);
            salt = generateSalt();
            writeDirect(salt, 0, len);
            initKey(salt);
            // write (maybe) encrypted
            write(magic, 0, len);
            checkedWriting = true;
        } else {
            // read unencrypted
            seek(0);
            byte[] buff = new byte[len];
            readFullyDirect(buff, 0, len);
            if (Utils.compareNotNull(buff, magic) != 0) {
                throw DbException.get(ErrorCode.FILE_VERSION_ERROR_1, name);
            }
            salt = new byte[len];
            readFullyDirect(salt, 0, len);
            initKey(salt);
            // read (maybe) encrypted
            readFully(buff, 0, Constants.FILE_BLOCK_SIZE);
            if (textMode) {
                buff[10] = 'B';
            }
            if (Utils.compareNotNull(buff, magic) != 0) {
                throw DbException.get(ErrorCode.FILE_ENCRYPTION_ERROR_1, name);
            }
        }
    }

	//increment 6
	private void increaseFileSize(int increment) {
		//pageCount 0
        for (int i = pageCount; i < pageCount + increment; i++) {
            freed.set(i);
			//freed(0,..., 5)
        }
        pageCount += increment;
        long newLength = (long) pageCount << pageSizeShift;
        file.setLength(newLength);
        writeCount++;
        fileLength = newLength;
    }

	public void setLength(long newLength) {
        if (SysProperties.CHECK && newLength % Constants.FILE_BLOCK_SIZE != 0) {
            DbException.throwInternalError("unaligned setLength " + name + " pos " + newLength);
        }
        checkPowerOff();
        checkWritingAllowed();
        try {
            if (newLength > fileLength) {
                long pos = filePos;
                file.position(newLength - 1);
                FileUtils.writeFully(file, ByteBuffer.wrap(new byte[1])); //在最后那个位置写0
                file.position(pos);
            } else {
                file.truncate(newLength);
            }
            fileLength = newLength;
        } catch (IOException e) {
            closeFileSilently();
            throw DbException.convertIOException(e, name);
        }
    }


org.h2.store.PageStore.openMetaIndex()

TODO 还有问题
**************非常重要****************************
        数据什么时候同步到硬盘
**************************************************
org.h2.engine.Session.commit(boolean)或org.h2.engine.Session.rollback()
	=>org.h2.engine.Database.commit(Session)
		=>org.h2.store.PageStore.commit(Session)
			=>org.h2.store.PageStore.checkpoint()
				=>org.h2.store.PageLog.removeUntil(int)
					=>org.h2.store.PageStore.setLogFirstPage(int, int, int)
						=>org.h2.store.PageStore.writeVariableHeader()
							=>org.h2.store.FileStore.sync()
								=>java.nio.channels.FileChannel.force(true)

**************************************************

    private void openForWriting() {
    	//readMode只有org.h2.store.PageStore.openExisting()调用了一次
    	//所以通过openForWriting()这里触发log.openForWriting，再触发setLogFirstPage
    	//进而触发writeVariableHeader同步数据到硬盘中会发生一次，
    	//writeVariableHeader的触发更多的是通过commit完成
        if (!readMode || database.isReadOnly()) {
            return;
        }
        readMode = false;
        recoveryRunning = true;
        log.free();
        logFirstTrunkPage = allocatePage();
        log.openForWriting(logFirstTrunkPage, false);
        recoveryRunning = false;
        freed.set(0, pageCount, true);
        checkpoint();
    }

或者还有这个:
java.lang.Error
	at org.h2.store.PageStore.writeVariableHeader(PageStore.java:995)
	at org.h2.store.PageStore.setLogFirstPage(PageStore.java:975)
	at org.h2.store.PageLog.removeUntil(PageLog.java:708)
	at org.h2.store.PageStore.checkpoint(PageStore.java:447)
	at org.h2.engine.Database.closeOpenFilesAndUnlock(Database.java:1207)
	at org.h2.engine.Database.close(Database.java:1160)
	at org.h2.engine.Database.removeSession(Database.java:1039)
	at org.h2.engine.Session.close(Session.java:562)
	at org.h2.server.TcpServerThread.closeSession(TcpServerThread.java:175)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:270)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Thread.java:662)
和这个:
java.lang.Error
	at org.h2.store.PageStore.writeVariableHeader(PageStore.java:995)
	at org.h2.store.PageStore.setLogFirstPage(PageStore.java:975)
	at org.h2.store.PageLog.openForWriting(PageLog.java:190)
	at org.h2.store.PageStore.compact(PageStore.java:509)
	at org.h2.engine.Database.closeOpenFilesAndUnlock(Database.java:1210)
	at org.h2.engine.Database.close(Database.java:1160)
	at org.h2.engine.Database.removeSession(Database.java:1039)
	at org.h2.engine.Session.close(Session.java:562)
	at org.h2.server.TcpServerThread.closeSession(TcpServerThread.java:175)
	at org.h2.server.TcpServerThread.process(TcpServerThread.java:270)
	at org.h2.server.TcpServerThread.run(TcpServerThread.java:149)
	at java.lang.Thread.run(Thread.java:662)
