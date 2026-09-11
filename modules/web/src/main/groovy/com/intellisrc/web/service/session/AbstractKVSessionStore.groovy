package com.intellisrc.web.service.session

import groovy.transform.CompileStatic
import org.eclipse.jetty.session.ObjectStreamSessionDataStore
import org.eclipse.jetty.session.SessionContext
import org.eclipse.jetty.session.SessionData
import org.eclipse.jetty.session.UnreadableSessionDataException
import org.eclipse.jetty.session.UnwriteableSessionDataException

import java.util.concurrent.ConcurrentHashMap

/**
 * Base class to persist the sessions into any key-value store.
 *
 * Each session is stored under a key that includes the context (in the
 * same way FileSessionDataStore does), using the same binary format, plus
 * a small extra entry with the expiry time of each session, so expired
 * sessions can be found and purged without deserializing them.
 *
 * Implementations only need to provide the basic key-value operations
 * (getValue, setValue, deleteKey, getKeys). See BerkeleySession or
 * RedisSession as examples.
 *
 * NOTE: the values stored as session attributes must be Serializable
 * to be able to survive a restart.
 *
 * @since 2026/09/11.
 */
@CompileStatic
abstract class AbstractKVSessionStore extends ObjectStreamSessionDataStore implements SessionPersistable {
    static final String EXPIRY_SUFFIX = ":expiry"
    static final String ENCODING = "UTF-8"

    /** In-memory index of session key -> expiry time (0 or less = never expires) */
    protected final Map<String, Long> expiryMap = new ConcurrentHashMap<>()
    protected String contextString = ""

    /**
     * Key under which a session is stored (it includes the context)
     * @param id
     * @return
     */
    protected String getIdWithContext(String id) {
        return contextString + "_" + id
    }

    @Override
    void initialize(SessionContext context) throws Exception {
        super.initialize(context)
        contextString = context.getCanonicalContextPath() + "_" + context.getVhost()
    }

    @Override
    boolean isPassivating() {
        return true
    }

    //--------- Key-Value operations to implement ---------

    /**
     * @param key
     * @return The raw value stored under key (null if not found)
     */
    abstract protected byte[] getValue(String key)

    /**
     * Store a raw value under a key
     * @param key
     * @param value
     * @return True if stored
     */
    abstract protected boolean setValue(String key, byte[] value)

    /**
     * Remove a key (and its value) from the store
     * @param key
     * @return True if removed
     */
    abstract protected boolean deleteKey(String key)

    /**
     * @return All keys in the store (including the expiry entries)
     */
    abstract protected Set<String> getKeys()

    //--------- Session storage (same binary format as FileSessionDataStore) ---------

