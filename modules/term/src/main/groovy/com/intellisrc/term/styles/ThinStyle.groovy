package com.intellisrc.term.styles

import groovy.transform.CompileStatic

/**
 * Draw tables in which horizontal and vertical lines are dotted:
┌╌╌╌╌╌╌╌╌┬╌╌╌╌╌╌┬╌╌╌╌╌╌╌╌╌┬╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┐
┊ Apple  ┊ 1000 ┊ 10.00   ┊ some@example.com     ┊
├╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┤
┊ Kiwi   ┊ 900  ┊ 2350.40 ┊ example@example.com  ┊
└╌╌╌╌╌╌╌╌┴╌╌╌╌╌╌┴╌╌╌╌╌╌╌╌╌┴╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┘
 */
@CompileStatic
class ThinStyle extends ClassicStyle {
    String rowSeparator = '╌'
    String colSeparator = '┊'
}
