PageLog是用来记事务日志的:

有9种日志类型
1. UNDO
2. COMMIT
3. PREPARE_COMMIT (用于XA，两阶段提交)
4. ROLLBACK
5. ADD
6. REMOVE
7. TRUNCATE
8. CHECKPOINT
9. FREE_LOG

还有一个NOOP(值为0)表示什么都不做

以下是每种日志类型的具体格式:

1. UNDO

字节数   代表什么
=======================
1        type 固定是UNDO (值是1)
VarInt   pageId

接下来分4种情况
如果page类型是org.h2.store.Page.TYPE_EMPTY
---------------------
VarInt   固定是1

如果page类型是除Page.TYPE_EMPTY以外的类型
如果使用压缩:
先对page进行压缩，如果压缩后的大小<pageSize
---------------------
VarInt   size 压缩后的大小
size     压缩后的字节

如果压缩后的大小>pageSize
---------------------
VarInt   固定是0
pageSize page未压缩的字节


未使用压缩的情形
---------------------
VarInt   固定是0
pageSize page未压缩的字节





2. COMMIT

字节数   代表什么
=======================
1        type 固定是COMMIT (值是2)
VarInt   sessionId




3. PREPARE_COMMIT

字节数   代表什么
=======================
1        type 固定是PREPARE_COMMIT (值是3)
VarInt   sessionId
String   transaction name

PREPARE_COMMIT要独占一个PageStreamData




4. ROLLBACK

字节数   代表什么
=======================
1        type 固定是ROLLBACK (值是4)
VarInt   sessionId




5. ADD

字节数   代表什么
=======================
1        type 固定是ADD (值是5)
VarInt   sessionId
VarInt   tableId
VarLong  row key
VarInt   row length
length   row data




6. REMOVE

字节数   代表什么
=======================
1        type 固定是REMOVE (值是6)
VarInt   sessionId
VarInt   tableId
VarLong  row key

比ADD少了后面两项






7. TRUNCATE

字节数   代表什么
=======================
1        type 固定是TRUNCATE (值是7)
VarInt   sessionId
VarInt   tableId








8. CHECKPOINT

字节数   代表什么
=======================
1        type 固定是CHECKPOINT (值是8)

会切换新的PageStreamData





9. FREE_LOG

字节数   代表什么
=======================
1        type 固定是FREE_LOG (值是9)
VarInt   page个数

page个{
    VarInt pageId
}








