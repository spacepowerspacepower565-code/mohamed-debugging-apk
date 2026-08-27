package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.MainActivity
import com.example.data.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.res.painterResource
import com.example.R

data class SparkleParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val alpha: Float,
    val life: Float,
    val decay: Float
)

@Composable
fun GameApp(viewModel: GameViewModel) {
    val context = LocalContext.current
    val progress by viewModel.userProgress.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val soundTrigger by viewModel.soundPlayTrigger.collectAsState()

    // Tone synthesis for crisp sound feedback without local raw files
    LaunchedEffect(soundTrigger) {
        if (soundTrigger > 0 && progress.soundEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    // Capture hardware back button
    BackHandler(enabled = currentScreen != GameScreen.MAIN_MENU) {
        when (currentScreen) {
            GameScreen.RIDDLE_PLAY, GameScreen.RIDDLES_MAP -> viewModel.navigateTo(GameScreen.MAIN_MENU)
            GameScreen.WORD_PLAY, GameScreen.WORDS_MAP -> viewModel.navigateTo(GameScreen.MAIN_MENU)
            GameScreen.CONGRATULATIONS -> viewModel.backToMapFromSuccess()
            else -> viewModel.navigateTo(GameScreen.MAIN_MENU)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (currentScreen == GameScreen.MAIN_MENU || currentScreen == GameScreen.RIDDLES_MAP || currentScreen == GameScreen.WORDS_MAP || currentScreen == GameScreen.WORD_PLAY) {
            Image(
                painter = painterResource(id = R.drawable.img_home_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AnimatedBackground(progress.selectedBackground)
        }

        // App Layout Frame
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (currentScreen != GameScreen.MAIN_MENU && currentScreen != GameScreen.RIDDLES_MAP && currentScreen != GameScreen.WORDS_MAP && currentScreen != GameScreen.WORD_PLAY) {
                        Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    } else {
                        Modifier
                    }
                )
        ) {
            if (currentScreen != GameScreen.MAIN_MENU && currentScreen != GameScreen.RIDDLES_MAP && currentScreen != GameScreen.WORDS_MAP && currentScreen != GameScreen.WORD_PLAY) {
                // Header: Gem state + Menu Back Button
                GameHeaderRow(
                    currentScreen = currentScreen,
                    gems = progress.gems,
                    onBack = {
                        when (currentScreen) {
                            GameScreen.RIDDLE_PLAY -> viewModel.navigateTo(GameScreen.RIDDLES_MAP)
                            GameScreen.WORD_PLAY -> viewModel.navigateTo(GameScreen.WORDS_MAP)
                            else -> viewModel.navigateTo(GameScreen.MAIN_MENU)
                        }
                    },
                    onShopTrigger = { viewModel.toggleShopDialog(true) }
                )
            }

            // Dynamic Screen view switcher
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(400)) { if (it > 0) it else -it } togetherWith
                            slideOutHorizontally(animationSpec = tween(400)) { if (it > 0) -it else it }
                },
                modifier = Modifier.weight(1f)
            ) { screen ->
                when (screen) {
                    GameScreen.MAIN_MENU -> MainMenuScreen(progress, viewModel)
                    GameScreen.RIDDLES_MAP -> RiddlesMapScreen(progress, viewModel)
                    GameScreen.WORDS_MAP -> WordsMapScreen(progress, viewModel)
                    GameScreen.RIDDLE_PLAY -> RiddlePlayScreen(progress, viewModel)
                    GameScreen.WORD_PLAY -> WordPlayScreen(progress, viewModel)
                    GameScreen.CONGRATULATIONS -> CongratulationsScreen(progress, viewModel)
                }
            }
        }

        // Overlay dialogs
        if (viewModel.showDailyRewardDialog.collectAsState().value) {
            DailyRewardDialog(progress, viewModel)
        }

        if (viewModel.showSettingsDialog.collectAsState().value) {
            SettingsDialog(progress, viewModel)
        }

        if (viewModel.showRanksDialog.collectAsState().value) {
            RanksDialog(progress, viewModel)
        }

        if (viewModel.showShopDialog.collectAsState().value) {
            ShopDialog(progress, viewModel)
        }

        if (viewModel.showNameChangeDialog.collectAsState().value) {
            NameChangeDialog(progress, viewModel)
        }

        if (viewModel.showTutorialDialog.collectAsState().value) {
            TutorialDialog(viewModel)
        }
    }
}

@Composable
fun GameHeaderRow(
    currentScreen: GameScreen,
    gems: Int,
    onBack: () -> Unit,
    onShopTrigger: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentScreen != GameScreen.MAIN_MENU) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF49454F), CircleShape)
                    .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f), CircleShape)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "الرجوع لخلف",
                    tint = Color(0xFFD0BCFF)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(44.dp))
        }

        // Title or state label
        Text(
            text = when (currentScreen) {
                GameScreen.MAIN_MENU -> "الرئيسية"
                GameScreen.RIDDLES_MAP -> "خريطة الألغاز"
                GameScreen.WORDS_MAP -> "خريطة الكلمات الكبرى"
                GameScreen.RIDDLE_PLAY -> "حل اللغز الذكي"
                GameScreen.WORD_PLAY -> "ربط الكلمات العجيب"
                GameScreen.CONGRATULATIONS -> "تهانينا! 🎊"
            },
            color = Color(0xFFE6E1E5),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color(0xFF2B2930), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

        // Gem Counter with hover/tap micro-interaction trigger
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        var isCoinPressed by remember { mutableStateOf(false) }
        val gemScale by animateFloatAsState(
            targetValue = if (isCoinPressed) 1.3f else if (isHovered) 1.2f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "GemBounceAnim"
        )

        LaunchedEffect(isCoinPressed) {
            if (isCoinPressed) {
                kotlinx.coroutines.delay(200)
                isCoinPressed = false
            }
        }

        Row(
            modifier = Modifier
                .testTag("gem_score_badge")
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        isCoinPressed = true
                        onShopTrigger()
                    }
                )
                .background(
                    color = Color(0xFF2B2930),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFFD0BCFF), RoundedCornerShape(16.dp))
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), ambientColor = Color(0xFFD0BCFF), spotColor = Color(0xFFD0BCFF))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "شراء جواهر",
                tint = Color(0xFFD0BCFF),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer(scaleX = gemScale, scaleY = gemScale)
            )
            Spacer(modifier = Modifier.width(4.dp))
            BubblyText(
                text = "$gems",
                fontSize = 16.sp,
                fillColor = Color(0xFFFFD700), // Sparkling clean yellow/gold gems fill
                strokeWidth = 6f
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Gem Diamond Icon
            Icon(
                imageVector = Icons.Default.Favorite, // Replaces generic diamond vector visually
                contentDescription = "جواهر",
                tint = Color(0xFFD0BCFF),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(scaleX = gemScale, scaleY = gemScale)
            )
        }
    }
}

