package com.intellisrc.db

import com.intellisrc.core.*
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
/**
 * Manage the DB connection pool
 * It handles automatically the number of connections as it
 * increases or decrease the pool according to demand.
 *
 * It will keep connections for some time (specified by expireSeconds) in which
 * they can be reused to increase performance
 *
 * @author Alberto Lepe
 */
class DBPool {
	// Time before a connection is discarded if it is not returned to the pool (usually it means close() is missing)
    protected int timeoutSeconds = Config.any.get("db.timeout", 60)
	// Expiration time of a connection without being used. Once it expires, a new connection should be created.
	protected int expireSeconds = Config.any.get("db.expire", 600)
	// Max reuse time of a connection. Even if the connection is being used, we will close connections which were created long ago.
	protected int maxLifeSeconds = Config.any.get("db.max.life", 3600)
	// Turn it true to debug connections
	protected String debugTimeoutPackage = Config.any.get("db.timeout.debug")
	protected boolean initialized = false
	ConcurrentLinkedQueue<Connector> availableConnections = new ConcurrentLinkedQueue<>()
	ConcurrentLinkedQueue<Connector> currentConnections = new ConcurrentLinkedQueue<>()
	List<TimeoutTrace> connTrace = []
	JDBC jdbc = null

	private static class TimeoutTrace {
		Connector connector
		List<StackTraceElement> trace = []
	}

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
				int waitTime = [expireSeconds, timeoutSeconds].min()
				if(waitTime > Secs.MINUTE) {
					Log.v("Connection expiration and timeout values are grater than a minute. Checking time set to 1 minute.")
					waitTime = Millis.MINUTE
				}
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
		// Expire connections which haven't been used in a while or where created long time ago:
		if(!availableConnections.isEmpty()) {
			Set<Connector> connectors = availableConnections.findAll {
                Connector conn ->
					return (conn.lastUsed && ChronoUnit.SECONDS.between(conn.lastUsed, SysClock.now) > expireSeconds) ||
	   					   (conn.creationTime && ChronoUnit.SECONDS.between(conn.creationTime, SysClock.now) > maxLifeSeconds)
			}.toSet()
            if(!connectors.empty) {
				int closed = connectors.size()
				connectors.each {
					it.lastUsed = null
					it.creationTime = null
					it.close()
					availableConnections.remove(it)
				}
				Log.v("%d Connection(s) expired after %d seconds. Available connections: %d", closed, expireSeconds, availableConnections.size())
			}
		}
		// Timeout (return) connections if they have not been returned after some time:
		if(!currentConnections.empty) {
			Set<Connector> connectors = currentConnections.findAll {
				Connector conn ->
					return conn.lastUsed && (ChronoUnit.SECONDS.between(conn.lastUsed, SysClock.now) > timeoutSeconds)
			}.toSet()
			if(!connectors.empty) {
				int closed = connectors.size()
				connectors.each {
					it.lastUsed = null
					it.close()
					if(debugTimeoutPackage) {
						TimeoutTrace timeoutTrace = connTrace.find {
							TimeoutTrace tt ->
								tt.connector == it
						}
						if(timeoutTrace) {
							if(timeoutTrace.trace.empty) {
								Log.v("No trace package found containing: %s (try using '*')", debugTimeoutPackage)
							} else {
								println "-------------------------------------------------------------------------------------------"
								timeoutTrace.trace.each {
									StackTraceElement ste ->
										println AnsiColor.YELLOW + ste.className + AnsiColor.RED + "." + ste.methodName + ":" + AnsiColor.GREEN + ste.lineNumber + AnsiColor.RESET
								}
								println "-------------------------------------------------------------------------------------------"
							}
						}
					}
					currentConnections.remove(it)
				}
				Log.w("%d Connection(s) timed out after %d seconds. Current connections: %d, Available connections: %d", closed, timeoutSeconds, currentConnections.size(), availableConnections.size())
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
			Connector c = availableConnections.poll()
			c.close()
		}
		while (!currentConnections.isEmpty()) {
			Connector c = currentConnections.poll()
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
	private synchronized Connector createNewConnectionForPool() {
		Connector conn = null
		try {
			conn = new JDBCConnector(jdbc)
		} catch (Exception e) {
			Log.e( "Unable to load the connection class", e)
		}
		return conn
	}

	synchronized Connector getConnectionFromPool() {
		Connector connection = null
		if(initialized) {
            increasePoolIfEmpty() // Here we create a new connection if needed and add them to availableConnections
            connection = availableConnections.poll()
			connection.lastUsed = SysClock.now
			if(!currentConnections.contains(connection)) {
				currentConnections.add(connection)
				Log.v( "[ADD] Current connections: " + currentConnections.size() + " sleeping: " + availableConnections.size())
			}
			if(debugTimeoutPackage) {
				connTrace << new TimeoutTrace(
					connector: connection,
					trace: Thread.currentThread().stackTrace.toList().findAll {
						return debugTimeoutPackage == "*" || it.className.contains(debugTimeoutPackage)
					}
				)
			}
		} else {
			Log.e( "Database Pool was not initialized")
		}
		return connection
	}

	synchronized void returnConnectionToPool(Connector connection) {
		if(connection) {
			currentConnections.remove(connection)
			if(!availableConnections.contains(connection)) {
				if(connection.open) { // Only keep opened connections
					availableConnections.add(connection)
				} else {
					connection.close()
				}
				Log.v("[DEL] Current connections: " + currentConnections.size() + " sleeping: " + availableConnections.size())
			}
		}
	}
}
