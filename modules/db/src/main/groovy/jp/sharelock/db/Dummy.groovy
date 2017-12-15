package jp.sharelock.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/13.
 */
@CompileStatic
class Dummy implements DB.Starter {
    @Override
    DB.Connector getNewConnection() {
        return new DummyConnector()
    }
}
