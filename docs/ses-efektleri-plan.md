# Ses efektleri planı: Ball Slide ve Mini Nonogram

**Yeni oyun eklerken dokunulacak her yerin listesi: [yeni-oyun-ekleme-rehberi.md](yeni-oyun-ekleme-rehberi.md)** (dosyalar, arama kelimeleri, PNG resim üretimi, kontrol listesi, açık işler).

Amaç: reel videolarındaki ses efektlerinin (müzik hariç) uygulamada da çalması. Bu dosya ayrı bir oturumda uygulanmak üzere yazıldı. Uygulamada şu an hiç ses altyapısı yok; yalnızca titreşim (`Haptics`) var.

## 1. Ses dosyaları

Hazır paket: `brainscroll-sfx.zip` (33 dosya, toplam ~2 MB, mono, 44.1 kHz, 16 bit WAV). Telif sorunu yok: hepsi `tools/reels/synth.py` ile sıfırdan sentezleniyor (örnek/sample yok).

Yeniden üretmek veya ayarlamak için: `python3 tools/reels/synth.py tools/reels/sfx-pack.json <çıktı klasörü>` (spec: `tools/reels/sfx-pack.json`). Sesi değiştirmek istersek spec'i düzenleyip tekrar çalıştırırız; stereo çıkar, mono'ya `ffmpeg -ac 1` ile indirilir.

Dosyalar şu an `docs/assets/sfx/` içinde duruyor (uygulamaya henüz bağlanmadı). Uygulayan oturum bunları `shared/src/commonMain/composeResources/files/sfx/` altına kopyalasın (mevcut `files/words` ile aynı mantık). WAV seçildi çünkü Android, iOS ve masaüstünde ek kodek gerektirmeden çalıyor; boyut zaten küçük.

| Dosya | Süre | Ne zaman | Çalma seviyesi |
|---|---|---|---|
| `ballslide_whoosh.wav` | 0.32 sn | Her kaydırma başlarken | 0.50 |
| `ballslide_cell_00..09.wav` | 0.60 sn | Top bir hücreyi geçerken (boyanan hücre) | 0.34 + 0.02 × (kaydırmadaki hücre sırası) |
| `win_ballslide_chord.wav` | 1.8 sn | Bölüm bitince (sonuç kartıyla birlikte) | 0.55 |
| `win_ballslide_sweep.wav` | 1.0 sn | Chord'dan 0.1 sn sonra | 0.40 |
| `nonogram_fill_00..09.wav` | 0.50 sn | Doldurulan her hücre | 0.30 |
| `nonogram_line_00..07.wav` | 0.80 sn | Bir satır/sütun tamamlanınca (çıngırak) | 0.30 |
| `win_nonogram_chord.wav` | 2.2 sn | Resim tamamlanınca | 0.55 |
| `win_nonogram_sweep.wav` | 1.2 sn | Chord'dan 0.1 sn önce başlar | 0.45 |

Çalma seviyesi, dosyanın kendi seviyesinin üstüne uygulanan çarpandır (0..1). Videolardaki karışımla birebir aynı.

## 2. Nota merdiveni (sesin "tatmin edici" olmasının sebebi)

Hücre sesleri bir pentatonik merdivenin notaları. Hangi sırayla çalarsa çalsın uyumlu duyulur. Merdiven: majör pentatonik, her 5 notada bir oktav yukarı.

