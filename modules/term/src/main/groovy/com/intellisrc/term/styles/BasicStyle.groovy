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
    // Draw box around table FIXME
    boolean getWindow() { return true }
    // Draw borders TODO
    boolean getBorders() { return true }
    // If true, will add space in cells (left/right) TODO
    boolean getPadding() { return true }
}
