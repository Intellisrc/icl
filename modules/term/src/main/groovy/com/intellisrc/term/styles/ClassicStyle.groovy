package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * The most classic table style: simple lines:
┌───────────┬───────────┬───────────────┬──────────────────────┐
│ Fruit     │ QTY       │ Price         │ Seller               │
├───────────┼───────────┼───────────────┼──────────────────────┤
│ Apple     │ 1000      │ 10.00         │ some@example.com     │
├───────────┼───────────┼───────────────┼──────────────────────┤
│ Kiwi      │ 900       │ 2350.40       │ example@example.com  │
├───────────┼───────────┼───────────────┼──────────────────────┤
│ Fruits: 4 │ Sum: 4302 │ Total: 2509.5 │                      │
└───────────┴───────────┴───────────────┴──────────────────────┘
 */
@CompileStatic
class ClassicStyle extends BasicStyle {
    String topLeft          = '┌'
    String bottomLeft       = '└'
    String topRight         = '┐'
    String bottomRight      = '┘'
    String intercept        = '┼'
    String verticalRight    = '├'
    String verticalLeft     = '┤'
    String horizontalDown   = '┬'
    String horizontalUp     = '┴'
    String rowSeparator     = '─'
    String colSeparator     = '│'
}
