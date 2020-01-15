package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * Use this interface if you need a single service
 *
 * @since 17/04/19.
 */
@CompileStatic
interface ServiciableSingle extends Serviciable {
    abstract Service getService()
}