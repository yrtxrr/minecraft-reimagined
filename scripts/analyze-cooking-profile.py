#!/usr/bin/env python3
"""Summarize measured normal-server cooking phases and their JFR samples (not causality)."""
import argparse
import collections
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('report', type=Path)
args = parser.parse_args()
report = json.loads(args.report.read_text())
if not report.get('complete') or [p['pots'] for p in report['phases']] != [0, 64, 64, 256]:
    raise SystemExit('Incomplete/unrecognized cooking workload')
jfr = str(Path(os.environ['JAVA_HOME']) / 'bin/jfr') if 'JAVA_HOME' in os.environ else 'jfr'
def stamp(value):
    return dt.datetime.fromisoformat(value.replace('Z', '+00:00')).timestamp()
def duration(value):
    match = re.fullmatch(r'PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?', value)
    if not match:
        raise ValueError(value)
    h, m, s = (float(v or 0) for v in match.groups())
    return h * 3600 + m * 60 + s
results = []
for phase in report['phases']:
    if phase['ticks'] != 600 or phase['mealOutput'] != (phase['pots'] * 3 if phase['active'] else 0):
        raise SystemExit('A measured phase did not complete the real cooking workload')
    recording = args.report.parent / Path(phase['jfr']).name
    output = subprocess.check_output([jfr, 'print', '--json', '--stack-depth', '64', '--events',
        'reimagined.CookingTick,jdk.ExecutionSample,jdk.NativeMethodSample,jdk.ObjectAllocationSample,jdk.GCPhasePause,jdk.GarbageCollection', str(recording)])
    events = json.loads(output)['recording']['events']
    start, end = stamp(phase['measurementStartUtc']), stamp(phase['measurementEndUtc'])
    events = [e for e in events if start <= stamp(e['values']['startTime']) <= end]
    ticks = [e for e in events if e['type'] == 'reimagined.CookingTick']
    if len(ticks) != 600:
        raise SystemExit(f'Expected600measured custom tick events, got{len(ticks)}')
    samples = [e for e in events if e['type'] in ('jdk.ExecutionSample', 'jdk.NativeMethodSample')]
    def frames(event):
        return (event['values'].get('stackTrace') or {}).get('frames', [])
    def method(frame):
        m = frame['method']
        return m['type']['name'] + '.' + m['name']
    counts = collections.Counter()
    main_counts = collections.Counter()
    for event in samples:
        for frame in frames(event):
            name = method(frame)
            if name.startswith('dev/reimagined/cooking/'):
                counts[name] += 1
                break
        thread = event['values'].get('sampledThread') or {}
        if thread.get('javaName') == 'Server thread' and frames(event):
            main_counts[method(frames(event)[0])] += 1
    pauses = [duration(e['values']['duration']) * 1000 for e in events if e['type'] == 'jdk.GCPhasePause']
    allocated = sum(e['values'].get('weight', 0) for e in events if e['type'] == 'jdk.ObjectAllocationSample')
    result = dict(phase)
    result.update(measuredJfrTicks=len(ticks), sampledAllocationMiB=allocated / 2**20,
                  gcPauseCount=len(pauses), gcPauseTotalMs=sum(pauses), gcPauseMaxMs=max(pauses, default=0),
                  cookingSamplePresence=dict(counts), mainThreadTopSamples=main_counts.most_common(8))
    slow = []
    for tick in sorted(ticks, key=lambda e: e['values']['durationNanos'], reverse=True)[:3]:
        finish = stamp(tick['values']['startTime'])
        elapsed = tick['values']['durationNanos'] / 1e9
        window = [e for e in samples if finish - elapsed <= stamp(e['values']['startTime']) <= finish]
        slow.append({'tick': tick['values']['tick'], 'ms': elapsed * 1000,
                     'sampledStacks': [[method(f) for f in frames(e)[:12]] for e in window[:8]]})
    result['longestTickNearbySamplesNotProofOfCause'] = slow
    results.append(result)
print(json.dumps({'revision': report['revision'], 'notes': 'JFR sampling/weighted allocations; no retained-memory or exact per-method cost verdict. Whole tick windows end at the custom Post event; nearby stacks are not attribution proof.', 'phases': results}, indent=2))
