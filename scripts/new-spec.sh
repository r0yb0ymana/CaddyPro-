#!/bin/bash
# Create a new spec from template

set -e

if [ -z "$1" ]; then
  echo "Usage: ./scripts/new-spec.sh <feature-name>"
  echo "Example: ./scripts/new-spec.sh user-auth"
  exit 1
fi

FEATURE_NAME="$1"
SPEC_FILE="specs/${FEATURE_NAME}.md"

if [ -f "$SPEC_FILE" ]; then
  echo "Error: Spec already exists at $SPEC_FILE"
  exit 1
fi

# Copy template
cp specs/feature-template.md "$SPEC_FILE"

# Replace placeholder in title
sed -i.bak "s/<name>/${FEATURE_NAME}/g" "$SPEC_FILE" && rm "${SPEC_FILE}.bak"

echo "✓ Created new spec at: $SPEC_FILE"
echo ""
echo "Next steps:"
echo "  1. Edit $SPEC_FILE with your feature details"
echo "  2. Run: claude /spec $SPEC_FILE"
echo "  3. Once spec is complete, run: claude /plan $SPEC_FILE"
