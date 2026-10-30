#!/usr/bin/env python3
"""Offline validation of the pinned modpack manifest and downloaded jars.

Checks that every required manifest entry has a present file whose SHA-512 matches
`pack/mods.json`, that each jar declares a `modId`, and that every required mod
dependency (other than Minecraft/NeoForge itself) is satisfied by another pinned mod.

Version ranges are reported but not evaluated: NeoForge's Maven version ordering is
not reproduced here, so a satisfied modId is not proof that its range matches.
Network access is not used.
"""
import argparse
import hashlib
import json
import sys
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "pack" / "mods.json"
LOADER_IDS = {"minecraft", "neoforge", "forge", "fml"}


def sha512(path: Path) -> str:
    digest = hashlib.sha512()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def mod_metadata(path: Path) -> tuple[str, str, list[tuple[str, str]]]:
    with zipfile.ZipFile(path) as jar:
        name = next((n for n in jar.namelist()
                     if n in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml")), None)
        if name is None:
            raise ValueError("no META-INF mods.toml")
        data = tomllib.loads(jar.read(name).decode("utf-8", "replace"))
    mods = data.get("mods") or []
    if not mods:
        raise ValueError("mods.toml declares no [[mods]] block")
    mod_id = mods[0].get("modId", "")
    version = mods[0].get("version", "")
    required = []
    for dep in data.get("dependencies", {}).get(mod_id, []) or []:
        if dep.get("type") == "required" and dep.get("modId") not in LOADER_IDS:
            required.append((dep.get("modId", ""), dep.get("versionRange", "")))
    return mod_id, version, required


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mods", type=Path, default=ROOT / "pack" / "mods",
                        help="directory holding the downloaded jars")
    args = parser.parse_args()

    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    problems: list[str] = []
    notes: list[str] = []
    present: dict[str, str] = {}
    required_deps: list[tuple[str, str, str]] = []

    for entry in manifest["mods"]:
        path = args.mods / entry["filename"]
        label = f"{entry['name']} ({entry['filename']})"
        if not path.exists():
            if entry.get("required"):
                problems.append(f"missing required file: {label}")
            else:
                notes.append(f"optional mod not downloaded: {entry['name']}")
            continue
        actual = sha512(path)
        if actual != entry["sha512"]:
            problems.append(f"sha512 mismatch: {label}")
        try:
            mod_id, version, deps = mod_metadata(path)
        except (ValueError, KeyError, tomllib.TOMLDecodeError) as error:
            problems.append(f"unreadable metadata: {label}: {error}")
            continue
        if not mod_id:
            problems.append(f"empty modId: {label}")
        present[mod_id] = version
        required_deps.extend((entry["name"], dep_id, rng) for dep_id, rng in deps)
        if entry["version"] not in version and version not in entry["version"]:
            notes.append(f"jar metadata version differs from manifest: {entry['name']}: "
                         f"manifest {entry['version']}, jar {version}")

    for owner, dep_id, rng in required_deps:
        if dep_id not in present:
            problems.append(f"unresolved required dependency: {owner} needs {dep_id} {rng}")
        elif rng:
            notes.append(f"dependency present, range unchecked: {owner} needs {dep_id} {rng}")

    print(f"manifest entries: {len(manifest['mods'])} | jars validated: {len(present)} | "
          f"non-loader required dependencies: {len(required_deps)}")
    for note in notes:
        print(f"note: {note}")
    for problem in problems:
        print(f"ERROR: {problem}")
    if problems:
        return 1
    print("OK: pinned files, hashes and required dependency closure validated offline")
    return 0


if __name__ == "__main__":
    sys.exit(main())
