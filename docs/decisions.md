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

## Mayın tarlası (karar: 2026-10-11)
- Dördüncü oyun. Akış sırası kelime, hafıza, renk tüpleri, **mayın tarlası**, kelime... (`EndlessFeed.games`).
- **Tahta:** tek boyut, **8x8 = 64 hücre, 10 mayın**. Zorluk kademeleri sonra.
- **İlk dokunuş her zaman güvenli:** mayınlar ilk açıştan **sonra** yerleşir, açılan hücre ve komşuları mayınsızdır (ilk hücre 0 olur ve bir alan açılır).
- **Tahta tahminsiz çözülebilir:** üretilen her tahta saf mantıkla (`MinesSolver`) bitirilebilir, şansa kalan yer yoktur. Çözülemeyen tahmin atılır ve yenisi üretilir. Hızlı akışta "haksız" bir kayıp hissini önler.
- **Kayıp:** mayına basınca **hemen** kayıp (hafızadaki "çok hata" gibi): şimşek serisi sıfırlanır, tüm mayınlar gösterilir, basılan mayın kırmızı. Kazanma: mayın olmayan her hücre açılır, kalan mayınlar bayraklı gösterilir.
- **Bayrak:** hücreye **uzun basma** ve altta **bayrak modu** düğmesi ikisi birden. Bayrak modunda dokunma bayrak koyar, uzun basma açar (ikisi yer değiştirir). Bayrak sayısı sınırsız, sayaç eksiye inebilir. Bayraklı hücre dokunmayla açılmaz.
- **Chord:** açık bir sayıya **çift dokunma**: çevresindeki bayrak sayısı sayıya eşitse bayraksız komşuların hepsi açılır (yanlış bayrak mayın açtırıp kaybettirir). Ekran okuyucuda sayının tıklaması chord'dur, bayrak için özel uzun tıklama eylemi vardır.
- **Kaydırma çakışması yok:** hücreler yalnızca dokunma/uzun basma/çift dokunma dinler, dikey sürükleme akışın sayfa kaydırmasına gider.
- **Süre** diğer oyunlar gibi cihazda: eylem araları toplamı, her aralık en fazla **15 sn**; ilk eylem (bayrak dahil) saati başlatır.
- **Veri:** `game_result`'a `game = 'mines'`. Tahta kaydedilmez, **yeniden üretilir**: `answer` = `8x8x10:<seed>:<ilk hücre>` (ilk açıştan önce -1), en fazla 21 karakter (sunucu sınırı 32). `guesses` = **yalnızca hamle sayısı** (açma + chord; bayrak hamle sayılmaz), tek öğe (sunucu sınırı 256). Cihazdaki yarım oyun tam eylem günlüğünü (`o12`, `f12`, `c12`) tutar ve oyunu seed'den aynen geri kurar. Yeni migration yok.
- Günlük mod ve sıralama yok (ilk sürüm). Profil: oynanan, kazanma %, en iyi süre.

