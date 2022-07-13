package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.jdbc.Derby
import com.intellisrc.db.jdbc.Firebird
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.JDBCServer
import com.intellisrc.db.jdbc.MariaDB
import com.intellisrc.db.jdbc.MySQL
import com.intellisrc.db.jdbc.Oracle
import com.intellisrc.db.jdbc.PostgreSQL
import com.intellisrc.db.jdbc.SQLServer
import com.intellisrc.db.jdbc.SQLite
import com.intellisrc.net.LocalHost
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate

/**
 * @since 2022/07/08.
 */
class AutoTest extends Specification {
    static File sqliteTmp = File.get("/tmp/sqlite.db")
    static Map<Object, Boolean> dbTest = [
        (Derby)     : true,
        (SQLite)    : true,
        (MariaDB)   : true,
        (MySQL)     : true,
        (Firebird)  : false,
        (Oracle)    : false,
        (SQLServer) : false,
        (PostgreSQL): false
    ] as Map<Object, Boolean>

    static boolean shouldSkip(Object jdbc) {
        boolean skip = jdbc instanceof JDBCServer
            && (jdbc as JDBCServer).port
            &&! LocalHost.hasOpenPort((jdbc as JDBCServer).port)
        if(skip) {
            Log.w("Test skipped for : %s (environment not ready)", jdbc.class.simpleName)
        }
        return skip
    }

    static List<JDBC> getTestable() {
        return dbTest.findAll {
            it.value &&! shouldSkip(it.key)
        }.collect { it.key } as List<JDBC>
    }

    static class User extends Model {
        @Column(primary = true, autoincrement = true)
        int id
        @Column
        String name
        @Column(unsigned = true)
        short age
        @Column
        boolean active = true
        @Column(nullable = true)
        Inet4Address ip4 = null
    }

    /**
     * Model used to update table
     */
    @ModelMeta(version = 2)
    static class UserV2 extends Model {
        @Column(primary = true, autoincrement = true)
        int id
        @Column
        String name
        @Column(unsigned = true)
        short age
        @Column
        boolean active = true
        @Column(nullable = true)
        Inet4Address ip4 = null
        @Column
        URL webpage = null
    }

    static class Alias extends Model {
        @Column(primary = true, autoincrement = true)
        int id
        @Column(ondelete = DeleteActions.CASCADE)
        User user
        @Column
        String name
        @Column
        LocalDate added
    }

    static class Aliases extends Table<Alias>{
        Aliases(Database database) { super(database) }
    }
    static class Users extends Table<User>{
        Users(Database database) { super(database) }
        Users(String name, Database database) { super(name, database) }
    }
    static class UsersV2 extends Table<UserV2>{
        UsersV2(String name, Database database) { super(name, database) }
    }

    JDBC getDB(Class type) {
        JDBC jdbc
        switch (type) {
            case Derby:
                jdbc = new Derby(
                    create  : true,
                    dbname  : File.get(File.tempDir, "derby").absolutePath
                    //memory  : true
                )
                //now = "CURRENT DATE"
                break
            case SQLite:
                jdbc = new SQLite(
                    memory  : true
                    //dbname: sqliteTmp.absolutePath
                )
                //now = "DATE('now')"
                break
            case MySQL:
                jdbc = new MySQL(
                    user    : "test",
                    hostname: "127.0.0.1",
                    password: "test",
                    dbname  : "test",
                    port    : 33006
                )
                break
            case MariaDB:
                jdbc = new MariaDB(
                    user    : "test",
                    hostname: "127.0.0.1",
                    password: "test",
                    dbname  : "test",
                    port    : 33007
                )
                break
            default:
                jdbc = null
                assert jdbc : "Unknown type : " + type.simpleName
        }
        return jdbc
    }

    def setup() {
        println "----------------------------------------------------------------------"
    }

    def cleanup() {
        if(sqliteTmp.exists()) {
            sqliteTmp.delete()
        }
        File derbyLog = File.get("derby.log")
        if(derbyLog.exists()) {
            derbyLog.delete()
        }
        Table.reset()
    }

    @Unroll
    def "Create table model"() {
        setup:
            DB.disableCache = true
            Database database = new Database(getDB(type))
            Users users = new Users(database)
            Aliases aliases = new Aliases(database)
        when:
            User u = new User(
                name : "Benjamin"
            )
        then:
            assert users.get(1) == null
            assert users.insert(u) == 1
            assert u.id == 1
        when:
            Alias alias = new Alias(
                user : u,
                name : "Ben",
                added: SysClock.now.toLocalDate()
            )
        then:
            assert aliases.insert(alias)
            assert aliases.table.field("name").get(1).hasValue()
        when:
            User u2 = users.find(name : "Benjamin")
        then:
            assert u2.id == 1
        when:
            u2.name = "Benji"
            u2.active = false
        then:
            assert users.update(u2)
        when:
            User u3 = users.get(1)
        then:
            assert u3
            assert u3.name == "Benji"
            assert ! u3.active
            assert aliases.get(1).name == "Ben"
        when:
            assert users.replace(new User(
                id  : u3.id,
                name: "Ben",
                age : 77
            ))
            assert users.get(1).age == (77 as short)
        then:
            assert users.find { it.name == "None" } == null
            assert users.get(20) == null
            assert users.delete(u)
            assert aliases.all.empty
        cleanup:
            Table.reset()
            aliases.drop()
            users.drop()
            aliases.quit()
            users.quit()
        where:
            type << testable
    }

    @Unroll
    def "Test Update"() {
        setup:
            DB.disableCache = true
            String tableName = "test_upd"
            Database database = new Database(getDB(type))
            Users users = new Users(tableName, database)
        when:
            int rows = 10
            (1..rows).each {
                User usr = new User(
                    name: "User${it}",
                    age : it + 20,
                    ip4 : "10.0.0.${it}".toInet4Address()
                )
                users.insert(usr)
            }
        then:
            assert users.all.size() == rows : "Number of rows failed before updating"
        when:
            UsersV2 users2 = new UsersV2(tableName, database)
            users2.updateTable() // Update it manually
        then:
            assert users.all.size() == rows : "Number of rows failed after updating"
        when:
            UserV2 u = new UserV2(
                name : "Benjamin",
                webpage: "http://example.com".toURL()
            )
            int uid = users2.insert(u)
        then:
            assert uid == rows + 1
            assert users.table.field("webpage").get(uid).toString().startsWith("http")
        cleanup:
            Table.reset()
            users?.drop()
            users?.quit()
        where:
            type << testable
    }

    @PendingFeature
    def "Test unique constraint"() {
        expect:
            assert false
    }
}
