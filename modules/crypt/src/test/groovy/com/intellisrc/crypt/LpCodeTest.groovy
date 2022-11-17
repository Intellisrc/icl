package com.intellisrc.crypt

import com.intellisrc.crypt.encode.LpCode
import spock.lang.Specification

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
            assert true
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
            println encoded
            char[] decoded = lpCode.decode(encoded)
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
    def "Replace Chars"() {
        setup:
            char[] s = "abcdefg".toCharArray()
        expect:
            assert replaceChars(
                s,
                "abc".toCharArray(),
                "123".toCharArray()
            ) == "123defg".toCharArray()
            assert replaceChars(
                s,
                "acdg".toCharArray(),
                "WXYZ".toCharArray()
            ) == "WbXYefZ".toCharArray()
    }
}
