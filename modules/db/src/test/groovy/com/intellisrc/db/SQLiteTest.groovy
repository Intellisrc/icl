package com.intellisrc.db

import com.intellisrc.etc.Log
import spock.lang.Specification

/**
 * @since 18/06/15.
 */
class SQLiteTest extends Specification {
    def "Create database"() {
        setup:
            Log.level = Log.Level.VERBOSE
            Database.init(new SQLite(
                dbname: "test.db"
            ))
        when: "Create connection"
            DB db = Database.connect()
        then: "Create table"
            assert db.set("""CREATE TABLE `test` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `name` VARCHAR NOT NULL
            );""")
        then: "Insert first"
            assert db.table("test").insert([
                    name : "Tester"
            ])
            assert db.lastID == 1
        then: "Insert Second"
            assert db.table("test").insert([
                    name : "Sampler"
            ])
            assert db.lastID == 2
        then: "Retrieve second"
            assert db.table("test").field("name").key("id").get(2).toString() == "Sampler"
        cleanup:
            new File("test.db").delete()
            Database.quit()
    }


}