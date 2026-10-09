# Faz 13 test listesi: ışıkları söndür (cihazda, elle)

Otomatik testler geçti (kurallar, çözülebilirlik, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve tahta
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, kelime...** Altıncı sayfa ışıkları söndür.
- [ ] Başlıkta "Işıkları söndür", altında "Nasıl Oynanır?", onun altında "Hamle 0" ve "En çok 7". 5x5 ışık ızgarası, ekrana sığıyor, ışıklar parmakla rahat vuruluyor (küçük telefonda da). Altta ipucu yazısı ve baştan başla düğmesi (soluk).
- [ ] Yanan ışık **sarı dolgu, içinde koyu daire**; sönük ışık koyu zemin, içinde **soluk halka**. Renk ayırt etmeden de (şekilden) fark ediliyor.
- [ ] Dikey kaydırma ızgara üzerinden de sayfayı değiştiriyor (ışıklar kaydırmayı yutmuyor).

## 2. Oynanış
- [ ] Bir ışığa dokun: o ve üst, alt, sol, sağ komşuları değişiyor (yanan söner, sönük yanar). Kenarda ve köşede olmayan komşu yok sayılıyor. Haptik hafif, "Hamle" 1 artıyor.
- [ ] Aynı ışığa ikinci kez dokunmak her şeyi eski haline getiriyor, ama "Hamle" yine artıyor.
- [ ] Renk geçişi yumuşak (kısa), takılma yok.
- [ ] **Baştan başla** düğmesi: tahta başlangıçtayken soluk ve basılmaz; birkaç dokunuştan sonra aktif. Basınca ışıklar başlangıç haline döner, **"Hamle" sayacı sıfırlanmaz**.
- [ ] "En çok 7": tahtayı 7 ya da daha az dokunuşla çözmek **gerçekten mümkün** (birkaç tahta dene; yöntem: üst satırdan başlayıp ışıkları aşağı doğru kovala).
- [ ] Tahta çok kolay ya da çok zor mu? (Seviyeler sonra gelecek, notunu al.)

## 3. Kazanma
- [ ] Son ışık sönünce: "Tüm ışıklar söndü!", "N hamle · mm:ss". Haptik **başarı** (bir kez). Şimşek serisi 1 artar. Konfeti yok.
- [ ] Bittikten sonra ışıklara dokunmak hiçbir şey yapmıyor, baştan başla düğmesi yok.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Işıkları söndür" ve "N hamle · süre".
- [ ] Kaybetmek yok: uygulama hiçbir durumda seriyi sıfırlamıyor.
- [ ] Süre makul: her eylem arası en fazla 15 sn sayılır (uygulamayı arka plana atıp dönmek şişirmiyor).

## 4. Yarım kalan oyun
- [ ] Birkaç ışığa dokun, bir kez baştan başla, birkaç dokunuş daha yap, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta, aynı yanan ışıklar, aynı "Hamle" sayısı ve "En çok 7".
- [ ] Bitirdiğin tahta yeniden açınca yarım oyun olarak **geri gelmiyor**.

## 5. Seri ve istatistikler
- [ ] Kazanç ortak seriyi artırır (diğer oyunlarla birlikte).
- [ ] Profil > İstatistikler: yeni **Işıkları söndür** bölümü (Oynanan, En iyi süre, En az hamle). Hiç oynamadıysan "-".

## 6. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Işıkları söndür** var. Kapatınca yeni sayfalarda gelmiyor, başlamış tahta kalıyor. En az bir oyun açık kalıyor.

## 7. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir tahta bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'lights' order by created_at desc limit 5;` Beklenen: `game = 'lights'`, `outcome = 'WON'`, `answer` "5x5x7:-12345" gibi, `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: geçmiş, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: lights satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme lights sonuçlarını da siler, birleştirme taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "lights", "outcome": "WON", "moves": "..."}`.

## 8. Haptik, erişilebilirlik
- [ ] Haptik: dokunuş hafif, baştan başla hafif, kazanma başarı.
- [ ] **VoiceOver / TalkBack:** ışık "3. satır, 4. sütun, ışık yanıyor" ya da "ışık sönük"; çift dokunma ışığı değiştirir. "Hamle" sayacı "Şu ana kadar hamle: N", "En çok 7" çipi "Bu tahta en çok 7 hamlede çözülebilir", baştan başla düğmesi "En baştan başla, hamleler sayılmaya devam eder" diye okunuyor.
- [ ] **Büyük yazı** ve **yüksek kontrast:** çipler ve ipucu yazısı okunuyor, ışık çerçeveleri görünüyor, ızgara ekrandan taşmıyor.

## 9. Nasıl Oynanır?
- [ ] Sayfada "Nasıl Oynanır?" düğmesi var, kuralları (dokunuş komşuları da değiştirir, kaybetmek yok, baştan başla, her tahta çözülebilir) anlatıyor. Dışına dokunmak, "Anladım" ve Android geri tuşu kapatıyor. Panel açıkken arkadaki ızgaraya dokunmak çalışmıyor.
