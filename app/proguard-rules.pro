# ProGuard rules for FlashForge

# Keep Compose
-dontwarn androidx.compose.**

# Keep Google Fonts provider
-keep class androidx.compose.ui.text.googlefonts.** { *; }
