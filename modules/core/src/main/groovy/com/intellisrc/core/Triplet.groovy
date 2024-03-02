//file:noinspection GrFinalVariableAccess
package com.intellisrc.core

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * A class to hold 3 different objects or values
 * @since 2023/09/25.
 */
@CompileStatic
@Canonical
class Triplet<X, Y, Z> {
    final X first
    final Y middle
    final Z last
}
