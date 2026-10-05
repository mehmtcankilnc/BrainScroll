# BrainScroll - Faz Planı

Kararlar için bkz. [decisions.md](decisions.md). Kutucukları ilerledikçe işaretle.
**TestFlight kapısı:** Her faz sonunda iOS CI yeşil olmalı, büyük özellik fazlarında (3, 5, 6, 7) ayrıca TestFlight'ta telefonda doğrulama yapılır.

---

## Faz 0 - Hesaplar ve hazırlık
Amaç: Kod yazmadan önce dış bağımlılıkları sıraya koymak (onay süreleri uzun olabilir).
- [x] GitHub public repo, MIT lisansı, `README`, `.gitignore`
- [x] Apple Developer: App ID `com.mehmtcan.brainscroll`, Sign in with Apple yeteneği, App Store Connect'te uygulama kaydı
- [x] App Store Connect API anahtarı (BrainScroll CI, App Manager). İmzalama sertifikası Faz 1'de (öneri: API anahtarıyla bulut yönetimli imzalama)
- [x] Google Cloud: OAuth onay ekranı + **web** istemcisi, Supabase'de Google sağlayıcısı (2026-10-05). Android/iOS istemcisi ve imza parmak izi **gerekmedi**: Google girişi tarayıcı OAuth'uyla yapılıyor (Faz 5)
- [x] Supabase projesi (Frankfurt), Anonymous girişi, manuel bağlama ve Apple sağlayıcısı (yalnızca native iOS, Client ID = bundle ID). Google Faz 5'te
- [x] `playbrainscroll.com`: basit açılış sayfası, gizlilik politikası (Cloudflare Workers statik varlıklar, `site/`), destek e-postası yönlendirmesi
- [x] Kelime listesi kaynakları: bkz. [word-lists.md](word-lists.md). **EN: SCOWL v2. TR: hunspell-tr (MPL-2.0, değiştirilmeden) geçerli tahminler için + kendi derlediğimiz cevap havuzu**
**Çıkış:** Tüm hesaplar ve anahtarlar hazır, sırlar GitHub Secrets'a girmeye hazır.

## Faz 1 - İskelet ve CI
Amaç: Boş uygulama üç hedefte derlensin, iOS CI + TestFlight hattı çalışsın.
- [x] KMP + Compose Multiplatform proje iskeleti (Android, iOS, Desktop), Gradle sürüm kataloğu
- [x] Klasör yapısı, paket adı `com.mehmtcan.brainscroll`
- [x] Temel tema (tokenlar, `BrainScrollTheme`, ilk bileşen `LetterTile`), TR/EN string altyapısı (Compose resources), Nunito fontu. Desktop'ta doğrulandı, iOS/Android ekranı TestFlight/emülatörde kontrol edilecek
- [x] GitHub Actions: Android + Desktop derleme ve birim testleri (`ci.yml`). Lint henüz yok
- [x] GitHub Actions: iOS derleme (macOS runner), imzalama, TestFlight'a yükleme (`ios.yml`, `xcodebuild` + API anahtarı)

> CI notları: iOS imzalama için Apple'ın bulut imzalaması kullanılıyor. **API anahtarı Admin rolünde olmalı** (App Manager yetmiyor, "Cloud signing permission error" veriyor). Archive imzasız alınır, imzalama export adımında yapılır (geliştirme profili için kayıtlı cihaz gerekmesin diye). Admin anahtarının secret'ları yalnızca `testflight` GitHub Environment'ında tutulur.
**Çıkış:** Telefonumda TestFlight'tan açılan "Merhaba BrainScroll" uygulaması. **Tamamlandı (2026-10-05):** Desktop, Android emülatörü ve iPhone'da (TestFlight) doğrulandı.

## Faz 2 - Wordle çekirdeği (saf Kotlin)
Amaç: UI'dan bağımsız, test edilebilir oyun mantığı.
- [x] Ortak `Game` arayüzü (yeni oyunlar eklemeye açık)
- [x] Wordle motoru: tahmin değerlendirme (tekrarlı harf kuralları dahil), kazanma/kaybetme durumu
- [x] TR harf kuralları (`ç ğ ı ö ş ü`, `İ/I` büyük-küçük harf tuzağı), EN kuralları
- [x] Kelime listesi yükleme (geçerli tahmin sözlüğü + sonsuz akış için cevap havuzu). Üretim betikleri ve TR cevap havuzu inceleme süreci için bkz. [word-lists.md](word-lists.md), `THIRD_PARTY_NOTICES.md` ile birlikte
- [x] Birim testleri (kenar durumlar: çift harf, TR harfleri)
**Çıkış:** Testleri geçen, platformdan bağımsız Wordle motoru. **Tamamlandı (2026-10-05).** EN listeleri SCOWL v2'den (`build_en.py`), TR geçerli tahmin listesi hunspell-tr'den (`build_tr.py`), TR cevap havuzu elle seçilip gözden geçirildi (1.102 kelime).

