package com.intellisrc.crypt.encode

import com.intellisrc.crypt.hash.Hash
import groovy.transform.CompileStatic

import static com.intellisrc.crypt.encode.LpCode.Charset.*

/*
 * @author: A.Lepe
 * @since: Sep 10,2010 (Originally created in PHP for Yayahuic Framework)
 *
 * This class provides 2-way string encoding as a way of obfuscation.
 * While it doesn't really provide security, it could be used to
 * create confusion or to reduce the storage size of a string.
 *
 * Features:
 * - Use of uncommon methods to encode/decode strings
 * - a salt key can be used to change output
 * - All the range of UTF-8 can be used (65k+ chars).
 * - Depending on the settings, the encoded string may
 *   result smaller than the length of the original string.
 * - Many possible combinations (input, output).
 *
 * In order to decode/decode a string 2 settings are required:
 *
 * 1) INPUT: Is the expected format of the string to encode (Default: ASCII).
 *
 * Closest the charset to the input, the smaller is the resulted encoded string.
 * For example, if only numbers are to being encoded, use NUM.
 * If the string to be encoded contains one or more characters not presented
 *      in the selected charset, the result will be garbage.
 * If the input string is a hash (MD5, SHA*, etc), use "HASH".
 * If a string is the result of an encoding method such as base64, unpack('H*',...) ]
 *      it is recommended to use "BASE64".
 * Crypt hashes (created by an standard Unix DES-based algorithm) may contain any ascii
 *      character when using salt strings, as those are stored within the hash itself.
 *      For that reason, use ASCII as input.
 * Even for the same string, different "INPUT" methods may result in different
 * encoded strings.
 *
 * 2) OUTPUT: Is the expected format AFTER being encoded (Default: ANUM).
 *
 * Wider the charset used, the smaller is the resulted encoded string.
 * Using the full UTF8 range (65k+) results in the highest entropy, but
 *      it may present some problems handling the resulted string.
 * Some visually interesting outputs are BRAILLE and LINES.
 * After UTF8, KANJI or KOR provides the wider ranges, giving a higher entropy.
 * HIDE may provide an extra difficulty to read as are not visual characters.
 * ALPHA,ANUM,HASH,LCASE,UCASE,NUM are safe to store (as in a DB) without worrying about
 * escaping chars or having problems with language encoding.
 *
 * 3) KEY: used to randomize output (any UTF-8 char is possible). It is optional.
 */
@CompileStatic
class LpCode {
    static enum Charset {
        BIT,    //Binary-like (2 chars: 0,1)
        NUM,    //0-9 (10 digits)
        // English:
        LCASE,  //a-z (26 chars)
        UCASE,  //A-Z (26 chars)
        ALPHA,  //a-zA-Z (52 chars)
        ANUM,   //0-9a-zA-Z (62 chars)
        ASCII,  //Any English ascii char (95 chars)
        LATIN,  //Ascii Extended (support for accents and other common symbols) (928 chars)
        UTF8,   //Any UTF8 char (65,535 chars)
        // Encoding
        HASH,   //0-9a-f //similar to md5, sha, etc (but with variable length) (16 chars)
        HASHUC, //0-9A-F //same as 'HASH' but uppercase (16 chars)
        BASE64, //0-9a-zA-Z=+/ (as result of Base64,etc) (65 chars)
        // Non-latin Languages
        GREEK,
        CYRILLIC,
        ARMENIAN,
        HEBREW,
        ARABIC,
        SYRIAC,
        THAANA,
        NKO,
        SAMARITAN,
        MANDAIC,
        DEVANAGARI,
        BENGALI,
        GURMUKHI,
        GUJARATI,
        ORIYA,
        TAMIL,
        TELEGU,
        KANNADA,
        MALAYAM,
        SINHALA,
        THAI,
        LAO,
        TIBETAN,
        MYANMAR,
        GEORGIAN,
        HANGUL,
        ETHIOPIC,
        CHEROKEE,
        UCAS,
        OGHAM,
        RUNIC,
        TAGALOG,
        HANUNOO,
        BUHID,
        TAGBANWA,
        KHMER,
        MONGOLIAN,
        LIMBU,
        TAILE,
        BUGINESE,
        TAITHAM,
        BALINESE,
        SUNDANESE,
        BATAK,
        LEPCHA,
        OICHIKI,
        COPTIC,
        GLAGOLITIC,
        TIFINAGH,
        HANZU,  //Chinese (inc. Japanese Kanji, 20,911 symbols)
        KOR,    //Korean (11,171 symbols)
        HIRA,   //Hiragana (83 chars)
        KANA,   //Katakana (86 chars)
        KANJI,  //Japanese Kanji (2131 symbols)
        VAI,
        BAMUM,
        JAVA,
        CHAM,
        TAIVIET,
        // Symbols and Other languages
        ENCLOSED,
        SUBSUP,
        CURRENCY,
        NUMFORM,
        ARROWS,
        DINGBATS,
        BRAILLE,   //Only Braille (255 chars)
        SYMBL,  //Symbols (2,031 symbols)
        TECH,
        LINES,  //Lines (64 symbols)
        BOX,
        BLOCK,
        SHAPES,
        HIDE,   //Non-displayable (use with caution) (2,047 symbols)
        PRIVATE,
    }

