package com.intellisrc.web

import com.intellisrc.web.service.Range
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

/**
 * Unit tests for {@link Range} byte-range parsing (HTTP {@code Range: bytes=...}).
 *
 * @since 2.10.5
 */
@Subject(Range)
class RangeTest extends Specification {

    static final long TOTAL = 1000L

    def "absent / empty / non-bytes header returns null (serve full 200)"() {
        expect:
            Range.parse(null, TOTAL) == null
            Range.parse("", TOTAL) == null
            Range.parse("   ", TOTAL) == null
            Range.parse("items=0-99", TOTAL) == null     // not a byte range
    }

    def "multi-range returns null (caller falls back to full 200)"() {
        expect:
            Range.parse("bytes=0-99,200-299", TOTAL) == null
    }

    @Unroll
    def "valid range '#header' -> start=#start end=#end"() {
        when:
            Range r = Range.parse(header, TOTAL)
        then:
            r != null
            r.isValid()
            r.total == TOTAL
            r.start == start
            r.end == end
            r.length == end - start + 1
            r.contentRangeHeader() == "bytes ${start}-${end}/${TOTAL}".toString()
        where:
            header           | start | end
            "bytes=0-99"     | 0     | 99
            "bytes=0-"       | 0     | 999       // open: until end
            "bytes=-500"     | 500   | 999       // suffix: last 500
            "bytes=500-"     | 500   | 999
            "bytes=950-2000" | 950   | 999       // end clamped to total-1
            "bytes=-2000"    | 0     | 999       // suffix larger than total -> whole file
            "bytes=999-999"  | 999   | 999       // single last byte
    }

    @Unroll
    def "invalid / out-of-range '#header' -> isValid()==false"() {
        when:
            Range r = Range.parse(header, TOTAL)
        then:
            r != null
            !r.isValid()
        where:
            header << [
                "bytes=1000-",      // start == total
                "bytes=2000-",      // start beyond total
                "bytes=200-100",    // start > end
                "bytes=-0",         // suffix of zero
                "bytes=abc-10",     // malformed
                "bytes=10",         // missing end dash
            ]
    }
}
