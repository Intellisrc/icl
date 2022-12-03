package com.intellisrc.db.auto

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.PostgreSQL
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

/**
 * @since 2022/07/08.
 */
class AutoTest extends Specification {
    static boolean ci = Config.env.get("gitlab.ci", Config.any.get("github.actions", false))
    static File sqliteTmp = File.get(File.tempDir, "sqlite.db")
    static File derbyTmp = File.get(File.tempDir, "derby.db")

    static Map<String, Integer> ports = [
        mysql : 33006,
        mariadb : 33007,
        postgres : 35432
    ]

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

    static class Address extends Model {
        @Column(primary = true)
        User user
        @Column(nullable = false)
        String address
        @Column(nullable = false)
        String zip
        @Column(nullable = false)
        String city
    }

    static class Aliases extends Table<Alias>{
        Aliases(Database database) { super(database) }
    }
    static class Users extends Table<User>{
        Users(Database database) { super(database) }
        Users(String name, Database database) { super(name, database) }
    }
    static class Emails extends Table<UserEmail> {
        Emails(Database database) { super(database) }
    }
    static class Inboxes extends Table<Inbox> {
        Inboxes(Database database) { super(database) }
    }
    static class Addresses extends Table<Address> {
        Addresses(Database database) { super(database) }
    }

    static List<JDBC> getTestable(boolean update = false) {
        List<JDBC> dbs = []
        /*dbs << new Derby(
            create: true,
            memory: true,
            useFK : !update
            //dbname  : derbyTmp.absolutePath
        )
        //now = "CURRENT DATE"
        dbs << new SQLite(
                dbname: sqliteTmp.absolutePath
        )
        if(!ci && LocalHost.hasOpenPort(ports.mariadb)) {
            dbs << new MariaDB(
                user: "test",
                hostname: "127.0.0.1",
                password: "test",
                dbname: "test",
                port: ports.mariadb
            )
        }
        if(!ci && LocalHost.hasOpenPort(ports.mysql)) {
            dbs << new MySQL(
                user: "test",
                hostname: "127.0.0.1",
                password: "test",
                dbname: "test",
                port: ports.mysql
            )
        }*/
        if(!ci && LocalHost.hasOpenPort(ports.postgres)) {
            dbs << new PostgreSQL(
                user: "test",
                hostname: "127.0.0.1",
                password: "test",
                dbname: "test",
                port: ports.postgres
            )
        }
        return dbs
    }

    def setup() {
        Log.i("Initializing Test...")
        PrintLogger printLogger = CommonLogger.default.printLogger
        printLogger.setLevel(Level.TRACE)
        if(sqliteTmp.exists()) { sqliteTmp.delete() }
        DB.clearCache()
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
            Database database = new Database(type)
            Users users = new Users(database)
            Aliases aliases = new Aliases(database)
            aliases.clear()
            users.clear()
        when:
            User u = new User(
                name : "Benjamin",
                age  : 99
            )
        then:
            assert users.get(1) == null
            assert users.insert(u) == 1
            assert u.uniqueId == 1
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
            assert u2.uniqueId == 1
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
                id  : u3.uniqueId,
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
    def "Multi-column Primary Key should work fine"() {
        setup:
            Database database = new Database(type)
            Users users = new Users(database)
            Emails emails = new Emails(database)
            Inboxes inboxes = new Inboxes(database)
            inboxes.clear()
            emails.clear()
            users.clear()
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
                Inbox inbox = inboxes.get([usr.uniqueId, email.uniqueId]).first()
                assert inbox.enabled
                inbox.enabled = false
                assert inboxes.update(inbox)
                assert ! inboxes.table.field("enabled").get([usr.uniqueId, email.uniqueId]).toBool()
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

    @Unroll
    def "Primary Key is Model"() {
        setup:
            Database database = new Database(type)
            Users users = new Users(database)
            Addresses addresses = new Addresses(database)
            addresses.clear()
            users.clear()
            assert ! addresses.pks.empty
        when:
            int rows = 3
            (1..rows).each {
                User usr = new User(
                    name: "User${it}",
                    age: it + 20,
                    ip4: "10.0.0.${it}".toInet4Address()
                )
                assert users.insert(usr)
                addresses.insert(new Address(
                    user: usr,
                    address: "Street $it number ${usr.id}",
                    zip: "9000${usr.id}",
                    city: "Gothic City"
                ))
            }
        then:
            User user = users.get(1)
            Address address = addresses.find("user", user)
            assert address.zip == "9000${user.id}"
        when:
            address.zip = "444444"
        then:
            assert addresses.update(address)
            assert addresses.find("user", user).zip == "444444"
        when:
            assert addresses.delete(address)
        then:
            assert addresses.all.size() == 2
        cleanup:
            Table.reset()
            [addresses, users].each {
                it?.drop()
                it?.quit()
            }
        where:
            type << testable
    }

    @Unroll
    def "Insert, update and delete in bulk"() {
        setup:
            Database database = new Database(type)
            Emails emails = new Emails(database)
            emails.clear()
            assert ! emails.pks.empty
        when:
            int rows = 500
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
            assert time < 15000
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
            assert emails.delete(newEmailList)
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
