package com.intellisrc.core

import com.intellisrc.core.props.AnyProperties
import com.intellisrc.core.props.EnvironmentProperties
import com.intellisrc.core.props.StringPropertiesYaml
import groovy.transform.CompileStatic

import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
@CompileStatic
class Config {
    static public Props global = new Props(new File(File.userDir, "config.properties"))
    static public Props system = new Props(System.properties)
    static public final Props sys = system //Alias
    static public EnvironmentProperties env = new EnvironmentProperties()
    static public AnyProperties any = new AnyProperties()

    /**
     * Static methods will call global instance
     * @param key
     * @return
     */
    static void reload()                                { global.reload() }
    static boolean exists()                             { global.exists() }
    static boolean matchKey(String key)                 { global.matchKey(key) }
    static boolean exists(String key)                   { global.exists(key) }

    static String get(String key, String defVal = "")           { global.get(key, defVal) }
    static boolean getBool(String key)                          { global.getBool(key) }
    static boolean get(String key, boolean defVal)              { global.get(key, defVal) }
    static short getShort(String key)                           { global.getShort(key) }
    static short get(String key, short defVal)                  { global.get(key, defVal) }
    static int getInt(String key)                               { global.getInt(key) }
    static int get(String key, int defVal)                      { global.get(key, defVal) }
    static long getLong(String key)                             { global.getLong(key) }
    static long get(String key, long defVal)                    { global.get(key, defVal) }
    static float getFloat(String key)                           { global.getFloat(key) }
    static float get(String key, float defVal)                  { global.get(key, defVal) }
    static double getDbl(String key)                            { global.getDbl(key) }
    static double get(String key, double defVal)                { global.get(key, defVal) }
    static BigInteger getBigInt(String key)                     { global.getBigInt(key) }
    static BigInteger get(String key, BigInteger defVal)        { global.get(key, defVal) }
    static BigDecimal getBigDec(String key)                     { global.getBigDec(key) }
    static BigDecimal get(String key, BigDecimal defVal)        { global.get(key, defVal) }
    static Enum get(String key, Enum defVal)                    { global.get(key, defVal) }

    static File getFile(String key, String defVal)              { global.getFile(key, defVal) }
    static File getFile(String key, File defVal)                { global.getFile(key, defVal) }
    static File get(String key, File defVal)                    { global.getFile(key, defVal) }
    static Optional<File> getFile(String key)                   { global.getFile(key) }

    static Optional<URI> getURI(String key)                     { global.getURI(key) }
    static Optional<URL> getURL(String key)                     { global.getURL(key) }
    static Optional<LocalTime> getTime(String key)              { global.getTime(key) }
    static Optional<LocalDate> getDate(String key)              { global.getDate(key) }
    static Optional<LocalDateTime> getDateTime(String key)      { global.getDateTime(key) }
    static Optional<Inet4Address> getInet4(String key)          { global.getInet4(key) }
    static Optional<Inet6Address> getInet6(String key)          { global.getInet6(key) }
    static Optional<byte[]> getBytes(String key)                { global.getBytes(key) }
    static Optional<Enum> getEnum(String key, Class<Enum> cls)  { global.getEnum(key, cls) }
    static Optional<Enum> get(String key, Class<Enum> cls)      { global.getEnum(key, cls) }

    static List getList(String key)                     { global.getList(key) }
    static List get(String key, List defVal)            { global.get(key, defVal) }
    static Set getSet(String key)                       { global.getSet(key) }
    static Set get(String key, Set defVal)              { global.get(key, defVal) }

    static Map getMap(String key)                       { global.getMap(key) }
    static Map get(String key, Map defVal)              { global.get(key, defVal) }

    static boolean setObj(String key, Object value)     { global.setObj(key, value) }
    static boolean set(String key, boolean val)         { global.set(key, val) }
    static boolean set(String key, String val)          { global.set(key, val) }
    static boolean set(String key, Number val)          { global.set(key, val) }
    static boolean set(String key, byte[] val)          { global.set(key, val) }
    static boolean set(String key, InetAddress val)     { global.set(key, val) }
    static boolean set(String key, File val)            { global.set(key, val) }
    static boolean set(String key, URI val)             { global.set(key, val) }
    static boolean set(String key, URL val)             { global.set(key, val) }
    static boolean set(String key, LocalTime val)       { global.set(key, val) }
    static boolean set(String key, LocalDate val)       { global.set(key, val) }
    static boolean set(String key, LocalDateTime val)   { global.set(key, val) }
    static boolean set(String key, Collection val)      { global.set(key, val) }
    static boolean set(String key, Map val)             { global.set(key, val) }
    static boolean set(String key, Enum val)            { global.set(key, val) }

    static boolean delete(String key) { return global.delete(key) }
    static boolean clear() {  return global.clear() }
    static Set<String> getKeys() { return global.keys }

    /**
     * Prevent instantiating this class
     */
    private Config() {}
    /**
     * CfgFile can be used with any file
     */
    static class Props extends StringPropertiesYaml {
        public final Properties props
        public File configFile = null
        protected boolean loaded = false

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
            configFile = File.get(configName + ".properties")
            this.props = props ?: new Properties()
            update()
        }

        /**
         * Reload properties from file (if exists)
         */
        void update() {
            if(configFile &&! loaded) {
                if (configFile.exists()) {
                    if (configFile.canRead()) {
                        FileInputStream input = new FileInputStream(configFile)
                        props.load(new InputStreamReader(input, Charset.forName("UTF-8")))
                        input.close()
                        loaded = true
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
         * Alias
         * @param key
         * @return
         */
        @Override
        boolean exists(String key) {
            update()
            return props.containsKey(key)
        }
        /**
         * Get value as String
         * @param key
         * @return
         */
        @Override
        String get(String key, String defVal) {
            update()
            String val = defVal
            if (exists(key)) {
                val = props.getProperty(key)
                if (val == null) {
                    val = defVal
                }
                if (val.startsWith('"') || val.startsWith("'")) {
                    Log.w("Settings in properties files don't need quotes. Please delete them to remove this warning.")
                    val = val.replaceAll(/^["']|["']$/, '')
                }
            }
            return val
        }
        /**
         * Set value in properties
         * @param key
         * @param value
         */
        @Override
        boolean set(String key, String value) {
            boolean ok = false
            // Load from file values to prevent loosing changes done directly
            update()
            // Set property value
            props.setProperty(key, value)
            // Save it
            if(configFile) {
                if (configFile.parentFile.canWrite()) {
                    props.store(configFile.newWriter(), null)
                    ok = true
                } else {
                    Log.e("Unable to write to: " + configFile.toString())
                }
            }
            return ok
        }
        /**
         * In case of properties, we will try to store it relative
         * @param key
         * @param val
         * @return
         */
        @Override
        boolean delete(String key) {
            return props.remove(key)
        }
        @Override
        boolean clear() {
            return configFile.delete() //fastest way
        }
        @Override
        Set<String> getKeys() {
            return props.keys().collect { it.toString() }.toSet()
        }
        /**
         * Reload configuration
         */
        void reload() {
            loaded = false
            update()
        }
    }
}