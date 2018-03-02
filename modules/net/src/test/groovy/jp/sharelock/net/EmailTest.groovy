package jp.sharelock.net

import spock.lang.Specification


/**
 * @since 18/03/02.
 */
class EmailTest extends Specification {
    def "Email Format test"() {
        expect:
            ["me@something-this.space","aaa+333@gmail.co.jp","home@super.long.domain.with.many.subdomains.com.mx"].each {
                assert new Email(it)
            }
        when:
            new Email("me@som\\ething-this.space")
        then:
            thrown Email.EmailMalformedException
        when:
            new Email("aaa@333@gmail.co.jp")
        then:
            thrown Email.EmailMalformedException

    }
}