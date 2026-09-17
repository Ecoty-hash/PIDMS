/**
 * PIDMS · 《数据库设计文档》 Markdown → Word 生成脚本
 *
 * 输入：database/数据库设计文档.md
 * 输出：database/数据库设计文档.docx
 *
 * 版式沿用「浅色蓝图纸风 v3」设计令牌：
 *   墨蓝 #16324F / 蓝图蓝 #1D6FD1 / 合格绿 #1E8E5E
 *   正文 #333333 / 弱化 #5B6B7D / 发丝线 #DBE3EE
 *   字体 Calibri + 微软雅黑（中文），代码 Consolas
 *
 * 依赖：node_modules 中需有 docx（npm i docx）
 * 用法：node gen_doc_docx.js
 */
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell,
  AlignmentType, BorderStyle, WidthType, ShadingType, VerticalAlign,
  HeadingLevel, Footer, PageNumber, NumberFormat, convertInchesToTwip, PageBreak,
} = require('docx');

const DB_DIR = path.resolve(__dirname, '..');   // database/
const MD_PATH = path.join(DB_DIR, '数据库设计文档.md');
const OUT_PATH = path.join(DB_DIR, '数据库设计文档.docx');
const ER_PNG = path.join(DB_DIR, 'ER关系图.png');

// ---------------------------------------------------------------- 设计令牌
const INK = '16324F';
const BLUE = '1D6FD1';
const SLATE = '333333';
const MUTED = '5B6B7D';
const LINE = 'DBE3EE';
const CJK = { ascii: 'Calibri', eastAsia: '微软雅黑', hAnsi: 'Calibri' };
const MONO = { ascii: 'Consolas', eastAsia: '微软雅黑', hAnsi: 'Consolas' };
const TABLE_W = 9026;

// ---------------------------------------------------------------- Markdown 解析
function parseInline(text) {
  // 返回 [{text, code, bold}]
  const out = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let last = 0, m;
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) out.push({ text: text.slice(last, m.index) });
    const tok = m[0];
    if (tok.startsWith('**')) out.push({ text: tok.slice(2, -2), bold: true });
    else out.push({ text: tok.slice(1, -1), code: true });
    last = m.index + tok.length;
  }
  if (last < text.length) out.push({ text: text.slice(last) });
  return out.length ? out : [{ text: '' }];
}

function splitRow(line) {
  let s = line.trim();
  if (s.startsWith('|')) s = s.slice(1);
  if (s.endsWith('|')) s = s.slice(0, -1);
  return s.split('|').map((c) => c.trim());
}

function parseBlocks(md) {
  const lines = md.replace(/\r\n/g, '\n').split('\n');
  const blocks = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    if (/^```/.test(line)) {                       // 代码围栏（mermaid → ER 图）
      const lang = line.slice(3).trim();
      const buf = [];
      i++;
      while (i < lines.length && !/^```/.test(lines[i])) buf.push(lines[i++]);
      i++;
      blocks.push({ type: 'code', lang, text: buf.join('\n') });
      continue;
    }
    if (/^#{1,6}\s/.test(line)) {
      const m = /^(#{1,6})\s+(.*)$/.exec(line);
      blocks.push({ type: 'h', level: m[1].length, text: m[2].trim() });
      i++;
      continue;
    }
    if (/^---+$/.test(line.trim())) { blocks.push({ type: 'hr' }); i++; continue; }
    if (/^>\s?/.test(line)) {
      const buf = [];
      while (i < lines.length && /^>\s?/.test(lines[i])) buf.push(lines[i++].replace(/^>\s?/, ''));
      blocks.push({ type: 'quote', text: buf.join(' ') });
      continue;
    }
    if (/^\s*[-*]\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) {
        const indent = lines[i].match(/^\s*/)[0].length;
        items.push({ text: lines[i].replace(/^\s*[-*]\s+/, ''), level: indent >= 2 ? 1 : 0 });
        i++;
      }
      blocks.push({ type: 'ul', items });
      continue;
    }
    if (/^\s*\d+\.\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        const indent = lines[i].match(/^\s*/)[0].length;
        items.push({ text: lines[i].replace(/^\s*\d+\.\s+/, ''), level: indent >= 2 ? 1 : 0 });
        i++;
      }
      blocks.push({ type: 'ol', items });
      continue;
    }
    if (/^\|/.test(line.trim())) {
      const rows = [];
      while (i < lines.length && /^\|/.test(lines[i].trim())) rows.push(splitRow(lines[i++]));
      const header = rows[0];
      const body = rows.slice(2);            // 跳过 |---| 分隔行
      blocks.push({ type: 'table', header, body });
      continue;
    }
    if (line.trim() === '') { i++; continue; }
    const buf = [];
    while (i < lines.length && lines[i].trim() !== '' &&
           !/^(#{1,6}\s|>|\s*[-*]\s|\s*\d+\.\s|\||```|---+$)/.test(lines[i])) {
      buf.push(lines[i++]);
    }
    blocks.push({ type: 'p', text: buf.join(' ') });
  }
  return blocks;
}

