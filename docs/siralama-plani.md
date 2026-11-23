# Akış oyunlarının sıralaması

Karar (2026-11-24): bütün akış oyunlarının kendi sıralama tablosu olur. Önceden yalnız günlük bulmaca (Tek Çizgi) tabloya girebiliyordu; akıştaki oyunlar "bu tabloda henüz kimse yok" diyordu.

## Nasıl çalışıyor
- **Yeni gönderim yok.** Akışta biten her oyun zaten buluta yedekleniyor (`game_result`: oyun, süre, seviye, yardım kullanıldı mı, tur/hamle). Tablo bu satırları okuyan yeni sunucu işlevi: `get_game_leaderboard` (`supabase/migrations/20261124090000_endless_boards.sql`). Günlük Tek Çizgi eski işlevi (`get_daily_game_leaderboard`, yıldız sıralaması) kullanmaya devam eder.
- **Kim görünür:** Google ya da Apple ile giriş yapmış ve kullanıcı adı olan oyuncu (adı otomatik atanır). Anonim hesap tabloya bakabilir ama listede yer almaz. Sonuç, giriş yapıldıktan sonra yedeğe yazılınca görünür; giriş yapmadan oynanan sonuçlar da ilk senkronda yüklenir.
- **Her oyuncunun tablodaki satırı en iyi sonucudur.** Tablo en iyi 50 oyuncuyu ve (ilk 50'de değilse) oyuncunun kendi satırını gösterir; "Herkes / Arkadaşlar" filtresi ve "Bugün / Tüm zamanlar" seçimi her oyun için vardır.
- **Seviye başına ayrı tablo.** Kolay, Orta ve Zor ayrı sıralanır (kelime bulmacası ayrıca dil başına). Seviyesi olmayan oyunlar Orta tablosundadır.

## Neye göre sıralanır
| Tür | Oyunlar | Sıralama | Sayılan sonuç |
| --- | --- | --- | --- |
| Süre | seri ve kelime dışındaki bütün oyunlar | önce en kısa süre, eşitlikte en az hamle | yardım almadan kazanılan bulmaca |
| Tur | Flaş Izgara, Farklı Olanı Bul, Renk Dizisi, Küp Katlama | önce en çok geçilen tur, eşitlikte en kısa süre | yardım almadan oynanan seri (kaybedilen seri de ne kadar ilerlediğiyle sayılır, en az 1 tur) |
| Tahmin | Kelime bulmacası | önce en az tahmin, eşitlikte en kısa süre | yardım almadan kazanılan bulmaca |
| Yıldız | Tek Çizgi (günlük) | önce en çok yıldız, eşitlikte en kısa süre | günlük bulmaca |

Satırda büyük yazılan sayı sıralama ölçütüdür (süre, tur, tahmin), küçük yazılan eşitlik bozandır. Eski "1/6" gösterimi "1 tahmin" oldu: neyin altıda biri olduğu anlaşılmıyordu.

## Sınırlar ve bilinen riskler
- Sonuçları telefon gönderdiği için süre ve tur değiştirilmiş bir istemciyle uydurulabilir. Önlem: sunucu bir saniyeden kısa ya da bir günden uzun süreyi, yardım kullanılmış sonucu ve zorluk dışı bir değeri almaz. Gerçek koruma (sunucuda doğrulama) yok; bu bir arkadaş sıralaması düzeyinde kabul edildi. Bozuk bir sıralama görülürse oyun oyun doğrulama ayrı iş olarak istenir.
- Tablo tüm satırlar üzerinden hesaplanır; `game_result_board_idx` indeksi oyun + seviye + gün okumasını hızlandırır. Oyuncu sayısı büyürse tabloyu özetleyen bir tabloya taşımak gerekir.
- Yeni oyun eklerken: seri oyunuysa `RankKind.Rounds` listesine (`social/SocialApi.kt` içindeki `rankKind()`) ve SQL işlevindeki `series` listesine eklenir; başka bir şey gerekmez.

## Doğrulama
- `supabase/tests/verify_endless_boards.mjs` (PGlite, gerçek Postgres): süre, tur, kelime ve bellek sıralaması, dışarıda kalanlar (yardım, kaybedilen bulmaca, başka seviye, kullanıcı adsız ya da anonim oyuncu, imkânsız süre), bugün/tüm zamanlar, arkadaş filtresi, hatalı girişler.
- Uygulama tarafı: `SocialViewModelTest`, `SocialUiTest` (seri ve süre tablosu, seviye seçimi).
- **Sunucuya uygulanması kullanıcının işi:** migration `supabase db push` ile gönderilmeden uygulamadaki tablolar çalışmaz ("Tablo yüklenemedi" görünür).
