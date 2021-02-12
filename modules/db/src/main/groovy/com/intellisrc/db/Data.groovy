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

    boolean isEmpty() {
        return this.data.isEmpty()
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
                str = (String) getFirstElement(map)
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
    	                val = (Integer) o
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
                val = (Float) getFirstElement(map)
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
                val = (Double) getFirstElement(map)
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
