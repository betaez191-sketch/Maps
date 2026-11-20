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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.auth.AuthRepository
import com.example.ui.components.AstroCatPeeking
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.components.WorldlyCrownLogo
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyLavender
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val signedIn = AuthRepository.attemptAutoSignIn(context)
        if (signedIn) {
            onAuthSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicSkyBackdrop(
            modifier = Modifier.fillMaxSize(),
            showSkyline = false,
            showClouds = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WorldlyCrownLogo(fontSize = 36.sp, showCrown = true)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.titleLarge,
                color = WorldlyTextPrimary
            )
            Text(
                text = "Let's explore the world together ♡",
                style = MaterialTheme.typography.bodyMedium,
                color = WorldlyTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Peeking Astro-Cat sitting atop the auth glass card
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = WorldlyObsidian.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(1.5.dp, WorldlyCyan.copy(alpha = 0.55f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 86.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 34.dp,
                            bottom = 24.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = WorldlySlate.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, WorldlyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = WorldlyEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Location sharing stays OFF until you opt in.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyTextPrimary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = WorldlyLavender,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Only mutually accepted friends can see your pin.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyTextPrimary
                                    )
                                }
                            }
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                color = WorldlyRose.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, WorldlyRose.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WorldlyRose,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Glowing Purple Pill Google Sign-In CTA
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
                                .clickable(enabled = !isLoading) {
                                    errorMessage = null
                                    isLoading = true
                                    scope.launch {
                                        when (val outcome = AuthRepository.signInWithGoogle(context)) {
                                            AuthRepository.SignInOutcome.Success -> {
                                                isLoading = false
                                                onAuthSuccess()
                                            }
                                            AuthRepository.SignInOutcome.Cancelled -> {
                                                isLoading = false
                                            }
                                            is AuthRepository.SignInOutcome.Error -> {
                                                isLoading = false
                                                errorMessage = outcome.message
                                            }
                                        }
                                    }
                                }
                                .testTag("google_sign_in_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = "Signing in...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = Color.White,
                                        shape = CircleShape,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "G",
                                                color = WorldlyCyanDim,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.auth_google_button),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.auth_privacy_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AstroCatPeeking(
                    size = 112.dp,
                    wink = true,
                    showHeartBubble = true,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
        }
    }
}
