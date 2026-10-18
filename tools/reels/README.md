# Reels üretici (hedef sayı + tanıtım)

Hedef sayı oyunundan Instagram/TikTok/Shorts için dikey (1080x1920, 30 fps) deneme videoları üretir:
soru, geri sayım, adım adım çözüm, seslendirme ve uygulama kartı. Görünüm uygulamanın kendi tasarımıdır
(`docs/design.md` paleti, Nunito fontu, ekran görüntüsünden ölçülen karo ve hedef kutusu).

`samples/` içinde üç örnek video var (iki hedef sayı, bir tanıtım).

## Videolar

| | Sayılar | İşlem | Süre | Not |
|---|---|---|---|---|
| **A** | 4 | 3 | ~17,5 sn | Uygulamadaki oyunla aynı kurallar |
| **B** | 6 | 5 | ~26 sn | Yalnızca reel için 6 sayılı varyant. 4 sayıyla en fazla 3 işlem yapılabildiği için uygulamada yok. B'de 1 rakamına izin var (yoksa tam 5 işlem isteyen hedef çok nadir). |

Kurallar uygulamayla aynıdır (`shared/.../game/target/TargetGenerator.kt`): iki sayı ve bir işlem, sonuç sıfırdan büyük
tam sayı olmalı (çıkarmada soldaki büyük, bölmede kalan yok), hedef 10-99 ve başlangıç sayılarından biri değil, hedef
**tam olarak** 3 (A) ya da 5 (B) işlem gerektirir. Sayılar uygulamanın tohumlarından değil, buradaki kendi üreticiden gelir.

## Kurulum

Gerekenler: Node 20+, ffmpeg (PATH'te), Python 3.10+.

```
cd tools/reels
npm install
npx playwright install chromium
pip install kokoro-onnx soundfile
```

Seslendirme modeli (yerel, internet gerekmez; ~350 MB) `tools/reels/models/` içine konur:

- https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/kokoro-v1.0.onnx
- https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/voices-v1.0.bin

(`KOKORO_DIR` ortam değişkeniyle başka bir klasör verilebilir. Windows'ta Python için `python`, diğerlerinde `python3`
kullanılır, `PYTHON` ile değiştirilebilir.)

## Kullanım

```
node build.mjs A                 # tam video  -> out/A/brainscroll_target_A.mp4
node build.mjs B
node build.mjs A stills          # sadece birkaç sabit kare (yerleşime bakmak için)
node build.mjs B --seed=12       # başka bir soru -> out/B-seed12/
python verify.py out/A/puzzle.json   # çözümü ve "daha az işlemle yapılamaz" iddiasını bağımsız doğrular
node promo.mjs                   # tanıtım reeli -> out/promo/brainscroll_promo.mp4 (~31 sn)
node promo.mjs stills            # tanıtım için sabit kareler
```

Her çalıştırma `out/<ad>/` içine `puzzle.json` (soru ve çözüm), `voice.json`, `timeline.json`, ses dosyaları ve mp4 yazar.
Yayınlamadan önce `verify.py` çalıştırmak iyi olur.

## Tanıtım reeli

Başlık: "Are you tired of downloading countless apps for various brain games?". Akış: başlık ve genel uygulama
ikonları duvarı, "What if they all lived in one feed?", altı oyunun uygulama ekranında oynanışı (Target number,
Sudoku, Memory match, Color tubes, Block Out, Pipes), "And so on." ve kapanış kartı.

Oyun ekranları `promo.html` içinde uygulamanın kodundan (`ui/<oyun>/*Page.kt`, `ui/feed/Hud.kt`, `ui/motion/Motion.kt`)
elle çevrildi: 360x640 dp ekran 2,6 kat büyütülür; ölçüler, renkler, dokunma sırası ve animasyon süreleri koddaki gibi.
Oyunlar gerçekten çalıştırılmaz, her oyunun hamleleri `promo.mjs` içindeki `SCRIPT` ile zamanlanır (dokunuş zamanları,
kazanma anı, sahne süresi). Tahtalar elle kurulmuştur (Sudoku, Block Out, Pipes uygulamanın üreticisinden gelmez).
Uygulamada bir oyunun görünümü ya da animasyonu değişirse `promo.html` içindeki ilgili `…Game` fonksiyonu güncellenmeli.
Oyun adları ayrı ses kayıtlarıdır ve sahne başlarken söylenir, böylece ekrandaki oyunla aynı anda duyulur.

## Dosyalar

- `puzzle.mjs`: soru üretici ve çözücü (genişlik öncelikli arama, en kısa çözümler).
- `build.mjs`: sıra: soru, konuşma metni, TTS, zaman çizelgesi, ses efektleri, kareler, mp4. `CONFIG` içinde varsayılan
  tohumlar ve geri sayım süreleri (A 6 sn, B 12 sn) var.
- `lib.mjs`: ortak adımlar (TTS, ses efektleri, ses karışımı, kare çizimi ve mp4).
- `promo.mjs`, `promo.html`: tanıtım reeli (zamanlama ve sahneler).
- `page.html`: hedef sayı videosunun kendisi. Her kare `render(t)` ile çizilir. Fontlar ve maskot uygulamanın klasörlerinden okunur
  (`shared/.../font/nunito_*.ttf`, `docs/assets/logo-mascot-draft.svg`).
- `tts.py`: seslendirme (kokoro-onnx, ses `af_heart`, İngilizce).
- `verify.py`: bağımsız doğrulama.

## Bilinenler

- Ses İngilizce. Altyazı yok (soru ve işlemler zaten ekranda).
- Uç kart "Coming soon" diyor; uygulama yayınlanınca `page.html` ve `promo.html` içindeki `#endPill` metni değişmeli. Tanıtımdaki "15 brain games" oyun sayısı değişirse güncellenmeli.
- Maskot hâlâ `logo-mascot-draft.svg` taslağı.
- Tasarım ölçüleri tek bir ekran görüntüsünden alındı (karo 190x190 kare, köşe ~41 px, çerçeve ~4-5 px `#565656`).
  Uygulamanın tasarımı değişirse `page.html` içindeki `.tile` ve `#targetBox` değerleri güncellenmeli.
