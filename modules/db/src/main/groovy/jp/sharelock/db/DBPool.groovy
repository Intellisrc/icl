package jp.sharelock.db

import jp.sharelock.etc.AndroidContext
import jp.sharelock.etc.Log
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

@groovy.transform.CompileStatic
@Singleton
/**
 * Manage the DB connection pool
 * If poolsize == 0, it will only act as a wrapper to the connection. That
 * means that connections will he handled and closed as requested (no pool)
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class DBPool {

	private static final String LOG_TAG = DBPool.getSimpleName()
    private int TIMEOUT_SEC = 600
	private boolean initialized = false
    private String databaseName
    private String connectionString
    private int currentConnections
	List<DB.Connector> availableConnections = []

	/**
	 * Initialize with database name and pool size (e.g. from Android)
	 * @param DatabaseName
	 * @param poolsize
     * @param timeout
	 */
	synchronized void init(String database, int timeout = TIMEOUT_SEC) {
		init(database, "", timeout)
	}
	/**
	 * Initialize with JDBC connection
	 * @param DatabaseName
	 * @param ConnectionStr
     * @param timeout
	 */
	synchronized void init(String database, String connectionStr, int timeout = TIMEOUT_SEC) {
		if(!initialized) {
			databaseName = database
			connectionString = connectionStr
			TIMEOUT_SEC = timeout
			Thread.startDaemon {
				long waitTime = (TIMEOUT_SEC < 60 ? TIMEOUT_SEC : 60) * 1000
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
						if (System.currentTimeSeconds() - conn.lastUsed > TIMEOUT_SEC) {
                            return conn
						}
					}
			}
            if(connector != null) {
                connector.lastUsed = 0
                connector.close()
                availableConnections.remove(connector)
                Log.v(LOG_TAG, "Connection timed out")
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
            Log.v(LOG_TAG, "Connection added to pool")
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
		Log.d(LOG_TAG, "Database has closed all connections")
	}

    /**
     * Get current number of connections used
     * @return
     */
	synchronized int currentConnections() {
		return currentConnections
	}

	synchronized boolean isInitialized() {
		return initialized
	}

	//Creating a connection
	private synchronized DB.Connector createNewConnectionForPool() {
		DB.Connector conn = null
		boolean isAndroid = System.getProperties().getProperty("java.vendor").contains("Android")
		try {
            if(connectionString == "dummy") {
                conn = new DummyConnector()
            } else if (isAndroid) {
				Constructor con = Class.forName("jp.sharelock.db.AndroidConnector").getConstructor(String, Class.forName("android.content.Context"))
				conn = (DB.Connector) con.newInstance(databaseName, AndroidContext.context)
			} else {
				Constructor con = Class.forName("jp.sharelock.db.JDBCConnector").getConstructor(String, String)
				conn = (DB.Connector) con.newInstance(connectionString, databaseName)
			}
		} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
			Log.e(LOG_TAG, "Unable to load the connection class: " + ex)
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
            Log.v(LOG_TAG, "Current connections: " + currentConnections + " sleeping: " + availableConnections.size())
		} else {
			Log.e(LOG_TAG, "Database Pool was not initialized")
		}
		return connection
	}

	synchronized void returnConnectionToPool(DB.Connector connection) {
        currentConnections--
        availableConnections.add(connection)
	}
}
