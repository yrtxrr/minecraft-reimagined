#!/usr/bin/env python3
"""Download the pinned single-player modpack from Modrinth's official CDN.

Files are never committed and are not redistributed by this repository. Each download is
checked against the SHA-512 recorded in pack/mods.json.
"""
import argparse
import hashlib
import json
import shutil
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "pack" / "mods.json"


def sha512(path: Path) -> str:
    digest = hashlib.sha512()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def download(url: str, target: Path) -> None:
    partial = target.with_name(target.name + ".part")
    request = urllib.request.Request(url, headers={"User-Agent": "reimagined-pack/0.1"})
    with urllib.request.urlopen(request, timeout=180) as response, partial.open("wb") as out:
        shutil.copyfileobj(response, out)
    partial.replace(target)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--destination", required=True, type=Path,
                        help="mods directory of the target NeoForge instance")
    parser.add_argument("--optional", action="store_true", help="also install optional mods")
    parser.add_argument("--dry-run", action="store_true", help="print the plan without downloading")
    args = parser.parse_args()

    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    destination = args.destination.expanduser()
    if not args.dry_run:
        destination.mkdir(parents=True, exist_ok=True)

    installed = skipped = 0
    for mod in manifest["mods"]:
        if not mod["required"] and not args.optional:
            print(f"skip optional: {mod['name']} {mod['version']}")
            continue
        target = destination / mod["filename"]
        if target.is_file() and sha512(target) == mod["sha512"]:
            print(f"ok: {mod['filename']}")
            skipped += 1
            continue
        if args.dry_run:
            print(f"would fetch: {mod['name']} {mod['version']} -> {target}")
            continue
        print(f"fetching: {mod['name']} {mod['version']} ({mod['size'] / 1048576:.1f} MB)")
        try:
            download(mod["url"], target)
        except Exception as error:  # noqa: BLE001 - report the exact failing mod
            print(f"error: {mod['name']} download failed: {error}", file=sys.stderr)
            return 1
        actual = sha512(target)
        if actual != mod["sha512"]:
            target.unlink(missing_ok=True)
            print(f"error: {mod['name']} SHA-512 mismatch", file=sys.stderr)
            return 1
        installed += 1

    if args.dry_run:
        print("Dry run only. Nothing was downloaded.")
    else:
        print(f"Done: {installed} downloaded, {skipped} already present, "
              f"Minecraft {manifest['minecraft']} / NeoForge {manifest['neoforge']}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
