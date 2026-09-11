package com.intellisrc.web.service.session

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.etc.Redis
import groovy.transform.CompileStatic

/**
 * Persists the sessions into a Redis server (com.intellisrc.etc.Redis),
 * so they survive a restart of the WebService.
 *
 * The Redis server is configured with redis.host and redis.port, and the
 * session keys are grouped under the prefix specified by
 * web.session.redis.prefix (default: "web.sessions").
 *
 * NOTE: the values stored as session attributes must be Serializable
 * to be able to survive a restart.
 *
 * @since 2026/09/11.
 */
@CompileStatic
class RedisSession extends AbstractKVSessionStore {
    protected Redis redis
    protected final String defaultPrefix = Config.any.get("web.session.redis.prefix", "sessions")

    /**
     * @param redis : Redis connection to use. If not specified, a new one is
     *               created with web.session.redis.prefix (default: "web.sessions")
     */
    RedisSession(Redis redis = null) {
        this.redis = redis ?: new Redis(defaultPrefix)
    }

    @Override
    String name() {
        return "redis"
    }

    //--------- Key-Value operations (values are stored as Base64 strings) ---------

    @Override
    protected byte[] getValue(String key) {
        String value = redis.get(key)
        if (value == null || value.isEmpty()) { return null }
        try {
            return Base64.getDecoder().decode(value)
        } catch (IllegalArgumentException e) {
            Log.w("Unable to decode session stored in Redis (key: %s)", key, e)
            return null
        }
    }

    @Override
    protected boolean setValue(String key, byte[] value) {
        return redis.set(key, Base64.getEncoder().encodeToString(value))
    }

    @Override
    protected boolean deleteKey(String key) {
        return redis.delete(key)
    }

    @Override
    protected Set<String> getKeys() {
        Set<String> keys = redis.getKeys()
        return keys ?: new HashSet<String>()
    }
}
