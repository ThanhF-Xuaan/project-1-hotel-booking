import { spawnSync } from 'node:child_process';

interface InspectedWorkbook {
  sheetName: string;
  rows: string[][];
  cellTypes: string[][];
  formulaCells: number;
}

// Inspect synthetic API downloads in memory with Python's standard library.
// No XLSX extraction to disk and no production dependency are required.
const INSPECT_XLSX = String.raw`
import io, json, posixpath, re, sys, zipfile
from xml.etree import ElementTree as ET
sys.stdout.reconfigure(encoding='utf-8')
ns = {'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(io.BytesIO(sys.stdin.buffer.read())) as archive:
    workbook = ET.fromstring(archive.read('xl/workbook.xml'))
    sheet = workbook.find('m:sheets/m:sheet', ns)
    relation_id = sheet.attrib['{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id']
    relations = ET.fromstring(archive.read('xl/_rels/workbook.xml.rels'))
    target = next(item.attrib['Target'] for item in relations if item.attrib['Id'] == relation_id)
    target = target.lstrip('/') if target.startswith('/') else posixpath.normpath(posixpath.join('xl', target))
    shared = []
    if 'xl/sharedStrings.xml' in archive.namelist():
        strings = ET.fromstring(archive.read('xl/sharedStrings.xml'))
        shared = [''.join(item.itertext()) for item in strings.findall('m:si', ns)]
    worksheet = ET.fromstring(archive.read(target))
    rows, types, formulas = [], [], 0
    for row in worksheet.findall('m:sheetData/m:row', ns):
        values, kinds = [], []
        for cell in row.findall('m:c', ns):
            index = 0
            for letter in re.match('[A-Z]+', cell.attrib['r']).group():
                index = index * 26 + ord(letter) - ord('A') + 1
            while len(values) < index:
                values.append('')
                kinds.append('')
            kind = cell.attrib.get('t', 'n')
            value = cell.find('m:v', ns)
            text = value.text if value is not None and value.text is not None else ''
            if kind == 'inlineStr':
                text = ''.join(node.text or '' for node in cell.findall('m:is//m:t', ns))
            elif kind == 's':
                text = shared[int(text)]
            if cell.find('m:f', ns) is not None:
                formulas += 1
            values[index - 1], kinds[index - 1] = text, kind
        rows.append(values)
        types.append(kinds)
    print(json.dumps({'sheetName':sheet.attrib['name'],'rows':rows,'cellTypes':types,'formulaCells':formulas},ensure_ascii=False))
`;

export function inspectSyntheticWorkbook(buffer: Buffer): InspectedWorkbook {
  const result = spawnSync('python', ['-c', INSPECT_XLSX], {
    input: buffer,
    encoding: 'utf-8',
    maxBuffer: 1024 * 1024,
    timeout: 10_000,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`Synthetic XLSX inspection failed: ${result.stderr.trim()}`);
  return JSON.parse(result.stdout) as InspectedWorkbook;
}
