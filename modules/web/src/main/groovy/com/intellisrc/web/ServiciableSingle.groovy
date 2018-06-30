package com.intellisrc.web

/**
 * Use this interface if you need a single service
 *
 * @since 17/04/19.
 */
@groovy.transform.CompileStatic
interface ServiciableSingle extends Serviciable {
    abstract Service getService()
}