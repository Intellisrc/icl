package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

/**
 * @since 2022/01/21.
 */
class OracleSQLManualTest extends OracleSQLTest {
    def "drop all"() {
        setup:
            DB db = getDB().connect()
            db.getSQL("SELECT SEQUENCE_NAME FROM USER_SEQUENCES").toList().each {
                if(! it.toString().startsWith("ISEQ\$")) {
                    db.setSQL("DROP SEQUENCE ${it}")
                }
            }
        expect:
            assert db.tables.empty || db.dropAllTables()
        cleanup:
            db.close()
    }
}
