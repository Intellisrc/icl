package com.intellisrc.db

import com.intellisrc.etc.Log
import com.intellisrc.db.DB.Connector
import com.intellisrc.db.DB.DBType
import com.intellisrc.db.DB.Statement

@groovy.transform.CompileStatic
/**
 * Implements a Connector to be used in DBPool
 * @author Alberto Lepe <lepe@intellisrc.com>
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
			Log.v( "DB got from Pool")
            try {
				if(!isOpen()) {
					connector.open()
					Log.v( "DB was opened")
				}
            } catch (e) {
                Log.e( "Unable to get connection :", e)
            }
        }
	}

	@Override
	void close() {
		pool?.returnConnectionToPool(connector)
		Log.v( "DB returned.")
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
