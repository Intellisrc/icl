package com.intellisrc.db

@groovy.transform.CompileStatic
/**
 * The goal of this class is to
 * convert the Database result into
 * something more useful
 *
 * //TODO: documentation
 *
 * @author A.Lepe
 */
class Data {
    private List<Map> data = []

    Data() {}
    Data(List<Map> data) {
        this.data = data
    }

    boolean isEmpty() {
        return this.data.isEmpty()
    }

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
    // Will return the first element of each row
    List toList() {
        List al = []
        if(!data.isEmpty()) {
            al = data.collect {
                it.values().first()
            }
        }
        return al as List
    }
    // It will return the first row as Hash
    Map toMap() {
        Map map = [:]
        if(!data.isEmpty()) {
            map = data.get(0)
        }
        return map
    }

    List<Map> toListMap() {
        return data
    }

    void addRow(Map row) {
        data.add(row)
    }

    private Object getFirstElement(Map map) {
        Iterator it = map.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next()
            it.remove() // avoids a ConcurrentModificationException
            return pair.value
        }
        return null
    }
}
