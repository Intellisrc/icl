package com.intellisrc.db.auto

import com.intellisrc.core.SysClock
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.jdbc.*

import java.time.LocalDate

/**
 * @since 2023/05/30.
 */
class ViewTest extends AutoTest {
    static class TestModel extends Model {
        @Column
        id
        @Column
        String name
        @Column
        int age
        @Column
        LocalDate added
    }
    static class TestView extends View<TestModel> {
        TestView(String name, Database database, String sql) {
            super(name, database, sql)
        }
    }
    def "Should create view"() {
        setup:
            Database database = new Database(jdbc)
            Users users = new Users(database)
            Aliases aliases = new Aliases(database)
            aliases.clear()
            users.clear()
            String sql = ""
            //noinspection GroovyFallthrough
            switch (jdbc) {
                case Derby:
                case SQLite:
                case MySQL:
                case MariaDB:
                case PostgreSQL:
                    sql = """CREATE VIEW test_view AS SELECT u.id, u.name, u.age, a.added 
                             FROM users u LEFT JOIN aliases a ON(u.id = a.user_id)"""
                    break
            }
            assert sql : "SQL not specified"
            TestView view = new TestView("test_view", database, sql)
        when:
            User u = new User(
                name : "Benjamin",
                age  : 99
            )
        then:
            assert users.insert(u)
        when:
            Alias alias = new Alias(
                user : u,
                name : "Ben",
                added: SysClock.now.toLocalDate()
            )
        then:
            assert aliases.insert(alias)
        then:
            assert view.fields.any { it.name == "name" }
            assert view.fields.any { it.name == "added" }
            assert view.getAll().first().age == u.age
            assert view.getAll().first().added == alias.added
        cleanup:
             view.drop()
        where:
            jdbc << testable
    }
}
