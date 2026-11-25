# BrainScroll Pro: tek seferlik satın alım planı

Ayrı bir oturumda uygulanmak üzere yazıldı. Bu belge 22 Ekim 2026'da `docs/plus-plan.md` (abonelik planı) yerine geçti; o plan bırakıldı, gerekçesi bölüm 1'de. Kodla ilgili tespitler 22 Ekim 2026'daki çalışma ağacına göre. Uygulayan oturum önce bölüm 13'teki "başlamadan kontrol et" listesine baksın.

**Revizyon, 23 Kasım 2026:** Aşama A kodlanmış ve iOS'ta TestFlight'ta denenmiş durumda; bu revizyon hazır kodun üzerine yapılacak değişiklikleri tanımlar (bölüm 18). Özet: (1) Türkiye için kesin olarak ayrı, daha ucuz fiyat. (2) Seri koruma: ücretsizde 15 günde 1 hak (en fazla 1), Pro'da haftada 1 hak (en fazla 3). (3) Ana sayfa, Akış ve Profil'in sağ üstüne mağaza ikonu; ipucu paketleri ve Pro buradan alınır (bölüm 7.4). (4) Kozmetik askıya alındı; ücretsizde de Pro'da da aynı varsayılan görünüm (bölüm 8.5). (5) Arkadaş sınırı fikri değerlendirildi, şimdilik yapılmıyor (bölüm 12).

## 1. Neden tek seferlik Pro, abonelik değil

- Plus'a konan şeyler (ipucu limiti, seri hakkı, tema) tek seferlik rahatlıklar. Abonelik, her ay yeni değer alınan şeylerde tutar; burada kullanıcı "ipucumu kısıp Plus'a soktular" diye okur.
- Uygulamada reklam yok ve olmayacak, yani "reklamsız" gibi bir koz da yok. Kalan dürüst anlatım: "uygulamayı destekle, rahatla".
- Türkiye'de ve genel olarak düşük gelirli pazarlarda aylık abonelikte dönüşüm düşük, iptal yüksek. Tek seferlik ödeme psikolojik olarak çok daha kolay.
- Abonelik için gerçek değer (geçmiş günlük arşivi, antrenman modu, beceri istatistikleri, özel arkadaş ligleri) henüz yok. Değeri olmayan abonelik satılmaz. Bu özellikler gelince bölüm 12'deki ölçütlere bakıp abonelik yeniden konuşulur.
- Eski plandaki altyapı işinin çoğu aynen kullanılır: `LocalPlus` benzeri durum katmanı, ipucu kotası, seri kuralı parametresi, tema jetonları, RevenueCat. Değişenler: ürün türü (non-consumable), deneme ve yenileme kısımlarının kalkması, abonelik bitişi (hoşgörü süresi, `streak_floor`) sorunlarının ortadan kalkması.

## 2. Verilen kararlar

| Konu | Karar |
|---|---|
| Model | Tek seferlik, süresiz "BrainScroll Pro" (non-consumable). Abonelik yok, deneme yok. |
| Fiyat | $4.99 baz fiyat (kesin). Türkiye için KESİN olarak ayrı, belirgin biçimde daha ucuz bir fiyat uygulanır; mağazanın otomatik dönüşümüne bırakılmaz, iki mağazada da TR fiyatı elle girilir (bölüm 4). Diğer ülkelerde mağazanın otomatik dönüşümü/basamakları kullanılır. Kodda sabit fiyat yazılmaz. |
| Ad | "BrainScroll Pro" (kesin). |
| Ücretsiz günlük ipucu | 5 (kesin). |
| Family Sharing | Açık (kesin). |
| Seri kurtarma | Seri kopmuşsa ve Pro bunu geri getirecekse, tek seferlik kurtarma paywall'ı gösterilir (bölüm 7.3). Pro alınınca kopuk serinin geri gelmesi kabul edildi (bölüm 6). |
| Ek ürün | İpucu paketi (consumable), 2. aşamada (bölüm 11, Aşama B). Bakiye sunucuda tutulur. Mağaza ikonundan satılır (bölüm 7.4). |
| Mağaza | Ana sayfa, Akış ve Profil'in sağ üstünde mağaza ikonu; Pro ve ipucu paketleri tek yerden (bölüm 7.4). |
| Pro içeriği | Sınırsız ipucu, seri korumada daha çok hak (haftada 1, en fazla 3), destekçi rozeti. Kozmetik askıda (bölüm 8.5). |
| Reklam | Üçüncü taraf reklam yok. Kendi uygulama içi tanıtım sonraya, yalnızca ücretsiz kullanıcıya ve sadece Pro kartı olarak. |
| Ana ilke | Hiçbir oyun, günlük bulmaca, sıralama, arkadaş özelliği kilitlenmez. Pro yalnızca kolaylık ve görünüm satar. Rekabeti bozmaz: ipucu kullanılan sonuçlar zaten `assisted` olarak istatistik dışı, kozmetik oyuna etki etmez. |
| Gelecek özellikler | Mağaza metni ve uygulama yalnızca bugün var olan şeyleri vaat eder. Gelecekteki sunucu maliyetli özellikler (arşiv, ligler) Pro'ya dahil edilecek diye söz verilmez; o karar o zaman verilir (bölüm 12). |

## 3. Ücretsiz ve Pro karşılaştırması

| Özellik | Ücretsiz | Pro |
|---|---|---|
| Oyunlar, günlük bulmaca, favoriler, sıralamalar, arkadaşlar, zorluk seviyeleri | Hepsi | Hepsi |
| İpucu (guide) | Günde 3 | Sınırsız |
| Seri koruma | 15 günde 1 hak, en fazla 1 saklanır | Haftada (7 günde) 1 hak, en fazla 3 saklanır |
| Görünüm | Varsayılan görünüm | Aynı varsayılan görünüm (kozmetik askıda) |
| Rozet | Yok | Profilde "Pro" rozeti |

## 4. Ürünler ve mağaza tanımları

| Ürün | Kimlik | Tür | Aşama |
|---|---|---|---|
| BrainScroll Pro | `brainscroll_pro` | Non-consumable (Apple) / one-time product, "buy" seçeneği (Play) | A |
| İpucu paketi (küçük) | `brainscroll_hints_20` | Consumable | B |
| İpucu paketi (büyük) | `brainscroll_hints_75` | Consumable | B |

- Entitlement (hak): tek bir `pro` (RevenueCat kullanılırsa). İpucu paketleri entitlement değildir.
- Apple: "Aile Paylaşımı" (Family Sharing) AÇIK (karar verildi). Not: App Store Connect'te bir ürün için Family Sharing açıldıktan sonra kapatılamaz; açarak oluştur. Google Play'de aile kitaplığı uygulama/oyun satın alımları için ayrı bir geliştirici ayarıdır, Play Console'da ilgili seçeneği aç. RevenueCat, aile üyesine paylaşılan hakkı otomatik tanır; sandbox'ta test et.
- Aile paylaşımı yüzünden bir mağaza hesabı birden fazla uygulama hesabında Pro'yu tetikleyebilir; bu istenen davranış, ek önlem yok.
- Türkiye fiyatı (karar, 23 Kasım 2026): Türkiye'de Pro ve ipucu paketleri için ayrı, daha düşük fiyat kullanılır. App Store Connect'te ürünün fiyat programında Türkiye'yi elle ayarla (baz ülke fiyatından bağımsız); Play Console'da ürünün ülke bazlı fiyatında Türkiye'yi elle gir. Karar (24 Kasım 2026): Pro için Türkiye'de 129,99₺, diğer tüm ülkelerde $4,99 (mağazanın o ülke basamağı). İki mağazada da elle girilir. Paketler için de aynı kural: Türkiye'de 20'lik 39,99₺ ve 75'lik 99,99₺, diğer ülkelerde $0,99 ve $2,99 (karar, 24 Kasım 2026). Fiyat kodda sabit değildir, mağazadan gelir, bu yüzden kod değişmez.
- Çeviriler: İngilizce ve Türkçe başlık ve açıklama. Paywall'daki fiyat mağazadan gelen yerelleştirilmiş fiyattır, elle yazılmaz.
- Yenileme, deneme, iptal metni YOK; bunlar tek seferlik üründe yanıltıcı olur. Metin açıkça "Tek seferlik ödeme, süresiz" der.

