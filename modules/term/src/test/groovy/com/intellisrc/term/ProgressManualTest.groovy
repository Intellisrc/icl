package com.intellisrc.term

import static com.intellisrc.core.Millis.getMILLIS_10

/**
 * @since 19/07/10.
 */
class ProgressManualTest {
    static void main(String[] args) {
        int total = 500
        (0..total).each {
            Progress.summary(it, total, "So far... ")
            sleep(MILLIS_10)
        }
        (0..total).each {
            Progress.bar(it, total, "Done", 50)
            sleep(MILLIS_10)
        }
    }
}
