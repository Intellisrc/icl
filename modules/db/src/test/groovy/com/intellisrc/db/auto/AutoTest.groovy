package com.intellisrc.db.auto

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.UpdateActions
import com.intellisrc.db.jdbc.*
import com.intellisrc.log.CommonLogger
import com.intellisrc.log.PrintLogger
import com.intellisrc.net.Email
import com.intellisrc.net.LocalHost
import com.intellisrc.term.TableMaker
import org.junit.jupiter.api.Assumptions
import org.slf4j.event.Level
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * @since 2022/07/08.
 */
abstract class AutoTest extends Specification {
    static boolean ci = Config.env.get("gitlab.ci", Config.any.get("github.actions", false))
    boolean tested = false

    @SuppressWarnings('unused')
    boolean shouldSkip() {
        JDBC conn = connJdbc
        boolean skip = conn instanceof JDBCServer
            && (ci || ((conn as JDBCServer).hostname &&! LocalHost.hasOpenPort((conn as JDBCServer).port)))
        if(skip) {
            Log.w("Test skipped for : %s (environment not ready)", conn.class.simpleName)
        }
        return skip
    }

    static class User extends Model {
        @Column(primary = true, autoincrement = true)
        int id
        @Column
        String name = ""
        @Column(unsigned = true)
        short age = 0
        @Column
        boolean active = true
        @Column(nullable = true)
        Inet4Address ip4 = null
    }

    static class Alias extends Model {
        @Column(primary = true, autoincrement = true)
        int id
        @Column(ondelete = DeleteActions.CASCADE, onupdate = UpdateActions.CASCADE)
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
        @Column(primary = true, ondelete = DeleteActions.CASCADE, onupdate = UpdateActions.CASCADE)
        User user
        @Column(primary = true, ondelete = DeleteActions.CASCADE, onupdate = UpdateActions.CASCADE)
        UserEmail email
        @Column
        boolean enabled = true
    }

    static class Address extends Model {
        @Column(primary = true, ondelete = DeleteActions.CASCADE, onupdate = UpdateActions.CASCADE)
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

    abstract JDBC getConnJdbc()
    Level getLogLevel() {
        return Level.TRACE
    }

    def setup() {
        Assumptions.assumeTrue(! shouldSkip(), "Condition not met, skipping.")
        tested = true
        Log.i("Setting up Test...")
        PrintLogger printLogger = CommonLogger.default.printLogger
        printLogger.setLevel(logLevel)
        DB.clearCache()
    }

    def cleanup() {
        if(tested) {
            DB.clearCache()
            JDBC conn = connJdbc
            Database database = new Database(conn)
            DB db = database.connect()
            db.dropAllTables()
            db.close()
            database.quit()
            Log.i("AutoTest Cleanup completed")
        }
    }

    def "Create table model"() {
        setup:
            JDBC jdbc = connJdbc
            Database database = new Database(jdbc)
            DB db = database.connect()
            db.dropAllTables()
            db.close()

            Users users = new Users(database)
            Aliases aliases = new Aliases(database)
            aliases.clear()
            users.clear(true) //TRUNCATE fails if it has FK in it
        when:
            User u = new User(
                name : "Benjamin",
                age  : 99
            )
            User v = new User(
                name : "Angelina",
                age  : 88
            )
            User w = new User(
                name : "Anthony",
                age  : 77
            )
        then:
            assert users.get(1) == null
            assert users.insert(u) == 1
            assert u.uniqueId == 1
            assert users.insert(v) == 2
            assert users.insert(w) == 3
            assert users.count() == 3
            assert users.count(age : v.age) == 1
            assert users.count(jdbc.getFieldForQuery("age") + " > ?", 80) == 2
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
            Alias a1 = aliases.get(1, false)
            Map aMap = aliases.getRecord(1)
        then:
            assert a1.user.id == 1
            assert a1.user.name == ""
            assert aMap.containsKey("user_id")
            assert aMap.user_id == 1
        when:
            User u2 = users.find(name : u.name)
            List<Map> userList = users.findRecords(name : u.name)
        then:
            assert u2.uniqueId == 1
            assert userList.size() == 1
            assert userList.first().age == u.age
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
            try {
                aliases.reset()
                users.reset()
                aliases.drop(true)
                users.drop(true)
                database.quit()
            } catch(Exception ignore) {}
    }

