package jp.sharelock.etc

import spock.lang.Specification

import static jp.sharelock.etc.Bytes.*
/**
 * @since 17/04/11.
 */
class BytesTest extends Specification  {
    def "Bytes and String"() {
        setup:
            String str = "Hello there みなんさん!"
        when:
            byte[] bytes = fromString(str)
        then:
            //println "Bytes: "+bytes
            assert bytes.length > 0
            assert toString(bytes) == str
    }
    def "Bytes and char"() {
        setup:
            String str = "This is これ！"
        when:
            char[] chars = str.toCharArray()
            byte[] bytes = fromString(str)
            byte[] newbytes = fromChars(chars)
        then: "Bytes are equal"
            assert newbytes == bytes
        then: "Strings are equal"
            assert new String(toChars(bytes)) == str
    }
    def "Bytes and HEX"() {
        setup:
            String str = "分かった？"
        when:
            byte[] bytes = fromString(str)
            String hex = toHex(bytes)
        then:
            println "HEX: "+hex
            assert bytes
            assert fromHex(hex) == bytes
            assert toString(bytes) == str
    }
}
