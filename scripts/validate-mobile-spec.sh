#!/bin/bash
# Validate that a mobile spec file is complete

set -e

if [ -z "$1" ]; then
  echo "Usage: ./scripts/validate-mobile-spec.sh <spec-file>"
  echo "Example: ./scripts/validate-mobile-spec.sh specs/login-screen.md"
  exit 1
fi

SPEC_FILE="$1"

if [ ! -f "$SPEC_FILE" ]; then
  echo "Error: Spec file not found: $SPEC_FILE"
  exit 1
fi

echo "Validating mobile spec: $SPEC_FILE"
echo ""

ERRORS=0
WARNINGS=0

# Check for required sections
check_section() {
  local section="$1"
  if ! grep -q "^## $section" "$SPEC_FILE"; then
    echo "✗ Missing section: $section"
    ERRORS=$((ERRORS + 1))
  else
    echo "✓ Found section: $section"
  fi
}

check_section "1. Problem statement"
check_section "2. Goals"
check_section "3. Non-goals"
check_section "4. Functional requirements"
check_section "5. Acceptance criteria"
check_section "6. Constraints"

# Check for placeholders
if grep -q "<name>" "$SPEC_FILE"; then
  echo "✗ Found unreplaced placeholder: <name>"
  ERRORS=$((ERRORS + 1))
fi

if grep -q "<Screen" "$SPEC_FILE" || grep -q "<Component" "$SPEC_FILE" || grep -q "<Flow" "$SPEC_FILE"; then
  echo "✗ Found unreplaced template placeholders"
  ERRORS=$((ERRORS + 1))
fi

# Check that acceptance criteria exist
AC_COUNT=$(grep -c "^### A[0-9]" "$SPEC_FILE" 2>/dev/null || echo "0")
if [ "$AC_COUNT" -eq 0 ]; then
  # Also check for - A1: format
  AC_COUNT=$(grep -c "^- A[0-9]" "$SPEC_FILE" 2>/dev/null || echo "0")
fi

if [ "$AC_COUNT" -eq 0 ]; then
  echo "✗ No acceptance criteria found (expected format: ### A1: or - A1:)"
  ERRORS=$((ERRORS + 1))
else
  echo "✓ Found $AC_COUNT acceptance criteria"
fi

# Check for Given-When-Then in acceptance criteria
GWT_COUNT=$(grep -ci "GIVEN:\|WHEN:\|THEN:" "$SPEC_FILE" 2>/dev/null || echo "0")
if [ "$GWT_COUNT" -eq 0 ]; then
  echo "⚠ No Given-When-Then format found in acceptance criteria"
  echo "  (Recommended for testable criteria)"
  WARNINGS=$((WARNINGS + 1))
else
  echo "✓ Found Given-When-Then format ($GWT_COUNT occurrences)"
fi

# Mobile-specific checks
echo ""
echo "Mobile-specific checks:"

# Check for platform sections
ANDROID_COUNT=$(grep -ci "android\|kotlin\|compose\|material" "$SPEC_FILE" 2>/dev/null || echo "0")
IOS_COUNT=$(grep -ci "ios\|swift\|swiftui\|apple\|iphone" "$SPEC_FILE" 2>/dev/null || echo "0")

if [ "$ANDROID_COUNT" -gt 0 ]; then
  echo "✓ Android-related content found"
else
  echo "⚠ No Android-specific content found"
  WARNINGS=$((WARNINGS + 1))
fi

if [ "$IOS_COUNT" -gt 0 ]; then
  echo "✓ iOS-related content found"
else
  echo "⚠ No iOS-specific content found"
  WARNINGS=$((WARNINGS + 1))
fi

# Check for accessibility section or mentions
A11Y_COUNT=$(grep -ci "accessibility\|voiceover\|talkback\|screen reader\|a11y" "$SPEC_FILE" 2>/dev/null || echo "0")
if [ "$A11Y_COUNT" -gt 0 ]; then
  echo "✓ Accessibility requirements mentioned ($A11Y_COUNT occurrences)"
else
  echo "⚠ No accessibility requirements found"
  echo "  (Recommended: Add accessibility acceptance criteria)"
  WARNINGS=$((WARNINGS + 1))
fi

# Check for touch target mentions
TOUCH_COUNT=$(grep -ci "touch target\|44pt\|48dp\|tap target" "$SPEC_FILE" 2>/dev/null || echo "0")
if [ "$TOUCH_COUNT" -gt 0 ]; then
  echo "✓ Touch target requirements found"
fi

# Check for data model section
if grep -q "^## [0-9]*\. Data Model\|^## Data Model\|^### .*State" "$SPEC_FILE"; then
  echo "✓ Data model section found"
else
  echo "⚠ No data model section found"
  echo "  (Recommended for screens with state)"
  WARNINGS=$((WARNINGS + 1))
fi

# Check for API contract if networking is involved
if grep -qi "api\|endpoint\|request\|response" "$SPEC_FILE"; then
  if grep -q '```json' "$SPEC_FILE" || grep -q "API Contract\|Request\|Response" "$SPEC_FILE"; then
    echo "✓ API contract documented"
  else
    echo "⚠ API mentioned but no contract documented"
    echo "  (Recommended: Document request/response formats)"
    WARNINGS=$((WARNINGS + 1))
  fi
fi

# Summary
echo ""
echo "─────────────────────────────────"
if [ $ERRORS -eq 0 ]; then
  if [ $WARNINGS -eq 0 ]; then
    echo "✓ Spec validation passed!"
  else
    echo "✓ Spec validation passed with $WARNINGS warning(s)"
  fi
  echo ""
  echo "Next step: claude /plan $SPEC_FILE"
  exit 0
else
  echo "✗ Spec validation failed with $ERRORS error(s) and $WARNINGS warning(s)"
  echo ""
  echo "Fix the errors above, then run: claude /spec $SPEC_FILE"
  exit 1
fi
