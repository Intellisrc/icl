//file:noinspection unused
package com.intellisrc.crypt.encode

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.lang.reflect.Modifier
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/*
 * @author: A.Lepe
 * @since: Sep 10,2010 (Originally created in PHP for Yayahuic Framework)
 *
 * This class provides 2-way string encoding as a way of obfuscation.
 * While it doesn't really provide security, it could be used to
 * create confusion or to reduce the storage size of a string (see notes).
 * Or... you can use it just for fun! :)
 *
 * Features:
 * - Use of uncommon methods to encode/decode strings
 * - a salt key can be used to change output
 * - 2 Planes of UTF-8 can be used (128k+ chars) (see notes).
 * - Depending on the settings, the encoded string may
 *   result smaller than the length of the original string.
 * - Many possible combinations (input, output).
 * - Translation from one charset to another.
 * - Get a numeric representation of a string
 *
 * NOTES:
 *  1. If you encode, for example, from HASH (16 chars) to ALPHA (52 chars) the resulting
 *     string length will be smaller than the original string. The opposite is also true:
 *     If you encode an alphanumeric string (ALPHA) to NUMBERS (10 chars), the resulting
 *     string length will be longer than the original string.
 *  2. If you store data and you use any of the characters inside the SMP (Plane 2), which
 *     are composed of 4 bytes, you may need prepare your code to handle such strings. For
 *     example:
 *     "日本語".toCharArray().length == 3  // Any character inside the BMP (Basic Multilingual Plane) can be stored inside a `char`
 *     "🕙🫛🎸🛡".toCharArray().length == 8 // Instead of iterating over those characters you will need to use getCodePoints() method.
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
 * 3) KEY: used to randomize output. It is optional.
 *
 * NOTE: There is no font that will display all characters in this list.
 *       Some fonts of the most complete are:
 *
 * Recommended fonts:
 *  - Noto - Google (77,000 characters)
 *  - Unifont (65,000 characters)
 *  - Dejavu (6,000 characters)
 *
 * Fonts marked with [**] may require specific font to display
 *
 * https://www.ssec.wisc.edu/~tomw/java/unicode.html
 * https://en.wikipedia.org/wiki/Unicode_block
 * http://www.unicode.org/charts/
 */

@CompileStatic
class LpCode {
    //NOTE: The smaller the chunkSize, the larger the output, higher the CPU usage and lower the Memory usage.
    int chunkSize = 100      //For large size texts, it is better to use chunks (for very large texts, 1024 might be better)
    int glueChar = 0x1E      //Which character to use to glue a chunk (it can be part of the OUTPUT charset)
    /* ************ PARTS ***************** */
    //Binary-like (2 chars: 0,1) : 10111010111011111010100111000011111101000101000100
    static public final List<Integer> BIT = 0x30..0x31
    //Space character (used only to complement any other)
    static public final List<Integer> SPACE = [0x20]
    //New line characters. Use it when your text might contain new line characters (return character and line feed)
    static public final List<Integer> NEWLINES = [0x0A,0x0D]
    // = and + symbols (2 chars) : Used to complement others : +===+=+=++=++=+=+=++=
    static public final List<Integer> EQUALPLUS = [0x3D, 0x2B]
    // a to f (6 chars) : ebacedaebfccedafcfaeeeefbacaef
    static public final List<Integer> aTof = 0x61..0x66
    // A to F (6 chars) : CCEEFEBEFEFCCAACFCFEBACEDAEBFCCEDA
    static public final List<Integer> AtoF = 0x41..0x46
    //0-9 (10 digits) : 058507141981398903957820054
    static public final List<Integer> NUMBERS = 0x30..0x39
    //a-z (26 chars) : iofwbwhqvtjdtglpsuxvpjomkrinrzkfgb
    static public final List<Integer> LOWERCASE = 0x61..0x7A
    //A-Z (26 chars) : YRXAFMKZCIOFWBWHQVTJDTGLPSU
    static public final List<Integer> UPPERCASE = 0x41..0x5A

    /* ************ BLOCKS **************** */
    //Any English ascii char (96 chars) : r~<s>3,o2F$%]<|5tu^9bmfN>R^
    static public final List<Integer> BASIC = 0x20..0x7F
    //Extended Ascii (without BASIC) (719 chars) : ÂŅʚƵȲɭʵ˿ɠȠƸ̹Ŕ̴ǅƛʸȕȴ³ɭŠ̩ͧ
    static public final List<Integer> EXTENDED = 0xA1..0x036F
    // Latin (inc. COMBINING) ( 91 chars) : ᵎꞾⱯỡꬶṭⱨṥ᷃ừḭậ᷽ꞚᶔᴇẮꞑṊ᷃Ɒꬹꞎỿ
    static public final List<Integer> LATIN = (0x1D00..0x1D65) + (0x1D6B..0x1EFF) + (0x1AB0..0x1AC0) + (0x2C60..0x2C7F) -
        [0x1DFA, 0xA7C0, 0xA7C1] + (0xA722..0xA7CA) + (0xA7F2..0xA7FF) + (0xAB30..0xAB64)
    // Greek (344 chars) : ͼᾧὃμΠὛΕῷἛἸά῾ῪΗπἧ῝ἫἘἶι
    static public final List<Integer> GREEK = (0x0370..0x03E1) + (0x1D66..0x1D6A) + (0x1F00..0x1FFE) -
        [0x0378, 0x0379, 0x0380, 0x0381, 0x0382, 0x0383, 0x038B, 0x038D, 0x03A2, 0x1F16, 0x1F17,
         0x1F1E, 0x1F1F, 0x1F46, 0x1F47, 0x1F4E, 0x1F4F, 0x1F58, 0x1F5A, 0x1F5C, 0x1F5E, 0x1F7E,
         0x1F7F, 0x1FB5, 0x1FC5, 0x1FD4, 0x1FD5, 0x1FDC, 0x1FF0, 0x1FF1, 0x1FF5] + [0xAB65]
    // Cyrillic (265 chars) : ꙟꙭЯꙝꚌјꚗьѭ꙯ⷼчЃчⷻФꚟгЮ
    static public final List<Integer> CYRILLIC = (0x0400..0x047F) + (0x1C80..0x1C88) + (0x2DE0..0x2DFF) + (0xA640..0xA69F)
    // Armenian (93 chars) : ֈճկվոՁ՞Տ՟տրՂՙղՉքՇքԱՆ՟ՐպՋ
    static public final List<Integer> ARMENIAN = (0x0531..0x058A) - [0x0557, 0x0558] + (0xFB13..0xFB17)
    // Hebrew (133 chars) : ײלטְּלﬡּ׆֫ײַﬨֱבּחתּ֠ךֲּﬞ֬֘טצּ֭אּ֜
    static public final List<Integer> HEBREW = (0x0591..0x05F4) - (0x05C8..0x05CF) - (0x05EB..0x05EF) + (0xFB1D..0xFB4F) -
        [0xFB37, 0xFB3D, 0xFB3F, 0xFB42, 0xFB45]
    // Arabic (1109 chars) : ࣵﺫ﮽ۣجﺤلﻼﱂﷺﵝﻞﹿۙﲤﰾﱿﵸ
    static public final List<Integer> ARABIC = (0x0600..0x06FF) + (0x08A0..0x08FF) - [0x061D, 0x08B5] - (0x08C8..0x08D2) +
        (0xFB50..0xFBC1) + (0xFBD3..0xFDC7) - [0xFD90, 0xFD91] + [0xFDCF] + (0xFDF0..0xFDFF) +
        (0xFE70..0xFEFC) - [0xFE76]
    // Syriac (108 chars) : ܸ܂݇܈ݑܰݪܬܚܼ݂ݢܐ܈ݯܶܥܮ܈ݣݏܳܚܤܮܷݩܗ݅ܯݝݚ܍ݒ
    static public final List<Integer> SYRIAC = (0x0700..0x076F) - [0x070E, 0x070F, 0x074B, 0x074C]

    // Thaana - Maldivas (50 chars) : ޔޟޢޖާޏޙޯށޤުހޟހޅްޒީޟގެޔޛޕޭާާޮޣޚޅޕޫޫޣޣދޤޕިުޱޒލހ
    static public final List<Integer> THAANA = 0x0780..0x07B1
    // NKO - West Africa (62 chars) : ߅ߕߛߌ߳ߢ߭ߥ߇ߔߟ߾ߏ߉ߣߨ߭߃߹߈ߓ߀ߙ
    static public final List<Integer> NKO = (0x07C0..0x07FF) - [0x07FB, 0x07FC]
    // Samaritan - Aramaic (ancient Greek, Arabic and Hebrew) (62 chars) : ࠵࠘ࠄࠬࠓ࠭࠰࠻ࠅࠕࠛࠌ࠵ࠢ࠭ࠥࠇࠔࠟ࠾ࠏࠉࠣࠨ࠭ࠃ࠻ࠈࠓࠀ࠙ࠨࠨ࠳࠷ࠈࠧࠄࠔࠄ
    static public final List<Integer> SAMARITAN = (0x0800..0x083F) - [0x082E, 0x082F]
    // Mandaic - Southeastern Aramaic (Iraq, Iran) (29 chars) : Not available in some fonts
    static public final List<Integer> MANDAIC = (0x0840..0x085B) + [0x085E]
    // Devanagari - India (160 chars) : ऩउ़ुऺ३फबअठॹꣻ॓ऐऽ꣮छॖँीऌॿऐड꣪ॿएऀ꣩३ड़
    static public final List<Integer> DEVANAGARI = (0x0900..0x097F) + (0xA8E0..0xA8FF)
    // Bengali - India (92 chars) : ঢঙঠৄ৽ষগঠ২ৣঠ১ঝছে৷ট৸ঢ়ঘৱ৸ণওতাওে৪ৣআ
    static public final List<Integer> BENGALI = (0x0985..0x09FE) - [0x0984, 0x098D, 0x098E, 0x0991, 0x0992, 0x09A9, 0x09B1, 0x09B3,
                                                                    0x09B4, 0x09B5, 0x09BA, 0x09BB, 0x09C5, 0x09C6,
                                                                    0x09C9, 0x09CA, 0x09DE, 0x09E4, 0x09E5] -
        (0x09CF..0x09D6) - (0x09D8..0x09DB)
    // Gutmukhi - India (92 chars) : বঢঙঠৄ৽ষগঠ২ৣঠ১ঝছে৷ট৸ঢ়ঘৱ৸
    static public final List<Integer> GUTMUKHI = (0x0985..0x09FE) - [0x0984, 0x098D, 0x098E, 0x0991, 0x0992, 0x09A9, 0x09B1,
                                                                     0x09B3, 0x09B4, 0x09B5, 0x09BA, 0x09BB, 0x09C5, 0x09C6,
                                                                     0x09C9, 0x09CA, 0x09DE, 0x09E4, 0x09E5] - (0x09CF..0x09D6) - (0x09D8..0x09DB)
    // Gujarati - India (91 chars) : ૐગૹૈ૮ઝૌગૉડ૭ૡબૠ૪૰બાૣગએા૩ઢૢીી૧તિઝ
    static public final List<Integer> GUJARATI = (0x0A81..0x0AFF) - [0x0A84, 0x0A8E, 0x0A92, 0x0AA9, 0x0AB1, 0x0AB4, 0x0ABA, 0x0ABB,
                                                                     0x0AC6, 0x0ACA, 0x0ACE, 0x0ACF, 0x0AE4, 0x0AE5] -
        (0x0AD1..0x0ADF) - (0x0AF2..0x0AF8)
    // Oriya - India (91 chars) : ଛଢ଼ଙୱ୍୭ଟୗଙ୕ଣ୬ୠମୟ୩୯ମୀୢଙଐ
    static public final List<Integer> ORIYA = (0x0B01..0x0B77) - [0x0B04, 0x0B0D, 0x0B0E, 0x0B11, 0x0B12, 0x0B29, 0x0B31, 0x0B34,
                                                                  0x0B3A, 0x0B3B, 0x0B45, 0x0B46, 0x0B49, 0x0B4A, 0x0B5E, 0x0B64, 0x0B65] -
        (0x0B4E..0x0B54) - (0x0B58..0x0B5B)
    // Tamil - India (76 chars) : ௶ை௵ௐஙோௐ௮எு௧ஐ௰ளட
    static public final List<Integer> TAMIL = (0x0B82..0x0BFA) - [0x0B84, 0x0B91, 0x0B9B, 0x0B9D, 0x0BC9, 0x0BCE, 0x0BCF] -
        (0x0B8B..0x0B8D) - (0x0B96..0x0B98) - (0x0BA0..0x0BA2) -
        (0x0BA5..0x0BA7) - (0x0BAB..0x0BAD) - (0x0BC3..0x0BC5) -
        (0x0BD1..0x0BD6) - (0x0BD8..0x0BE5)
    // Telegu - India (99 chars) : తఎ౹డ౭౷ఆే౾౺ధఖ౦౽ౄ౨౷ొ౨ఙేౄ౫౺ోఋ౺ేరస౸ంద
    static public final List<Integer> TELEGU = (0x0C00..0x0C7F) - [0x0C0D, 0x0C11, 0x0C29, 0x0C3A, 0x0C3B, 0x0C3C, 0x0C45, 0x0C49,
                                                                   0x0C57, 0x0C64, 0x0C65, 0x0C70] -
        (0x0C4E..0x0C54) - (0x0C5B..0x0C5F) - (0x0C72..0x0C76)
    // Kannada - India (96 chars) : ವೖ೫ೀಔುಥಳಒೢೱಞೣಠಕಌೞಔನ಄ಅೃಞ೮ಗ೦೧ೄಛೋ೎ಱಠಶ
    static public final List<Integer> KANNADA = (0x0C80..0x0CF2) - [0x0C8D, 0x0C91, 0x0CA9, 0x0CB4, 0x0CBA, 0x0CBB, 0x0CC5, 0x0CC9,
                                                                    0x0CDF, 0x0CE4, 0x0CE5, 0x0CF0] - (0x0CD7..0x0CDD)
    // Malayam (118 chars) : ൙൲ഥപ൯ംഢഃഉ൧ഌദ൬൏ഴ൝ഫൠആാ൱൮൞ഡേ൭൧ഞഘഫഡ൞
    static public final List<Integer> MALAYAM = (0x0D00..0x0D7F) - [0x0D0D, 0x0D11, 0x0D45, 0x0D49, 0x0D64, 0x0D65] - (0x0D50..0x0D53)
    // Sinhala (94 chars) : නෘතෆඦයන්ෲඹෛඅඕථඐ෫෬ඣඤඵඪ෩ඵෟ෯
    static public final List<Integer> SINHALA = (0x0D81..0x0DF4) - [0x0D84, 0x0DB2, 0x0DBC, 0x0DBE, 0x0DBF, 0x0DD5, 0x0DD7, 0x0DF0, 0x0DF1] -
        (0x0D97..0x0D99) - (0x0DCB..0x0DCE) - (0x0DE0..0x0DE5)
    // Thai (87 chars) : ฎกึ๓฿๑๋นฯธำณเฐฎญถ๚๗ฒฉ์ชษ
    static public final List<Integer> THAI = (0x0E01..0x0E5B) - (0x0E3B..0x0E3E)
    // Tai Viet (72 chars) : ꪈꪁꪗꪤꪊꪺꪹꪓꪎꪪꪺꪧꪺꪢꪵꪩꪜꪥꪧꪺꫛꪹꪧꫜꪓꪬꫀ
    static public final List<Integer> TAI_VIET = (0xAA80..0xAAC2) + (0xAADB..0xAADF)
    // Lao (79 chars) : ດດ໑ໜບ຺ລຶຈພ່ຯແຼຉຆີໟຜຳ
    static public final List<Integer> LAO = (0x0E81..0x0EDF) - [0x0E83, 0x0E85, 0x0E8B, 0x0EA4, 0x0EA6, 0x0EA8, 0x0EA9, 0x0EAC, 0x0EBE, 0x0EBF,
                                                                0x0EC5, 0x0EC6, 0x0ECE, 0x0ECF, 0x0EDA, 0x0EDB]

