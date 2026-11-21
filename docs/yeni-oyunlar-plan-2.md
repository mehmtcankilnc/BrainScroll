# Yeni oyunlar planı 2 (reel odaklı)

**Yeni oyun eklerken dokunulacak her yerin listesi: [yeni-oyun-ekleme-rehberi.md](yeni-oyun-ekleme-rehberi.md)** (dosyalar, arama kelimeleri, PNG resim üretimi, kontrol listesi, açık işler).

Durum: **Kibrit Çöpü cihazda denendi, düzeltmeler yapıldı. Lazer Aynalar cihazda denendi. Düğüm Çöz cihazda denendi. Meyve Denklemi kodlandı ve testleri geçti** (cihaz denemesi bekliyor). Dört oyun da tamam.

Amaç: TikTok/Instagram'da ilk 2 saniyede anlaşılan, "yapabilir misin?" kancası olan ve zeka oyunu mantığına uyan dört yeni oyun. Önce HTML prototiple denendi (`docs/playground/oyun-denemeleri.html`), dördü de beğenildi. Sıra: Kibrit Çöpü, Lazer Aynalar, Düğüm Çöz, Emoji Denklemi (reel gücüne göre).

## Ortak kararlar
- **Oyun tahtası kuralı (2026-11-21, birlikte karar verildi):** serbest çizimli oyunlarda (Kibrit Çöpü, Düğüm Çöz) oynama alanı bir tahta kutusuyla çerçevelenir (Hanoi'nin kule kutusu gibi); hücre ızgaralı oyunlarda (Lazer Aynalar, Boru, Renk Akışı, Kayan Top...) çerçeveyi ızgaranın kendisi verir, dış tahta çizilmez. Eski oyunlara tahta eklenmez.
- **Süre (2026-11-21):** süre, sayfanın ekranda olduğu süredir (sayfa ekrana gelince başlar; başka sayfa/sekme/arka plan durdurur; çözülünce donar; düşünme sınırsız). Tek Çizgi (günlük bulmaca) hariç tüm oyunlarda geçerli. Rehber adımı karşılaştırması süreyi saymaz (`withoutClock`).
- Çalışma düzeni [yeni-oyunlar-plan.md](yeni-oyunlar-plan.md) ile aynı: tek oyun, bitince dur, cihazda dene, "devam" de. Commit'e dokunulmaz.
- Üç seviye (Kolay/Orta/Zor) hepsinde. Seviye `LEVELLED_GAMES` içinde, sunucuya yeni bir şey gerekmez (`game` sütununda kısıt yok, `difficulty` zaten var).
- Kayıt: `answer` en fazla 32, `guesses` en fazla 256 karakter. Sonuç `guesses` = hamle sayısı.
- Ortak özellikler: istatistik, favoriler, "Nasıl oynanır" (TR/EN), "Bana yol göster" rehberi, yarım kalan oyun kaydı, tek çözüm (üretici doğrular).
- Kontrol listesi (hafızadaki "new game checklist"): animasyonun ilk karesi, kazanma efekti son animasyonu bekler, titreşim, erişilebilirlik, küçük ekran, hareketi azalt, TR ve EN.

| Oyun | TR | EN | Kimlik | Durum |
| --- | --- | --- | --- | --- |
| 1 | Kibrit Çöpü | Matchsticks | `matchsticks` | tamam (cihazda denendi) |
| 2 | Lazer Aynalar | Laser Mirrors | `lasers` | tamam (cihazda denendi) |
| 3 | Düğüm Çöz | Untangle | `untangle` | tamam (cihazda denendi) |
| 4 | Meyve Denklemi | Fruit Equations | `fruitmath` | kodlandı, cihaz bekliyor |

## 1. Kibrit Çöpü (KODLANDI)
- `A ± B = C` (tek haneli) ya da `A + B = CD`: tek çöpü taşıyarak eşitliği doğru yap. Çöpe dokun, sonra noktalı boş yere dokun.
- Yanlış hamleden sonra çöpü **geri almak** gerekir. Kaybetmek yok. Hamle sayısı gösterilmez: sonuç sadece **süre** (istatistik: oyun sayısı ve en iyi süre).
- Seviyeler: Kolay = çöp aynı sembol içinde kalır (6→9), Orta = başka sembole gider, Zor = iki haneli sonuç. Hepsinde **tek çözüm** (üretici doğrular). Sol taraf 1-9 (sıfırlı eşitlik yok).
- Kayıt: `answer` = çöpler (en fazla 6 karakter), eylemler `n<hamle>` ve `s<çöp maskeleri>`. Sonuç `guesses` = hamle sayısı.
- Rehber: doğru çöp ipucu renginde yanar; elde yanlış bir çöp varsa kırmızı görünür; doğru çöp alınınca konacak yer yanar. Yanlış hamle varsa önce geri koymayı söyler.
- Görünüm: çöpler bir oyun tahtasının üstünde, köşelerde üst üste binmez; boş yerler soluk çöp olarak görünür. Dokunma alanı geniştir (aranan şey daha yakın sayılır).
- Bilinen eksik: TalkBack için çöp taşıma eylemi yok (ekran okuyucu eşitliği okur ama çöpü taşıyamaz).

## 2. Lazer Aynalar (KODLANDI)
- 5x5 / 6x6 / 7x7. Kenardan bir lazer sağa doğru çıkar; bazı hücrelerde döndürülebilir ayna ("/" ya da "\"), sarı hedefler. Aynaya dokununca çevrilir; ışın tüm hedeflerden geçince kazanılır. Kaybetmek yok, "başa al" düğmesi var.
- Seviyeler: Kolay 5x5 (3+1 ayna, 2 hedef), Orta 6x6 (5+2, 3 hedef), Zor 7x7 (7+3, 4 hedef). Başlangıçtan kazanmak için en az 2/3/4 aynanın çevrilmesi gerekir. Oyalayıcı aynalar ışının hiç uğramadığı aynalardır (hangi yönde durursa dursun kazandırır).
- Üretici: ışının yolu önce çizilir (aynalar yol üzerinde), hedefler yola konur, oyalayıcılar eklenir, sonra yönler karıştırılır: her tahta çözülebilir (çözücü tüm ayna dizilimlerini dener, "en az çevirme" bulur). Tek çözüm şart değil.
- Kayıt: `answer` = tahta (en fazla 26 karakter: boyut, lazer satırı, hücre başına ayna ve hedef bitleri), eylemler `n<çevirme>` ve `m<ayna yönleri>`. Sonuç `guesses` = çevirme sayısı (ekranda gösterilmez).
- Gösterim: yalnızca süre (Kibrit Çöpü'ndeki gibi). Hedefler ışın ulaştıkça yanar, ışın her çevirmede yeniden çizilir, çözüm kartı ışının sonunu bekler. Üstte "Yanan N / M" rozeti.
- Rehber: en az çevirmeli çözümün bir aynasını halkayla gösterir.
- Bilinen eksik: TalkBack için ayna çevirme eylemi yok, dokunma ile çalışır (hücreler büyük).

## 3. Düğüm Çöz (KODLANDI)
- 7 / 10 / 14 nokta. Noktaları parmakla sürükle, hiçbir çizgi başka bir çizgiyi kesmesin. Kesişen çizgiler kırmızı, üstte "Kesişen N" rozeti. Tahta, **parmak kalkınca** çözülmüş sayılır (sürükleme ortasında kesişme sıfıra insen de bitmez; böylece sürükleme asla yarıda kesilmez, Color Flow'daki hatanın dersi).
- Seviyeler: Kolay 7 nokta, Orta 10, Zor 14 (en az 11 çizgi / 16 / 23). Başlangıçta en az 4 / 7 / 11 çift çizgi kesişir (noktalar çembere rastgele sırayla dizilir).
- Üretici: noktalar rastgele konur, çiftler en kısadan başlayarak, hiçbir çizgiyi kesmiyorsa eklenir (kesişmesiz çizim); en kısa çizgiler tutulur, noktalar birbirine bağlanır, her noktaya en az 2 çizgi verilir. Tüm hesap tam sayıyla (yuvarlama yok).
- Kayıt: `answer` = tahta (en fazla 26 karakter: nokta sayısı, her nokta çifti için "çizgi var mı" biti, her noktanın çember sırası). Oyundaki konumlar `p...` (her koordinat 3 onaltılık hane) ve `n<hamle>` ile kaydedilir. Sonuç `guesses` = bırakılan nokta sayısı (ekranda gösterilmez).
- Gösterim: yalnızca süre. Saat ilk noktayı tutunca başlar.
- Rehber: kayıtta çözüm yok, bu yüzden çalışma anında kesişmesiz bir yerleşim bulunur (tavlama + çizgilere uzaklığı artıran cilalama), elindeki konuma döndürülüp ölçeklenerek hizalanır ve en uzaktaki nokta halkayla gösterilir; noktaya kesikli çizgiyle gidilecek yer bağlanır. Rehberi izlemek 75 tahtada da çözüme götürüyor (test).
- Bilinen eksik: TalkBack ile sürükleme yok (dokunma/sürükleme gerekir). Kenar boşluğu 40 birim: nokta tahtanın kenarına yapışmaz.

## 4. Meyve Denklemi (KODLANDI, ikinci sürüm)
- En çok 4 meyve. Satırlar zincir gibi: ilk satırda yalnızca ilk meyve, her sonraki satırda bir önceki meyve ve kendi meyvesi. Son satırdaki soru işaretinin yerine cevabı **gömülü sayı tuş takımıyla yazarsın** (1-5, 6-0, silme ve onay; kelime oyunundaki klavye gibi). Yanlış cevap kırmızı yanıp söner ve silinir, tekrar denenir; kaybetmek yok. (Çoktan seçmeli sürümde 4 seçenekten rastgele basarak %25 şansla bulunabiliyordu; tuş takımı bunu kapatıyor.)
- Seviyeler işlemlerle ayrılır (meyve sayısıyla değil): Kolay 3 meyve, yalnızca toplama (değerler 1-7); Orta 4 meyve, toplama ve çarpma (1-6, en az bir çarpma satırı); Zor 4 meyve, toplama, çarpma, bölme, kare alma (A² + B, A²) ve **üs** (1-9, en az 2 toplama dışı satır, bunlardan en az biri üs, toplamlar en çok 99). Üs satırında tabanı 2, 3 veya 4 değerli bir meyve olur ve üssü başka bir meyvedir (üs 2 veya daha büyük, sonuç 3 haneyi geçmez): "A^B" (önceki meyve taban) ya da "B^A" (kendi meyvesi taban); taban büyük, üs küçük ve yukarıda çizilir. Son satır "toplam", "A × B + C" ya da "A^B" olabilir. Bölme hep kalansız bölünür, çarpma ve kare çok büyümez.
- Her tahtanın değerleri satırlardan tek biçimde okunur (`readValues`, testte 250 tahtada doğrulanıyor; üste üs "tek sayı" olarak, tabanda "tek kök" olarak okunur).
- Yanlış cevapta tahta kısa süre yana sallanır (±12 dp, 6 adım, yaklaşık 330 ms; hareketi azalt açıkken sallanmaz, kırmızı yanıp sönme kalır).
- Kayıt: `answer` = tahta (en fazla 12 karakter), `guesses` = `m<yanlış cevap sayısı>`. (Üs eklenince son satırın kodu 2 bit oldu, eski sürümlerin kayıtları okunamaz.) Sonuç `guesses` = yanlış cevap sayısı (istatistikte "Hatasız" sayılır, ekranda gösterilmez). Eski (çoktan seçmeli) sürümün kayıtları okunamaz, yeni bulmaca başlar.
- Gösterim: yalnızca süre; satırlar bir tahta kartının içinde. Bütün satırlarda aynı meyve boyutu, cevap sayısı meyvelerden bağımsız büyüklükte (son satır küçülmez). Yazı boyutu sistem yazı ölçeğinden bağımsız.
- Rehber: meyvelerin değerlerini yazar, son satırı halkayla gösterir.
- Meyveler emoji ile çizilir (sistem fontu).
- Bilinen eksik: TalkBack satırların meyvelerini okumaz (satırlar yalnızca görsel); tuşları okur.

## Sığdırma dersi (2026-11-21)
- Alt alan (sonuç kartı, klavye) sabit yükseklikti: büyük yazıda sonuç kartı taşıp sonraki sayfanın üstünde görünüyordu. Şimdi tüm oyun sayfalarında alt alan "en az 152 dp"; kart daha uzunsa alan büyür, tahta küçülür (`heightIn(min = BottomAreaHeight)`).
- Yeni dört oyunun sayfa içeriği kırpılır değil, sığdırılır: Lazer Aynalar'ın tahtası artık yüksekliğe de göre boyutlanır; Meyve Denklemi'nin satırları sığacak boyutta çizilir.
- Küçük ekran testleri artık çözümden sonra sonuç kartının ekran içinde olduğunu ve tahtayla üst üste binmediğini de denetliyor (Kibrit Çöpü, Lazer Aynalar, Düğüm Çöz, Meyve Denklemi).
