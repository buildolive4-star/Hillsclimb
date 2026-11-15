package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.model.AppLanguage
import com.example.model.PlayerSaveData
import com.example.ui.art.drawAnimatedMenuLandscape
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
  saveData: PlayerSaveData,
  canClaimDaily: Boolean,
  claimableMissionsCount: Int,
  onPlay: () -> Unit,
  onOpenGarage: () -> Unit,
  onOpenUpgrades: () -> Unit,
  onOpenMaps: () -> Unit,
  onOpenDailyRewards: () -> Unit,
  onOpenMissions: () -> Unit,
  onOpenShop: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenSketchfab: (String) -> Unit,
  onToggleSound: () -> Unit,
) {
  val selectedVehicle =
    remember(saveData.selectedVehicleId) { GameCatalog.vehicleById(saveData.selectedVehicleId) }
  val selectedMap = remember(saveData.selectedMapId) { GameCatalog.mapById(saveData.selectedMapId) }
  val equippedSkinId = saveData.equippedSkinByVehicle[saveData.selectedVehicleId] ?: "default"
  val equippedSkin =
    remember(equippedSkinId) { GameCatalog.skins.firstOrNull { it.id == equippedSkinId } }
  val isHindi = saveData.settings.language == AppLanguage.HINDI

  var animTimeSec by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { nanos ->
        if (lastNanos != 0L) {
          animTimeSec += ((nanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        }
        lastNanos = nanos
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Live Scenic Animated Landscape with Large Hero Vehicle & Background Vehicle
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawAnimatedMenuLandscape(
        timeSec = animTimeSec,
        selectedVehicle = selectedVehicle,
        skin = equippedSkin,
      )
    }

    // Top/Bottom subtle cinematic gradient scrims for crisp UI contrast
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  DarkNavyBg.copy(alpha = 0.60f),
                  Color.Transparent,
                  DarkNavyBg.copy(alpha = 0.72f),
                )
            )
          )
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // TOP SECTION: Avatar + Username + Level + XP Bar | Logo | Coins + Diamonds + Daily + Sound + Settings
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Player Avatar + Username + Level + XP Bar
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MidnightBluePrimary.copy(alpha = 0.90f),
          border = BorderStroke(1.5.dp, ElectricBlueSecondary.copy(alpha = 0.7f)),
          modifier =
            Modifier.defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(18.dp))
              .clickable(onClick = onOpenProfile)
              .testTag("profile_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PlayerAvatarBadge(avatarId = saveData.avatarId, sizeDp = 38)
            Column(modifier = Modifier.widthIn(min = 105.dp, max = 148.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = saveData.playerName,
                  style = MaterialTheme.typography.labelLarge,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = GoldenYellowHighlight,
                ) {
                  Text(
                    text = "LV ${saveData.playerLevel}",
                    color = DarkNavyBg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CardNavy)
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxHeight()
                      .fillMaxWidth(saveData.levelProgressFraction().coerceIn(0.06f, 1f))
                      .clip(RoundedCornerShape(50))
                      .background(
                        Brush.horizontalGradient(listOf(RacingOrangeAccent, GoldenYellowHighlight))
                      )
                )
              }
            }
          }
        }

        // Game Brand Logo Crest
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MidnightBluePrimary.copy(alpha = 0.88f),
          border = BorderStroke(2.dp, GoldenYellowHighlight.copy(alpha = 0.85f)),
          shadowElevation = 8.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Terrain,
              contentDescription = null,
              tint = RacingOrangeAccent,
              modifier = Modifier.size(26.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "HILL RUSH INDIA",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
              )
              Text(
                text = if (isHindi) "प्रीमियम आर्केड रेसिंग" else "PREMIUM ARCADE RACING",
                style = MaterialTheme.typography.labelSmall,
                color = GoldenYellowHighlight,
              )
            }
          }
        }

        // Right Cluster: Coin & Diamond Wallets + Daily Gift + Sound + Settings
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CurrencyChipsRow(coins = saveData.coins, diamonds = saveData.diamonds)

          // Daily Gift Quick Icon Button
          Box {
            FilledIconButton(
              onClick = onOpenDailyRewards,
              colors =
                IconButtonDefaults.filledIconButtonColors(
                  containerColor = MidnightBluePrimary.copy(alpha = 0.9f)
                ),
              modifier = Modifier.size(48.dp).testTag("daily_rewards_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.CardGiftcard,
                contentDescription = "Daily Rewards",
                tint = if (canClaimDaily) GoldenYellowHighlight else TextSecondary,
              )
            }
            if (canClaimDaily) {
              Box(
                modifier =
                  Modifier.align(Alignment.TopEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(DangerRed)
              )
            }
          }

          FilledIconButton(
            onClick = onToggleSound,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MidnightBluePrimary.copy(alpha = 0.9f)
              ),
            modifier = Modifier.size(48.dp).testTag("sound_toggle_button"),
          ) {
            Icon(
              imageVector =
                if (saveData.settings.soundEnabled || saveData.settings.musicEnabled)
                  Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = "Sound",
              tint = if (saveData.settings.soundEnabled) SuccessGreen else TextSecondary,
            )
          }

          FilledIconButton(
            onClick = onOpenSettings,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MidnightBluePrimary.copy(alpha = 0.9f)
              ),
            modifier = Modifier.size(48.dp).testTag("settings_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = "Settings",
              tint = TextPrimary,
            )
          }
        }
      }

      // CENTER SECTION: Open Hero View on Left + Floating Action Card & START RACING CTA on Right
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left side displays active rig badge above hero vehicle without covering the scenic canvas
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MidnightBluePrimary.copy(alpha = 0.78f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
          modifier = Modifier.align(Alignment.Top).padding(top = 6.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Filled.DirectionsCar, null, tint = RacingOrangeAccent, modifier = Modifier.size(20.dp))
            Column {
              Text(
                text = selectedVehicle.name.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Text(
                text = "LV ${saveData.vehicleAvgLevel(selectedVehicle.id)} • ${selectedMap.name} (Best ${saveData.bestDistanceByMap[selectedMap.id] ?: 0}m)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
          }
        }

        // Right Racing Action Panel: Quick Tune + Dominant Glowing START RACING CTA
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.widthIn(min = 240.dp, max = 300.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ArcadeGradientButton(
              text = if (isHindi) "अपग्रेड" else "UPGRADE",
              icon = Icons.Filled.Build,
              colors = listOf(ElectricBlueSecondary, Color(0xFF1A4FD6)),
              onClick = onOpenUpgrades,
              modifier = Modifier.weight(1f),
              testTag = "upgrade_button",
            )
            ArcadeGradientButton(
              text = if (isHindi) "3D मॉडल" else "3D HUB",
              icon = Icons.Filled.ViewInAr,
              colors = listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)),
              onClick = { onOpenSketchfab(selectedVehicle.name) },
              modifier = Modifier.weight(1f),
              testTag = "sketchfab_hub_button",
            )
            ArcadeGradientButton(
              text = if (isHindi) "इनाम" else "GIFTS",
              icon = Icons.Filled.CardGiftcard,
              colors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
              badgeCount = if (canClaimDaily) 1 else 0,
              onClick = onOpenDailyRewards,
              modifier = Modifier.weight(1f),
              testTag = "quick_daily_button",
            )
          }

          GlowingStartRacingButton(
            text = if (isHindi) "रेस शुरू करें" else "START RACING",
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            testTag = "play_button",
          )
        }
      }

      // BOTTOM SECTION: Premium 5-Item Navigation Bar (HOME | GARAGE | MAPS | MISSIONS | SHOP)
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        PremiumBottomNavDock(
          currentTab = BottomNavTab.HOME,
          missionBadge = claimableMissionsCount,
          onSelectTab = { tab ->
            when (tab) {
              BottomNavTab.HOME -> Unit
              BottomNavTab.GARAGE -> onOpenGarage()
              BottomNavTab.MAPS -> onOpenMaps()
              BottomNavTab.MISSIONS -> onOpenMissions()
              BottomNavTab.SHOP -> onOpenShop()
            }
          },
        )
      }
    }
  }
}
