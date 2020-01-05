### 项目用途

* [H2数据库](http://www.h2database.com/html/main.html) 源代码学习研究(包括代码注释、文档、用于代码分析的测试用例)


### 目录结构

* my-docs: 综合文档

* my-test: 用于代码分析的测试用例

* h2: H2数据库的最新源代码，在源代码中附加了便于分析理解代码的注释


### 把代码导入Eclipse

* 运行 mvn eclipse:eclipse 生成Eclipse项目，打开Eclipse，选择File -> Import -> Existing Projects into Workspace


### 运行或调试H2

* 右击 /h2-research/my-test/my/test/MyServer.java 文件，点Run As或Debug As -> Java Application

* 如果出现"TCP server running ..."这样的提示就ok啦


### 测试

* my.test 包中的类几乎都可直接运行
