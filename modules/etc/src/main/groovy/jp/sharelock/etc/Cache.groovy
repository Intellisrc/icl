package jp.sharelock.etc

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@groovy.transform.CompileStatic
/**
 * Manage Cache of objects
 * @author Alberto Lepe <lepe@sharelock.jp>
 * @param <V>
 */
class Cache<V> {
    interface onNotFound {
        V call()
    }
    final int DEFAULT_TIMEOUT = 60 //seconds
    private class CacheObj {
        V       value
        long    expire
        CacheObj(V obj, long time) {
            value = obj
            expire = time
        }
    }
    final ConcurrentMap<String, CacheObj> cache = new ConcurrentHashMap<>()

	/**
	 * Returns true if cache is empty
	 * @return 
	 */
	boolean isEmpty() {
		return cache.isEmpty()
	}
	/**
	 * Returns true if key exists
	 * @param key
	 * @return 
	 */
    boolean exists(final String key) {
        return cache.containsKey(key) &&! expired(key)
    }
    /**
     * Return true if expired and remove it from cache
     * @param key
     * @return
     */
    private boolean expired(final String key) {
        def expired = false
        if(cache.get(key)?.expire < new Date().getTime()) {
            Log.d( "Key [$key] expired.")
            del(key)
            expired = true
        }
        return expired
    }
	/**
	 * Returns value if found, or set object if not
	 * @param key
	 * @param default_val
	 * @return
	 */
    V get(final String key, onNotFound notFound = null, long time = DEFAULT_TIMEOUT) {
        V ret = null
        if(exists(key)) {
            ret = cache.get(key).value
			Log.v( "[$key] read from cache")
        } else {
            if(notFound) {
                ret = notFound.call()
				if(ret) {
					Log.v( "[$key] added to cache")
				}
            }
            set(key, ret, time)
        }
        return ret
    }

	/**
	 * Set value to key. If value is null, it will be removed
	 * @param key
	 * @param value 
	 */
    void set(final String key, final V value, long time = DEFAULT_TIMEOUT) {
		if(value == null) {
			del(key)
		} else {
	        cache.put(key, new CacheObj(value, new Date().getTime() + (time * 1000)))
		}
	}

	/**
	 * Removes a key from the cache
	 * @param key
	 * @return 
	 */
	boolean del(final String key) {
		if(cache.containsKey(key)) {
			cache.remove(key)
			return true
		}
		return false
	}
	
	/**
	 * Clear all values
	 */
	void clear() {
		cache.clear()
	}

	/**
	 * Return cache size
	 * @return 
	 */
	int size() {
		return cache.size()
	}

    /**
     * Will remove expired elements
     * it doesn't run automatically. Needs to be called
     */
    void garbageCollect() {
        cache.each {
            String key, CacheObj co ->
                expired(key)
        }
    }
}