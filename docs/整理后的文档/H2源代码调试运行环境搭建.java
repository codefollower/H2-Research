
1. 需要事先安装好下面这些工具

JDK 1.5+
TortoiseSVN 1.7+
Eclipse 3.5+


2. 下载H2数据库的源代码

H2数据库的源代码放在Google code: http://h2database.googlecode.com/svn
目前最新的版本是1.3.168，所有的版本可以在svn的tags目录中找到，
因为H2目前比较稳定了，代码提交频率不是很高，不过也会有一些更新，
为了能保持与H2作者同步，所以这里我们选用trunk里的代码: 
http://h2database.googlecode.com/svn/trunk/h2
可以直接用浏览器打开这个URL，或者用TortoiseSVN的Repository Browser打开它。
如下图所示:


用TortoiseSVN checkout这个svn的代码: http://h2database.googlecode.com/svn/trunk/h2
放到E:\H2\trunk (你可以换别的目录，但是要修改下文中与之相关的地方)


3. 一些好习惯

我每次看一个新的开源项目时，都会写一新的代码例子还有各种分析文档，然后要及时提交到本地svn中以免丢失，
比如我在E:\H2建了一个目录my-h2，此目录中还有两个子目录: my-h2-src、my-h2-docs
my-h2-src用来放我自己分析代码时写的各种例子，
my-h2-docs放一些分析文档。


4. 导入Eclipse

H2不是一个用maven构建的项目，也没用ant，它用Java自己写了一个构建程序，
不过我感觉有些复杂，所以也没用它的，还是直接修改Eclipse项目脚本比较简单。

H2有一些编译期依赖，比如lucene、servlet、jdk tools等，
所以可以在E:\H2中建个lib目录，把所有的依赖jar包放到里面。(注: 这些jar在此文最后的附件中有)

因为H2也可以区分Client和Server端，所以为了方便调试分析代码，可以建立两个Eclipse workspace，
这两个Eclipse workspace的".classpath"和".project"文件可以完全一样。(注: 这两个文件在此文最后的附件中有)

".project"文件:

<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>h2</name>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
	<linkedResources>
		<link>
			<name>my-h2-src</name>
			<type>2</type>
			<location>E:/H2/my-h2/my-h2-src</location>
		</link>

		<link>
			<name>h2-main-src</name>
			<type>2</type>
			<location>E:/H2/trunk/src/main</location>
		</link>

		<link>
			<name>h2-tools-src</name>
			<type>2</type>
			<location>E:/H2/trunk/src/tools</location>
		</link>

		<link>
			<name>h2-test-src</name>
			<type>2</type>
			<location>E:/H2/trunk/src/test</location>
		</link>
	</linkedResources>
</projectDescription>

使用了linkedResources，这样多个Eclipse workspace可以共享同样的Java源代码，无论在哪个workspace中改了在另一边都能察觉到。


".classpath"文件:
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="my-h2-src"/>
	<classpathentry kind="src" path="h2-main-src"/>
	<classpathentry kind="src" path="h2-tools-src"/>
	<classpathentry kind="src" path="h2-test-src"/>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
	<classpathentry kind="lib" path="E:/H2/lib/junit-4.8.2.jar"/>
	<classpathentry kind="lib" path="E:/H2/lib/lucene-core-3.4.0.jar"/>
	<classpathentry kind="lib" path="E:/H2/lib/org.eclipse.osgi_3.6.2.R36x_v20110210.jar"/>
	<classpathentry kind="lib" path="E:/H2/lib/servlet-api-2.4.jar"/>
	<classpathentry kind="lib" path="E:/H2/lib/slf4j-api-1.6.1.jar"/>
	<classpathentry kind="lib" path="E:/H2/lib/tools.jar"/>
	<classpathentry kind="output" path="target/classes"/>
</classpath>

前4个src条目都是对前面linkedResources中的引用。

接着在E:\H2中建立两个目录: eclipse-workspace-server、eclipse-workspace-client，
然后把上面两个文件都copy一份到这两个目录中。

紧接着，打开Eclipse，出来一个"Workspace Launcher"框，编辑一下，改成h2-server，这样就会得到一个新的Workspace，

进入Eclipse后，点"File->Import->General->Existing Projects into Workspace"，
最后打开E:\H2\eclipse-workspace-server就能看到H2这个项目了。

再打开Eclipse，出来一个"Workspace Launcher"框，编辑一下，改成h2-client，这样就会得到一个新的h2-client Workspace，

进入Eclipse后，点"File->Import->General->Existing Projects into Workspace"，
最后打开E:\H2\eclipse-workspace-client也能看到H2这个项目。



5. 在Eclipse中让H2跑起来

在h2-server那个workspace中，在my-h2-src下新建一个类，如下:
package my.test;

import java.sql.SQLException;
import java.util.ArrayList;

public class MyServer {
	public static void main(String[] args) throws SQLException {
		ArrayList<String> list = new ArrayList<String>();
		list.add("-tcp");
		org.h2.tools.Server.main(list.toArray(new String[list.size()]));
	}
}

在编辑区右击，点"Run As->Java Aplication"，
如果在Console中出现"TCP server running at..."这样的提示就表示H2 Server起动正常了。


然后转到h2-client那个eclipse workspace，在my-h2-src下新建一个类，如下:

package my.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JDBCTest {
	public static void main(String[] args) throws Exception {
		Class.forName("org.h2.Driver");

		Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mydb", "sa", "");
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("DROP TABLE IF EXISTS my_table");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS my_table(name varchar(20))");
		stmt.executeUpdate("INSERT INTO my_table(name) VALUES('zhh')");

		ResultSet rs = stmt.executeQuery("SELECT name FROM my_table");
		rs.next();
		System.out.println(rs.getString(1));

		stmt.close();
		conn.close();
	}
}

在编辑区右击，点"Run As->Java Aplication"，
如果在Console中出现"zhh"这样的提示就表示H2 Client也正常了。

另外，转到E:\H2\eclipse-workspace-server目录，会发现在此目录中多了一个mydb.h2.db文件，my_table的数据也放在此文件中。


6. H2代码调试简单入门

直接点Eclipse中那个红色的Terminate按钮停掉h2-server，
然后在my.test.MyServer类的编辑区右击，点"Debug As->Java Aplication"，
启动完后，在org.h2.server.TcpServer.listen()方法中打个断点，如下图:

重新运行上面的JDBCTest例子，在h2-server的org.h2.server.TcpServer中就进入断点了，
在h2-server的org.h2.command.Parser.parse(String)中再设个新断点，然后按F8会跳到里面。

