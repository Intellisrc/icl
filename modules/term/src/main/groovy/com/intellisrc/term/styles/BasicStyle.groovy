package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * If you want to implement your own style, extending this
 * class is the recommended method
 */
@CompileStatic
abstract class BasicStyle implements Stylable {
    String getHorizontalBorder() { return rowSeparator }
    String getVerticalBorder() { return colSeparator }
    // Draw box around table
    boolean getWindow() { return true }
    // Draw borders
    boolean getBorders() { return true }
    // If true, will add space in cells (left/right)
    boolean getPadding() { return true }
}
