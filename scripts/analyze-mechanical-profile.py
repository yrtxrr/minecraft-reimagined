#!/usr/bin/env python3
"""Summarize measured windows in the optional ordinary-server JFR harness (not a GameTest benchmark)."""
import argparse
from collections import Counter
from datetime import datetime, timedelta
import json
from pathlib import Path
import re
import shutil
import subprocess


def instant(value):
    return datetime.fromisoformat(value.replace('Z', '+00:00'))


def seconds(value):
    if isinstance(value, (float, int)):
        return float(value)
    match = re.fullmatch(r'PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?', value or '')
    if not match:
        return None
    return sum(float(n or 0) * scale for n, scale in zip(match.groups(), (3600, 60, 1)))


def frames(values):
    result = []
    for frame in (values.get('stackTrace') or {}).get('frames', []):
        method = frame.get('method') or {}
        kind = method.get('type') or method.get('declaringClass') or {}
        owner = kind.get('name', '?').replace('/', '.')
        result.append(owner + '.' + method.get('name', '?'))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path, help='reports/<label>-<timestamp>.json from a complete profile')
    parser.add_argument('--jfr', default=shutil.which('jfr'), help='JDK21 jfr executable')
    parser.add_argument('--output', type=Path, help='Optional generated summary JSON; keep it outside Git')
    args = parser.parse_args()
    if not args.jfr:
        raise SystemExit('Use --jfr /path/to/jdk21/bin/jfr')
    report = json.loads(args.report.read_text())
    if not report.get('complete'):
        raise SystemExit('Profile is incomplete: ' + str(report.get('error', 'missing completion marker')))
    summary = {
        'sourceReport': str(args.report),
        'label': report.get('label'),
        'revision': report.get('revision'),
        'notes': [
            'CPU and allocation events are samples, not exact counts or proof of absence.',
            'Allocation weights estimate sampled bytes; they are not a deterministic allocation counter.',
            'Tick statistics come from individual vanilla tick samples, not averaged Minecraft JFR tick events.',
            'Heap deltas without comparable GC are not retained-memory/leak evidence.',
            'No connected clients: client render and network scalability are not measured.',
        ],
        'phases': [],
    }
    types = ','.join(('reimagined.MechanicalTick', 'jdk.ExecutionSample', 'jdk.ObjectAllocationSample', 'jdk.GarbageCollection', 'jdk.GCHeapSummary'))
    for phase in report.get('phases', []):
        if phase.get('ticks') != report.get('measurementTicksPerPhase') or phase.get('millsWithOutput') != phase.get('components'):
            raise SystemExit('Incomplete/idle workload phase: ' + str(phase.get('components')))
        recording = args.report.parent / Path(phase['jfr']).name
        if not recording.is_file():
            raise SystemExit('Missing JFR recording: ' + str(recording))
        if recording.stat().st_size > 128 * 1024 * 1024:
            raise SystemExit('Recording exceeds this helper\'s128MiB analysis bound; use JDK tooling directly')
        process = subprocess.run([args.jfr, 'print', '--json', '--events', types, str(recording)], check=True, capture_output=True, text=True)
        events = json.loads(process.stdout)['recording']['events']
        start = instant(phase['measurementStartUtc'])
        end = start + timedelta(seconds=phase['wallSeconds'])
        samples, mechanical, allocations = Counter(), Counter(), Counter()
        event_counts = Counter()
        allocation_weight = 0
        gc_pause_seconds = 0.0
        gc_pause_values = 0
        heaps_after_gc = []
        for event in events:
            values = event.get('values', {})
            timestamp = values.get('startTime')
            if not timestamp or not start <= instant(timestamp) <= end:
                continue
            kind = event['type']
            event_counts[kind] += 1
            stack = frames(values)
            if kind == 'jdk.ExecutionSample' and stack:
                samples[stack[0]] += 1
                seen = {frame for frame in stack if frame.startswith('dev.reimagined.engineering.')
                        and not frame.startswith('dev.reimagined.engineering.profile.')}
                mechanical.update(seen)
            elif kind == 'jdk.ObjectAllocationSample':
                weight = values.get('weight', 0)
                if isinstance(weight, (int, float)):
                    allocation_weight += weight
                    if stack:
                        allocations[stack[0]] += weight
            elif kind == 'jdk.GarbageCollection':
                pause = seconds(values.get('sumOfPauses'))
                if pause is not None:
                    gc_pause_seconds += pause
                    gc_pause_values += 1
            elif kind == 'jdk.GCHeapSummary' and values.get('when') == 'After GC':
                heaps_after_gc.append(values.get('heapUsed'))
        # The custom event is emitted once per measured server tick, and nowhere in warmup.
        tick_events = sum(1 for event in events if event['type'] == 'reimagined.MechanicalTick')
        if tick_events != phase['ticks']:
            raise SystemExit(f'JFR tick events do not match actual measured phase: {tick_events} != {phase["ticks"]}')
        summary['phases'].append({
            **phase,
            'measuredWindowEventCounts': dict(event_counts),
            'jfrMeasuredTickEvents': tick_events,
            'cpuTopSampledLeafFrames': samples.most_common(20),
            'mechanicalFramesPresentInCpuSamples': mechanical.most_common(20),
            'sampledAllocationWeightBytes': allocation_weight,
            'sampledAllocationTopLeafFramesByWeight': allocations.most_common(20),
            'gcSumOfPausesMs': gc_pause_seconds * 1000 if gc_pause_values else None,
            'afterGcHeapUsedBytes': heaps_after_gc,
        })
    if len(summary['phases']) != 3:
        raise SystemExit('Expected completed0/16/64 profile phases')
    rendered = json.dumps(summary, indent=2, ensure_ascii=False) + '\n'
    if args.output:
        args.output.write_text(rendered)
    else:
        print(rendered, end='')


if __name__ == '__main__':
    main()