- Ball Slide: dosya `ballslide_cell_XX`, XX = `baslangic + hücre sırası`. Videoda `baslangic = floor(kaydırma_no × 0.7)`. Yani her kaydırma bir öncekinin biraz yukarısından başlıyor ve bölüm ilerledikçe ton yükseliyor. En fazla 10 nota var (00–09), sonuna gelince `min(index, 9)` kullanılsın (ya da 9'da kalsın).
- Nonogram hücre: `nonogram_fill_XX`, XX = `floor(çizgi_no / 3) + hücre_sırası` (çizgi = parmağın tek hareketi, hücre sırası = o hareketteki sıra). 9'dan sonra 9'da kalsın.
- Nonogram satır/sütun bitişi: `nonogram_line_XX`, XX = o oyunda şimdiye kadar çalınan çıngırak sayısı mod 8. Aynı anda birden fazla satır/sütun biterse (aralarında 0.12 sn'den az varsa) yalnızca bir çıngırak çal.

Bunlar uygulama sayaçlarıdır, durumu (state) bozmasın: sayaç UI tarafında `remember` ile tutulsun, bölüm yeniden başlayınca sıfırlansın.

## 3. Uygulamada nereye bağlanacak

Bugünkü titreşim sistemi örnek alınacak: `ui/haptics/Haptics.kt` içinde `HapticKind` + `LocalHaptics` (varsayılanı hiçbir şey yapmayan `NoHaptics`) + `expect fun rememberPlatformHaptics()` ve her platform için ayrı `actual` dosyası var. Ses için aynı kalıp:

1. `ui/sound/SoundEffects.kt` (commonMain): `fun interface SoundEffects { fun play(sound: Sfx, volume: Float) }`, `enum class Sfx` (yukarıdaki tablodaki gruplar), `LocalSoundEffects` (varsayılan `NoSoundEffects`, test ve preview için sessiz), `expect fun rememberPlatformSoundEffects(): SoundEffects`.
2. Android (`androidMain`): `SoundPool` (kısa efektler için doğru araç, düşük gecikme, aynı anda birden çok ses). `USAGE_GAME`, `maxStreams` ~8. Dosyalar açılışta yüklenip `load` bitince çalınabilir olsun.
3. iOS (`iosMain`): `AVAudioPlayer` havuzu veya `AVAudioEngine` + `AVAudioPlayerNode`. Ses kategorisi `AVAudioSessionCategoryAmbient` olsun: telefon sessiz moddayken çalmasın, kullanıcının müziğini kesmesin.
4. Masaüstü (`jvmMain`): `javax.sound.sampled.Clip` ile çal veya şimdilik hiçbir şey yapma (haptics de masaüstünde hiçbir şey yapmıyor).
5. `App.kt` içinde `LocalHaptics provides ...` satırının yanına `LocalSoundEffects provides rememberPlatformSoundEffects()`.
6. Dosyalar `Res.readBytes("files/sfx/...")` ile okunur (Compose Resources).

Çağrı yerleri:

- **Ball Slide** (`ui/ballslide/BallSlidePage.kt`):
  - `latestSlide` içinde `BallSlideResult.Moved/Solved` dönünce `ballslide_whoosh` (şu an orada titreşim var).
  - Top bir hücreden geçerken hücre sesi: kaydırma animasyonu `tween(duration(path.size), FastOutSlowInEasing)` ve ilerleme `Animatable` (progress 0..path.size). `progress` her `j + 0.35` eşiğini geçtiğinde j. hücrenin sesini çal (ör. `snapshotFlow { progress.value }` ile, geçilen son hücre indeksini tutarak). Videoda bu eşik aynen böyle: top hücreye girince değil, hücrenin ortasına yaklaşınca çalıyor.
  - Geri alma (undo) animasyonunda hücre sesi çalmasın (yalnız hafif bir whoosh çalınabilir; karar verilecek, öneri: sessiz).
  - Kazanınca: `OutcomeHaptics` ile aynı yerde, yani `rememberSettledStatus` Won olduğunda `win_ballslide_chord`, 0.1 sn sonra `win_ballslide_sweep`. Önceden bitmiş bir bölüm sayfaya dönüldüğünde (restore) çalmasın: `OutcomeHaptics` bunu zaten `wasPlaying` ile ayırıyor, aynı mantık.
- **Nonogram** (`ui/nonogram/NonogramPage.kt`):
  - Parmak hareketi bitip `onPaint(...) != NonogramResult.Ignored` olunca (şu an orada titreşim var) hücreleri sırayla çal: her hücre arasında ~80 ms (videodaki `perCell`). Tek dokunuşta tek nota.
  - Yalnızca hücre doldurmada çal (`NonogramMark.FILLED`); çarpı koymada ve silmede sessiz (ya da çok kısa tik, karar verilecek, öneri: sessiz).
  - Satır/sütun tamamlanınca: `LaunchedEffect(state.rowDone, state.columnDone)` zaten yeni biten satırı buluyor; orada çıngırak çal (kurallar yukarıda).
  - Kazanınca: `win_nonogram_sweep`, 0.1 sn sonra `win_nonogram_chord` (resmin renk değişimiyle beraber). Videoda sweep, chord'dan 0.1 sn önce başlıyor.

## 4. Ayar ve erişilebilirlik

- **Sessiz anahtar:** Profil/ayarlar ekranına "Ses efektleri" açma/kapama ekle (varsayılan açık). Ayar hâlihazırda titreşim için nasıl saklanıyorsa öyle saklansın (Proje içinde titreşim ayarı bulunamadıysa, mevcut ayar deposuna yeni bir bool eklenir; uygulayan oturum kontrol etsin).
- **Reduce motion** ses için ayrı bir kural gerektirmez; ayrı bir ayar varsa ona bağla.
- **Ekran okuyucu:** TalkBack/VoiceOver açıkken efektler konuşmayı bozmasın; nonogram'daki "satır tamamlandı" duyurusu zaten var, çıngırak onu bastırmasın (düşük seviye, kısa).
- Telefon sessizdeyse (iOS ambient, Android ringer) çalmasın; sistem sesi kapalıysa ayrıca bir şey yapmaya gerek yok.

## 5. Test

- `LocalSoundEffects` varsayılanı sessiz olduğu için mevcut Compose UI testleri değişmeden geçmeli.
- Birim test: sayaç mantığı (hangi dosya çalınacak) saf bir fonksiyon olarak ayrılsın: `ballSlideCellSound(slideNo, cellNo)`, `nonogramFillSound(strokeNo, cellNo)`, `nonogramLineSound(count)`. Sahte `SoundEffects` ile hangi sesin kaç kez çalındığı doğrulanır.
- Elle kontrol: gerçek cihazda hızlı peş peşe kaydırma (sesler üst üste binmeli ama kesilmemeli, `maxStreams` yeter mi), uygulama arka plana gidince sesin durması, bölüm yeniden başlatma.

## 6. Sonraki oyunlar

Aynı paket yapısı diğer oyunlara kolayca eklenir (`<oyun>_*.wav`). Yeni ses lazımsa `sfx-pack.json`'a eklenip `synth.py` ile üretilir; böylece tüm sesler aynı "marimba + çıngırak" ailesinde kalır. Reel ses tasarımı da aynı kaynaktan çıktığı için videolardaki ve uygulamadaki sesler hep eşleşir.

## Yeni dört oyunun sesleri (2026-11-21)
- Kibrit: çöp alma `matchsticks_pick`, bırakma `matchsticks_place`. Lazer: ayna çevirme `lasers_turn`, her yeni yanan hedef `lasers_lit` (ışın yarı yoldayken). Untangle: tutma `matchsticks_pick`, bırakma `untangle_drop`, kesişim azalınca `untangle_fewer`. Meyve Denklemi: ortak sesler (tuş tık, yanlış uyarı, kazanma akoru).
- Titreşim aynı kaldı (Light/Warning + kazanma); bu sayfalar ortak sesleri `withoutSound()` ile kapatıp kendi sesini çalar. Geri yüklenen tahta sessiz kalır.
- TalkBack: Lazer'de her ayna için "çevir" eylemi; Untangle'da "sonraki nokta" + dört yönde adım; Kibrit'te yer imleci (sonraki/önceki yer, "çöpü al/koy") ve okunan yer; Meyve'de her satır sesli okunur ("… eşittir …").
