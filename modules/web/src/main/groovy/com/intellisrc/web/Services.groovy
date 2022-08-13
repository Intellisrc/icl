package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * Alternative to ServiciableMultiple as class
 * @since 2022/08/02.
 */
@CompileStatic
abstract class Services implements ServiciableMultiple {
    String path = ""
    String allowOrigin = null   // Localhost only
    String allowType = "*/*"
}
