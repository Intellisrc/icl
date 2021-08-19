package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.DB.Connector
import com.intellisrc.db.DB.DBType
import com.intellisrc.db.DB.Statement
import groovy.transform.CompileStatic

@CompileStatic
/**
 * Implements a Connector to be used in DBPool
 * @author Alberto Lepe <lepe@intellisrc.com>
 */
class PoolConnector implements Connector {
	protected Connector currentConnector
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
		return currentConnector?.isOpen()
	}

	@Override
	String getName() {
		return pool.starter.name ?: currentConnector.name
	}

	@Override
	DBType getType() {
		return pool?.starter?.type ?: currentConnector?.type
	}

	@Override
	void open() {
        if(!isOpen()) {
            currentConnector = pool?.getConnectionFromPool()
			Log.v( "DB got from Pool")
            try {
				if(!isOpen()) {
					currentConnector.open()
					Log.v( "DB was opened")
				}
            } catch (e) {
                Log.e( "Unable to get connection :", e)
            }
        }
	}

	@Override
	boolean close() {
		pool?.returnConnectionToPool(currentConnector)
		Log.v( "DB returned.")
		return true
	}

	@Override
	Statement prepare(Query query) {
		return currentConnector?.prepare(query)
	}

	@Override
	void onError(Exception ex) {
		currentConnector?.onError(ex)
	}
}
