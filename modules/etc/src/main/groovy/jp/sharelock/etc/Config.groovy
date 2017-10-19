package jp.sharelock.etc

@groovy.transform.CompileStatic
/**
 *
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class Config {
    private static final String LOG_TAG = Config.getSimpleName()
	static String fileName = "config.properties"
	/**
	 * Get Stream depending on platform
	 * @return 
	 */
	private static InputStream getInputStream(String fn) {
		InputStream inputStream = null
		try {
			inputStream = new FileInputStream(fn)
		} catch (FileNotFoundException ex) {
			Log.e(LOG_TAG, "Unable to open file: "+fn+". "+ex)
		}
		return inputStream
	}

		private static OutputStream getOutputStream(String fn) {
		OutputStream ouputStream = null
		try {
			ouputStream = new FileOutputStream(fn)
		} catch (FileNotFoundException ex) {
			Log.e(LOG_TAG, "Unable to open file: "+fn+". "+ex)
		}
		return ouputStream
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
	 * Get value as int from a specific file
	 * @param fn
	 * @param key
	 * @return 
	 */
	static int getInt(String fn, String key) {
		String str = get(fn, key)
		if(str.isEmpty()) {
			str = "0"
		}
		return Integer.parseInt(str)
	}
	
	/**
	 * using Default filename
	 * @param key
	 * @return 
	 */
	static String get(String key) {
		return get(fileName, key)
	}
	/**
	 * Get value as String
	 * @param fn : filename
	 * @param key
	 * @return 
	 */
	static String get(String fn, String key) {
		Properties prop = new Properties()
		String value = ""
		InputStream inputStream = getInputStream(fn)
		if (inputStream != null) {
			try {
				prop.load(inputStream)
				value = prop.getProperty(key)
				inputStream.close()
			} catch (IOException ex) {
				Log.e(LOG_TAG, "Getting config key failed: "+ex)
			}
		}
		return value
	}

	/**
	 * Usign Default filename
	 * @param key
	 * @param value 
	 */
	static void set(String key, Object value) {
		set(fileName, key, value)
	}
	/**
	 * Set value in properties
	 * @param fn : filename
	 * @param key
	 * @param value 
	 */
	static void set(String fn, String key, Object value) {
		Properties prop = new Properties()
		OutputStream ouputStream = getOutputStream(fn)
		InputStream inputStream = getInputStream(fn)
		if (ouputStream != null) {
			try {
				prop.load(inputStream)
				inputStream.close()
				prop.setProperty(key, value.toString())
				prop.store(ouputStream, null)
				ouputStream.close()
			} catch (IOException ex) {
				Log.e(LOG_TAG, "Setting config key failed : "+ex)
			}
		}
	}
}