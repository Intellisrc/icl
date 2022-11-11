package com.intellisrc.crypt

import com.intellisrc.crypt.encode.LpCode
import spock.lang.Specification

import static com.intellisrc.crypt.encode.LpCode.Charset.*
import static com.intellisrc.crypt.encode.LpCode.replaceChars
import static com.intellisrc.crypt.encode.LpCode.toStr

//https://en.wikipedia.org/wiki/Unicode_block
class LpCodeTest extends Specification {
    def "Test encoding/decoding"() {
        setup:
            char[] toEncode = "HelloWorldThisMustWork".toCharArray()
        expect:
            LpCode lpCode = new LpCode(ALPHA, NUM)
            char[] encoded = lpCode.encode(toEncode)
            println "ENC = " + encoded
            char[] decoded = lpCode.decode(encoded)
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
            LpCode lpCode1 = new LpCode(ALPHA, ASCII, seed)
            LpCode lpCode2 = new LpCode(ALPHA, ASCII, seed)
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
    def "Test fix"() {
        setup:
            char[] s = "H[ell^`\\o".toCharArray()
            LpCode lp = new LpCode(ALPHA, HASH)
        when:
            char[] st1 = lp.fixStr(s, true, true)
        then:
            assert st1 == "HAellBCDo".toCharArray()
            assert lp.fixStr(st1, true, false) == s
        when:
            char[] st2 = lp.fixStr(st1, false, true)
        then:
            assert st2 == "HAe77BCD8".toCharArray()
            assert lp.fixStr(st2, false, false) == st1
    }

    def "Test toStr"() {
        setup:
            String s = toStr(12345 as BigInteger, getLM(HIRA))
        expect:
            println s
            assert s == "ぁめぽ"
    }
}
