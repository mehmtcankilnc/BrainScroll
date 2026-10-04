# BrainScroll - Tasarım Belgesi (Tokens)

Son güncelleme: 2026-10-04 | Aktif palet: **N1 Yumuşak yeşil** (Neşeli ailesi): **yeşil = doğru, sarı = yakın/bekleyen**. Renk değerleri henüz **geçici**.
Renk değerleri (§2.1) değişebilir. Değişmeyen kısım **roller ve anlamsal tokenlar** (§2.2, §2.3). Palet değişirse yalnızca `Palette` ve `DarkColors` güncellenir, bileşen kodu etkilenmez. Nihai renk kararı gerçek cihazda (Android emülatörü + TestFlight) oynanabilir bir ekranla verilir.
Kararlar için bkz. [decisions.md](decisions.md), fazlar için [plan.md](plan.md).

## 1. İlkeler

- **Karakter:** Cesur, neşeli, ama sakin. Nötr koyu zemin üzerinde canlı renkler. Mor-mavi gradyan, cam efekti ve zemine renk basma yok.
- **Koyu tema öncelikli.** Açık tema sonra eklenir. Bu yüzden bileşenler **yalnızca anlamsal token** kullanır, hiçbir bileşende ham hex olmaz.
- **Tek tasarım sistemi, iki platform.** Material 3'ün hazır görünümü veya iOS taklidi yok. Düğme, kart, karo, klavye gibi bileşenler kendi sistemimizde tanımlı.
- **Renk tek başına anlam taşımaz.** Her durumun bir şekil/doku karşılığı vardır (bkz. Erişilebilirlik).
- **Düz yüzeyler.** Gölge ve gradyan yok. Derinlik yalnızca yüzey tonu farkıyla verilir.
- **İsimlendirme:** "Wordle" The New York Times'ın tescilli markasıdır. Uygulama içinde ve mağaza metinlerinde kullanılmaz.

| Oyun | TR adı | EN adı |
|---|---|---|
| Wordle tarzı | Kelime bulmacası | Word puzzle |
| Sudoku | Sudoku | Sudoku |
| Hafıza eşleştirme | Hafıza eşleştirme | Memory match |
| Satranç | Satranç bulmacası | Chess puzzle |

## 2. Renk tokenları

### 2.1 Ham palet (ilkel değerler)

Bileşenler bunları doğrudan kullanmaz, yalnızca anlamsal tokenlar bunlara işaret eder.

| İsim | Değer | Not |
|---|---|---|
| `neutral.900` | `#121212` | Sayfa zemini |
| `neutral.800` | `#1F1F1F` | Yüzey (kart, chip, tuş) |
| `neutral.700` | `#2A2A2A` | Yükseltilmiş yüzey |
| `neutral.600` | `#343434` | Açık kare (satranç) |
| `neutral.500` | `#3A3A3A` | Çizgi, pasif ikon |
| `neutral.400` | `#6B6B6B` | Sönük metin (yok olan harf) |
| `neutral.300` | `#8C8C8C` | İkincil metin |
| `neutral.50` | `#F5F5F5` | Ana metin |
| `green.500` | `#7ED957` | Başarı / doğru |
| `green.900` | `#10240A` | Yeşil üstü metin |
| `yellow.500` | `#FFC93C` | Bekleyen / seçili / yakın |
| `yellow.900` | `#2B2200` | Sarı dolgu üstü metin (gerekirse) |
| `blue.500` | `#4DB8FF` | Doğru cevap serisi (şimşek) |
| `red.500` | `#FF5A5A` | Hata |
| `orange.500` | `#FF9F43` | Gün serisi (alev) |

### 2.2 Anlamsal tokenlar (bileşenlerin kullandığı katman)

