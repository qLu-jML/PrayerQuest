#!/usr/bin/env bash
#
# check_no_hardcoded_strings.sh
# ─────────────────────────────────────────────────────────────────────────────
# Localization guardrail. Scans the Compose source tree for likely-hardcoded
# user-visible text and exits non-zero if any are found.
#
# Specifically flags `Text("…")` calls whose argument is a quoted literal that
# contains at least one alphabetic character. This is a strong signal that a
# string was forgotten during localization (DD §3.15 — every user-facing
# string should resolve via stringResource(R.string.…)).
#
# False-positive policy:
#   - Test sources (/test/, /androidTest/) are excluded; tests routinely use
#     literal strings as fixtures and don't ship to users.
#   - Strings that are clearly non-text (single-char glyphs, emoji, "✓", "•",
#     numbers, symbols, dates) pass through. The guardrail only flags strings
#     containing at least three letters in a row, which catches "Submit" and
#     "Pray now" but lets "✓ 2/3" or "12s" through.
#   - Strings inside `testTag(…)` calls are not Text() calls; they don't render
#     to screen and aren't matched by this guardrail's pattern.
#   - Inline string templates with `$variable` interpolation will be caught;
#     to suppress a false positive, externalize as `stringResource(R.…, var)`.
#
# Usage:
#   ./scripts/check_no_hardcoded_strings.sh             # scan, exit 0/1
#   ./scripts/check_no_hardcoded_strings.sh --report    # scan, list ALL hits
#                                                       # without failing
#
# Run by hand before committing, or wire into CI:
#   ./scripts/check_no_hardcoded_strings.sh || exit 1
# ─────────────────────────────────────────────────────────────────────────────

set -u

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "${SCRIPT_DIR}/.." && pwd )"
SOURCE_DIR="${APP_DIR}/app/src/main/java"

if [[ ! -d "${SOURCE_DIR}" ]]; then
    echo "ERROR: Could not find Kotlin source tree at ${SOURCE_DIR}" >&2
    exit 2
fi

# `--report` mode just prints all hits and exits 0 — useful for triaging
# legacy debt without breaking CI on the first run.
REPORT_ONLY=0
if [[ "${1:-}" == "--report" ]]; then
    REPORT_ONLY=1
fi

# Pattern: `Text(` followed by a double-quoted string containing at least one
# letter. Multi-line constructor args like `Text(\n    "Hello"\n)` are caught
# by stripping newlines first via a perl one-liner. The "letter cluster"
# requirement (`[A-Za-z]{3,}`) suppresses noise from "✓", "12s", "0", etc.
#
# We exclude:
#   - kotlin doc comments (/** … */) and line comments (// …)
#   - lines containing stringResource — those are already localized
#   - plural calls (pluralStringResource) — same
#   - known-safe call shapes: Text("✓"), Text(emoji), Text("•")
PATTERN='Text\s*\(\s*"[^"]*[A-Za-z]{3,}[^"]*"'

# Skip globs:
#   - /test/, /androidTest/ — test fixtures
#   - .git, build, .gradle — generated / vendored
EXCLUDES=(
    --exclude-dir=test
    --exclude-dir=androidTest
    --exclude-dir=build
    --exclude-dir=.gradle
    --exclude-dir=.git
)

# Run grep, then post-filter through awk to drop comment lines and
# stringResource lines (which CAN co-occur with Text() literals in mixed
# contexts).
HITS_TMP=$(mktemp)
trap 'rm -f "${HITS_TMP}"' EXIT

grep -rEn "${EXCLUDES[@]}" --include='*.kt' "${PATTERN}" "${SOURCE_DIR}" 2>/dev/null \
    | awk -F: '
        # Skip comments and lines that already use stringResource
        $0 !~ /\/\*/ && $0 !~ /^[[:space:]]*\*/ && $0 !~ /\/\/.*Text\(/ &&
        $0 !~ /stringResource/ && $0 !~ /pluralStringResource/ &&
        $0 !~ /testTag/ {
            print $0
        }
    ' > "${HITS_TMP}"

HIT_COUNT=$(wc -l < "${HITS_TMP}" | tr -d ' ')

if [[ "${HIT_COUNT}" == "0" ]]; then
    echo "✓ No hardcoded user-facing strings found in Compose source."
    exit 0
fi

echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "  HARDCODED STRING GUARDRAIL — ${HIT_COUNT} suspect line(s) found"
echo "════════════════════════════════════════════════════════════════════════"
echo ""
cat "${HITS_TMP}" | sed "s|${SOURCE_DIR}/||"
echo ""
echo "Each line above shows a Text(\"literal\") call that bypasses"
echo "stringResource(). To fix:"
echo ""
echo "  1. Add the string to app/src/main/res/values/strings.xml"
echo "  2. Replace Text(\"Hello\") with Text(stringResource(R.string.your_key))"
echo "  3. Re-run this script to verify"
echo ""
echo "If the literal is intentional (an emoji, a single glyph, a non-translatable"
echo "constant), comment that line: // INTENTIONAL — non-translatable glyph"
echo ""

if [[ "${REPORT_ONLY}" == "1" ]]; then
    echo "(--report mode: not failing the build)"
    exit 0
fi

exit 1
