#!/usr/bin/env bash
# Regenerate docs/REPORT.pdf from docs/REPORT.md.
# Requires LibreOffice (soffice) for the HTML -> PDF step; pdfinfo (poppler-utils)
# is optional and only used to report the resulting page count.
set -euo pipefail

cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

python3 scripts/md2html.py docs/REPORT.md "$tmp/REPORT.html"
soffice --headless --convert-to pdf --outdir "$tmp" "$tmp/REPORT.html" >/dev/null 2>&1
mv "$tmp/REPORT.pdf" docs/REPORT.pdf

echo "docs/REPORT.pdf regenerated ($(pdfinfo docs/REPORT.pdf 2>/dev/null | awk '/^Pages/{print $2}') pages)"
