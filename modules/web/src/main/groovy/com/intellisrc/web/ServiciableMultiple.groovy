package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * Prefer Services over this interface
 *
 * @since 17/04/19.
 */
@CompileStatic
interface ServiciableMultiple extends Serviciable {
    abstract List<Service> getServices()
}