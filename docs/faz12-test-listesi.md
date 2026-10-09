# Faz 12 test listesi: sudoku (cihazda, elle)

Otomatik testler geçti (üretici, kurallar, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve tahta
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, kelime...** Beşinci sayfa sudoku.
- [ ] Başlıkta "Sudoku", altında "Nasıl Oynanır?" düğmesi, onun altında "Hata 0/3" ve "Notlar". 9x9 tahta, 3x3 kutular arasında belirgin boşluk, yaklaşık 36 dolu hücre.
- [ ] Tahta ekrana sığıyor, hücreler okunuyor. **Küçük telefonda** (iPhone SE gibi) tahta, sayı tuşları ve düğmeler sığıyor; hücreler parmakla vurulabilir.
- [ ] Altta iki satır sayı tuşu (1-5, 6-9 ve silgi). Tuşlar rahat vurulabiliyor.
- [ ] Dikey kaydırma tahta ve tuşlar üzerinden de sayfayı değiştiriyor (hücreler kaydırmayı yutmuyor).

## 2. Oynanış
- [ ] Boş hücreye dokun: sarı çerçeve, aynı satır/sütun/kutu açık zemin, aynı sayılı hücreler ince sarı çerçeve. Dolu (verilen) hücreyi de seçebilirsin, ama değiştiremezsin.
- [ ] Sayı tuşu: **doğru** sayı hücreye yerleşir (mavi), haptik orta. Doğru sayı **kalıcı**: tekrar seçip başka sayı ya da silgi denersen bir şey olmaz.
- [ ] **Yanlış** sayı: hücre kırmızı, "Hata 1/3", haptik uyarı. Silgi ya da başka bir sayı ile düzelir. Aynı yanlış sayıyı tekrar basmak hata artırmıyor.
- [ ] Hata sayacı 2/3'ten itibaren kırmızı.
- [ ] **Notlar** düğmesi: açıkken sayı tuşları gri, sayı tuşuna basınca hücrede küçük not çıkar (1 sol üstte, 9 sağ altta). Aynı tuşa tekrar basınca not kalkar. Notlar okunuyor.
- [ ] Notlar kapalıyken doğru sayı koyunca aynı satır/sütun/kutudaki **o sayının notları kendiliğinden silinir**, diğer notlar kalır.
- [ ] Silgi: yanlış sayıyı kaldırır, boş hücrede notları temizler.
- [ ] Bir sayı tahtada 9 kez doğru yerleşince tuşu soluklaşır ve basılmaz.
- [ ] Seçili hücre yokken sayı tuşuna basmak hiçbir şey yapmıyor.

## 3. Kaybetme
- [ ] 3. hatada: sonuç kartı "Çok fazla hata", süre. Haptik uyarı (bir kez), başarı yok. Şimşek serisi **sıfırlanır**.
- [ ] Bittikten sonra sayı tuşları yok, hücre seçimi serbest ama değişiklik yok.

## 4. Kazanma
- [ ] Tüm hücreler doğru dolunca: "Sudoku çözüldü!" ve süre. Haptik **başarı** (bir kez). Şimşek serisi 1 artar.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Sudoku" ve süre.
- [ ] **Tahminsiz:** hiçbir tahtada şansa kalmak zorunda değilsin; her adım mantıkla bulunuyor (birkaç tahta oyna, zorlandığın bir tahta var mı not al).
- [ ] Zorluk **yeterince** keyifli mi? (Seviyeler sonra gelecek: çok kolay/zor geliyorsa not al.)
- [ ] Süre makul: her eylem arası en fazla **30 sn** sayılır (uygulamayı arka plana atıp dönmek süreyi şişirmiyor).
- [ ] Yeni tahta açılırken **takılma yok** (kaydırınca sayfa hemen geliyor; tahta üretimi hissedilmiyor).

## 5. Yarım kalan oyun
- [ ] Birkaç sayı koy, birkaç not yaz, bir hata yap, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta, aynı sayılar, aynı notlar, "Hata" sayısı aynı, Notlar düğmesi kapalı.
- [ ] Bitirdiğin tahta yeniden açınca yarım oyun olarak **geri gelmiyor**.

## 6. Seri ve istatistikler
- [ ] Kazanç ortak seriyi artırır, kayıp sıfırlar (diğer oyunlarla birlikte).
- [ ] Profil > İstatistikler: yeni **Sudoku** bölümü (Oynanan, Kazanma %, En iyi süre). En iyi süre yalnızca **kazanılan** oyunlar için, hiç kazanmadıysan "-".

## 7. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Sudoku** var. Kapatınca yeni sayfalarda gelmiyor, başlamış tahta kalıyor. En az bir oyun açık kalıyor.

## 8. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir tahta bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'sudoku' order by created_at desc limit 5;` Beklenen: `game = 'sudoku'`, `answer` "9x9x36:-12345" gibi, `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: sudoku geçmişi, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: sudoku satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme sudoku sonuçlarını da siler, birleştirme taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "sudoku", "outcome": "WON" veya "LOST", "moves": "..."}`.

## 9. Haptik, erişilebilirlik
- [ ] Haptik: doğru sayı orta, yanlış uyarı, not hafif, kayıp uyarı, kazanma başarı.
- [ ] **VoiceOver / TalkBack:** hücre "3. satır, 4. sütun, 7" (verilen sayı için "verilen sayı", yanlış için "yanlış", notlu boş hücre için "notlar: 2, 5"). Hücreye çift dokunma seçer, sayı tuşu "5 koy" (not modunda "5 notu"), Notlar düğmesi açık/kapalı durumunu söylüyor, "Hata" sayacı "izin verilen 3 hatanın 1 tanesi" diye okunuyor.
- [ ] **Büyük yazı** ve **yüksek kontrast:** tahtadaki sayılar hücreye sığıyor (tahta yazı boyutuna göre büyümüyor, bilerek), tuşlar ve çipler okunuyor, hücre çerçeveleri görünüyor.

## 10. Nasıl Oynanır?
- [ ] Sudoku sayfasında "Nasıl Oynanır?" düğmesi var, kuralları (9x9, hata sınırı 3, notlar, silgi, mantıkla çözülür) anlatıyor. Dışına dokunmak, "Anladım" ve Android geri tuşu kapatıyor. Panel açıkken arkadaki tahtaya dokunmak çalışmıyor.