@Composable
fun MainMenuScreen(progress: UserProgress, viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Foreground Meadow & Bokeh Flowers styling
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Beautiful green meadow base
            val meadowPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.72f)
                quadraticTo(w * 0.25f, h * 0.64f, w * 0.55f, h * 0.76f)
                quadraticTo(w * 0.8f, h * 0.69f, w, h * 0.75f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = meadowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4CAF50).copy(0.45f), Color(0xFF1B5E20).copy(0.72f))
                )
            )

            // Blurry Bokeh Flowers (Red, Pink, Light Blue, Yellow)
            val flowers = listOf(
                Pair(Offset(w * 0.15f, h * 0.82f), Color(0xFFFF5252)), // Red
                Pair(Offset(w * 0.35f, h * 0.76f), Color(0xFFFF80AB)), // Pink
                Pair(Offset(w * 0.65f, h * 0.79f), Color(0xFF40C4FF)), // Light Blue
                Pair(Offset(w * 0.85f, h * 0.84f), Color(0xFFFFD740)), // Yellow
                Pair(Offset(w * 0.25f, h * 0.89f), Color(0xFFFFD740)), // Yellow
                Pair(Offset(w * 0.55f, h * 0.86f), Color(0xFFFF5252)), // Red
                Pair(Offset(w * 0.78f, h * 0.91f), Color(0xFFFF80AB)), // Pink
                Pair(Offset(w * 0.45f, h * 0.93f), Color(0xFF40C4FF)), // Light Blue
                Pair(Offset(w * 0.08f, h * 0.94f), Color(0xFFFFD740))  // Yellow
            )
            flowers.forEach { (pos, color) ->
                drawCircle(
                    color = color.copy(alpha = 0.55f),
                    radius = 22f,
                    center = pos
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 8f,
                    center = pos
                )
            }
        }

        // Entire vertical UI stack
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Simulated System Status Bar Row (top right/left elements)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Digital Clock "2:22" + notification warning dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "2:22",
                        color = Color(0xFF161E54), // Dark Navy Blue
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.example.ui.theme.fredokaFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFFE65100), CircleShape)
                    )
                }

                // Right: system icons in dark navy blue
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📶", fontSize = 13.sp)
                    Text("🔋", fontSize = 13.sp)
                }
            }

            // 2. TOP HUD ACTIONS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Diamond badge on the Left
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .background(
                                color = Color(0xB3FFFFFF), // capsule soft white-translucent background
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(1.5.dp, Color.White.copy(0.6f), RoundedCornerShape(24.dp))
                            .padding(start = 28.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%,d", progress.gems),
                            color = Color(0xFF161E54), // dark navy blue font
                            fontSize = 17.sp,
                            fontFamily = com.example.ui.theme.fredokaFontFamily,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF26A69A), CircleShape) // neon green topping up gems
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable { viewModel.toggleShopDialog(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "شراء جواهر",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Left edge 3D crystal gem icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .shadow(4.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💎", fontSize = 18.sp)
                    }
                }

                // Cloud asset
                Text(
                    text = "☁️",
                    fontSize = 44.sp,
                    modifier = Modifier.offset(x = (-8).dp)
                )

                // Settings button on the Right
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF3F51B5), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape)
                        .clickable { viewModel.toggleSettingsDialog(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات والخيارات",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 3. MID-SCREEN CHUNKS (Floating calendar + Main logo "كلمات")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Left Floating calendar icon
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFF111111), RoundedCornerShape(12.dp))
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleDailyRewardDialog(true) },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Red header of calendar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .background(Color(0xFFFF3D00), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(2.5.dp).background(Color.White, CircleShape))
                                    Box(modifier = Modifier.size(2.5.dp).background(Color.White, CircleShape))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(4) { Box(modifier = Modifier.size(5.dp).background(Color(0xFF111111), RoundedCornerShape(1.dp))) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(4) { Box(modifier = Modifier.size(5.dp).background(Color(0xFF111111), RoundedCornerShape(1.dp))) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(3) { Box(modifier = Modifier.size(5.dp).background(Color(0xFF111111), RoundedCornerShape(1.dp))) }
                                }
                            }
                        }
                        // Exclamation warning label badge of notification
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .background(Color(0xFFFF3D00), CircleShape)
                                .border(1.2.dp, Color.White, CircleShape)
                                .align(Alignment.TopStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "!",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Words Game logo text "كلمات"
                BubblyText(
                    text = "كلمات",
                    fontSize = 80.sp,
                    fontFamily = com.example.ui.theme.cairoFontFamily,
                    fillColor = Color.White,
                    strokeColor = Color(0xFF161E54).copy(alpha = 0.82f),
                    strokeWidth = 14f,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            // 4. FROSTED GLASSMORPHISM PROGRESS CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(26.dp),
                        clip = false
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .background(
                        color = Color(0x3D011627), // Beautiful deep translucent/glassy blue-slate
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                val solvedTotal = (progress.currentRiddleLevel - 1) + (progress.currentWordLevel - 1)
                val currentRankTier = (solvedTotal / 100) + 1
                val currentRankProgressInBracket = solvedTotal % 100
                val rankTitleCurrent = when (currentRankTier) {
                    1 -> "برونزي I"
                    2 -> "برونزي II"
                    3 -> "فضي I"
                    4 -> "فضي II"
                    5 -> "ذهبي I"
                    else -> "بلاتيني I"
                }
                val rankTitleNext = when (currentRankTier + 1) {
                    1 -> "برونزي I"
                    2 -> "برونزي II"
                    3 -> "فضي I"
                    4 -> "فضي II"
                    5 -> "ذهبي I"
                    else -> "بلاتيني I"
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Ranks pills row (RTL, right-to-left)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom capsule Current Rank "برونزي I"
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = rankTitleCurrent,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.cairoFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Arrow symbol reflecting left direction
                            Text(
                                text = ">", 
                                color = Color.White.copy(alpha = 0.61f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Custom capsule Next Rank "برونزي II"
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = rankTitleNext,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.cairoFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Row for "تقدم الرتبة" and "0/100" progress value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تقدم الرتبة",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = com.example.ui.theme.cairoFontFamily
                            )

                            Text(
                                text = "$currentRankProgressInBracket/100",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = com.example.ui.theme.fredokaFontFamily,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar status track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    ) {
                        val ratio = (currentRankProgressInBracket.toFloat() / 100f).coerceIn(0f, 1f)
                        if (ratio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)) // beautiful green fill
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            // 5. ACTION CONTROLS ROW (المستويات & التحدي اليومي 3D buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Orange levels button: "المستويات"
                val levelsInteraction = remember { MutableInteractionSource() }
                val isLevelsPressed by levelsInteraction.collectIsPressedAsState()
                val levelsOffset by animateDpAsState(targetValue = if (isLevelsPressed) 4.dp else 0.dp, label = "levels_press")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .offset(y = levelsOffset)
                        .shadow(elevation = if (isLevelsPressed) 0.dp else 6.dp, shape = RoundedCornerShape(32.dp))
                        .background(Color(0xFFFF9800), shape = RoundedCornerShape(32.dp))
                        .then(
                            if (!isLevelsPressed) {
                                Modifier.drawBehind {
                                    drawRoundRect(
                                        color = Color(0xFFE65100), // Burnt orange 3D bottom shadow
                                        topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                        size = Size(size.width, 4.dp.toPx()),
                                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = levelsInteraction,
                            indication = null
                        ) {
                            viewModel.navigateTo(GameScreen.RIDDLES_MAP)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BubblyText(
                        text = "المستويات",
                        fontSize = 24.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        fillColor = Color.White,
                        strokeColor = Color(0xFFE65100),
                        strokeWidth = 6f
                    )
                }

                // Purple daily puzzle map button: "التحدي اليومي"
                val dailyInteraction = remember { MutableInteractionSource() }
                val isDailyPressed by dailyInteraction.collectIsPressedAsState()
                val dailyOffset by animateDpAsState(targetValue = if (isDailyPressed) 4.dp else 0.dp, label = "daily_press")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .offset(y = dailyOffset)
                        .shadow(elevation = if (isDailyPressed) 0.dp else 6.dp, shape = RoundedCornerShape(32.dp))
                        .background(Color(0xFFAB47BC), shape = RoundedCornerShape(32.dp))
                        .then(
                            if (!isDailyPressed) {
                                Modifier.drawBehind {
                                    drawRoundRect(
                                        color = Color(0xFF4A148C), // Deep Purple 3D bottom shadow
                                        topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                        size = Size(size.width, 4.dp.toPx()),
                                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = dailyInteraction,
                            indication = null
                        ) {
                            viewModel.navigateTo(GameScreen.WORDS_MAP)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BubblyText(
                        text = "التحدي اليومي",
                        fontSize = 24.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        fillColor = Color.White,
                        strokeColor = Color(0xFF4A148C),
                        strokeWidth = 6f
                    )
                }

                // How to play guide button described by user
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFF4D69DA).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.toggleTutorialDialog(true) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ℹ️ كيف تلعب؟ دليل الضغط والسحب 👆",
                            color = Color(0xFF283593),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }
                }
            }

            // Minimalist home bar indicator at the absolute bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun MenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFF49454F), CircleShape)
                .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f), CircleShape)
                .shadow(4.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFFE6E1E5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color(0xFF1C1B1F).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PlayLauncherButton(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .height(72.dp)
            .padding(start = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "انطلق الآن اللعب",
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

fun getLevelTierColors(levelNum: Int): Pair<Color, Color> {
    return when {
         levelNum <= 100 -> Pair(Color(0xFFFFFFFF), Color(0xFF161E54)) // Pristine white + dark navy blue
         levelNum <= 200 -> Pair(Color(0xFFE8F5E9), Color(0xFF1B5E20)) // Mint green + deep forest green
         levelNum <= 300 -> Pair(Color(0xFFFFFDE7), Color(0xFFE65100)) // Lemon cream yellow + dark golden ochre
         levelNum <= 400 -> Pair(Color(0xFFE3F2FD), Color(0xFF0D47A1)) // Soft sky blue + ocean
         levelNum <= 500 -> Pair(Color(0xFFF3E5F5), Color(0xFF4A148C)) // Lavender purple + amethyst
         levelNum <= 600 -> Pair(Color(0xFFFCE4EC), Color(0xFF880E4F)) // Rose pink + rose
         levelNum <= 700 -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100)) // Apricot orange + chestnut orange
         levelNum <= 800 -> Pair(Color(0xFFF5F5DC), Color(0xFF3E2723)) // Desert beige + espresso
         levelNum <= 900 -> Pair(Color(0xFFE0F2F1), Color(0xFF004D40)) // Sage turquoise + dark teal
         else -> Pair(Color(0xFFECEFF1), Color(0xFF1A1A1A)) // Cosmic gray + obsidian
    }
}

@Composable
fun RiddlesMapScreen(progress: UserProgress, viewModel: GameViewModel) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val currentLevel = progress.currentRiddleLevel
    val totalLevels = 1000

    LaunchedEffect(currentLevel) {
        val targetIndex = ((currentLevel - 1) / 4 * 4 - 8).coerceIn(0, totalLevels - 1)
        gridState.animateScrollToItem(targetIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 2. Fixed Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency/Gem Status Indicator (Left Side)
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .background(
                            color = Color(0xB3FFFFFF), // capsule soft white-translucent background
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.5.dp, Color.White.copy(0.6f), RoundedCornerShape(24.dp))
                        .padding(start = 28.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%,d", progress.gems),
                        color = Color(0xFF161E54), // dark navy blue font
                        fontSize = 17.sp,
                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFF26A69A), CircleShape) // neon green topping up gems
                            .border(1.dp, Color.White.copy(0.8f), CircleShape)
                            .clickable { viewModel.toggleShopDialog(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "شراء جواهر",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Left edge inner element: 3D multifaceted blue crystal gem reflecting light
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💎", fontSize = 18.sp)
                }
            }

            // Screen Title (Center Area)
            BubblyText(
                text = "المستويات",
                fontSize = 32.sp,
                fontFamily = com.example.ui.theme.cairoFontFamily,
                fillColor = Color.White,
                strokeColor = Color(0xFF161E54), // Subtle dark blue outer stroke/shadow
                strokeWidth = 10f
            )

            // Back Navigation Button (Right Side)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                    .background(Color(0xFF26A69A), CircleShape) // A perfectly circular sea-green/emerald green (#26A69A)
                    .border(2.dp, Color.White.copy(0.8f), CircleShape)
                    .clickable { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward, // white arrow pointing right (→) indicating Arabic return layout
                    contentDescription = "رجوع",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Blur Overlays & Grid Layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            // Glassmorphic background layer with backdrop blur exclusive to scrollable area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(6.dp)
                    .background(
                        color = Color(0x3B011627), // Deep beautiful translucent backdrop
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .border(1.5.dp, Color.White.copy(0.15f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            )

            // Explicit RTL ordering for level increment direction alignment
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, top = 16.dp, end = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(totalLevels) { index ->
                        val levelNum = index + 1
                        val isUnlocked = levelNum <= progress.currentRiddleLevel
                        val isSolved = levelNum < progress.currentRiddleLevel
                        val isCurrent = levelNum == progress.currentRiddleLevel

                        val tierColors = getLevelTierColors(levelNum)
                        val cardBgColor = tierColors.first
                        val cardTextColor = tierColors.second

                        if (isCurrent) {
                            // State A: Active/Current Level (Orange 3D button)
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val pressOffset by animateDpAsState(
                                targetValue = if (isPressed) 4.dp else 0.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                label = "press_offset"
                            )

                            val scaleAnim by animateFloatAsState(
                                targetValue = if (isPressed) 0.98f else 1.05f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "scale"
                            )

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
                                    .padding(bottom = 4.dp) // extra padding for bottom shadow space
                                    .offset(y = pressOffset)
                                    .shadow(
                                        elevation = if (isPressed) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        clip = false
                                    )
                                    .background(
                                        color = Color(0xFFFF9800), // Striking bright solid orange
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    // Burnt-orange bottom border/shadow layer of 4dp to give tactile look
                                    .then(
                                        if (!isPressed) {
                                            Modifier.drawBehind {
                                                drawRoundRect(
                                                    color = Color(0xFFE65100), // dark burnt-orange
                                                    topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                                    size = Size(size.width, 4.dp.toPx()),
                                                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        viewModel.selectRiddleLevel(levelNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Centered level number extra-bold solid white text
                                    BubblyText(
                                        text = "$levelNum",
                                        fontSize = 25.sp,
                                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                                        fillColor = Color.White,
                                        strokeColor = Color(0xFFE65100),
                                        strokeWidth = 6f
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "العب",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = com.example.ui.theme.cairoFontFamily
                                    )
                                }
                            }
                        } else if (isUnlocked) {
                            // State B: Unlocked & Completed levels (Procedural colored text & background)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0xFFCBD5E1),
                                        spotColor = Color(0xFF94A3B8)
                                    )
                                    .background(
                                        color = cardBgColor,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.selectRiddleLevel(levelNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$levelNum",
                                    color = cardTextColor,
                                    fontSize = 24.sp,
                                    fontFamily = com.example.ui.theme.fredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // State C: Locked Levels
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(
                                        color = Color(0x3BFFFFFF), // semi-transparent matte gray overlay
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "$levelNum",
                                        color = Color(0xFF1E3C72).copy(alpha = 0.28f),
                                        fontSize = 20.sp,
                                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "مغلق",
                                        tint = Color(0xFF94A3B8), // small silver lock icon centered
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WordsMapScreen(progress: UserProgress, viewModel: GameViewModel) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val currentLevel = progress.currentWordLevel
    val totalLevels = 1000

    LaunchedEffect(currentLevel) {
        val targetIndex = ((currentLevel - 1) / 4 * 4 - 8).coerceIn(0, totalLevels - 1)
        gridState.animateScrollToItem(targetIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 2. Fixed Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency/Gem Status Indicator (Left Side)
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .background(
                            color = Color(0xB3FFFFFF), // capsule soft white-translucent background
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.5.dp, Color.White.copy(0.6f), RoundedCornerShape(24.dp))
                        .padding(start = 28.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%,d", progress.gems),
                        color = Color(0xFF161E54), // dark navy blue font
                        fontSize = 17.sp,
                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFF26A69A), CircleShape) // neon green topping up gems
                            .border(1.dp, Color.White.copy(0.8f), CircleShape)
                            .clickable { viewModel.toggleShopDialog(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "شراء جواهر",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Left edge inner element: 3D multifaceted blue crystal gem reflecting light
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💎", fontSize = 18.sp)
                }
            }

            // Screen Title (Center Area)
            BubblyText(
                text = "التحدي اليومي",
                fontSize = 32.sp,
                fontFamily = com.example.ui.theme.cairoFontFamily,
                fillColor = Color.White,
                strokeColor = Color(0xFF161E54), // Subtle dark blue outer stroke/shadow
                strokeWidth = 10f
            )

            // Back Navigation Button (Right Side)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                    .background(Color(0xFF26A69A), CircleShape) // A perfectly circular sea-green/emerald green (#26A69A)
                    .border(2.dp, Color.White.copy(0.8f), CircleShape)
                    .clickable { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward, // white arrow pointing right (→) indicating Arabic return layout
                    contentDescription = "رجوع",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Blur Overlays & Grid Layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            // Glassmorphic background layer with backdrop blur exclusive to scrollable area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(6.dp)
                    .background(
                        color = Color(0x3B011627), // Deep beautiful translucent backdrop
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .border(1.5.dp, Color.White.copy(0.15f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            )

            // Explicit RTL ordering for level increment direction alignment
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, top = 16.dp, end = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(totalLevels) { index ->
                        val levelNum = index + 1
                        val isUnlocked = levelNum <= progress.currentWordLevel
                        val isSolved = levelNum < progress.currentWordLevel
                        val isCurrent = levelNum == progress.currentWordLevel

                        val tierColors = getLevelTierColors(levelNum)
                        val cardBgColor = tierColors.first
                        val cardTextColor = tierColors.second

                        if (isCurrent) {
                            // State A: Active/Current Level (Orange 3D button)
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val pressOffset by animateDpAsState(
                                targetValue = if (isPressed) 4.dp else 0.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                label = "press_offset"
                            )

                            val scaleAnim by animateFloatAsState(
                                targetValue = if (isPressed) 0.98f else 1.05f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "scale"
                            )

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
                                    .padding(bottom = 4.dp) // extra padding for bottom shadow space
                                    .offset(y = pressOffset)
                                    .shadow(
                                        elevation = if (isPressed) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        clip = false
                                    )
                                    .background(
                                        color = Color(0xFFFF9800), // Striking bright solid orange
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    // Burnt-orange bottom border/shadow layer of 4dp to give tactile look
                                    .then(
                                        if (!isPressed) {
                                            Modifier.drawBehind {
                                                drawRoundRect(
                                                    color = Color(0xFFE65100), // dark burnt-orange
                                                    topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                                    size = Size(size.width, 4.dp.toPx()),
                                                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        viewModel.selectWordLevel(levelNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Centered level number extra-bold solid white text
                                    BubblyText(
                                        text = "$levelNum",
                                        fontSize = 25.sp,
                                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                                        fillColor = Color.White,
                                        strokeColor = Color(0xFFE65100),
                                        strokeWidth = 6f
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "العب",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = com.example.ui.theme.cairoFontFamily
                                    )
                                }
                            }
                        } else if (isUnlocked) {
                            // State B: Unlocked & Completed levels (Procedural colored text & background)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0xFFCBD5E1),
                                        spotColor = Color(0xFF94A3B8)
                                    )
                                    .background(
                                        color = cardBgColor,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.selectWordLevel(levelNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$levelNum",
                                    color = cardTextColor,
                                    fontSize = 24.sp,
                                    fontFamily = com.example.ui.theme.fredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // State C: Locked Levels
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(
                                        color = Color(0x3BFFFFFF), // semi-transparent matte gray overlay
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "$levelNum",
                                        color = Color(0xFF1E3C72).copy(alpha = 0.28f),
                                        fontSize = 20.sp,
                                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "مغلق",
                                        tint = Color(0xFF94A3B8), // small silver lock icon centered
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiddlePlayScreen(progress: UserProgress, viewModel: GameViewModel) {
    val levelId by viewModel.selectedRiddleLevelId.collectAsState()
    val riddleInput by viewModel.riddleInput.collectAsState()
    val isChecking by viewModel.isCheckingRiddle.collectAsState()
    val level = GameData.getRiddleForLevel(levelId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper card containing the question query
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "لـغـز رقـم ",
                        color = Color(0xFFF12711),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BubblyText(
                        text = "$levelId",
                        fontSize = 24.sp,
                        fillColor = Color(0xFFFF8DA1),
                        strokeWidth = 8f
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Large styled Arabic riddle text
                Text(
                    text = level.question,
                    color = Color(0xFF333333),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }

        // Center check mechanics with artificial loading
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = riddleInput,
                onValueChange = { viewModel.updateRiddleInput(it) },
                label = { Text("اكتب إجابتك الذكية هنا...") },
                placeholder = { Text("مثال: الديك لا يبيض") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFF12711), RoundedCornerShape(12.dp))
                    .testTag("riddle_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isChecking) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "جاري التحقق بتقنيات الذكاء الاصطناعي...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.submitRiddleAnswer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("riddle_verify_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "التحقق بالفحص الذكي للذكاء الاصطناعي"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التحقق بالذكاء الاصطناعي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sliding anime motivational riddle banner
        val riddleMotivations = listOf(
            "الألغاز تنشط الوصلات العصبية وتصنع عقلاً خارقاً! 🧠🔥",
            "كل مفكر كبير يبدأ بحل لغز صغير خطوة بخطوة 🌟",
            "إذا استصعبت الحل، تفكر خارج الصندوق، فالإجابة قريبة جداً 🏹",
            "واصل التطور وافتح مخترع الألغاز الكامن بداخلك 💪"
        )
        val selectedRiddleMotiv = remember(levelId) { riddleMotivations.random() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF263238).copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFFA726).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "💡", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedRiddleMotiv,
                color = Color(0xFFFFB74D),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Dummy spacing to balance weights
        Spacer(modifier = Modifier.height(10.dp))
    }

    // Success overlay dialogue
    if (viewModel.showRiddleSuccessDialog.collectAsState().value) {
        SuccessOverlayDialog(
            title = "أحسنت الإجابة! 🎉",
            body = "إجابتك صحيحة وسليمة تماماً. الإجابة النموذجية هي: \n\"${level.answer}\"",
            reward = "+5 جوهرة",
            onDismiss = { viewModel.dismissRiddleSuccess() }
        )
    }
}

@Composable
fun WordPlayScreen(progress: UserProgress, viewModel: GameViewModel) {
    val levelId by viewModel.selectedWordLevelId.collectAsState()
    val swipedWord by viewModel.swipedWord.collectAsState()
    val foundWords by viewModel.foundWords.collectAsState()
    val selectedGridCells by viewModel.selectedGridCells.collectAsState()
    val foundWordPaths by viewModel.foundWordPaths.collectAsState()
    val swipeAttemptCount by viewModel.swipeAttemptCount.collectAsState()
    val level = remember(levelId) { GameData.getWordLevel(levelId) }

    val wordColorsList = remember {
        listOf(
            Color(0xFFE91E63), // Modern Pink
            Color(0xFF9C27B0), // Purple
            Color(0xFF00BCD4), // Cool Cyan
            Color(0xFF4CAF50), // Fresh Green
            Color(0xFFFF5722), // Salmon Coral
            Color(0xFF3F51B5), // Indigo Blue
            Color(0xFF2196F3)  // Bright Sky Blue
        )
    }

    // Track layout bounds of each grid cell to accurately map drags/swipes
    val cellBounds = remember { mutableMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }
    var containerCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    val isVerifying by viewModel.isVerifying.collectAsState()
    val verificationResult by viewModel.verificationResult.collectAsState()

    // Shake animation state for incorrect validation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isVerifying, verificationResult) {
        if (isVerifying && verificationResult == false) {
            repeat(4) {
                shakeOffset.animateTo(12f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-12f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(40, easing = LinearEasing))
        }
    }

    // White Flash pop alpha animation for correct response
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(isVerifying, verificationResult) {
        if (isVerifying && verificationResult == true) {
            flashAlpha.snapTo(1f)
            flashAlpha.animateTo(0f, animationSpec = tween(500))
        }
    }

    // Particle effect states
    val particles = remember { mutableStateListOf<SparkleParticle>() }

    // Particle update Loop ticking every 16ms
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            if (particles.isNotEmpty()) {
                val updated = particles.map { p ->
                    p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        vx = p.vx * 0.98f, // slow deceleration
                        vy = p.vy + 0.12f, // downward gravitational pull
                        alpha = (p.life - p.decay).coerceIn(0f, 1f),
                        life = p.life - p.decay
                    )
                }.filter { it.life > 0f }
                particles.clear()
                particles.addAll(updated)
            }
        }
    }

    // Particle Spawning on touch/drag movement
    LaunchedEffect(currentDragPosition) {
        currentDragPosition?.let { pos ->
            repeat(4) {
                val angle = Math.random() * 2 * Math.PI
                val speed = (1f + Math.random() * 4f).toFloat()
                particles.add(
                    SparkleParticle(
                         x = pos.x,
                         y = pos.y,
                         vx = (Math.cos(angle) * speed).toFloat(),
                         vy = (Math.sin(angle) * speed - 1.5f).toFloat(), // scatter upwards/around
                         size = (4f + Math.random() * 8f).toFloat(),
                         alpha = 1.0f,
                         life = 1.0f,
                         decay = (0.04f + Math.random() * 0.04f).toFloat()
                    )
                )
            }
        }
    }

    // Kinetic mechanical counter for gems
    val animatedGems = remember { Animatable(progress.gems.toFloat()) }
    LaunchedEffect(progress.gems) {
        animatedGems.animateTo(
            targetValue = progress.gems.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Stat-reward flying "+10" anim states
    var showPlusTenAnimation by remember { mutableStateOf(false) }
    var plusTenOffset by remember { mutableStateOf(0f) }
    var plusTenAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(isVerifying, verificationResult) {
        if (isVerifying && verificationResult == true) {
            plusTenOffset = 180f // start at the board center
            plusTenAlpha = 1f
            showPlusTenAnimation = true
            
            // upward translation
            val animJob = launch {
                animate(
                    initialValue = 180f,
                    targetValue = -300f,
                    animationSpec = tween(1100, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    plusTenOffset = value
                }
            }
            val alphaJob = launch {
                animate(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = tween(1100, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    plusTenAlpha = value
                }
            }
            
            animJob.join()
            alphaJob.join()
            showPlusTenAnimation = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "word_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val floatTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Active clue navigation state
    var activeClueIndex by remember(levelId) { mutableStateOf(0) }
    val totalClues = level.wordsToFind.size
    
    // Safety check for out of bounds
    if (activeClueIndex >= totalClues) {
        activeClueIndex = 0
    }

    // Live Running Timer state
    var secondsPassed by remember(levelId) { mutableStateOf(9) }
    LaunchedEffect(levelId) {
        secondsPassed = 0
        while (true) {
            kotlinx.coroutines.delay(1000)
            secondsPassed++
        }
    }
    val timerText = String.format("%02d:%02d", secondsPassed / 60, secondsPassed % 60)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // 1. TOP HEADER ROW (Gems Capsule + Level display + Green Back button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gems capsule on the Left
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.clickable { viewModel.toggleShopDialog(true) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .background(
                            color = Color(0x7F0D1B2A),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.5.dp, Color.White.copy(0.45f), RoundedCornerShape(24.dp))
                        .padding(start = 24.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%,d", animatedGems.value.toInt()),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = com.example.ui.theme.fredokaFontFamily,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(1.dp, Color.White.copy(0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "شراء جواهر",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💎", fontSize = 18.sp)
                }
            }

            // Center: Level display ("المستوى X")
            BubblyText(
                text = "المستوى $levelId",
                fontSize = 24.sp,
                fontFamily = com.example.ui.theme.cairoFontFamily,
                fillColor = Color.White,
                strokeColor = Color(0xFF1E3C72).copy(alpha = 0.6f),
                strokeWidth = 10f
            )

            // Right: Help/Tutorial + Green circle Back Button Layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info/Tutorial circular button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF3F51B5), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(6.dp, CircleShape)
                        .clickable { viewModel.toggleTutorialDialog(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❔",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF10B981), CircleShape) // Vibrant mint green
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(6.dp, CircleShape)
                        .clickable { viewModel.navigateTo(GameScreen.WORDS_MAP) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward, // back arrow pointing right in RTL
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. OVERLAPPING CLUE PANEL WITH FIXED TIMER PILL ON TOP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // The Main Clue Card Content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp) // Space for overlapping timer
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activeWord = level.wordsToFind.getOrNull(activeClueIndex) ?: ""
                    val isSolved = foundWords.contains(activeWord)
                    val info = WordClueLookup.getClueForWord(activeWord)

                    AnimatedContent(
                        targetState = activeClueIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "clue_anim"
                    ) { index ->
                        val word = level.wordsToFind.getOrNull(index) ?: ""
                        val wordIsSolved = foundWords.contains(word)
                        val wordInfo = WordClueLookup.getClueForWord(word)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (wordIsSolved) {
                                Text(
                                    text = "حُلت: $word ✅",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 22.sp,
                                    fontFamily = com.example.ui.theme.cairoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = wordInfo.riddle,
                                    color = Color(0xFF1E3C72),
                                    fontSize = 17.sp,
                                    fontFamily = com.example.ui.theme.cairoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimal Pager Tabs indicators for multiple target clues
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        level.wordsToFind.forEachIndexed { idx, word ->
                            val isWordSolved = foundWords.contains(word)
                            val isCurrentTab = idx == activeClueIndex
                            Box(
                                modifier = Modifier
                                    .size(width = if (isCurrentTab) 22.dp else 8.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrentTab -> Color(0xFFFFA726)
                                            isWordSolved -> Color(0xFF4CAF50)
                                            else -> Color.LightGray.copy(alpha = 0.6f)
                                        }
                                    )
                                    .clickable { activeClueIndex = idx }
                            )
                        }
                    }
                }
            }

            // The Floating Timer Pill overlapping the top border of the Clue Card
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFA726), Color(0xFFFF8F00))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 5.dp)
            ) {
                Text(
                    text = timerText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = com.example.ui.theme.fredokaFontFamily,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Swiped Word real-time preview (Floating beautifully) -> Omit/Replace with simple space to avoid duplication!
        Spacer(modifier = Modifier.height(8.dp))

        // 3. 4x4 Connected Swipe Board with Rounded 3D Square Cards
        Box(
            modifier = Modifier
                .size(310.dp)
                .background(Color.Black.copy(0.35f), RoundedCornerShape(24.dp))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { containerCoordinates = it }
                    .pointerInput(level.gridLetters) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val rootPos = containerCoordinates?.positionInRoot() ?: Offset.Zero
                                val offsetInRoot = rootPos + offset
                                val hitCellIndex = cellBounds.entries
                                    .firstOrNull { it.value.contains(offsetInRoot) }?.key
                                if (hitCellIndex != null) {
                                    currentDragPosition = offset
                                    viewModel.startSwipe(hitCellIndex, level.gridLetters[hitCellIndex])
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentDragPosition = (currentDragPosition ?: Offset.Zero) + dragAmount
                                currentDragPosition?.let { pos ->
                                    val rootPos = containerCoordinates?.positionInRoot() ?: Offset.Zero
                                    val offsetInRoot = rootPos + pos
                                    val hitCellIndex = cellBounds.entries
                                        .firstOrNull { it.value.contains(offsetInRoot) }?.key
                                    if (hitCellIndex != null) {
                                        viewModel.continueSwipe(hitCellIndex, level.gridLetters[hitCellIndex])
                                    }
                                }
                            },
                            onDragEnd = {
                                currentDragPosition = null
                                viewModel.finishWordSwipe()
                            },
                            onDragCancel = {
                                currentDragPosition = null
                                viewModel.cancelWordSwipe()
                            }
                        )
                    }
            ) {
                // Letter Cards Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(level.gridLetters) { index, letter ->
                        val isCellSelected = selectedGridCells.contains(index)
                        val isCellPartOfSolved = foundWordPaths.any { it.contains(index) }

                        val scale by animateFloatAsState(if (isCellSelected) 1.25f else 1.0f, label = "cell_scale")
                        val shape = RoundedCornerShape(16.dp)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .onGloballyPositioned { layoutCoordinates ->
                                    cellBounds[index] = androidx.compose.ui.geometry.Rect(
                                        layoutCoordinates.positionInRoot(),
                                        layoutCoordinates.size.toSize()
                                    )
                                }
                                .clip(shape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = when {
                                            isCellSelected -> {
                                                when {
                                                    isVerifying && verificationResult == true -> listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)) // grass green
                                                    isVerifying && verificationResult == false -> listOf(Color(0xFFF44336), Color(0xFFD32F2F)) // vibrant red
                                                    else -> listOf(Color(0xFFFFEB3B), Color(0xFFFBC02D)) // vibrant glowing active yellow!
                                                }
                                            }
                                            isCellPartOfSolved -> listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)) // correct grass-green Completion color!
                                            else -> listOf(Color(0xFFFFFFFF), Color(0xFFECF2F6)) // unselected off-white
                                        }
                                    )
                                )
                                .border(
                                    width = if (isCellSelected || isCellPartOfSolved) 2.5.dp else 1.dp,
                                    color = if (isCellSelected || isCellPartOfSolved) Color.White else Color.White.copy(alpha = 0.6f),
                                    shape = shape
                                )
                                .shadow(
                                    elevation = if (isCellSelected || isCellPartOfSolved) 2.dp else 4.dp,
                                    shape = shape
                                )
                                .testTag("word_cell_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                color = if (isCellSelected || isCellPartOfSolved) Color.White else Color(0xFF1E293B),
                                fontSize = 28.sp,
                                fontFamily = com.example.ui.theme.cairoFontFamily,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Foreground Canvas overlay removed to match clean video highlights without lines or followers.
            }

            // White starlight Flash Pop overlay!
            if (flashAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha.value * 0.75f), RoundedCornerShape(24.dp))
                )
            }
        }

        // Real-time Swiped Words "الكلمات الممكنة" Dynamic smoky gray Capsule directly below the puzzle box
        Box(
            modifier = Modifier
                .height(56.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (swipedWord.isNotEmpty()) {
                val capsuleBgColor = when {
                    isVerifying && verificationResult == true -> Color(0xFF4CAF50)
                    isVerifying && verificationResult == false -> Color(0xFFD32F2F)
                    else -> Color(0xFF37474F).copy(alpha = 0.85f)
                }
                
                Box(
                    modifier = Modifier
                        .graphicsLayer(translationX = shakeOffset.value) // SHAKE EFFECT on wrong submission
                        .background(capsuleBgColor, RoundedCornerShape(24.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "الكلمة الممكنة: $swipedWord",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // 3.5 Center underline issue reporter link
        val context = androidx.compose.ui.platform.LocalContext.current
        Text(
            text = "الإبلاغ عن مشكلة",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            fontFamily = com.example.ui.theme.cairoFontFamily,
            modifier = Modifier
                .clickable {
                    Toast.makeText(context, "تم إرسال بلاغك للمراجعة! شكرًا لك 🛠️", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 4.dp)
        )

        // 4. LOWER ACTION/BOOSTER BUTTONS (Flame 150 & Target 100)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Booster (Fire/Flame 🔥)
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFECEFF1))
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape)
                        .clickable { viewModel.useFireBooster() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 34.sp)
                }

                Box(
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .background(Color(0xFF263238).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "150",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.fredokaFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        BubblyText(text = "💎", fontSize = 10.sp)
                    }
                }
            }

            // Right Booster (Target Bullseye 🎯)
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFECEFF1))
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape)
                        .clickable { viewModel.useTargetBooster() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎯", fontSize = 34.sp)
                }

                Box(
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .background(Color(0xFF263238).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "100",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.fredokaFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        BubblyText(text = "💎", fontSize = 10.sp)
                    }
                }
            }
        }

        if (showPlusTenAnimation) {
            Box(
                modifier = Modifier
                    .offset(y = plusTenOffset.dp)
                    .graphicsLayer(alpha = plusTenAlpha)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFA726), RoundedCornerShape(16.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+10 💎",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = com.example.ui.theme.fredokaFontFamily
                )
            }
        }
    }
}
}

@Composable
fun CongratulationsScreen(progress: UserProgress, viewModel: GameViewModel) {
    val lastCompletedType by viewModel.lastCompletedType.collectAsState()
    val lastCompletedLevelId by viewModel.lastCompletedLevelId.collectAsState()
    val lastCompletedReward by viewModel.lastCompletedReward.collectAsState()

    // 1. Victory Title Scale Pop Up Animation
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.25f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Rank local conversion helper
    fun getRankForLevel(levelId: Int): Pair<String, String> {
        return when {
            levelId <= 2 -> "برونزي I" to "برونزي II"
            levelId <= 4 -> "برونزي II" to "برونزي III"
            levelId <= 6 -> "برونزي III" to "فضي I"
            levelId <= 8 -> "فضي I" to "فضي II"
            levelId <= 10 -> "فضي II" to "فضي III"
            levelId <= 15 -> "فضي III" to "ذهبي I"
            levelId <= 20 -> "ذهبي I" to "ذهبي II"
            levelId <= 30 -> "ذهبي II" to "ذهبي III"
            else -> "ذهبي III" to "أمير الألغاز 👑"
        }
    }

    // Rank data
    val (currentRank, nextRank) = remember(lastCompletedLevelId) {
        getRankForLevel(lastCompletedLevelId)
    }
    val targetPercent = remember(lastCompletedLevelId) {
        ((lastCompletedLevelId * 25) % 100).coerceAtLeast(15)
    }
    
    // 2. Linear Progress smooth animation from 0% to the targetPercent (0f to 1f representation)
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(targetPercent) {
        progressAnim.animateTo(
            targetValue = targetPercent / 100f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    // Discovered words count text
    val discoveredWordsText = remember(lastCompletedType, lastCompletedLevelId) {
        if (lastCompletedType == "words") {
            val level = GameData.getWordLevel(lastCompletedLevelId)
            "+${level.wordsToFind.size} كلمة"
        } else {
            "+1 لغز"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            )
    ) {
        FallingConfettiEffect()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // -- UPPER BLOCK: "عبقري!" with Scale Pop-Up --
            Text(
                text = "عبـقـري! 🧠",
                color = Color(0xFF0D47A1), // Deep Nile Blue
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                fontFamily = com.example.ui.theme.cairoFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    .shadow(12.dp, shape = RoundedCornerShape(12.dp), ambientColor = Color(0xFF0D47A1))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // -- MIDDLE BLOCK: Glassmorphism Rank Progress Card --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    .background(Color.White.copy(0.12f), RoundedCornerShape(28.dp))
                    .border(1.5.dp, Color.White.copy(0.25f), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header listing current and next rank
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentRank,
                            color = Color.White.copy(0.85f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                        Text(
                            text = "◀",
                            color = Color.White.copy(0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = nextRank,
                            color = Color(0xFF00E5FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }

                    // Progress percentage label
                    Text(
                        text = "$targetPercent/100",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.example.ui.theme.fredokaFontFamily
                    )

                    // 3D/Fluid custom canvas progress bar filling RTL (right to left)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                    ) {
                        val barWidth = size.width
                        val barHeight = size.height
                        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)

                        // 1. Draw Background Track
                        drawRoundRect(
                            color = Color.White.copy(0.15f),
                            size = size,
                            cornerRadius = cornerRadius
                        )

                        // 2. Draw animated filled progress from Right to Left!
                        val filledWidth = barWidth * progressAnim.value
                        drawRoundRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(x = barWidth - filledWidth, y = 0f),
                            size = androidx.compose.ui.geometry.Size(width = filledWidth, height = barHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // Discovered words capsule
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(0.3f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "عدد الكلمات المكتشفة: $discoveredWordsText",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // -- LOWER BLOCK: Giant 3D Action Buttons --
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val nextLevelNum = lastCompletedLevelId + 1

                // 1. Next Level 3D Button (Orange Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { viewModel.nextLevelFromSuccess() }
                ) {
                    // Shadow layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .offset(y = 6.dp)
                            .background(Color(0xFFE65100), RoundedCornerShape(32.dp))
                    )
                    // Button Layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00))
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(1.5.dp, Color.White.copy(0.40f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "المستوى $nextLevelNum",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }
                }

                // 2. Main Menu 3D Button (Purple Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                ) {
                    // Shadow layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .offset(y = 6.dp)
                            .background(Color(0xFF4A148C), RoundedCornerShape(32.dp))
                    )
                    // Button Layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFBA68C8), Color(0xFF7B1FA2))
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(1.5.dp, Color.White.copy(0.35f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الرئيسية",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessOverlayDialog(
    title: String,
    body: String,
    reward: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Trophy Vector Image
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "ذهبية",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    color = Color(0xFF38EF7D),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = body,
                    color = Color.DarkGray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reward State
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "المكافأة المحرزة: $reward 💎",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("متابعة للخريطة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DailyRewardDialog(progress: UserProgress, viewModel: GameViewModel) {
    Dialog(
        onDismissRequest = { viewModel.toggleDailyRewardDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(top = 28.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            // Main white container box with 28dp smooth rounded corners and thick violet base
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(26.dp)
                        clip = true
                    }
                    .background(Color.White)
            ) {
                // Background & thick base
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Draw thick dark purple bottom bar representation (base)
                            val heightOffset = 8.dp.toPx()
                            drawRoundRect(
                                color = Color(0xFF4A148C), // Deep premium purple base
                                topLeft = Offset(0f, size.height - heightOffset),
                                size = Size(size.width, heightOffset)
                            )
                        }
                        .padding(bottom = 8.dp) // Offset for the thick purple border base
                ) {
                    Spacer(modifier = Modifier.height(28.dp))

                    // Centered oval blue badge title
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(Color(0xFFE1F5FE), RoundedCornerShape(24.dp))
                            .border(1.5.dp, Color(0xFFB3E5FC), RoundedCornerShape(24.dp))
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "المهام اليومية",
                            color = Color(0xFF0D47A1), // Dark blue text
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tasks list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val unlockedBgSize = progress.unlockedBackgrounds.split(",").size
                        val isBgUnlocked = if (unlockedBgSize > 1) 1 else 0

                        val m1 = MissionModel(
                            id = 1,
                            title = "اشترى خلفية جديدة",
                            current = isBgUnlocked,
                            target = 1,
                            gemsReward = 50,
                            isClaimed = viewModel.mission1Claimed.collectAsState().value
                        )
                        val m2 = MissionModel(
                            id = 2,
                            title = "التحدي اليومي خلال 5 دقائق",
                            current = if (viewModel.dailyChallengeFinished.collectAsState().value) 1 else 0,
                            target = 1,
                            gemsReward = 75,
                            isClaimed = viewModel.mission2Claimed.collectAsState().value
                        )
                        val m3 = MissionModel(
                            id = 3,
                            title = "استخدم كشف الحرف 3 مرات",
                            current = viewModel.hintsUsedCount.collectAsState().value,
                            target = 3,
                            gemsReward = 100,
                            isClaimed = viewModel.mission3Claimed.collectAsState().value
                        )
                        val m4 = MissionModel(
                            id = 4,
                            title = "العب لمدة 45 دقيقة",
                            current = viewModel.playTimeMinutes.collectAsState().value,
                            target = 45,
                            gemsReward = 125,
                            isClaimed = viewModel.mission4Claimed.collectAsState().value
                        )
                        val m5 = MissionModel(
                            id = 5,
                            title = "10 مستويات متتالية بدون مساعدة",
                            current = viewModel.consecutiveNoHelp.collectAsState().value,
                            target = 10,
                            gemsReward = 150,
                            isClaimed = viewModel.mission5Claimed.collectAsState().value
                        )

                        val allMissions = listOf(m1, m2, m3, m4, m5)

                        allMissions.forEachIndexed { idx, mission ->
                            MissionRow(mission) {
                                viewModel.claimMission(mission.id)
                            }
                            if (idx < allMissions.lastIndex) {
                                Divider(
                                    color = Color(0xFFEEEEEE),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Optional Reset button for endless replay and testing
                    TextButton(
                        onClick = { viewModel.resetMissions() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "إعادة ضبط وتحديث المهام اليومية 🔄",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Top-Left prominent offset orange/red close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-12).dp)
                    .size(38.dp)
                    .shadow(6.dp, CircleShape)
                    .background(Color(0xFFFF3D00), CircleShape) // bright red circle close
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { viewModel.toggleDailyRewardDialog(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.example.ui.theme.fredokaFontFamily
                )
            }
        }
    }
}

// Data class representing missions inside GameApp.kt
data class MissionModel(
    val id: Int,
    val title: String,
    val current: Int,
    val target: Int,
    val gemsReward: Int,
    val isClaimed: Boolean
)

@Composable
fun MissionRow(mission: MissionModel, onClaim: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right Side: Task description text + Progress state
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = mission.title,
                color = Color(0xFF1565C0), // Blue text
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = com.example.ui.theme.cairoFontFamily
            )
            
            // Progress bubble e.g. "0/3"
            Text(
                text = "${mission.current.coerceAtMost(mission.target)}/${mission.target}",
                color = if (mission.current >= mission.target) Color(0xFF4CAF50) else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = com.example.ui.theme.fredokaFontFamily
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Left Side: Reward display + claims action button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mission.isClaimed) {
                // Already Claimed text
                Text(
                    text = "✓ تم الاستلام",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.example.ui.theme.cairoFontFamily
                )
            } else if (mission.current >= mission.target) {
                // Ready to claim bouncing button
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                        .clickable { onClaim() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "استلم",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.example.ui.theme.cairoFontFamily
                    )
                }
            } else {
                // Reward info (blue diamond icon 💎 + purple reward value)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💎",
                        fontSize = 15.sp
                    )
                    Text(
                        text = "+${mission.gemsReward}",
                        color = Color(0xFF7B1FA2), // Purple text
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.example.ui.theme.fredokaFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun CustomSpeakerIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw the speaker body
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w * 0.7f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.65f)
            close()
        }
        drawPath(path, color = tint)
        
        // Draw 2 simple sound wave arcs on the right side using standard drawArc
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.4f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.2f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.8f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
fun CustomMusicNoteIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Two filled circles at the bottom
        drawCircle(
            color = tint,
            radius = w * 0.15f,
            center = Offset(w * 0.3f, h * 0.75f)
        )
        drawCircle(
            color = tint,
            radius = w * 0.15f,
            center = Offset(w * 0.75f, h * 0.65f)
        )
        
        // Stems going up
        drawLine(
            color = tint,
            start = Offset(w * 0.42f, h * 0.75f),
            end = Offset(w * 0.42f, h * 0.2f),
            strokeWidth = 2.5.dp.toPx()
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.87f, h * 0.65f),
            end = Offset(w * 0.87f, h * 0.1f),
            strokeWidth = 2.5.dp.toPx()
        )
        
        // Connecting bar at the top
        val barPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.4f, h * 0.28f)
            lineTo(w * 0.9f, h * 0.18f)
            lineTo(w * 0.9f, h * 0.05f)
            lineTo(w * 0.4f, h * 0.15f)
            close()
        }
        drawPath(barPath, color = tint)
    }
}

@Composable
fun CustomVibrationIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw phone in the middle
        val phoneRect = androidx.compose.ui.geometry.RoundRect(
            left = w * 0.32f,
            top = h * 0.15f,
            right = w * 0.68f,
            bottom = h * 0.85f,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(phoneRect)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        
        // Home key or speaker bar
        drawCircle(
            color = tint,
            radius = 2.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.75f)
        )
        
        // Wave lines on the left side
        val waveL = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            quadraticTo(w * 0.12f, h * 0.5f, w * 0.2f, h * 0.65f)
            moveTo(w * 0.12f, h * 0.25f)
            quadraticTo(w * 0.02f, h * 0.5f, w * 0.12f, h * 0.75f)
        }
        drawPath(
            path = waveL,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Wave lines on the right side
        val waveR = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.8f, h * 0.35f)
            quadraticTo(w * 0.88f, h * 0.5f, w * 0.8f, h * 0.65f)
            moveTo(w * 0.88f, h * 0.25f)
            quadraticTo(w * 0.98f, h * 0.5f, w * 0.88f, h * 0.75f)
        }
        drawPath(
            path = waveR,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
fun CustomImageIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Image frame
        val frameRect = androidx.compose.ui.geometry.RoundRect(
            left = w * 0.15f,
            top = h * 0.2f,
            right = w * 0.85f,
            bottom = h * 0.8f,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(frameRect)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        
        // Sun/Circle in the picture
        drawCircle(
            color = tint,
            radius = w * 0.08f,
            center = Offset(w * 0.35f, h * 0.42f)
        )
        
        // Mountains inside the frame
        val mountPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.74f)
            lineTo(w * 0.45f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.65f)
            lineTo(w * 0.72f, h * 0.53f)
            lineTo(w * 0.8f, h * 0.74f)
        }
        drawPath(
            path = mountPath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

@Composable
fun SettingsDialog(progress: UserProgress, viewModel: GameViewModel) {
    Dialog(
        onDismissRequest = { viewModel.toggleSettingsDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .padding(top = 40.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Main White Container Card with 28dp smooth rounded corners and Drop Shadow with blue glow hint
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0xFF4D69DA).copy(alpha = 0.25f),
                            spotColor = Color(0xFF4D69DA).copy(alpha = 0.5f)
                        )
                        .border(1.5.dp, Color(0xFFE8EFFF), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 46.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Section One: Audio and Vibration [الصوت والاهتزاز]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.7f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Section Header Bar: Pale blue background & speaker icon
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEBF1FF))
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    CustomSpeakerIcon(
                                        tint = Color(0xFF283593),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الصوت والاهتزاز",
                                        color = Color(0xFF283593),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = com.example.ui.theme.cairoFontFamily
                                    )
                                }

                                // Interactive controls and slider lists inside
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 1. Sounds Effects Control
                                    var soundVol by remember { mutableStateOf(if (progress.soundEnabled) 1f else 0f) }
                                    val soundPercent = (soundVol * 100).toInt()

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CustomSpeakerIcon(
                                                    tint = Color(0xFFAB47BC), // Pale purple/mauve
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "الأصوات",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = com.example.ui.theme.cairoFontFamily
                                                )
                                            }
                                            Text(
                                                text = "$soundPercent%",
                                                color = Color(0xFF283593),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = com.example.ui.theme.cairoFontFamily
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Slider(
                                            value = soundVol,
                                            onValueChange = {
                                                soundVol = it
                                                viewModel.toggleSound(it > 0.05f)
                                            },
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF4D69DA),
                                                inactiveTrackColor = Color(0xFFE8EFFF)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                        )
                                    }

                                    // Faint Sep Line 1
                                    Divider(
                                        color = Color(0xFFE0E0E0).copy(alpha = 0.8f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )

                                    // 2. Background Music Control
                                    var musicVol by remember { mutableStateOf(1f) }
                                    val musicPercent = (musicVol * 100).toInt()

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CustomMusicNoteIcon(
                                                    tint = Color(0xFFAB47BC), // Pale purple/mauve
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "أصوات الخلفية",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = com.example.ui.theme.cairoFontFamily
                                                )
                                            }
                                            Text(
                                                text = "$musicPercent%",
                                                color = Color(0xFF283593),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = com.example.ui.theme.cairoFontFamily
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Slider(
                                            value = musicVol,
                                            onValueChange = {
                                                musicVol = it
                                            },
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF4D69DA),
                                                inactiveTrackColor = Color(0xFFE8EFFF)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                        )
                                    }

                                    // Faint Sep Line 2
                                    Divider(
                                        color = Color(0xFFE0E0E0).copy(alpha = 0.8f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )

                                    // 3. Vibration Level Toggle Control
                                    val vibrationOn = progress.vibrationEnabled
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CustomVibrationIcon(
                                                tint = Color(0xFFAB47BC), // Pale purple/mauve
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "الاهتزاز",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = com.example.ui.theme.cairoFontFamily
                                            )
                                        }

                                        // Custom animated toggle capsule with white knob
                                        Box(
                                            modifier = Modifier
                                                .width(52.dp)
                                                .height(30.dp)
                                                .background(
                                                    color = if (vibrationOn) Color(0xFF4D69DA) else Color(0xFFE0E0E0),
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.toggleVibration(!vibrationOn) }
                                                .padding(3.dp),
                                            contentAlignment = if (vibrationOn) Alignment.CenterStart else Alignment.CenterEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color.White, CircleShape)
                                                    .shadow(2.dp, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section Two: Game Backgrounds [خلفيات اللعبة]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.7f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Section Header Bar: Pale blue background & gallery/image icon
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEBF1FF))
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    CustomImageIcon(
                                        tint = Color(0xFF283593),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "خلفيات اللعبة",
                                        color = Color(0xFF283593),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = com.example.ui.theme.cairoFontFamily
                                    )
                                }

                                // Interactive Big 3D Pop Button (تصفح الخلفيات) inside white area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val offsetBy = if (isPressed) 3.dp else 0.dp

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(58.dp)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                viewModel.toggleSettingsDialog(false)
                                                viewModel.toggleShopDialog(true)
                                            }
                                    ) {
                                        // 3D Shadow/Depth Layer (Dark purple)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(Color(0xFF7B1FA2), RoundedCornerShape(20.dp))
                                        )
                                        // Active/Face Layer (Radiant purple BA68C8)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .align(Alignment.TopCenter)
                                                .offset(y = offsetBy)
                                                .background(Color(0xFFBA68C8), RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "تصفح الخلفيات",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                fontFamily = com.example.ui.theme.cairoFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Area: Preserve Player Name Changing Action seamlessly and elegantly
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleSettingsDialog(false)
                                viewModel.toggleNameChangeDialog(true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF283593))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF283593)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تغيير اسم اللاعب (الحالي: ${progress.username})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = com.example.ui.theme.cairoFontFamily
                            )
                        }
                    }
                }

                // 2. Head Capsule (عنوان النافذة: الإعدادات) merging with the container and protruding half outwards
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp)
                        .background(Color(0xFFE8EFFF), RoundedCornerShape(24.dp))
                        .border(1.5.dp, Color(0xFF4D69DA).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 36.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "الإعدادات",
                        color = Color(0xFF283593),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                // 3. Outstanding Close Button prominently overlapping top-left corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-12).dp)
                        .size(38.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(6.dp, CircleShape)
                        .clickable { viewModel.toggleSettingsDialog(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

data class RankDialogItem(
    val isBronze: Boolean,
    val isSapphire: Boolean,
    val number: Int,
    val title: String,
    val isCompleted: Boolean,
    val conditionText: String
)

@Composable
fun MedalIcon(isBronze: Boolean, isSapphire: Boolean = false, number: Int = 1) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(38.dp, 48.dp)) {
        val w = size.width
        val h = size.height
        
        if (isSapphire) {
            // Ribbon going down straight fully: Sky blue sides, dark blue center
            drawRect(
                color = Color(0xFF81D4FA),
                topLeft = Offset(w * 0.18f, 0f),
                size = androidx.compose.ui.geometry.Size(w * 0.64f, h)
            )
            drawRect(
                color = Color(0xFF1565C0),
                topLeft = Offset(w * 0.32f, 0f),
                size = androidx.compose.ui.geometry.Size(w * 0.36f, h)
            )
            
            // Top partial circle of sapphire medal
            drawCircle(
                color = Color(0xFF0D47A1), // Sapphire rim
                radius = w * 0.38f,
                center = Offset(w * 0.5f, h * 0.95f)
            )
            drawCircle(
                color = Color(0xFF1E88E5), // Sapphire core
                radius = w * 0.30f,
                center = Offset(w * 0.5f, h * 0.94f)
            )
        } else {
            // Inverted V shape ribbon strap
            val outerRibbon = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.2f, 0f)
                lineTo(w * 0.5f, h * 0.45f)
                lineTo(w * 0.8f, 0f)
                lineTo(w * 0.9f, 0f)
                lineTo(w * 0.5f, h * 0.55f)
                lineTo(w * 0.1f, 0f)
                close()
            }
            drawPath(outerRibbon, color = Color(0xFF81D4FA))
            
            val innerRibbon = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.3f, 0f)
                lineTo(w * 0.5f, h * 0.35f)
                lineTo(w * 0.7f, 0f)
                lineTo(w * 0.8f, 0f)
                lineTo(w * 0.5f, h * 0.48f)
                lineTo(w * 0.2f, 0f)
                close()
            }
            drawPath(innerRibbon, color = Color(0xFF1565C0))
            
            // Circular Medal at lower half
            val medalCenter = Offset(w * 0.5f, h * 0.65f)
            val medalRadius = w * 0.32f
            
            val baseColor = if (isBronze) Color(0xFFD87040) else Color(0xFFC0C0C0)
            val rimColor = if (isBronze) Color(0xFF8B4513) else Color(0xFF707070)
            
            drawCircle(
                color = baseColor,
                radius = medalRadius,
                center = medalCenter
            )
            drawCircle(
                color = rimColor,
                radius = medalRadius,
                center = medalCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            
            // Draw Roman number paths
            val textPath = androidx.compose.ui.graphics.Path()
            if (number == 1) {
                textPath.moveTo(w * 0.5f, h * 0.55f)
                textPath.lineTo(w * 0.5f, h * 0.75f)
            } else if (number == 2) {
                textPath.moveTo(w * 0.42f, h * 0.57f)
                textPath.quadraticTo(w * 0.5f, h * 0.52f, w * 0.58f, h * 0.57f)
                textPath.lineTo(w * 0.42f, h * 0.73f)
                textPath.lineTo(w * 0.58f, h * 0.73f)
            } else if (number == 3) {
                textPath.moveTo(w * 0.42f, h * 0.56f)
                textPath.quadraticTo(w * 0.55f, h * 0.61f, w * 0.42f, h * 0.65f)
                textPath.quadraticTo(w * 0.55f, h * 0.69f, w * 0.42f, h * 0.74f)
            }
            drawPath(
                path = textPath,
                color = rimColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

@Composable
fun RanksDialog(progress: UserProgress, viewModel: GameViewModel) {
    Dialog(
        onDismissRequest = { viewModel.toggleRanksDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .padding(top = 40.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Main Container Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0xFF4D69DA).copy(alpha = 0.25f),
                            spotColor = Color(0xFF4D69DA).copy(alpha = 0.5f)
                        )
                        .border(1.5.dp, Color(0xFFE8EFFF), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .clip(androidx.compose.ui.graphics.RectangleShape)
                            .padding(top = 36.dp, bottom = 0.dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        val ranksList = listOf(
                            RankDialogItem(isBronze = true, isSapphire = false, number = 1, title = "برونزي I", isCompleted = true, conditionText = ""),
                            RankDialogItem(isBronze = true, isSapphire = false, number = 2, title = "برونزي II", isCompleted = false, conditionText = "0/100"),
                            RankDialogItem(isBronze = true, isSapphire = false, number = 3, title = "برونزي III", isCompleted = false, conditionText = "250 كلمة"),
                            RankDialogItem(isBronze = false, isSapphire = false, number = 1, title = "فضي I", isCompleted = false, conditionText = "450 كلمة"),
                            RankDialogItem(isBronze = false, isSapphire = false, number = 2, title = "فضي II", isCompleted = false, conditionText = "700 كلمة"),
                            RankDialogItem(isBronze = false, isSapphire = false, number = 3, title = "فضي III", isCompleted = false, conditionText = "1000 كلمة"),
                            RankDialogItem(isBronze = false, isSapphire = true, number = 1, title = "ياقوتي I", isCompleted = false, conditionText = "2000 كلمة")
                        )
                        
                        ranksList.forEachIndexed { index, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(57.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Right Component: Medals + Middle Title
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MedalIcon(isBronze = item.isBronze, isSapphire = item.isSapphire, number = item.number)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Rank Title
                                        Text(
                                            text = item.title,
                                            color = Color(0xFF283593),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            fontFamily = com.example.ui.theme.cairoFontFamily
                                        )
                                    }
                                    
                                    // Left Component: Status Badges
                                    if (item.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE8F5E9), CircleShape)
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "✓",
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF5F5F5), CircleShape)
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.conditionText,
                                                color = Color(0xFF757575),
                                                fontSize = 11.sp,
                                                fontFamily = com.example.ui.theme.cairoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            if (index < ranksList.size - 1) {
                                Divider(
                                    color = Color(0xFFE0E0E0).copy(alpha = 0.6f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
                
                // Head Capsule (العنوان: الرتب)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp)
                        .background(Color(0xFFE8EFFF), RoundedCornerShape(24.dp))
                        .border(1.5.dp, Color(0xFF4D69DA).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 36.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "الرتب",
                        color = Color(0xFF283593),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Outstanding Close Button prominently overlapping top-left corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-12).dp)
                        .size(38.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(6.dp, CircleShape)
                        .clickable { viewModel.toggleRanksDialog(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ShopDialog(progress: UserProgress, viewModel: GameViewModel) {
    Dialog(onDismissRequest = { viewModel.toggleShopDialog(false) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "متجر الخلفيات والثيمات المدهشة",
                    color = Color(0xFF1E3C72),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // List background theme purchase cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundTheme.values().forEach { theme ->
                        val isUnlocked = progress.unlockedBackgrounds.split(",").contains(theme.bgId)
                        val isSelected = progress.selectedBackground == theme.bgId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    3.dp,
                                    if (isSelected) Color(0xFFFFD700) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = theme.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "التكلفة: ${theme.cost} 💎", fontSize = 11.sp, color = Color.Gray)
                                }

                                if (isSelected) {
                                    Text(
                                        text = "مفعلة حالياً",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                } else if (isUnlocked) {
                                    Button(
                                        onClick = { viewModel.selectBackground(theme.bgId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("تفعيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.buyBackground(theme.bgId, theme.cost) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("شراء الخلفية", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.toggleShopDialog(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("موافق", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NameChangeDialog(progress: UserProgress, viewModel: GameViewModel) {
    var textInput by remember { mutableStateOf(TextFieldValue(progress.username)) }

    Dialog(onDismissRequest = { viewModel.toggleNameChangeDialog(false) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تعديل اسم اللاعب الخاص بك",
                    color = Color(0xFF1E3C72),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("الاسم المستعار الجديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { viewModel.toggleNameChangeDialog(false) }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                    Button(onClick = { viewModel.updateUsername(textInput.text) }) {
                        Text("تعديل وحفظ الكامني", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Drawing full composed skies and mountain layers purely on canvas to avoid bloated APKs
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCustomImmersiveBackground(bgId: String) {
    val gradientBrush = when (bgId) {
        "SUNSET" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
        )
        "FOREST" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
        "OCEAN" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF114357), Color(0xFFF29492))
        )
        "GALAXY" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF02001F), Color(0xFF1F1147), Color(0xFF11022F))
        )
        else -> Brush.radialGradient(
            colors = listOf(Color(0xFF3B2F5E), Color(0xFF1C1B1F)),
            center = Offset(size.width, 0f),
            radius = size.width * 1.5f
        )
    }

    // Sky Back
    drawRect(brush = gradientBrush)

    // Cosmic Stars if Galaxy is selected
    if (bgId == "GALAXY") {
        val starSeedSource = listOf(
            Offset(50f, 100f), Offset(180f, 250f), Offset(400f, 80f),
            Offset(720f, 320f), Offset(250f, 500f), Offset(600f, 750f),
            Offset(100f, 900f), Offset(550f, 1100f), Offset(320f, 1300f)
        )
        starSeedSource.forEach { offset ->
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 3f, center = offset)
        }
    }

    // Sun / Moon vectors
    if (bgId == "SUNSET") {
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = 110f,
            center = Offset(size.width / 2, size.height * 0.35f)
        )
    }

    // Deep mountains profiles at bottom
    val mountainPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.72f)
        quadraticTo(size.width * 0.25f, size.height * 0.65f, size.width * 0.5f, size.height * 0.75f)
        quadraticTo(size.width * 0.75f, size.height * 0.82f, size.width, size.height * 0.68f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(
        path = mountainPath,
        color = when (bgId) {
            "FOREST" -> Color(0xFF003300).copy(0.48f)
            "SUNSET" -> Color(0xFF6A1B29).copy(0.48f)
            "OCEAN" -> Color(0xFF0F3A4A).copy(0.48f)
            else -> Color(0xFF2A2145).copy(0.35f)
        }
    )

    // Overlapping front mountain for depth
    val frontMountainPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.85f)
        quadraticTo(size.width * 0.35f, size.height * 0.75f, size.width * 0.68f, size.height * 0.88f)
        quadraticTo(size.width * 0.85f, size.height * 0.82f, size.width, size.height * 0.84f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(
        path = frontMountainPath,
        color = when (bgId) {
            "FOREST" -> Color(0xFF001A00).copy(0.72f)
            "SUNSET" -> Color(0xFF45101A).copy(0.72f)
            "OCEAN" -> Color(0xFF07212B).copy(0.72f)
            else -> Color(0xFF1C1B1F).copy(0.85f)
        }
    )
}

@Composable
fun BubblyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontFamily: androidx.compose.ui.text.font.FontFamily = com.example.ui.theme.fredokaFontFamily,
    fillColor: Color = Color(0xFFFF8DA1), // Lovely bubble pink matching Image 2
    strokeColor: Color = Color(0xFF1C1B1F), // Thick dark outer-inline stroke
    strokeWidth: Float = 10f,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Center
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Deep shadow outer border
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Black,
            textAlign = textAlign,
            style = androidx.compose.ui.text.TextStyle(
                color = strokeColor.copy(alpha = 0.35f),
                drawStyle = Stroke(
                    width = strokeWidth + 6f,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            ),
            modifier = Modifier.offset(x = 2.dp, y = 2.dp)
        )

        // Main outline border
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Black,
            textAlign = textAlign,
            style = androidx.compose.ui.text.TextStyle(
                color = strokeColor,
                drawStyle = Stroke(
                    width = strokeWidth,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        )

        // Colored core (Default represents the custom pink/rose look)
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Black,
            textAlign = textAlign,
            style = androidx.compose.ui.text.TextStyle(
                color = fillColor
            )
        )
    }
}

@Composable
fun AnimatedBackground(bgId: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val timeFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * 3.1415926).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val gradientBrush = when (bgId) {
            "SUNSET" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
            )
            "FOREST" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            )
            "OCEAN" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF114357))
            )
            "GALAXY" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF02001F), Color(0xFF1F1147), Color(0xFF11022F))
            )
            "ANIME_SAKURA" -> Brush.verticalGradient(
                colors = listOf(Color(0xFFFFA2B8), Color(0xFFFFD1DC), Color(0xFFFFECEF))
            )
            "CYBERPUNK_NEON" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0D0221), Color(0xFF26144D), Color(0xFF000714))
            )
            else -> Brush.radialGradient(
                colors = listOf(Color(0xFF3B2F5E), Color(0xFF1C1B1F)),
                center = Offset(width, 0f),
                radius = width * 1.5f
            )
        }

        drawRect(brush = gradientBrush)

        when (bgId) {
            "GALAXY" -> {
                val stars = listOf(
                    Offset(0.1f * width, 0.08f * height), Offset(0.3f * width, 0.15f * height),
                    Offset(0.75f * width, 0.05f * height), Offset(0.9f * width, 0.22f * height),
                    Offset(0.45f * width, 0.35f * height), Offset(0.2f * width, 0.5f * height),
                    Offset(0.85f * width, 0.48f * height), Offset(0.55f * width, 0.65f * height),
                    Offset(0.12f * width, 0.8f * height), Offset(0.7f * width, 0.76f * height),
                    Offset(0.4f * width, 0.9f * height)
                )
                stars.forEachIndexed { i, pos ->
                    val scaleOffset = (i * 1.5).toFloat()
                    val twinkle = kotlin.math.sin(floatAnim + scaleOffset) * 0.4f + 0.6f
                    drawCircle(
                        color = Color.White.copy(alpha = twinkle),
                        radius = (3f + (i % 3) * 1.5f) * twinkle,
                        center = pos
                    )

                    if (i % 3 == 0) {
                        drawRect(
                            color = Color(0xFF81D4FA).copy(alpha = twinkle * 0.3f),
                            topLeft = Offset(pos.x - 6f, pos.y - 2f),
                            size = androidx.compose.ui.geometry.Size(12f, 4f)
                        )
                        drawRect(
                            color = Color(0xFF81D4FA).copy(alpha = twinkle * 0.3f),
                            topLeft = Offset(pos.x - 2f, pos.y - 6f),
                            size = androidx.compose.ui.geometry.Size(4f, 12f)
                        )
                    }
                }
            }
            "ANIME_SAKURA" -> {
                val petalColors = listOf(Color(0xFFFFB7C5), Color(0xFFFFC0CB), Color(0xFFFFD1DC))
                for (i in 0 until 18) {
                    val initialX = (i * 0.08f) * width
                    val initialY = (i * 0.12f) * height
                    
                    val speedX = 0.03f * width
                    val speedY = 0.04f * height
                    
                    val curX = (initialX + timeFactor * speedX) % width
                    val curY = (initialY + timeFactor * speedY) % height
                    
                    val sizeFactor = 6f + (i % 4) * 3f
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = sizeFactor * 1.6f,
                        center = Offset(curX, curY)
                    )
                    
                    drawCircle(
                        color = petalColors[i % petalColors.size].copy(alpha = 0.85f),
                        radius = sizeFactor,
                        center = Offset(curX, curY)
                    )
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = sizeFactor * 0.4f,
                        center = Offset(curX - sizeFactor * 0.3f, curY - sizeFactor * 0.3f)
                    )
                }

                val moonX = width * 0.8f
                val moonY = height * 0.15f
                val moonRadius = 60.dp.toPx()
                
                drawCircle(
                    color = Color(0xFFFFECEF).copy(alpha = 0.15f),
                    radius = moonRadius * 1.5f,
                    center = Offset(moonX, moonY)
                )
                drawCircle(
                    color = Color(0xFFFFF9C4),
                    radius = moonRadius,
                    center = Offset(moonX, moonY)
                )
                drawCircle(
                    color = Color(0xFFFFA2B8),
                    radius = moonRadius * 0.95f,
                    center = Offset(moonX - moonRadius * 0.35f, moonY - moonRadius * 0.2f)
                )
            }
            "CYBERPUNK_NEON" -> {
                val gridY = height * 0.65f
                val skylineColors = listOf(Color(0xFF1E0B36), Color(0xFF150726), Color(0xFF0F041C))
                
                for (layer in 0 until 2) {
                    val layerColor = skylineColors[layer]
                    val pathSkys = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, gridY)
                        var buildingX = 0f
                        while (buildingX < width) {
                            val bWidth = (40f + (layer * 30f) + ((buildingX.toInt() % 7) * 15f)).coerceAtLeast(30f)
                            val bHeight = (100f + (layer * 50f) + ((buildingX.toInt() % 11) * 20f)).coerceAtLeast(60f)
                            lineTo(buildingX, gridY - bHeight)
                            lineTo(buildingX + bWidth, gridY - bHeight)
                            buildingX += bWidth
                        }
                        lineTo(width, gridY)
                        close()
                    }
                    drawPath(path = pathSkys, color = layerColor)
                }

                drawLine(
                    color = Color(0xFFFF007F),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFFF007F).copy(alpha = 0.35f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 14.dp.toPx()
                )

                val totalLines = 14
                for (i in 0..totalLines) {
                    val startX = (i.toFloat() / totalLines.toFloat()) * width
                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = 0.45f),
                        start = Offset(startX, gridY),
                        end = Offset((startX - width / 2) * 2f + width / 2, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                for (i in 0 until 12) {
                    val initialX = (i * 0.09f) * width
                    val initialY = gridY + (i % 4) * 0.08f * height
                    
                    val speedY = -0.05f * height
                    val curX = initialX
                    val curY = gridY + ((initialY - gridY + timeFactor * speedY) % (height - gridY))
                    
                    drawRect(
                        color = if (i % 2 == 0) Color(0xFF00F0FF).copy(alpha = 0.8f) else Color(0xFFFF007F).copy(alpha = 0.8f),
                        topLeft = Offset(curX, curY),
                        size = androidx.compose.ui.geometry.Size(12f, 12f)
                    )
                }
            }
            "SUNSET" -> {
                val sunX = width / 2f
                val sunY = height * 0.35f
                val sunRadius = 110f
                val sunBreath = kotlin.math.sin(floatAnim) * 10f
                
                drawCircle(
                    color = Color(0xFFFFEB3B).copy(alpha = 0.15f),
                    radius = (sunRadius + 30f) + sunBreath,
                    center = Offset(sunX, sunY)
                )
                drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = 0.35f),
                    radius = sunRadius + sunBreath,
                    center = Offset(sunX, sunY)
                )
                drawCircle(
                    color = Color(0xFFFFEB3B),
                    radius = sunRadius,
                    center = Offset(sunX, sunY)
                )

                val cloudColors = listOf(Color(0xFFE94057).copy(0.4f), Color(0xFF8A2387).copy(0.3f))
                for (i in 0 until 4) {
                    val speed = 0.015f * width
                    val initialX = (i * 0.3f) * width
                    val cloudY = height * (0.2f + i * 0.12f)
                    val cloudX = (initialX + timeFactor * speed) % width
                    
                    val cloudWidth = 140.dp.toPx()
                    val cloudHeight = 35.dp.toPx()
                    
                    drawRoundRect(
                        color = cloudColors[i % cloudColors.size],
                        topLeft = Offset(cloudX - cloudWidth / 2, cloudY),
                        size = androidx.compose.ui.geometry.Size(cloudWidth, cloudHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                }
            }
            "FOREST" -> {
                for (i in 0 until 20) {
                    val initialX = (i * 0.06f) * width
                    val initialY = (i * 123f) % height

                    val speedX = (kotlin.math.cos((floatAnim + i).toDouble()) * 60f).toFloat()
                    val speedY = -0.012f * height

                    val curX = (initialX + speedX) % width
                    val curY = (initialY + timeFactor * speedY) % height

                    val brightness = kotlin.math.sin(floatAnim + i * 2f) * 0.35f + 0.65f
                    drawCircle(
                        color = Color(0xFFCCFF00).copy(alpha = brightness * 0.15f),
                        radius = 16f,
                        center = Offset(curX, curY)
                    )
                    drawCircle(
                        color = Color(0xFFAEEA00).copy(alpha = brightness * 0.9f),
                        radius = 4f,
                        center = Offset(curX, curY)
                    )
                }
            }
            "OCEAN" -> {
                for (i in 0 until 18) {
                    val initialX = (i * 0.06f) * width
                    val initialY = height - (i * 0.07f) * height

                    val speedY = -0.035f * height
                    val curX = (initialX + kotlin.math.sin(floatAnim + i) * 20f) % width
                    val curY = ((initialY + timeFactor * speedY) % height + height) % height

                    val bubbleRadius = 4f + (i % 3) * 3f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = bubbleRadius,
                        center = Offset(curX, curY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = bubbleRadius * 0.3f,
                        center = Offset(curX - bubbleRadius * 0.3f, curY - bubbleRadius * 0.3f)
                    )
                }
            }
        }

        if (bgId != "GALAXY" && bgId != "CYBERPUNK_NEON" && bgId != "ANIME_SAKURA") {
            val mountainPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.72f)
                quadraticTo(width * 0.25f, height * 0.65f, width * 0.5f, height * 0.75f)
                quadraticTo(width * 0.75f, height * 0.82f, width, height * 0.68f)
                lineTo(width, height)
                close()
            }
            drawPath(
                path = mountainPath,
                color = when (bgId) {
                    "FOREST" -> Color(0xFF003300).copy(0.48f)
                    "SUNSET" -> Color(0xFF6A1B29).copy(0.48f)
                    "OCEAN" -> Color(0xFF0F3A4A).copy(0.48f)
                    else -> Color(0xFF2A2145).copy(0.35f)
                }
            )

            val frontMountainPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.85f)
                quadraticTo(width * 0.35f, height * 0.75f, width * 0.68f, height * 0.88f)
                quadraticTo(width * 0.85f, height * 0.82f, width, height * 0.84f)
                lineTo(width, height)
                close()
            }
            drawPath(
                path = frontMountainPath,
                color = when (bgId) {
                    "FOREST" -> Color(0xFF001A00).copy(0.72f)
                    "SUNSET" -> Color(0xFF45101A).copy(0.72f)
                    "OCEAN" -> Color(0xFF07212B).copy(0.72f)
                    else -> Color(0xFF1C1B1F).copy(0.85f)
                }
            )
        } else if (bgId == "ANIME_SAKURA") {
            val hillsPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.8f)
                quadraticTo(width * 0.2f, height * 0.75f, width * 0.45f, height * 0.83f)
                quadraticTo(width * 0.75f, height * 0.88f, width, height * 0.78f)
                lineTo(width, height)
                close()
            }
            drawPath(path = hillsPath, color = Color(0xFFFFB7C5).copy(alpha = 0.5f))

            val frontHillsPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.88f)
                quadraticTo(width * 0.3f, height * 0.84f, width * 0.6f, height * 0.91f)
                quadraticTo(width * 0.82f, height * 0.87f, width, height * 0.89f)
                lineTo(width, height)
                close()
            }
            drawPath(path = frontHillsPath, color = Color(0xFFFFA2B8).copy(alpha = 0.75f))
        }
    }
}

