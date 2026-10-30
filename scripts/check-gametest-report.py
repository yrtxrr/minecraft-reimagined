#!/usr/bin/env python3
"""Windows/Unix offline check that the current report executed every declared GameTest."""
from pathlib import Path
import argparse,re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--report',type=Path,default=root/'run-gametest/reports/gametest.xml');args=p.parse_args()
expected=sum(len(re.findall(r'@GameTest\(',s.read_text(encoding='utf-8'))) for s in (root/'src/main/java/dev/reimagined/gametest').glob('*.java'))
report=ET.parse(args.report).getroot();cases=report.findall('.//testcase')
failed=report.findall('.//failure')+report.findall('.//error');skipped=report.findall('.//skipped')
if not expected or len(cases)!=expected or failed or skipped:raise SystemExit(f'FAIL: expected{expected},executed{len(cases)},failures{len(failed)},skipped{len(skipped)}')
print(f'GameTest report: {len(cases)}/{expected} passed. Delete stale report before running; report alone does not establish recency.')
