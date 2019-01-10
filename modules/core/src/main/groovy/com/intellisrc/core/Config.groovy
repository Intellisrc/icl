package com.intellisrc.core

@groovy.transform.CompileStatic
/**
 * This class will combine System Properties and config.properties into a
 * single configuration. Multiple methods are defined to handle it easier
 *
 * @author Alberto Lepe <lepe@intellisrc.com>
 */
class Config {
    static private CfgFile global = new CfgFile(new File(SysInfo.userDir, "config.properties"))
    static CfgFile system = new CfgFile(null, System.properties)

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
    static void set(String key, Object value)   { global.set(key, value) }

    /**
     * CfgFile can be used with any file
     */
    static class CfgFile {
        private final Properties config
        private final File configFile
        private boolean useFile = false

        CfgFile(File cfgFile = null, Properties props = null) {
            if(cfgFile) {
                configFile = cfgFile
                useFile = true
            }
            config = props ?: new Properties()
        }

        void update() {
            if(useFile) {
                if (configFile.exists()) {
                    if (configFile.canRead()) {
                        config.load(configFile.newDataInputStream())
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
            return useFile && configFile.exists()
        }

        /**
         * Returns true if any key starts with some string
         * @param key
         * @return
         */
        boolean matchKey(String key) {
            update()
            config.keys().find {
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
            return config.containsKey(key)
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
                val = config.getProperty(key)
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
         * Set value in properties
         * @param key
         * @param value
         */
        void set(String key, Object value) {
            update()
            config.setProperty(key, value.toString())
            if(useFile) {
                if (configFile.parentFile.canWrite()) {
                    config.store(configFile.newWriter(), null)
                } else {
                    Log.e("Unable to write to: " + configFile.toString())
                }
            }
        }
    }
}