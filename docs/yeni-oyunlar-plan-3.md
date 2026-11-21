# Yeni oyunlar planı 3 (çeşitliliği artırma)

**Yeni oyun eklerken dokunulacak her yerin listesi: [yeni-oyun-ekleme-rehberi.md](yeni-oyun-ekleme-rehberi.md)** (dosyalar, arama kelimeleri, PNG resim üretimi, kontrol listesi, açık işler).

Durum: **Kod Kırıcı, Dikdörtgenler ve Flaş Izgara kodlandı, testleri geçti, cihazda denendi ve beğenildi (2026-11-22). Plan tamam.** Prototip: https://claude.ai/artifact/YPwPsovrKULmZC86nMY4E5 (dört fikrin hepsi oynanabilir; kaynak yerel oturum dosyası, kalıcı kopyası yok). Uygulamaya alınacak olanlar: Kod Kırıcı, Flaş Izgara, Dikdörtgenler. Farklı Ton **askıda** (aşağıda).

Çalışma düzeni [yeni-oyunlar-plan-2.md](yeni-oyunlar-plan-2.md) ile aynı: tek oyun, bitince dur, cihazda dene, "devam" de, commit'e dokunulmaz. Ortak kararlar (süre kuralı, tahta kuralı, 3 seviye, kayıt sınırları, nasıl oynanır, rehber, kontrol listesi, ses ve TalkBack) oradaki gibidir. Reel planına da (docs/oyun-reelleri-plan.md) her oyun için bir sahne eklenir.

| Oyun | TR | EN | Kimlik | Durum |
| --- | --- | --- | --- | --- |
| 1 | Kod Kırıcı | Code Breaker | `codebreaker` | tamam (cihazda denendi; her top Wordle gibi mavi/kesik sarı halka alır) |
| 2 | Dikdörtgenler | Rectangles | `rectangles` | tamam (cihazda denendi) |
| 3 | Flaş Izgara | Flash Grid | `flashgrid` | kodlandı, cihaz bekliyor |
| (askıda) | Farklı Ton | Odd Shade | `oddshade` | **askıda** |

Önerilen sıra: Kod Kırıcı, Dikdörtgenler, Flaş Izgara (ilk ikisinin üretici ve çözücüsü yazılabilir ve mevcut altyapıya oturur).

## 1. Kod Kırıcı (Mastermind tarzı)
- Gizli renk kodunu çıkarım yoluyla bul. Her tahminde siyah nokta = doğru renk doğru yer, halka = doğru renk yanlış yer.
- **Deneme sınırı yok** (kullanıcı kararı): kaç denemede bulursa. Deneme listesi ekrana sığmazsa yukarı kayar, en yeni deneme görünür kalır; renk paleti ve "Kontrol" düğmesi altta sabit.
- Seviyeler: Kolay = 4 hane, 5 renk, tekrar yok; Orta = 4 hane, 6 renk, tekrar yok; Zor = 5 hane, 8 renk, tekrar var.
- Her renk bir **simgeyle** de gösterilir (▲ ● ■ ◆ ★ ✚ ♥ ☾), renk körleri için ve TalkBack için.
- Sonuç: deneme sayısı (hamle yerine) ve süre. Kod rastgele üretilir (kayıt kodu `answer` içinde saklanır). Bulmaca "tek çözümlü" değildir, doğası gereği her tahminde bilgi verir.
- Rehber: o ana kadarki ipuçlarıyla kesin olan bir rengi (ya da kesin yanlış bir rengi) işaretler. Çözücü: kalan olası kod sayısını hesaplar.
- Reel: kalıp A ("5 denemede kır"), geri sayım ve ipucu noktalarının yanıp sönmesi.
- **Uygulama (kodlandı):** `game/codebreaker/` (CodePuzzle, CodeGame, CodeGenerator + CodeSolver, CodeRound), `ui/codebreaker/CodeBreakerPage.kt`, `ui/howto/CodeBreakerHowTo.kt`. Kayıt: `answer` = `<renk sayısı><u|r><kod rakamları>` (örn. `6u0321`), `guesses` = `n<toplam deneme>` + son 36 tahmin (rakam metni; 256 karakter sınırı için), sonuç `guesses` = deneme sayısı. İstatistik: oynanan, en iyi süre, en az deneme. Rehber: o ana kadarki ipuçlarına uyan ilk kodu önerir (`CodeSolver`). Çözücüyü izleyen oyuncu 15 denemede her tahtayı çözüyor (testte). TalkBack: her renk/yer/deneme okunur, yeni deneme canlı bölge olarak duyurulur. Sesler: ortak ses seti (dokunuş, "iyi" sesi kontrolde, kazanma akoru); oyuna özel ses yok.

