# BrainScroll final test listesi: Android (cihazda, elle)

Bütün eski faz test listelerinin (Faz 8-17, Ana Sayfa yenilemesi) birleşimi ve **son tasarım kararlarına göre güncel hali**. Otomatik testler (JVM, 701 test) geçti; bu liste **gerçek cihazda** görülmesi gerekenler için. Her maddeyi yaptıkça kutuyu işaretle (`- [x]`). Farklı çıkan adımı not al (madde numarası + ne gördün).

> Güncel kararlar: akışta **"Bu akışta çözülen"** sayacı (rehbersiz kazanılanlar, kayıp sıfırlamaz); **hafıza ve sudokuda hata sınırı yok** (yalnız kelime ve mayın kaybedilebilir); ipucu butonu yok, rehber ampul ikonu; alttan açılan paneller; sıralamada filtre sheet'i; favorilerden "Tekrar oyna"; Google girişinde rastgele kullanıcı adı.

**İlerleme:** her bölümün başlığındaki sayıları kendin takip edebilirsin; bölüm bitince başlığın yanına ✅ yaz.


---

## 0. Hazırlık

- [ ] Son sürüm cihaza yüklü: `./gradlew :androidApp:installDebug` ya da APK. Eski sürümün üstüne kur (yarım oyunların ve geçmişin korunması da test edilecek).
- [ ] Elinde **iki Google hesabı** var (A: ana test hesabı, B: silme testi için harcanabilir).
- [ ] Mümkünse **ikinci bir cihaz** ya da uygulamayı silip yeniden kurma imkânı (birleştirme ve geri yükleme testleri için).
- [ ] Supabase panelinde SQL editörü açık (senkron, telemetri ve birleştirme kontrolleri için). Migration `20261011090000_daily_games.sql` uygulanmış olmalı.
- [ ] Sistem ayarları nerede: `Ayarlar > Erişilebilirlik`, `Ekran`, `Ses ve titreşim`.
- [ ] En az bir **küçük telefon** (ya da küçük ekran boyutu) ve bir normal telefon elinin altında.

---

## 1. Uygulama ikonu ve açılış

- [ ] Ana ekranda ve uygulama çekmecesinde uygulamanın ikonu **yeşil beyin maskotu, koyu zeminde** (varsayılan Android ikonu değil). Yuvarlak ve kare ikon şekillerinde de (başlatıcı ayarı) maskot kesilmeden duruyor.
- [ ] Son uygulamalar ekranında ve Ayarlar > Uygulamalar listesinde de aynı ikon.
- [ ] Uygulama açılıyor, ilk ekran Ana Sayfa. Karanlık tema (uygulama şimdilik yalnız koyu).

---

## 2. Ana Sayfa

- [ ] Üstte uygulamanın adı ve sağda **yalnız günlük seri** (alev). Akış sayacı (şimşek) burada **yok**. Alt gezinme çubuğu: Ana Sayfa / Akış / Profil.
- [ ] Günlük seri alevine dokununca açıklama paneli (alttan) açılıyor.
- [ ] **Günün bulmacası** kartı: başlık, günün oyunu (resim + ad), kelime günüyse sağda dil etiketi, açıklama ve **yalnız yazıdan oluşan yeşil buton** ("Bulmacayı çöz ›", oynarken "Devam et ›", bitince "Sonucu gör ›"). Buton çerçevesiz ve italik **değil**, sonunda küçük ok var. Kartın her yerine dokunmak günlük sayfayı açıyor.
- [ ] Bağlantı yokken kelime günü kartı yalnızca açıklama gösteriyor ("internet bağlantısı gerekir"), diğer oyunlar çevrimdışı oynanıyor.
- [ ] **Akışın** kartı: başlık ve sağında yeşil **"Düzenle ›"**, seçili oyunların ilk üçünün resmi ve adı, dördüncü yerde seçili oyunların devamını anlatan **"+N oyun"** karosu (dokununca bir şey olmuyor, tercih değiştirmiyor; hepsi üçten azsa karo yok), altta tek yeşil ana buton.
- [ ] Ana buton: yarım akış **yoksa** "Akışı başlat", **varsa** "Devam et". "Yeni akışı başlat" gibi ikinci bir buton ana sayfada **yok** (yeni akış Düzenle sayfasından başlatılıyor).
- [ ] Karta (başlık, resimler, boş yer) dokununca Akışı düzenle sayfası açılıyor; "Düzenle ›" da aynı yere gidiyor. Ana butona dokununca akış başlıyor/devam ediyor.
- [ ] Bir bulmacaya harf yazıp ana sayfaya dön: ana buton "Devam et" oldu, dokununca aynı bulmacaya yazdığın harflerle dönüyor.
- [ ] Resimler **gerçek oyun ekranlarına benziyor** (kelime tahtası, hafıza kartları, tüpler, mayın, sudoku, ışıklar, kayan bulmaca, hedef sayı, borular).
- [ ] "Bugün en hızlılar" günün bulmacasının altında, "Tümü" sıralamayı açıyor. Anonim hesapta giriş yapmaya çağıran not var.
- [ ] **Küçük telefonda** ve **büyük yazıyla**: kartlar taşmıyor, oyun adları tek satırda kesilerek sığıyor, düğmeler vurulabiliyor.

---

## 3. Akışı düzenle sayfası

