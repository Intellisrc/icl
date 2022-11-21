# CRYPT Module (ICL.crypt)

Offers methods to encode, decode, hash and encrypt
information. It is built using the BouncyCastle
library and simplifying its usage without reducing
its safety.

`Crypt` based classes were built using `byte` and `char` arrays
instead of `String` to prevent those values to reside
in memory.

[JavaDoc](https://intellisrc.gitlab.io/common/#crypt)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/crypt)

### Examples

This module provides a simple interface for all hashing
algorithms supported by BouncyCastle.

```groovy
// List of supported algorithms:
List<String> listAlgo = Hash.getAlgorithms()
listAlgo.each {
    println
}
```

```groovy
// Hash bytes:
Hash hash = new Hash(key: "SomeKey".bytes)
byte[] bytes = hash.asBytes("TIGER") //Using TIGER algorithm
Log.i("TIGER HASH: %s", Bytes.toHex(bytes))
```

You can also sign messages:

```groovy
String msg = "Message"
def hash = new Hash(key: "secret".bytes)
byte[] hashed = hash.sign(msg, Hash.BasicAlgo.SHA256)
String signedHex = hash.signHex(msg, Hash.BasicAlgo.SHA256)
```

To hash passwords, this module provides implementations
for BCrypt (CPU intensive) and SCrypt (Memory Intensive)

```groovy
// Generate random strings:
byte[] rand = crypt.randomChars(50, Crypt.Complexity.HIGH)

// Get BCrypt password hash:
def hasher = new PasswordHash(password: rand)
String hash = hasher.BCrypt()
hasher.clear() //Optional to clear used memory

byte[] pwd = "Some password provided by user".bytes
def verifier = new PasswordHash(password: pwd)
// Verify that provided password matches stored hash:
verifier.verify(hash)
```

For encryption/decryption, this module provides implementations for
AES and PGP. 

```groovy
byte[] key = "584F5A53487853313550577939666B37665973623145516D37327655347A4F43".bytes
byte[] secret = "Hello Earth!".bytes

//If no key is provided, it will generate one. You can get it with `aes.key`
AES aes = new AES(key: key)
byte[] encodedBytes = aes.encrypt(secret)
String encoded = Bytes.toHex(encodedBytes)
byte[] decoded = aes.decrypt(Bytes.fromHex(encoded))
println Bytes.toString(decoded)
// will print: "Hello Earth!"
```

```groovy
PGP pgp = new PGP()
byte[] original = "This is a super secret message!".bytes
byte[] encrypted = pgp.encrypt(original)
println "encrypted data = '" + Bytes.toHex(encrypted) + "'"
byte[] decrypted = pgp.decrypt(encrypted)
println "decrypted data = '" + Bytes.toString(decrypted) + "'"
assert decrypted == original
// When no key is specified, it will generate one for you, this is your key:
println "Used Key: " + Bytes.toString(pgp.key)
```

## LpCode

This class uses blocks in the UTF-8 range to convert and randomize (when using seed) a string.
It can be used for:
* Obfuscate strings in a non-conventional way
* Reduce the amount of data to store
* Just to have fun!

### Encode/Decode Example

```groovy
char[] toEncode = "HelloWorld".toCharArray()
long seed = new Random().nextLong()
LpCode lpCode = new LpCode(ALPHA, HANZU, seed)

String encoded = lpCode.encode(toEncode)
println encoded // Will return something like: 
// 竢茫哊鰱

String original = lpCode.decode(encoded)
println original
// HelloWorld
```

> NOTE: Be sure that your input charset (ALPHA in the above example), includes all the characters that you expect in 
> the string you are going to encode.

You have more than 200 charset blocks that you can choose from: more than 100 within the BMP (Basic Multilingual Plane), 
and more than 100 within the SMP (Supplementary Multilingual Plane). Encoding "HelloWorld" in some of those blocks, will
result in something like:

> NOTE: There is no font that will display all characters in this list.  Some fonts of the most complete are: Noto - Google (77,000 characters)
        and Unifont (65,000 characters)

| BLOCK           | chars | encoded                                                   | length |
|-----------------|-------|-----------------------------------------------------------|--------|
 | BIT             | 2     | 0010111100111011110111001000101011111110110011101100001   | 55     | 
 | EQUALPLUS       | 2     | ==+=++++==+++=++++=+++==+===+=+=+++++++=++==+++=++====+   | 55     | 
 | aTof            | 6     | aecfaabfafeaecbdaacbeb                                    | 22     | 
 | AtoF            | 6     | AECFAABFAFEAECBDAACBEB                                    | 22     | 
 | NUMBERS         | 10    | 31565257065952665                                         | 17     | 
 | LOWERCASE       | 26    | kphbfkaksitl                                              | 12     | 
 | UPPERCASE       | 26    | KPHBFKAKSITL                                              | 12     | 
 | BASIC           | 96    | $vwQc6^=?                                                 | 9      | 
 | EXTENDED        | 719   | žå̫̇ƶ˛                                                    | 6      | 
 | LATIN           | 791   | ᶍꝼꝶꞭḲᴸ                                                    | 6      | 
 | GREEK           | 344   | ΐᾢρΎἽύΈ                                                   | 7      | 
 | CYRILLIC        | 265   | ѺлⷽъꙊЮꙚ                                                   | 7      | 
 | ARMENIAN        | 93    | ԷլՈՓշ՜ՔՃՖ                                                 | 9      | 
 | HEBREW          | 133   | בוֹ׆וּרּ֜֙ײ                                                  | 8      | 
 | ARABIC          | 1109  | ؘﯵﵭﱍࢦﮙ                                                    | 6      | 
 | SYRIAC          | 108   | ܁ܢ܀ܦݥܕ݁ݝݫ                                                 | 9      | 
 | THAANA          | 50    | ޔީޘޤޛއޏޘޘޙ                                                | 10     | 
 | NKO             | 62    | ߂߈ߛߘߗߙ߆ߚߌߥ                                                | 10     | 
 | SAMARITAN       | 62    | ࠂࠈࠛ࠘ࠗ࠙ࠆࠚࠌࠥ                                                | 10     | 
 | MANDAIC         | 29    | ࡂࡍࡋࡔࡓࡁࡗࡊࡄࡔ࡙ࡐ                                              | 12     | 
 | DEVANAGARI      | 160   | ऎ꣮॰क़ꣾोॗय़                                                  | 8      | 
 | BENGALI         | 92    | ঌথঅ৭বও়৳ৃ                                                 | 9      | 
 | GUTMUKHI        | 92    | ঌথঅ৭বও়৳ৃ                                                 | 9      | 
 | GUJARATI        | 91    | ઊઇ૩ૐછપ૩ૹૉ                                                 | 9      | 
 | ORIYA           | 91    | ଊଇ୨ଢ଼ଝବ୨ୱ୕                                                 | 9      | 
 | TAMIL           | 76    | ஺மஂல௶௫௭உர                                                 | 9      | 
 | TELEGU          | 99    | ఃూ౬ఀవౢఙౖఘ                                                 | 9      | 
 | KANNADA         | 96    | ಄೨೩ವೋಘೄಟಡ                                                 | 9      | 
 | MALAYAM         | 118   | ഀഏൻൖമഭമ൳൩                                                 | 9      | 
 | SINHALA         | 94    | ඇ෴ඊනදධ෉෨ඕ                                                 | 9      | 
 | THAI            | 87    | ฌ๛ดร่ฎฏผฑ                                                 | 9      | 
 | TAI_VIET        | 72    | ꪺꪅꪫꪄꪢꪻꪤ꪿ꪟ                                                 | 9      | 
 | LAO             | 79    | ຟຍຘໃແໞຼນພ                                                 | 9      | 
 | TIBETAN         | 213   | ༁༝࿘ཽ༁ྃྌ࿍                                                  | 8      | 
 | MYANMAR         | 223   | ကၻငလ၏ꩱါ့                                                  | 8      | 
 | GEORGIAN        | 174   | ႧⴊᲿსᲥᲑⴉᲟ                                                  | 8      | 
 | ETHIOPIC        | 496   | ሁⶸፀኘቈሧዱ                                                   | 7      | 
 | CHEROKEE        | 172   | ᎨꭷᏅꮪᏆꮯꮾꮏ                                                  | 8      | 
 | UCAS            | 710   | ᓫᕺᣜᑩᐄᙱ                                                    | 6      | 
 | OGHAM           | 29    | ᚂᚍᚋᚔᚓᚁᚗᚊᚄᚔᚙᚐ                                              | 12     | 
 | RUNIC           | 89    | ᚩᛩᛪᚮᛞᛅᛟᛁᛂ                                                 | 9      | 
 | TAGALOG         | 22    | ᜂᜅ᜶ᜑᜊᜀ᜵ᜐ᜵ᜀᜁ᜶ᜋ                                             | 13     | 
 | HANUNOO         | 23    | ᜠ᜴ᜱᜢ᜴ᜢ᜶ᜡ᜴᜵᜴ᜣᜥ                                             | 13     | 
 | BUHID           | 20    | ᝉᝇᝆᝋᝄᝋᝋᝎᝅᝋᝒᝇᝏ                                             | 13     | 
 | TAGBANWA        | 18    | ᝠᝲᝮᝲᝩ᝭ᝨ᝭ᝬᝬᝢᝫᝤ᝭                                            | 14     | 
 | KHMER           | 144   | ហឡៀភ៳្ៅស                                                  | 8      | 
 | MONGOLIAN       | 153   | ᠙ᢂᢙᡰᡚ᠃ᡛᠡ                                                  | 8      | 
 | LIMBU           | 68    | ᤀᤘᤖ᥀ᤍ᥀ᤳᤸᤲᤨ                                                | 10     | 
 | TAILE           | 118   | ᥐᥞ᧘ᦫᦉᦈᦉ᧐ᧀ                                                 | 9      | 
 | BUGINESE        | 30    | ᨁᨋᨇᨄᨎᨔᨌᨁᨓᨃᨄᨙ                                              | 12     | 
 | TAI_THAM        | 127   | ᩰᨪᨣᩏᩚᨽᨷᩍ                                                  | 8      | 
 | BALINESE        | 121   | ᭳ᬬ᭼᭜ᬑᬦ᭧ᬋ                                                  | 8      | 
 | SUNDANESE       | 72    | ᮺᮅ᮫ᮄᮢᮻᮤᮿᮟ                                                 | 9      | 
 | BATAK           | 56    | ᯆᯰᯌ᯲ᯎ᯦ᯝᯪᯔᯇ                                                | 10     | 
 | LEPCHA          | 74    | ᰮᰡᰅᰦᰁᰫᰣᰴᰝ                                                 | 9      | 
 | OL_CHIKI        | 48    | ᱮᱩᱥ᱐ᱵᱲᱼᱜᱛᱯ                                                | 10     | 
 | VEDIC           | 42    | ᳣᳑ᳶ᳤᳣᳝᳜ᳫᳳᳫ᳗                                               | 11     | 
 | GLAGOLITIC      | 94    | ⰅⱞⰈⰬⰪⰫⱁⱔⰓ                                                 | 9      | 
 | COPTIC          | 123   | ⳣⲗⲐⳟⲪⲘⳉⳀ                                                  | 8      | 
 | TIFINAGH        | 59    | ⴳⵥⵕⵆⴲⵁⴰⴵⵓⵔ                                                | 10     | 
 | RADICALS        | 329   | ⺡⽢⼽⺗⺓⽺⾺                                                   | 7      | 
 | BOPOMOFO        | 71    | ㆶㄊㄔㄌㄑㆺㄙㆮㆮ                                                 | 9      | 
 | HANGUL          | 666   | ㅶ㊂ㅭㅦᆡᅧ                                                    | 6      | 
 | HANZU           | 27558 | 㯶䆶瑩矅                                                      | 4      | 
 | IDEOGRAPHIC     | 879   | ㋜㎫都㍇藍꜁                                                    | 6      | 
 | FW_NUM          | 10    | ３１５６５２５７０６５９５２６６５                                         | 17     | 
 | FW_LOW          | 26    | ｋｐｈｂｆｋａｋｓｉｔｌ                                              | 12     | 
 | FW_UP           | 26    | ＫＰＨＢＦＫＡＫＳＩＴＬ                                              | 12     | 
 | FULL_WIDTH      | 103   | ＃Ｅ￥ｐ￦ｏｊ｀Ｊ                                                 | 9      | 
 | KOREAN          | 11172 | 각채댈쓫핟                                                     | 5      | 
 | HIRAGANA        | 102   | ㇲぱづじㇾじねぃぺ                                                 | 9      | 
 | KATAKANA        | 84    | ケゲケワダソヨギエ                                                 | 9      | 
 | HW_KATAKANA     | 63    | ｢ﾍﾝｷｭﾞﾕﾉﾜﾒ                                                | 10     | 
 | YI_SYLLABLE     | 1223  | ꀎꋙꎔꇴꇭꅲ                                                    | 6      | 
 | LISU            | 48    | ꓮꓩꓥꓐꓵꓲꓼꓜꓛꓯ                                                | 10     | 
 | VAI             | 300   | ꔹꖡꕖꕿꗩꔋꖯ                                                   | 7      | 
 | BAMUM           | 88    | ꚪꛫꚵꛮꛂꚺꛪꛦꛗ                                                 | 9      | 
 | SYLOTI          | 45    | ꠀꠊꠐ꠪ꠦꠑꠘꠧꠑꠖ꠨                                               | 11     | 
 | PHAGS_PA        | 56    | ꡆꡰꡌꡲꡎꡦꡝꡪꡔꡇ                                                | 10     | 
 | SAURASHTRA      | 82    | ꢓ꣎꣔ꢹꢌꢹꢸꢱꢗ                                                 | 9      | 
 | KAYAH           | 48    | ꤞꤙꤕ꤀ꤥꤢ꤬ꤌꤋꤟ                                                | 10     | 
 | REJANG          | 37    | ꤷꥏꤼꥑꤹꥋꤳꥅꥒꥏꥍ                                               | 11     | 
 | JAVA            | 91    | ꦈꦅ꧋꧃ꦗꦥ꧋꧕ꦿ                                                 | 9      | 
 | CHAM            | 83    | ꨑ꩘ꩀꨮꨭ꩔ꨕꩀꨤ                                                 | 9      | 
 | MEETEI          | 79    | ꯄꫩꫴꯡꯟ꯸ꯜꫵꯃ                                                 | 9      | 
 | BRAILLE         | 255   | ⢛⠷⣺⠖⣾⢖⢰                                                   | 7      | 
 | SUPERSCRIPT     | 14    | ⁴⁼⁻⁻⁸⁰⁴⁶ⁱ⁼ⁱ⁺⁸ⁿ⁹                                           | 15     | 
 | SUBSCRIPT       | 28    | ₄₃₁₆ₑ₄ₚₐₖ₄₍₇                                              | 12     | 
 | CURRENCY        | 32    | ₠₤₻₼₼₸₡₶₽₸₹₿                                              | 12     | 
 | SYMBOLS         | 2552  | ◸∥⯗≧⧨                                                     | 5      | 
 | CIRCLE_NUMS     | 71    | ⓹⑤⑮⑦⑫⓽⑳➆➆                                                 | 9      | 
 | CIRCLE_NEG_NUMS | 31    | ⓿⓴❶❷❽⓬❺⓭➐⓮➏❻                                              | 12     | 
 | PAREN_NUMS      | 20    | ⑽⑻⑺⑿⑸⑿⑿⒂⑹⑿⒆⑻⒃                                             | 13     | 
 | PAREN_LOW       | 26    | ⒦⒫⒣⒝⒡⒦⒜⒦⒮⒤⒯⒧                                              | 12     | 
 | CIRCLE_UP       | 26    | ⓀⓅⒽⒷⒻⓀⒶⓀⓈⒾⓉⓁ                                              | 12     | 
 | CIRCLE_LOW      | 26    | ⓚⓟⓗⓑⓕⓚⓐⓚⓢⓘⓣⓛ                                              | 12     | 
 | BOX             | 128   | ╊╦┼╣┪╼╍╟                                                  | 8      | 
 | BLOCK           | 33    | ▚▛▐▆▄▆▂■▗░▖                                               | 11     | 
 | HEXAGRAM        | 64    | ䷁䷖䷦䷝䷸䷄䷞䷵䷜䷟                                                | 10     | 
 | PRIVATE         | 6400  |                                                      | 5      | 
 | UTF8            | 65536 | 鷭䕾束                                                      | 4      | 
 | HASH            | 16    | 868cdd346e564f                                            | 14     | 
 | HASH_UP         | 16    | 868CDD346E564F                                            | 14     | 
 | ALPHA           | 52    | orodlleHWl                                                | 10     | 
 | ANUM            | 62    | 28ronp6qcB                                                | 10     | 
 | BASE64          | 64    | 1mCtU4uRsv                                                | 10     | 
 | ASCII           | 815   | ¶ɯY­͓ȗ                                                    | 6      | 
 | CIRCLES         | 154   | ⓣ⓺②⑰⓿⓷⓪㉕                                                  | 8      | 
 | CJK             | 40861 | ᇠ萞烹㞮                                                      | 4      | 
 | INDIAN          | 705   | ৷झॣదભঅ                                                    | 6      | 
 | SE_ASIA         | 1673  | ംေળჩౕಬ                                                    | 6      | 
 | VISIBLE         | 48687 | Ʋ몋꾜⡊                                                      | 4      | 
| SMP ----------  | ----- | --------------------------------------------------------- | -----  |
| GREEK_SMP       | 80    | 𐅘𐅡𐆌𐅠𐆎𐆊𐆁𐅐𐅏                                        | 18     | 
| ROMAN           | 13    | 𐆙𐆙𐆚𐆘𐆙𐆕𐆒𐆒𐆔𐆖𐆔𐆚𐆜𐆐𐆛                            | 30     | 
| AEGEAN          | 57    | 𐄉𐄫𐄾𐄞𐄷𐄪𐄍𐄭𐄟𐄲                                      | 20     | 
| PHAISTOS        | 46    | 𐇽𐇛𐇱𐇧𐇣𐇘𐇼𐇼𐇨𐇕                                      | 20     | 
| LYCIAN          | 29    | 𐊂𐊍𐊋𐊔𐊓𐊁𐊗𐊊𐊄𐊔𐊙𐊐                                  | 24     | 
| CARIAN          | 49    | 𐊹𐊩𐊦𐋅𐋈𐋄𐊣𐊦𐋍𐊧                                      | 20     | 
| OLD_ITALIC      | 36    | 𐌊𐌗𐌆𐌑𐌋𐌞𐌠𐌏𐌆𐌓𐌟                                    | 22     | 
| GOTHIC          | 30    | 𐌮𐌸𐌴𐌱𐌻𐍁𐌹𐌮𐍀𐌰𐌱𐍆                                  | 24     | 
| OLD_PERMIC      | 43    | 𐍐𐍸𐍶𐍙𐍓𐍴𐍞𐍠𐍠𐍸𐍵                                    | 22     | 
| UGARTIC         | 31    | 𐎀𐎔𐎁𐎂𐎈𐎌𐎅𐎍𐎛𐎎𐎚𐎆                                  | 24     | 
| OLD_PERSIAN     | 51    | 𐎰𐎬𐎴𐏕𐎧𐏌𐎰𐏊𐏉𐎵                                      | 20     | 
| DESERET         | 80    | 𐐘𐐡𐑌𐐠𐑎𐑊𐑁𐐐𐐏                                        | 18     | 
| SHAVIAN         | 48    | 𐑮𐑩𐑥𐑐𐑵𐑲𐑼𐑜𐑛𐑯                                      | 20     | 
| OSMANYA         | 40    | 𐒃𐒁𐒠𐒤𐒤𐒆𐒨𐒅𐒗𐒣𐒏                                    | 22     | 
| OSAGE           | 72    | 𐓮𐒵𐓟𐒴𐓒𐓯𐓘𐓳𐓏                                        | 18     | 
| ELBASAN         | 40    | 𐔃𐔁𐔞𐔢𐔢𐔆𐔦𐔅𐔗𐔡𐔏                                    | 22     | 
| ALBANIAN        | 53    | 𐔻𐕠𐕇𐔻𐕡𐕔𐕈𐕣𐕉𐕔                                      | 20     | 
| CYPRIOT         | 55    | 𐠋𐠐𐠦𐠥𐠤𐠖𐠡𐠎𐠖𐠀                                      | 20     | 
| ARAMAIC         | 31    | 𐡀𐡔𐡁𐡂𐡈𐡌𐡅𐡍𐡜𐡎𐡛𐡆                                  | 24     | 
| PALMYRENE       | 32    | 𐡠𐡤𐡻𐡼𐡼𐡸𐡡𐡶𐡽𐡸𐡹𐡿                                  | 24     | 
| NABATAEAN       | 40    | 𐢃𐢁𐢞𐢪𐢪𐢆𐢮𐢅𐢗𐢩𐢏                                    | 22     | 
| HATRAN          | 26    | 𐣪𐣯𐣧𐣡𐣥𐣪𐣠𐣪𐣲𐣨𐣴𐣫                                  | 24     | 
| PHOENICIAN      | 29    | 𐤂𐤍𐤋𐤔𐤓𐤁𐤗𐤊𐤄𐤔𐤙𐤐                                  | 24     | 
| LYDIAN          | 27    | 𐤦𐤱𐤦𐤫𐤫𐤸𐤲𐤹𐤵𐤪𐤫𐤶                                  | 24     | 
| MEROITIC        | 242   | 𐧡𐥸𐥶𐥳𐤯𐤹𐤓                                            | 14     | 
| KHAROSHTHI      | 68    | 𐨀𐨠𐨞𐩅𐨓𐩅𐨸𐩁𐨵𐨯                                      | 20     | 
| OLD_ARABIAN     | 64    | 𐩡𐩶𐪆𐩽𐪘𐩤𐩾𐪕𐩼𐩿                                      | 20     | 
| MANICHEAN       | 51    | 𐫑𐫍𐫕𐫶𐫈𐫭𐫑𐫫𐫦𐫖                                      | 20     | 
| AVESTAN         | 61    | 𐬂𐬦𐬤𐬓𐬥𐬱𐬵𐬉𐬛𐬐                                      | 20     | 
| PARTHIAN        | 30    | 𐭁𐭋𐭇𐭄𐭎𐭔𐭌𐭁𐭓𐭃𐭄𐭛                                  | 24     | 
| PAHLAVI         | 56    | 𐭦𐮜𐭬𐮪𐭮𐮋𐮂𐮏𐭹𐭧                                      | 20     | 
| OLD_TURKIC      | 73    | 𐰳𐱂𐰀𐰷𐰈𐰨𐰃𐰑𐱇                                        | 18     | 
| OLD_HUNGARIAN   | 158   | 𐲐𐳅𐲒𐲌𐳇𐲟𐴔𐳽                                          | 16     | 
| RUMI            | 31    | 𐹠𐹴𐹡𐹢𐹨𐹬𐹥𐹭𐹻𐹮𐹺𐹦                                  | 24     | 
| SOGDIAN         | 82    | 𐼓𐽎𐽔𐽁𐼌𐽁𐽀𐼹𐼗                                        | 18     | 
| BRAHMI          | 109   | 𑀁𑀎𑀰𑀸𑀋𑀣𑀣𑀗𑁜                                        | 18     | 
| KAITHI          | 66    | 𑂀𑂳𑂢𑂆𑂴𑂶𑂼𑂾𑂹𑂷                                      | 20     | 
| SORA_SOMPENG    | 36    | 𑃙𑃦𑃕𑃠𑃚𑃴𑃶𑃞𑃕𑃢𑃵                                    | 22     | 
| CHAKMA          | 70    | 𑄀𑄃𑄁𑄂𑄉𑄅𑄷𑄛𑄕𑄣                                      | 20     | 
| MAHAJANI        | 39    | 𑅔𑅘𑅠𑅱𑅴𑅨𑅠𑅘𑅦𑅓𑅵                                    | 22     | 
| SHARADA         | 96    | 𑆄𑇖𑇗𑆱𑇃𑆖𑆾𑆝𑆟                                        | 18     | 
| SHINHALA        | 20    | 𑇪𑇨𑇧𑇬𑇥𑇬𑇬𑇯𑇦𑇬𑇳𑇨𑇰                                | 26     | 
| KHOJKI          | 62    | 𑈂𑈈𑈜𑈙𑈘𑈚𑈆𑈛𑈌𑈦                                      | 20     | 
| MULTANI         | 38    | 𑊅𑊡𑊌𑊚𑊐𑊁𑊚𑊩𑊚𑊐𑊟                                    | 22     | 
| KHUDAWADI       | 69    | 𑊰𑊽𑊳𑊻𑊽𑋑𑊳𑋀𑋞𑋌                                      | 20     | 
| GRANTHA         | 85    | 𑌓𑍁𑌘𑌂𑌟𑌝𑍰𑌟𑌆                                        | 18     | 
| NEWA            | 97    | 𑐄𑐪𑐐𑑒𑐆𑑖𑐒𑑘𑑁                                        | 18     | 
| TIRHUTA         | 82    | 𑒓𑓆𑓔𑒹𑒌𑒹𑒸𑒱𑒗                                        | 18     | 
| SIDDHAM         | 92    | 𑖇𑖜𑖀𑗌𑖢𑖊𑖬𑗒𑖳                                        | 18     | 
| MODI            | 79    | 𑘛𑘉𑘔𑘸𑘶𑙘𑘳𑘕𑘚                                        | 18     | 
| TAKRI           | 67    | 𑚀𑚥𑚅𑚟𑚁𑚒𑚨𑛉𑚖𑚘                                      | 20     | 
| AHOM            | 58    | 𑜄𑜰𑜍𑜊𑜴𑜸𑜙𑜄𑜚𑜳                                      | 20     | 
| WARANG_CITI     | 84    | 𑢰𑢱𑢰𑣮𑢿𑢼𑣧𑢭𑢧                                        | 18     | 
| ZANABAZAR       | 72    | 𑨺𑨅𑨫𑨄𑨢𑨻𑨤𑨿𑨟                                        | 18     | 
| SOYOMBO         | 83    | 𑩡𑪝𑪇𑩾𑩽𑪙𑩥𑪇𑩴                                        | 18     | 
| PAU_CIN_HAU     | 57    | 𑫅𑫧𑫷𑫚𑫰𑫦𑫉𑫩𑫛𑫮                                      | 20     | 
| BHAIKSUKI       | 97    | 𑰄𑰫𑰑𑱞𑰆𑱢𑰓𑱤𑱃                                        | 18     | 
| MARCHEN         | 68    | 𑱰𑲈𑲆𑲪𑱽𑲪𑲠𑲥𑲟𑲙                                      | 20     | 
| CUNEIFORM       | 1235  | 𒀍𒒏𒁢𒉃𒊡𒉻                                              | 12     | 
| EGYPTIAN        | 1071  | 𓀝𓄱𓂵𓃲𓊰𓂯                                              | 12     | 
| ANATOLIAN       | 583   | 𔐀𔐱𔕵𔘌𔓿𔖇𔖗                                            | 14     | 
| BAMUM_SMP       | 570   | 𖠀𖢊𖢚𖣔𖤎𖡢𖦽                                            | 14     | 
| MRO             | 43    | 𖩀𖩩𖩧𖩉𖩃𖩥𖩎𖩐𖩐𖩩𖩦                                    | 22     | 
| BASSA_VAH       | 36    | 𖫚𖫧𖫖𖫡𖫛𖫰𖫲𖫟𖫖𖫣𖫱                                    | 22     | 
| PAHAWH_HMONG    | 127   | 𖭙𖬊𖬃𖬯𖬺𖬝𖬗𖬭                                          | 16     | 
| MIAO            | 149   | 𖼙𖼙𖼅𖾟𖽿𖼞𖽬𖽔                                          | 16     | 
| TANGUT          | 6892  | 𗀐𘢨𗔙𘑏𘔂                                                | 10     | 
| NUSHU           | 396   | 𛅺𛆉𛈋𛈛𛋝𛉟𛊯                                            | 14     | 
| DUPLOYAN        | 144   | 𛰠𛰡𛰾𛰗𛱩𛱐𛱃𛰟                                          | 16     | 
| MUSICAL         | 549   | 𝀀𝄽𝆏𝇙𝂤𝇆𝆊                                            | 14     | 
| MAYAN           | 20    | 𝋩𝋧𝋦𝋫𝋤𝋫𝋫𝋮𝋥𝋫𝋲𝋧𝋯                                | 26     | 
| TAIXUANJING     | 87    | 𝌋𝍖𝌓𝌢𝍃𝌍𝌎𝌛𝌐                                        | 18     | 
| COUNTING        | 25    | 𝍰𝍵𝍫𝍧𝍴𝍣𝍠𝍧𝍦𝍡𝍠𝍠                                  | 24     | 
| MATH            | 1020  | 𝐥𝚙𝕵𝞍𝖣𝒯                                              | 12     | 
| GLAGOLITIC_SMP  | 38    | 𞀅𞀠𞀋𞀘𞀎𞀁𞀘𞀪𞀘𞀎𞀞                                    | 22     | 
| NYIAKENG        | 71    | 𞅆𞄅𞄏𞄇𞄌𞅎𞄔𞄼𞄼                                        | 18     | 
| WANCHO          | 59    | 𞋃𞋵𞋥𞋖𞋂𞋑𞋀𞋅𞋣𞋤                                      | 20     | 
| MENDE_KIKAKUI   | 213   | 𞠁𞠝𞣔𞡹𞠁𞡿𞢈𞣉                                          | 16     | 
| ADLAM           | 88    | 𞤊𞥋𞤕𞥒𞤢𞤚𞥊𞥆𞤷                                        | 18     | 
| SIYAQ           | 68    | 𞱱𞲉𞲇𞲨𞱾𞲨𞲟𞲤𞲞𞲘                                      | 20     | 
| ARABIC_SMP      | 143   | 𞸤𞺮𞺛𞸔𞺔𞺇𞺕𞸌                                          | 16     | 
| MAHJONG         | 44    | 🀀🀘🀀🀤🀌🀟🀤🀁🀦🀊🀋                                    | 22     | 
| DOMINO          | 100   | 🀳🁉🁻🁓🂀🁼🀵🁔🁻                                        | 18     | 
| CARDS           | 82    | 🂵🃪🃰🃝🂬🃝🃜🃕🂹                                        | 18     | 
| CHESS           | 84    | 🨐🨑🨐🩎🨟🨜🩇🨍🨇                                        | 18     | 
| CHESS_CH        | 14    | 🩢🩪🩩🩩🩦🩠🩢🩤🩡🩪🩡🩨🩦🩭🩧                            | 30     | 
| COMMA_NUM       | 10    | 🄄🄂🄆🄇🄆🄃🄆🄈🄁🄇🄆🄊🄆🄃🄇🄇🄆                        | 34     | 
| PAREN_UP        | 26    | 🄚🄟🄗🄑🄕🄚🄐🄚🄢🄘🄣🄛                                  | 24     | 
| SQUARE_UP       | 26    | 🄺🄿🄷🄱🄵🄺🄰🄺🅂🄸🅃🄻                                  | 24     | 
| CIRCLE_UP_NEG   | 26    | 🅚🅟🅗🅑🅕🅚🅐🅚🅢🅘🅣🅛                                  | 24     | 
| SQUARE_UP_NEG   | 26    | 🅺🅿🅷🅱🅵🅺🅰🅺🆂🅸🆃🅻                                  | 24     | 
| ARROWS          | 150   | 🠛🢫🡮🠳🢛🠧🠜🠝                                          | 16     | 
| SYMBOLS_SMP     | 654   | 🙞🝁𐜝𐚾🝨🜚                                              | 12     | 
| EMOJI           | 1325  | 🌉🕒🫳🚆🐑🗕                                              | 12     | 
| IDEOGRAMS       | 269   | 𐂗𐃑🈛🈲𐀘𐀟𐁙                                            | 14     | 
| BLOCK_SMP       | 203   | 🬁🯊🮨🬤🮊🮭🭙🮡                                          | 16     | 
| DIGITS          | 10    | 🯳🯱🯵🯶🯵🯲🯵🯷🯰🯶🯵🯹🯵🯲🯶🯶🯵                        | 34     | 
| HIEROGLYPHIC    | 1896  | 𓀀𔑜𓌵𓎧𓄫𓍿                                              | 12     | 
| LINES_SMP       | 151   | ䷖𝌾䷜䷑𝍖𝌔䷹𝍆                                              | 12     | 
| WEDGE           | 1316  | 𐎉𒑀𒌎𐎀𒄲𒍮                                              | 12     | 
| VISIBLE_SMP     | 68498 | Å𓆠픸𘛐                                                    | 6      | 

As you can see, the resulting length is sometimes shorter and sometimes longer than the original string (length 10):

If you encode, for example, from HASH (16 chars) to ALPHA (52 chars) the resulting
   string length will be smaller than the original string (as we have more characters in the output
   charset to choose from). The opposite is also true:
   If you encode an alphanumeric string (ALPHA) to NUMBERS (10 chars), the resulting
   string length will be longer than the original string.

> NOTE: If you store data, and you use any of the characters inside the SMP (Plane 2), which
   are composed of multiple bytes, you may need to prepare your code to handle such strings. For
   example:
```
"日本語".toCharArray().length == 3  // Any character inside the BMP (Basic Multilingual Plane) can be stored inside a single `char`
"🕙🫛🎸🛡".toCharArray().length == 8 // Instead of iterating over those characters you will need to use getCodePoints() method.
```

#### Customizing

You can create your own charsets as well. The easiest way is to add two or more charsets
which are already defined:

```groovy
// LpCode.* is imported statically:
List<Integer> alphaNumericLowercase = NUMBERS + LOWERCASE
println new LpCode(alphaNumericLowercase, BRAILLE).encode("code900".toCharArray()) 
// ⠇⠎⠿⣳⡀
```

Or by adding their UTF-8 codes (see source code for examples) or using `getCodePoints` to get them from a String:

```groovy
// LpCode.* is imported statically:
List<Integer> phoneNumber = NUMBERS + getCodePoints("-()+")
println new LpCode(phoneNumber, KOREAN).encode("+55(0)1-800-222-3333".toCharArray())
// 귯좒퀆깋뒜뒧
```

#### To BigInteger

When you encode a String, `LpCode` will convert it first into a numeric representation and
then it will use the output charset to generate the encoded String. 

You can store that intermediary number, which can later be used to decode or continue encoding a string:

```groovy
// LpCode.* is imported statically:
int seed = 1000 // To randomize output

BigInteger num = toNumber("hello".toCharArray(), LOWERCASE, seed)
println num // 2556852

String orig = toString(num, LOWERCASE, seed)
println orig // hello

String encoded = toString(num, HEXAGRAM, seed)
println encoded // ䷈䷍䷳䷯
```

#### Translate

You can also use `LpCode` to "translate" (replace characters 1 to 1) from one charset to another, for example:

```groovy
// LpCode.* is imported statically:
println translate("something", LOWERCASE, SQUARE_UP_NEG)
// 🆂🅾🅼🅴🆃🅷🅸🅽🅶
```

Using custom charset:

```groovy
List<Integer> l33t = getCodePoints("48©Δ3ғ6ԨїԏϏ1мИ0ϼ9Я57μύώ×Ч2")
println translate("something", LOWERCASE, l33t)
// 50м37ԨїИ6
println translate("50м37ԨїИ6", l33t, LOWERCASE)
// something
```

> NOTE: In the case the target charset uses an out-of-bounds character, it will not translate. In other words, if you
> translate letters (lowercase) into numbers, only the first 10 characters (a-j) will be translated, the rest will be
> included without change.

## BouncyCastle and JAVA 8: 

To use BouncyCastle library, you don't need to do anything,
but if you are using Java 8 and the list of supported algorithms doesn't include
all the supported by that library, you may need to add it manually
to your `java.security` file:

### Download
* Go to https://www.bouncycastle.org/latest_releases.html
* Download the package with 'prov' in it, for example: bcprov-jdk15on-156.jar

### Install
* Copy the JAR into $JAVA_HOME/jre/lib/ext/
* Add the next line into: $JAVA_HOME/jre/lib/security/java.security

`security.provider.10=org.bouncycastle.jce.provider.BouncyCastleProvider`

below the other similar lines (you may need to fix the number before '=' sign).
