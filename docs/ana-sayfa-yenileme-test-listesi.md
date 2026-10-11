# Ana Sayfa yenilemesi test listesi (cihazda, elle)

Otomatik testler geçti (ekran görüntüleri 20-24). Bu liste **gerçek cihazda** görülmesi gerekenler için.

## 1. Ana Sayfa
- [ ] Üstte eski sade seri şeridi (alev ve şimşek) duruyor, alt gezinme çubuğu da aynı. Selamlama başlığı yok.
- [ ] **Akışın** kartı: açık oyunların ilk üçünün resmi ve adı, dördüncü yerde "+N oyun" karosu (ya da "Ekle"), "Düzenle >" bağlantısı, "Kelime dili: Türkçe >" satırı, yeşil "Akışa başla" düğmesi.
- [ ] Resimler **gerçek oyun ekranlarına benziyor** (renk, şekil, sayı): kelime tahtası, hafıza kartları, renk tüpleri, mayın tarlası, sudoku, ışıklar, kayan bulmaca, hedef sayı, borular. Hiçbiri referans uygulamadaki resimlere benzemiyor.
- [ ] "Akışa başla" Akış sekmesini açıyor (kelime klavyesi görünüyor, alt çubuk yok).
- [ ] "Düzenle", "+N oyun" karosu ve "Kelime dili" satırı akışı düzenle sayfasını açıyor.
- [ ] **Günün bulmacası** kartı: başlık, sağda TR/EN çipleri, solda kelime tahtası resmi (seçili dilin harfleriyle), "Kelime bulmacası", "Bugün herkes aynı bulmacayı çözüyor." ve çerçeveli yeşil "Bulmacayı çöz" düğmesi. Oynarken "Devam et", bitince "Sonucu gör", bağlantı yokken yalnızca açıklama. Kartın her yerine dokunmak günlük sayfayı açıyor.
- [ ] "Bugün en hızlılar" günün bulmacasının altında, "Tümü" sıralamayı açıyor.
- [ ] **Küçük telefonda** ve **büyük yazıyla**: kartlar taşmıyor, oyun adları tek satırda kesilerek sığıyor, düğmeler vurulabiliyor.

## 2. Akışı düzenle sayfası
- [ ] "Akışını oluştur", "Oynamak istediğin oyunları seç.", sağda yeşil "9 oyun seçili".
- [ ] Dokuz oyunun hepsi kart olarak iki sütunda, kaydırınca hepsine ulaşılıyor. Her kartta resim ve ad, seçili kartlarda yeşil çerçeve ve köşede yeşil onay işareti, seçili olmayanda boş halka.
- [ ] Karta dokunmak oyunu kapatıyor/açıyor, "N oyun seçili" güncelleniyor, Ana Sayfa'ya dönünce resimler değişmiş.
- [ ] Son açık oyun kapanmıyor, "En az bir oyun açık kalır." görünüyor.
- [ ] **Türkçe | English** anahtarı kelime dilini değiştiriyor; kelime resmi harfleri değişiyor, Ana Sayfa'daki "Kelime dili" satırı yeni dili gösteriyor. Dili akış başladıktan sonra değiştirmek eski davranışla aynı (başlamış sayfalar kalır).
- [ ] Altta sabit "Akışa başla" düğmesi Akış sekmesini açıyor. Geri düğmesi ve Android geri tuşu Ana Sayfa'ya dönüyor.
- [ ] Kapattığın oyunlar Akış'ta yeni sayfalarda gelmiyor.

## 3. Erişilebilirlik
- [ ] **VoiceOver / TalkBack:** kartlar oyun adıyla onay kutusu gibi okunuyor ("seçili" / "seçili değil"), resimler atlanıyor, "Düzenle", "+N oyun" ve "Kelime dili" düğme olarak okunuyor.
- [ ] **Hareketi azalt** ve **yüksek kontrast**: sayfa bozulmuyor (resimler sabit).
