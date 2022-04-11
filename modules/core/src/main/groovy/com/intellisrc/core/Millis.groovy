package com.intellisrc.core

import groovy.transform.CompileStatic

/**
 * Time conversion to milliseconds
 *
 * NOTE:
 * MONTH is calculated as 30 days (use MONTH_31D if you want to get a 31 days month)
 * YEAR is calculated as 365 days (use YEAR_LEAP if you want to get 366 days year)
 * @since 2022/03/04.
 */
@CompileStatic
class Millis {
    static final int SECOND       = 1000
    static final int SECOND_2     = 2000
    static final int SECOND_3     = 3000
    static final int SECOND_5     = 5000
    static final int SECOND_10    = 10000
    static final int SECOND_15    = 1500
    static final int SECOND_20    = 20000
    static final int SECOND_30    = 30000
    static final int SECOND_45    = 45000
    static final int MINUTE       = 60 * SECOND
    static final int MIN_2        = 2 * MINUTE
    static final int MIN_3        = 3 * MINUTE
    static final int MIN_5        = 5 * MINUTE
    static final int MIN_10       = 10 * MINUTE
    static final int MIN_15       = 15 * MINUTE
    static final int MIN_20       = 20 * MINUTE
    static final int HALF_HOUR    = 30 * MINUTE
    static final int MIN_45       = 45 * MINUTE
    static final int HOUR         = 60 * MINUTE
    static final int HOUR_2       = 120 * MINUTE
    static final int HOUR_4       = 4 * HOUR
    static final int HOUR_8       = 8 * HOUR
    static final int HALF_DAY     = 12 * HOUR
    static final int DAY          = 24 * HOUR
    static final int WEEK         = 7 * DAY
    static final int MONTH        = 30 * DAY
    static final int MONTH_31D    = 31 * DAY
    static final int YEAR         = 365 * DAY
    static final int YEAR_LEAP    = 366 * DAY
}