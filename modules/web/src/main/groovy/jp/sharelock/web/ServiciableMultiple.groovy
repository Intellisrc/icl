package jp.sharelock.web

/**
 * Use this interface if you need more than one service
 *
 * @since 17/04/19.
 */
@groovy.transform.CompileStatic
interface ServiciableMultiple extends Serviciable {
    abstract List<Service> getServices()
}