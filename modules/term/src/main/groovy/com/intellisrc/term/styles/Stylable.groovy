package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * This interface is to be used by TableMaker
 */
@CompileStatic
interface Stylable {
    String getTopLeft()
    String getBottomLeft()
    String getTopRight()
    String getBottomRight()
    String getIntercept()
    String getVerticalLeft()
    String getVerticalRight()
    String getHorizontalDown()
    String getHorizontalUp()
    String getRowSeparator()
    String getColSeparator()
    String getHorizontalBorder()
    String getVerticalBorder()
    boolean getWindow()
    boolean getBorders()
    boolean getPadding()
}