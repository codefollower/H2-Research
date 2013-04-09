以下是格式分析
------------------------------------------------------------------

字节数   代表什么

头
=======================
1        type 固定是Page.TYPE_STREAM_TRUNK (值是7)
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))
4        parent 前一个PageStreamTrunk的pageId，第一个PageStreamTrunk的parent和trunkPageId一样
4        logKey
4        nextTrunk 下一个PageStreamTrunk的pageId
2        pageCount PageStreamData的个数

(头部占17字节)

体:
=======================
pageCount个
{
	4         pageId PageStreamData页的pageId
}
