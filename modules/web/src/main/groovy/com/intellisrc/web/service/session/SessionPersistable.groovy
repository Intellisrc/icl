package com.intellisrc.web.service.session

import groovy.transform.CompileStatic
import org.eclipse.jetty.session.SessionData
import org.eclipse.jetty.session.SessionDataStore

/**
 * Common contract for the stores used to keep the sessions of a WebService.
 *
 * It preserves the important method declarations of FileSessionDataStore (Jetty),
 * so that implementations extending it (like FileSession) are almost trivial,
 * while other implementations (like BerkeleySession or RedisSession) can be
 * built on top of any key-value backend.
 *
 * To add a new implementation, extend one of the Jetty base classes
 * (NullSessionDataStore, FileSessionDataStore or ObjectStreamSessionDataStore)
 * and implement this interface, then assign it to WebService.sessionType
 * (or select it with web.session.type).
 *
 * @see com.intellisrc.web.WebService#sessionType
 * @since 2026/09/11.
 */
@CompileStatic
interface SessionPersistable extends SessionDataStore {

    /**
     * Store the session data persistently
     * @param id : session id
     * @param data : session data to store
     * @param lastSaveTime : time of the previous save (0 if never saved)
     */
    void doStore(String id, SessionData data, long lastSaveTime) throws Exception

    /**
     * Load a session from the store
     * @param id : session id
     * @return The session data or null if not found
     */
    SessionData doLoad(String id) throws Exception

    /**
     * Check if a session exists and has not expired
     * @param id : session id
     * @return True if the session exists
     */
    boolean doExists(String id) throws Exception

    /**
     * Delete a session from the store
     * @param id : session id
     * @return True if the session was deleted
     */
    boolean delete(String id) throws Exception

    /**
     * Check which of the candidate sessions have actually expired
     * @param candidates : session ids that might have expired
     * @param time : current time (ms)
     * @return The ids of the expired sessions
     */
    Set<String> doCheckExpired(Set<String> candidates, long time)

    /**
     * Find the sessions that expired at or before the given time
     * @param timeLimit : time (ms)
     * @return The ids of the expired sessions
     */
    Set<String> doGetExpired(long timeLimit)

    /**
     * Remove the sessions that expired a long time ago
     * @param timeLimit : time (ms)
     */
    void doCleanOrphans(long timeLimit)

    /**
     * @return True if the store serializes the sessions (passivates them)
     */
    boolean isPassivating()

    /**
     * Name identifying this store (shown in logs and used by web.session.type)
     * @return Store name (e.g. "memory", "file", "berkeley", "redis")
     */
    String name()
}
