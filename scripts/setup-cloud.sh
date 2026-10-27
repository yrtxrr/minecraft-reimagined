#!/usr/bin/env bash
set -euo pipefail
cd /workspace/minecraft-reimagined
mkdir -p /workspace/.tools /workspace/.gradle
if [[ ! -x /workspace/.tools/jdk-21.0.8/bin/javac ]]; then
    case "$(uname -m)" in x86_64) ;; *) echo 'This cloud bootstrap requires x86_64; install JDK 21 for your architecture.' >&2; exit 1;; esac
    reimagined_jdk_archive=$(mktemp /tmp/reimagined-jdk.XXXXXX.tar.gz)
    trap 'rm -f "$reimagined_jdk_archive"' EXIT
    curl --fail --location --retry 2 https://download.oracle.com/java/21/archive/jdk-21.0.8_linux-x64_bin.tar.gz -o "$reimagined_jdk_archive"
    printf '%s  %s\n' 'd87272944278713fc7a120cf024d2818d136b5debc750aa17045e3c6f045b867' "$reimagined_jdk_archive" | sha256sum --check --status
    tar -xzf "$reimagined_jdk_archive" -C /workspace/.tools
fi
# Preserve unrelated local Gradle settings. Never put credential values in setup files.
python3 - <<'PY'
from pathlib import Path
import os, urllib.parse
path = Path('/workspace/.gradle/gradle.properties')
updates = {'org.gradle.java.installations.paths': '/workspace/.tools/jdk-21.0.8'}
if Path('/etc/ssl/certs/java/cacerts').is_file():
    updates['systemProp.javax.net.ssl.trustStore'] = '/etc/ssl/certs/java/cacerts'
proxy = os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY')
if proxy:
    u = urllib.parse.urlsplit(proxy)
    if u.username or u.password:
        raise SystemExit('Authenticated proxy requires platform-supported Java configuration')
    for scheme in ('http', 'https'):
        updates[f'systemProp.{scheme}.proxyHost'] = u.hostname
        updates[f'systemProp.{scheme}.proxyPort'] = str(u.port or 80)
lines = path.read_text().splitlines() if path.exists() else []
lines = [line for line in lines if line.split('=', 1)[0] not in updates]
path.write_text('\n'.join(lines + [f'{key}={value}' for key, value in updates.items()]) + '\n')
PY
source scripts/cloud-shell.sh
./gradlew --no-daemon build
