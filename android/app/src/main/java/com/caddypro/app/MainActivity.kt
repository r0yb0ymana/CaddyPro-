package com.caddypro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.caddypro.app.domain.usecase.HasProfileUseCase
import com.caddypro.app.ui.navigation.CaddyProNavigation
import com.caddypro.app.ui.navigation.Screen
import com.caddypro.app.ui.theme.CaddyProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Main Activity for CaddyPro
 *
 * Single-activity architecture using Jetpack Compose and Navigation Compose.
 * Implements first-launch detection to route to ProfileSetup or BagList.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var hasProfileUseCase: HasProfileUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CaddyProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    // First-launch detection
                    LaunchedEffect(Unit) {
                        startDestination = withContext(Dispatchers.IO) {
                            if (hasProfileUseCase()) {
                                Screen.BagList.route
                            } else {
                                Screen.ProfileSetup.route
                            }
                        }
                    }

                    // Show loading while determining start destination
                    if (startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val navController = rememberNavController()
                        CaddyProNavigation(
                            navController = navController,
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }
}
