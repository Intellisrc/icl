package com.intellisrc.term

/**
 * @since 19/04/15.
 */
class ProgressTest {
    static void main(String[] args) {
        int total = 500
        (0..total).each {
            Progress.summary(it, total, "So far... ")
            sleep(5)
        }
        (0..total).each {
            Progress.bar(it, total, "Done", 50)
            sleep(5)
        }
    }
}