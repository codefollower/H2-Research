    private void writeHead() {
        data.reset();
        data.writeByte((byte) Page.TYPE_DATA_NODE);
        data.writeShortInt(0);
        if (SysProperties.CHECK2) {
            if (data.length() != START_PARENT) {
                DbException.throwInternalError();
            }
        }
        data.writeInt(parentPageId);
        data.writeVarInt(index.getId()); //索引对像id
        data.writeInt(rowCountStored); //行数
        data.writeShortInt(entryCount); //leaf的页数
    }

[8, 7, 9, 10, 11, 12, 13, 14, 0, 0, 0, 0]

最基本的注意点: PageDataNode的子结点要么全是PageDataNode，要是全是PageDataLeaf，不会混合两者。

类图:
与org.h2.index.PageDataLeaf类似

类图:

org.h2.util.CacheObject (此类中的pos字段就是PageDataNode的pageId)
	org.h2.store.Page
		org.h2.index.PageData
			org.h2.index.PageDataNode


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


org.h2.index.PageData的字段有:
	 protected final PageDataIndex index
	 protected int parentPageId
	 protected final Data data
	 protected int entryCount
	 protected long[] keys;
	 protected boolean written
	 protected int memoryEstimated


org.h2.index.PageDataNode的字段有:
    private int[] childPageIds;
    private int rowCountStored = UNKNOWN_ROWCOUNT; // -1;
    private int rowCount = UNKNOWN_ROWCOUNT; // -1;
	private int length;


以下是格式分析
------------------------------------------------------------------

字节数   代表什么

头
=======================
1        type Page.TYPE_DATA_NODE
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))
4        parentPageId
VarInt   index对象id(实际是表对象id)
4        rowCountStored
2        entryCount 分隔点的个数(keys数组的有效个数)，也等于子节点个数-1

体:
=======================
4        rightmost child page id (最右边的子页id)

entryCount个
{
	4         child page id
	VarLong   key
}
