package com.intellisrc.crypt.encode

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
        BIT     (0x30, 0x31),               //Binary-like (2 chars: 0,1)
        NUM     (0x30, 0x39),               //0-9 (10 digits)
        // English:
        LCASE   (0x61, 0x7A),               //a-z (26 chars)
        UCASE   (0x41, 0x5A),               //A-Z (26 chars)
        ALPHA   (0x41, 0x7A, 6),   //a-zA-Z (52 chars)
        ANUM    (0x30, 0x7A, 13),  //0-9a-zA-Z (62 chars)
        ASCII   (0x20, 0x7F),               //Any English ascii char (95 chars)
        LATIN   (0x20, 0xFF, 7),   //Ascii Extended (support for accents and other common symbols) (928 chars)
        UTF8    (0x0,  0xFFFF),             //Any UTF8 char (65,535 chars)
        // Encoding
        HASH    (0x30, 0x3F, 6),   //0-9a-f //similar to md5, sh,a etc (but with variable length) (16 chars)
        HASHUC  (0x30, 0x3F, 6),   //0-9A-F //same as 'HASH' but uppercase (16 chars)
        BASE64  (0x30, 0x7A, 4),   //0-9a-zA-Z=+/ (as result of Base64, etc) (65 chars)
        // Non-latin Languages
        /* TODO:
        GREEK   (0x,0x),
        CYRILLIC(0x,0x),
        ARMENIAN(0x,0x),
        HEBREW  (0x,0x),
        ARABIC  (0x,0x),
        SYRIAC  (0x,0x),
        THAANA  (0x,0x),
        NKO     (0x,0x),
        SAMARITAN(0x,0x),
        MANDAIC (0x,0x),
        DEVANAGARI(0x,0x),
        BENGALI(0x,0x),
        GURMUKHI(0x,0x),
        GUJARATI(0x,0x),
        ORIYA(0x,0x),
        TAMIL(0x,0x),
        TELEGU(0x,0x),
        KANNADA(0x,0x),
        MALAYAM(0x,0x),
        SINHALA(0x,0x),
        THAI(0x,0x),
        LAO(0x,0x),
        TIBETAN(0x,0x),
        MYANMAR(0x,0x),
        GEORGIAN(0x,0x),
        HANGUL(0x,0x),
        ETHIOPIC(0x,0x),
        CHEROKEE(0x,0x),
        UCAS(0x,0x),
        OGHAM(0x,0x),
        RUNIC(0x,0x),
        TAGALOG(0x,0x),
        HANUNOO(0x,0x),
        BUHID(0x,0x),
        TAGBANWA(0x,0x),
        KHMER(0x,0x),
        MONGOLIAN(0x,0x),
        LIMBU(0x,0x),
        TAILE(0x,0x),
        BUGINESE(0x,0x),
        TAITHAM(0x,0x),
        BALINESE(0x,0x),
        SUNDANESE(0x,0x),
        BATAK(0x,0x),
        LEPCHA(0x,0x),
        OICHIKI(0x,0x),
        COPTIC(0x,0x),
        GLAGOLITIC(0x,0x),
        TIFINAGH(0x,0x),*/
        //FIXME: confirm numbers:
        HANZU   (0x4E00, 0x9FAE),           //Chinese (inc. Japanese Kanji, 20,911 symbols)
        KOR     (0xAC00, 0xD7A2),           //Korean (11,171 symbols)
        HIRA    (0x3041, 0x3093),           //Hiragana (83 chars)
        KANA    (0x30A1, 0x30F4),           //Katakana (86 chars)
        //KANJI(0x,0x),  //Japanese Kanji (2131 symbols)
        /* TODO:
        VAI(0x,0x),
        BAMUM(0x,0x),
        JAVA(0x,0x),
        CHAM(0x,0x),
        TAIVIET(0x,0x),*/
        // Symbols and Other languages
        /* TODO:
        ENCLOSED(0x,0x),
        SUBSUP(0x,0x),
        CURRENCY(0x,0x),
        NUMFORM(0x,0x),
        ARROWS(0x,0x),
        DINGBATS(0x,0x),*/
        BRAILLE (0x2800, 0x28FE),           //Braille (255 chars)
        SYMBOLS (0x2010, 0x27FE),           //Symbols (2(0x,0x),031 symbols)
        LINES   (0x4DC0, 0x4DFE),           //Lines (64 symbols)
        /* TODO:
        BOX(0x,0x),
        BLOCK(0x,0x),
        SHAPES(0x,0x),
        TECH(0x,0x),*/
        HIDE   (0xD800, 0xDFFE)             //Non-displayable (use with caution) (2,047 symbols)
        //PRIVATE(0x,0x),

        int from
        int to
        int max
        /**
         *
         * @param from
         * @param to
         * @param substitute
         */
        Charset(int from, int to, int substitute = 0) {
            this.from = from
            this.to = to - substitute
            this.max = this.to - this.from
        }

        // Reduce the range of a method by replacing unused chars
        char[] fixStr(char[] str, boolean e) {
            String t = ""
            String f = ""
            switch(this) {
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
    }

    private Charset input
    private Charset output
    private long seed

    LpCode(Charset input = ASCII, Charset output = ANUM, long seed = 0) {
        this.input = input
        this.output = output
        this.seed = seed
    }
    char[] encode(char[] str) {
        input.max++
        if(seed) {
            str = randomize(str)
        }
        BigInteger num = toNum(input.fixStr(str,false), limits)
        input.max--
        return fixStr(toStr(num, output),true)
    }
    char[] decode(char[] str) {
        BigInteger num = toNum(output.fixStr(str,false), limits)
        input.max++
        str = fixStr(toStr(num, limits),true,true)
        if(seed) {
            str = randomize(str, true)
        }
        return str
    }

    static BigInteger toNum(char[] str, Limits limits) {
         int len = str.length
         BigInteger n = 0
         str.eachWithIndex {
             char c, int s ->
                 int r = (c as int) - limits.low
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
    /*
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
            case SYMBOLS: limits.low = 0x2010; limits.max = 0x7EF;    break
            case UCASE:   limits.low = 0x41;   limits.max = 0x1A;     break
            case UTF8:    limits.low = 0x0;    limits.max = 0xFFFF;   break
            default:
                Log.w("Not implemented yet: %s", charset.toString())
        }
        return limits
    }*/

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
    /**
     * Randomize char array
     * @param str
     * @return
     */
    char[] randomize(char[] str, boolean reverse = false) {
        Random random = new Random(seed)
        List<Integer> pos = (0..(str.length - 1)).toList()
        pos.shuffle(random)
        char[] shuffled = new char[str.length]
        pos.eachWithIndex {
            int p, int i ->
                if(reverse) {
                    shuffled[p] = str[i]
                } else {
                    shuffled[i] = str[p]
                }
        }
        return shuffled
    }
}
