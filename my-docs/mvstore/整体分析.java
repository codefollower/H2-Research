org.h2.mvstore.MVStore的MVMap<String, String> meta存放哪些key/value：

key                            value
=============================================
"name." + mapName              mapId
"map."+ mapId                  {map metadata}形如"k1:v1, k2:v2..."这样的字符串格式
"root." + mapId                map的root page pos

"chunk." + chunkId             Chunk.asString()
"setting.storeVersion"         storeVersion


运行时，一个数据库对应一个MVStore实例、对应一个TransactionStore实例
并且对应一个preparedTransactions MVMap和一个undoLog MVMap

	MVStore
	TransactionStore

     * The persisted map of open transaction.
     * Key: transactionId, value: [ status, name ].
     */
    final MVMap<Long, Object[]> preparedTransactions;

    /**
     * The undo log.
     * Key: [ transactionId, logId ], value: [ opType, mapId, key, oldValue ].
     */
    final MVMap<long[], Object[]> undoLog;


主表记录对应一个MVPrimaryIndex实例
每个索引对应一个MVSecondaryIndex实例

一个MVTable类型的表只有一个MVPrimaryIndex实例，可以有多个MVSecondaryIndex实例，
每个MVPrimaryIndex实例和MVSecondaryIndex实例都有一个MVMap，
在对MVPrimaryIndex和MVSecondaryIndex增加或删除数据时，除了为各自的MVMap进行操作外，
还往undoLog这个MVMap增加或删除log，当需要rollback时，
根据transactionId和logId取出值，值里有[ opType, mapId, key, oldValue ]，
根据mapId能从MVStore实例中取出MVPrimaryIndex实例和MVSecondaryIndex实例对应的MVMap，
然后根据oldValue是否为null，null表示进行的是增加操作，rollback时，
就按key从MVPrimaryIndex实例和MVSecondaryIndex实例对应的MVMap中删除，若不为null，就表示删除操作，
此时要重新put回去。

TODO undoLog这个MVMap中的opType似乎没用，因为根据oldValue是否为null，就能判断操作类型了(增加或删除)



MVStore.Builder可配置的参数:
autoCommitBufferSize
autoCompactFillRate
backgroundExceptionHandler
cacheSize
compress (LZF和Deflate)
encryptionKey
fileName
fileStore
pageSplitSize
readOnly


=================================
MVStore格式

	MVStore {
		4096字节  StoreHeader
		4096字节  StoreHeader
		[ Chunk ] *

		128字节   ChunkFooter
	}

	StoreHeader {
		4096字节是一个块的大小，上面两个是一样的，防止块损坏，
		一个StoreHeader的大小不固定，取决于具体的值，但是不会超过4096字节，
		StoreHeader有8个属性:
		-----------------------
		H:3,
		blockSize:4096,
		created:1368493299954,
		format:1,
		block:0,   //lastChunk.block
		chunk:0,   //lastChunk.id
		version:0, //lastChunk.version
		fletcher:3bde3c9a (使用Fletcher32算法得到的checksum，是对前面7个属性求checksum)
		-----------------------
	}

	ChunkFooter {
		chunk:1,block:2,version:1,fletcher:84d0d5f6
	}

	Chunk {
		[ ChunkHeader MVMapPage MetaMapPage FileHeader] *
	}

    //每个ChunkHeader最多占1024字节
	ChunkHeader {
		byte    固定是'c'
		int     length
		int     id
		int     pageCount
		long    metaRootPos
		long    maxLength
		long    maxLengthLive
	}

	MVMapPage = Page
	MetaMapPage = Page

	Page {
		int     page length(包含这4字节)
		short   check value(并不是校验和，不用计算所有字节)，check value的值取决于当前page所在chunk的chunkId、start位置(或称offset)、page length
		varInt  map id
		varInt  key count
		byte    page type(0: leaf, 1: node)

		接下来分两种情况:
		===========================
		1. 如果不需压缩: {
			if (type == leaf) {
				key count {
					Object  key
				}
				key count {
					Object  value
				}
			}
			else if (type == node) {
				key count + 1 {
					long     child page pos
				}

				key count + 1 {
					varLong  child key count
				}
				
				key count {
					Object  key
				}
			}
		}

		2. 如果需要压缩，先按1的方式写入buffer，计算1的字节长度，然后尝试压缩buffer中的字节，
		如果"压缩后的长度+(压缩后的长度-buffer长度)的差的VarIntLen < buffer长度"那么就进行压缩:
		重写page type
		byte         page type(2: leaf+compressed, 3: node+compressed)
		varInt       buffer长度-压缩后的长度
		压缩后的长度 压缩后的字节
	}

	meta map样例:

	PageLeaf{
		id = 23191477
		pos = 1099511630162
		version = -1
		keyCount = 10

		chunk.1:id:1,length:2352,maxLength:3008,maxLengthLive:1024,metaRoot:274878040014,pageCount:7,pageCountLive:2,start:8192,time:16,version:1
		chunk.2:id:2,length:426,maxLength:512,maxLengthLive:512,metaRoot:549755816272,pageCount:1,pageCountLive:1,start:16384,time:16,version:2
		chunk.3:id:3,length:2865,maxLength:3424,maxLengthLive:2656,metaRoot:824633869138,pageCount:7,pageCountLive:6,start:24576,time:16,version:3
		chunk.4:id:4,length:2147483647,maxLength:0,maxLengthLive:0,metaRoot:0,pageCount:0,pageCountLive:0,start:9223372036854775807,time:16,version:4
		map.1:name:data
		map.2:name:data2
		name.data:1
		name.data2:2
		root.1:824633866563
		root.2:824633793603
	}