    // Tibetan (213 chars) : ༭ྯ༅࿈࿇ླྀཀྵྱ༃ཿཅྣ྇༑ླྀདྷ
    static public final List<Integer> TIBETAN = (0x0F00..0x0FDA) - (0x0F6D..0x0F70) - [0x0F98, 0x0FBD]
    // Myanmar (223 chars) : ႜꧫၽꩦꧭ၅၉ဒႊꩪꩳၶꧻၰ၊ၢႈ꧷႒ႉ၇
    static public final List<Integer> MYANMAR = (0x1000..0x109F) + (0xA9E0..0xA9FE) + (0xAA60..0xAA7F)
    // Georgian (174 chars) : ᲴᲓთვⴎტႩᲠႤᲘ჻ნᲹⴒႩგᲺⴝⴒᲡႺᲫᲪჱ
    static public final List<Integer> GEORGIAN = (0x10A0..0x10FF) + (0x1C90..0x1CBF) - [0x10C6, 0x10CE, 0x10CF, 0x1CBB, 0x1CBC] -
        (0x10C8..0x10CC) + (0x2D00..0x2D25) + [0x2D27, 0x2D2D]
    // Ethiopic (496 chars) : ቔᎋቘዹⷉሩጻⶐጡዠአጐ፰ቁ፵
    static public final List<Integer> ETHIOPIC = (0x1200..0x1399) - [0x1249, 0x124E, 0x124F, 0x1257, 0x1259, 0x125E, 0x125F,
                                                                     0x1289, 0x128E, 0x128F, 0x12B1, 0x12B6, 0x12B7, 0x12BF,
                                                                     0x12C1, 0x12C6, 0x12C7, 0x12D7, 0x1311, 0x1316, 0x1317,
                                                                     0x135B, 0x135C] - (0x137D..0x137F) +
        (0x2D80..0x2D96) + (0x2DA0..0x2DDE) - [0x2DA7, 0x2DAF, 0x2DB7, 0x2DBF,
                                               0x2DC7, 0x2DCF, 0x2DD7] +
        (0xAB01..0xAB16) - [0xAB07, 0xAB08, 0xAB0F, 0xAB10, 0xAB27] + (0xAB20..0xAB2E)
    // Cherokee (172 chars) : ᏃꮼᏞꮡꭽᏊꮅꮂꮮᏂꮎᏻᎹꮀꭽꮔꮕᏘꮓᏃꮛᏡꮾᎦꮇ
    static public final List<Integer> CHEROKEE = (0x13A0..0x13FD) - [0x13F6, 0x13F7] + (0xAB70..0xABBF)
    // Unified Canadian Aborigine Syllabics (710 chars) : ᖺᓽᔴᕪᘏᑟᖹᖷᓧᐼᑸᖭᖍᢵᐟᣂᑶᐾᙆᔲᔣᢵᗧᘫᐏ
    static public final List<Integer> UCAS = (0x1400..0x167F) + (0x18B0..0x18F5)
    // OGHAM (29 chars) : ᚙᚓᚋᚒᚘ᚜ᚗᚗᚓᚔᚔᚍ᚛ᚋᚕᚑᚂᚒ
    static public final List<Integer> OGHAM = (0x1680..0x169C)
    // Runic (89 chars) : ᚵᛝᛶᛯᛱᚧᚴᛦᚹᛂᛍᛛ᛫ᛯᚮᚼᚤᚶᛅᛪᛈᚹᚮᛘᚡᚰᚠᛚᛄᛘᚠᛔᛩ
    static public final List<Integer> RUNIC = (0x16A0..0x16F8)
    // Tagalog - Philippines (22 chars) [**] : ᜀᜋᜈᜋᜅᜁᜎᜏᜐᜐ᜔᜶ᜋᜌᜑ᜵ᜈᜄᜓᜑᜁᜋ᜶ᜄᜒᜁᜅᜓᜎᜉᜋ
    static public final List<Integer> TAGALOG = (0x1700..0x1714) + [0x1735, 0x1736] - [0x170D]
    // (23 chars) : ᜦᜬᜪᜨᜦᜤᜠᜭᜦᜠᜧᜮᜬᜦ᜶ᜣᜲᜥᜳᜯᜧ᜴
    static public final List<Integer> HANUNOO = (0x1720..0x1736)
    // (20 chars) : ᝑᝄᝍᝌᝍᝅᝄᝈᝒᝏᝎᝊᝎᝋᝓᝎᝅᝆᝇᝈ
    static public final List<Integer> BUHID = (0x1740..0x1753)
    // (18 chars) : [**] ᝪᝩ᝭ᝰᝣᝦᝥᝦᝲᝩᝧᝲᝠᝪᝰᝰᝯᝢᝥᝩᝦᝨᝬᝨᝰ᝭ᝥᝧᝠ᝭ᝯᝪᝲᝤᝠᝣᝤᝢᝬ᝭ᝣᝣᝩᝠᝮ
    static public final List<Integer> TAGBANWA = (0x1760..0x1773) - [0x1773, 0x1771]
    // (144 chars) : ឞញួ᧱᧡᧧ឤ᧽៲ទឨុឬ២᧬ច៤
    static public final List<Integer> KHMER = (0x1780..0x17F9) + (0x19E0..0x19FF) - [0x17B4, 0x17B5, 0x17DE, 0x17DF] - (0x17EA..0x17EF)
    // (153 chars) : ᠅ᠮᢑᡉᢠᠻᢉᢦᢙᡏᡸᡀᢋᠩᡭ᠈ᡄᢪᠢᠲᡀᡙᢉᢗᢀᠱᡚᢝᢦᢒ
    static public final List<Integer> MONGOLIAN = (0x1800..0x18AA) - (0x180B..0x180F) - (0x181A..0x181F) - (0x1879..0x187F)
    // SMP: + (0x11660..0x1166C)
    // (68 chars) : ᥉᥀ᤋᤉᤚᤛᤒᤀᤒᤨᤛᤂᤂᤛᤰ᥀ᤇᤩᤝᤸ᥍ᤴᤈᤋᤴᤖ᥊
    static public final List<Integer> LIMBU = (0x1900..0x194F) - [0x191F] - (0x192C..0x192F) - (0x193C..0x193F) - (0x1941..0x1943)
    // Taile / new Taile (118 chars) : ᥜᦁᧃᦨᦏᦶᦆᦹᥖᦙᧈᧅᦷᥱᦡᧄᦾᥬᥦᦆᥱ
    static public final List<Integer> TAILE = (0x1950..0x19DF) - [0x196E, 0x196F] - (0x1975..0x197F) -
        (0x19AC..0x19AF) - (0x19CA..0x19CF) - (0x19DB..0x19DD)
    // (30 chars) : ᨋᨇᨃᨀᨔᨔ᨞ᨎᨂᨁᨎᨔᨎᨈᨆ᨟ᨙᨇᨐᨘ
    static public final List<Integer> BUGINESE = (0x1A00..0x1A1F) - [0x1A1C, 0x1A1D]
    // Tai Tham (127 chars) : ᪁ᩑᩁᩥᩇᩲ᪉ᩲᨡᩧᩥ᪠᪇ᩑᨹᨺᩀᩖᩘᩈᩣ᪃ᩝᩕ
    static public final List<Integer> TAI_THAM = (0x1A20..0x1AAD) - [0x1A5F, 0x1A7D, 0x1A7E] - (0x1A8A..0x1A8F) - (0x1A9A..0x1A9F)
    // (121 chars) : ᭥ᬙᬎ᭙ᬙ᭖ᬺ᭛ᬂ᭷ᬤᬡ᭹ᬢᬀ
    static public final List<Integer> BALINESE = (0x1B00..0x1B7C) - (0x1B4C..0x1B4F)
    // (72 chars) : ᮺ᮹ᮓᮎ᮪ᮺᮧᮺᮢ᮵ᮩᮜᮥᮧᮺ᳃᮹ᮧ᳄ᮓᮬ᳀ᮅᮙᮻᮅ
    static public final List<Integer> SUNDANESE = (0x1B80..0x1BBF) + (0x1CC0..0x1CC7)
    // (56 chars) : ᯏᯘᯥᯟᯑᯛᯤᯍᯧᯐᯐᯗᯠ᯦ᯩᯁᯫᯉᯫᯐᯇᯢᯫ᯼ᯅ
    static public final List<Integer> BATAK = (0x1BC0..0x1BFF) - (0x1BF4..0x1BFB)
    // (74 chars) : ᰘ᰿ᰛᰢ᱄ᰟᰌᰈᰃᰓᰬ᰿ᰝ᱃ᰁ᱅᰾ᰑ᱈ᰪ᰿ᰶ᰾ᰣᰛᰋ᱁ᰨᰬ
    static public final List<Integer> LEPCHA = (0x1C00..0x1C4F) - (0x1C38..0x1C3A) - (0x1C4A..0x1C4C)
    // OL CHIKI (48 chars) : ᱙᱐ᱦᱤᱫᱹ᱑ᱟᱠ᱙ᱸ᱒ᱩᱛᱠᱳᱼ᱖᱔ᱯᱢᱲᱽᱡᱢᱜᱦᱳᱢᱼ᱘ᱴ᱐
    static public final List<Integer> OL_CHIKI = (0x1C50..0x1C7F)
    // (42 chars) : ᳵ᳤ᳱᳲ᳣᳝ᳩᳲ᳥᳗᳗᳕᳚ᳲ᳔᳭᳝᳐ᳩ᳝᳜᳠ᳩᳬ᳜᳡ᳫᳱ᳢᳞᳛ᳳ᳝᳹ᳩ᳷᳨ᳱᳶ
    static public final List<Integer> VEDIC = (0x1CD0..0x1CF9)
    // (94 chars) : ⰡⰵⰬⱂⱜⰴⱍⰃⰓⰩⰎⱗⱘⰞⰟⰰⰥⱕⰰⱑⱛⰋⰛⰫⰺⰧ
    static public final List<Integer> GLAGOLITIC = (0x2C00..0x2C5E) - [0x2C2F]
    // (123 chars) : ⲄⲙⲣⲕⲼⳳⳋⲄⲜ⳺ⳭⲨⲨⲶⳐⳎⲄⲶⳋⲀⳉⳑⲃ⳰Ⳕ⳽ⲻⲙ
    static public final List<Integer> COPTIC = (0x2C80..0x2CF3) + (0x2CF9..0x2CFF) //SMP: + (0x102E0..0x102FB)
    // (59 chars) : ⵛⵘⴺ⵿ⵞⴾⵘⵠⵘⵃⵢⵑⴽ⵿ⴺⴻⵉⴷⵜⴴⵍⵘⵎⵘⵜⵟⴰⴳⵣⵆⴷ
    static public final List<Integer> TIFINAGH = (0x2D30..0x2D67) + [0x2D6F, 0x2D70, 0x2D7F]
    // (329 chars) : ⼚⼨⾩⾍⺲⿈⻆⺻⼭⺬⾝⻖⼕⺄⿀⼨
    static public final List<Integer> RADICALS = (0x2E80..0x2EF3) - [0x2E9A] + (0x2F00..0x2FD5)
    // (71 chars) : ㄨㆱㆴㄎㄢㄦㆹㆡㄈㆰㄡㆺㄍㆣㄓㄮ
    static public final List<Integer> BOPOMOFO = (0x3105..0x312F) + (0x31A0..0x31BB)
    // Hangul (666 chars) : ힺퟓᅹ㉆ퟄ㉿ꥵᆈᆑ㉭㊐㊯㉷㈄㉾ᇩ㈛
    static public final List<Integer> HANGUL = (0x1100..0x11FF) + (0x3131..0x318E) - [0x3164, 0x321F] + (0x3200..0x3250) +
        (0x3260..0x32B0) + (0xA960..0xA97C) + (0xD7B0..0xD7C6) + (0xD7CB..0xD7FB) +
        (0xFFA1..0xFFDC) - [0xFFBF, 0xFFC0, 0xFFC0, 0xFFC8, 0xFFC9, 0xFFD8, 0xFFD9] //Half witdh
    // Chinese (inc. Japanese Kanji, 27,558 chars) : 筥貴籙㗧熒鮼䈥駊虹蒤冽㬬
    static public final List<Integer> HANZU = (0x3400..0x4DB5) + (0x4E00..0x9FEF)
    // (879 chars) : 黎㎹粒磊省痢㌒嬨㎤㋂㋆奈㋔
    static public final List<Integer> IDEOGRAPHIC = (0x3190..0x319F) + (0x31C0..0x31E3) + (0x32C0..0x33FF) + (0xA700..0xA721) +
        (0xE801..0xE805) + (0xF900..0xFACE) + (0xFAD2..0xFAD4) + [0xFAD8, 0xFAD9]
    // Full Width Numbers (10 chars) : ６８１７３５５２０５８５０７１
    static public final List<Integer> FW_NUM = (0xFF10..0xFF19)
    // (26 chars) : ｆｍｋｚｃｉｏｆｗｂｗｈ
    static public final List<Integer> FW_LOW = (0xFF41..0xFF5A)
    // (26 chars) : ＲＸＡＦＭＫＺＣＩＯＦＷ
    static public final List<Integer> FW_UP = (0xFF21..0xFF3A)
    // (103 chars) : ６ｐＵＮ１Ｉ％ａＢｐ＝￥Ｌ＝ＷＴ
    static public final List<Integer> FULL_WIDTH = (0xFF01..0xFF60) + (0xFFE0..0xFFE6)
    // Korean (11,172 chars) : 김넾딓묡쌷픑콁쉟뀶웞툙쓭놭띠삚
    static public final List<Integer> KOREAN = (0xAC00..0xD7A3)
    // Hiragana (102 chars) : めがどとやゐこぷりぐどぱにㇹ
    static public final List<Integer> HIRAGANA = (0x31F0..0x31FF) + (0x3041..0x3096)
    // Katakana (84 chars) : ペゾァクビヴテゴオヮホデケグ
    static public final List<Integer> KATAKANA = (0x30A1..0x30F4)
    // (63 chars) : ﾊﾃｨﾔｹﾛﾞﾁ｣ﾛｿ･ﾆﾐﾈﾌﾇｧﾋｽﾒﾝ
    static public final List<Integer> HW_KATAKANA = (0xFF61..0xFF9F)
    // (1223 chars) : ꇕꎧꂾꈳꏨꆵꀘꈆꍨꀇꑎꐡꁥꀧꆧꁬꁒ
    static public final List<Integer> YI_SYLLABLE = (0xA000..0xA4C6)
    // (48 chars) : ꓺꓴꓫꓪꓷꓜꓙꓐꓦꓤꓫꓹꓑꓟꓠꓙꓸꓒꓩꓛꓠ
    static public final List<Integer> LISU = (0xA4D0..0xA4FF)
    // (300 chars) : ꗔꖤꔂꔮ꘤ꘋꕡꗲꕠꔘꕮꔴꖾꗘꔌꔑ
    static public final List<Integer> VAI = (0xA500..0xA62B)
    // (88 chars) : ꚿꛛꛄ꛷ꛧꛋꛚꛫꛡꛇꛃꚰꛘ꛶ꛈꛬꚧ꛰ꚪ꛱ꚴ
    static public final List<Integer> BAMUM = (0xA6A0..0xA6F7)
    // (45 chars) [**] : ꠍꠕꠊꠓ꠆ꠈꠦꠗꠐꠥꠋ꠨ꠎꠇꠙꠧꠖꠡꠢꠊꠟꠃꠒꠒꠑꠈꠕꠡꠘꠗꠥꠉꠔ
    static public final List<Integer> SYLOTI = (0xA800..0xA82C)
    // (56 chars) : ꡟꡑꡛꡤꡍꡧꡐꡐꡗꡠꡦꡩꡁꡫꡉꡫꡐꡇꡢꡫ꡴ꡅꡥꡍꡔꡋꡢꡱ
    static public final List<Integer> PHAGS_PA = (0xA840..0xA877)
    // (82 chars) : ꢩꢦꢗꢄꢾꢰꢿꢍꢎꢎꢑꢷꢜꢘꢽꢦꢴꢏꢪ
    static public final List<Integer> SAURASHTRA = (0xA880..0xA8C5) + (0xA8CE..0xA8D9)
    // (48 chars) : ꤝꤏꤎ꤬ꤪꤤꤛꤚꤧꤌ꤉꤀ꤖꤔꤛꤩ꤁ꤏꤐ꤉ꤨ꤂ꤙꤋꤐꤣ꤬꤆꤄ꤟꤒ
    static public final List<Integer> KAYAH = (0xA900..0xA92F)
    // (37 chars) : ꤵꥇꥈꤸꥑꥄꤿꥁꥎꤷꤾꥅꥊꤸꥒꤼ꥓ꥌꤳꥐꤷꥍꤰꥀ
    static public final List<Integer> REJANG = (0xA930..0xA953) + [0xA95F]
    // (91 chars) : ꦘ꧄꧔ꦕꦾꦈ꧅ꦕ꧃ꦓ꧕ꦾ꧑ꦙ
    static public final List<Integer> JAVA = (0xA980..0xA9DF) - [0xA9CE] - (0xA9DA..0xA9DD)
    // (83 chars) : ꨆꩁꨃꨥꨁꨇꩉ꩓ꨫꨙ꩙꩐ꨩ꩖ꨭꨌꨲ
    static public final List<Integer> CHAM = (0xAA00..0xAA36) + (0xAA40..0xAA5F) - [0xAA4E, 0xAA4F, 0xAA5A, 0xAA5B]
    // (79 chars) : ꯀꫨꯜꫭ꯸ꯂꫫꯧꫭ꯸ꯣꯤꫦꯐꯐꫲ꯶ꯖꯨꫠꯦꯅꯚ꯭ꫣꫫꯩꯆꯃꫦ
    static public final List<Integer> MEETEI = (0xAAE0..0xAAF6) + (0xABC0..0xABF9) - [0xABEE, 0xABEF]
    // Braille (255 chars) : ⡟⡂⣳⢛⢖⣗⠿⣿⢐⢵⢆⣴⠂⢊⡙⡜⡁⢾⣓⣙⣛⡮⢺⡨
    static public final List<Integer> BRAILLE = (0x2801..0x28FF)
    // Superscript (14 chars) : ⁸⁾⁸⁺ⁿ⁸⁸⁼⁷⁽⁸⁷⁾⁰⁹⁺⁷
    static public final List<Integer> SUPERSCRIPT = [0x2070, 0x2071] + (0x2074..0x207F)
    // (28 chars) : ₋ₕₑ₂ₒ₇ₙ₎₏₂₅₏ₘ₏
    static public final List<Integer> SUBSCRIPT = (0x2080..0x209C) - [0x208F]
    // (32 chars) : ₤₹₹₤€₴€₴₹₼₽₳₻₦₼₡₰₣₧
    static public final List<Integer> CURRENCY = (0x20A0..0x20BF)
    // (2552 chars) : ⊰⮫⚒◘⋓⪲⛱⏊✍ⅵ⟄
    static public final List<Integer> SYMBOLS = (0x2010..0x2027) + [0x203E, 0x2043, 0x20DB, 0x20DC, 0x20E6, 0x20E8] + (0x2030..0x205E) +
        (0x20D0..0x20F0) + (0x2100..0x218B) + (0x2190..0x2426) + (0x2440..0x244A) +
        (0x25A1..0x2775) + (0x2794..0x27FF) + (0x2900..0x2BFF) - [0x2B74, 0x2B75, 0x2B96] +
        (0x2E00..0x2E52) + (0x2FF0..0x2FFB) + (0x3001..0x303F) + (0x3099..0x30A0) +
        (0xA830..0xA839) + (0xAB66..0xAB6B) + [0xE83A] + (0xFB00..0xFB08) + (0xFB10..0xFB12) +
        (0xFE10..0xFE19) + (0xFE20..0xFE6B) - [0xFE53, 0xFE67] + (0xFFE8..0xFFEF) +
        (0xFFF9..0xFFFD)
    // Circled numbers (71 chars) : ㊱㊳⑭㊺➈㊲㉗➇⑦⓾⑲㉗⑥④⑳⓷
    //                                                  0         1-20
    static public final List<Integer> CIRCLE_NUMS = [0x24EA] + (0x2460..0x2473) +
    //       21-35              36-50         1-10 (single line) 1-10 (double line)
        (0x3251..0x325F) + (0x32B1..0x32BF) + (0x2780..0x2789) + (0x24F5..0x24FE)
    // Negative circled numbers (31 chars) : ❺➓⓱❻⓿➋❷❾⓫❻⓮➒➍⓱⓴
    //                                                      0           1-10              11-20              1-10 colored
    static public final List<Integer> CIRCLE_NEG_NUMS =  [0x24FF] + (0x2776..0x277F) + (0x24EB..0x24F4) + (0x278A..0x2793)
    // Numbers with parenthesis (20 chars) : ⑴⑸⑾⑴⒁⒂⑶⒆⑺⑺⒄⑵⒃⒀⑽⑽
    static public final List<Integer> PAREN_NUMS = (0x2474..0x2487) // 1-20
    // Lowercase letters with parenthesis (26 chars) : ⒯⒲⒩⒰⒮⒜⒞⒴⒭⒳⒜⒡⒨
    static public final List<Integer> PAREN_LOW = (0x249c..0x24B5)
    // Circled uppercase letters (26 chars) : ⒹⒼⓍⒺⒸⓀⓂⒾⒷⒽⓀⓅⓌⓊ
    static public final List<Integer> CIRCLE_UP = (0x24B6..0x24CF)
    // Circled lowercase letters (26 chars) : ⓣⓦⓝⓤⓢⓐⓒⓨⓡⓧⓐⓕⓜⓚⓩ
    static public final List<Integer> CIRCLE_LOW = (0x24D0..0x24E9)
    // Box drawing (128 chars) : ┃╾│╻╜┳┪┴┵╜┺╴┛┞═┡┡┎┵╜┽╘┲┶┈┉┫╗
    static public final List<Integer> BOX = (0x2500..0x257F)
    // Block drawing (33 chars) : ▇▖░▗▔▅▀▘▁▋▞▋▔▔▙▏▂▄▛▍▅▔▏▍▕▞▝▔▌▄▄▝▆▛▒▒
    static public final List<Integer> BLOCK = (0x2580..0x25A0)
    // Hexagram (64 chars) : ䷀䷎䷷䷎䷸䷜䷙䷉䷵䷪䷙䷹䷺䷹䷦䷂䷼䷄䷃䷡
    static public final List<Integer> HEXAGRAM = (0x4DC0..0x4DFF)
    // Non-displayable (use with caution) (2,048 chars)
    //static public final List<Integer> SURROGATES = (0xD800..0xDFFF) === DOESN'T ENCODE/DECODE CORRECTLY
    //Random characters which can or not display
    static public final List<Integer> PRIVATE = (0xE000..0xF8FF)
    //Any UTF8 char (65,536 chars) use with caution as many characters can not be rendered
    static public final List<Integer> UTF8 = 0x0..0xFFFF