    static class Limits {
        int low
        int max
    }

    private Charset input
    private Charset output
    private String seed //seed //TODO: convert to char[]

    LpCode(Charset input = ASCII, Charset output = ANUM, char[] seed) {
        this.input = input
        this.output = output
        this.seed = seed.length > 0 ? Hash.MD5(seed) : ""
    }
    char[] encode(char[] str) {
        Limits limits = getLM(input)
        limits.max++
        BigInteger num = toNum(fixStr(str,true,false), limits)
        if(seed) {
            /*srand(hexdec(substr(s,5,10))+strlen($num))
            $num = implode("",arr_shuffle(str_split($num)))
            srand() //restore randomness*/
        }
        limits.max--
        limits = getLM(output)
        return fixStr(toStr(num, limits),false,true)
    }
    char[] decode(char[] str) {
        Limits limits = getLM(output)
        BigInteger num = toNum(fixStr(str,false,false), limits)
        if(seed) {
            /*srand(hexdec(substr(s,5,10))+strlen($num))
            $num = implode("",arr_unshuffle(str_split($num)))
            srand() //restore randomness*/
        }
        limits = getLM(input)
        limits.max++
        str = fixStr(toStr(num, limits),true,true)
        return str
    }

    static BigInteger toNum(char[] str, Limits limits) {
         int len = str.length
         BigInteger n = 0
         str.eachWithIndex {
             char c, int s ->
                 BigInteger r = (c as int) - limits.low
                 n = (s == len - 1) ? n + (r + 1) : (n + (r + 1)) * limits.max
         }
         return n
    }

    static char[] toStr(BigInteger num, Limits limits) {
        LinkedList<Character> anum = new LinkedList<>()
        while(num-- >= 1) {
            BigInteger c = (num % limits.max) + limits.low
            anum.offerFirst(c as char)
            num = num.divide(limits.max as BigInteger)
        }
        return anum.toArray() as char[]
    }
    // Reduce the range of a method by replacing unused chars
    char[] fixStr(char[] str, boolean i, boolean e) {
        String t = ""
        String f = ""
        Charset cs = i ? input : output
        switch(cs) {
            case ALPHA:   f = "`_^[\\]";          t = "CFBADE";         break
            case ANUM:    f = "`_^[\\]@?>=";      t = "5409216378";     break
            case BASE64:  f = "`_^[\\]@?><:;";    t = "47+820/61935";   break
            case HASH:    f = "homniljpkg";       t = "3810974256";     break
        }
        if(f) {
            str = e ? replaceChars(str,f.toCharArray(),t.toCharArray())
                    : replaceChars(str,t.toCharArray(),f.toCharArray())
        }
        return str
    }
    // lower, length (diff + 1)
    static Limits getLM(Charset charset) {
        Limits limits = new Limits()
        switch(charset) {
            case ALPHA:   limits.low = 0x47;   limits.max = 0x34;     break
            case ANUM:    limits.low = 0x3D;   limits.max = 0x3E;     break
            case ASCII:   limits.low = 0x20;   limits.max = 0x5F;     break
            case BASE64:  limits.low = 0x3A;   limits.max = 0x41;     break
            case BIT:     limits.low = 0x30;   limits.max = 0x2;      break
            case BRAILLE: limits.low = 0x2800; limits.max = 0xFF;     break
            case HANZU:   limits.low = 0x4E00; limits.max = 0x51AF;   break
            case HASH:    limits.low = 0x61;   limits.max = 0x10;     break
            case HIDE:    limits.low = 0xD800; limits.max = 0x7FF;    break
            case HIRA:    limits.low = 0x3041; limits.max = 0x53;     break
            case KANA:    limits.low = 0x30A1; limits.max = 0x54;     break
            case KOR:     limits.low = 0xAC00; limits.max = 0x2BA3;   break
            case LCASE:   limits.low = 0x61;   limits.max = 0x1A;     break
            case LINES:   limits.low = 0x4DC0; limits.max = 0x3F;     break
            case NUM:     limits.low = 0x30;   limits.max = 0xA;      break
            case SYMBL:   limits.low = 0x2010; limits.max = 0x7EF;    break
            case UCASE:   limits.low = 0x41;   limits.max = 0x1A;     break
            case UTF8:    limits.low = 0x0;    limits.max = 0xFFFF;   break
        }
        return limits
    }
    /**
     * replace characters one by one
     * @param str
     * @param from
     * @param to
     * @return
     */
    static char[] replaceChars(char[] str, char[] from, char[] to) {
        char[] out = Arrays.copyOf(str, str.length)
        str.eachWithIndex {
            char it, int idx ->
                int p = from.toList().indexOf(it)
                if (p >= 0) {
                    out[idx] = to[p]
                }
        }
        return out
    }
}
