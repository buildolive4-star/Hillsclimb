package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.model.*
import com.example.ui.art.drawCartoonVehicle
import com.example.ui.art.drawShowroomBackdrop
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun GarageScreen(
  saveData: PlayerSaveData,
  onSelectVehicle: (VehicleId) -> Unit,
  onUnlockVehicle: (VehicleId) -> Boolean,
  onOpenUpgradeScreen: (VehicleId) -> Unit,
  onBuyOrEquipSkin: (VehicleId, String) -> Boolean,
  onOpenSketchfab: (VehicleSpec) -> Unit,
  onTogglePrefer3d: (Boolean) -> Unit,
  onRaceNow: () -> Unit,
  onBack: () -> Unit,
) {
  var selectedCategory by remember { mutableStateOf(VehicleCategory.ALL) }
  var previewVehicleId by remember(saveData.selectedVehicleId) {
    mutableStateOf(saveData.selectedVehicleId)
  }

  val filteredVehicles =
    remember(selectedCategory) {
      if (selectedCategory == VehicleCategory.ALL) GameCatalog.vehicles
      else GameCatalog.vehicles.filter { it.category == selectedCategory }
    }

  val previewSpec = remember(previewVehicleId) { GameCatalog.vehicleById(previewVehicleId) }
  val isUnlocked = previewVehicleId in saveData.unlockedVehicles
  val isSelected = previewVehicleId == saveData.selectedVehicleId
  val equippedSkinId = saveData.equippedSkinByVehicle[previewVehicleId] ?: "default"
  val equippedSkin = remember(equippedSkinId) { GameCatalog.skins.firstOrNull { it.id == equippedSkinId } }

  var animTimeSec by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { n ->
        if (last != 0L) animTimeSec += ((n - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = n
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(DarkNavyBg)) {
    // Dark Blue Automotive Showroom Lighting Backdrop
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawShowroomBackdrop(timeSec = animTimeSec, accentColor = ElectricBlueSecondary)
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(bottom = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // TOP: Garage Title + Coins + Diamonds
      ScreenTopBar(
        title = "SHOWROOM GARAGE",
        coins = saveData.coins,
        diamonds = saveData.diamonds,
        onBack = onBack,
      )

      // MIDDLE: Left Category Rail | Center Animated Vehicle | Right 6-Bar Statistics
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // LEFT SIDE: Category Selection
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = CardNavy.copy(alpha = 0.86f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
          modifier = Modifier.width(126.dp).fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "CATEGORY",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
            VehicleCategory.entries.forEach { cat ->
              val active = cat == selectedCategory
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) ElectricBlueSecondary else MidnightBluePrimary)
                    .clickable { selectedCategory = cat }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                Text(
                  text = cat.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (active) TextPrimary else TextSecondary,
                )
              }
            }
          }
        }

        // CENTER: Hero Animated Vehicle Showroom Pedestal + Action Buttons
        Column(
          modifier = Modifier.weight(1.25f).fillMaxHeight(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = previewSpec.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
              )
              Text(
                text = previewSpec.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
            if (isUnlocked) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MidnightBluePrimary,
                border = BorderStroke(1.dp, GoldenYellowHighlight.copy(alpha = 0.7f)),
              ) {
                Text(
                  text = "LV ${saveData.vehicleAvgLevel(previewVehicleId)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = GoldenYellowHighlight,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }

          val equipped3d = saveData.equipped3dModelFor(previewVehicleId)
          val show3dNow = equipped3d != null && saveData.prefer3dShowroom

          // Animated Showroom Vehicle (Interactive 360° 3D Sketchfab Rig OR 2D Cartoon Preview)
          Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (show3dNow && equipped3d != null) {
              Sketchfab3DModelViewport(
                entryFilePath = equipped3d.entryFilePath,
                modelTitle = equipped3d.name,
                authorName = equipped3d.authorName,
                licenseLabel = equipped3d.licenseLabel,
                autoRotate = true,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
              )
            } else {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val idleBounce = sin(animTimeSec * 3.8f) * 3.5f
                val comp = (sin(animTimeSec * 3.8f) * 0.05f).coerceAtLeast(0f)
                drawCartoonVehicle(
                  spec = previewSpec,
                  centerPx = Offset(size.width * 0.5f, size.height * 0.55f + idleBounce),
                  pixelsPerMeter = (size.height * 0.36f).coerceIn(24f, 44f),
                  angleRad = 0.02f,
                  rearWheelAngleRad = animTimeSec * 3.8f,
                  frontWheelAngleRad = animTimeSec * 3.8f,
                  rearCompressionM = comp,
                  frontCompressionM = comp,
                  skin = equippedSkin,
                )
              }
            }

            // Top-left 3D Sketchfab Quick Controls (Toggle 2D/3D + Browse Free 3D)
            Row(
              modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(50),
                color = ElectricBlueSecondary.copy(alpha = 0.90f),
                modifier =
                  Modifier.clickable { onOpenSketchfab(previewSpec) }
                    .testTag("garage_sketchfab_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(Icons.Filled.ViewInAr, null, tint = TextPrimary, modifier = Modifier.size(15.dp))
                  Text(
                    text = if (equipped3d != null) "CHANGE 3D" else "GET FREE 3D",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                  )
                }
              }
              if (equipped3d != null) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = if (show3dNow) RacingOrangeAccent else CardNavy,
                  modifier = Modifier.clickable { onTogglePrefer3d(!saveData.prefer3dShowroom) },
                ) {
                  Text(
                    text = if (show3dNow) "3D ON" else "2D VIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (show3dNow) DarkNavyBg else TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }
            }

            if (!isUnlocked) {
              Surface(
                shape = RoundedCornerShape(50),
                color = DarkNavyBg.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, GoldenYellowHighlight),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(Icons.Filled.Lock, null, tint = GoldenYellowHighlight, modifier = Modifier.size(16.dp))
                  Text("LOCKED", style = MaterialTheme.typography.labelSmall, color = GoldenYellowHighlight)
                }
              }
            }
          }

          // Paint Selector + SELECT / UPGRADE / UNLOCK CTA Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (!isUnlocked) {
              val canAfford =
                saveData.coins >= previewSpec.unlockCostCoins &&
                  saveData.diamonds >= previewSpec.unlockCostDiamonds
              val priceLabel =
                if (previewSpec.unlockCostDiamonds > 0)
                  "UNLOCK • %,d COINS + %d♦".format(previewSpec.unlockCostCoins, previewSpec.unlockCostDiamonds)
                else "UNLOCK • %,d COINS".format(previewSpec.unlockCostCoins)
              ArcadeGradientButton(
                text = priceLabel,
                icon = Icons.Filled.LockOpen,
                colors = listOf(GoldenYellowHighlight, RacingOrangeAccent),
                textColor = DarkNavyBg,
                enabled = canAfford,
                onClick = { onUnlockVehicle(previewVehicleId) },
                modifier = Modifier.weight(1f),
                testTag = "unlock_vehicle_button",
              )
            } else {
              if (!isSelected) {
                ArcadeGradientButton(
                  text = "SELECT",
                  icon = Icons.Filled.CheckCircle,
                  colors = listOf(ElectricBlueSecondary, Color(0xFF1A4FD6)),
                  onClick = { onSelectVehicle(previewVehicleId) },
                  modifier = Modifier.weight(1f),
                  testTag = "select_vehicle_button",
                )
              } else {
                ArcadeGradientButton(
                  text = "RACE",
                  icon = Icons.Filled.PlayArrow,
                  colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
                  textColor = DarkNavyBg,
                  onClick = onRaceNow,
                  modifier = Modifier.weight(1f),
                  testTag = "garage_race_button",
                )
              }
              ArcadeGradientButton(
                text = "UPGRADE",
                icon = Icons.Filled.Build,
                colors = listOf(SuccessGreen, EmeraldDark),
                onClick = {
                  onSelectVehicle(previewVehicleId)
                  onOpenUpgradeScreen(previewVehicleId)
                },
                modifier = Modifier.weight(1f),
                testTag = "garage_open_upgrade_button",
              )
            }
          }
        }

        // RIGHT SIDE: Vehicle Statistics (SPEED, ENGINE, GRIP, SUSPENSION, STABILITY, FUEL)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = CardNavy.copy(alpha = 0.88f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
          modifier = Modifier.weight(0.95f).fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("VEHICLE STATS", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            StatProgressRow(
              label = "SPEED",
              fraction = previewSpec.maxSpeed / 36f,
              color = SkyCyan,
              valueText = "${(previewSpec.maxSpeed * 3.6f).toInt()} km/h",
            )
            StatProgressRow(
              label = "ENGINE",
              fraction = previewSpec.enginePower / 35f,
              color = RacingOrangeAccent,
              valueText = "${previewSpec.enginePower.toInt()} HP",
            )
            StatProgressRow(
              label = "GRIP",
              fraction = previewSpec.handling,
              color = ElectricBlueSecondary,
              valueText = "${(previewSpec.handling * 100).toInt()}%",
            )
            StatProgressRow(
              label = "SUSPENSION",
              fraction = previewSpec.suspensionStiffness / 225f,
              color = GoldenYellowHighlight,
              valueText = "${previewSpec.suspensionStiffness.toInt()}",
            )
            StatProgressRow(
              label = "STABILITY",
              fraction = previewSpec.stability,
              color = SuccessGreen,
              valueText = "${(previewSpec.stability * 100).toInt()}%",
            )
            StatProgressRow(
              label = "FUEL",
              fraction = previewSpec.fuelCapacity / 180f,
              color = Color(0xFFFB7185),
              valueText = "${previewSpec.fuelCapacity.toInt()} L",
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // BOTTOM: Horizontal Vehicle Selection Carousel
      LazyRow(
        modifier = Modifier.fillMaxWidth().height(98.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredVehicles) { v ->
          val unlocked = v.id in saveData.unlockedVehicles
          val focused = v.id == previewVehicleId
          val active = v.id == saveData.selectedVehicleId
          Surface(
            shape = RoundedCornerShape(16.dp),
            color =
              when {
                focused -> CardNavyElevated
                !unlocked -> MidnightBluePrimary.copy(alpha = 0.72f)
                else -> CardNavy
              },
            border =
              BorderStroke(
                width = if (focused) 2.5.dp else 1.dp,
                color =
                  when {
                    focused -> RacingOrangeAccent
                    active -> SuccessGreen
                    else -> Color.White.copy(alpha = 0.12f)
                  },
              ),
            modifier =
              Modifier.width(152.dp)
                .fillMaxHeight()
                .clickable {
                  previewVehicleId = v.id
                  if (unlocked) onSelectVehicle(v.id)
                }
                .testTag("vehicle_card_${v.id.name.lowercase()}"),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().padding(8.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = v.name,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (unlocked) TextPrimary else TextSecondary,
                  maxLines = 1,
                )
                if (!unlocked) {
                  Icon(Icons.Filled.Lock, "Locked", tint = GoldenYellowHighlight, modifier = Modifier.size(14.dp))
                } else if (active) {
                  Icon(Icons.Filled.CheckCircle, "Equipped", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                }
              }

              Canvas(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                drawCartoonVehicle(
                  spec = v,
                  centerPx = Offset(size.width * 0.5f, size.height * 0.62f),
                  pixelsPerMeter = 12.5f,
                  angleRad = 0f,
                  rearWheelAngleRad = 0f,
                  frontWheelAngleRad = 0f,
                  rearCompressionM = 0f,
                  frontCompressionM = 0f,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                val has3d = saveData.equipped3dModelFor(v.id) != null
                Text(
                  text =
                    when {
                      !unlocked -> "LOCKED"
                      has3d -> "LV ${saveData.vehicleAvgLevel(v.id)} • 3D"
                      else -> "LV ${saveData.vehicleAvgLevel(v.id)}"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    when {
                      !unlocked -> TextSecondary
                      has3d -> RacingOrangeAccent
                      else -> SuccessGreen
                    },
                )
                Text(
                  text = if (unlocked) "OWNED" else "%,d".format(v.unlockCostCoins),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (unlocked) SkyCyan else GoldenYellowHighlight,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun UpgradeScreen(
  saveData: PlayerSaveData,
  initialVehicleId: VehicleId,
  onUpgradePart: (VehicleId, UpgradeType) -> Boolean,
  onRaceNow: () -> Unit,
  onBack: () -> Unit,
) {
  val vehicleId =
    if (initialVehicleId in saveData.unlockedVehicles) initialVehicleId else saveData.selectedVehicleId
  val spec = remember(vehicleId) { GameCatalog.vehicleById(vehicleId) }
  val equippedSkinId = saveData.equippedSkinByVehicle[vehicleId] ?: "default"
  val equippedSkin = remember(equippedSkinId) { GameCatalog.skins.firstOrNull { it.id == equippedSkinId } }

  var animTimeSec by remember { mutableFloatStateOf(0f) }
  var recentUpgradeText by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { n ->
        if (last != 0L) animTimeSec += ((n - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = n
      }
    }
  }

  LaunchedEffect(recentUpgradeText) {
    if (recentUpgradeText != null) {
      delay(1500)
      recentUpgradeText = null
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(DarkNavyBg)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawShowroomBackdrop(
        timeSec = animTimeSec,
        accentColor = if (recentUpgradeText != null) SuccessGreen else RacingOrangeAccent,
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
      ScreenTopBar(
        title = "TUNE & UPGRADE • ${spec.name}",
        coins = saveData.coins,
        diamonds = saveData.diamonds,
        onBack = onBack,
      )

      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Left: Large Vehicle Preview + Live Upgrade Spark + Overall Rating + Race CTA
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = CardNavy.copy(alpha = 0.85f),
          border = BorderStroke(1.5.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
          modifier = Modifier.weight(0.85f).fillMaxHeight().padding(bottom = 8.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(spec.name.uppercase(), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
              Surface(shape = RoundedCornerShape(8.dp), color = GoldenYellowHighlight) {
                Text(
                  text = "AVG LV ${saveData.vehicleAvgLevel(vehicleId)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = DarkNavyBg,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val bounce = sin(animTimeSec * 4.5f) * 3f
                drawCartoonVehicle(
                  spec = spec,
                  centerPx = Offset(size.width * 0.5f, size.height * 0.56f + bounce),
                  pixelsPerMeter = (size.height * 0.34f).coerceIn(22f, 40f),
                  angleRad = 0.03f,
                  rearWheelAngleRad = animTimeSec * 3.5f,
                  frontWheelAngleRad = animTimeSec * 3.5f,
                  rearCompressionM = 0f,
                  frontCompressionM = 0f,
                  skin = equippedSkin,
                )
              }

              androidx.compose.animation.AnimatedVisibility(
                visible = recentUpgradeText != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
              ) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = SuccessGreen,
                ) {
                  Text(
                    text = recentUpgradeText.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkNavyBg,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  )
                }
              }
            }

            // Summary Stat Boosts reflecting current upgrade levels
            val engLvl = saveData.upgradeLevel(vehicleId, UpgradeType.ENGINE)
            val fuelLvl = saveData.upgradeLevel(vehicleId, UpgradeType.FUEL_TANK)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
              StatProgressRow(
                label = "TUNED TOP SPEED",
                fraction = (spec.maxSpeed * (1f + (engLvl - 1) * 0.045f)) / 48f,
                color = RacingOrangeAccent,
                valueText = "${(spec.maxSpeed * (1f + (engLvl - 1) * 0.045f) * 3.6f).toInt()} km/h",
              )
              StatProgressRow(
                label = "TUNED FUEL TANK",
                fraction = (spec.fuelCapacity * (1f + (fuelLvl - 1) * 0.11f)) / 320f,
                color = SuccessGreen,
                valueText = "${(spec.fuelCapacity * (1f + (fuelLvl - 1) * 0.11f)).toInt()} L",
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            GlowingStartRacingButton(
              text = "TEST ON TRACK",
              onClick = onRaceNow,
              modifier = Modifier.fillMaxWidth().height(50.dp),
              testTag = "upgrade_race_button",
            )
          }
        }

        // Right: 8 Upgrade Cards Grid
        LazyColumn(
          modifier = Modifier.weight(1.25f).fillMaxHeight().padding(bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(UpgradeType.entries) { part ->
            val curLvl = saveData.upgradeLevel(vehicleId, part)
            val isMax = curLvl >= 10
            val cost = GameCatalog.upgradeCostCoins(part, curLvl)
            val canAfford = !isMax && saveData.coins >= cost

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = CardNavy.copy(alpha = 0.92f),
              border =
                BorderStroke(
                  1.dp,
                  if (isMax) SuccessGreen.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f),
                ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(44.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(
                        Brush.linearGradient(
                          listOf(ElectricBlueSecondary.copy(alpha = 0.35f), MidnightBluePrimary)
                        )
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = upgradeIconFor(part),
                    contentDescription = part.title,
                    tint = GoldenYellowHighlight,
                    modifier = Modifier.size(24.dp),
                  )
                }

                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(part.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Surface(shape = RoundedCornerShape(6.dp), color = MidnightBluePrimary) {
                      Text(
                        text = "LV $curLvl / 10",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldenYellowHighlight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Text(
                    text = if (isMax) "Maximum Performance Reached!" else "Next: ${part.boostPerLevelLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMax) SuccessGreen else TextSecondary,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (step in 1..10) {
                      Box(
                        modifier =
                          Modifier.weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (step <= curLvl) SuccessGreen else MidnightBluePrimary)
                      )
                    }
                  }
                }

                if (isMax) {
                  Surface(shape = RoundedCornerShape(12.dp), color = EmeraldDark.copy(alpha = 0.65f)) {
                    Text(
                      text = "MAXED",
                      style = MaterialTheme.typography.labelLarge,
                      color = SuccessGreen,
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                  }
                } else {
                  ArcadeGradientButton(
                    text = "%,d".format(cost),
                    icon = Icons.Filled.ArrowUpward,
                    colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
                    textColor = DarkNavyBg,
                    enabled = canAfford,
                    onClick = {
                      if (onUpgradePart(vehicleId, part)) {
                        recentUpgradeText = "${part.title} LV ${curLvl + 1}!"
                      }
                    },
                    modifier = Modifier.width(114.dp),
                    testTag = "upgrade_${part.name.lowercase()}_button",
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
