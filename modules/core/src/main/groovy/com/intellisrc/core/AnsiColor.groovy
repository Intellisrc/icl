package com.intellisrc.core

import groovy.transform.CompileStatic

/**
 * Simple class to add colors to terminal
 * its in this package as Log requires it.
 * @since 19/04/15.
 *
 * ╔══════════╦════════════════════════════════╦═════════════════════════════════════════════════════════════════════════╗
 * ║  Code    ║             Effect             ║                                   Note                                  ║
 * ╠══════════╬════════════════════════════════╬═════════════════════════════════════════════════════════════════════════╣
 * ║ 0        ║  Reset / Normal                ║  all attributes off                                                     ║
 * ║ 1        ║  Bold or increased intensity   ║                                                                         ║
 * ║ 2        ║  Faint (decreased intensity)   ║  Not widely supported.                                                  ║
 * ║ 3        ║  Italic                        ║  Not widely supported. Sometimes treated as inverse.                    ║
 * ║ 4        ║  Underline                     ║                                                                         ║
 * ║ 5        ║  Slow Blink                    ║  less than 150 per minute                                               ║
 * ║ 6        ║  Rapid Blink                   ║  MS-DOS ANSI.SYS; 150+ per minute; not widely supported                 ║
 * ║ 7        ║  [[reverse video]]             ║  swap foreground and background colors                                  ║
 * ║ 8        ║  Conceal                       ║  Not widely supported.                                                  ║
 * ║ 9        ║  Crossed-out                   ║  Characters legible, but marked for deletion.  Not widely supported.    ║
 * ║ 10       ║  Primary(default) font         ║                                                                         ║
 * ║ 11–19    ║  Alternate font                ║  Select alternate font `n-10`                                           ║
 * ║ 20       ║  Fraktur                       ║  hardly ever supported                                                  ║
 * ║ 21       ║  Bold off or Double Underline  ║  Bold off not widely supported; double underline hardly ever supported. ║
 * ║ 22       ║  Normal color or intensity     ║  Neither bold nor faint                                                 ║
 * ║ 23       ║  Not italic, not Fraktur       ║                                                                         ║
 * ║ 24       ║  Underline off                 ║  Not singly or doubly underlined                                        ║
 * ║ 25       ║  Blink off                     ║                                                                         ║
 * ║ 27       ║  Inverse off                   ║                                                                         ║
 * ║ 28       ║  Reveal                        ║  conceal off                                                            ║
 * ║ 29       ║  Not crossed out               ║                                                                         ║
 * ║ 30–37    ║  Set foreground color          ║  See color table below                                                  ║
 * ║ 38       ║  Set foreground color          ║  Next arguments are `5;n` or `2;r;g;b`, see below                       ║
 * ║ 39       ║  Default foreground color      ║  implementation defined (according to standard)                         ║
 * ║ 40–47    ║  Set background color          ║  See color table below                                                  ║
 * ║ 48       ║  Set background color          ║  Next arguments are `5;n` or `2;r;g;b`, see below                       ║
 * ║ 49       ║  Default background color      ║  implementation defined (according to standard)                         ║
 * ║ 51       ║  Framed                        ║  not supported                                                          ║
 * ║ 52       ║  Encircled                     ║  not supported                                                          ║
 * ║ 53       ║  Overlined                     ║  not supported                                                          ║
 * ║ 54       ║  Not framed or encircled       ║  not supported                                                          ║
 * ║ 55       ║  Not overlined                 ║  not supported                                                          ║
 * ║ 60       ║  ideogram underline            ║  hardly ever supported                                                  ║
 * ║ 61       ║  ideogram double underline     ║  hardly ever supported                                                  ║
 * ║ 62       ║  ideogram overline             ║  hardly ever supported                                                  ║
 * ║ 63       ║  ideogram double overline      ║  hardly ever supported                                                  ║
 * ║ 64       ║  ideogram stress marking       ║  hardly ever supported                                                  ║
 * ║ 65       ║  ideogram attributes off       ║  reset the effects of all of 60-64                                      ║
 * ║ 90–97    ║  Set bright foreground color   ║  aixterm (not in standard)                                              ║
 * ║ 100–107  ║  Set bright background color   ║  aixterm (not in standard)                                              ║
 * ╚══════════╩════════════════════════════════╩═════════════════════════════════════════════════════════════════════════╝
 */
@CompileStatic
enum AnsiColor {
    static final String RESET       = "\u001B[0m"
    static final String BOLD        = "\u001B[1m"
    static final String UNDERLINE   = "\u001B[4m"
    static final String BLINK       = "\u001B[5m"
    static final String REVERSE     = "\u001B[7m"
    static final String NOBOLD      = "\u001B[22m"
    static final String NOUNDERLINE = "\u001B[24m"
    static final String NOBLINK     = "\u001B[25m"
    static final String NOREVERSE   = "\u001B[27m"
    static final String BLACK       = "\u001B[30m"
    static final String RED         = "\u001B[31m"
    static final String GREEN       = "\u001B[32m"
    static final String YELLOW      = "\u001B[33m"
    static final String BLUE        = "\u001B[34m"
    static final String PURPLE      = "\u001B[35m"
    static final String CYAN        = "\u001B[36m"
    static final String WHITE       = "\u001B[37m"
    static final String BACK_BLACK  = "\u001B[40m"
    static final String BACK_RED    = "\u001B[41m"
    static final String BACK_GREEN  = "\u001B[42m"
    static final String BACK_YELLOW = "\u001B[43m"
    static final String BACK_BLUE   = "\u001B[44m"
    static final String BACK_PURPLE = "\u001B[45m"
    static final String BACK_CYAN   = "\u001B[46m"
    static final String BACK_WHITE  = "\u001B[47m"
    static final String NOBACK      = "\u001B[49m"
    // Light colors:
    static final String L_BLACK     = "\u001B[90m"
    static final String L_RED       = "\u001B[91m"
    static final String L_GREEN     = "\u001B[92m"
    static final String L_YELLOW    = "\u001B[93m"
    static final String L_BLUE      = "\u001B[94m"
    static final String L_PURPLE    = "\u001B[95m"
    static final String L_CYAN      = "\u001B[96m"
    static final String L_WHITE     = "\u001B[97m"
    static final String L_BACK_BLACK  = "\u001B[100m"
    static final String L_BACK_RED    = "\u001B[101m"
    static final String L_BACK_GREEN  = "\u001B[102m"
    static final String L_BACK_YELLOW = "\u001B[103m"
    static final String L_BACK_BLUE   = "\u001B[104m"
    static final String L_BACK_PURPLE = "\u001B[105m"
    static final String L_BACK_CYAN   = "\u001B[106m"
    static final String L_BACK_WHITE  = "\u001B[107m"
}