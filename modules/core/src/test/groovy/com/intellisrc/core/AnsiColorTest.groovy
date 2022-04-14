package com.intellisrc.core

import spock.lang.Specification

import static com.intellisrc.core.AnsiColor.*

class AnsiColorTest extends Specification {
    def "Remove color should be the same"() {
        setup:
            String orig = "Hello World"
            String withColor = RED + orig + BLUE + " !!" + RESET
        expect:
            assert orig != withColor
            assert decolor(withColor) == orig + " !!"
    }

    def "Detect if string has color"() {
        expect:
            assert hasColor(str) == has
        where:
            str                 | has
            "Hello World"       | false
            ""                  | false
            RESET               | true
            BLUE + "Me"         | true
            "We are" + RED      | true
            "Some" + RESET      | true
            RED + "A" + RESET   | true
            RED                 | true
    }

    def "Get indexes of matches"() {
        setup:
            List<Map> list = []
            String colored = BLUE + "This is " + GREEN + "very " + RED + "important" + RESET
            String noColor = colored
        when:
            def match = (colored =~ colorRegex)
            while (match.find()) {
                //println("Start index: " + match.start())
                //println(" End index: " + match.end())
                list << [ c : match.group(), i : match.start() ]
                noColor = noColor.replace(match.group(), "")
            }
            println list
            String manual = "This is very important"
        then:
            assert manual == noColor
        when:
            list.each {
                Map map ->
                    manual = manual.insertAt((map.i as int), map.c.toString())
            }
            println manual
        then:
            assert manual == colored
    }
}
