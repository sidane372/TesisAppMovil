"""Extract the original RF/RNF rows without changing their identifiers."""
import argparse
import json
import re
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET


def extract(source):
    namespace = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    with ZipFile(source) as document:
        root = ET.fromstring(document.read('word/document.xml'))
    records = []
    for table in root.findall('.//w:tbl', namespace):
        for row in table.findall('w:tr', namespace):
            cells = [' '.join(''.join(text.itertext()) for text in cell.findall('.//w:t', namespace))
                     for cell in row.findall('w:tc', namespace)]
            match = re.match(r'^(RNF-\d{2}|RF-\d{2})', cells[0]) if cells else None
            if match and len(cells) >= 2:
                records.append({'id': match[1], 'requisito': cells[1],
                                'criterio_original': cells[2] if len(cells) > 2 else ''})
    expected = {f'RF-{n:02}' for n in range(1, 69)} | {f'RNF-{n:02}' for n in range(1, 23)}
    if len(records) != 90 or {record['id'] for record in records} != expected:
        raise ValueError('Expected exactly RF-01..RF-68 and RNF-01..RNF-22; review source tables.')
    return records


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    records = extract(args.source)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(records, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'{len(records)} requirements extracted')
