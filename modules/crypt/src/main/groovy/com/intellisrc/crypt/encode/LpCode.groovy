package com.intellisrc.crypt.encode

import groovy.transform.CompileStatic

import java.lang.reflect.Modifier

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
 * 1) INPUT: Is the expected format of the string to encode (Default: BASIC).
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
 *      For that reason, use BASIC or LATIN as input.
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
 * INVISIBLE may provide an extra difficulty to read as are not visual characters.
 * ALPHA,ANUM,HASH,LCASE,UCASE,NUM are safe to store (as in a DB) without worrying about
 * escaping chars or having problems with language encoding.
 *
 * 3) KEY: used to randomize output (any UTF-8 char is possible). It is optional.
 */
@CompileStatic
class LpCode {
    //Binary-like (2 chars: 0,1)
    static public final List<Integer> BIT = 0x30..0x31
    //0-9 (10 digits)
    static public final List<Integer> NUMBERS = 0x30..0x39
    //a-z (26 chars)
    static public final List<Integer> LOWERCASE = 0x61..0x7A
    //A-Z (26 chars)
    static public final List<Integer> UPPERCASE = 0x41..0x5A
    //a-zA-Z (52 chars)
    static public final List<Integer> ALPHA = LOWERCASE + UPPERCASE
    //0-9a-zA-Z (62 chars)
    static public final List<Integer> ANUM = NUMBERS + LOWERCASE + UPPERCASE
    // a to f (6 chars)
    static public final List<Integer> aTof = 0x61..0x66
    // A to F (6 chars)
    static public final List<Integer> AtoF = 0x41..0x46
    // = and + symbols (2 chars)
    static public final List<Integer> EQUALPLUS = [0x3D, 0x2B]
    //0-9a-f //similar to md5, sh,a etc (but with variable length) (16 chars)
    static public final List<Integer> HASH = NUMBERS + aTof
    //0-9A-F //same as 'HASH' but uppercase (16 chars)
    static public final List<Integer> HASHUC = NUMBERS + AtoF
    //0-9a-zA-Z=+/ (as result of Base64, etc) (64 chars)
    static public final List<Integer> BASE64 = ANUM + EQUALPLUS
    //Any English ascii char (96 chars)
    static public final List<Integer> BASIC = 0x20..0x7F
    //Extended Ascii (without BASIC) (719 chars)
    static public final List<Integer> EXTENDED = 0xA1..0x036F
    //Ascii Extended (support for accents and other common symbols) (815 chars)
    static public final List<Integer> LATIN = BASIC + EXTENDED
    // Greek (105 chars)
    static public final List<Integer> GREEK = (0x0370..0x03E1) - [0x0378,0x0379,0x0380,0x0381,0x0382,0x0383,0x038B,0x038D,0x03A2]
    // Cyrillic (128 chars)
    static public final List<Integer> CYRILLIC = 0x0400..0x047F
    // Armenian (88 chars)
    static public final List<Integer> ARMENIAN = (0x0531..0x058A) - [0x0557,0x0558]
    // Hebrew (88 chars)
    static public final List<Integer> HEBREW = (0x0591..0x05F4) - (0x05C8..0x05CF) - (0x05EB..0x05EE)
    // Arabic (339 chars)
    static public final List<Integer> ARABIC = (0x0600..0x06FF) + (0x08A0..0x08FF) - [0x061D,0x08B5] - (0x08C8..0x08D2)
    // Syriac (109 chars)
    static public final List<Integer> SYRIAC = (0x0700..0x076F) - [0x070E,0x074B,0x074C]
    // Thaana - Maldivas (50 chars)
    static public final List<Integer> THAANA = 0x0780..0x07B1
    // NKO - West Africa (62 chars)
    static public final List<Integer> NKO = (0x07C0..0x07FF) - [0x07FB,0x07FC]
    // Samaritan - Aramaic (ancient Greek, Arabic and Hebrew) (62 chars)
    static public final List<Integer> SAMARITAN = (0x0800..0x083F) - [0x082E,0x082F]
    // Mandaic - Southeastern Aramaic (Iraq, Iran) (29 chars)
    static public final List<Integer> MANDAIC = (0x0840..0x085B) + [0x085E]
    // Devanagari - India (128 chars)
    static public final List<Integer> DEVANAGARI = 0x0900..0x097F
    // Bengali - India (92 chars)
    static public final List<Integer> BENGALI = (0x0985..0x09FE) - [0x0984,0x098D,0x098E,0x0991,0x0992,0x09A9,0x09B1,0x09B3,
                                                                       0x09B4,0x09B5,0x09BA,0x09BB,0x09C5,0x09C6,
                                                                       0x09C9,0x09CA,0x09DE,0x09E4,0x09E5] -
                                                                       (0x09CF..0x09D6) - (0x09D8..0x09DB)
    // Gutmukhi - India (92 chars)
    static public final List<Integer> GUTMUKHI = (0x0985..0x09FE) - [0x0984,0x098D,0x098E,0x0991,0x0992,0x09A9,0x09B1,
                                                                        0x09B3,0x09B4,0x09B5,0x09BA,0x09BB,0x09C5,0x09C6,
                                                                        0x09C9,0x09CA,0x09DE,0x09E4,0x09E5] - (0x09CF..0x09D6) - (0x09D8..0x09DB)
    // Gujarati - India (91 chars)
    static public final List<Integer> GUJARATI = (0x0A81..0x0AFF) - [0x0A84,0x0A8E,0x0A92,0x0AA9,0x0AB1,0x0AB4,0x0ABA,0x0ABB,
                                                                     0x0AC6,0x0ACA,0x0ACE,0x0ACF,0x0AE4,0x0AE5] -
                                                                    (0x0AD1..0x0ADF) - (0x0AF2..0x0AF8)
    // Oriya - India (91 chars)
    static public final List<Integer> ORIYA = (0x0B01..0x0B77) - [0x0B04,0x0B0D,0x0B0E,0x0B11,0x0B12,0x0B29,0x0B31,0x0B34,
                                                                  0x0B3A,0x0B3B,0x0B45,0x0B46,0x0B49,0x0B4A,0x0B5E,0x0B64,0x0B65] -
                                                                    (0x0B4E..0x0B54) - (0x0B58..0x0B5B)
    // Tamil - India (76 chars)
    static public final List<Integer> TAMIL = (0x0B82..0x0BFA) - [0x0B84,0x0B91,0x0B9B,0x0B9D,0x0BC9,0x0BCE,0x0BCF] -
                                                                (0x0B8B..0x0B8D) - (0x0B96..0x0B98) - (0x0BA0..0x0BA2) -
                                                                (0x0BA5..0x0BA7) - (0x0BAB..0x0BAD) - (0x0BC3..0x0BC5) -
                                                                (0x0BD1..0x0BD6) - (0x0BD8..0x0BE5)
    // Telegu - India (99 chars)
    static public final List<Integer> TELEGU = (0x0C00..0x0C7F) - [0x0C0D,0x0C11,0x0C29,0x0C3A,0x0C3B,0x0C3C,0x0C45,0x0C49,
                                                                   0x0C57,0x0C64,0x0C65,0x0C70] -
                                                                (0x0C4E..0x0C54) - (0x0C5B..0x0C5F) - (0x0C72..0x0C76)
    // Kannada - India (96 chars)
    static public final List<Integer> KANNADA = (0x0C80..0x0CF2) - [0x0C8D,0x0C91,0x0CA9,0x0CB4,0x0CBA,0x0CBB,0x0CC5,0x0CC9,
                                                                    0x0CDF,0x0CE4,0x0CE5,0x0CF0] - (0x0CD7..0x0CDD)
    // Languages from India (765 chars)
    static public final List<Integer> INDIAN = DEVANAGARI + BENGALI + GUTMUKHI + GUJARATI + ORIYA + TAMIL + TELEGU + KANNADA
    // Malayam (118 chars)
    static public final List<Integer> MALAYAM = (0x0D00..0x0D7F) - [0x0D0D,0x0D11,0x0D45,0x0D49,0x0D64,0x0D65] - (0x0D50..0x0D53)
    // Sinhala (94 chars)
    static public final List<Integer> SINHALA = (0x0D81..0x0DF4) - [0x0D84,0x0DB2,0x0DBC,0x0DBE,0x0DBF,0x0DD5,0x0DD7,0x0DF0,0x0DF1] -
                                                                    (0x0D97..0x0D99) - (0x0DCB..0x0DCE) - (0x0DE0..0x0DE5)
    // Thai (87 chars)
    static public final List<Integer> THAI = (0x0E01..0x0E5B) - (0x0E3B..0x0E3E)
    // Lao (79 chars)
    static public final List<Integer> LAO = (0x0E81..0x0EDF) - [0x0E83,0x0E85,0x0E8B,0x0EA4,0x0EA6,0x0EA8,0x0EA9,0x0EAC,0x0EBE,0x0EBF,
                                                                0x0EC5,0x0EC6,0x0ECE,0x0ECF,0x0EDA,0x0EDB]
    // South-East Asia (378 chars)
    static public final List<Integer> SEASIA = MALAYAM + SINHALA + THAI + LAO