    /* ************ COMBOS **************** */
    //0-9a-f //similar to md5, sh,a etc (but with variable length) (16 chars)
    // Sample: d86987e6f13634f4378325c8deabba
    static public final List<Integer> HASH = NUMBERS + aTof
    //0-9A-F //same as 'HASH' but uppercase (16 chars)
    // Sample: 56412AD4B5D66855E8779CE9F83AB8
    static public final List<Integer> HASH_UP = NUMBERS + AtoF
    //a-zA-Z (52 chars)
    // Sample: qzzndkvqmITTiQYYONLnOsY
    static public final List<Integer> ALPHA = LOWERCASE + UPPERCASE
    //0-9a-zA-Z (62 chars)
    // Sample: u3ngV1s3IpO4iJjkv5LRCpYjb7KV
    static public final List<Integer> ANUM = NUMBERS + LOWERCASE + UPPERCASE
    //0-9a-zA-Z=+/ (as result of Base64, etc) (64 chars)
    // Sample: +CrCsQN8peNtuta1w32V5gee=
    static public final List<Integer> BASE64 = ANUM + EQUALPLUS
    //Ascii Extended (support for accents and other common symbols) (815 chars)
    // Sample: ʛòàƃ͏ĔʥĻ̠ņġĥ̫ɕȴǍȫ̹̀ȸGőʕ˩«ačÚǘ̳̏ʠŔ
    static public final List<Integer> ASCII = BASIC + EXTENDED
    // Alphanumeric in a circle (154 chars)
    // Sample: ⒻⒻ⓮ⓔⓙ㊽ⓓ⓶㉟Ⓒ㊷Ⓥ㊼Ⓡ㉛➍ⓐ
    static public final List<Integer> CIRCLES = CIRCLE_LOW + CIRCLE_UP + CIRCLE_NUMS + CIRCLE_NEG_NUMS
    // Japanese, Korean, Chinese (40861 chars)
    // Sample: 눹㍔鹼셣怣쭮縑쓂渕緣业뙚湊矊损
    static public final List<Integer> CJK = RADICALS + BOPOMOFO + HANGUL + IDEOGRAPHIC + HANZU + KOREAN + HIRAGANA + KATAKANA
    // Languages from India (705 chars)
    // Sample: ఎ୧ୟꣾ५ூଌಜ౬৵एથऔા௰ಶ
    static public final List<Integer> INDIAN = DEVANAGARI + BENGALI + GUTMUKHI + GUJARATI + ORIYA + TAMIL + TELEGU + KANNADA
    // South-East Asia (1673 chars)
    // Sample: ᲔୱญოᲣฅఊනⴁഐ೑ౙव௺ಯ
    static public final List<Integer> SE_ASIA = MALAYAM + SINHALA + THAI + LAO + MYANMAR + TAI_VIET + INDIAN + GEORGIAN + BALINESE
    // Visible characters : safe option instead of UTF8 (48392 chars)
    // Sample: 蝼ꧽ䅛ෑ桻髂ႅ炙⣾傀㭧瞉䷄쫢뢰䶂㣨
    static public final List<Integer> VISIBLE = ASCII + GREEK + CYRILLIC + ARMENIAN + HEBREW + ARABIC + SYRIAC + THAANA +
        NKO + SAMARITAN + MANDAIC + INDIAN + SE_ASIA + TIBETAN + CJK + BRAILLE + SYMBOLS + HEXAGRAM - SPACE

