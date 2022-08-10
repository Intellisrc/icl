package com.intellisrc.db.auto

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.jdbc.*
import com.intellisrc.log.CommonLogger
import com.intellisrc.log.PrintLogger
import com.intellisrc.net.Email
import com.intellisrc.net.LocalHost
import org.slf4j.event.Level
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static com.intellisrc.db.Query.SortOrder.DESC

/**
 * @since 2022/07/08.
 */
class AutoTest extends Specification {
    static boolean ci = Config.env.get("gitlab.ci", Config.any.get("github.actions", false))
    static File sqliteTmp = File.get(File.tempDir, "sqlite.db")
    static File derbyTmp = File.get(File.tempDir, "derby.db")
    static Map<Object, Boolean> dbTest = [
        (Derby)     : true,
        (SQLite)    : true,
        (MariaDB)   : !ci,
        (MySQL)     : !ci,
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

    static class UserEmail extends Model {
        @Column(primary = true, autoincrement = true)
        int id
        @Column
        Email email
    }

    static class Inbox extends Model {
        @Column(primary = true)
        User user
        @Column(primary = true)
        UserEmail email
        @Column
        boolean enabled = true
    }

    static class Aliases extends Table<Alias>{
        Aliases(Database database) { super(database) }
    }
    static class Users extends Table<User>{
        Users(Database database) { super(database) }
        Users(String name, Database database) { super(name, database) }
    }
    static class UsersV2 extends Table<UserV2>{
        boolean execFired = false
        UsersV2(String name, Database database) { super(name, database) }

        @Override
        boolean execOnUpdate(DB table, int prevVersion, int currVersion) {
            execFired = true
            return false
        }
    }
    static class Emails extends Table<UserEmail> {
        Emails(Database database) { super(database) }
    }
    static class Inboxes extends Table<Inbox> {
        Inboxes(Database database) { super(database) }
    }

    JDBC getDB(Class type) {
        JDBC jdbc
        switch (type) {
            case Derby:
                jdbc = new Derby(
                    create  : true,
                    memory  : true,
                    //dbname  : derbyTmp.absolutePath
                )
                //now = "CURRENT DATE"
                break
            case SQLite:
                jdbc = new SQLite(
                    dbname: sqliteTmp.absolutePath
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
        Log.i("Initializing Test...")
        PrintLogger printLogger = CommonLogger.default.printLogger
        printLogger.setLevel(Level.TRACE)
    }

    def cleanup() {
        if(sqliteTmp.exists()) {
            sqliteTmp.delete()
        }
        if(derbyTmp.exists()) {
            derbyTmp.deleteDir()
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
                name : "Benjamin",
                age  : 99
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
            assert Table.getFieldName("some_name") == "someName"
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
            assert users.find("name", "Ben").age == (77 as short)
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
            assert users.getAll(5).size() == 5 : "Limit failed"
            assert users.getAll("age", DESC).first().id == rows : "Limit failed"
            assert users.getAll("age", DESC, 5).last().id == rows - 5 + 1 : "Sort with limit failed"
        when:
            int times = 0
            List<Short> ageList = []
            users.chunkSize = 3
            users.getAll({
                times++
                it.each {
                    ageList << it.age
                }
            })
        then:
            assert times == Math.ceil(rows / 3) as int : "Number of chunks were incorrect"
            assert ageList.size() == rows // Checking for duplicated
            assert ageList.unique().size() == rows // No duplication
        when:
            UsersV2 users2 = new UsersV2(tableName, database)
            users2.updateTable() // Update it manually
        then:
            assert users2.execFired : "execOnUpdate was not fired"
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

    def "Multi-column Primary Key should work fine"() {
        setup:
            DB.disableCache = true
            Database database = new Database(getDB(type))
            Users users = new Users(database)
            Emails emails = new Emails(database)
            Inboxes inboxes = new Inboxes(database)
        when:
            int rows = 3
            (1..rows).each {
                User usr = new User(
                    name: "User${it}",
                    age : it + 20,
                    ip4 : "10.0.0.${it}".toInet4Address()
                )
                UserEmail email = new UserEmail(
                    email: new Email("user${it}@example.com")
                )
                assert users.insert(usr)
                assert emails.insert(email)
                inboxes.insert(new Inbox(
                    user: usr,
                    email: email
                ))
                Inbox inbox = inboxes.get([usr.id, email.id]).first()
                assert inbox.enabled
                inbox.enabled = false
                assert inboxes.update(inbox)
                assert ! inboxes.table.field("enabled").get([usr.id, email.id]).toBool()
            }
        then:
            assert users.all.size() == rows     : "Number of rows failed"
            assert emails.all.size() == rows    : "Number of rows failed"
            assert inboxes.all.size() == rows   : "Number of rows failed"
        then:
            [inboxes, users, emails].each {
                Table t ->
                    assert t.deleteAll()
                    assert t.all.size() == 0
            }
        cleanup:
            Table.reset()
            [inboxes, users, emails].each {
                it?.drop()
                it?.quit()
            }
        where:
            type << testable
    }
    def "Insert, update and delete in bulk"() {
        setup:
            DB.disableCache = true
            Database database = new Database(getDB(type))
            Emails emails = new Emails(database)
            assert ! emails.pks.empty
        when:
            int rows = 10
            List<UserEmail> emailList = []
            (1..rows).each {
                emailList << new UserEmail(
                    email: new Email("user${it}@example.com")
                )
            }
            Log.i("Inserting rows...")
            LocalDateTime start = SysClock.now
            assert emails.insert(emailList)
            long time = ChronoUnit.MILLIS.between(start, SysClock.now)
            Log.i("%d new records, took: %d ms", rows, time)
            //assert time < 5000
        then:
            assert emails.count() == rows    : "Number of rows failed"
        then:
            List<UserEmail> newEmailList = []
            emails.getAll({
                List<UserEmail> chunk ->
                    assert chunk.size() <= rows
                    chunk.each {
                        it.email = new Email(it.email.toString().replace("example.com", "example.net"))
                        newEmailList << it
                    }
            })
            assert emails.update(newEmailList)
        then:
            assert emails.clear()
            assert emails.count() == 0
        cleanup:
            Table.reset()
            [emails].each {
                it?.drop()
                it?.quit()
            }
        where:
            type << testable
    }
}
