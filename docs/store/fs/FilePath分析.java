org.h2.store.fs中的类都没提供读写方法，读写是在org.h2.store.FileStore提供

FilePath有9个子类:
org.h2.store.fs.FilePathDisk        模式前缀(scheme prefix)是"file"，如"file:E:\\H2\\tmp\\my.txt"，没有比缀时默认也是它
org.h2.store.fs.FilePathMem         模式前缀是"memFS"
org.h2.store.fs.FilePathMemLZF      模式前缀是"memLZF"
org.h2.mvstore.cache.FilePathCache  模式前缀是"cache"
org.h2.store.fs.FilePathNio         模式前缀是"nio"
org.h2.store.fs.FilePathNioMapped   模式前缀是"nioMapped"
org.h2.store.fs.FilePathRec         模式前缀是"rec"
org.h2.store.fs.FilePathSplit       模式前缀是"split"
org.h2.store.fs.FilePathZip         模式前缀是"zip"


file:/E:/H2/eclipse-workspace-client/target/classes/org/h2/store/fs/
file:/E:/H2/eclipse-workspace-client/target/classes/