- [ ] "Akışını oluştur", "Oynamak istediğin oyunları seç.", sağda yeşil "N oyun seçili". Altta sabit "Akışı başlat" düğmesi.
- [ ] Dokuz oyun iki sütunda kart olarak, kaydırınca hepsine ulaşılıyor, **kaydırma akıcı**. Seçili kartta yeşil çerçeve ve onay işareti, seçili değilde boş halka.
- [ ] Karta dokunmak oyunu açıp kapatıyor, sayı güncelleniyor, ana sayfadaki resimler değişiyor. Son açık oyun kapanmıyor ("En az bir oyun açık kalır.").
- [ ] Kelime dili: **Türkçe ve English birlikte seçilebiliyor**, biri her zaman açık. İkisi açıkken kelime bulmacalarının başlığında TR/EN rozeti var.
- [ ] "Akışı başlat": yarım bulmaca **yoksa** soru sormadan akış başlıyor; **varsa** "Yeni akış başlatılsın mı?" onayı çıkıyor (Vazgeç: hiçbir şey olmuyor; onay: yarım bulmaca siliniyor, akış ilk sayfadan başlıyor, ana sayfadaki "Devam et" kayboluyor).
- [ ] Kapattığın oyunlar akışta yeni sayfalarda gelmiyor, başlamış bulmaca kalıyor. Uygulamayı kapat-aç: oyun ve dil seçimleri duruyor; yeni kurulumda bütün oyunlar seçili.
- [ ] Geri okuna, sistem geri tuşuna ya da geri hareketine basmak Ana Sayfa'ya dönüyor.

---

## 4. Akış (ortak)

Her oyunun kendi bölümü aşağıda (8). Burası bütün sayfalar için ortak olanlar.

- [ ] Sıra: **kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, kayan bulmaca, hedef sayı, boru bağla**, sonra başa dönüyor (seçili oyunlara göre).
- [ ] Üst çubuk: geri oku, oyun adı, **?**, sağda **ampul** ve yanında **şimşek + sayı**. Kelime bulmacasında dil rozeti (TR/EN) başlığın **altında** küçük yazı. Ampul ve şimşek **ekran dışına taşmıyor**, hiçbir oyunun oyun alanının üstüne binmiyor. **Küçük telefonda** başlık gerekirse "…" ile kısalıyor ama ampul ve sayı görünüyor.
- [ ] Bitmemiş bulmacadan **serbestçe kaydırılıyor** (atlama hakkı yok), geri gelince kaldığı yerden devam. Dikey kaydırma tahtalar üzerinden de sayfayı değiştiriyor (hücreler kaydırmayı yutmuyor).
- [ ] Alt menü akışta da görünüyor, "Akış" seçili. Alt menüden Akış'a basınca: yarım bulmaca varsa ona dönüyor; son bulmaca bitmiş ve yarım yoksa seçili oyunlardan yeni akış kuruluyor.
- [ ] ?: o oyunun kurallarını **alttan kayarak açılan** panelde gösteriyor (bölüm 7).
- [ ] Şimşek rozetine dokununca "Bu akışta çözülen" açıklama paneli açılıyor (bölüm 5).
- [ ] Büyük yazıda üst çubuk, ampul ve alt çubuk kesilmiyor.

---

## 5. "Bu akışta çözülen" sayacı

Karar: rehbersiz kazanılan bulmaca sayısı. Kayıp ya da yarım bırakma sıfırlamaz.

- [ ] Akışın üst çubuğundaki sayı akış başında **0**.
- [ ] Bir bulmacayı **rehbersiz kazan**: eski sayının üstünde büyükçe **"+1"** yükselip sönüyor, sayı bir artıyor ve rozet kısa bir "pop" yapıyor.
- [ ] **Kaybet** (kelimede 6 hak bitir ya da mayına bas): sayı **sıfırlanmıyor**, olduğu gibi kalıyor.
- [ ] Yarım bırakıp kaydır: sayı değişmiyor. **Rehberle** çözülen bulmaca: sayı **artmıyor**, "+1" çıkmıyor.
- [ ] Ana ekrana çıkıp geri dön: sayı duruyor. Alt menüden Akış'a dönünce son bulmaca bitmişse kurulan **otomatik yeni akış** sayıyı **koruyor**.
- [ ] Düzenle sayfasından **bilerek yeni akış başlat** ya da favoriden **Tekrar oyna**: sayı **0**'a dönüyor.
- [ ] Uygulamayı **tamamen kapat-aç**: sayı 0'dan başlıyor (yarım oyunlar geri geliyor ama sayı sıfır).
- [ ] Şimşek rozetine dokununca açıklama paneli: başlık "Bu akışta çözülen", "Şu an" ve "En iyi" değerleri, dört madde (rehbersiz sayı, kayıp/yarım sıfırlamaz, rehberle çözülen sayılmaz, yeni akış/yeniden açılış sıfırlar). Metin doğru dilde.
- [ ] **Reduce motion** açıkken sayı hemen değişiyor, "+1" animasyonu yok.
- [ ] Profil'de **"Akış rekoru"** kutusu tek akışta en çok çözülenin değeri; rekor kırınca güncelleniyor, uygulamayı kapat-açınca duruyor. Yeni bir akış başlatınca rekor **düşmüyor**.

---

## 6. Rehber (ampul)

Yalnız akışta (günlük bulmacada yok).

