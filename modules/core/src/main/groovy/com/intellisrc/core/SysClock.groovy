package com.intellisrc.core

import groovy.transform.CompileStatic

import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.util.concurrent.TimeUnit

/**
 * This class is to be used instead of LocalDateTime.now(), etc.
 * If this class is used in a whole project, it can be changed
 * to any specific time so unit tests can be easily coded without
 * need of mocking core classes.
 *
 * Usage:
 * LocalDateTime myTime = SysClock.dateTime
 *
 * @since 2019/12/10.
 */
@CompileStatic
class SysClock {
    static public Clock clock = Clock.systemDefaultZone()
    static void setClockAt(LocalDateTime ldt) {
        ZonedDateTime ldtZoned = ldt.atZone(ZoneId.systemDefault())
        ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of("UTC"))
        clock = Clock.fixed(utcZoned.toInstant(), ZoneId.systemDefault())
    }
    // Alias:
    static LocalDateTime getNow() {
        return getDateTime()
    }
    static LocalDateTime getDateTime() {
        LocalDateTime.now(clock)
    }
    static LocalDate getDate() {
        LocalDate.now(clock)
    }
    static LocalTime getTime() {
        LocalTime.now(clock)
    }
    static long seconds(Temporal from, Temporal to = getDateTime()) {
        if(from) {
            return ChronoUnit.SECONDS.between(from, to)
        } else {
            Log.w("From time was null")
            return 0
        }
    }
    static long minutes(Temporal from, Temporal to = getDateTime()) {
        if(from) {
            return ChronoUnit.MINUTES.between(from, to)
        } else {
            Log.w("From time was null")
            return 0
        }
    }
    /**
     * Return elapsed time as String
     * @param value
     * @return
     */
    static String getTimeSince(LocalDateTime from, LocalDateTime to = dateTime) {
        long age = from ? ChronoUnit.MILLIS.between(from, to) : 0
        return millisToString(age)
    }
    /**
     * Convert millis into human readable format
     * @param millis
     * @return
     */
    static String millisToString(long millis) {
        String ageString = "0"
        if (millis > 0) {
            ageString = TimeUnit.MILLISECONDS.toDays(millis) + "d"
            if ("0d" == ageString) {
                ageString = TimeUnit.MILLISECONDS.toHours(millis) + "h"
                if ("0h" == ageString) {
                    ageString = TimeUnit.MILLISECONDS.toMinutes(millis) + "m"
                    if ("0m" == ageString) {
                        ageString = TimeUnit.MILLISECONDS.toSeconds(millis) + "s"
                        if ("0s" == ageString) {
                            ageString = millis + "ms"
                        }
                    }
                }
            }
        }
        return ageString
    }
}
