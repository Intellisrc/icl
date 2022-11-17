//file:noinspection unused
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
 *
 * Recommended fonts:
 *  - Dejavu
 *
 * https://www.ssec.wisc.edu/~tomw/java/unicode.html
 * https://en.wikipedia.org/wiki/Unicode_block
 */

@CompileStatic
class LpCode {
    /* ************ PARTS ***************** */
    //Binary-like (2 chars: 0,1) : 10111010111011111010100111000011111101000101000100
    static public final List<Integer> BIT = 0x30..0x31
    //Space character (used only to complement any other)
    static public final List<Integer> SPACE = [0x20]
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
    // Tagalog - Philippines (22 chars)  : Not available in some fonts
    static public final List<Integer> TAGALOG = (0x1700..0x1714) + [0x1735, 0x1736] - [0x170D]
    // (23 chars) : ᜦᜬᜪᜨᜦᜤᜠᜭᜦᜠᜧᜮᜬᜦ᜶ᜣᜲᜥᜳᜯᜧ᜴
    static public final List<Integer> HANUNOO = (0x1720..0x1736)
    // (20 chars) : ᝑᝄᝍᝌᝍᝅᝄᝈᝒᝏᝎᝊᝎᝋᝓᝎᝅᝆᝇᝈ
    static public final List<Integer> BUHID = (0x1740..0x1753)
    // (18 chars) : Not available in some fonts
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
    // (45 chars) : Not available in some fonts
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
    static public final List<Integer> CIRCLE_NUMS = (0x2460..0x2473) + [0x24EA] + (0x2780..0x2789) + (0x24F5..0x24FE) +
        (0x3251..0x325F) + (0x32B1..0x32BF)
    // Negative circled numbers (31 chars) : ❺➓⓱❻⓿➋❷❾⓫❻⓮➒➍⓱⓴
    static public final List<Integer> CIRCLE_NEG_NUMS = (0x24EB..0x24F4) + [0x24FF] + (0x2776..0x277F) + (0x278A..0x2793)
    // Numbers with parenthesis (20 chars) : ⑴⑸⑾⑴⒁⒂⑶⒆⑺⑺⒄⑵⒃⒀⑽⑽
    static public final List<Integer> PAREN_NUMS = (0x2474..0x2487)
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
    static public final List<Integer> INVISIBLE = (0xD800..0xDFFF)
    //Any UTF8 char (65,536 chars) use with caution as many characters can not be rendered
    static public final List<Integer> UTF8 = 0x0..0xFFFF

    /* ************ COMBOS **************** */
    //0-9a-f //similar to md5, sh,a etc (but with variable length) (16 chars)
    static public final List<Integer> HASH = NUMBERS + aTof
    //0-9A-F //same as 'HASH' but uppercase (16 chars)
    static public final List<Integer> HASH_UP = NUMBERS + AtoF
    //a-zA-Z (52 chars)
    static public final List<Integer> ALPHA = LOWERCASE + UPPERCASE
    //0-9a-zA-Z (62 chars)
    static public final List<Integer> ANUM = NUMBERS + LOWERCASE + UPPERCASE
    //0-9a-zA-Z=+/ (as result of Base64, etc) (64 chars)
    static public final List<Integer> BASE64 = ANUM + EQUALPLUS
    //Ascii Extended (support for accents and other common symbols) (815 chars)
    static public final List<Integer> ASCII = BASIC + EXTENDED
    // Alphanumeric in a circle (154 chars)
    static public final List<Integer> CIRCLES = CIRCLE_LOW + CIRCLE_UP + CIRCLE_NUMS + CIRCLE_NEG_NUMS
    // Japanese, Korean, Chinese (40861 chars)
    static public final List<Integer> CJK = RADICALS + BOPOMOFO + HANGUL + IDEOGRAPHIC + HANZU + KOREAN + HIRAGANA + KATAKANA
    // Languages from India (705 chars)
    static public final List<Integer> INDIAN = DEVANAGARI + BENGALI + GUTMUKHI + GUJARATI + ORIYA + TAMIL + TELEGU + KANNADA
    // South-East Asia (1673 chars)
    static public final List<Integer> SE_ASIA = MALAYAM + SINHALA + THAI + LAO + MYANMAR + TAI_VIET + INDIAN + GEORGIAN + BALINESE
    // Visible characters : safe option instead of UTF8 (48392 chars)
    static public final List<Integer> VISIBLE = ASCII + GREEK + CYRILLIC + ARMENIAN + HEBREW + ARABIC + SYRIAC + THAANA +
        NKO + SAMARITAN + MANDAIC + INDIAN + SE_ASIA + TIBETAN + CJK + BRAILLE + SYMBOLS + HEXAGRAM - SPACE

