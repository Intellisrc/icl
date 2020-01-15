package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * This is the common interface to define Services.
 * By itself is useless. Please extend it and implement
 * its usage inside WebService
 *
 * All paths can include: (@see: http://sparkjava.com/documentation#routes)
 * <code>
 * /                                : root directory
 * /foo                             : will work with "/foo", but not "/foo/"
 * /foo/bar                         : subdirectory
 * /foo/:var                        : You can read directory using: request.params(":var")
 * /foo/*_/some/*_/bar              : Array can be get with: request.splat()  (*_ = *)
 * </code>
 *
 * You can read GET parameters like: request.queryParams("name")
 *
 * @since 17/04/03.
 */
@CompileStatic
trait Serviciable {
    String getPath() { return "/" }
}
