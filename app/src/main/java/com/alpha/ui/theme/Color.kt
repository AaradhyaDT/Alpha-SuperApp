package com.alpha.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Alpha Color Palette — V2
 *
 * Design concept: Industrial HUD / Robotics Control Interface
 *
 * Primary   → Electric Cobalt   — active controls, highlights, CTAs
 * Secondary → Cool Slate        — supporting UI, chips, toggles
 * Tertiary  → Amber Warning     — status indicators, alerts, accents
 * Error     → Signal Red        — errors, critical states
 *
 * Dark surfaces use near-black with a subtle cool (blue) undertone
 * so the cobalt primary doesn't clash with a warm background.
 * Light surfaces use a cool off-white for the same reason.
 */

// ─────────────────────────────────────────────
// PRIMARY — Electric Cobalt
// ─────────────────────────────────────────────
val Cobalt80  = Color(0xFF9BBFFF)   // Dark mode: on-primary-container text, tonal buttons
val Cobalt60  = Color(0xFF5B8DEF)   // Dark mode: primary (main brand action color)
val Cobalt40  = Color(0xFF1A56DB)   // Light mode: primary
val Cobalt20  = Color(0xFF0D2D6B)   // Light mode: primary-container

// ─────────────────────────────────────────────
// SECONDARY — Cool Slate
// ─────────────────────────────────────────────
val Slate80   = Color(0xFFB0BEC5)   // Dark mode: secondary, chips, toggle tracks
val Slate60   = Color(0xFF78909C)   // Dark mode: on-secondary-container
val Slate40   = Color(0xFF546E7A)   // Light mode: secondary
val Slate20   = Color(0xFF1C2F38)   // Light mode: secondary-container

// ─────────────────────────────────────────────
// TERTIARY — Amber Warning (HUD status color)
// ─────────────────────────────────────────────
val Amber80   = Color(0xFFFFD180)   // Dark mode: tertiary, sensor/status highlights
val Amber60   = Color(0xFFFFAB40)   // Dark mode: on-tertiary-container
val Amber40   = Color(0xFFFF8F00)   // Light mode: tertiary
val Amber20   = Color(0xFF4A2800)   // Light mode: tertiary-container

// ─────────────────────────────────────────────
// ERROR — Signal Red
// ─────────────────────────────────────────────
val SignalRed80  = Color(0xFFFF8A80)  // Dark mode: error
val SignalRed40  = Color(0xFFD32F2F)  // Light mode: error

// ─────────────────────────────────────────────
// DARK SURFACES (cool-undertone near-blacks)
// ─────────────────────────────────────────────
val DarkBg        = Color(0xFF0D0F14)  // Background — deepest, near pitch-black with blue cast
val DarkSurface   = Color(0xFF13161E)  // Surface — cards, bottom sheets (slightly lifted)
val DarkSurface2  = Color(0xFF1A1E28)  // Surface variant — dialogs, elevated containers
val DarkOutline   = Color(0xFF2E3348)  // Outline — dividers, borders, input strokes

// ─────────────────────────────────────────────
// LIGHT SURFACES (cool off-whites)
// ─────────────────────────────────────────────
val LightBg       = Color(0xFFF2F4F8)  // Background — cool off-white, never pure #FFF
val LightSurface  = Color(0xFFFFFFFF)  // Surface — cards, sheets
val LightSurface2 = Color(0xFFE8ECF2)  // Surface variant — subtle tonal containers
val LightOutline  = Color(0xFFBCC4D0)  // Outline — dividers, borders

// ─────────────────────────────────────────────
// FIXED SEMANTIC COLORS (theme-independent)
// ─────────────────────────────────────────────
val StatusGreen   = Color(0xFF00E676)  // Connected / OK — used in connection status chip
val StatusRed     = Color(0xFFFF1744)  // Disconnected / Error
val StatusAmber   = Color(0xFFFFAB40)  // Warning / Standby / Connecting