    /*
        SMP: Supplementary Multilingual Plane
        NOTE: Be aware that the following blocks are formed with more bytes and it might require special handling
     */
    // (80 chars) : 𐅬𐅤𐆄𐅇𐅉𐅴𐆌𐆇𐅏𐆄𐆇𐅻𐅃𐅗𐅔𐅸𐅗𐅊𐅟𐅎𐅻𐅜𐅧𐅥𐅝𐆅𐆁𐅜𐅣
    static public final List<Integer> GREEK_SMP = (0x10140..0x1018E) + (0x101A0)
    // (13 chars) : 𐆚𐆙𐆘𐆜𐆑𐆒𐆛𐆛𐆑𐆓𐆖𐆐𐆒𐆖𐆕𐆔𐆘𐆜𐆖𐆔𐆐𐆘𐆚𐆐𐆐𐆛𐆓
    static public final List<Integer> ROMAN = (0x10190..0x1019C)
    // (57 chars) : 𐄎𐄖𐄱𐄠𐄼𐄲𐄊𐄯𐄊𐄝𐄉𐄔𐄔𐄢𐄕𐄜𐄟𐄤𐄀𐄸𐄍𐄚𐄖
    static public final List<Integer> AEGEAN = (0x10100..0x10102) + (0x10107..0x10133) + (0x10137..0x1013F)
    // (46 chars) : 𐇧𐇙𐇝𐇩𐇬𐇱𐇼𐇗𐇯𐇞𐇘𐇞𐇭𐇕𐇠𐇔𐇔𐇮𐇭𐇡𐇝𐇥𐇩𐇠𐇷𐇧𐇱𐇠𐇬𐇽𐇔𐇴𐇳𐇮𐇡𐇚
    static public final List<Integer> PHAISTOS = (0x101D0..0x101FD)
    // (29 chars) : 𐊖𐊖𐊒𐊕𐊕𐊌𐊚𐊊𐊔𐊐𐊃𐊓𐊆𐊘𐊏𐊎𐊃𐊄𐊎𐊙𐊎𐊅𐊃𐊕𐊃𐊊𐊍𐊘𐊙
    static public final List<Integer> LYCIAN = (0x10280..0x1029C)
    // (49 chars) : 𐊪𐊧𐋅𐊼𐋅𐊸𐋋𐋃𐊿𐊮𐊴𐋂𐋍𐊡𐋎𐊠𐋐𐊫𐊢𐊮𐊾
    static public final List<Integer> CARIAN = (0x102A0..0x102D0)
    // (36 chars) : 𐌏𐌓𐌃𐌆𐌄𐌘𐌆𐌑𐌉𐌔𐌛𐌕𐌢𐌅𐌐𐌄𐌃𐌓𐌈𐌂𐌅𐌍𐌗𐌙𐌡
    static public final List<Integer> OLD_ITALIC = (0x10300..0x10323)
    // (30 chars) : 𐌳𐌽𐍃𐌲𐍇𐍇𐌽𐌲𐌿𐍅𐍉𐌿𐌻𐌼𐌾𐍈
    static public final List<Integer> GOTHIC = (0x1032D..0x1034A)
    // (43 chars) [**] : 𐍢𐍰𐍐𐍭𐍲𐍔𐍦𐍰𐍯𐍟𐍫𐍺𐍟𐍠𐍚𐍰𐍠𐍪𐍨𐍨𐍞𐍷𐍐𐍘𐍥𐍕𐍢𐍰𐍶𐍛𐍡𐍘𐍟𐍚
    static public final List<Integer> OLD_PERMIC = (0x10350..0x1037A)
    // (31 chars) : 𐎙𐎈𐎑𐎘𐎜𐎀𐎔𐎝𐎋𐎘𐎌𐎉𐎂𐎑𐎒𐎟𐎛𐎃𐎝𐎁𐎛𐎚𐎑𐎎𐎛𐎅𐎋𐎋𐎍𐎋𐎖𐎐𐎇𐎚𐎈𐎙𐎔
    static public final List<Integer> UGARTIC = (0x10380..0x1039D) + [0x1039F]
    // (51 chars) : 𐏓𐎫𐏎𐏕𐏎𐎢𐎹𐎷𐎮𐎪𐎿𐏑𐎼𐎼𐎹𐎹𐏒𐏐𐏔𐎳
    static public final List<Integer> OLD_PERSIAN = (0x1039F..0x103D5) - (0x103C4..0x103C7)
    // (80 chars) : 𐐄𐐗𐐙𐑄𐐌𐐇𐐟𐐄𐐇𐑋𐐓𐐧𐐤𐑈𐐧𐐚𐐯𐐞𐑋𐐬𐐷𐐵𐐭𐐅𐐁𐐬𐐳𐐂
    static public final List<Integer> DESERET = (0x10400..0x1044F)
    // (48 chars) : 𐑻𐑵𐑪𐑫𐑶𐑝𐑘𐑑𐑧𐑥𐑪𐑸𐑐𐑞𐑡𐑘𐑹𐑓𐑨𐑚𐑡𐑲𐑽𐑗𐑕𐑮
    static public final List<Integer> SHAVIAN = (0x10450..0x1047F)
    // (40 chars) : 𐒊𐒆𐒩𐒋𐒥𐒂𐒊𐒘𐒕𐒕𐒖𐒠𐒝𐒣𐒅𐒙𐒙𐒄𐒋𐒘𐒕𐒗𐒁𐒑𐒐𐒋𐒎𐒚𐒨
    static public final List<Integer> OSMANYA = (0x10480..0x104A9) - [0x1049E,0x1049F]
    // (72 chars) : 𐒻𐓯𐓬𐓂𐒿𐓟𐓯𐓚𐓯𐓓𐓨𐓜𐓍𐓘𐓚𐓯𐓶𐓬𐓚𐓹𐓂𐓡𐓵𐒴
    static public final List<Integer> OSAGE = (0x104B0..0x104D3) + (0x104D8..0x104FB)
    // (40 chars) [**] : 𐔆𐔧𐔋𐔣𐔂𐔊𐔘𐔕𐔕𐔖𐔞𐔝𐔡𐔅𐔙𐔙𐔄𐔋𐔘𐔕𐔗𐔁𐔑𐔐𐔋𐔎𐔚𐔦𐔉𐔦𐔍𐔟𐔕𐔚𐔜𐔘
    static public final List<Integer> ELBASAN = (0x10500..0x10527)
    // Caucasian Albanian (53 chars) : 𐕚𐔰𐔷𐔾𐔶𐕢𐕡𐕁𐔼𐕄𐕍𐕈𐕒𐕖𐕏𐕁𐕠𐔺𐕇𐕑𐕀𐕅𐕘𐕓𐔶𐔿𐕞𐕯𐕜𐕁𐕐𐕟𐔻𐕘𐔸𐕡
    static public final List<Integer> ALBANIAN = (0x10530..0x10563) + [0x1056F]
    // (55 chars) : 𐠯𐠃𐠮𐠃𐠮𐠕𐠄𐠏𐠑𐠥𐠿𐠍𐠘𐠖𐠂𐠩𐠯𐠱𐠍𐠟𐠨𐠑𐠰𐠟𐠤𐠿𐠟
    static public final List<Integer> CYPRIOT = (0x10800..0x10838) - [0x10806,0x10807,0x10809,0x10836] + [0x1083C, 0x1083F]
    // Imperial Aramaic (31 chars) : 𐡔𐡜𐡋𐡛𐡌𐡉𐡂𐡑𐡒𐡞𐡚𐡃𐡜𐡁𐡚𐡝𐡑𐡎𐡚𐡅𐡋𐡋𐡍𐡋𐡙𐡐𐡇𐡝𐡈𐡘𐡔
    static public final List<Integer> ARAMAIC = (0x10840..0x1085F) - [0x10856]
    // (32 chars) [**] : 𐡸𐡥𐡭𐡵𐡭𐡵𐡸𐡽𐡼𐡲𐡺𐡧𐡽𐡠𐡱𐡢𐡦𐡯𐡭𐡵𐡸𐡿𐡫𐡥𐡳𐡭𐡶𐡰𐡥𐡥𐡰𐡶𐡳𐡬𐡿𐡹𐡱
    static public final List<Integer> PALMYRENE = (0x10860..0x1087F)
    // (40 chars) [**] : 𐢚𐢊𐢆𐢯𐢋𐢫𐢂𐢊𐢘𐢕𐢕𐢖𐢧𐢝𐢩𐢅𐢙𐢙𐢄𐢋𐢘𐢕𐢗𐢁𐢑𐢐𐢋𐢎𐢚𐢮𐢉
    static public final List<Integer> NABATAEAN = (0x10880..0x1089E) + (0x108A7..0x108AF)
    // (26 chars) [**] : 𐣫𐣾𐣣𐣩𐣯𐣤𐣽𐣠𐣽𐣦𐣱𐣻𐣵𐣨𐣢𐣵𐣧𐣪𐣮𐣲𐣴𐣼𐣻𐣮𐣨𐣯𐣭𐣫𐣰𐣩𐣬
    static public final List<Integer> HATRAN = (0x108E0..0x108F5) - [0x108F3] + (0x108FB..0x108FF)
    // (29 chars) : 𐤆𐤘𐤏𐤎𐤃𐤄𐤎𐤙𐤎𐤅𐤃𐤕𐤃𐤊𐤍𐤘𐤙𐤆𐤁𐤙𐤟𐤃𐤍𐤓𐤋𐤊𐤕𐤈𐤊𐤅𐤀𐤉𐤇𐤐𐤀𐤔𐤉𐤋𐤈𐤖
    static public final List<Integer> PHOENICIAN = (0x10900..0x1091B) + [0x1091F]
    // (27 chars) : 𐤵𐤠𐤩𐤷𐤩𐤪𐤦𐤷𐤯𐤰𐤱𐤢𐤿𐤪𐤰𐤶𐤵𐤱𐤵𐤤𐤧𐤿𐤱𐤩𐤵𐤴𐤿𐤳𐤡𐤠𐤦𐤯𐤧𐤱𐤵𐤪𐤫𐤬𐤹
    static public final List<Integer> LYDIAN = (0x10920..0x10939) + [0x1093F]
    // (242 chars) Hieroglyphic : 𐥦𐧮𐥴𐤬𐤦𐧵𐧍𐧿𐥯𐦡𐤬𐦪𐧉𐦦𐦜𐥃𐧠𐦫𐧓𐧉
    static public final List<Integer> MEROITIC = (0x10908..0x109B7) + (0x109BC..0x109FF) - [0x109D0,0x109D1]
    // (68 chars) : 𐨅𐩓𐩄𐨐𐨎𐨣𐨢𐨛𐨁𐨛𐨮𐨢𐨃𐨃𐨢𐨲𐩄𐨌𐨱𐨤𐩀𐩗𐨸𐨏𐨐𐨸𐨟𐩒𐨁𐩄𐨭𐨑𐩔𐨌𐨥𐩈
    static public final List<Integer> KHAROSHTHI = (0x10A00..0x10A03) + [0x10A05,0x10A06] + (0x10A0C..0x10A13) +
                                                   (0x10A15..0x10A17) + (0x10A19..0x10A35) + (0x10A38..0x10A3A) +
                                                   (0x10A3F..0x10A48) + (0x10A50..0x10A58)
    // (64 chars) : 𐪁𐪏𐩶𐪏𐩹𐪝𐪘𐪈𐩴𐩫𐪘𐩸𐩻𐩸𐩧𐪃𐩽𐪅𐪂𐩠𐪇
    static public final List<Integer> OLD_ARABIAN = (0x10A60..0x10A9F)
    // (51 chars) : 𐫐𐫑𐫳𐫴𐫍𐫄𐫛𐫘𐫮𐫜𐫦𐫇𐫣𐫬𐫵𐫑𐫝𐫉𐫏𐫠𐫝𐫢
    static public final List<Integer> MANICHEAN = (0x10AC0..0x10AE6) + (0x10AEB..0x10AF6)
    // (61 chars) : 𐬼𐬒𐬛𐬞𐬍𐬇𐬰𐬁𐬑𐬵𐬘𐬪𐬛𐬞𐬼𐬡𐬪𐬡𐬮
    static public final List<Integer> AVESTAN = (0x10B00..0x10B35) + (0x10B39..0x10B3F)
    // (30 chars) [**] : 𐭝𐭐𐭟𐭅𐭉𐭟𐭛𐭜𐭞𐭈𐭊𐭆𐭂𐭁𐭕𐭕𐭟𐭏𐭃𐭀𐭏𐭕𐭏𐭉𐭇𐭞𐭚𐭆𐭑𐭛𐭟𐭎𐭉𐭓𐭘𐭐𐭞𐭑𐭟𐭊
    static public final List<Integer> PARTHIAN = (0x10B40..0x10B55) + (0x10B58..0x10B5F)
    // (56 chars) [**] : 𐭲𐮎𐮪𐭩𐭣𐮐𐮯𐭦𐮌𐭫𐮑𐮑𐮫𐭢𐭨𐭭𐮀𐭯𐮈𐭯𐮑𐮆𐭤𐭯𐭽𐮄𐭩𐮌𐮚𐮊𐭤𐭸𐮙𐭮𐭹𐮐𐭡𐮋𐭥
    static public final List<Integer> PAHLAVI = (0x10B60..0x10B72) + (0x10B78..0x10B91) + (0x10B99..0x10B9C) + (0x10BA9..0x10BAF)
    // (73 chars) : 𐰉𐱃𐰅𐰶𐰃𐰰𐰬𐰴𐰯𐰑𐰰𐰋𐰩𐰐𐰙𐱆𐰠𐰷𐰟𐰱𐰤𐰤𐱂𐰕𐰢𐰨𐰸𐰔𐰤𐰻𐰷𐰍𐰻𐰾𐰹
    static public final List<Integer> OLD_TURKIC = (0x10C00..0x10C48)
    // (158 chars) : 𐲊𐳥𐳚𐴳𐲙𐴸𐲚𐳡𐳻𐲄𐲄𐴱𐲃𐳎𐴅𐳕𐳱𐳨𐳕𐳓𐲇𐳝𐲇
    static public final List<Integer> OLD_HUNGARIAN = (0x10C80..0x10CB2) + (0x10CC0..0x10CF2) + (0x10CFA..0x10D27) + (0x10D30..0x10D39)
    // (31 chars) : 𐹺𐹱𐹮𐹻𐹥𐹫𐹫𐹭𐹫𐹶𐹰𐹧𐹺𐹨𐹹𐹴𐹨𐹳𐹩𐹶𐹦𐹹𐹺𐹫𐹢𐹭𐹷
    static public final List<Integer> RUMI = (0x10E60..0x10E7E)
    // (82 chars) [**] : 𐼌𐼏𐼏𐼐𐼾𐼝𐼙𐽄𐼧𐼽𐼎𐼳𐼘𐼛𐽐𐼠𐽆𐽏𐼡𐼝𐼃𐼽𐼵𐼖𐽈𐽖𐼇𐼦𐼚𐽑𐼥𐼳𐼓𐼓𐼣
    static public final List<Integer> SOGDIAN = (0x10F00..0x10F27) + (0x10F30..0x10F59)
    // (109 chars) : 𑁩𑀿𑁖𑁄𑀜𑀯𑁃𑁖𑁠𑀫𑁉𑀫𑁯𑀲𑀺𑀍𑀊𑀵𑁉𑀔𑁚𑀡𑁅𑁧𑀟𑀬𑁧𑀯𑀅
    static public final List<Integer> BRAHMI = (0x11000..0x1104D) + (0x11052..0x1106F) + [0x1107F]
    // (66 chars) : 𑂩𑂥𑂉𑂵𑂧𑂿𑂛𑂸𑂄𑂤𑂏𑂳𑂋𑂄𑂉𑂣𑂆𑂙𑂜𑂔𑂕𑂳𑂱𑂛𑂫𑂸𑂴𑂍𑂴𑂿
    static public final List<Integer> KAITHI = (0x11080..0x110C1)
    // (36 chars) [**] : 𑃕𑃨𑃗𑃞𑃖𑃥𑃱𑃢𑃸𑃒𑃡𑃕𑃐𑃠𑃙𑃓𑃒𑃚𑃤𑃦𑃷𑃐𑃤𑃨𑃓𑃍𑃹
    static public final List<Integer> SORA_SOMPENG = [0x110CD] + (0x110D0..0x110E8) + (0x110F0..0x110F9)
    // (70 chars) : 𑄩𑄗𑄊𑅆𑄯𑄈𑄉𑄹𑄸𑄔𑄱𑅅𑄉𑄛𑄻𑄚𑄼𑄲𑄡𑄼𑄒𑅂𑅁𑄻𑄊𑄒𑄊𑄊𑄽𑄏𑄷𑄖𑄿𑄱𑄕𑅄𑄴𑄦
    static public final List<Integer> CHAKMA = (0x11100..0x11134) + (0x11136..0x11146)
    // (39 chars) [**] : 𑅩𑅤𑅡𑅤𑅩𑅬𑅴𑅠𑅑𑅥𑅶𑅞𑅱𑅨𑅞𑅳𑅮𑅣𑅘𑅖𑅗𑅩𑅩𑅣𑅟
    static public final List<Integer> MAHAJANI = (0x11150..0x11176)
    // (96 chars) : 𑇃𑆲𑇝𑆐𑇙𑆰𑇏𑇘𑆻𑆓𑆺𑆢𑆱𑆑𑇓𑇟𑆝𑇒𑆟𑆒𑆍𑇎𑆓𑆧𑆅𑆄𑆼𑆝𑇝𑆔𑇕𑇔𑆿
    static public final List<Integer> SHARADA = (0x11180..0x111DF)
    // (38 chars) : 𑇳𑇳𑇤𑇯𑇬𑇯𑇧𑇤𑇨𑇲𑇱𑇮𑇪𑇮𑇭𑇴𑇮𑇧𑇦𑇩𑇨
    static public final List<Integer> SHINHALA = (0x111E1..0x111F4)
    // (62 chars) : 𑈢𑈯𑈧𑈆𑈔𑈡𑈼𑈎𑈈𑈥𑈨𑈯𑈂𑈻𑈉𑈕𑈁𑈛𑈨𑈨𑈳𑈷𑈉𑈩
    static public final List<Integer> KHOJKI = (0x11200..0x1123E) - [0x11212]
    // (38 chars) [**] : 𑊊𑊣𑊡𑊛𑊧𑊊𑊘𑊝𑊍𑊙𑊐𑊑𑊊𑊧𑊨𑊟𑊦𑊀𑊏𑊖𑊍𑊋𑊖𑊡𑊥𑊆𑊝𑊄𑊢𑊡
    static public final List<Integer> MULTANI = (0x11280..0x112A9) - [0x11287,0x11289,0x1128E,0x1129E]
    // (69 chars) : 𑊶𑋥𑊼𑋀𑋖𑋔𑋖𑊶𑋘𑋷𑋄𑊸𑊳𑋓𑋄𑋑𑊺𑋰𑋳𑋰𑋦𑋏𑋈𑋸𑋪𑋑𑋥𑋸
    static public final List<Integer> KHUDAWADI = (0x112B0..0x112EA) + (0x112F0..0x112F9)
    // (85 chars) : 𑌖𑌂𑌽𑍰𑌖𑌌𑌆𑍟𑍝𑍰𑌚𑌲𑍢𑌿𑌓𑍞𑌢𑍃𑌌𑍍𑍴𑌞𑍡𑌮𑍧𑍦𑌓𑌧𑌁𑌈
    static public final List<Integer> GRANTHA = (0x11300..0x11303) + (0x11305..0x1130C) + (0x1130F..0x11310) + (0x11313..0x11328) +
                                                (0x1132A..0x11330) + [0x11332,0x11333] + (0x11335..0x11339) + (0x1133C..0x11344) +
                                                [0x11347,0x11348,0x1134B,0x1134C,0x1134D,0x11350,0x11357] + (0x1135D..0x11363) +
                                                (0x11366..0x1136C) + (0x11370..0x11374)
    // (97 chars) : 𑐈𑑙𑐿𑐑𑐓𑑗𑐎𑐳𑐭𑐏𑐩𑑏𑐻𑐚𑐬𑐼𑐨𑑘𑑉𑐞𑑐𑑅𑐮𑐢𑐇𑑀𑐁𑐘
    static public final List<Integer> NEWA = (0x11400..0x11461) - [0x1145C]
    // (82 chars) : 𑒨𑒧𑒖𑒅𑒿𑒱𑒾𑒌𑒏𑒏𑒐𑒶𑒝𑒙𑒼𑒧𑒵𑒎𑒫𑒘𑒛𑓐𑒠𑒾𑓇𑒡𑒝𑒃
    static public final List<Integer> TIRHUTA = (0x11480..0x114C7) + (0x114D0..0x114D9)
    // (92 chars) : 𑗝𑗅𑗓𑖉𑖱𑖣𑖘𑖑𑖖𑖵𑗝𑖨𑖏𑖖𑗆𑗅𑖖𑗇𑖕𑖓𑖴𑗗𑖗𑗖𑖾𑖎𑗑𑗖𑖛
    static public final List<Integer> SIDDHAM = (0x11580..0x115B5) + (0x115B8..0x115DD)
    // (79 chars) : 𑘭𑘑𑘑𑙃𑙗𑘗𑘰𑘡𑘬𑘄𑘛𑘺𑘧𑘷𑘲𑘇𑘂𑘭𑙘𑘙𑘫𑘿𑙒𑙓𑘁𑘤𑘕𑘢𑙒𑙁𑙀𑘍
    static public final List<Integer> MODI = (0x11600..0x11644) + (0x11650..0x11659)
    // (67 chars) : 𑚌𑚌𑚢𑚤𑚩𑛆𑛅𑚏𑚅𑚵𑚈𑚏𑚃𑛂𑚅𑚲𑚓𑚲𑚷𑚔𑚚𑚋𑚨𑚕𑚸𑚧𑚰
    static public final List<Integer> TAKRI = (0x11680..0x116B8) + (0x116C0..0x116C9)
    // (58 chars) : 𑜗𑜏𑜰𑜡𑜐𑜶𑜓𑜎𑜄𑜃𑜢𑜏𑜌𑜕𑜌𑜌𑜺𑜘𑜈𑜳𑜂𑜂𑜼𑜠𑜁𑜰𑜋𑜲
    static public final List<Integer> AHOM = (0x11700..0x1171A) + (0x1171D..0x1172B) + (0x11730..0x1173F)
    // (84 chars) : 𑣓𑣿𑣄𑢲𑢨𑣬𑣛𑣇𑢱𑢮𑢲𑢮𑣲𑣑𑢪𑢧𑣔𑣐𑢽𑢢𑢶𑢣𑣨𑣈𑢬𑢫𑣜𑣭𑢠
    static public final List<Integer> WARANG_CITI = (0x118A0..0x118F2) + [0x118FF]
    // (72 chars) : 𑨀𑨖𑨥𑨋𑨻𑨸𑨒𑨏𑨫𑨻𑨦𑨻𑨣𑨴𑨨𑨝𑨤𑨦𑨻𑩂𑨸𑨦𑩅𑨒𑨭
    static public final List<Integer> ZANABAZAR = (0x11A00..0x11A47)
    // (83 chars) : 𑩗𑩹𑩛𑪍𑩞𑩲𑪖𑪁𑩝𑪃𑩧𑩢𑩯𑩴𑩣𑪀𑩷𑩐𑩐𑪛𑩝𑩜𑪕𑩦𑪍𑩦𑩒
    static public final List<Integer> SOYOMBO = (0x11A50..0x11AA2)
    // (57 chars) : 𑫭𑫜𑫷𑫮𑫆𑫫𑫆𑫙𑫅𑫐𑫐𑫞𑫑𑫘𑫛𑫠𑫀𑫳𑫉𑫖𑫒𑫢𑫞𑫞𑫮𑫑
    static public final List<Integer> PAU_CIN_HAU = (0x11AC0..0x11AF8)
    // (97 chars) : 𑱡𑱙𑰝𑱞𑰋𑱥𑱁𑰐𑰒𑱣𑰑𑰲𑰬𑰎𑰨𑱛𑰽𑰝𑰯𑰾𑰫𑱤
    static public final List<Integer> BHAIKSUKI = (0x11C00..0x11C45) - [0x11C09,0x11C37] + (0x11C50..0x11C6C)
    // (68 chars) : 𑲃𑱴𑲈𑱲𑲦𑱻𑲭𑱺𑲅𑲁𑲖𑲜𑱿𑲦𑱼𑱲𑲅𑱸𑱲𑲊
    static public final List<Integer> MARCHEN = (0x11C70..0x11C8F) + (0x11C92..0x11CB6) - [0x11CA8]
    // (1235 chars) : 𒀩𒓲𒍜𒄅𒊠𒅳𒂝𒃽𒌮𒐣𒁂𒍾
    static public final List<Integer> CUNEIFORM = (0x12000..0x12399) + (0x12400..0x12474) + (0x12480..0x12543)
    // (1071 chars) Hieroglyphic : 𓂷𓎁𓋤𓉗𓁾𓂞𓋮𓁢𓆳𓇈𓋶𓏁𓊙𓃨𓊇𓀿𓏌
    static public final List<Integer> EGYPTIAN = (0x13000..0x1342E)
    // (583 chars) Hieroglyphic : 𔒶𔒋𔔣𔕒𔓟𔔄𔐩𔐭𔒥𔐖𔖥𔓿𔒗𔘵𔕢𔑛𔖂𔐗𔘐𔑷𔙁
    static public final List<Integer> ANATOLIAN = (0x14400..0x14646)
    // (570 chars) : 𖦷𖦷𖢍𖢒𖠝𖤅𖦕𖠥𖨩𖡖𖧃𖦍𖢯𖤙𖧫𖦘𖨭𖡷𖧆𖣽𖢏𖣨
    static public final List<Integer> BAMUM_SMP = (0x16800..0x16A39)
    // (136 chars) [**] : 𖩖𖩣𖩞𖩏𖩛𖩮𖩏𖩐𖩊𖩣𖩐𖩚𖩘𖩘𖩎𖩦𖩀𖩈𖩕𖩅𖩒𖩣𖩩𖩋𖩑𖩈𖩏𖩊𖩕𖩨𖩚
    static public final List<Integer> MRO = (0x16A40..0x16A69) - [0x16A5F] + [0x16A6E,0x16A6F]
    // (142 chars) [**] : 𖫨𖫖𖫡𖫙𖫤𖫫𖫥𖫴𖫕𖫠𖫔𖫓𖫣𖫘𖫒𖫕𖫝𖫧𖫩𖫳𖫓𖫧𖫨𖫒𖫑𖫵𖫴𖫐𖫙𖫚𖫓𖫨𖫜𖫖
    static public final List<Integer> BASSA_VAH = (0x16AD0..0x16AED) + (0x16AF0..0x16AF5)
    // (127 chars) : 𖬔𖮋𖬢𖬽𖬘𖭿𖭠𖭜𖭫𖬰𖬠𖭅𖬦𖭝𖭳𖭝𖬀𖭑𖭅𖮃𖭱𖬰𖬘𖬛𖬡𖬷𖬹𖬩𖭃𖭭𖬼𖬴
    static public final List<Integer> PAHAWH_HMONG = (0x16B00..0x16B45) + (0x16B50..0x16B77) - [0x16B5A,0x16B62] + (0x16B7D..0x16B8F)
    // (149 chars) : 𖼇𖼜𖽲𖽲𖾜𖽑𖼷𖼹𖽭𖽆𖼱𖼩𖾝𖼷𖽆𖾂𖼦𖼜𖽭𖾙𖽩𖽔𖼱𖼛𖽈𖽕𖽰
    static public final List<Integer> MIAO = (0x16F00..0x16F4A) + (0x16F4F..0x16F87) + (0x16F8F..0x16F9F)
    // (6892 chars) : 𗸓𘏶𗜛𗤭𗷕𗛋𗞆𘋑𘏁𗋩𘪉𗕤𗾤𘗗
    static public final List<Integer> TANGUT = [0x16FE0] + (0x17000..0x187F7) + (0x18800..0x18AF2)
    // (396 chars) [**] : 𛉭𛈞𛆤𛊀𛅷𛈠𛋺𛊪𛉸𛉒𛆏𛆹𛉊𛈙𛅼𛈞𛈖𛉢𛇯
    static public final List<Integer> NUSHU = (0x1B170..0x1B2FB)
    // (144 chars) : 𛱟𛱼𛰕𛱗𛲒𛱨𛰴𛱋𛰟𛰈𛰺𛲀𛱞𛱱𛰥𛲜𛲃𛰐𛰩𛰸𛰭𛱡𛱶𛰄𛱥
    static public final List<Integer> DUPLOYAN = (0x1BC00..0x1BC6A) + (0x1BC70..0x1BC7C) + (0x1BC80..0x1BC88) + (0x1BC90..0x1BC9F) - [0x1BCAA,0x1BCAB,0x1BC9D]
    // (549 chars) : 𝈨𝂬𝆴𝁴𝂯𝀧𝅬𝃍𝈹𝁅𝇖𝈾𝈋𝈽𝆗𝅘𝅥𝅱𝇌𝁢𝇆𝅘𝅥𝅲𝅐𝅘𝀜𝃤𝁺𝅈𝄻𝀯𝅭𝅆𝆢
    static public final List<Integer> MUSICAL = (0x1D000..0x1D0F5) + //Byzantine
                                                (0x1D100..0x1D1EA) - [0x1D127,0x1D128] + //General
                                                (0x1D200..0x1D245) //Greek
    // (20 chars) : 𝋮𝋯𝋫𝋯𝋪𝋲𝋯𝋤𝋧𝋦𝋩𝋠𝋥𝋲𝋯𝋤𝋰
    static public final List<Integer> MAYAN = (0x1D2E0..0x1D2F3)
    // (87 chars) * Similar to HEXAGRAMS : 𝌴𝍏𝌻𝍍𝍇𝌙𝌯𝌖𝌳𝌓𝌺𝌎𝌌𝌍𝌔𝍔𝍓𝌐
    static public final List<Integer> TAIXUANJING = (0x1D300..0x1D356)
    // (25 chars) : 𝍫𝍵𝍢𝍪𝍬𝍴𝍷𝍧𝍨𝍩𝍰𝍮𝍢𝍤𝍴𝍢𝍸𝍦𝍧𝍢
    static public final List<Integer> COUNTING = (0x1D360..0x1D378)
    // (1020 chars) : 𝟉𝛸𝛞𝛑𝑃𝐲𝛾𝙟𝔶𝛙𝗞𝐎𝔃𝔉𝜭𝗐𝗖
    static public final List<Integer> MATH = (0x1D400..0x1D7FF) - [0x1D6A6,0x1D6A7,0x1D7CD,0x1D7CE]
    // (38 chars) [**] : 𞀓𞀕𞀗𞀨𞀧𞀐𞀈𞀠𞀛𞀡𞀄𞀋𞀠𞀞𞀘𞀦𞀋𞀖𞀝𞀊𞀗𞀎𞀏𞀋𞀦𞀪𞀟𞀩𞀀𞀌𞀔𞀊𞀈𞀔𞀞𞀤𞀆𞀝𞀄𞀣𞀞𞀣𞀌𞀖𞀦𞀣𞀤𞀌𞀟𞀔𞀅𞀃𞀉𞀉𞀐𞀛𞀃𞀋𞀣𞀄𞀐𞀩𞀀𞀉𞀘𞀦𞀍𞀧𞀛𞀁
    static public final List<Integer> GLAGOLITIC_SMP = (0x1E000..0x1E02A) - [0x1E007,0x1E019,0x1E01A,0x1E022,0x1E025]
    // (71 chars) [**] : 𞄟𞄂𞅁𞄜𞅏𞄉𞄣𞄏𞄻𞄜𞄄𞄅𞅁𞄟𞅆𞄨𞄡𞄔𞄖𞄅𞅅𞄽𞅀𞄕𞄔
    static public final List<Integer> NYIAKENG = (0x1E100..0x1E12C) + (0x1E130..0x1E13D) + (0x1E140..0x1E149) + [0x1E14E,0x1E14F]
    // (59 chars) : 𞋪𞋩𞋋𞋿𞋯𞋏𞋩𞋱𞋩𞋒𞋳𞋠𞋌𞋿𞋋𞋊𞋘𞋆𞋭𞋅𞋜𞋩𞋟𞋩𞋭𞋮𞋁𞋂𞋲
    static public final List<Integer> WANCHO = (0x1E2C0..0x1E2F9) + [0x1E2FF]
    // (213 chars) : 𞣀𞡵𞡨𞢭𞠂𞡺𞡄𞢟𞢂𞠐𞡵𞡓𞡩𞠨𞡹𞡻𞠑𞠪𞢃𞡥𞢚𞠉
    static public final List<Integer> MENDE_KIKAKUI = (0x1E800..0x1E8D6) - [0x1E8C5,0x1E8C6]
    // (88 chars) : 𞥟𞤩𞥑𞤆𞥕𞤋𞥔𞤕𞤢𞥔𞤱𞤓𞤲𞤄𞤆𞤘𞤧𞤩𞤣𞥑𞤴𞥓𞤳𞤲𞤚𞤪𞤀𞥄𞤺𞤱𞤁𞤢𞥟
    static public final List<Integer> ADLAM = (0x1E900..0x1E94B) + (0x1E950..0x1E959) + [0x1E95E,0x1E95F]
    // (68 chars) :           𞲮𞲗𞱻𞲁𞱲
    static public final List<Integer> SIYAQ = (0x1EC71..0x1ECB4)
    // (143 chars) : 𞸛𞹶𞺘𞺥𞺒𞺯𞺦𞺬𞺴𞺌𞹡𞸮𞹇𞺷𞺖𞹹𞹵
    static public final List<Integer> ARABIC_SMP = (0x1EE00..0x1EEBB) + [0x1EEF0,0x1EEF1] - [0x1EE04,0x1EE20,0x1EE23,0x1EE25,0x1EE26,
                                                                                             0x1EE28,0x1EE33,0x1EE38,0x1EE3A,0x1EE48,
                                                                                             0x1EE4A,0x1EE4C,0x1EE50,0x1EE53,0x1EE55,
                                                                                             0x1EE56,0x1EE58,0x1EE5A,0x1EE5C,0x1EE5E,
                                                                                             0x1EE60,0x1EE63,0x1EE65,0x1EE66,0x1EE6B,
                                                                                             0x1EE73,0x1EE78,0x1EE7D,0x1EE7F,0x1EE8A,
                                                                                             0x1EEA4,0x1EEAA] -
                                                    (0x1EE3C..0x1EE41) - (0x1EE43..0x1EE46) - (0x1EE9C..0x1EEA0)
    // (44 chars) : 🀔🀍🀛🀏🀪🀖🀃🀔🀍🀍🀚🀪🀈🀅🀙🀇
    static public final List<Integer> MAHJONG = (0x1F000..0x1F02B)
    // (100 chars) : 🂂🁰🁏🁵🁭🂀🁥🀹🁉🀸🀱🁠🁲🂍🀻🁱🂒🁌🀻🀴🀴🁼🀱🁩🀸🁐🁨🁌🀷
    static public final List<Integer> DOMINO = (0x1F030..0x1F093)
    // (82 chars) : 🂿🃅🃓🂹🃠🃍🃈🂸🂥🃣🃕🃢🂬🂮🂮🂲🃚
    static public final List<Integer> CARDS = (0x1F0A0..0x1F0F5) - [0x1F0AF,0x1F0B0,0x1F0C0,0x1F0D0]
    // (84 chars) : 🨊🨇🨴🨰🨝🨂🨖🨃🩈🨨🨌🨋🨼🩍🨀🨿🨏🨩🨦
    static public final List<Integer> CHESS = (0x1FA00..0x1FA53)
    // (14 chars) : 🩧🩭🩧🩩🩬🩧🩧🩫🩤🩪🩧🩤🩭
    static public final List<Integer> CHESS_CH = (0x1FA60..0x1FA6D)
    // (10 chars) : 🄅🄅🄆🄈🄃🄉🄅🄇🄇🄂🄁🄇🄈🄇🄁🄉🄃🄄🄃🄊🄈🄃🄅
    static public final List<Integer> COMMA_NUM = (0x1F101..0x1F10A)
    // (26 chars) : 🄖🄡🄤🄢🄘🄒🄢🄗🄚🄞🄣🄥🄦🄤
    static public final List<Integer> PAREN_UP = (0x1F110..0x1F129)
    // (26 chars) : 🄺🄸🅈🅂🄸🅇🄰🄴🄹🄻🄼🄺🄴🅈🄵🄳🄱🄶🅉🄲
    static public final List<Integer> SQUARE_UP = (0x1F130..0x1F149)
    // (26 chars) : 🅧🅐🅧🅖🅡🅤🅢🅘🅒🅢🅗🅚🅞🅣🅥🅦🅤🅞🅘🅟
    static public final List<Integer> CIRCLE_UP_NEG = (0x1F150..0x1F169)
    // (26 chars) : 🅸🆇🅰🅴🅹🅻🅼🅺🅴🆈🅵🅳🅱🅶🆉🅲🅶🅾🅱🆄🆇🆀🆃🆆🆀
    static public final List<Integer> SQUARE_UP_NEG = (0x1F170..0x1F189)
    // (150 chars) : 🠘🠊🠝🡙🠝🢫🠇🠴🡫🠼🠤🠧🠗🢗🠩🢀🡖🠽🠋🢟🡴🠿🢝🢔🠓🢑🡡🡿🢅🠶
    static public final List<Integer> ARROWS = (0x1F800..0x1F847) - (0x1F80C..0x1F80F) + (0x1F850..0x1F859) + (0x1F860..0x1F887) + (0x1F890..0x1F8AD) + [0x1F8B0,0x1F8B1]
    // (654 chars) : 𐚓🄋🙔𐙵🜶🜩🝤🜎🙶𐛖🄋🟁🙴𐜢🙪𐚼🆍🝝𐜜🙷🅭🟕🆔🜾🜇
    static public final List<Integer> SYMBOLS_SMP = (0x10600..0x10736) + (0x10740..0x10755) + (0x10760..0x10767) +
                                                    (0x1F650..0x1F67F) + // Dingbats
                                                    (0x1F700..0x1F773) + // Alchemical
                                                    (0x1F780..0x1F7D8) + // Geometric
                                                    [0x1F100] + (0x1F10B..0x1F10F) + (0x1F12A..0x1F12F) + (0x1F14A..0x1F14F) + (0x1F16A..0x1F16F) + (0x1F18A..0x1F1AD) // Assorted
    // (1325 chars) : 🥿🩲👙🪞🧄🛡🗦🕙🫛🗐🪵🟨📎🔝🖘🎸🐔🥕🔬🏾🫵🛣
    static public final List<Integer> EMOJI = (0x1F300..0x1F64F) + (0x1F680..0x1F6D7) + (0x1F6E0..0x1F6EC) + (0x1F6F0..0x1F6FC) +
                                              (0x1F90C..0x1F9FF) + (0x1FA70..0x1FA7C) + (0x1FA80..0x1FA88) + (0x1FA90..0x1FABD) +
                                              (0x1FABF..0x1FAC5) + (0x1FACE..0x1FADB) + (0x1FAE0..0x1FAE8) + (0x1FAF0..0x1FAF8) +
                                              (0x1F7E0..0x1F7EB) // Geometric
    // (269 chars) : 𐀨𐀤𐃂𐀲𐂇𐃚🈘𐃐𐂬𐀅🈖𐂽𐀈𐃥𐂠𐂭𐂮𐃠🈖𐂻𐂋𐃛𐃥𐁌𐀚𐃄
    static public final List<Integer> IDEOGRAMS = (0x10000..0x1005D) - [0x1000C,0x10027,0x1003B,0x1003E,0x1004E,0x1004F] +
                                                  (0x10080..0x100FA) + (0x1F200..0x1F202) + (0x1F210..0x1F23B) + (0x1F240..0x1F248) +
                                                  [0x1F250,0x1F251]
    // (203 chars) : 🭖🬡🯈🬌🮿🮵🮎🭡🬗🭀🮟
    static public final List<Integer> BLOCK_SMP = (0x1FB00..0x1FBCA)
    // (10 chars) : 🯴🯱🯶🯰🯵🯰🯸🯹🯰🯲🯸🯹🯸🯱🯲
    static public final List<Integer> DIGITS = (0x1FBF0..0x1FBF9)