    /*
        SMP: Supplementary Multilingual Plane
        NOTE: Be aware that the following blocks are formed with more bytes and it might require special handling
     */
    static public final List<Integer> GREEK_SMP = (0x10140..0x1018E) + (0x101A0)
    // ( chars)
    static public final List<Integer> ROMAN = (0x10190..0x1019C)
    // ( chars)
    static public final List<Integer> AEGEAN = (0x10100..0x10102) + (0x10107..0x10133) + (0x10137..0x1013F)
    // ( chars)
    static public final List<Integer> PHAISTOS = (0x101D0..0x101FD)
    // ( chars)
    static public final List<Integer> LYCIAN = (0x10280..0x1029C)
    // ( chars)
    static public final List<Integer> CARIAN = (0x102A0..0x102D0)
    // ( chars)
    static public final List<Integer> OLD_ITALIC = (0x10300..0x10323)
    // ( chars)
    static public final List<Integer> GOTHIC = (0x1032D..0x1034A)
    // ( chars)
    static public final List<Integer> OLD_PERMIC = (0x10350..0x1037A)
    // ( chars)
    static public final List<Integer> UGARTIC = (0x10380..0x1039D) + [0x1039F]
    // ( chars)
    static public final List<Integer> OLD_PERSIAN = (0x1039F..0x103D5) - (0x103C4..0x103C7)
    // ( chars)
    static public final List<Integer> DESERET = (0x10400..0x1044F0)
    // ( chars)
    static public final List<Integer> SHAVIAN = (0x10450..0x1047F)
    // ( chars)
    static public final List<Integer> OSMANYA = (0x10480..0x104A9) - [0x1049E,0x1049F]
    // ( chars)
    static public final List<Integer> OSAGE = (0x104B0..0x104D3) + (0x104D8..0x14FB)
    // ( chars)
    static public final List<Integer> ELBASAN = (0x10500..0x10527)
    // Caucasian Albanian ( chars)
    static public final List<Integer> ALBANIAN = (0x10530..0x10563) + [0x1056F]
    // ( chars)
    static public final List<Integer> CYPRIOT = (0x10800..0x10838) - [0x10806,0x10807,0x10809,0x10836] + [0x1083C, 0x1083F]
    // Imperial Aramaic ( chars)
    static public final List<Integer> ARAMAIC = (0x10840..0x1085F) - [0x10856]
    // ( chars)
    static public final List<Integer> PALMYRENE = (0x10860..0x1087F)
    // ( chars)
    static public final List<Integer> NABATAEAN = (0x10880..0x1089E) + (0x108A7..0x108AF)
    // ( chars)
    static public final List<Integer> HATRAN = (0x108E0..0x108F5) - [0x108F3] + (0x108FB..0x108FF)
    // ( chars)
    static public final List<Integer> PHOENICIAN = (0x10900..0x1091B) + [0x1091F]
    // ( chars)
    static public final List<Integer> LYDIAN = (0x10920..0x10939) + [0x1093F]
    // ( chars) Hieroglyphic
    static public final List<Integer> MEROITIC = (0x10908..0x109B7) + (0x109BC..0x109FF) - [0x109D0,0x109D1]
    // ( chars)
    static public final List<Integer> KHAROSHTHI = (0x10A00..0x10A03) + [0x10A05,0x10A06] + (0x10A0C..0x10A13) +
                                                   (0x10A15..0x10A17) + (0x10A19..0x10A35) + (0x10A38..0x10A3A) +
                                                   (0x10A3F..0x10A48) + (0x10A50..0x10A58)
    // ( chars)
    static public final List<Integer> OLD_ARABIAN = (0x10A60..0x10A9F)
    // ( chars)
    static public final List<Integer> MANICHEAN = (0x10AC0..0x10AE6) + (0x10AEB..0x10AF6)
    // ( chars)
    static public final List<Integer> AVESTAN = (0x10B00..0x10B35) + (0x10B39..0x10B3F)
    // ( chars)
    static public final List<Integer> PARTHIAN = (0x10B40..0x10B55) + (0x10B58..0x10B5F)
    // ( chars)
    static public final List<Integer> PAHLAVI = (0x10B60..0x10B72) + (0x10B78..0x10B91) + (0x10B99..0x10B9C) + (0x10BA9..0x10BAF)
    // ( chars)
    static public final List<Integer> OLD_TURKIC = (0x10C00..0x10C48)
    // ( chars)
    static public final List<Integer> OLD_HUNGARIAN = (0x10C80..0x10CB2) + (0x10CC0..0x10CF2) + (0x10CFA..0x10D27) + (0x10D30..0x10D39)
    // ( chars)
    static public final List<Integer> RUMI = (0x10E60..0x10E7E)
    // ( chars)
    static public final List<Integer> SOGDIAN = (0x10F00..0x10F27) + (0x10F30..0x10F59)
    // ( chars)
    static public final List<Integer> BRAHMI = (0x11000..0x1104D) + (0x11052..0x1106F) + [0x1107F]
    // ( chars)
    static public final List<Integer> KAITHI = (0x11080..0x110C1)
    // ( chars)
    static public final List<Integer> SORA_SOMPENG = [0x110CD] + (0x110D0..0x110E8) + (0x110F0..0x110F9)
    // ( chars)
    static public final List<Integer> CHAKMA = (0x11100..0x11134) + (0x11136..0x11146)
    // ( chars)
    static public final List<Integer> MAHAJANI = (0x11150..0x11176)
    // ( chars)
    static public final List<Integer> SHARADA = (0x11180..0x111DF)
    // ( chars)
    static public final List<Integer> SHINHALA = (0x111E1..0x111F4)
    // ( chars)
    static public final List<Integer> KHOJKI = (0x11200..0x1123E) - [0x11212]
    // ( chars)
    static public final List<Integer> MULTANI = (0x11280..0x112A9) - [0x11287,0x11289,0x1128E,0x1129E]
    // ( chars)
    static public final List<Integer> KHUDAWADI = (0x112B0..0x112EA) + (0x112F0..0x112F9)
    // ( chars)
    static public final List<Integer> GRANTHA = (0x11300..0x11303) + (0x11305..0x1130C) + (0x1130F..0x11310) + (0x11313..0x11328) +
                                                (0x1132A..0x11330) + [0x11332,0x11333] + (0x11335..0x11339) + (0x1133C..0x11344) +
                                                [0x11347,0x11348,0x1134B,0x1134C,0x1134D,0x11350,0x11357] + (0x1135D..0x11363) +
                                                (0x11366..0x1136C) + (0x11370..0x11374)
    // ( chars)
    static public final List<Integer> NEWA = (0x11400..0x11461) - [0x1145C]
    // ( chars)
    static public final List<Integer> TIRHUTA = (0x11480..0x114C7) + (0x114D0..0x114D9)
    // ( chars)
    static public final List<Integer> SIDDHAM = (0x11580..0x115B5) + (0x115B8..0x115DD)
    // ( chars)
    static public final List<Integer> MODI = (0x11600..0x11644) + (0x11650..0x11659)
    // ( chars)
    static public final List<Integer> TAKRI = (0x11680..0x116B8) + (0x116C0..0x116C9)
    // ( chars)
    static public final List<Integer> AHOM = (0x11700..0x1171A) + (0x1171D..0x1172B) + (0x11730..0x1173F)
    // ( chars)
    static public final List<Integer> WARANG_CITI = (0x118A0..0x118F2) + [0x118FF]
    // ( chars)
    static public final List<Integer> ZANABAZAR = (0x11A00..0x11A47)
    // ( chars)
    static public final List<Integer> SOYOMBO = (0x11A50..0x11AA2)
    // ( chars)
    static public final List<Integer> PAU_CIN_HAU = (0x11AC0..0x11AF8)
    // ( chars)
    static public final List<Integer> BHAIKSUKI = (0x11C00..0x11C45) - [0x11C09,0x11C37] + (0x11C50..0x11C6C)
    // ( chars)
    static public final List<Integer> MARCHEN = (0x11C70..0x11C8F) + (0x11C92..0x11CB6) - [0x11CA8]
    // ( chars)
    static public final List<Integer> CUNEIFORM = (0x12000..0x12399) + (0x12400..0x12474) + (0x12480..0x12543)
    // ( chars) Hieroglyphic
    static public final List<Integer> EGYPTIAN = (0x13000..0x1342E)
    // ( chars) Hieroglyphic
    static public final List<Integer> ANATOLIAN = (0x14400..0x14646)
    // ( chars)
    static public final List<Integer> BAMUM_SMP = (0x16800..0x16A39)
    // ( chars)
    static public final List<Integer> MRO = (0x16A40..0x16A69) - [0x16A5F] + [0x16A6E,0x16A6F]
    // ( chars)
    static public final List<Integer> BASSA_VAH = (0x16AD0..0x16AED) + (0x16AF0..0x16AF5)
    // ( chars)
    static public final List<Integer> PAHAWH_HMONG = (0x16B00..0x16B45) + (0x16B50..0x16B77) - [0x16B5A,0x16B62] + (0x16B7D..0x16B8F)
    // ( chars)
    static public final List<Integer> MIAO = (0x16F00..0x16F4A) + (0x16F4F..0x16F87) + (0x16F8F..0x16F9F)
    // ( chars)
    static public final List<Integer> TANGUT = [0x16FE0] + (0x17000..0x187F7) + (0x18800..0x18AF2)

