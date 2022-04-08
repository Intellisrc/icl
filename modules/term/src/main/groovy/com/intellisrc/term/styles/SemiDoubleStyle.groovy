package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * This style draws a border with double lines and
 * internal lines are single:
 *
╔════════╤══════╤═════════╤══════════════════════╗
║ Fruit  │ QTY  │ Price   │ Seller               ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Apple  │ 1000 │ 10.00   │ some@example.com     ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Kiwi   │ 900  │ 2350.40 │ example@example.com  ║
╚════════╧══════╧═════════╧══════════════════════╝
 */
@CompileStatic
class SemiDoubleStyle extends DoubleLineStyle {
    String intercept        = '┼'
    String verticalRight    = '╟'
    String verticalLeft     = '╢'
    String horizontalDown   = '╤'
    String horizontalUp     = '╧'
    String rowSeparator     = '─'
    String colSeparator     = '│'
    String horizontalBorder = '═'
    String verticalBorder   = '║'
}