## Faz 3 - Arayüz ve kaydırma akışı
Amaç: İlk oynanabilir sürüm (sadece yerel).
- [x] `VerticalPager` akışı, yalnızca mevcut + sonraki sayfa canlı
- [x] Wordle arayüzü: ızgara, ekran klavyesi (TR/EN düzeni), harf çevirme animasyonları
- [x] Sonsuz akış üreteci (yerel)
- [x] Yarım kalan oyunun korunması (kaydırıp geri dönünce devam)
- [x] Atlama hakkı (akış başına 1) ve doğru cevap serisi mantığı
- [x] TR + EN yerelleştirme
> Faz 3 notları (2026-10-05): Kod ve başsız UI testleri (`FeedScreenTest`, ekran görüntüleri `shared/build/screenshots`) tamam, Android debug derlemesi başarılı. **Android emülatöründe doğrulandı (2026-10-05):** klavyede dikey sürükleme sayfayı kaydırmıyor, harf çevirme akıcı, İ/I tuşları çalışıyor. Sonradan düzeltilenler: hak bitince ileri kaydırma baştan engellenir, dil akış başlayınca kilitlenir, hata sonrası sonraki satır sallanmaz. **iPhone'da (TestFlight) doğrulanacak.** Henüz yapılmayanlar: "hareketi azalt" ayarı, ekran okuyucu etiketleri (Faz 8), kazanma kutlaması, 30 dk hareketsizlikte akışın sıfırlanması (şimdilik akış = uygulama oturumu), yarım oyunun uygulama kapanınca korunması (Faz 4, SQLDelight).
**Çıkış:** Android/Desktop'ta tam oynanabilir akış. **TestFlight doğrulaması bekliyor.**