    /* ************ SMP COMBOS **************** */
    // Hieroglyphics (1896 chars) : 𓋢𓁯𓂱𓍞𓌴𔖥𔒺
    static public final List<Integer> HIEROGLYPHIC = EGYPTIAN + MEROITIC + ANATOLIAN
    // Lines (151 chars) : 𝌭𝍍𝌴䷏䷋𝌅𝌰𝌛𝌀𝌈䷫𝍖𝌁𝌂䷛䷖
    static public final List<Integer> LINES_SMP = HEXAGRAM + TAIXUANJING
    // (1317 chars) : 𒀊𒋫𒐭𒔫𒌻𒊊𒉲𒑙𒀂𒉹
    static public final List<Integer> WEDGE = UGARTIC + OLD_PERSIAN + CUNEIFORM
    // (68,498 chars) : ㋟讞🬵턇𐊈휹🯈⛷擶
    static public final List<Integer> VISIBLE_SMP = VISIBLE + GREEK_SMP + ROMAN + AEGEAN + PHAISTOS + LYCIAN + CARIAN + OLD_ITALIC +
        GOTHIC + OLD_PERMIC + UGARTIC + OLD_PERSIAN + DESERET + SHAVIAN + OSMANYA + OSAGE + ELBASAN + ALBANIAN + CYPRIOT + ARAMAIC +
        PALMYRENE + NABATAEAN + HATRAN + PHOENICIAN + LYDIAN + MEROITIC + KHAROSHTHI + OLD_ARABIAN + MANICHEAN + AVESTAN + PARTHIAN +
        PAHLAVI + OLD_TURKIC + OLD_HUNGARIAN + RUMI + SOGDIAN + BRAHMI + KAITHI + SORA_SOMPENG + CHAKMA + MAHAJANI + SHARADA + SHINHALA +
        KHOJKI + MULTANI + KHUDAWADI + GRANTHA + NEWA + TIRHUTA + SIDDHAM + MODI + TAKRI + AHOM + WARANG_CITI + ZANABAZAR + SOYOMBO +
        PAU_CIN_HAU + BHAIKSUKI + MARCHEN + CUNEIFORM + EGYPTIAN + ANATOLIAN + BAMUM_SMP + MRO + BASSA_VAH + PAHAWH_HMONG + MIAO +
        TANGUT + DUPLOYAN + MUSICAL + MAYAN + TAIXUANJING + COUNTING + MATH + GLAGOLITIC_SMP + NYIAKENG + WANCHO + MENDE_KIKAKUI +
        ADLAM + SIYAQ + ARABIC_SMP + MAHJONG + DOMINO + CARDS + COMMA_NUM + PAREN_UP + SQUARE_UP + CIRCLE_UP_NEG + SQUARE_UP_NEG +
        ARROWS + SYMBOLS_SMP + EMOJI + CHESS + CHESS_CH + IDEOGRAMS + BLOCK_SMP + DIGITS

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Wrapper around a list of characters with special properties
     */
    static class CharSet {
        final private List<Integer> chars

