# Faz 17 test listesi: akış, dil, günlük rotasyon (cihazda, elle)

Otomatik testler geçti (669 test, SQL doğrulaması). Bu liste **gerçek cihazda** görülmesi gerekenler için. Migration `20261011090000_daily_games.sql` Supabase'e uygulanmadan günlük sonuç gönderimi çalışmaz (uygulama hata vermez, sonucu sonra tekrar dener).

## 1. Hızlı düzeltmeler
- [ ] Akışını oluştur sayfasında kaydırma akıcı (eskiden kasıyordu).
- [ ] Sudoku resminde yalnız 3x3'lük alan, Renk tüpleri resminde tüplerin üstünde boşluk yok.
- [ ] Akışta alt menü görünüyor ve doğru sekme seçili.

## 2. Dil
- [ ] Ayarlar > Uygulama dili: açılır listede Cihaz dili / Türkçe / English. Seçince bütün metinler anında değişiyor, bulunduğun sayfa kalıyor. Uygulamayı kapatıp açınca seçim duruyor.
- [ ] Günün bulmacasında dil anahtarı yok, uygulama dilinde.
- [ ] Akışını oluştur: kelime dili TR ve EN birlikte seçilebiliyor, biri her zaman açık. İkisi açıkken kelime bulmacalarının başlığında TR/EN rozeti var.
- [ ] iOS: uygulama dilini değiştirince metinler değişiyor mu (bu yol Windows'ta denenemedi).

## 3. Akış başlığı ve seriler
- [ ] Başlık: geri, oyun adı, ?, sağda şimşek + sayı. ? o oyunun kurallarını açıyor.
- [ ] Atla yok. Bitmemiş bulmacadan serbestçe kaydırılıyor, geri gelince kaldığı yerden devam.
- [ ] Şimşek, ana sayfadaki iki seri, profildeki iki kutu ve günlük ekranındaki alev açıklama modalını açıyor. Modallar dilde doğru.

## 4. Konfeti ve mayın
- [ ] Her oyunda kazanınca konfeti, kaybedince yok, kaydırıp geri gelince tekrar yağmıyor.
- [ ] Mayına basınca flaş, şok halkası, kıvılcımlar ve tahtada kısa sarsıntı. Açılan hücreler ve bayrak "pop" ile beliriyor.

## 5. Profil, sıralama
- [ ] Profil: hesap kartı (misafir / Google / yedek durumu), iki seri kutusu, üç sayı, açıklamalı liste.
- [ ] Sıralama: geri okunun yanında başlık; Hız / Seri / Arkadaşlar tek kontrol; seçenekler hep görünür ve adları üstünde.

## 6. Günün bulmacası rotasyonu
- [ ] Ana sayfa kartında günün oyunu (resim + ad). Ertesi gün başka oyun, bir tur (9 gün) bitmeden tekrar yok. İki telefonda aynı gün aynı oyun.
- [ ] Kelime dışı günde: karta dokununca oyun, başlıkta oyun adı + "Bugünün bulmacası", ?, alev. Tahta iki telefonda aynı. Yarım bırakıp çıkınca kaldığı yerden devam.
- [ ] Bitirince bitiş kartında "Bugünlük bu kadar..." yazıyor (kaydır ipucu yok), konfeti (kazanınca). Gün serisi artıyor.
- [ ] Uygulamayı kapatıp açınca bitmiş bulmaca için sonuç kartı ve sıradaki bulmaca geri sayımı.
- [ ] Uçak modunda bitir: oyun çalışıyor, bağlantı gelince sonuç sıralamada görünüyor (migration uygulandıktan sonra).
- [ ] Sıralama > Hız > Bugün: "Bugünün oyunu: X", süre ve hamle. Tüm zamanlar: oyun seçici, kelime seçilince dil seçici çıkıyor.

## 7. İpucu ve rehber (akış)
- [ ] Bitmemiş bulmacanın altında İpucu ve Bana yol göster düğmeleri var; bulmaca bitince kayboluyor. Günlük bulmacada yok.
- [ ] İpucu: oyunun genel taktiği çıkıyor, kapatınca hiçbir şey değişmemiş. Seri aynı.
- [ ] Rehber, her oyunda: halka bir hücreyi/kartı/tüpü gösteriyor ve üstte bir cümle var ("Rehber kullanıldı: bu bulmaca kazanma serini değiştirmez" dahil). Bir hamle yapınca halka ve cümle kayboluyor, tekrar basınca sonraki adım geliyor.
  - Mayın: ilk basışta ortadaki hücre, sonra güvenli hücre / bayrak konacak hücre.
  - Sudoku: tek rakam sığan hücre ya da rakamın tek yeri; yanlış rakam varsa önce silmeni söylüyor.
  - Tüpler: önce alınacak tüp halkalı, ona dokununca dökülecek tüp halkalanıyor. Takılı konumda "takıldın" diyor.
  - Hedef sayı: "3 × 4 işlemini yap" gibi, doğru iki sayı halkalı.
  - Kayan bulmaca, boru, ışıklar, hafıza, kelime de adım veriyor (kelime: "3. harf A").
  - Kayan bulmacada zor bir konumda rehber bir an düşünebilir; ekran donmamalı.
- [ ] Rehberle bitirdiğin bulmaca: kazanma serisi (şimşek) ne artıyor ne sıfırlanıyor; profilde o oyunun istatistiklerinde ve en iyi sürede görünmüyor. Uygulamayı kapatıp açınca da böyle.

## 8. Ana sayfa ve akış başlangıcı
- [ ] Sıra: seriler, Günün bulmacası, (yarım akış varsa) Önceki akışa devam et, Akışın, en hızlılar.
- [ ] Akışın kartında düzenle butonu ve kelime dili satırı yok; "Akışa başla" yazısında ok yok. Karta (başlığa, resimlere, boş yere) dokununca düzenleme ekranı açılıyor, Akışa başla'ya dokununca akış başlıyor.
- [ ] Bir bulmacaya harf yazıp ana sayfaya dön: "Önceki akışa devam et" çıkıyor, dokununca o bulmacaya, yazdığın harflerle dönüyor.
- [ ] Aynı durumda Akışa başla: yeni akış, yazdıkların silinmiş, "devam et" düğmesi kayboluyor.
- [ ] Alt menüden Akış: yarım bulmaca varsa ona dönüyor; yoksa seçtiğin oyunlardan yeni akış (bir oyunu kapatıp dene). Uygulamayı kapat-aç: oyun ve dil seçimleri duruyor; yeni kurulumda bütün oyunlar seçili.
