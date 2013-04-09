最基本的注意点: PageBtreeNode的子结点要么全是PageBtreeNode，要是全是PageBtreeLeaf，不会混合两者。

类图:
与org.h2.index.PageBtreeLeaf类似

类图:

org.h2.util.CacheObject (此类中的pos字段就是PageBtreeLeaf的pageId)
	org.h2.store.Page
		org.h2.index.PageBtree
			org.h2.index.PageBtreeNode


CacheObject的直接子类只有org.h2.util.CacheHead和org.h2.store.Page



org.h2.store.Page类定义了8种页面类型，分别对应8个子类:

页面类型id　页面名称            对应的类
------------------------------------------------------------------
0           TYPE_EMPTY          有没类，主要用于异常或其他情况

1           TYPE_DATA_LEAF      org.h2.index.PageDataLeaf
2           TYPE_DATA_NODE      org.h2.index.PageDataNode
3           TYPE_DATA_OVERFLOW  org.h2.index.PageDataOverflow

4           TYPE_BTREE_LEAF     org.h2.index.PageBtreeLeaf
5           TYPE_BTREE_NODE     org.h2.index.PageBtreeNode

6           TYPE_FREE_LIST      org.h2.store.PageFreeList
7           TYPE_STREAM_TRUNK   org.h2.store.PageStreamTrunk
8           TYPE_STREAM_DATA    org.h2.store.PageStreamData
------------------------------------------------------------------

PageDataLeaf、PageBtreeLeaf、PageBtreeNode这三者的可以在业面类型字段中加上FLAG_LAST
FLAG_LAST表示此页是最后一页。



org.h2.util.CacheObject的字段有:
	public CacheObject cachePrevious
	public CacheObject cacheNext
	public CacheObject cacheChained
	private int pos
	private boolean changed

org.h2.store.Page的字段有:
	protected int changeCount;
	
	两个抽象方法:

	moveTo(Session, int) //移动当前page到新的位置
	write() //写当前页面的数据到硬盘

	提供了三类static方法，add、insert、remove，用于从数据中给原有元素加上一个(可能是负值)、增加一个新元素、删除一个元素
	这类方法多用于在rows数组和offsets、keys数组中，rows用于放记录，offsets用于放记录在page中的相对位置，keys用于放记录的key。


org.h2.index.PageBtree的字段有:
	 protected final PageBtreeIndex index
	 protected int parentPageId
	 protected final Data data
	 protected int[] offsets
	 protected int entryCount
	 protected SearchRow[] rows
	 protected int start
	 protected boolean onlyPosition
	 protected boolean written
	 protected int memoryEstimated


org.h2.index.PageBtreeNode的字段有:
    private final boolean pageStoreInternalCount; //默认是false，通过参数PAGE_STORE_INTERNAL_COUNT设置
    /**
     * The page ids of the children.
     */
    private int[] childPageIds;

    private int rowCountStored = UNKNOWN_ROWCOUNT; // -1;

    private int rowCount = UNKNOWN_ROWCOUNT; // -1;


以下是格式分析
------------------------------------------------------------------

字节数   代表什么

头
=======================
1        type 有两值(Page.TYPE_BTREE_NODE | Page.FLAG_LAST(最后一页)) 或 Page.TYPE_BTREE_NODE
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))
4        parentPageId
VarInt   index对象id(注意，不是表对象id，PageDataNode才是表对象id)
4        rowCountStored 已存储的行数
2        entryCount 分隔点的个数(rows数组的有效个数)，也等于子节点个数-1

体:
=======================
4        rightmost child page id (最右边的子页id)

entryCount个
{
	4         child page id
	2         offset
}

entryCount个
{	(从offset开始写)

	columnCount个
	{
		VarLong key
		Value   索引列值 (如果onlyPosition为true，那么不包含这一部分，只有前面的VarLong key)
	}
	
}

(****一个PageBtreeNode下面挂4个PageBtreeLeaf， 超过4个时切分****)
上面这句话理解错了，如果PageBtreeLeaf个数小于4时快速返回-1表示不需要对PageBtreeNode切分，
PageBtreeNode什么时候切分需要看pageSize和索引字段的长度，
不过PageBtreeNode最少有4个PageBtreeLeaf