- [ ] Ampul ikonu üst çubukta, streak rozetinin solunda; **bitmiş bulmacada kayboluyor**. İpucu butonu artık **yok**.
- [ ] Ampule bas: tahtanın **altındaki sabit yerde** cümle çıkıyor (sağında **çarpı**), cümle yumuşakça beliriyor; **oyun alanı boyut değiştirmiyor, takılma yok**. Cümle halkalı hücrenin/kartın/tüpün üstünü kapatmıyor.
- [ ] Çarpıya basınca cümle kayboluyor. Bir hamle yapınca eski adım ve halka kayboluyor, tekrar ampule basınca sonraki adım geliyor.
- [ ] Halka: hücre/kart/tüpün **kendi şeklini** izliyor (özellikle **tüplerde**: üstte hafif köşeli, altta yuvarlak), yanıp sönüyor. Hareketi azalt açıkken sabit.
- [ ] Cümleler sığıyor: **tüplerde** "Halkalı tüpe dokun, sonra halkalanan tüpe dök." gibi kısa; uzun cümle en fazla 4 satırda "…" ile kesiliyor.
- [ ] Mayın: ilk basışta ortadaki hücre, sonra güvenli hücre / bayrak konacak hücre; kesin hamle yoksa "Şu an kesin bir hamle yok".
- [ ] Sudoku: tek rakam sığan hücre ya da rakamın tek yeri; yanlış rakam varsa önce silmeni söylüyor.
- [ ] Tüpler/hedef sayı **çıkmaza girdiysen**: "takıldın / hedefe artık ulaşılamıyor" cümlesi ve **"Baştan başla ›"** butonu; butona basınca bulmaca başlangıca dönüyor, tekrar sorunca yeni adım geliyor.
- [ ] Hedef sayı: "3 × 4 işlemini yap" gibi, doğru iki sayı halkalı. Kayan bulmaca, boru, ışıklar, hafıza ve kelime de adım veriyor (kelime: "3. harf A"). Kayan bulmacada zor konumda rehber bir an düşünebilir; ekran donmamalı.
- [ ] Rehberi kullandığın bulmaca: sayaç artmıyor, o oyunun istatistiklerinde ve en iyi sürede görünmüyor. Uygulamayı kapat-aç: yine böyle. ("Rehber kullanıldı" yazısı kutuda **yok**; bu bilgi şimşek açıklama panelinde.)

---

## 7. Alttan açılan paneller (Nasıl oynanır, seri bilgisi, filtreler)

- [ ] **Nasıl oynanır** (?) ve **seri açıklaması** (alev/şimşek) ve **Sıralama filtreleri** panelleri **alttan kayarak açılıyor**, kapanırken aşağı kayıyor (düz bir pencere gibi değil). Arkası **kararıyor**.
- [ ] Karartma **alt sekme çubuğunu**, **durum çubuğunu** (üstteki saat/pil alanı) ve alttaki **gezinme çubuğunu** da kaplıyor; hiçbiri parlak kalmıyor.
- [ ] Kapatma: "Anladım" düğmesi, panelin dışına dokunmak ve Android geri tuşu/hareketi. "Kapatmak için dışına dokun" yazısı **yok**. Geri tuşu/hareketi uygulamadan ya da akıştan çıkmıyor, yalnız paneli kapatıyor.
- [ ] Panel açıkken arkadaki oyuna dokunmak/kaydırmak çalışmıyor, kelime klavyesi yazdırmıyor.
- [ ] Büyük yazıda panel taşmıyor, adımlar kaydırılabiliyor, "Anladım" görünür kalıyor.
- [ ] Onay kutuları (yeni akış, çıkış, hesabı sil) da tüm ekranı (alt çubuk ve sistem çubukları dahil) karartıyor.
- [ ] Kural metinleri güncel: kelime 6 deneme; hafıza 6 çift ve **hata yalnız sayılır**; 7 tüp; 10 mayın; sudoku **hata sınırı yok**; ışıklar, kayan bulmaca, hedef sayı, boru bağla kuralları doğru.
- [ ] TalkBack: ? düğmesi "Nasıl oynanır?" diye, panel başlığı başlık olarak okunuyor.

---

## 8. Oyunlar

Her oyun için ortak kontroller: akışta sırası, yarım kalan oyun (uygulamayı kapat-aç: aynı tahta, aynı sayaçlar; bitirdiğin tahta geri gelmiyor), kalp ile favori, süre makul (eylemler arası üst sınır: tüpler/mayın/ışıklar/kayan/hedef/boru 15 sn, hafıza 10 sn, sudoku 30 sn).


