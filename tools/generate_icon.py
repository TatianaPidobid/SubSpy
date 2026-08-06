"""
Generates the SubSpy app icon: pure-black rounded background, light-green
spider web from the corners to the centre, a bright-green minimalist spider
with a white dollar sign in its body.

Outputs Android adaptive-icon layers (background + foreground) at every density
and a 512x512 Play-Store style preview.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFont

# Colours
BLACK = (0, 0, 0, 255)
WEB = (165, 214, 167, 255)        # Green80  #A5D6A7 (light green)
SPIDER = (0, 230, 118, 255)       # GreenAccent #00E676 (bright green)
SPIDER_DARK = (0, 200, 104, 255)  # slight shade for legs
WHITE = (255, 255, 255, 255)

SS = 4  # supersample factor

CENTER = (0.5, 0.5)


def _pt(cx, cy, ang_deg, r):
    a = math.radians(ang_deg)
    return (cx + r * math.cos(a), cy + r * math.sin(a))


def draw_web(d, S):
    cx, cy = 0.5 * S, 0.5 * S
    w = max(2, int(S * 0.006))
    # radial spokes (offset so 4 of them point at the corners)
    n = 12
    for k in range(n):
        ang = 15 + k * (360 / n)
        far = _pt(cx, cy, ang, S * 0.85)
        d.line([(cx, cy), far], fill=WEB, width=w)
    # concentric web rings connecting the spokes
    radii = [0.16, 0.26, 0.37, 0.49, 0.62, 0.76]
    for rr in radii:
        pts = [_pt(cx, cy, 15 + k * (360 / n), rr * S) for k in range(n)]
        pts.append(pts[0])
        d.line(pts, fill=WEB, width=max(1, int(S * 0.0045)), joint="curve")


def _bezier(p0, p1, p2, steps=24):
    out = []
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        out.append((x, y))
    return out


def draw_spider(d, S, scale=1.0):
    cx, cy = 0.5 * S, 0.53 * S
    rb = 0.145 * S * scale          # abdomen radius
    lw = max(3, int(S * 0.014 * scale))

    # legs: 4 per side, each an arched bezier (knee = raised elbow) -> foot
    leg_anchor_y = cy - 0.03 * S
    # (knee_x, knee_y, foot_x, foot_y) offsets in units of S, for the right side
    legs = [
        (0.20, -0.26, 0.33, -0.10),
        (0.28, -0.14, 0.44, -0.05),
        (0.29, 0.00, 0.46, 0.10),
        (0.24, 0.13, 0.38, 0.28),
    ]
    for sign in (-1, 1):
        for (kx, ky, fx, fy) in legs:
            start = (cx + sign * rb * 0.45, leg_anchor_y)
            knee = (cx + sign * kx * S, leg_anchor_y + ky * S)
            foot = (cx + sign * fx * S, leg_anchor_y + fy * S)
            pts = _bezier(start, knee, foot, steps=32)
            d.line(pts, fill=SPIDER, width=lw, joint="curve")
            # little foot dot
            fr = lw * 0.6
            d.ellipse([foot[0] - fr, foot[1] - fr, foot[0] + fr, foot[1] + fr], fill=SPIDER)

    # head (cephalothorax) above the abdomen
    hr = 0.075 * S * scale
    hx, hy = cx, cy - rb - hr * 0.55
    d.ellipse([hx - hr, hy - hr, hx + hr, hy + hr], fill=SPIDER)

    # abdomen (main round body)
    d.ellipse([cx - rb, cy - rb, cx + rb, cy + rb], fill=SPIDER)

    # white dollar sign inside the body
    try:
        font = ImageFont.truetype("arialbd.ttf", int(rb * 1.7))
    except Exception:
        font = ImageFont.truetype("arial.ttf", int(rb * 1.7))
    txt = "$"
    bbox = d.textbbox((0, 0), txt, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text((cx - tw / 2 - bbox[0], cy - th / 2 - bbox[1]), txt, font=font, fill=WHITE)


def render_background(px):
    S = px * SS
    img = Image.new("RGBA", (S, S), BLACK)
    d = ImageDraw.Draw(img)
    draw_web(d, S)
    return img.resize((px, px), Image.LANCZOS)


def render_foreground(px):
    S = px * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # foreground spider sits in the adaptive-icon safe zone -> keep it smaller
    draw_spider(d, S, scale=1.0)
    return img.resize((px, px), Image.LANCZOS)


def rounded_mask(px, radius_frac=0.22):
    m = Image.new("L", (px, px), 0)
    dd = ImageDraw.Draw(m)
    r = int(px * radius_frac)
    dd.rounded_rectangle([0, 0, px - 1, px - 1], radius=r, fill=255)
    return m


def render_preview(px=512):
    S = px * SS
    img = Image.new("RGBA", (S, S), BLACK)
    d = ImageDraw.Draw(img)
    draw_web(d, S)
    draw_spider(d, S, scale=1.0)
    img = img.resize((px, px), Image.LANCZOS)
    mask = rounded_mask(px)
    out = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main():
    base = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
    base = os.path.abspath(base)
    densities = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }
    for folder, px in densities.items():
        d = os.path.join(base, folder)
        os.makedirs(d, exist_ok=True)
        render_background(px).save(os.path.join(d, "ic_launcher_background.png"))
        render_foreground(px).save(os.path.join(d, "ic_launcher_foreground.png"))
        print("wrote", folder, px)

    out_dir = os.path.join(os.path.dirname(__file__), "..", "tools")
    render_preview(512).save(os.path.join(out_dir, "icon_preview.png"))
    print("wrote preview")


if __name__ == "__main__":
    main()