    // Tibetan (124 chars)
    static public final List<Integer> TIBETAN = (0x0F00..0x0F7F) - (0x0F6D..0x0F70)
    // Myanmar ( chars)
    //static public final List<Integer> MYANMAR =
    // Georgian ( chars)
    //static public final List<Integer> GEORGIAN=
    // Hangul ( chars)
    //static public final List<Integer> HANGUL=
    // Ehiopic ( chars)
    //static public final List<Integer> ETHIOPIC=
    // Ucas ( chars)
    //static public final List<Integer> UCAS=
    // Ogham ( chars)
    //static public final List<Integer> OGHAM=
    // Runic ( chars)
    //static public final List<Integer> RUNIC=
    // Tagalog ( chars)
    //static public final List<Integer> TAGALOG=
    // ( chars)
    //static public final List<Integer> HANUNOO=
    // ( chars)
    //static public final List<Integer> BUHID=
    // ( chars)
    //static public final List<Integer> TAGBANWA=
    // ( chars)
    //static public final List<Integer> KHMER=
    // ( chars)
    //static public final List<Integer> MONGOLIAN=
    // ( chars)
    //static public final List<Integer> LIMBU=
    // ( chars)
    //static public final List<Integer> TAILE=
    // ( chars)
    //static public final List<Integer> BUGINESE=
    // ( chars)
    //static public final List<Integer> TAITHAM=
    // ( chars)
    //static public final List<Integer> BALINESE=
    // ( chars)
    //static public final List<Integer> SUNDANESE=
    // ( chars)
    //static public final List<Integer> BATAK=
    // ( chars)
    //static public final List<Integer> LEPCHA=
    // ( chars)
    //static public final List<Integer> OICHIKI=
    // ( chars)
    //static public final List<Integer> COPTIC=
    // ( chars)
    //static public final List<Integer> GLAGOLITIC=
    // ( chars)
    //static public final List<Integer> TIFINAGH=
    // Chinese (inc. Japanese Kanji, 20,911 chars)
    static public final List<Integer> HANZU    = (0x4E00..0x9FAE)
    // Korean (11,171 chars)
    static public final List<Integer> KOREAN   = (0xAC00..0xD7A2)
    // Hiragana (83 chars)
    static public final List<Integer> HIRAGANA = (0x3041..0x3093)
    // Katakana (84 chars)
    static public final List<Integer> KATAKANA = (0x30A1..0x30F4)
    // Japanese Kanji (2,131 chars)
    //static public final List<Integer> KANJI    =
    // (32,249 chars)
    static public final List<Integer> CJK = HANZU + KOREAN + HIRAGANA + KATAKANA
    // ( chars)
    //static public final List<Integer> VAI=
    // ( chars)
    //static public final List<Integer> BAMUM=
    // ( chars)
    //static public final List<Integer> JAVA=
    // ( chars)
    //static public final List<Integer> CHAM=
    // ( chars)
    //static public final List<Integer> TAIVIET=
    // ( chars)
    // Symbols and Other languages
    // ( chars)
    //static public final List<Integer> ENCLOSED=
    // ( chars)
    //static public final List<Integer> SUBSUP=
    // ( chars)
    //static public final List<Integer> CURRENCY=
    // ( chars)
    //static public final List<Integer> NUMFORM=
    // ( chars)
    //static public final List<Integer> ARROWS=
    // ( chars)
    //static public final List<Integer> DINGBATS=
    // Braille (255 chars)
    static public final List<Integer> BRAILLE = (0x2800..0x28FE)
    // Symbols (2,031 chars)
    static public final List<Integer> SYMBOLS = (0x2010..0x27FE)
    // Lines (63 chars)
    static public final List<Integer> LINES = (0x4DC0..0x4DFE)
    // ( chars)
    //static public final List<Integer> BOX =
    // ( chars)
    //static public final List<Integer> BLOCK =
    // ( chars)
    //static public final List<Integer> SHAPES =
    // ( chars)
    //static public final List<Integer> TECH =
    // Non-displayable (use with caution) (2,047 chars)
    static public final List<Integer> INVISIBLE = (0xD800..0xDFFE)
    // Visible characters : safe option instead of UTF8 ( chars)
    static public final List<Integer> VISIBLE = LATIN + GREEK + CYRILLIC + ARMENIAN + HEBREW + ARABIC + SYRIAC + THAANA +
         NKO + SAMARITAN + MANDAIC + INDIAN + SEASIA + TIBETAN + CJK + BRAILLE + SYMBOLS + LINES
    //Any UTF8 char (65,536 chars) use with caution as many characters can not be rendered
    static public final List<Integer> UTF8 = 0x0..0xFFFF