下面的数字是16进制

file header:
=================
H:2,block:2,blockSize:1000,chunk:129,created:149b6ac28ee,format:1,version:129,fletcher:424fcf1c
H:2,block:2,blockSize:1000,chunk:129,created:149b6ac28ee,format:1,version:129,fletcher:424fcf1c


footer
===========
chunk:128,block:4,version:128,fletcher:8097485b


readChunkHeader:
================
chunk:129,block:2,len:1,map:1f,max:900,next:3,pages:8,root:4a4000012743,time:1ad588de,version:129


readChunkFooter
======================
chunk:129,block:2,version:129,fletcher:7e97485b

readChunkHeader:
================
chunk:127,block:3,len:1,map:1f,max:900,next:4,pages:8,root:49c000012743,time:1ad42913,version:127
chunk:1,block:2,len:1,map:1,max:5a0,next:3,pages:e,root:40000119c4,time:274e1,version:1



















==========================================================================================
以下是老版本(已经过时)
==========================================================================================

org.h2.mvstore.MVStore的MVMapConcurrent<String, String> meta存放哪些key/value：

key                            value
=============================================
"map."+ mapId                  {map metadata}形如"k1:v1, k2:v2..."这样的字符串格式
"name." + mapName              mapId

"chunk." + chunkId             Chunk.asString()
"root." + mapId                pagePos
"setting.storeVersion"         storeVersion
"rollbackOnOpen"               lastCommittedVersion


运行时，一个数据库对应一个MVStore实例、对应一个TransactionStore实例
并且对应一个preparedTransactions MVMap和一个undoLog MVMap

	MVStore
	TransactionStore

     * The persisted map of open transaction.
     * Key: transactionId, value: [ status, name ].
     */
    final MVMap<Long, Object[]> preparedTransactions;

    /**
     * The undo log.
     * Key: [ transactionId, logId ], value: [ opType, mapId, key, oldValue ].
     */
    final MVMap<long[], Object[]> undoLog;


主表记录对应一个MVPrimaryIndex实例
每个索引对应一个MVSecondaryIndex实例

一个MVTable类型的表只有一个MVPrimaryIndex实例，可以有多个MVSecondaryIndex实例，
每个MVPrimaryIndex实例和MVSecondaryIndex实例都有一个MVMap，
在对MVPrimaryIndex和MVSecondaryIndex增加或删除数据时，除了为各自的MVMap进行操作外，
还往undoLog这个MVMap增加或删除log，当需要rollback时，
根据transactionId和logId取出值，值里有[ opType, mapId, key, oldValue ]，
根据mapId能从MVStore实例中取出MVPrimaryIndex实例和MVSecondaryIndex实例对应的MVMap，
然后根据oldValue是否为null，null表示进行的是增加操作，rollback时，
就按key从MVPrimaryIndex实例和MVSecondaryIndex实例对应的MVMap中删除，若不为null，就表示删除操作，
此时要重新put回去。

TODO undoLog这个MVMap中的opType似乎没用，因为根据oldValue是否为null，就能判断操作类型了(增加或删除)



