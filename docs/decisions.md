# BrainScroll - Karar Belgesi

Son güncelleme: 2026-10-05

## Ürün
- Ad: **BrainScroll** | Alan adı: `playbrainscroll.com` | Paket kimliği: `com.mehmtcan.brainscroll`
- Konsept: TikTok/Instagram tarzı dikey kaydırmalı akışta hızlı zeka oyunları.
- İlk oyun: **Wordle tarzı** (5 harf). Sonraki oyunlar: Sudoku, hafıza eşleştirme, örüntü tanıma.
- Diller: arayüz ve kelime listesi **TR + EN** ile başlar, çoklu dile açık tasarlanır.

## Teknoloji
- Kotlin Multiplatform + **Compose Multiplatform** (tüm arayüz paylaşımlı). Hedefler: Android, iOS, Desktop (JVM, sadece geliştirme).
- Günlük geliştirme Android emülatörü + Desktop üzerinde yapılır (Windows). iOS derlemesi **GitHub Actions macOS runner** ile.
- iOS CI **ilk günden** kurulur. Her büyük özellikten sonra **TestFlight'ta** doğrulanır.
- Yerel veri: SQLDelight (offline-first, gerçeğin kaynağı). DI: Koin. Ağ: Supabase Kotlin SDK (`supabase-kt`) / Ktor.
- Varsayılanlar: tek modül ile başla (erken çoklu modül yok), KMP ViewModel, Compose Navigation, min Android 8 / iOS 15.
- Repo: **public**, lisans **MIT**. Kelime listeleri yalnızca public repoda yayınlanabilir lisanslı kaynaklardan alınır.

## Backend: Supabase
- **Offline-first:** Kendi outbox'ımız. Yerel DB kaynak, kuyruk internet gelince Supabase'e gönderilir. PowerSync kullanılmaz.
- Veri çoğunlukla eklemeli (oyun sonuçları); seri/istatistik bu kayıtlardan türetilir. Favoriler: son yazan kazanır.
- **Favori = bitmiş bulmaca** (sonuç kartındaki kalp). Favoriler Profil sekmesinde listelenir (2026-10-05). Silmek yerine `is_favorite` bayrağı + `updated_at` tutulur, sonra senkron için.
- **Yarım oyunlar** uygulama kapanınca korunur ve açılışta ilk sayfalar olarak geri yüklenir. Atlama hakkı ve oturum her soğuk başlangıçta yenilenir, doğru cevap serisi kayıtlardan geri hesaplanır.
- Lider tabloları sadece online okunur, son görüntü önbelleğe alınır.
- Sırlar GitHub Secrets'ta. Service-role anahtarı hiçbir yerde yok. Anon anahtarı public olabilir.

## Kimlik
- **Google girişi tarayıcı üzerinden OAuth (PKCE)**, derin bağlantı `com.mehmtcan.brainscroll://login-callback`. Apple girişi yalnızca iOS'ta yerel (kimlik jetonu + nonce). Anonim hesaptan girişte önce kimlik **bağlanır**, kimlik başka hesaba aitse o hesaba giriş yapılır (2026-10-05).
- İlk açılışta **anonim hesap**. İsteğe bağlı olarak **Google** ve **Apple** hesabına bağlama (Apple zorunlu, çünkü Google sunuyoruz).
- Anonim hesap, zaten var olan bir hesaba bağlanırsa ilerleme **birleştirilir** (kayıtlar eklemeli olduğu için güvenli).

## Bulmacalar
- **Günlük bulmaca:** herkese aynı, cevap **sunucuda gizli**, skor ve süre **sunucuda** doğrulanır/ölçülür (Postgres RPC; Edge Function değil, soğuk başlangıç yok).
  - Akış: `start` (sunucu başlangıç zamanını yazar) -> tahmin başına `guess` (sunucu renk geri bildirimi döner) -> bitişte sunucu süreyi hesaplar.
  - **v1'de günlük bulmaca online-only.** Offline'da sonsuz akış oynanır.
- **Sonsuz akış:** yerelde üretilir, tamamen offline, sıfır gecikme.
- Yarım kalan oyun **korunur** (devam ettirilir).
- Sadece **5 harf** ile başlanır, sonra genişletilir.
- **Dil seçimi (TR/EN) akış başlamadan yapılır**, ilk harf yazılınca ya da ilk atlamada kilitlenir (2026-10-05). Akış boyunca tek dil.
- Doğrulama sözlüğü (geçerli tahminler) repoda olabilir, **cevap listesi repoda olmaz**.

## "Gün" tanımı
- Tüm gün sınırları **Europe/Istanbul** saat dilimine göre (UTC+3, yaz saati yok). Seri ve lider tabloları aynı sınırı kullanır.

## Seriler
- **Gün serisi:** günlük bulmacayı **bitirmek** yeter (kaybetsen de). Kazanma oranı ayrı istatistik.
- **Seri dondurma** hakkı vardır (kazanma/verilme kuralı Faz 6'da netleşir; öneri: her 7 günlük seride 1 hak, en fazla 1 biriken).
- **Doğru cevap serisi (sonsuz akış):** yanlış/kayıp seriyi bozar. **Atlamak bozmaz, ama akış başına yalnızca 1 atlama hakkı** vardır.
  - Açık detay (Faz 3): "akış" = bir uygulama oturumu mu, yoksa belirli bir süre aralığı mı? Öneri: oturum (soğuk başlangıç veya 30 dk hareketsizlik sonrası sıfırlanır).

## Lider tabloları
- Türler: **günlük bulmaca hızı** ve **seri uzunluğu** (güncel + en uzun). Kapsam: **genel** ve **arkadaşlar**.
- Hız sıralaması: **önce az tahmin, eşitlikte kısa süre**.
- Arkadaş ekleme: **kullanıcı adı + davet bağlantısı/kodu** (rehber erişimi yok).

## Açık / sonraya kalan
- Supabase ücretsiz plan duraklatma davranışı: yayın öncesi plan kararı.
- Gizlilik politikası ve destek sayfası `playbrainscroll.com` üzerinde yayınlanacak (App Store için gerekli).
- Seri dondurma kesin kuralları (Faz 6), atlama hakkı "akış" tanımı (Faz 3).
