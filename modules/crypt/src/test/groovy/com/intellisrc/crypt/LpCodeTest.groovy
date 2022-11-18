package com.intellisrc.crypt

import com.intellisrc.crypt.encode.LpCode
import spock.lang.Specification
import spock.lang.Unroll

import static com.intellisrc.crypt.encode.LpCode.*

class LpCodeTest extends Specification {

    def "Print samples"() {
        setup:
            char[] toEncode = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz".toCharArray()
        expect:
            println "Original: [$toEncode] (${toEncode.length})"
            charsets.each {
                LpCode lpCode = new LpCode(LOWERCASE, it.value, 9999)
                String encoded = lpCode.encode(toEncode)
                println it.key + " (" + it.value.length + ") [" + encoded + "] (" + encoded.length() + ")"
                assert toEncode == lpCode.decode(encoded.toCharArray())
            }
    }

    def "Test encoding/decoding"() {
        setup:
            char[] toEncode = "HelloWorldThisMustWork".toCharArray()
        expect:
            LpCode lpCode = new LpCode(ALPHA, VISIBLE)
            char[] encoded = lpCode.encode(toEncode)
            println "ENC = " + encoded
            char[] decoded = lpCode.decode(encoded)
            println "DEC = " + decoded
            assert encoded != toEncode
            assert toEncode == decoded
    }

    def "Test encoding/decoding with seed"() {
        setup:
            char[] toEncode = "HelloWorld".toCharArray()
            long seed = new Random().nextLong()
            println "Using seed: " + seed
        expect:
            LpCode lpCode = new LpCode(ALPHA, HANZU, seed)
            char[] encoded = lpCode.encode(toEncode)
            println "Encoded: " + encoded
            char[] decoded = lpCode.decode(encoded)
            println "Decoded: " + decoded.toString()
            assert encoded != toEncode
            assert toEncode == decoded
    }

    def "Encoding with the same seed should return same value"() {
        setup:
            char[] toEncode = "HelloWorldThisMustWork".toCharArray()
            long seed = new Random().nextLong()
            println "Using seed: " + seed
            LpCode lpCode1 = new LpCode(ALPHA, LATIN, seed)
            LpCode lpCode2 = new LpCode(ALPHA, LATIN, seed)
            char[] encoded1 = lpCode1.encode(toEncode)
            char[] encoded2 = lpCode2.encode(toEncode)
        expect:
            assert encoded1 == encoded2
    }

    def "Test ord"() {
        setup:
            char a = "a"
            char b = "あ"
        expect:
            assert (a as int) == 97
            assert (b as int) == 12354
    }
    def "Test chr"() {
        setup:
            int a = 97
            int b = 12354
        expect:
            assert (a as char).toString() == "a"
            assert (b as char).toString() == "あ"
    }
    def "Storing and restoring number"() {
        setup:
            char[] s = "asoasdiasdqwempoiapsoidas".toCharArray()
            BigInteger num = toNumber(s, LOWERCASE, 999)
            println "Number: " + num
        expect:
            assert toCharArray(num, LOWERCASE, 999) == s
    }

    @Unroll
    def "From numbers to string"() {
        setup:
            BigInteger num = 128344192321353453454
            char[] str = toCharArray(num, charset, seed)
            println "String: " + str.toString()
        expect:
            assert toString(num, charset, seed) == str.toString()
            assert toNumber(str, charset, seed) == num
        where:
            charset | seed
            NUMBERS | 0
            NUMBERS | 999
            DIGITS  | 0
            DIGITS  | 999
    }

    def "SMP Test"() {
        setup:
            LpCode lpCode = new LpCode(LOWERCASE, EGYPTIAN)
            String orig = "helloworldthisisatest"
            String enc = lpCode.encode(orig.toCharArray())
            println enc
            String dec = lpCode.decode(enc.toCharArray())
            println dec
        expect :
            assert orig == dec
    }
}
