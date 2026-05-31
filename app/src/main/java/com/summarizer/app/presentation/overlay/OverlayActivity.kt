package com.summarizer.app.presentation.overlay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.summarizer.app.core.designsystem.YouTubeSummarizerTheme
import com.summarizer.app.presentation.MainActivity

/**
 * Android Share Target Activity.
 * Intercepts plain text share intents containing YouTube / Vanced URLs,
 * launching an overlay Bottom Sheet to display quick bulleted summaries on-device.
 */
class OverlayActivity : ComponentActivity() {

    // Lazy load the Overlay ViewModel using our ServiceLocator factory
    private val viewModel: OverlayViewModel by viewModels {
        OverlayViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture shared text block from Android's system share sheet
        val sharedText = handleSharedIntent(intent)

        setContent {
            YouTubeSummarizerTheme {
                OverlayScreen(
                    viewModel = viewModel,
                    onDismiss = {
                        // Smoothly close the activity and return to YouTube
                        finish()
                    },
                    onOpenMainApp = {
                        // Onboarding redirection: Launch the configurations panel
                        val mainIntent = Intent(this@OverlayActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }

        // Trigger summarization pipeline asynchronously
        val forceRegenerate = intent.getBooleanExtra("FORCE_REGENERATE", false)
        viewModel.loadSummary(sharedText, forceRegenerate)
    }

    /**
     * Extracts shared plain text payload from standard SEND intents.
     */
    private fun handleSharedIntent(intent: Intent?): String? {
        if (intent == null) return null
        return if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }
    }
}
