package com.intellisrc.db

import spock.lang.Specification
/**
 * @since 2021/07/19.
 */
class QueryTest extends Specification {
    def "Using same key in multiple where should use only one"() {
        setup :
            Query query = new Query()
            query.setTable("test").setKeys(["myid"])
                    .setWhere("myid = ?", 5)
                    .setWhere([ myid : 10 ])
                    .setWhere([1,2,3,4])
                    .setWhere(9)
                    .setType(DB.DBType.MYSQL).setAction(Query.Action.SELECT)
        expect:
            println query.toString()
            query.argsList.each { println " ---> ${it}" }
            assert query.argsList.size() == 1
    }
}
