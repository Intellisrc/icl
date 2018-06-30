package com.intellisrc.crypt

import com.intellisrc.crypt.encode.AES
import com.intellisrc.etc.Bytes
import spock.lang.Specification

/**
 * @since 17/04/18.
 */
class AESTest extends Specification {
    def "AES Simple"() {
        given:
            AES aes = new AES()
            def secret = "Hello Earth!"
        when:
            def encodedBytes = aes.encrypt(secret.getBytes())
            def encoded = Bytes.toHex(encodedBytes)
            def decoded = aes.decrypt(Bytes.fromHex(encoded))
        then:
            println "KEY : "+Bytes.toHex(aes.key)
            println "ENCODED : "+encoded+" ("+Bytes.toString(encodedBytes)+")"
            assert !encoded.isEmpty()
            assert Bytes.toString(decoded) == secret
    }
    def "AES Decrypt Message"() {
        given:
            def key = "584F5A53487853313550577939666B37665973623145516D37327655347A4F43"
            AES aes = new AES(key : Bytes.fromHex(key))
        when:
            def encoded = "36BE9734BF441E4260D86A2833FAFD7F071B68B18BD36E26BA44CBBC334922A5"
            def decoded = aes.decrypt(Bytes.fromHex(encoded))
        then:
            println "DECODED : "+Bytes.toString(decoded)
            assert decoded.length > 0
            assert Bytes.toString(decoded) == "Hello Earth!"
    }
    def "AES Multiple"() {
        given:
            AES aes = new AES()
            def secret1 = "Hello Earth!"
            def secret2 = "Hello Earth!"
        when:
            def encoded1 = aes.encrypt(secret1.getBytes())
            def decoded1 = aes.decrypt(encoded1)
            def encoded2 = aes.encrypt(secret2.getBytes())
            def decoded2 = aes.decrypt(encoded2)
        then:
            assert encoded1.length > 0
            assert encoded2.length > 0
            assert encoded1 != encoded2
            println "ENCODED1 : "+Bytes.toHex(encoded1)
            println "ENCODED2 : "+Bytes.toHex(encoded2)
            assert Bytes.toString(decoded1) == secret1
            assert Bytes.toString(decoded2) == secret2
            println "DECODED : "+Bytes.toString(decoded1)
    }
}
