package com.intellisrc.core

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * A class to hold 4 different objects or values
 * @since 2023/09/25.
 */
@CompileStatic
@Canonical
class Quadruplet<W, X, Y, Z> {
    final W first
    final X middleFirst
    final Y middleLast
    final Z last
}