package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * Draws a table with compact rows. This style is safe to display in any font or terminal:
 */
/*
 +-------------------+--------------------------------+-----+
 | Name              | Email                          | Age |
 |-------------------+--------------------------------+-----|
 | Joshep Patrishius | jp@example.com                 | 41  |
 |-------------------+--------------------------------+-----|
 | Zoe Mendoza       | you-know-who@example.com       | 54  |
 |-------------------+--------------------------------+-----|
 | All names here are fictitious                            |
 +----------------------------------------------------------+
*/
@CompileStatic
class SafeStyle extends BasicStyle {
    String topLeft          = '+'
    String bottomLeft       = '+'
    String topRight         = '+'
    String bottomRight      = '+'
    String intercept        = '+'
    String verticalRight    = '|'
    String verticalLeft     = '|'
    String horizontalDown   = '+'
    String horizontalUp     = '+'
    String rowSeparator     = '-'
    String colSeparator     = '|'
}
