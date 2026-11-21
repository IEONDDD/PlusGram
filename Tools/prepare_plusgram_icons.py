from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
RES = ROOT / "TMessagesProj/src/main/res"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
VARIANTS = {
    "classic": ("plusgram-icon-source.png", (130, 28, 40)),
    "light": ("plusgram-icon-light.png", (128, 24, 38)),
    "burgundy": ("plusgram-icon-burgundy.png", (255, 243, 231)),
    "pomegranate": ("plusgram-icon-pomegranate.png", (154, 28, 55)),
}

# The one-color mark is used by the launcher, Quick Settings and notifications.
MARK = (
    "M240,348 C240,288 288,240 348,240 H574 Q598,240 598,265 V293 "
    "Q598,318 574,318 H348 Q318,318 318,348 V676 Q318,706 348,706 H676 "
    "Q706,706 706,676 V450 Q706,426 731,426 H759 Q784,426 784,450 V676 "
    "C784,736 736,784 676,784 H348 C288,784 240,736 240,676 Z "
    "M731,162 H759 Q784,162 784,187 V240 H837 Q862,240 862,265 V293 "
    "Q862,318 837,318 H784 V371 Q784,396 759,396 H731 Q706,396 706,371 "
    "V318 H653 Q628,318 628,293 V265 Q628,240 653,240 H706 V187 Q706,162 731,162 Z "
    "M381,482 L644,388 C655,384 664,390 662,402 L610,626 "
    "C607,637 597,640 587,633 L521,590 L474,625 C466,631 457,626 459,617 "
    "L471,575 Q475,561 487,551 L575,469 L452,549 Q446,554 438,550 L378,522 "
    "C356,512 357,491 381,482 Z"
)


def mark_mask():
    # On the flat light artwork, the green channel is a direct measure of ink coverage.
    with Image.open(ASSETS / VARIANTS["light"][0]) as image:
        green = image.convert("RGB").getchannel("G")
        paper = green.getpixel((0, 0))
        ink = VARIANTS["light"][1][1]
        return green.point([max(0, min(255, round(255 * (paper - value) / (paper - ink))))
                            for value in range(256)])


def separate_background(art, mask):
    # Fill under each run of ink using its neighboring background colors.
    background = art.copy()
    source = art.load()
    pixels = background.load()
    coverage = mask.load()
    x_start, y_start, x_end, y_end = mask.getbbox()
    for y in range(y_start, y_end):
        x = x_start
        while x < x_end:
            if not coverage[x, y]:
                x += 1
                continue
            left = x - 1
            while x < x_end and coverage[x, y]:
                x += 1
            first, last = source[left, y], source[x, y]
            for offset in range(1, x - left):
                fraction = offset / (x - left)
                pixels[left + offset, y] = tuple(
                    round(start + (end - start) * fraction) for start, end in zip(first, last)
                )
    return background


def launcher_shape(image, circular):
    size = image.width
    mask = Image.new("L", (size * 4, size * 4))
    bounds = (0, 0, mask.width - 1, mask.height - 1)
    draw = ImageDraw.Draw(mask)
    if circular:
        draw.ellipse(bounds, fill=255)
    else:
        draw.rounded_rectangle(bounds, radius=round(mask.width * 0.23), fill=255)
    result = image.copy()
    result.putalpha(mask.resize(image.size, Image.Resampling.LANCZOS))
    return result


def adaptive_background(image, size):
    # Android draws the center 72 dp of each 108 dp layer.
    visible = size * 2 // 3
    inset = (size - visible) // 2
    center = image.resize((visible, visible), Image.Resampling.LANCZOS)
    strip = Image.new("RGB", (size, visible))
    strip.paste(center, (inset, 0))
    strip.paste(center.crop((0, 0, 1, visible)).resize((inset, visible)), (0, 0))
    strip.paste(center.crop((visible - 1, 0, visible, visible)).resize((inset, visible)),
                (inset + visible, 0))
    result = Image.new("RGB", (size, size))
    result.paste(strip, (0, inset))
    result.paste(strip.crop((0, 0, size, 1)).resize((size, inset)), (0, 0))
    result.paste(strip.crop((0, visible - 1, size, visible)).resize((size, inset)), (0, inset + visible))
    return result


def adaptive_foreground(mask, color, size):
    visible = size * 2 // 3
    inset = (size - visible) // 2
    alpha = Image.new("L", (size, size))
    alpha.paste(mask.resize((visible, visible), Image.Resampling.LANCZOS), (inset, inset))
    result = Image.new("RGBA", (size, size), color)
    result.putalpha(alpha)
    return result


def write_adaptive(name, layer):
    xml = f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/{layer}_background" />
    <foreground android:drawable="@drawable/{layer}_foreground" />
    <monochrome android:drawable="@drawable/plusgram_icon_monochrome" />
</adaptive-icon>
'''
    for suffix in ("", "_round"):
        (RES / "mipmap-anydpi-v26" / f"{name}{suffix}.xml").write_text(xml, encoding="utf-8")


def write_glyphs():
    for name, size, viewport, transform in (
        ("plusgram_icon_monochrome", 108, 1536, 'android:translateX="256" android:translateY="256"'),
        ("plusgram_icon_tile", 24, 1024, 'android:scaleX="1.25" android:scaleY="1.25" android:pivotX="512" android:pivotY="512"'),
        ("plusgram_notification", 24, 1024, 'android:scaleX="1.25" android:scaleY="1.25" android:pivotX="512" android:pivotY="512"'),
    ):
        xml = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{size}dp" android:height="{size}dp"
    android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">
    <group {transform}>
        <path android:fillColor="#FFFFFF" android:pathData="{MARK}" />
    </group>
</vector>
'''
        (RES / "drawable" / f"{name}.xml").write_text(xml, encoding="utf-8")


def generate_icons():
    mask = mark_mask()
    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(exist_ok=True)
    for variant, (filename, color) in VARIANTS.items():
        with Image.open(ASSETS / filename) as source:
            art = source.convert("RGB")
        classic = variant == "classic"
        name = "ic_launcher" if classic else f"ic_launcher_plusgram_{variant}"
        layer = "plusgram_icon" if classic else f"plusgram_icon_{variant}"
        adaptive_background(separate_background(art, mask), 432).save(nodpi / f"{layer}_background.png")
        if classic:
            launcher_shape(art.resize((512, 512), Image.Resampling.LANCZOS), False).save(ASSETS / "logo.png")
            write_adaptive("ic_launcher_exteraless", layer)
        else:
            adaptive_foreground(mask, color, 432).save(nodpi / f"{layer}_foreground.png")
        for density, scale in DENSITIES.items():
            mipmap = RES / f"mipmap-{density}"
            drawable = RES / f"drawable-{density}"
            size = round(48 * scale)
            icon = art.resize((size, size), Image.Resampling.LANCZOS)
            launcher_shape(icon, False).save(mipmap / f"{name}.png")
            launcher_shape(icon, True).save(mipmap / f"{name}_round.png")
            if classic:
                icon.save(drawable / "plusgram_icon.png")
                foreground = adaptive_foreground(mask, color, round(108 * scale))
                foreground.save(drawable / "plusgram_icon_foreground.png")
                foreground.save(mipmap / "ic_launcher_foreground.png")
        write_adaptive(name, layer)
    write_glyphs()


if __name__ == "__main__":
    generate_icons()
