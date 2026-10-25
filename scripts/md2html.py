#!/usr/bin/env python3
"""Minimal Markdown -> print-ready HTML for the mini-project report.

Supports only what docs/REPORT.md actually uses: ATX headings, fenced code
blocks, pipe tables, bold/inline-code spans, bullet lists, hr and paragraphs.
"""
import html
import re
import sys

CSS = """
@page { margin: 13mm 14mm; }
body { font-family: "Liberation Serif", Georgia, serif; font-size: 8.75pt;
       line-height: 1.24; color: #111; }
h1 { font-family: "Liberation Sans", Arial, sans-serif; font-size: 14.5pt;
     margin: 0 0 2pt; line-height: 1.2; }
h2 { font-family: "Liberation Sans", Arial, sans-serif; font-size: 10.2pt;
     margin: 7pt 0 2pt; padding-bottom: 1.5pt; border-bottom: 0.6pt solid #bbb; }
h3 { font-family: "Liberation Sans", Arial, sans-serif; font-size: 10pt; margin: 8pt 0 2pt; }
p { margin: 0 0 3.4pt; text-align: justify; }
p.hdr { text-align: left; margin-bottom: 2.5pt; }
.subtitle { font-size: 9.5pt; color: #333; margin-bottom: 1pt; }
.meta { font-family: "Liberation Mono", monospace; font-size: 8.2pt; color: #444;
        margin-bottom: 8pt; }
code { font-family: "DejaVu Sans Mono", monospace; font-size: 7.7pt; }
pre { font-family: "DejaVu Sans Mono", "Noto Sans Mono", monospace; font-size: 5.5pt;
      line-height: 1.1; background: #f6f6f6; border: 0.5pt solid #ddd;
      padding: 4pt 5pt; margin: 4pt 0 6pt; white-space: pre; }
table { border-collapse: collapse; width: 100%; margin: 2pt 0 4.5pt; font-size: 7.7pt; }
th, td { border: 0.5pt solid #bbb; padding: 1.9pt 3.4pt; text-align: left;
         vertical-align: top; }
th { background: #eee; font-family: "Liberation Sans", Arial, sans-serif; font-size: 7.9pt; }
ul { margin: 0 0 5pt; padding-left: 15pt; }
li { margin-bottom: 1.5pt; }
hr { border: none; border-top: 0.6pt solid #ccc; margin: 7pt 0; }
"""


def inline(text: str) -> str:
    """Escape, then apply emphasis. Code spans are stashed behind placeholders first so
    that emphasis spanning a code span (e.g. **9 - `Infinity` leaks**) still matches."""
    spans: list[str] = []

    def stash(m):
        spans.append(f"<code>{html.escape(m.group(1))}</code>")
        return f"\x00{len(spans) - 1}\x00"

    text = re.sub(r"`([^`]+)`", stash, text)
    text = html.escape(text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text, flags=re.S)
    text = re.sub(r"(?<![*\w])\*([^*]+)\*(?![*\w])", r"<em>\1</em>", text)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"\1", text)
    return re.sub(r"\x00(\d+)\x00", lambda m: spans[int(m.group(1))], text)


def convert(md: str) -> str:
    lines = md.split("\n")
    body, i, n = [], 0, len(lines)
    para: list[str] = []
    bullets: list[str] = []

    seen_h2 = [False]

    def flush():
        if para:
            cls = "" if seen_h2[0] else ' class="hdr"'
            body.append(f"<p{cls}>{inline(' '.join(para))}</p>")
            para.clear()
        if bullets:
            body.append("<ul>" + "".join(f"<li>{inline(b)}</li>" for b in bullets) + "</ul>")
            bullets.clear()

    while i < n:
        line = lines[i]

        if line.startswith("```"):
            flush()
            i += 1
            block = []
            while i < n and not lines[i].startswith("```"):
                block.append(lines[i])
                i += 1
            body.append("<pre>" + html.escape("\n".join(block)) + "</pre>")
            i += 1
            continue

        if re.match(r"^\|.*\|\s*$", line) and i + 1 < n and re.match(r"^\|[\s:|-]+\|\s*$", lines[i + 1]):
            flush()
            cells = lambda r: [c.strip() for c in r.strip().strip("|").split("|")]
            head = cells(line)
            i += 2
            rows = []
            while i < n and re.match(r"^\|.*\|\s*$", lines[i]):
                rows.append(cells(lines[i]))
                i += 1
            thead = "".join(f"<th>{inline(c)}</th>" for c in head)
            tbody = "".join(
                "<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in rows
            )
            body.append(f"<table><thead><tr>{thead}</tr></thead><tbody>{tbody}</tbody></table>")
            continue

        m = re.match(r"^(#{1,4})\s+(.*)$", line)
        if m:
            flush()
            lvl = len(m.group(1))
            if lvl >= 2:
                seen_h2[0] = True
            body.append(f"<h{lvl}>{inline(m.group(2))}</h{lvl}>")
            i += 1
            continue

        if re.match(r"^(---+|___+|\*\*\*+)\s*$", line):
            flush()
            body.append("<hr/>")
            i += 1
            continue

        m = re.match(r"^\s*(?:[-*]|\d+\.)\s+(.*)$", line)
        if m:
            if para:
                body.append(f"<p>{inline(' '.join(para))}</p>")
                para.clear()
            bullets.append(m.group(1))
            i += 1
            continue

        if not line.strip():
            flush()
            i += 1
            continue

        if bullets:
            bullets[-1] += " " + line.strip()
        else:
            para.append(line.strip())
        i += 1

    flush()
    return f"<html><head><meta charset='utf-8'><style>{CSS}</style></head><body>{''.join(body)}</body></html>"


if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    with open(dst, "w") as fh:
        fh.write(convert(open(src).read()))
