org.h2.mvstore.MVStore的MVMapConcurrent<String, String> meta存放哪些key/value：

key                            value
=============================================
"map."+ mapId                  mapName
"name." + mapName              mapId

"chunk." + chunkId             Chunk.asString()
"root." + mapId                pagePos
"setting.storeVersion"         storeVersion
"rollbackOnOpen"               lastCommittedVersion



运行时，一个数据库对应一个MVStore实例、对应一个TransactionStore实例
并且对应一个openTransactions MVMap和一个undoLog MVMap，以及一个openTransactionMap HashMap

	MVStore
	TransactionStore

     * The persisted map of open transaction.
     * Key: transactionId, value: [ status, name ].
     */
    final MVMap<Long, Object[]> openTransactions;

    /**
     * The map of open transaction objects.
     * Key: transactionId, value: transaction object.
     */
    final HashMap<Long, Transaction> openTransactionMap = New.hashMap();

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

