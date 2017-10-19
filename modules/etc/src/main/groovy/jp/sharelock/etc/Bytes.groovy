package jp.sharelock.etc

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
/**
 * @since 17/04/11.
 */
@groovy.transform.CompileStatic
class Bytes {
    static final String LOG_TAG = Bytes.simpleName

    /**
     * Converts a char[] to byte[]
     * Based on: http://stackoverflow.com/questions/5513144/
     */
    static byte[] fromChars(char[] chars, String encoding = "UTF8") {
        if(encoding == "UTF8") { encoding = "UTF-8" } //Fix encoding string
        CharBuffer charBuffer = null
        ByteBuffer byteBuffer = null
        byte[] bytes = new byte[0]
        try {
            charBuffer = CharBuffer.wrap(chars)
            byteBuffer = Charset.forName(encoding).encode(charBuffer)
            bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit())
        } catch( Exception e) {
            Log.w(LOG_TAG,"Unable to convert to Bytes: "+encoding+" ("+e+")")
        }
        Arrays.fill(byteBuffer.array(), (byte) 0) // clear sensitive data
        return bytes
    }

    /**
     * Converts a String to byte[]
     * Whenever possible, use char[] instead of String (see notes in crypt.Crypt)
     * @param str
     * @param encoding
     * @return
     */
    static byte[] fromString(String str, String encoding = "UTF8") {
        byte[] bytes = new byte[0]
        try {
            bytes = str.getBytes(encoding)
        } catch (UnsupportedEncodingException e) {
            Log.w(LOG_TAG,"Invalid encoding: "+encoding+" ("+e+")")
        }
        return bytes
    }

    /**
     * Converts HEX String to byte array
     * Based on: http://stackoverflow.com/questions/11208479/
     * @param str
     * @return
     */
    static byte[] fromHex(String str) {
        int len = str.length()
        byte[] data = new byte[len / 2]
        for (int i = 0; i < len; i += 2) {
            int di = (i / 2) as int
            data[di] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i+1), 16))
        }
        return data
    }

     /**
     * Converts byte array to HEX string
     * Based on: http://stackoverflow.com/questions/15429257/
      *          http://stackoverflow.com/questions/9655181/
     * @param str
     * @return
     */
    static String toHex(byte[] bytes) {
        //return String.format("%02x", bytes) <-- this is simplest but slower
        char[] hexArray = "0123456789ABCDEF".toCharArray()
        char[] hexChars = new char[bytes.length * 2]
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF
            hexChars[j * 2] = hexArray[v >>> 4]         //Intellij sees this as an error, but it is not
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]    //Intellij sees this as an error, but it is not
        }
        return new String(hexChars)
    }

    /**
     * Converts a byte array to String
     * Note: prefer toChars if possible
     * @param bytes
     * @param encoding
     * @return
     */
    static String toString(byte[] bytes, String encoding = "UTF8") {
        return new String(bytes, encoding)
    }

    /**
     * Converts byteArray to CharArr
     * Based on: http://stackoverflow.com/questions/4931854/
     * @param bytes
     * @param encoding
     * @return
     */
    static char[] toChars(byte[] bytes, String encoding = "UTF8") {
        if(encoding == "UTF8") { encoding = "UTF-8" } //Fix encoding string
        CharBuffer charBuffer = null
        char[] chars = new char[0]
        try {
            charBuffer = Charset.forName(encoding).decode(ByteBuffer.wrap(bytes))
            chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())
        } catch( Exception e) {
            Log.w(LOG_TAG,"Unable to convert bytes to chars using encoding: "+encoding+" ("+e+")")
        }
        Arrays.fill(charBuffer.array(), (char) 0) // clear sensitive data
        return chars
    }

}
