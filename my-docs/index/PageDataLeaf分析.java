字节数   代表什么

头
=======================
1        type 有两值(Page.TYPE_DATA_LEAF | Page.FLAG_LAST(最后一页)) 或 Page.TYPE_DATA_LEAF
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))
4        parentPageId
VarInt   index对象id(实际是表对象id)
VarInt   columnCount
2        entryCount (行数)

体:
=======================
4        with overflow: the first overflow page id(如果firstOverflowPageId !=0)

entryCount个
{
	VarLong   key
	2         offset
}

entryCount个
{	(从offset开始写)

	columnCount个
	{
		column value
	}
	
}



page size = 128

rowLength = 12

0 1 2 3 4 5 6 7 ... 116 117 118 119 120 121 122 123 124 125 126 127

( /* key:1 */ 1, 'abcdef1234')

116 117 118 119 120 121 122 123 124 125 126 127
1   10  a   b   c   d   e   f   1   2   3   4


104 ... 115
( /* key:2 */ 2, 'abcdef1234')

92 ... 103
( /* key:3 */ 3, 'abcdef1234')

80 ... 91
( /* key:4 */ 4, 'abcdef1234')

68 ... 79
( /* key:5 */ 5, 'abcdef1234')

56 ... 67
( /* key:6 */ 6, 'abcdef1234')

44 ... 55
( /* key:7 */ 7, 'abcdef1234')


keys = [1, 2, 3, 4, 5, 6, 7, 0, 0, 0]
rows = [( /* key:1 */ 1, 'abcdef1234'), ( /* key:2 */ 2, 'abcdef1234'), ( /* key:3 */ 3, 'abcdef1234'), ( /* key:4 */ 4, 'abcdef1234'), ( /* key:5 */ 5, 'abcdef1234'), ( /* key:6 */ 6, 'abcdef1234'), ( /* key:7 */ 7, 'abcdef1234'), null, null, null]
offsets = [116, 104, 92, 80, 68, 56, 44, 0, 0, 0]


entryCount = 7
last = 44
rowLength = 12
start = 32
keyOffsetPairLen = 3

last - rowLength = 44 - 12 = 32
start + keyOffsetPairLen = 32 + 3 = 35

当前行: ( /* key:8 */ 8, 'abcdef1234')

也就是说先看看当前page(128字节)如果要存当前行的话是否有足够空间存keyOffsetPair，
因为当前行的长度是12，page中只剩下44个字节了(0到43)，再存12个就只剩32个可用，
而start当前已经到32了，再存当前行的keyOffsetPair的话要多加3，则start位置移到35，超过剩余可用的32个字节了。
所以此时要对当前的PageDataLeaf进行切割。

		if (entryCount > 0 && last - rowLength < start + keyOffsetPairLen) {
            int x = findInsertionPoint(row.getKey()); //x = 7
            if (entryCount > 1) {
                if (entryCount < 5) {
                    // required, otherwise the index doesn't work correctly
                    return entryCount / 2;
                }
                if (index.isSortedInsertMode()) {
                    return x < 2 ? 1 : x > entryCount - 1 ? entryCount - 1 : x;
                }
                // split near the insertion point to better fill pages
                // split in half would be:
                // return entryCount / 2;
                int third = entryCount / 3;
                return x < third ? third : x >= 2 * third ? 2 * third : x; //返回4 (从rows[4]=( /* key:5 */ 5, 'abcdef1234')开始切割)
            }
            return x;
        }

新的PageDataLeaf的
rows = [( /* key:5 */ 5, 'abcdef1234'), ( /* key:6 */ 6, 'abcdef1234'), ( /* key:7 */ 7, 'abcdef1234'), null, null, null]

