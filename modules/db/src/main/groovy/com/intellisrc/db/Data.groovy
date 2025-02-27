package com.intellisrc.db

import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.*

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
    /**
     * Add data
     * @param data
     */
    Data(Collection<Map> data) {
        this.data = data.toList()
    }
    /**
     * Alternative as Map
     * @param data
     */
    Data(Map data) {
        this.data = [data]
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
                val = toBoolean(getFirstElement(map), null)
            }
        }
        return val
    }

    /**
     * Converts object to boolean
     * @param object
     * NOTE: passing handler = null will convert generic strings/values to boolean
     * @return
     */
    static boolean toBoolean(Object object, JDBC.BooleanHandle handler) {
        return switch (handler) {
            case BOOLEAN, ENUM -> object.toString().trim().toLowerCase() == "true"
            case NUMBER     -> object.toString().trim().isNumber() && parseInt(object.toString()) == 1
            case CHAR       -> object.toString().trim().toLowerCase() == "y"
            // Generic conversion (use null)
            default         -> ["y","t","on","1","true"].contains(object.toString().trim().toLowerCase())
        }
    }
    /**
     * Converts boolean to numeric
     * @param object
     * @return
     */
    static int booleanAsInt(boolean value) {
        return value ? 1 : 0
    }
    /**
     * Converts boolean to char
     * @param object
     * @return
     */
    static char booleanAsChar(boolean value) {
        return (value ? 'y' : 'n') as char
    }
    /**
     * Convert boolean to representation in database
     * @param object
     * @param handler
     * @return
     */
    static Object booleanToValue(boolean value, JDBC.BooleanHandle handler) {
        return switch (handler) {
            case BOOLEAN -> value
            case NUMBER -> booleanAsInt(value)
            case CHAR -> booleanAsChar(value)
            case ENUM -> value.toString().toUpperCase()
        }
    }
    /**
     * Returns true if Object is an int (that includes numbers like: 1.00)
     * @param object
     * @return
     */
    static boolean isInt(Object object) {
        return object.toString().matches(/^\d+(\.0+)?$/)
    }
    /**
     * Parse object as int. It will throw and exception if it is not an int
     * Integer.parseInt() fails for numbers like: "0.0"
     * @param object
     * @return
     */
    static int parseInt(Object object) throws AssertionError {
        assert isInt(object) : "${object.toString()} is not an int"
        return object.toString().toBigDecimal().intValue()
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