MVStore.Builder可配置的参数:
autoCommitBufferSize
autoCompactFillRate
backgroundExceptionHandler
cacheSize
compress (LZF和Deflate)
encryptionKey
fileName
fileStore
pageSplitSize
readOnly


=================================
MVStore格式

	MVStore {
		4096字节  StoreHeader
		4096字节  StoreHeader
		[ Chunk ] *

		128字节   ChunkFooter
	}

	StoreHeader {
		4096字节是一个块的大小，上面两个是一样的，防止块损坏，
		一个StoreHeader的大小不固定，取决于具体的值，但是不会超过4096字节，
		StoreHeader有8个属性:
		-----------------------
		H:3,
		blockSize:4096,
		created:1368493299954,
		format:1,
		block:0,   //lastChunk.block
		chunk:0,   //lastChunk.id
		version:0, //lastChunk.version
		fletcher:3bde3c9a (使用Fletcher32算法得到的checksum，是对前面7个属性求checksum)
		-----------------------
	}

	ChunkFooter {
		chunk:1,block:2,version:1,fletcher:84d0d5f6
	}

	Chunk {
		[ ChunkHeader MVMapPage MetaMapPage FileHeader] *
	}

    //每个ChunkHeader最多占1024字节
	ChunkHeader {
		byte    固定是'c'
		int     length
		int     id
		int     pageCount
		long    metaRootPos
		long    maxLength
		long    maxLengthLive
	}

	MVMapPage = Page
	MetaMapPage = Page

	Page {
		int     page length(包含这4字节)
		short   check value(并不是校验和，不用计算所有字节)，check value的值取决于当前page所在chunk的chunkId、start位置(或称offset)、page length
		varInt  map id
		varInt  key count
		byte    page type(0: leaf, 1: node)

		接下来分两种情况:
		===========================
		1. 如果不需压缩: {
			if (type == leaf) {
				key count {
					Object  key
				}
				key count {
					Object  value
				}
			}
			else if (type == node) {
				key count + 1 {
					long     child page pos
				}

				key count + 1 {
					varLong  child key count
				}
				
				key count {
					Object  key
				}
			}
		}

		2. 如果需要压缩，先按1的方式写入buffer，计算1的字节长度，然后尝试压缩buffer中的字节，
		如果"压缩后的长度+(压缩后的长度-buffer长度)的差的VarIntLen < buffer长度"那么就进行压缩:
		重写page type
		byte         page type(2: leaf+compressed, 3: node+compressed)
		varInt       buffer长度-压缩后的长度
		压缩后的长度 压缩后的字节
	}

	meta map样例:

	PageLeaf{
		id = 23191477
		pos = 1099511630162
		version = -1
		keyCount = 10

		chunk.1:id:1,length:2352,maxLength:3008,maxLengthLive:1024,metaRoot:274878040014,pageCount:7,pageCountLive:2,start:8192,time:16,version:1
		chunk.2:id:2,length:426,maxLength:512,maxLengthLive:512,metaRoot:549755816272,pageCount:1,pageCountLive:1,start:16384,time:16,version:2
		chunk.3:id:3,length:2865,maxLength:3424,maxLengthLive:2656,metaRoot:824633869138,pageCount:7,pageCountLive:6,start:24576,time:16,version:3
		chunk.4:id:4,length:2147483647,maxLength:0,maxLengthLive:0,metaRoot:0,pageCount:0,pageCountLive:0,start:9223372036854775807,time:16,version:4
		map.1:name:data
		map.2:name:data2
		name.data:1
		name.data2:2
		root.1:824633866563
		root.2:824633793603
	}

下面的数字是16进制

file header:
=================
H:2,block:2,blockSize:1000,chunk:129,created:149b6ac28ee,format:1,version:129,fletcher:424fcf1c
H:2,block:2,blockSize:1000,chunk:129,created:149b6ac28ee,format:1,version:129,fletcher:424fcf1c


footer
===========
chunk:128,block:4,version:128,fletcher:8097485b


readChunkHeader:
================
chunk:129,block:2,len:1,map:1f,max:900,next:3,pages:8,root:4a4000012743,time:1ad588de,version:129


readChunkFooter
======================
chunk:129,block:2,version:129,fletcher:7e97485b

readChunkHeader:
================
chunk:127,block:3,len:1,map:1f,max:900,next:4,pages:8,root:49c000012743,time:1ad42913,version:127
chunk:1,block:2,len:1,map:1,max:5a0,next:3,pages:e,root:40000119c4,time:274e1,version:1