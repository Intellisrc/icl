package jp.sharelock.db

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
    private ArrayList<HashMap> data

    Data() {}
    Data(ArrayList<HashMap> data) {
        this.data = data
    }

    boolean isEmpty() {
        return this.data.isEmpty()
    }

	@Override
    String toString() {
        String str = ""
        if(!data.isEmpty()) {
            HashMap map = data.get(0)
            if(!map.isEmpty()) {
                str = (String) getFirstElement(map)
            }
        }
        return str
    }
    Integer toInt() {
        int val = 0
        if(!data.isEmpty()) {
            HashMap map = data.get(0)
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
            HashMap map = data.get(0)
            if(!map.isEmpty()) {
                val = (Double) getFirstElement(map)
            }
        }
        return val
    }	
    // Will return the first element of each row
    ArrayList toArray() {
        ArrayList al = new ArrayList()
        if(!data.isEmpty()) {
            HashMap map = data.get(0)
            if(!map.isEmpty()) {
                al.add(getFirstElement(map))
            }
        }
        return al
    }
    // It will return the first row as Hash
    HashMap toHash() {
        HashMap map = new HashMap()
        if(!data.isEmpty()) {
            map = data.get(0)
        }
        return map
    }

    ArrayList<HashMap> toArrHash() {
        return data
    }

    void addRow(HashMap row) {
        data.add(row)
    }

    private Object getFirstElement(HashMap map) {
        Iterator it = map.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next()
            it.remove() // avoids a ConcurrentModificationException
            return pair.getValue()
        }
        return null
    }
}
