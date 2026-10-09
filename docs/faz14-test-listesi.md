# Faz 14 test listesi: kayan bulmaca (cihazda, elle)

Otomatik testler geçti (kurallar, gerçek çözücüyle çözülebilirlik, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve tahta
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, kayan bulmaca, kelime...** Yedinci sayfa kayan bulmaca.
- [ ] Başlıkta "Kayan bulmaca", altında "Nasıl Oynanır?", onun altında "Hamle 0". 3 sütun x 4 satır, 11 numaralı taş ve bir boş yer. Tahta ekrana sığıyor, taşlar büyük ve rahat vuruluyor (küçük telefonda da). Altta ipucu yazısı ve baştan başla düğmesi (soluk).
- [ ] Yerinde olan taşların sayısı ve çerçevesi **yeşil**, diğerleri beyaz.
- [ ] Dikey kaydırma taşlar üzerinden de sayfayı değiştiriyor (taşlar kaydırmayı yutmuyor).

## 2. Oynanış
- [ ] Boşluğun yanındaki taşa dokun: taş **kayarak** boşluğa gidiyor (kısa, akıcı), "Hamle" 1 artıyor, haptik hafif.
- [ ] Boşlukla aynı satırda/sütunda ama uzakta (arada taş olan) bir taşa dokun: arada kalanlar **birlikte** kayıyor, yine **tek hamle**.
- [ ] Boşlukla aynı hizada olmayan taşa ya da boşluğa dokunmak hiçbir şey yapmıyor: hamle artmıyor, haptik yok.
- [ ] Hızlı arka arkaya dokununca animasyon takılmıyor, taşlar yerlerine oturuyor, yanlış yere gitmiyor.
- [ ] **Baştan başla** düğmesi: tahta başlangıçtayken soluk ve basılmaz; bir kaydırmadan sonra aktif. Basınca taşlar başlangıç yerine döner, **"Hamle" sayacı sıfırlanmaz**.
- [ ] Taşlar bir yere takılmadan her tahta **çözülebiliyor** (birkaç tahta dene). Tahta çok kolay ya da çok zor mu? (Seviyeler sonra gelecek, notunu al.)

## 3. Kazanma
- [ ] Son taş yerine oturunca: tüm sayılar yeşil, "Bulmaca çözüldü!", "N hamle · mm:ss". Haptik **başarı** (bir kez). Şimşek serisi 1 artar. Konfeti yok.
- [ ] Bittikten sonra taşlara dokunmak hiçbir şey yapmıyor, baştan başla düğmesi yok.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Kayan bulmaca" ve "N hamle · süre".
- [ ] Kaybetmek yok: uygulama hiçbir durumda seriyi sıfırlamıyor.
- [ ] Süre makul: her eylem arası en fazla 15 sn sayılır (uygulamayı arka plana atıp dönmek şişirmiyor).

## 4. Yarım kalan oyun
- [ ] Birkaç taş kaydır, bir kez baştan başla, birkaç kaydırma daha yap, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta, taşlar aynı yerde, aynı "Hamle" sayısı.
- [ ] Bitirdiğin tahta yeniden açınca yarım oyun olarak **geri gelmiyor**.

## 5. Seri ve istatistikler
- [ ] Kazanç ortak seriyi artırır (diğer oyunlarla birlikte).
- [ ] Profil > İstatistikler: yeni **Kayan bulmaca** bölümü (Oynanan, En iyi süre, En az hamle). Hiç oynamadıysan "-".

## 6. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Kayan bulmaca** var. Kapatınca yeni sayfalarda gelmiyor, başlamış tahta kalıyor. En az bir oyun açık kalıyor.

## 7. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir tahta bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'slide' order by created_at desc limit 5;` Beklenen: `game = 'slide'`, `outcome = 'WON'`, `answer` "3x4x80:-12345" gibi, `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: geçmiş, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: slide satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme slide sonuçlarını da siler, birleştirme taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "slide", "outcome": "WON", "moves": "..."}`.

## 8. Haptik, erişilebilirlik
- [ ] Haptik: kaydırma hafif, baştan başla hafif, kazanma başarı.
- [ ] **VoiceOver / TalkBack:** taş "7 numaralı taş, 2. satır, 3. sütun" (yerindeyse "yerinde"); çift dokunma taşı kaydırır. "Hamle" sayacı "Şu ana kadar hamle: N", baştan başla düğmesi "En baştan başla, hamleler sayılmaya devam eder" diye okunuyor.
- [ ] **Büyük yazı** ve **yüksek kontrast:** çip ve ipucu yazısı okunuyor, taş çerçeveleri görünüyor, tahta ekrandan taşmıyor, sayılar taşa sığıyor (tahta yazı boyutuna göre büyümüyor, bilerek).
- [ ] **Hareketi azalt** açıkken taşlar kaymadan yerine geçiyor.

## 9. Nasıl Oynanır?
- [ ] Sayfada "Nasıl Oynanır?" düğmesi var, kuralları (aynı hizadaki taş kayar, arada kalanlar birlikte, kaybetmek yok, baştan başla, yeşil = yerinde) anlatıyor. Dışına dokunmak, "Anladım" ve Android geri tuşu kapatıyor. Panel açıkken arkadaki taşlara dokunmak çalışmıyor.
