package com.intellisrc.core

import spock.lang.Specification

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * @since 2020/05/25.
 */
class SysClockTest extends Specification {
    def "Same as NOW"() {
        setup:
            SysClock.setClockAt(LocalDateTime.now())
        expect:
            assert Math.abs(SysClock.now.toMillis() - LocalDateTime.now().toMillis()) < 100
        when:
        sleep( 1250)
        then:
            assert Math.abs(SysClock.now.toMillis() - LocalDateTime.now().toMillis()) < 100
    }
    def "SysClock should not FREEZE"() {
        setup :
            SysClock.setClockAt(LocalDateTime.now())
            LocalDateTime time = SysClock.now.YMDHms.toDateTime() //Be sure is not reference
        when:
            sleep(1100)
        then:
            assert time.YMDHms != SysClock.now.YMDHms

    }
    def "Setting clock"() {
        setup :
            def time = "2000-01-01 10:00:00".toDateTime()
            SysClock.setClockAt(time)
        when:
            (1..2).each {
                sleep(1000)
                println SysClock.now.YMDHms
            }
        then:
            assert SysClock.date.YMD == time.toLocalDate().YMD
            assert ChronoUnit.SECONDS.between(time, SysClock.now) == 2
    }
    def "Specifying Zone in clock"() {
        setup:
            SysClock.setTimeZone("Asia/Tokyo") //UTC+9
            if(fixedTime) {
                def time = "2000-01-01 10:00:00".toDateTime()
                SysClock.setClockAt(time)
            }
            LocalDateTime timeOrig = SysClock.now.YMDHms.toDateTime() //Be sure is not reference
        when:
            SysClock.setTimeZone("Asia/Dubai") //UTC+4
        then:
            assert ChronoUnit.HOURS.between(SysClock.now, timeOrig) == 4
            println String.format("Time Tokyo: %s , Dubai: %s", timeOrig.YMDHms, SysClock.now.YMDHms)
        when:
            SysClock.setTimeZone("UTC") //UTC
        then:
            assert ChronoUnit.HOURS.between(SysClock.now, timeOrig) == 8
            println String.format("Time Tokyo: %s , UTC: %s", timeOrig.YMDHms, SysClock.now.YMDHms)
        where:
            fixedTime   |   notused
               true     |   0
               false    |   0
    }
}
