package com.intellisrc.db

import spock.lang.Specification

/**
 * @since 10/28/17.
 */
class JDBCConnectorTest extends Specification {
    def "Testing URL parse"() {
        setup:
            def jdbc = new JDBCConnector("mysql://user1:pass1@localhost1:1234/mydb")
        expect:
            def url = jdbc.getJDBCStr()
            assert url
            println url
    }
}
