#!/usr/bin/env python3
"""Convert the approved, pinned Lucide SVG masters to Android VectorDrawables."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'design/branding/icons/lucide'
DESTINATION = ROOT / 'android/app/src/main/res/drawable'


def path_data(element):
    kind = element.tag.split('}')[-1]
    a = element.attrib
    if kind == 'path':
        return a['d']
    if kind == 'line':
        return f"M{a['x1']},{a['y1']} L{a['x2']},{a['y2']}"
    if kind == 'circle':
        x, y, r = map(float, (a['cx'], a['cy'], a['r']))
        return f'M{x-r:g},{y:g} a{r:g},{r:g} 0 1,0 {2*r:g},0 a{r:g},{r:g} 0 1,0 {-2*r:g},0'
    if kind == 'rect':
        x, y = float(a.get('x', 0)), float(a.get('y', 0))
        w, h = float(a['width']), float(a['height'])
        rx = float(a.get('rx', 0))
        ry = float(a.get('ry', rx))
        if not rx:
            return f'M{x:g},{y:g} h{w:g} v{h:g} h{-w:g} Z'
        return (f'M{x+rx:g},{y:g} H{x+w-rx:g} A{rx:g},{ry:g} 0 0,1 {x+w:g},{y+ry:g} '
                f'V{y+h-ry:g} A{rx:g},{ry:g} 0 0,1 {x+w-rx:g},{y+h:g} '
                f'H{x+rx:g} A{rx:g},{ry:g} 0 0,1 {x:g},{y+h-ry:g} '
                f'V{y+ry:g} A{rx:g},{ry:g} 0 0,1 {x+rx:g},{y:g} Z')
    raise ValueError(f'Unsupported SVG element: {kind}')


def main():
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for source in sorted(SOURCE.glob('*.svg')):
        paths = []
        for element in ET.parse(source).getroot():
            d = path_data(element)
            paths.append(f'    <path android:pathData="{d}" android:fillColor="@android:color/transparent" android:strokeColor="#FF000000" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" />')
        output = DESTINATION / f"lucide_{source.stem.replace('-', '_')}.xml"
        output.write_text('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                          '    android:width="24dp" android:height="24dp"\n'
                          '    android:viewportWidth="24" android:viewportHeight="24">\n'
                          + '\n'.join(paths) + '\n</vector>\n', encoding='utf-8')


if __name__ == '__main__':
    main()
