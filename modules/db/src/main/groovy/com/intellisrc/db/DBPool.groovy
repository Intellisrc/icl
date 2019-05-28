package com.intellisrc.db

import com.intellisrc.core.Log

@groovy.transform.CompileStatic
/**
 * Manage the DB connection pool
 * It handles automatically the number of connections as it
 * increases or decrease the pool according to demand.
 * @author Alberto Lepe <lepe@intellisrc.com>
 */
class DBPool {
    private int timeoutSeconds = 600
	private boolean initialized = false
    private int currentConnections
	List<DB.Connector> availableConnections = []
	DB.Starter starter

	/**
	 * Initialize with JDBC connection
	 * @param DatabaseName
	 * @param ConnectionStr
     * @param timeout
	 */
	synchronized void init(DB.Starter dbStarter, int timeout = 0) {
		if(!initialized) {
			starter = dbStarter
			timeoutSeconds = timeout ?: timeoutSeconds
			Thread.startDaemon {
				long waitTime = (timeoutSeconds < 60 ? timeoutSeconds : 60) * 1000
				while (initialized) {
					timeoutPool()
					sleep waitTime
				}
			}
		}
		initialized = true
	}

	/**
	 * Closes connections that are timed out
	 */
	synchronized void timeoutPool() {
		if(!availableConnections.isEmpty()) {
			DB.Connector connector = availableConnections.find {
                DB.Connector conn ->
					if (conn.lastUsed > 0) {
						if (System.currentTimeSeconds() - conn.lastUsed > timeoutSeconds) {
                            return conn
						}
					}
			}
            if(connector != null) {
                connector.lastUsed = 0
                connector.close()
                availableConnections.remove(connector)
                Log.w( "DB timed out")
            }
		}
	}

    /**
     * Increases Pool connection
     * @param DatabaseName
     * @param ConnectionStr
     */
    synchronized void increasePool() {
        if (availableConnections.isEmpty()) {
            availableConnections.add(createNewConnectionForPool())
            Log.v( "DB added to pool")
        }
    }

    /**
     * Close all connections
     */
	synchronized void quit() {
		while (!availableConnections.isEmpty()) {
			availableConnections.first().close()
            availableConnections.remove(0)
		}
		Log.i( "Database has closed all connections")
	}

    /**
     * Get current number of connections used
     * @return
     */
	synchronized int getCurrentConnections() {
		return currentConnections
	}

	synchronized boolean isInitialized() {
		return initialized
	}

	//Creating a connection
	private synchronized DB.Connector createNewConnectionForPool() {
		DB.Connector conn = null
		try {
			conn = starter.getNewConnection()
		} catch (Exception e) {
			Log.e( "Unable to load the connection class", e)
		}
		return conn
	}

	synchronized DB.Connector getConnectionFromPool() {
		DB.Connector connection = null
		if(initialized) {
            increasePool()
            connection = availableConnections.first()
			connection.lastUsed = System.currentTimeSeconds()
            availableConnections.remove(connection)
            currentConnections++
            Log.v( "Current connections: " + currentConnections + " sleeping: " + availableConnections.size())
		} else {
			Log.e( "Database Pool was not initialized")
		}
		return connection
	}

	synchronized void returnConnectionToPool(DB.Connector connection) {
        currentConnections--
        availableConnections.add(connection)
	}
}
