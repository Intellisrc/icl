package com.intellisrc.db.auto

import com.intellisrc.core.SysClock
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.jdbc.Derby
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.SQLite
import spock.lang.Specification

import java.time.LocalDate

/**
 * @since 2022/07/08.
 */
class AutoTest extends Specification {
    static File sqliteTmp = File.get("/tmp/sqlite.db")

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

    static class Aliases extends Table<Alias>{
        Aliases(Database database) { super(database) }
    }
    static class Users extends Table<User>{
        Users(Database database) { super(database) }
    }

    JDBC getDB(Class type) {
        JDBC jdbc
        switch (type) {
            case Derby:
                jdbc = new Derby(
                    hostname: "localhost",
                    create  : true,
                    memory  : true
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
            default:
                jdbc = null
                assert jdbc : "Unknown type : " + type.simpleName
        }
        return jdbc
    }

    def cleanup() {
        if(sqliteTmp.exists()) {
            sqliteTmp.delete()
        }
        File derbyLog = File.get("derby.log")
        if(derbyLog.exists()) {
            derbyLog.delete()
        }
    }

    def "Create table model"() {
        setup:
            Database database = new Database(getDB(type))
            Users users = new Users(database)
            Aliases aliases = new Aliases(database)
            //noExceptionThrown()
        when:
            User u = new User(
                name : "Benjamin"
            )
        then:
            assert users.get(1) == null
            assert users.insert(u)
            assert u.id == 1
        when:
            Alias alias = new Alias(
                user : u,
                name : "Ben",
                added: SysClock.now.toLocalDate()
            )
        then:
            assert aliases.insert(alias)
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
        where:
            type    | enabled
            Derby   | true
            //SQLite  | true
    }
}
