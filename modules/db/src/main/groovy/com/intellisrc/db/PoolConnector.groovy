package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.sql.Connection

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
		return pool.jdbc.dbname ?: currentConnector.name
	}

	@Override
	JDBC getJdbc() {
		return pool?.jdbc ?: currentConnector?.jdbc
	}

	@Override
	List<String> getTables() {
		return open() ? currentConnector.tables : []
	}

	@Override
	List<ColumnInfo> getColumns(String table) {
		return open() ? currentConnector.getColumns(table) : []
	}

	@Override
	boolean open() {
		boolean isopen = isOpen()
        if(!isopen) {
            currentConnector = pool?.getConnectionFromPool()
			Log.v( "DB got from Pool")
            try {
				if(currentConnector.open()) {
					Log.v("DB was opened")
					isopen = true
				} else {
					Log.w("Unable to connect")
				}
            } catch (e) {
                Log.e( "Unable to get connection :", e)
            }
        }
		return isopen
	}

	@Override
	void clear(Connection connection) {
		if(open) {
			currentConnector.clear(connection)
		}
	}

	@Override
	boolean close() {
		boolean returned = false
		if(currentConnector) {
			pool?.returnConnectionToPool(currentConnector)
			Log.v("DB returned.")
			returned = true
		}
		return returned
	}

	@Override
	ResultStatement execute(Query query, boolean silent) {
		return open() ? currentConnector.execute(query, silent) : null
	}

    @Override
    boolean commit(List<Query> queries) {
        return open() ? currentConnector.commit(queries) : false
    }

	@Override
	void rollback() {
		if(open()) {
			currentConnector.rollback()
		}
	}

	@Override
	void onError(Throwable ex) {
		currentConnector?.onError(ex)
	}
}