| Token | Koyu tema | Kullanım |
|---|---|---|
| `bg.page` | `neutral.900` | Ekran zemini |
| `bg.surface` | `neutral.800` | Kart, chip, klavye tuşu, kapalı kart |
| `bg.raised` | `neutral.700` | Üstte duran yüzey (diyalog, menü) |
| `border.subtle` | `neutral.500` @ %55 | Boş karo çerçevesi, hücre çizgisi |
| `border.strong` | `neutral.300` | Dolu giriş çerçevesi, kutu çizgisi (Sudoku 3x3) |
| `text.primary` | `neutral.50` | Gövde metin |
| `text.secondary` | `neutral.300` | Destek metin, pasif sekme |
| `text.disabled` | `neutral.400` | Yok olan harf, pasif öğe |
| `state.correct.fill` | `green.500` | Doğru karo, eşleşen kart, seçili Sudoku hücresi, doğru hamle |
| `state.correct.on` | `green.900` | Doğru dolgu üzerindeki metin/ikon |
| `state.pending.fill` | `yellow.500` | Seçili taş, açık kart, "yakın" harf çerçevesi |
| `state.pending.on` | `yellow.900` | Sarı dolgu üzerindeki metin (çoğunlukla çerçeve kullanılır) |
| `state.error` | `red.500` | Hata, çakışan hücre, hata sayacı |
| `state.absent.fill` | `neutral.800` | Kelimede olmayan harf karosu |
| `state.absent.on` | `neutral.400` | Olmayan harf metni |
| `streak.day` | `orange.500` | Gün serisi (alev) |
| `streak.answer` | `blue.500` | Doğru cevap serisi (şimşek) |
| `highlight.sameValue` | `green.500` @ %22 | Sudoku: seçili rakamla aynı rakamlar |
| `highlight.peer` | `bg.surface` | Sudoku: seçili hücrenin satır/sütun/kutusu |
| `highlight.lastMove` | `green.500` @ %40 | Satranç: doğru hamlenin başlangıç/varış karesi |
| `highlight.selection` | `yellow.500` @ %25 + çerçeve | Satranç: seçili kare |
| `board.light` / `board.dark` | `neutral.600` / `neutral.700` | Satranç kareleri |

### 2.3 Oyunlar arası durum eşlemesi

Bir renk her oyunda aynı anlamı taşır. Oyuncu bir oyunu öğrenince diğerlerini de okur.

| Anlam | Kelime | Sudoku | Hafıza | Satranç |
|---|---|---|---|---|
| Doğru / başarı (yeşil) | Doğru yerde harf | Seçili hücre | Eşleşen kart | Doğru hamle karesi |
| Bekleyen / seçili (sarı çerçeve) | Yanlış yerde harf | Not modu vurgusu | Açık, eşleşmemiş kart | Seçili taş, gidebileceği kareler |
| Yok / pasif (gri) | Olmayan harf | Verilen rakam (beyaz) | Kapalı kart | Tahta |
| Hata (kırmızı) | , | Çakışan rakam | , | Yanlış hamle uyarısı |

### 2.4 Açık tema

Faz 8'de eklenir. Kural: yalnızca §2.2'deki anlamsal tokenların açık değerleri tanımlanır, bileşen koduna dokunulmaz. Yeşil üzerindeki koyu metin ve sarı çerçeve kontrastları açık zeminde yeniden doğrulanır (açık zeminde sarı çerçeve zayıf kalabilir, gerekirse koyu bir sarı tonu gerekir).

## 3. Tipografi

