# Faz 8 test listesi (cihazda, elle)

Otomatik testler (JVM, SQL, canlı kontrol) geçti. Bu liste **gerçek cihazda** görülmesi gerekenler için. Her adımda "Beklenen" yazıyor, farklıysa not al. Kutuları işaretleyerek ilerle.

## 0. Hazırlık

- [ ] Son commit TestFlight'a çıktı ve iPhone'a yüklendi (App Store Connect > TestFlight > yeni build).
- [ ] Android'de son sürüm yüklü (`./gradlew :androidApp:installDebug` ya da APK).
- [ ] Elinde **iki Google hesabı** var (A: ana test hesabı, B: silme testi için harcanabilir).
- [ ] Mümkünse **ikinci bir cihaz** ya da aynı cihazda uygulamayı silip yeniden kurma imkânı (birleştirme testi için).
- [ ] Supabase panelinde SQL editörü açık (telemetri ve birleştirme kontrolleri için).
- [ ] Sistem ayarları nerede: iOS `Ayarlar > Erişilebilirlik`, `Ayarlar > Ekran ve Parlaklık`, `Ayarlar > Ses ve Haptikler`. Android `Ayarlar > Erişilebilirlik`, `Ekran`, `Ses ve titreşim`.

---

## 1. Haptik (titreşim)

Cihazda **sistem haptikleri/dokunma geri bildirimi açık** olsun (iOS: Ses ve Haptikler > Sistem Haptikleri; Android: Ses ve titreşim > Dokunma geri bildirimi). Sessiz modda iPhone'da haptikler yine çalışır.

- [ ] **Tuş:** Akış sekmesinde klavyede bir harfe bas. Beklenen: her harfte hafif tık. Silme ve GİR tuşunda da.
- [ ] **Sekme:** Alt çubukta başka bir sekmeye geç. Beklenen: hafif tık. Zaten açık olan sekmeye tekrar basınca **tık olmamalı**.
- [ ] **Favori kalbi:** Biten bir bulmacanın sonuç kartında kalbe bas. Beklenen: hafif tık (hem doldururken hem boşaltırken).
- [ ] **Reddedilen tahmin:** 3 harf yazıp GİR'e bas ("Yeterli harf yok"). Beklenen: tuş tıkından sonra bir **uyarı** titreşimi.
- [ ] **Listede olmayan kelime:** "ZZZZZ" yaz, GİR. Beklenen: uyarı titreşimi.
- [ ] **Kazanma:** Bir bulmacayı çöz. Beklenen: **başarı** titreşimi (iOS'ta belirgin çift vuruş, Android'de onay hissi).
- [ ] **Kaybetme:** Bir bulmacayı 6 yanlış tahminle kaybet. Beklenen: uyarı titreşimi, başarı titreşimi **yok**.
- [ ] **Günlük bulmaca:** Aynı üç durumu (reddedilen, kazanma, kaybetme) Günlük sekmesinde de dene.
- [ ] **Zaten bitmiş bulmaca sessiz:** Biten bir bulmacadan kaydırıp geri dön / uygulamayı kapat-aç / Günlük sekmesine yeniden gir. Beklenen: **titreşim yok**.
- [ ] **Sistem ayarı:** Sistem haptiklerini kapat, tuşlara bas. Beklenen: titreşim yok (Android'de dokunma geri bildirimi kapalıyken; iOS'ta Sistem Haptikleri kapalıyken). Ayarı geri aç.

## 2. Animasyonlar (hareket açıkken)

Önce `Hareketi azalt`/`Animasyonları kaldır` ayarının **kapalı** olduğundan emin ol.

