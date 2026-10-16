# Yeni oyunlar planı

Durum: **Hanoi Kuleleri tamam** (cihazda denendi). **Vezirler kodlandı ve testleri geçti** (cihaz denemesi bekliyor). **Çadırlar kodlandı ve testleri geçti** (cihaz denemesi bekliyor). **Jigsaw Sudoku kodlandı ve testleri geçti** (cihaz denemesi bekliyor). **Blok Çıkışı kodlandı ve testleri geçti** (cihaz denemesi bekliyor). Beş oyunun beşi de kodlandı. **Çapraz bulmaca da kodlandı** (aşağıda "Çapraz bulmaca" bölümü; cihaz denemesi bekliyor).

Beş yeni oyun, sırayla ve birer birer: **Hanoi Kuleleri, Vezirler, Çadırlar, Jigsaw Sudoku, Blok Çıkışı**. Çapraz bulmaca bu planın dışında (aşağıda "Ertelenen").

## Ortak kararlar

- Hepsi **sonsuz akışta** yeni oyun olarak eklenir (Pipes ve Sudoku gibi). Günün bulmacası Tek Çizgi olarak kalır. Akış düzenleyicide açılıp kapanır, varsayılan açık.
- Ortak özellikler (mevcut oyunlardaki gibi): istatistik, favoriler, "Nasıl oynanır" (TR ve EN), "Bana yol göster" rehberi (kullanılırsa tur nötr sayılır), tahta ve eylemler cihazda kayıtlı, yarım kalan oyun geri gelir, süre = düşünme süresi (15 sn üst sınır), ana sayfa küçük resmi.
- Tek seviye (seviyeler sonraya). Sunucuya yeni bir şey gerekmez (sonsuz akış sonuçları zaten eşitleniyor, oyun kimlikleri serbest). `answer` en fazla 32, `guesses` en fazla 256 karakter olmalı.
- Her oyun için aşamalar: (1) kurallar, çözücü, üretici ve testleri, (2) tur, kayıt, `GameKind`, istatistik, rehber, (3) Compose sayfası ve UI testleri, (4) metinler (TR/EN), nasıl oynanır, küçük resim, profil satırı. Tam liste: yeni oyun için dokunulan yerler (`GameKind`, üretici, tur, `EndlessFeed` `when`'leri, `FeedActions` ve ViewModel'ler, `GamePage`, UI, `GameNames`/`GameThumbnails`/`HowToPlay`, `App.kt` ve `FeedViewModel` oyun listeleri, rehber dosyaları, `Stats.kt`/`ProfileScreen`, `strings.xml`).
- **Çalışma düzeni:** tek plan dosyası (bu). Bir oyun bitince durup özet veririm, sen cihazda denersin, "devam" dersen sonrakine geçerim. Commit'e dokunmam.
- İsimler ve kimlikler:

  | Oyun | TR | EN | Kimlik |
  | --- | --- | --- | --- |
  | 1 | Hanoi Kuleleri | Tower of Hanoi | `hanoi` |
  | 2 | Vezirler | Queens | `queens` |
  | 3 | Çadırlar | Tents | `tents` |
  | 4 | Jigsaw Sudoku | Jigsaw Sudoku | `jigsaw` |
  | 5 | Blok Çıkışı | Block Out | `blockout` |

## 1. Hanoi Kuleleri (TAMAM)

- 3 kule (A, B, C), **4 disk**. Disk sayısı parametre (seviyeler sonra).
- Büyük disk küçüğün üstüne konamaz. Oynanış: önce kaynak, sonra hedef kuleye dokun (Renk Tüpleri gibi). Kaybetmek yok. Geri al yok, sadece Sıfırla (hamleler sayılmaya devam eder).
- Her tahtada başlangıç dizilişi **rastgele** (her disk herhangi bir kulede, her kulede büyükten küçüğe sıralı) ve **hedef kule rastgele**. Hedef: tüm diskler hedef kulede.
- "En az N" = durum uzayı taramasıyla en kısa çözüm. En az 5'ten kısa tahtalar elenir.
- Rehber: en kısa çözümün sonraki hamlesi.
- Kayıt: başlangıç dizilişi ve hedef (kısa metin), eylemler `m<kaynak><hedef>` ve `r`. Sonuç `guesses` = hamle sayısı.