    static protected class CharSet {
        final Set<Integer> chars
        CharSet(Collection<Integer> chars) {
            this.chars = chars.toSet()
        }
        int getLength() {
            return chars.size()
        }
        int getPosition(int chr) {
            return chars.toList().indexOf(chr)
        }
    }

    private CharSet input
    private CharSet output
    private long seed

    LpCode(Collection<Integer> input = BASIC, Collection<Integer> output = ANUM, long seed = 0) {
        this.input = new CharSet(input)
        this.output = new CharSet(output)
        this.seed = seed
    }
    char[] encode(char[] str) {
        if(seed) {
            str = randomize(str)
        }
        BigInteger num = toNum(str, input)
        return toStr(num, output)
    }
    char[] decode(char[] str) {
        BigInteger num = toNum(str, output)
        str = toStr(num, input)
        if(seed) {
            str = randomize(str, true)
        }
        return str
    }

    static protected BigInteger toNum(char[] str, CharSet charset) {
         int len = str.length
         BigInteger n = 0
         str.eachWithIndex {
             char c, int s ->
                 //int r = (c as int) - charset.low
                 int r = charset.getPosition(c as int)
                 //noinspection GrReassignedInClosureLocalVar
                 n = (s == len - 1) ? n + (r + 1) : (n + (r + 1)) * (charset.length)
         }
         return n
    }

    static protected char[] toStr(BigInteger num, CharSet charset) {
        LinkedList<Character> anum = new LinkedList<>()
        while(num-- >= 1) {
            int c = charset.chars[(num % (charset.length)) as int]
            anum.addFirst(c as char)
            num = num.divide((charset.length) as BigInteger)
        }
        return anum.toArray() as char[]
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

    static void printLengths() {
        try {
            LpCode.declaredFields.each {
                if (Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)) {
                    println it.name + " : " + (it.get(null) as List<Integer>).size()
                }
            }
        } catch(Exception ignore) {}
    }
}