- [ ] **Harf pop'u:** Bir harfe basınca kare küçük başlayıp hafifçe taşarak yerine oturuyor.
- [ ] **Çevirme:** Geçerli tahmin gönder. Beklenen: karolar sırayla (soldan sağa, yaklaşık 0,1 sn arayla) dönüp renklenir.
- [ ] **Sarsma:** Eksik harfle GİR. Beklenen: satır sağa sola sarsılır, uyarı mesajı çıkar.
- [ ] **Sunucu beklerken nabız (Günlük):** Tahmin gönderirken satır hafifçe nabız gibi soluyor (ağ hızlıysa çok kısa görünür; uçak modunda göndermeyi dene, "bağlantı yok" uyarısı gelir ve satır geri döner).
- [ ] **Sonuç kartı girişi:** Bulmaca bitince sonuç kartı aşağıdan yukarı kayarak ve solarak belirir (ani çıkmaz).
- [ ] **Seri chip'i:** Akışta doğru cevaplayınca üstteki şimşek sayacı kısa süre büyüyüp yerine oturur. Günlükte kazanınca alev (gün serisi) için aynısı.
- [ ] **Konfeti:** Günlük bulmacayı **kazan**. Beklenen: üstten renkli konfeti yağar, yaklaşık 2 sn sonra kaybolur, dokunmaları engellemez.
- [ ] **Kayıpta konfeti yok:** Günlük bulmacayı kaybet (başka dilde dene). Beklenen: konfeti yok.
- [ ] **Tekrar kutlanmaz:** Günlük sekmesinden çık, geri gir; uygulamayı kapat-aç. Beklenen: konfeti **tekrar çıkmaz**, sonuç kartı animasyonsuz görünür.
- [ ] **Geri kaydırma:** Akışta bitmiş bir bulmacaya geri kaydır. Beklenen: karolar tekrar dönmez, anında renkli.
- [ ] **7. gün serisi konfetisi** (7 gün gerekir, istersen atla): 7. ardışık günde bitirince konfeti çıkar (kaybetsen de).
- [ ] Bilinen küçük kusur: aynı gün ikinci dilde bitirince, o gün 7. gün kilometre taşıysa konfeti bir kez daha çıkabilir.

## 3. "Hareketi azalt"

- [ ] **iOS:** `Ayarlar > Erişilebilirlik > Hareket > Hareketi Azalt` aç. **Android:** `Ayarlar > Erişilebilirlik > Animasyonları kaldır` aç (bazı telefonlarda `Geliştirici seçenekleri > Animasyon ölçeği` = kapalı).
- [ ] Uygulamaya dön (Android'de uygulamayı arka plana atıp geri getir; iOS'ta anında algılanır).
- [ ] Geçerli tahmin: karolar **anında** renklenir, çevirme yok.
- [ ] Eksik harfle GİR: **sarsma yok**, mesaj ve uyarı titreşimi var.
- [ ] Harf yazarken **pop yok**.
- [ ] Günlük kazan: **konfeti yok**, sonuç kartı anında görünür, seri chip'i büyümez.
- [ ] Günlükte sunucu beklerken satır sabit soluk görünür, nabız atmaz.
- [ ] Ayarı kapat, uygulamaya dön: animasyonlar geri geldi.

## 4. Erişilebilirlik

### 4a. Ekran okuyucu (iOS VoiceOver / Android TalkBack)

Aç: iOS `Erişilebilirlik > VoiceOver`; Android `Erişilebilirlik > TalkBack`. İpucu: iOS'ta üçlü Home/yan tuş kısayolu ayarla.

- [ ] **Karo:** Gönderilmiş bir satırda her karoya dokun. Beklenen: "A harfi, doğru yerde" / "A harfi, kelimede, yanlış yerde" / "A harfi, kelimede yok" (İngilizce cihazda "Letter A, correct place" gibi).
- [ ] **Yazılan harf:** Henüz göndermediğin bir harfe dokun: "A harfi, yazıldı". **Boş karolar sessiz** (30 kez okumuyor).
- [ ] **Klavye:** Harf tuşu "Z harfi"; tahmin sonrası "A harfi, doğru yerde" gibi durumunu söylüyor. GİR "Tahmini gönder", ok "Sil" okunuyor.
- [ ] **Sekme çubuğu:** Açık sekme "seçili" olarak okunuyor.
- [ ] **Seri chip'leri:** "Doğru cevap serisi: 3" ve (Günlük) "Gün serisi: 2" cümle olarak okunuyor, çıplak sayı değil.
- [ ] **Dil seçimi (EN/TR):** Seçili olan "seçili" diye okunuyor, diğeri değil.
- [ ] **Kalp:** "Favorilere ekle" / ekledikten sonra "Favorilerden çıkar".
- [ ] **Bildirimler:** Reddedilen tahminde "Yeterli harf yok" mesajı **kendiliğinden okunuyor** (üstüne gitmeden).
- [ ] **Sıralama sekmesi:** Filtre chip'leri (Hız, Seri, Arkadaşlar, EN/TR, Bugün...) seçili/seçili değil diye okunuyor. Tablo satırları tek blok olarak okunuyor ("1, alice, 2/6 · 00:30").
- [ ] **Profil / Hesap:** "Hesabı sil" ve onay düğmeleri okunuyor.
- [ ] Not: Gerçek bir ekran okuyucu kullanıcısı gibi bir bulmacayı baştan sona çözmeyi dene; takıldığın yeri not al.

