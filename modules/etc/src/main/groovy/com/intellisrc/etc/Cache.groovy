package com.intellisrc.etc

import com.intellisrc.core.*
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.regex.Pattern

@CompileStatic
/**
 * Manage Cache of objects
 * @author Alberto Lepe <lepe@intellisrc.com>
 * @param <V>
 *
 * gcInterval  : how many seconds to execute Garbage Collector. Set it to zero to disabled it (for any crazy reason)
 * extend       : if true, it will extend time upon access. If false, it will expire when the time is due (without changing expire value).
 */
class Cache<V> {
    static public final int DISABLED = 0
    static public final int FOREVER = -1
    static protected int defaultGCInterval = Config.any.get("cache.gc", Secs.MIN_2) //seconds
    static protected int defaultTimeout = Config.any.get("cache.timeout", FOREVER) //seconds
    interface NotFound {
        V call()
    }
    static interface CacheAccess {
        void call(String key)
    }
	public int gcInterval = defaultGCInterval
    public int timeout = defaultTimeout
    public boolean extend  = false
    public boolean quiet = false // When true, it won't print read (verbose) log

    /**
     * Object used as data structure inside Cache
     * long expire : stores the time in which will expire
     * length      : time in milliseconds that will be stored (used to extend cache)
     * value       : value to store
     */
    protected static class CacheItem<V> {
        V               value
        LocalDateTime   expire
        protected int     length = 0
        protected boolean forever = false

        CacheItem(V obj, int timeToStoreSec = defaultTimeout) {
            if(timeToStoreSec != 0) { //Do not store if its disabled
                value = obj
                if (timeToStoreSec > 0) {
                    expire = SysClock.dateTime.plusSeconds(timeToStoreSec)
                    length = timeToStoreSec
                } else if (timeToStoreSec == FOREVER) {
                    forever = true
                }
            }
        }
    }
    protected final ConcurrentMap<String, CacheItem<V>> cache = new ConcurrentHashMap<>()

	/**
	 * Constructor
	 */
	Cache() {
        Thread.start {
            while(gcInterval > 0) {
                sleep(gcInterval * Millis.SECOND)
                garbageCollect()
            }
        }
	}
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
    boolean contains(final String key) {
        return cache.containsKey(key) &&! expired(key)
    }
    /**
     * Return true if expired and remove it from cache
     * @param key
     * @return
     */
    boolean expired(final String key) {
        boolean expired = false
        CacheItem<V> obj = cache.get(key)
        if(obj) {
            if(!obj.forever) {
                def now = SysClock.dateTime
                if (obj.expire < now) {
                    Log.v("Key [$key] expired.")
                    del(key)
                    expired = true
                } else {
                    if (extend) {
                        obj.expire = now.plusSeconds(obj.length)
                    }
                }
            }
        }
        return expired
    }
	/**
	 * Returns value if found, or set object if not
	 * @param key
	 * @param default_val
	 * @return
	 */
    V get(final String key, NotFound notFound = null, int time = timeout) {
        return get(key, notFound, null, null, time)
    }
    /**
     * Returns value if found, or set object if not
     * @param key
     * @param notFound
     * @param onHit : Will be called if cache was hit
     * @param onStore : Will be called if item was stored in cache
     * @param time
     * @return
     */
    V get(final String key, NotFound notFound, CacheAccess onHit, CacheAccess onStore = null, int time = timeout) {
        V ret = null
        if(key && time) {
            if (contains(key)) {
                ret = cache.get(key).value
                if(! quiet) {
                    Log.v("[$key] read from cache")
                }
                onHit?.call(key)
            } else {
                if (notFound) {
                    ret = notFound.call()
                    if (ret != null) {
                        Log.v("[$key] added to cache")
                        set(key, ret, onStore, time)
                    }
                }
            }
        } else if(notFound) {
            ret = notFound.call()
        }
        return ret
    }

	/**
	 * Set value to key. If value is null, it will be removed
	 * @param key
	 * @param value 
	 */
    void set(final String key, final V value, CacheAccess onStore = null, int time = timeout) {
		if(value == null) {
			del(key)
		} else {
	        cache.put(key, new CacheItem(value, time))
            onStore?.call(key)
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
     * Remove all keys matching pattern
     * @param match
     * @return
     */
    boolean clear(final String match) {
        return cache.removeAll {
            it.key.contains(match)
        }
    }
    /**
     * Remove all keys matching pattern
     * @param match
     * @return
     */
    boolean clear(final Pattern match) {
        return cache.removeAll {
            it.key.matches(match)
        }
    }
	
	/**
	 * Clear all values
	 */
	boolean clear() {
		cache.clear()
        return cache.isEmpty()
	}

	/**
	 * Return cache size
	 * @return 
	 */
	int size() {
		return cache.size()
	}

    /**
     * Return all keys in cache
     * @return
     */
    List<String> keys() {
        return cache.keySet().toList()
    }

    /**
     * Return values in cache (without modification)
     * @return
     */
    List<V> values() {
        return Collections.unmodifiableList(cache.values().collect { it.value })
    }

    /**
     * Will remove expired elements
     * It is initialized automatically
     */
    void garbageCollect() {
        cache.each {
            String key, CacheItem co ->
                expired(key)
        }
    }
}