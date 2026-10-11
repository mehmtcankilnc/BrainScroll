# Faz 16 test listesi: boru bağla (cihazda, elle)

Otomatik testler geçti (ağaç üretimi, "en az" değerinin kaba kuvvetle doğrulanması, kurallar, kalıcılık, akış, arayüz). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 0. Hazırlık
- [ ] Son sürüm TestFlight'a çıktı ve iPhone'a yüklendi, Android'de son sürüm yüklü. Hiçbir şeyi silmene gerek yok.

## 1. Akış ve tahta
- [ ] Akışta sıra **kelime, hafıza, renk tüpleri, mayın tarlası, sudoku, ışıkları söndür, kayan bulmaca, hedef sayı, boru bağla, kelime...** Dokuzuncu sayfa boru bağla.
- [ ] Başlıkta "Boru bağla", altında "Nasıl Oynanır?", "Hamle 0" ve "En az N" (genelde 25-40 arası). 5x5 tahta, ekrana sığıyor, karolar parmakla rahat vuruluyor (küçük telefonda da). Altta ipucu yazısı ve baştan başla düğmesi (soluk).
- [ ] Borular kalın ve yuvarlak uçlu, ortada küçük düğüm. **Buluşan** yönler **yeşil** ve karonun kenarına kadar uzanıyor, komşu karo ile kesintisiz birleşiyor. **Açık uçlar** gri, kısa ve ucunda **küçük halka** var. Halkadan açık ucu renk olmadan da ayırt edebiliyorsun.
- [ ] Tahta gerçekten ağaç gibi: her karoda 1, 2 ya da 3 yön var, artı (4 yön) ya da boş karo yok.
- [ ] Dikey kaydırma karolar üzerinden de sayfayı değiştiriyor (karolar kaydırmayı yutmuyor).

## 2. Oynanış
- [ ] Karoya dokun: karo **saat yönünde** 90° dönüyor (kısa, akıcı animasyon), "Hamle" 1 artıyor, haptik hafif. Ters yöne döndürme yok.
- [ ] Hızlı arka arkaya dokununca animasyon takılmıyor, karo doğru yönde kalıyor (dört dokunuşta başa dönüyor).
- [ ] Yönler buluştukça yeşile dönüyor, bozdukça griya dönüyor.
- [ ] Düz karo iki dokunuşta aynı görünüme dönüyor (simetrik), dokunuş yine sayılıyor.
- [ ] **Baştan başla** düğmesi: tahta başlangıçtayken soluk ve basılmaz; bir dokunuştan sonra aktif. Basınca karolar başlangıç yönüne **animasyonsuz** dönüyor, "Hamle" sayacı sıfırlanmıyor.
- [ ] "En az N": gerçekten N dokunuşla çözmek mümkün, daha azıyla çözülemiyor (birkaç tahtada dene; yöntem: köşelerden ve uçlardan başla, her uç komşusuyla buluşsun).
- [ ] Tahtalar çok kolay ya da çok zor mu? Notunu al (seviyeler sonra gelecek). Her tahta çözülebiliyor, takılan tahta yok.

## 3. Kazanma
- [ ] Son açık uç bağlanınca: tüm borular yeşil, "Tüm borular bağlandı!", "N hamle · mm:ss". Haptik **başarı** (bir kez). Şimşek serisi 1 artar. Konfeti yok.
- [ ] Bittikten sonra karolara dokunmak hiçbir şey yapmıyor, baştan başla düğmesi yok.
- [ ] Kalp: favoriye ekler. Profil > Favoriler satırında "Boru bağla" ve "N hamle · süre".
- [ ] Kaybetmek yok: uygulama hiçbir durumda seriyi sıfırlamıyor.
- [ ] Süre makul: her eylem arası en fazla 15 sn sayılır (uygulamayı arka plana atıp dönmek şişirmiyor).
- [ ] Yeni tahta açılırken takılma yok (kaydırınca sayfa hemen geliyor).

## 4. Yarım kalan oyun
- [ ] Birkaç karoyu döndür, bir kez baştan başla, birkaç dokunuş daha yap, uygulamayı **tamamen kapat**, yeniden aç.
- [ ] Beklenen: aynı tahta, karolar aynı yönde, aynı "Hamle" sayısı ve "En az N".
- [ ] Bitirdiğin tahta yeniden açınca yarım oyun olarak **geri gelmiyor**.

## 5. Seri ve istatistikler
- [ ] Kazanç ortak seriyi artırır (diğer oyunlarla birlikte).
- [ ] Profil > İstatistikler: yeni **Boru bağla** bölümü (Oynanan, En iyi süre, En az hamle). Hiç oynamadıysan "-".

## 6. Ana Sayfa kişiselleştirme
- [ ] Ana Sayfa'daki oyun çiplerinde **Boru bağla** var. Kapatınca yeni sayfalarda gelmiyor, başlamış tahta kalıyor. En az bir oyun açık kalıyor.

## 7. Senkron ve hesaplar
- [ ] Google ile girişli hesapla bir tahta bitir, "Her şey yedeklendi" yazana kadar bekle.
- [ ] Supabase: `select game, mode, outcome, answer, guesses, duration_ms from game_result where game = 'pipes' order by created_at desc limit 5;` Beklenen: `game = 'pipes'`, `outcome = 'WON'`, `answer` 25 karakterlik onaltılık dizi (`3a8c...`), `guesses` hamle sayısı (tek sayı).
- [ ] Yeniden kurulumda aynı hesapla giriş: geçmiş, Profil sayıları ve favoriler geri geliyor.
- [ ] Eski sürüm: pipes satırlarını yok sayar, uygulama bozulmaz.
- [ ] Hesap silme pipes sonuçlarını da siler, birleştirme taşır.
- [ ] Telemetri: `select at, name, props from app_event where name = 'endless_finished' order by at desc limit 5;` Beklenen props: `{"game": "pipes", "outcome": "WON", "moves": "..."}`.

## 8. Haptik, erişilebilirlik
- [ ] Haptik: dokunuş hafif, baştan başla hafif, kazanma başarı.
- [ ] **VoiceOver / TalkBack:** karo "3. satır, 2. sütun, köşe parça, açık yönler: kuzey, doğu, açık uç var" ya da "bağlı" diye okunuyor; çift dokunma karoyu döndürüyor. "Hamle" ve "En az" çipleri anlamlı okunuyor, baştan başla düğmesi açıklamalı.
- [ ] **Büyük yazı** ve **yüksek kontrast:** çipler ve ipucu yazısı okunuyor, karo çerçeveleri görünüyor, tahta ekrandan taşmıyor.
- [ ] **Hareketi azalt** açıkken karolar dönüş animasyonu olmadan yön değiştiriyor.

## 9. Nasıl Oynanır?
- [ ] Sayfada "Nasıl Oynanır?" düğmesi var, kuralları (açık uç nedir, dokunmak çeyrek döndürür, yeşil/gri, kaybetmek yok, "en az") anlatıyor. Dışına dokunmak, "Anladım" ve Android geri tuşu kapatıyor. Panel açıkken arkadaki tahtaya dokunmak çalışmıyor.