    def "Multi-column Primary Key should work fine"() {
        setup:
            JDBC jdbc = connJdbc
            Database database = new Database(jdbc)
            Users users = new Users(database)
            Emails emails = new Emails(database)
            Inboxes inboxes = new Inboxes(database)
            inboxes.clear()
            emails.clear(true)
            users.clear(true)
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
                assert inbox.user : "User was null"
                assert inbox.email : "Email was null"
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
                    assert t.clear(true)
                    assert t.all.size() == 0
            }
        cleanup:
            [inboxes, users, emails].each {
                it?.reset()
                it?.drop()
            }
            database.quit()
    }

    def "Primary Key is Model"() {
        setup:
            JDBC jdbc = connJdbc
            Database database = new Database(jdbc)
            Users users = new Users(database)
            Addresses addresses = new Addresses(database)
            addresses.clear()
            users.clear(true)
            assert ! addresses.primaryKeys.empty
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
                    zip: "9000${usr.id}".toString(),
                    city: "Gothic City"
                ))
            }
        then:
            User user = users.get(1)
            Address address = addresses.find("user", user)
            assert address.zip == "9000${user.id}".toString()
        when:
            address.zip = "444444"
        then:
            assert addresses.update(address)
        when:
            List<Address> info = addresses.all
            new TableMaker(info.collect { it.toMap() }).print()
        then:
            assert addresses.find("user", user).zip == address.zip
            assert addresses.findRecord("zip", address.zip).containsKey("user_id")
        when:
            addresses.getAll("zip", Query.SortOrder.ASC, {
                List<Address> chunk ->
                    assert chunk.size() == rows
                    assert chunk.first().user.name == ""
            }, false)
            addresses.getRecords("zip", Query.SortOrder.ASC, {
                List<Map> chunk ->
                    assert chunk.size() == rows
                    assert chunk.first().user_id as int > 0
            })
        then:
            assert addresses.delete(address)
            assert addresses.all.size() == 2
        cleanup:
            addresses.reset()
            [addresses, users].each {
                it?.drop(true)
            }
            database.quit()
    }

    def "Insert, update and delete in bulk"() {
        setup:
            JDBC jdbc = connJdbc
            Database database = new Database(jdbc)
            Emails emails = new Emails(database)
            emails.clear(true)
            assert ! emails.primaryKeys.empty
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
        then:
            assert emails.insert(emailList)
        when:
            long time = ChronoUnit.MILLIS.between(start, SysClock.now)
            Log.i("%d new records, took: %d ms", rows, time)
        then:
            //assert time < 15000
            assert emails.count() == rows    : "Number of rows failed"
        when:
            List<Map> recs1 = emails.getRecords(20, 10)
            List<Map> recs2 = emails.getRecords("id", Query.SortOrder.ASC, 100, 100)
            List<Map> recsInv = emails.getRecords("id", Query.SortOrder.DESC)
        then:
            assert recs1.size() == 20 //NOTE: Oracle does not keep order as MySQL, so we can't be sure which elements we get unless we sort them
            assert recs2.size() == 100
            assert recs2.first().email.toString().contains("101")
            assert recsInv.first().email.toString().contains("500")
        when:
            int numToDelete = 10
            List<Integer> toDelete = emails.all.subList(0, numToDelete).collect { it.id }
        then:
            assert emails.deleteByPK(toDelete) : "Unable to delete IDs"
            assert emails.count() == rows - numToDelete
        when:
            String userToFind = "user500@example.com"
            List<UserEmail> newEmailList = []
            emails.getAll({
                List<UserEmail> chunk ->
                    assert chunk.size() <= rows
                    chunk.each {
                        it.email = new Email(it.email.toString().replace("example.com", "example.net"))
                        newEmailList << it
                    }
            })
            List<Map> few = emails.getRecords("id", Query.SortOrder.DESC, 10, 10)
            emails.getRecords({
                List<Map> chunk ->
                    assert chunk.first().email instanceof String
            })
            Map email500 = emails.findRecord("id", 500)
            List<Map> finder = emails.findRecords("id", 500)
            emails.findRecords("id", 500, {
                List<Map> chunk ->
                    assert chunk.size() == 1
                    assert chunk.first().email == userToFind
            })
        then:
            assert emails.update(newEmailList)
            assert emails.delete(newEmailList)
            assert emails.count() == 0
            assert emails.clear(true)
            assert emails.count() == 0
            assert few.size() == 10
            assert few.first().id as int == 490
            assert few.last().id as int == 481
            assert email500.email == userToFind
            assert finder.size() == 1
            assert finder.first().email == userToFind
        cleanup:
            emails.reset()
            [emails].each {
                it?.drop(true)
            }
            database.quit()
    }
}