// ---------------------------------------------------------------- 渲染
function runs(text, { size = 21, color = SLATE, forceCode = false } = {}) {
  return parseInline(text).flatMap((t) => {
    const parts = t.text.split('<br>');
    const out = [];
    parts.forEach((p, idx) => {
      if (idx > 0) out.push(new TextRun({ break: 1 }));
      if (p === '') return;
      out.push(new TextRun({
        text: p,
        bold: !!t.bold,
        size,
        color: t.bold ? BLUE : color,
        font: (t.code || forceCode) ? MONO : CJK,
      }));
    });
    return out;
  });
}

function body(text) {
  return new Paragraph({
    spacing: { before: 60, after: 60, line: 276 },
    children: runs(text),
  });
}

function heading(level, text) {
  if (level === 1) {
    return new Paragraph({
      heading: HeadingLevel.HEADING_1,
      spacing: { before: 360, after: 180 },
      children: runs(text, { size: 30, color: INK }),
    });
  }
  if (level === 2) {
    return new Paragraph({
      heading: HeadingLevel.HEADING_2,
      spacing: { before: 260, after: 140 },
      children: runs(text, { size: 24, color: INK }),
    });
  }
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 220, after: 120 },
    children: runs(text, { size: 22, color: INK }),
  });
}

function listItem(text, reference, level) {
  return new Paragraph({
    numbering: { reference, level },
    spacing: { before: 60, after: 60, line: 276 },
    children: runs(text),
  });
}

// 排版宽度估算（英寸）。10pt 下：Calibri 比例字 ≈0.0625in/字，
// Consolas 等宽 ≈0.0764in/字，中日韩全角 0.1389in/字。
const W_PROP = 0.064, W_MONO = 0.086, W_CJK = 0.1389, CELL_PAD = 0.16;

function textIn(text) {
  return parseInline(text).reduce((acc, t) => {
    const per = t.code ? W_MONO : W_PROP;
    for (const ch of t.text)
      acc += /[⺀-鿿＀-￯　-〿]/.test(ch) ? W_CJK : per;
    return acc;
  }, 0);
}

function colNeed(cell) {
  return String(cell).split('<br>').reduce((m, l) => Math.max(m, textIn(l)), 0);
}

function columnWidths(header, rows) {
  const AVAIL = TABLE_W / 1440;                       // 6.27in
  const MIN_IN = 0.62, MAX_IN = 2.60;

  const need = header.map((h, c) => {
    const w = Math.max(colNeed(h), ...rows.map((r) => colNeed(r[c] || '')));
    return Math.min(MAX_IN, Math.max(MIN_IN, w + CELL_PAD));
  });

  let total = need.reduce((a, b) => a + b, 0);
  let widths;
  if (total <= AVAIL) {
    // 富余宽度按各列内容占比再分配，避免「说明」列被撑得过宽
    const extra = AVAIL - total;
    const share = need.reduce((a, b) => a + b, 0);
    widths = need.map((w) => w + extra * (w / share));
    widths[widths.length - 1] += AVAIL - widths.reduce((a, b) => a + b, 0);
  } else {
    widths = need.map((w) => (w / total) * AVAIL);
  }
  return widths.map((w) => Math.round(w * 1440));
}

function table(header, rows) {
  const widths = columnWidths(header, rows);

  const cellMargins = { top: 40, bottom: 40, left: 80, right: 80 };
  const border = { style: BorderStyle.SINGLE, size: 4, color: LINE };
  const borders = { top: border, bottom: border, left: border, right: border,
                    insideHorizontal: border, insideVertical: border };

  const headRow = new TableRow({
    tableHeader: true,
    children: header.map((h, c) => new TableCell({
      width: { size: widths[c], type: WidthType.DXA },
      shading: { type: ShadingType.CLEAR, fill: INK, color: 'auto' },
      margins: cellMargins,
      verticalAlign: VerticalAlign.CENTER,
      children: [new Paragraph({
        spacing: { before: 20, after: 20, line: 240 },
        children: runs(h, { size: 20, color: 'FFFFFF' }),
      })],
    })),
  });

  const bodyRows = rows.map((r) => new TableRow({
    children: header.map((_, c) => new TableCell({
      width: { size: widths[c], type: WidthType.DXA },
      margins: cellMargins,
      verticalAlign: VerticalAlign.CENTER,
      children: [new Paragraph({
        spacing: { before: 20, after: 20, line: 240 },
        children: runs(r[c] || '', { size: 20 }),
      })],
    })),
  }));

  return new Table({
    columnWidths: widths,
    width: { size: TABLE_W, type: WidthType.DXA },
    borders,
    rows: [headRow, ...bodyRows],
  });
}

function hr() {
  return new Paragraph({
    spacing: { before: 160, after: 160 },
    border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: LINE, space: 1 } },
    children: [new TextRun({ text: '' })],
  });
}