### 4b. Büyük yazı

- [ ] **iOS:** `Ayarlar > Erişilebilirlik > Ekran ve Yazı Boyutu > Daha Büyük Metin` aç, kaydırıcıyı **en sona** çek. **Android:** `Ekran > Yazı boyutu ve ekran boyutu` en büyük.
- [ ] **Alt sekme çubuğu:** "Sıralama" tek satırda, bölünmüyor (çubuk yazısı en fazla 1,3 kata kadar büyür).
- [ ] **Akış:** Üst çubuk (şimşek, Atla, dil) ve klavye kesilmeden görünüyor.
- [ ] **Günlük giriş ekranı:** Başlık, tarih, açıklama ve Başla düğmesi taşmadan görünüyor.
- [ ] **Sıralama:** Filtre chip'leri (Tüm zamanlar vb.) **alt satıra geçiyor**, dikey sıkışıp harf harf bölünmüyor. Satırlarda uzun isim "..." ile kısalıyor.
- [ ] **Profil:** İstatistik kutuları **2x2** düzenine geçiyor, etiketler ("Oynanan", "Kazanma", "En iyi seri") kesilmiyor.
- [ ] **Hesap kartı ve gizlilik kartı:** Metinler kesilmeden uzuyor, kaydırılabiliyor.
- [ ] Ayarı normale döndür.

### 4c. Yüksek kontrast