page size = 128

rowLength = 12

[( /* 8 */ '1000000008' ), ( /* 7 */ '1000000007' ), ( /* 6 */ '1000000006' ), ( /* 5 */ '1000000005' ), ( /* 4 */ '1000000004' ), ( /* 3 */ '1000000003' ), ( /* 2 */ '1000000002' ), ( /* 1 */ '1000000001' ), null, null]
[116, 104, 92, 80, 68, 56, 44, 32, 0, 0]

splitPoint= 2
从下标为2的位置开始split(切分)，从splitPoint(包含)到结束的元素放在page2
从开始到splitPoint的元素在page1，
splitPoint前面的元素单独抽出来做为父结点的分隔key，
也就是说中间层的结点中的分隔key，>=分隔key的在左边的子结点，<分隔key的在右边的子结点
pivot = ( /* 7 */ '1000000007' )


page1 = 2
[( /* 8 */ '1000000008' ), ( /* 7 */ '1000000007' ), ( /* 1 */ '1000000001' ), ( /* 1 */ '1000000001' ), ( /* 1 */ '1000000001' )]

page2 = 6
[( /* 6 */ '1000000006' ), ( /* 5 */ '1000000005' ), ( /* 4 */ '1000000004' ), ( /* 3 */ '1000000003' ), ( /* 2 */ '1000000002' ), ( /* 1 */ '1000000001' ), null, null, null, null]


[116, 104, 92, 80, 68]
[116, 104, 92, 80, 0]


PageBtreeNode分隔前
---------------------------
childPageIds = [74, 86, 84, 81, 79, 76, 73, 0, 0, 0, 0]
rows(分隔key) = [( /* 37 */ '1000000037' ), ( /* 31 */ '1000000031' ), ( /* 25 */ '1000000025' ), ( /* 19 */ '1000000019' ), ( /* 13 */ '1000000013' ), ( /* 7 */ '1000000007' ), null, null, null, null]
entryCount = 6
splitPoint =3
pivot = ( /* 25 */ '1000000025' )
---------------------------


PageBtreeNode p2
---------------------------
childPageIds = [81, 79, 76, 73, 0, 0]
rows(分隔key) = [( /* 19 */ '1000000019' ), ( /* 13 */ '1000000013' ), ( /* 7 */ '1000000007' ), null, null]
entryCount = 3
---------------------------
pageId=89

PageBtreeNode p1
---------------------------
childPageIds = [74, 86, 84, 73, 73, 73]
rows(分隔key) = [( /* 37 */ '1000000037' ), ( /* 31 */ '1000000031' ), ( /* 25 */ '1000000025' ), ( /* 7 */ '1000000007' ), ( /* 7 */ '1000000007' )]
entryCount = 2
---------------------------
pageId=90 (新分配一个pageId)

得到一个新的PageBtreeNode后，
此PageBtreeNode的childPageIds是[90, 89] rows(分隔key) = [( /* 25 */ '1000000025' )]


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



splitPoint = 3
childPageIds = [46, 45, 48, 50, 52, 54, 56, 0, 0, 0, 0]
firstChild = 50

entryCount = 2
[46, 45, 48, 56, 56, 56]

[50, 52, 54, 56, 0, 0]




