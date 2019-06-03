package com.intellisrc.core

@groovy.transform.CompileStatic
/**
 * This class provides an easy way to work with .properties files and Properties classes.
 * However, this class can also be used to create properties on memory, without any file:
 * `new Config.Props()`
 *
 * This class can be used as static: `Config.getInt("something")` which is equivalent of
 * `Config.global.getInt("something")` (which reads from "config.properties")
 *
 * Additionally, the 'system' property is set to manage the System.properties,
 * so you can access like: `Config.system.getBool("somekey")`
 *
 * @author Alberto Lepe <lepe@intellisrc.com>
 */
class Config {
    static Props global = new Props(new File(SysInfo.userDir, "config.properties"))
    static Props system = new Props(System.properties)

    /**
     * Static methods will call global instance
     * @param key
     * @return
     */
    static boolean exists()                     { global.exists() }
    static boolean matchKey(String key)         { global.matchKey(key) }
    static boolean hasKey(String key)           { global.hasKey(key) }
    static String get(String key)               { global.get(key) }
    static int getInt(String key)               { global.getInt(key) }
    static double getDbl(String key)            { global.getDbl(key) }
    static boolean getBool(String key)          { global.getBool(key) }
    static File getFile(String key)             { global.getFile(key) }
    static void set(String key, Object value)   { global.set(key, value) }

    /**
     * Prevent instantiating this class
     */
    private Config() {}
    /**
     * CfgFile can be used with any file
     */
    static class Props {
        final Properties properties
        final File configFile

        /**
         * Constructor to create new properties on memory
         */
        Props() {
            properties = new Properties()
        }
        /**
         * Initialize with properties
         * @param props
         */
        Props(Properties props) {
            properties = props
        }
        /**
         * Constructor using File
         * @param cfgFile
         * @param props
         */
        Props(File cfgFile, Properties props = null) {
            configFile = cfgFile
            properties = props ?: new Properties()
        }
        /**
         * Constructor using only configuration name: new Config("database")
         * @param configName
         * @param props
         */
        Props(String configName, Properties props = null) {
            configFile = new File(SysInfo.userDir + configName + ".properties")
            properties = props ?: new Properties()
        }

        /**
         * Reload properties from file (if exists)
         */
        void update() {
            if(configFile) {
                if (configFile.exists()) {
                    if (configFile.canRead()) {
                        properties.load(configFile.newDataInputStream())
                    }
                    if (!configFile.canWrite()) {
                        Log.w("Configuration configFile: " + configFile.toString() + " is not writable. Any attempt to change settings will fail.")
                    }
                } else {
                    Log.v("Configuration not found (" + configFile.toString() + "). All settings will be loaded from System")
                }
            }
        }

        /**
         * Returns true if config file exists
         * @return
         */
        boolean exists() {
            return configFile && configFile.exists()
        }

        /**
         * Returns true if any key starts with some string
         * @param key
         * @return
         */
        boolean matchKey(String key) {
            update()
            properties.keys().find {
                String pkey ->
                    pkey.startsWith(key)
            }
        }

        /**
         *
         * @param key
         * @return
         */
        boolean hasKey(String key) {
            update()
            return properties.containsKey(key)
        }

        /**
         * Get value as String
         * @param key
         * @return
         */
        String get(String key) {
            update()
            String val = ""
            if (hasKey(key)) {
                val = properties.getProperty(key)
                if (val == null) {
                    val = ""
                }
                if (val.startsWith('"') || val.startsWith("'")) {
                    Log.w("Settings in properties files don't need quotes. Please delete them to remove this warning.")
                    val = val.replaceAll(/^["']|["']$/, '')
                }
            }
            return val
        }

        /**
         * Get value as int
         * @param key
         * @return
         */
        int getInt(String key) {
            String str = get(key)
            if (str.isEmpty()) {
                str = "0"
            }
            return Integer.parseInt(str)
        }

        /**
         * Get value as double
         * @param key
         * @return
         */
        double getDbl(String key) {
            String str = get(key)
            if (str.isEmpty()) {
                str = "0.0"
            }
            return Double.parseDouble(str)
        }

        /**
         * Get value as boolean
         * @param key
         * @return
         */
        boolean getBool(String key) {
            boolean val = true
            String str = get(key)
            if (str == null || str.isEmpty() || str == "0" || str.toLowerCase() == "false") {
                val = false
            }
            return val
        }

        /**
         * Get File from path
         * @param key
         * @return
         */
        File getFile(String key) {
            File pathFile = null
            String path = get(key)
            if(path) {
                switch (path) {
                    case ~/^\/.*$/:
                        pathFile = new File(path)
                        break
                    case ~/^~.*$/:
                        pathFile = new File(SysInfo.homeDir, path.replace('~/', ''))
                        break
                    default:
                        pathFile = new File(SysInfo.userDir, path)
                }
            }
            return pathFile
        }

        /**
         * Set value in properties
         * @param key
         * @param value
         */
        void set(String key, Object value) {
            update()
            properties.setProperty(key, value.toString())
            if(configFile) {
                if (configFile.parentFile.canWrite()) {
                    properties.store(configFile.newWriter(), null)
                } else {
                    Log.e("Unable to write to: " + configFile.toString())
                }
            }
        }
    }
}