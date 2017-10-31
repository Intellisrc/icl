package jp.sharelock.crypt

import jp.sharelock.crypt.hash.PasswordHash
import jp.sharelock.etc.Bytes
import spock.lang.Specification

/**
 * @since 17/04/12.
 */
class PasswordHashTest extends Specification {
    def "Simple case"() {
        when:
            def ph = new PasswordHash()
            String hash = ph.BCrypt()
            char[] pass = ph.password
            byte[] salt = ph.salt
        then:
            assert !hash.isEmpty()
            assert hash.startsWith('$')
            assert pass.length > 0
            assert salt.length > 0
        when:
            println "HASH $hash"
            println "PASS $pass"
            println "SALT " + Bytes.toString(salt)
        then:
            assert ph.verify(hash)
            assert hash == ph.BCrypt() //Be sure that repetition does not affect
            ph.clear()
    }
    def "Using NO-HEADER"() {
        when:
            def ph = new PasswordHash()
            String hash = ph.BCryptNoHeader()
            char[] pass = ph.password
            byte[] salt = ph.salt
        then:
            assert !hash.isEmpty()
            assert !hash.startsWith('$')
            assert pass.length > 0
            assert salt.length > 0
        when:
            println "HASH $hash"
            println "PASS $pass"
            println "SALT " + Bytes.toString(salt)
        then:
            assert ph.verify(hash)
            assert hash == ph.BCryptNoHeader() //Be sure that repetition does not affect
    }
    def "Provided password , default algorithm"() {
        when:
            char[] pwd = "Hello Earth!".toCharArray()
            def ph = new PasswordHash(password: pwd)
            String hash = ph.hash()
            char[] pass = ph.password
            byte[] salt = ph.salt
        then:
            assert !hash.isEmpty()
            assert pass.length > 0
            assert salt.length > 0
        when:
            println "HASH $hash"
            println "PASS $pass"
            println "SALT " + Bytes.toString(salt)
        then:
            assert ph.verify(hash)
            assert hash == ph.BCrypt() //Be sure that repetition does not affect

    }
    def "Provided password and key<salt>"() {
        when:
            char[] pwd = "Hello Earth!".toCharArray()
            byte[] saltin = Crypt.randomBytes(16)
            def ph = new PasswordHash(password: pwd, key: saltin)
            String hash = ph.hash()
            char[] pass = ph.password
            byte[] saltout = ph.salt
        then:
            assert !hash.isEmpty()
            assert pass.length > 0
            assert saltout.length > 0
        when:
            println "HASH $hash"
            println "PASS $pass"
            println "SALT IN  " + Bytes.toHex(saltin)
            println "SALT OUT " + Bytes.toString(saltout)
        then:
            assert ph.verify(hash)
            assert saltin == saltout
            assert hash == ph.BCrypt() //Be sure that repetition does not affect
    }
    def "Provided password and FIXED key<salt>"() {
        when:
            char[] pwd = "Hello Earth!".toCharArray()
            byte[] saltin = Bytes.fromHex("2F44C70AF4A99D4E85277D030E186A26")
            def ph = new PasswordHash(password: pwd, key: saltin)
            String hash = ph.hash()
            char[] pass = ph.password
            byte[] saltout = ph.salt
        then:
            assert !hash.isEmpty()
            assert pass.length > 0
            assert saltout.length > 0
        when:
            println "HASH $hash"
            println "PASS $pass"
            println "SALT IN  " + Bytes.toHex(saltin)
            println "SALT OUT " + Bytes.toString(saltout)
        then:
            assert ph.verify(hash)
            assert hash == '$2y$10$JyRFAtQnlS4DH1yBBffoHes/PbN5NHFU4FLVQkvHmY1gqxzGkY.hy'
            assert saltin == saltout
            assert hash == ph.BCrypt() //Be sure that repetition does not affect
    }
    def "SCrypt simple"() {
        when:
            char[] pwd = "Hello Earth!".toCharArray()
            def ph = new PasswordHash(password: pwd)
            def hash = ph.SCrypt()
            def salt = ph.salt
        then:
            assert hash.length() > 0
            println "HASH using random salt: "+hash
            println "SALT " + Bytes.toString(salt)
            assert hash == ph.SCrypt() //Be sure that repetition does not affect
    }

    def "SCrypt with salt"() {
        when:
            char[] pwd = "Hello Earth!".toCharArray()
            byte[] saltin = Crypt.randomBytes(16)
            def ph = new PasswordHash(password: pwd, salt: saltin, cost: 12)
            def hash = ph.SCrypt()
            def saltout = ph.salt
        then:
            assert hash.length() > 0
            println "SALT IN  " + Bytes.toString(saltin)
            println "SALT OUT " + Bytes.toString(saltout)
            assert saltin == saltout
            println "HASH using fixed salt: "+hash
            assert hash == ph.SCrypt() //Be sure that repetition does not affect
    }

    def "SCrypt with hard-coded salt"() {
        when:
        char[] pwd = "Hello Earth!".toCharArray()
        byte[] saltin = Bytes.fromString("VWSYob8uOI1rxZKx")
        def ph = new PasswordHash(password: pwd, salt: saltin, cost: 12)
        def hash = ph.SCrypt()
        def saltout = ph.salt
        then:
        assert hash.length() > 0
        println "SALT IN  " + Bytes.toString(saltin)
        println "SALT OUT " + Bytes.toString(saltout)
        assert saltin == saltout
        println "HASH using fixed salt: "+hash
        assert hash == '$s0$14$8$1$DF7A3D360498325C72C983868BD378FF'
        assert hash == ph.SCrypt() //Be sure that repetition does not affect
    }

    def "SCrypt incorrect key size should generate a new one"() {
        when:
        char[] pwd = "Hello Earth!".toCharArray()
        byte[] saltin = Crypt.randomChars(20) //Key should be 16
        def ph = new PasswordHash(password: pwd, salt: saltin, cost: 12)
        def hash = ph.SCrypt()
        def saltout = ph.salt
        then:
        assert hash.length() > 0
        println "SALT IN  " + Bytes.toString(saltin)
        println "SALT OUT " + Bytes.toString(saltout)
        assert saltin != saltout
    }
}
