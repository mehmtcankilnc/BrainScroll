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

## Günlük bulmaca (karar: 2026-10-06)
- **Dil başına ayrı bulmaca:** her gün biri TR biri EN. İkisi de oynanabilir. Gün serisi o gün herhangi birini bitirmekle sayılır. Sıralamalar (Faz 7) dil başına ayrı.
- **Cevap yalnızca veritabanında.** Hangi gün hangi kelime olduğu, o günün ilk isteğinde veritabanı tarafından rastgele seçilir ve repoya hiç yazılmaz. Havuz bitene kadar kelime tekrar etmez.
- **Süre sunucu saatiyle**, Başla düğmesine basıldığı an başlar. Başlatmayı tekrar çağırmak saati sıfırlamaz. Bir bulmaca gece yarısından önce başlayıp sonra bitirilirse başladığı güne sayılır.
- Tahmin renklendirmesi sunucuda, uygulamadaki `evaluateGuess` ile aynı kural. Sözlük denetimi önce uygulamada (yazım hatası hak yemez).
- **Seri dondurma:** serinin her 7'nin katına ulaşması 1 hak kazandırır, en fazla 1 birikir, kaçırılan bir gün hak varsa otomatik harcanır.

## "Gün" tanımı
- Tüm gün sınırları **Europe/Istanbul** saat dilimine göre (UTC+3, yaz saati yok). Seri ve lider tabloları aynı sınırı kullanır.

