package com.intellisrc.term

import com.intellisrc.core.AnsiColor
import groovy.transform.CompileStatic

import java.text.DecimalFormat

/**
 * Display progress of command
 * @since 19/04/15.
 */
@CompileStatic
class Progress {
    /**
     * Display progress in a simple way
     * @param current
     * @param total
     * @param label
     */
    static void summary(int current, int total, String label = "") {
        double pp = (current / total) * 100.0d
        print "\rProgress $label: " + current + "/" + total + " : " + new DecimalFormat("##.00").format(pp) + "% "
    }
    /**
     * Display progress with bar
     * @param current
     * @param total
     * @param label
     */
    static void bar(int current, int total, String label = "", int size = 100) {
        double pp = (current / total) * 100.0d
        int p = pp.toFloat().round()
        int green   = ((p > 50 ? 50 : (p > 0 ? p : 0)) * size / 100d).toFloat().round()
        p -= 50
        int yellow  = ((p > 25 ? 25 : (p > 0 ? p : 0)) * size / 100d).toFloat().round()
        p -= 25
        int red     = ((p > 0 ? p : 0) * size / 100d).toFloat().round()
        p -= 25
        int filler  = (Math.abs(p) * (size / 100d)).toFloat().round()
        print "\r" + AnsiColor.BLUE + label + AnsiColor.RESET +
                AnsiColor.BOLD + "[" + AnsiColor.RESET +
                AnsiColor.GREEN + ("|" * green) +
                AnsiColor.YELLOW + ("|" * yellow) +
                AnsiColor.RED + ("|" * red) + AnsiColor.RESET +
                (" " * filler) +
                AnsiColor.BOLD + "] " + new DecimalFormat("##.00").format(pp) + "% " + "(" + current + "/" + total + ")"
    }
}