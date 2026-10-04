# BrainScroll

Kaydırılabilir bir akışta hızlı zeka oyunları: kelime bulmacası, Sudoku, hafıza eşleştirme ve satranç bulmacası. Seri tut, istatistiklerini takip et, favori kaydet, lider tablosunda yarış.

Kotlin Multiplatform + Compose Multiplatform (Android, iOS), Supabase backend.

> Durum: erken aşama. Proje iskeleti kuruldu, oyunlar henüz yok.

## Proje yapısı

- `shared/`: Android, iOS ve Desktop'ın paylaştığı Kotlin ve Compose kodu (`commonMain`, platforma özel `androidMain`, `iosMain`, `jvmMain`)
- `androidApp/`: Android uygulama giriş noktası
- `iosApp/`: iOS uygulama giriş noktası (Xcode projesi, CI'da macOS ile derlenir)
- `desktopApp/`: Desktop (JVM) giriş noktası, geliştirme için
- `site/`: playbrainscroll.com statik sitesi

## Çalıştırma

- Android: `./gradlew :androidApp:assembleDebug` (ya da Android Studio'dan çalıştır)
- Desktop: `./gradlew :desktopApp:run` (sıcak yenileme: `./gradlew :desktopApp:hotRun --auto`)
- Testler: `./gradlew :shared:jvmTest` ve `./gradlew :shared:testAndroidHostTest`
- iOS: yalnızca macOS'ta Xcode ile (bizde CI üzerinden)

## Belgeler

- [Kararlar](docs/decisions.md)
- [Faz planı](docs/plan.md)
- [Tasarım ve tokenlar](docs/design.md)

## Lisans

[MIT](LICENSE)
