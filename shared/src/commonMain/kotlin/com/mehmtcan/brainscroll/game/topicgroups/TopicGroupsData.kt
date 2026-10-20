package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * One topic of the word groups game: a name and the words that belong to it.
 *
 * [tier] says how obvious the topic is (0 plain, 1 a little less, 2 for the hard level). [family] names the kind of topic: two
 * topics of one family (fruits and vegetables) are easy to mix up, which is what the hard level wants.
 */
class TopicGroup(val title: String, val words: List<String>, val tier: Int, val family: String)

/**
 * Every topic of the game, per language. A puzzle is saved as the positions of its four topics in these lists, so the lists only
 * ever grow at the end: a topic or a word that is changed or moved would change a game that was saved. A word is in one topic of
 * a language only (a test checks it), and a topic holds only words that fit nothing else here, so a puzzle never has a word that
 * could go in two of its groups.
 *
 * The words are written as they are shown: in capitals, with the capitals of the language (Turkish has İ and I).
 */
object TopicGroupsData {
    fun topics(language: Language): List<TopicGroup> = when (language) {
        Language.EN -> english
        Language.TR -> turkish
    }

    private fun t(title: String, tier: Int, family: String, words: String) = TopicGroup(title, words.split(' '), tier, family)

    private val english = listOf(
        // Tier 0
        t("Fruits", 0, "food", "APPLE BANANA GRAPE LEMON PEACH MANGO CHERRY"),
        t("Farm animals", 0, "animals", "COW SHEEP GOAT HORSE PIG DONKEY"),
        t("Colors", 0, "colors", "RED BLUE GREEN PINK BROWN PURPLE GRAY"),
        t("Body parts", 0, "body", "ARM LEG KNEE ELBOW ANKLE WRIST SHOULDER"),
        t("Planets", 0, "space", "MARS VENUS SATURN JUPITER MERCURY NEPTUNE"),
        t("Musical instruments", 0, "music", "PIANO GUITAR VIOLIN FLUTE DRUM TRUMPET HARP"),
        t("Weather", 0, "nature", "RAIN SNOW FOG HAIL WIND THUNDER"),
        t("On the dinner table", 0, "home", "SPOON FORK PLATE KNIFE BOWL NAPKIN"),
        t("Clothes", 0, "clothes", "SHIRT SOCKS JACKET SCARF GLOVES DRESS SKIRT"),
        t("Vegetables", 0, "food", "CARROT ONION POTATO SPINACH CUCUMBER LETTUCE CELERY"),
        t("Wild animals", 0, "animals", "LION TIGER ZEBRA GIRAFFE WOLF ELEPHANT KANGAROO"),
        t("Sports", 0, "sports", "TENNIS GOLF RUGBY BOXING HOCKEY CRICKET SWIMMING"),
        t("Countries", 0, "geography", "FRANCE BRAZIL JAPAN EGYPT CANADA INDIA MEXICO"),
        // Tier 1
        t("Chess pieces", 1, "games", "ROOK BISHOP KNIGHT PAWN"),
        t("Playing cards", 1, "games", "ACE JACK QUEEN KING JOKER"),
        t("Gemstones", 1, "materials", "DIAMOND EMERALD SAPPHIRE OPAL TOPAZ PEARL"),
        t("Programming languages", 1, "tech", "PYTHON JAVA KOTLIN RUST SCALA HASKELL"),
        t("Birds", 1, "animals", "EAGLE OWL PARROT PENGUIN SPARROW FALCON FLAMINGO"),
        t("Capital cities", 1, "geography", "PARIS LONDON ROME MADRID BERLIN TOKYO CAIRO"),
        t("Seas and oceans", 1, "geography", "PACIFIC ATLANTIC ARCTIC BALTIC CARIBBEAN ADRIATIC"),
        t("Stationery", 1, "school", "PENCIL ERASER STAPLER MARKER SHARPENER NOTEBOOK"),
        t("Tools", 1, "tools", "HAMMER WRENCH PLIERS DRILL SAW CHISEL SHOVEL"),
        t("Dances", 1, "music", "WALTZ TANGO BALLET SAMBA POLKA RUMBA"),
        t("Breakfast", 1, "food", "TOAST OMELETTE PANCAKE CEREAL BAGEL WAFFLE"),
        t("Trees", 1, "nature", "OAK PINE MAPLE BIRCH WILLOW CEDAR ELM"),
        t("Sea creatures", 1, "animals", "SHARK WHALE DOLPHIN OCTOPUS JELLYFISH SEAHORSE LOBSTER"),
        t("Insects", 1, "animals", "ANT BEE BUTTERFLY BEETLE MOSQUITO DRAGONFLY LADYBUG"),
        t("Shapes", 1, "school", "CIRCLE SQUARE TRIANGLE OVAL RHOMBUS PENTAGON HEXAGON"),
        t("Drinks", 1, "drinks", "TEA COFFEE MILK JUICE SODA LEMONADE COCOA"),
        // Tier 2
        t("Car parts", 2, "transport", "ENGINE BRAKE CLUTCH BUMPER RADIATOR EXHAUST"),
        t("Greek gods", 2, "myth", "ZEUS HERA APOLLO ATHENA ARES HERMES POSEIDON"),
        t("Painters", 2, "art", "MONET PICASSO DALI REMBRANDT MATISSE WARHOL"),
        t("Composers", 2, "music", "MOZART BEETHOVEN BACH CHOPIN VIVALDI HANDEL"),
        t("Scientists", 2, "science", "NEWTON EINSTEIN DARWIN CURIE TESLA GALILEO"),
        t("Units of measure", 2, "science", "METER LITER GRAM INCH MILE OUNCE"),
        t("Punctuation", 2, "language", "COMMA PERIOD HYPHEN BRACKET SEMICOLON"),
        t("Cheeses", 2, "food", "CHEDDAR BRIE GOUDA RICOTTA PARMESAN FETA"),
        t("Pasta", 2, "food", "SPAGHETTI PENNE LASAGNA RAVIOLI MACARONI FUSILLI"),
        t("Spices", 2, "food", "CUMIN PAPRIKA CINNAMON NUTMEG TURMERIC SAFFRON CLOVE"),
    )