## Sudoku (karar: 2026-10-09)
- Beşinci oyun. Akış sırası kelime, hafıza, renk tüpleri, mayın tarlası, **sudoku**, kelime... (`EndlessFeed.games`).
- **Tahta:** klasik **9x9**, tek seviye, **36 ipucu (verilen sayı)**. Zorluk seviyeleri sonra: üretici ve tur zaten `clues` (ipucu sayısı) parametresi alıyor, kayıtta tahtanın adında yazılı, yani seviye eklemek yeni bir veri biçimi gerektirmez. Az ipucu = zor, ama aşağıdaki "salt mantık" sözü zorluğun alt sınırını belirler (yalnızca tekli kurallar yetmeyen tahtalar için daha ileri teknikler gerekir, bu seviye kararıyla birlikte alınacak).
- **Tahta tek çözümlü ve tahminsiz:** üretilen her tahta yalnızca "çıplak tekli" (bir hücrede tek aday kalması) ve "gizli tekli" (bir satır, sütun ya da kutuda bir sayının tek yeri kalması) ile çözülebilir. Bu, tek çözüm demektir ve mayın tarlasındaki "şansa kalan yer yok" sözüyle aynı. Üretim: sabit bir desenden tam bir ızgara (satır/sütun/bant ve sayı karıştırma), sonra hücreler rastgele sırayla boşaltılır ve boşaltma yalnızca tahta hâlâ tekli kurallarla çözülüyorsa kalır. 36'ya inilemezse birkaç ipucu fazla kalır (testte en çok 42).
- **Tahta tohumdan yeniden üretilir:** `seed` (ve `clues`) aynı tahtayı verir (`kotlin.random.Random(seed)`), tahta kaydedilmez. `answer` = `9x9x36:<seed>`, en fazla 20 karakter (sunucu sınırı 32).
- **Hata kuralı:** sayı çözümden farklıysa **hata**, kırmızı görünür, **3. hata oyunu kaybettirir** (hafızadaki "6. hata" gibi, şimşek serisi sıfırlanır). Doğru sayı **kalıcıdır** (silinmez, değiştirilmez); yanlış sayı silinebilir ya da başka sayıyla değiştirilebilir (her yanlış deneme yine hata sayılır). Aynı yanlış sayıyı tekrar basmak hata sayılmaz. Kazanmak için tüm hücreler çözümle aynı olmalı.
- **Notlar:** "Notlar" düğmesi açıkken sayı tuşları hücreye küçük **kalem notu** yazar (aç/kapa). Doğru sayı konunca aynı satır, sütun ve kutudaki o sayının notları **kendiliğinden silinir**. Silgi: yanlış sayıyı geri alır ya da hücrenin notlarını temizler. Yanlış sayı çevredeki notlara dokunmaz.
- **Arayüz:** hücre seç, sonra sayı tuşu (1-9, silgi). Seçili hücre sarı çerçeve, aynı satır/sütun/kutu ve aynı sayılar açık zemin ve sarı ince çerçeve. Verilen sayılar beyaz kalın, oyuncunun sayıları mavi, yanlışlar kırmızı. 9 kez doğru yerleşen sayının tuşu soluk. Kutular arasında geniş boşluk.
- **Hamle:** yerleştirilen sayı sayısı (notlar ve silme hamle sayılmaz).
- **Süre** diğer oyunlar gibi cihazda: eylem araları toplamı, ama sudoku daha çok düşünme istediği için her aralık en fazla **30 sn** (diğer oyunlarda 10-15 sn). İlk eylem (not dahil) saati başlatır.
- **Veri:** `game_result`'a `game = 'sudoku'`. `guesses` = **yalnızca hamle sayısı** tek öğe (sunucu sınırı 256). Cihazdaki yarım oyun tam eylem günlüğünü tutar (`p12:5` sayı koy, `n12:5` not aç/kapa, `e12` sil) ve tahtayı tohumdan aynen geri kurar. Yeni migration yok.
- Günlük mod ve sıralama yok (ilk sürüm). Profil: oynanan, kazanma %, en iyi süre.

