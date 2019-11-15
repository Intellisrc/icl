package com.intellisrc.etc

import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * @since 2019/10/03.
 */
@CompileStatic
class TimeClock {
    /**
     * Return elapsed time as String
     * @param value
     * @return
     */
    static String getTimeSince(LocalDateTime from, LocalDateTime to = LocalDateTime.now()) {
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
