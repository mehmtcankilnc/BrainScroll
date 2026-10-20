# Oyun reelleri planı (Ball Slide, Mini Nonogram, Topic Groups)

Hedef: TikTok ve Instagram Reels, haftada 4 video. Uygulama henüz yayında değil; videolar çıkış ya da inceleme döneminde paylaşılacak ("Coming soon" kapanışı). Dil İngilizce (global), 1080x1920, 30 fps. Üç video, üç ayrı his: ikisi "tatmin edici çözüm", biri "dur ve çöz".

## Ortak karar
- **Görünüm:** uygulamanın gerçek ekranı (kodundan çevrilmiş, `ui/<oyun>/*Page.kt`): ölçüler, renkler, animasyon süreleri ve dokunuş sırası koddaki gibi. Hepsi **zor seviyede** (uygulamada Hard).
- **Yerleşim:** telefon ekranı biraz küçük (ölçek 2,2) ve yukarıda, üstte büyük bir yazı bandı (TikTok'un alt yazı alanı sonuç kartının üstünü az kapatsın). Kapanışta anne maskot, "brainscroll" ve "Coming soon".
- **Ses:** hepsinde üretilmiş (telifsiz) müzik yatağı ve oyuna özel ses efektleri. Seslendirme yalnızca Topic Groups'ta (ve onun kapanışında), diğer ikisi sessiz.
- **Kapanış kartı (hepsi, promo dahil):** "15 brain games. One feed." yerine sayı yok: **"Brain games of every kind. One feed."** (promo reelinde seslendirme de buna uyar.)
- **Süre:** her video 25-38 sn, ilk 2 saniyede kanca.

## 1. Ball Slide ("kaç kaydırmada yaparsın?") ~32 sn, sessiz
- **Tahta:** uygulamanın zor seviyesi (6x6, 6-10 duvar, en az 8-12 kaydırma). Uygulamanın üreticisi ve çözücüsü JS'e çevrildi, tahta aranıp seçildi: **7 duvar, 29 boyanacak hücre, en kısa çözüm 11 kaydırma ve tek çözüm** (tahta tohum 665). Duvarlar dengeli dağılmış, çıkmaz yollar çok.
- **Akış:** (1) "Paint every cell." tahta açılır (3 sn). (2) "Can you do it in 11 slides?" yazısı, tahta sabit, **9 sn geri sayım** (çubuk + tik sesleri, son 3 tik yüksek). (3) Çözüm: 11 kaydırma, her biri parmak izi + topun gerçek kayma animasyonu (hücre başına 70 ms, 150-450 ms), boyanan hücreler topun geçişiyle dolar. (4) Son hücre dolunca uygulamanın kazanma kartı ("Every cell is painted! 11 slides · 0:10"), yazı: "11 slides. 0:10. Beat that." (5) Kapanış kartı.
- **Ses:** kaydırma = hafif "vızz", boyanan her hücre = pentatonik gamda yükselen küçük nota (çözüm boyunca gam yükselir, doyum hissi), son kaydırma = akor.
- **Not:** karttaki süre, çözüm kısmının oynatma süresidir (düşünme süresi değil), Target videolarındaki gibi.

## 2. Mini Nonogram, zor 10x10 ("ne çıkacak?") ~28 sn, sessiz
- **Resim:** **Roket** (zor seviye 10x10 resimlerinden). Uygulama resmin rengini kimliğinden seçiyor: roket **mavi**. Mantıkla tam çözülebilir (uygulamanın çözücüsüyle doğrulandı), ipuçları: satırlar `2 · 4 · 6 · 6 · 2 2 · 1 6 1 · 8 · 2 4 2 · 1 4 1 · 1 1`.
- **Akış:** (1) "Fill the numbers." boş tahta ve ipuçları (2 sn). (2) 24 sürükleme/dokunuş, **uygulamanın mantık çözücüsünün gerçek sırasıyla** (ilk satır/sütunlardan kesin çıkan kareler), her biri parmak izi ve uygulamadaki gibi doluyor; çizgi tamamlanınca ipucu soluyor. Yazı: "What is hiding in this grid?" (3) Son kare dolunca resim kendi rengine (mavi) döner (uygulamadaki 400 ms), ipuçları silinir, tahta hafifçe yaklaşır, arkada yumuşak bir ışık; yazı "A rocket!" ve uygulamanın sonuç kartı ("It is a rocket!"). (4) Kapanış kartı. Roket seçimi "yakında çıkıyor" mesajıyla da uyumlu.
- **Ses:** her vuruş yumuşak marimba notası, tamamlanan satır/sütun daha yüksek ve parlak nota, resim belirince yükselen akor.

## 3. Topic Groups, zor ("dur ve çöz") ~38 sn, seslendirmeli (birlikte karar verildi)
- **Konular (zor seviye kuralı: iki benzer grup + iki farklı, biri en zor katman):** Fruits, Vegetables (aynı aile, kafa karıştırır), Gemstones, Composers. Kelimeler uygulamanın listesinden, aynı kelime iki grupta yok.
- **Akış:** (1) Ses: "Sixteen words. Four hidden groups. Can you find them all?" ve yazı. (2) **12 sn geri sayım** (çubuk, tik); ortada Shuffle'a basılır, kelimeler uygulamadaki gibi kayarak yer değiştirir. (3) Dördü seçilir (seçili kelime açık renge döner, hafif büyür), grup yukarı uçar, rengini alır, konu başlığı belirir; **ses konuyu söyler** ("Fruits." ...). Dört grup, sondaki grup bir öncekinden sonra kendiliğinden gelir (uygulamadaki gibi). (4) Kazanma kartı ("All groups found! 0 mistakes · 0:19"). (5) Ses: "BrainScroll. Brain games of every kind. One feed." ve kapanış kartı.
- Yanlış deneme eklenmedi: uygulamanın metni 1 yanlışta "1 mistakes" yazıyor (çoğul düzeltmesi yok, aşağıda not).

## Üretim hattı
- `play.html` (üç sahne + kapanış, `render(t)` ile kare kare), `play.mjs` (`node play.mjs ballslide|nonogram|topics [stills]`), `shared.js` (promo ile ortak yardımcılar: easing, HUD, çipler, düğmeler, sonuç kartı, dokunuş efekti).
- `synth.py`: müzik yatağı ve ses efektleri üretici (numpy, telifsiz). `lib.mjs` ona bağlanır.
- `ballslide.mjs`, `nonogram.mjs`: uygulamanın çözücülerinin JS karşılığı (tahta seçimi ve sıralama için). 
- Kontrol: her video için sabit kareler (yerleşim, taşma, TikTok güvenli alanı), çözümün doğrulaması (Ball Slide 11 hamlede tüm hücreler boyalı, Nonogram sonunda doluluk resimle birebir), ses seviyesi (kırpılma yok, ortalama ses düzeyi), ffprobe (süre, çözünürlük, kodek).

## Bilinen küçük bulgu
- Topic Groups sonuç metni `topicgroups_result` = "%1$d mistakes": 1 yanlışta "1 mistakes" çıkıyor. İstersen uygulamada çoğul/tekil ayrımı eklenir (reel etkilenmez).
