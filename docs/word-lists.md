# Kelime listesi kaynakları ve lisans araştırması

Tarih: 2026-10-04 | Kapsam: Kelime bulmacası (5 harf) için TR ve EN listeleri
Bu belge hukuki danışmanlık değildir. Aşağıdaki bilgiler 4 Ekim 2026'da yapılan web araştırmasına dayanır. **Doğrulandı** işaretli maddeler ilgili sayfadan okundu, **Doğrulanmadı** işaretliler okunamadı ya da kaynak belirsiz. Bir listeyi projeye almadan önce lisans dosyasını ve kaynağını yeniden kontrol et.

## Neye ihtiyacımız var

| Liste | Ne için | Nerede durur |
|---|---|---|
| **Geçerli tahmin sözlüğü** (5 harfli tüm geçerli kelimeler, çekimli biçimler dahil) | Oyuncunun yazdığı kelimenin geçerli olup olmadığını denetlemek | Uygulama içinde gömülü (derlenen pakete girer), repoda da görünür |
| **Cevap havuzu (sonsuz akış)** | Yerelde üretilen oyunlar için cevap seçmek | Uygulama içinde gömülü, tanınan/yaygın kelimeler |
| **Günlük bulmaca cevapları** | Günlük bulmaca | **Sunucuda gizli** (Supabase), repoya ve uygulamaya girmez |

Önemli sonuç: Günlük cevapları sunucuda olduğu için **bu listelerin lisansı yalnızca ilk iki liste için kritik**. İkisi de uygulama paketine girdiği için lisansın **ikili (derlenmiş) dağıtıma izin vermesi** şart.

## İngilizce