    @Override
    void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        String key = getIdWithContext(id)
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            DataOutputStream out = new DataOutputStream(baos)
            out.writeUTF(id)
            out.writeUTF(_context.getCanonicalContextPath())
            out.writeUTF(_context.getVhost())
            out.writeUTF(data.getLastNode())
            out.writeLong(data.getCreated())
            out.writeLong(data.getAccessed())
            out.writeLong(data.getLastAccessed())
            out.writeLong(data.getCookieSet())
            out.writeLong(data.getExpiry())
            out.writeLong(data.getMaxInactiveMs())
            out.flush()
            serializeAttributes(data, baos)
            setValue(key, baos.toByteArray())
            // Expiry entry (to find expired sessions without deserializing them):
            setValue(key + EXPIRY_SUFFIX, Long.toString(data.getExpiry()).getBytes(ENCODING))
            expiryMap.put(key, data.getExpiry())
        } catch (Exception e) {
            throw new UnwriteableSessionDataException(id, _context, e)
        }
    }

    @Override
    SessionData doLoad(String id) throws Exception {
        byte[] blob = getValue(getIdWithContext(id))
        if (blob == null || blob.length == 0) {
            return null
        }
        try {
            DataInputStream di = new DataInputStream(new ByteArrayInputStream(blob))
            String sid = di.readUTF()
            String contextPath = di.readUTF()
            String vhost = di.readUTF()
            String lastNode = di.readUTF()
            long created = di.readLong()
            long accessed = di.readLong()
            long lastAccessed = di.readLong()
            long cookieSet = di.readLong()
            long expiry = di.readLong()
            long maxInactive = di.readLong()
            SessionData data = newSessionData(sid, created, accessed, lastAccessed, maxInactive)
            data.setContextPath(contextPath)
            data.setVhost(vhost)
            data.setLastNode(lastNode)
            data.setCookieSet(cookieSet)
            data.setExpiry(expiry)
            data.setMaxInactiveMs(maxInactive)
            deserializeAttributes(data, di)
            return data
        } catch (Exception e) {
            throw new UnreadableSessionDataException(id, _context, e)
        }
    }

    @Override
    boolean delete(String id) throws Exception {
        String key = getIdWithContext(id)
        expiryMap.remove(key)
        deleteKey(key + EXPIRY_SUFFIX)
        return deleteKey(key)
    }

    @Override
    boolean doExists(String id) throws Exception {
        String key = getIdWithContext(id)
        Long expiry = expiryMap.get(key)
        if (expiry == null) {
            // Not in the index (yet): check the store directly
            expiry = expiryOf(key + EXPIRY_SUFFIX)
            if (expiry != null) {
                expiryMap.put(key, expiry)
            }
        }
        if (expiry == null) { return false }
        // Expiry <= 0 means the session never expires:
        return expiry <= 0 || expiry > System.currentTimeMillis()
    }

    @Override
    Set<String> doCheckExpired(Set<String> candidates, long time) {
        Set<String> expired = new HashSet<>()
        if (candidates != null) {
            for (String id : candidates) {
                Long expiry = expiryMap.get(getIdWithContext(id))
                // No such session in the store, or expired:
                if (expiry == null || (expiry > 0 && expiry <= time)) {
                    expired.add(id)
                }
            }
        }
        return expired
    }

    @Override
    Set<String> doGetExpired(long timeLimit) {
        Set<String> expired = new HashSet<>()
        for (Map.Entry<String, Long> entry : expiryMap.entrySet()) {
            Long expiry = entry.getValue()
            if (expiry != null && expiry > 0 && expiry <= timeLimit) {
                expired.add(getIdFromKey(entry.getKey()))
            }
        }
        return expired
    }

    @Override
    void doCleanOrphans(long time) {
        Set<String> keys = getKeys()
        if (keys != null) {
            for (String key : keys) {
                if (key.endsWith(EXPIRY_SUFFIX)) {
                    Long expiry = expiryOf(key)
                    if (expiry != null && expiry > 0 && expiry <= time) {
                        String sessionKey = key.substring(0, key.length() - EXPIRY_SUFFIX.length())
                        deleteKey(key)
                        deleteKey(sessionKey)
                        expiryMap.remove(sessionKey)
                    }
                }
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        // Re-build the expiry index from the store:
        expiryMap.clear()
        Set<String> keys = getKeys()
        if (keys != null) {
            for (String key : keys) {
                if (key.endsWith(EXPIRY_SUFFIX)) {
                    Long expiry = expiryOf(key)
                    if (expiry != null) {
                        expiryMap.put(key.substring(0, key.length() - EXPIRY_SUFFIX.length()), expiry)
                    } else {
                        deleteKey(key) // Unreadable entry
                    }
                }
            }
        }
        super.doStart()
    }

    /**
     * Read the expiry time stored in an expiry entry
     * @param expiryKey
     * @return The expiry time or null if unreadable
     */
    protected Long expiryOf(String expiryKey) {
        byte[] value = getValue(expiryKey)
        if (value != null && value.length > 0) {
            try {
                return Long.parseLong(new String(value, ENCODING))
            } catch (NumberFormatException ignore) {}
        }
        return null
    }

    /**
     * Extract the session id from a key that includes the context
     * @param key
     * @return
     */
    protected String getIdFromKey(String key) {
        return key.substring(contextString.length() + 1)
    }
}