## 5. Ücretsiz ipucu ekonomisi (ayrıntı)

### 5.1 Bugünkü kod
- `FeedViewModel.requestGuide(index)` (`ui/feed/FeedViewModel.kt:254`): sayfa ve `f.guideTask(index)` alınır, arka planda adım aranır. Adım bulunamazsa `guideNothing` yayınlanır. Bulunursa `f.markGuided(index)` çağrılır ve `GuideState` yayınlanır.
- Düğme: `Hud.kt` içinde `onGuide: (() -> Unit)?`, `GuideIconButton(onGuide)`. `FeedScreen.kt:204` bulmaca bittiyse `null` verir.
- Test etkisi: `markGuided` birçok testte doğrudan çağrılıyor (`FeedPersistenceTest`, `EndlessFeedTest`, `LevelsOf*Test` vb.). Kota mantığını `markGuided` içine KOYMA; yalnızca `FeedViewModel.requestGuide` çevresine koy. Böylece mevcut testler kırılmaz.

### 5.2 Kurallar
- Ücretsiz kullanıcı: yerel günde 3 ipucu (`GuideQuota.FREE_PER_DAY = 3`, 24 Kasım 2026'da 5'ten düşürüldü, tek yerde sabit). Pro: sınırsız. Aşama B'den sonra: önce günlük ücretsiz hak harcanır, bitince satın alınmış paket bakiyesinden düşer.
- Kota yalnızca gerçek bir adım gösterildiği noktada (yani `markGuided` çağrısının hemen yanında, `step != null` dalında) düşer. `guideNothing` kota harcamaz.
- Sıra önemli: kota kontrolü adım aranmadan ÖNCE yapılır (hak yoksa paywall açılır, boşuna arama yapılmaz), düşüş adım bulunduktan SONRA yapılır. Arama sırasında kullanıcı çift dokunursa iki arama ve iki düşüş olmasın: arama sürerken ikinci dokunuş yok sayılır (mevcut `viewModelScope.launch` için bir "arıyor" bayrağı ekle).
- Aynı sayfada aynı bulmaca için ikinci ipucu da bir hak harcar. Ekran yeniden çizildiği için aynı `GuideState` yeniden gösterilirse hak düşmez (düşüş yalnızca `requestGuide` içinde olur, gösterim sırasında değil).
- Günlük sıfırlama: cihazın yerel gece yarısı (günlük bulmaca Istanbul gün indeksine bağlı, ipucu kotası bundan ayrı; yurt dışındaki kullanıcıya garip saat vermemek için). Saat değiştirerek kandırılabilir, kabul edilir.
- Saf fonksiyon: `GuideQuota.state(day, used, pro, bonus) -> QuotaState(freeLeft, bonusLeft, unlimited)` ve `GuideQuota.spend(...)`. Birim testleri bölüm 10'da.

### 5.3 Arayüz
- Düğme kaybolmaz. Kalan ücretsiz hak düğmenin üstünde küçük rozet olarak görünür ("2"); Pro'da rozet yerine küçük bir "∞" ya da hiçbir şey (sade olan seçilsin).
- Hak bitince düğme sönük, ama dokunulabilir; dokunma paywall'ı açar (bölüm 7). Düğme gizlenmez, çünkü gizlemek sorun yaratır ("ipucu nereye gitti").
- Erişilebilirlik: rozetin içerik açıklaması ("2 ücretsiz ipucu kaldı"), sönük durumda "ipucu hakkı bitti, Pro ile sınırsız".
- Metinler her iki dilde `strings.xml` içinde.

### 5.4 Saklama
- Mevcut yerel ayar tablosu kalıbı (`GameRepository` içinde `settings.get/put`, `SETTING_*` sabitleri): `guide_quota_day` (yerel gün sayısı) ve `guide_quota_used`.
- Gün değişmişse okurken `used = 0` sayılır, yazma ilk harcamada yapılır.
- Dikkat: `wipeLocalData()` (hesap silme, `GameRepository.kt:215`) `settings.deleteAllExcept(...)` ile yalnızca dil, gizlilik ve ses ayarını korur. Hesap silmek kota sıfırlayarak bedava hak vermesin; `guide_quota_*` anahtarlarını bu listeye ekle. Pro önbelleği (bölüm 8.3) de listeye girmeli.

## 6. Seri koruma (Pro kuralı)

- `stats/DayStreaks.kt`: `FREEZE_EVERY = 7`, `MAX_FREEZES = 1`, `compute(finishedDays, today)`; dosya içinde "yerel taslak" notu var, kural `docs/decisions.md` Faz 6 maddesinde.
- Değişiklik: `compute(finishedDays, today, pro: Boolean = false)`. Sabitler bir `FreezeRule(every, max)` değeri; `FREE`/`PRO` olarak iki sabit; varsayılan ücretsiz.
- **Revizyon 23 Kasım 2026:** `FreezeRule.FREE = (every 15, max 1)`, `FreezeRule.PRO = (every 7, max 3)`. Önceki değerler 7/1 ve 4/3 idi. Etkileri:
  - Ücretsiz kural cömertlikten kısılıyor (7 → 15). Seri her zaman tüm geçmişten hesaplandığı için, yeni sürümü alan mevcut kullanıcıların serisi kısalabilir ya da kopabilir. Uygulama henüz yayında değilse (yalnızca TestFlight) sorun değil; yayında kullanıcı varsa serisi geriye dönük kısalmasın diye "eski kuralla hesapla" geçişi düşünülmeli. Yayın öncesi yapılıyorsa ek önlem gerekmez.
  - Ücretsiz ile Pro arasındaki fark büyüdü (15 gün / 1 hak ile 7 gün / 3 hak). `DayStreaks.rescue` daha sık dolu dönecek, yani seri kurtarma paywall'ı (7.3) daha çok kullanıcıya çıkabilir. Eşikler (en az 3 gün, kopuş başına 1 kez, günde 1 paywall) aynı kalır; bölüm 15'teki "Şimdi değil" ve bırakma oranlarına bak.
  - Seri bilgi sayfası (`StreakInfoSheet`) ve kilometre taşı (`DailyScreen.kt`, `rule.every`) metinleri/değerleri yeni sayılarla güncellenir; sayılar metinde sabit yazılmaz, kuraldan gelir.
- Çağıranlar: `DayStreaks.FREEZE_EVERY` doğrudan `DailyScreen.kt:256` içinde kullanılıyor (kilometre taşı). Kural parametre olunca orası da `rule.every` kullanmalı. `DayStreaks.compute` çağrılarının hepsini bul (`grep`) ve `pro` değerini ver.
- Seri geçmiş günlerden baştan hesaplanıyor. Sonuç:
  - Pro alınınca geçmiş de Pro kuralıyla yeniden hesaplanır. Yan etki: kopmuş bir seri, yeni kuralla "köprülenmiş" sayılıp geri gelebilir. KARAR: bu kabul edildi ve bilerek bir özellik olarak kullanılıyor (bölüm 7.3 "seri kurtarma"). `purchased_day` ya da iki kurallı hesap gerekmez, hesap her zaman tüm geçmiş üzerinden `pro` bayrağıyla yapılır.
  - Kurtarma iddiası sahte olmamalı: paywall'daki "serini geri getir" mesajı yalnızca `compute(..., pro = true).current > compute(..., pro = false).current` olduğunda gösterilir (yani Pro gerçekten seri kazandırıyorsa). Aksi halde kurtarma paywall'ı çıkmaz. Bu karşılaştırma saf fonksiyon olarak yazılır: `DayStreaks.rescue(finishedDays, today): Rescue?` (`Rescue(freeStreak, proStreak)` ya da `null`).
  - Abonelikteki "bitince seri kısalır" sorunu yok, Pro süresiz. Tek istisna iade (refund) ya da ihtilaf: o durumda seri ücretsiz kuralla hesaplanır, kısalabilir; kabul edilir.
