package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/13.
 */
@CompileStatic
class Dummy implements DB.Starter {
    @Override
    String getName() {
        return "dummy"
    }

    @Override
    String getConnectionString() {
        return "dummy://dummy"
    }

    @Override
    DB.Connector getNewConnection() {
        return new DummyConnector()
    }
}
