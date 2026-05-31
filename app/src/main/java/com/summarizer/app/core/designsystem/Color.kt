package com.summarizer.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Premium, minimal color tokens matching modern SaaS/startup styling.
 * Avoids pure harsh blacks and hyper-saturated primaries to maximize visual comfort.
 */

// --- DARK SCHEME COLORS ---
val DarkBackground = Color(0xFF000000)      // Pure Black
val DarkSurface = Color(0xFF121212)         // Deep dark gray card surface
val DarkSurfaceVariant = Color(0xFF272727)  // Medium dark gray border
val DarkPrimary = Color(0xFFFFFFFF)         // White minimal accent (replaces purple)
val DarkOnPrimary = Color(0xFF000000)       // Black text on white primary
val DarkSecondary = Color(0xFF333333)       // Subtle gray highlight
val DarkOnBackground = Color(0xFFFFFFFF)    // Pure white text primary
val DarkOnSurface = Color(0xFFEEEEEE)       // Soft white secondary text
val DarkOnSurfaceVariant = Color(0xFFAAAAAA) // Muted gray secondary text

// --- LIGHT SCHEME COLORS ---
val LightBackground = Color(0xFFF8FAFC)     // Crisp Soft Grey-White
val LightSurface = Color(0xFFFFFFFF)        // Pure White card surface
val LightSurfaceVariant = Color(0xFFE2E8F0) // Soft border line grey
val LightPrimary = Color(0xFF4F46E5)         // Deep vibrant Indigo
val LightOnPrimary = Color(0xFFFFFFFF)       // White text on indigo primary
val LightSecondary = Color(0xFF0F766E)       // Darker Sage Teal
val LightOnBackground = Color(0xFF0F172A)    // Deep Navy-Black text primary
val LightOnSurface = Color(0xFF1E293B)       // Soft Navy text secondary
val LightOnSurfaceVariant = Color(0xFF64748B)// Slate grey secondary text

// --- EXTRA UI COLORS ---
val ShimmerStart = Color(0xFF334155).copy(alpha = 0.6f)
val ShimmerEnd = Color(0xFF475569).copy(alpha = 0.3f)
val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val CardBorderColor = Color(0xFFFFFFFF).copy(alpha = 0.05f)
