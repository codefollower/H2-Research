类图:

org.h2.util.CacheObject (此类中的pos字段就是PageBtreeLeaf的pageId)
	org.h2.store.Page
		org.h2.index.PageBtree
			org.h2.index.PageBtreeLeaf


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

PageDataLeaf、PageBtreeLeaf、PageBtreeNode这三者可以在页面类型字段中加上FLAG_LAST
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


org.h2.index.PageBtreeLeaf的字段有:
	private final boolean optimizeUpdate;
    private boolean writtenData;




以下是格式分析
------------------------------------------------------------------

字节数   代表什么

头
=======================
1        type 有两值(Page.TYPE_BTREE_LEAF | Page.FLAG_LAST(最后一页)) 或 Page.TYPE_BTREE_LEAF
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))
4        parentPageId
VarInt   index对象id(注意，不是表对象id，PageDataLeaf才是表对象id)
2        entryCount (行数)

体:
=======================
entryCount个
{
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








[Console output redirected to file:E:\H2\tmp\myout.java]
TCP server running at tcp://169.254.252.117:9092 (only local connections)
PageBtreeNode {
	pageId = 8
	parentPageId = 0
	childPageIds = 81, 80, 92, 105
	childPageIds.length = 4
	entryCount = 3
	rows = {
		( /* key:16 */ 16, '1000000016', null)
		( /* key:32 */ 32, '1000000032', null)
		( /* key:48 */ 48, '1000000048', null)
	}

	PageBtreeNode {
		pageId = 81
		parentPageId = 8
		childPageIds = 55, 54
		childPageIds.length = 2
		entryCount = 1
		rows = {
			( /* key:8 */ 8, '1000000008', null)
		}

		PageBtreeNode {
			pageId = 55
			parentPageId = 81
			childPageIds = 45, 44
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:4 */ 4, '1000000004', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 55
				pageId = 45
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
				parentPageId = 55
				pageId = 44
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
			pageId = 54
			parentPageId = 81
			childPageIds = 47, 48
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:12 */ 12, '1000000012', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 54
				pageId = 47
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
				parentPageId = 54
				pageId = 48
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
	}
	PageBtreeNode {
		pageId = 80
		parentPageId = 8
		childPageIds = 60, 65
		childPageIds.length = 2
		entryCount = 1
		rows = {
			( /* key:24 */ 24, '1000000024', null)
		}

		PageBtreeNode {
			pageId = 60
			parentPageId = 80
			childPageIds = 50, 52
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:20 */ 20, '1000000020', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 60
				pageId = 50
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
				parentPageId = 60
				pageId = 52
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
		}
		PageBtreeNode {
			pageId = 65
			parentPageId = 80
			childPageIds = 56, 58
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:28 */ 28, '1000000028', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 65
				pageId = 56
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
				parentPageId = 65
				pageId = 58
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
		}
	}
	PageBtreeNode {
		pageId = 92
		parentPageId = 8
		childPageIds = 70, 75
		childPageIds.length = 2
		entryCount = 1
		rows = {
			( /* key:40 */ 40, '1000000040', null)
		}

		PageBtreeNode {
			pageId = 70
			parentPageId = 92
			childPageIds = 61, 63
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:36 */ 36, '1000000036', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 70
				pageId = 61
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
				parentPageId = 70
				pageId = 63
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
		PageBtreeNode {
			pageId = 75
			parentPageId = 92
			childPageIds = 66, 68
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:44 */ 44, '1000000044', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 75
				pageId = 66
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:41 */ 41, '1000000041', null)
					( /* key:42 */ 42, '1000000042', null)
					( /* key:43 */ 43, '1000000043', null)
					( /* key:44 */ 44, '1000000044', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 75
				pageId = 68
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:45 */ 45, '1000000045', null)
					( /* key:46 */ 46, '1000000046', null)
					( /* key:47 */ 47, '1000000047', null)
					( /* key:48 */ 48, '1000000048', null)
				}
			}
		}
	}
	PageBtreeNode {
		pageId = 105
		parentPageId = 8
		childPageIds = 82, 87, 93, 98, 106
		childPageIds.length = 5
		entryCount = 4
		rows = {
			( /* key:56 */ 56, '1000000056', null)
			( /* key:64 */ 64, '1000000064', null)
			( /* key:72 */ 72, '1000000072', null)
			( /* key:80 */ 80, '1000000080', null)
		}

		PageBtreeNode {
			pageId = 82
			parentPageId = 105
			childPageIds = 71, 73
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:52 */ 52, '1000000052', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 82
				pageId = 71
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:49 */ 49, '1000000049', null)
					( /* key:50 */ 50, '1000000050', null)
					( /* key:51 */ 51, '1000000051', null)
					( /* key:52 */ 52, '1000000052', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 82
				pageId = 73
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:53 */ 53, '1000000053', null)
					( /* key:54 */ 54, '1000000054', null)
					( /* key:55 */ 55, '1000000055', null)
					( /* key:56 */ 56, '1000000056', null)
				}
			}
		}
		PageBtreeNode {
			pageId = 87
			parentPageId = 105
			childPageIds = 76, 78
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:60 */ 60, '1000000060', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 87
				pageId = 76
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:57 */ 57, '1000000057', null)
					( /* key:58 */ 58, '1000000058', null)
					( /* key:59 */ 59, '1000000059', null)
					( /* key:60 */ 60, '1000000060', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 87
				pageId = 78
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:61 */ 61, '1000000061', null)
					( /* key:62 */ 62, '1000000062', null)
					( /* key:63 */ 63, '1000000063', null)
					( /* key:64 */ 64, '1000000064', null)
				}
			}
		}
		PageBtreeNode {
			pageId = 93
			parentPageId = 105
			childPageIds = 83, 85
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:68 */ 68, '1000000068', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 93
				pageId = 83
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:65 */ 65, '1000000065', null)
					( /* key:66 */ 66, '1000000066', null)
					( /* key:67 */ 67, '1000000067', null)
					( /* key:68 */ 68, '1000000068', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 93
				pageId = 85
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:69 */ 69, '1000000069', null)
					( /* key:70 */ 70, '1000000070', null)
					( /* key:71 */ 71, '1000000071', null)
					( /* key:72 */ 72, '1000000072', null)
				}
			}
		}
		PageBtreeNode {
			pageId = 98
			parentPageId = 105
			childPageIds = 88, 90
			childPageIds.length = 2
			entryCount = 1
			rows = {
				( /* key:76 */ 76, '1000000076', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 98
				pageId = 88
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:73 */ 73, '1000000073', null)
					( /* key:74 */ 74, '1000000074', null)
					( /* key:75 */ 75, '1000000075', null)
					( /* key:76 */ 76, '1000000076', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 98
				pageId = 90
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:77 */ 77, '1000000077', null)
					( /* key:78 */ 78, '1000000078', null)
					( /* key:79 */ 79, '1000000079', null)
					( /* key:80 */ 80, '1000000080', null)
				}
			}
		}
		PageBtreeNode {
			pageId = 106
			parentPageId = 105
			childPageIds = 94, 96, 99, 101, 107
			childPageIds.length = 5
			entryCount = 4
			rows = {
				( /* key:84 */ 84, '1000000084', null)
				( /* key:88 */ 88, '1000000088', null)
				( /* key:92 */ 92, '1000000092', null)
				( /* key:96 */ 96, '1000000096', null)
			}

			PageBtreeLeaf {
				indexId = 15
				parentPageId = 106
				pageId = 94
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:81 */ 81, '1000000081', null)
					( /* key:82 */ 82, '1000000082', null)
					( /* key:83 */ 83, '1000000083', null)
					( /* key:84 */ 84, '1000000084', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 106
				pageId = 96
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:85 */ 85, '1000000085', null)
					( /* key:86 */ 86, '1000000086', null)
					( /* key:87 */ 87, '1000000087', null)
					( /* key:88 */ 88, '1000000088', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 106
				pageId = 99
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:89 */ 89, '1000000089', null)
					( /* key:90 */ 90, '1000000090', null)
					( /* key:91 */ 91, '1000000091', null)
					( /* key:92 */ 92, '1000000092', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 106
				pageId = 101
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:93 */ 93, '1000000093', null)
					( /* key:94 */ 94, '1000000094', null)
					( /* key:95 */ 95, '1000000095', null)
					( /* key:96 */ 96, '1000000096', null)
				}
			}
			PageBtreeLeaf {
				indexId = 15
				parentPageId = 106
				pageId = 107
				start = 18
				offsets = 114, 100, 86, 72
				entryCount = 4
				rows = {
					( /* key:97 */ 97, '1000000097', null)
					( /* key:98 */ 98, '1000000098', null)
					( /* key:99 */ 99, '1000000099', null)
					( /* key:100 */ 100, '1000000100', null)
				}
			}
		}
	}
}

---------------------
