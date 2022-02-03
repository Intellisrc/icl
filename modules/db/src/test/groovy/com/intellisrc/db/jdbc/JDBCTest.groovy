package com.intellisrc.db.jdbc

import com.intellisrc.core.Log
import com.intellisrc.db.ColumnInfo
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import spock.lang.Specification

import static com.intellisrc.db.Query.SortOrder.*

/**
 * @since 2022/01/20.
 */
abstract class JDBCTest extends Specification {
    abstract JDBC getDB()
    void clean(DB db, String table) {}
    /**
     * Create a table:
     *      id      : int auto-increment
     *      name    : varchar
     *      version : float / double
     *      active  : enum(Y,N)
     *      updated : datetime
     * @param name
     * @return
     */
    String getTableCreate(String nam) { return "" }
    List<String> getTableCreateMulti(String name) { [] }

    def setup() {
        DB db = getDB().connect()
        db.dropAllTables()
        db.close()
    }

    def "Simple Connection"() {
        setup:
            String table = "linux"
        when:
            JDBC jdbc = getDB()
        then:
            assert jdbc : "JDBC object is empty"
        when:
            DB db = jdbc.connect()
            Object setDate = {
                String d ->
                    return jdbc.supportsDate ? d.toDate() : d
            }
        then:
            assert db : "Unable to connect"
        then:
            assert getTableCreate(table) || getTableCreateMulti(table) : "Missing table create"
        then: "Create table"
            assert getTableCreate(table) ? db.setSQL(getTableCreate(table)) : db.setSQL(getTableCreateMulti(table))
        then: "List tables"
            assert ! db.tables.empty
        then: "Must be open"
            assert db.opened
            assert ! db.closed
        when: "Table info"
            List<ColumnInfo> info = db.table(table).info()
            Log.i("Table Info:")
            Log.i("-----------------------------------------------------------")
            info.each {
                Log.i("\t\t%s",it.toString())
            }
            Log.i("-----------------------------------------------------------")
        then: "Must have information"
            assert ! info.empty
        then: "Must have a primary key"
            assert info.find { it.primaryKey }
        then: "Table must exists"
            assert db.table(table).exists()
        then: "Insert first"
            assert db.table(table).insert(
                [ name : "Ubuntu",     active: "Y",    updated: setDate("2022-08-01"),     version: 3.7 ],
            )
            assert db.lastID == 1
        then: "Insert Second"
            assert db.table(table).insert(
                [ name : "Debian",     active: "Y",    updated: setDate("2022-09-01"),     version: 9.1 ],
            )
            assert db.lastID == 2
        then: "Retrieve all"
            assert db.table(table).get().toListMap().size() == 2
        then: "Columns must match (getting first with automatic PK)"
            assert db.table(table).get(1).toMap().keySet().size() == 5
        then: "Retrieve second (using manual PK)"
            assert db.table(table).field("name").key("id").get(2).toString() == "Debian"
        then: "Bulk insert"
            assert db.table(table).insert([
                [ name : "RedHat",      active: "N",    updated: setDate("2009-11-01"),     version: 3.3 ],
                [ name : "Slackware",   active: "N",    updated: setDate("2007-09-01"),     version: 2.1 ],
                [ name : "Suse",        active: "N",    updated: setDate("2006-01-01"),     version: 3.9 ],
                [ name : "Kali",        active: "Y",    updated: setDate("2021-12-01"),     version: 7.4 ],
                [ name : "Fedora",      active: "N",    updated: setDate("2012-03-01"),     version: 7.3 ],
                [ name : "MX Linux",    active: "Y",    updated: setDate("2021-04-01"),     version: 1.2 ],
                [ name : "Manjaro",     active: "Y",    updated: setDate("2022-06-01"),     version: 3.2 ],
            ])
        then:
            assert db.table(table).count().get().toInt() == 9
        when: "Get selected IDs"
            List list = db.table(table).fields("name", "version").get([1,3,5]).toListMap()
        then:
            assert list.size() == 3
            assert list.last().keySet().size() == 2
            assert (list.last().version as float) == 3.9f
        then: "Get by date"
            assert db.table(table).field("name").key("updated").get("2021-12-01".toDate()).toString() == "Kali"
        then: "Get using map"
            if(jdbc.supportsDate) {
                assert db.table(table).get([
                    updated: "2022-06-01".toDate()
                ]).toMap().name == "Manjaro"
            } else {
                assert db.table(table).get([
                    updated: "2022-06-01"
                ]).toMap().name == "Manjaro"
            }
        when: "Get using where"
            list = db.table(table).field("name").where("version > ?", 5).get().toList()
        then:
            assert list.size() == 3
            assert list.contains("Fedora".toString())
        when: "Get using where multiple args"
            list = db.table(table).field("name").where("version > ? AND version < ?", [2, 5]).get().toList()
        then:
            assert list.size() == 5
            assert list.contains("Ubuntu".toString())
        then: "Get the maximum"
            assert db.table(table).field("version").max("version").get().toFloat() == 9.1f
        then: "Get the minimum"
            assert db.table(table).field("version").min("version").get().toFloat() == 1.2f
        then: "Get the average"
            assert db.table(table).field("version").avg("version").get().toFloat() > 0
        then: "Order ASC"
            assert db.table(table).field("version").order("version", ASC).get().toListMap().first().version as float == 1.2f
        then: "Order DESC multiple columns"
            assert db.table(table).field("version").order([
                version : DESC,
                updated : DESC
            ]).get().toListMap().first().version as float == 9.1f
        then: "Limit"
            assert db.table(table).limit(5).get().toListMap().size() == 5
        then: "Limit with offset"
            assert db.table(table).limit(3, 2).get().toListMap().size() == 3
        when: "Limit with Order"
            list = db.table(table).field("version").order("name", ASC).limit(2).get().toList()
        then: "Match limit and order"
            assert list.size() == 2
            assert list.first() as float == 9.1f // Debian
        when: "Limit with Order with offset"
            list = db.table(table).field("version").order("name", ASC).limit(2, 2).get().toList()
        then: "Match limit and order"
            assert list.size() == 2
            assert list.first() as float == 7.4f // Kali
        then: "Group By"
            assert db.table(table).count().group("active").get([ active : "Y" ]).toInt() == 5
        when: "Group By with Order"
            List<Map> listOfMaps = db.table(table).fields("active").order([
                active : ASC    // N, Y   NOTE: when ordering an enum in MySQL/MariaDB is ordered based on the ordinal not the label
            ]).group("active").get().toListMap()
            listOfMaps.each {
                Log.i("%s --> %s", it.name, it.active)
            }
        then: "Match order and group"
            assert listOfMaps.size() == 2
            assert listOfMaps.first().active == "N"
            assert listOfMaps.last().active == "Y"
        then: "Updating with single ID"
            assert db.table(table).key("id").update([
                name : "Kubuntu"
            ], 1)
        then: "Be sure that it was updated"
            assert db.table(table).field("name").get(1).toString() == "Kubuntu"
        then: "Update when ID is string"
            assert db.table(table).key("name").update([
                name : "Xubuntu"
            ], "Kubuntu")
        then: "Be sure that it was updated"
            assert db.table(table).field("name").get(1).toString() == "Xubuntu"
        then: "Updating with more than one ID"
            assert db.table(table).key("id").update([
                active : "N"
            ],[8,9])
        then: "Update with map criteria"
            assert db.table(table).update([ active: "N" ], [ active: "Y" ])
        then: "Update with id, multiple"
            assert db.table(table).update([
                3 : [ version : 3.4f ],
                4 : [ version : 2.2f ]
            ])
        then: "Verify update"
            assert db.table(table).field("version").get(3).toFloat() == 3.4f
            assert db.table(table).field("version").get(4).toFloat() == 2.2f
        then: "Replace single"
            assert db.table(table).replace([id: 9 , name: "Mandrake"])
            assert db.table(table).field("name").get(9).toString() == "Mandrake"
        then: "Replace Multiple"
            assert db.table(table).replace([
                [id : 9,    name: "Mandriva", version : 3.2f, active: "Y", updated: setDate("2022-06-01") ],
                [id : 8,    name: "MXLinux",  version : 1.2f, active: "Y", updated: setDate("2021-04-01") ],
                [id : 10,   name: "Mint",     version : 7.8f, active: "Y", updated: setDate("2022-01-01") ] // Must be inserted
            ])
            assert db.table(table).field("name").get(8).toString() == "MXLinux"
            assert db.table(table).field("name").get(9).toString() == "Mandriva"
            assert db.table(table).field("name").get(10).toString() == "Mint"
        then: "Delete by Id"
            assert db.table(table).key("id").delete(10)
        then: "Count using column"
            assert db.table(table).count("name").get().toInt() == 9
        then: "Delete several IDs"
            assert db.table(table).key("id").delete(1,2,9)
            assert db.table(table).count().get().toInt() == 6
        then: "delete using Map criteria"
            if(jdbc.supportsDate) {
                assert db.table(table).delete([updated: "2021-12-01".toDate()])
            } else {
                assert db.table(table).delete([updated: "2021-12-01"])
            }
            assert db.table(table).count().get().toInt() == 5
        then: "delete using String IDs"
            assert db.table(table).key("name").delete("RedHat","Slackware")
            assert db.table(table).count().get().toInt() == 3
        then: "truncate"
            assert db.table(table).truncate()
            assert db.table(table).count().get().toInt() == 0
        cleanup:
            db?.dropAllTables()
            clean(db, table)
            db?.close()
    }

