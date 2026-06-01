package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.ColumnInfo
import com.intellisrc.db.DB
import com.intellisrc.db.Data
import com.intellisrc.db.Database
import com.intellisrc.db.DatabaseConnectionException
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.TableDefinition
import com.intellisrc.log.CommonLogger
import com.intellisrc.log.PrintLogger
import com.intellisrc.net.LocalHost
import com.intellisrc.term.TableMaker
import org.slf4j.event.Level
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.time.LocalDate

import static com.intellisrc.db.Query.SortOrder.ASC
import static com.intellisrc.db.Query.SortOrder.DESC
import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.*

/**
 * @since 2022/01/20.
 */
abstract class JDBCTest extends Specification {
    static boolean ci = Config.env.get("gitlab.ci", Config.any.get("github.actions", false))

    abstract JDBC getDB()

    String engine = ""
    String charSet = "UTF8"

    @SuppressWarnings('unused')
    boolean shouldSkip() {
        JDBC jdbc = this.getDB()
        boolean skip = jdbc instanceof JDBCServer
            && (ci || ((jdbc as JDBCServer).hostname &&! LocalHost.hasOpenPort((jdbc as JDBCServer).port)))
        if(skip) {
            Log.w("Test skipped for : %s (environment not ready)", jdbc.class.simpleName)
        }
        return skip
    }

    boolean createTable(DB db, String table) {
        return db.table(table).createTable([
            new ColumnDefinition(
                name: "id",
                primaryKey: true,
                autoIncrement: true,
                type: Integer
            ),
            new ColumnDefinition(
                name: "name",
                type: String,
                length: 10,
                nullable: false,
                unique: true
            ),
            new ColumnDefinition(
                name: "version",
                type: Float,
                defaultValue: 1
            ),
            new ColumnDefinition(
                name: "active",
                type: Boolean,
                defaultValue: true
            ),
            new ColumnDefinition(
                name: "updated",
                type: LocalDate
            )
        ] as TableDefinition, engine, charSet)
    }

    boolean createTablePK(DB db, String table) {
        println "Creating table (Multiple PK): $table ..."
        return db.table(table).createTable([
            new ColumnDefinition(
                name: "uid",
                nullable: false,
                type: Short,
                primaryKey: true
            ),
            new ColumnDefinition(
                name: "gid",
                nullable: false,
                type: Short,
                primaryKey: true
            ),
            new ColumnDefinition(
                name: "name",
                type: String,
                length: 30,
                nullable: false
            )
        ] as TableDefinition, engine, charSet)
    }

    void clean(DB db, String table) {}

    def setup() {
        try {
            Log.i("Initializing Test...")
            PrintLogger printLogger = CommonLogger.default.printLogger
            printLogger.setLevel(Level.TRACE)
            DB db = getDB().connect()
            db.dropAllTables()
            db.clearCache()
            db.close()
        } catch(DatabaseConnectionException dce) {
            Log.w("Connection failed: %s", dce)
        }
    }

