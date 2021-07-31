package com.intellisrc.core.props

import groovy.transform.CompileStatic

/**
 * A class which has common get/set methods
 * @since 2021/07/26.
 */
@CompileStatic
trait PropertiesRW implements PropertiesGet, PropertiesSet {}