- Testler: `DayStreaksTest` ve `DayStreaksProTest` yeni sayılara göre güncellenir (ücretsiz: 15. günde hak, en fazla 1; Pro: 7. günde hak, 3'e kadar birikim, kopma). Eski sayıların sabit yazıldığı beklenen değerler değişir; testler kural sabitinden türetilmişse değişmez.
- `docs/decisions.md` Faz 6 maddesindeki Pro farkı (şu an "4 günde 1 hak") kod değişince yeni sayılarla güncellenir: ücretsiz 15/1, Pro 7/3.

## 7. Paywall ve deneyim

### 7.1 Tetikleyiciler (biri hariç hepsi kullanıcının kendi dokunuşuyla)
1. Ücretsiz ipucu bitince ipucu düğmesi.
2. Profilde "BrainScroll Pro" kartı.
3. Seri koruma bilgi sayfası (`StreakInfoSheet`): "Pro ile haftada 1 hak, 3'e kadar saklama".
4. Mağaza ikonu (bölüm 7.4). Ücretsiz ipucu bitince ipucu düğmesi de aynı mağaza ekranını açar.
5. Seri kurtarma: seri kopmuşsa tek seferlik, kendiliğinden çıkan paywall (tek istisna, bölüm 7.3).
6. İleride: kendi tanıtım kartı (bölüm 12).

Asla (7.3'teki seri kurtarma hariç): ilk açılışta, bulmacanın ortasında zorla, ilk oyundan önce, geri tuşunda, oyunu kazandıktan hemen sonra araya girerek, ya da kapatılamayan şekilde.

### 7.3 Seri kurtarma paywall'ı (kendiliğinden çıkan tek paywall)
Karar: gün serisi kopmuşken kullanıcı oyuna girerse "serini Pro ile geri getir" paywall'ı gösterilir.

Çıkış koşulları (hepsi birden):
- Kullanıcı Pro değil.
- `DayStreaks.rescue(finishedDays, today)` boş değil: Pro kuralıyla seri gerçekten daha uzun (ör. ücretsizde 0, Pro'da 12). Pro seri kazandırmıyorsa hiç gösterilmez (yanıltıcı olmasın).
- Kurtarılacak seri anlamlı: Pro'nun geri getireceği seri en az 3 gün (kısa seri için rahatsız etme).
- Bu kopuş için daha önce gösterilmemiş: `streak_rescue_shown` ayarında kopuşu tanımlayan değer saklanır (kopmadan önce bitirilen son günün gün indeksi). Aynı kopuş için en fazla bir kez; kullanıcı kapatırsa o kopuş için bir daha çıkmaz. Yeni bir kopuş yeniden hak verir.
- Bugün başka bir paywall gösterilmedi (günde en fazla 1 kendiliğinden paywall).

Çıkış zamanı ve yeri:
- "Oyuna girerse" = Akış sekmesine ya da ana sayfadaki günlük karta ilk gelişinde, bir bulmaca BAŞLAMADAN önce. Bulmacanın ortasında ya da çözdükten hemen sonra asla.
- Soğuk açılışın ilk saniyelerinde çıkmasın; ana sayfa/akış yüklendikten ve kullanıcı bir şeye dokunduktan sonra çıksın.
- Telemetri kaynağı: `streak_rescue`.

İçerik:
- Başlık: "Serin koptu, geri getirebilirsin." Altında somut sayı: "Pro ile {n} günlük serin geri gelir." (n = `proStreak`) ve "Pro, haftada 1 seri koruma hakkı verir."
- Ana düğme "Pro'yu al", kolay görünen "Şimdi değil". Sahte aciliyet ve "son şans" dili yok.
- Satın alınca seri anında yeniden hesaplanır, ana sayfadaki şerit güncellenir, kısa kutlama gösterilir.
- Mağaza metnine "seri kurtarma" yazılmaz; tek madde "Daha fazla seri koruma hakkı" kalır (yanlış beklenti oluşmasın).

Tasarım notu: bu paywall kullanıcının kaybettiği şeye dokunduğu için baskı hissi verebilir. Sınırları (kopuş başına tek sefer, kapatılabilir, kısa seri için çıkmaz) bu yüzden bilerek dar tutuldu. Genişletme/daraltma kararı bölüm 15'teki verilere göre.

### 7.2 Ekran
- Tek ekran, alt sayfa (bottom sheet) ya da tam ekran; mevcut sheet kalıbı (`StreakInfoSheet`, `PersonalizeSheet`) örnek alınsın.
- Başlık, 3 madde (sınırsız ipucu, daha çok seri koruma, destekçi rozeti), tek büyük fiyat kartı (fiyat mağazadan: "Tek seferlik, süresiz"), ana düğme "Pro'yu al", altında "Satın alımı geri yükle", kullanım koşulları ve gizlilik bağlantıları.
- Kapatma düğmesi her zaman görünür ve ilk andan erişilebilir.
- Dürüstlük dili: "Oyunlar her zaman ücretsiz. Pro sana kolaylık ve görünüm verir ve uygulamayı geliştirmeme destek olur." Sahte aciliyet, geri sayım, "son fırsat" yok.
- Mevcut bileşenler ve `Motion` değerleri kullanılır. Azaltılmış hareket (reduce motion) ayarına saygı.
- Başarılı satın almada kısa kutlama (`CelebrateWin`/konfeti dili), sesli/haptik geri bildirim (mevcut `LocalHaptics`, `LocalSoundEffects`), Pro rozeti ve ipucu rozetinin "∞" olması anında görünür.
- Ücretsiz sürümde "Satın alma hatası", "iptal edildi", "bekleniyor" (ebeveyn onayı bekleyen, Apple "Ask to Buy") durumları ayrı ayrı ele alınır; bekleyen durumda mesaj verilir, kilit açılmaz, sonradan geldiğinde açılır.
- Profilde Pro olan kullanıcıya: "Pro aktif, teşekkürler" satırı ve "Satın alımı yönet" bağlantısı yok (abonelik yok); onun yerine "Satın alımı geri yükle" kalır.

### 7.4 Mağaza ikonu ve mağaza ekranı (revizyon, 23 Kasım 2026)
Yer:
- Yalnızca Ana sayfa, Akış ve Profil ekranlarında. İstatistikler, Favoriler, Sıralama, Arkadaşlar, Ayarlar ve oyun ekranlarında görünmez.
- Üst çubukta sağ üst köşe, seri göstergesinin SAĞINDA. Seri göstergesi bir ikon genişliği kadar sola kayar. Kodda üç ekranın üst çubuğu (`HomeScreen.kt` `StreakStrip`, `FeedScreen.kt`, `ProfileScreen.kt`) ortak bir `StoreButton` bileşenini kullanır; tek yerde tanımlanır, ekran başına kopya yazılmaz.
- Dar ekran ve büyük yazı tipinde seri göstergesiyle yan yana sığması kontrol edilir (seri sayısı üç haneye çıkınca bile taşmaz). 48dp dokunma alanı, içerik açıklaması "Mağaza".
- İkon vurgu renginde dolgulu yuvarlak bir düğmedir (soluk çizgi ikon fazla görünmez kalmıştı, 23 Kasım 2026). Bildirim noktası ve "yeni" etiketi YOK. Tek hareket: Ana sayfa ve Profil açıldığında yarım saniye sonra bir kez hafifçe sallanır (yaklaşık 0,5 sn, ±14°), tekrarlanmaz; Akış'ta hiç sallanmaz, azaltılmış hareket açıkken de sallanmaz.

İçerik (mağaza ekranı, `PaywallSheet` kalıbında alt sayfa, her zaman kapatılabilir):
- Pro olmayan kullanıcı: üstte Pro kartı (paywall içeriği, fiyat mağazadan, "Tek seferlik, süresiz"), altında ipucu paketleri (Aşama B: 20 ve 75). Bugünkü ücretsiz ipucu hakkı ve paket bakiyesi özetlenir. "Satın alımı geri yükle" altta.
- Pro kullanıcı: ipucu paketi bir işe yaramaz (zaten sınırsız), bu yüzden ikon Pro'da gizlenir; seri göstergesi eski yerine döner. (Karar noktası, bölüm 16.)
- Aşama A ile B arası: paketler B'de gelir. Yayından önce B de bitiyorsa ikon ve paketler birlikte çıkar. B gecikirse ikon yalnızca Pro kartı ve geri yükleme ile çıkar; paketler gelince aynı ekrana eklenir.
- Ücretsiz ipucu bitince ipucu düğmesi aynı mağaza ekranını açar (ayrı bir paywall yazılmaz). Paywall'ın "Pro: sınırsız" ve "20 ipucu" iki seçeneği bu ekranın kendisidir.
- Seri kurtarma paywall'ı (7.3) ayrı kalır; yalnızca Pro'yu anlatır, paket göstermez.
- Telemetri kaynağı: `store_icon`.

## 8. Teknik tasarım

### 8.1 Satın alım altyapısı
Öneri: RevenueCat (KMP SDK `purchases-kmp`, iOS'ta StoreKit, Android'de Play Billing'i tek API'de sarıyor). Alternatif: StoreKit 2 ve Play Billing'i doğrudan `expect/actual` ile yazmak; bu durumda makbuz doğrulaması için Supabase edge function gerekir, iade takibi sende olur, ilk sürüm için ağır.
- Başlamadan RevenueCat'in güncel fiyatı, ücretsiz kademe sınırı (aylık takip edilen gelir eşiği) ve KMP SDK sürümünü doğrula; bu bilgi değişmiş olabilir. Kaynaklar: [RevenueCat KMP kurulum](https://www.revenuecat.com/docs/getting-started/installation/kotlin-multiplatform), [purchases-kmp](https://klibs.io/project/RevenueCat/purchases-kmp).
- Tek seferlik ürün için de çalışır: entitlement `pro`, ürün `brainscroll_pro`.
- RevenueCat'in "restore behavior" ayarı (satın alımın hangi kullanıcı kimliğine taşınacağı) tek seferlik üründe önemli; bölüm 8.4'te ele alınıyor.

### 8.2 Uygulama katmanı
Ses ve titreşim katmanlarındaki kalıbı izle (`LocalHaptics`, `LocalSoundEffects`, `expect/actual`; örnek: `ui/sound/SoundEffects.kt`):
- `ui/pro/ProState.kt` (commonMain): `interface ProState { val isPro: StateFlow<Boolean>; suspend fun product(): ProProduct?; suspend fun purchase(): PurchaseResult; suspend fun restore(): RestoreResult }` ve `LocalPro` composition local. Varsayılan: ücretsiz, hiçbir şey yapmaz (önizleme ve test için sessiz).
- `PurchaseResult`: `Success`, `Cancelled`, `Pending`, `Failed(reason)`. `RestoreResult`: `Restored`, `NothingToRestore`, `Failed`.
- Masaüstü (jvm): her zaman ücretsiz, satın alım arayüzü gizli (bölüm 14'te kabul ölçütü).
- Kota, seri ve kozmetik `isPro` değerine bakar; hepsi saf fonksiyonlarla test edilir.
- Profil ekranına `PlusSection`/`ProSection` (`ui/profile/` içinde, `SoundSection.kt` ve `PrivacySection.kt` ile aynı kalıpta), ayarlar bölümlerinin yanına.
- ViewModel: `FeedViewModel` zaten çok şey tutuyor (hesap, kota). Kota ve Pro için yeni, küçük bir `ProViewModel`/`ProController` yaz; `FeedViewModel` ona yalnızca "kullanabilir mi / harca" sorusunu sorsun. Böylece feed testleri `ProController` yerine sahte bir sürüm verir.

### 8.3 Yerel önbellek (çevrimdışı)
- Son bilinen `pro = true` durumu yerel ayara yazılır (`pro_cached`). Uygulama çevrimdışı açılırsa kullanıcı Pro'sunu kaybetmesin.
- Tek seferlik satın almada bitiş tarihi yok, bu yüzden hoşgörü süresi gerekmez. Ama iade / ihtilaf durumunda RevenueCat `false` dönerse önbellek temizlenir (yalnızca "kesin false" yanıtı; ağ hatası önbelleği silmez).
- Açılışta: önbellek `true` ise hemen Pro olarak başla, SDK yanıtı gelince doğrula.
- Dikkat: `wipeLocalData()` listesine `pro_cached` ekle (bölüm 5.4). Ama hesap silme Pro'yu geri almaz; satın alım mağaza hesabına bağlıdır. Yeni anonim hesapta "Satın alımı geri yükle" veya otomatik geri yükleme Pro'yu getirir (8.4).

### 8.4 Hesap bağlantısı (en riskli yer)
- Uygulamada anonim Supabase hesabı + Apple girişiyle birleştirme var (`AccountMerge.kt`, `SupabaseAccountService`). Tek seferlik satın almada kullanıcı Pro'yu aldıktan sonra anonim hesap Apple hesabıyla birleşirse hak kaybolmamalı.
- Yaklaşım: RevenueCat `appUserID` olarak Supabase kullanıcı kimliği; hesap birleşince `logIn(yeniKimlik)` ile satın alımın taşınması. Test et: (a) anonim → satın al → Apple girişiyle birleştir, (b) yeni cihazda Apple giriş + geri yükleme, (c) hesap silme sonrası yeni anonim hesapta geri yükleme.
- RevenueCat'in "restore behavior" ayarını bilinçli seç: "Transfer to new App User ID" tek seferlik ürün için doğru çoğunluk seçimi (kullanıcı cihaz/hesap değiştirince hak taşınır). Bir mağaza hesabını birden fazla uygulama hesabına paylaştırmak istismara açık, ama bu uygulamada Pro'nun istismar değeri düşük; kabul edilir.
- Sunucu tarafı (isteğe bağlı, v1'de yok): RevenueCat webhook'u → Supabase edge function → `entitlements(user_id, pro, source, updated_at)`. Ancak sunucu tarafı özellik (arşiv, lig) çıkarsa gerekir; ekleme maliyeti 1-2 gün.

### 8.5 Kozmetik (ASKIDA, 23 Kasım 2026)
**Karar:** Şimdilik kozmetik yok. Ücretsizde de Pro'da da aynı, bugünkü varsayılan görünüm kullanılır. Pro'nun içeriği: sınırsız ipucu, daha çok seri koruma, rozet. Kodda bu iş zaten yapılmış durumda (`ui/theme/Look.kt`, `ui/profile/LookSection.kt`, `theme_accent` / `theme_amoled` ayarları, `LookTest`). Uygulayan oturum: seçiciyi (`LookSection`) profilden kaldırır/gizler, `Look` her koşulda varsayılan görünümü uygular; mantık ve testler silinmez, geri açmak ucuz kalsın (yeniden açılmayacağı netleşirse silinir). Saklı `theme_*` ayarları yok sayılır. Aşağıdaki metin askıdaki tasarımın arşividir; bölüm 7.1, 10 ve 14'te kozmetik artık geçerli değil.

- Tema şu an yalnızca koyu (`BrainScrollTheme(highContrast)`; `DarkColors` ve `highContrast()`). Renk ham değerleri `Color.kt` içindeki `Palette` nesnesinde, ekranlar yalnızca `Tokens.kt` içindeki anlamsal jetonları kullanıyor (bu kuralı bozma). Ekranlarda sabit renk kullanma.
- v1 kapsamı:
  1. **Saf siyah (AMOLED) tema**: arka plan jetonlarının (`Neutral900`'dan türeyenler) `0xFF000000` yapılması. Yüksek kontrast modu (HighContrast) ile birlikte çalışmalı; ikisi birden açıksa yüksek kontrast kazanır. Kontrast oranlarını kontrol et.
  2. **Vurgu rengi seçenekleri**: 4-5 renk (şu an ağırlıklı yeşil `Green500`; mavi/turuncu/sarı/kırmızı `Palette`'te hazır). Hangi jetonların "vurgu" olduğunu `Tokens.kt` içinde belirle; doğru/yanlış anlamı taşıyan renkleri (correct = yeşil, error = kırmızı) DEĞİŞTİRME, yoksa oyun anlamı bozulur ve renk körlüğü açısından zarar olur.
  3. Nonogram'da resme göre renk seçimi zaten var; uygulama vurgu rengini bu seçime karıştırma.
- Mascot kıyafeti v1'de yok, ileri sürüm; sanat işi gerektirir. Seçicide "yakında" yok, hiç göstermeyelim (söz vermemek için).
- Seçim yerel ayarda saklanır (`theme_accent`, `theme_amoled`). Pro değilse (iade vs.) varsayılan görünüm uygulanır, seçim saklı kalır, tekrar Pro olunca geri gelir.
- Kilitli seçeneklerin önizlemesi: dokununca renk 2 sn önizlenir ve altta "Pro ile kullan" düğmesi çıkar; zorla paywall açılmaz.

## 9. Gizlilik ve telemetri
- Uygulamada `telemetry` paketi ve `PrivacySection` var. Olaylar: paywall açıldı (kaynak: ipucu / profil / seri / mağaza ikonu / seri kurtarma), satın alma başladı, başarılı, iptal, hata, geri yükleme. Yalnızca kullanıcı analitiği açmışsa gönderilir.
- Satın alma bilgisini başka verilerle birleştirip kişisel veri üretme; RevenueCat'e giden tek kimlik Supabase kullanıcı kimliği (anonim olabilir).
- Mağaza gizlilik beyanları: App Store gizlilik etiketleri ve Play veri güvenliği formu "satın alımlar" verisini içerecek şekilde güncellenir. `site/privacy.html` içindeki politikaya RevenueCat'in (üçüncü taraf işleyici) satın alma işlemesi ve iade/geri yükleme açıklaması eklenir.

## 10. Test planı

### 10.1 Birim testleri (`commonTest`)
- `GuideQuota`: gün değişimi, 0/1/5 kullanım (5. kullanımdan sonra 6. engellenir), Pro sınırsız, `guideNothing` hak düşürmez, arama sürerken çift dokunuş tek düşüş, bonus bakiyesi (Aşama B) sırası (önce ücretsiz sonra bakiye).
- `DayStreaks`: testler yeni kurallara göre (ücretsiz 15/1, Pro 7/3); Pro kuralı yeni testler; Pro ile kopuk serinin geri gelmesi; `rescue(...)` gerçekten seri kazandırdığında dolu, kazandırmadığında ve seri kopuk değilken `null` döner.
- Seri kurtarma gösterim koşulları (saf fonksiyon): Pro değil, `rescue != null`, en az 3 gün, aynı kopuşta ikinci kez çıkmaz, yeni kopuşta yeniden çıkar, günde en fazla bir kendiliğinden paywall.
- `ProController` (sahte `ProState` ile): `isPro` değişince kota, seri ve tema tepki verir; önbellek: ağ hatası önbelleği silmez, kesin `false` siler.
- Kozmetik: askıda; `Look` Pro olsun olmasın varsayılan görünümü verir.
- Mevcut feed testleri kırılmamalı: kota yalnızca `FeedViewModel.requestGuide` yolunda, ve testlerde `LocalPro`/sahte denetleyici varsayılanı "kota işlemez" olabilir ya da testler kota testlerini kendi içinde kurar.

### 10.2 Arayüz ve elle test (cihazda)
- Küçük ekran, dar ekran, büyük yazı tipi, TR ve EN dilleri (`feedback-new-game-checklist` ile aynı kontroller): paywall, rozet, mağaza ikonu (seri göstergesiyle yan yana sığıyor mu).
- Erişilebilirlik: ekran okuyucu (TalkBack/VoiceOver) paywall sırası, kapatma düğmesi, fiyat okunuşu.
- Azaltılmış hareket açıkken kutlama ve geçişler.

### 10.3 Sandbox matrisi
| Senaryo | iOS (sandbox / StoreKit config) | Android (lisans test hesabı) |
|---|---|---|
| Satın alma → Pro açılır | ✔ | ✔ |
| Uygulamayı kapat-aç, çevrimdışı aç → Pro durur | ✔ | ✔ |
| Satın alma iptali (kullanıcı vazgeçti) | ✔ | ✔ |
| Bekleyen (Ask to Buy / yavaş ödeme) | ✔ | ✔ |
| Yeni cihaz/yeniden kurulum + "geri yükle" | ✔ | ✔ |
| Anonim → Apple birleştirme sonrası Pro durur | ✔ | n/a |
| Hesap silme → yeni anonim hesapta geri yükleme | ✔ | ✔ |
| İade → Pro kapanır, tema varsayılana döner | ✔ | ✔ |
| Aşama B: paket satın al, bakiye, uygulamayı sil/kur | ✔ | ✔ |

## 11. Aşamalar ve tahmini süre

### Aşama A: Pro (hedef 7-8 gün kodlama)
1. Mağaza hesap/sözleşme işleri ve ürün tanımları (paralel, 1-2 gün + onay bekleme). Bölüm 13.
2. `ProState` iskeleti, `LocalPro`, RevenueCat entegrasyonu, yerel önbellek, hesap eşleme (2 gün).
3. `GuideQuota` saf fonksiyon + testler, `FeedViewModel` kota akışı, düğme rozeti (1 gün).
4. Paywall ekranı, profil bölümü, geri yükleme (1.5 gün).
5. Seri koruma Pro kuralı + `DailyScreen` kilometre taşı + `DayStreaks.rescue` + seri kurtarma paywall'ı (bölüm 7.3) + testler (1 gün).
6. Kozmetik: askıya alındı (23 Kasım 2026), bu adım yok; yalnızca seçici arayüzden kaldırılır (bölüm 8.5).
7. Mağaza ikonu ve mağaza ekranı: `StoreButton`, üç ekranın üst çubuğu, seri göstergesini sola kaydırma, Pro kartı, ipucu düğmesinin mağazaya bağlanması (1 gün, bölüm 7.4).
8. Sandbox'ta uçtan uca deneme (1 gün).

### Aşama B: İpucu paketi (hedef 4-5 gün; Aşama A'dan sonra, A mağaza incelemesindeyken başlanabilir)
Karar: bakiye sunucuda (Supabase) tutulur; cihaz değişimi ve yeniden kurulumda kaybolmaz. Consumable'lar mağazadan "geri yükle" ile gelmez, bu yüzden bakiyenin tek doğruluk kaynağı sunucudur.

Tasarım:
- Tablolar: `hint_balance(user_id pk, balance int check (balance >= 0), updated_at)` ve `hint_credits(transaction_id pk, user_id, amount, created_at)`. Yeni göç dosyası `supabase/migrations/<tarih>_hint_balance.sql` (mevcut `account_merge`, `difficulty` göçleriyle aynı stil). RLS: kullanıcı yalnızca kendi satırını okur; yazma yalnızca sunucu fonksiyonlarıyla.
- Kredi (ekleme): bakiyeyi istemci değil sunucu artırır. RevenueCat webhook'u → Supabase edge function → `hint_credits` içinde `transaction_id` benzersiz (aynı webhook iki kez gelirse çift kredi yok, idempotent) → `balance += amount`. `supabase/functions/` klasörü henüz yok; bu iş ilk edge function'ı kurar (sırlar için `supabase secrets`). Webhook yetkilendirmesi: RevenueCat'in paylaşılan gizli başlığı edge function içinde doğrulanır.
- Harcama: istemci `spend_hint()` RPC çağırır (security definer, `update ... set balance = balance - 1 where balance > 0 returning balance`), atomik. İstemci yalnızca kalanı gösterir; başarısızsa ipucu verilmez.
- Çevrimdışı: günlük ücretsiz 5 hak çevrimdışıyken de çalışır. Bakiyeden harcama internet ister; çevrimdışıyken bakiyeli ipucu "bağlantı gerekli" der. Bakiyenin son değeri yalnızca göstermek için önbelleklenir, harcama hakkı vermez.
- Satın alma → bakiye gecikmesi: webhook birkaç saniye geç gelebilir. Satın alma sonrası arayüz "ipucu paketi ekleniyor…" der, kısa aralıkla (ör. 2 sn, en fazla ~20 sn) bakiyeyi sorgular; gelmezse "satın alımın işleniyor, birazdan gelir" der. İstemci bakiyeyi elle artırmaz.
- Hesap birleştirme (`AccountMerge.kt`, mevcut `account_merge` göçü): anonim hesabın bakiyesi Apple girişiyle birleşirken yeni hesaba toplanarak taşınır. Birleştirme SQL'ine `hint_balance` taşıması eklenir ve `supabase/tests` içine test yazılır.
- Hesap silme: bakiye silinir (`on delete cascade`). Hesap silme onayında "satın aldığın ipucu paketleri de silinir" yazılır.
- İstismar yüzeyi: istemcinin artırma yolu yok, yalnızca azaltma; bakiye negatif olamaz (`check`); aynı işlem kimliğiyle ikinci kredi reddedilir.
- Aile paylaşımı consumable'lar için geçerli değildir; bakiye hesaba özeldir.

Ürünler ve arayüz:
- Paketler: 20 ($0.99) ve 75 ($2.99), büyük pakette indirim net olsun. Pro sahibi bunları görmez (zaten sınırsız).
- Paketler mağaza ekranında (bölüm 7.4) Pro kartının altında durur. Pro almamış ve kotası bitmiş kullanıcıya ipucu düğmesi bu ekranı açar: "Pro: sınırsız" (öne çıkan) ve "20 ipucu". Birini dayatmaz; her zaman kapatılabilir.
- Harcama sırası: önce günlük ücretsiz 3 hak, bitince bakiye (`GuideQuota.spend`).
- Pro'ya sonradan geçen kullanıcının bakiyesi silinmez; Pro sınırsız olduğu için harcanmaz. İade/ihtilafla Pro kapanırsa bakiye yine kullanılabilir.
- Aşama B'yi A'dan ayırmanın nedeni: sunucu tarafı (edge function, webhook, göç, birleştirme) ayrı bir risk ve test yüzeyi; Pro'nun yayınını geciktirmesin.

### Aşama B: kodlandı (24 Kasım 2026), cihazda ve mağazada denenmedi
Kodlanan ve yerelde (PGlite, `commonTest`, `jvmTest`) test edilen:
- Sunucu: `supabase/migrations/20261125090000_hint_balance.sql`. Tablolar `hint_balance` ve `hint_credit` (tekil; plandaki `hint_credits` yerine). Uygulama tablolara doğrudan erişmez, yalnızca `my_hint_balance()` (oku) ve `spend_hint()` (bir düş, bakiye yoksa NULL) çağırır. `credit_hints(user, işlem, miktar)` yalnızca service role (webhook) içindir; aynı işlem kimliği ikinci kez gelirse `duplicate` döner, bakiye artmaz. `complete_account_merge` yeniden tanımlandı: anonim hesabın bakiyesi toplanarak taşınır, `hint_credit` satırları da taşınır (birleşmeden sonra gelen tekrar bildirimi çift kredi yapmasın). Hesap silinince ikisi de silinir.
- Webhook: `supabase/functions/revenuecat-webhook/` (`handler.ts` saf kurallar, `index.ts` Deno bağlantısı). Yalnızca `NON_RENEWING_PURCHASE` ve `brainscroll_hints_20` / `brainscroll_hints_75` ürünleri işlenir; Pro satın alımı, iade ve test olayları yok sayılır. Veritabanı hatasında 500 döner, RevenueCat yeniden dener (kredi idempotent). İade edilen paketin ipuçları geri alınmaz (harcanmış olabilir; kabul edilir). Hesap kimliği (`app_user_id`) UUID değilse (RevenueCat anonim kimliği) yok sayılır; bu yüzden uygulama, mağaza hesabı tanıyana kadar paket satmaz (`ProController.identified`).
- Uygulama: `HintBalanceApi` (Supabase RPC), `HintWallet` (bakiye, önbellek, `awaitCredit`: satın alımdan sonra 2 sn aralıkla en çok 20 sn bekler), `ProState.packs()/buyPack()`, mağaza ekranında paket satırları (`PaywallSheet`; Pro kartının altında, kurtarma ekranında yok), rozet ücretsiz ve satın alınmış ipuçlarının toplamını gösterir. Harcama sırası: önce günlük ücretsiz hak (yerel), bitince bakiye (sunucu). Bakiyeli ipucu adım bulunduktan sonra sunucuda düşülür; sunucu "yok" derse mağaza ekranı açılır, bağlantı yoksa "internet gerekiyor" uyarısı çıkar ve ipucu verilmez.
- Ürün kimlikleri `ProConfig.HINT_PACKS` ve `handler.ts` içinde aynıdır; biri değişirse öteki de değişmeli. Mağazada paket tanımlı değilse mağaza ekranında paket bölümü hiç görünmez (Aşama B "karanlık" yayınlanabilir).

Senin yapacakların (sırayla):
1. `npx supabase db push` (yeni göç). Ardından `supabase/tests` içinde `node verify_hints.mjs` ve `node verify_webhook.mjs`.
2. Rastgele uzun bir gizli değer üret ve ayarla: `npx supabase secrets set REVENUECAT_WEBHOOK_SECRET=<değer>`; `npx supabase functions deploy revenuecat-webhook` (JWT doğrulaması `config.toml` içinde kapalı, kimlik doğrulamayı işlev kendisi yapar).
3. App Store Connect'te iki tüketilebilir (consumable) ürün: `brainscroll_hints_20` ve `brainscroll_hints_75` (fiyatlar bölüm 16'da kesin: 20'lik $0,99 ve Türkiye'de elle 39,99₺; 75'lik $2,99 ve Türkiye'de elle 99,99₺). RevenueCat'te iki ürünü ekle (entitlement bağlama gerekmez).
4. RevenueCat > Integrations > Webhooks: adres `https://lxrfnaxecvofmgkdtvrn.supabase.co/functions/v1/revenuecat-webhook`, Authorization başlığı = 2. adımdaki değer, hem sandbox hem production olayları açık.
5. Sandbox'ta dene: paket al, bakiye birkaç saniyede gelsin; uygulamayı sil/kur, bakiye dursun; misafir hesapla paket al, sonra Apple girişiyle birleştir, bakiye taşınsın; uçak modunda bakiyeli ipucu "internet gerekiyor" desin.

## 12. Sonraya (abonelik yeniden değerlendirme ölçütleri)
Abonelik yalnızca şu şartlar sağlanınca konuşulur:
- Sürekli değer üreten en az bir özellik çıktı: geçmiş günlük bulmaca arşivi, antrenman modu + beceri istatistikleri ("beyin profili"), özel arkadaş ligleri. Bunların sunucu maliyeti vardır; bu yüzden abonelik gerekçesi olurlar.
- Pro sahiplerine bu özelliklerin ne olacağı en baştan kararlaştırılmış: seçenek (a) Pro'ya dahil (sevgili ama gelir kısıtı), (b) ayrı aboneliğe bağlı, Pro sahipleri için indirimli, (c) bir tutar karşılığı "yükselt". Pro'yu satın alanlara sonradan "bu da ücretli" demek güveni bozar; bu yüzden satın alma ekranında gelecekten söz edilmez ve karar şeffaf duyurulur.
- Veri: bölüm 15'teki göstergeler Pro'nun yeterince alıcı bulduğunu ya da bulmadığını gösteriyor.
- Arkadaş sınırı (fikir, 23 Kasım 2026): ücretsizde 5 arkadaş, Pro'da fazlası. ŞİMDİLİK YAPILMIYOR. Gerekçe: arkadaş eklemek uygulamanın büyüme döngüsü (her arkadaş yeni kullanıcı getirir) ve sınıra ilk takılan, en bağlı kullanıcıdır; "hiçbir arkadaş özelliği kilitlenmez" ilkesiyle (bölüm 2) çelişir. Pro'ya gerçek değer katan sosyal bir şey (özel lig, grup sıralaması) çıkınca, sınırı arkadaş SAYISINA değil o özelliğe bağlamak daha sağlıklı. Önce arkadaş sayısı dağılımı ölçülür (kaç kullanıcı 5'e yaklaşıyor).
- Kendi uygulama içi tanıtım (reklam) sadece ücretsiz kullanıcıya, yalnızca "Pro" ve başka kendi uygulamaların kartı; ilk gün hiç, sonra 15-20 kaydırmada bir, bulmaca ortasında asla.

## 13. Başlamadan kontrol et
- `git status`: 23 Ekim 2026'da kontrol edildi. Ses efektleri ve diğer işler commit'lenmiş (son commit `8c36b2d Add sound effects and nonogram reveal animation`), çalışma ağacında yalnızca iki izlenmeyen belge var (`docs/pro-plan.md`, `docs/ses-efektleri-plan.md`). Yine de başlamadan `git status` ve `git log -3` çalıştır; yeni bir değişiklik varsa ona dokunma. Commit atma (kullanıcı kendisi atar).
- `docs/plan.md` ve `docs/decisions.md`: para kazanma kararı yok; bu plan ilk kayıt. Karar özeti `docs/decisions.md`'ye eklensin (tek seferlik Pro, abonelik gerekçesi, seri kuralı farkı).
- `docs/plus-plan.md` (abonelik planı) silindi, bu plan onun yerine geçti. Başka belgelerde ona bağlantı yok (kontrol edildi).
- Mağaza işleri (kullanıcıda): Apple Developer üyeliği, Ücretli Uygulamalar Sözleşmesi, vergi ve banka bilgileri; Google Play satıcı (merchant) hesabı. Bunlar yayını bloke eder, uzun sürer, hemen başlasın.
- RevenueCat hesabı, proje, API anahtarları (iOS ve Android ayrı). Anahtarlar kullanıcıda; kodda yer tutucu bırak ve nereye yazılacağını söyle (sırların repoya girmediğinden emin ol).
- Mevcut testlerin yeşil olduğunu doğrula (başlangıç noktası).
- Aşama B için: `supabase/functions/` klasörü şu an yok (yalnızca `migrations`, `tests`, `config.toml`). Edge function kurulumu ve `supabase secrets` ilk kez yapılacak; Supabase CLI sürümünü ve proje bağlantısını doğrula.

## 14. Kabul ölçütleri
- Ücretsiz kullanıcı günde 3 ipucu alır; 4. dokunuşta paywall açılır; ipucu bulunamadığında kota harcanmaz; arama sürerken çift dokunuş tek hak harcar.
- Pro satın alınca ipucu sınırsız, seri koruma haftada 1 hak ve en fazla 3 (ücretsizde 15 günde 1 hak, en fazla 1), Pro rozeti görünür; görünüm ücretsizle aynı. Uygulama yeniden açılınca ve çevrimdışıyken de korunur.
- Satın alma iptali, bekleyen ve hata durumları anlaşılır mesajla ele alınır; hiçbirinde Pro açılmaz (bekleyen sonradan onaylanırsa açılır).
- "Satın alımı geri yükle" yeni cihazda hakkı geri getirir. Anonim hesap Apple girişiyle birleşince hak kaybolmaz. Hesap silip yeni anonim hesapta geri yükleyince hak gelir.
- İade sonrası ipucu kotası ve seri kuralları ücretsize döner.
- Masaüstünde (jvm) satın alım arayüzü görünmez ve çökme yok.
- Hesap silme sonrası ipucu kotası sıfırlanarak bedava hak üretmez.
- Paywall her zaman kapatılabilir. Kendiliğinden yalnızca seri kurtarma durumunda çıkar (7.3); ilk açılışta, bulmaca ortasında ya da bulmaca bitişinde asla.
- Seri kopmuş ve Pro seri kazandıracaksa, bulmaca başlamadan önce kurtarma paywall'ı bir kez çıkar; kapatılırsa aynı kopuş için bir daha çıkmaz; Pro seri kazandırmıyorsa hiç çıkmaz; satın alınca seri anında geri gelir.
- Tüm metinler İngilizce ve Türkçe, fiyatlar mağazadan yerelleştirilmiş gelir; "yenileme" ya da "deneme" ifadesi hiçbir yerde geçmez.
- Türkiye mağazalarında Pro (ve Aşama B'de paketler) diğer ülkelerin dönüşümünden belirgin biçimde ucuzdur; fiyat elle girilmiştir, iki mağazada da test hesabıyla doğrulanır.
- Mağaza ikonu yalnızca Ana sayfa, Akış ve Profil'de, seri göstergesinin sağında görünür; diğer ekranlarda görünmez; Pro'da gizlidir; dar ekranda ve büyük yazı tipinde seriyle birlikte taşmaz; ipucu düğmesi hak bitince aynı ekranı açar.
- Aşama B (ayrıca): paket satın alınca bakiye sunucuda artar (webhook idempotent, çift kredi yok); harcama önce günlük ücretsiz haktan, sonra bakiyeden olur; Pro'da bakiye harcanmaz; yeniden kurulumda ve yeni cihazda bakiye durur; anonim → Apple birleşmesinde bakiye toplanarak taşınır; çevrimdışıyken bakiyeli harcama engellenir ama günlük ücretsiz haklar çalışır.

## 15. Ölçüm (karar vermek için)
Yalnızca telemetri açıkken, anonim sayımlarla:
- Paywall görüntüleme / kaynağa göre dağılım.
- Görüntüleme → satın alma dönüşümü (hedef: tek yönlü bir önsezi ölçütü olarak yüzde 1-3 aktif kullanıcı; bu tahmindir, gerçek veri gelince güncelle).
- Ücretsiz ipucu bitimi: kaç kullanıcı günlük 5 hakkı bitiriyor. Çok az kişi bitiriyorsa limit çok cömert (3-4'e inebilir), çok kişi bitiriyorsa Pro'yu sert hissettiriyor olabilir.
- Seri kurtarma paywall'ı: gösterim sayısı, dönüşüm, "Şimdi değil" oranı ve o paywall sonrası uygulamayı bırakma oranı. Kullanıcıyı kaçırıyorsa daralt ya da kaldır.
- Hangi tetikleyici (ipucu / profil / seri / mağaza ikonu / seri kurtarma) en çok satın almaya götürüyor.
- Ücretsiz kullanıcıların arkadaş sayısı dağılımı (arkadaş sınırı fikrini ileride değerlendirmek için, bölüm 12).
- İade oranı.

## 16. Kararlar ve kalan açık sorular
Karar verildi (23 Ekim 2026): fiyat $4.99, ücretsiz günlük ipucu 5 (24 Kasım 2026'da 3'e düşürüldü), ad "BrainScroll Pro", kopuk serinin Pro ile geri gelmesi kabul (ve seri kurtarma paywall'ı eklendi, bölüm 7.3), Aşama B bakiyesi sunucuda, Family Sharing açık.

Karar verildi (23 Kasım 2026, revizyon): Türkiye için ayrı, daha ucuz fiyat (kesin); seri koruma ücretsizde 15 günde 1 hak ve en fazla 1, Pro'da haftada 1 hak ve en fazla 3; Ana sayfa/Akış/Profil'de sağ üstte mağaza ikonu, ipucu paketleri ve Pro buradan; kozmetik askıya alındı (ücretsiz ve Pro aynı görünüm); arkadaş sınırı şimdilik yok.

Karar verildi (24 Kasım 2026), bu bölümdeki açık soruların hepsi kapandı:
- Pro kullanıcıda mağaza ikonu gizli kalır (alınacak bir şey yok; durum profildeki altın PRO hapında görünür).
- Ücretsiz seri kuralı 7 → 15: yayından önce uygulanır, ek önlem yok. Şu an yalnızca test kullanıcıları var, serilerinin kısalması sorun değil.
- Seri kurtarma ekranının sıklığı ve eşikleri yayından sonraki ilk verilerle (bölüm 15) gözden geçirilir; şimdi değişmez. Eşikler aynen kalır: kurtarılacak seri en az 3 gün, kopuş başına 1 gösterim, günde en fazla 1 kendiliğinden paywall.
- Kozmetik askıda kalır (ücretsiz ve Pro aynı görünüm).
- Fiyatlar: Pro Türkiye'de 129,99₺, diğer ülkelerde $4,99. Paketler: 20 ipucu $0,99 (Türkiye 39,99₺), 75 ipucu $2,99 (Türkiye 99,99₺). Büyük paket ipucu başına belirgin ucuz: 39,99₺/20 = 2,00₺, 99,99₺/75 = 1,33₺.

## 18. Revizyon uygulama listesi (23 Kasım 2026)
Aşama A'nın kodlanmış hali üzerine yapılacak değişiklikler, hepsi küçük:
1. `FreezeRule.FREE = (15, 1)`, `FreezeRule.PRO = (7, 3)`; `StreakInfoSheet`, `DailyScreen.kt` kilometre taşı ve `strings.xml` (TR/EN) metinleri güncellenir; `DayStreaksTest`, `DayStreaksProTest` ve `docs/decisions.md` Faz 6 maddesi yeni sayılara göre düzeltilir (bölüm 6).
2. Kozmetik: `LookSection` profilden kaldırılır/gizlenir, `Look` varsayılanı uygular, `LookTest` güncellenir (bölüm 8.5).
3. `StoreButton` bileşeni, üç ekranın üst çubuğunda seri göstergesinin sağı, seri sola kayar; mağaza ekranı (Pro kartı, Aşama B'de paketler), ipucu düğmesinin hak bitince mağazaya bağlanması, `store_icon` telemetri kaynağı (bölüm 7.4).
4. TR fiyatı: mağaza panellerinde (kullanıcıda) elle girilir; kod değişmez.
5. Aşama B (paketler) bu revizyonun parçası değil; mağaza ekranı paketleri alacak yeri hazır bırakır.

## 19. Play Store'a çıkarken yasal metinlerde yapılacaklar (25 Kasım 2026'da not edildi)
Şu an `site/terms.html`, `site/privacy.html` (ve `en/` karşılıkları) iOS odaklı yazıldı. Android'i açarken sırayla:
1. **Hesap silme adresi (zorunlu).** Google Play, uygulama içi hesap silmenin yanında, uygulamayı yüklemeden de hesabın silinmesini isteyebileceğin bir web adresi ister (Play Console > Veri güvenliği formundaki "hesap silme URL'si"). Şimdi yalnızca uygulama içi düğme ve e-posta var. `site/delete-account.html` (TR/EN) yazılmalı: nasıl silinir, hangi veri silinir (ipucu bakiyesi dahil), süre. Gizlilik sayfası bölüm 5 bu adrese bağlanmalı.
2. **Veri güvenliği formu gizlilik sayfasıyla birebir uyumlu olmalı.** Formda toplanan veriler: hesap kimliği, oyun sonuçları, kullanıcı adı, satın alma geçmişi (ipucu bakiyesi, işlem numaraları), anonim kullanım sayıları ve çökme raporları. "Üçüncü taraflarla paylaşım": Supabase, RevenueCat, Google (giriş). Uyumsuzluk, uygulamanın reddine yol açar.
3. **Gizlilik politikası, Play Console'a girilecek adres:** `https://playbrainscroll.com/en/privacy` (ve TR adresi). Sayfa herkese açık ve giriş istemez olmalı (şu an öyle).
4. **Terms bölüm 2:** Google Play ifadesi zaten var ("Google Play koşulları da geçerlidir"). Yayından sonra Google Play'in kendi ödeme ve iade koşulları açıkça anılabilir. Play'in tüketici iade süreleri Apple'dan farklıdır; bölüm 5'teki "Google Play'de sipariş geçmişinden" cümlesi kontrol edilmeli.
5. **Terms bölüm 4:** Satın alma sağlayıcısı olarak "Apple veya Google" zaten yazılı. Google Play faturalama için ek bir cümle gerekmez, ancak Play'de ipucu paketlerinin "tüketilebilir ürün" olarak tanımlandığı ve satın alındıktan sonra onaylandığı (RevenueCat bunu yapar) doğrulanmalı.
6. **Hedef kitle ve yaş:** Play Console'da hedef yaş grubu seçilir. Gizlilik sayfası "13 yaş altına yönelik değil" diyor; Console'daki hedef yaş buna uymalı (18 yaş ve üstü ya da 13+ seçilmeli, "çocuklara yönelik" seçilirse Aileler politikası devreye girer ve gizlilik metni değişmek zorunda kalır).
7. **Uygulama bağlantısı doğrulaması:** `site/.well-known/assetlinks.json` şimdilik yalnızca debug imzasını içeriyor. Play uygulama imzası (Play Console > Uygulama bütünlüğü) SHA-256 parmak izi eklenmeli, yoksa davet bağlantıları Android'de uygulamayı açmaz.
8. **Ana sayfa:** "Yakında Google Play'de" rozeti gerçek mağaza bağlantısına çevrilir; SSS'deki "Hangi cihazlarda" yanıtı güncellenir.
9. **Tarih:** Metinler değişince iki dildeki "Son güncelleme" tarihi de değiştirilir.

## 20. Mağaza ekranında güven bölümü (25 Kasım 2026)
Paketlerin altına dört kısa güvence eklendi (`TrustNotes` in `PaywallSheet.kt`): ipucu ne yapar, abonelik değil (yenilenmez, habersiz ücret yok), vazgeçersen iade Apple/Google'dan, veri güvende (kart bilgisi görülmez, veri satılmaz). Hepsi bugün doğru olan şeyler; metinler `trust_*` dizgilerinde. Gizlilik ve Koşullar bağlantıları zaten ekranın altında.
- **Yorum ve puan şimdilik gösterilmiyor, uydurma gösterilmeyecek.** Yayında gerçek puan olmadığı için bir sayı ya da yorum yazmak yanıltıcı olur. Yayından sonra mağazanın gerçek puanı ve gerçek yorumlarından kısa alıntılar (yorum sahibinin izniyle) bir `ProConfig.RATING` gibi bir değerden okunup, yalnızca gerçek bir değer varsa gösterilebilir. Karar için ölçüt: en az yüz gerçek puan ve 4,5 üzeri ortalama. Değeri elle güncellemek gerekir, bayat kalmasın diye aylık kontrol notu düş.
- **Seri kurtarma ekranı** bu bölümü almadı (yer dar). "Tek seferlik ödeme" etiketi orada var; istenirse bir satır eklenir.

## 17. Başlangıç prompt'u (yeni oturum için)
> `docs/pro-plan.md` dosyasındaki BrainScroll Pro (tek seferlik) planının Aşama A'sını uygula. Önce 13. bölümdeki kontrolleri yap, çalışma ağacındaki başka değişikliklere dokunma, commit atma. İş sırasını 11. bölümden izle; RevenueCat anahtarları ve mağaza ürün tanımları bende, onları yer tutucuyla bırak ve nereye yazılacağını söyle. Kota mantığını `markGuided` içine değil `FeedViewModel.requestGuide` yoluna koy. Bittiğinde hangi kısımların sandbox'ta denendiğini ve hangilerinin mağaza hesabı beklediğini yaz. Aşama B'ye geçme. Seri kurtarma paywall'ı (bölüm 7.3) Aşama A'nın parçasıdır.
