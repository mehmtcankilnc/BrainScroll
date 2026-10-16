package com.mehmtcan.brainscroll.game.crossword

/**
 * The words of the crossword and their clues, one `WORD|clue` per line, made from tools/crossword/bank by `Gen.java bank` (do not
 * edit by hand: change the files of that folder and run it again). The pieces are cut to fit the limit of a string constant.
 */
internal object CrosswordBankData {
    val TR: List<String> = listOf(
        """
ABA|Kalın dokuma kumaş
ABLA|Büyük kız kardeş
ACELE|Acil, ivedi
ACEMİ|Yeni başlayan
ACI|Tatlı karşıtı
ACİL|Acele, ivedi
AD|İsim
ADA|Denizle çevrili kara
ADALET|Hak
ADAM|Erkek kişi
ADIM|Yürürken atılır
ADİ|Değersiz, bayağı
AF|Bağışlama
AFET|Büyük felaket
AFYON|Kaymağıyla ünlü şehrimiz
AFİYET|Yemek öncesi dilek
AH|Sızlanma sözü
AHIR|İneklerin barınağı
AHLAK|Törel değerler
AHŞAP|Odundan yapılmış şey
AK|Beyaz
AKIL|Zekâ
AKILLI|Zeki
AKREP|Kuyruğunda iğne taşıyan hayvan
AKSESUAR|Giysiyi tamamlayan süs
AKTÖR|Erkek oyuncu
AKŞAM|Günün son bölümü
ALAN|Geniş açık yer
ALARM|Uyarı sesi
ALAY|Bir askerî topluluk
ALBAY|Subay rütbesi
ALBÜM|Fotoğraflık
ALET|Araç gereç
ALIN|Kaşların üstü
ALKOL|İçkinin içindeki madde
ALP|Yiğit
ALTI|Bir sayı
ALTIN|Değerli sarı maden
ALTINCI|Beşinciden sonra gelen
AMAÇ|Maksat, erek, gaye
AMBAR|Tahıl deposu
AMCA|Babanın erkek kardeşi
AMPUL|Odayı aydınlatan eşya
AN|Çok kısa zaman
ANA|Anne
ANAYASA|Temel devlet yasası
ANI|Hatıra
ANIT|Abide
ANKA|Efsane kuş
ANKARA|Başkentimiz
ANLAM|Mana, meal
ANLAYIŞ|Hoşgörü
ANNE|Çocuğu olan kadın
ANTİK|Çok eski
APTAL|Akılsız
ARA|Aralık
ARABA|Dört tekerlekli kara taşıtı
ARAÇ|Taşıt
ARENA|Gösteri alanı
ARI|Bal yapan hayvan
ARK|Su kanalı
ARKADAŞ|Dost
ARMAĞAN|Hediye
ARMUT|Sulu bir meyve
AROMA|Hoş koku veya tat
ARPA|Tahıl çeşidi
ARSA|Boş yapı yeri
ARZU|İstek
ARŞ|Gökyüzünün en yüksek katı
ARŞİV|Belgelerin saklandığı yer
AS|İskambilde bir kart
ASA|Baston
ASANSÖR|Katlar arasında çıkarır
ASKER|Er
ASLAN|Hayvanların kralı
ASİ|İsyancı
AT|Bir binek hayvanı
ATA|Dede
ATLAMAK|Zıplamak
ATLAS|Harita kitabı
ATLET|Spor yapan kişi
AVAZ|Yüksek ses
AVCI|Av yapan
AVUKAT|Mahkemede savunan kişi
AVUÇ|Elin içi
AVİZE|Tavan lambası
AY|Dünya'nın uydusu
AYAK|Yürümeyi sağlayan organ
AYAKKABI|Kundura
AYAZ|Soğuk kuru hava
AYDIN|Okumuş kimse
AYET|Kur'an cümlesi
AYGIT|Cihaz
AYI|Kış uykusuna yatan hayvan
AYIK|Sarhoş olmayan
AYLIK|Ay başı alınan ücret
AYNA|Yansıtan bir eşya
AYRAN|Yoğurttan yapılan içecek
AZ|Çok karşıtı
AZAP|Ağır acı
AÇ|Tok karşıtı
AÇLIK|Karnın boş olması
AĞA|Köyün ileri geleni
AĞI|Zehir
AĞIL|Koyunların barınağı
AĞUSTOS|Bir ay
AŞ|Yemek
AŞAĞI|Yukarı karşıtı
AŞURE|Bir tatlı
BABA|Erkek ebeveyn
BACA|Dumanı çıkaran
BACI|Kız kardeş
BADEM|Kabuklu bir kuruyemiş
BAGAJ|Arabanın yük alan bölümü
BAHAR|Kışı izleyen mevsim
BAHTİYAR|Mutlu
BAHÇE|Çiçek ve ağaç yetişen yer
BAKAN|Hükümet üyesi
BAKIM|Özen
BAKIR|Kırmızımsı maden
BAKIŞ|Bakma işi
BAKKAL|Mahalle dükkânı
BAL|Arının yaptığı besin
BALIK|Suda yüzen hayvan
BALKON|Evin dışa açık yeri
BALO|Süslü dans gecesi
BALON|Havayla şişirilir
BALTA|Odun yaran eşya
BAMBU|Tropik kamış
BAMYA|Tüylü bir sebze
BANT|Yapışkan şerit
BANYO|Yıkanılan yer
BAR|İçki servis edilen yer
BARAJ|Suyu tutan yapı
BARIŞ|Savaş karşıtı
BATI|Doğu karşıtı
BAYAN|Hanım
BAYAT|Taze karşıtı
BAZI|Birkaç
BAĞ|Üzüm yetişen yer
BAĞIŞ|Karşılıksız verme
BAŞ|Vücudun en üst kısmı
BAŞAK|Buğday sapının ucu
BAŞARI|Kazanım
BAŞKA|Farklı
BAŞKENT|Devletin yönetim şehri
BEBE|Küçük çocuk
BEBEK|Yenidoğan
BEBEKLİK|Kundak dönemi
BEDEL|Karşılık olarak verilen
BEDEN|Vücut
BEKLEME|Duraksama
BEL|Gövdenin ortası
BELGE|Resmî kâğıt
BELKİ|Bir ihtimal sözü
BEN|Vücuttaki küçük leke
BENEK|Küçük leke
BENZİN|Araba yakıtı
BETON|Çimentodan yapılan karışım
BEY|Efendi anlamında
BEYANAT|Açıklama
BEYAZ|Kar rengi, ak
BEYİN|Düşünce organı
BEŞİK|Bebeğin uyuduğu yer
BIYIK|Üst dudak kılları
BIÇAK|Keskin bir eşya
BOKS|Kurallı dövüş
BOL|Bereketli
BOMBA|Patlayıcı
BORU|Su taşıyan
BORÇ|Geri verilecek para
BOY|Uzunluk
BOYAMAK|Renklendirmek
BOYUT|Uzunluk, genişlik ve yükseklik
BOZUK|Çalışmayan
BOĞA|Güreşte kızdırılan hayvan
BOĞAZ|İki denizi birleştirir
BOŞ|Dolu karşıtı
BRAVO|Takdir sözü
BRONZ|Tunç
BU|İşaret zamiri
BUGÜN|İçinde bulunulan gün
BUHAR|Kaynayan suyun gazı
BUKET|Çiçek demeti
BULUT|Yağmur taşır
BURS|Öğrenciye verilen para
BURUN|Koklama organı
BUZ|Donmuş su
BUĞU|Su buharı
BÖLGE|Yöre
BÖLÜM|Parça
BÜLBÜL|Güzel öten kuş
BÜRO|Ofis
BÜTÇE|Gelir gider hesabı
BÜTÜN|Tam
BÜYÜK|Küçük karşıtı
BÜYÜME|Boy atma, uzama
BİBER|Tatlı veya acı bir sebze
BİLEK|Elin kola bağlandığı yer
BİLET|Sinemaya girişte alınır
BİLEZİK|Bileğe takılan takı
BİLGİ|Öğrenilen şey
BİLİM|Bilgi alanı
BİLİNÇ|Farkındalık
BİN|Yüzün on katı
BİNA|Yapı
BİR|Bir sayı
BİRA|Arpadan yapılan içecek
BİRAZ|Az miktar
BİRİKİM|Tasarruf
BİSİKLET|Pedalla giden araç
BİT|Saçta bulunan haşere
BİTKİ|Nebat
BİTİŞ|Sona erme
BİÇİM|Şekil
CADDE|Geniş sokak
CADI|Kötü büyücü
CAHİL|Bilgisiz
CAMBAZ|Halat üstünde gösteri yapan
CAMİ|İbadet yeri
CANAVAR|Korkunç yaratık
CANLI|Yaşayan
CANİ|Suçlu kimse
CAZ|Bir müzik türü
CEBİR|Bir matematik dalı
CEKET|Bir üst giysisi
CEMAAT|Topluluk
CENK|Savaş
CENNET|Ödül yurdu
CEP|Pantolonda bulunur
CEPHE|Savaş hattı
CERRAH|Ameliyat yapan
CESET|Cansız beden
CESUR|Korkusuz
CEVAHİR|Mücevher
CEVAP|Yanıt
CEVİZ|Kabuğu sert bir yemiş
CEZA|Suçun karşılığı
CÜMLE|Tümce
CÜZDAN|Para taşınan eşya
CİHAZ|Alet
CİLA|Parlatma maddesi
CİLT|Deri
CİMRİ|Cömert karşıtı
CİN|Masallarda geçen ruh
DAHİL|İçinde olan
DAKİKA|Saatin altmışta biri
DAL|Ağacın kolu
DALGA|Denizin kabarması
DALGIÇ|Denizin altına dalan kişi
DAM|Evin çatısı
DAMAR|Kan taşır
DAMGA|Mühür izi
DAMLA|Katre
DANA|İnek yavrusu
DANS|Müzikle yapılan hareket
DAR|Geniş karşıtı
DAVUL|Tokmakla çalınır
DAYAK|Patak, kötek
DAĞ|Zirvesi olan yükselti
DAĞCILIK|Zirve tırmanma sporu
DAİRE|Apartmandaki ev
DEFA|Kere, kez
DEFTER|Not tutulan sayfalar
DEHA|Üstün zekâ
DEKAN|Fakülte başkanı
DELİ|Aklı dengesiz birey
DEMET|Bağlı çiçek grubu
DEMİR|Simgesi Fe olan element
DENEY|Laboratuvarda yapılır
DENGE|Ağırlık eşitliği
DENGESİZ|Balansa sahip olmayan
DENİZ|Derya
DEPO|Eşya saklanan yer
DEPREM|Yer sarsıntısı
DERE|Küçük akarsu
DERGİ|Süreli yayın
DERMAN|Güç, takat
DERS|Okulda işlenir
DERİ|Hayvan postu
DERİN|Sığ karşıtı
DETAY|Ayrıntı
DEV|Çok iri
DEVAM|Sürdürme
DEVE|Hörgüçlü bir hayvan
DEVLET|Ülkeyi yöneten kurum
DEVRE|Dönem
DEVİR|Çağ
DEĞİRMEN|Un öğüten yapı
DO|Bir nota
DOKTOR|Hekim
DOKU|Hücre topluluğu
DOKUZ|Bir sayı
DOLAR|Amerika para birimi
DOLMUŞ|Paylaşılan minibüs
DOLU|Boş karşıtı
DOMATES|Kırmızı salata sebzesi
DOMUZ|Çamurda yatan çiftlik hayvanı
DON|Soğukta oluşan buz
DONDURMA|Külahta yenen tatlı
DORUK|Zirve
DOST|Arkadaş, yaren
DOSTLUK|Arkadaşlık
DOSYA|Evrak klasörü
DOĞA|Tabiat
DOĞAL|Yapay karşıtı
DOĞRU|Yalan karşıtı
DOĞU|Batı karşıtı
DOĞUM|Dünyaya geliş
DRAMA|Acıklı oyun
DUBLÖR|Tehlikeli sahneyi oynayan kişi
DUL|Eşi ölmüş
DURAK|Otobüs bekleme yeri
DURU|Berrak
DUT|Bir meyve
DUVAR|Oda sınırları
DUYGU|His
DUYU|Algı yeteneği
DÖNEM|Devre
DÖRDÜNCÜ|Üçüncüden sonra gelen
DÖRT|Bir sayı
DÖVME|Deriye işlenen resim
DÖVİZ|Yabancı para, kur
DÖŞEK|Yatak
DÜMEN|Gemi direksiyonu
DÜNYA|Üzerinde yaşadığımız gezegen
DÜZEN|Tertip
DÜZENLİ|Tertipli
DÜZEY|Seviye
DÜĞME|Gömlek ilikler
DÜĞÜM|İpte bağlanan yer
DÜĞÜN|Evlenme töreni
DÜŞ|Uykuda görülür
DÜŞMAN|Dost karşıtı
DÜŞÜK|Yüksek karşıtı, alçak
DİK|Yatay olmayan
DİKEY|Yatay karşıtı
DİKKAT|Özen, itina
DİL|Konuşma organı
DİLEK|Temenni, arzu
DİLENCİ|Sokakta para isteyen
DİLİM|Parça
DİN|İnanç sistemi
DİNLEME|Kulak verme
DİNÇ|Zinde
DİPLOMA|Mezuniyet belgesi
DİREK|Dik duran uzun çubuk
DİYET|Beslenme düzeni
DİZ|Bacakta bulunan bir eklem
DİZİ|Sıra, seri
DİĞER|Öteki
DİŞ|Ağızda bulunur
DİŞÇİ|Diş doktoru
DİŞİ|Erkek olmayan
EBE|Doğum yaptıran kadın
EBEVEYN|Anne ve baba
EDEBİYAT|Yazın sanatı
EDEP|Terbiye
EFEKT|Etki
EFOR|Çaba, gayret
EFSANE|Anlatılan destan
EGEMEN|Hâkimiyet
EJDER|Ateş püskürten efsane yaratık
EK|Eklenen şey
EKLEM|Kemiklerin birleşme yeri
EKMEK|Fırından çıkan besin
EKONOMİ|Mal ve para bilimi
EKRAN|Televizyonun görüntü yüzü
EKSEN|Dünyanın dönme çizgisi
EKSİK|Tam olmayan
EKİM|Bir ay
EKİN|Bir tarla ürünü
ELBİSE|Giyilen bir kıyafet
ELEK|Un süzen eşya
ELEKTRİK|Ampulü yakan enerji
ELEŞTİRİ|Beğenmeme yazısı
ELMA|Bir meyve
ELMAS|Çok sert değerli taş
ELÇİ|Devlet temsilcisi
EMEK|Alın teri
EMLAK|Ev, arsa gibi mülk
EMNİYET|Güvenlik
EMİR|Buyruk
EN|Genişlik
ENDİŞE|Kaygı
ENGEL|Önü kesen şey, mani
ENKAZ|Yıkıntı
ENLEM|Dünyada yatay çizgi
ER|Rütbesiz asker
ERDEM|Ahlaki üstünlük
ERGEN|Gençlik çağındaki kişi
ERGUVAN|Mor çiçekli ağaç
ERKEK|Dişi olmayan
ERKEN|Geç karşıtı
ERZAK|Yiyecek stoku
ERİK|Bir meyve
ESANS|Koku özü
ESAS|Temel
ESER|Yapıt
ESKİ|Yeni karşıtı
ESMER|Koyu tenli
ESNAF|Dükkân sahibi
ESNEK|Elastik
ESNEMEK|Uykuda ağız açmak
ESPRİ|Nükte
ESİR|Tutsak
ET|Kasaptan alınan şey
ETEK|Bir kadın giysisi
ETKİN|Çalışkan, faal
ETKİNLİK|Faaliyet
ETNİK|Bir soya ait
ETRAF|Çevre
EV|Hane
EVCİL|Evde beslenen hayvan
EVET|Olumlu cevap
EVLAT|Çocuk
EVLİ|Evlenmiş kimse
EVLİLİK|Nikâhlı yaşam
EVRAK|Belgeler
EVREN|Kâinat
EVRİM|Gelişme
EY|Seslenme sözü
EYER|At sırtına konur
EYLEM|Fiil
EYLEMCİ|Gösteri yapan
EYLÜL|Bir ay
EYVAH|Bir üzüntü sözü
EZAN|Camide okunur
EZBER|Hafızadan söyleme
EŞ|Karı ya da koca
EŞARP|Başa bağlanan örtü
EŞEK|Anıran hayvan
EŞSİZ|Benzeri olmayan, tek
FA|Bir nota
FABRİKA|Üretim tesisi
FAKİR|Yoksul
FAL|Kahveden bakılır
FALCI|Fal bakan
FAR|Arabanın ön lambası
FARE|Peynir seven kemirgen
FARK|Ayrım
FARKLI|Başka
FASULYE|Kuru baklagil
FATURA|Ödeme belgesi
FAYDA|Yarar
FAİZ|Nema
FELAKET|Büyük yıkım
FELÇ|Kas kaybı, inme
FEN|Bilim dalı
FENA|Kötü
FENER|El lambası
FERAH|Geniş, rahat
FESLEĞEN|Kokulu otlardan biri
FIKRA|Kısa komik anlatı
FIRIN|Ekmeğin pişirildiği yer
FOK|Bir kutup hayvanı
FOLYO|İnce metal sargı
FORUM|Açık tartışma
FOSİL|Taşlaşmış canlı kalıntısı
FOTOĞRAF|Işıkla yakalanan görüntü
FUAR|Ticaret sergisi
FUTBOL|Bir top oyunu
FİDAN|Genç ağaç
FİDE|Genç bitki
FİKİR|Düşünce
FİL|Hortumlu bir hayvan
FİLM|Sinemada izlenir
FİLOZOF|Bilge düşünür
FİLİZ|Tohumdan çıkan yeni sürgün
FİNAL|Son
FİNALİST|Son aşamaya kalan kişi
FİNİŞ|Yarışta bitiş çizgisi
FİRAVUN|Mısır hükümdarı
FİRMA|Şirket
FİYAT|Etiketteki rakam
FİZİK|Madde ve enerji bilimi
GAGA|Kuşun ağzı
GALAKSİ|Yıldız kümesi
GAR|Tren istasyonu
GARAJ|Arabaların saklandığı yer
GARANTİ|Güvence
GARİP|Tuhaf
GAYE|Amaç, maksat, erek
GAYET|Oldukça
GAZ|Maddenin bir hali
GAZETE|Her gün çıkan basılı yayın
GAZOZ|Köpüklü bir içecek
GAZİ|Savaştan sağ dönen
GECE|Gündüz karşıtı
GELENEK|Âdet
GELİN|Düğünün kadın kahramanı
GELİR|Kazanç
GEMİ|Bir deniz taşıtı
GENEL|Herkesi ilgilendiren
GENÇ|Yaşlı karşıtı
GENİŞ|Dar karşıtı
GEREK|Lüzum
GEREÇ|Malzeme
GERİ|Arka
GEZEGEN|Mars ve Venüs gibi gök cismi
GEÇ|Erken karşıtı
GEÇİT|Dağlar arası yol
GIDA|Yiyecek
GORİL|Dev maymun
GRUP|Topluluk
GURUR|Övünme duygusu
GÖBEK|Karnın ortası
GÖL|Çevresi kara olan su
GÖLGE|Güneşin vurmadığı yer
GÖNÜL|Kalp
GÖREV|Vazife
GÖRÜŞ|Fikir
GÖVDE|Ağacın ana kısmı
GÖZ|Görme organı
GÖZLEM|Dikkatle izleme
GÖZLÜK|Kötü görenin aracı
GÖĞÜS|Kalbin bulunduğu yer
GÜL|Dikenli bir çiçek
GÜLÜŞ|Gülme işi
GÜMÜŞ|Parlak beyaz maden
GÜN|Yirmi dört saat
GÜNAH|Dinen kötü sayılan
GÜNEY|Kuzey karşıtı
GÜNEŞ|Gündüzün yıldızı
GÜVERCİN|Barış simgesi kuş
GÜZ|Sonbahar
GÜZEL|Çirkin karşıtı
GÜÇLÜ|Kuvvetli
GİDİŞ|Geliş karşıtı
GİRİŞ|Başlangıç
GİTAR|Telli bir çalgı
GİYSİ|Elbise
GİYİNMEK|Elbise giymek
GİZ|Sır
GİZEM|Sır
GİZLİ|Açık olmayan, saklı
HACİM|Uzayda kaplanan boşluk
HAFIZA|Bellek
HAFTA|Yedi gün
HAFİF|Ağır karşıtı
HAK|Adalet
HAKEM|Maçı yöneten kişi
HAKSIZ|Adaletsiz
HAKİM|Davaya bakan kişi
HAL|Durum
HALAT|Kalın ip
HALK|Toplum
HAM|Pişmemiş
HAMAM|Yıkanılan buharlı yer
HAMSİ|Karadeniz balığı
HAMUR|Yoğrulan karışım
HAN|Kervansaray
HANE|Ev
HANGAR|Uçak barınağı
HAP|Yutulan ilaç
HAPİS|Cezaevi
HARAP|Yıkık
HARF|Alfabenin parçası
HARP|Savaş
HARİTA|Bir yeri gösteren çizim
HASAT|Ürünün toplanması
HASRET|Özlem
HASTA|Sağlıklı olmayan
HAT|Çizgi
HAVA|Solunan şey
HAVLU|Yıkanınca kurulanılır
HAVUZ|Yüzülen yer
HAVUÇ|Turuncu kök sebze
HAYAL|Gerçek olmayan düş
HAYAT|Yaşam
HAYIR|Evet karşıtı
HAYVAN|Canlıların bir grubu
HAZ|Zevk
HAZIR|Amade
HAZİRAN|Bir ay
HAÇ|Hristiyanların simgesi
HEDEF|Amaç, maksat
HEKİM|Doktor
HELAL|Dince serbest
HELVA|Tatlı lokma
HEMEN|Çabucak
HEPSİ|Tamamı
HEYET|Kurul
HEYKEL|Taştan oyma sanat eseri
HIRKA|Önü açık örgü
HIZ|Süratlilik
HIZLI|Çabuk giden
HOBİ|Boş zaman uğraşı
HOCA|Camide öğretmen
HOROZ|İbikli bir hayvan
HOŞ|Güzel, tatlı
HUKUK|Yasalar bilimi
HUZUR|Rahatlık
HÜCRE|Canlıların temel birimi
HÜCUM|Saldırı
HÜKÜM|Yargı kararı
HÜKÜMET|Devleti yönetenler
HÜR|Özgür
HÜZÜN|Keder
HİLAL|Yeni ay
HİLE|Oyunda aldatma
HİS|Duygu
HİSSE|Pay
HİSSİYAT|Duygular
HİÇ|Sıfır
ILIK|Ne sıcak ne soğuk
IRAK|Ortadoğu'da bir ülke
IRK|Soy
IRMAK|Nehir
ISLAK|Kuru karşıtı
ISLIK|Ağızla çıkarılan ses
ISRAR|Diretme, inat
ISSIZ|Kimsenin olmadığı yer
JOKER|Her yerde kullanılan kart
KABA|Nazik olmayan
KABAK|Tatlı ve yemek yapılan sebze
KABARIK|Şişmiş
KABLO|Elektrik telleri demeti
KABUL|Onaylama
KABUS|Kötü rüya
KABİN|Küçük bölme
KADEH|İçki bardağı
KADER|Alın yazısı
KADRO|Ekip, takım
KAFA|Baş
KAFES|Kuşun kapalı yuvası
KAHRAMAN|Yiğit
KAHVE|Telveli içecek
KAKAO|Çikolata hammaddesi
KAKTÜS|Çöl bitkisi
KALDIRIM|Yayaların yolu
KALE|Eski savunma yapısı
KALEM|Yazı aracı
KALEMLİK|Kalem konulan kap
KALIN|İnce karşıtı
KALIP|Dökümde şekil verir
KAMPANYA|İndirim dönemi
KAMU|Halk, toplum
KAN|Damarda akar
KANAL|Yapay su yolu
KANAT|Kuşun uçma organı
KANCA|Asmaya yarayan çengel
KANIT|Delil
KANUN|Yasa
KAP|Yemek konan
KAPICI|Apartman görevlisi
KAR|Bir yağış türü
KARA|Siyah
KARAKOL|Polis merkezi
KARANLIK|Işıksız
KARAR|Verilen hüküm
KARGA|Kara kanatlı bir kuş
KARGO|Taşınan paket
KARIN|Midenin bulunduğu yer
KARNE|Okul notları belgesi
KARPUZ|Bir meyve
KARTAL|Güçlü yırtıcı bir kuş
KAS|Güç veren doku
KASA|Para saklamaya yarayan eşya
KASABA|Küçük yerleşim yeri
KASAP|Et satan kişi
KASET|Ses kayıt bandı
KASIM|Bir ay
KASIT|Bilerek yapma
KASK|Başa takılan koruyucu
KAT|Binanın bir bölümü
KATI|Maddenin bir hali
KATILIM|Bir işe iştirak etme
KATKI|Yardım
KATİL|Cinayet işleyen
KAVAK|Uzun boylu bir ağaç
KAVUN|Güzel kokulu bir meyve
KAVURMA|Kızartılmış et
KAYA|Büyük taş kütle
KAYAK|Bir kış sporu
KAYGI|Endişe
KAYIK|Küçük tekne
KAYIP|Yitik, zayi
KAYMAK|Sütün yağlı üst tabakası
KAZ|Bir su kuşu
KAZAK|Örgü bir üst giysisi
KAZAN|Büyük yemek kabı
KAZANÇ|Gelir
KAZIK|Yere çakılan sivri direk
KAÇAK|Yasa dışı
KAÇIŞ|Firar etme işi
KAĞIT|Yazının yazıldığı şey
KAŞ|Gözün üstündeki tüyler
KAŞAR|Yağlı bir peynir
KEDER|Hüzün
KEDİ|Miyavlayan hayvan
KEFAL|Bir deniz balığı
KEFEN|Cenazenin beyaz örtüsü
KEFİL|Güvence veren
KEKİK|Aromalı bir ot
KEL|Saçsız
KELEBEK|Rengârenk kanatlı böcek
KEMAN|Yaylı bir çalgı
KEMANCI|Keman çalan kişi
KEMER|Pantolonu tutan şey
KEMİK|İskeleti oluşturur
KEP|Başa takılan
KEPEK|Saçta oluşan beyaz pul
KESE|Küçük para torbası
KESİK|Bıçak yarası
KESİM|Bölüm
KESİN|Kuşkusuz
KEYİF|Zevk
KEÇİ|Sakallı bir hayvan
KEŞKE|Pişmanlık sözü
KEŞİF|Bulma
KILIÇ|Uzun kesici bir silah
KIR|Şehir dışı
KIRIK|Parçalanmış
KISIM|Bölüm
KIYAS|Karşılaştırma
KIYMA|Çekilmiş et
KIZ|Erkek çocuğun karşıtı
KIZIL|Kırmızı
KIŞ|Bir mevsim
KIŞLA|Askerlerin eğitim yeri
KLASÖR|Evrak dosyası
KLİMA|İklimlendirme cihazı
KLİŞE|Basmakalıp söz
KOBRA|Zehirli bir yılan
KOCA|Yaşça büyük
KOKTEYL|Karışık içki
KOL|Omuzdan bileğe kadar
KOLA|Gazlı bir içecek
KOLAY|Zor karşıtı
KOLAYLIK|Güçlük karşıtı
KOLEJ|Özel okul
KOLYE|Bir süs eşyası
KOLİ|Kargo paketi
KOMUT|Buyruk, emir
KOMİK|Güldüren kişi
KOMİSER|Polis amiri
KOMŞU|Yan evde oturan
KONAK|Büyük köşk
KONSER|Müzik gösterisi
KONTROL|Denetim
KONU|Mevzu
KONUK|Misafir
KONUM|Bulunulan yer, mekan
KONUT|Ev, hane
KOPYA|Aslının benzeri
KORKU|Ürperten duygu
KORKUNÇ|Dehşet verici
KORU|Küçük orman
KOYU|Açık olmayan
KOYUN|Yün veren hayvan
KOZ|Kelebeğin yuvası
KOÇ|Erkek koyun
KOŞU|Bir atletizm dalı
KOŞUCU|Koşan kişi
KOŞUL|Şart
KRAVAT|Boyuna bağlanan şey
KREDİ|Bankadan alınan borç
KUBBE|Yarım küre çatı
KUDUZ|Köpek hastalığı
KUKLA|İple oynatılan oyuncak
KUL|Tanrı karşısında insan
KULAK|Duyma organı
KULAKLIK|Müzik dinlemeye yarayan araç
KULÜP|Dernek
KUM|Plajda bulunur
KUMAR|Şansa dayalı oyun
KUMAŞ|Elbise dikilen malzeme
KUMBARA|Para biriktirilen kap
KUNDURA|Ayakkabı
KUPA|Şampiyona verilen ödül
KUR|Döviz fiyatı
KURAL|Uyulacak ilke, kaide
KURAM|Teori
KURBAN|Bayramda kesilir
KURT|Uluyan bir yırtıcı hayvan
KURU|Islak karşıtı
KURUM|Resmî teşkilat
KURYE|Paket taşıyan kişi
KURŞUN|Ağır gri maden
KUSUR|Eksiklik
KUTU|Karton kap
KUTUP|Dünyanın uç noktası
KUYRUK|Hayvanın arka uzantısı
KUYU|Su çıkarılan çukur
KUYUMCU|Altın satan kişi
KUZEN|Amca çocuğu
KUZEY|Güney karşıtı
KUŞ|Kanatlı hayvanların genel adı
KUŞAK|Yaş grubu, nesil
KUŞKU|Şüphe
KÖFTE|Bir et yemeği
KÖK|Ağacın toprakta kalan kısmı
KÖKEN|Kaynak, memba
KÖMÜR|Yanan siyah taş
KÖPEK|Havlayan hayvan
KÖPRÜ|Nehrin iki yakasını bağlar
KÖR|Göremeyen
KÖTÜ|İyi karşıtı
KÖY|Küçük yerleşim yeri
KÖŞE|İki duvarın birleştiği yer
KÜF|Ekmekte oluşan mantar
KÜFÜR|Hakaret, argo
KÜL|Yanma sonucu oluşan toz
KÜLAH|Dondurma konan şey
KÜLTÜR|Toplumun değerleri
KÜLÇE|Maden parçası
KÜME|Matematikte topluluk
KÜMES|Tavukların yuvası
KÜNYE|Kimlik bilgileri
KÜREK|Toprak kazmaya yarayan alet
KÜRSÜ|Konuşmacının yeri
KÜTÜK|Kesilmiş ağaç gövdesi
KÜVET|Banyoda yıkanılan kap
KÜÇÜK|Büyük karşıtı
KİBAR|Nazik
KİBİR|Kendini beğenmişlik
KİL|Çamur gibi toprak
KİLER|Yiyecek odası
KİLO|Bir ağırlık birimi
KİLİM|Dokunmuş yer örtüsü
KİLİT|Anahtarla açılır
KİM|Bir soru sözü
KİMSE|Birisi
KİMYA|Maddeleri inceleyen bilim
KİN|Öç
KİR|Pislik
KİRAZ|Bir meyve
KİRLİ|Temiz karşıtı
KİRPİ|Dikenli bir hayvan
KİTAP|Sayfalardan oluşur
KİTLE|Topluluk
LA|Bir nota
LAF|Söz
LAHANA|Yapraklı sebze
LAMBA|Aydınlatma aracı
LANET|Beddua
LARVA|Böceğin kurtçuğu
LASTİK|Tekerlek kaplaması
LAZER|Güçlü ışık
LAZIM|Gerekli
LEGAL|Yasal
LEKE|Kir izi
LEVHA|Yazılı tabela
LEZZET|Tat
LEZİZ|Çok lezzetli
LOKANTA|Yemek servis eden yer
LOKUM|Şekerli tatlı
LOR|Tuzsuz bir peynir
LÜFER|Bir balık
LÜGAT|Sözlük
LİDER|Önder
LİG|Takımların yarıştığı düzen
LİKÖR|Tatlı içki
LİMAN|Gemilerin yanaştığı yer
LİMON|Ekşi sarı bir meyve
LİMİT|Sınır
LİRA|Para birimimiz
LİSANS|Üniversite derecesi
LİSTE|Sıralı kayıt
LİTRE|Sıvı ölçüsü
MADDE|Yer kaplayan şey
MADEN|Yer altında çıkarılır
MAFYA|Suç örgütü
MAHALLE|Şehrin küçük bölümü
MAHKEME|Bir adalet makamı
MAKAM|Mevki
MAKARNA|Bir hamur yemeği
MAKAS|Kâğıt keser
MAKET|Küçültülmüş örnek
MAKUL|Mantıklı
MAKİNE|Çalışan aygıt
MAL|Mülk
MALZEME|Yapım gereci
MAMUT|Tüylü soyu tükenmiş fil
MANAV|Sebze meyve satan kişi
MANGO|Tropik bir meyve
MANTAR|Şemsiye biçimli yiyecek
MANTI|Bir hamur yemeği
MARANGOZ|Ahşap işçisi
MARTI|Denizde uçan bir kuş
MARUL|Yeşil salata yaprağı
MASAJ|Kas gevşetme
MASAL|Bir varmış bir yokmuş
MASKE|Yüzü örter
MASRAF|Gider
MASUM|Suçsuz
MAT|Parlak olmayan
MATEM|Yas
MAVİ|Gökyüzünün rengi
MAYA|Hamuru kabartır
MAYIN|Patlayıcı tuzak
MAYIS|Bir ay
MAYMUN|Ağaçta zıplayan hayvan
MAZOT|Dizel yakıt
MAĞAZA|Büyük dükkân
MECBURİ|Zorunlu
MEDYA|Haber kuruluşları
MEKAN|Yer, konum
MEKTEP|Okul
MEKTUP|Yazılı posta
MELEK|Göksel varlık
MEMUR|Devlet görevlisi
MERAK|Öğrenme isteği
MERDİVEN|Kat çıkılan basamaklar
MERKEZ|Orta nokta
MERMİ|Silahın ateşlediği şey
MERT|Cesur
MESAJ|İleti
MESELE|Konu
MESLEK|Uğraş
METAL|Maden
METRE|Uzunluk birimi
METRO|Yer altı treni
METİN|Yazı parçası
MEVKİ|Konum
MEVSİM|Yılın dörtte biri
MEYDAN|Şehirde geniş alan
MEYVE|Ağaçta yetişen yiyecek
MEZAR|Ölünün yatağı
MEZUN|Okulu bitiren
MEŞE|Palamutlu ağaç
MISIR|Koçanlı tahıl
MOBİL|Taşınabilir
MODA|Giyim akımı
MODEL|Örnek
MODEM|İnternet cihazı
MODÜL|Birim, ünite
MOLA|Dinlenme arası
MOLOZ|Yıkıntı parçaları
MOR|Bir renk
MORAL|Ruh hali
MOTEL|Yol kenarı oteli
MOTOR|Aracı yürüten makine
MOTİF|Süs deseni
MUCİT|İcat yapan kişi
MUHABBET|Sohbet
MUHTAR|Mahalle yöneticisi
MUKAVVA|Kalın karton
MUM|Işık veren bir eşya
MUMYA|Antik Mısır'da korunmuş ceset
MUTFAK|Yemek pişen oda
MUTLU|Sevinçli
MUZ|Bir meyve
MÜCADELE|Savaş, uğraş
MÜDÜR|Kurum başkanı
MÜJDE|İyi haber
MÜKEMMEL|Kusursuz
MÜZİK|Seslerle yapılan sanat
MÜZİSYEN|Sanatçı çalgıcı
MÜŞTERİ|Alışveriş eden
Mİ|Bir nota
MİDYE|Kabuklu deniz canlısı
MİKROFON|Ses yükselten şey
MİL|Deniz ölçüsü
MİLLET|Ulus
MİLLİ|Ulusal
MİMAR|Bina tasarlayan kişi
MİMARİ|Bina sanatı
MİMİK|Yüz hareketi
MİNİK|Çok küçük
MİRAS|Atadan kalan
MİSAFİR|Konuk
NADİR|Az bulunan, ender
NAKLİYE|Taşıma
NAKİT|Eldeki para
NAL|Atın ayağına çakılır
NAMAZ|Dinî ibadet
NAMUS|Onur
NANE|Ferahlatan ot
NAR|Bir meyve
NASIL|Bir soru sözü
NASİP|Kısmet
NAZ|Cilve
NAZİK|Kibar
NE|Bir soru sözü
NEDEN|Sebep
NEFES|Soluk
NEFRET|Tiksinme
NEFİS|Çok lezzetli, leziz
NEHİR|Büyük akarsu
NEM|Havadaki su buharı
NESNE|Cisim
NESNEL|Öznel olmayan
NESİL|Kuşak
NEZLE|Burun akıntısı
NEŞE|Sevinç
NOKTA|Cümle sonu işareti
NOTA|Müzik işareti
NUMARA|Sayı
NUTUK|Uzun konuşma
NÖBET|Sıra bekleme
NÜFUS|Halk sayısı
NİKAH|Evlenme akdi
NİMET|Lütuf
NİNE|Büyükanne
NİSAN|Bir ay
NİYET|Amaç
NİÇİN|Neden
NİŞAN|Evlilik öncesi tören
OBJEKTİF|Nesnel
OCAK|Yılın ilk ayı
ODA|Evin bir bölümü
ODAK|Merkez nokta
ODUN|Sobada yakılan şey
OK|Yayın fırlattığı
OKUL|Öğrencinin gittiği yer
OKYANUS|Çok geniş deniz
OLAY|Vaka
OLTA|Bir balıkçı eşyası
OMLET|Yumurtadan yapılır
OMUZ|Kolun gövdeye bağlandığı yer
ON|Bir sayı
ONUR|Haysiyet
OPERA|Şarkılı sahne eseri
OPTİK|Görme ile ilgili
ORAN|Nispet
ORDU|Askerî güç
ORGAN|Vücut parçası
ORKESTRA|Çalgıcı topluluğu
ORMAN|Ağaçlı geniş alan
ORTA|Tam merkez
ORTAM|Çevre
OT|Bir bitki
OTEL|Konaklanan yer
OTOBÜS|Yolcu taşıyan araç
OTOPARK|Çoklu araba garajı
OTURMAK|Çömelip yere konmak
OVA|Dağlar arasındaki düzlük
OY|Seçimde verilir
OYA|İğne ya da tığla yapılan süs
OYUN|Çocukların eğlencesi
OĞLAN|Erkek çocuk
PABUÇ|Ayakkabı
PADİŞAH|Osmanlı hükümdarı
PAGAN|Çok tanrıya inanan
PAK|Temiz
PAKET|Sarılı eşya
PALTO|Kışlık üst giysisi
PAMUK|Çukurova'da yetişen bitki lifi
PANDA|Bambu yiyen ayı
PANEL|Tartışma toplantısı
PANTOLON|Bir alt giysisi
PANİK|Ani korku
PAPAZ|Kilise görevlisi
PAPAĞAN|Konuşan renkli kuş
PARA|Alışverişte verilir
PARAŞÜT|Havada süzülmeyi sağlar
PARKE|Zemin kaplaması
PARLAK|Işıltılı
PARTİ|Siyasi örgüt
PARÇA|Bölüm
PAS|Demirin çürümesi
PASAJ|Dar kapalı çarşı
PASAPORT|Yurt dışı kimliği
PASTA|Doğum günü tatlısı
PASİF|Hareketsiz
PAT|Satrançta berabere
PATATES|Kızartması yapılan bir sebze
PAY|Hisse
PAZAR|Bir gün
PEDAL|Bisikleti döndürür
PEK|Çok, fazlasıyla
PEMBE|Bir gül rengi
PENCERE|Işık giren cam
PERON|Tren bekleme yeri
PERUK|Takma saç
PERVAZ|Pencere kenarlığı
PERİ|Masal kahramanı
PERŞEMBE|Çarşambadan sonraki gün
PETEK|Arının bal yaptığı yer
PEÇE|Yüzü örten
PINAR|Kaynak, memba
PLAJ|Kumsal
PLAKA|Araç numarası
PLAN|Tasarı
PLATO|Yüksek düz alan
POKER|Kâğıt oyunu
POLEN|Çiçek tozu
POLİS|Asayişi sağlayan kişi
POMPA|Su çeker
PORTAKAL|Turunçgillerden bir meyve
PORTRE|Yüz resmi
POST|Hayvan derisi
POSTA|Mektup servisi
POŞET|Alışverişte taşınır
PRENS|Kralın oğlu
PRENSES|Kralın kızı
PROJE|Tasarı
PRİZ|Fişin takıldığı yer
PUAN|Skor
PUL|Mektuba yapışır
PUSU|Saklanıp bekleme
PUT|Tapılan heykel
PİDE|Ramazan ekmeği
PİLAV|Pirinçten yapılan yemek
PİLOT|Uçağı kullanan kişi
PİRE|Zıplayan bir haşere
PİRİNÇ|Pilav hammaddesi
PİYES|Tiyatro oyunu
PİYON|Satrançta bir taş
PİZZA|İtalyan yemeği
PİŞMEK|Ateşte olgunlaşmak
RADAR|Uçak izleme cihazı
RADYO|Sesli yayın aracı
RAF|Kitap dizilen yer
RAHAT|Sıkıntısız, sakin
RAKAM|Sayıyı gösteren işaret
RAKET|Tenis oyun aracı
RAKI|Anasondan yapılan bir içki
RAKİP|Karşı taraf
RAMPA|Eğik yol
RAPOR|Resmî yazılı bildirim
RAY|Tren yolu
RE|Bir nota
REJİM|Yönetim biçimi
REKOR|En iyi derece
RENKLİ|Siyah beyaz olmayan
RESMİ|Devlete ait
RESİM|Ressam eseri
RET|Kabul etmeme
REZİL|Utanç verici
REÇEL|Meyveden yapılan tatlı
REÇETE|Doktorun yazdığı ilaç kâğıdı
ROBOT|Makine insan
RODEO|Boğa binme yarışı
ROKET|Bazuka
ROMAN|Uzun kurgu eser
ROTA|Gemi yolu
ROZET|Göğüse takılan işaret
RUH|Tin
RUTİN|Alışılmış düzen
RÜTBE|Askerî derece
RİTİM|Ölçülü ses düzeni
RİZE|Bir şehrimiz
SAAT|Zamanı gösteren eşya
SABAH|Günün başı
SABIR|Sakin bekleme
SABİT|Yerinden oynamayan
SADE|Süssüz
SAF|Karışık olmayan
SAHNE|Tiyatro oynanan yer
SAHTE|Gerçek olmayan
SAHİL|Deniz kıyısı
SAHİP|Malik
SAKAL|Erkek yüzünün kılları
SAKAT|Sağlam olmayan
SAKIZ|Çiğnenen şeker
SAKSI|Çiçeğin toprak kabı
SAL|Bir deniz taşıtı
SALATA|Çiğ sebze karışımı
SALEP|Bir kış içeceği
SALON|Geniş oturma odası
SAMAN|Hayvan yemi
SAN|Ün, ad
SANAL|Gerçek olmayan
SANAT|Güzel beceri
SANDALYE|Üzerine oturulan şey
SANDIK|Kapaklı büyük kutu
SANKİ|Öyleymiş gibi
SARAY|Padişah konutu
SARI|Bir renk
SARMA|Yaprağa sarılan yemek
SATIR|Yazı dizisi
SATIŞ|Satma işi
SAUNA|Buharlı oda
SAVAŞ|Harp
SAVCI|İddia makamı
SAVUNMA|Korunma
SAYFA|Kitabın yaprağı
SAZ|Telli çalgı
SAÇ|Başın kılları
SAĞ|Sol karşıtı
SAĞIR|Duyamayan kişi
SAĞLIK|Sıhhat, esenlik
SEBEP|Neden
SEDYE|Hasta taşıma aracı
SEFALET|Yoksulluk
SEFER|Yolculuk
SEHPA|Salon masası
SEKRETER|Ofis yardımcısı
SEKİZ|Bir sayı
SEL|Taşan dere
SELAM|Merhaba
SEN|İkinci tekil kişi
SENET|Borç belgesi
SENFONİ|Orkestra eseri
SEPET|Örülmüş taşıma kabı
SERA|Bitki yetiştirilen yer
SERGİ|Eserlerin gösterildiği yer
SERT|Yumuşak karşıtı
SERÇE|Küçük kahverengi kuş
SERİ|Dizi
SERİN|Hafif soğuk
SES|Kulakla duyulan
SET|Tenisin bir bölümü
SEVDA|Aşk
SEVGİ|Sevme duygusu
SEYAHAT|Yolculuk
SEZON|Mevsim
SEÇME|Ayırma
SEÇİM|Oy verme günü
SICAK|Soğuk karşıtı
SIFAT|Niteleme sözü
SIFIR|Nötr sayı
SINAV|Test
SINIF|Dersin yapıldığı oda
SINIR|Ülkeleri ayırır
SIRA|Beklerken girilen dizi
SIRT|Gövdenin arkası
SITMA|Sivrisinekle bulaşan hastalık
SIĞIR|İnek ve boğa
SOBA|Isıtmaya yarayan bir eşya
SOFRA|Yemek masası
SOHBET|Konuşma
SOKAK|Evlerin arasındaki yol
SOL|Sağ karşıtı
SOMON|Pembe etli balık
SON|Bitiş
SONRA|Önce karşıtı
SORGU|Sorulu inceleme
SORU|Sual
SORUN|Çözülmesi gereken şey
SOSİS|İnce et ürünü
SOYUT|Somut karşıtı
SOĞUK|Sıcak karşıtı
SOĞUKLUK|Soğuk olma durumu
SPOR|Bedeni geliştiren faaliyet
SPORCU|Yarışan kişi
STADYUM|Maç yapılan yer
STRES|Gerginlik
SU|Hayat kaynağı
SUCUK|Baharatlı et ürünü
SULAK|Suyu bol olan yer
SULTAN|Padişah
SUNUM|Gösterim
SUÇLU|Cezalandırılacak kişi
SÖZCÜ|Adına konuşan
SÖZLÜ|Yazılı olmayan
SÖZLÜK|Lügat
SÜMBÜL|Güzel kokulu bahar çiçeği
SÜPER|Harika
SÜPÜRGE|Yeri temizleme aleti
SÜRAT|Hız
SÜRE|Zaman aralığı
SÜREÇ|Gidişat
SÜRGÜN|Zorla uzak yere gönderme
SÜRÜCÜ|Şoför
SÜT|İneğin verdiği besin
SÜTLÜ|Sütü olan
SÜTUN|Direk
Sİ|Bir nota
SİGORTA|Güvence ödemesi
SİHİR|Büyü
SİLAH|Savaş aracı
SİLGİ|Silmeye yarayan eşya
SİMGE|Sembol
SİMİT|Susamlı halka
SİNEK|Vızıldayan haşere
SİNEMA|Film izlenen yer
SİNİR|Asap, öfke
SİREN|Uyarı sesi çıkarır
SİRK|Palyaçoların yeri
SİRKE|Ekşi sıvı
SİS|Görüşü azaltan bulut
SİVRİ|Ucu keskin
SİVİL|Askerî olmayan
SİYAH|Kömür rengi
TABAK|Yemek konan eşya
TABAN|Tavan karşıtı
TABELA|Dükkânın yazılı levhası
TABLO|Çerçeveli resim
TABUT|Cenaze sandığı
TAHT|Kralın oturduğu yer
TAHTA|Odun levha
TAKIM|Aynı renkte oynayan grup
TAKSİ|Sarı araç
TAKVİM|Günleri gösterir
TAKİP|İzleme
TALEP|İstek
TAMAM|Eksiksiz
TAMAMEN|Bütünüyle
TAMİR|Onarım
TAN|Şafak
TANIK|Olayı gören, şahit
TANIM|Açıklama, tarif
TANRI|Yaratıcı
TARAF|Yan
TARAK|Saçı düzelten şey
TARHANA|Kurutulmuş çorba karışımı
TARIM|Çiftçilik, ziraat
TARLA|Ekin ekilen yer
TARTI|Ağırlık ölçer
TARZ|Biçim, stil
TARİF|Yemek reçetesi, tanım
TARİH|Geçmişi inceleyen bilim dalı
TAS|Çukur kap
TASARIM|Çizim planı
TASMA|Köpeğin boynuna takılır
TAT|Lezzet
TATLI|Şekerli yemek
TATİL|Dinlenme zamanı
TAVA|Yassı pişirme kabı
TAVAN|Taban karşıtı
TAVLA|Zar ve pulla oynanan bir oyun
TAVUK|Yumurtlayan bir kuş
TAVŞAN|Uzun kulaklı hayvan
TAY|Atın yavrusu
TAZE|Bayat karşıtı
TAZİYE|Başsağlığı
TAŞ|Sert kaya parçası
TEBEŞİR|Kara tahta kalemi
TEBRİK|Kutlama
TEK|Çift olmayan
TEKERLEK|Dönen yuvarlak parça
TEKNE|Küçük gemi
TEL|İnce metal ip
TELAŞ|Acele heyecanı
TELEFON|Uzaktan konuşma aracı
TEMAS|Dokunma
TEMBEL|Çalışmayı sevmeyen
TEMMUZ|Bir ay
TEMİZ|Kirli karşıtı
TEN|Derinin rengi
TENCERE|Yemek pişirilen kap
TENİS|Raketle oynanan spor
TEPE|Küçük yükselti
TEPKİ|Karşılık, reaksiyon
TEPSİ|Yemek taşıma kabı
TERAZİ|Tartı aleti
""",
        """
TERLİK|Evde giyilen ayakkabı
TERZİ|Elbise diken kişi
TEYZE|Annenin kız kardeşi
TEZ|Çabuk
TEŞEKKÜR|Minnet sözü
TIP|Hekimlik bilimi
TIRAŞ|Saç veya sakal kesme
TOHUM|Ekilince bitki verir
TOK|Aç karşıtı
TOKAT|Şamar, sille
TON|Bin kilogram
TOPLANTI|Bir araya gelme
TOPRAK|Ekinin yetiştiği yer
TOPUK|Ayağın arkası
TORBA|Eşya konan kese
TORUN|Çocuğun çocuğu
TOTAL|Toplam
TRAKTÖR|Tarla aracı
TREN|Raylarda giden bir taşıt
TUHAF|Garip
TUTAR|Toplam miktar
TUTKU|Çok güçlü istek
TUTUKLU|Cezaevinde suçlu
TUZAK|Kurulan düzen, kapan
TUZLU|Tuzu fazla olan
TUĞLA|Evin yapı taşı
TÖREN|Resmî kutlama
TÜFEK|Av silahı
TÜL|Perde kumaşı
TÜMÖR|Vücutta oluşan şişlik
TÜNEL|Dağı delen yol
TÜP|Gaz saklanan kap
TÜRKÜ|Halk şarkısı
TÜRKİYE|Ülkemiz
TÜRLÜ|Çeşitli
TÜTÜN|Sigara yaprağı
TÜY|Kuşun örtüsü
TÜZÜK|Kuruluş yönetmeliği
TİLKİ|Kurnaz hayvan
TİP|Karakter, çeşit
TİTİZ|Özenli
TİYATRO|Sahne sanatı
UCUZ|Pahalı karşıtı
UFAK|Küçük
ULAK|Haberci
ULAMA|Eklenerek bağlama
ULAŞIM|Taşıma düzeni
ULU|Büyük
ULUS|Millet
UMUT|Beklenti, ümit
UNUTMAK|Aklından çıkarmak
USTA|Çırağın hocası
UTANÇ|Mahcubiyet
UYANIK|Açıkgöz
UYGUN|Yakışan
UYKU|Gözleri kapatıp dinlenme
UYKUSUZ|Uykusu olmayan
UYUM|Ahenk
UZAK|Yakın karşıtı
UZAY|Yıldızların bulunduğu yer
UZMAN|Alanında bilgili
UÇAK|Havada giden taşıt
UÇURTMA|Rüzgârda yükselir
UÇURUM|Dik yamaç
VADİ|Dağlar arasındaki çukur
VAGON|Treni oluşturan bölüm
VAHŞİ|Yabani
VAKİT|Zaman
VALİLİK|İlin yönetim binası
VALİZ|Bavul
VAN|Gölüyle ünlü şehrimiz
VAPUR|Bir deniz taşıtı
VAR|Yok'un tersi
VATAN|Yurt
VAZİFE|Görev
VEBA|Salgın bir hastalık
VEFA|Sadakat
VEFAT|Ölüm
VEKİL|Temsilci
VEREM|Akciğer hastalığı
VERİ|Data
VERİM|Ürün miktarı
VESİKA|Resimli belge
VOTKA|Rus içkisi
VÜCUT|Beden
VİCDAN|İç ses
VİDA|Dişli çivi
VİDEO|Hareketli görüntü
VİLLA|Bahçeli lüks ev
VİRAJ|Yolda dönemeç
VİRÜS|Hastalık yapan mikrop
VİSKİ|İskoç içkisi
VİTRİN|Camlı sergi bölmesi
VİŞNE|Bir meyve
YAHNİ|Bir et yemeği
YAHUT|Veya
YAK|Tüylü Himalaya sığırı
YAKA|Gömleğin boyun kısmı
YAKIN|Uzak karşıtı
YAKIT|Benzin, mazota verilen ad
YAKLAŞIK|Tahminen
YALAN|Doğru olmayan söz
YAMA|Delik kapatır
YANAK|Yüzün iki yanı
YANGIN|İtfaiyenin söndürdüğü
YANIT|Cevap
YANKI|Geri gelen ses, akis
YAPAY|Doğal olmayan
YAPI|Bina
YAPIM|Üretim, imal
YAPIT|Eser
YAR|Sevgili
YARAR|Fayda
YARDIM|El uzatma
YARGI|Hüküm
YARI|Bütünün ikide biri
YARIM|Tam olmayan
YARIN|Bugünden sonraki gün
YARIŞ|Hız karşılaştırması
YARIŞMA|Birinci seçilen etkinlik
YAS|Ölüm acısı
YASA|Kanun
YASAK|İzin verilmeyen
YASAL|Kanuna uygun
YATAK|Uyunan mobilya
YATAY|Dik olmayan
YAVAŞ|Hızlı karşıtı
YAVRU|Hayvanın çocuğu
YAY|Ok fırlatır
YAYA|Yürüyen kişi
YAYIN|Radyo ya da televizyon çıktısı
YAYLA|Yazın çıkılan yüksek otlak
YAZ|Bir mevsim
YAZAR|Romanı kaleme alan
YAZIK|Bir üzüntü sözü
YAZLIK|Tatil evi
YAĞ|Zeytinden çıkarılır
YAĞIŞ|Gökten düşen su
YAĞMUR|Gökten düşen damlalar
YAŞAM|Hayat
YAŞLI|Genç olmayan
YAŞLILIK|Gençlik karşıtı
YEDEK|Asıl olmayan, ikinci
YEL|Rüzgâr, bad
YELEK|Kolsuz bir giysi
YEM|Hayvan yiyeceği
YEMEK|Öğün
YEMİN|And içme
YEMİŞ|Ağaçta biten tatlı yiyecek
YENGE|Ağabeyin eşi
YENİ|Eski karşıtı
YENİDEN|Tekrar
YER|Üzerinde durulan
YEREL|Bölgesel
YERLİ|Yabancı olmayan
YETKİ|Yönetme hakkı
YETİM|Anne babasız çocuk
YEĞEN|Kardeşin çocuğu
YEŞİL|Bir renk
YIKIM|Yıkma işi
YIL|On iki ay
YILAN|Sürüngen
YILDIZ|Gökte ışıldar
YIĞIN|Birikim, stok
YOK|Var karşıtı
YOKSUL|Fakir
YOL|Gidilen güzergâh
YOLCU|Taşıtla giden
YOLCULUK|Seyahat
YONCA|Dört yapraklı ot
YORGAN|Üstümüze örtülür
YORUM|Anlamlandırma
YOSUN|Suda biten bitki
YOĞUN|Kıvamlı
YUDUM|Bir içimlik sıvı
YUFKA|İnce açılmış hamur
YULAF|At yemi
YUMAK|İp topu
YURT|Memleket
YUVA|Kuşun evi
YÜKSEK|Alçak karşıtı
YÜREK|Kalp
YÜZDE|Yüzün parçası, oran
YÜZYIL|Yüz yıllık zaman
YÜZÜK|Parmağa takılır
YİRMİ|Bir sayı
YİĞİT|Cesur
ZAFER|Galibiyet
ZAM|Fiyat artışı
ZAMBAK|Beyaz kokulu bir çiçek
ZAR|Tavlada atılır
ZARAR|Kayıp, ziyan
ZARİF|İnce, nazik
ZAYIF|Güçsüz
ZEBRA|Çizgili bir hayvan
ZEHİR|Öldürücü madde
ZEHİRLİ|Öldürücü
ZEKİ|Akıllı
ZEMİN|Taban
ZENGİN|Varlıklı
ZEYTİN|Yağı çıkan küçük meyve
ZIMBA|Kâğıtları tutturan şey
ZOR|Kolay karşıtı
ZORBA|Baskıcı
ZULÜM|Eziyet
ZURNA|Üflemeli bir çalgı
ZÜMRÜT|Yeşil değerli bir taş
ZİHİN|Akıl
ZİNDE|Dinç
ZİRVE|Doruk
ÇABA|Gayret, efor
ÇABUK|Hızlı
ÇADIR|Kampta kurulur
ÇAKI|Katlanır küçük bıçak
ÇAKIL|Küçük yuvarlak taş
ÇALGI|Müzik aleti
ÇAM|İğne yapraklı ağaç
ÇAMAŞIR|İç giysi
ÇAMUR|Islak toprak
ÇANAK|Derin yemek kabı
ÇANTA|Eşya taşıyan şey
ÇARE|Çözüm yolu
ÇARŞAF|Yatak örtüsü
ÇARŞI|Dükkânlar bölgesi
ÇATI|Evin üstü
ÇAVDAR|Siyah tahıl
ÇAVUŞ|Astsubay rütbesi
ÇAY|Kırmızı renkli bir içecek
ÇAĞ|Devir
ÇAĞRI|Davet
ÇEKİM|Fotoğraf alma
ÇEKİRGE|Zıplayan yeşil böcek
ÇEKİÇ|Çivi çakmaya yarar araç
ÇELİK|Sert metal
ÇEREZ|Atıştırmalık
ÇERÇEVE|Resmi saran şey
ÇETE|Suçlular grubu
ÇETİN|Zor
ÇEVRE|Etraf
ÇEVİK|Atik, seri, hızlı
ÇEYİZ|Gelinin eşyası
ÇEŞME|Su akan yer
ÇEŞİT|Tür
ÇIKIŞ|Giriş karşıtı
ÇINAR|Gür gövdeli bir ağaç
ÇIRAK|Ustanın yanında öğrenen
ÇOBAN|Koyun güden kişi
ÇOCUK|Küçük insan
ÇOK|Az karşıtı
ÇORAP|Ayağa giyilen şey
ÇUBUK|İnce sopa
ÇUKUR|Oyuk
ÇUVAL|Kalın torba
ÇÖL|Sahra
ÇÖPLÜK|Atıkların yığıldığı yer
ÇÖZÜM|Sorunun cevabı
ÇÜRÜK|Bozulmuş
ÇİFT|İki parça
ÇİFTÇİ|Tarlada çalışan
ÇİLE|Dert, eziyet
ÇİLEK|Bir meyve
ÇİM|Yeşil bitki örtüsü
ÇİMENTO|Beton hammaddesi
ÇİNGENE|Gezgin halk
ÇİT|Bahçeyi çevirir
ÇİVİ|Çekiçle çakılır
ÇİZGİ|Hat
ÇİÇEK|Bahçede açar
ÖD|Safra
ÖDÜL|Kazananın aldığı şey
ÖFKE|Kızgınlık
ÖKÜZ|Çift süren hayvan
ÖLÇEK|Harita oranı
ÖLÇÜ|Uzunluk birimi
ÖLÜ|Yaşamayan, cansız
ÖLÜM|Yaşamın sonu
ÖMÜR|Yaşam süresi
ÖN|Arka karşıtı
ÖNCE|Sonra karşıtı
ÖNEM|Değer
ÖNERİ|Teklif
ÖNLEM|Tedbir
ÖRDEK|Vakvaklayan kuş
ÖRGÜT|Organizasyon
ÖRNEK|Misal
ÖRÜMCEK|Ağ ören böcek
ÖTEKİ|Diğeri
ÖYKÜ|Hikâye
ÖZEL|Genel karşıtı
ÖZET|Kısaltılmış anlatım
ÖZGÜN|Orijinal
ÖZGÜR|Bağımsız
ÖZGÜRLÜK|Bağımsızlık
ÖZLEM|Hasret
ÖĞLE|Gün ortası
ÖĞLEN|Gün ortası
ÖĞRENCİ|Okula giden kişi
ÖĞRETMEN|Sınıfta ders veren kişi
ÖĞÜT|Nasihat
ÜCRET|Maaş
ÜLKE|Devlet toprağı
ÜNLEM|Şaşkınlık anlatan sözcük
ÜNSÜZ|Sessiz harf
ÜNİTE|Birim
ÜREME|Çoğalma
ÜRKEK|Korkak
ÜRÜN|Mahsul
ÜST|Alt karşıtı
ÜSTÜN|Üstte olan, faik
ÜZGÜN|Kederli
ÜZÜM|Bağda yetişir
ÜÇ|Bir sayı
ÜÇGEN|Üç kenarlı şekil
İBRET|Ders alınacak olay
İBRİK|Çay ya da su kabı
İDARE|Yönetim
İDDİA|Savunulan görüş
İDEAL|Hayal edilen en iyi, ülkü
İFADE|Anlatım
İFLAS|Batma
İHALE|Açık artırmayla verilen iş
İKLİM|Bir yerin hava durumu
İKRAM|Konuğa sunulan şey
İKSİR|Sihirli içecek
İKİ|Bir sayı
İKİLİ|İki parçalı
İKİZ|Aynı anda doğan
İL|Vilayet
İLAÇ|Hastaya verilir
İLERİ|Geri karşıtı
İLGİ|Merak
İLHAM|Esin
İLKBAHAR|Bir mevsim
İLKE|Prensip
İLKEL|Ham, gelişmemiş
İLMEK|Örgünün tek halkası
İLİK|Kemiğin içi
İMAM|Camide namaz kıldıran kişi
İMSAK|Oruca başlama vakti
İNANÇ|Güven, iman
İNAT|Direnç
İNCE|Kalın karşıtı
İNCELEME|Dikkatle bakma
İNCİ|İstiridye mücevheri
İNCİR|Tatlı kuru meyve
İNEK|Süt veren bir hayvan
İNSAF|Merhamet
İNSAN|Akıllı canlı
İNSANLIK|Beşeriyet
İP|Dikişte kullanılır
İPEK|Kozadan elde edilen kumaş
İPTAL|Geçersiz kılma
İRADE|Kararlılık
İRONİ|İnce alay
İRİ|Büyük yapılı
İSKELE|Gemilerin yanaştığı yer
İSRAF|Boşa harcama
İSTASYON|Tren durağı
İSTEK|Dilek
İSYAN|Başkaldırı
İSİM|Ad
İTAAT|Söz dinleme
İYİCE|Tam olarak
İZ|Geride kalan işaret
İZCİ|Kamp yapan kişi
İÇERİ|Dışarı karşıtı
İŞ|Çalışma
İŞARET|Belirti
İŞGAL|Zorla ele geçirme
İŞLEM|Matematikte yapılan hesap
İŞLEV|Görev
İŞSİZ|Çalışmayan
İŞTAH|Yeme isteği
İŞÇİ|Fabrikada çalışan
ŞAFAK|Gün doğarken, tan
ŞAH|Satrançta bir taş
ŞAKA|Eğlence için söylenen
ŞAL|Omuza atılan örtü
ŞAMPİYON|Birinci olan kişi
ŞAN|Ün
ŞANS|Talih
ŞANSLI|Talihli
ŞAPKA|Başa takılan şey
ŞARAP|Üzümden yapılan içki
ŞARKI|Sözlü müzik
ŞART|Koşul
ŞEF|Mutfağın başı
ŞEHİR|Büyük yerleşim yeri
ŞEKİL|Biçim
ŞEMSİYE|Yağmurda kullanılan eşya
ŞENLİK|Eğlence
ŞEREF|Onur
ŞERİT|Dar bant
ŞOFÖR|Araba kullanan kişi
ŞU|İşaret zamiri
ŞUBAT|Bir ay
ŞURUP|Tatlı sıvı ilaç
ŞÖLEN|Büyük ziyafet
ŞÖYLE|Bu biçimde
ŞÜKÜR|Teşekkür
ŞÜPHE|Kuşku
ŞİFRE|Gizli kod
ŞİMDİ|Şu an
ŞİRİN|Sevimli
ŞİİR|Dizelerden oluşan yazı türü
ŞİŞE|Cam kap
""",
    )

