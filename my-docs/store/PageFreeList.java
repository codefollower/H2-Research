以下是格式分析
------------------------------------------------------------------

字节数   代表什么

头
=======================
1        type 固定是Page.TYPE_FREE_LIST (值是6)
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))


(头部占3字节)

体:
=======================
pageCount个
{
	1         pageId
}
