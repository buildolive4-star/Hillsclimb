package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.model.GraphicsQuality
import com.example.model.MapId
import com.example.model.PlayerSaveData
import com.example.ui.art.drawGameWorldBackground
import com.example.ui.components.ArcadeGradientButton
import com.example.ui.components.GlowingStartRacingButton
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun MapSelectionScreen(
  saveData: PlayerSaveData,
  onSelectMap: (MapId) -> Unit,
  onUnlockMap: (MapId) -> Boolean,
  onRaceNow: () -> Unit,
  onBack: () -> Unit,
) {
  val initialIndex = GameCatalog.maps.indexOfFirst { it.id == saveData.selectedMapId }.coerceAtLeast(0)
  val pagerState = rememberPagerState(initialPage = initialIndex) { GameCatalog.maps.size }
  val scope = rememberCoroutineScope()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "SELECT CIRCUIT (${pagerState.currentPage + 1} / ${GameCatalog.maps.size})",
      coins = saveData.coins,
      diamonds = saveData.diamonds,
      onBack = onBack,
    )

    // Horizontal Swipe Navigation Pager with Expanded Selected Map Card
    HorizontalPager(
      state = pagerState,
      contentPadding = PaddingValues(horizontal = 84.dp),
      pageSpacing = 16.dp,
      modifier = Modifier.fillMaxWidth().weight(1f),
    ) { page ->
      val map = GameCatalog.maps[page]
      val isFocused = page == pagerState.currentPage
      val scale by animateFloatAsState(if (isFocused) 1f else 0.90f, label = "map_card_scale")
      val isUnlocked = map.id in saveData.unlockedMaps
      val bestDist = saveData.bestDistanceByMap[map.id] ?: 0
      val bestScore = saveData.bestScoreByMap[map.id] ?: 0

      Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardNavy,
        border =
          BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) RacingOrangeAccent else Color.White.copy(alpha = 0.14f),
          ),
        shadowElevation = if (isFocused) 14.dp else 2.dp,
        modifier =
          Modifier.fillMaxSize()
            .padding(vertical = 8.dp)
            .scale(scale)
            .clickable { scope.launch { pagerState.animateScrollToPage(page) } }
            .testTag("map_card_${map.id.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Left: Large Illustrated Environment Artwork
          Box(
            modifier =
              Modifier.weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawGameWorldBackground(
                map = map,
                cameraX = 24f,
                timeSec = 1.5f,
                quality = GraphicsQuality.MEDIUM,
              )
              val bodyPath =
                Path().apply {
                  moveTo(0f, size.height)
                  var x = 0f
                  while (x <= size.width + 12f) {
                    val y = size.height * 0.72f - sin(x * 0.024f) * map.hillAmplitude * 6f
                    lineTo(x, y)
                    x += 12f
                  }
                  lineTo(size.width, size.height)
                  close()
                }
              drawPath(bodyPath, Color(map.groundBodyColor))
              val crust =
                Path().apply {
                  var x = 0f
                  moveTo(0f, size.height * 0.72f)
                  while (x <= size.width + 12f) {
                    val y = size.height * 0.72f - sin(x * 0.024f) * map.hillAmplitude * 6f
                    lineTo(x, y)
                    x += 12f
                  }
                }
              drawPath(crust, Color(map.groundTopColor), style = Stroke(width = 9f))
            }

            // Difficulty Pill Overlay
            Surface(
              shape = RoundedCornerShape(50),
              color = Color(map.difficulty.colorHex),
              modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            ) {
              Text(
                text = map.difficulty.label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkNavyBg,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }

            if (!isUnlocked) {
              Box(
                modifier = Modifier.fillMaxSize().background(DarkNavyBg.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  Icons.Filled.Lock,
                  contentDescription = "Locked",
                  tint = GoldenYellowHighlight,
                  modifier = Modifier.size(44.dp),
                )
              }
            }
          }

          // Right: Map Details, Records & START CTA
          Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = map.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
              )
              Text(
                text = map.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }

            // Records Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MidnightBluePrimary,
                modifier = Modifier.weight(1f),
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Text("DISTANCE RECORD", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  Text("${bestDist}m", style = MaterialTheme.typography.titleLarge, color = SkyCyan)
                }
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MidnightBluePrimary,
                modifier = Modifier.weight(1f),
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Text("BEST SCORE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  Text("%,d".format(bestScore), style = MaterialTheme.typography.titleLarge, color = GoldenYellowHighlight)
                }
              }
            }

            // Physics Conditions Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              map.physicsTags.forEach { tag ->
                Surface(shape = RoundedCornerShape(8.dp), color = CardNavyElevated) {
                  Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }

            if (!isUnlocked) {
              val canAfford = saveData.coins >= map.unlockCostCoins
              ArcadeGradientButton(
                text = "UNLOCK • %,d COINS".format(map.unlockCostCoins),
                icon = Icons.Filled.LockOpen,
                colors = listOf(GoldenYellowHighlight, RacingOrangeAccent),
                textColor = DarkNavyBg,
                enabled = canAfford,
                onClick = { onUnlockMap(map.id) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "unlock_map_button",
              )
            } else {
              GlowingStartRacingButton(
                text = "START",
                onClick = {
                  onSelectMap(map.id)
                  onRaceNow()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                testTag = "play_selected_map_button",
              )
            }
          }
        }
      }
    }

    // Bottom Page Dots Indicator
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(GameCatalog.maps.size) { idx ->
        val selected = idx == pagerState.currentPage
        Box(
          modifier =
            Modifier.padding(horizontal = 4.dp)
              .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
              .clip(CircleShape)
              .background(if (selected) RacingOrangeAccent else CardNavyElevated)
              .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
        )
      }
    }
  }
}
