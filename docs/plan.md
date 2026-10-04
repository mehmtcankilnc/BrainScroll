# BrainScroll - Faz Planı

Kararlar için bkz. [decisions.md](decisions.md). Kutucukları ilerledikçe işaretle.
**TestFlight kapısı:** Her faz sonunda iOS CI yeşil olmalı, büyük özellik fazlarında (3, 5, 6, 7) ayrıca TestFlight'ta telefonda doğrulama yapılır.

---

## Faz 0 - Hesaplar ve hazırlık
Amaç: Kod yazmadan önce dış bağımlılıkları sıraya koymak (onay süreleri uzun olabilir).
- [ ] GitHub public repo, MIT lisansı, `README`, `.gitignore`
- [ ] Apple Developer: App ID `com.mehmtcan.brainscroll`, Sign in with Apple yeteneği, App Store Connect'te uygulama kaydı
- [ ] App Store Connect API anahtarı (CI imzalama/yükleme için) ve imzalama sertifikası (Mac'siz, `openssl` ile CSR)
- [ ] Google Cloud / Play Console: OAuth istemcileri (Android + iOS + web), Google ile giriş yapılandırması
- [ ] Supabase projesi oluştur (bölge seç), Auth sağlayıcıları aç (Anonymous, Google, Apple)
- [ ] `playbrainscroll.com`: basit açılış sayfası, gizlilik politikası, destek e-postası
- [ ] Kelime listesi kaynakları: TR ve EN için lisansı public repoya uygun listeleri belirle
**Çıkış:** Tüm hesaplar ve anahtarlar hazır, sırlar GitHub Secrets'a girmeye hazır.

## Faz 1 - İskelet ve CI
Amaç: Boş uygulama üç hedefte derlensin, iOS CI + TestFlight hattı çalışsın.
- [ ] KMP + Compose Multiplatform proje iskeleti (Android, iOS, Desktop), Gradle sürüm kataloğu
- [ ] Klasör yapısı, paket adı `com.mehmtcan.brainscroll`
- [ ] Temel tema, TR/EN string altyapısı (Compose resources)
- [ ] GitHub Actions: Android + Desktop derleme, birim testleri, lint
- [ ] GitHub Actions: iOS derleme (macOS runner), imzalama, TestFlight'a yükleme (fastlane veya `xcodebuild` + API anahtarı)
**Çıkış:** Telefonumda TestFlight'tan açılan "Merhaba BrainScroll" uygulaması.

## Faz 2 - Wordle çekirdeği (saf Kotlin)
Amaç: UI'dan bağımsız, test edilebilir oyun mantığı.
- [ ] Ortak `Game` arayüzü (yeni oyunlar eklemeye açık)
- [ ] Wordle motoru: tahmin değerlendirme (tekrarlı harf kuralları dahil), kazanma/kaybetme durumu
- [ ] TR harf kuralları (`ç ğ ı ö ş ü`, `İ/I` büyük-küçük harf tuzağı), EN kuralları
- [ ] Kelime listesi yükleme (geçerli tahmin sözlüğü + sonsuz akış için cevap havuzu)
- [ ] Birim testleri (kenar durumlar: çift harf, TR harfleri)
**Çıkış:** Testleri geçen, platformdan bağımsız Wordle motoru.

## Faz 3 - Arayüz ve kaydırma akışı
Amaç: İlk oynanabilir sürüm (sadece yerel).
- [ ] `VerticalPager` akışı, yalnızca mevcut + sonraki sayfa canlı
- [ ] Wordle arayüzü: ızgara, ekran klavyesi (TR/EN düzeni), harf çevirme animasyonları
- [ ] Sonsuz akış üreteci (yerel)
- [ ] Yarım kalan oyunun korunması (kaydırıp geri dönünce devam)
- [ ] Atlama hakkı (akış başına 1) ve doğru cevap serisi mantığı
- [ ] TR + EN yerelleştirme
**Çıkış:** Android/Desktop'ta tam oynanabilir akış. **TestFlight doğrulaması.**

## Faz 4 - Yerel veri
Amaç: Offline-first temeli.
- [ ] SQLDelight şeması: oyun sonuçları, yarım oyunlar, favoriler, ayarlar
- [ ] Seri ve istatistikleri kayıtlardan türetme (gün serisi, doğru cevap serisi, doğruluk, tamamlama)
- [ ] İstatistik ve favoriler ekranları
- [ ] Gün sınırı yardımcıları (Europe/Istanbul) + testler
- [ ] Seri dondurma (yerel kural taslağı)
**Çıkış:** Uygulama kapanıp açılınca her şey yerinde, istatistikler doğru.

## Faz 5 - Supabase, kimlik ve senkron
Amaç: Hesaplar ve outbox ile bulut yedek.
- [ ] Veritabanı şeması + RLS politikaları (kullanıcı yalnızca kendi verisini yazar/okur)
- [ ] Anonim oturum (ilk açılış), oturumun kalıcı saklanması
- [ ] Google ve Apple ile giriş / anonim hesabı bağlama
- [ ] Var olan hesaba bağlamada ilerleme birleştirme
- [ ] Outbox senkron: yerel kayıtlar -> Supabase, yeniden deneme, çevrimdışı dayanıklılık
- [ ] Yeni cihazda geri yükleme
**Çıkış:** İki cihazda aynı hesapla veri tutarlı. **TestFlight doğrulaması** (Apple girişi gerçek cihazda).

## Faz 6 - Günlük bulmaca (sunucu doğrulamalı)
Amaç: Adil, hile korumalı günlük bulmaca.
- [ ] Cevap tablosu (sadece sunucuda, istemciye RLS ile kapalı), günlük cevap zamanlaması
- [ ] Postgres RPC: `start_daily`, `submit_guess` (renk geri bildirimi), bitiş ve süre hesabı (sunucu saati)
- [ ] İstemci: günlük oyun akışı, isteği animasyonla maskeleme, `start` ön yükleme
- [ ] Online-only davranışı ve çevrimdışı mesajı
- [ ] Gün serisi sunucu tarafı (günlük sonuç kaydı), seri dondurma kesin kuralları
- [ ] Kötüye kullanım testleri (tekrar gönderim, saat oynama, eşzamanlı oturum)
**Çıkış:** Günlük bulmaca çalışıyor, süre ve skor sunucuda. **TestFlight doğrulaması.**

## Faz 7 - Lider tabloları ve arkadaşlar
Amaç: Rekabet.
- [ ] Kullanıcı adı seçimi (benzersiz, uygunsuz içerik filtresi)
- [ ] Günlük hız tablosu (az tahmin, eşitlikte kısa süre) - genel
- [ ] Seri tabloları (güncel + en uzun) - genel
- [ ] Arkadaş ekleme: davet kodu / bağlantı (deep link), arkadaş listesi
- [ ] Arkadaşlar arası tablolar
- [ ] Önbellek: son görüntü offline'da gösterilir
**Çıkış:** Arkadaşınla günlük bulmacada yarışabiliyorsun. **TestFlight doğrulaması.**

## Faz 8 - Cilalama ve yayın
- [ ] Haptik, animasyon cilası, erişilebilirlik (ekran okuyucu, büyük yazı)
- [ ] Çökme ve analitik raporlama (gizlilik uyumlu), hesap silme akışı (App Store zorunluluğu)
- [ ] Mağaza varlıkları: ikon, ekran görüntüleri, açıklamalar (TR/EN), gizlilik beyanı
- [ ] Supabase plan kararı (duraklatma riski), yedekleme
- [ ] App Store ve Google Play gönderimi
**Çıkış:** Mağazalarda yayında v1.0.

## Faz 9+ - Yeni oyunlar
Her oyun `Game` arayüzünü uygular: Sudoku, hafıza eşleştirme, satranç bulmacası, örüntü tanıma. Her biri için günlük + sonsuz mod kararı ayrıca alınır. Görsel tasarımlar [design.md](design.md) içinde tokenlarla tanımlıdır.

### Satranç bulmacası - teknik notlar

Dört oyun içinde teknik olarak en ağır olanı. Kendi alt fazı olarak (Faz 9c gibi) ele alınmalı.

**Bulmaca kaynağı**
- [ ] Lichess açık bulmaca veritabanı (CC0, CSV). Alanlar: `PuzzleId, FEN, Moves, Rating, RatingDeviation, Popularity, NbPlays, Themes, GameUrl, OpeningTags`. Lisans ve alan şemasını başlamadan resmi sayfadan doğrula.
- [ ] Veri formatı: `Moves` UCI notasyonunda ve **ilk hamle rakibindir**. `FEN` rakibin ilk hamlesinden önceki pozisyondur. Oyuncunun gördüğü pozisyon bu hamle uygulandıktan sonraki pozisyondur, çözüm ikinci hamleden başlar.
- [ ] Veritabanı milyonlarca bulmaca içerir, uygulamaya **seçilmiş bir alt küme** gömülür (puan aralığı ve temaya göre, ör. birkaç bin bulmaca). Yeni içerik Supabase üzerinden indirilebilir.
- [ ] Zorluk eşlemesi: bulmaca puanı -> Kolay/Orta/Zor bantları. Sonsuz akışta oyuncunun başarısına göre uyarlanabilir.

**Kural motoru (en büyük iş)**
- [ ] Hamle üretimi, hamle uygulama, şah çekme/mat/pat tespiti, rok, en passant, terfi, FEN okuma/yazma `commonMain` içinde yazılır.
- [ ] Doğrulanmış bir KMP satranç kütüphanesi bilinmiyor. JVM kütüphaneleri (`expect/actual` ile) iOS'ta çalışmaz. **Öneri: kendi motorumuzu yaz**, böylece her iki platformda aynı çalışır ve Kotlin öğrenmek için iyi bir alıştırma olur. Başlamadan önce güncel KMP seçeneklerini yeniden araştır.
- [ ] Doğrulama: **perft testleri** (bilinen pozisyonlarda hamle sayısı sabit olmalı) ve örnek bulmacaların tüm hamle dizisini oynatıp her adımın yasal, sonunun beklenen sonuç olduğunu kontrol eden testler.
- [ ] Sonuç kartındaki "Re8#" için SAN (cebirsel notasyon) üretimi gerekir: belirsizlik giderme ve şah/mat işaretleri dahil. İlk sürümde atlanıp yalnızca UCI ya da basit notasyon gösterilebilir.

**Çözüm doğrulama**
- [ ] Oyuncunun hamlesi bulmacanın çözüm hamlesiyle karşılaştırılır. Mat bulmacalarında Lichess gibi **alternatif matlar da doğru sayılır**. Çok hamleli bulmacada her doğru hamleden sonra rakibin karşılığı otomatik oynanır.
- [ ] Yanlış hamle: kısa uyarı animasyonu ve haptik, doğru cevap serisini bozar (karar: ilk yanlış bozar mı, bir deneme hakkı mı olur; Faz 9c başında netleştirilecek).
- [ ] İpucu: kaynak karesini vurgular. İpucu kullanılan çözüm sıralamaya girmez, seriyi bozmaz (öneri).

**Günlük bulmaca ve adalet**
- [ ] Lichess çözümleri **kamuya açık** olduğu için, günlük satranç bulmacasında cevabı gizlemek (Faz 6'daki gibi) hile koruması sağlamaz, ayrıca bir satranç motoru da bulmacayı çözer. **İlk sürümde satranç sıralamalı günlük bulmacaya girmez**, yalnızca sonsuz akışta ve istatistiklerde yer alır. Sıralamaya katma kararı ayrıca alınır.

**Arayüz**
- [ ] Taş seti: repoya (MIT) uygun, açık lisanslı bir set seç ve lisansı doğrula. Unicode satranç sembolleri platforma göre farklı çizildiği için kullanılmaz. Taşlar vektör (SVG/`ImageVector`) olarak gömülür.
- [ ] Tahta `Canvas` ile ya da 64 kareyi tek bir yerleşimde çizen bir bileşenle yapılır. Her hamlede yalnızca değişen kareler yeniden çizilsin.
- [ ] Etkileşim: dokun-dokun ve sürükle-bırak. Terfi seçimi için küçük bir seçici. Siyah oynuyorsa tahta çevrilir. Koordinat gösterimi ayarlardan açılıp kapanır.
- [ ] Kaydırma jesti çakışması: tahta üzerindeki sürükleme dikey sayfa kaydırmasını tetiklememeli (tahta bölgesi kaydırmayı yutar, oyun bitince serbest kalır).
- [ ] Erişilebilirlik: kare ve taş sesli okunur ("e1, beyaz kale"), hamle duyurulur.

**Test ve doğrulama**
- [ ] Kural motoru birim testleri `commonTest` içinde, Android/Desktop/iOS'ta aynı sonuçlar.
- [ ] Taş çizimi ve tahta çevirme iOS'ta TestFlight ile gözle doğrulanır.

---

## Riskler
| Risk | Önlem |
|---|---|
| Mac'siz iOS hata ayıklama zor | iOS CI ilk günden, her büyük özellikte TestFlight, günlük geliştirme Android/Desktop |
| macOS CI süresi/maliyeti | Public repo (ücretsiz), iOS işini yalnızca main ve etiketlerde çalıştır |
| Apple/Google giriş kurulumu uzun sürebilir | Hesap işleri Faz 0'da başlar |
| TR `İ/I` ve kelime listesi lisansı | Faz 2'de testli ele al, lisansı Faz 0'da doğrula |
| Supabase ücretsiz plan duraklaması | Yayından önce plan kararı (Faz 8) |
