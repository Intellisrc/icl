package com.intellisrc.crypt

import com.intellisrc.core.SysClock
import com.intellisrc.crypt.encode.LpCode
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static com.intellisrc.crypt.encode.LpCode.*

class LpCodeTest extends Specification {

    def "Test all blocks"() {
        setup:
            char[] toEncode = "hello".toCharArray()
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

    def "Test encoding/decoding with mistaken input"() {
        setup:
            char[] toEncode = "!Hello World This Must NOT Work 1234".toCharArray()
        expect:
            LpCode lpCode = new LpCode(ANUM, BRAILLE)
            char[] encoded = lpCode.encode(toEncode)
            println "ENC = " + encoded
            char[] decoded = lpCode.decode(encoded)
            println "DEC = " + decoded
            assert encoded != toEncode
            assert toEncode != decoded
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

    def "Encoding with a different seed should return a different value"() {
        setup:
            char[] toEncode = "HelloWorldThisMustWork".toCharArray()
            long seed1 = new Random().nextLong()
            long seed2 = new Random().nextLong()
            println "Using seed1: " + seed1
            println "Using seed2: " + seed2
            LpCode lpCode1 = new LpCode(ALPHA, LATIN, seed1)
            LpCode lpCode2 = new LpCode(ALPHA, LATIN, seed2)
            char[] encoded1 = lpCode1.encode(toEncode)
            char[] encoded2 = lpCode2.encode(toEncode)
        expect:
            assert encoded1 != encoded2
    }

    def "Storing and restoring number"() {
        setup:
            char[] s = "asoasdiasdqwempoiapsoidas".toCharArray()
            BigInteger num = toNumber(s, LOWERCASE, 999)
            println "Number: " + num
        expect:
            assert toCharArray(num, LOWERCASE, 999) == s
    }

    def "By chunks"() {
        setup:
            int glue = getCodePoints("|").first() //BRAILLE.first() //You can use a character included inside the OUTPUT charset
            char[] s = ("""The Basic Latin or C0 Controls and Basic Latin Unicode block is the first block of the Unicode standard, and the only block which is encoded in one byte in UTF-8. The block contains all the letters and control codes of the ASCII encoding. It ranges from U+0000 to U+007F, contains 128 characters and includes the C0 controls, ASCII punctuation and symbols, ASCII digits, both the uppercase and lowercase of the English alphabet and a control character.""").toCharArray()
            println String.format("ORIGINAL (len: %d): %s", s.length, s.toString())
            LpCode lpCode = new LpCode(BASIC, BRAILLE)
            LocalDateTime start = SysClock.now
            println "ENCODED ALL: " + lpCode.encode(s).toString()
            println "LAPSED: " + ChronoUnit.MILLIS.between(start, SysClock.now) //There is an initialization penalty
        expect:
            [10, 25, 50, 100, 200, 500].each {
                start = SysClock.now
                String chunks = lpCode.encodeByChunks(s, glue, it).toString()
                println "BY CHUNKS [$it]: " + chunks
                println "LAPSED: " + ChronoUnit.MILLIS.between(start, SysClock.now)
                String decoded = lpCode.decodeByChunks(chunks.toCharArray(), glue).toString()
                println "DECODED: " + decoded
                assert decoded == s.toString()
            }
        when:
            start = SysClock.now
            println "ENCODED ALL: " + lpCode.encode(s).toString()
            println "LAPSED: " + ChronoUnit.MILLIS.between(start, SysClock.now)
        then:
            assert true
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

    static final String origNum = "0123456789"
    static final String origLow = "something"
    static final List<Integer> custom1 = getCodePoints("OlZEASGTBg") //L33t for numbers
    static final List<Integer> custom2 = getCodePoints("48©Δ3ғ6ԨїԏϏ1мИ0ϼ9Я57μύώ×Ч2") //L33t for alphabet
    def "Translate must work"() {
        expect:
            assert translate(value, from, to) == result
        where:
            value   | from      | to                | result
            origNum | NUMBERS   | CIRCLE_NUMS       | "⓪①②③④⑤⑥⑦⑧⑨"
            origNum | NUMBERS   | CIRCLE_NEG_NUMS   | "⓿❶❷❸❹❺❻❼❽❾"
            origNum | NUMBERS   | COMMA_NUM         | "🄁🄂🄃🄄🄅🄆🄇🄈🄉🄊"
            origNum | NUMBERS   | DIGITS            | "🯰🯱🯲🯳🯴🯵🯶🯷🯸🯹"
            origNum | NUMBERS   | PAREN_NUMS        | "⑴⑵⑶⑷⑸⑹⑺⑻⑼⑽"
            origNum | NUMBERS   | FW_NUM            | "０１２３４５６７８９"
            origNum | NUMBERS   | custom1           | "OlZEASGTBg"
            origNum | NUMBERS   | LOWERCASE         | "abcdefghij"
            origLow | LOWERCASE | UPPERCASE         | "SOMETHING"
            origLow | LOWERCASE | CIRCLE_UP         | "ⓈⓄⓂⒺⓉⒽⒾⓃⒼ"
            origLow | LOWERCASE | CIRCLE_LOW        | "ⓢⓞⓜⓔⓣⓗⓘⓝⓖ"
            origLow | LOWERCASE | SQUARE_UP         | "🅂🄾🄼🄴🅃🄷🄸🄽🄶"
            origLow | LOWERCASE | SQUARE_UP_NEG     | "🆂🅾🅼🅴🆃🅷🅸🅽🅶"
            origLow | LOWERCASE | PAREN_LOW         | "⒮⒪⒨⒠⒯⒣⒤⒩⒢"
            origLow | LOWERCASE | PAREN_UP          | "🄢🄞🄜🄔🄣🄗🄘🄝🄖"
            origLow | LOWERCASE | FW_LOW            | "ｓｏｍｅｔｈｉｎｇ"
            origLow | LOWERCASE | FW_UP             | "ＳＯＭＥＴＨＩＮＧ"
            origLow | LOWERCASE | custom2           | "50м37ԨїИ6"
            origLow | LOWERCASE | NUMBERS           | "som4t78n6"
            origLow | LOWERCASE | CIRCLE_NUMS       | "⑱⑭⑫④⑲⑦⑧⑬⑥"

    }
}