    def "Connection with Pool"() {
        setup:
            Database database = new Database(getDB())
            String table = "linux"
        when: "Create connection"
            DB db = database.connect()
        then:
            assert getTableCreate(table) || getTableCreateMulti(table) : "Missing table create"
        then: "Create table"
            assert getTableCreate(table) ? db.setSQL(getTableCreate(table)) : db.setSQL(getTableCreateMulti(table))
        then : "Be sure the table is there"
            assert db.tables.size() == 1
            assert db.tables.contains(table)
        then: "Insert first"
            assert db.table(table).insert(
                [ name : "Ubuntu",     active: "Y",    updated: "2022-08-01",     version: 3.7 ],
            )
            assert db.lastID == 1
        then: "Insert Second"
            assert db.table(table).insert(
                [ name : "Debian",     active: "Y",    updated: "2022-09-01",     version: 9.1 ],
            )
            assert db.lastID == 2
        then: "Retrieve second"
            assert db.table(table).field("name").key("id").get(2).toString() == "Debian"
        then: "Drop table"
            assert db.table(table).drop()
        then: "Confirm drop"
            assert db.tables.empty
        cleanup:
            clean(db, table)
            database.quit()
    }

    def "Test Drop all tables"() {
        when:
            DB db = getDB().connect()
            String table = "linux"
            boolean isSingleStm = getTableCreate(table) != ""
            int numTables = 5
        then: "Create table"
            (1..numTables).each {
                println "Creating ${table}${it}..."
                assert isSingleStm ? db.setSQL(getTableCreate("${table}${it}")) : db.setSQL(getTableCreateMulti("${table}${it}"))
            }
        then: "Be sure we have all tables"
            List<String> tables = db.tables
            assert tables.size() == numTables
            println tables
        then: "List tables"
            (1..numTables).each {
                assert tables.contains("${table}${it}".toString()) : "${table}${it} was not found"
            }
        when: "Drop tables"
            db.dropAllTables()
        then:
            assert db.tables.empty
        cleanup:
            clean(db, table)
            db?.close()
    }

}
