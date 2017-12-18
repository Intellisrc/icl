package jp.sharelock.db

import jp.sharelock.etc.Log
import jp.sharelock.db.DB.Connector
import jp.sharelock.db.DB.DBType
import jp.sharelock.db.DB.Statement

@groovy.transform.CompileStatic
/**
 * Implements a Connector to be used in DBPool
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class PoolConnector implements Connector {
	Connector connector
	private final DBPool pool
	long lastUsed = 0

	PoolConnector(DBPool dbPool) {
		pool = dbPool
	}

	DB getDB() {
		if(pool.initialized) {
			return new DB(this)
		} else {
			Log.e( "Pool has not been initialized")
			return null
		}
	}

	@Override
	boolean isOpen() {
		return connector?.isOpen()
	}

	@Override
	void open() {
        if(!isOpen()) {
            connector = pool?.getConnectionFromPool()
			Log.d( "DB got from Pool")
            try {
				if(!isOpen()) {
					connector.open()
					Log.d( "DB was opened")
				}
            } catch (e) {
                Log.e( "Unable to get connection :", e)
            }
        }
	}

	@Override
	void close() {
		pool?.returnConnectionToPool(connector)
		Log.d( "DB returned.")
	}

	@Override
	Statement prepare(Query query) {
		return connector?.prepare(query)
	}

	@Override
	void onError(Exception ex) {
		connector?.onError(ex)
	}

	@Override
	DBType getType() {
		return connector?.getType()
	}
}
