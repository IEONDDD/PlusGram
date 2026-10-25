from pathlib import Path
import argparse
from PIL import Image, ImageDraw, ImageOps, ImageChops
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'TMessagesProj/src/main/res'


def remove_black_corners(image_rgb: Image.Image) -> Image.Image:
    """Removes outer black background from a rounded squircle image with smooth antialiasing."""
    arr = np.array(image_rgb.convert('RGB')).astype(float)
    lum = 0.299 * arr[:, :, 0] + 0.587 * arr[:, :, 1] + 0.114 * arr[:, :, 2]
    H, W = lum.shape

    alpha = np.ones((H, W), dtype=float) * 255.0

    # Corners region: x < 280 or x > W - 280, y < 280 or y > H - 280
    for y in range(H):
        for x in range(W):
            if (x < 280 or x > W - 280) and (y < 280 or y > H - 280):
                # Expected background color of the squircle in this corner
                if x < W / 2 and y < H / 2:
                    bg_lum = 198.0  # top-left pink
                elif x < W / 2 and y >= H / 2:
                    bg_lum = 220.0  # bottom-left yellow
                else:
                    bg_lum = 243.0  # right side cream

                val = lum[y, x] / bg_lum
                val = max(0.0, min(1.0, val))

                if val < 0.05:
                    alpha[y, x] = 0.0
                    arr[y, x] = [0.0, 0.0, 0.0]
                elif val > 0.95:
                    alpha[y, x] = 255.0
                else:
                    a = ((val - 0.05) / 0.90) * 255.0
                    alpha[y, x] = a
                    # Unpremultiply so there is no dark fringe at the border
                    a_frac = max(0.01, a / 255.0)
                    arr[y, x] = np.clip(arr[y, x] / a_frac, 0, 255)

    rgba = np.dstack([arr.astype(np.uint8), alpha.astype(np.uint8)])
    return Image.fromarray(rgba, 'RGBA')


def generate_icons(source_path: Path):
    source_raw = Image.open(source_path)
    # Remove black corners if present
    artwork = remove_black_corners(source_raw)

    square = ImageOps.contain(artwork, (1024, 1024), Image.Resampling.LANCZOS)
    canvas = Image.new('RGBA', (1024, 1024), (0, 0, 0, 0))
    canvas.alpha_composite(square, ((1024 - square.width) // 2, (1024 - square.height) // 2))

    # Save master assets
    canvas.save(ROOT / 'assets/plusgram-icon-source.png')
    canvas.resize((512, 512), Image.Resampling.LANCZOS).save(ROOT / 'assets/logo.png')

    for density, scale in [('mdpi', 1), ('hdpi', 1.5), ('xhdpi', 2), ('xxhdpi', 3), ('xxxhdpi', 4)]:
        mipmap = RES / f'mipmap-{density}'
        drawable = RES / f'drawable-{density}'
        mipmap.mkdir(parents=True, exist_ok=True)
        drawable.mkdir(parents=True, exist_ok=True)

        size = round(48 * scale)
        icon = canvas.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(mipmap / 'ic_launcher.png')
        icon.save(drawable / 'plusgram_icon.png')

        # Round icon with circular mask
        mask = Image.new('L', (size * 4, size * 4), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
        mask = mask.resize((size, size), Image.Resampling.LANCZOS)
        rounded = icon.copy()
        rounded.putalpha(ImageChops.multiply(icon.getchannel('A'), mask))
        rounded.save(mipmap / 'ic_launcher_round.png')

        # Adaptive icon foreground (108x108 with 72x72 safe artwork)
        layer_size = round(108 * scale)
        artwork_size = round(72 * scale)
        layer = Image.new('RGBA', (layer_size, layer_size), (0, 0, 0, 0))
        offset = (layer_size - artwork_size) // 2
        layer.alpha_composite(canvas.resize((artwork_size, artwork_size), Image.Resampling.LANCZOS), (offset, offset))
        layer.save(drawable / 'plusgram_icon_foreground.png')

    # XML resources
    adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/plusgram_icon_background" />
    <foreground android:drawable="@drawable/plusgram_icon_foreground" />
    <monochrome android:drawable="@drawable/plusgram_icon_monochrome" />
</adaptive-icon>
'''
    (RES / 'mipmap-anydpi-v26').mkdir(parents=True, exist_ok=True)
    for name in ['ic_launcher', 'ic_launcher_round']:
        (RES / 'mipmap-anydpi-v26' / f'{name}.xml').write_text(adaptive, encoding='utf-8')

    (RES / 'drawable').mkdir(parents=True, exist_ok=True)
    (RES / 'drawable/plusgram_icon_background.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FCEDEA" />
</shape>
''', encoding='utf-8')

    mark = '''<path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M12,5 H5 V19 H19 V12 M16,5 H21 M18.5,2.5 V7.5 M10,9 H12 V16 M10,16 H14" />'''
    for name, size, transform in [
        ('plusgram_icon_monochrome', 108, '<group android:scaleX="2.5" android:scaleY="2.5" android:translateX="24" android:translateY="24">'),
        ('plusgram_icon_tile', 24, '<group>'),
        ('plusgram_notification', 24, '<group>'),
    ]:
        xml = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{size}dp" android:height="{size}dp" android:viewportWidth="{size}" android:viewportHeight="{size}">
    {transform}{mark}</group>
</vector>
'''
        (RES / 'drawable' / f'{name}.xml').write_text(xml, encoding='utf-8')

    print('Successfully generated all Plusgram icon assets with clean alpha.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=Path)
    args = parser.parse_args()
    generate_icons(args.source)
