package com.intellisrc.db

import org.apache.commons.lang.RandomStringUtils
import spock.lang.Specification


/**
 * @since 18/07/04.
 */
class BerkeleyDBTest extends Specification {
    def "General test"() {
        setup:
            int randomStringLength = 32
            String charset = (('a'..'z') + ('A'..'Z') + ('0'..'9')).join()
            String randomString = RandomStringUtils.random(randomStringLength, charset.toCharArray())
            def db = new BerkeleyDB("test")
            db.add("some","value")
            db.add("other","val")
            db.add(randomString,"moons")
            db.close()
            db = new BerkeleyDB("test")
        expect: "Not empty"
            assert db.size == 3
            assert db.getStr("some") == "value"
        when:
            db.delete("other")
        then: "Delete successfully"
            assert db.size == 2
        when:
            db.destroy()
            db = new BerkeleyDB("test")
        then: "Destroyed completely"
            assert db.size == 0
            db.destroy()
        cleanup:
            File dir = new File(".berkeley")
            if(dir.exists()) {
                dir.deleteDir()
                println "Directory removed"
            }
    }
}