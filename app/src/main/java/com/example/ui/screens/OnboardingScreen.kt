package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AstroCatHeroMascot
import com.example.ui.components.AstroCatPeeking
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.components.WorldlyCrownLogo
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyGlass
import com.example.ui.theme.WorldlyLavender
import com.example.ui.theme.WorldlyPink
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicSkyBackdrop(
            modifier = Modifier.fillMaxSize(),
            showSkyline = pagerState.currentPage == 0,
            showClouds = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = WorldlyTextSecondary
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AstroCatHeroMascot(holdingStar = false)
                        Spacer(modifier = Modifier.height(10.dp))
                        WorldlyCrownLogo(fontSize = 40.sp, showCrown = true)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connect • Share • Explore ♡",
                            style = MaterialTheme.typography.titleMedium,
                            color = WorldlyTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.onboarding_body_1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    1 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AstroCatPeeking(size = 136.dp, wink = false, showHeartBubble = true)
                        Surface(
                            color = WorldlyGlass.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.5.dp, WorldlyCyan.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = WorldlyEmerald,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_title_2),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = WorldlyTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_body_2),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WorldlyTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AstroCatHeroMascot(holdingStar = true)
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Explore more together\nwith WORLDLY ♡",
                            style = MaterialTheme.typography.headlineLarge,
                            color = WorldlyTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.onboarding_body_3),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WorldlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                repeat(3) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (selected) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) WorldlyPink else WorldlyBorder)
                    )
                }
            }

            val isLast = pagerState.currentPage == 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(WorldlyCyanDim, WorldlyCyan, WorldlyLavender)
                        )
                    )
                    .clickable {
                        if (isLast) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                    .testTag("onboarding_next_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLast) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (isLast) "Enter WORLDLY" else stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    if (!isLast) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
