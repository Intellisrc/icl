package com.intellisrc.db

import groovy.transform.CompileStatic

@CompileStatic
/**
 * The goal of this class is to
 * convert the Database result into
 * something more useful
 *
 * @author A.Lepe
 */
class Data {
    private List<Map> data = []

    Data(List<Map> data) {
        this.data = data
    }
    /**
     * Return true if row was not found
     * @return
     */
    boolean isEmpty() {
        return this.data.isEmpty()
    }
    /**
     * Return true if row was found and the first column of the first row is NOT empty
     * This method is for testing a single column value
     * @return
     */
    boolean hasValue() {
        boolean has = false
        if(!data.isEmpty()) {
            Map map = data.get(0)
            if(!map.isEmpty()) {
                Object value = getFirstElement(map)
                has = value != null &&! value.toString().empty
            }
        }
        return has
    }
    /**
     * Return first column of the first row as Boolean
     * This method will try different options for "true",
     * like "yes", "1", "on", etc
     *
     * NOTE: non-empty values are not automatically translated into "true",
     * if you want ot test if a column has or not any value, use: 'hasValue'
     * @return
     */
    boolean toBool() {
        boolean val = false
        if(!data.isEmpty()) {
            Map map = data.get(0)
            if(!map.isEmpty()) {
                val = ["true","on","yes","1","t","y"].contains(getFirstElement(map)?.toString()?.toLowerCase())
            }
        }
        return val
    }
    /**
     * Returns the first column of the first row as String
     * @return
     */
	@Override
    String toString() {
        String str = ""
        if(!data.isEmpty()) {
            Map map = data.get(0)
            if(!map.isEmpty()) {
                str = (getFirstElement(map) ?: "").toString()
            }
        }
        return str
    }
    /**
     * Returns the first column of the first row as int
     * @return
     */
    Integer toInt() {
        int val = 0
        if(!data.isEmpty()) {
            Map map = data.get(0)
            if(!map.isEmpty()) {
				Object o = getFirstElement(map)
                switch(o) {
				    case Long:
    					val = ((Long) o).intValue()
                        break
                    case Double:
					    val = ((Double) o).intValue()
                        break
				    default:
    	                val = (o ?: 0) as int
				}
            }
        }
        return val
    }
    /**
     * Returns the first column of the first row as float
     * @return
     */
    Float toFloat() {
        Float val = 0
        if(!data.isEmpty()) {
            Map map = data.get(0)
            if(!map.isEmpty()) {
                val = (getFirstElement(map) ?: 0) as float
            }
        }
        return val
    }
    /**
     * Returns the first column of the first row as double
     * @return
     */
    Double toDbl() {
        Double val = 0
        if(!data.isEmpty()) {
            Map map = data.get(0)
            if(!map.isEmpty()) {
                val = (getFirstElement(map) ?: 0) as double
            }
        }
        return val
    }
    /**
     * Returns the first element of each row
     * @return
     */
    List toList() {
        List al = []
        if(!data.isEmpty()) {
            al = data.collect {
                it.values().first()
            }
        }
        return al as List
    }
    /**
     * Returns the first row as Map
     * @return
     */
    Map toMap() {
        Map map = [:]
        if(!data.isEmpty()) {
            map = data.get(0)
        }
        return map
    }

    /**
     * Returns all rows as Map
     * @return
     */
    List<Map> toListMap() {
        return data
    }

    /**
     * Returns the first element of a Map
     * @param map
     * @return
     */
    private static Object getFirstElement(Map map) {
        Iterator it = map.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next()
            it.remove() // avoids a ConcurrentModificationException
            return pair.value
        }
        return null
    }
}
