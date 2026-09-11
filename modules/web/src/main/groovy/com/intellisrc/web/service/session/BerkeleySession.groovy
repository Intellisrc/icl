package com.intellisrc.web.service.session

import com.intellisrc.core.Config
import com.intellisrc.etc.BerkeleyDB
import groovy.transform.CompileStatic
import org.eclipse.jetty.session.SessionData

/**
 * Persists the sessions into a BerkeleyDB key-value store
 * (com.intellisrc.etc.BerkeleyDB), so they survive a restart of the WebService.
 *
 * The database name can be specified with web.session.berkeley.db
 * (default: "web_sessions") and it is stored in the directory specified
 * by berkeley.dir (default: ".berkeley").
 *
 * NOTE: the values stored as session attributes must be Serializable
 * to be able to survive a restart.
 *
 * @since 2026/09/11.
 */
@CompileStatic
class BerkeleySession extends AbstractKVSessionStore {
    protected BerkeleyDB db
    protected final String defaultDbName = Config.any.get("web.session.berkeley", "web_sessions")
    protected boolean closedByStop = false

    /**
     * @param db : BerkeleyDB to use. If not specified, a new one is created
     *            with web.session.berkeley.db (default: "web_sessions")
     */
    BerkeleySession(BerkeleyDB db = null) {
        this.db = db ?: new BerkeleyDB(defaultDbName, "session")
    }

    @Override
    String name() {
        return "berkeley"
    }

    @Override
    protected void doStart() throws Exception {
        if (closedByStop) {
            // The database was closed on stop: open it again
            db = new BerkeleyDB(defaultDbName, "session")
            closedByStop = false
        }
        super.doStart()
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop()
        db.close()
        closedByStop = true
    }

    @Override
    void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        super.doStore(id, data, lastSaveTime)
        db.sync() // Flush deferred writes to disk
    }

    //--------- Key-Value operations ---------

    @Override
    protected byte[] getValue(String key) {
        return db.getBytes(key).orElse(null)
    }

    @Override
    protected boolean setValue(String key, byte[] value) {
        return db.set(key, value)
    }

    @Override
    protected boolean deleteKey(String key) {
        return db.delete(key)
    }

    @Override
    protected Set<String> getKeys() {
        Set<String> keys = db.getKeys()
        return keys ?: new HashSet<String>()
    }
}
