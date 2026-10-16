# Bulmaca kelime incelemesi: durum notları

- TR 2, 3 ve 4 harfliler incelendi ve bankaya işlendi (2026-10-15).
- TR 5 harflilerde **kaldığımız yer: G harfi** (kullanıcı G'de kaldığını söyledi; G kelimelerine bakarken GAZOZ ... GİZLİ arası zaten işaretlenmiş görünür). Devam G harfinden yapılacak, H'den değil.
- Kullanıcı bazı H sonrası harflerde de (K, M, P, S, T, V, Y, Z, Ç, Ö) değişiklik yaptı; hepsi `pending_changes_5a.txt` içinde.
- `pending_changes_5a.txt`: ilk 102 satır bankaya **işlendi**. Sonraki 64 satır (GAZOZ'dan başlayan) **henüz işlenmedi**:
  `python tools/crossword/bank_apply.py tools/crossword/pending_changes_5a.txt --force` (işlenmiş olanlar "not found" der, zararsız)
- TR 6-8 harfliler ve EN bankası henüz incelenmedi.
- Bank değişince tahtalar yeniden üretilmeli (en son, tek seferde): `Gen.java make TR 125 570` (bkz. docs/yeni-oyunlar-plan.md bölüm 6). Şu an oyundaki tahtalar eski bankayla üretildi; silinen kelimeler bazı tahtalarda geçiyor, bank derlemesi (Gen.java bank) kelimeleri çıkardığı için o tahtalar yeniden üretilmeden oyuna alınmamalı.
- İnceleme sayfası: `python tools/crossword/review.py` ile yenilenir, ama yenileyince açık sayfadaki kayıtsız değişiklikler gider (sayfa `data:` adresinde, tarayıcı kaydetmiyor). Yenilemeden önce değişiklikleri txt'ye al.

- 2026-10-16: kullanıcının yapıştırdığı tam 5 harfli listesi (`pending_changes_5b.txt`, 5a'yı da kapsar) bankaya **işlendi** (169 ipucu, 97 kelime silindi). 4 ipucu uzunluk/ipucu-ele-verme kuralı için kısaltıldı: KABAK, KLİMA, YAKIT, ZIMBA. Banka: TR 1752 kelime (5 harfli 924). `pending_changes_5a.txt` artık gereksiz (5b içinde).
- 2026-10-16: TR 6 harflilerin ilk bölümü (`pending_changes_6_8.txt`: ADALET ... ŞİMŞEK, 33 satır) işlendi (25 ipucu, 9 kelime silindi). 6 harflilerde kaldığı yer belli değil (liste A'dan Ş'ye uzanıyor, ama sadece bir kısmı değiştirilmiş); 7 ve 8 harfliler ve EN henüz bakılmadı.
- 2026-10-16: TR 7-8 harfliler (`pending_changes_6_8b.txt` ve `6_8c.txt`) işlendi; sırada EN banka.