### 8a. Kelime bulmacası

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 5 harfli kelime, 6 deneme. Geçerli tahmin: karolar sırayla (soldan sağa, ~0,1 sn arayla) dönüp renkleniyor. Eksik harf: "Yeterli harf yok" mesajı + satır sarsılıyor. Listede olmayan kelime: uyarı.
- [ ] **Klavye (TR ve EN):** "Sil" tuşu (**kırmızı**) ikinci satırın sonunda, "GİR/ENTER" (**ana buton yeşili**) üçüncü satırın sonunda ve silmenin hemen altında. İkisi de harf tuşlarıyla **aynı yükseklikte**, klavyeyle bitişik, aralarında boşluk yok; Enter 3,4 tuş genişliğinde, üçüncü satırda solda boşluk kalmıyor. Türkçe klavyede Ğ Ü Ş İ Ö Ç var.
- [ ] **Silme tuşuna basılı tut:** ilk dokunuşta bir harf siliyor, ~0,4 sn sonra hızla arka arkaya silmeye devam ediyor, bırakınca duruyor. Basılı tutunca tek bir hafif titreşim.
- [ ] Harf tuşları tahmin sonrası durumunu gösteriyor (doğru/başka yerde/yok); doğru harfin yeşili ile Enter'ın yeşili karışmıyor (Enter geniş ve sağ altta).
- [ ] Kazanma: başarı titreşimi, sayaç +1, konfeti. Kaybetme (6 yanlış): uyarı, başarı yok, **sayaç sıfırlanmıyor**.
- [ ] Dil: iki dil açıksa rozet var, her bulmaca kendi dilinde; ilk harf yazılınca o bulmaca kilitleniyor.
- [ ] Fiziksel klavye (varsa Bluetooth): harf, Enter, Backspace çalışıyor.
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8b. Hafıza eşleştirme

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 12 kart (4x3), 6 şekil, her biri iki kez. Başlıkta "Hamle 0" ve "Hata: 0" (sınırsız sayaç, `/6` **yok**).
- [ ] Karta dokun: yatay dönerek açılıyor, şekil + sarı çerçeve. İki aynı kart: yeşil dolgu, kısa pop, hamle +1. İki farklı kart: "Hata: 1", ~1 sn sonra kapanıyor, uyarı titreşimi.
- [ ] Beklemeden başka karta dokun: yanlış çift hemen kapanıyor, yeni kart açılıyor. Yanlış çiftten birine tekrar dokunmak yeni hamlenin ilk kartı. Açık/eşleşmiş karta dokunmak bir şey yapmıyor.
- [ ] Hata sayacı 0'dan büyükse **kırmızı**. **Hata sınırı yok**: 6'dan fazla hata yapınca oyun **bitmiyor**, kaybetmek yok. Sonuç yalnız kazanma: "Tüm çiftler bulundu!", "N hamle · mm:ss".
- [ ] Yarım kalan oyun: eşleşmiş kartlar eşleşmiş, açık kalan tek kart kapalı, sayaçlar aynı.
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8c. Renk tüpleri

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 7 tüp (üstte 4, altta 3 ortalı), 5 dolu, 2 boş; her renkte **şekil** var. Hiçbir tüp başlangıçta tek renkle dolu değil.
- [ ] Dolu tüpe dokun: yukarı kalkıyor, sarı çerçeve. Aynı tüpe tekrar: iniyor. Uygun hedefe dokun: üstteki aynı renk grubu geçiyor (sığdığı kadar), "Hamle" +1. Uygunsuz hedef: dökülmüyor, dokunduğun tüp yeni seçim.
- [ ] **Dökme animasyonu (~1 sn):** tüp hedefin üstüne gelip eğiliyor, akıntı hedefe doluyor. **Aynı renkten iki kare (ör. iki yıldız) dökülürken hedefte önce tek yıldız büyüyüp sonra ikinciye geçmiyor**: ilk yıldız dolarken ikincisi büyüyor, sonunda ani değişim yok.
- [ ] Tamamlanan tüpte küçük konfeti; **kazanma anında tüplerde ekranı büyütüyormuş gibi bir "pop" yok**.
- [ ] Geri al / yeniden başlat büyük yuvarlak ikon butonları: hamle sayısı **düşmüyor**, uygun olmayınca soluk. Animasyon sürerken çalışmıyorlar.
- [ ] Kazanma: tüpler yeşil çerçeveli, "Tüm renkler ayrıldı!". Kaybetme yok (çıkmazda geri al / baştan başla).
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8d. Mayın tarlası

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 8x8, "Mayın 10". Uzun bas: bayrak (sayaç azalıyor, orta titreşim). **Bayrak modu** düğmesi sarı çerçeveli; açıkken dokun = bayrak, uzun bas = aç. **İlk dokunuş güvenli**, her tahta tahminsiz çözülebilir. Sayıya çift dokun = chord (yanlış bayrakla chord mayın açar).
- [ ] **Kaybetme (mayına basma):** bastığın mayından başlayarak **diğer mayınlar sırayla, en yakından uzağa, kısa aralıklarla** patlıyor (her patlama küçük bir flaş + halka + birkaç kıvılcım, abartısız); mayınlar patlama sırasıyla görünüyor; kısa hafif sarsıntı. Toplam süre uzamıyor (çok mayında aralık kısalıyor). Uyarı titreşimi bir kez, **sayaç sıfırlanmıyor**.
- [ ] Kazanma: kalan mayınlar bayraklı, "Tahta temizlendi!", konfeti, başarı titreşimi, sayaç +1.
- [ ] Yarım kalan oyun: aynı tahta, açık alan ve bayraklar, bayrak modu kapalı.
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8e. Sudoku

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 9x9, ~36 dolu hücre, 3x3 kutular arasında belirgin boşluk; küçük telefonda sığıyor. Üstte "Hata: 0" çipi (**sınırsız sayaç, `/3` yok**; sıfırdan büyükse kırmızı).
- [ ] **Numpad:** üstte **tek satırda 1-9** (yüksek, rahat vurulan tuşlar), altında iki **yuvarlak ikon butonu**: kalem (notlar) ve silgi. Notlar açıkken kalem butonu sarı çerçeveli. Butonlar renk tüplerindeki geri al/yeniden başlat ve mayındaki bayrak butonuyla **aynı görünümde**.
- [ ] Boş hücre seç: sarı çerçeve, aynı satır/sütun/kutu açık zemin. **Doğru** sayı yerleşiyor (kalıcı, orta titreşim). **Yanlış** sayı: hücre kırmızı, "Hata" +1, uyarı; silgi ya da başka sayı ile düzelir. Aynı yanlışı tekrar basmak hata artırmıyor.
- [ ] **Hata sınırı yok**: 3'ten fazla yanlış yapınca oyun bitmiyor, kaybetmek yok. Sonuç yalnız kazanma ("Sudoku çözüldü!").
- [ ] Notlar: sayı tuşu hücrede küçük not (1 sol üst, 9 sağ alt); doğru sayı koyunca aynı satır/sütun/kutudaki o sayının notları siliniyor. Silgi yanlış sayıyı ya da notları siliyor. Bir sayı 9 kez yerleşince tuşu soluyor.
- [ ] Bir satır/sütun/kutu tamamlanınca hücrelerde kısa bir yeşil **dalga** (hücreler hafif büyüyüp yerine oturuyor).
- [ ] Yeni tahta açılırken takılma yok. Zorluk keyifli mi (not al).
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8f. Işıkları söndür

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 5x5, "Hamle 0", "En çok 7". Yanan ışık sarı dolgu + koyu daire, sönük: soluk halka. Dokunuş o ve 4 komşusunu çeviriyor (kenarlarda olmayan komşu yok sayılıyor), hamle +1, aynı ışığa ikinci dokunuş geri çeviriyor ama hamle artıyor.
- [ ] Baştan başla (soluk/aktif), hamle sayacı sıfırlanmıyor. "En çok 7" ile çözmek mümkün. Kaybetmek yok. Kazanma: "Tüm ışıklar söndü!".
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8g. Kayan bulmaca

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 3x4, 11 taş. Yerinde olan taşlar yeşil. Boşluğun yanındaki taş kayıyor; aynı hizadaki uzak taşa dokununca arada kalanlar **birlikte** kayıyor, tek hamle. Hizada olmayan taşa dokunmak bir şey yapmıyor. Hızlı dokunuşta animasyon takılmıyor.
- [ ] Baştan başla hamle sayısını sıfırlamıyor. Her tahta çözülebilir. Kazanma: "Bulmaca çözüldü!".
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8h. Hedef sayı

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] Hedef kartı (10-99, sarı), 4 sayı taşı (1-9), + − × ÷ tuşları, geri al / baştan başla. "Hamle" ve "En az N".
- [ ] Sayı seç → işlem seç → ikinci sayı: sonuç taşı oluşuyor, "7 + 4 = 11" satırı, hamle +1. Geçersiz işlemler (negatif/sıfır, kalanlı bölme) soluk, dokununca uyarı. Hepsini kullanmak zorunda değilsin. Son üç işlem listede.
- [ ] Geri al hamleyi düşürmüyor, baştan başla hamleyi sıfırlamıyor. 4 basamaklı sonuçlar (6561) taşa sığıyor. Kazanma: "Hedefe ulaştın!".
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