PageBtreeNode {
	pageId = 8
	parentPageId = 0
	childPageIds = 84, 83, 89
	childPageIds.length = 3
	entryCount = 2
	rows = {
		( /* key:8 */ 8, '1000000008', null)
		( /* key:16 */ 16, '1000000016', null)
	}

	PageBtreeNode {
		pageId = 84
		parentPageId = 8
		childPageIds = 13, 12
		childPageIds.length = 2
		entryCount = 1
		rows = {
			( /* key:4 */ 4, '1000000004', null)
		}

		PageBtreeLeaf {
			indexId = 15
			parentPageId = 84
			pageId = 13
			start = 18
			offsets = 115, 102, 89, 76
			entryCount = 4
			rows = {
				( /* key:1 */ 1, '1000000001', null)
				( /* key:2 */ 2, '1000000002', null)
				( /* key:3 */ 3, '1000000003', null)
				( /* key:4 */ 4, '1000000004', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 84
			pageId = 12
			start = 18
			offsets = 115, 102, 89, 76
			entryCount = 4
			rows = {
				( /* key:5 */ 5, '1000000005', null)
				( /* key:6 */ 6, '1000000006', null)
				( /* key:7 */ 7, '1000000007', null)
				( /* key:8 */ 8, '1000000008', null)
			}
		}
	}
	PageBtreeNode {
		pageId = 83
		parentPageId = 8
		childPageIds = 15, 16
		childPageIds.length = 2
		entryCount = 1
		rows = {
			( /* key:12 */ 12, '1000000012', null)
		}

		PageBtreeLeaf {
			indexId = 15
			parentPageId = 83
			pageId = 15
			start = 18
			offsets = 115, 102, 89, 76
			entryCount = 4
			rows = {
				( /* key:9 */ 9, '1000000009', null)
				( /* key:10 */ 10, '1000000010', null)
				( /* key:11 */ 11, '1000000011', null)
				( /* key:12 */ 12, '1000000012', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 83
			pageId = 16
			start = 18
			offsets = 115, 102, 89, 75
			entryCount = 4
			rows = {
				( /* key:13 */ 13, '1000000013', null)
				( /* key:14 */ 14, '1000000014', null)
				( /* key:15 */ 15, '1000000015', null)
				( /* key:16 */ 16, '1000000016', null)
			}
		}
	}
	PageBtreeNode {
		pageId = 89
		parentPageId = 8
		childPageIds = 18, 81, 85, 87, 90, 92
		childPageIds.length = 6
		entryCount = 5
		rows = {
			( /* key:20 */ 20, '1000000020', null)
			( /* key:24 */ 24, '1000000024', null)
			( /* key:28 */ 28, '1000000028', null)
			( /* key:32 */ 32, '1000000032', null)
			( /* key:36 */ 36, '1000000036', null)
		}

		PageBtreeLeaf {
			indexId = 15
			parentPageId = 89
			pageId = 18
			start = 18
			offsets = 114, 100, 86, 72
			entryCount = 4
			rows = {
				( /* key:17 */ 17, '1000000017', null)
				( /* key:18 */ 18, '1000000018', null)
				( /* key:19 */ 19, '1000000019', null)
				( /* key:20 */ 20, '1000000020', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 89
			pageId = 81
			start = 18
			offsets = 114, 100, 86, 72
			entryCount = 4
			rows = {
				( /* key:21 */ 21, '1000000021', null)
				( /* key:22 */ 22, '1000000022', null)
				( /* key:23 */ 23, '1000000023', null)
				( /* key:24 */ 24, '1000000024', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 89
			pageId = 85
			start = 18
			offsets = 114, 100, 86, 72
			entryCount = 4
			rows = {
				( /* key:25 */ 25, '1000000025', null)
				( /* key:26 */ 26, '1000000026', null)
				( /* key:27 */ 27, '1000000027', null)
				( /* key:28 */ 28, '1000000028', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 89
			pageId = 87
			start = 18
			offsets = 114, 100, 86, 72
			entryCount = 4
			rows = {
				( /* key:29 */ 29, '1000000029', null)
				( /* key:30 */ 30, '1000000030', null)
				( /* key:31 */ 31, '1000000031', null)
				( /* key:32 */ 32, '1000000032', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 89
			pageId = 90
			start = 18
			offsets = 114, 100, 86, 72
			entryCount = 4
			rows = {
				( /* key:33 */ 33, '1000000033', null)
				( /* key:34 */ 34, '1000000034', null)
				( /* key:35 */ 35, '1000000035', null)
				( /* key:36 */ 36, '1000000036', null)
			}
		}
		PageBtreeLeaf {
			indexId = 15
			parentPageId = 89
			pageId = 92
			start = 18
			offsets = 114, 100, 86, 72
			entryCount = 4
			rows = {
				( /* key:37 */ 37, '1000000037', null)
				( /* key:38 */ 38, '1000000038', null)
				( /* key:39 */ 39, '1000000039', null)
				( /* key:40 */ 40, '1000000040', null)
			}
		}
	}
}
