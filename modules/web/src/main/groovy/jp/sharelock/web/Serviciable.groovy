package jp.sharelock.web

@groovy.transform.CompileStatic
/**
 * This is the common interface to define Services.
 * By itself is useless. Please extend it and implement
 * its usage inside WebService
 *
 * @since 17/04/03.
 */
interface Serviciable {
    abstract String getPath()
}
