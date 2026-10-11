# Faz 15 test listesi: hedef sayı (cihazda, elle)

Otomatik testler geçti (işlem kuralları, oluşturma algoritması ve "en az" değerinin kesinliği, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve ekran
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, kayan bulmaca, hedef sayı, kelime...** Sekizinci sayfa hedef sayı.
- [ ] Başlıkta "Hedef sayı", altında "Nasıl Oynanır?", "Hamle 0" ve "En az N". Altında hedef kartı (büyük sarı sayı, 10-99 arası), ortada 4 sayı taşı (1-9), en altta + − × ÷ tuşları ve geri al / baştan başla düğmeleri (soluk).
- [ ] Hedef hiçbir zaman başlangıç sayılarından biri değil. Tahta ekrana sığıyor, taşlar ve tuşlar rahat vuruluyor (küçük telefonda da).
- [ ] Dikey kaydırma taşlar üzerinden de sayfayı değiştiriyor.

## 2. Oynanış
- [ ] Bir sayıya dokun: sarı çerçeve. İşlem tuşları o ana kadar soluk, sayı seçilince canlanıyor. Seçili sayıya tekrar dokunmak seçimi bırakıyor.
- [ ] İşlem tuşuna dokun: sarı çerçeve. İkinci sayıya dokun: iki sayı yerine **sonuç** geliyor (sonuç taşı listenin sonunda), altında "7 + 4 = 11" gibi satır çıkıyor, "Hamle" 1 artıyor. Haptik: seçimler hafif, işlem orta.
- [ ] Bir işlem seçiliyken o işlemle **geçerli olmayan** sayılar soluklaşıyor (çıkarmada büyükten küçüğü çıkarmak gerekir: 3 − 7 geçersiz, 7 − 3 geçerli; bölmede 7 ÷ 2 geçersiz, 6 ÷ 2 geçerli). Soluk sayıya dokunursan hiçbir şey olmuyor, uyarı titreşimi geliyor, hamle sayılmıyor, seçim sıfırlanıyor.
- [ ] Sonuç sıfır ya da negatif **olamıyor**, bölme **kalanlı olamıyor**.
- [ ] Bir sayı yalnızca bir kez kullanılabiliyor (kullanılan sayı masadan kalkıyor). Sonuç taşı yeniden kullanılabiliyor.
- [ ] **Hepsini kullanmak zorunda değilsin:** hedefe daha az sayıyla ulaşınca oyun bitiyor, masada kalan sayılar umursanmıyor.
- [ ] Son üç işlem listede görünüyor (4. işlemden sonra eskisi kayıyor), sayfa zıplamıyor.
- [ ] **Geri al:** son işlemi geri alıyor (sayılar eski haline dönüyor), "Hamle" azalmıyor. İşlem yokken soluk. **Baştan başla:** tüm işlemleri geri alıyor, "Hamle" sıfırlanmıyor. Geri al ya da baştan başla seçili sayıyı bırakıyor.
- [ ] "En az N": hedefe gerçekten N işlemle ulaşmak mümkün (birkaç oyunda dene). Daha az ile ulaşılamıyor.
- [ ] Her hedefe ulaşılabiliyor (hiç "olmayan" bir hedef yok). Hedefler çok kolay (tek işlemle) ya da çok zor mu? Notunu al (seviyeler sonra gelecek).

## 3. Kazanma
- [ ] Bir sayı hedefe eşit olunca: o taş ve hedef yeşil, "Hedefe ulaştın!", "N hamle · mm:ss". Haptik **başarı** (bir kez). Şimşek serisi 1 artar.
- [ ] Bittikten sonra sayılara ve tuşlara dokunmak hiçbir şey yapmıyor, geri al ve baştan başla yok.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Hedef sayı" ve "N hamle · süre".
- [ ] Kaybetmek yok: uygulama hiçbir durumda seriyi sıfırlamıyor.
- [ ] Süre makul: her eylem arası en fazla 15 sn sayılır (uygulamayı arka plana atıp dönmek şişirmiyor).

## 4. Yarım kalan oyun
- [ ] Birkaç işlem yap, bir tanesini geri al, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı hedef, aynı masa, aynı işlem listesi, aynı "Hamle" sayısı. Seçili sayı ve işlem kalmıyor.
- [ ] Bitirdiğin oyun yeniden açınca yarım oyun olarak **geri gelmiyor**.

## 5. Seri ve istatistikler
- [ ] Kazanç ortak seriyi artırır (diğer oyunlarla birlikte).
- [ ] Profil > İstatistikler: yeni **Hedef sayı** bölümü (Oynanan, En iyi süre, En az hamle). Hiç oynamadıysan "-".

## 6. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Hedef sayı** var. Kapatınca yeni sayfalarda gelmiyor, başlamış oyun kalıyor. En az bir oyun açık kalıyor.

## 7. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir oyun bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'target' order by created_at desc limit 5;` Beklenen: `game = 'target'`, `outcome = 'WON'`, `answer` "2367>48" gibi (4 rakam, `>`, hedef), `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: geçmiş, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: target satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme target sonuçlarını da siler, birleştirme taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "target", "outcome": "WON", "moves": "..."}`.

## 8. Haptik, erişilebilirlik
- [ ] Haptik: seçimler hafif, işlem orta, geçersiz hamle uyarı, kazanma başarı.
- [ ] **VoiceOver / TalkBack:** hedef kartı "Hedef: 48", sayı "7 sayısı" (seçiliyse "seçili"), işlem tuşları "Artı / Eksi / Çarpı / Bölü" (seçiliyse "seçili", sayı seçilmeden devre dışı), "Hamle" ve "En az" çipleri anlamlı okunuyor, geri al ve baştan başla düğmeleri açıklamalı. Çift dokunma ile tam hamle yapılabiliyor.
- [ ] **Büyük yazı** ve **yüksek kontrast:** sayılar taşa sığıyor (taş sayısı büyüdükçe, örn. 4 basamaklı sonuçta yazı küçülüyor ama okunuyor), işlem tuşları ve çipler okunuyor, çerçeveler görünüyor.
- [ ] Büyük bir sonuç (3-4 basamak, örn. 9 × 9 × 9 × 9 = 6561) taşa sığıyor.

## 9. Nasıl Oynanır?
- [ ] Sayfada "Nasıl Oynanır?" düğmesi var, kuralları (hedef, sayı + işlem + sayı, her sayı bir kez, tam sayı sonuç, kaybetmek yok, "en az") anlatıyor. Dışına dokunmak, "Anladım" ve Android geri tuşu kapatıyor. Panel açıkken arkadaki oyuna dokunmak çalışmıyor.
