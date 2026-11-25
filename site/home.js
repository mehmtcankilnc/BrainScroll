/* BrainScroll home page: mascot, feed preview, live demos, reveal and the glow that follows the section in view. */
(function () {
  "use strict";
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  /* ---------- Reveal on scroll ---------- */
  var rv = $$(".rv");
  if ("IntersectionObserver" in window && !reduce) {
    var io = new IntersectionObserver(function (es) {
      es.forEach(function (e) { if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); } });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.08 });
    rv.forEach(function (n) { io.observe(n); });
  } else {
    rv.forEach(function (n) { n.classList.add("in"); });
  }

  /* ---------- The glow follows the section in view ---------- */
  var worlds = $$("[data-world]");
  if ("IntersectionObserver" in window && worlds.length) {
    var wio = new IntersectionObserver(function (es) {
      es.forEach(function (e) {
        if (e.isIntersecting) document.documentElement.style.setProperty("--world", e.target.getAttribute("data-world"));
      });
    }, { rootMargin: "-45% 0px -50% 0px", threshold: 0 });
    worlds.forEach(function (n) { wio.observe(n); });
  }

  /* ---------- Mascots: eyes follow the pointer, a tap changes the face ---------- */
  var mascots = $$(".mascot-svg");
  var faces = ["smile", "joy", "wow", "wink"];
  var pointer = null, raf = 0;
  function look() {
    raf = 0;
    if (!pointer) return;
    mascots.forEach(function (m) {
      var r = m.getBoundingClientRect();
      if (r.bottom < 0 || r.top > innerHeight) return;
      var cx = r.left + r.width / 2, cy = r.top + r.height * 0.52;
      var dx = pointer.x - cx, dy = pointer.y - cy;
      var d = Math.hypot(dx, dy) || 1;
      var k = Math.min(1, d / 260) * 3.2;
      var x = (dx / d) * k, y = (dy / d) * k;
      $$(".pupil", m).forEach(function (p) { p.style.transform = "translate(" + x.toFixed(2) + "px," + y.toFixed(2) + "px)"; });
    });
  }
  if (!reduce) {
    addEventListener("pointermove", function (e) {
      pointer = { x: e.clientX, y: e.clientY };
      if (!raf) raf = requestAnimationFrame(look);
    }, { passive: true });
  }
  mascots.forEach(function (m) {
    var wrap = m.closest(".mascot-wrap") || m.parentNode;
    var t = 0;
    function next() {
      t = (t + 1) % faces.length;
      m.setAttribute("data-face", faces[t]);
      if (!reduce) { wrap.classList.remove("hop"); void wrap.offsetWidth; wrap.classList.add("hop"); }
      clearTimeout(m._back);
      if (t !== 0) m._back = setTimeout(function () { t = 0; m.setAttribute("data-face", "smile"); }, 2600);
    }
    m.addEventListener("click", next);
    m.setAttribute("tabindex", "0");
    m.setAttribute("role", "button");
    m.addEventListener("keydown", function (e) { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); next(); } });
  });

  /* ---------- Feed preview in the phone ---------- */
  var feed = $("#feed");
  if (feed) {
    var track = $(".feed-track", feed), cards = $$(".feed-card", feed), dots = $$(".feed-dots button");
    var cur = 0, timer = 0, held = false;
    var worldOf = function (i) { return cards[i].style.getPropertyValue("--c"); };
    var go = function (i) {
      cur = (i + cards.length) % cards.length;
      track.style.transform = "translateY(" + (-100 * cur) + "%)";
      dots.forEach(function (d, k) { d.setAttribute("aria-current", k === cur ? "true" : "false"); });
      var phone = feed.closest(".phone");
      if (phone) phone.style.setProperty("--world", worldOf(cur));
    };
    var play = function () {
      clearInterval(timer);
      if (reduce || held) return;
      timer = setInterval(function () { go(cur + 1); }, 3200);
    };
    dots.forEach(function (d, k) { d.addEventListener("click", function () { go(k); play(); }); });
    feed.addEventListener("click", function () { go(cur + 1); play(); });
    var ph = feed.closest(".phone");
    ph.addEventListener("pointerenter", function () { held = true; clearInterval(timer); });
    ph.addEventListener("pointerleave", function () { held = false; play(); });
    document.addEventListener("visibilitychange", function () { if (document.hidden) clearInterval(timer); else play(); });
    go(0); play();
  }

  /* ---------- Memory match demo ---------- */
  var mem = $("#memory");
  if (mem) {
    var grid = $(".mem-grid", mem), movesEl = $(".demo-stat", mem), msg = $(".demo-msg", mem), again = $(".btn-ghost", mem);
    var shapes = ["circle", "square", "triangle", "diamond", "star", "plus"];
    var label = mem.getAttribute("data-card");
    var openCards = [], moves = 0, matched = 0, lock = false;
    var shuffle = function (a) { for (var i = a.length - 1; i > 0; i--) { var j = Math.floor(Math.random() * (i + 1)); var t = a[i]; a[i] = a[j]; a[j] = t; } return a; };
    var build = function () {
      grid.innerHTML = ""; moves = 0; matched = 0; openCards = []; lock = false;
      movesEl.textContent = "0"; msg.textContent = "";
      shuffle(shapes.concat(shapes)).forEach(function (s, i) {
        var b = document.createElement("button");
        b.type = "button"; b.className = "mem-card"; b.setAttribute("data-s", s);
        b.setAttribute("aria-label", label + " " + (i + 1));
        b.innerHTML = '<span class="mem-in"><span class="mem-back"></span><span class="mem-face"><svg viewBox="0 0 24 24" aria-hidden="true"><use href="#sh-' + s + '"/></svg></span></span>';
        b.addEventListener("click", function () { flip(b); });
        grid.appendChild(b);
      });
    };
    var flip = function (b) {
      if (lock || b.classList.contains("open") || b.classList.contains("done")) return;
      b.classList.add("open"); openCards.push(b);
      if (openCards.length < 2) return;
      moves++; movesEl.textContent = String(moves); lock = true;
      var a = openCards[0], c = openCards[1];
      if (a.getAttribute("data-s") === c.getAttribute("data-s")) {
        setTimeout(function () {
          [a, c].forEach(function (x) { x.classList.remove("open"); x.classList.add("done"); x.disabled = true; });
          openCards = []; lock = false; matched++;
          if (matched === shapes.length) msg.textContent = mem.getAttribute("data-win").replace("{n}", moves);
        }, 380);
      } else {
        setTimeout(function () { a.classList.add("shake"); c.classList.add("shake"); }, 350);
        setTimeout(function () {
          [a, c].forEach(function (x) { x.classList.remove("open", "shake"); });
          openCards = []; lock = false;
        }, 950);
      }
    };
    again.addEventListener("click", build);
    build();
  }

  /* ---------- Colour tubes demo ---------- */
  var tb = $("#tubes");
  if (tb) {
    var host = $(".tubes", tb), tStat = $(".demo-stat", tb), tMsg = $(".demo-msg", tb), tAgain = $(".btn-ghost", tb);
    var colors = [["#4db8ff", "o"], ["#ffc93c", "t"], ["#e0568f", "s"], ["#7ed957", "d"]];
    var CAP = 4, tubes = [], sel = -1, tMoves = 0, won = false;
    var shuf = function (a) { for (var i = a.length - 1; i > 0; i--) { var j = Math.floor(Math.random() * (i + 1)); var t = a[i]; a[i] = a[j]; a[j] = t; } return a; };
    var solvedTube = function (t) { return t.length === CAP && t.every(function (x) { return x === t[0]; }); };
    var newGame = function () {
      var balls = [];
      colors.forEach(function (c, i) { for (var k = 0; k < CAP; k++) balls.push(i); });
      var t;
      do {
        shuf(balls);
        t = [];
        for (var i = 0; i < colors.length; i++) t.push(balls.slice(i * CAP, (i + 1) * CAP));
      } while (t.some(solvedTube));
      t.push([]); t.push([]);
      tubes = t; sel = -1; tMoves = 0; won = false;
      tStat.textContent = "0"; tMsg.textContent = "";
      draw();
    };
    var draw = function () {
      host.innerHTML = "";
      tubes.forEach(function (t, i) {
        var el = document.createElement("button");
        el.type = "button";
        el.className = "tube" + (i === sel ? " sel" : "") + (solvedTube(t) ? " full" : "");
        el.setAttribute("aria-label", tb.getAttribute("data-tube") + " " + (i + 1));
        el.setAttribute("aria-pressed", i === sel ? "true" : "false");
        t.forEach(function (c) {
          var b = document.createElement("i");
          b.className = "ball"; b.style.setProperty("--b", colors[c][0]); b.setAttribute("data-s", colors[c][1]);
          el.appendChild(b);
        });
        el.addEventListener("click", function () { pick(i); });
        host.appendChild(el);
      });
    };
    var pick = function (i) {
      if (won) return;
      if (sel === -1) { if (tubes[i].length) { sel = i; draw(); } return; }
      if (sel === i) { sel = -1; draw(); return; }
      var from = tubes[sel], to = tubes[i];
      var top = from[from.length - 1];
      var ok = to.length < CAP && (to.length === 0 || to[to.length - 1] === top);
      if (!ok) { sel = tubes[i].length ? i : -1; draw(); return; }
      while (from.length && from[from.length - 1] === top && to.length < CAP) to.push(from.pop());
      sel = -1; tMoves++; tStat.textContent = String(tMoves);
      draw();
      if (tubes.every(function (t) { return t.length === 0 || solvedTube(t); })) {
        won = true; tMsg.textContent = tb.getAttribute("data-win").replace("{n}", tMoves);
      }
    };
    tAgain.addEventListener("click", newGame);
    newGame();
  }
})();
