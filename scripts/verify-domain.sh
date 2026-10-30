#!/usr/bin/env bash
# Runs actual gameplay-rule tests without Minecraft artifacts. Not a substitute for Gradle/server checks.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:=/workspace/.tools/jdk-21.0.8}"
test_java="$JAVA_HOME/bin/java"
test_javac="$JAVA_HOME/bin/javac"
test_tools="${REIMAGINED_TEST_TOOLS:-/workspace/.tools/testing}"
mkdir -p "$test_tools" build/domain-tests/classes build/domain-tests/results
junit="$test_tools/junit-platform-console-standalone-1.11.4.jar"
if [[ ! -f "$junit" ]]; then
    curl --fail --location --retry 2 https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar -o "$junit"
fi
printf '%s  %s\n' 'b016ef6b1c3454d6d7c2c88ce081dabf289699686af6622d6e4e2e1b54b4a2fc' "$junit" | sha256sum --check --status
# Remove stale compiled classes so deleted tests cannot be reported as current results.
rm -rf build/domain-tests/classes build/domain-tests/results
mkdir -p build/domain-tests/classes build/domain-tests/results
"$test_javac" --release 21 -cp "$junit" -d build/domain-tests/classes \
  src/main/java/dev/reimagined/season/SeasonCalendar.java \
  src/main/java/dev/reimagined/civilization/SettlementType.java \
  src/main/java/dev/reimagined/civilization/TechEra.java \
  src/main/java/dev/reimagined/civilization/SettlementEconomy.java \
  src/main/java/dev/reimagined/progression/Skill.java \
  src/main/java/dev/reimagined/progression/SkillProgress.java \
  src/main/java/dev/reimagined/content/ManaRules.java \
  src/main/java/dev/reimagined/engineering/Rotation.java \
  src/test/java/dev/reimagined/season/SeasonCalendarTest.java \
  src/test/java/dev/reimagined/civilization/SettlementEconomyTest.java \
  src/test/java/dev/reimagined/progression/SkillProgressTest.java \
  src/test/java/dev/reimagined/content/ManaRulesTest.java \
  src/test/java/dev/reimagined/engineering/RotationTest.java
"$test_java" -jar "$junit" execute --class-path build/domain-tests/classes \
  --scan-class-path --fail-if-no-tests --reports-dir build/domain-tests/results
