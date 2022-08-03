package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * Prefer SingleService instead this interface
 *
 * @since 17/04/19.
 */
@CompileStatic
interface ServiciableSingle extends Serviciable {
    abstract Service getService()
}