        CharSet(Collection<Integer> chars) {
            this.chars = chars.toList().unique()
            assert length > 1 : "Charset must be at least 1 character"
        }

        int getLength() {
            return chars.size()
        }

        int getPosition(int chr) {
            return chars.toList().indexOf(chr)
        }

        int getAt(int index) {
            return chars[index]
        }

        List<Integer> getChars() {
            return chars.collect() // Create a clone of the List so we don't modify it
        }
    }

    private CharSet input
    private CharSet output
    private long seed
    /**
     * Constructor using a CharSet as input and a list as output
     * @param input
     * @param output
     * @param seed
     */
    LpCode(CharSet input, Collection<Integer> output, long seed = 0) {
        this.input = input
        this.output = new CharSet(output)
        this.seed = seed
    }
    /**
     * Constructor using a list of characters and a CharSet
     * @param input
     * @param output
     * @param seed
     */
    LpCode(Collection<Integer> input, CharSet output, long seed = 0) {
        this.input = new CharSet(input)
        this.output = output
        this.seed = seed
    }
    /**
     * Constructor using input and output as CharSet
     * @param input
     * @param output
     * @param seed
     */
    LpCode(CharSet input, CharSet output, long seed = 0) {
        this.input = input
        this.output = output
        this.seed = seed
    }
    /**
     * Constructor using two lists of characters
     * @param input
     * @param output
     * @param seed
     */
    LpCode(Collection<Integer> input = BASIC, Collection<Integer> output = ANUM, long seed = 0) {
        this.input = new CharSet(input)
        this.output = new CharSet(output)
        this.seed = seed
    }
    /**
     * Get the BigInteger from an array of characters for a specific CharSet
     * @param chars
     * @param charSet
     * @param seed
     * @return
     */
    static BigInteger toNumber(char[] chars, CharSet charSet, long seed = 0) {
        LpCode lpCode = new LpCode(charSet, charSet, seed)
        if(seed) { chars = lpCode.randomize(chars) }
        return toNum(chars, charSet)
    }
    /**
     * Get the BigInteger from an array of characters for a specific list of characters
     * @param chars
     * @param charSet
     * @param seed
     * @return
     */
    static BigInteger toNumber(char[] chars, Collection<Integer> charSet, long seed = 0) {
        return toNumber(chars, new CharSet(charSet), seed)
    }
    /**
     * Convert number to char array
     * @param number
     * @param charSet
     * @param seed
     * @return
     */
    static char[] toCharArray(BigInteger number, CharSet charSet, long seed = 0) {
        LpCode lpCode = new LpCode(charSet, charSet, seed)
        char[] chars = toStr(number, charSet)
        if(seed) { chars = lpCode.randomize(chars, true) }
        return chars
    }
    /**
     * Convert number to char array using a list of characters
     * @param number
     * @param charSet
     * @param seed
     * @return
     */
    static char[] toCharArray(BigInteger number, Collection<Integer> charSet, long seed = 0) {
        return toCharArray(number, new CharSet(charSet), seed)
    }
    /**
     * Convert number to string
     * @param number
     * @param charSet
     * @param seed
     * @return
     */
    static String toString(BigInteger number, CharSet charSet, long seed = 0) {
        LpCode lpCode = new LpCode(charSet, charSet, seed)
        char[] chars = toStr(number, charSet)
        return (seed ? lpCode.randomize(chars, true) : chars).toString()
    }
    /**
     * Convert number to string
     * @param number
     * @param charSet
     * @param seed
     * @return
     */
    static String toString(BigInteger number, Collection<Integer> charSet, long seed = 0) {
        return toString(number, new CharSet(charSet), seed)
    }

