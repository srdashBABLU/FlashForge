package com.truelokal.flashforge.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truelokal.flashforge.ui.theme.AccentAmber
import com.truelokal.flashforge.ui.theme.AccentAmberDark
import com.truelokal.flashforge.ui.theme.AccentAmberLight
import kotlinx.coroutines.launch

/**
 * Premium onboarding screen with a multipage sliding flow and dynamic background glows.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // Dynamic accent color based on active page
    val animatedAccentColor by animateColorAsState(
        targetValue = when (pagerState.currentPage) {
            0 -> AccentAmber
            1 -> AccentAmberLight
            else -> AccentAmberDark
        },
        animationSpec = tween(600),
        label = "accentGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF070710), Color(0xFF0F0F24))
                    } else {
                        listOf(Color(0xFFF5F5FA), Color(0xFFE8E8F0))
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient background glowing radial spot
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.35f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.35f),
                    radius = size.minDimension * 0.7f
                ),
                center = Offset(size.width / 2f, size.height * 0.35f),
                radius = size.minDimension * 0.7f
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sliding pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPageContent(
                        title = "Welcome to FlashForge",
                        description = "Transform your device flash into a professional illumination and communication toolkit. Experience next-level utility.",
                        icon = Icons.Rounded.FlashlightOn,
                        accentColor = AccentAmber,
                        isDark = isDark
                    )
                    1 -> OnboardingPageContent(
                        title = "Intelligent Signals",
                        description = "Broadcast standard SOS alerts, adjust flashlight intensity smoothly, or run custom strobes with precise microsecond timing.",
                        icon = Icons.Rounded.Security,
                        accentColor = AccentAmberLight,
                        isDark = isDark
                    )
                    2 -> OnboardingPageContent(
                        title = "Optical Morse Decoder",
                        description = "Point the camera at any light flash to decode it into readable text instantly using local secure frames processing.",
                        icon = Icons.Rounded.CameraAlt,
                        accentColor = AccentAmberDark,
                        isDark = isDark
                    )
                }
            }

            // Navigation controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) animatedAccentColor
                                    else (if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f))
                                )
                        )
                    }
                }

                // Action button
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < 2) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onFinished()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedAccentColor,
                        contentColor = Color(0xFF070710)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (pagerState.currentPage == 2) "Get Started" else "Next",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing nested target rings for premium feel
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerPt = Offset(size.width / 2f, size.height / 2f)
                // Outer ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.08f),
                    center = centerPt,
                    radius = size.minDimension * 0.45f
                )
                // Inner ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.15f),
                    center = centerPt,
                    radius = size.minDimension * 0.35f
                )
                // Core ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f),
                    center = centerPt,
                    radius = size.minDimension * 0.25f
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Glassmorphic outlined cards
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x0CFFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.02f))
                    } else {
                        listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.02f))
                    }
                )
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 26.sp,
                        fontSize = 15.sp
                    ),
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
