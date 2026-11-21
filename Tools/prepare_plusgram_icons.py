import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'TMessagesProj/src/main/res'


def adaptive_layer(canvas: Image.Image, scale: float) -> Image.Image:
    # The central 72dp match the square preview. Extend edge colors into the
    # extra 18dp on each side so launcher motion cannot expose a seam.
    size = round(108 * scale)
    inset = round(18 * scale)
    art_size = size - 2 * inset
    art = canvas.resize((art_size, art_size), Image.Resampling.LANCZOS)
    strip = Image.new('RGBA', (size, art_size))
    strip.paste(art, (inset, 0))
    for edge, left in [(0, 0), (art_size - 1, inset + art_size)]:
        column = art.crop((edge, 0, edge + 1, art_size))
        strip.paste(column.resize((inset, art_size), Image.Resampling.NEAREST), (left, 0))

    layer = Image.new('RGBA', (size, size))
    layer.paste(strip, (0, inset))
    for edge, top in [(0, 0), (art_size - 1, inset + art_size)]:
        row = strip.crop((0, edge, size, edge + 1))
        layer.paste(row.resize((size, inset), Image.Resampling.NEAREST), (0, top))
    return layer


def generate_icons(source_path: Path):
    with Image.open(source_path) as artwork:
        canvas = ImageOps.fit(artwork.convert('RGBA'), (1024, 1024), Image.Resampling.LANCZOS)

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

        mask = Image.new('L', (size * 4, size * 4))
        ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
        mask = mask.resize((size, size), Image.Resampling.LANCZOS)
        rounded = icon.copy()
        rounded.putalpha(ImageChops.multiply(icon.getchannel('A'), mask))
        rounded.save(mipmap / 'ic_launcher_round.png')

        layer = adaptive_layer(canvas, scale)
        layer.save(drawable / 'plusgram_icon_foreground.png')
        layer.save(mipmap / 'ic_launcher_foreground.png')

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
    <solid android:color="#F0E3E1" />
</shape>
''', encoding='utf-8')

    mark = '''<path android:fillColor="#FFFFFFFF"
        android:pathData="M13,4.5 H5 Q2.5,4.5 2.5,7 V20 Q2.5,22.5 5,22.5 H18 Q20.5,22.5 20.5,20 V12.4 A1.45,1.45 0 0,0 17.6,12.4 V18.7 Q17.6,19.6 16.7,19.6 H6.3 Q5.4,19.6 5.4,18.7 V8.3 Q5.4,7.4 6.3,7.4 H13 A1.45,1.45 0 0,0 13,4.5 Z
                          M18,4.5 V2.8 A1.4,1.4 0 0,1 20.8,2.8 V4.5 H22.5 A1.4,1.4 0 0,1 22.5,7.3 H20.8 V9 A1.4,1.4 0 0,1 18,9 V7.3 H16.3 A1.4,1.4 0 0,1 16.3,4.5 Z
                          M7.5,13 L15.8,9.8 Q16.7,9.5 16.5,10.4 L15,17 Q14.8,17.8 14.1,17.3 L12.1,15.9 L10.6,17.1 Q10,17.6 10.1,16.8 L10.4,15 L14.2,11.7 L9.5,14.6 L7.5,13.8 Q6.7,13.4 7.5,13 Z" />'''
    for name, size, transform in [
        ('plusgram_icon_monochrome', 108, '<group android:scaleX="2.5" android:scaleY="2.5" android:translateX="21" android:translateY="24">'),
        ('plusgram_icon_tile', 24, '<group android:translateX="-1.2">'),
        ('plusgram_notification', 24, '<group android:translateX="-1.2">'),
    ]:
        xml = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{size}dp" android:height="{size}dp" android:viewportWidth="{size}" android:viewportHeight="{size}">
    {transform}{mark}</group>
</vector>
'''
        (RES / 'drawable' / f'{name}.xml').write_text(xml, encoding='utf-8')

    print('Successfully generated all Plusgram icon assets.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=Path)
    args = parser.parse_args()
    generate_icons(args.source)