| Kaynak | Lisans | Durum | Uygunluk |
|---|---|---|---|
| **SCOWL v2** ([en-wl/wordlist](https://github.com/en-wl/wordlist)) | MIT benzeri. Birçok kaynaktan, BSD uyumlu lisanslarla derlenmiş | Doğrulandı | **Uygun, önerilen.** Boyut seviyeleri (35 küçük ... 85 nadir) yaygınlığı verir: cevap havuzu için küçük bir seviye, geçerli tahminler için büyük bir seviye seçilir. |
| **ENABLE** | Kamu malı (SCOWL'ün kaynaklarından biri, orada belgeli) | Doğrulandı | **Uygun**, çapraz kontrol ve geçerli tahmin sözlüğü için. |
| Moby Words, 12dicts | Kamu malı (SCOWL kaynak listesinde) | Doğrulandı | Uygun, ama gerek yok. |
| NYT Wordle'ın orijinal listeleri | Telifli (The New York Times) | Doğrulandı (marka/telif gerçeği) | **Kullanma.** |
| Collins Scrabble Words, TWL | Telifli | Doğrulanmadı | **Kullanma.** |

**SCOWL'ün şartları:** Telif bildirimleri korunmalı. Debian'ın SCOWL telif dosyasına göre bazı kaynaklar ek bildirim ister (WordNet için Princeton telif ve lisans metni, UKACD için bildirim metni birebir, Ispell için BSD tarzı bildirim). Bu yüzden repoda bir `THIRD_PARTY_NOTICES.md` tutup SCOWL'ün `Copyright` dosyasındaki ilgili metinleri eksiksiz eklemeliyiz.

**Öneri:** SCOWL v2'yi kaynak al. Cevap havuzu için 5 harfli, düşük boyut seviyeli, uygunsuz kelime filtresinden geçmiş bir alt küme. Geçerli tahmin sözlüğü için 5 harfli, daha yüksek seviyeli bir küme. ENABLE ile çapraz kontrol et.

## Türkçe (açık nokta)

Türkçe tarafı İngilizceden çok daha belirsiz.

| Kaynak | Lisans | Durum | Uygunluk |
|---|---|---|---|
| **hunspell-tr** ([tdd-ai/hunspell-tr](https://github.com/tdd-ai/hunspell-tr)) | **MPL-2.0** (repodaki LICENSE dosyası). Arama özetleri MIT diyordu, repo dosyası MPL-2.0 gösteriyor, **dosyaya güven** | Doğrulandı (lisans). Kaynak belirsiz | **Güçlü aday.** Kaynağı "Türkçe metin derlemleri ve sözlüklerden çıkarılmış kelime listeleri" diye geçiyor, hangi sözlüklerden (ör. TDK) olduğu belirtilmemiş. |
| **Zemberek-NLP** ([ahmetaa/zemberek-nlp](https://github.com/ahmetaa/zemberek-nlp)) | Kod Apache-2.0. **Sözlük (lexicon) verisinin ayrı lisansı** okuduğum sayfada belirtilmemiş | Kod lisansı doğrulandı, **veri lisansı doğrulanmadı** | Kullanmadan önce depodaki veri dosyalarının lisansını ve kaynağını doğrudan kontrol et. |
| **FrequencyWords** ([hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords)) | Veri **CC BY-SA 4.0**, kod MIT. OpenSubtitles'tan türetilmiş | Doğrulandı (lisans). Türkçe liste bulunduğu **doğrulanmadı** | Yaygınlık sıralaması için faydalı, ama **share-alike** şartı var. Uygulama paketine gömülecek veride bu şartın nasıl işleyeceği net değil, **kaçınmak daha güvenli**. |
| **TDK Güncel Türkçe Sözlük** | TDK'ya ait, açık lisans yok | Doğrulanmadı | **Kopyalama.** Türkçe Wordle uygulamalarının kelimeleri TDK'dan aldığı basında geçiyor, bu o listelerin kullanılabileceği anlamına gelmez. |
| **Hugo0/wordle** (MIT) | Depo MIT. Listeler çoğunlukla `wooorm/dictionaries` (Hunspell tabanlı) ve FrequencyWords kaynaklı. Türkçe için ayrı lisans belirtilmemiş | Doğrulandı (depo lisansı). Türkçe listenin lisansı **doğrulanmadı** | Depo lisansı listeyi kapsamıyor olabilir. **Kullanma**, kaynağı oradan izle. |
| TS Corpus kelime listesi (~3,2 milyon benzersiz kelime) | Doğrulanmadı | Doğrulanmadı | Çok geniş, ham derlem kelimesi, oyun için uygun değil. |

**Teknik not:** Hunspell sözlükleri çekimli biçimleri değil **kökleri ve ek kurallarını** tutar. Geçerli tahmin sözlüğü için "evler", "gözün" gibi çekimli 5 harfli biçimleri de içermesi gerekir. Bu yüzden kökleri ek kurallarıyla açan (`unmunch` benzeri) bir adım gerekir. Bu aşama Faz 2'nin içine girer.

**Öneri (geçici):**
1. **Geçerli tahmin sözlüğü** için hunspell-tr'yi aday al. MPL-2.0, dosya düzeyinde copyleft'tir: verilen dosyayı **değiştirmeden** kullanırsan MIT'li kodla bir arada dağıtmak sorun olmaz, ama sözlük dosyası MPL-2.0 olarak kalır, lisans metni ve kaynağa bağlantı bildirimde yer alır. Dosyayı değiştirip dağıtırsan değişiklikleri MPL-2.0 altında paylaşmak zorundasın.
2. **Cevap havuzu** için ayrıca **kendi elle derlediğin liste** en güvenli yol: hunspell-tr'den 5 harfli kökleri çıkar, sen (anadil konuşuru olarak) yaygın ve tanınan olanları işaretle. Bu hem kalite kontrolü (zor, nadir ya da uygunsuz kelimeleri ayıklamak) hem de listeyi kendi eserin yapmak için iyi. Birkaç bin kelimelik bir inceleme, tek seferlik bir iş.
3. **Kaynak belirsizliğini kapat:** hunspell-tr deposunda kaynağını sormak için bir issue aç ya da yazarlara ulaş (hangi sözlükler ve derlemler kullanıldı, TDK verisi var mı).

## Karar (2026-10-04)

**Türkçe: seçenek A + kendi cevap havuzumuz.**
- **Geçerli tahmin sözlüğü:** hunspell-tr (MPL-2.0), dosya **değiştirilmeden** kullanılır, lisans metni ve kaynak bağlantısı `THIRD_PARTY_NOTICES.md` içinde yer alır.
- **Cevap havuzu:** Kendi derlememiz. Betik hunspell-tr'den 5 harfli kökleri çıkarır, Claude adayları sınıflandırır (yaygın/nadir/özel isim/uygunsuz/ek), Mehmetcan örneklem kontrolü yapar ve itirazları düzeltir.
- **Kaynak belirsizliği kabul edildi:** hunspell-tr'nin hangi sözlüklerden türetildiği (TDK verisi olup olmadığı) doğrulanmayacak, hukuki kesinlik aranmıyor. Bu bilinçli bir risk kabulüdür. Risk istenirse sonradan B/C seçeneğine dönülerek ya da hunspell-tr deposuna kaynağı soran bir issue açılarak azaltılabilir.

## Açık kararlar

- **Veri dosyaları repoda mı olacak:** Public repoda tutmak şeffaf ama lisans yükümlülüklerini görünür kılar. Alternatif: dosyaları repoda tutmayıp derleme sırasında indiren bir betik kullanmak. Uygulama paketinin içindeki dağıtım yine lisansa tabidir.
- **Uygunsuz kelime filtresi:** Cevap havuzundan küfür ve hassas kelimeleri ayıklamak için hem TR hem EN için bir filtre listesi gerekir.
- **Hukuki gözden geçirme:** Mağaza yayınından önce lisans bildirimlerini (THIRD_PARTY_NOTICES.md) bir kez daha gözden geçirmek.

## Sonraki adımlar

- [x] `THIRD_PARTY_NOTICES.md`: SCOWL bildirimi eklendi (hunspell-tr bildirimi TR listesiyle gelecek).
- [ ] ~~hunspell-tr kaynağı hakkında depoya issue aç~~ (isteğe bağlı, risk kabul edildi, yapılmayacak).
- [x] SCOWL'den 5 harfli alt kümeleri üreten betik: `tools/wordlists/build_en.py` (Faz 2). Geçerli tahmin: boyut ≤80 (8813 kelime). Cevap havuzu: boyut ≤35, çekim/romen rakamı/engel listesi ayıklanmış (1986 kelime). Elle engel listesi: `tools/wordlists/blocklist_en.txt`.
- [x] hunspell-tr'yi açan betik ve 5 harf süzgeci: `tools/wordlists/build_tr.py` (Faz 2). Her son ek bayrağı tek bir ekle eşleştiği için yalnızca 5 harfli biçimler üretilir: 25.160 kelime. Bu liste hunspell-tr'den türetildiği için MPL-2.0 olarak kalır, `THIRD_PARTY_NOTICES.md`'de belirtildi. Not: sözlük gürültülü (ör. `ojcik`), bu bilinçli kabul edilen riskin parçası.
- [x] Türkçe cevap havuzu: 6 parçada okunan 10.663 aday kökten 1.102 kelime kaldı (2026-10-05: Mehmetcan tamamını 100'erli parçalarla gözden geçirdi, onaylandı; emir kipleri ve `depre` çıkarıldı. Kural: emir kipi cevap olmaz) (`tools/wordlists/tr_answers_curated.txt`). Sonradan itiraz çıkarsa kelime bu dosyadan silinip betik yeniden çalıştırılır.
