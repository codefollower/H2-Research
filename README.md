## 项目用途

[H2数据库](http://www.h2database.com/html/main.html)源代码学习研究(包括代码注释、文档、用于代码分析的测试用例)


## 目录结构

* my-docs: 综合文档

* my-test: 用于代码分析的测试用例

* h2: H2数据库的最新源代码，在源代码中附加了便于分析理解代码的注释


## 导入项目

* 打开Eclipse，选择File -> Import -> Existing Projects into Workspace


## 运行或调试H2

* 右击h2/test/my/test/MyServer.java文件，点Run As或Debug As -> Java Application

* 如果出现"TCP server running ..."这样的提示就ok啦


## 测试

* my.test包中的类几乎都可直接运行


# Welcome to H2, the Java SQL database. [![Build Status](https://travis-ci.org/h2database/h2database.svg?branch=master)](https://travis-ci.org/h2database/h2database)

## The main features of H2 are:

1. Very fast, open source, JDBC API
2. Embedded and server modes; in-memory databases
3. Browser based Console application
4. Small footprint: around 1.5 MB jar file size

More information: http://h2database.com

## Features

| | [H2](http://www.h2database.com/) | [Derby](http://db.apache.org/derby) | [HSQLDB](http://hsqldb.org) | [MySQL](http://mysql.com) | [PostgreSQL](http://www.postgresql.org) |
|---------------------------|-------|-------|-------|-------|-------|
| Pure Java                 | Yes   | Yes   | Yes   | No    | No    |
| Memory Mode               | Yes   | Yes   | Yes   | No    | No    |
| Encrypted Database        | Yes   | Yes   | Yes   | No    | No    |
| ODBC Driver               | Yes   | No    | No    | Yes   | Yes   |
| Fulltext Search           | Yes   | No    | No    | Yes   | Yes   |
| Multi Version Concurrency | Yes   | No    | Yes   | Yes   | Yes   |
| Footprint (jar/dll size)  | ~1 MB | ~2 MB | ~1 MB | ~4 MB | ~6 MB | 
