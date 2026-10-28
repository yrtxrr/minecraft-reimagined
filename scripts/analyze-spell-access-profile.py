#!/usr/bin/env python3
"""Validate and summarize the opt-in ordinary-server spell-ray probe; raw files stay ignored."""
import argparse
from collections import Counter
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('report', type=Path)
parser.add_argument('--jfr', required=True)
args = parser.parse_args()
report = json.loads(args.report.read_text())
if not report.get('complete') or [p['raysPerTick'] for p in report['phases']] != [0, 256]:
    raise SystemExit('Incomplete spell workload')
for phase in report['phases']:
    rays = phase['raysPerTick']
    recording = args.report.with_name(args.report.stem + f'-{rays}.jfr')
    if not recording.is_file() or not 0 < recording.stat().st_size < 128 * 1024 * 1024:
        raise SystemExit('Missing/empty/oversized JFR')
    data = subprocess.check_output([args.jfr, 'print', '--json', '--stack-depth', '64', '--events',
        'reimagined.SpellAccessTick,jdk.ExecutionSample,jdk.ObjectAllocationSample,jdk.GarbageCollection', str(recording)])
    events = json.loads(data)['recording']['events']
    counts = Counter(e['type'] for e in events)
    if counts['reimagined.SpellAccessTick'] != 600:
        raise SystemExit('Expected600 measured workload events')
    hotspots = Counter()
    for event in events:
        if event['type'] == 'jdk.ExecutionSample':
            for frame in (event['values'].get('stackTrace') or {}).get('frames', []):
                method = frame['method']; owner = method.get('type', {}).get('name', '').replace('/', '.')
                if owner.startswith('dev.reimagined.magic.SpellWorldAccess'):
                    hotspots[owner + '.' + method['name']] += 1
    phase['jfrEvents'] = counts
    phase['sampledAllocationBytes'] = sum(e['values'].get('weight', 0) for e in events if e['type'] == 'jdk.ObjectAllocationSample')
    phase['spellStackSamples'] = hotspots
print(json.dumps(report, indent=2))