## 2. Vezirler (Queens) (KODLANDI, cihaz denemesi bekliyor)

- 5×5 tahta, klasik 5 vezir: hiçbir ikisi aynı satırda, sütunda veya çaprazda olamaz.
- **2 vezir baştan kilitli** gelir (üreticinin seçtiği bir çözümden), kalan 3'ünü oyuncu yerleştirir. Tek çözüm şartı yok (en az bir çözüm var).
- Dokunmak vezir koyar ya da kaldırır, saldıran vezirler kırmızı. Kaybetmek yok, Sıfırla var.
- Her dokunuş bir hamle. "En az 3" gösterilir. Süre ana puan, hamle ikinci.
- Çözüm: 5 vezir yerleşmiş ve çatışma yok.
- Tahta boyutu parametre (ileride 6×6, 7×7).
- Rehber: çözümden bir vezirin yeri (oyuncunun koyduğu yanlış bir vezir kalmışsa onu kaldırmasını söyler).
- Uygulama notları: kayıt `answer` = `5:3,12` (boyut:kilitli hücreler), eylemler `t<hücre>` ve `r`. Sayfa `ui/queens/QueensPage.kt`, tag'ler `queensCell`/`queensRestart`/`queensBoard`. Saldıran vezir kırmızı ve üstünde çarpı (renge bağımlı değil), kilitli vezirde küçük asma kilit. Üretici 10 çözüm × 10 kilit çifti = en çok 100 farklı tahta verir (5×5'in doğası); tahta boyutu büyüyünce çeşitlilik artar.

## 3. Çadırlar (Tents) (KODLANDI, cihaz denemesi bekliyor)

- 6×6, ağaçlar ve satır/sütun sayıları. Her ağacın yanına (dik komşu) bir çadır, her çadır tek bir ağaca ait, iki çadır birbirine değmez (çapraz dahil), sayılar tam.
- Dokunma: boş → çadır → çimen işareti (nokta) → boş. Çimen sadece oyuncunun notu, kontrole girmez.
- Kuralı ihlal eden yerler kırmızı (değen çadırlar, aşılan satır/sütun sayısı).
- **Benzersiz çözüm şart**, çözücüyle doğrulanır (Tek Çizgi gibi).
- İstatistik: sadece en iyi süre. Hamle sayısı yok.
- Rehber: mantıkla çıkarılabilen bir hücre.
- Uygulama notları: 6×6, 8 ağaç/çadır, üretici rastgele tahtayı çözücüyle doğrular (tam 1 çözüm). Kayıt `answer` = `6:<ağaç maskesi hex>:<çözüm çadırları hex>` (tahtayı tamamen kurar, favori yeniden oynatma da bundan; ≤32 karakter), eylemler `t<hücre>` (boş→çadır→çimen→boş) ve `r`. Kazanma kural kontrolüyle (sayılar, değmeme, ağaç-çadır eşleşmesi). Kırmızı: değen çadırlar, sayısı aşılan satır/sütundaki çadırlar, yanında ağaç olmayan çadır; kırmızı çadırda çarpı var. Sayı yanında tamamlanınca yeşil, aşılınca kırmızı. Üstte hamle yerine "Çadır 3/8" sayacı; istatistik ve favoride sadece süre. Rehber: yanlış çadır varsa önce onu kaldırtır, yoksa düz mantıkla çıkan (`TentsSolver.deduce`), o yoksa çözümden bir çadır gösterir.

## 4. Jigsaw Sudoku (KODLANDI, cihaz denemesi bekliyor)

