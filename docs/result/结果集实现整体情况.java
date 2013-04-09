主要接口:
org.h2.result.ResultInterface

它的两个实现类是:
org.h2.result.ResultRemote (在JDBC端使用，也就是Client端)
org.h2.result.LocalResult  (在Server端使用，Local这个词是相对于Server自身来讲的，因为结果集就在Server本地，所以用Local)

