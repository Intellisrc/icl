package com.intellisrc.net

import spock.lang.Specification

/**
 * @since 18/03/02.
 */
class EmailTest extends Specification {
    def "Email Format test"() {
        expect:
            assert Email.isValid(email, strict) == valid
        where:
            // (some from: https://gist.github.com/cjaoude/fd9910626629b53c4d25)
            email                                                   | valid     | strict
            // Valid cases
            "me@something-this.space"                               | true      | false
            "aaa+333@gmail.co.jp"                                   | true		| false
            "home@super.long.domain.with.many.subdomains.com.mx"    | true		| false
            "a@a.jp"                                                | true		| false
            "firstname.lastname@example.com"                        | true		| false
            "email@subdomain.example.com"                           | true		| false
            "firstname+lastname@example.com"                        | true		| false
            "email@123.123.123.123"                                 | true		| false
            "1234567890@example.com"                                | true		| false
            "email@example-one.com"                                 | true		| false
            "_______@example.com"                                   | true		| false
            "email@example.name"                                    | true		| false
            "email@example.museum"                                  | true		| false
            "firstname-lastname@example.com"                        | true		| false

            // Strict cases
            "áÇÈñÏç@example.com"                                    | true		| false
            "honto@本当.jp"                                          | true		| false
            "あいうえお@example.com"                                  | true      | false
            "áÇÈñÏç@example.com"                                    | false 	| true
            "honto@本当.jp"                                          | false		| true
            "あいうえお@example.com"                                  | false     | true

            // Invalid cases
            "me@som\\ething-this.space"                             | false		| false
            "aaa@333@gmail.co.jp"                                   | false		| false
            ".aaa@gmail.co.jp"                                      | false		| false
            "plainaddress"                                          | false		| false
            '#@%^%#$@#$@#.com'                                      | false		| false
            "@example.com"                                          | false		| false
            "Joe Smith <email@example.com>"                         | false		| false
            "email.example.com"                                     | false		| false
            "email@example@example.com"                             | false		| false
            ".email@example.com"                                    | false		| false
            "email.@example.com"                                    | false		| false
            "email..email@example.com"                              | false		| false
            "email@example.com (Joe Smith)"                         | false		| false
            "email@example"                                         | false		| false
            "email@-example.com"                                    | false		| false
            //"email@111.222.333.44444"                               | false		| false
            "email@example..com"                                    | false		| false
            "Abc..123@example.com"                                  | false		| false
            '(),:;<>[\\]@example.com'                               | false		| false
            //'just"not"right@example.com'                            | false		| false
            'this\\ is"really"not\\allowed@example.com'             | false		| false
    }
}