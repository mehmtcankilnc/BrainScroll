# Yeni oyun ekleme rehberi

Plan 3'teki üç oyunun (Kod Kırıcı, Dikdörtgenler, Flaş Izgara) eklenmesi sırasında çıkan, bir oyunun uygulamaya girmesi için dokunulması gereken her yerin listesi. Bundan sonraki her yeni oyun bu listeyle eklenir. Oyun planları: [yeni-oyunlar-plan.md](yeni-oyunlar-plan.md), [-2](yeni-oyunlar-plan-2.md), [-3](yeni-oyunlar-plan-3.md). Çalışma düzeni: tek oyun, bitince dur, cihazda dene, "devam" de, commit'e dokunulmaz.

## 1. Dosyalar (hepsine dokunulur)

Oyun mantığı (`shared/src/commonMain/kotlin/com/mehmtcan/brainscroll/`):
- `game/<oyun>/` altında: bulmaca modeli, oyun durumu (`...Game`), üretici (tek çözümlü olmalı, çözücüyle doğrulanır), `...Round` (`FeedRound`'u uygular: `snapshot`, `withoutClock`, kayıt, `moveCount`).
- `game/FeedRound.kt` ve `GameKind` enum'u: yeni giriş (`id` küçük harf ve rakam, 2-32 harf; sunucu tablosu bu biçimi kabul eder).
- `game/Difficulty.kt` ve `game/GameLevels.kt`: 3 seviye (Kolay/Orta/Zor), `LEVELLED_GAMES`.
- `game/wordle/EndlessFeed.kt`: oyunu akışa ve tur üretimine bağla.
- `stats/Stats.kt`: oyuna özel istatistik hesabı. `ui/profile/ProfileScreen.kt`: profildeki satır.
- `game/guide/Guide.kt`, `BoardGuides.kt`, `ui/feed/GuideText.kt`: rehber (`GuideReason`/`GuideStep`).

Ekran ve bağlantı:
- `ui/<oyun>/<Oyun>Page.kt`: oyun sayfası. `ui/feed/GamePage.kt`: sayfayı seçen `when`.
- `ui/feed/FeedViewModel.kt` + `FeedActions.kt` + `ui/daily/DailyGameViewModel.kt` + `App.kt`: oyunun eylemleri (yerleştir, geri al, yeniden başlat, gönder ...). Toplu işlem için `addGames` gibi tek kayıtlı fonksiyonlar kullanılır.
- `ui/howto/<Oyun>HowTo.kt` + `HowToPlay.kt`: "nasıl oynanır" kartı.
- `ui/home/GameNames.kt` (ad), `ui/theme/GameWorld.kt` (oyunun rengi).
- `ui/home/GameSearch.kt`: **arama kelimeleri** (`GameKeywords`, Türkçe + İngilizce). Kelime yalnızca oyunda gerçekten olan şey için yazılır: "renk" yalnızca renk tüpleri, renk akışı, kod kırıcıda var; vezirler, nonogram, kayan top'ta renk kelimesi yok. Sayı içeren her oyuna "sayı rakam matematik işlem" gibi kelimeler girer (kibrit çöpü ve meyve denklemi dahil). Test: `GameSearchTest` her oyunun kelimesi olduğunu kontrol eder.
- `ui/home/GameThumbnails.kt`: oyunun küçük resmi (`<Oyun>Thumb`, gerçek oyun renkleri ve mantığıyla çizilir) **ve** `GameThumbnailImage` içindeki `when` satırı (aşağıya bak).
- `composeResources/values/strings.xml` ve `values-tr/strings.xml`: tüm metinler iki dilde. Metinlerde kesme işareti kullanma (`\'` ekranda ters eğik çizgiyle görünür).
- Sunucu: sıralama fonksiyonu artık her oyun kimliğini kabul eder (migration `20261121090000`), yeni oyun için sunucu değişikliği gerekmez.

## 2. Akış düzenleme ekranındaki resim (PNG)

Düzenleme ekranı resimleri canlı çizmez, uygulamaya gömülü PNG gösterir (`composeResources/drawable/thumb_<id>.png`, Kelime oyunu için `thumb_word_tr` ve `thumb_word_en`). Canlı çizim telefonda ilk açılışta kasıyordu; bitmap önbelleği ve arka plan hazırlığı da yetmedi, dosya çözümü akıcı çıktı.

Yeni oyunda veya bir resim değişince:
1. Resmi `GameThumbnails.kt`'ye yaz, `GameThumbnail` içindeki `when`'e ekle.
2. PNG'leri üret (PowerShell):
   `$env:GENERATE_THUMBS = "1"; ./gradlew.bat :shared:jvmTest --tests "*ThumbnailPngTest"`
3. `GameThumbnailImage` içindeki `when`'e `GameKind.X -> Res.drawable.thumb_x` ve import'unu ekle.
4. Normal test koşusunda `ThumbnailPngTest.everyGameHasItsPicture` dosyası eksik oyunu yakalar.

Yalnızca koyu tema üretilir (uygulamada açık tema yok). AMOLED (Pro) görünümünde çerçeve çok az açık görünebilir. Ana sayfa ve sıralama rozetleri hâlâ canlı `GameThumbnail` kullanır; `warmUpThumbnailSamples()` bunların tahtalarını uygulama açılırken arka planda hazırlar.

## 3. Bu oturumda öğrenilen kurallar

- **Otomatik başlama yok.** Hafıza gerektiren oyunda (Flaş Izgara) tahtanın üstünde Başla düğmesi olur; turlar arasında yeni düğme olmaz. Oyundan kayıp dönünce desen yeniden gösterilmez, "gizlendi" mesajı çıkar. Altta her zaman yeniden başlat düğmesi (yeni desen üretir, Başla'ya döner) bulunur.
- **İpucu yazısı boyutu:** oyun içi ipucu/mesaj yazısı her oyunda `type.body`. `caption` veya `title` kullanılmaz (üç oyunda küçük çıkmıştı).
- **Tek hücreli tıklama:** Dikdörtgenler'de tek "1" hücresine dokunmak kendiliğinden seçer; benzer kısayollar oyunun başında düşünülür.
- **Kaydırılabilir liste içeren sayfa** (Kod Kırıcı): içerik ekrana sığıyorsa liste kaydırılmaz (`userScrollEnabled = canScrollForward || canScrollBackward`), yoksa sonraki oyuna geçiş kaydırması zorlaşır. Liste her zaman son satırda başlar (animasyonsuz).
- **Süre kuralı:** sayfa süresi sayfanın ekranda kaldığı süredir (`onShown/onHidden`).
- **Kayıt sınırları:** tahta kodlaması `BitText` ile en çok 32 karakter (`answer`), kaydedilen `guesses` en çok 256 karakter.
- **Ses:** `SoundHaptics` ortak sesleri çalar; yeni ses yapmak yerine mevcut ailelerden birini kullan (Flaş Izgara nonogram'ın pentatonik merdivenini kullanır).
- **Akış düzenleme:** "Tümünü seç" tek yönlüdür (sadece açar, en az bir oyun seçili kalır, aramadayken yalnızca bulunanları seçer). Arama alanı hep açıktır. Seçili kart işareti içi dolu halka, tümünü seç ikonu dolu onay kutusudur (ikisi karışmasın).
- **Boş sıralama tablosu:** bütün oyunlar için aynı kesikli podyum ikonu ve davet eden metin. Sıralama tabloları yalnızca günlük bulmaca sonuçlarını gösterir; akıştaki oyunların sonuçları tabloya yazılmaz, bu yüzden bu tablolar boştur (ayrı iş olarak istenebilir).

## 4. Kontrol listesi (her oyun için bitmeden önce)

Mevcut kural: `feedback-new-game-checklist` hafıza dosyası (özeti):
1. Animasyonun ilk karesi bitiş halini göstermez.
2. Kazanma efektleri (kart, titreşim, konfeti) son animasyondan sonra gelir (`rememberSettledStatus`, `OutcomeHaptics`).
3. Testleri bir kez bilerek bozup başarısız olduğunu gör.
4. Titreşim: seç/koy hafif, hamle orta, reddedilen hamle uyarı, başarı kazanma gösterildikten sonra, yeniden başlat hafif.
5. Erişilebilirlik: her dokunulan şeyin açıklaması ve durumu, sonuçta canlı bölge, hareketi azalt, yüksek kontrast, 360x600 küçük ekran + yazı 1.5x (`SmallScreenUiTest`).
6. Tüm testler Türkçe ve İngilizce (`JAVA_TOOL_OPTIONS=-Duser.language=en -Duser.country=US`).
7. Cihazda dene: hem Türkçe hem İngilizce.

Bu rehbere ek olarak her yeni oyunda: arama kelimeleri (`GameSearch.kt`), PNG üretimi (bölüm 2), `docs/oyun-reelleri-plan.md`'ye sahne, nasıl oynanır kartı, rehber, istatistik satırı.

## 5. Açık işler (sonraki oturum)

- Plan 3'ün üç oyunu (Kod Kırıcı, Dikdörtgenler, Flaş Izgara) kodlandı, cihazda denendi ve beğenildi. Farklı Ton (Odd Shade) bilerek askıda; sebepleri `yeni-oyunlar-plan-3.md`'de.
- Üç yeni oyun için `docs/oyun-reelleri-plan.md`'ye reel sahnesi **henüz eklenmedi**. Ses planına (`docs/ses-efektleri-plan.md`) Flaş Izgara'nın nonogram merdivenini kullandığı notu eklenebilir.
- Sunucu migration'ı `20261121090000_game_boards_all_games.sql` uzak veritabanına **uygulandı** (`npx supabase db push --remote`).
- Aşağıdakiler birikmiş, commit edilmemiş değişiklikler: yeni oyunlar, düzenleme ekranı yenilemesi (arama, tümünü seç, PNG resimler), sıralama boş durumu, ipucu yazı boyutları, Kod Kırıcı kaydırma düzeltmesi.
- Diğer bekleyenler hafıza dosyalarında: Pro Aşama B, yayın aşaması, Faz 10 adımları, zorluk dengelemesi.
