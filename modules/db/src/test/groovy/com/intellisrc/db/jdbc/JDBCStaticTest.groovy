package com.intellisrc.db.jdbc

import spock.lang.Specification

/**
 * @since 2022/01/28.
 */
class JDBCStaticTest extends Specification {
    def "Get JDBC from settings for sqlite"() {
        when:
            JDBC jdbc = JDBC.fromSettings([
                type : "sqlite",
                memory : true
            ])
        then:
            assert jdbc instanceof SQLite
            assert (jdbc as SQLite).memory
    }
    def "Get JDBC from settings JDBCServer"() {
        when:
            JDBC jdbc = JDBC.fromSettings([
                type : "derby",
                user : "someuser",
                pass : "somepass",
                host : "myhostname",
                memory : true
            ])
        then:
            assert jdbc.password == "somepass"
            assert jdbc.user == "someuser"
            assert (jdbc as JDBCServer).hostname == "myhostname"
            assert jdbc instanceof Derby
            assert (jdbc as Derby).memory
    }
}
