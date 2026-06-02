package com.summarizer.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.summarizer.app.core.designsystem.YouTubeSummarizerTheme
import com.summarizer.app.presentation.history.HistoryScreen
import com.summarizer.app.presentation.history.HistoryViewModel
import com.summarizer.app.presentation.settings.SettingsScreen
import com.summarizer.app.presentation.settings.SettingsViewModel

/**
 * Navigation screen routes.
 */
enum class Screen {
    HISTORY,
    SETTINGS
}

/**
 * Main Launcher Activity managing global screens navigation state.
 */
class MainActivity : ComponentActivity() {

    // Lazy load ViewModels using our clean Factory mappings
    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModel.Factory(applicationContext)
    }
    
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Lightweight enum-based navigation backstack
            var currentScreen by remember { mutableStateOf(Screen.HISTORY) }

            YouTubeSummarizerTheme {
                if (currentScreen == Screen.SETTINGS) {
                    BackHandler {
                        settingsViewModel.loadSettings()
                        currentScreen = Screen.HISTORY
                    }
                }

                when (currentScreen) {
                    Screen.HISTORY -> {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onSettingsClick = { currentScreen = Screen.SETTINGS }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBackClick = { 
                                // Refresh configurations (like prompt/keys) when backing into history screen
                                settingsViewModel.loadSettings()
                                currentScreen = Screen.HISTORY 
                            }
                        )
                    }
                }
            }
        }
    }
}
