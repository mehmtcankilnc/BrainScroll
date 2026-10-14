# Yeni oyunlar planı

Durum: **Hanoi Kuleleri tamam** (kod, testler ve cihaz denemesi bitti). Sıradaki: **Vezirler** (yeni oturumda).

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

## 2. Vezirler (Queens)

- 5×5 tahta, klasik 5 vezir: hiçbir ikisi aynı satırda, sütunda veya çaprazda olamaz.
- **2 vezir baştan kilitli** gelir (üreticinin seçtiği bir çözümden), kalan 3'ünü oyuncu yerleştirir. Tek çözüm şartı yok (en az bir çözüm var).
- Dokunmak vezir koyar ya da kaldırır, saldıran vezirler kırmızı. Kaybetmek yok, Sıfırla var.
- Her dokunuş bir hamle. "En az 3" gösterilir. Süre ana puan, hamle ikinci.
- Çözüm: 5 vezir yerleşmiş ve çatışma yok.
- Tahta boyutu parametre (ileride 6×6, 7×7).
- Rehber: çözümden bir vezirin yeri.

## 3. Çadırlar (Tents)

- 6×6, ağaçlar ve satır/sütun sayıları. Her ağacın yanına (dik komşu) bir çadır, her çadır tek bir ağaca ait, iki çadır birbirine değmez (çapraz dahil), sayılar tam.
- Dokunma: boş → çadır → çimen işareti (nokta) → boş. Çimen sadece oyuncunun notu, kontrole girmez.
- Kuralı ihlal eden yerler kırmızı (değen çadırlar, aşılan satır/sütun sayısı).
- **Benzersiz çözüm şart**, çözücüyle doğrulanır (Tek Çizgi gibi).
- İstatistik: sadece en iyi süre. Hamle sayısı yok.
- Rehber: mantıkla çıkarılabilen bir hücre.

## 4. Jigsaw Sudoku

- 9×9, 3×3 kutular yerine düzensiz, bağlı 9'luk bölgeler. Kurallar: satır, sütun ve bölgede 1-9 bir kez.
- Mevcut Sudoku'nun her şeyi yeniden kullanılır (kalem notları, 3 hata hakkı, yanlış rakam hata, doğru rakam kesin).
- Üretici: rastgele bağlı bölgeler, çözülmüş tahta, ipuçları **sadece tekil çözümle (naked/hidden single) çözülecek şekilde** bırakılır. İpucu sayısı tekil çözümle çözülebildiği sürece azaltılır, alt sınır 28. Tahtalar farklı zorlukta çıkar.
- Kayıt: bölgeler ve ipuçları tahtanın kendisi olarak (Pipes kalıbı), sunucu sınırı 32 karakter olduğu için `answer` kısa tutulur (tam tahta yalnızca cihazdaki yarım oyunda).
- Rehber: mevcut Sudoku rehberi (tekil çözüm adımı), bölgelere uyarlanır.

## 5. Blok Çıkışı (Block Out)

- 5×5 tahta, farklı renk ve uzunlukta parçalar (Rush Hour benzeri). Parçalar **sadece uzun eksenleri boyunca** kayar, üst üste binemez.
- Turuncu "x" işaretli **hedef parça** tahtanın ortasındaki satırdadır. Çıkış sağ kenarda çizilir; hedef parça çıkışa ulaşınca çözülür. Kaybetmek yok, Sıfırla var.
- Parmakla **sürükleyerek** kaydırılır (dokunup hücreye basmak da çalışır). Birden fazla kare kaydırmak **tek hamle**. Ekran okuyucu için parçaya "yukarı/aşağı/sol/sağ kaydır" eylemleri.
- Üretici: rastgele parçalar, tüm durum uzayı taranarak en kısa çözüm ("en az N") bulunur, **en az 8 hamle** gerektirmeyen tahtalar elenir. Her tahta çözülebilir.
- Rehber: en kısa çözümün sonraki hamlesi.

### Hanoi'den kalan notlar (sonraki oyunlar için)

- Şablon dosyalar: `game/hanoi/` (oyun, çözücü, üretici, tur), `ui/hanoi/HanoiPage.kt`, rehber `HanoiGuide` (`BoardGuides.kt`), testler `game/hanoi/*Test`, `HanoiStatsTest`, `HanoiPersistenceTest`, `HanoiUiTest`. Yeni oyunda dokunulan yerlerin listesi `hanoi_wire.py` benzeri bir betikle yapıldı: `GameKind`, `FeedRound.moveCount`, `Guide.kt`, `BoardGuides.kt`, `GuideText.kt`, `EndlessFeed`, `FeedActions`, `FeedViewModel`, `DailyGameViewModel`, `Stats.kt`, `GamePage`, `GameNames`, `GameThumbnails`, `HowToPlay`, `ProfileScreen`, `App.kt`, `strings.xml` (TR/EN).
- Hanoi sırasında eklenen ortak altyapı (sonraki oyunlar da kullanır): `rememberSettledStatus` ve `WinSignal` (`ui/motion/WinSignal.kt`). Her yeni sayfa `OutcomeHaptics(status, page.id)` çağırmalı ve son hamlesinin animasyon süresi kadar bekleme vermeli. Konfeti artık sayfa kazanmayı gösterene kadar bekliyor.
- Kontrol listesi (hafızada: `feedback-new-game-checklist`): animasyonun ilk karesi, kazanma efektlerinin beklemesi, titreşim, erişilebilirlik (`selected`, açıklama, canlı bölge), küçük ekran ve 1,5× yazı, hareketi azalt, hem Türkçe hem İngilizce test.
- Hanoi'de seçilen sınırlar: tahta bölgesi `HanoiPage`te disk yüksekliği 28 dp, kule yüksekliği disk sayısından hesaplanıyor; hedef kule yeşil ton ve bayrakla, seçili kule sarı kalın kenarlıkla ayrılıyor.

## Ertelenen: Çapraz bulmaca

İçerik gerektiriyor (ipucu metinleri ve cevaplar, telif ve kalite). Önce diğer beş oyun, sonra ayrıca konuşulacak. Seçenekler o zaman: küçük bir bulmaca bankası (TR/EN, kısa ipuçlarıyla) yazılıp üreticiyle ok-ipuçlu 5×5 veya 6×6 tahta kurmak.

## Açık noktalar

- Yok (Çapraz bulmaca hariç).
