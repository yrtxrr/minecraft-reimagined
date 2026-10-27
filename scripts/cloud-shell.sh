#!/usr/bin/env bash
# Usage: source scripts/cloud-shell.sh
# Keep cloud caches writable; the host may expose only a JRE and a read-only home.
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
if [[ -x /workspace/.tools/jdk-21.0.8/bin/javac ]]; then
    export JAVA_HOME=/workspace/.tools/jdk-21.0.8
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -f /etc/ssl/certs/java/cacerts ]]; then
    export JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
# Java does not automatically use HTTPS_PROXY; forward only an unauthenticated host and port.
read -r reimagined_proxy_host reimagined_proxy_port < <(python3 - <<'PYPROXY'
import os, urllib.parse
value = os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY')
if value:
    parsed = urllib.parse.urlsplit(value)
    if parsed.username or parsed.password:
        raise SystemExit('Authenticated proxy needs platform-supported Java configuration')
    print(parsed.hostname, parsed.port or 80)
else:
    print()
PYPROXY
)
if [[ -n "$reimagined_proxy_host" ]]; then
    export JAVA_OPTS="${JAVA_OPTS:-} -Dhttp.proxyHost=$reimagined_proxy_host -Dhttp.proxyPort=$reimagined_proxy_port -Dhttps.proxyHost=$reimagined_proxy_host -Dhttps.proxyPort=$reimagined_proxy_port"
fi
unset reimagined_proxy_host reimagined_proxy_port
