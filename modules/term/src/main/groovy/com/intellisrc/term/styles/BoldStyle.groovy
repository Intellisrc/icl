package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * Draws a table with bold lines:
 ┏━━━━━━━━┳━━━━━━┳━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━┓
 ┃ Fruit  ┃ QTY  ┃ Price   ┃ Seller               ┃
 ┣━━━━━━━━╋━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━┫
 ┃ Kiwi   ┃ 900  ┃ 2350.40 ┃ example@example.com  ┃
 ┗━━━━━━━━┻━━━━━━┻━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━┛
 */
@CompileStatic
class BoldStyle extends BasicStyle {
    String topLeft          = '┏'
    String bottomLeft       = '┗'
    String topRight         = '┓'
    String bottomRight      = '┛'
    String intercept        = '╋'
    String verticalRight    = '┣'
    String verticalLeft     = '┫'
    String horizontalDown   = '┳'
    String horizontalUp     = '┻'
    String rowSeparator     = '━'
    String colSeparator     = '┃'
}
