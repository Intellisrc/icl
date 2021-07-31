package com.intellisrc.crypt

import spock.lang.Specification
import static com.intellisrc.etc.Bytes.*

/**
 * @since 17/04/11.
 */
class CryptTest extends Specification {
    def "randomAlpha"() {
        setup:
            Crypt crypt = new Crypt()
            (0..10).each {
                byte[] rand = crypt.randomChars(50)
                println toString(rand)
            }
            byte[] rand1 = crypt.randomChars(50)
            byte[] rand2 = crypt.randomChars(50)
            byte[] rand3 = crypt.randomChars(50)
        when:
            String str1 = toString(rand1)
            String str2 = toString(rand2)
            String str3 = toString(rand3)
        then:
            assert str1 != str2 && str2 != str3 && str1 != str3
    }

    def "randomPassword"() {
        setup:
            Crypt crypt = new Crypt()
            (0..10).each {
                byte[] rand = crypt.randomChars(50, Crypt.Complexity.MEDIUM)
                println toString(rand)
            }
            byte[] rand1 = crypt.randomChars(50, Crypt.Complexity.MEDIUM)
            byte[] rand2 = crypt.randomChars(50, Crypt.Complexity.MEDIUM)
            byte[] rand3 = crypt.randomChars(50, Crypt.Complexity.MEDIUM)
        when:
            String str1 = toString(rand1)
            String str2 = toString(rand2)
            String str3 = toString(rand3)
        then:
            assert str1 != str2 && str2 != str3 && str1 != str3
    }

    def "randomAscii"() {
        setup:
            Crypt crypt = new Crypt()
            (0..10).each {
                byte[] rand = crypt.randomChars(50, Crypt.Complexity.HIGH)
                println toString(rand)
            }
            byte[] rand1 = crypt.randomChars(50, Crypt.Complexity.HIGH)
            byte[] rand2 = crypt.randomChars(50, Crypt.Complexity.HIGH)
            byte[] rand3 = crypt.randomChars(50, Crypt.Complexity.HIGH)
        when:
            String str1 = toString(rand1)
            String str2 = toString(rand2)
            String str3 = toString(rand3)
        then:
            assert str1 != str2 && str2 != str3 && str1 != str3
    }
}