    // Others missing here

    static public final List<Integer> SYMBOLS_SMP = (0x10600..0x10736) + (0x10740..0x10755) + (0x10760..0x10767)

    static public final List<Integer> EMOJI = (0x1F90C..0x1F9FF) + (0x1FA70..0x1FA7C) + (0x1FA80..0x1FA88) + (0x1FA90..0x1FABD) +
                                              (0x1FABF..0x1FAC5) + (0x1FACE..0x1FADB) + (0x1FAE0..0x1FAE8) + (0x1FAF0..0x1FAF8)

    static public final List<Integer> CHESS = (0x1FA00..0x1FA53)
    static public final List<Integer> CHESS_CH = (0x1FA60..0x1FA6D)
    static public final List<Integer> IDIOGRAMS = (0x10000..0x1005D) - [0x1000C,0x10027,0x1003B,0x1003E,0x1004E,0x1004F] +
                                                  (0x10080..0x100FA)
    // ( chars)
    static public final List<Integer> BLOCK_SMP = (0x1FB00..0x1FBCA)
    // (10 chars)
    static public final List<Integer> DIGITS = (0x1FBF0..0x1FBF9)


    /* ************ SMP COMBOS **************** */
    // Hieroglyphics ( chars)
    static public final List<Integer> HIEROGLYPHIC = EGYPTIAN + MEROITIC + ANATOLIAN

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