    val EN: List<String> = listOf(
        """
ABLE|Having the skill
ABOUT|Approximately
ABOVE|Overhead
ABUSE|Mistreat
ACCENT|Way of pronouncing
ACCEPT|Receive willingly
ACE|Top playing card
ACID|Sour chemical
ACROSS|From one side to the other
ACT|Perform on stage
ACTION|Movie genre
ACTIVE|Energetic
ACTIVITY|Planned event
ACTOR|Stage performer
ACTUAL|Real
AD|Commercial break content
ADAPT|Adjust
ADD|Sum up
ADMIT|Confess
ADOPT|Take in as one's own
ADULT|Grown-up
ADVICE|Counsel
AFFAIR|Matter
AFRAID|Scared
AFTER|Following
AGAIN|Once more
AGE|Years lived
AGED|Elderly
AGENDA|Meeting list
AGENT|Spy or broker
AGO|In the past
AGREE|Share an opinion
AH|Sound of relief
AHEAD|In front
AID|Help
AIDE|Assistant
AIM|Take a target
AIR|We breathe it
AIRPLANE|Flying machine
ALARM|Warning signal
ALBUM|Collection of songs
ALE|Pub drink
ALERT|Watchful
ALIKE|Similar
ALIVE|Living
ALL|Everything
ALLOW|Permit
ALMOND|Oval nut
ALONE|By oneself
ALONG|Together with
ALPHABET|A to Z
ALSO|In addition
ALTER|Change
ALWAYS|Every time
AM|Morning abbreviation
AMID|In the middle of
AMONG|In the midst of
AMOUNT|Quantity
AN|Article before a vowel
ANCHOR|Ship holder
ANCIENT|Very old
AND|Plus
ANGEL|Winged messenger
ANGER|Fury
ANGLE|Corner measure
ANGRY|Mad
ANIMAL|Zoo resident
ANKLE|Foot joint
ANNUAL|Yearly
ANSWER|Reply
ANT|Tiny colony insect
ANTS|Colony insects
ANY|Whichever
ANYBODY|Any person
ANYONE|Any person
APART|Separated
APE|Tailless primate
APPETITE|Desire for food
APPLE|Orchard fruit
APPLY|Put to use
ARC|Curved line
ARCH|Curved structure
ARCHER|Bow user
ARE|Form of be
AREA|Region
ARENA|Stadium
ARGUE|Quarrel
ARISE|Get up
ARM|Between shoulder and hand
ARMOR|Knight's protection
ARMY|Soldiers
ARROW|Archer's shot
ART|Painting or sculpture
ARTIST|Painter
AS|Equally
ASH|Fire remains
ASIDE|To one side
ASK|Pose a question
ASSET|Valuable item
AT|Preposition of place
ATE|Had dinner
ATTACK|Assault
AUDIO|Sound
AUNT|Parent's sister
AUTUMN|Fall season
AVOID|Stay away from
AWAKE|Not asleep
AWARD|Prize
AWARE|Conscious
AWAY|Not here
AWE|Wonder
AXE|Wood chopper
BABY|Newborn
BACK|Rear
BACON|Breakfast strips
BAD|Not good
BADGE|Pinned emblem
BAG|Carries groceries
BAKE|Cook in an oven
BAKER|Bread maker
BAKERY|Bread shop
BALD|Hairless
BALL|Round toy
BALLET|Dance on tiptoe
BAN|Forbid
BANANA|Yellow fruit
BAND|Music group
BANK|Money place
BANNER|Flag
BAR|Pub counter
BARBER|Haircut giver
BARE|Naked
BARN|Farm building
BARREL|Wooden cask
BASE|Bottom
BASEBALL|Diamond game
BASEMENT|Underground floor
BASIC|Fundamental
BASIN|Washing bowl
BASIS|Foundation
BASKET|Woven carrier
BAT|Flying mammal
BATCH|Group of cookies
BATH|Tub soak
BATTLE|Fight
BAY|Sea inlet
BE|Exist
BEACH|Sandy shore
BEAD|Necklace piece
BEAM|Ray of light
BEAN|Chili ingredient
BEAR|Large furry mammal
BEARD|Facial hair
BEAST|Wild animal
BEAT|Defeat
BEAUTY|Loveliness
BED|Where you sleep
BEE|Honey maker
BEEF|Steak meat
BEER|Hoppy drink
BEG|Plead
BEGIN|Start
BEHIND|At the back
BEING|Existence
BELIEF|Faith
BELL|Ringing metal cup
BELLY|Stomach
BELOW|Underneath
BENCH|Park seat
BEND|Curve
BERRY|Small juicy fruit
BESIDE|Next to
BEST|Top quality
BET|Wager
BETTER|Superior
BEYOND|Past
BIG|Large
BIKE|Two-wheeler
BILL|Restaurant check
BIN|Trash holder
BIRD|Flying animal
BIRTH|Coming into life
BIRTHDAY|Yearly celebration
BISHOP|Church official
BIT|Small piece
BITE|Chomp
BITTER|Not sweet
BLACK|Coal color
BLADE|Knife edge
BLAME|Hold responsible
BLANK|Empty
BLANKET|Bed covering
BLAST|Explosion
BLAZE|Big fire
BLEED|Lose blood
BLEND|Mix
BLESS|Wish well
BLIND|Unable to see
BLOCK|Cube of wood
BLOOD|Red fluid
BLOOM|Flower
BLOSSOM|Flower bloom
BLOW|Gust
BLUE|Sky color
BLUR|Fuzzy image
BOARD|Plank
BOAST|Brag
BOAT|Floats on water
BOLD|Daring
BOLT|Fastening screw
BOMB|Explosive
BONE|Skeleton part
BONUS|Extra reward
BOOK|You read it
BOOST|Lift up
BOOT|Winter footwear
BOOTH|Small stall
BORDER|Frontier
BORN|Brought into life
BOSS|Manager
BOTH|The two
BOTTLE|Water holder
BOTTOM|Lowest part
BOUND|Tied
BOUNDARY|Limit line
BOW|Bend forward
BOWL|Soup dish
BOX|Cardboard container
BOY|Young male
BRA|Undergarment
BRAG|Boast
BRAIN|Thinking organ
BRAKE|Car stopper
BRANCH|Tree limb
BRAND|Product name
BRAVE|Courageous
BREAD|Toast base
BREAK|Smash
BREATH|Air taken in
BREED|Type of dog
BRICK|Wall block
BRIDE|Wedding woman
BRIDGE|River crossing
BRIEF|Short
BRIGHT|Shining
BRIM|Hat edge
BRING|Carry here
BROAD|Wide
BROKEN|Smashed
BROOK|Small stream
BROWN|Chocolate color
BRUSH|Painter's tool
BUBBLE|Soap ball
BUCKET|Pail
BUD|Flower in the making
BUDGET|Spending plan
BUILD|Construct
BUILDING|Tall structure
BULB|Light source
BULK|Large amount
BUN|Hamburger bread
BUNCH|Cluster
BURN|Set on fire
BURST|Pop open
BUS|Public transport vehicle
BUSH|Low shrub
BUSINESS|Company
BUSY|Occupied
BUTTER|Bread spread
BUTTON|Shirt fastener
BUY|Purchase
BUYER|Shopper
BUZZ|Bee sound
BY|Next to
CAB|Taxi
CABIN|Log house
CABLE|Thick wire
CAFE|Coffee shop
CAGE|Zoo enclosure
CAKE|Birthday dessert
CALF|Baby cow
CALL|Phone
CALM|Peaceful
CAME|Arrived
CAMERA|Photo taker
CAMP|Tent site
CAN|Metal container
CANAL|Man-made waterway
CANDLE|Wax light
CANDY|Sweet treat
CANE|Walking stick
CANVAS|Painter's cloth
CAP|Baseball headwear
CAPE|Superhero garment
CAPTAIN|Ship's leader
CAR|Four-wheeled vehicle
CARD|Playing item
CARE|Concern
CARGO|Ship's load
CARPET|Floor covering
CARROT|Orange vegetable
CARRY|Haul
CART|Shopping pusher
CASE|Luggage piece
CASH|Paper money
CAST|Actors in a play
CASTLE|Fortress
CAT|Purring pet
CATCH|Grab a ball
CATTLE|Farm cows
CAUGHT|Grabbed
CAULDRON|Witch's pot
CAUSE|Reason
CAVE|Underground hollow
CEILING|Room top
CELL|Prison room
CENTER|Middle
CEREMONY|Formal event
CHAIN|Linked rings
CHAIR|Seat with a back
CHALK|Blackboard writer
CHAMPION|Winner
CHANCE|Opportunity
CHANGE|Coins back
CHAPTER|Book section
CHARM|Appeal
CHART|Graph
CHASE|Pursue
CHEAP|Low in price
CHEAT|Break the rules
CHECK|Examine
CHEEK|Part of the face
CHEER|Shout of joy
CHEESE|Mouse snack
CHEF|Kitchen boss
CHERRY|Red pitted fruit
CHESS|Board game with kings
CHEST|Treasure box
CHIEF|Leader
CHILD|Kid
CHILDREN|Kids
CHILL|Cold feeling
CHIMNEY|Smoke stack
CHIN|Face's lower part
CHINA|Fine porcelain
CHIP|Crisp slice
CHOICE|Option
CHOIR|Singing group
CHOOSE|Select
CHORD|Notes played together
CHURCH|House of worship
CINEMA|Movie theater
CIRCLE|Round shape
CITY|Large town
CIVIL|Polite
CLAIM|Assert
CLAP|Applaud
CLASS|School group
CLASSIC|Timeless
CLAY|Potter's material
CLEAN|Spotless
CLEAR|Transparent
CLERK|Office worker
CLEVER|Smart
CLICK|Mouse sound
CLIENT|Customer
CLIFF|Steep rock face
CLIMATE|Weather pattern
CLIMB|Go up
CLIP|Paper holder
CLOCK|Time keeper
CLOSE|Shut
CLOSET|Wardrobe
CLOTH|Fabric
CLOUD|Sky puff
CLOWN|Circus joker
CLUB|Golf stick
CLUE|Hint
COACH|Team trainer
COAL|Black fuel
COAST|Seashore
COAT|Winter garment
CODE|Secret system
COFFEE|Morning brew
COG|Gear tooth
COIN|Small money
COLD|Chilly
COLUMN|Pillar
COMB|Hair tool
COME|Arrive
COMMON|Ordinary
COMPASS|North finder
COMPUTER|Laptop
CONCERT|Live music event
CONTEST|Competition
COOK|Prepare food
COOKIE|Biscuit
COOL|Not warm
COPPER|Reddish metal
COPY|Duplicate
CORD|Thin rope
CORE|Center
CORN|Maize
CORNER|Where walls meet
COST|Price
COT|Small bed
COTTON|Soft plant fiber
COUCH|Sofa
COUGH|Throat clearing
COUNT|Say numbers
COUNTRY|Nation
COURAGE|Bravery
COURT|Tennis place
COUSIN|Aunt's child
COVER|Lid
COW|Gives milk
COZY|Snug
CRACK|Split
CRADLE|Baby bed
CRAFT|Skilled work
CRASH|Collision
CRAZY|Wild
CREAM|Coffee whitener
CREATURE|Living being
CREDIT|Praise
CREW|Ship staff
CRICKET|Chirping insect
CRIME|Illegal act
CROP|Harvest
CROSS|Angry
CROWD|Large group
CROWN|King's headgear
CRUSH|Squash
CRY|Shed tears
CRYSTAL|Clear gem
CUB|Young bear
CUBE|Six-sided solid
CUP|Tea holder
CUPBOARD|Dish storage
CURE|Remedy
CURIOUS|Eager to know
CURVE|Bend
CUSTOM|Tradition
CUT|Slice
CUTE|Adorable
CYCLE|Repeating loop
DAD|Father
DAILY|Every day
DAIRY|Milk farm
DAISY|White flower
DAM|River barrier
DAMAGE|Harm
DAMP|Slightly wet
DANCE|Move to music
DANGER|Peril
DARE|Challenge
DARK|Without light
DATA|Information
DATE|Calendar day
DAUGHTER|Female child
DAWN|Sunrise
DAY|Twenty-four hours
DEAD|No longer alive
DEAF|Unable to hear
DEAL|Bargain
DEALT|Handed out cards
DEAR|Beloved
DEATH|End of life
DEBATE|Formal argument
DEBT|Money owed
DEBUT|First appearance
DECADE|Ten years
DECEMBER|Last month
DEEP|Far down
DEER|Antlered animal
DEFEAT|Beat
DEFEND|Protect
DEGREE|Diploma
DELAY|Hold up
DELICATE|Fragile
DEN|Lion's home
DENT|Small hollow in metal
DEPTH|How deep
DESERT|Sandy wasteland
DESIGN|Plan
DESIRE|Wish
DESK|Where you study
DETAIL|Small fact
DEW|Morning moisture
DIAL|Phone face
DICE|Gambling cubes
DID|Performed
DIE|Stop living
DIET|Eating plan
DIG|Use a shovel
DIM|Not bright
DINNER|Evening meal
DINOSAUR|Prehistoric giant
DIP|Quick swim
DIRECT|Straight
DIRT|Soil
DISCOVER|Find
DISH|Plate
DISTANCE|How far
DIVE|Plunge
DO|Perform
DOCK|Boat landing
DOCTOR|Medical professional
DOE|Female deer
DOG|Barking pet
DOING|Performing
DOLL|Child's figure
DOLLAR|Buck
DOLPHIN|Clever sea mammal
DOME|Rounded roof
DOMESTIC|Of the home
DON|Put on
DONE|Finished
DONKEY|Braying animal
DOOR|Entry to a room
DOSE|Medicine amount
DOT|Tiny round mark
DOUBLE|Twice as much
DOUBT|Uncertainty
DOVE|Peace bird
DOWN|Not up
DOZEN|Twelve
DRAFT|Rough version
DRAG|Pull along
DRAGON|Fire-breathing beast
DRAIN|Sink outlet
DRAMA|Theater play
DRANK|Had a beverage
DRAW|Sketch
DRAWER|Desk compartment
DREAM|Sleep vision
DRESS|Gown
DREW|Sketched
DRIED|Not wet
DRIFT|Float along
DRILL|Boring tool
DRINK|Beverage
DRIP|Fall in drops
DRIVE|Steer a car
DRIVER|Car operator
DROP|Let fall
DROUGHT|Dry spell
DROVE|Steered a car
DRUM|Beaten instrument
DRY|Not wet
DUAL|Double
DUCK|Quacking bird
DUE|Expected
DUKE|Noble rank
DULL|Not sharp
DUMB|Not smart
DUNE|Sand hill
DUO|Pair
DUSK|Evening twilight
DUST|Fine dirt
DYE|Color fabric
EACH|Every one
EAGER|Keen
EAGLE|Bird of prey
EAR|Hearing organ
EARLY|Ahead of time
EARN|Make money
EARTH|Our planet
EASE|Comfort
EASILY|With no trouble
EAST|Direction of sunrise
EASY|Simple
EAT|Have a meal
EBB|Retreat, as the tide
EDGE|Border
EDIT|Revise
EFFORT|Hard try
EGG|Laid by a hen
EH|Sound of doubt
EIGHT|Number after seven
ELBOW|Arm joint
ELDER|Older person
ELECT|Choose by vote
ELEPHANT|Trunked giant
ELF|Santa's helper
ELK|Large deer
ELSE|Other
EMPTY|Hollow
EMU|Flightless Australian bird
END|Finish
ENEMY|Opponent
ENERGY|Power
ENGINE|Car motor
ENJOY|Take pleasure in
ENOUGH|Sufficient
ENTER|Go in
ENTRANCE|Way in
ENTRY|Doorway
EPIC|Grand in scale
EQUAL|Same in value
ER|Hesitation sound
ERA|Period of time
ERROR|Mistake
ESCAPE|Get away
ESSAY|School composition
EVE|Night before
EVEN|Level
EVENING|Dusk time
EVENT|Happening
EVER|At any time
EVERY|Each
EVIL|Wicked
EWE|Female sheep
EX|Former partner
EXACT|Precise
EXAM|Test
EXAMPLE|Instance
EXCITING|Thrilling
EXCUSE|Reason given
EXERCISE|Workout
EXIST|Be
EXIT|Way out
EXPERT|Specialist
EXTRA|Additional
EYE|Seeing organ
FABRIC|Cloth
FACE|Front of the head
FACT|True statement
FAD|Passing craze
FADE|Lose color
FAIL|Not pass
FAINT|Weak
FAIR|Carnival
FAIRY|Tiny magical being
FAITH|Belief
FAKE|Not real
FALL|Autumn
FALSE|Untrue
FAME|Celebrity
FAMILIAR|Well known
FAMILY|Parents and kids
FAMOUS|Well known
FAN|Cools a room
FANCY|Elegant
FANG|Snake tooth
FAR|Not near
FARE|Ticket price
FARM|Where crops grow
FARMER|Crop grower
FAST|Quick
FASTEN|Attach
FAT|Not thin
FATAL|Deadly
FATE|Destiny
FATHER|Dad
FAULT|Blame
FEAR|Fright
FEAST|Large meal
FEATHER|Bird's covering
FED|Gave food
FEE|Charge
FEED|Give food
FEEL|Touch
FEET|Foot plural
FELL|Dropped
FELT|Soft fabric
FENCE|Yard border
FERN|Leafy plant
FERRY|Passenger boat
FESTIVAL|Celebration
FEVER|High temperature
FEW|Not many
FIBER|Dietary thread
FIELD|Open meadow
FIFTH|Number five in order
FIFTY|Half a hundred
FIG|Mediterranean fruit
FIGHT|Brawl
FIGURE|Number
FILE|Folder item
FILL|Make full
FILM|Movie
FINAL|Last
FIND|Locate
FINE|Okay
FINGER|Hand digit
FIRE|Flames
FIREWORK|Sky explosion
FIRM|Solid
FIRST|Number one
FISH|Swims in water
FIT|In good shape
FIVE|Number after four
FIX|Repair
FLAG|Banner on a pole
FLAME|Fire tongue
FLASH|Brief light
FLAT|Level
FLEA|Dog parasite
FLEET|Group of ships
FLESH|Muscle tissue
FLEW|Went by air
FLIP|Turn over
FLOAT|Stay on water
FLOCK|Group of sheep
FLOOD|Overflow of water
FLOOR|Room base
FLOUR|Baking powder
FLOW|Stream
FLOWER|Rose, for one
FLUID|Liquid
FLY|Insect with wings
FOAM|Bubbles
FOCUS|Concentrate
FOE|Enemy
FOLD|Crease
FOLDER|File holder
FOLK|People
FOND|Affectionate
FOOD|What we eat
FOOTBALL|Gridiron game
FOR|In favor of
FORCE|Strength
FOREHEAD|Brow
FOREST|Woods
FORGET|Fail to remember
FORK|Dining utensil
FORM|Shape
FORMAL|Dressy
FORT|Military stronghold
FORTH|Onward
FORTUNE|Luck
FORTY|Four tens
FORUM|Discussion place
FOSSIL|Ancient remains
FOUND|Discovered
FOUNTAIN|Spouting water feature
FOUR|Two plus two
FOX|Sly red animal
FRACTION|Part of a whole
FRAME|Picture border
FRANK|Honest
FRAUD|Scam
FREE|No cost
FREEDOM|Liberty
FREEZE|Turn to ice
FRESH|New
FRIDAY|Weekend eve
FRIEND|Pal
FRIENDLY|Kind
FROG|Hopping amphibian
FRONT|Forward side
FROST|Icy coating
FROZEN|Iced over
FRUIT|Orchard produce
FRY|Cook in oil
FUEL|Gasoline
FULL|Not empty
FUN|Enjoyment
FUND|Pool of money
FUNNY|Amusing
FURY|Rage
FUTURE|Time to come
GAIN|Obtain
GALAXY|Star system
GALE|Strong wind
GAME|Play activity
GAP|Opening
GARAGE|Car shelter
GARDEN|Plot of flowers
GARLIC|Pungent bulb
GAS|Fuel for stoves
GATE|Garden entrance
GATHER|Collect
GAZE|Stare
GEAR|Equipment
GEM|Precious stone
GENEROUS|Giving
GENTLE|Soft
GET|Obtain
GHOST|Spooky spirit
GIANT|Huge being
GIFT|Present
GIN|Juniper spirit
GINGER|Spicy root
GIRAFFE|Long-necked animal
GIRL|Young female
GIVEN|Presented
GLAD|Happy
GLANCE|Quick look
GLASS|Window material
GLOBE|World model
GLORIOUS|Splendid
GLORY|Fame
GLOVE|Hand cover
GLOW|Soft light
GLUE|Sticky stuff
GO|Move along
GOAL|Aim
GOAT|Horned farm animal
GOES|Departs
GOLD|Yellow metal
GOLDEN|Made of a yellow metal
GONE|Left
GOOD|Fine
GOT|Obtained
GRAB|Snatch
GRACE|Elegance
GRADE|School mark
GRAIN|Wheat kernel
GRAM|Metric weight unit
GRAND|Impressive
GRANT|Bestow
GRAPE|Wine fruit
GRASP|Hold tightly
GRASS|Lawn cover
GRATEFUL|Thankful
GRAVE|Burial place
GRAY|Mix of black and white
GREAT|Wonderful
GREEN|Leaf color
GREET|Say hello
GREW|Became bigger
GRID|Pattern of lines
GRIEF|Deep sorrow
GRILL|Barbecue rack
GRIN|Big smile
GRIP|Hold tightly
GROSS|Disgusting
GROUND|Earth
GROUP|Collection
GROVE|Small woods
GROW|Get bigger
GROWTH|Increase
GUARD|Protector
GUESS|Estimate
GUEST|Visitor
GUIDE|Tour leader
GUITAR|Strummed instrument
GULF|Large bay
GUM|Chewy candy
GUN|Firearm
GURU|Spiritual teacher
GUT|Belly
GUY|Fellow
GYM|Workout place
HABIT|Regular practice
HAIL|Frozen rain
HAIR|Head covering
HALF|One of two parts
HALL|Corridor
HALT|Stop
HAM|Pork cut
HAMMER|Nail driver
HAND|Has five fingers
HANDLE|Grip
HANDSOME|Good-looking
HAPPEN|Occur
HAPPY|Joyful
HARBOR|Port
HARD|Not soft
HARM|Hurt
HARP|Plucked string instrument
HARSH|Severe
HAS|Owns
HASTE|Hurry
HAT|Worn on the head
HATCH|Break out of an egg
HATE|Dislike strongly
HAVE|Own
HAVEN|Safe place
HAWK|Bird of prey
HAY|Cattle feed
HEAD|Where the brain is
HEAL|Get better
HEALTH|Wellness
HEAP|Pile
HEAR|Listen
HEART|Pumping organ
HEAT|Warmth
HEAVY|Weighty
HEDGE|Garden barrier
HEEL|Back of the foot
HEIGHT|How tall
HELD|Gripped
HELMET|Head protector
HELP|Assist
HEN|Egg-laying bird
HENCE|Therefore
HER|Belonging to a woman
HERB|Basil, for one
HERD|Cattle group
HERE|At this spot
HERO|Brave figure
HI|Friendly greeting
HIDDEN|Concealed
HIDE|Conceal
HIGH|Tall
HIKE|Long walk
HILL|Small mountain
HIM|That man
HINT|Subtle clue
HIP|Where the leg joins the body
HIRE|Employ
HIS|Belonging to a man
HIT|Strike
HOBBY|Pastime
HOLD|Grasp
HOLE|Gap
HOLIDAY|Day off
HOLY|Sacred
HOME|Where you live
HONEST|Truthful
HONEY|Bee product
HONOR|Respect
HOOD|Jacket cover
HOOK|Fishing tool
HOP|Jump on one foot
HOPE|Wish
HORN|Car sounder
HORSE|Riding animal
HOSE|Garden water pipe
HOSPITAL|Medical center
HOST|Party giver
HOT|Not cold
HOTEL|Place to stay
HOUR|Sixty minutes
HOUSE|Home
HOW|Question of method
HUB|Center of activity
HUG|Warm embrace
HUGE|Enormous
HUM|Sing with closed lips
HUMAN|Person
HUMOR|Comedy
HUNGRY|Wanting food
HUNT|Chase game
HUNTER|Game chaser
HURRY|Rush
HURT|Pain
HUSBAND|Married man
HUT|Small shack
HYMN|Church song
ICE|Frozen water
ICY|Very cold
IDEA|Thought
IDEAL|Perfect
IDLE|Not working
IF|Conditional word
ILL|Sick
IMAGE|Picture
IMAGINE|Picture in the mind
IN|Inside
INCH|Twelfth of a foot
INDEX|Book list
INK|Pen filler
INNER|Inside
INPUT|Data entered
INSTANT|Immediate
INSTRUCT|Teach
INTO|Entering
IRE|Anger
IRON|Metal for pressing
IRONY|Opposite meaning
IS|Form of be
ISLAND|Land in the sea
ISLANDER|Lives on a small land
ISSUE|Topic
IT|Neutral pronoun
ITEM|Single thing
IVORY|Elephant tusk material
IVY|Climbing plant
JACKET|Short coat
JAIL|Prison
JAM|Spread on toast
JAR|Glass container
JAW|Chewing bone
JAZZ|Improvised music
JEANS|Denim pants
JET|Fast plane
JEWEL|Gem
JEWELRY|Rings and necklaces
JOB|Occupation
JOG|Slow run
JOIN|Connect
JOINT|Elbow, for example
JOKE|Funny story
JOLLY|Cheerful
JOURNEY|Long trip
JOY|Great happiness
JUDGE|Court official
JUICE|Orange drink
JUMP|Leap
JUNGLE|Dense tropical forest
JURY|Court panel
JUST|Fair
KEEN|Eager
KEEP|Hold on to
KETTLE|Tea boiler
KEY|Opens a lock
KEYBOARD|Typing device
KICK|Strike with the foot
KID|Young goat
KILL|End life
KIN|Relatives
KIND|Gentle
KINDLY|With care
KING|Male ruler
KISS|Peck
KIT|Set of tools
KITCHEN|Cooking room
KITE|Flown on a windy day
KNEE|Leg joint
KNIFE|Cutting tool
KNIGHT|Armored rider
KNIT|Make with needles
KNOB|Door handle
KNOCK|Rap on a door
KNOT|Tied rope loop
KNOW|Be aware
LAB|Science room
LABEL|Tag
LABOR|Hard work
LACE|Delicate fabric
LACK|Shortage
LAD|Young man
LADDER|Climbing tool
LAID|Placed
LAKE|Inland water body
LAMP|Gives light
LANDMARK|Famous sight
LANGUAGE|Speech system
LAP|One round of a track
LARGE|Big
LASER|Concentrated beam
LATER|Afterward
LAUGH|Giggle
LAW|Rule of the land
LAWN|Mowed grass
LAWYER|Court advocate
LAY|Put down
LAYER|Level
LAZY|Idle
LEAD|Guide
LEADER|One in front
LEAF|Part of a tree
LEAGUE|Sports association
LEAN|Thin
LEAP|Big jump
LEARN|Study
LEASE|Rent
LEAST|Smallest amount
LEAVE|Depart
LED|Guided
LEFT|Not right
LEG|Limb for walking
LEGAL|Lawful
LEGEND|Famous tale
LEMON|Sour citrus
LEND|Loan
LESS|Not as much
LESSON|Class period
LET|Allow
LETTER|Alphabet character
LEVEL|Flat
LID|Pot cover
LIE|Untruth
LIFE|Existence
LIFT|Raise
LIGHT|Not heavy
LIKE|Enjoy
LIKELY|Probable
LIMB|Arm or leg
LIME|Green citrus
LIMIT|Boundary
LINE|Queue
LINEN|Flax fabric
LINK|Chain part
LION|King of beasts
LIP|Part of the mouth
LIQUID|Not solid
LIST|Shopping record
LISTEN|Pay attention to sounds
LITTLE|Small
LIVE|Exist
LIVER|Organ that filters blood
LIVING|Alive
LO|Behold
LOAD|Burden
LOAF|Bread unit
LOAN|Borrowed money
LOCAL|Nearby
LOCK|Door fastener
LOCKER|Gym storage
LODGE|Cabin
LOG|Piece of firewood
LOGIC|Reasoning
LONE|Solitary
LONG|Not short
LOOK|Glance
LOOP|Circle of rope
LOOSE|Not tight
LORD|Noble title
LOSE|Misplace
LOSS|Defeat
LOT|Plenty
LOUD|Noisy
LOVE|Deep affection
LOVELY|Beautiful
LOVER|Sweetheart
LOW|Not high
LOWER|Beneath
LUCK|Fortune
LUCKY|Fortunate
LUNCH|Midday meal
LUNG|Breathing organ
MACHINE|Mechanical device
MAD|Angry
MADE|Created
MAGAZINE|Glossy periodical
MAGIC|Wizardry
MAID|Hotel cleaner
MAIL|Post
MAIN|Principal
MAJOR|Important
MAKE|Create
MAKER|Creator
MAKING|Creating
MALE|Man or boy
MALL|Shopping center
MAMMAL|Warm-blooded animal
MAN|Adult male
MANAGE|Handle
MANOR|Large country house
MANY|A lot
MAP|Shows the way
MAPLE|Syrup tree
MARBLE|Smooth stone
MARCH|Walk in step
MARK|Stain
MARKET|Place to buy
MASK|Face cover
MASS|Large amount
MASTER|Expert
MAT|Doormat
MATCH|Fire starter
MATE|Partner
MATTER|Issue
MAY|Month after April
MAYBE|Perhaps
MAYOR|City leader
MAZE|Puzzle of paths
ME|Myself
MEADOW|Grassy field
MEAL|Lunch or dinner
MEAN|Unkind
MEAT|Animal flesh
MEDAL|Winner's award
MEDIA|News outlets
MEET|Come together
MELT|Turn to liquid
MEMBER|Part of a group
MEMO|Office note
MEMORY|Recall
MEN|Adult males
MENU|Restaurant list
MERCY|Compassion
MERE|Nothing more than
MERELY|Only
MERIT|Worth
MESS|Untidy state
MET|Encountered
METAL|Iron or tin
METER|Measuring unit
METHOD|Procedure
MIDDLE|Center
MIDNIGHT|Twelve at night
MIGHT|Power
MILD|Gentle
MILE|Distance unit
MILK|White drink
MIND|Brain
MINE|Underground pit
MINOR|Not major
MINT|Cool flavor
MINUS|Less
MINUTE|Sixty seconds
MIRROR|Reflective glass
MISS|Fail to hit
MIST|Light fog
MIX|Stir together
MIXED|Blended
MOAN|Groan
MODE|Method
MODEL|Runway figure
MOM|Mother
MOMENT|Instant
MONEY|Cash
MONKEY|Banana lover
MONTH|Four weeks or so
MOOD|Feeling
MOON|Night light in the sky
MORAL|Ethical
MOSS|Green forest carpet
MOST|The majority
MOTH|Night flier
MOTHER|Mom
MOTION|Movement
MOTOR|Engine
MOUNT|Climb up
MOUNTAIN|High peak
MOUSE|Cheese lover
MOUTH|Speaking opening
MOVE|Shift
MOVIE|Film
MUCH|A lot of
MUD|Wet soil
MULE|Donkey cross
MUSCLE|Flexed tissue
MUSEUM|Art gallery
MUSIC|Organized sound
MUST|Have to
MY|Belonging to me
MYTH|Ancient story
NA|Not so, informally
NAIL|Hammered into wood
NAKED|Unclothed
NAME|Title
NAP|Short sleep
NASTY|Mean
NATION|Country
NATURE|The outdoors
NAVY|Dark blue
NEAR|Close
NEARLY|Almost
NEAT|Tidy
NECK|Head support
NECKLACE|Jewelry for the throat
NEED|Require
NEEDLE|Sewing tool
NEIGHBOR|Person next door
NEPHEW|Sibling's son
NERVE|Sensing fiber
NEST|Bird's home
NET|Fisher's tool
NEVER|Not ever
NEW|Not old
NEWS|Current events
NEXT|Following
NICE|Pleasant
NIGHT|Dark time
NIL|Zero
NINE|One less than ten
NO|Opposite of yes
NOBLE|Of high rank
NOBODY|No one
NOD|Head bob
NOISE|Loud sound
NONE|Not any
NOON|Midday
NOR|Neither's partner
NORM|Standard
NORMAL|Usual
NORTH|Compass direction
NOSE|Used for smelling
NOT|Negative word
NOTE|Brief message
NOTEBOOK|School pad
NOTICE|See
NOVEL|Long story book
NOVEMBER|Eleventh month
NOW|At this moment
NUMBER|Digit
NUN|Convent resident
NURSE|Hospital caretaker
NUT|Squirrel's snack
OAK|Mighty tree
OAR|Rowing tool
OBEY|Follow orders
OBJECT|Thing
OCCUR|Happen
OCEAN|Vast sea
OCTOBER|Tenth month
ODD|Not even
ODOR|Smell
OF|Belonging to
OFF|Not on
OFFER|Present
OFFICE|Workplace
OFTEN|Frequently
OH|Sound of surprise
OIL|Olive product
OKAY|All right
OLD|Not young
OLIVE|Martini fruit
ON|Opposite of off
ONCE|One time
ONE|First number
ONION|Layered bulb
ONLY|Solely
ONTO|Upon
OPEN|Not closed
OPERA|Sung drama
OPPOSITE|Reverse
OPT|Choose
OR|Alternative word
ORAL|Spoken
ORANGE|Citrus fruit
ORB|Sphere
ORBIT|Path around a planet
ORDER|Command
ORE|Mined rock
ORIGIN|Beginning
OTHER|Different
OUR|Belonging to us
OUT|Not in
OUTDOORS|Outside
OUTER|External
OVAL|Egg shape
OVEN|Baking appliance
OVER|Above
OWE|Be in debt
OWL|Night bird
OWNER|Proprietor
OX|Plow animal
OXYGEN|We breathe it
PACE|Speed
PACK|Bundle
PACKET|Small bundle
PAD|Notebook
PAGE|Book leaf
PAID|Settled up
PAIN|Ache
PAINT|Wall color
PAINTING|Canvas art
PAIR|Two
PAL|Friend
PALACE|Royal home
PALE|Light in color
PALM|Hand's center
PAN|Frying vessel
PANCAKE|Flat breakfast item
PANEL|Flat board
PANT|Gasp
PAPER|Writing sheet
PARADE|Street march
PARENT|Mom or dad
PARK|Green space in town
PARROT|Talking bird
PART|Piece
PARTY|Celebration
PASS|Go by
PASSPORT|Travel document
PAST|Previous
PASTA|Spaghetti
PASTRY|Baked dough treat
PAT|Gentle tap
PATCH|Fix a hole
PATH|Trail
PATROL|Guard round
PAUSE|Brief stop
PAW|Cat's foot
PAY|Wages
PEA|Green sphere in a pod
PEACE|Calm
PEACH|Fuzzy fruit
PEAK|Summit
PEAR|Fruit with a narrow top
PEARL|Oyster gem
PEBBLE|Small stone
PEEL|Remove the skin
PEEP|Quick look
PEG|Hat hook
PEN|Writing tool
PENGUIN|Flightless polar bird
PEOPLE|Humans
PEPPER|Spicy seasoning
PERFUME|Fragrance
PERIOD|Full stop
PERK|Job benefit
PERSON|Individual
PET|Home animal
PHASE|Stage
PHONE|Calling device
PHOTO|Picture
PHYSICAL|Of the body
PI|About 3.14
PIANO|Keyed instrument
PICK|Choose
PICKLE|Cucumber in brine
PICNIC|Meal outdoors
PIE|Dessert with crust
PIECE|Part
PIER|Dock
PIG|Oinking animal
PILE|Heap
PILLOW|Head cushion
PILOT|Plane driver
PINE|Evergreen tree
PINK|Light red
PIPE|Water conduit
PIT|Hole in the ground
PITCH|Throw
PIZZA|Cheesy pie
PLACE|Put
PLAIN|Simple
PLAN|Scheme
PLANE|Flying machine
PLANET|Mars or Venus
PLANT|Green organism
PLASTIC|Synthetic material
PLATE|Dinner dish
PLATFORM|Raised stage
PLAY|Have fun
PLAZA|Public square
PLEA|Urgent request
PLEASANT|Nice
PLENTY|Lots
PLOT|Story line
PLUG|Socket fitting
PLUS|Added
PLY|Layer of wood
POCKET|Pants pouch
POD|Pea case
POEM|Verse
POET|Verse writer
POETRY|Verse
POINT|Sharp tip
POLE|Long stick
POLICE|Law enforcers
POLITE|Well mannered
POLL|Survey
POND|Small lake
PONY|Small horse
POOL|Swimming place
POOR|Not rich
POP|Fizzy drink
POPULAR|Well liked
PORK|Pig meat
PORT|Harbor
POSE|Strike a stance
POST|Pole
POT|Cooking vessel
POTATO|Spud
POUND|Strike hard
POUR|Tip out
POWDER|Fine dust
POWER|Strength
PRAY|Talk to God
PRESENT|Gift
PRESS|Squeeze
PRETTY|Attractive
PREY|Hunted animal
PRICE|Cost
PRIDE|Self-respect
PRIME|Top quality
PRINCE|King's son
PRINCESS|King's daughter
PRINT|Publish
PRIOR|Earlier
PRISON|Jail
PRIZE|Award
PRO|Expert
PROBLEM|Puzzle to solve
PROGRAM|TV show
PROMISE|Vow
PROOF|Evidence
PROTECT|Guard
PROUD|Pleased with oneself
PROVE|Demonstrate
PUB|Place for beer
PUBLIC|Open to all
PUDDLE|Rain pool
PULL|Tug
PUMP|Inflation device
PUN|Play on words
PUPIL|Student
PUPPET|String toy
PURE|Unmixed
PURPLE|Grape color
PURSE|Small handbag
PUSH|Shove
PUT|Place
PUZZLE|Brain teaser
QUA|As a, in Latin
QUEEN|Female ruler
QUEST|Search
QUESTION|Query
QUICK|Fast
QUIET|Silent
QUIT|Give up
QUITE|Fairly
QUIZ|Short test
QUOTE|Repeat words
RABBIT|Hopping pet
RACE|Speed contest
RACK|Shelf frame
RACKET|Tennis gear
RADIO|Broadcast receiver
RAFT|Simple boat
RAG|Old cloth
RAGE|Fury
RAID|Surprise attack
RAIL|Train track
RAIN|Falls from clouds
RAINBOW|Arc after a storm
RAISE|Lift
RAISIN|Dried grape
RALLY|Gathering
RAM|Male sheep
RAN|Sprinted
RANCH|Cattle farm
RANDOM|Chosen by chance
RANGE|Spread
RANK|Position
RAP|Spoken-word music
RAPID|Fast
RARE|Uncommon
RAT|Rodent
RATE|Speed or price
RATHER|Somewhat
RATIO|Proportion
RAW|Uncooked
RAY|Beam of light
REACH|Stretch out
REACT|Respond
READ|Look at words
READER|Book fan
READY|Prepared
REAL|Genuine
REALLY|Truly
REALM|Kingdom
REAR|Back
REASON|Cause
REBEL|Defy authority
RECENT|Not long ago
RECORD|Vinyl disc
RED|Color of a rose
REFER|Mention
REGION|Area
REGULAR|Usual
RELAX|Unwind
RELY|Depend
REMAIN|Stay
REMOTE|Far away
RENT|Lease
REPAIR|Fix
REPLY|Answer
REPORT|News story
RESCUE|Save
RESPECT|Esteem
REST|Relax
RESULT|Outcome
RETURN|Come back
REWARD|Prize
RHYTHM|Beat
RIB|Part of the chest
RIBBON|Gift tie
RICE|Paddy grain
RICH|Wealthy
RICHES|Wealth
RID|Free from
RIDE|Go on a horse
RIDER|Horse mounter
RIDGE|Mountain crest
RIFLE|Long gun
RIFT|Split
RIGHT|Correct
RIGID|Stiff
RIM|Edge
RING|Worn on a finger
RISE|Go up
RISK|Danger
RIVAL|Competitor
RIVER|Flowing water
ROAD|Cars drive on it
ROAR|Lion's sound
ROAST|Cook in an oven
ROB|Steal from
ROBE|Bathing wrap
ROBOT|Mechanical man
ROCK|Hard stone
ROCKET|Space vehicle
ROCKY|Full of stones
ROD|Fishing pole
ROLE|Part in a play
ROLL|Bun
ROMAN|Ancient Italian
ROOF|Top of a house
ROOM|Space in a house
ROOT|Underground plant part
ROPE|Thick cord
ROSE|Thorny flower
ROT|Decay
ROUGH|Bumpy
ROUND|Circular
ROUTE|Path
ROW|Line of seats
ROYAL|Fit for a king
RUB|Massage
RUDE|Impolite
RUG|Floor covering
RULE|Regulation
RULER|Straight edge
RUN|Jog fast
RURAL|Countryside
RUSH|Hurry
RUST|Iron decay
RYE|Whiskey grain
SAC|Pouch in the body
SAD|Unhappy
SADDLE|Horse seat
SADLY|Unhappily
SAFE|Secure
SAFETY|Being secure
SAG|Droop
SAID|Spoke
SAIL|Boat cloth
SAILOR|Ship hand
SAINT|Holy person
SALAD|Leafy dish
SALARY|Pay
SALE|Discount event
SALON|Hair studio
SALT|Shaker content
SAME|Identical
SAMPLE|Taste test
SAND|Beach covering
SANDWICH|Lunch between bread
SANE|Rational
SANG|Performed a song
SAP|Tree fluid
SAT|Took a seat
SATURDAY|Day before Sunday
SAUCE|Pasta topper
SAUSAGE|Link of meat
SAVE|Rescue
SAW|Cuts wood
SAY|Speak
SCALE|Fish covering
SCARE|Frighten
SCENE|Movie part
SCHOOL|Learning place
SCISSORS|Paper cutters
SCOPE|Range
SCORE|Game result
SCOUT|Look ahead
SCREEN|TV display
SEA|Salty water
SEAL|Marine mammal
SEARCH|Look for
SEASON|Spring or fall
SEAT|Chair
SECOND|After first
SECRET|Hidden thing
SECURE|Safe
SEE|Look
SEED|Planted to grow
SEEKER|Searcher
SELECT|Pick
SELF|One's own person
SELL|Trade for money
SEND|Dispatch
SENIOR|Older
SENSE|Smell or taste
SENT|Mailed
SERIES|Sequence
SERVE|Wait on
SERVER|Restaurant waiter
SET|Place
SETTLE|Calm down
SEVEN|Number after six
SEW|Stitch
SHADE|Cool spot under a tree
SHADOW|Dark shape
SHAKE|Tremble
SHALL|Will
SHAME|Disgrace
SHAPE|Form
SHARE|Split
SHARK|Toothy fish
SHARP|Pointed
SHE|That woman
SHED|Garden hut
SHEEP|Wool animal
SHEET|Bed cover
SHELF|Book holder
SHELL|Beach find
SHIELD|Protective plate
SHIFT|Move
SHINE|Glow
SHIP|Large boat
SHIRT|Top garment
SHOCK|Surprise
SHOE|Footwear
SHOOT|Fire a gun
SHOP|Store
SHOPPING|Buying things
SHORE|Coast
SHORT|Not long
SHOT|Attempt
SHOULDER|Arm joint
SHOUT|Yell
SHOW|Display
SHUT|Close
SHY|Timid
SICK|Unwell
SIDE|Edge
SIGH|Breath of relief
SIGHT|Vision
SIGN|Notice board
SIGNAL|Alert
SILENT|Quiet
SILK|Smooth fabric
SILLY|Foolish
SILVER|Shiny metal
SIMPLE|Easy
SINCE|From then
SING|Perform vocally
SINGER|Vocalist
SINK|Go under
SIP|Small drink
SIR|Polite male title
SISTER|Female sibling
SIT|Take a seat
SIX|Half a dozen
SIZE|Dimensions
SKELETON|Bones
SKETCH|Quick drawing
SKI|Snow gear
SKILL|Ability
SKIN|Body covering
SKIP|Jump over
SKIRT|Waist garment
SKY|Where clouds are
SLAM|Shut loudly
SLAVE|Forced laborer
SLEEP|Rest at night
SLEEVE|Arm part of a shirt
SLICE|Cut piece
SLID|Glided
SLIDE|Playground item
SLIM|Thin
SLIP|Lose footing
SLOPE|Incline
SLOT|Narrow opening
SLOW|Not fast
SMALL|Tiny
SMART|Clever
SMELL|Scent
SMILE|Happy expression
SMOKE|Fire cloud
SMOOTH|Even
SNAKE|Slithering reptile
SNAP|Break sharply
SNEEZE|Allergy reaction
SNOW|White winter flakes
SO|Therefore
SOAK|Drench
SOAP|Used in the bath
SOCCER|Game with goals
SOFA|Couch
SOFT|Not hard
SOIL|Earth
SOLAR|Of the sun
SOLD|Traded
SOLE|Only
SOLID|Firm
SOLVE|Figure out
SOME|A few
SOMEBODY|Some person
SONG|Tune with lyrics
SOON|Before long
""",
        """
SORE|Painful
SORRY|Apologetic
SORT|Kind
SOUL|Spirit
SOUND|Noise
SOUP|Served in a bowl
SOUTH|Opposite of north
SOY|Bean for tofu
SPA|Relaxation resort
SPACE|Outer region
SPARE|Extra
SPARK|Tiny fire bit
SPEAK|Talk
SPECIAL|Unusual
SPEED|Velocity
SPELL|Say letters
SPEND|Pay out
SPICE|Pepper, e.g.
SPIDER|Web builder
SPIN|Rotate
SPINE|Backbone
SPIRIT|Soul
SPLENDID|Magnificent
SPLIT|Divide
SPOON|Soup utensil
SPORT|Athletic game
SPOT|Mark
SPRAY|Fine mist
SPRING|Season after winter
SPY|Secret agent
SQUAD|Small team
SQUARE|Four-sided shape
STABLE|Horse home
STACK|Pile
STADIUM|Sports arena
STAFF|Workers
STAGE|Theater platform
STAIR|Step
STAKE|Pointed post
STAMP|Letter sticker
STAND|Rise
STAR|Twinkles at night
STARE|Look fixedly
START|Begin
STATE|Condition
STATUE|Sculpture
STAY|Remain
STEAK|Grilled beef
STEAL|Take illegally
STEAM|Water vapor
STEEL|Strong alloy
STEEP|Sharp incline
STEM|Flower support
STEP|Stair
STICK|Twig
STICKY|Gooey
STILL|Motionless
STIR|Mix with a spoon
STOCK|Supply
STONE|Pebble
STOOD|Was upright
STOP|Halt
STORE|Shop
STORM|Thunder event
STORY|Tale
STOVE|Cooker
STRANGER|Unknown person
STRAW|Drinking tube
STREAM|Brook
STREET|City road
STRESS|Pressure
STRICT|Severe
STRIP|Narrow band
STRIPE|Zebra feature
STRONG|Powerful
STUDENT|Learner
STUDIO|Artist's workroom
STUDY|Learn
STUFF|Things
STY|Pig pen
STYLE|Fashion
SUB|Long sandwich
SUBJECT|Topic
SUCH|Of that kind
SUDDEN|Unexpected
SUE|Take to court
SUGAR|Sweetener
SUIT|Business outfit
SUITE|Hotel rooms
SUM|Total
SUMMER|Hot season
SUN|Our daytime star
SUNNY|Bright
SUNSET|Evening glow
SUNSHINE|Bright daylight
SUPER|Great
SUPPLY|Provide
SURE|Certain
SURELY|Certainly
SURPRISE|Unexpected event
SWAM|Moved in water
SWAN|Graceful bird
SWAP|Trade
SWEAR|Curse
SWEAT|Perspiration
SWEATER|Knit pullover
SWEEP|Clean with a broom
SWEET|Sugary
SWIFT|Quick
SWIM|Move in water
SWING|Playground seat
SWITCH|Light control
SWORD|Knight's weapon
SYMBOL|Sign
TAB|Small flap
TABLE|Dinner furniture
TABLET|Pill
TACK|Small nail
TAG|Label
TAIL|Dog's wagger
TAKE|Grab
TALE|Story
TALENT|Gift
TALK|Speak
TALL|Not short
TAME|Not wild
TAN|Sun-browned
TANK|Large container
TAP|Faucet
TAPE|Sticky strip
TAR|Road surface
TASK|Chore
TASTE|Flavor
TAX|Government charge
TEA|British drink
TEACH|Instruct
TEACHER|Classroom instructor
TEAM|Group of players
TEAPOT|Brewing vessel
TEAR|Rip
TEE|Golf peg
TEEN|Adolescent
TEETH|Chewers
TELL|Inform
TEMPLE|House of worship
TEMPO|Speed of music
TEN|Number after nine
TEND|Look after
TENNIS|Racket sport
TENT|Camping shelter
TENTH|After ninth
TERM|Word or period
TERMS|Conditions
TEST|Exam
TEXT|Written words
THAN|Comparison word
THANK|Show gratitude
THAT|Pointed-out one
THE|Common article
THEFT|Stealing
THEIR|Belonging to them
THEM|Those people
THEME|Subject
THEN|At that time
THESE|Those near
THEY|Those people
THICK|Not thin
THIN|Slim
THING|Object
THINK|Ponder
THIRD|After second
THIRST|Need for water
THIS|The one here
THOSE|The ones there
THOUSAND|Ten hundred
THREAD|Sewing string
THREE|Number after two
THREW|Tossed
THRONE|King's seat
THROW|Toss
THUMB|Short finger
THURSDAY|Day after Wednesday
THUS|Therefore
THY|Old word for your
TICK|Clock sound
TICKET|Entry pass
TIDE|Ocean rise and fall
TIDY|Neat
TIE|Neck wear
TIGER|Striped cat
TIGHT|Not loose
TILE|Floor square
TILL|Until
TIMBER|Wood
TIME|Clock topic
TIMER|Countdown device
TIN|Metal can
TINY|Very small
TIP|Waiter's extra
TIRE|Wheel rubber
TIRED|Sleepy
TITLE|Name of a book
TO|Toward
TOAD|Warty amphibian
TODAY|This day
TOE|Foot digit
TOGETHER|With each other
TOKEN|Arcade coin
TOLD|Informed
TOLL|Bridge fee
TOMATO|Red salad fruit
TOMB|Burial place
TOMORROW|The next day
TONE|Sound quality
TONGUE|Taste organ
TOO|Also
TOOK|Grabbed
TOOL|Hammer, e.g.
TOOTH|Chewer
TOP|Opposite of bottom
TOPIC|Subject
TORCH|Flaming light
TORN|Ripped
TOTAL|Sum
TOUCH|Feel
TOUGH|Hard to chew
TOUR|Sightseeing trip
TOW|Pull a car
TOWARD|In the direction of
TOWEL|Drying cloth
TOWER|Tall structure
TOWN|Small city
TOXIC|Poisonous
TOY|Child's plaything
TRACE|Faint sign
TRACK|Race course
TRACTOR|Farm vehicle
TRADE|Exchange
TRAFFIC|Cars on the road
TRAIL|Hiking path
TRAIN|Locomotive
TRAIT|Characteristic
TRAP|Snare
TRAVEL|Journey
TRAY|Serving platter
TREASURE|Pirate's prize
TREAT|Special goody
TREATY|Peace agreement
TREE|Has branches and leaves
TREND|Fashion direction
TRIAL|Court case
TRIBE|Clan
TRICK|Prank
TRIED|Attempted
TRIM|Cut neatly
TRIO|Group of three
TRIP|Journey
TROOP|Soldier group
TROPICAL|Of the hot zone
TRUCK|Large vehicle
TRUE|Correct
TRULY|Honestly
TRUNK|Elephant's nose
TRUST|Faith
TRUTH|Fact
TRY|Attempt
TUB|Bath container
TUBE|Pipe
TUG|Pull hard
TULIP|Spring bulb
TUNE|Melody
TUNNEL|Underground passage
TURKEY|Thanksgiving bird
TURN|Rotate
TUTOR|Private teacher
TWELVE|Dozen
TWENTY|Two tens
TWICE|Two times
TWIN|One of a pair
TWIST|Turn
TWO|Number after one
TYPE|Kind
UGLY|Unattractive
UH|Pause sound
ULTRA|Extreme
UMBRELLA|Rain shield
UNCLE|Parent's brother
UNDER|Beneath
UNION|Joining
UNIT|Single item
UNITE|Join together
UNITY|Oneness
UNIVERSE|All of space
UNLESS|Except if
UNTIL|Up to the time
UP|Opposite of down
UPDATE|Make current
UPON|On top of
UPPER|Higher
UPSET|Distressed
URBAN|Of the city
URGE|Strong desire
URN|Vase for ashes
US|You and me
USAGE|Way of using
USE|Put to work
USED|Not new
USER|Operator
USUAL|Normal
VACATION|Time off
VAIN|Conceited
VALID|Legitimate
VALLEY|Between mountains
VALUE|Worth
VAN|Delivery vehicle
VARY|Change
VASE|Flower holder
VAST|Huge
VAT|Large tub
VEIL|Bride's cover
VEIN|Blood vessel
VELVET|Soft fabric
VERB|Action word
VERY|Extremely
VESSEL|Ship
VEST|Sleeveless layer
VET|Animal doctor
VIA|By way of
VICTIM|Harmed person
VIDEO|Moving pictures
VIEW|Scenery
VINE|Grape plant
VIOLIN|Fiddle
VIRUS|Cause of flu
VISA|Travel permit
VISIT|Call on
VITAL|Essential
VOCAL|Of the voice
VOICE|Speaking sound
VOID|Empty space
VOLCANO|Erupting mountain
VOLT|Electrical unit
VOLUME|Loudness
VOTE|Cast a ballot
VOW|Solemn promise
WAD|Bundle
WAG|Shake, like a tail
WAGE|Pay
WAGON|Cart
WAIT|Stay put
WAITER|Restaurant server
WAKE|Rouse
WALK|Stroll
WALL|Room divider
WALLET|Cash holder
WAND|Magician's stick
WANT|Desire
WAR|Armed conflict
WARD|Hospital section
WARM|Mildly hot
WARMTH|Gentle heat
WARN|Alert
WAS|Past of is
WASH|Clean
WASTE|Garbage
WATCH|Wrist timepiece
WATER|Clear drink
WATT|Power unit
WAVE|Ocean ripple
WAX|Candle material
WAY|Path
WE|You and I
WEAK|Not strong
WEALTH|Riches
WEAPON|Armament
WEAR|Have on
WEARY|Tired
WEB|Spider's creation
WED|Marry
WEED|Unwanted plant
WEEK|Seven days
WEEKEND|Saturday and Sunday
WEEKLY|Every seven days
WEIGH|Measure heaviness
WEIRD|Strange
WELL|Water source
WENT|Departed
WEPT|Cried
WERE|Past of are
WEST|Direction of sunset
WET|Not dry
WHALE|Giant sea mammal
WHAT|Question word
WHEAT|Bread grain
WHEEL|Round turner
WHEN|At what time
WHERE|Question of place
WHICH|What one
WHILE|During
WHISPER|Soft speech
WHISTLE|Referee's tool
WHITE|Snow color
WHO|Which person
WHOLE|Entire
WHOM|Objective who
WHOSE|Belonging to whom
WHY|Question of reason
WIDE|Broad
WIDEN|Broaden
WIFE|Married woman
WIG|Fake hair
WILD|Untamed
WILDLIFE|Animals in nature
WILL|Testament
WIN|Come first
WIND|Moving air
WINDOW|Glass opening
WINE|Grape drink
WING|Bird's limb
WINTER|Cold season
WIPE|Dry off
WIRE|Thin metal strand
WISDOM|Deep knowledge
WISE|Smart
WISH|Hope for
WIT|Clever humor
WITH|Along side
WITHIN|Inside
WIZARD|Spell caster
WOE|Great sorrow
WOKE|Stirred from sleep
WOLF|Howling predator
WOMAN|Adult female
WON|Came first
WONDER|Marvel
WOO|Court
WOOD|Material from trees
WOODEN|Made of timber
WORD|Unit of speech
WORE|Had on
WORK|Labor
WORKER|Employee
WORLD|Planet
WORM|Garden crawler
WORRY|Fret
WORSE|Less good
WORST|Least good
WORTH|Value
WOULD|Past of will
WOUND|Injury
WRAP|Cover with paper
WRIST|Hand joint
WRITE|Pen words
WRITER|Author
WRONG|Incorrect
YACHT|Luxury boat
YAK|Himalayan ox
YAM|Sweet potato
YAP|Bark shrilly
YARD|Three feet
YARN|Knitting thread
YAWN|Tired mouth opening
YEAR|Twelve months
YELL|Shout
YELLOW|Lemon color
YEN|Japanese money
YES|Opposite of no
YET|Up to now
YIELD|Give way
YO|Casual greeting
YOGA|Stretching practice
YOGHURT|Cultured milk
YOU|The listener
YOUNG|Not old
YOUR|Belonging to you
YOURSELF|You alone
YOUTH|Early life
ZAP|Hit with a jolt
ZEAL|Enthusiasm
ZEBRA|Striped equine
ZERO|Nothing
ZINC|Galvanizing metal
ZIP|Closes a jacket
ZIT|Skin blemish
ZONE|Area
ZOO|Place with animals
ZOOM|Move fast
""",
    )
}
