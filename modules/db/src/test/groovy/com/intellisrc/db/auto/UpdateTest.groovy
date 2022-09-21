package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.ModelMeta
import spock.lang.Unroll

import static com.intellisrc.db.Query.SortOrder.DESC

class UpdateTest extends AutoTest {

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
    static class UsersV2 extends Table<UserV2>{
        boolean execFired = false
        UsersV2(String name, Database database) { super(name, database) }

        @Override
        boolean execOnUpdate(DB table, int prevVersion, int currVersion) {
            execFired = true
            return false
        }
    }

    @Unroll
    def "Simple Update without data"() {
        setup:
            DB.enableCache = false
            String tableName = "users"
            Database database = new Database(jdbc)
            Users users = new Users(tableName, database)
        when:
            UsersV2 users2 = new UsersV2(tableName, database)
            users2.updateTable() // Update it manually
            DB.enableCache = false //updateTable re-enable it (just in case)
            UserV2 u = new UserV2(
                name : "Benjamin",
                age : 22,
                webpage: "http://example.com".toURL()
            )
            int uid = users2.insert(u)
        then:
            assert users2.count() > 0
            Log.i("ID was: %d", uid)
            assert uid == 1
        when:
            int uid2 = users2.insert(new UserV2(
                name: "Clara",
                age: 28,
                webpage: "http://example.net".toURL()
            ))
        then:
            assert uid2 == 2
        cleanup:
            Log.i("Cleaning database...")
            users?.drop()
            users?.quit()
        where:
            jdbc << testable
    }

    @Unroll
    def "Update with data"() {
        setup:
            DB.enableCache = false
            String tableName = "users"
            Database database = new Database(jdbc)
            Users users = new Users(tableName, database)
        when:
            int rows = 10
            List<User> userList = []
            (1..rows).each {
                userList << new User(
                    name: "User${it}",
                    age : it + 20,
                    ip4 : "10.0.0.${it}".toInet4Address()
                )
            }
            users.insert(userList)
        then:
            assert users.count() == rows : "Number of rows failed before updating"
            assert users.getAll(5).size() == 5 : "Limit failed"
            assert users.getAll("age", DESC).first().uniqueId == rows : "Limit failed"
            assert users.getAll("age", DESC, 5).last().uniqueId == rows - 5 + 1 : "Sort with limit failed"
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
            assert users.count() == rows : "Number of rows failed after updating"
        when:
            UserV2 u = new UserV2(
                name : "Benjamin",
                age : 22,
                webpage: "http://example.com".toURL()
            )
            int uid = users2.insert(u)
        then:
            assert uid == rows + 1
            assert users.table.field("webpage").get(uid).toString().startsWith("http")
        cleanup:
            Log.i("Cleaning database...")
            users?.drop()
            users?.quit()
        where:
            jdbc << testable
    }

}
