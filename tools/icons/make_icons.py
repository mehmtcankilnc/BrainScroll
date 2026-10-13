"""Draws the BrainScroll mascot (docs/assets/logo-mascot-draft.svg) into the app icons.

Writes the iOS 1024 px icon, the Android legacy PNGs and the adaptive icon foreground (a vector drawable).
Run from the repository root: python tools/icons/make_icons.py
"""
from PIL import Image, ImageDraw
import os

BG = "#121212"
GREEN = "#7ED957"
DARK = "#4FA82E"
INK = "#10240A"
ORANGE = (255, 159, 67)

CIRCLES = [(40, 50, 18), (34, 70, 16), (52, 36, 16), (80, 50, 18), (86, 70, 16), (68, 36, 16), (50, 80, 15), (70, 80, 15), (60, 58, 24)]
# Quadratic strokes (start, control, end) and the one cubic of the stem.
QUADS = [((38, 50), (44, 45), (49, 51)), ((82, 50), (76, 45), (71, 51)), ((40, 72), (46, 67), (51, 73)), ((80, 72), (74, 67), (69, 73))]
SMILE = ((52, 77), (60, 86), (68, 77))
STEM = ((60, 34), (55, 42), (63, 48), (60, 54))


def quad(p0, p1, p2, n=24):
    pts = []
    for i in range(n + 1):
        t = i / n
        pts.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]))
    return pts


def cubic(p0, p1, p2, p3, n=32):
    pts = []
    for i in range(n + 1):
        t = i / n
        a, b, c, d = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t * t, t ** 3
        pts.append((a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1]))
    return pts


def mascot(draw, ox, oy, k):
    """Draws the mascot with raw svg coordinates mapped by x' = ox + x*k."""
    def P(p):
        return (ox + p[0] * k, oy + p[1] * k)

    def line(pts, color, width):
        pts = [P(p) for p in pts]
        w = width * k
        draw.line(pts, fill=color, width=max(1, round(w)), joint="curve")
        for x, y in (pts[0], pts[-1]):
            draw.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=color)

    def disc(cx, cy, r, color):
        x, y = P((cx, cy))
        r *= k
        draw.ellipse((x - r, y - r, x + r, y + r), fill=color)

    for cx, cy, r in CIRCLES:
        disc(cx, cy, r, GREEN)
    for q in QUADS:
        line(quad(*q), DARK, 3.5)
    line(cubic(*STEM), DARK, 4)
    disc(48, 62, 8, "#ffffff")
    disc(72, 62, 8, "#ffffff")
    disc(49.5, 63.5, 4, BG)
    disc(73.5, 63.5, 4, BG)
    line(quad(*SMILE), INK, 3.5)
    disc(40, 73, 5, ORANGE)
    disc(80, 73, 5, ORANGE)


def render(size, fraction=0.78, rounded=False, circle=False):
    """A square icon: the mascot (about [fraction] of the width) on the dark ground."""
    ss = 4
    big = size * ss
    img = Image.new("RGBA", (big, big), BG)
    d = ImageDraw.Draw(img)
    k = big * fraction / 80.0  # the mascot is about 80 raw units wide
    ox = big / 2 - 60 * k
    oy = big / 2 - 57 * k
    mascot(d, ox, oy, k)
    if rounded or circle:
        mask = Image.new("L", (big, big), 0)
        md = ImageDraw.Draw(mask)
        if circle:
            md.ellipse((0, 0, big - 1, big - 1), fill=255)
        else:
            md.rounded_rectangle((0, 0, big - 1, big - 1), radius=big * 0.2, fill=255)
        img.putalpha(mask)
    return img.resize((size, size), Image.LANCZOS)


def vector_foreground():
    k = 0.70
    ox, oy = 54 - 60 * k, 54 - 57 * k

    def fmt(v):
        return f"{v:.2f}".rstrip("0").rstrip(".")

    def circle(cx, cy, r, color):
        return (f'    <path android:fillColor="{color}" android:pathData="M{fmt(cx - r)},{fmt(cy)}a{fmt(r)},{fmt(r)} 0 1,0 {fmt(2 * r)},0a{fmt(r)},{fmt(r)} 0 1,0 {fmt(-2 * r)},0z" />')

    def stroke(path, color, width):
        return (f'    <path android:pathData="{path}" android:strokeColor="{color}" android:strokeWidth="{width}" '
                f'android:strokeLineCap="round" android:fillColor="#00000000" />')

    lines = []
    for cx, cy, r in CIRCLES:
        lines.append(circle(cx, cy, r, GREEN))
    for (a, b, c) in QUADS:
        lines.append(stroke(f"M{a[0]},{a[1]} Q{b[0]},{b[1]} {c[0]},{c[1]}", DARK, 3.5))
    a, b, c, e = STEM
    lines.append(stroke(f"M{a[0]},{a[1]} C{b[0]},{b[1]} {c[0]},{c[1]} {e[0]},{e[1]}", DARK, 4))
    lines.append(circle(48, 62, 8, "#FFFFFF"))
    lines.append(circle(72, 62, 8, "#FFFFFF"))
    lines.append(circle(49.5, 63.5, 4, BG))
    lines.append(circle(73.5, 63.5, 4, BG))
    a, b, c = SMILE
    lines.append(stroke(f"M{a[0]},{a[1]} Q{b[0]},{b[1]} {c[0]},{c[1]}", INK, 3.5))
    lines.append(circle(40, 73, 5, "#8CFF9F43"))
    lines.append(circle(80, 73, 5, "#8CFF9F43"))
    body = "\n".join(lines)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            f'  <group android:translateX="{fmt(ox)}" android:translateY="{fmt(oy)}" android:scaleX="{k}" android:scaleY="{k}">\n'
            f'{body}\n  </group>\n</vector>\n')


def main():
    res = "androidApp/src/main/res"
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in sizes.items():
        render(px, 0.74, rounded=True).save(f"{res}/mipmap-{name}/ic_launcher.png")
        render(px, 0.70, circle=True).save(f"{res}/mipmap-{name}/ic_launcher_round.png")
    with open(f"{res}/drawable-v24/ic_launcher_foreground.xml", "w", encoding="utf-8", newline="\n") as f:
        f.write(vector_foreground())
    with open(f"{res}/drawable/ic_launcher_background.xml", "w", encoding="utf-8", newline="\n") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
                f'    <path android:fillColor="{BG}" android:pathData="M0,0h108v108h-108z" />\n</vector>\n')
    # iOS wants a square without rounded corners or transparency: the system rounds it.
    render(1024, 0.74).convert("RGB").save("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png")


if __name__ == "__main__":
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
    main()
