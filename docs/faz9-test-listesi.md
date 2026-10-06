# Faz 9 test listesi: hafıza eşleştirme ve karışık akış (cihazda, elle)

Otomatik testler geçti (kurallar, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için. Her adımda "Beklenen" yazıyor.

## 0. Hazırlık
- [ ] Son commit TestFlight'a çıktı ve iPhone'a yüklendi. Android'de son sürüm yüklü.
- [ ] Önce **uygulamayı normal aç**, hiçbir şeyi silmene gerek yok. Eski yarım kelime bulmacaların ve geçmişin korunması da test edilecek.
- [ ] Supabase SQL editörü açık (senkron ve telemetri kontrolü için).

## 1. Karışık akış
- [ ] Akış sekmesini aç. İlk sayfa **kelime bulmacası** (klavyeli).
- [ ] Bitirmeden yukarı kaydır (atlama hakkı gider, "Atlandı, atlama hakkın bitti" mesajı). İkinci sayfa **Hafıza eşleştirme**: başlık, iki sayaç ("Hamle 0", "Hata 0/6") ve 4x3 kapalı kart, altında "İki kart çevir, çiftleri bul".
- [ ] Bir sonraki sayfa yine **kelime bulmacası**: sıra dönüşümlü (kelime, hafıza, kelime, hafıza...).
- [ ] Hafıza sayfasında dil seçici (TR/EN, üstte) duruyor ama hafızayı etkilemiyor. Kelime sayfalarında hâlâ çalışıyor ve ilk harf yazılınca kilitleniyor.
- [ ] Atlama hakkı **iki oyun arasında ortak**: kelimede kullandıysan hafıza sayfasını bitirmeden terk edemezsin ("Önce bu bulmacayı bitir").

## 2. Hafıza oyunu
**Kartlar**
- [ ] Kapalı kart: koyu yüzey, ince çerçeve, soluk iki daireli beyin işareti.
- [ ] Bir karta dokun: kart **yatay eksende dönerek** açılır, yüzünde bir şekil (daire, üçgen, yıldız, kalp, elmas, kare, artı, ay) ve **sarı çerçeve** görünür. Şekillerin hepsi birbirinden **şekille** ayırt ediliyor (renkler yardımcı).
- [ ] 12 kartta 6 farklı şekil, her biri iki kez var.

**Eşleşme**
- [ ] İki aynı karta dokun: kartlar açık kalır, **yeşil dolgu** olur, şekil koyu renkte. Kısa bir "pop" animasyonu var. "Hamle" sayacı 1 arttı, hata artmadı.
- [ ] Haptik: ilk kart açılınca hafif, eşleşmede **orta** titreşim.

**Hata**
- [ ] İki farklı karta dokun: "Hata 1/6", ikisi de açık kalır, yaklaşık **1 saniye sonra kendiliğinden kapanır**. Haptik: uyarı titreşimi.
- [ ] Kapanmasını beklemeden **başka bir karta dokun**: yanlış çift hemen kapanır ve yeni kart açılır (bekleme yok).
- [ ] Yanlış çiftteki kartlardan birine tekrar dokun: o kart yeni bir hamlenin ilk kartı olarak açılır (yok sayılmaz).
- [ ] Hata sayacı 4/6'dan itibaren **kırmızı**.
- [ ] Zaten açık olan karta ya da eşleşmiş karta dokunmak hiçbir şey yapmıyor, sayaçlar artmıyor.

**Kazanma**
- [ ] 6 çiftin hepsini bul: sonuç kartı aşağıdan kayarak gelir: "Tüm çiftler bulundu!", "N hamle · mm:ss", "↑ Sıradaki bulmaca için yukarı kaydır". Haptik: **başarı** (bir kez). Üstteki şimşek serisi 1 artar ve kısa süre büyür.
- [ ] Konfeti **yok** (konfeti yalnızca günlük bulmaca içindi).
- [ ] Süre makul: düşünme süresi sayılır, her iki hamle arası en fazla 10 sn (uygulamayı arka plana atıp dönmek süreyi şişirmiyor).

**Kaybetme**
- [ ] 6 hata yap: "Çok fazla hata" sonuç kartı, kalan **tüm kartlar açık** gösterilir (yerlerini görmek için). Haptik: uyarı, başarı yok. Şimşek serisi **sıfırlanır**.

## 3. Yarım kalan oyun
- [ ] Hafıza sayfasında birkaç kart çevir (bazıları eşleşmiş, biri açık kalmış), uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta (şekiller aynı yerde), eşleşmiş kartlar eşleşmiş, hamle ve hata sayıları aynı. Açık kalan tek kart **kapalı**. Yanlış çift ekrandaysa kapanmış.
- [ ] Süre devam ediyor ama kapalıyken geçen süre eklenmiyor.
- [ ] Bitirdiğin bir hafıza oyunu, uygulamayı kapatıp açınca **yarım oyun olarak geri gelmiyor** (yerine yeni bir tahta var).

## 4. Seri ve istatistikler
- [ ] **Ortak seri:** kelime kazan (seri 1), hafıza kazan (seri 2), kelime kazan (seri 3). Sonra bir oyunu kaybet: seri 0.
- [ ] Profil > Sonsuz akış kutuları (oynanan, kazanma, seri, en iyi seri) **iki oyunu birlikte** sayıyor. "Tahmin dağılımı" yalnızca kelimeler.
- [ ] Profil'de yeni **Hafıza eşleştirme** bölümü: Oynanan, Kazanma %, En iyi süre, En az hamle. Hiç kazanmadıysan süre ve hamle "-".
- [ ] En iyi süre **kazanılan** oyunların en hızlısı (kaybedilen hızlı oyun sayılmıyor).
- [ ] Günlük bulmaca istatistikleri değişmedi (hafıza orada yok).

## 5. Favori
- [ ] Biten bir hafıza oyununda kalbe bas. Haptik hafif.
- [ ] Profil > Favoriler: satırda "Hafıza eşleştirme" ve "N hamle · mm:ss" (kaybedilmişse "Çözülmedi" benzeri metin), kelime cevabı gibi sayılar/şekil numaraları **görünmüyor**.

## 6. Senkron ve hesaplar (iki cihaz ya da yeniden kurulum)
- [ ] Google ile giriş yapmış hesapla bir hafıza oyunu bitir. Profil'de "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, left(answer, 40) answer, guesses, duration_ms from game_result where game = 'memory' order by created_at desc limit 5;` Beklenen: satır var, `game = 'memory'`, `mode = 'ENDLESS'`, `answer` "0,3,5,..." gibi şekil numaraları, `guesses` açılan kartların sırası.
- [ ] Başka cihazda (ya da silip yeniden kurduktan sonra) aynı hesapla giriş yap. Beklenen: hafıza geçmişi **geri gelir** (Profil > Hafıza eşleştirme sayıları, favoriler).
- [ ] **Eski sürümde** bu hesapla giriş yapabiliyorsan (eski TestFlight build'i varsa): hafıza satırları orada **yok sayılır**, uygulama bozulmaz.
- [ ] Hesap silme (Faz 8) hafıza sonuçlarını da siler (cihazda Profil'de hafıza sayıları sıfırlanır).
- [ ] Hesap birleştirme (anonim > var olan Google): anonim hesapta oynanan hafıza sonuçları birleştirme sonrası hesapta duruyor.

## 7. Telemetri (Faz 8 anahtarı açıkken)
- [ ] Bir hafıza oyunu bitir, ~10 sn bekle: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;`
- [ ] Hafıza için props: `{"game": "memory", "outcome": "WON", "moves": "9"}` (kelime için `game: word`, `guesses`, `language`). Kimlik yok.

## 8. Haptik, animasyon, erişilebilirlik
**Haptik**
- [ ] İlk kart: hafif. Eşleşme: orta. Yanlış çift: uyarı. Kazanma: başarı. Kaybetme: uyarı. Zaten bitmiş hafıza sayfasına geri kaydırınca: sessiz.

**Animasyon**
- [ ] Kart çevirme akıcı (~0,3 sn). Eşleşen kartlarda pop. Sonuç kartı kayarak geliyor.
- [ ] **Hareketi azalt** açıkken (iOS: Hareketi Azalt, Android: Animasyonları kaldır): kartlar **anında** açılıp kapanıyor, pop yok. Yanlış çiftin 1 saniyelik görünme beklemesi **yine var** (görmek için gerekli).

**Ekran okuyucu (VoiceOver / TalkBack)**
- [ ] Kapalı kart: "Kapalı kart, 7. konum".
- [ ] Açık kart: "Yıldız kartı, 7. konum". Eşleşmiş: "Eşleşti: Yıldız, 7. konum".
- [ ] Sayaçlar: "Şu ana kadar hamle: 3", "Hata: 6 hakkın 2 tanesi kullanıldı".
- [ ] Sonuç mesajı ("Tüm çiftler bulundu!") kendiliğinden okunuyor.
- [ ] Bir hafıza oyununu sadece ekran okuyucuyla bitirebiliyor musun? Takıldığın yeri not al.

**Büyük yazı ve yüksek kontrast**
- [ ] En büyük yazıda sayaçlar ve sonuç kartı kesilmeden görünüyor, kartlar taşmıyor.
- [ ] Kontrastı artır: kapalı kart çerçevesi daha belirgin, açık kart çerçevesi daha kalın.

## 9. Gerilemeler
- [ ] Kelime bulmacası aynı (klavye, çevirme, sarsma, haptik, günlük bulmaca ayrı sekmede olduğu gibi çalışıyor).
- [ ] Günlük bulmaca, Sıralama, Profil, hesap silme, birleştirme, gizlilik anahtarı çalışıyor.
- [ ] Atlama hakkı 30 dk sonra geri geliyor (hafıza sayfasında da).
- [ ] Çevrimdışı (uçak modu): hafıza ve kelime akışı çalışıyor.

## Bulursan bana yaz
Başarısız adımın numarası, cihaz ve sürüm, ne gördün, mümkünse ekran görüntüsü. Özellikle: kart çevirmenin akıcılığı, yanlış çiftin 1 sn beklemesinin hissi (çok kısa/uzun mu), 6 hata sınırının zorluğu (çok kolay/zor mu), ve ekran okuyucuyla oynanabilirlik.
