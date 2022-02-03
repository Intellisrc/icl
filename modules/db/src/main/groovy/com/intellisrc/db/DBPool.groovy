package com.intellisrc.db

import com.intellisrc.core.AnsiColor
import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
/**
 * Manage the DB connection pool
 * It handles automatically the number of connections as it
 * increases or decrease the pool according to demand.
 * @author Alberto Lepe
 */
class DBPool {
	// Time before a connection is discarded if it is not returned to the pool (usually it means close() is missing)
    protected int timeoutSeconds = Config.get("db.timeout", 60)
	// Max life of a connection. Once it expires, a new connection should be created.
	protected int expireSeconds = Config.get("db.expire", 600)
	// Turn it true to debug connections
	protected boolean debugTimeout = Config.get("db.timeout.debug", false)
	protected boolean initialized = false
	Map<DB.Connector, List<StackTraceElement>> connTrace = [:]
	ConcurrentLinkedQueue<DB.Connector> availableConnections = new ConcurrentLinkedQueue<>()
	ConcurrentLinkedQueue<DB.Connector> currentConnections = new ConcurrentLinkedQueue<>()
	JDBC jdbc = null

	/**
	 * Initialize with JDBC connection
	 * @param DatabaseName
	 * @param ConnectionStr
     * @param timeout
	 */
	synchronized void init(JDBC jdbcObj, int timeout = 0, int expiration = 0) {
		if(!initialized) {
			jdbc = jdbcObj
			timeoutSeconds = timeout ?: timeoutSeconds
			expireSeconds = expiration ?: expireSeconds
			if(expireSeconds < timeoutSeconds) {
				Log.w("Connection expiration time should be usually greater than timeout: Expire: %d < Timeout: %d",
						expireSeconds, timeoutSeconds)
			}
			// Start with one connector:
			Thread.startDaemon {
				int minTime = [expireSeconds, timeoutSeconds].min()
				long waitTime = (minTime < 60 ? minTime : 60) * 1000
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
			Set<DB.Connector> connectors = availableConnections.findAll {
                DB.Connector conn ->
					return conn.lastUsed > 0 && (System.currentTimeSeconds() - conn.lastUsed > expireSeconds)
			}.toSet()
            if(!connectors.empty) {
				int closed = connectors.size()
				connectors.each {
					it.lastUsed = 0
					it.close()
					availableConnections.remove(it)
				}
				Log.v("%d Connection(s) expired after %d seconds. Available connections: %d", closed, expireSeconds, availableConnections.size())
			}
		}
		if(!currentConnections.empty) {
			Set<DB.Connector> connectors = currentConnections.findAll {
				DB.Connector conn ->
					return conn.lastUsed > 0 && (System.currentTimeSeconds() - conn.lastUsed > timeoutSeconds)
			}.toSet()
			if(!connectors.empty) {
				int closed = connectors.size()
				connectors.each {
					it.lastUsed = 0
					it.close()
					currentConnections.remove(it)
					if(debugTimeout) {
						println "-------------------------------------------------------------------------------------------"
						connTrace[it].each {
							StackTraceElement ste ->
								println AnsiColor.YELLOW + ste.className + AnsiColor.RED + "." + ste.methodName + ":" + AnsiColor.GREEN + ste.lineNumber + AnsiColor.RESET
						}
						println "-------------------------------------------------------------------------------------------"
					}
				}
				Log.w("%d Connection(s) timed out after %d seconds. Current connections: %d", closed, timeoutSeconds, currentConnections.size())
			}
		}
	}

	/**
	 * Increases Pool connection
	 * @param DatabaseName
	 * @param ConnectionStr
     */
    synchronized void increasePoolIfEmpty() {
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
			DB.Connector c = availableConnections.poll()
			c.close()
		}
		while (!currentConnections.isEmpty()) {
			DB.Connector c = currentConnections.poll()
			c.close()
		}
		Log.i( "Database has closed all connections")
	}

    /**
     * Get current number of connections used
     * @return
     */
	synchronized int getActive() {
		return currentConnections.size()
	}
	synchronized int getAvailable() {
		return availableConnections.size()
	}

	synchronized boolean isInitialized() {
		return initialized
	}

	//Creating a connection
	private synchronized DB.Connector createNewConnectionForPool() {
		DB.Connector conn = null
		try {
			conn = new JDBCConnector(jdbc)
		} catch (Exception e) {
			Log.e( "Unable to load the connection class", e)
		}
		return conn
	}

	synchronized DB.Connector getConnectionFromPool() {
		DB.Connector connection = null
		if(initialized) {
            increasePoolIfEmpty()
            connection = availableConnections.poll()
			connection.lastUsed = System.currentTimeSeconds()
			currentConnections.add(connection)
			if(debugTimeout) {
				connTrace[connection] = Thread.currentThread().stackTrace.toList()
			}
            Log.v( "Current connections: " + currentConnections.size() + " sleeping: " + availableConnections.size())
		} else {
			Log.e( "Database Pool was not initialized")
		}
		return connection
	}

	synchronized void returnConnectionToPool(DB.Connector connection) {
		if(connection) {
			currentConnections.remove(connection)
			availableConnections.add(connection)
			Log.v("Current connections: " + currentConnections.size() + " sleeping: " + availableConnections.size())
		}
	}
}
