package jp.sharelock.etc

import groovy.transform.CompileStatic

/**
 * This is an implementation of Cache
 * as singleton. So it can be used as:
 *
 * CacheObj.instance.get("X")
 *
 * @since 17/10/30.
 */
@CompileStatic
@Singleton
class CacheObj extends Cache<Object> {}
