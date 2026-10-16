# Tek Çizgi: günün bulmacası planı

> **GÜNCELLEME (2026-10-16): Bu plan artık geçerli değil.** Tek Çizgi günlük bulmaca olmaktan çıktı ve **feed oyunu** oldu; günlük bulmaca artık **Çapraz Bulmaca**. Tek Çizgi'nin yıldız mantığı, günlük takvim (`LineSchedule`), paylaşım metni ve sunucu skor kodu kaldırıldı: feed'de diğer oyunlar gibi yalnızca **hata** (geri alınan kareler) ve **hamle** (çizgiye konan kare sayısı) tutulur, sonuçta tek tahmin hamle sayısıdır, ipucu kullanılan tahta istatistikten dışlanır. Günlük bulmacanın yıldız/süre/sıralama mantığı için `docs/yeni-oyunlar-plan.md` bölüm 6'ya bakın (`CrosswordScoring`, `CrosswordSchedule`, `supabase/migrations/20261016090000_crossword_daily.sql`). Aşağıdaki metin tarihsel kayıt olarak duruyor; Tek Çizgi'nin kuralları, çözücüsü ve üreticisi geçerli.


Durum: Aşama 1-5 tamam, sunucu ve cihaz testleri yapıldı.

**Yayına çıkmadan önce:** `LineSchedule.LAUNCH_DAY_INDEX` (`LineSchedule.kt`) yayın gününün gün numarasına ayarlanmalı (şu an yer tutucu: 20740). Paylaşılan "Tek Çizgi #N" numarası buradan sayılıyor.

## Amaç

Günün bulmacası şu an 9 oyunu sırayla döndürüyor (`DailyRotation.kt`). Bunun yerine tek, orijinal ve alışkanlık yapan bir günlük bulmaca: **Tek Çizgi**. Izgarada numaralı noktalar var; 1'den başlayıp sırayla geçen tek bir çizgiyle **tüm kareleri** doldurup son numarada bitirirsin.

## Kararlar

### Ürün
- Tek Çizgi tek günlük bulmaca. Eski rotasyon kalkar; diğer oyunlar yalnızca feed'de oynanır.
- Tek Çizgi feed'de çıkmaz. `GameKind` olarak eklenir (günlük akış `EndlessFeed` üstünden çalıştığı için şart) ama feed oyun listesine (`App.kt` varsayılan `games`) girmez.
- Kelime günlüğünün sunucu doğrulamalı kodu (`DailyScreen`, `DailyViewModel`, `SupabaseDailyApi`) dokunulmadan kalır. Temizlik ayrı iş.
- Zorluk takvimi (haftanın günü, Europe/Istanbul gün sınırı `IstanbulDay`):

  | Gün | Izgara | Başlangıç nokta sayısı |
  | --- | --- | --- |
  | Pzt-Çar | 5×5 | 4 |
  | Per-Cum | 6×6 | 5 |
  | Cmt-Paz | 7×7 | 6 |

  Duvar yok (sonraki sürüm).
