package com.intellisrc.core

import spock.lang.Specification

import java.time.temporal.ChronoUnit

/**
 * @since 2020/05/25.
 */
class SysClockTest extends Specification {
    def "Setting clock"() {
        setup :
            def time = "2000-01-01 10:00:00".toDateTime()
            SysClock.setClockAt(time)
        when:
            sleep(2000)
        then:
            assert SysClock.date.YY == time.toLocalDate().YY
            assert ChronoUnit.SECONDS.between(time, SysClock.now) == 2
    }
}
