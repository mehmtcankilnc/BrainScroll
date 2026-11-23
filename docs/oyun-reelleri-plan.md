# Oyun reelleri planı (uygulamadaki her oyun için ayrı reel)

**Yeni oyun eklerken dokunulacak her yerin listesi: [yeni-oyun-ekleme-rehberi.md](yeni-oyun-ekleme-rehberi.md)** (dosyalar, arama kelimeleri, PNG resim üretimi, kontrol listesi, açık işler).

Hedef: TikTok ve Instagram Reels. Uygulama henüz yayında değil; videolar çıkış ya da inceleme döneminde paylaşılacak ("Coming soon" kapanışı). Dil İngilizce (global), 1080x1920, 30 fps. **Uygulamadaki 25 oyunun her biri için bir reel** çıkarılabilir: bu belge her oyunun kancasını, akışını, sesini ve süresini ayrı ayrı yazar. Üç reel (Ball Slide, Mini Nonogram, Topic Groups) daha önce ayrıntılı kararlaştırıldı, onlar en altta "hazır" olarak duruyor.

## 1. Ortak kararlar (her reel için)
- **Görünüm:** uygulamanın gerçek ekranı (kodundan çevrilmiş, `ui/<oyun>/*Page.kt`): ölçüler, renkler, animasyon süreleri ve dokunuş sırası koddaki gibi. Hepsi **zor seviyede** (uygulamada Hard). Oyunun kendi dünya rengi (glow yok, kartlar düz) ve gerçek sonuç kartı.
- **Yerleşim:** telefon ekranı biraz küçük (ölçek 2,2) ve yukarıda, üstte büyük yazı bandı (TikTok'un alt yazı alanı sonuç kartını kapatmasın). Alt kenar boş kalır.
- **Süre:** 25-38 sn, **ilk 2 saniyede kanca** (soru, geri sayım ya da çarpıcı bir görüntü). Sonuç kartı en az 3 sn ekranda kalır.
- **Kapanış kartı (hepsi):** anne maskot, "brainscroll", **"Brain games of every kind. One feed."** ve "Coming soon". Sayı yazılmaz (oyun sayısı değişiyor).
- **Ses:** üretilmiş (telifsiz) müzik yatağı + oyuna özel ses efektleri. Seslendirme yalnızca bilgi taşıyan reellerde (Topic Groups gibi); diğerleri sessiz. Ses efektleri uygulamadaki paketle (`tools/reels/sfx-pack.json`) aynı ailedir (marimba + çan), böylece reel ile uygulama aynı sesler.
- **Gerçeklik kuralı:** çözüm gerçekten çözülmüş olur (uygulamanın çözücüsünün JS karşılığı ya da kanıtlanmış tahta), sonuç kartındaki süre çözüm kısmının oynatma süresidir.
- **Ortak yazı kalıpları (alt başlık bandı):** "Can you do it in N moves?", "Beat that.", "Only 1% find it.", "Wait for it..." gibi; hiçbirinde yanıltıcı iddia yok (sayı gerçek en kısa çözümdür).

## 2. Reel kalıpları (her oyun birine girer)
- **A. Geri sayım + çözüm:** tahta açılır, "N hamlede yapabilir misin?" ve 8-12 sn geri sayım, sonra çözüm oynar, sonuç kartı, "Beat that." (Ball Slide kalıbı).
- **B. Ne çıkacak?:** tahta kısım kısım dolar, son anda sürpriz (resim, desen, yıldız) belirir (Nonogram kalıbı).
- **C. Dur ve çöz:** ipucu verilir, sessiz bir an, sonra cevap adım adım gelir (Topic Groups, Sudoku, Word).
- **D. Tatmin edici akış:** parmak izi olmadan, tek bir akıcı hareket zinciri (Lazer, Tubes, Pipes, Color Flow, Arrow Escape).
- **E. Hile gibi duran hamle:** "Impossible" görünen bir şeyi tek hamleyle çözmek (Kibrit, Tents, Queens).

## 3. Oyun oyun plan
Her satır: kalıp, kanca, akış, ses. "Sonuç" her zaman uygulamanın gerçek sonuç kartıdır.

### Kelime ve sayı
1. **Word puzzle (kalıp C):** kanca "Guess the 5-letter word in 4 tries." Akış: iki yanlış tahmin, harf renkleri uygulamadaki gibi döner, üçüncü tahminde ipucu çakışır, dördüncüde doğru kelime; tuş takımı kullanılır. Ses: her harfe yumuşak tık, doğru harfe nota, kelime bulununca akor. ~28 sn.
2. **Sudoku (kalıp C):** kanca "Only ONE number fits here." Akış: zor tahtada tek bir hücre vurgulanır, kalan sayılar elenir (geri sayım 6 sn), rakam girilir, zincirleme 4-5 hücre daha dolar. Ses: sessiz tik, her doğru rakama artan nota. ~30 sn.
3. **Jigsaw Sudoku (kalıp C):** kanca "Sudoku with crooked boxes." Akış: düzensiz kutuların renkli sınırı vurgulanır, bir kutuyu tamamlama zinciri, yan etki olarak bölge dolunca parlar. Ses: Sudoku ile aynı. ~30 sn.
4. **Target number (kalıp A):** kanca "Make 100 with these numbers." Akış: sayılar ve işlem düğmeleri, 10 sn geri sayım, çözüm adım adım (ara sonuçlar yazılır), sonuç. Ses: tik tik, işlem sesi, doğru sonuçta akor. ~28 sn.
5. **Axis Sum (kalıp A):** kanca "Row and column sums are the only clue." Akış: ipuçları vurgulanır, geri sayım, hücreler doldurulur (en kesin olandan başlar). Ses: marimba. ~30 sn.
6. **Fruit Equations (kalıp C):** kanca "🍎 + 🍎 + 🍌 = 18. What is 🍌 × 🍇?" Akış: satırlar bir bir vurgulanır, meyvenin değeri küçük baloncukla yazılır, son satır soru işareti, tuş takımıyla cevap yazılır; zor seviyede üs ve bölme çıkınca "Wait, powers?" yazısı. Yanlış cevaplı sahne tahtayı sallar (uygulamadaki gerçek sallanma). Ses: meyve baloncuğu için kısa pluck, doğru cevapta akor. ~32 sn.

### Mantık ve ızgara
7. **Queens (kalıp E):** kanca "One queen per colour. None can touch." Akış: renkli bölgeler, tek hücrelik bir bölge ilk kraliçeyi verir, zincir elemelerle yedi kraliçe yerleşir. Ses: taç sesi (pluck), son kraliçede akor. ~30 sn.
8. **Tents (kalıp E):** kanca "Every tree needs a tent. Not touching." Akış: sayı ipuçları, kesin çadırlar, çimen işaretleri, sonuçta kamp alanı. Ses: yumuşak "pof". ~30 sn.
9. **Minesweeper (kalıp C):** kanca "No guessing needed." Akış: zor tahtada ilk açılış, 1-2-1 kalıbı gibi kesin mantıkla ilerleyen sıra, bayraklar, sonunda temiz tahta. Mayına basma sahnesi yok (uygulamadaki `mines_boom` sesi kapanışta yok). Ses: tık, bayrakta kısa nota. ~30 sn.
10. **Lights Out (kalıp D):** kanca "Turn them ALL off." Akış: dolu tahta, en kısa çözüm sırasıyla dokunuşlar (çözücünün JS karşılığı), ışıklar dalga gibi söner. Ses: her dokunuşta tık, son ışık sönünce alçak "dim" akoru. ~25 sn.
11. **Mini Nonogram:** hazır, bölüm 6.
12. **Line (One Line, günlük bulmaca) (kalıp A):** kanca "Draw it in ONE line." Akış: şekil, 8 sn geri sayım, tek hamlede çizim, bölüm sonu rozeti ve seri bilgisi ("Daily puzzle. New one every day."). Ses: çizgi boyunca yükselen glissando, sonuç akoru. ~25 sn.

### Hareket ve yerleştirme
13. **Sliding puzzle (kalıp D):** kanca "This is 3 moves from solved." Akış: neredeyse çözülmüş karışık tahta, kısa çözüm, parçalar yerine oturur; ses: tahta kayma sesi. ~25 sn. (Kanca doğru hamle sayısıyla yazılır.)
14. **Tower of Hanoi (kalıp A):** kanca "7 discs. Can you do it in 127 moves?" Akış: hızlandırılmış (kayıtlı) çözüm: ilk 8 hamle gerçek hızda, sonra 4x, sonuç. Ses: her disk "thunk" (uygulamadaki `hanoi_thunk`), hızlanınca ritim. ~35 sn.
15. **Block Out (kalıp E):** kanca "Every piece. One fit." Akış: boş tahta ve parçalar, doğru parça doğru yere sürüklenir (parmak izi), son parça oturunca tahta kapanır. Ses: parça oturunca "tok". ~28 sn.
16. **Arrow Escape (kalıp D):** kanca "Tap the arrow that can escape." Akış: ok ağı, bir okun çıkışı açıktır, sırayla oklar çıkar, ağ boşalır; hızlanan ritim. Ses: whoosh + yükselen nota. ~28 sn.
17. **Color Flow (kalıp D):** kanca "Connect every colour. Fill the board." Akış: çiftler, çizgiler sırayla çekilir, son çizgi tahtayı tamamlar, hafif parlama. Ses: çizgi boyunca marimba, çift bağlanınca çan. ~28 sn.
18. **Pipes (kalıp D):** kanca "Make the water flow." Akış: dağınık borular, dokunuşlarla döndürme, su kaynaktan hedefe akar. Ses: dönüş tık, su akarken "pour" (uygulamadaki `tubes_pour` ailesi). ~28 sn.
19. **Color tubes (kalıp D):** kanca "Sort the colours. Don't get stuck." Akış: karışık tüpler, dökmeler, sonunda her tüp tek renk. Ses: `tubes_pour`. ~28 sn.

### Bellek ve dikkat
20. **Memory match (kalıp A):** kanca "Remember where they are." Akış: kartlar kısa süre açılır, kapanır, sonra çiftler tek tek bulunur, yanlış çift ters döner. Ses: çevirme tık, eşleşme nota. ~28 sn.

### Yeni dört oyun (kendi ses ve erişilebilirliğiyle)
21. **Matchsticks (kalıp E):** kanca "Move ONE stick. Fix the sum." Akış: yanlış denklem (örn. 6 + 4 = 4), 6 sn geri sayım, elin tuttuğu kibrit ışıklı gösterilir (uygulamadaki seçili çöp), tek çöp taşınır, denklem doğru olur, sonuç. Ses: çöp kaldırma (`matchsticks_pick`), bırakma (`matchsticks_place`), kazanma akoru. ~26 sn.
22. **Laser Mirrors (kalıp D):** kanca "Light every target." Akış: ışın başlar, aynalar sırayla çevrilir, ışın yolu her seferinde yeniden çizilir, hedefler tek tek yanar, son hedefte tahta yeşile döner. Ses: ayna çevirme tık (`lasers_turn`), hedef yanışı çan (`lasers_lit`), kazanma akoru. ~28 sn.
23. **Untangle (kalıp D):** kanca "No lines may cross." Akış: dolaşık düğüm, noktalar sürüklenir, kırmızı çizgiler yeşile döner, son kesişim kalkınca yeşil dalga. Ses: tutma/bırakma (`matchsticks_pick`, `untangle_drop`), çizgi azalınca çan (`untangle_fewer`), kazanma akoru. ~28 sn.
24. **Fruit Equations için isteğe bağlı ikinci kısa reel:** yalnızca üs satırı ("Wait, powers?"), 15 sn, aynı kalıp.

### Plan 4 oyunları (sahne eklenir)
26. **Yalnız Sayı (kalıp E):** kanca "Every number must stand alone." Akış: 5x5 tahta, tekrar eden sayılar kırmızı yanar, biri karalanınca kırmızıların bir kısmı söner, karalananlar birbirine değmez, son karalamada tahta sonuç kartına döner. Ses: karalamada hafif dokunuş, kazanma akoru. ~25 sn. 
27. **Sıfır-Bir (kalıp A):** kanca "Fill it with 0 and 1. Never three alike." Akış: 6x6 tahta, verili rakamlar, yan yana iki 1'in yanına 0 gelir, yarısı dolan satırın kalanı 1 olur, hata yapınca kareler kırmızı yanar, son rakamla tahta sonuç kartına döner. Ses: rakamda hafif dokunuş, kazanma akoru. ~25 sn. 
28. **Ampuller (kalıp C):** kanca "Light every cell." Akış: 5x5 tahta, ilk ampulle ışık satır ve sütun boyunca yayılır, iki ampul birbirini görünce kırmızı yanıp söner, sayılı duvar tutunca yeşile döner, son ampulde bütün tahta aydınlanır. Ses: ampul koyunca hafif "tık", son ampulde akor. ~25 sn. 
29. **Büyük-Küçük (kalıp E):** kanca "Which one is bigger?" Akış: 4x4 tahta, iki verili sayı, işaretin ucu küçük kareyi gösterir; "bu kare yanındakinden büyük" diye bir kare 4 olur, satırdaki eksik sayı tamamlanır, kırmızı işaret yeşile döner, son sayıda tahta çözülür. Ses: sayı koyunca hafif "tık", son sayıda akor. ~25 sn. 
30. **Çitler (kalıp E):** kanca "How many lines?" Akış: 4x4 tahta, noktalar ve birkaç sayı; 3 olan kareye üç çizgi çizilir, 0 olan karenin kenarlarına çarpı konur, çizgiler zincirlenir, son kenarla halka kapanır, içi dolar ve sayılar yeşile döner. Ses: çizgi koyunca hafif "tık", halka kapanınca akor. ~25 sn.

### Hazır olan üçü (ayrıntı aşağıda)
25. **Ball Slide** ve **Topic Groups**: hazır, bölüm 6.

Not: numaralar yalnızca listedir, yayın sırası değildir (bölüm 4). 25 oyunun her birinin kendi reeli var (Fruit Equations için ek bir kısa reel isteğe bağlı).

## 4. Yayın önerisi (haftada 4 video)
- Hafta 1: Ball Slide (A), Lazer Aynalar (D), Mini Nonogram (B), Kibrit Çöpü (E).
- Hafta 2: Topic Groups (C), Untangle (D), Color tubes (D), Sudoku (C).
- Hafta 3: Queens (E), Lights Out (D), Fruit Equations (C), Arrow Escape (D).
- Hafta 4: Tower of Hanoi (A), Color Flow (D), Pipes (D), Tents (E).
- Sonra: Word, Memory, Minesweeper, Jigsaw Sudoku, Target, Axis Sum, Block Out, Sliding puzzle, One Line.
- Kural: üst üste iki aynı kalıp yok; sessiz oyunlar arasında bir seslendirmeli video.

## 5. Üretim hattı (tek hat, her oyun için sahne)
- `play.html` (sahneler + kapanış, `render(t)` ile kare kare), `play.mjs` (`node play.mjs <oyun> [stills]`), `shared.js` (ortak yardımcılar: easing, HUD, çipler, düğmeler, sonuç kartı, dokunuş efekti).
- **Her yeni oyun için eklenecekler:** `tools/reels/<oyun>.mjs` (tahta seçimi ve çözüm sırası, uygulamanın çözücüsünün JS karşılığı), `play.html` içinde oyunun sahne işlevi, `sfx-pack.json`'da oyunun sesleri (uygulamadakiyle aynı dosyalar).
- `synth.py`: müzik yatağı ve ses efektleri (numpy, telifsiz). `lib.mjs` ona bağlanır.
- **Oyun başına işler (sırayla):** (1) çözücü ve tahta seçimi, (2) gerçek ekran sahnesi, (3) ses, (4) yazı bandı, (5) kapanış, (6) kontrol.
- **Kontrol:** her video için sabit kareler (yerleşim, taşma, TikTok güvenli alanı), çözümün doğrulaması, ses seviyesi (kırpılma yok), ffprobe (süre, çözünürlük, kodek).
- **Sıra önerisi:** önce çözücüsü JS'e çevrilmiş ve ses paketi hazır oyunlar (Lazer, Kibrit, Untangle, Tubes, Hanoi), sonra diğerleri.

## 6. Hazır reeller (kararlaştırıldı)
### Ball Slide ("kaç kaydırmada yaparsın?") ~32 sn, sessiz
- **Tahta:** zor seviye (6x6, 7 duvar, 29 boyanacak hücre, en kısa çözüm 11 kaydırma ve tek çözüm; tohum 665).
- **Akış:** (1) "Paint every cell." tahta açılır (3 sn). (2) "Can you do it in 11 slides?" 9 sn geri sayım (çubuk + tik, son 3 tik yüksek). (3) 11 kaydırma, parmak izi + gerçek kayma animasyonu, hücreler dolar. (4) Kazanma kartı ("Every cell is painted! 11 slides · 0:10"), yazı "11 slides. 0:10. Beat that." (5) Kapanış.
- **Ses:** hafif "vızz", boyanan her hücre için yükselen pentatonik nota, son kaydırmada akor.

### Mini Nonogram, zor 10x10 ("ne çıkacak?") ~28 sn, sessiz
- **Resim:** Roket (mavi). İpuçları: satırlar `2 · 4 · 6 · 6 · 2 2 · 1 6 1 · 8 · 2 4 2 · 1 4 1 · 1 1`.
- **Akış:** (1) "Fill the numbers." (2) 24 sürükleme, mantık çözücüsünün gerçek sırasıyla. (3) Son kare dolunca mavi renk (400 ms), "A rocket!", sonuç kartı. (4) Kapanış.
- **Ses:** vuruşta marimba, tamamlanan çizgide parlak nota, resim belirince akor.

### Topic Groups, zor ("dur ve çöz") ~38 sn, seslendirmeli
- **Konular:** Fruits, Vegetables, Gemstones, Composers.
- **Akış:** (1) Ses: "Sixteen words. Four hidden groups. Can you find them all?" (2) 12 sn geri sayım, Shuffle. (3) Dört grup seçilir, konu söylenir. (4) "All groups found! 0 mistakes · 0:19". (5) Ses: "BrainScroll. Brain games of every kind. One feed." ve kapanış.

## 7. Bilinen küçük bulgu
- Topic Groups sonuç metni `topicgroups_result` = "%1$d mistakes": 1 yanlışta "1 mistakes" çıkıyor. Uygulamada tekil/çoğul ayrımı eklenebilir (reel etkilenmez).