- 9×9, 3×3 kutular yerine düzensiz, bağlı 9'luk bölgeler. Kurallar: satır, sütun ve bölgede 1-9 bir kez.
- Mevcut Sudoku'nun her şeyi yeniden kullanılır (kalem notları, 3 hata hakkı, yanlış rakam hata, doğru rakam kesin).
- Üretici: rastgele bağlı bölgeler, çözülmüş tahta, ipuçları **sadece tekil çözümle (naked/hidden single) çözülecek şekilde** bırakılır. İpucu sayısı tekil çözümle çözülebildiği sürece azaltılır, alt sınır 28. Tahtalar farklı zorlukta çıkar.
- Kayıt: bölgeler ve ipuçları tahtanın kendisi olarak (Pipes kalıbı), sunucu sınırı 32 karakter olduğu için `answer` kısa tutulur (tam tahta yalnızca cihazdaki yarım oyunda).
- Rehber: mevcut Sudoku rehberi (tekil çözüm adımı), bölgelere uyarlanır.
- Uygulama notları: Sudoku genellendi, ayrı oyun sınıfı yok: `SudokuLayout` (bölge haritası, satır/sütun/bölge birimleri, komşular), `SudokuVariant` (Classic/Jigsaw), `SudokuGame/Round/Snapshot/Page` ikisini de oynatır, `GameKind.Jigsaw` ayrı istatistik, favori ve akış kimliği verir; akıştaki sayı/not/sil eylemleri paylaşılır. Üretici `JigsawGenerator`: satırlardan başlayıp bağlantıyı bozmadan hücre takaslarıyla 9 bağlı bölge, geri izlemeli çözülmüş tahta, tekil çözümle çözülebildiği sürece ipucu azaltma (alt sınır 28; denemelerde hepsi 28'e indi). Kayıt `answer` = `9x9x28:<tohum>` (Sudoku gibi tohumdan yeniden üretilir; 32 karakter sınırı tam tahtayı kaydetmeye izin vermiyor, bu yüzden plandaki "tahtanın kendisi" yerine tohum). Bölgeler yalnızca kalın sınır çizgisiyle ayrılır, bölge rengi yok (temadaki renkler zaten doğru/seçili/girilen anlamı taşıyor; renkli denendi, kaldırıldı). Rehber cümleleri "bölge" der (`JigsawOnlyDigit/Place`).

## 5. Blok Çıkışı (Block Out) (KODLANDI, cihaz denemesi bekliyor)

- 5×5 tahta, farklı renk ve uzunlukta parçalar (Rush Hour benzeri). Parçalar **sadece uzun eksenleri boyunca** kayar, üst üste binemez.
- Üzerinde sağa bakan çift ok (››) olan turuncu **hedef parça** tahtanın ortasındaki satırdadır (ilk planda "x" işaretiydi; ok hem çıkışı gösterdiği hem de yasak gibi okunmadığı için değişti). Diğer parçalar canlı renklerde (mavi, yeşil, mor, turkuaz, pembe), kenarlıksız düz dolgu; sarı kenarlık yalnızca dokunulup seçilmiş parçada. Çıkış sağ kenarda çizilir; hedef parça çıkışa ulaşınca çözülür. Kaybetmek yok, Sıfırla var.
- Parmakla **sürükleyerek** kaydırılır (dokunup hücreye basmak da çalışır). Birden fazla kare kaydırmak **tek hamle**. Ekran okuyucu için parçaya "yukarı/aşağı/sol/sağ kaydır" eylemleri.
- Üretici: rastgele parçalar, tüm durum uzayı taranarak en kısa çözüm ("en az N") bulunur, **en az 8 hamle** gerektirmeyen tahtalar elenir. Her tahta çözülebilir.
- Rehber: en kısa çözümün sonraki hamlesi.
- Uygulama notları: parçalar 2-3 hücre, 6-10 diğer parça. Kayıt `answer` = 25 karakterlik hücre metni (`.` boş, `X` hedef, `A`.. diğer parçalar; parçalar ilk hücrelerinin okuma sırasına göre numaralanır), eylemler `m<parça>:<yer>` (yer = yatık parçada sütun, dik parçada satır) ve `r`. Üretici rastgele tahtayı tırmanma (küçük değişikliklerle zorlaştırma) ile en az 8 hamleye çıkarır; çözücü bit maskeli BFS (tahta başına ~8 ms). Sayfa: sürükleme (`detectHorizontal/VerticalDragGestures`, bırakınca en yakın hücreye) ve dokunup hücreye dokunma; ekran okuyucu için parçada iki özel eylem (bir hücre kaydır). Rehber: ilk hamlenin parçasını halkalar ve yön/hücre sayısını söyler.

### Hanoi'den kalan notlar (sonraki oyunlar için)

- Şablon dosyalar: `game/hanoi/` (oyun, çözücü, üretici, tur), `ui/hanoi/HanoiPage.kt`, rehber `HanoiGuide` (`BoardGuides.kt`), testler `game/hanoi/*Test`, `HanoiStatsTest`, `HanoiPersistenceTest`, `HanoiUiTest`. Yeni oyunda dokunulan yerlerin listesi `hanoi_wire.py` benzeri bir betikle yapıldı: `GameKind`, `FeedRound.moveCount`, `Guide.kt`, `BoardGuides.kt`, `GuideText.kt`, `EndlessFeed`, `FeedActions`, `FeedViewModel`, `DailyGameViewModel`, `Stats.kt`, `GamePage`, `GameNames`, `GameThumbnails`, `HowToPlay`, `ProfileScreen`, `App.kt`, `strings.xml` (TR/EN).
- Hanoi sırasında eklenen ortak altyapı (sonraki oyunlar da kullanır): `rememberSettledStatus` ve `WinSignal` (`ui/motion/WinSignal.kt`). Her yeni sayfa `OutcomeHaptics(status, page.id)` çağırmalı ve son hamlesinin animasyon süresi kadar bekleme vermeli. Konfeti artık sayfa kazanmayı gösterene kadar bekliyor.
- Kontrol listesi (hafızada: `feedback-new-game-checklist`): animasyonun ilk karesi, kazanma efektlerinin beklemesi, titreşim, erişilebilirlik (`selected`, açıklama, canlı bölge), küçük ekran ve 1,5× yazı, hareketi azalt, hem Türkçe hem İngilizce test.
- Hanoi'de seçilen sınırlar: tahta bölgesi `HanoiPage`te disk yüksekliği 28 dp, kule yüksekliği disk sayısından hesaplanıyor; hedef kule yeşil ton ve bayrakla, seçili kule sarı kalın kenarlıkla ayrılıyor.

## 6. Çapraz bulmaca (16×16'ya GENİŞLETİLDİ, cihaz denemesi bekliyor)

İlk sürüm 5×5'ti; 5×5 çok basit kaldı ve satır/sütunlarda anlamsız harf dizileri çıkabiliyordu (ör. "ÖKAPA"). Karar: **16×16 oklu bulmaca**, her kare ya ipucu ya da anlamlı bir kelimenin harfi, **hiçbir satır veya sütunda kelime olmayan harf dizisi yok**. Kimlik `crossword`, adlar Çapraz Bulmaca / Crossword, TR ve EN, harfler Wordle'ın ekran klavesiyle girilir.

- **Kurallar (hem üretici hem testler denetler):** iki ya da daha çok harflik her yatay/dikey dizi bankadan bir kelimedir ve tahtada bir kelime iki kez geçmez; her dizinin ilk harfinin yanında bir ipucu karesi vardır: düz ok (yataysa solunda, dikeyse üstünde) ya da bir kez dönen ok (yataysa üstünde, dikeyse solunda; kenardaki cevaplar için); her harf karesi en az bir dizide; her ipucu karesi 1 ya da 2 ipucu taşır; 2×2 ipucu bloğu yok, 2×2 harf bloğu en çok 6. Yanlış harf hata sayılır ama devam edilir, doğru harf kesindir, kaybetmek yok.
- **İçerik:** `tools/crossword/bank/*.txt` (kaynak, her satır `KELİME|ipucu`), TR ~1970 ve EN ~2260 kelime, 2-8 harf, ipucu en çok 30 karakter. `python tools/crossword/bank_check.py` biçimi, tekrarı ve ipucunun cevabı ele vermesini denetler, `bank_dedupe.py` tekrarları atar. `java tools/crossword/Gen.java bank` dosyaları `CrosswordBankData.kt`ye çevirir (elle düzenlenmez). Bir kelime tahtada geçiyorsa bankadan çıkarılmamalı (ipucu kelimeden bulunur; `CrosswordPuzzle` bulamazsa tahtayı açmaz).
- **Üretici (çevrimdışı):** 16×16'yı telefonda aramak ağır, bu yüzden tahtalar önceden üretilir: `java tools/crossword/Gen.java make TR 120 600` (dil, kaç tahta, en çok kaç saniye), `check` (hepsini bankaya karşı doğrular), `write` (`CrosswordBoards.kt`yi yazar, dil başına en çok 120 tahta). Yerleşimi benzetilmiş tavlama çizer (hata sayısı: kapsanmayan harf, boşta ipucu karesi, ok dağıtılamayan dizi, blok, uzun/çok kısa dizi), sonra diziler kısıt yayılımlı bir arama ile bankadan doldurulur; aynı yerleşim birkaç kez farklı kelimelerle doldurulur. Ara çıktı `tools/crossword/out/TR.txt` ve `EN.txt`de durur, `make` eklemeye devam eder. Yeni tahta eklemek için `make` + `write`; mevcut tahtaların sırası değişmemeli.
- **Ok dağıtımı:** `CrosswordLayout.slots` (Kotlin) Gen.java ile aynı eşleştirmeyi yapar (önce her ipucu karesine kendine ait bir dizi, sonra kalanlar artırıcı yollarla); aynı yerleşim hep aynı sonucu verir.
- **Kayıt:** `answer` = `b<numara>` (hazır tahta listesindeki sıra; dil kayıtta ayrı). Üretici tohumu yalnızca bir tahta seçer (`CrosswordGenerator.generate(seed, dil)`). Eylemler `p<kare>:<harf>`, `e<kare>`.
- **Dil:** akışta Kelime ile aynı dil seçimini kullanır; iki dil açıksa her bulmaca rastgele birini alır.
- **Sayfa (yakınlaştırma):** tahta uzak görünümle açılır (16×16 hepsi ekranda, ipucu yazıları okunmaz) ve altta "Yakınlaştırmak için tahtaya çift dokun" yazar. Çift dokunuş oraya sabit boyutta (kare 56 dp, ipucu yazıları okunur) yakınlaştırır ve dokunulan kareyi seçer; yakınken tek parmakla tahta sürüklenir (sınırlar tahtayla kısıtlı), tek dokunuş kare seçer (aynı kareye ikinci dokunuş yatay/dikey değiştirir), klavye ve seçili cevabın ipucu şeridi ancak yakınken görünür. Çift dokunuş ya da köşedeki "−" düğmesi uzaklaştırır; çözülünce tahta kendiliğinden uzaklaşıp bütününü gösterir. İmleç cevap boyunca ilerlerken görünüm onu izler. Ekran okuyucu bir kareye basınca tahta o kareye yakınlaşır. Yakınlaştırma hareketi azaltılmışsa anında olur.
- **Ok çizimi:** ok, cevabın ilk harfine yakın kenarda durur ve cevabın yönünü gösterir (sağ kenar: cevap sağdaki kareden başlar, alt kenar: alttaki kareden başlar). Bir kare iki ipucu taşıyorsa üst yarı ve alt yarı olarak bölünür.
- **Rehber:** önce yanlış harfi geri aldırır, yoksa ilk boş karenin harfini söyler; rehber bir kareyi gösterince tahta ona yakınlaşır.
- **Bilinen sınırlar:** tahta yalnızca üretilmiş 120+120 tahtadan seçilir (tohum başına sabit); çok tahta gerekirse `make` ile çoğaltılır. Yerleşimlerde 2-3 harfli kelimeler çoktur (banka büyüdükçe `caps` ile azaltılabilir). Cihaz denemesi (parmakla sürükleme, çift dokunuşun hissi, küçük ekran) yapılmadı; bilgisayarda Compose testleri geçti.

### Günlük bulmaca olarak Çapraz Bulmaca (2026-10-16)

Günlük bulmaca artık her gün **Çapraz Bulmaca** (`dailyGame` varsayılanı `GameKind.Crossword`); **Tek Çizgi akışa taşındı** (`App.kt` ve `FeedViewModel` oyun listesinde `GameKind.Line` var, `GameKind.Crossword` yok). Çapraz Bulmaca feed'de çıkmaz; günlük akış `EndlessFeed` üstünde çalıştığı için oyun türü olarak kalır.

- **Günün tahtası:** `CrosswordSchedule.puzzleFor(gün, dil)`: hazır tahta listesinden gün numarasına göre, listenin her tahtasını bir kez dolaşan (adım listeyle aralarında asal) dağınık sırayla seçilir; aynı dil ve gün herkese aynı tahta. Dil, uygulamanın çözülmüş dili (`resolvedAppLanguage`). Tahta listesi büyürse sıra kayar (kayıtlı sonuçlar bozulmaz, çünkü sonucun `answer`ı tahtanın numarasıdır).
- **Yıldız:** hata sayısına göre. En çok 3 hata = 3 yıldız, 4-10 hata = 2, 11 ve fazlası = 1. Hata = yazılan her yanlış harf (geri alınsa da). Günlük sayfada rehber/ipucu düğmesi yok. Süre yıldızı etkilemez, aynı yıldızdaki oyuncuları sıralar. Sabitler `CrosswordScoring` içinde (`THREE_STARS_UP_TO`, `TWO_STARS_UP_TO`); değiştirirsen SQL'deki `crossword_stars` da değişmeli.
- **Sıralama:** önce yıldız (çok olan iyi), sonra süre. Sunucu tablosu `crossword`; satırın `moves` alanı yıldız sayısı. Skor kodu = 1 + hata (en çok 999); uygulama bunu `moves` olarak gönderir, sunucu yıldızı aynı kuralla hesaplar (`crossword_stars`). Bulut eşitlemesi ve `submit_daily_game_result` aynı kimliği (`daily-<gün>-crossword`) yazdığı için günlük sonucun tek tahmini skor kodudur (`DailyGameViewModel.buildFeed` çevirir). Sıralama dile göre ayrılmaz: TR ve EN oyuncular aynı tabloda yarışır (tahtalar farklı).
- **Sunucu:** `supabase/migrations/20261016090000_crossword_daily.sql` (testleri `supabase/tests/verify_daily_games.mjs`, `npm test` yeşil). Henüz projeye GÖNDERİLMEDİ; Tek Çizgi migration'ından (`20261014090000_line_daily.sql`) sonra gönderilmeli.
- **Arayüz:** günlük sayfada (`LocalDailyPage`) üstte canlı yıldız çubuğu ve "i" bilgi sayfası (`StarsBar`, `StarsInfoSheet`, genel bileşenler `ui/components/StarsBar.kt`); sonuç kartında yıldızlar ve Paylaş. Paylaşım: `Çapraz Bulmaca #N ⭐⭐⭐ 12:48 🔥7` (`CrosswordShare`), numara `CrosswordSchedule.LAUNCH_DAY_INDEX`'ten sayılır (şimdilik yer tutucu 20740: yayın günü girilecek). Ana sayfa kartı çözülünce yıldız, süre ve sonraki bulmacaya kalan süreyi gösterir (`CrosswordDoneInfo`); karta dokununca çözülmüş tahta açılır.
- **İstatistik:** Çapraz Bulmaca artık yalnızca günlük sonuçlardan: oynanan, en iyi süre, üç yıldızlı tahta (`computeCrosswordStats`). Tek Çizgi feed oyunu olarak diğerleri gibi: oynanan, en iyi süre, en az hamle (ipucu kullanılan tahta sayılmaz).
- **Tek Çizgi (feed):** sayfada yıldız çubuğu, paylaşım ve yıldız yok; iki sayaç: Hata (geri alınan kare: geri al düğmesi, temizleme, eski kareye dokunma) ve Hamle (çizgiye konan kare, geri alınanlar tekrar konunca yine sayılır). Sonucun tek tahmini hamle sayısı. İpucu düğmesi durur; ipucu kullanılan tahta `assisted` olur ve istatistiğe girmez. Feed tahtası 5×5.
- **Bilinen sınırlar:** daha önce kendi oyun listesini kaydetmiş oyuncularda Tek Çizgi otomatik açık gelmez (kayıtlı liste süzülüyor); düzenleyiciden açabilirler. Günlük sıralama dillere bölünmedi.

## Açık noktalar

- Yok (Çapraz bulmaca hariç).
