package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Theme Modes
enum class AppThemeMode(val title: String, val icon: String) {
    SYSTEM("Sistem Varsayılanı", "⚙️"),
    LIGHT("Açık Tema", "☀️"),
    DARK("Koyu Tema", "🌙"),
    AMOLED("AMOLED Siyah", "🖤")
}

// Brand Vitality Primary Accents
val VitalTealPrimary = Color(0xFF0D9488)
val VitalTealDark = Color(0xFF14B8A6)
val VitalTealLight = Color(0xFF0F766E)
val VitalTealAmoled = Color(0xFF2DD4BF)

// Metric-Specific Colors (Universal & Consistent Across Themes)
val ColorSteps = Color(0xFF10B981)        // Emerald
val ColorHeartRate = Color(0xFFF43F5E)    // Rose Coral
val ColorSleep = Color(0xFF8B5CF6)        // Indigo Violet
val ColorSpO2 = Color(0xFF0EA5E9)         // Sky Oxygen Blue
val ColorStress = Color(0xFFF59E0B)       // Warm Amber
val ColorCalories = Color(0xFFFF7A00)     // Energetic Orange
val ColorDistance = Color(0xFF06B6D4)     // Cyan
val ColorWater = Color(0xFF38BDF8)        // Light Blue Water

// Rule Engine Status Colors
val StatusGoodGreen = Color(0xFF16A34A)
val StatusAttentionYellow = Color(0xFFD97706)
val StatusCriticalRed = Color(0xFFDC2626)
val StatusOptimalTeal = Color(0xFF0D9488)
val StatusLowBlue = Color(0xFF2563EB)

// Light Theme Palette
val LightPrimary = Color(0xFF0D9488)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFCCFBF1)
val LightOnPrimaryContainer = Color(0xFF115E59)
val LightSecondary = Color(0xFF475569)
val LightSecondaryContainer = Color(0xFFF1F5F9)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightOutline = Color(0xFFE2E8F0)

// Dark Theme Palette
val DarkPrimary = Color(0xFF2DD4BF)
val DarkOnPrimary = Color(0xFF042F2E)
val DarkPrimaryContainer = Color(0xFF115E59)
val DarkOnPrimaryContainer = Color(0xFFCCFBF1)
val DarkSecondary = Color(0xFF94A3B8)
val DarkSecondaryContainer = Color(0xFF1E293B)
val DarkBackground = Color(0xFF0B132B)
val DarkSurface = Color(0xFF162238)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

// True AMOLED Theme Palette (#000000 Pitch Black background)
val AmoledPrimary = Color(0xFF2DD4BF)
val AmoledOnPrimary = Color(0xFF000000)
val AmoledPrimaryContainer = Color(0xFF042F2E)
val AmoledOnPrimaryContainer = Color(0xFF5EEAD4)
val AmoledSecondary = Color(0xFFA1A1AA)
val AmoledSecondaryContainer = Color(0xFF18181B)
val AmoledBackground = Color(0xFF000000) // True Pitch Black
val AmoledSurface = Color(0xFF0E0E10)    // High contrast black-slate
val AmoledSurfaceVariant = Color(0xFF18181B)
val AmoledOnSurface = Color(0xFFF4F4F5)
val AmoledOnSurfaceVariant = Color(0xFFA1A1AA)
val AmoledOutline = Color(0xFF27272A)
