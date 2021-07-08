package com.intellisrc.db

import spock.lang.Specification

import java.security.SecureRandom


/**
 * @since 18/07/04.
 */
class BerkeleyDBTest extends Specification {
    BerkeleyDB db
    void setup() {
        db = new BerkeleyDB("test")
    }
    void cleanup() {
        File dir = new File(".berkeley")
        assert dir.exists()
        dir.deleteDir()
        println "Directory removed"
    }
    def "General test"() {
        setup:
            int randomStringLength = 32
            List<String> charset = (('a'..'z') + ('A'..'Z') + ('0'..'9'))
            String randomString = (0..randomStringLength).collect {charset.random(1).first() }.join("")
            println "Random String: $randomString"
            db.add("some","value")
            db.add("other","val")
            db.add(randomString,"moons")
            // Close it to be sure data is written to disk
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
    }
    def "Binary data"() {
        setup:
            byte[] bytes = new byte[2000]
            byte[] kbyte = new byte[20]
            new SecureRandom().nextBytes(bytes)
            db.add("strkey", bytes)
            db.add(kbyte, bytes)
        expect:
            assert db.get("strkey") == bytes
            assert db.get(kbyte) == bytes
        cleanup:
            db.destroy()
    }

}