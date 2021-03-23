package com.intellisrc.core

import groovy.transform.CompileStatic

import java.nio.charset.Charset

@CompileStatic
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
    static public Props global = new Props(new File(SysInfo.userDir, "config.properties"))
    static public Props system = new Props(System.properties)

    /**
     * Static methods will call global instance
     * @param key
     * @return
     */
    static boolean exists()                             { global.exists() }
    static boolean matchKey(String key)                 { global.matchKey(key) }
    static boolean hasKey(String key)                   { global.hasKey(key) }

    static String get(String key, String defval = "")           { global.get(key, defval) }
    static int getInt(String key, int defval = 0)               { global.getInt(key, defval) }
    static double getDbl(String key, double defval = 0)         { global.getDbl(key, defval) }
    static float getFloat(String key, float defval = 0)         { global.getFloat(key, defval) }
    static boolean getBool(String key, boolean defval = false)  { global.getBool(key, defval) }
    static File getFile(String key, File defval = null)         { global.getFile(key, defval) }
    static List<String> getList(String key, List<String> defval = []) { global.getList(key, defval) }
    static List<String> getList(String key, CharSequence separator, List<String> defval = []) { global.getList(key, separator, defval) }

    static void set(String key, Object value)                   { global.set(key, value) }

    /**
     * Prevent instantiating this class
     */
    private Config() {}
    /**
     * CfgFile can be used with any file
     */
    static class Props {
        public final Properties props
        public File configFile = null

        /**
         * Constructor to create new properties on memory
         */
        Props() {
            props = new Properties()
        }
        /**
         * Initialize with properties
         * @param props
         */
        Props(Properties props) {
            this.props = props
        }
        /**
         * Constructor using File
         * @param cfgFile
         * @param props
         */
        Props(File cfgFile, Properties props = null) {
            configFile = cfgFile
            this.props = props ?: new Properties()
            update()
        }
        /**
         * Constructor using only configuration name: new Config("database")
         * @param configName
         * @param props
         */
        Props(String configName, Properties props = null) {
            configFile = SysInfo.getFile(configName + ".properties")
            this.props = props ?: new Properties()
            update()
        }

        /**
         * Reload properties from file (if exists)
         */
        void update() {
            if(configFile) {
                if (configFile.exists()) {
                    if (configFile.canRead()) {
                        FileInputStream input = new FileInputStream(configFile)
                        props.load(new InputStreamReader(input, Charset.forName("UTF-8")))
                        input.close()
                    }
                    if (!configFile.canWrite()) {
                        Log.w("Configuration configFile: " + configFile.toString() + " is not writable. Any attempt to change settings will fail.")
                    }
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
        boolean hasKey(String key) {
            update()
            return props.containsKey(key)
        }

        /**
         * Get value as String
         * @param key
         * @return
         */
        String get(String key, String defval = "") {
            update()
            String val = defval
            if (hasKey(key)) {
                val = props.getProperty(key)
                if (val == null) {
                    val = defval
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
        int getInt(String key, int defval = 0) {
            int val = defval
            if(hasKey(key)) {
                String str = get(key)
                if (! str.empty) {
                    val = Integer.parseInt(str)
                }
            }
            return val
        }

        /**
         * Get value as double
         * @param key
         * @return
         */
        float getFloat(String key, float defval = 0) {
            float val = defval
            if(hasKey(key)) {
                String str = get(key)
                if (! str.empty) {
                    val = Float.parseFloat(str)
                }
            }
            return val
        }

        /**
         * Get value as double
         * @param key
         * @return
         */
        double getDbl(String key, double defval = 0) {
            double val = defval
            if(hasKey(key)) {
                String str = get(key)
                if (! str.empty) {
                    val = Double.parseDouble(str)
                }
            }
            return val
        }

        /**
         * Get value as boolean
         * @param key
         * @return
         */
        boolean getBool(String key, boolean defval = false) {
            boolean val = defval
            if(hasKey(key)) {
                String str = get(key)
                val = !(str == null || str.empty || str == "0" || str.toLowerCase() == "false")
            }
            return val
        }

        /**
         * Get File from path
         * @param key
         * @return
         */
        File getFile(String key, File defval = null) {
            File pathFile = defval
            if(hasKey(key)) {
                String path = get(key)
                if (path) {
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
            }
            return pathFile
        }

        /**
         * Return a list separated by custom separator
         * @param key
         * @param defval
         * @return
         */
        List<String> getList(String key, CharSequence separator, List<String> defval = []) {
            List<String> list
            if(hasKey(key)) {
                list = get(key).tokenize(separator)
            } else {
                list = defval
            }
            return list
        }
        /**
         * Return a list separated by comma
         * @param key
         * @param defval
         * @return
         */
        List<String> getList(String key, List<String> defval = []) {
            return getList(key, ",", defval)
        }

        /**
         * Set value in properties
         * @param key
         * @param value
         */
        void set(String key, Object value) {
            update()
            props.setProperty(key, value.toString())
            if(configFile) {
                if (configFile.parentFile.canWrite()) {
                    props.store(configFile.newWriter(), null)
                } else {
                    Log.e("Unable to write to: " + configFile.toString())
                }
            }
        }
    }
}