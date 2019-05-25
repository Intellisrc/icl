package com.intellisrc.term

import org.jline.reader.Candidate
import org.jline.reader.ParsedLine
import spock.lang.Specification


/**
 * @since 18/07/13.
 */
class MatchAnyCompleterTest extends Specification {
    def "Testing matches"() {
        setup:
            def mac = new MatchAnyCompleter([
                    "somevalue",
                    "welcome_trashcan",
                    "pecan_seaweed",
                    "yeswecan",
                    "canwest",
                    "nomatchhere"
            ])
            def cmdLine = new ParsedLine() {
                @Override
                String word() {
                    return null
                }

                @Override
                int wordCursor() {
                    return 0
                }

                @Override
                int wordIndex() {
                    return 0
                }

                @Override
                List<String> words() {
                    return ["we","can"]
                }

                @Override
                String line() {
                    return null
                }

                @Override
                int cursor() {
                    return 0
                }
            }
            List<Candidate> selected = []
            mac.complete(null, cmdLine, selected)
        expect:
            selected.each {
                println it.value()
            }
            assert selected.size() == 4
    }
}