function erImage() {
  const img = fs.readFileSync(ER_PNG);
  const px = { w: 4276, h: 2489 };
  const width = 5715000;                            // 与旧版文档一致的显示宽度
  const height = Math.round(width * px.h / px.w);
  return new Paragraph({
    spacing: { before: 120, after: 120 },
    alignment: AlignmentType.CENTER,
    children: [new ImageRun({ data: img, type: 'png',
                              transformation: { width: width / 9525, height: height / 9525 } })],
  });
}

// ---------------------------------------------------------------- 组装
const md = fs.readFileSync(MD_PATH, 'utf8');
const blocks = parseBlocks(md);
const children = [];
let coverTableDone = false;

// 封面：md 的一级标题拆成「主标题 + 副标题」
const first = blocks.shift();
if (first && first.type === 'h') {
  const [main, sub] = first.text.split('·').map((s) => s.trim());
  children.push(new Paragraph({
    spacing: { before: 240, after: 40 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: main, bold: true, color: INK, size: 44, font: CJK })],
  }));
  children.push(new Paragraph({
    spacing: { before: 0, after: 200 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: sub || '', bold: true, color: BLUE, size: 32, font: CJK })],
  }));
  children.push(new Paragraph({
    spacing: { before: 80, after: 120 },
    border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: LINE, space: 1 } },
    children: [new TextRun({ text: '' })],
  }));
}

for (const b of blocks) {
  switch (b.type) {
    case 'h': children.push(heading(b.level, b.text)); break;
    case 'p': children.push(body(b.text)); break;
    case 'ul': b.items.forEach((it) => children.push(listItem(it.text, 'bullet-list', it.level))); break;
    case 'ol': b.items.forEach((it) => children.push(listItem(it.text, 'ordered-list', it.level))); break;
    case 'quote':
      children.push(new Paragraph({
        spacing: { before: 60, after: 60, line: 276 },
        children: runs(b.text, { color: MUTED }),
      }));
      break;
    case 'table': {
      children.push(table(b.header, b.body));
      if (!coverTableDone) {
        // 封面信息表之后分页，正文从第 2 页开始
        coverTableDone = true;
        children.push(new Paragraph({ children: [new PageBreak()] }));
      } else {
        children.push(new Paragraph({}));
      }
      break;
    }
    case 'hr': children.push(hr()); break;
    case 'code':
      if (b.lang === 'mermaid') {
        children.push(erImage());
      } else {
        b.text.split('\n').forEach((l) => children.push(new Paragraph({
          spacing: { before: 0, after: 0, line: 240 },
          children: [new TextRun({ text: l, size: 18, font: MONO, color: SLATE })],
        })));
      }
      break;
    default: break;
  }
}

const numbering = {
  config: [
    {
      reference: 'bullet-list',
      levels: [
        { level: 0, format: NumberFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: convertInchesToTwip(0.28), hanging: convertInchesToTwip(0.18) } } } },
        { level: 1, format: NumberFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: convertInchesToTwip(0.62), hanging: convertInchesToTwip(0.18) } } } },
      ],
    },
    {
      reference: 'ordered-list',
      levels: [
        { level: 0, format: NumberFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: convertInchesToTwip(0.32), hanging: convertInchesToTwip(0.22) } } } },
        { level: 1, format: NumberFormat.DECIMAL, text: '%1.%2', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: convertInchesToTwip(0.68), hanging: convertInchesToTwip(0.28) } } } },
      ],
    },
  ],
};

const doc = new Document({
  creator: 'PIDMS',
  title: 'PIDMS 企业管理系统 · 数据库设计文档',
  styles: {
    default: {
      document: {
        run: { size: 21, color: SLATE, font: CJK },
        paragraph: { spacing: { line: 276 } },
      },
    },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { bold: true, color: INK, size: 30, font: CJK },
        paragraph: { keepNext: true, spacing: { before: 360, after: 180 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { bold: true, color: INK, size: 24, font: CJK },
        paragraph: { keepNext: true, spacing: { before: 260, after: 140 }, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { bold: true, color: INK, size: 22, font: CJK },
        paragraph: { keepNext: true, spacing: { before: 220, after: 120 }, outlineLevel: 2 } },
    ],
  },
  numbering,
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
      },
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            new TextRun({ text: 'PIDMS 数据库设计文档 · 第 ', size: 18, color: MUTED, font: CJK }),
            new TextRun({ children: [PageNumber.CURRENT], size: 18, color: MUTED, font: CJK }),
            new TextRun({ text: ' 页 / 共 ', size: 18, color: MUTED, font: CJK }),
            new TextRun({ children: [PageNumber.TOTAL_PAGES], size: 18, color: MUTED, font: CJK }),
            new TextRun({ text: ' 页', size: 18, color: MUTED, font: CJK }),
          ],
        })],
      }),
    },
    children,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(OUT_PATH, buf);
  console.log('saved', OUT_PATH, buf.length, 'bytes;', children.length, 'blocks');
});
