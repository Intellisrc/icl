package com.intellisrc.etc

import com.intellisrc.core.SysClock
import com.intellisrc.core.SysInfo
import spock.lang.Specification

import java.security.SecureRandom
import java.time.LocalDateTime


/**
 * @since 18/07/04.
 */
class BerkeleyDBTest extends Specification {
    BerkeleyDB db
    void setup() {
        db = new BerkeleyDB("test")
    }
    void cleanup() {
        db.destroy()
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
            db.set("some","value")
            db.set("other","val")
            db.set(randomString,"moons")
            // Close it to be sure data is written to disk
            db.close()
            db = new BerkeleyDB("test")
        expect: "Not empty"
            assert db.size == 3
            assert db.getBytesAsString("some") == "value"
        when:
            db.delete("other")
        then: "Delete successfully"
            assert db.size == 2
        when:
            db.destroy()
            db = new BerkeleyDB("test")
        then: "Destroyed completely"
            assert db.size == 0
    }
    def "Binary data"() {
        setup:
            byte[] bytes = new byte[2000]
            byte[] kbyte = new byte[20]
            new SecureRandom().nextBytes(bytes)
            db.set("strkey", bytes)
            db.setBytes(kbyte, bytes)
        expect:
            assert db.getBytes("strkey").get() == bytes
            assert db.getBytes(kbyte) == bytes
    }
    def "Storing common classes"() {
        setup:
            URI uri = "http://localhost:9999/".toURI()
            LocalDateTime now = SysClock.now
            Inet4Address ip = "10.0.0.1".toInet4Address()
            Inet6Address ip6 = "fe80::1ff:fe23:4567:890a".toInet6Address()
            File file = SysInfo.getFile("example.txt")
        when:
            db.set("uri", uri)
            db.set("time", now)
            db.set("ip", ip)
            db.set("ip6", ip6)
            db.set("file", file)
        then:
            assert db.getURI("uri").get() == uri
            assert db.getDateTime("time").get().YMDHms == now.YMDHms
            assert db.getInet4("ip").get() == ip
            assert db.getInet6("ip6").get() == ip6
            assert db.getFile("file").get() == file
    }
    def "Storing Lists"() {
        setup:
            List<String> data = ["one","two","tres"]
            db.set("spanglish", data)
            println db.get("spanglish")
        expect:
            assert db.getList("spanglish").size() == data.size()
            assert db.get("spanglish", [] as List) == data
    }
    def "Storing Maps"() {
        setup:
            Map data = [one : 1, two : 2, tres : 3]
            db.set("spanglish", data)
            println db.get("spanglish")
        expect:
            assert db.getMap("spanglish").keySet().size() == data.keySet().size()
            assert db.get("spanglish", [:]) == data

    }
}