### 8i. Boru bağla

- [ ] Akışta doğru sırada geliyor, başlıkta oyun adı ve "Nasıl oynanır?" (?), tahta küçük telefonda da ekrana sığıyor ve parmakla rahat kullanılıyor.
- [ ] 5x5, "Hamle 0", "En az N" (genelde 25-40). Buluşan yönler yeşil ve komşuyla kesintisiz, açık uçlar gri ve ucunda halka. Karoya dokun: saat yönünde 90° dönüyor (kısa animasyon), hamle +1; hızlı dokunuşta takılma yok.
- [ ] Baştan başla animasyonsuz, hamleyi sıfırlamıyor. "En az N" gerçekten N dokunuşla çözülüyor. Kazanma: "Tüm borular bağlandı!". Yeni tahta takılmadan geliyor.
- [ ] Oyun ortak kontrolleri: yarım kalan oyun doğru geri geliyor, kalp ile favori (Profil > Favoriler'de satır), "Ana Sayfa > Düzenle" ile oyun kapatılınca yeni sayfalarda gelmiyor, hesaplı: sonuç Supabase'e gidiyor (bölüm 14), ekran okuyucu ve büyük yazı ile kullanılabiliyor.

---

## 9. Günlük bulmaca (günün oyunu, rotasyon)

- [ ] Ana sayfa kartında günün oyunu; **ertesi gün başka oyun**, 9 gün dolmadan tekrar yok. İki telefonda aynı gün aynı oyun ve aynı tahta.
- [ ] Kelime günü: Başla, süre sunucu saatiyle, tahmin, sonuç, sıradaki bulmaca geri sayımı. Dil **uygulama dilinde**, dil anahtarı yok.
- [ ] Diğer oyun günü: karta dokununca oyun, başlıkta oyun adı + "Bugünün bulmacası", ?, alev. Yarım bırakıp çık: kaldığı yerden devam. Bitince "Bugünlük bu kadar…" (kaydır ipucu yok), kazanınca konfeti, gün serisi artıyor.
- [ ] Uygulamayı kapat-aç: bitmiş bulmaca için sonuç kartı ve geri sayım. Uçak modunda bitir: oyun çalışıyor, bağlantı gelince sonuç sıralamada görünüyor.
- [ ] Günlük bulmacada ampul/rehber **yok**. Gün serisi: her bitiş sayılıyor (kaybetsen de); 7. günde konfeti, her 7'nin katında dondurma hakkı.
- [ ] Kazanınca konfeti, kaybedince yok; çıkıp girince **tekrar kutlanmıyor**.

---

## 10. Sıralama

- [ ] Başlık geri okunun yanında. Üstte **alt çizgili sekmeler**: Hız / Seri / Arkadaşlar (seçili sekmenin altında kısa yeşil çizgi). Eski "yuvarlak segment" görünüm **yok**.
- [ ] Sekmelerin altında **tek satır özet** (ör. "Bugün · Kelime bulmacası · Türkçe", seri sekmesinde "Güncel"; arkadaş filtresi açıksa "Arkadaşlar" da). Satıra basınca **alttan filtre paneli** açılıyor (dönem, oyun/dil, güncel/en uzun, herkes/arkadaşlar), seçimler anında uygulanıyor, "Anladım" kapatıyor.
- [ ] Hız > Bugün: günün oyununun tablosu (kelime değilse hamle, kelimeyse tahmin + süre). Tüm zamanlar: oyun seçici, kelime seçilince dil seçici. Satırlar tek blok, benim satırım vurgulu "(sen)".
- [ ] **Seri** sekmesi **günlük seriyi** sıralıyor (akıştaki sayı burada yok): Güncel / En uzun.
- [ ] Anonim hesap yalnız bakabiliyor, giriş yapmaya çağrılıyor. Çevrimdışı: son görüntü "Çevrimdışı" notuyla.
- [ ] **Kullanıcı adı:** Google/Apple ile giriş yapınca **rastgele bir ad otomatik** veriliyor (ör. `redpanda051`, renk+hayvan+üç rakam) ve "Kullanıcı adın … Ayarlardan değiştirebilirsin" bildirimi çıkıyor; sıralamada bu adla görünüyorsun. İsim alınamazsa (ağ hatası) eski "Kullanıcı adı seç" kartı çıkıyor.
- [ ] Arkadaşlar: davet kodum + "Kopyala", kodla arkadaş ekle, kullanıcı adıyla istek, kabul/reddet/çıkar (48 dp dokunma alanı), arkadaş filtresi. Davet linki (`https://playbrainscroll.com/invite?code=...`) uygulamayı açıyor; uygulama yokken sayfa kodu gösteriyor.

---

## 11. Profil

- [ ] **Hesap kartı:** Google/Apple ile girişliyken harf simgesi, **kullanıcı adı** ve altında "Google ile giriş yapıldı"; kart **dokunulabilir değil** (Ayarlar'a gitmiyor, "her şey yedeklendi" yazısı yok). Misafir/çevrimdışıysa dokununca Ayarlar'a (giriş için) gidiyor.
- [ ] İki kutu: **Günlük seri** (alev, şu an + en iyi; dokununca açıklama) ve **Akış rekoru** (şimşek, tek akışta en çok çözülen; dokununca açıklama, "Şu an" satırı yok). Altında üç özet sayı (oynanan, kazanma %, kazanılan) ve liste: İstatistikler, Favoriler, Sıralama, Ayarlar.
- [ ] **İstatistikler:** başlık geri okunun yanında; üstte üç özet sayı; altında **oyun başına tek satır** (ad + oynanma sayısı + ok). Satıra basınca **kayarak açılıyor**, ok dönüyor: günlük (dil filtresi İkisi/EN/TR, tahmin dağılımı, gün serisi, en iyi süre), akış (oynanan, kazanma %, tahmin dağılımı), hafıza, tüpler, mayın (oynanan, kazanma %, en iyi süre), sudoku (oynanan, en iyi süre; **kazanma oranı yok**), ışıklar/kayan/hedef/borular (oynanan, en iyi süre, en az hamle). Akış için eski seri/en iyi seri kutuları **yok**.
- [ ] **Favoriler:** başlık geri okunun yanında. Birden fazla oyun varsa üstte **oyuna göre filtre çipleri** (Tümü + oyunlar). Satırda oyun adı/sonuç/tarih; oyununun en iyi süresine sahip kazanılmış favoride **"★ En iyi süre"**. Her satırda **"Tekrar oyna ›"**: dokununca o bulmaca yeni akışın ilk sayfası oluyor (yarım akış varsa önce "Yeni akış başlatılsın mı?" onayı); yeni sonuç kendi sonucu olarak kaydediliyor, sayaç 0'dan başlıyor. Mayında aynı tohum ama tahta farklı olabilir (mayınlar ilk dokunuştan sonra yerleşir).
- [ ] Favorisi olmayan oyuncuda boş durum metni.

---

## 12. Ayarlar

- [ ] Başlık "Ayarlar" geri okunun yanında.
- [ ] **Uygulama dili:** alan dokununca **gerçek bir akordeon gibi** kayarak açılıyor, ok dönüyor; Cihaz dili / Türkçe / English. Seçince bütün metinler anında değişiyor, bulunduğun sayfa kalıyor, uygulamayı kapat-açınca seçim duruyor.
- [ ] **Hesap:** giriş yapmışsa sağlayıcı, e-posta, **kullanıcı adı + "Değiştir"** (dokununca alan kayarak açılıyor; geçersiz/alınmış adda doğru bildirim, başarılıda "Kullanıcı adı kaydedildi" ve sıralamada yeni ad). "Her şey yedeklendi" yazısı **yok**.
- [ ] **Çıkış yap** butonu ana yeşil renkte; basınca "Çıkış yapılsın mı?" onayı (Vazgeç: hiçbir şey olmaz; Çıkış yap: çıkıyor, anonim hesaba dönüyor). **Hesabı sil** butonu **kırmızı**; basınca onay ("Hesabın silinsin mi?", "Kalıcı olarak sil" kırmızı, "Vazgeç").
- [ ] Anonim hesapta: "Google ile giriş yap" (ana renk) görünüyor ve **Hesabı sil görünmüyor**.
- [ ] **Paylaşım:** "BrainScroll'u geliştirmeye yardım et" yazısı **normal yazı boyutunda** (dikkat çekmiyor), sağında küçük **?** ve **açma/kapama anahtarı** (kayan animasyonlu). **?**'a basınca uzun açıklama altta kayarak açılıyor, tekrar basınca kapanıyor. Anahtarı kapat/aç, uygulamayı kapat-aç: seçim hatırlanıyor.
- [ ] Büyük yazıda tüm kartlar kesilmeden uzuyor, kaydırılabiliyor.

---

## 13. Hesap işlemleri

- [ ] **Google girişi** çalışıyor (tarayıcıya gidip "Giriş yapıldı" ile dönüyor), çıkış yapınca anonime dönüyor.
- [ ] **Hesap silme (gerçekten siler, B hesabıyla):** hazırlık: giriş, kullanıcı adı, birkaç bulmaca çöz, bir favori, günlük bulmaca. Vazgeç yolunda hiçbir şey silinmiyor. Uçak modunda "Kalıcı olarak sil": "Hesap silinemedi…" ve **hiçbir veri silinmiyor**. Gerçek silme: "Hesap silindi", anonim hesap, istatistikler/favoriler sıfır, yeni akış, kullanıcı adı gitti, dil seçimi ve paylaşım anahtarı **korundu**, akış rekoru sıfırlandı. Aynı B hesabıyla tekrar giriş: sıfırdan yeni hesap.
- [ ] Supabase: `select count(*) from profile where lower(username) = 'silme_testi';` → 0 (yeniden aldıysan 1). Anonim hesapta "Hesabı sil" görünmüyor.
- [ ] **Birleştirme:** A hesabı eski EN günlük sonuç + kullanıcı adıyla çıkış yapılmış; yeni anonim kurulumda TR günlük + birkaç akış bulmacası + bir favori çöz; sonra A ile giriş: ~10 sn sonra sıralamada TR sonucum kullanıcı adımla, eski EN sonuç duruyor, gün serisi ikisini birlikte sayıyor, profil ve favori yerinde. Aynı gün aynı dilde çakışmada A'nın kendi sonucu kalıyor. Hiç kullanılmamış hesapla giriş: veri kaybolmuyor. Tarayıcıdan vazgeçince / uçak modunda giriş: uygulama bozulmuyor. Giriş sonrası ağ yokken birleştirme bir sonraki açılışta tamamlanıyor.
- [ ] Supabase doğrulama: `select r.mode, r.language, r.day_index, r.duration_ms from game_result r join profile p on p.user_id = r.user_id where lower(p.username) = lower('KULLANICI_ADIN') order by r.day_index, r.language;` ve `select count(*) from auth.users where is_anonymous;` (1 azaldı).

---

## 14. Senkron ve yedekleme (Supabase)

- [ ] Girişli hesapla her oyun türünden birer bulmaca bitir, ~10 sn bekle. `select game, mode, outcome, left(answer, 40) answer, guesses, duration_ms from game_result order by created_at desc limit 20;` → word, memory, tubes, mines (`8x8x10:seed:ilk`), sudoku (`9x9x36:seed`), lights (`5x5x7:seed`), slide (`3x4x80:seed`), target (`2367>48`), pipes (25 karakter hex) satırları var; `guesses` hamle sayısı (kelime ve hafızada tahmin/kart sırası).
- [ ] Yeniden kurulumda aynı hesapla giriş: bütün oyunların geçmişi, profil sayıları ve favoriler geri geliyor. Eski sürüm bilinmeyen oyun satırlarını yok sayıyor, bozulmuyor.
- [ ] Hesap silme bütün oyun sonuçlarını siler; birleştirme hepsini taşır.
- [ ] **Günlük** diğer oyun günlerinde sonuç sunucuya gidiyor (migration uygulanmış olmalı); `game_result` satırında `mode = 'DAILY'` ve doğru `game`.
- [ ] Not: akıştaki "Bu akışta çözülen" sayısı ve rekoru **yedeklenmiyor**, yalnız cihazda (yeniden kurulumda sıfırlanması beklenen davranış).

---

## 15. Anonim kullanım sayıları ve gizlilik

- [ ] Ayarlar'daki paylaşım anahtarı açıkken (~10 sn sonra): `select at, name, platform, app_version, props from app_event order by at desc limit 30;` → `app_open`, `tab_viewed` (`{"tab": "daily"}`), `daily_started`, `daily_finished`, `endless_finished` (`{"game": "memory", "outcome": "WON", "moves": "9"}`, kelime için `guesses`, `language`), giriş/arkadaş/silme olayları. `platform` doğru (`android`), `app_version` sürümle uyumlu. **Kimlik yok** (props'ta kullanıcı kimliği, e-posta, ad yok).
- [ ] Anahtarı **kapat**: `select count(*) from app_event;` sayısı artmıyor (sekme gez, bulmaca çöz, 15 sn bekle); geri aç: yeniden artıyor.
- [ ] Gizlilik politikası (web): `https://playbrainscroll.com/privacy` (TR) ve `/en/privacy` açılıyor; "Anonim kullanım sayıları ve çökme raporları" bölümü doğru; saklama bölümü "Hesabı sil" akışını anlatıyor. **Hukuki metin, kendin oku ve onayla.**

---

## 16. Haptik (titreşim)

- [ ] Sistem **dokunma geri bildirimi açık** (Ses ve titreşim > Dokunma geri bildirimi). Kapalıyken titreşim yok, aç.
- [ ] Klavye harfi/Enter/Sil: hafif tık. Sekme değişimi: hafif (açık sekmeye tekrar basınca tık yok). Favori kalbi: hafif. Reddedilen tahmin ve listede olmayan kelime: uyarı.
- [ ] Kazanma: **başarı** (bir kez). Kaybetme (kelime, mayın): uyarı, başarı yok. Oyun içi: hafıza ilk kart hafif / eşleşme orta / yanlış uyarı; tüp seçme hafif / dökme orta; mayın açma hafif / bayrak orta; sudoku doğru orta / yanlış uyarı / not hafif; ışık, kayan, boru dokunuşu hafif; hedef sayı seçim hafif / işlem orta / geçersiz uyarı.
- [ ] Zaten bitmiş bulmacaya geri dönünce, uygulamayı kapat-açınca, günlük sekmesine girince **titreşim yok**.

---

## 17. Animasyon ve "hareketi azalt"

- [ ] Hareket **açıkken**: harf pop'u, karo çevirme (soldan sağa), eksik tahmin sarsması, sonuç kartı aşağıdan kayarak gelme, "+1" ve rozet pop'u, alttan açılan panellerin kayması, akordeonların kayarak açılması, anahtarın (switch) kayması, konfeti (kazanınca, ~2 sn, dokunmayı engellemiyor), mayın zincirleme patlaması, sudoku dalgası, tüp dökmesi.
- [ ] Akışta bitmiş bulmacaya geri kaydırınca animasyonlar **tekrar oynamıyor**, konfeti tekrar yağmıyor.
- [ ] **Hareketi azalt:** `Ayarlar > Erişilebilirlik > Animasyonları kaldır` (bazı telefonlarda `Geliştirici seçenekleri > Animasyon ölçeği` kapalı) aç, uygulamaya dön (arka plana atıp getir).
- [ ] Hareketi azalt açıkken: karolar anında renkleniyor, sarsma yok, pop yok, konfeti yok, sonuç kartı anında, sayı hemen değişiyor ("+1" yok), tüp/kart/taş/boru geçişleri animasyonsuz, mayın zinciri anında. Yanlış hafıza çiftinin ~1 sn görünme beklemesi **yine var**. Ayarı kapatınca animasyonlar geri geliyor.

---

## 18. Erişilebilirlik

- [ ] TalkBack aç. Kelime karosu "A harfi, doğru yerde" / "…kelimede, yanlış yerde" / "…kelimede yok"; yazılan harf "yazıldı", boş karolar sessiz. Klavye tuşları durumunu söylüyor; GİR "Tahmini gönder", silme "Sil" (basılı tutma yok, tek dokunuş).
- [ ] Sekme çubuğu: açık sekme "seçili". Akış sayacı "Bu akışta çözülen: 3", günlük seri "Günlük seri: 2" cümle olarak okunuyor. Ampul "Bana yol göster", rehber cümlesi **kendiliğinden okunuyor**, çarpı "Kapat". Kalp "Favorilere ekle/çıkar". Reddedilen tahmin mesajı kendiliğinden okunuyor.
- [ ] Oyunlar: hafıza "Kapalı kart, 7. konum" / "Yıldız kartı" / "Eşleşti: Yıldız", sayaçlar ("Hata sayısı: 2"); tüp "3. tüp, alttan başlayarak: Daire, Yıldız" / "boş" / "seçili" / "tamamlandı"; mayın "3. satır, 4. sütun, kapalı/bayraklı/açık, çevresinde 2 mayın" (+ bayrak için eylem menüsü); sudoku hücresi (verilen/yanlış/notlar), "5 koy" / "5 notu", kalem butonu açık/kapalı; ışık, kayan taş, hedef sayı, boru karosu anlamlı okunuyor, çift dokunma ile oynanıyor. Bir oyunu yalnız ekran okuyucuyla bitirebiliyor musun? Takıldığın yeri not al.
- [ ] Sıralama: sekmeler ve filtre satırı "seçili" diye okunuyor, tablo satırı tek blok ("1, alice, 2/6 · 00:30"). Ayarlar: anahtar "açık/kapalı", **?** "Paylaşım hakkında bilgi", akordeon durumu, onay kutuları okunuyor. Düzenle sayfası kartları "seçili/seçili değil".
- [ ] **Büyük yazı** (en büyük adım): alt çubuk (en fazla 1,3 kat büyür) tek satır; akış üst çubuğu, klavye ve numpad kesilmiyor; günlük giriş ekranı taşmıyor; istatistik kutuları 2x2'ye geçiyor; Sıralama filtre sheet'i kaydırılabiliyor; profil/ayarlar kartları kesilmeden uzuyor; tahtalardaki sayılar hücreye sığıyor (tahta yazıyla büyümüyor, bilerek). Ayarı geri al.
- [ ] **Yüksek kontrast:** `Erişilebilirlik > Yüksek kontrastlı metin` (bazı telefonlarda yok, atla): çerçeveler belirgin, soluk yazılar parlak, renklerin anlamı aynı.
- [ ] Dokunma hedefleri: ampul, **?**, geri oku, sıralama filtre satırı, "Kabul et / Reddet / Çıkar", sudoku rakamları, Enter/Sil tuşları rahat vurulabiliyor (en az ~44-48 dp).

---

## 19. Gerilemeler ve çevrimdışı

- [ ] Günlük bulmaca baştan sona (Başla, süre, tahmin, sonuç, geri sayım, ertesi gün yeni bulmaca).
- [ ] Çevrimdışı (uçak modu): akış ve bütün oyunlar çalışıyor, günlük kelime bulmacası "internet bağlantısı gerekiyor" diyor, Sıralama son görüntüyü "Çevrimdışı" notuyla gösteriyor. Ağ gelince bekleyen sonuçlar yükleniyor.
- [ ] Android geri tuşu/hareketi: alt sayfada sayfayı kapatıyor, akışta Ana Sayfa'ya dönüyor, Ana Sayfa'da uygulamadan çıkarıyor; panel/onay açıkken yalnız onu kapatıyor. Kenardan kenara (edge-to-edge) ekranda durum ve gezinme çubuğu içeriği kesmiyor. Ekran döndürme/katlanır ekran (varsa) bozulmuyor.
- [ ] Uygulamayı arka plana atıp 1 saat sonra dön: akış, yarım bulmaca, sayaç (uygulama arka planda öldürülmediyse) duruyor; öldürüldüyse yarım oyunlar geri geliyor, sayaç 0.

---

## 20. Bulursan bana yaz

- [ ] Her başarısız adım için: **kaçıncı madde**, **cihaz ve sürüm** (Pixel 7, Android 15), **ne gördün**, mümkünse ekran görüntüsü.
- [ ] Özellikle bak: (1) alttan panellerin ve konfetinin akıcılığı düşük güçlü cihazlarda, (2) rehber cümlesi gelip giderken tahtanın **oynamaması**, (3) Enter/Sil tuşlarının hissi ve Türkçe klavyenin genişliği, (4) sudoku rakam tuşlarının dar gelip gelmediği (9 tuş tek satır), (5) mayın zincirleme patlamanın hızı (abartılı mı), (6) ekran okuyucuyla bir oyunun baştan sona çözülebilmesi, (7) rastgele kullanıcı adının gerçek Google hesabında atanması, (8) çökme: akış varken **yeni akış başlatınca** uygulamanın kapanması (sorun sürüyorsa `adb logcat -b crash` çıktısı / Xcode crash günlüğü).