    @IgnoreIf({ instance.shouldSkip() })
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
            def q = {
                String s ->
                    return jdbc.getFieldForQuery(s)
            }
        then:
            assert db : "Unable to connect"
        then: "Create table"
            assert createTable(db, table) : "Table creation failed"
        then: "List tables"
            assert ! db.tables.empty : "Tables not found"
        then: "Must be open"
            assert db.opened : "Database not opened"
            assert ! db.closed : "Database was closed"
        when: "Table info"
            List<ColumnInfo> info = db.table(table).info()
            new TableMaker(info.collect { it.toMap() }).print()
        then: "Must have information"
            assert ! info.empty : "No information found"
        then: "Must have a primary key"
            assert info.find { it.primaryKey }
        then: "Primary key must be auto-increment"
            assert info.find { it.primaryKey }.autoIncrement
        then: "Primary key and name must not be nullable"
            assert info.findAll { ! it.nullable }.collect { it.name } == ["id","name"]
        then: "Primary key and name must be unique"
            assert info.findAll { it.unique }.collect { it.name } == ["id","name"]
        then: "Name must not have default value" // In case of ID, depending on database it may contain serial information
            assert info.find { it.name == "name" }.defaultValue == null
        then: "It must have default values"
            assert info.find { it.name == "version" }.defaultValue as int == 1
            assert info.find { it.name == "active" }.defaultValue as boolean
        then: "Table must exists"
            assert db.table(table).exists()
        then: "Insert first"
            assert db.table(table).insert(
                [ name : "Ubuntu",     active: true,    updated: setDate("2022-08-01"),     version: 3.7 ],
            )
        when:
            new TableMaker(db.table(table).get().toListMap()).print()
            assert db.lastID == 1
        then: "Insert Second"
            assert db.table(table).insert(
                [ name : "Debian",     active: true,    updated: setDate("2022-09-01"),     version: 9.1 ],
            )
        when:
            new TableMaker(db.table(table).get().toListMap()).print()
            assert db.lastID == 2
        then: "Count all"
            assert db.table(table).count().get().toInt() == 2
        when:
            new TableMaker(db.table(table).get().toListMap()).print()
        then: "Columns must match (getting first with automatic PK)"
            assert db.table(table).limit(1).get().toMap().keySet().size() == 5
            assert db.table(table).field("name").get(1).toString() == "Ubuntu"
        then: "Retrieve second (using manual PK)"
            assert db.table(table).field("name").key("id").get(2).toString() == "Debian"
        then: "Bulk insert"
            assert db.table(table).insert([
                [ name : "RedHat",      active: false,   updated: setDate("2009-11-01"),     version: 3.3 ],
                [ name : "Slackware",   active: false,   updated: setDate("2007-09-01"),     version: 2.1 ],
                [ name : "Suse",        active: false,   updated: setDate("2006-01-01"),     version: 3.9 ],
                [ name : "Kali",        active: true,    updated: setDate("2021-12-01"),     version: 7.4 ],
                [ name : "Fedora",      active: false,   updated: setDate("2012-03-01"),     version: 7.3 ],
                [ name : "MX Linux",    active: true,    updated: setDate("2021-04-01"),     version: 1.2 ],
                [ name : "Manjaro",     active: true,    updated: setDate("2022-06-01"),     version: 3.2 ],
            ])
        when:
            new TableMaker(db.table(table).get().toListMap()).print()
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
            list = db.table(table).field("name").where(q("version") + " > ?", 5).get().toList()
        then:
            assert list.size() == 3
            assert list.contains("Fedora".toString())
        when: "Get using where multiple args"
            list = db.table(table).field("name").where(q("version") + " > ? AND " + q("version") + " < ?", [2, 5]).get().toList()
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
            assert db.table(table).count().group("active").get([ active : true ]).toInt() == 5
        when: "Group By with Order"
            new TableMaker(db.table(table).get().toListMap()).print()
            List<Map> listOfMaps = db.table(table).fields("active").order(
                active : ASC    // false, true   NOTE: when ordering an enum is ordered based on the ordinal not the label
            ).group("active").get().toListMap()
            listOfMaps.each {
                Log.i("--> %s", it.active)
            }
        then: "Match order and group"
            assert listOfMaps.size() == 2
            //noinspection GroovyFallthrough
            switch (jdbc.booleanHandle) {
                case BOOLEAN:
                    assert listOfMaps.first().active == false
                    assert listOfMaps.last().active == true
                    break
                case NUMBER:
                    def first = listOfMaps.first().active
                    def second = listOfMaps.last().active
                    assert switch (first) {
                        case Number -> Data.parseInt(first) == 0 && Data.parseInt(second) == 1
                        case Boolean -> !first && second
                        default -> false // It should not go here
                    }
                    break
                case CHAR:
                    assert listOfMaps.first().active.toString() == "n"
                    assert listOfMaps.last().active.toString() == "y"
                    break
            }
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
                active : false
            ],[8,9])
        then: "Update with map criteria"
            assert db.table(table).update([ active: false ], [ active: true ])
        then: "Update multiple"
            assert db.table(table).update([
                [ version : 3.4f ],
                [ version : 2.2f ]
            ], [
                [ id: 3 ],
                [ id: 4 ]
            ])
        then: "Verify update"
            assert db.table(table).field("version").get(3).toFloat() == 3.4f
            assert db.table(table).field("version").get(4).toFloat() == 2.2f
        then: "Replace single"
            assert db.table(table).replace([id: 9 , name: "Mandrake"])
            assert db.table(table).field("name").get(9).toString() == "Mandrake"
        then: "Replace Multiple"
            assert db.table(table).replace([
                [id : 9,    name: "Mandriva", version : 3.2f, active: true, updated: setDate("2022-06-01") ],
                [id : 8,    name: "MXLinux",  version : 1.2f, active: true, updated: setDate("2021-04-01") ],
                [id : 10,   name: "Mint",     version : 7.8f, active: true, updated: setDate("2022-01-01") ] // Must be inserted
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
            assert db.table(table).delete([updated: setDate("2021-12-01")])
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

    @IgnoreIf({ instance.shouldSkip() })
    def "Connection with Pool"() {
        setup:
            JDBC jdbc = getDB()
            Database database = new Database(jdbc)
            String table = "linux"
            Object setDate = {
                String d ->
                    return jdbc.supportsDate ? d.toDate() : d
            }
        when: "Create connection"
            DB db = database.connect()
        then: "Create table"
            assert createTable(db, table)
        then : "Be sure the table is there"
            assert db.tables.size() == 1
            assert db.hasTable(table)
        then: "Insert first"
            assert db.table(table).insert(
                [ name : "Ubuntu", active: true, updated: setDate("2022-08-01"), version: 3.7 ],
            )
            assert db.lastID == 1
        then: "Insert Second"
            assert db.table(table).insert(
                [ name : "Debian", active: true, updated: setDate("2022-09-01"), version: 9.1 ],
            )
            assert db.lastID == 2
        then: "Retrieve second"
            assert db.table(table).field("name").key("id").get(2).toString() == "Debian"
        then: "Drop table"
            assert db.table(table).drop()
        then: "Confirm drop"
            assert db.tables.empty : "Tables not empty"
        cleanup:
            db?.dropAllTables() //In case of exceptions
            clean(db, table)
            database?.quit()
    }

    @IgnoreIf({ instance.shouldSkip() })
    def "Test Drop all tables"() {
        setup:
            JDBC jdbc = getDB()
            DB db = jdbc.connect()
            String table = "linux"
        when: "Create table"
            int numTables = 5
            (1..numTables).each {
                assert createTable(db, table + it)
            }
        then: "Be sure we have all tables"
            List<String> tables = db.tables.collect { it.toLowerCase() }
            assert tables.size() == numTables
            println tables
        then: "List tables"
            (1..numTables).each {
                assert tables.contains("${table}${it}".toString().toLowerCase()) : "${table}${it} was not found"
            }
        when: "Drop tables"
            db.dropAllTables()
        then:
            assert db.tables.empty
        cleanup:
            db?.dropAllTables() //In case of exceptions
            clean(db, table)
            db?.close()
    }

    @IgnoreIf({ instance.shouldSkip() })
    def "Test NULL"() {
        setup:
            JDBC jdbc = getDB()
            DB db = jdbc.connect()
            String table = "nullable"
            Object setDate = {
                String d ->
                    return jdbc.supportsDate ? d.toDate() : d
            }
        when: "Create table"
            assert createTable(db, table)
        then: "Insert values"
            assert db.table(table).insert([
                [ name : "RedHat",      active: false,   updated: null,     version: 3.3 ],
                [ name : "Slackware",   active: false,   updated: null,     version: 2.1 ],
                [ name : "Suse",        active: false,   updated: setDate("2006-01-01"),     version: 3.9 ],
                [ name : "Kali",        active: true,    updated: setDate("2021-12-01"),     version: 7.4 ],
                [ name : "Fedora",      active: false,   updated: setDate("2012-03-01"),     version: 7.3 ],
                [ name : "MX Linux",    active: true,    updated: setDate("2021-04-01"),     version: 1.2 ],
                [ name : "Manjaro",     active: true,    updated: setDate("2022-06-01"),     version: 3.2 ],
            ])
        then: "Select with null"
            assert db.table(table).get(updated: null).toList().size() == 2
        when: "Drop tables"
            db.dropAllTables()
        then:
            assert db.tables.empty
        cleanup:
            db?.dropAllTables() //In case of exceptions
            clean(db, table)
            db?.close()
    }

    @IgnoreIf({ instance.shouldSkip() })
    def "Multiple key support"() {
        setup:
            JDBC jdbc = getDB()
            DB db = jdbc.connect()
            String table = "mulpk"
        expect: "No tables"
            assert db.tables.empty
        when: "Create table"
            assert createTablePK(db, table)
        then : "Be sure the table is there"
            assert db.tables.size() == 1
            assert db.hasTable(table)
        then: "Insert values"
            assert db.table(table).insert([
                [ uid : 1, gid : 1, name : "User1-1" ],
                [ uid : 2, gid : 1, name : "User2-1" ],
                [ uid : 2, gid : 2, name : "User2-2" ],
                [ uid : 3, gid : 2, name : "User3-2" ],
                [ uid : 3, gid : 3, name : "User3-3" ],
                [ uid : 4, gid : 3, name : "User4-3" ],
                [ uid : 4, gid : 4, name : "User4-4" ],
                [ uid : 5, gid : 4, name : "User5-4" ],
                [ uid : 5, gid : 5, name : "User5-5" ],
                [ uid : 5, gid : 6, name : "User5-6" ],
            ])
        then: "Select using multiple keys"
            assert db.table(table).get().toListMap().size() == 10
            assert db.table(table).get([1,1]).toMap().name == "User1-1"
            assert db.table(table).keys(["uid","gid"]).get([4,3]).toMap().name == "User4-3"
        then: "Update"
            assert db.table(table).update([name: "User2-1-upd"], [2,1])
            assert db.table(table).get([2,1]).toMap().name == "User2-1-upd"
        then: "Update Using map"
            assert db.table(table).update([name: "User2-1-upd2"], [gid : 1, uid : 2])
            assert db.table(table).get([2,1]).toMap().name == "User2-1-upd2"
        when:
            new TableMaker(db.table(table).get().toListMap()).print(true)
        then: "Replace"
            assert db.table(table).replace([name: "User2-1"], [2,1])
            assert db.table(table).get([2,1]).toMap().name == "User2-1"
        when:
            new TableMaker(db.table(table).get().toListMap()).print(true)
        then: "Delete"
            assert db.table(table).delete([[1,1] , [2,1]])
            assert db.table(table).get().toListMap().size() == 8
        cleanup:
            db?.dropAllTables() //In case of exceptions
            clean(db, table)
            assert db?.tables?.empty
            db?.close()
    }
}
