package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * Alternative to ServiciableSingle as class
 * @since 2022/08/02.
 */
@CompileStatic
abstract class SingleService implements ServiciableSingle {
    String path = ""
    String allowOrigin = null   // Localhost only
    String allowType = "*/*"
}
