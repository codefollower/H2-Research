/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.test.command.ddl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.h2.engine.Constants;
import org.h2.security.SHA256;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

import my.test.TestBase;

public class CreateUserTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new CreateUserTest().start();
    }

    @Override
    public void init() throws Exception {
        // prop.setProperty("user", "SA2");
        // prop.setProperty("password", "78");
    }

    // 测试org.h2.command.Parser.parseCreateUser()和org.h2.command.ddl.CreateUser
    // 和org.h2.engine.User
    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("DROP SCHEMA IF EXISTS TEST_SCHEMA2");

        stmt.executeUpdate("DROP ROLE IF EXISTS sa1");
        // stmt.executeUpdate("CREATE ROLE IF NOT EXISTS sa1");

        stmt.executeUpdate("DROP USER IF EXISTS SA222 CASCADE");
        stmt.executeUpdate("DROP USER IF EXISTS sa1 CASCADE");
        // stmt.executeUpdate("DROP USER IF EXISTS SA2 CASCADE");
        stmt.executeUpdate("DROP USER IF EXISTS SA3 CASCADE");

        stmt.executeUpdate("CREATE USER IF NOT EXISTS sa1 PASSWORD 'abc' ADMIN");
        // X不加也是可以的
        stmt.executeUpdate("CREATE USER IF NOT EXISTS SA2 SALT X'123456' HASH X'78' ADMIN"); // X'...'必须是偶数个
        stmt.executeUpdate("CREATE USER IF NOT EXISTS SA3 IDENTIFIED BY abc"); // 密码不加引号

        stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS TEST_SCHEMA2 AUTHORIZATION SA2");

        stmt.executeUpdate("DROP USER IF EXISTS guest");
        stmt.executeUpdate("CREATE USER IF NOT EXISTS guest COMMENT 'create a guest user' PASSWORD 'abc'");

        stmt.executeUpdate("ALTER USER SA2 SET PASSWORD '123'");
        stmt.executeUpdate("ALTER USER SA2 SET SALT X'123456' HASH X'78'");

        stmt.executeUpdate("ALTER USER SA2 RENAME TO SA222");
        stmt.executeUpdate("DROP SCHEMA IF EXISTS TEST_SCHEMA2");
        stmt.executeUpdate("ALTER USER SA222 ADMIN false");
        // rightTest();

        byte[] userPasswordHash = SHA256.getKeyPasswordHash("SA222", "test".toCharArray());
        byte[] salt = new byte[Constants.SALT_LEN];
        MathUtils.randomBytes(salt);

        byte[] passwordHash = SHA256.getHashWithSalt(userPasswordHash, salt);

        String passwordHashStr = "X'" + StringUtils.convertBytesToHex(passwordHash) + "'";
        String saltStr = "X'" + StringUtils.convertBytesToHex(salt) + "'";

        stmt.executeUpdate("ALTER USER SA222 SET SALT " + saltStr + " HASH " + passwordHashStr);

        Properties prop = new Properties();
        prop.setProperty("user", "SA222");
        prop.setProperty("password", StringUtils.convertBytesToHex(userPasswordHash));
        prop.setProperty("PASSWORD_HASH", "true");
        Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mydb", prop);
        conn.close();

        // H2的bug，重命名用户后将不能通过原来的密码登录
        // stmt.executeUpdate("ALTER USER sa RENAME TO root");
    }

    void rightTest() throws Exception {
        stmt.executeUpdate("DROP TABLE IF EXISTS CreateUserTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 int)");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 IDENTITY)");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 IDENTITY(1,10))");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 SERIAL(1,10)))");

        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 IDENTITY(1,10),PRIMARY KEY(f1))");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 int,PRIMARY KEY(f1))");

        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 int,CONSTRAINT IF NOT EXISTS my_constraint
        // COMMENT IS 'haha' INDEX int)");

        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 int,f2 int,CONSTRAINT IF NOT EXISTS
        // my_constraint COMMENT IS 'haha' INDEX my_int(f1,f2))");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS TEST9.public.CreateUserTest (f1 int,f2 int,"
        // + "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' CHECK f1>0)");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 int,f2 int,"
        // +
        // "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' UNIQUE KEY INDEX my_constraint2(f1,f2) INDEX
        // myi)");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateUserTest (f1 int,f2 int,"
        // + "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' FOREIGN KEY(f1,f2)) INDEX my-i REFERENCES(f1)");

        stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole1");
        stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole2");
        stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole3");

        // GRANT
        stmt.executeUpdate("GRANT SELECT,DELETE,INSERT ON CreateUserTest TO PUBLIC");
        stmt.executeUpdate("GRANT UPDATE ON CreateUserTest TO PUBLIC");
        stmt.executeUpdate("GRANT SELECT,DELETE,INSERT,UPDATE ON CreateUserTest TO SA2");
        stmt.executeUpdate("GRANT SELECT,DELETE,INSERT,UPDATE ON CreateUserTest TO myrole1");

        stmt.executeUpdate("GRANT myrole1 TO myrole2");
        // stmt.executeUpdate("GRANT myrole2 TO myrole2");
        stmt.executeUpdate("GRANT myrole2 TO myrole1");
        stmt.executeUpdate("GRANT myrole1 TO myrole3");
        stmt.executeUpdate("GRANT myrole3 TO PUBLIC");

        stmt.executeUpdate("GRANT myrole1 TO PUBLIC");
        stmt.executeUpdate("GRANT myrole1 TO SA3");
        stmt.executeUpdate("GRANT myrole1 TO myrole2");
        // stmt.executeUpdate("GRANT myrole2 TO myrole2");//cyclic role grants are not allowed

        // REVOKE
        stmt.executeUpdate("REVOKE SELECT,DELETE,INSERT,UPDATE ON CreateUserTest FROM PUBLIC");
        stmt.executeUpdate("REVOKE SELECT,DELETE,INSERT,UPDATE ON CreateUserTest FROM SA2");
        stmt.executeUpdate("REVOKE SELECT,DELETE,INSERT,UPDATE ON CreateUserTest FROM myrole1");

        stmt.executeUpdate("REVOKE myrole1 FROM PUBLIC");
        stmt.executeUpdate("REVOKE myrole1 FROM SA3");
        stmt.executeUpdate("REVOKE myrole1 FROM myrole2");
    }
}
