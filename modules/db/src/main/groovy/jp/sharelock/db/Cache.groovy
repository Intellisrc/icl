package jp.sharelock.db

import jp.sharelock.etc.Log

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@groovy.transform.CompileStatic
/**
 * Manage Cache of objects
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class Cache<V> {
    final static String LOG_TAG = Cache.simpleName
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
            Log.d(LOG_TAG, "Key [$key] expired.")
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
    V get(final String key, final V obj = null, long time = DEFAULT_TIMEOUT) {
        V ret
        if(exists(key)) {
            ret = cache.get(key).value
        } else {
            set(key, obj, time)
            ret = obj
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