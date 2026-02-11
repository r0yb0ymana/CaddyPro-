#!/bin/bash

# scaffold-feature.sh - Generates feature boilerplate for Android and iOS

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ -z "$1" ]; then
    echo "Usage: $0 <FeatureName>"
    echo "Example: $0 UserProfile"
    exit 1
fi

FEATURE_NAME=$1
# Capitalize first letter just in case
FEATURE_NAME="$(tr '[:lower:]' '[:upper:]' <<< ${FEATURE_NAME:0:1})${FEATURE_NAME:1}"
# Lowercase for packages
FEATURE_LOWER="$(tr '[:upper:]' '[:lower:]' <<< $FEATURE_NAME)"

echo -e "${BLUE}Scaffolding feature: ${FEATURE_NAME}${NC}"

# ==========================================
# Android Setup
# ==========================================
echo -e "${GREEN}Generating Android boilerplate...${NC}"
ANDROID_BASE="android/app/src/main/java/com/example/app"
# Adjust base path if user has renamed package (simple heuristic: look for App.kt)
APP_FILE=$(find android/app/src/main/java -name "App.kt" | head -n 1)
if [ -n "$APP_FILE" ]; then
    ANDROID_BASE=$(dirname "$APP_FILE")
fi

ANDROID_UI_DIR="$ANDROID_BASE/ui/screens/$FEATURE_LOWER"
ANDROID_DOMAIN_DIR="$ANDROID_BASE/domain/usecase/$FEATURE_LOWER"
ANDROID_DATA_DIR="$ANDROID_BASE/data/repository/$FEATURE_LOWER"

mkdir -p "$ANDROID_UI_DIR"
mkdir -p "$ANDROID_DOMAIN_DIR"
mkdir -p "$ANDROID_DATA_DIR"

# ViewModel
cat > "$ANDROID_UI_DIR/${FEATURE_NAME}ViewModel.kt" <<EOF
package $(grep "package " "$APP_FILE" | cut -d' ' -f2).ui.screens.$FEATURE_LOWER

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ${FEATURE_NAME}ViewModel @Inject constructor(
    // private val useCase: Get${FEATURE_NAME}UseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(${FEATURE_NAME}State())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: ${FEATURE_NAME}Action) {
        when (action) {
            is ${FEATURE_NAME}Action.Load -> loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: Call use case
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
EOF

# State
cat > "$ANDROID_UI_DIR/${FEATURE_NAME}State.kt" <<EOF
package $(grep "package " "$APP_FILE" | cut -d' ' -f2).ui.screens.$FEATURE_LOWER

data class ${FEATURE_NAME}State(
    val isLoading: Boolean = false,
    val error: String? = null
)
EOF

# Action
cat > "$ANDROID_UI_DIR/${FEATURE_NAME}Action.kt" <<EOF
package $(grep "package " "$APP_FILE" | cut -d' ' -f2).ui.screens.$FEATURE_LOWER

sealed interface ${FEATURE_NAME}Action {
    data object Load : ${FEATURE_NAME}Action
}
EOF

# Screen
cat > "$ANDROID_UI_DIR/${FEATURE_NAME}Screen.kt" <<EOF
package $(grep "package " "$APP_FILE" | cut -d' ' -f2).ui.screens.$FEATURE_LOWER

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ${FEATURE_NAME}Screen(
    viewModel: ${FEATURE_NAME}ViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(${FEATURE_NAME}Action.Load)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$FEATURE_NAME Screen")
    }
}
EOF

# ==========================================
# iOS Setup
# ==========================================
echo -e "${GREEN}Generating iOS boilerplate...${NC}"
IOS_BASE="ios/App/Sources"
IOS_UI_DIR="$IOS_BASE/UI/Screens/$FEATURE_NAME"
IOS_DOMAIN_DIR="$IOS_BASE/Core/Domain/UseCases/$FEATURE_NAME"

mkdir -p "$IOS_UI_DIR"
mkdir -p "$IOS_DOMAIN_DIR"

# ViewModel
cat > "$IOS_UI_DIR/${FEATURE_NAME}ViewModel.swift" <<EOF
import SwiftUI
import Observation

@Observable
@MainActor
final class ${FEATURE_NAME}ViewModel {
    var state = ${FEATURE_NAME}State()
    
    // private let useCase: ${FEATURE_NAME}UseCase
    
    init() {
        // self.useCase = ...
    }
    
    func handle(_ action: ${FEATURE_NAME}Action) {
        switch action {
        case .load:
            loadData()
        }
    }
    
    private func loadData() {
        state.isLoading = true
        Task {
            // TODO: Call use case
            state.isLoading = false
        }
    }
}
EOF

# Models
cat > "$IOS_UI_DIR/${FEATURE_NAME}Models.swift" <<EOF
import Foundation

struct ${FEATURE_NAME}State {
    var isLoading = false
    var error: String?
}

enum ${FEATURE_NAME}Action {
    case load
}
EOF

# View
cat > "$IOS_UI_DIR/${FEATURE_NAME}View.swift" <<EOF
import SwiftUI

struct ${FEATURE_NAME}View: View {
    @State private var viewModel = ${FEATURE_NAME}ViewModel()
    
    var body: some View {
        VStack {
            if viewModel.state.isLoading {
                ProgressView()
            } else {
                Text("$FEATURE_NAME View")
            }
        }
        .task {
            viewModel.handle(.load)
        }
        .navigationTitle("$FEATURE_NAME")
    }
}

#Preview {
    ${FEATURE_NAME}View()
}
EOF

echo -e "${BLUE}Done! Created files for feature '$FEATURE_NAME'.${NC}"
echo "Android: $ANDROID_UI_DIR"
echo "iOS: $IOS_UI_DIR"