- **Aile:** Yuvarlak, geometrik bir sans. Uygulamaya **gömülü** (Compose'ta platform fontuna güvenmeyiz). Açık lisanslı (OFL).
- **Zorunlu kriter:** Türkçe harfler (`ç ğ ı İ ö ş ü` ve büyük halleri) **doğrulanmış olmalı**. Seçim yapılmadan önce her aday için bir test ekranı çizilir.
- **Geçici seçim:** Nunito. Adaylar: Nunito, Quicksand, Baloo 2. Nihai karar Faz 1'de ekran testiyle verilir.
- Büyük-küçük harf dönüşümlerinde `İ/I` tuzağına karşı `Locale.ROOT` yerine açık TR kuralı kullanılır (bkz. plan Faz 2).

| Stil | Boyut (sp) | Ağırlık | Kullanım |
|---|---|---|---|
| `display` | 32 | 700 | Büyük sayı (süre, sonuç) |
| `title` | 22 | 700 | Ekran başlığı |
| `heading` | 18 | 500 | Oyun başlığı |
| `body` | 16 | 400 | Gövde |
| `label` | 14 | 500 | Düğme, chip, sekme |
| `caption` | 12 | 400 | Yardımcı metin |
| `tile` | 22 | 500 | Karo / hücre harf ve rakamları (ölçeklenir) |

Yazı boyutu sistem ayarına uyar. Izgara içeriği (karo, hücre) sabit kalıp sınırlı ölçeklenir ki yerleşim bozulmasın.

## 4. Boşluk, boyut ve köşe

- **Birim:** 4 dp.
- **Boşluk ölçeği:** `xs 4`, `sm 8`, `md 12`, `lg 16`, `xl 24`, `2xl 32`.
- **Ekran kenar boşluğu:** 16 dp.
- **Dokunma hedefi:** en az 48 dp (görsel öğe küçük olsa bile).

| Token | Değer | Kullanım |
|---|---|---|
| `radius.tile` | 8 | Kelime karosu, Sudoku vurgusu |
| `radius.control` | 12 | Düğme, hafıza kartı, araç düğmesi |
| `radius.card` | 16 | Sonuç kartı, diyalog |
| `radius.pill` | 999 | Chip, etiket |
| `radius.board` | 8 | Satranç tahtası dış köşesi |

| Bileşen boyutu | Değer |
|---|---|
| Kelime karosu | 56 dp (ızgara genişliğine göre ölçeklenir, boşluk 6) |
| Sudoku hücresi | ekran genişliğinin 1/9'u, ince çizgi 0,5, kutu çizgisi 1,5 dp |
| Hafıza kartı | 4 sütunda eşit, boşluk 8 |
| Satranç karesi | tahta genişliğinin 1/8'i |
| HUD yüksekliği | 40 dp |
| Alt sekme çubuğu | 64 dp |

## 5. Yükselti ve çizgi

Gölge yok. Katmanlar `bg.page` -> `bg.surface` -> `bg.raised` ile ayrılır. Çizgi kalınlıkları: ince 0,5 dp, orta 1,5 dp, vurgu çerçevesi 2,5 dp (seçili/yakın durumlar).

## 6. Hareket ve haptik

| Token | Süre | Eğri | Kullanım |
|---|---|---|---|
| `motion.fast` | 100 ms | standart | Tuş basma, harf girişi "pop" |
| `motion.base` | 200 ms | standart | Durum geçişleri, chip güncelleme |
| `motion.slow` | 400 ms | standart | Sonuç kartı girişi |
| `motion.flip` | 500 ms | standart | Karo çevirme (karo başına 100 ms kademe) |
| `motion.pop` | 250 ms | overshoot | Harf yerleşme, seri artışı |

- Standart eğri: `cubic-bezier(0.2, 0, 0, 1)`. Overshoot: `cubic-bezier(0.34, 1.56, 0.64, 1)`.
- Sayfa geçişi (pager) fizik tabanlı yay animasyonudur ve kesilebilir (kullanıcı yarıda geri dönebilir).
- Günlük bulmacada ağ gecikmesi, harf çevirme animasyonunun arkasında gizlenir (plan Faz 6).
- Sistem "hareketi azalt" ayarına uyulur: çevirme ve pop animasyonları anlık geçişe dönüşür.
- Konfeti yalnızca günlük bulmaca kazanma ve seri kilometre taşlarında.

| Haptik | Olay |
|---|---|
| Hafif | Tuş, kare/hücre seçme, kart çevirme |
| Orta | Doğru cevap, eşleşme |
| Uyarı | Yanlış cevap, geçersiz hamle |
| Başarı | Bulmaca tamamlama, seri artışı |

## 7. Bileşen kataloğu

Hepsi §2.2 ve §4'teki tokenlarla tanımlanır.

| Bileşen | Özet |
|---|---|
| **HUD** | Üstte, 40 dp. Solda gün serisi (alev) ve doğru cevap serisi (şimşek) chip'leri, sağda oyuna göre sayaç (süre, atlama hakkı). Seri artınca chip kısa süre vurgulanır. |
| **Alt sekme çubuğu** | Dört sekme: **Akış, Günlük, Sıralama, Profil**. Aktif sekme `text.primary`, diğerleri `text.secondary`. |
| **Oyun başlığı** | `heading` + `pill` etiketleri (boyut/zorluk, hata sayacı). |
| **Kelime karosu** | Boş: `border.subtle`. Dolu giriş: `border.strong`. Doğru: `state.correct`. Yakın: boş içerik + 2,5 dp `state.pending` çerçeve. Yok: `state.absent`. |
| **Kelime klavyesi** | TR düzeni (Q, W, X yok; `Ğ Ü Ş İ Ö Ç` var), EN düzeni ayrı. Tuşlar karo durumunu yansıtır. Kaydırma jestini yutmaz. |
| **Sudoku hücresi** | Verilen rakam beyaz, kullanıcı rakamı `state.correct` renginde, seçili hücre `state.correct` dolgu, çakışma `state.error`. Notlar `text.secondary`, hücre içinde 3x3 ızgarada. |
| **Sudoku rakam tuşu** | 9 tuş, seçili rakam `state.correct`. Araç satırı: geri al, sil, not (aktifken `state.pending` sarı çerçeve), ipucu. |
| **Hafıza kartı** | Kapalı: `bg.surface` + soluk beyin ikonu. Açık: `state.pending` çerçeve. Eşleşen: `state.correct` dolgu. İçerik rengi değil şekil (ikon). |
| **Satranç tahtası** | `board.light/dark` kareler, beyaz taşlar `text.primary`, siyah taşlar koyu + ince açık kontur. Seçili kare `highlight.selection`, gidebileceği kareler sarı nokta. Siyah oynuyorsa tahta çevrilir. |
| **Sonuç kartı** | `bg.surface`, `radius.card`. Başlık, süre, seri, favori kalbi. Satrançta hamle notasyonu ve "ilk denemede" rozeti. |
| **Kaydır ipucu** | Yukarı ok + "Sıradaki oyun için kaydır". Otomatik ilerleme yok. |
| **Düğme** | İkincil: `bg.surface`. Birincil (görünümde en fazla bir tane): `state.correct` dolgu + `state.correct.on`. |
| **Chip** | `bg.surface`, `radius.pill`, `label`. |

## 8. Ekran düzeni

- Her akış sayfası tam ekran. Kaydırma jesti yalnızca **oyun alanı/başlık** bölgesinde alınır. Klavye, rakam takımı ve satranç tahtası dikey kaydırmayı yutmaz (yanlış sayfa geçişi olmasın). Oyun bittikten sonra tüm sayfa kaydırılabilir.
- Günün bulmacası **ayrı sekme**, akışın içinde değil.
- İlk açılışta onboarding ekranı yok. Kullanıcı doğrudan ilk oyuna düşer, "nasıl oynanır" katmanı oyunun içinde.
- Pager'da yalnızca mevcut ve sonraki sayfa canlı tutulur.

## 9. Erişilebilirlik

- **Renk körü uyumu:** Doğru = dolu, yakın = çerçeveli/boş, yok = sönük. Durumlar renk olmadan da ayırt edilir. Hafıza'da eşleşme ikonla, Sudoku'da çakışma yalnızca renkle değil çakışan hücrenin çerçeve vurgusuyla da belirtilir (uygulamada eklenecek).
- **Yüksek kontrast ayarı:** Çerçeve kalınlıklarını artırır ve sönük metni açar.
- **Kontrast:** Ana renklerin koyu zemin üzerindeki oranları yeterli görünüyor (sarı, mavi, kırmızı, ikincil gri). Kesin oranlar kodda bir araçla doğrulanır, bu belgedeki hiçbir rakam ölçüm değildir.
- **Ekran okuyucu:** Karo "A harfi, doğru yerde", Sudoku hücresi "Satır 3, sütun 4, rakam 6, kullanıcı girişi", hafıza kartı "Kapalı kart, 7. konum", satranç karesi "e1, beyaz kale" şeklinde okunur.
- **Dokunma hedefi:** 48 dp altındaki görsel öğelerin dokunma alanı genişletilir.
- Sistem yazı boyutu ve "hareketi azalt" ayarlarına uyulur.

## 10. Açık kararlar

- **HUD renk yakınlığı:** Gün serisi alevi (`orange.500`, `#FF9F43`) ile bekleyen/yakın sarısı (`yellow.500`, `#FFC93C`) birbirine yakın tonlar. Alev yalnızca HUD'da küçük bir ikon olduğu için karışması beklenmiyor, gerçek cihazda doğrulanacak. Şimşek mavisi artık `state.pending` ile çakışmıyor.
- **Palet:** N1 aktif ama değerler **geçici**. Sudoku, Hafıza ve Satranç taslakları N1 ile çizildi ve sarının "bekleyen" rolü üç oyunda da ayırt edici göründü. Mockup'taki gözlem, gerçek cihaz doğrulaması değil. Nihai karar Faz 3'te gerçek ekranla verilir.
- **Satranç noktaları:** Gidebileceği kare noktaları (`yellow.500`) açık karelerde biraz sönük kalabiliyor. Nokta opaklığı artırılabilir, uygulamada ayarlanacak.
- **Oyun başına vurgu rengi:** Şimdilik hiç yok, tüm oyunlar aynı paleti paylaşır. Oyun sayısı artınca ihtiyaç doğarsa ayrı karar.
- **Font:** Nunito geçici (§3). Türkçe glif testi Faz 1'de.
- **İkon seti:** Mock'larda Tabler (outline, MIT) kullanıldı. Compose Multiplatform'a hangi biçimde (vektör/ImageVector) gireceği Faz 1'de belirlenir.
- **Logo/ikon:** Yön seçildi: **beyin maskotu** (yeşil, gülümseyen, yanaklı bir beyin karakteri, koyu zeminde). Taslak: [assets/logo-mascot-draft.svg](assets/logo-mascot-draft.svg). Bu taslak elle yapılmış basit geometridir, nihai illüstrasyon değildir. Açık işler:
  - Beyin şeklinin, yüzün ve oranların rafine edilmesi (gerekirse bir illüstratörle).
  - Yüz ifadeleri seti (sevinç, üzüntü, düşünme) ve seri artışı/hata anları için animasyon.
  - Platform ikonları: iOS 1024x1024 (köşe maskesini sistem uygular), Android uyarlanabilir ikon (ön/arka katman), mağaza görselleri.
  - Küçük boyutta (29 px) okunurluk testi, yeşil zeminli ters varyant (alternatif) hâlâ seçenek olarak duruyor.
  - Maskot renkleri N1 paletine bağlı, palet değişirse yeniden çizilir.
  - Kelime işareti: "brain" (`text.primary`) + "scroll" (`state.correct`), yazı tipi final font seçildikten sonra.
- **Satranç taş seti:** Lisansı repoya uygun, açık lisanslı bir set seçilecek (bkz. plan, Satranç notları).

## 11. Kodda tokenlar (Compose Multiplatform taslağı)

Önerilen yer: `composeApp/src/commonMain/kotlin/com/mehmtcan/brainscroll/ui/theme/`

```kotlin
// Color.kt - ham palet, yalnızca Theme.kt tarafından kullanılır
internal object Palette {
    val Neutral900 = Color(0xFF121212)
    val Neutral800 = Color(0xFF1F1F1F)
    val Neutral300 = Color(0xFF8C8C8C)
    val Neutral50  = Color(0xFFF5F5F5)
    val Green500   = Color(0xFF7ED957)
    val Green900   = Color(0xFF10240A)
    val Yellow500  = Color(0xFFFFC93C)
    val Blue500    = Color(0xFF4DB8FF)
    val Red500     = Color(0xFFFF5A5A)
    val Orange500  = Color(0xFFFF9F43)
}

// Tokens.kt - bileşenlerin okuduğu anlamsal katman
@Immutable
data class BrainScrollColors(
    val bgPage: Color,
    val bgSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val correctFill: Color,
    val correctOn: Color,
    val pendingFill: Color,
    val error: Color,
    val streakDay: Color,
    val streakAnswer: Color,
)

val DarkColors = BrainScrollColors(
    bgPage = Palette.Neutral900,
    bgSurface = Palette.Neutral800,
    textPrimary = Palette.Neutral50,
    textSecondary = Palette.Neutral300,
    correctFill = Palette.Green500,
    correctOn = Palette.Green900,
    pendingFill = Palette.Yellow500,
    error = Palette.Red500,
    streakDay = Palette.Orange500,
    streakAnswer = Palette.Blue500,
)

val LocalBrainScrollColors = staticCompositionLocalOf { DarkColors }
```

Kural: bileşen kodunda `Color(0x...)` yazılmaz, yalnızca `LocalBrainScrollColors.current` okunur. Açık tema eklendiğinde yalnızca `LightColors` tanımlanır.
