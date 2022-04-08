package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * This style draw tables with double lines:
 ╔═══════════╦═══════════╦═══════════════╦══════════════════════╗
 ║ Apple     ║ 1000      ║ 10.00         ║ some@example.com     ║
 ╠═══════════╬═══════════╬═══════════════╬══════════════════════╣
 ║ Fruits: 4 ║ Sum: 4302 ║ Total: 2509.5 ║                      ║
 ╚═══════════╩═══════════╩═══════════════╩══════════════════════╝
 */
@CompileStatic
class DoubleLineStyle extends BasicStyle {
    String topLeft          = '╔'
    String bottomLeft       = '╚'
    String topRight         = '╗'
    String bottomRight      = '╝'
    String intercept        = '╬'
    String verticalRight    = '╠'
    String verticalLeft     = '╣'
    String horizontalDown   = '╦'
    String horizontalUp     = '╩'
    String rowSeparator     = '═'
    String colSeparator     = '║'
}
