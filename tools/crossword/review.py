"""
Makes tools/crossword/review.html: one page to read the words and clues of the bank, change a clue, or mark a word for removal.
The changes are kept in the browser, and a button copies them as text (`TR|KELİME|yeni ipucu`, or `TR|KELİME|-` for a removal) to be
applied with tools/crossword/bank_apply.py.

Usage: python tools/crossword/review.py
"""
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent


def load(lang):
    rows = []
    for path in sorted((HERE / "bank").glob(lang + "_*.txt")):
        for line in path.read_text(encoding="utf-8").splitlines():
            if "|" in line:
                word, clue = (p.strip() for p in line.split("|", 1))
                rows.append([word, clue])
    rows.sort(key=lambda r: (len(r[0]), r[0]))
    return rows


PAGE = """<!doctype html>
<html lang="tr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Bulmaca ipuçları</title>
<style>
:root{--bg:#fff;--fg:#1c1c1e;--mut:#6b6b70;--line:#e2e2e6;--card:#f6f6f8;--acc:#2f6fed;--bad:#d4372c}
@media (prefers-color-scheme:dark){:root{--bg:#141416;--fg:#ececf0;--mut:#9a9aa2;--line:#2a2a2f;--card:#1d1d21;--acc:#6b9bff;--bad:#ff6b5e}}
body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.4 system-ui,sans-serif}
header{position:sticky;top:0;background:var(--bg);border-bottom:1px solid var(--line);padding:12px 16px;display:flex;flex-wrap:wrap;gap:8px;align-items:center;z-index:2}
header input,header select,header button{font:inherit;padding:6px 10px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg)}
header button.primary{background:var(--acc);color:#fff;border-color:var(--acc)}
#count{color:var(--mut);margin-left:auto}
table{width:100%;border-collapse:collapse}
td{padding:6px 16px;border-bottom:1px solid var(--line);vertical-align:middle}
td.w{font-weight:700;letter-spacing:.04em;white-space:nowrap;width:1%}
input.word{font:inherit;font-weight:700;letter-spacing:.04em;width:9ch;padding:5px 6px;border:1px solid transparent;border-radius:6px;background:transparent;color:var(--fg);text-transform:uppercase}
input.word:hover,input.word:focus{border-color:var(--line);background:var(--card)}
tr.renamed input.word{border-color:var(--acc)}
#add{display:flex;gap:8px;padding:10px 16px;border-bottom:1px solid var(--line);flex-wrap:wrap}
#add input{font:inherit;padding:6px 10px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg)}
#add input#aclue{flex:1;min-width:160px}
#add button{font:inherit;padding:6px 12px;border:1px solid var(--acc);background:var(--acc);color:#fff;border-radius:8px}
td.n{color:var(--mut);width:1%;text-align:right}
input.clue{width:100%;box-sizing:border-box;font:inherit;padding:5px 8px;border:1px solid transparent;border-radius:6px;background:transparent;color:var(--fg)}
input.clue:hover,input.clue:focus{border-color:var(--line);background:var(--card)}
tr.changed input.clue{border-color:var(--acc)}
tr.removed td{opacity:.55}tr.removed td.w{text-decoration:line-through}
td.x{width:1%;white-space:nowrap}
td.x button{font:inherit;border:1px solid var(--line);background:var(--card);color:var(--bad);border-radius:6px;padding:3px 8px;cursor:pointer}
.len{font-size:12px;color:var(--mut);padding:10px 16px 4px;background:var(--card)}
@media (max-width:600px){td{padding:6px 8px}}
</style></head><body>
<header>
<select id="lang"><option value="TR">Türkçe</option><option value="EN">English</option></select>
<select id="len"><option value="0">Tüm uzunluklar</option></select>
<input id="q" type="search" placeholder="Ara: kelime veya ipucu" size="22">
<label><input id="only" type="checkbox"> yalnız değişenler</label>
<button class="primary" id="copy">Değişiklikleri kopyala</button>
<button id="reset">Sıfırla</button>
<span id="count"></span>
</header>
<div id="add"><input id="aword" placeholder="Yeni kelime" size="10"><input id="aclue" placeholder="Yeni kelimenin ipucu (en çok 30 karakter)"><button id="addb">Ekle</button></div>
<div id="out" hidden style="padding:10px 16px;border-bottom:1px solid var(--line)"><div style="color:var(--mut);margin-bottom:6px">Bu metni kopyala (Ctrl+C) ve sohbete yapıştır, ya da dosya olarak indir.</div><textarea id="outt" rows="8" style="width:100%;box-sizing:border-box;font:13px/1.4 ui-monospace,monospace;padding:8px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg)" readonly></textarea><div style="margin-top:6px"><button id="dl" style="font:inherit;padding:6px 12px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg)">Dosya olarak indir</button> <button id="close" style="font:inherit;padding:6px 12px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg)">Kapat</button></div></div>
<table id="t"></table>
<script>
const DATA = __DATA__;
const PRESET = __PRESET__;
const KEY = "crossword-review-v1";
let edits = {}, renames = {}, adds = {};
// The changes are kept in the browser's storage and, where that is shut (a page opened from a data: address), in window.name,
// which survives a reload of the same tab.
let stored = null;
try { stored = localStorage.getItem(KEY); } catch (e) {}
if (!stored && window.name.startsWith("cw:")) stored = window.name.slice(3);
if (stored) { try { const st = JSON.parse(stored); edits = st.edits || {}; renames = st.renames || {}; adds = st.adds || {}; } catch (e) {} }
else {
  // First time: the changes that were written down but are not in the bank yet.
  for (const line of PRESET) {
    const [lang, word, ...rest] = line.split("|"), v = rest.join("|"), k = lang + "|" + word;
    if (v === "-") edits[k] = "-";
    else if (DATA[lang].some(r => r[0] === word)) edits[k] = v;
    else adds[k] = v;
  }
}
const $ = id => document.getElementById(id);
function save() {
  const text = JSON.stringify({edits, renames, adds});
  try { localStorage.setItem(KEY, text); } catch (e) {}
  window.name = "cw:" + text;
}
function up(t) { return t.trim().toLocaleUpperCase($("lang").value === "TR" ? "tr" : "en"); }
function total() { return Object.keys(edits).length + Object.keys(renames).length + Object.keys(adds).length; }
function lens() { const set = new Set(DATA[$("lang").value].map(r => r[0].length)); return [...set].sort((a, b) => a - b); }
function fillLens() { const cur = $("len").value; $("len").innerHTML = '<option value="0">Tüm uzunluklar</option>' + lens().map(n => `<option value="${n}">${n} harf</option>`).join(""); $("len").value = cur; if ($("len").value !== cur) $("len").value = "0"; }
function render() {
  const lang = $("lang").value, len = +$("len").value, q = $("q").value.trim().toLocaleLowerCase("tr"), only = $("only").checked;
  let html = "", shown = 0, last = 0;
  const addedHere = Object.entries(adds).filter(([k]) => k.startsWith(lang + "|"));
  if (addedHere.length && !len && !q && !only) {
    html += '<tr><td class="len" colspan="4">Eklenenler</td></tr>';
    for (const [k, clue] of addedHere) html += `<tr class="changed" data-k="${k}" data-add="1"><td class="w">${k.split("|")[1]}</td><td><input class="clue" value="${clue.replace(/"/g, "&quot;")}"></td><td class="n">${clue.length}</td><td class="x"><button>sil</button></td></tr>`;
  }
  for (const [word, clue] of DATA[lang]) {
    const k = lang + "|" + word, e = edits[k], rn = renames[k];
    if (len && word.length !== len) continue;
    if (q && !(word.toLocaleLowerCase("tr").includes(q) || (e && e !== "-" ? e : clue).toLocaleLowerCase("tr").includes(q))) continue;
    if (only && !e && !rn) continue;
    if (word.length !== last) { last = word.length; html += `<tr><td class="len" colspan="4">${last} harfli</td></tr>`; }
    const removed = e === "-", changed = e && !removed;
    const cur = changed ? e : clue;
    html += `<tr class="${removed ? "removed" : (changed || rn) ? "changed" : ""}${rn ? " renamed" : ""}" data-k="${k}"><td class="w"><input class="word" value="${rn || word}" ${removed ? "disabled" : ""}></td><td><input class="clue" value="${cur.replace(/"/g, "&quot;")}" ${removed ? "disabled" : ""}></td><td class="n">${cur.length}</td><td class="x"><button>${removed ? "geri al" : "çıkar"}</button></td></tr>`;
    shown++;
  }
  $("t").innerHTML = html;
  $("count").textContent = shown + " kelime · " + total() + " değişiklik";
}
$("t").addEventListener("change", ev => {
  const tr = ev.target.closest("tr"), k = tr.dataset.k, [lang, word] = k.split("|");
  if (tr.dataset.add) {
    if (ev.target.classList.contains("clue")) { const v = ev.target.value.trim(); if (v) adds[k] = v; }
    save(); render(); return;
  }
  if (ev.target.classList.contains("clue")) {
    const orig = DATA[lang].find(r => r[0] === word)[1], v = ev.target.value.trim();
    if (!v || v === orig) delete edits[k]; else edits[k] = v;
  } else if (ev.target.classList.contains("word")) {
    // A new answer: the old word goes and the new one is added with the clue as it stands now.
    const v = up(ev.target.value);
    if (!v || v === word) delete renames[k]; else renames[k] = v;
  }
  save(); render();
});
$("t").addEventListener("click", ev => {
  if (ev.target.tagName !== "BUTTON") return;
  const tr = ev.target.closest("tr"), k = tr.dataset.k;
  if (tr.dataset.add) delete adds[k];
  else if (edits[k] === "-") delete edits[k]; else edits[k] = "-";
  save(); render();
});
$("addb").onclick = () => {
  const w = up($("aword").value), c = $("aclue").value.trim();
  if (w.length < 2 || w.length > 8 || !c) { alert("Kelime 2-8 harf, ipucu dolu olmalı."); return; }
  adds[$("lang").value + "|" + w] = c; $("aword").value = ""; $("aclue").value = ""; save(); render();
};
["lang"].forEach(id => $(id).addEventListener("change", () => { fillLens(); render(); }));
["len", "q", "only"].forEach(id => $(id).addEventListener("input", render));
$("copy").onclick = async () => {
  const lines = [];
  for (const [k, v] of Object.entries(edits)) if (!renames[k]) lines.push(k + "|" + v);
  for (const [k, nw] of Object.entries(renames)) {
    const [lang, word] = k.split("|"), clue = edits[k] && edits[k] !== "-" ? edits[k] : DATA[lang].find(r => r[0] === word)[1];
    lines.push(k + "|-", lang + "|" + nw + "|" + clue);
  }
  for (const [k, v] of Object.entries(adds)) lines.push(k + "|" + v);
  const text = lines.join(String.fromCharCode(10));
  $("out").hidden = false; $("outt").value = text || "(değişiklik yok)"; $("outt").focus(); $("outt").select();
  try { document.execCommand("copy"); } catch (e) {}
  try { await navigator.clipboard.writeText(text); } catch (e) {}
  $("count").textContent = lines.length + " satır aşağıda";
  $("dl").onclick = () => { const a = document.createElement("a"); a.href = URL.createObjectURL(new Blob([text], {type: "text/plain;charset=utf-8"})); a.download = "degisiklikler.txt"; a.click(); };
  $("close").onclick = () => { $("out").hidden = true; render(); };
};
$("reset").onclick = () => { if (confirm("Tüm değişiklikler silinsin mi?")) { edits = {}; renames = {}; adds = {}; save(); render(); } };
save(); fillLens(); render();
</script></body></html>
"""

if __name__ == "__main__":
    data = {"TR": load("tr"), "EN": load("en")}
    # The changes written down in pending_changes*.txt that the bank does not have yet are put on the page as changes.
    preset = []
    for path in sorted(HERE.glob("pending_changes*.txt")):
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.count("|") < 2:
                continue
            lang, word, clue = line.split("|", 2)
            current = dict(data.get(lang, [])).get(word)
            if clue == "-" and current is not None:
                preset.append(line)
            elif clue != "-" and current != clue:
                preset.append(line)
    html = PAGE.replace("__DATA__", json.dumps(data, ensure_ascii=False)).replace("__PRESET__", json.dumps(preset, ensure_ascii=False))
    (HERE / "review.html").write_text(html, encoding="utf-8")
    print("changes put on the page:", len(preset))
    sys.stdout.reconfigure(encoding="utf-8")
    print("review.html:", {k: len(v) for k, v in data.items()})
