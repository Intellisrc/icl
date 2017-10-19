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
	private static final String LOG_TAG = PoolConnector.getSimpleName()
	Connector connector
	long lastUsed = 0
	
	DB getDB() {
		if(DBPool?.getInstance()?.isInitialized()) {
			return new DB(this)
		} else {
			Log.e(LOG_TAG, "Pool has not been initialized")
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
            connector = DBPool?.getInstance()?.getConnectionFromPool()
			Log.d(LOG_TAG, "Connection got from Pool")
            try {
				if(!isOpen()) {
					connector.open()
					Log.d(LOG_TAG, "Connection was opened")
				}
            } catch (e) {
                Log.e(LOG_TAG, "Unable to get connection :" + e)
            }
        }
	}

	@Override
	void close() {
		DBPool?.getInstance()?.returnConnectionToPool(connector)
		Log.d(LOG_TAG, "Connection returned.")
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