## Faz 4 - Yerel veri
Amaç: Offline-first temeli.
- [x] SQLDelight şeması: oyun sonuçları, yarım oyunlar, favoriler, ayarlar
- [x] Seri ve istatistikleri kayıtlardan türetme (gün serisi, doğru cevap serisi, doğruluk, tamamlama)
- [x] İstatistik ve favoriler ekranları
- [x] Gün sınırı yardımcıları (Europe/Istanbul) + testler
- [x] Seri dondurma (yerel kural taslağı)
> Faz 4 notları (2026-10-05): Şema `shared/src/commonMain/sqldelight/` (game_result, in_progress_round, favorite, setting). Seri/istatistik kayıtlardan türetilir (`stats/`). Favori = bitmiş bulmaca (sonuç kartındaki kalp), Profil sekmesinde listelenir. Gün sınırı `IstanbulDay`. Seri dondurma yalnızca taslak (`DayStreaks`, her 7 günde 1 hak, en fazla 1), günlük bulmaca Faz 6'da gelince ekrana bağlanır, kesin kurallar o zaman. Uygulama yeniden başlayınca yarım oyunlar, seri ve dil geri yüklenir; atlama hakkı yeni oturumda yenilenir. Testler: 135 (JVM, gerçek SQLite bellekte + başsız UI). CI'a `compileIosMainKotlinMetadata` eklendi (commonMain'de JVM'e özgü API'yi yakalar). **Cihazda doğrulanacak:** Android'de uygulamayı kapatıp açınca yarım oyun/seri/favori, iPhone'da (NativeSqliteDriver derlemesi dahil, CI + TestFlight).
**Çıkış:** Uygulama kapanıp açılınca her şey yerinde, istatistikler doğru. **Cihaz doğrulaması bekliyor.**

## Faz 5 - Supabase, kimlik ve senkron
Amaç: Hesaplar ve outbox ile bulut yedek.
- [x] Veritabanı şeması + RLS politikaları (kullanıcı yalnızca kendi verisini yazar/okur) (`supabase/migrations/`, PGlite ile doğrulandı; projeye `npx supabase db push` ile uygulanacak)
- [x] Anonim oturum (ilk açılış), oturumun kalıcı saklanması (supabase-kt oturum yöneticisi)
- [x] Google (tarayıcı OAuth, PKCE) ve Apple (iOS yerel) ile giriş / anonim hesabı bağlama (Android'de Google girişi doğrulandı 2026-10-05; Apple iPhone'da doğrulanacak)
- [x] Var olan hesaba bağlamada ilerleme birleştirme (kimlik zaten varsa o hesaba giriş yapılır, yerel geçmiş o hesaba yüklenir)
- [x] Outbox senkron: yerel kayıtlar -> Supabase, yeniden deneme, çevrimdışı dayanıklılık (`SyncEngine`, `SyncCoordinator`)
- [x] Yeni cihazda geri yükleme (giriş sonrası ilk senkron buluttan çeker)
> Faz 5 notları (2026-10-05): Veritabanı v2 (`sync_queue`, migration `1.sqm`). Günlük (DAILY) sonuçlar istemciden yüklenmez, Faz 6'da sunucu yazar. Google girişi tarayıcıdan olduğu için Android SHA-1 / iOS Google istemcisi gerekmedi. Açık nokta: anonim hesap bir Google hesabına bağlanamazsa (kimlik başka kullanıcıda) o hesaba giriş yapılır, anonim hesaba sunucuda yazılmış DAILY sonuçlar yetim kalır (Faz 6'da birleştirme RPC'si düşünülmeli). Testler: 176 JVM testi (sahte bulut, PGlite ile SQL).
**Çıkış:** İki cihazda aynı hesapla veri tutarlı. **TestFlight doğrulaması** (Apple girişi gerçek cihazda) bekliyor.

## Faz 6 - Günlük bulmaca (sunucu doğrulamalı)
Amaç: Adil, hile korumalı günlük bulmaca.
- [x] Cevap tablosu (sadece sunucuda, istemciye RLS ile kapalı), günlük cevap zamanlaması (`daily_puzzle`: o günün ilk isteğinde veritabanı rastgele seçer, havuz bitmeden tekrar etmez, program repoda yok)
- [x] Postgres RPC: `start_daily`, `get_daily_state`, `submit_daily_guess` (renk geri bildirimi uygulamadaki `evaluateGuess` ile aynı kural), bitiş ve süre hesabı (sunucu saati) (`supabase/migrations/20261006090000_daily_puzzle.sql`, PGlite'ta 62 kontrolle doğrulandı)
- [x] İstemci: günlük oyun akışı (Günlük sekmesi), isteği animasyonla maskeleme (gönderilen satır nabız atar, cevap gelince çevrilir). `start` ön yüklemesi yerine **Başla düğmesi**: süre yanlışlıkla başlamasın diye sunucu saati düğmeye basınca başlar
- [x] Online-only davranışı ve çevrimdışı mesajı (tekrar dene düğmesiyle)
- [x] Gün serisi (günlük sonuç sunucuda yazılır, istemci kayıtlardan hesaplar), seri dondurma kesin kuralı: **7 günde 1 hak, otomatik, en fazla 1 birikir** (`DayStreaks`)
- [x] Kötüye kullanım testleri (tekrar gönderim, saat oynama, eşzamanlı oturum): tekrar gönderim, tekrar başlatma, istemci saatinin etkisizliği ve istemcinin günlük sonuç yazamaması test edildi. Canlı projede 10 eşzamanlı tahmin (tam 6'sı kabul edildi, tek sonuç satırı) ve 8 eşzamanlı başlatma (tek saat) denendi, hepsi geçti (2026-10-06).
> Faz 6 notları (2026-10-06): Günlük bulmaca dil başına ayrı (TR ve EN), ikisi de oynanabilir, gün serisi herhangi birini bitirmekle sayılır. Tahmin sözlüğü sunucuda denetlenmez, uygulama kendi listesiyle önce kontrol eder (yazım hatası tahmin hakkı yemez). Sonuç yerelde de hemen kaydedilir (seri anında güncellenir), sonra buluttan gelen aynı sonuç yok sayılır. Veritabanı v3 (`duration_ms`). 218 JVM testi.
**Çıkış:** Günlük bulmaca çalışıyor, süre ve skor sunucuda. **Migration'lar canlı projede, canlı doğrulama geçti. iPhone (TestFlight) doğrulaması bekliyor.**

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
