以下是格式分析
------------------------------------------------------------------

字节数   代表什么

头
=======================
1        type 固定是Page.TYPE_STREAM_DATA (值是8)
2        checksum 预先写0，在写完page后回填(见org.h2.store.PageStore.writePage(int, Data))
4        trunk PageStreamTrunk的pageId
4        logKey

(头部占11字节)

体:
=======================
pageSize-11  data
