有三种产生TableView的方式:
1. CREATE VIEW语句

如: CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view COMMENT IS 'my view'(f1,f2) 
		AS SELECT id,name FROM CreateViewTest


2. 嵌入在FROM中的临时视图

如: select id,name from (select id,name from CreateViewTest)


3. WITH RECURSIVE语句

如: WITH RECURSIVE my_tmp_table(f1,f2) 
		AS (select id,name from CreateViewTest UNION ALL select 1, 2) 
			select f1, f2 from my_tmp_table
RECURSIVE这个关键字是可选的


只有2可以带Parameters，它是通过这个方法调用: org.h2.table.TableView.createTempView(Session, User, String, Query, Query)
只有3的recursive是true 


TableView构造函数
	=> init
		=> ViewIndex
		=> initColumnsAndTables
			=> removeViewFromTables
			=> compileViewQuery //重新对select语句进行解析和prepare
			=> setColumns
			=> addViewToTables

removeViewFromTables和addViewToTables相对应，把view自身加到相关的Table中
一个view可以与多个table相关，取决于select语句中涉及的表个数:
对于以下例子:
CREATE OR REPLACE FORCE VIEW my_view (f1,f2) AS SELECT t1.f1, t2.f2 FROM t1, t2?
那么my_view就涉及两个表t1，t2
removeViewFromTables就是从t1，t2中把my_view删除，
而addViewToTables就是把my_view加到t1，t2中。

为企么要有removeViewFromTables和addViewToTables呢，
因为init方法在OR REPLACE时也会通过TableView类的replace方法调用，所以在t1，t2中已有my_view了，
但是CREATE VIEW语句会变，所以此时的my_view对象有可能是旧的，所以要删除

initColumnsAndTables就是根据select语句定义view中有哪些字段，这些字段的类型跟select中的select表达式列表中的对应表达式一样

		//select字段个数比view字段多的情况，多出来的按select字段原来的算
		//这里实际是f1、name
		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1) " //
				+ "AS SELECT id,name FROM CreateViewTest";

		//select字段个数比view字段少的情况，view中少的字段被忽略
		//这里实际是f1，而f2被忽略了，也不提示错误
		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
				+ "AS SELECT id FROM CreateViewTest";

		//不管加不加FORCE，跟上面也一样
		sql = "CREATE OR REPLACE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
				+ "AS SELECT id FROM CreateViewTest";






虽然执行了两次initColumnsAndTables，但是init中还建立了ViewIndex
所以这里是必须调用init的
replace
	=> init
		=> ViewIndex
		=> initColumnsAndTables
			=> removeViewFromTables
			=> compileViewQuery //重新对select语句进行解析和prepare
			=> setColumns
			=> addViewToTables
	=> recompile
		=> compileViewQuery
		=> getViews
		=> initColumnsAndTables
		=> getViews中得到的依赖于当前view的都需要recompile


