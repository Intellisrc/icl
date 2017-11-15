package jp.sharelock.etc

@groovy.transform.CompileStatic
/**
 * This class will combine System Properties and config.properties into a
 * single configuration. Multiple methods are defined to handle it easier
 *
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class Config {
	static String fileName = "config.properties"
    static String filePath = SysInfo.getUserDir()
    static Properties props = System.getProperties()
    private static Properties config = new Properties()
    private static File configFile

    /**
     * Be sure filePath ends with File Separator
     * @param fp
     */
    static void setFilePath(String fp) {
        if(!fp.endsWith(File.separator)) {
            fp += File.separator
        }
        filePath = fp
    }

    static update() {
        configFile = new File(filePath + fileName)
        if(configFile.exists()) {
            if(configFile.canRead()) {
                config.load(configFile.newDataInputStream())
                config.keys().each {
                    String key ->
                        props.setProperty(key, config.getProperty(key))
                }
            }
            if(!configFile.canWrite()) {
                Log.w( "Configuration configFile: "+configFile.toString()+" is not writable. Any attempt to change settings will fail.")
            }
        } else {
            Log.d( "Configuration not being used ("+configFile.toString()+"). All settings will be loaded from System")
        }
    }

    /**
     * Returns true if config file exists
     * @return
     */
    static boolean exists() {
        update()
        return configFile.exists()
    }

    /**
     * Returns true if any key starts with some string
     * @param key
     * @return
     */
    static boolean matchKey(String key) {
        update()
        props.keys().find {
            String pkey ->
                pkey.startsWith(key)
        }
    }

    /**
     *
     * @param key
     * @return
     */
    static boolean hasKey(String key) {
        update()
        return props.containsKey(key)
    }

    /**
     * Prevent updating object
     */
    static void setProps() {
        //Do nothing.
        Log.w( "Properties can not be updated directly. Use set() instead.")
    }

	/**
	 * Get value as String
	 * @param key
	 * @return
	 */
	static String get(String key) {
        update()
        String val = config.getProperty(key)
        if(val == null) { val = "" }
        if(val.startsWith('"') || val.startsWith("'")) {
            Log.w( "Settings in properties files don't need quotes. Please delete them to remove this warning.")
            val = val.replaceAll(/^["']|["']$/,'')
        }
        return val
	}

    /**
     * Get value as int
     * @param key
     * @return
     */
    static int getInt(String key) {
        String str = get(key)
        if(str.isEmpty()) {
            str = "0"
        }
        return Integer.parseInt(str)
    }

    /**
     * Get value as boolean
     * @param key
     * @return
     */
    static boolean getBool(String key) {
        boolean val = true
        String str = get(key)
        if(str == null || str.isEmpty() || str == "0" || str.toLowerCase() == "false") {
            val = false
        }
        return val
    }

	/**
	 * Set value in properties
	 * @param key
	 * @param value 
	 */
	static void set(String key, Object value) {
        update()
        config.setProperty(key, value.toString())
        if(configFile.canWrite()) {
            config.store(configFile.newWriter(), null)
        } else {
            Log.e( "Unable to write to: "+configFile.toString())
        }
	}

}