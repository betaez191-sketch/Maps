package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyGlass
import com.example.ui.theme.WorldlyLavender
import com.example.ui.theme.WorldlyMidnight
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyPink
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextPrimary

@Composable
fun WorldlyCrownLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    showCrown: Boolean = true
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "WORLDL",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, WorldlyLavender)
                    )
                )
            )
            Box(contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "Y",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, WorldlyLavender)
                        )
                    )
                )
                if (showCrown) {
                    Canvas(
                        modifier = Modifier
                            .size((fontSize.value * 0.52f).dp)
                            .offset(x = 3.dp, y = (-fontSize.value * 0.28f).dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val crown = Path().apply {
                            moveTo(w * 0.1f, h * 0.85f)
                            lineTo(w * 0.05f, h * 0.28f)
                            lineTo(w * 0.34f, h * 0.58f)
                            lineTo(w * 0.5f, h * 0.15f)
                            lineTo(w * 0.66f, h * 0.58f)
                            lineTo(w * 0.95f, h * 0.28f)
                            lineTo(w * 0.9f, h * 0.85f)
                            close()
                        }
                        drawPath(crown, color = WorldlyLavender)
                        drawPath(crown, color = Color.White, style = Stroke(width = 1.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun CosmicSkyBackdrop(
    modifier: Modifier = Modifier,
    showSkyline: Boolean = false,
    showClouds: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "twinkle")
    val twinkle by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "starAlpha"
    )

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    WorldlyMidnight,
                    Color(0xFF140B2D),
                    Color(0xFF1F1147),
                    WorldlyMidnight
                )
            )
        )

        // Subtle ringed cosmic planets
        val planetCenter = Offset(size.width * 0.18f, size.height * 0.12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(WorldlyLavender, WorldlyCyanDim),
                center = planetCenter,
                radius = 34f
            ),
            radius = 26f,
            center = planetCenter
        )
        rotate(-20f, pivot = planetCenter) {
            drawOval(
                color = WorldlyPink.copy(alpha = 0.6f),
                topLeft = Offset(planetCenter.x - 44f, planetCenter.y - 10f),
                size = Size(88f, 20f),
                style = Stroke(width = 3f)
            )
        }

        val planet2 = Offset(size.width * 0.86f, size.height * 0.18f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(WorldlyCyan, WorldlySlate),
                center = planet2,
                radius = 24f
            ),
            radius = 18f,
            center = planet2
        )
        rotate(16f, pivot = planet2) {
            drawOval(
                color = WorldlyLavender.copy(alpha = 0.55f),
                topLeft = Offset(planet2.x - 32f, planet2.y - 7f),
                size = Size(64f, 14f),
                style = Stroke(width = 2.4f)
            )
        }

        // Stars & 4-point cosmic sparkles
        val stars = listOf(
            Offset(0.10f, 0.24f) to 4f,
            Offset(0.28f, 0.08f) to 3f,
            Offset(0.45f, 0.16f) to 5f,
            Offset(0.68f, 0.09f) to 3.5f,
            Offset(0.82f, 0.30f) to 4.5f,
            Offset(0.14f, 0.48f) to 3.5f,
            Offset(0.90f, 0.52f) to 4f,
            Offset(0.22f, 0.72f) to 3f,
            Offset(0.76f, 0.68f) to 4f
        )
        stars.forEachIndexed { idx, (pos, r) ->
            val cx = size.width * pos.x
            val cy = size.height * pos.y
            val a = if (idx % 2 == 0) twinkle else 1.35f - twinkle
            drawCircle(
                color = (if (idx % 3 == 0) WorldlyAmber else WorldlyLavender).copy(alpha = a.coerceIn(0.2f, 0.95f)),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        // Fluffy purple clouds at the bottom
        if (showClouds) {
            val cloudY = size.height * 0.90f
            val cloudCircles = listOf(
                Offset(0.08f, 0.94f) to 90f,
                Offset(0.24f, 0.91f) to 110f,
                Offset(0.45f, 0.95f) to 105f,
                Offset(0.68f, 0.92f) to 115f,
                Offset(0.90f, 0.94f) to 95f
            )
            cloudCircles.forEach { (pt, r) ->
                drawCircle(
                    color = WorldlyGlass.copy(alpha = 0.75f),
                    radius = r,
                    center = Offset(size.width * pt.x, size.height * pt.y)
                )
            }
            drawRect(
                color = WorldlyObsidian.copy(alpha = 0.65f),
                topLeft = Offset(0f, cloudY + 20f),
                size = Size(size.width, size.height)
            )
        }

        // City skyline silhouette
        if (showSkyline) {
            val baseY = size.height
            val buildings = listOf(
                0.02f to 0.14f, 0.08f to 0.21f, 0.15f to 0.16f, 0.22f to 0.24f,
                0.31f to 0.15f, 0.39f to 0.20f, 0.48f to 0.13f, 0.56f to 0.23f,
                0.65f to 0.17f, 0.74f to 0.25f, 0.83f to 0.16f, 0.91f to 0.20f
            )
            buildings.forEach { (xRatio, hRatio) ->
                val bx = size.width * xRatio
                val bw = size.width * 0.068f
                val bh = size.height * hRatio
                drawRect(
                    color = WorldlyMidnight.copy(alpha = 0.92f),
                    topLeft = Offset(bx, baseY - bh),
                    size = Size(bw, bh)
                )
                // Tiny glowing windows
                var wy = baseY - bh + 18f
                while (wy < baseY - 24f) {
                    drawRect(
                        color = WorldlyLavender.copy(alpha = 0.35f),
                        topLeft = Offset(bx + bw * 0.25f, wy),
                        size = Size(6f, 8f)
                    )
                    drawRect(
                        color = WorldlyAmber.copy(alpha = 0.3f),
                        topLeft = Offset(bx + bw * 0.60f, wy),
                        size = Size(6f, 8f)
                    )
                    wy += 22f
                }
            }
        }
    }
}