    private val turkish = listOf(
        // Tier 0
        t("Meyveler", 0, "food", "ELMA MUZ ÜZÜM KİRAZ ŞEFTALİ KARPUZ ÇİLEK"),
        t("Çiftlik hayvanları", 0, "animals", "İNEK KOYUN KEÇİ EŞEK MANDA BOĞA"),
        t("Renkler", 0, "colors", "KIRMIZI MAVİ YEŞİL PEMBE TURUNCU MOR GRİ"),
        t("Vücut parçaları", 0, "body", "KOL BACAK DİZ DİRSEK BİLEK OMUZ PARMAK"),
        t("Gezegenler", 0, "space", "MARS VENÜS SATÜRN JÜPİTER MERKÜR NEPTÜN URANÜS"),
        t("Müzik aletleri", 0, "music", "PİYANO GİTAR KEMAN FLÜT DAVUL SAZ NEY"),
        t("Hava olayları", 0, "nature", "YAĞMUR KAR SİS DOLU RÜZGAR FIRTINA ŞİMŞEK"),
        t("Mutfak eşyaları", 0, "home", "KAŞIK ÇATAL BIÇAK TABAK KASE TENCERE"),
        t("Giysiler", 0, "clothes", "GÖMLEK ÇORAP CEKET ATKI ELDİVEN ELBİSE ETEK"),
        t("Sebzeler", 0, "food", "HAVUÇ SOĞAN PATATES ISPANAK SALATALIK MARUL KEREVİZ"),
        t("Vahşi hayvanlar", 0, "animals", "ASLAN KAPLAN ZEBRA ZÜRAFA KURT AYI KANGURU"),
        t("Sporlar", 0, "sports", "TENİS GOLF RAGBİ BOKS HOKEY YÜZME GÜREŞ"),
        t("Ülkeler", 0, "geography", "FRANSA BREZİLYA JAPONYA MISIR KANADA HİNDİSTAN MEKSİKA"),
        // Tier 1
        t("Satranç taşları", 1, "games", "AT FİL KALE VEZİR ŞAH PİYON"),
        t("İskambil kartları", 1, "games", "AS PAPAZ KIZ VALE JOKER"),
        t("Değerli taşlar", 1, "materials", "ELMAS ZÜMRÜT YAKUT SAFİR İNCİ TOPAZ OPAL"),
        t("Programlama dilleri", 1, "tech", "PYTHON JAVA KOTLIN RUST SCALA HASKELL"),
        t("Kuşlar", 1, "animals", "KARTAL BAYKUŞ PAPAĞAN PENGUEN SERÇE ŞAHİN FLAMİNGO"),
        t("Başkentler", 1, "geography", "PARİS LONDRA ROMA MADRİD BERLİN TOKYO ATİNA"),
        t("Türkiye'nin şehirleri", 1, "geography", "İSTANBUL İZMİR BURSA ANTALYA ADANA KONYA TRABZON ANKARA"),
        t("Denizler", 1, "geography", "KARADENİZ AKDENİZ EGE MARMARA HAZAR BALTIK"),
        t("Kırtasiye", 1, "school", "KALEM SİLGİ CETVEL DEFTER KALEMTRAŞ ZIMBA MAKAS"),
        t("El aletleri", 1, "tools", "ÇEKİÇ ANAHTAR TORNAVİDA PENSE MATKAP TESTERE"),
        t("Danslar", 1, "music", "HALAY ZEYBEK HORON SAMBA TANGO VALS BALE"),
        t("Kahvaltılıklar", 1, "food", "ZEYTİN BAL REÇEL SİMİT OMLET MENEMEN PEYNİR TEREYAĞI"),
        t("Ağaçlar", 1, "nature", "MEŞE ÇAM ÇINAR KAVAK SÖĞÜT ARDIÇ KAYIN"),
        t("Deniz canlıları", 1, "animals", "BALİNA YUNUS AHTAPOT KARİDES MİDYE İSTAKOZ YENGEÇ"),
        t("Böcekler", 1, "animals", "KARINCA ARI KELEBEK SİNEK PİRE"),
        t("Şekiller", 1, "school", "DAİRE KARE ÜÇGEN OVAL ALTIGEN BEŞGEN"),
        t("İçecekler", 1, "drinks", "ÇAY KAHVE AYRAN ŞALGAM LİMONATA SALEP BOZA"),
        t("Yemekler", 1, "food", "KEBAP LAHMACUN MANTI PİDE DÖNER GÖZLEME"),
        // Tier 2
        t("Araba parçaları", 2, "transport", "MOTOR FREN DEBRİYAJ TAMPON RADYATÖR EGZOZ"),
        t("Yunan tanrıları", 2, "myth", "ZEUS HERA APOLLON ATHENA ARES HERMES POSEIDON"),
        t("Ressamlar", 2, "art", "MONET PICASSO DALİ REMBRANDT MATISSE WARHOL"),
        t("Besteciler", 2, "music", "MOZART BEETHOVEN BACH CHOPIN VİVALDİ HANDEL"),
        t("Bilim insanları", 2, "science", "NEWTON EINSTEIN DARWIN CURIE TESLA GALİLEO"),
        t("Ölçü birimleri", 2, "science", "METRE LİTRE GRAM SANTİM KİLO DÖNÜM"),
        t("Noktalama işaretleri", 2, "language", "NOKTA VİRGÜL TİRE ÜNLEM PARANTEZ TIRNAK"),
        t("Tatlılar", 2, "food", "BAKLAVA KÜNEFE SÜTLAÇ KADAYIF LOKUM REVANİ"),
        t("Baharatlar", 2, "food", "KİMYON KEKİK TARÇIN SUMAK KARANFİL ZENCEFİL"),
    )
}
