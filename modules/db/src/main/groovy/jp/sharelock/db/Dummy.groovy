package jp.sharelock.db

/**
 * @since 17/12/13.
 */
class Dummy implements DB.Starter {
    @Override
    DB.Connector getNewConnection() {
        return new DummyConnector()
    }
}
