package com.intellisrc.tools

import groovy.transform.CompileStatic
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import org.jline.utils.AttributedString

/**
 * @since 18/07/13.
 */
@CompileStatic
class MatchAnyCompleter implements Completer {
    protected final List<Candidate> candidateList = []

    MatchAnyCompleter(List<String> list) {
        assert list
        list.each {
            candidateList << new Candidate(AttributedString.stripAnsi(it), it, null, null, null, null, true)
        }
    }

    @Override
    void complete(final LineReader reader, final ParsedLine commandLine, final List<Candidate> selected) {
        assert commandLine != null
        assert selected != null
        selected.addAll(candidateList.findAll {
            Candidate candidate ->
                commandLine.words().stream().allMatch {
                    String keyword ->
                        candidate.value().contains(keyword)
                }
        })
    }
}