@Composable
fun FallingConfettiEffect() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val colors = listOf(
            Color(0xFFFFC107), Color(0xFFFF5722), Color(0xFF4CAF50),
            Color(0xFF00BCD4), Color(0xFF9C27B0), Color(0xFFFF4081)
        )

        for (i in 0 until 50) {
            val seedX = (i * 0.04f + 0.02f) * width
            val fallSpeed = 0.5f + (i % 5) * 0.15f
            val curY = (progress * height * fallSpeed + (i * 15f)) % height
            
            val wobbleX = seedX + kotlin.math.sin(progress * 3.14159f * 6f + i) * 20f
            val rotation = progress * 360f * (1f + (i % 3))
            
            val shapeSizeWidth = 8.dp.toPx()
            val shapeSizeHeight = 15.dp.toPx()
            
            drawContext.canvas.save()
            drawContext.canvas.translate(wobbleX, curY)
            drawContext.canvas.rotate(rotation)
            
            drawRect(
                color = colors[i % colors.size],
                topLeft = Offset(-shapeSizeWidth / 2, -shapeSizeHeight / 2),
                size = androidx.compose.ui.geometry.Size(shapeSizeWidth, shapeSizeHeight)
            )
            
            drawContext.canvas.restore()
        }
    }
}

@Composable
fun TutorialDialog(viewModel: GameViewModel) {
    Dialog(
        onDismissRequest = { viewModel.toggleTutorialDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            24.dp,
                            RoundedCornerShape(28.dp),
                            ambientColor = Color(0xFF4D69DA).copy(alpha = 0.25f),
                            spotColor = Color(0xFF4D69DA).copy(alpha = 0.5f)
                        )
                        .border(1.5.dp, Color(0xFFE8EFFF), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 46.dp, bottom = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "أهلاً بك يا بطل! تخيل أنك تلعب بلعبتك المفضلة على الأرض، وتريد نقلها من مكان إلى آخر باستخدام أصابعك السحرية على شاشة الهاتف أو الجهاز اللوحي.\n\nحركة \"الضغط والسحب\" سهلة وممتعة جداً، وكأنك تقوم بمغامرة صغيرة بإصبعك. دعنا نتعلمها معاً في ثلاث خطوات بسيطة:",
                            color = Color(0xFF333333),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = com.example.ui.theme.cairoFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        // Step 1
                        TutorialStepCard(
                            stepNumber = "1",
                            title = "الضغط (كأنك تمسك اللعبة السحرية) 👆",
                            description = "أولاً، اختر الصورة أو الزر الذي تريد تحريكه على الشاشة. ضع إصبعك عليه واضغط بلطف، تماماً كأنك تضع إصبعك فوق قطرة عسل صغيرة أو كأنك تضغط على زر جرس ألعابك.\n\nالسر الكبير هنا: لا ترفع إصبعك أبداً! ابقه ملتصقاً بالشاشة كأنه مغناطيس قوي.",
                            backgroundColor = Color(0xFFFFF3E0),
                            accentColor = Color(0xFFE65100)
                        )

                        // Step 2
                        TutorialStepCard(
                            stepNumber = "2",
                            title = "السحب (رحلة الإصبع السحرية) 🗺️",
                            description = "الآن، وبينما إصبعك لا يزال يلمس الشاشة ومثبتاً عليها، ابدأ بتحريك إصبعك ببطء على الزجاج باتجاه المكان الجديد الذي تريد الذهاب إليه.\n\nتخيل أن إصبعك عبارة عن سيارة سباق صغيرة تسير على طريق ناعم، وتجر خلفها تلك الصورة. سترى أن الصورة تتبع إصبعك وتتحرك معه أينما ذهب؛ يميناً، يساراً، إلى الأعلى، أو إلى الأسفل!",
                            backgroundColor = Color(0xFFF3E5F5),
                            accentColor = Color(0xFF4A148C)
                        )

                        // Step 3
                        TutorialStepCard(
                            stepNumber = "3",
                            title = "الإفلات أو الترك (الوصول بسلام) ✋",
                            description = "عندما تصل سيارتك (إصبعك) ومَعها الصورة إلى المكان الصحيح الذي تريده، حان وقت النهاية السعيدة! ببساطة... ارفع إصبعك إلى الأعلى في الهواء واترك الشاشة.\n\nستلاحظ أن الصورة استقرت وجلست في مكانها الجديد بنجاح وبقيت هناك.",
                            backgroundColor = Color(0xFFE8F5E9),
                            accentColor = Color(0xFF1B5E20)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Playground Divider Title
                        Text(
                            text = "🧪 جرب الحركة السحرية بنفسك!",
                            color = Color(0xFF283593),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.example.ui.theme.cairoFontFamily,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "اسحب الكرة البلورية 🔮 من اليمين وضعها فوق صندوق الكنز 🎯 في اليسار لتجريب السحب!",
                            color = Color(0xFF666666),
                            fontSize = 12.sp,
                            fontFamily = com.example.ui.theme.cairoFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        // Active interactive Playground
                        InteractivePlayground(
                            onPlaygroundSuccess = {
                                viewModel.triggerSound()
                            }
                        )

                        Text(
                            text = "عمل رائع يا بطل! الأمر يشبه تماماً عندما تأخذ \"ملصقاً\" (ستيكر) من دفترك وتلصقه في مكان آخر. أنت الآن مستعد لتجربتها بنفسك كالمحترفين!",
                            color = Color(0xFF333333),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = com.example.ui.theme.cairoFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Outstanding Close Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-12).dp)
                        .size(38.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(6.dp, CircleShape)
                        .clickable { viewModel.toggleTutorialDialog(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                // Header capsule badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp)
                        .background(Color(0xFFE8EFFF), RoundedCornerShape(24.dp))
                        .border(1.5.dp, Color(0xFF4D69DA).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 30.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "دليل طريقة اللعب 🎮",
                        color = Color(0xFF283593),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialStepCard(
    stepNumber: String,
    title: String,
    description: String,
    backgroundColor: Color,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.example.ui.theme.fredokaFontFamily
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.example.ui.theme.cairoFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color(0xFF37474F),
                    fontSize = 13.sp,
                    fontFamily = com.example.ui.theme.cairoFontFamily,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun InteractivePlayground(onPlaygroundSuccess: () -> Unit) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var success by remember { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Target px calculations
    val targetXThresholdPx = with(density) { -130.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color(0xFFF5F7FF), RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFF4D69DA).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        if (success) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 رائع جداً! لقد نجحت في التعلم! 🎉",
                    color = Color(0xFF2E7D32),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.example.ui.theme.cairoFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        dragOffset = Offset.Zero
                        success = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "أعد التجريب 🔮",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.example.ui.theme.cairoFontFamily
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left target
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFFFFEBEE), CircleShape)
                        .border(2.dp, Color(0xFFE53935), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 22.sp)
                        Text(
                            text = "الوجهة",
                            color = Color(0xFFE53935),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.example.ui.theme.cairoFontFamily
                        )
                    }
                }

                // Connecting line
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(2.dp)
                        .background(Color.LightGray, RoundedCornerShape(100)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◀◀◀ اسحب لليمين ◀◀◀",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.example.ui.theme.cairoFontFamily,
                        modifier = Modifier.offset(y = (-8).dp)
                    )
                }

                // Right starting bounds
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                        .border(1.5.dp, Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "البداية",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.example.ui.theme.cairoFontFamily
                    )
                }
            }

            // Draggable magic ball
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(
                        x = with(density) { dragOffset.x.toDp() },
                        y = with(density) { dragOffset.y.toDp() }
                    )
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE040FB), Color(0xFF6200EA))
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(4.dp, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (dragOffset.x < targetXThresholdPx) {
                                    success = true
                                    onPlaygroundSuccess()
                                } else {
                                    dragOffset = Offset.Zero
                                }
                            },
                            onDragCancel = {
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newX = (dragOffset.x + dragAmount.x).coerceIn(targetXThresholdPx * 1.1f, 0f)
                                val newY = (dragOffset.y + dragAmount.y).coerceIn(-50f, 50f)
                                dragOffset = Offset(newX, newY)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🔮", fontSize = 28.sp)
            }
        }
    }
}

