#!/usr/bin/env bash
# Run the official headless NeoForge harness, preserving its exit status and requiring every test.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -f run-gametest/reports/gametest.xml
./gradlew runGameTestServer "$@"
python3 - <<'PY'
from pathlib import Path
import re
import xml.etree.ElementTree as ET
path = Path('run-gametest/reports/gametest.xml')
if not path.is_file():
    raise SystemExit('No current GameTest XML report; startup alone is not a passing test run.')
expected = sum(len(re.findall(r'@GameTest\(', p.read_text())) for p in Path('src/main/java/dev/reimagined/gametest').glob('*.java'))
root = ET.parse(path).getroot()
cases = root.findall('.//testcase')
failed = root.findall('.//failure') + root.findall('.//error')
skipped = root.findall('.//skipped')
if expected == 0 or len(cases) != expected or failed or skipped:
    raise SystemExit(f'Incomplete integration run: expected={expected}, executed={len(cases)}, failures={len(failed)}, skipped={len(skipped)}')
print(f'Verified current GameTest report: {len(cases)}/{expected} passed, no failures or skips.')
PY
