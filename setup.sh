#!/bin/bash

# setup.sh - Interactive setup script for the repository
#
# This script helps new users configure the repository for their project.
# It handles renaming package/bundle identifiers, setting up API keys,
# and initializing the development environment.

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}   Claude-in-the-Loop: Project Setup Helper      ${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

# Function to prompt user
prompt_user() {
    read -p "$1: " response
    echo "$response"
}

# Function to replace text in files
replace_in_files() {
    local search="$1"
    local replace="$2"
    local file_pattern="$3"
    
    echo -e "${YELLOW}Replacing '$search' with '$replace' in $file_pattern...${NC}"
    
    # Use find and sed to replace text
    # macOS requires an empty string for the -i flag
    if [[ "$OSTYPE" == "darwin"* ]]; then
        find . -name "$file_pattern" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/node_modules/*" -type f -exec sed -i '' "s|$search|$replace|g" {} +
    else
        find . -name "$file_pattern" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/node_modules/*" -type f -exec sed -i "s|$search|$replace|g" {} +
    fi
}

# 1. Project Naming
echo -e "${GREEN}Step 1: Project Naming${NC}"
PROJECT_NAME=$(prompt_user "Enter your project name (e.g., MyApp)")
PACKAGE_NAME=$(prompt_user "Enter your Android package name (e.g., com.example.myapp)")
BUNDLE_ID=$(prompt_user "Enter your iOS bundle identifier (e.g., com.example.myapp)")

if [ -n "$PROJECT_NAME" ]; then
    # Update README title
    sed -i '' "s/# claude-in-the-loop/# $PROJECT_NAME/" README.md
    
    # Update iOS project name in project.yml
    sed -i '' "s/name: App/name: $PROJECT_NAME/" ios/project.yml
fi

if [ -n "$PACKAGE_NAME" ]; then
    # Update Android package name in build.gradle.kts
    replace_in_files "com.example.app" "$PACKAGE_NAME" "*.gradle.kts"
    replace_in_files "com.example.app" "$PACKAGE_NAME" "*.kt"
    replace_in_files "com.example.app" "$PACKAGE_NAME" "*.xml"
    
    echo -e "${YELLOW}Note: You may need to manually move Android source files to match the new package structure.${NC}"
fi

if [ -n "$BUNDLE_ID" ]; then
    # Update iOS bundle identifier
    replace_in_files "com.example.app" "$BUNDLE_ID" "project.yml"
    replace_in_files "com.example.app" "$BUNDLE_ID" "*.swift"
fi

# 2. Environment Setup
echo -e "\n${GREEN}Step 2: Environment Setup${NC}"
API_URL=$(prompt_user "Enter your development API base URL (optional)")

if [ -n "$API_URL" ]; then
    replace_in_files "https://api.dev.example.com/" "$API_URL" "*.gradle.kts"
    replace_in_files "https://api.dev.example.com/" "$API_URL" "*.swift"
fi

# 3. Tool Installation
echo -e "\n${GREEN}Step 3: Development Tools${NC}"
echo "Checking for required tools..."

if ! command -v brew &> /dev/null; then
    echo -e "${RED}Homebrew is not installed. Please install it first.${NC}"
else
    if ! command -v pre-commit &> /dev/null; then
        echo -e "${YELLOW}Installing pre-commit...${NC}"
        brew install pre-commit
    fi
    
    if ! command -v xcodegen &> /dev/null; then
        echo -e "${YELLOW}Installing XcodeGen...${NC}"
        brew install xcodegen
    fi
    
    if ! command -v ktlint &> /dev/null; then
         echo -e "${YELLOW}Installing ktlint...${NC}"
         brew install ktlint
    fi

     if ! command -v swiftlint &> /dev/null; then
         echo -e "${YELLOW}Installing swiftlint...${NC}"
         brew install swiftlint
    fi

    if ! command -v swiftformat &> /dev/null; then
         echo -e "${YELLOW}Installing swiftformat...${NC}"
         brew install swiftformat
    fi

    if ! command -v fastlane &> /dev/null; then
         echo -e "${YELLOW}Installing fastlane...${NC}"
         brew install fastlane
    fi
fi

# 4. Initialize Project
echo -e "\n${GREEN}Step 4: Initializing Project${NC}"

# Install pre-commit hooks
if command -v pre-commit &> /dev/null; then
    echo "Installing pre-commit hooks..."
    pre-commit install
fi

# Generate iOS project
if [ -d "ios" ] && command -v xcodegen &> /dev/null; then
    echo "Generating iOS project..."
    cd ios
    xcodegen generate
    cd ..
fi

# 5. Check for Production Configs
echo -e "\n${GREEN}Step 5: Production Readiness Checks${NC}"
if [ ! -f "android/app/google-services.json" ]; then
    echo -e "${YELLOW}⚠ Missing android/app/google-services.json${NC}"
    echo "  Required for Firebase/Crashlytics. Download from Firebase Console."
fi

if [ ! -f "ios/App/GoogleService-Info.plist" ]; then
    echo -e "${YELLOW}⚠ Missing ios/App/GoogleService-Info.plist${NC}"
    echo "  Required for Firebase/Crashlytics. Download from Firebase Console."
fi

echo -e "\n${BLUE}=================================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""
echo "Next steps:"
echo "1. Verify the Android package structure matches your new package name."
echo "2. Open the Android project in Android Studio."
echo "3. Open the iOS project in Xcode (ios/$PROJECT_NAME.xcodeproj)."
echo "4. Create your first spec using ./scripts/new-spec.sh"
echo ""
echo "Happy coding!"