## Seriler
- **Gün serisi:** günlük bulmacayı **bitirmek** yeter (kaybetsen de). Kazanma oranı ayrı istatistik.
- **Seri dondurma** hakkı vardır (kazanma/verilme kuralı Faz 6'da netleşir; öneri: her 7 günlük seride 1 hak, en fazla 1 biriken).
- **Doğru cevap serisi (sonsuz akış):** yanlış/kayıp seriyi bozar. **Atlamak bozmaz, ama akış başına yalnızca 1 atlama hakkı** vardır.
  - "Akış" = oturum (karar, 2026-10-10): soğuk başlangıç veya 30 dk hareketsizlik sonrası atlama hakkı yenilenir. Seri ve atlanmış bulmacalar etkilenmez.

## Lider tabloları
- Türler: **günlük bulmaca hızı** ve **seri uzunluğu** (güncel + en uzun). Kapsam: **genel** ve **arkadaşlar**.
- Hız sıralaması: **önce az tahmin, eşitlikte kısa süre**.
- Arkadaş ekleme: **kullanıcı adı + davet bağlantısı/kodu** (rehber erişimi yok).
- **Faz 7 kararları (2026-10-06):**
  - Kullanıcı adı ve tablolara girmek için **Google/Apple girişi şart**. Anonim hesaplar tablolarda ve arkadaş listelerinde yer almaz.
  - Kullanıcı adı: 3-16 karakter, `a-z 0-9 _`, büyük/küçük harfe duyarsız benzersiz, küçük bir TR/EN yasaklı kelime listesi sunucuda denetlenir.
  - Günlük hız tablosu: **bugün** ve **tüm zamanlar** (kişinin tüm günlerdeki en iyi tek sonucu: az tahmin, eşitlikte kısa süre). Dil başına ayrı.
  - Arkadaşlık: davet kodu/bağlantısıyla **anında ve karşılıklı**, onay adımı yok. Kullanıcı adı ile ekleme **istek + kabul** ile olur (kullanıcı adı herkese açık olduğu için).

## Hafıza eşleştirme ve karışık akış (karar: 2026-10-10)
- İkinci oyun **hafıza eşleştirme**. Akış artık **karışık**: sayfalar **dönüşümlü** gelir (kelime, hafıza, kelime...). Sıralama sonra değişebilir, bu yüzden oyun sırası tek bir yerde (`EndlessFeed.games`) tanımlıdır.
- **Tahta:** tek boyut, **4x3 = 12 kart, 6 çift**. Zorluk kademeleri (4x4, 4x5) sonra, oyuncunun başarısına göre.
- **Kart içeriği:** koddan çizilen vektör şekiller (daire, üçgen, yıldız, kalp, elmas, kare, artı, ay), her biri farklı şekil **ve** farklı renkte, içerik renkle değil şekille ayırt edilir. Emoji yok.
- **Kurallar:** her hamle iki kart açmak. Eşleşirse kartlar kalır, eşleşmezse **hata** sayılır ve kartlar kısa süre sonra (ya da sonraki dokunuşta) kapanır. **6. hatada oyun kaybedilir.** Tüm çiftler bulunursa kazanılır. Ölçüt: hamle sayısı ve süre.
- **Süre cihazda ölçülür** (günlük bulmaca gibi sunucuda değil, çünkü sonsuz akışta sıralama yok). Süre, art arda iki hamle arasındaki sürelerin toplamıdır (her aralık en fazla 10 sn sayılır), yani uygulama arka planda kalsa şişmez.
- **Seri:** doğru cevap serisi (şimşek) **tüm oyunları birlikte** sayar: her galibiyet +1, her kayıp sıfırlar. Atlama hakkı oturum başına bir tane ve oyunlar arasında ortaktır.
- **Dil:** hafızada dil yoktur. Akıştaki dil seçimi yalnızca kelime sayfalarını etkiler.
- **Veri:** hafıza sonuçları mevcut `game_result` tablosuna `game = 'memory'` ile yazılır, sunucu şemasında değişiklik gerekmez (`answer` = kart dizilimi, `guesses` = açılan kartların sırası). Yarım kalan hafıza oyunu da korunur (mevcut `in_progress_round` tablosunda).
- Hafıza için günlük mod **yok** (ilk sürüm), sıralama yok.

## Renk tüpleri (karar: 2026-10-11)
- Üçüncü oyun. Akış sırası kelime, hafıza, **renk tüpleri**, kelime... (`EndlessFeed.games`).
- **Tahta:** tek boyut, **5 renk x 4 birim = 7 tüp** (5 dolu, 2 boş), tüp kapasitesi 4. Zorluk kademeleri sonra.
- **Kural:** bir tüpün üstündeki aynı renk grubu, boş ya da üstü aynı renkte olan başka bir tüpe, sığdığı kadar dökülür. Her tüp boş ya da tek renkle dolu olunca kazanılır. Tek renkli tüpü boş tüpe dökmek geçersizdir (boşuna hamle).
- **Kayıp yok.** Oyuncu sıkışırsa **geri al** (sınırsız, tek hamle) ya da **yeniden başlat** (tüpleri başa alır) kullanır. İkisi de **hamle sayacını geri vermez**: sayaç dökmelerin toplamıdır, yani denemenin bedelidir. Sonuç hep galibiyettir; seriyi yalnızca artırır, hiç sıfırlamaz.
- **Her tahta çözülebilir:** karıştırılan tahta bir çözücüyle (`TubesGame.canSolve`) kontrol edilir, çözülemeyen ya da başlangıçta bitmiş tüp içeren tahta atılır.
- **Renk körlüğü:** her renk aynı zamanda bir **şekildir** (kart şekilleriyle aynı beş şekil), içerik yalnızca renkle ayırt edilmez (design.md ilkesi). Ekran okuyucu tüpü alttan başlayarak şekil adlarıyla okur.
- **Süre** hafızadaki gibi cihazda ölçülür: eylemler arası süreler toplamı, her aralık en fazla **15 sn** (düşünmek daha uzun sürer).
- **Veri:** `game_result`'a `game = 'tubes'`. `answer` = başlangıç tahtası (`0121|2013|...|`, boş tüp boş), en fazla 26 karakter (sunucu sınırı 32). `guesses` = **yalnızca hamle sayısı** tek öğe olarak: sunucu `guesses` için en fazla 256 karakter kabul ediyor ve çok geri alan bir oyunun eylem günlüğü sığmazdı. Tam eylem günlüğü (`3>5`, `u`, `r`) yalnızca cihazdaki yarım oyunda (`in_progress_round`) durur ve oyunu aynen geri kurmaya yarar. Yeni migration yok.
- Günlük mod ve sıralama yok (ilk sürüm).

## Açık / sonraya kalan
- Supabase ücretsiz plan duraklatma davranışı: yayın öncesi plan kararı.
- Gizlilik politikası ve destek sayfası `playbrainscroll.com` üzerinde yayınlanacak (App Store için gerekli).
- Hesap birleştirme: anonim hesap mevcut bir Google/Apple hesabına geçerse sonuçlar bilet yoluyla taşınır (2026-10-10, plan Faz 8).

## Ana Sayfa (karar: 2026-10-07)
- **Yer:** yeni **Ana Sayfa** sekmesi, ilk sekme ve açılış ekranı. Sekme sırası: Ana Sayfa, Akış, Günlük, Sıralama, Profil.
- **Bölüm sırası (yukarıdan aşağı):** Seriler, Bugünün bulmacası (en belirgin kart, TR/EN ve durum), Oyun kartları, En hızlılar önizlemesi, Akışını kişiselleştir.
- **Seriler:** gün serisi + doğru cevap serisi, seri dondurma hakkı rozeti. Bugünün günlüğü bitmediyse gün serisinde küçük "bugün bekliyor" göstergesi. Anonim dahil herkese gösterilir; sıralamaya girmek için giriş ipucu.
- **Popüler oyunlar:** v1'de gerçek sayaç yok, oyun kartları listesi. Karta dokunmak o oyunu Akış'ın ilk sayfası yapar, sonrası açık oyun türleriyle karışık devam eder. Gerçek popülerlik sayacı sonra.
- **En hızlılar:** bugünün ilk 3-5 kişisi, "Tümü" ile Sıralama sekmesine. Dil, akış dili ayarından. Anonim görebilir.
- **Kişiselleştirme (v1):** yalnızca **oyun türü** (kelime/hafıza aç-kapa) ve **kelime dili** (TR/EN). Zorluk ayarı kademeler gelince. En az bir oyun açık kalır; varsayılan ikisi de açık.
- **Ayarlar:** Ana sayfada kart + alt sayfa (sheet), Profil'den de link. Değişiklik sonraki yeni sayfalardan geçerli, yarım oyun bozulmaz. Cihazda saklanır (hesap senkronu sonra).
- **Çevrimdışı:** günlük kart "bağlantı gerekli" pasif; hız önizlemesi son önbellekle (yoksa boş durum mesajı); seriler ve oyun kartları yerelden çalışır.

## Sadeleştirme (karar: 2026-10-07)
- **Neden:** Akış (oyun) ekranı sade ve beğeniliyor; diğer sayfalar ve nav bar kalabalık. Hedef: ekran başına tek ana iş, ikincil şeyler gizli, yüzey (kart) yalnızca tıklanabilir/ana öğelerde, listeler arka plansız ve ince ayraçlı.
- **Nav bar:** 3 sekme: Ana Sayfa, Akış, Profil. Günlük ve Sıralama sekme değil, geri düğmeli tam ekran alt sayfa. Günlük Ana Sayfa kartından, Sıralama Ana Sayfa "Tümü" bağlantısından (ve Profil) açılır.
- **Akış:** nav bar gizli, sol üstte küçük ev düğmesi Ana Sayfa'ya döner.
- **Ana Sayfa (Q6 değişti, oyun kartları kalktı):** en üstte kutusuz tek satır seri şeridi (alev + şimşek sayıları, dondurma rozeti, bugün bekliyor için küçük nokta), günlük kart (tamamı dokunulur), satır içi kişiselleştirme (oyun çipleri + kelime dili çipleri), en altta en hızlılar ilk 3 satır. Akışa tek yol Akış sekmesi. Satır içi kişiselleştirme beğenilmezse panele (PersonalizeSheet) geri dönülür.
- **Sıralama:** üstte Hız / Seri / Arkadaşlar; dil, kapsam, herkes/arkadaşlar tek "Filtre" arkasında. Varsayılan: akış dili, Bugün, Herkes.
- **Profil:** kısa özet kartı + İstatistikler / Favoriler / Ayarlar satırları, her biri kendi alt sayfasında.
- **İş sırası:** (1) Ana Sayfa + nav bar, (2) Sıralama, (3) Profil, (4) görsel dil taraması. Her parçadan sonra ekran görüntüsü, onay beklenir.