## Işıkları söndür (karar: 2026-10-09)
- Altıncı oyun. Akış sırası kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, **ışıkları söndür**, kelime... (`EndlessFeed.games`).
- **Kural:** 5x5 ızgara. Bir ışığa dokunmak onu ve hemen üstündeki, altındaki, solundaki, sağındaki ışığı değiştirir (yanan söner, sönük yanar; kenar ve köşede olmayan komşu yok sayılır). Tüm ışıklar sönünce oyun kazanılır.
- **Her tahta çözülebilir:** tahta **geriye doğru** üretilir. Sönük tahtadan başlayıp **7 farklı hücreye** dokunulur (tohumla seçilir). Dokunuşlar sırayla ilgisizdir ve iki kez dokunmak iptal eder, yani aynı 7 hücreye yeniden dokunmak tüm ışıkları söndürür. Böylece her tahta **en çok 7 dokunuşla** çözülür. Çok az ışığı olan (4'ten az) tahta atılır ve yenisi üretilir.
- **Kaybetmek yok** (renk tüpleri gibi): kazanç ortak seriyi artırır. Aynı ışığa ikinci dokunuş zaten geri almadır, bu yüzden ayrı bir "geri al" yok; yalnızca **baştan başla** düğmesi var. Baştan başlamak hamle sayacını geri vermez (renk tüpleri ile aynı karar). Tahta başlangıçtaysa düğme soluk.
- **"En çok 7" göstergesi:** hamle sayacının yanında, tahtanın en çok kaç dokunuşta çözüleceği yazar (adil bir hedef, en iyi çözüm bundan kısa olabilir). Zorluk seviyeleri sonra: üretici ve tur `size` (tahta boyu) ve `taps` (dokunuş sayısı) parametrelerini alıyor, kayıtta tahtanın adında yazılı (`5x5x7:<seed>`); çok dokunuş = daha zor.
- **Görünüm:** yanan ışık sarı dolgu ve içinde koyu daire, sönük ışık koyu zemin ve içinde soluk halka (renk körlüğünde şekille de ayırt edilir). Geçiş 200 ms renk animasyonu ("hareketi azalt" açıkken anında).
- **Süre** diğer oyunlar gibi cihazda: eylem araları toplamı, her aralık en fazla **15 sn**; ilk dokunuş saati başlatır.
- **Veri:** `game_result`'a `game = 'lights'`. `answer` = `5x5x7:<seed>`, en fazla 19 karakter (sunucu sınırı 32). `guesses` = **yalnızca dokunuş sayısı** tek öğe (sunucu sınırı 256). Cihazdaki yarım oyun eylem günlüğünü tutar (`t12` dokun, `r` baştan başla) ve tahtayı tohumdan aynen geri kurar. Yeni migration yok.
- Günlük mod ve sıralama yok (ilk sürüm). Profil: oynanan, en iyi süre, en az hamle.

## Kayan bulmaca (karar: 2026-10-09)
- Yedinci oyun. Akış sırası kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, **kayan bulmaca**, kelime... (`EndlessFeed.games`).
- **Tahta:** **3 sütun x 4 satır**, 11 numaralı taş ve bir boş hücre (telefon gibi dik, taşlar rahat vurulur). Hedef: 1 sol üstte, sayılar sırayla, boşluk en sonda. Taşlar yalnızca sayıdır (resim yok).
- **Hamle:** boşlukla **aynı satır ya da sütundaki** bir taşa dokunmak onu boşluğa kaydırır; arada başka taşlar varsa **hepsi birlikte** bir adım ilerler. Bu **tek hamle** sayılır. Boşlukla aynı hizada olmayan taş ve boşluğun kendisi yok sayılır (hamle sayılmaz, haptik yok).
- **Her tahta çözülebilir:** tahta bitmiş resimden **geçerli kaydırmalarla karıştırılır** (80 rastgele tek taş kaydırması, bir öncekini hemen geri almadan, tohumla). Bu çözülebilirliği garanti eder (3 sütunda çözülebilirlik = ters çevrilmiş çift sayısı çift, testte doğrulanıyor, ayrıca gerçek bir çözücüyle birkaç tahta çözülüyor). Taşların yerinden toplam uzaklığı 14'ten azsa tahta çok kolay sayılır ve karıştırma sürer.
- **Kaybetmek yok** (renk tüpleri ve ışıkları söndür gibi): kazanç ortak seriyi artırır. Yalnızca **baştan başla** düğmesi var (geri al yok; hamleyi geri almak için taşı geri kaydırırsın). Baştan başlamak hamle sayacını geri vermez. Tahta başlangıçtaysa düğme soluk.
- **"En çok" göstergesi yok:** karıştırma 80 adım olduğu için bu sayı en iyi çözümü anlatmazdı. Skor: hamle ve süre.
- **Görünüm:** koyu zeminli yuvarlak köşeli taşlar, büyük kalın sayı. **Yerinde olan taşın sayısı ve çerçevesi yeşil** (yardım ve ilerleme göstergesi). Taş yeni hücresine **200 ms kayarak** gider ("hareketi azalt" açıkken anında). Sayı boyutu taşa göre, yazı boyutu ayarına göre değil (taşa sığmalı).
- **Süre** diğer oyunlar gibi cihazda: eylem araları toplamı, her aralık en fazla **15 sn**; ilk kaydırma saati başlatır.
- **Veri:** `game_result`'a `game = 'slide'`. `answer` = `3x4x80:<seed>` (sütun x satır x karıştırma adımı), en fazla 19 karakter (sunucu sınırı 32). `guesses` = **yalnızca hamle sayısı** tek öğe (sunucu sınırı 256). Cihazdaki yarım oyun eylem günlüğünü tutar (`m7` 7. hücreden kaydır, `r` baştan başla) ve tahtayı tohumdan aynen geri kurar. Yeni migration yok.
- Zorluk seviyeleri sonra: tahta boyu (3x3, 4x4) ve karıştırma. Günlük mod ve sıralama yok (ilk sürüm). Profil: oynanan, en iyi süre, en az hamle.

## Nasıl Oynanır? (karar: 2026-10-12)
- **Her oyun sayfasında** (kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, kayan bulmaca) başlığın hemen altında **"Nasıl Oynanır?"** düğmesi var (soru işaretli hap). Aynı yerde durur, böylece oyundan oyuna kayma olmaz.
- Düğme, o oyunun kurallarını **alt panel** olarak açar (Ana Sayfa'daki kişiselleştirme paneliyle aynı desen): arkası karartılır, numaralı 4-5 kısa adım, "Anladım" düğmesi. Dışına dokunmak, "Anladım" ya da Android geri tuşu kapatır. Panel açıkken arkadaki oyun dokunuş, kaydırma ve fiziksel klavye almaz.
- Metinler TR/EN, her oyunun kuralı ve **kaybetme/kazanma** koşulu ile oyuna özel ipuçları (bayrak, chord, geri al, şekiller) bir yerde (`ui/howto/HowToPlay.kt` + `howto_*` metinleri). Yeni oyun eklerken yalnızca `contentOf` içine bir satır ve metinler eklenir.
- Kural değişirse (ör. mayın sayısı, deneme sayısı) bu metinlerin de güncellenmesi gerekir.

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