## 2. Dikdörtgenler (Shikaku)
- Tahtayı dikdörtgenlere böl; her dikdörtgende tam bir sayı olmalı ve sayı alanına eşit olmalı. Sürükleyerek çiz, dokununca sil.
- **Kullanıcı kararı:** boş bir "1" hücresine dokununca otomatik seçilir (tek hücrelik dikdörtgen).
- Tahta: 6x6 (Kolay), 7x7 (Orta), 8x8 (Zor). Üretici tahtayı rastgele dikdörtgenlere böler, sayıyı her birine yerleştirir; **tek çözüm** çözücüyle doğrulanır (prototipte doğrulama yok).
- Izgara oyunu: dış tahta çizilmez (ızgara kendisi çerçeve). Sürükleme parmakla rahat olsun diye hücre dokunma alanı geniş.
- TalkBack: imleçle hücre seç, "dikdörtgen başlat/bitir" eylemleri (Kibrit'teki yer imleci gibi).
- Reel: kalıp E ("her dikdörtgen bir sayıyı sarar").
- **Uygulama (kodlandı):** `game/rectangles/` (RectPuzzle, RectSolver, RectGenerator, RectGame, RectRound), `ui/rectangles/RectanglesPage.kt`, `ui/howto/RectanglesHowTo.kt`. Tahta 6x6 / 7x7 / 8x8 (en büyük dikdörtgen 8 / 10 / 12 hücre, en az 8 / 10 / 12 parça); üretici her tahtayı çözücüyle **tek çözüm** olarak doğrular. Kayıt: `answer` = BitText (boyut, sayı bitmap'i, sayılar 4 bit; en fazla 25 karakter), `guesses` = `n<konan dikdörtgen sayısı>` + her dikdörtgen `xywh` (4 rakam), sonuç `guesses` = konan dikdörtgen sayısı. Kurallar: dikdörtgen tam bir sayı taşır ve sayı alanına eşittir, üst üste binmez; izin verilmeyen çizim kırmızı yanıp söner (Uyarı titreşimi). Boş "1"e dokunmak otomatik seçer; dolu dikdörtgene dokunmak kaldırır. Rehber: çözümden ilk çözülebilir dikdörtgeni gösterir (ya da yanlış olanı kaldırmayı söyler). TalkBack: imleç (sonraki/önceki hücre ve sayı), "köşe koy / bitir", "buradakini kaldır". Sonuç yalnızca süre (istatistik: oynanan, en iyi süre).

## 3. Flaş Izgara (görsel hafıza)
- Birkaç kare kısa süre yanar, hafızadan dokun. Her turda bir kare artar, bir yanlış dokunuşta oyun biter.
- **Kullanıcı kararı:** doğru bilince bir sonraki turun izleme ekranı öncesi "Başlat" düğmesi çıkmaz, doğrudan izleme başlar.
- Süre klasik anlamda ölçülmez (en yüksek kare sayısı sonuçtur), bu yüzden "süre kuralı" yerine sonuç = ulaşılan kare sayısı.
- Seviyeler: Kolay = 3x3, Orta = 4x4, Zor = 5x5 (başlangıç kare sayısı ve gösterim süresi de ayrı).
- Bulmaca değil refleks/hafıza oyunu: kayıt "yarım kalan oyun" için tur numarası ve tohum yeterli.
- TalkBack: desen kare numaralarıyla sesli okunur, kareler numaralı düğmedir.
- Reel: kalıp C ("3 saniyede ezberle").
- **Uygulama (kodlandı):** `game/flashgrid/` (FlashPuzzle, FlashGame + FlashGenerator, FlashRound), `ui/flashgrid/FlashGridPage.kt`, `ui/howto/FlashGridHowTo.kt`. Seviyeler: Kolay 3x3 (3-6 kare, 4 tur), Orta 4x4 (3-9 kare, 7 tur), Zor 5x5 (4-13 kare, 10 tur). Bir yanlış kare koşuyu bitirir (sonuç: KAYIP, geçilen tur sayısı); son turu geçmek KAZANÇ. Kayıt: `answer` = `<boyut><başlangıç><son (hex)><tohum base36>`, `guesses` = `r<geçilen tur>`; her turun kareleri tohumdan türer. **Başlat düğmesi (2026-11-21, cihaz denemesinden sonra):** koşu, tahtanın üstündeki Başla düğmesiyle başlar; kareler kendiliğinden yanmaz (kullanıcı o an odakta olmayabilir). Turlar arasında düğme yok: geçilen turun kareleri 0,7 sn yeşil kalır, sonraki tur kendiliğinden yanar. Sayfa ekrana tekrar gelince ya da yarım bırakılıp dönülünce kareler **tekrar gösterilmez** (kareleri bilmeden kaydırıp dönen oyuncu doğru yerleri göremesin); altta her zaman bir "baştan başlat" düğmesi vardır, yeni karelerle (yeni tohum) Başla ekranına döner. Kayıt: `r<tur>` + `s1` (başlandı); `answer` yeni tohumla değişir. Bitmiş koşu baştan başlatılamaz.
- Eski not (turlar arası düğme yok): Yanan kareler her gösterimde (sayfa ekrana her gelişinde) yeniden gösterilir (`showCount`). Kareler yanarken dokunuş yok sayılır. İstatistik: oynanan, en iyi seri (tur). TalkBack: yanan karelerin satır/sütunları sesli okunur; kareler düğme. Sesler: bulunan kare için pentatonik merdiven (Nonogram'ın sesleri), yanlışta ortak uyarı.

## Askıya alınan: Farklı Ton (Odd Shade)
Fikir: ızgarada bir karenin tonu farklı, onu bul; ızgara büyür, fark küçülür; 14 tur. Prototip beğenildi ve çalışıyor, ama **şimdilik eklenmiyor**. Nedenleri:
- **Erişilebilirlik ve adalet sorunu:** renk körü ya da renk zayıfı oyuncular (kullanıcının kendisi dahil) zorlanır. Fark yalnızca açıklıkta olduğu için çoğu oyuncu oynayabilir (kullanıcı Orta'da 14/14'ü 10-16 sn'de yaptı), ama zor seviyede ekran parlaklığı ve göz farkı oyunu adil olmaktan çıkarıyor.
- **"Renk körü modu" fikri reddedildi:** açık/kapalı bir anahtar herkesin kolaylaştırmak için açabileceği bir yol olur. Çözüm olarak herkese tek palet (mavi, mor, sarı tonlar) ve ortak fark sınırları düşünüldü, ama bu da oyunu uygulamanın "bulmaca" kimliğinden çok bir **algı testi** yapıyor.
- **Zor seviyede 13. ve 14. turlar** (fark %2,5'te) kullanıcıda çok zordu; %3,5'e çıkarılınca bitirilebildi, ama kullanıcıya göre bu oyunun çok yapılması gerektiğini düşünmüyor.
- Üstelik bu oyun uygulamanın sakin bulmaca akışına tam oturmuyor (refleks/algı), diğer üçü oturuyor.
- Sonuç: kullanıcı daha sonra düşünecek. Karar verilirse yapılacaklar: tek palet, seviye başına en küçük fark (Kolay 9, Orta 5, Zor 3,5), yanlış dokunuşta +1 sn ceza, sonuç kartında süre ve hata sayısı.