    LpCode(CharSet input, Collection<Integer> output, long seed = 0) {
        this.input = input
        this.output = new CharSet(output)
        this.seed = seed
    }

    LpCode(Collection<Integer> input, CharSet output, long seed = 0) {
        this.input = new CharSet(input)
        this.output = output
        this.seed = seed
    }

    LpCode(CharSet input, CharSet output, long seed = 0) {
        this.input = input
        this.output = output
        this.seed = seed
    }

    LpCode(Collection<Integer> input = BASIC, Collection<Integer> output = ANUM, long seed = 0) {
        this.input = new CharSet(input)
        this.output = new CharSet(output)
        this.seed = seed
    }

    char[] encode(char[] str) {
        if (seed) {
            str = randomize(str)
        }
        BigInteger num = toNum(str, input)
        return toStr(num, output)
    }

    char[] decode(char[] str) {
        BigInteger num = toNum(str, output)
        str = toStr(num, input)
        if (seed) {
            str = randomize(str, true)
        }
        return str
    }

    static protected BigInteger toNum(char[] chars, CharSet charset) {
        String str = chars.toString()
        int len = str.codePointCount(0, str.length())
        BigInteger n = 0
        for (int offset = 0, s = 0; offset < str.length(); s++) {
            int cp = str.codePointAt(offset)
            int r = charset.getPosition(cp)
            n = (s == len - 1) ? n + (r + 1) : (n + (r + 1)) * (charset.length)
            offset += Character.charCount(cp)
        }
        return n
    }

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
                if (reverse) {
                    shuffled[p] = str[i]
                } else {
                    shuffled[i] = str[p]
                }
        }
        return shuffled
    }

    static Map<String, CharSet> getCharsets() {
        Map charsets = [:] as Map<String, CharSet>
        try {
            LpCode.declaredFields.each {
                if (Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)) {
                    List<Integer> list = it.get(null) as List<Integer>
                    if (list.size() > 1) {
                        charsets[it.name] = new CharSet(list)
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return charsets
    }
}