@Composable
fun AstroCatPeeking(
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    wink: Boolean = false,
    showHeartBubble: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "peekBob")
    val bob by transition.animateFloat(
        initialValue = -2f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "bobY"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.BottomCenter) {
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = bob.dp)
        ) {
            val w = this.size.width
            val h = this.size.height

            // Soft purple glow halo behind helmet
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WorldlyCyan.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.55f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.5f, h * 0.55f)
            )

            // Left & Right Cat Ears
            val leftEar = Path().apply {
                moveTo(w * 0.20f, h * 0.48f)
                lineTo(w * 0.16f, h * 0.16f)
                lineTo(w * 0.40f, h * 0.30f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(w * 0.80f, h * 0.48f)
                lineTo(w * 0.84f, h * 0.16f)
                lineTo(w * 0.60f, h * 0.30f)
                close()
            }
            drawPath(leftEar, color = Color(0xFFF3ECFF))
            drawPath(rightEar, color = Color(0xFFF3ECFF))
            drawPath(leftEar, color = WorldlyCyan, style = Stroke(width = 4f))
            drawPath(rightEar, color = WorldlyCyan, style = Stroke(width = 4f))

            // Inner pink-lavender ears
            val innerLeft = Path().apply {
                moveTo(w * 0.23f, h * 0.42f)
                lineTo(w * 0.21f, h * 0.24f)
                lineTo(w * 0.35f, h * 0.32f)
                close()
            }
            val innerRight = Path().apply {
                moveTo(w * 0.77f, h * 0.42f)
                lineTo(w * 0.79f, h * 0.24f)
                lineTo(w * 0.65f, h * 0.32f)
                close()
            }
            drawPath(innerLeft, color = WorldlyPink.copy(alpha = 0.85f))
            drawPath(innerRight, color = WorldlyPink.copy(alpha = 0.85f))

            // Helmet outer white shell
            drawOval(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.14f, h * 0.26f),
                size = Size(w * 0.72f, h * 0.66f)
            )
            drawOval(
                color = WorldlyLavender,
                topLeft = Offset(w * 0.14f, h * 0.26f),
                size = Size(w * 0.72f, h * 0.66f),
                style = Stroke(width = 4f)
            )

            // Side headphones
            drawRoundRect(
                color = WorldlyCyan,
                topLeft = Offset(w * 0.08f, h * 0.46f),
                size = Size(w * 0.10f, h * 0.24f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = WorldlyCyan,
                topLeft = Offset(w * 0.82f, h * 0.46f),
                size = Size(w * 0.10f, h * 0.24f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Dark cosmic visor
            drawOval(
                color = Color(0xFF0E081D),
                topLeft = Offset(w * 0.22f, h * 0.37f),
                size = Size(w * 0.56f, h * 0.45f)
            )

            // Kawaii eyes + ':3' mouth
            if (wink) {
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.33f, h * 0.57f),
                    end = Offset(w * 0.40f, h * 0.53f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.40f, h * 0.53f),
                    end = Offset(w * 0.34f, h * 0.50f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(Color.White, radius = w * 0.042f, center = Offset(w * 0.37f, h * 0.56f))
            }
            drawCircle(Color.White, radius = w * 0.042f, center = Offset(w * 0.63f, h * 0.56f))
            // Blush cheeks
            drawOval(
                color = WorldlyPink.copy(alpha = 0.65f),
                topLeft = Offset(w * 0.28f, h * 0.61f),
                size = Size(w * 0.08f, h * 0.04f)
            )
            drawOval(
                color = WorldlyPink.copy(alpha = 0.65f),
                topLeft = Offset(w * 0.64f, h * 0.61f),
                size = Size(w * 0.08f, h * 0.04f)
            )
            // Cute w mouth
            val mouth = Path().apply {
                moveTo(w * 0.45f, h * 0.61f)
                quadraticTo(w * 0.475f, h * 0.66f, w * 0.50f, h * 0.61f)
                quadraticTo(w * 0.525f, h * 0.66f, w * 0.55f, h * 0.61f)
            }
            drawPath(mouth, color = Color.White, style = Stroke(width = 3.2f, cap = StrokeCap.Round))

            // Little peeking paws on bottom rim
            drawOval(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.20f, h * 0.80f),
                size = Size(w * 0.16f, h * 0.14f)
            )
            drawOval(
                color = WorldlyCyan,
                topLeft = Offset(w * 0.20f, h * 0.80f),
                size = Size(w * 0.16f, h * 0.14f),
                style = Stroke(width = 3f)
            )
            drawOval(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.64f, h * 0.80f),
                size = Size(w * 0.16f, h * 0.14f)
            )
            drawOval(
                color = WorldlyCyan,
                topLeft = Offset(w * 0.64f, h * 0.80f),
                size = Size(w * 0.16f, h * 0.14f),
                style = Stroke(width = 3f)
            )
        }

        if (showHeartBubble) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = WorldlyPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AstroCatHeroMascot(
    modifier: Modifier = Modifier,
    holdingStar: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "heroFloat")
    val floatY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "heroFloatY"
    )

    Box(
        modifier = modifier.offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(210.dp)) {
            val w = size.width
            val h = size.height

            // Cosmic aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WorldlyCyan.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.52f),
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = Offset(w * 0.5f, h * 0.52f)
            )

            // Suit body
            drawRoundRect(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.30f, h * 0.48f),
                size = Size(w * 0.40f, h * 0.30f),
                cornerRadius = CornerRadius(48f, 48f)
            )
            drawRoundRect(
                color = WorldlyLavender,
                topLeft = Offset(w * 0.30f, h * 0.48f),
                size = Size(w * 0.40f, h * 0.30f),
                cornerRadius = CornerRadius(48f, 48f),
                style = Stroke(width = 4f)
            )

            // Helmet & cat ears (top half)
            val leftEar = Path().apply {
                moveTo(w * 0.25f, h * 0.34f)
                lineTo(w * 0.21f, h * 0.10f)
                lineTo(w * 0.41f, h * 0.21f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(w * 0.75f, h * 0.34f)
                lineTo(w * 0.79f, h * 0.10f)
                lineTo(w * 0.59f, h * 0.21f)
                close()
            }
            drawPath(leftEar, color = Color(0xFFF5F0FF))
            drawPath(rightEar, color = Color(0xFFF5F0FF))
            drawPath(leftEar, color = WorldlyCyan, style = Stroke(width = 4f))
            drawPath(rightEar, color = WorldlyCyan, style = Stroke(width = 4f))

            drawOval(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.20f, h * 0.16f),
                size = Size(w * 0.60f, h * 0.40f)
            )
            drawOval(
                color = WorldlyLavender,
                topLeft = Offset(w * 0.20f, h * 0.16f),
                size = Size(w * 0.60f, h * 0.40f),
                style = Stroke(width = 4f)
            )
            // Visor
            drawOval(
                color = Color(0xFF0E081D),
                topLeft = Offset(w * 0.27f, h * 0.23f),
                size = Size(w * 0.46f, h * 0.28f)
            )
            drawCircle(Color.White, radius = 6.5f, center = Offset(w * 0.39f, h * 0.36f))
            drawCircle(Color.White, radius = 6.5f, center = Offset(w * 0.61f, h * 0.36f))
            val mouth = Path().apply {
                moveTo(w * 0.45f, h * 0.40f)
                quadraticTo(w * 0.475f, h * 0.44f, w * 0.50f, h * 0.40f)
                quadraticTo(w * 0.525f, h * 0.44f, w * 0.55f, h * 0.40f)
            }
            drawPath(mouth, color = Color.White, style = Stroke(width = 3f, cap = StrokeCap.Round))

            if (holdingStar) {
                // Glowing golden star cradled by paws
                val cx = w * 0.50f
                val cy = h * 0.68f
                val outerR = w * 0.14f
                val innerR = w * 0.065f
                val star = Path().apply {
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val angle = Math.toRadians((i * 36.0) - 90.0)
                        val px = cx + (r * kotlin.math.cos(angle)).toFloat()
                        val py = cy + (r * kotlin.math.sin(angle)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(WorldlyAmber.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = outerR * 1.9f
                    ),
                    radius = outerR * 1.9f,
                    center = Offset(cx, cy)
                )
                drawPath(star, color = WorldlyAmber)
                drawPath(star, color = Color.White, style = Stroke(width = 2.5f))
            } else {
                // Hugging glowing purple Planet Earth!
                val earthCenter = Offset(w * 0.50f, h * 0.72f)
                val earthR = w * 0.21f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(WorldlyLavender, WorldlyCyanDim, Color(0xFF3B0764)),
                        center = Offset(earthCenter.x - 14f, earthCenter.y - 14f),
                        radius = earthR * 1.3f
                    ),
                    radius = earthR,
                    center = earthCenter
                )
                drawCircle(
                    color = WorldlyPink.copy(alpha = 0.5f),
                    radius = earthR,
                    center = earthCenter,
                    style = Stroke(width = 3f)
                )
                // Continents accents
                drawOval(
                    color = WorldlyPink.copy(alpha = 0.38f),
                    topLeft = Offset(earthCenter.x - earthR * 0.55f, earthCenter.y - earthR * 0.35f),
                    size = Size(earthR * 0.6f, earthR * 0.45f)
                )
                drawOval(
                    color = WorldlyCyan.copy(alpha = 0.35f),
                    topLeft = Offset(earthCenter.x + earthR * 0.05f, earthCenter.y + earthR * 0.05f),
                    size = Size(earthR * 0.55f, earthR * 0.45f)
                )
            }

            // Little paws wrapping on sides
            drawOval(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.25f, h * 0.60f),
                size = Size(w * 0.14f, h * 0.11f)
            )
            drawOval(
                color = Color(0xFFF5F0FF),
                topLeft = Offset(w * 0.61f, h * 0.60f),
                size = Size(w * 0.14f, h * 0.11f)
            )
        }
    }
}
