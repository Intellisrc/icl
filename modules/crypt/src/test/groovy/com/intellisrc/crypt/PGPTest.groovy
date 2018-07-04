package com.intellisrc.crypt

import com.intellisrc.crypt.encode.PGP
import com.intellisrc.etc.Bytes
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import spock.lang.Specification


/**
 * PGP Tests
 * @since 18/07/04.
 */
class PGPTest extends Specification {
    def "General test using random key"() {
        setup:
            PGP pgp = new PGP()
            byte[] original = "This is a super secret message. Please don't read it out loud!".bytes
        expect:
            byte[] encrypted = pgp.encrypt(original)
            println "encrypted data = '" + Bytes.toHex(encrypted) + "'"
            byte[] decrypted= pgp.decrypt(encrypted)
            println "decrypted data = '"+Bytes.toString(decrypted)+"'"
            assert decrypted == original
            println "Key: " + Bytes.toString(pgp.key)
    }
    def "General test using own key"() {
        setup:
        PGP pgp = new PGP(
                key : "this key is lame".bytes,
                algorithm: PGPEncryptedDataGenerator.IDEA
        )
        byte[] original = "This is a super secret message. Please don't read it out loud!".bytes
        expect:
        byte[] encrypted = pgp.encrypt(original)
        println "encrypted data = '" + Bytes.toHex(encrypted) + "'"
        byte[] decrypted= pgp.decrypt(encrypted)
        println "decrypted data = '"+Bytes.toString(decrypted)+"'"
        assert decrypted == original
        println "Key: " + Bytes.toString(pgp.key)
    }
    def "Using armor"() {
        setup:
        PGP pgp = new PGP(armor: true)
        byte[] original = "This is a super secret message. Please don't read it out loud!".bytes
        expect:
        byte[] encrypted = pgp.encrypt(original)
        println "encrypted data = '" + Bytes.toString(encrypted) + "'"
        byte[] decrypted= pgp.decrypt(encrypted)
        println "decrypted data = '"+Bytes.toString(decrypted)+"'"
        assert decrypted == original
    }
    def "Separate objects"() {
        setup:
            char[] pwd = Bytes.toChars(Crypt.randomChars(Random.range(10,20)))
            println "PWD: $pwd"
            PGP pgp1 = new PGP(key: Bytes.fromChars(pwd))
            byte[] data = Crypt.randomBytes(1024)
            println "DATA: " + Bytes.toHex(data)
        expect:
            byte[] encrypted = pgp1.encrypt(data)
            println "Encrypted : " + Bytes.toHex(encrypted)
        when:
            PGP pgp2 = new PGP(key: Bytes.fromChars(pwd))
        then:
            byte[] decrypted = pgp2.decrypt(encrypted)
        expect:
            assert data == decrypted
    }
}