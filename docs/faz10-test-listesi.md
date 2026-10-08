# Faz 10 test listesi: renk tüpleri (cihazda, elle)

Otomatik testler geçti (kurallar, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve tahta
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, kelime...** Üçüncü sayfa renk tüpleri.
- [ ] Başlıkta "Renk tüpleri", altında "Hamle 0" sayacı. 7 tüp: üstte 4, altta 3 (ortalı). 5 tüp dolu (4 renk bloğu), 2 tüp boş.
- [ ] Hiçbir tüp başlangıçta tek renkle dolu değil. Telefon dikey ve yatay (varsa) dönmede tahta ekrana sığıyor.
- [ ] Her renk bloğunda **şekil** var (mavi daire, sarı üçgen, turuncu yıldız, kırmızı kalp, yeşil elmas). Renk körüysen renkleri şekilden ayırabiliyorsun.

## 2. Oynanış
- [ ] Dolu bir tüpe dokun: tüp **yukarı kalkar**, sarı çerçeve. Haptik hafif.
- [ ] Aynı tüpe tekrar dokun: tüp iner, hiçbir şey olmaz.
- [ ] Kalkık tüpten sonra **uygun** bir tüpe dokun (boş ya da üstü aynı renk): üstteki aynı renk grubu geçer, "Hamle 1". Haptik orta.
- [ ] **Sığdığı kadar** dökülür: hedefte 1 yer varsa 2'li grubun yalnızca 1'i geçer.
- [ ] Uygunsuz hedefe dokun (üstte başka renk ya da dolu tüp): dökülmez, dokunduğun tüp yeni seçim olur. Boş tüp seçili kalmaz.
- [ ] Tek renkli tüpü boş tüpe dökmek **olmuyor** (boşuna hamle).
- [ ] **Dökme animasyonu (~1 sn):** seçili tüp hedef tüpün üstüne gelir, eğilir, renk ince bir akıntıyla yavaş yavaş hedefe dolar, tüp yerine döner. Animasyon sürerken dokunuşlar, geri al ve yeniden başlat çalışmaz. Hareketi azalt açıkken dökme anında olur.
- [ ] Bir tüp **tamamlanınca** küçük bir büyüme (pop) ve kısa yeşil parlama. Abartılı değil, bir kez.
- [ ] **Geri al / yeniden başlat** yalnızca ikon: büyük (56 dp) yuvarlak düğmeler, geri alınacak bir şey yokken soluk. Kolayca vurulabiliyor.
- [ ] **Geri al:** son dökmeyi geri alır, "Hamle" sayısı **düşmez**. Geri alınacak bir şey yokken düğme soluk.
- [ ] **Yeniden başlat:** tüplerin hepsini başa alır, hamle sayısı **düşmez**. Başlangıçtayken soluk.
- [ ] Sıkışmak mümkün ama her zaman çıkış var (geri al / yeniden başlat). Kaybetme yok.

## 3. Kazanma
- [ ] Her tüp boş ya da tek renkle dolu olunca (son dökme animasyonu bittikten sonra): tüpler **yeşil çerçeveli**, sonuç kartı aşağıdan kayarak gelir: "Tüm renkler ayrıldı!", "N hamle · mm:ss". Haptik **başarı** (bir kez). Şimşek serisi 1 artar.
- [ ] Bittikten sonra tüplere dokunmak hiçbir şey yapmıyor, geri al/yeniden başlat görünmüyor.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Renk tüpleri", "N hamle · mm:ss".
- [ ] Süre makul: her eylem arası en fazla 15 sn sayılır (uygulamayı arka plana atıp dönmek şişirmiyor).

## 4. Yarım kalan oyun
- [ ] Birkaç dök, bir tane geri al, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta, aynı tüp içerikleri, aynı hamle sayısı. Seçili (kalkık) tüp yok.
- [ ] Bitirdiğin oyun yeniden açınca yarım oyun olarak **geri gelmiyor**.

## 5. Seri ve istatistikler
- [ ] Renk tüpleri galibiyeti ortak seriyi **artırır**; seriyi hiç sıfırlamaz.
- [ ] Profil > İstatistikler: yeni **Renk tüpleri** bölümü (Oynanan, En iyi süre, En az hamle). Hiç bitirmediysen "-".
- [ ] "Sonsuz akış" kutuları üç oyunu birlikte sayıyor, "Tahmin dağılımı" yalnızca kelimeler.

## 6. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Renk tüpleri** var. Kapatınca yeni sayfalarda gelmiyor, başlamış tahta kalıyor. En az bir oyun açık kalıyor.

## 7. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir tahta bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'tubes' order by created_at desc limit 5;` Beklenen: `game = 'tubes'`, `outcome = 'WON'`, `answer` "0121|2013|...|" gibi, `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: renk tüpleri geçmişi, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: tubes satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme tubes sonuçlarını da siler, birleştirme tubes sonuçlarını taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "tubes", "outcome": "WON", "moves": "..."}`.

## 8. Haptik, animasyon, erişilebilirlik
- [ ] Haptik: seçme hafif, dökme orta, geçersiz boş tüp uyarı, kazanma başarı.
- [ ] **Hareketi azalt** açıkken tüp kalkması anında olur.
- [ ] **VoiceOver / TalkBack:** tüp "3. tüp, alttan başlayarak: Daire, Yıldız, Kalp", boş "2. tüp, boş", seçili "seçili", tamamlanmış "tamamlandı". Geri al ve yeniden başlat düğmeleri açıklamalı.
- [ ] **Büyük yazı** ve **yüksek kontrast**: düğmeler ve tüp çerçeveleri okunuyor.