- [ ] **iOS:** `Ekran ve Yazı Boyutu > Kontrastı Artır`. **Android:** `Erişilebilirlik > Yüksek kontrastlı metin` (Android'de bu ayar bazı telefonlarda yok, yoksa atla).
- [ ] Beklenen: boş karoların ve klavyenin çerçeveleri belirgin şekilde daha açık/kalın; soluk gri yazılar (alt başlıklar, ipuçları) daha parlak. Renklerin anlamı (yeşil/sarı/koyu) aynı.
- [ ] Ayarı geri al.

### 4d. Dokunma hedefleri

- [ ] Sıralama sekmesindeki küçük chip'lere (Hız, Bugün, EN) ve arkadaş listesindeki "Kabul et / Reddet / Çıkar" düğmelerine **rahat dokunabiliyorsun** (hedef 48 dp, görünüş küçük olsa da).

---

## 5. Hesap silme

**Dikkat: gerçekten siler.** Harcanabilir B hesabını kullan.

### 5a. Hazırlık
- [ ] B hesabıyla giriş yap (Profil > Google ile giriş yap).
- [ ] Sıralama sekmesinden bir kullanıcı adı al (ör. `silme_testi`).
- [ ] Birkaç sonsuz akış bulmacası çöz, bir bulmacayı favorile, günlük bulmacayı çöz.
- [ ] Profil'de istatistiklerin ve favorinin göründüğünü not et.

### 5b. İptal yolu
- [ ] Profil > Hesap kartı > **Hesabı sil**. Beklenen: düğme bir onay kutusuna dönüşür: "Hesabın silinsin mi?" + uyarı metni + "Kalıcı olarak sil" ve "Vazgeç".
- [ ] **Vazgeç**'e bas. Beklenen: kutu kapanır, hiçbir şey silinmez, istatistikler yerinde.

### 5c. Çevrimdışı yol
- [ ] Uçak modunu aç. Hesabı sil > Kalıcı olarak sil. Beklenen: "Hesap silinemedi. Bağlantını kontrol edip tekrar dene" mesajı; **hiçbir veri silinmez**, hesap hâlâ giriş yapmış.
- [ ] Uçak modunu kapat.

### 5d. Gerçek silme
- [ ] Hesabı sil > Kalıcı olarak sil. Beklenen: "Hesap silindi" mesajı.
- [ ] Profil: artık **anonim hesap** görünüyor ("Anonim hesap...", Google ile giriş düğmesi).
- [ ] İstatistikler **sıfırlandı** (oynanan 0), favoriler boş, akış yeni bulmacalarla başladı.
- [ ] Sıralama sekmesi: kullanıcı adın gitti, anonim görünüm.
- [ ] Dil seçimin (TR/EN) ve gizlilik anahtarının durumu **korundu**.
- [ ] Aynı B hesabıyla tekrar Google girişi yap. Beklenen: **sıfırdan yeni hesap** (eski veri geri gelmedi, kullanıcı adı yok).
- [ ] Supabase SQL: eski kullanıcı adın serbest mi: `select count(*) from profile where lower(username) = 'silme_testi';` Beklenen: 0 (yeniden aldıysan 1).
- [ ] Anonim hesapta "Hesabı sil" düğmesi **görünmüyor**.
- [ ] Apple ile giriş yapmış bir hesabı da silmeyi dene. Beklenen: aynı akış çalışır. Bilinen eksik: Apple belirteci iptali yok (yayın aşamasında yapılacak).

---

## 6. Anonim kullanım sayıları ve çökme raporları

### 6a. Anahtar
- [ ] Profil'de "BrainScroll'u geliştirmeye yardım et" kartı var, durum "Paylaşım açık".
- [ ] **Kapat**'a bas: durum "Paylaşım kapalı", düğme "Aç" oldu.
- [ ] Uygulamayı tamamen kapatıp aç: seçim **hatırlanıyor**.
- [ ] Tekrar **Aç**.

### 6b. Olaylar gerçekten geliyor mu (Supabase SQL editörü)
Uygulamada: aç, birkaç sekme gez, bir günlük bulmacayı başlat ve bitir, bir sonsuz bulmaca bitir. ~10 sn bekle. Sonra:

```sql
select at, name, platform, app_version, props
from app_event order by at desc limit 30;
```
- [ ] `app_open`, `tab_viewed` (props `{"tab": "daily"}` gibi), `daily_started`, `daily_finished` (props `outcome`, `guesses`, `language`), `endless_finished` satırları var.
- [ ] `platform` doğru (`ios` / `android`), `app_version` TestFlight sürümüyle uyumlu (ör. `1.0 (23)`).
- [ ] **Kimlik yok:** tabloda kullanıcı/cihaz kimliği kolonu yok; props içinde kullanıcı kimliği, e-posta, kullanıcı adı yok.
- [ ] Sosyal: arkadaş eklediğinde `friend_added`, Google girişte `signed_in` (props `provider`), hesap silince `account_deleted` satırı.

### 6c. Kapalıyken gelmiyor mu
- [ ] Anahtarı **Kapat**. Önce satır sayısını not et: `select count(*) from app_event;`
- [ ] Uygulamada sekmeler gez, bulmaca çöz, 15 sn bekle. Sayıyı tekrar bak. Beklenen: **artmadı**.
- [ ] Anahtarı geri **Aç**, birkaç sekme gez. Beklenen: yeniden artıyor.

### 6d. Çökme raporu (geçici "çökert" düğmesiyle)
Profil'deki gizlilik kartının en altında geçici bir **"TEST: crash the app"** düğmesi var. Test bitince kaldırılacak (kodda `TEMP-CRASH-BUTTON` ile işaretli).
- [ ] Anahtar **açıkken** düğmeye bas. Beklenen: uygulama kapanır (çöker).
- [ ] Uygulamayı yeniden aç, internet açık, ~10 sn bekle. Supabase SQL: `select at, platform, app_version, kind, message, left(stack, 300) from crash_report order by at desc limit 5;` Beklenen: yeni satır, `message` "Test crash from the TEMP crash button", `kind` `kotlin.IllegalStateException`, `stack` Kotlin yığın izi.
- [ ] Aynı raporun **ikinci kez gelmediğini** kontrol et (uygulamayı bir daha aç, satır sayısı artmasın).
- [ ] Anahtarı **kapat**, düğmeye bas, uygulamayı aç, 10 sn bekle. Beklenen: **yeni satır yok**.
- [ ] Çevrimdışı çökme: anahtar açık, uçak modu açık, düğmeye bas, uygulamayı uçak modundayken aç (rapor gitmez), sonra uçak modunu kapat, 10 sn bekle. Beklenen: rapor bu sefer gelir (sonraki açılışta gönderilir).
- [ ] Platform ve sürüm doğru (`ios`/`android`, TestFlight sürümü).
- [ ] Not: Kotlin dışı (yerel) çökmeler burada görünmez, onları App Store Connect > TestFlight > Çökmeler ve Play Console gösterir.

### 6e. Gizlilik politikası (web)
- [ ] `https://playbrainscroll.com/privacy` (TR) ve `/en/privacy` açılıyor, "Son güncelleme: 9 Ekim 2026".
- [ ] "Anonim kullanım sayıları ve çökme raporları" bölümü var; 90 gün, kapatma yolu ve "kimlik yok" cümleleri doğru. **Hukuki bir metin, kendin oku ve onayla.**
- [ ] Saklama bölümü uygulamadaki "Hesabı sil" akışını anlatıyor.

---

## 7. Oturum sıfırlama (sonsuz akış, 30 dk)

- [ ] Akışta bir bulmacayı bitirmeden aşağı kaydır (atlama hakkını kullan). Üstte "Atla 0" yazıyor, ikinci bir bitmemiş bulmacadan ileri kaydırmak engelleniyor ("Önce bu bulmacayı bitir").
- [ ] **29 dakika sonra** (ya da cihaz saatini 29 dk ileri al): hâlâ "Atla 0".
- [ ] **31 dakika sonra** (cihaz saatini 31 dk ileri alıp uygulamaya dön ya da gerçekten bekle): "Atla **1**" oldu, ileri kaydırma yeniden serbest.
- [ ] Doğru cevap serin (şimşek sayacı) ve daha önce atladığın bulmaca **olduğu gibi** duruyor.
- [ ] **Hareketle canlı tut:** 20 dk arayla bir harf yazıp sil (toplam 1,5 saat). Beklenen: sıfırlanmıyor.
- [ ] **Soğuk başlangıç:** Uygulamayı tamamen kapatıp aç: "Atla 1" ile başlıyor.
- [ ] Cihaz saatini normale çevir (otomatik ayara geri al).

## 8. Profil günlük istatistikleri (dil bazlı)

- [ ] Günlük bulmacayı **TR** çöz, sonra **EN** çöz (farklı sürelerle olması iyi).
- [ ] Profil > Günlük bulmaca bölümü: **İkisi / EN / TR** seçimi var, varsayılan "İkisi".
- [ ] "İkisi": oynanan 2, en iyi süre ikisinin hızlısı.
- [ ] **TR** seç: oynanan 1, en iyi süre TR'nin süresi, tahmin dağılımı yalnızca TR'ye ait, altında not: "Gün serisi iki dilden birini bitirmekle sayılır."
- [ ] **EN** seç: aynısı EN için.
- [ ] Gün serisi ve en iyi seri kutuları **her seçimde aynı** (iki dil birlikte).
- [ ] Sekme değiştirip dönünce seçim sıfırlanması normal (varsayılan "İkisi").

## 9. Hesap birleştirme

Amaç: anonim hesapla oynanan günlük sonuçların, **önceden var olan** bir Google hesabına geçince kaybolmaması.

### 9a. Hazırlık: "var olan hesap" oluştur
- [ ] Cihaz 1'de (ya da şimdiki kurulumda) A Google hesabıyla giriş yap, kullanıcı adı al, **dünün günlük bulmacası olmasa da bugünküyü (EN)** çöz.
- [ ] Çıkış yap.

### 9b. Anonim oyun
- [ ] Cihaz 2'de (ya da uygulamayı silip yeniden kurunca) **giriş yapmadan** oyna: günlük bulmacayı **TR** çöz, birkaç sonsuz bulmaca çöz, birini favorile.
- [ ] Profil: "Anonim hesap" görünüyor, oynanan sayıları not et.

### 9c. Birleştirme
- [ ] Profil > **Google ile giriş yap**, A hesabını seç. Tarayıcıdan dönünce "Giriş yapıldı".
- [ ] Profil ve Sıralama'da bekle ~10 sn. Beklenen:
  - [ ] Sıralama > Hız > TR > Bugün: **senin TR sonucun** (kullanıcı adınla) tabloda.
  - [ ] Sıralama > Hız > EN > Bugün: A hesabının eski EN sonucu da duruyor.
  - [ ] Seri tabloları: gün serin iki sonucun günlerini birlikte sayıyor.
  - [ ] Profil'de sonsuz istatistikler ve favori yerinde.
- [ ] Supabase SQL ile doğrula (A hesabının kullanıcı kimliği yerine kullanıcı adını kullan):
```sql
select r.mode, r.language, r.day_index, r.duration_ms
from game_result r join profile p on p.user_id = r.user_id
where lower(p.username) = lower('KULLANICI_ADIN') order by r.day_index, r.language;
```
  Beklenen: hem eski EN hem yeni TR günlük sonuçlar aynı kullanıcıda.
- [ ] Anonim hesap silinmiş olmalı: `select count(*) from auth.users where is_anonymous;` (önceki sayıya göre 1 azaldı).

### 9d. Çakışma
- [ ] Yeni anonim kurulumda **aynı gün ve aynı dilde** (EN) günlük bulmacayı çöz, sonra A hesabıyla gir (A'nın da bugün EN sonucu var). Beklenen: tabloda **A'nın kendi sonucu** kalır, anonimin ikinci EN sonucu eklenmez.

### 9e. Bağlama (birleştirme gerekmeyen)
- [ ] Yeni bir anonim kurulumda, **hiç kullanılmamış** bir Google hesabıyla giriş yap. Beklenen: hesap bağlanır, **veriler kaybolmaz**, ek bir işlem yok (aynı hesap).

### 9f. Yarıda bırakma ve çevrimdışı
- [ ] "Google ile giriş yap"a bas, tarayıcıdan **vazgeç / geri dön**. Beklenen: anonim hesap duruyor, her şey normal.
- [ ] Uçak modunda "Google ile giriş yap" dene. Beklenen: giriş başarısız mesajı, uygulama bozulmaz.
- [ ] Girişten sonra ağ yokken birleştirme teslim edilemezse bir sonraki açılışta (ağ varken) kendiliğinden tamamlanır: giriş yap, hemen uçak modu, uygulamayı kapat, ağı aç, uygulamayı aç, 10 sn bekle, 9c'deki kontrolleri yap.
- [ ] Apple ile aynı akışı dene (iOS): anonim > Apple ile giriş, var olan Apple hesabına geç.

---

## 10. Gerilemeler (eski özellikler bozulmadı mı)

- [ ] **Apple girişi** çalışıyor (hata kodu yok).
- [ ] **Google girişi** çalışıyor, çıkış yapınca anonime dönüyor.
- [ ] **Günlük bulmaca:** Başla, süre sunucu saatiyle, tahmin, sonuç, geri sayım, ertesi gün yeni bulmaca.
- [ ] **Sıralama:** kullanıcı adı al, tablolar, arkadaş ekle (kod + kullanıcı adıyla istek), arkadaş filtresi.
- [ ] **Davet linki:** `https://playbrainscroll.com/invite?code=...` Notlar'dan uygulamayı açıyor, uygulama yüklü değilken sayfa kodu gösteriyor.
- [ ] **Yedekleme:** Profil'de "Her şey yedeklendi".
- [ ] **Çevrimdışı:** Uçak modunda akış çalışıyor, günlük bulmaca "internet bağlantısı gerekiyor" diyor, Sıralama son görüntüyü "Çevrimdışı" notuyla gösteriyor.

## 11. Bulursan bana yaz

Her başarısız adım için: **kaçıncı madde**, **cihaz ve sürüm** (ör. iPhone 14, iOS 26.x / Pixel 7, Android 15), **ne gördün**, mümkünse ekran görüntüsü. Özellikle bakılması gerekenler:
1. TestFlight'ta konfeti ve çevirmenin akıcılığı (düşük güçlü cihazlarda).
2. VoiceOver / TalkBack ile bir bulmacanın baştan sona çözülebilmesi.
3. Birleştirmenin gerçek Google hesabıyla (9c, 9d).
4. Çökme raporunun gerçekten gelmesi (6d, geçici düğmeyle).
