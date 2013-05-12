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
fileName
encrypt
readOnly
cacheSize
compress
writeBufferSize
writeDelay


Page格式

int     page length(包含这4字节)
short   check value(并不是校验和，不用计算所有字节)，check value的值取决于当前page所在chunk的chunkId、start位置(或称offset)、page length
varInt  map id
varInt  key count
byte    page type(0: leaf, 1: node)

接下来分两种情况:
===========================
1. 如果不需压缩: {
	key count {
		Object  key
	}
	if (type == leaf) {
		key count {
			Object  value
		}
	}
	if (type == node) {
		key count + 1 {
			long     child page pos
		}

		key count + 1 {
			varLong  child key count
		}
	}
}

2. 如果需要压缩，先按1的方式写入buffer，计算1的字节长度，然后尝试压缩buffer中的字节，
如果"压缩后的长度+(压缩后的长度-buffer长度)的差的VarIntLen < buffer长度"那么就进行压缩:
重写page type
byte         page type(2: leaf+compressed, 3: node+compressed)
varInt       buffer长度-压缩后的长度
压缩后的长度 压缩后的字节


