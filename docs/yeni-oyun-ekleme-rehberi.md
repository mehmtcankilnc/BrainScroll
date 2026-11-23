# Yeni oyun ekleme rehberi

Bir oyunun uygulamaya girmesi için dokunulması gereken her yerin listesi. Plan 3 ve plan 4'teki sekiz oyunun eklenmesinden çıktı, genel düzenlemeden sonra (2026-11-22/23: arka planda üretim, ipucu nedenleri, çok kartlı nasıl oynanır, sayfa süresi) baştan yazıldı. Bundan sonraki her yeni oyun bu listeyle eklenir.

Oyun planlarının hepsi tamamlandı ve silindi, özetleri decisions.md içinde ("Silinen oyun planları"). Kararların nedenleri: [decisions.md](decisions.md).

Çalışma düzeni: tek oyun, bitince dur, cihazda dene, "devam" de. Commit'e dokunulmaz (kullanıcı ister, kısa İngilizce mesajla).

## 0. Bir oyun neye benzemeli (planı yazmadan önce)

Bu beş şey oyunun tasarımına girer; sonradan eklemek pahalıdır.

1. **Tek çözümlü ve mantıkla çözülür.** Üretici tahtayı kurar, çözücü tek çözüm olduğunu doğrular. Tahmin gerektiren tahta üretilmez. Çözücü aynı zamanda ipucunun kaynağıdır (bölüm 4).
2. **Üretim hızlı ve saftır.** Üretici yalnız `seed`, seviye ve dilden çıkar; ortak durum (feed'in rastgele sayıları, torbalar) kullanmaz. Zor seviye telefonda uzun sürse de uygulama donmaz çünkü üretim arka planda koşar (bölüm 3). Yine de zor seviye 1-2 saniyeyi geçmemeli; `LineGeneratorTest` benzeri bir zamanlama testi yaz.
3. **Hamleler geri alınır ya da oyun kaybedilemez.** Akışta bir sayfa terk edilir, geri gelinir. Yarım oyun kaydedilir ve olduğu gibi geri kurulur (hamle listesini tekrar oynatarak).
4. **Oyunun görünümü kendine ait.** Nasıl oynanır kartı, küçük resim ve ipucu halkası bu görünümü taklit etmez, aynısını çizer (bölüm 5 ve 6).
5. **Hafıza gerektiren oyunda otomatik başlama yok** (bölüm 8).

## 1. Dosyalar (hepsine dokunulur)

Oyun mantığı (`shared/src/commonMain/kotlin/com/mehmtcan/brainscroll/`):
- `game/<oyun>/` altında: bulmaca modeli, oyun durumu (`...Game`), üretici, çözücü, `...Round` (`FeedRound`'u uygular: `snapshot`, kayıt (`toStored`/`restore`), `toFinished`, `moveCount`).
- `game/FeedRound.kt` ve `GameKind` enum'u: yeni giriş (`id` küçük harf ve rakam, 2-32 harf; sunucu tablosu bu biçimi kabul eder).
- `game/Difficulty.kt` ve `game/GameLevels.kt`: 3 seviye (Kolay/Orta/Zor), `LEVELLED_GAMES`. Seviye testi: `LevelsOf...Test` ailesinden birine eklenir.
- `game/wordle/EndlessFeed.kt`: **üç yer.** `buildRound(plan)` içinde üretici çağrısı (`GameKind.X -> XGenerator.generate(p.seed, level).let { XRound(index, p.id, it, language, now) }`), kayıttan geri kurma (`GameKind.X -> XRound.restore(...)`) ve oyuna özel eylem fonksiyonları (`placeX`, `restartX` ...). `buildRound` ortak duruma dokunmaz, yalnız `PagePlan`'dan (seed, seviye, dil) çalışır.
- `stats/Stats.kt`: oyuna özel istatistik hesabı. `ui/profile/ProfileScreen.kt`: profildeki satır.
- `game/guide/Guide.kt`, `BoardGuides.kt`, `SearchGuides.kt`, `ui/feed/GuideText.kt`: ipucu (bölüm 4).

Ekran ve bağlantı:
- `ui/<oyun>/<Oyun>Page.kt`: oyun sayfası. `ui/feed/GamePage.kt`: sayfayı seçen `when`.
- `ui/feed/FeedViewModel.kt` + `FeedActions.kt` + `ui/daily/DailyGameViewModel.kt` + `App.kt`: oyunun eylemleri (yerleştir, geri al, yeniden başlat, gönder ...). Toplu işlem için `addGames` gibi tek kayıtlı fonksiyonlar kullanılır.
- `ui/howto/<Oyun>HowTo.kt` (+ gerekirse `<Oyun>Scenes.kt`) ve `HowToPlay.kt` içindeki `cardOf`: "nasıl oynanır" kartı (bölüm 5).
- `ui/home/GameNames.kt` (ad), `ui/theme/GameWorld.kt` (oyunun rengi).
- `ui/home/GameSearch.kt`: **arama kelimeleri** (`GameKeywords`, Türkçe + İngilizce). Kelime yalnızca oyunda gerçekten olan şey için yazılır: "renk" yalnızca renk tüpleri, renk akışı, kod kırıcıda var; vezirler, nonogram, kayan top'ta renk kelimesi yok. Sayı içeren her oyuna "sayı rakam matematik işlem" gibi kelimeler girer (kibrit çöpü ve meyve denklemi dahil). Test: `GameSearchTest` her oyunun kelimesi olduğunu kontrol eder.
- `ui/home/GameThumbnails.kt`: oyunun küçük resmi (`<Oyun>Thumb`, gerçek oyun renkleri ve mantığıyla çizilir) **ve** `GameThumbnailImage` içindeki `when` satırı (bölüm 7).
- `composeResources/values/strings.xml` ve `values-tr/strings.xml`: tüm metinler iki dilde. **Metinlerde kesme işareti yok** (`\'` ekranda ters eğik çizgiyle görünür); `HowToContentTest` bunu denetler.
- Sunucu: sıralama fonksiyonu her oyun kimliğini kabul eder (migration `20261121090000`), yeni oyun için sunucu değişikliği gerekmez.

## 2. Test dosyaları (yeni oyunla birlikte yazılır)

- `commonTest/.../game/<oyun>/`: oyun kuralları, üretici (tek çözüm, boyut, tohumdan aynı tahta), çözücü, `Round` (kayıt ve geri kurma, `moveCount`).
- `jvmTest/.../ui/<Oyun>UiTest.kt`: sayfa, dokunuşlar, kazanma, `snap()` ekran görüntüleri (`shared/build/screenshots`, gözle bakılır).
- `SmallScreenUiTest`: 360x600 ve yazı 1,5x. `HowToStepsUiTest`: yeni oyunun adı `fiveSteps(...)` listesine girer. `GuideReasonsTest` ve `GuideRingUiTest`: ipucu.
- Testlerde `start(games = listOf(GameKind.Word, GameKind.X))` kullanılır. Tahtalar testte hemen yapılır (`buildInBackground = false` varsayılan); iskelet sayfası `GeneratingPageUiTest` ve `PendingPagesTest` içinde sınanır.
- Yeni test yazınca **bir kez bilerek bozup başarısız olduğunu gör.**

## 3. Arka planda üretim (bir oyun yapmak zorunda olduğu şey)

Üretim ana iş parçacığında koşmaz. `EndlessFeed` yeni sayfa için önce bir `PagePlan` yapar (oyun, seviye, `seed`, kimlik, dil; paylaşılan rastgele sayıları kullanan her karar burada, ana iş parçacığında alınır), sonra tahtayı `build(plan)` ile kurar. `FeedViewModel` bunu `Dispatchers.Default` üzerinde sırayla çalıştırır; bitene kadar sayfa `PendingRound` olur ve ekranda `GeneratingPage` iskeleti ("Bulmaca hazırlanıyor") görünür.

Yeni oyun için sonuçları:
- Üretici ve `...Round` kurucusu **saf** olmalı: yalnız `PagePlan`'dan gelen `seed`/seviye/dil. Global değişken, ortak `Random`, `now()` dışında saat yok.
- Aynı `seed` ve seviye her zaman aynı tahtayı verir (kayıt ve günlük bulmaca buna dayanır).
- `PendingRound` üzerinde oyun eylemleri etkisizdir (`as? XRound` kontrolü); yeni eylem eklerken bu kontrolü unutma.
- Başlatıcıda `init` içinde coroutine başlatan bir sınıfta, coroutine'in kullandığı alanlar `init`'ten **önce** bildirilmeli (bir kez `buildQueue` sırası yüzünden NPE alındı).

## 4. İpucu: ne + neden

İpucu tek bir çözücü adımıdır ve artık "ne yap" kadar "neden" de söyler. Kuralı: **cümle iki parça, "Buraya 4 koy. Satırda 1-2-3 var ve sağdaki işaret 3'ten büyük diyor."** (kesme işareti yazıda yok; örnek anlatım için).

- `GuideStep(reason, cells, numbers, text, evidence, rule)`: `reason` hangi cümle kalıbı, `cells` cevabın yeri, `evidence` cevabı doğuran hücreler (Mayın'da çevredeki sayılar, Sudoku'da satırdaki rakamlar, işaret...), `rule` aynı `reason` içinde hangi kuralın çalıştığı (cümlenin bir yerini seçer).
- Sayfa `evidence` hücrelerine **yumuşak kesik halka** çizer (`isGuideEvidence`, `guideEvidence` etiketi), cevaba ise kalın, nabızlı halka (`GuideMark`). Böylece neden gözle de görülür.
- Çözücü kuralı ve dayanak hücrelerini **döndürmeli** (`forcedWhy`, `why`, `lineBlock` gibi). Bunun için çözücü yazarken adımı "bu hücre şu yüzden" biçiminde kaydet; sadece sonucu tutan çözücüden neden çıkmaz.
- Aramaya dayalı oyunlarda (en kısa yolun ilk adımı) hamle sınıflandırılır ve sınıfa özel neden yazılır (Hanoi: büyük diski açmak, Tüpler: aynı renge dökmek ...). Kesin bir neden yoksa dürüst cümle: "Bu, en kısa çözümün ilk adımı çünkü ...".
- Metin: her cümle en çok yaklaşık 70 karakter, rakamlar `numbers`'tan; banner en çok 6 satır. `GuideText.kt` içindeki `when` her `GuideReason`'ı kapsar; TR ve EN anahtarları eklenir.
- **Çözücünün doğruluğu** rastgele kısmi atamayla sınanır (`GuideReasonsTest`): rastgele bir kısmi doldurma yap, çözücünün çıkardığı her hücrenin tek çözümde gerçekten o değerde olduğunu doğrula. Çitler'de çözücünün kapalı-döngü kuralı yanlış çıkmıştı ve bu testle yakalandı. Üretici de çözücünün çözemediği tahtayı reddeder.
- Çitler dersi: ipucu yalnız **çizilecek** çizgiyi gösterir, çarpı önermez ("burası çarpı olmalı" ipucu alakasız kalıyordu).
- Kendi ipucunu zaten açıklayan oyunlar (Konu Grupları, Meyve Denklemi) ya da neden anlatılamayanlar (Flaş Izgara) değişmeden kalabilir; bu bilerek karar olarak yazılır.

## 5. Nasıl oynanır kartı

Her oyunun kartı **5 ile 7 adım**dır (`HowToContentTest` denetler: en az 5, en çok 7, son kart tek "spoiler", TR ve EN anahtar kümesi aynı).

Şablon:
1. **Amaç** (kazanmak ne demek; gerekirse hareketli mini örnek).
2. **Kurallar ve dokunuş**: 1-3 kart (kontrol kartları; eski üç kart bu rolde).
3. **Nasıl çözülür**: 1-2 teknik kartı (`title = howto_title_solve`). Oyunun gerçek çıkarım yöntemi: "tek gizli hücre", "satır yarı doldu, kalan öbür rakam" gibi. Kart "neden"i gösterir, tahmin göstermez.
4. **İlk adım** (son kart, `spoiler = true`, `title = howto_title_spoiler`): nereden başlanacağı. Başlık spoiler olduğunu açıkça söyler; ayrı düğme yok, oyuncu geçer.

Metin kuralları: TR ana metin, EN çeviri; kesme işareti yok; her kartın metni 2-3 cümle. Uzun metin kısa ekran + büyük yazıda kaydırılır (orta bölüm kaydırılabilir, başlık, noktalar ve düğmeler sabit); yine de kısa tut.

**Resim oyunun kendi görünümüdür.** Bu en çok geri dönen hata oldu: genel sahne motoruyla çizilen rakamlı kareler, düz renkli hücreler oyunun gerçek görünümünü (duvar, taş, disk, tüp, çubuk, şekilli top) vermiyor ve kullanıcı reddetti. İki yol var:

- **Oyunun kendi çizimini kullan (varsayılan).** Oyun sayfasının çizim yardımcılarını (`drawWall`, `drawFlowBoard`, `Peg`, `drawMatchBoard`, `drawUntangle`, `FruitRow`...) çağıran bir `XDemo(step: Int, t: Float)` yaz; `step` 0.. adımı seçer, `t` 0..1 döngü zamanıdır. Adımlar veri odaklı olsun (Hanoi'de `start` + `moves` listesi, Tüpler'de `start` + `pours`). `HowToStep(caption, { t -> XDemo(step = 3, t = t) }, cycleMillis, title, spoiler)`. Elin boyutu oyunun hücresine göre (`cell * 0.95f`; kibrit çöpünde `size.width * 0.2f`; küçük el yeni hata sayıldı).
- **Genel sahne motoru (`DemoScene`)** yalnız oyunun görünümü zaten düz ızgara olan oyunlar için (Sudoku, Mayın, çıkarım ızgaraları). Bir sahne başlangıç hücreleri + zamanlı olaylardır: `demoScene(rows, cols) { row(0, "53..7"); ring(0.1f, 0.5f, SceneRole.Target, 2 to 2); place(0.7f, 4, 4, "5") }` ve `sceneStep(caption, scene, title, spoiler)`. Olaylar: `place/open/close/mark/tap/ring/tint/edge/line/turn`; bölge, kenar çizgisi, boru ucu, düğüm ve ok/chevron işaretleri vardır. Dile bağlı kelimeler için `languageSceneStep` (Konu Grupları). `ring(... Target ...)` ipucunun cevap halkasıdır, `Evidence` dayanak halkası: kart ipucunu da öğretir. Çok kelimeli panolarda `uniformText()` (tüm kelimeler aynı boyutta).

Kasma olmasın (kullanıcının koşulu):
- Zaman `State<Float>` olarak çizim aşamasında okunur (`rememberDemoTimeState`); kart kare başına yeniden birleştirilmez.
- Boyuta bağlı her şey (hücre kutuları, ölçülmüş metin, hücre başına olay listesi) `drawWithCache` içinde boyut başına bir kez hazırlanır; kare yalnız değişen şeyi çizer, nesne üretmez (tek `Path` yeniden kullanılır).
- Hareketi azalt açıkken sabit kare (`REDUCED_DEMO_TIME`). Sahne testleri: `DemoSceneTest` (her olay döngünün içinde, kare süresi), eski demolar için `HowToDemoStepsTest` (üç anda ekran görüntüsü).

Resmi yazınca: `shared/build/screenshots` içindeki `49_scene_*` ve `50_*` görüntülerine **gözle bak** ve oyunun gerçek sayfasıyla yan yana karşılaştır. `HowToStepsUiTest.fiveSteps` kartı baştan sona gezer, kartın ekran içinde kaldığını ve son kartın spoiler olduğunu doğrular.

## 6. Süre

Zamanlı oyunda **sayfa süresi** kuralı: süre sayfanın ekranda kaldığı süredir. `FeedRound.onShown()` sayacı başlatır (ya da sürdürür), `onHidden()` durdurur ve o ana kadarını toplar; `toStored().input` ve `toFinished().durationMs` canlı toplamı (`total()`) kullanır. Tahta bitince sayaç durur. İlk dokunuşla başlayan ya da iki hamle arasını sınırlayan süre **yanlıştır**: Tek Çizgi'de tahtaya bakıp kafadan çözen 2-3 saniyede bitiriyordu, düzeltildi (`docs/decisions.md`, "Tek Çizgi süresi").

- Yalnız bakılıp çıkılan tahtanın da süresi kaydedilsin: `isUntouched = actions.isEmpty() && elapsedMs == 0L` (aksi halde uygulamayı kapatıp açan süreyi sıfırlar).
- Feed dışında oynanan sayfa (günlük oyun ekranı) kendi `DailyGameViewModel.onResumed/onPaused` çağrılarıyla `pageShown/pagesHidden` tetikler; yeni günlük oyun ekranı bunu atlamamalı.
- Süre geriye alınmaz, sonuç ekranındaki süre sayfa kapanınca değişmez.

## 7. Akış düzenleme ekranındaki resim (PNG)

Düzenleme ekranı resimleri canlı çizmez, uygulamaya gömülü PNG gösterir (`composeResources/drawable/thumb_<id>.png`, Kelime oyunu için `thumb_word_tr` ve `thumb_word_en`). Canlı çizim telefonda ilk açılışta kasıyordu; bitmap önbelleği ve arka plan hazırlığı da yetmedi, dosya çözümü akıcı çıktı.

Yeni oyunda veya bir resim değişince:
1. Resmi `GameThumbnails.kt`'ye yaz, `GameThumbnail` içindeki `when`'e ekle.
2. PNG'leri üret (PowerShell):
   `$env:GENERATE_THUMBS = "1"; ./gradlew.bat :shared:jvmTest --tests "*ThumbnailPngTest"`
3. `GameThumbnailImage` içindeki `when`'e `GameKind.X -> Res.drawable.thumb_x` ve import'unu ekle.
4. Normal test koşusunda `ThumbnailPngTest.everyGameHasItsPicture` dosyası eksik oyunu yakalar.

Yalnızca koyu tema üretilir (uygulamada açık tema yok). AMOLED (Pro) görünümünde çerçeve çok az açık görünebilir. Ana sayfa ve sıralama rozetleri hâlâ canlı `GameThumbnail` kullanır; `warmUpThumbnailSamples()` bunların tahtalarını uygulama açılırken arka planda hazırlar.

## 8. Oyun sayfası kuralları

- **Otomatik başlama yok.** Hafıza gerektiren oyunda (Flaş Izgara) tahtanın üstünde Başla düğmesi olur; turlar arasında yeni düğme olmaz. Oyundan kayıp dönünce desen yeniden gösterilmez, "gizlendi" mesajı çıkar. Altta her zaman yeniden başlat düğmesi (yeni desen üretir, Başla'ya döner) bulunur.
- **İpucu ve mesaj yazısı** her oyunda `type.body`. `caption` veya `title` kullanılmaz (üç oyunda küçük çıkmıştı).
- **Sayı seçme düzeni.** Sayı girilen oyunda önce hücre seçilir, sonra altta numpad benzeri sayı tuşlarından sayı verilir (Büyük-Küçük, Hedef Sayı). Sayıları sırayla döndürmek yorucu bulundu.
- **Tek hücreli kısayollar** oyunun başında düşünülür (Dikdörtgenler'de tek "1" hücresine dokunmak kendiliğinden seçer).
- **Kaydırılabilir liste içeren sayfa** (Kod Kırıcı): içerik ekrana sığıyorsa liste kaydırılmaz (`userScrollEnabled = canScrollForward || canScrollBackward`), yoksa sonraki oyuna geçiş kaydırması zorlaşır. Liste her zaman son satırda başlar (animasyonsuz).
- **Kod Kırıcı geri bildirimi** örnek alınabilir: düz yeşil halka doğru yer, kesik yeşil halka başka yer, gri halka yok. Renk körlüğü için yalnız renge güvenme: desen (düz/kesik) farkı olsun.
- **Kayıt sınırları:** tahta kodlaması `BitText` ile en çok 32 karakter (`answer`), kaydedilen `guesses` en çok 256 karakter.
- **Ses:** `SoundHaptics` ortak sesleri çalar; yeni ses yapmak yerine mevcut ailelerden birini kullan (Flaş Izgara nonogram'ın pentatonik merdivenini kullanır).
- **Akış düzenleme:** "Tümünü seç" tek yönlüdür (sadece açar, en az bir oyun seçili kalır, aramadayken yalnızca bulunanları seçer). Arama alanı hep açıktır. Seçili kart işareti içi dolu halka, tümünü seç ikonu dolu onay kutusudur (ikisi karışmasın).
- **Boş sıralama tablosu:** bütün oyunlar için aynı kesikli podyum ikonu ve davet eden metin. Sıralama tabloları yalnızca günlük bulmaca sonuçlarını gösterir; akıştaki oyunların sonuçları tabloya yazılmaz.

## 9. Kontrol listesi (her oyun için bitmeden önce)

Mevcut kural: `feedback-new-game-checklist` hafıza dosyası (özeti):
1. Animasyonun ilk karesi bitiş halini göstermez.
2. Kazanma efektleri (kart, titreşim, konfeti) son animasyondan sonra gelir (`rememberSettledStatus`, `OutcomeHaptics`).
3. Testleri bir kez bilerek bozup başarısız olduğunu gör.
4. Titreşim: seç/koy hafif, hamle orta, reddedilen hamle uyarı, başarı kazanma gösterildikten sonra, yeniden başlat hafif.
5. Erişilebilirlik: her dokunulan şeyin açıklaması ve durumu, sonuçta canlı bölge, hareketi azalt, yüksek kontrast, 360x600 küçük ekran + yazı 1,5x (`SmallScreenUiTest`).
6. Tüm testler Türkçe ve İngilizce (`JAVA_TOOL_OPTIONS=-Duser.language=en -Duser.country=US`).
7. Zor seviye üretimi arka planda ve hızlı; iskelet sayfası görünür, uygulama donmaz.
8. İpucu "ne + neden" söyler, dayanak halkası görünür, çözücü rastgele kısmi atamayla sınanmış.
9. Nasıl oynanır kartı 5-7 adım, son kart spoiler, resimler oyunun kendi görünümünde, el boyutu doğru.
10. Süre sayfa süresi (`onShown/onHidden`), tahta bitince durur.
11. Cihazda dene: hem Türkçe hem İngilizce, küçük ekranda da.

Bu listeye ek olarak her yeni oyunda: arama kelimeleri (`GameSearch.kt`), PNG üretimi (bölüm 7), `docs/oyun-reelleri-plan.md`'ye sahne, istatistik satırı, `docs/decisions.md`'ye karar notu, hafıza notu.

## 10. Açık işler

- Plan 3 (Kod Kırıcı, Dikdörtgenler, Flaş Izgara) ve plan 4 (Yalnız Sayı, Sıfır-Bir, Ampuller, Büyük-Küçük, Çitler) kodlandı, cihazda denendi ve beğenildi. Farklı Ton (Odd Shade) bilerek askıda; sebepleri `decisions.md` içinde ("Silinen oyun planları").
- Genel düzenleme (2026-11-22/23): Kod Kırıcı halka renkleri, arka planda üretim ve iskelet sayfası, ipucu nedenleri, 33 oyunun 5-7 kartlı nasıl oynanır'ı kodlandı. Nasıl oynanır'da cihaz denemesi sonrası 11 oyunun resimleri oyunun kendi çizimine çevrildi (düzeltme cihazda yeniden denenecek). Çıkarım ızgaralarının (Nonogram, Çadırlar, Vezirler, Çitler, Borular, Oklar...) resimleri genel sahne motoruyla çiziliyor; kullanıcı gerçek görünümle karşılaştırdı, sorun yok.
- Yeni oyun fikirleri: Ayna, Farklı Olanı Bul, Çapraz Toplam, Renk Dizisi, Küp Katlama: kodlandı ve cihazda denendi (plan silindi, kararlar decisions.md içinde); oynanabilir örnekleri Oyun Atölyesi sayfasında.
- Üç yeni oyun için `docs/oyun-reelleri-plan.md`'ye reel sahnesi **henüz eklenmedi**. Ses kararlarına (`docs/decisions.md`) Flaş Izgara'nın nonogram merdivenini kullandığı notu eklenebilir.
- Sunucu migration'ı `20261121090000_game_boards_all_games.sql` uzak veritabanına **uygulandı**.
- Commit edilmemiş değişiklikler çok birikti (yeni oyunlar, düzenleme ekranı, ipucu nedenleri, arka planda üretim, nasıl oynanır, Tek Çizgi süresi). Commit kullanıcının isteğine bağlı.
- Diğer bekleyenler hafıza dosyalarında: Pro Aşama B, yayın aşaması, Faz 10 adımları, zorluk dengelemesi.