- Nokta sayısı: duvarsız tahtada kenar uzunluğu kadar numarayla benzersiz çözüm neredeyse hiç çıkmıyor (ölçüldü: 5×5'te 5-11, 6×6'da 10-29, 7×7'de 15-31). Bu yüzden üretici benzersiz olana kadar numara ekler, sonra gereksiz olanları geri alır. Sonuç: 5×5'te ~6, 6×6'da ~8, 7×7'de ~10 numara. Takvimdeki 4/5/6 sadece başlangıç değeri.
- Çözüm **her zaman benzersiz**.
- Giriş: parmakla sürükleme. Ek olarak çizginin ucuna bitişik kareye dokunmak çizgiyi uzatır. Çizgiyi kendi üzerinden geri sürüklemek ya da çizginin bir karesine dokunmak çizgiyi o noktaya kısaltır.
- Kural: numaralar sırayla geçilir; son numara ancak 25. (tüm kareler dolduğunda) kare olarak girilebilir. Çözüm: tüm kareler dolu ve çizgi son numarada biter.
- Puan:
  - Yıldız geri alma sayısına göre: 0 geri alma = 3, 1-3 = 2, fazlası = 1.
  - İpucu kullanıldıysa yıldız bir düşer (3→2, 1→0, alt sınır 0). Kaç ipucu kullanıldığı fark etmez.
  - İpucuyla çözülen tur günlük seriyi yine artırır.
- Sınırsız deneme (sıfırla serbest), süre eylem araları toplamı (boşta kalma aralığı başına üst sınırlı; mevcut turlarla aynı). Arşiv yok, yalnızca bugünün bulmacası.
- İpucu: çözümden sonraki 3 hücreyi gösterir. Mevcut kılavuz sistemine (`Guide.kt`) EKLENMEDİ: o sistem turu "nötr" yapıyor (seri/istatistik yok), oysa ipucuyla çözülen tur seriyi artırıyor. İpucu oyunun kendi düğmesi (`LineRound.showHint`).
- Paylaşım: metin, panoya kopyalanır. `Tek Çizgi #N ⭐⭐⭐ 0:48 🔥7`. N = `LAUNCH_DAY_INDEX`'ten bu yana geçen gün + 1. Görsel paylaşım yok.
- Çözülen günde ana sayfadaki günlük kart yıldız, süre ve sonraki bulmacaya kalan süreyi (`millisUntilNextDay`) gösterir. Karta dokununca çözülmüş tahta ve Paylaş düğmesi açılır, yeniden oynanamaz.
- İsimler: TR "Tek Çizgi", EN "One Line". Nasıl oynanır, kılavuz ve `strings.xml` metinleri iki dilde.

### Teknik
- Bulmaca tarihten (gün numarası) türetilen seed ile üretilir. Kendi sabit algoritmamız (SplitMix64), `kotlin.random.Random`'a bağımlı değil.
- Üretici: rastgele Hamilton yolu çiz, yol üzerine numaraları yerleştir, çözücüyle benzersizliği doğrula; değilse nokta ekle / yeniden dene.
- Kayıt biçimi Pipes/Sudoku kalıbıyla: tahtanın kendisi kaydedilir (boyut ve numara hücreleri, örn. `7:3,10,25`; çözüm kaydedilmez, açılışta çözücüyle yeniden bulunur) ve eylem günlüğü (`d12` sürükle, `t12` dokun, `u` geri al, `r` sıfırla, `h` ipucu). Biten sonucun `answer`'ı sadece boyut (`7x7`, sunucu sınırı 32 karakter), `guesses` = [geri alma, ipucu]. Üretici sonradan değişse de eski sonuçlar bozulmaz.
- Seri (`DayStreaks.compute`) olduğu gibi kalır; eski rotasyonun günlük sonuçları seride sayılmaya devam eder. Tek Çizgi sonuçları `Mode.DAILY` ile kaydedilir.
- Sonuç mevcut `submitGameResult` hattıyla sunucuya gider. Supabase'e yeni oyun kimliği için migration yazılır (`answer`/`guesses` sınırlarına dikkat). Günlük sıralama: önce yıldız, sonra süre.
- Günlük kimliği `daily-<gün>-<oyun id>` (mevcut `dailyGameId`).

## Aşamalar

Her aşama testler yeşil olunca biter. Commit kararı kullanıcıda.

1. **Kurallar, çözücü, üretici. (TAMAM)** `game/line/` altında `LineGame`, `LineSolver`, `LineGenerator`. Testler: 365 gün × tüm boyutlar için benzersiz çözüm, aynı seed aynı bulmaca, geçersiz hamleler, geri alma, sıra kuralı.
2. **Tur ve entegrasyon. (TAMAM)** `LineRound` (`FeedRound`), `Snapshot`, eylem günlüğü, `GameKind.Line` + `moveCount`, `EndlessFeed` `when`'leri (feed listesine ekleme yok), `FeedActions`/ViewModel eylemleri, `Stats.kt` `computeLineStats`, kılavuz, kalıcılık testi.
3. **Compose sayfası. (TAMAM)** `ui/line/LinePage.kt`: ızgara çizimi, sürükleme ve bitişik dokunma, sayfa kaydırmasıyla çakışma yönetimi (`WordleKeyboard` kalıbı), çözüm animasyonu/haptik. UI testleri (başsız Compose, `UiTestSupport`).
4. **Günlük geçişi. (TAMAM)** `DailyRotation` yerine Tek Çizgi, ana sayfa kartı, tamamlandı ekranı, `GamePage` eşlemesi, `GameNames`/`GameThumbnails`/`HowToPlay`.
5. **Sunucu ve paylaşım. (TAMAM)** Supabase migration ve doğrulama testi, sıralama kuralı, paylaşım metni (panoya), metinler.

Yeni oyun için dokunulacak yerlerin tam listesi (13 madde) aşama 2-5 içinde ele alınır: `GameKind`, üretici, tur, `EndlessFeed` `when`'leri, `FeedActions`/ViewModel'lar, `GamePage`, UI, `GameNames`/`GameThumbnails`/`HowToPlay`, `App.kt`, kılavuz dosyaları, `Stats.kt`/`ProfileScreen`, `strings.xml`, sunucu sınırları.

## Açık noktalar

- İpucuyla çözülen turların istatistikte nötr sayılması (diğer oyunlardaki "rehberli tur" kuralı). Karar: ipucuyla çözülen tur nötr SAYILMAZ, istatistiğe ve sıralamaya girer (yıldız zaten 1 düşüyor). => zaten ipucu ile çözülürse yıldızı 1 düşürelim dedik. o yüzden sıralamaya girsin.
- `LAUNCH_DAY_INDEX` değeri yayın günü belirlenince girilecek. => evet
- Duvarlar ve görsel paylaşım sonraki sürüm. => olur

## Uygulama notları

- Günün tahtası arka planda üretiliyor (`DailyGameViewModel.makeLine`), 7×7 telefonda saniyenin bir kısmını alabilir. Başlamış tahta kayıttan okunur, üretim gerekmez. Üretici yayından sonra DEĞİŞTİRİLMEMELİ: çözülmüş tahta, günün tahtasından yeniden çizildiği için eski günlerin görüntüsü değişir.
- Eski rotasyon (`DailyRotation`) silindi, `dailyGameId` `DailyGameId.kt`'de. Kelime günlüğü kodu duruyor ama hiçbir gün seçilmiyor.
- Seri hesabı değişmedi: eski günlük sonuçlar seride sayılıyor.
- Sonucun tek tahmini bir **skor kodu**: `1 + geri alma (en çok 99) + 100 × ipucu (en çok 9)` (`LineScoring.encode`). Sunucuya `moves` olarak bu gidiyor ve sunucu yıldızı aynı kuralla hesaplıyor (`line_stars`). `line` tablosu önce yıldıza, sonra süreye göre sıralanıyor ve satırın `moves` alanı yıldız sayısı. Bunu seçme nedeni: sunucudaki mevcut hat tek tamsayı bekliyor, bulut eşitlemesi (`guesses`) ile `submit_daily_game_result` aynı kimliği (`daily-<gün>-line`) yazıyor ve ikisinin biçimi aynı olmak zorunda.
- Sunucu migration'ı: `supabase/migrations/20261014090000_line_daily.sql` (testleri `supabase/tests/verify_daily_games.mjs` içinde, `npm test` yeşil). Henüz projeye GÖNDERİLMEDİ.
- Paylaşım: `LineShare.text`, panoya kopyalanır. Numara `LineSchedule.LAUNCH_DAY_INDEX`'ten sayılır (şimdilik yer tutucu 20740).
