package com.mehmtcan.brainscroll.ui.home

import com.mehmtcan.brainscroll.game.GameKind

/**
 * What a player may type to find a game, in Turkish and in English (whichever language the app is in, both work): the things the game is
 * about, not only its name. "renk" finds every game that is about colours, whether or not "renk" is in its name.
 */
internal val GameKeywords: Map<GameKind, String> = mapOf(
    GameKind.Word to "kelime harf bulmaca wordle tahmin dil türkçe english word letter guess puzzle language anagram",
    GameKind.Memory to "hafıza bellek kart eşleştir çift eşleştirme memory card match pair remember concentration",
    GameKind.Tubes to "renk tüp sıvı sırala dök boya color colour tube liquid sort pour water",
    GameKind.Mines to "mayın tarla bomba bayrak sayı rakam mantık mine minesweeper bomb flag number digit logic",
    GameKind.Sudoku to "sudoku sayı rakam kare mantık number digit logic grid",
    GameKind.Lights to "ışık lamba söndür aç kapat anahtar lights lamp switch toggle bulb",
    GameKind.Slide to "kaydır kaydırmalı yap boz sayı parça sliding slide tile number puzzle",
    GameKind.Target to "hedef sayı işlem hesap matematik toplama çarpma target math number arithmetic calculate",
    GameKind.Pipes to "boru su bağla döndür akış pipe pipes water connect rotate flow",
    GameKind.Hanoi to "hanoi kule disk halka tower disc discs move stack",
    GameKind.Queens to "vezir kraliçe satranç bölge mantık queens queen chess region logic",
    GameKind.Tents to "çadır ağaç kamp orman tents tent tree camp forest",
    GameKind.Jigsaw to "jigsaw sudoku bulmaca parça şekil bölge sayı region shape number",
    GameKind.BlockOut to "blok parça yerleştir tetris sığdır block piece place fit",
    GameKind.Arrows to "ok oklar yön çıkış kaç arrow arrows direction escape exit",
    GameKind.Nonogram to "nonogram piksel resim boya ipucu çizim mantık picross pixel picture paint draw clue griddlers logic",
    GameKind.AxisSum to "eksen toplam satır sütun sayı axis sum row column number math",
    GameKind.BallSlide to "top kaydır boya duvar hücre ball slide paint wall cell",
    GameKind.ColorFlow to "renk akış çizgi bağla nokta flow color colour line connect path dot",
    GameKind.TopicGroups to "konu grup kelime kategori bağlantı topic group word category connections",
    GameKind.Matchsticks to "kibrit çöp çöpü eşitlik denklem sayı rakam matematik işlem matchstick matchsticks match equation stick number digit math arithmetic",
    GameKind.Lasers to "lazer ayna ışın hedef ışık laser mirror mirrors beam light target",
    GameKind.Untangle to "düğüm çöz çizgi nokta kesişme untangle knot line graph cross",
    GameKind.FruitMath to "meyve denklem sayı rakam matematik işlem toplama çarpma fruit equation number digit math arithmetic emoji algebra sum",
    GameKind.CodeBreaker to "kod kır şifre renk tahmin çıkarım mastermind code break cipher color colour guess deduction",
    GameKind.Rectangles to "dikdörtgen shikaku kare bölge alan sayı bölme rectangle rectangles area number divide shape",
    GameKind.LoneNumber to "yalnız sayı hitori karala kopya tekrar sayı rakam bulmaca mantık gölge siyah kare lone number hitori shade repeat duplicate unique number digit logic puzzle black cell",
    GameKind.ZerosOnes to "sıfır bir binairo takuzu ikili sayı rakam bulmaca mantık denge zeros ones binairo takuzu binary number digit puzzle logic balance",
    GameKind.CrossSum to "çapraz toplam kakuro toplam blok rakam sayı bulmaca mantık hesap matematik cross sums kakuro sum block digit number puzzle logic math",
    GameKind.OddOne to "farklı olanı bul çift bul uymayan aykırı kural sayı rakam asal çift tek kare matematik mantık odd one out different rule number digit prime even odd square math logic",
    GameKind.Mirror to "ayna simetri yansıma desen boya çizgi eksen yarım tamamla bulmaca mantık mirror symmetry reflection pattern paint axis half complete puzzle logic",
    GameKind.Fences to "çit çitler halka döngü çember kapalı çizgi nokta birleştir slitherlink sayı bulmaca mantık fence fences loop closed line dot connect dots number puzzle logic slither",
    GameKind.GreaterLess to "büyük küçük büyüktür küçüktür futoshiki eşitsizlik işaret sayı rakam bulmaca mantık latin kare greater less greater than less than futoshiki inequality sign number digit puzzle logic latin square",
    GameKind.Lamps to "ampul ampuller akari ışık lamba aydınlat sayı rakam bulmaca mantık duvar light up lamp lamps bulb akari light illuminate number digit puzzle logic wall",
    GameKind.CubeFold to "küp katlama küp katla açılım net uzay şekil yüz simge boyut zihinsel döndürme cube folding fold net space shape face symbol rotation",
    GameKind.ColorSeq to "renk dizisi simon dizi ses hafıza bellek ritim ışık düğme tekrar color sequence simon sequence sound memory rhythm light button repeat",
    GameKind.FlashGrid to "flaş ızgara hafıza bellek kare ezber ışık desen flash grid memory remember tile light pattern",
    GameKind.Line to "tek çizgi çizgi günlük one line daily draw path euler",
)

/** Lower case, and the Turkish letters (and accents) as their plain ones, so "isik" finds "ışık" and "CIZGI" finds "çizgi". */
internal fun foldForSearch(text: String): String {
    val out = StringBuilder(text.length)
    for (ch in text) {
        out.append(
            when (ch) {
                'I', 'İ', 'ı' -> 'i'
                'ş', 'Ş' -> 's'
                'ğ', 'Ğ' -> 'g'
                'ü', 'Ü' -> 'u'
                'ö', 'Ö' -> 'o'
                'ç', 'Ç' -> 'c'
                'â', 'Â' -> 'a'
                'î', 'Î' -> 'i'
                'û', 'Û' -> 'u'
                else -> ch.lowercaseChar()
            },
        )
    }
    return out.toString()
}

/**
 * Whether [game], shown under the name [name], is what [query] looks for. Every word of the query has to fit: it is in the name, or it is
 * the start of one of the game's keywords. An empty query fits every game.
 */
internal fun gameMatchesQuery(game: GameKind, name: String, query: String): Boolean {
    val words = foldForSearch(query).split(' ', '\t').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val foldedName = foldForSearch(name)
    val keywords = foldForSearch(GameKeywords[game].orEmpty()).split(' ').filter { it.isNotEmpty() }
    return words.all { word -> foldedName.contains(word) || keywords.any { it.startsWith(word) } }
}
