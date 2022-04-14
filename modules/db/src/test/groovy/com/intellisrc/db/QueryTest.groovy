package com.intellisrc.db

import com.intellisrc.db.jdbc.SQLite
import spock.lang.Specification

import static com.intellisrc.db.Query.Action.*

/**
 * @since 2021/07/19.
 */
class QueryTest extends Specification {
    def "Using same key in multiple where should use only one"() {
        setup :
            Query query = new Query(new SQLite(memory: true), SELECT)
            query.setTable("test").setKeys(["myid"])
                    .setWhere("myid = ?", 5)
                    .setWhere([ myid : 10 ])
                    .setWhere([1,2,3,4])
                    .setWhere(9)
        expect:
            println query.toString()
            query.args.each { println " ---> ${it}" }
            assert query.args.size() == 7
    }
    def "When passing NULL, should create IS NULL"() {
        when :
            Query query = new Query(new SQLite(memory: true), SELECT)
            query.setTable("test").setKeys(["myid"]).setWhere(null)
        then :
            println query.toString()
            assert query.toString().contains("IS NULL")
        when :
            query = new Query(new SQLite(memory: true), SELECT)
            query.setTable("test").setKeys(["myid"]).setWhere([myid : null])
        then :
            println query.toString()
            assert query.toString().contains("IS NULL")
    }
}
