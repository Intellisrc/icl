package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * Use this interface if you need more than one service
 *
 * @since 17/04/19.
 */
@CompileStatic
interface ServiciableMultiple extends Serviciable {
    abstract List<Service> getServices()
}