    /**
     * The following methods are static wrappers for `translate`
     * @param chars : char array or String
     * @param from  : CharSet or List<Integer>
     * @param to    : CharSet or List<Integer>
     * @return char array or String
     */
    static char[] translate(char[] chars, CharSet from, CharSet to) {
        return new LpCode(from, to).translate(chars)
    }
    static char[] translate(char[] chars, Collection<Integer> from, CharSet to) {
        return new LpCode(from, to).translate(chars)
    }
    static char[] translate(char[] chars, CharSet from, Collection<Integer> to) {
        return new LpCode(from, to).translate(chars)
    }
    static char[] translate(char[] chars, Collection<Integer> from, Collection<Integer> to) {
        return new LpCode(from, to).translate(chars)
    }
    static String translate(String str, CharSet from, CharSet to) {
        return new LpCode(from, to).translate(str.toCharArray()).toString()
    }
    static String translate(String str, Collection<Integer> from, CharSet to) {
        return new LpCode(from, to).translate(str.toCharArray()).toString()
    }
    static String translate(String str, CharSet from, Collection<Integer> to) {
        return new LpCode(from, to).translate(str.toCharArray()).toString()
    }
    static String translate(String str, Collection<Integer> from, Collection<Integer> to) {
        return new LpCode(from, to).translate(str.toCharArray()).toString()
    }
    /**
     * Translates from one charset to another
     * `Translate` won't encode, it is just a 1 to 1 conversion.
     * For example:  A -> 🅰 -> 🅐 -> 🄰 -> a -> 1
     *
     * NOTE: In the case the target charset uses a out-of-bounds character,
     * it will not translate
     *
     * @param str
     * @return
     */
    char[] translate(char[] str) {
        String res = ""
        List<Integer> cps = getCodePoints(str.toString())
        cps.each {
            int idx = input.getPosition(it)
            if(idx >= 0) {
                if(idx <= output.length) {
                    res += Character.toString(output[idx])
                } else {
                    res += Character.toString(it)
                }
            } else {
                res += Character.toString(it)
            }
        }
        return res.toCharArray()
    }
    /**
     * Encode an array of characters
     * @param str
     * @return
     */
    char[] encode(char[] str) {
        if (seed) {
            str = randomize(str)
        }
        BigInteger num = toNum(str, input)
        return toStr(num, output)
    }
    /**
     * Encode an input stream and write it into an output stream
     * @param inputStream : string to encode
     * @param outputStream
     */
    void encode(InputStream inputStream, OutputStream outputStream, Collection<Integer> glue, int chunkCharSize = chunkSize) {
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        BufferedReader br = new BufferedReader(reader)
        OutputStreamWriter writer = new OutputStreamWriter(outputStream)
        CharBuffer buff = CharBuffer.allocate(chunkSize)
        boolean init = false // flag to know if we already started
        while(br.read(buff) > 0) {
            buff.flip()
            String encoded = encode(buff.chars).toString()
            String sep = init ? glue.collect { Character.toString(it) }.join("") : ""
            writer.write(sep + encoded)
            writer.flush()
            buff.clear()
            init = true
        }
    }
    void encode(InputStream inputStream, OutputStream outputStream, int glue = glueChar, int chunkCharSize = chunkSize) {
        encode(inputStream, outputStream, [glue], chunkCharSize)
    }
    void encode(InputStream inputStream, OutputStream outputStream, String glue, int chunkCharSize = chunkSize) {
        encode(inputStream, outputStream, getCodePoints(glue), chunkCharSize)
    }
    /**
     * Encode splitting the input string in chunks and glue them with some separator
     * This method helps to reduce the memory consumption by trading cpu usage
     * @param str
     * @param glue
     * @param chunkSize
     * @return
     */
    String encodeByChunks(char[] str, Collection<Integer> glue, int chunkCharSize = chunkSize) {
        List<String> buff = []
        glue.each {
            output.chars.removeElement(it) // Remove it from charset if it is included
        }
        (0..str.length - 1).step(chunkCharSize).each {
            int next = it + chunkCharSize
            if(next > str.length - 1) { next = str.length }
            buff << encode(str.toList().subList(it, next).toArray() as char[]).toString()
        }
        return buff.join(codePointsToString(glue))
    }
    String encodeByChunks(char[] str, int glue = glueChar, int chunkCharSize = chunkSize) {
        return encodeByChunks(str, [glue], chunkCharSize)
    }
    String encodeByChunks(char[] str, String glue, int chunkCharSize = chunkSize) {
        return encodeByChunks(str, getCodePoints(glue), chunkCharSize)
    }
    /**
     * Decode an array of characters
     * @param str
     * @return
     */
    char[] decode(char[] str) {
        BigInteger num = toNum(str, output)
        str = toStr(num, input)
        if (seed) {
            str = randomize(str, true)
        }
        return str
    }
    /**
     * Decode an input stream and write it into an output stream
     * @param inputStream : encoded string
     * @param outputStream
     */
    void decode(InputStream inputStream, OutputStream outputStream, Collection<Integer> glue) {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream)
        Scanner scanner = new Scanner(inputStream)
        String div = codePointsToString(glue)
        scanner.useDelimiter(Pattern.quote(div))
        while(scanner.hasNext()) {
            String part = scanner.next().replaceAll(div, "")
            String decoded = decode(part.toCharArray()).toString()
            writer.write(decoded)
            writer.flush()
        }
    }
    void decode(InputStream inputStream, OutputStream outputStream, int glue = glueChar) {
        decode(inputStream, outputStream, [glue])
    }
    void decode(InputStream inputStream, OutputStream outputStream, String glue) {
        decode(inputStream, outputStream, getCodePoints(glue))
    }
    /**
     * Decode a string which was previously encoded by "encodeByChunks"
     * @param str
     * @param glue
     * @return
     */
    String decodeByChunks(char[] str, Collection<Integer> glue) {
        List<String> buff = []
        glue.each {
            output.chars.removeElement(it) // Remove it from charset if it is included
        }
        String div = codePointsToString(glue)
        str.toString().tokenize(div).each {
            buff << decode(it.toCharArray()).toString()
        }
        return buff.join("")
    }
    String decodeByChunks(char[] str, int glue = glueChar) {
        return decodeByChunks(str, [glue])
    }
    String decodeByChunks(char[] str, String glue) {
        return decodeByChunks(str, getCodePoints(glue))
    }
    /**
     * Converts an array of characters to a number
     * @param chars
     * @param charset
     * @return
     */
    static protected BigInteger toNum(char[] chars, CharSet charset) {
        String str = chars.toString()
        List<Integer> codePoints = getCodePoints(str)
        int len = codePoints.size()
        BigInteger n = 0
        codePoints.eachWithIndex {
            int cp, int s ->
                int r = charset.getPosition(cp)
                if(r == -1) { // Not found in charset
                    Log.w("Character: [%s](0x%d) not found in charset: [%s ~ %s]", Character.toString(cp), cp, Character.toString(charset.chars.min()), Character.toString(charset.chars.max()))
                    r = Random.range(0, charset.length - 1) //Replace by a random character
                }
                //noinspection GrReassignedInClosureLocalVar
                n = (s == len - 1) ? n + (r + 1) : (n + (r + 1)) * (charset.length)
        }
        return n
    }

    /**
     * Get all codePoints in String
     * @param str
     * @return
     */
    static List<Integer> getCodePoints(String str) {
        List<Integer> list = []
        for (int offset = 0, s = 0; offset < str.length(); s++) {
            int cp = str.codePointAt(offset)
            list << cp
            offset += Character.charCount(cp)
        }
        return list
    }
    /**
     * Get the codePoint of a single character
     * @param str
     * @return
     */
    static Integer getCodePoint(String str) {
        return getCodePoints(str).first()
    }
    /**
     * Converts a number into an array of characters
     * @param num
     * @param charset
     * @return
     */
    static protected char[] toStr(BigInteger num, CharSet charset) {
        LinkedList<Character> anum = new LinkedList<>()
        while (num-- >= 1) {
            int c = charset.chars[(num % (charset.length)) as int]
            Character.toChars(c).toList().reverse().each {
                anum.addFirst(it)
            }
            num = num.divide((charset.length) as BigInteger)
        }
        return anum.toArray() as char[]
    }
    /**
     * Randomize a char array (keeping each character)
     * @param str
     * @param reverse
     * @return
     */
    char[] randomize(char[] chars, boolean reverse = false) {
        String str = chars.toString()
        Random random = new Random(seed)
        List<Integer> cps = getCodePoints(str)
        List<Integer> pos = (0..(cps.size() - 1)).toList()
        pos.shuffle(random)
        List<String> out = []
        pos.withIndex().each {
            int p, int i ->
                if(reverse) {
                    out[p] = Character.toString(cps[i])
                } else {
                    out[i] = Character.toString(cps[p])
                }
        }
        return out.join("").toCharArray()
    }
    /**
     * Get the list of all defined charsets
     * @return
     */
    static Map<String, CharSet> getCharsets() {
        Map charsets = [:] as Map<String, CharSet>
        try {
            LpCode.declaredFields.each {
                if (Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)) {
                    List<Integer> list = it.get(null) as List<Integer>
                    if (list.size() > 2 || it.name == "BIN") {
                        charsets[it.name] = new CharSet(list)
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return charsets
    }
    /**
     * Convert codePoints to String
     * @param codes
     * @return
     */
    static String codePointsToString(Collection<Integer> codes) {
        return codes.collect { Character.toString(it) }.join("")
    }
}
