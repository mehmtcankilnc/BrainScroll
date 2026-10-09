# Faz 11 test listesi: mayın tarlası (cihazda, elle)

Otomatik testler geçti (kurallar, çözücü, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve tahta
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, mayın tarlası, kelime...** Dördüncü sayfa mayın tarlası.
- [ ] Başlıkta "Mayın tarlası", altında "Mayın 10" sayacı. 8x8 kapalı hücre, ekrana sığıyor, hücreler parmakla rahat vurulabiliyor. Altta ipucu yazısı ve bayrak modu düğmesi.
- [ ] Dikey kaydırma tahta üzerinden de sayfayı değiştiriyor (hücreler kaydırmayı yutmuyor).

## 2. Oynanış
- [ ] **İlk dokunuş güvenli:** her yeni tahtada ilk açtığın hücre 0 olur ve bir alan açılır, hiç kaybetmezsin. (Birkaç tahtada dene.)
- [ ] Hücreye dokun: açılır. Sayılar renkli (1 mavi, 2 yeşil, 3 kırmızı, 4 sarı...). Haptik hafif.
- [ ] **Uzun bas:** bayrak koyar, "Mayın" sayacı 1 azalır, haptik orta. Bayraklı hücreye dokunmak **açmaz**. Tekrar uzun bas: bayrak kalkar.
- [ ] **Bayrak modu** düğmesi: açıkken sarı çerçeveli. Açıkken dokunma bayrak koyar/kaldırır, uzun basma **açar**. Kapatınca normale döner.
- [ ] **Çift dokunma (chord):** bir sayının çevresine o sayı kadar bayrak koy, sayıya çift dokun: bayraksız komşular açılır. Bayrak sayısı eşit değilken hiçbir şey olmaz.
- [ ] Yanlış bayrakla chord yaparsan mayın açılır ve kaybedersin.
- [ ] Bayrak sayısı mayından fazla olabilir, sayaç kırmızı eksiye iner.
- [ ] "Hamle" sayacı yok (bilerek): bayrak hamle sayılmaz.

## 3. Kaybetme
- [ ] Mayına basınca: tüm mayınlar görünür, bastığın mayın kırmızı zeminli. Sonuç kartı "Mayına bastın", süre. Haptik uyarı (bir kez), başarı yok. Şimşek serisi **sıfırlanır**.
- [ ] Bittikten sonra hücrelere dokunmak hiçbir şey yapmıyor, bayrak düğmesi yok.

## 4. Kazanma
- [ ] Mayın olmayan her hücre açılınca: kalan mayınlar bayraklı görünür, "Tahta temizlendi!" ve süre. Haptik **başarı** (bir kez). Şimşek serisi 1 artar.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Mayın tarlası" ve süre.
- [ ] **Tahminsiz:** hiçbir tahtada şansa kalmak zorunda değilsin, her adım mantıkla bulunabiliyor (birkaç tahta oyna).
- [ ] Süre makul: her eylem arası en fazla 15 sn sayılır (uygulamayı arka plana atıp dönmek şişirmiyor).

## 5. Yarım kalan oyun
- [ ] Birkaç hücre aç, birkaç bayrak koy, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta (aynı sayılar, aynı açık alan, aynı bayraklar), aynı sayaç. Bayrak modu kapalı.
- [ ] Bitirdiğin tahta yeniden açınca yarım oyun olarak **geri gelmiyor**.
- [ ] Hiç açmadan yalnızca bayrak koyup kapatınca da tahta (bayraklarla) geri geliyor.

## 6. Seri ve istatistikler
- [ ] Kazanç ortak seriyi artırır, kayıp sıfırlar (diğer oyunlarla birlikte).
- [ ] Profil > İstatistikler: yeni **Mayın tarlası** bölümü (Oynanan, Kazanma %, En iyi süre). En iyi süre yalnızca **kazanılan** oyunlar için, hiç kazanmadıysan "-".

## 7. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Mayın tarlası** var. Kapatınca yeni sayfalarda gelmiyor, başlamış tahta kalıyor. En az bir oyun açık kalıyor.

## 8. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir tahta bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'mines' order by created_at desc limit 5;` Beklenen: `game = 'mines'`, `answer` "8x8x10:-12345:27" gibi, `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: mayın geçmişi, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: mines satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme mines sonuçlarını da siler, birleştirme taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "mines", "outcome": "WON" veya "LOST", "moves": "..."}`.

## 9. Haptik, erişilebilirlik
- [ ] Haptik: açma hafif, bayrak orta, kayıp uyarı, kazanma başarı.
- [ ] **VoiceOver / TalkBack:** hücre "3. satır, 4. sütun, kapalı", bayraklı "bayraklı", açık "açık, çevresinde 2 mayın". Çift dokunma kapalı hücreyi açar ya da bayrak modundaysa bayrak koyar, sayıya çift dokunma chord yapar. Bayrak için özel eylem ("Bayrak koy ya da kaldır") eylemler menüsünde.
- [ ] **Büyük yazı** ve **yüksek kontrast:** sayılar ve hücre çerçeveleri okunuyor.

## 10. Nasıl Oynanır? (dört oyun için)
- [ ] Kelime, hafıza, renk tüpleri ve mayın tarlası sayfalarının hepsinde başlığın altında "Nasıl Oynanır?" düğmesi var, her sayfada aynı yerde.
- [ ] Düğme alt paneli açar: oyunun adı, numaralı adımlar, "Anladım". Metinler doğru oyunun kuralını anlatıyor ve güncel (6 deneme, 6 çift ve 6. hata, 7 tüp, 10 mayın).
- [ ] Dışına dokunmak, "Anladım" ve Android geri tuşu paneli kapatıyor (geri tuşu uygulamadan ya da akıştan çıkmıyor).
- [ ] Panel açıkken arkadaki oyuna dokunmak ya da kaydırmak çalışmıyor, klavye (kelime) yazdırmıyor.
- [ ] **Büyük yazı** ile panel taşmıyor, adımlar kaydırılabiliyor, "Anladım" görünür kalıyor.
- [ ] **VoiceOver / TalkBack:** düğme "Nasıl Oynanır?" olarak okunuyor, panel başlığı başlık olarak.
- [ ] Kelime ve mayın sayfalarında düğme eklenince tahta/klavye hâlâ ekrana sığıyor (küçük telefonda da).
