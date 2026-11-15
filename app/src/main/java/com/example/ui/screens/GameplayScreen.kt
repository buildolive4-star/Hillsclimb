package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.SoundEngine
import com.example.data.GameCatalog
import com.example.model.PlayerSaveData
import com.example.model.VehicleUpgradeLevels
import com.example.physics.GameOverReason
import com.example.physics.VehiclePhysicsEngine
import com.example.ui.art.*
import com.example.ui.components.ArcadeGradientButton
import com.example.ui.components.Sketchfab3DModelViewport
import com.example.ui.theme.*
import com.example.viewmodel.RunSummary

@Composable
fun GameplayScreen(
  saveData: PlayerSaveData,
  soundEngine: SoundEngine,
  consumeBoosters: () -> Triple<Boolean, Boolean, Boolean>,
  onRunFinished: (
    distanceMeters: Int,
    score: Int,
    timeSurvivedSec: Int,
    coinsEarned: Int,
    diamondsEarned: Int,
    stuntsCompleted: Int,
    stuntBonusCoins: Int,
    backflips: Int,
    frontflips: Int,
    fuelCans: Int,
    didCrash: Boolean,
    reasonText: String,
  ) -> RunSummary,
  onDoubleRewards: (Int) -> Unit,
  onExitToMenu: () -> Unit,
  onOpenGarage: () -> Unit,
) {
  val vehicle = remember(saveData.selectedVehicleId) { GameCatalog.vehicleById(saveData.selectedVehicleId) }
  val map = remember(saveData.selectedMapId) { GameCatalog.mapById(saveData.selectedMapId) }
  val upgrades = remember(saveData.vehicleUpgrades, saveData.selectedVehicleId) {
    saveData.vehicleUpgrades[saveData.selectedVehicleId] ?: VehicleUpgradeLevels()
  }
  val equippedSkinId = saveData.equippedSkinByVehicle[saveData.selectedVehicleId] ?: "default"
  val skin = remember(equippedSkinId) { GameCatalog.skins.firstOrNull { it.id == equippedSkinId } }

  var runKey by remember { mutableIntStateOf(1) }
  val activeBoosters = remember(runKey) { consumeBoosters() }

  var engine by remember(runKey, vehicle.id, map.id) {
    mutableStateOf(
      VehiclePhysicsEngine(
        vehicle = vehicle,
        map = map,
        upgrades = upgrades,
        autoBalanceAssist = saveData.settings.autoBalanceAssist,
        controlSensitivity = saveData.settings.controlSensitivity,
        fuelBoostActive = activeBoosters.first,
        nitroBoostActive = activeBoosters.second,
        magnetBoostActive = activeBoosters.third,
      )
    )
  }

  var isPaused by remember { mutableStateOf(false) }
  var runSummary by remember { mutableStateOf<RunSummary?>(null) }
  var doubledUsed by remember { mutableStateOf(false) }

  var gasPressed by remember { mutableStateOf(false) }
  var brakePressed by remember { mutableStateOf(false) }
  var turboPressed by remember { mutableStateOf(false) }

  var camX by remember(runKey) { mutableFloatStateOf(0f) }
  var camY by remember(runKey) { mutableFloatStateOf(1.2f) }
  var frameTick by remember { mutableLongStateOf(0L) }
  var animTimeSec by remember { mutableFloatStateOf(0f) }

  var hudDistance by remember(runKey) { mutableIntStateOf(0) }
  var hudCoins by remember(runKey) { mutableIntStateOf(0) }
  var hudScore by remember(runKey) { mutableIntStateOf(0) }
  var hudFuelFraction by remember(runKey) { mutableFloatStateOf(1f) }
  var hudSpeedKmh by remember(runKey) { mutableIntStateOf(0) }
  var hudTurboCharge by remember(runKey) { mutableFloatStateOf(1f) }

  val equipped3dModel = remember(saveData.equipped3dModelByVehicle, saveData.downloaded3dModels, vehicle.id) {
    saveData.equipped3dModelFor(vehicle.id)
  }
  var use3dRig by remember(equipped3dModel?.uid) {
    mutableStateOf(equipped3dModel != null && saveData.prefer3dShowroom)
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        if (!engine.isRunEnded) isPaused = true
        gasPressed = false
        brakePressed = false
        turboPressed = false
        soundEngine.stopEngine()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      soundEngine.stopEngine()
    }
  }

  BackHandler {
    if (runSummary != null) {
      onExitToMenu()
    } else {
      isPaused = !isPaused
      if (isPaused) soundEngine.stopEngine() else soundEngine.startEngine()
    }
  }

  LaunchedEffect(runKey, isPaused, engine.isRunEnded) {
    if (!isPaused && !engine.isRunEnded) soundEngine.startEngine() else soundEngine.stopEngine()
  }

  LaunchedEffect(runKey, isPaused) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { nanos ->
        if (lastNanos != 0L && !isPaused) {
          val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
          animTimeSec += dt

          val wasEnded = engine.isRunEnded
          val events =
            engine.step(
              frameDeltaSec = dt,
              gasPressed = gasPressed,
              brakePressed = brakePressed,
              turboPressed = turboPressed,
            )

          if (events.coinsDelta > 0 && !events.stuntTriggered) soundEngine.playCoin()
          if (events.diamondsDelta > 0) soundEngine.playStunt()
          if (events.fuelCollected) soundEngine.playFuel()
          if (events.stuntTriggered) soundEngine.playStunt()
          if (events.jumped) soundEngine.playJump()
          if (events.landedIntensity > 0.35f) soundEngine.playLand(events.landedIntensity)
          if (events.crashedNow) soundEngine.playCrash()

          soundEngine.updateEngine(
            rpm = engine.rpmNormalized,
            throttle = if (gasPressed) 1f else if (brakePressed) 0.55f else 0.1f,
          )

          val lookAhead = (engine.velX * 0.24f).coerceIn(-1.5f, 4.5f)
          val targetCamX = engine.posX + 2.4f + lookAhead
          val targetCamY = engine.posY + 0.95f
          camX += (targetCamX - camX) * (dt * 7.5f).coerceIn(0f, 1f)
          camY += (targetCamY - camY) * (dt * 5.5f).coerceIn(0f, 1f)

          hudDistance = engine.maxDistanceMeters
          hudCoins = engine.coinsCollectedRun
          hudScore = engine.liveScore
          hudFuelFraction = (engine.fuel / engine.maxFuel).coerceIn(0f, 1f)
          hudSpeedKmh = engine.speedKmh.toInt()
          hudTurboCharge = engine.turboCharge

          if (!wasEnded && engine.isRunEnded && runSummary == null) {
            soundEngine.stopEngine()
            val reasonLabel =
              if (engine.gameOverReason == GameOverReason.CRASHED) "Driver Crashed!"
              else "Out of Fuel!"
            runSummary =
              onRunFinished(
                engine.maxDistanceMeters,
                engine.liveScore,
                engine.elapsedTimeSec.toInt(),
                engine.coinsCollectedRun,
                engine.diamondsCollectedRun,
                engine.stuntsCompletedRun,
                engine.stuntBonusCoinsRun,
                engine.backflipsRun,
                engine.frontflipsRun,
                engine.fuelCansCollectedRun,
                engine.gameOverReason == GameOverReason.CRASHED,
                reasonLabel,
              )
          }
          frameTick = nanos
        }
        lastNanos = nanos
      }
    }
  }

  val lowFuelPulse = rememberInfiniteTransition(label = "low_fuel_pulse")
  val lowFuelScale by
    lowFuelPulse.animateFloat(
      initialValue = 1f,
      targetValue = 1.07f,
      animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
      label = "low_fuel_scale",
    )

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Live 2D Physics Canvas (keeps vehicle visible behind darkened Game Over scrim!)
    Canvas(modifier = Modifier.fillMaxSize()) {
      @Suppress("UNUSED_EXPRESSION")
      frameTick

      val pixelsPerMeter = (size.height / 11.2f).coerceIn(30f, 82f)
      val screenCenterX = size.width * 0.32f
      val screenCenterY = size.height * 0.56f

      drawGameWorldBackground(map, camX, animTimeSec, saveData.settings.graphicsQuality)
      drawTerrainAndProps(engine.terrain, map, camX, camY, pixelsPerMeter, screenCenterX, screenCenterY)
      drawPickupsAndParticles(
        engine.terrain,
        engine.particles,
        camX,
        camY,
        pixelsPerMeter,
        screenCenterX,
        screenCenterY,
        animTimeSec,
      )

      val vehScreenX = screenCenterX + (engine.posX - camX) * pixelsPerMeter
      val vehScreenY = screenCenterY - (engine.posY - camY) * pixelsPerMeter
      if (!use3dRig || equipped3dModel == null) {
        drawCartoonVehicle(
          spec = vehicle,
          centerPx = Offset(vehScreenX, vehScreenY),
          pixelsPerMeter = pixelsPerMeter,
          angleRad = engine.angle,
          rearWheelAngleRad = engine.rearWheelAngle,
          frontWheelAngleRad = engine.frontWheelAngle,
          rearCompressionM = engine.rearComp,
          frontCompressionM = engine.frontComp,
          skin = skin,
        )
      }

      drawWeatherParticles(map.weather, animTimeSec, saveData.settings.graphicsQuality)
    }

    // Optional Live 3D Sketchfab Vehicle Overlay synced to physics pitch & position
    if (use3dRig && equipped3dModel != null) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        frameTick
        val pxPerM = (constraints.maxHeight / 11.2f).coerceIn(30f, 82f)
        val scX = constraints.maxWidth * 0.32f
        val scY = constraints.maxHeight * 0.56f
        val density = LocalDensity.current.density
        val vehScreenX = scX + (engine.posX - camX) * pxPerM
        val vehScreenY = scY - (engine.posY - camY) * pxPerM
        val boxDp = ((pxPerM * 3.8f) / density).dp.coerceIn(130.dp, 240.dp)

        Sketchfab3DModelViewport(
          entryFilePath = equipped3dModel.entryFilePath,
          modelTitle = equipped3dModel.name,
          authorName = equipped3dModel.authorName,
          licenseLabel = equipped3dModel.licenseLabel,
          autoRotate = false,
          isOpaque = false,
          interactiveCamera = false,
          sideProfilePitchRad = engine.angle,
          modifier =
            Modifier.size(boxDp)
              .offset(
                x = (vehScreenX / density).dp - boxDp / 2f,
                y = (vehScreenY / density).dp - boxDp * 0.54f,
              ),
        )
      }
    }

    // Floating Stunts
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val pxPerM = (constraints.maxHeight / 11.2f).coerceIn(30f, 82f)
      val scX = constraints.maxWidth * 0.32f
      val scY = constraints.maxHeight * 0.56f
      val density = LocalDensity.current.density
      engine.floatingTexts.forEach { ft ->
        val sx = scX + (ft.x - camX) * pxPerM
        val sy = scY - (ft.y - camY) * pxPerM
        val alpha = (ft.life / ft.maxLife).coerceIn(0f, 1f)
        Text(
          text = ft.text,
          color = Color(ft.colorHex).copy(alpha = alpha),
          style =
            MaterialTheme.typography.titleLarge.copy(
              shadow = Shadow(Color.Black, Offset(2f, 2f), 6f)
            ),
          modifier =
            Modifier.offset(
              x = (sx / density).dp - 70.dp,
              y = (sy / density).dp - 16.dp,
            ),
        )
      }
    }

    // 2. Minimal, Clean Racing HUD
    if (runSummary == null) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // TOP HUD: Left (Pause + Distance) | Center (Coins + Score) | Right (Fuel + Speed)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          // TOP LEFT: Pause Button + Current Distance
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledIconButton(
              onClick = {
                soundEngine.playClick()
                isPaused = true
              },
              colors =
                IconButtonDefaults.filledIconButtonColors(
                  containerColor = MidnightBluePrimary.copy(alpha = 0.78f)
                ),
              modifier =
                Modifier.size(48.dp)
                  .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                  .testTag("pause_button"),
            ) {
              Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = TextPrimary)
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MidnightBluePrimary.copy(alpha = 0.78f),
              border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
            ) {
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                  text = "${hudDistance}m",
                  style = MaterialTheme.typography.titleLarge,
                  color = TextPrimary,
                  modifier = Modifier.testTag("hud_distance"),
                )
              }
            }
          }

          // TOP CENTER: Coin Counter + Score
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MidnightBluePrimary.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, GoldenYellowHighlight.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  Icons.Filled.MonetizationOn,
                  contentDescription = "Coins",
                  tint = GoldenYellowHighlight,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "$hudCoins",
                  style = MaterialTheme.typography.titleLarge,
                  color = GoldenYellowHighlight,
                  modifier = Modifier.testTag("hud_coins"),
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text("SCORE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                  text = "%,d".format(hudScore),
                  style = MaterialTheme.typography.labelLarge,
                  color = TextPrimary,
                )
              }
            }
          }

          // TOP RIGHT: Fuel Bar (turns orange & pulses when low!) + Speed Indicator
          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val isLowFuel = hudFuelFraction < 0.28f
            val fuelColor by
              animateColorAsState(
                targetValue = if (isLowFuel) RacingOrangeAccent else SuccessGreen,
                label = "hud_fuel_color",
              )
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MidnightBluePrimary.copy(alpha = 0.80f),
              border =
                BorderStroke(
                  width = if (isLowFuel) 2.dp else 1.dp,
                  color = if (isLowFuel) RacingOrangeAccent else Color.White.copy(alpha = 0.22f),
                ),
              modifier = if (isLowFuel) Modifier.scale(lowFuelScale) else Modifier,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Filled.LocalGasStation,
                  contentDescription = "Fuel",
                  tint = fuelColor,
                  modifier = Modifier.size(20.dp),
                )
                Box(
                  modifier =
                    Modifier.width(108.dp)
                      .height(10.dp)
                      .clip(RoundedCornerShape(50))
                      .background(DarkNavyBg)
                ) {
                  Box(
                    modifier =
                      Modifier.fillMaxHeight()
                        .fillMaxWidth(hudFuelFraction)
                        .clip(RoundedCornerShape(50))
                        .background(fuelColor)
                  )
                }
                if (isLowFuel) {
                  Text(
                    text = "LOW!",
                    style = MaterialTheme.typography.labelSmall,
                    color = RacingOrangeAccent,
                  )
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (equipped3dModel != null) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (use3dRig) RacingOrangeAccent else MidnightBluePrimary.copy(alpha = 0.78f),
                  modifier = Modifier.clickable { use3dRig = !use3dRig },
                ) {
                  Text(
                    text = if (use3dRig) "3D RIG" else "2D RIG",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (use3dRig) DarkNavyBg else TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MidnightBluePrimary.copy(alpha = 0.78f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(Icons.Filled.Speed, null, tint = SkyCyan, modifier = Modifier.size(16.dp))
                  Text(
                    text = "$hudSpeedKmh km/h",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyCyan,
                  )
                }
              }
            }
          }
        }

        // BOTTOM CONTROLS: Circular Semi-Transparent Brake (Left) & Gas + Turbo (Right)
        if (!isPaused) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
          ) {
            CircularControlButton(
              icon = Icons.Filled.FastRewind,
              label = "BRAKE",
              pressed = brakePressed,
              tintColor = DangerRed,
              sizeDp = 88,
              onPressedChange = { brakePressed = it },
              modifier = Modifier.testTag("brake_pedal"),
            )

            Row(
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              verticalAlignment = Alignment.Bottom,
            ) {
              CircularControlButton(
                icon = Icons.Filled.Bolt,
                label = "${(hudTurboCharge * 100).toInt()}%",
                pressed = turboPressed,
                tintColor = ElectricBlueSecondary,
                sizeDp = 70,
                onPressedChange = { turboPressed = it },
                modifier = Modifier.testTag("turbo_button"),
              )
              CircularControlButton(
                icon = Icons.Filled.FastForward,
                label = "GAS",
                pressed = gasPressed,
                tintColor = RacingOrangeAccent,
                sizeDp = 94,
                onPressedChange = { gasPressed = it },
                modifier = Modifier.testTag("gas_pedal"),
              )
            }
          }
        }
      }
    }

    // 3. Pause Overlay
    if (isPaused && runSummary == null) {
      Box(
        modifier = Modifier.fillMaxSize().background(DarkNavyBg.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardNavy,
          border = BorderStroke(2.dp, ElectricBlueSecondary),
          modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(0.72f),
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text("RACE PAUSED", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Text(
              text = "${vehicle.name} • ${map.name} • ${hudDistance}m",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            ArcadeGradientButton(
              text = "RESUME RACE",
              icon = Icons.Filled.PlayArrow,
              colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
              textColor = DarkNavyBg,
              onClick = {
                soundEngine.playClick()
                isPaused = false
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "resume_button",
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              ArcadeGradientButton(
                text = "RESTART",
                icon = Icons.Filled.Refresh,
                colors = listOf(ElectricBlueSecondary, Color(0xFF1A4FD6)),
                onClick = {
                  soundEngine.playClick()
                  isPaused = false
                  runKey++
                },
                modifier = Modifier.weight(1f),
                testTag = "restart_pause_button",
              )
              ArcadeGradientButton(
                text = "HOME",
                icon = Icons.Filled.Home,
                colors = listOf(CardNavyElevated, MidnightBluePrimary),
                onClick = {
                  soundEngine.playClick()
                  onExitToMenu()
                },
                modifier = Modifier.weight(1f),
                testTag = "pause_menu_button",
              )
            }
          }
        }
      }
    }

    // 4. Exciting Game Over Overlay with Animated Counters (darkened world + vehicle visible behind)
    val summary = runSummary
    if (summary != null) {
      GameOverOverlay(
        summary = summary,
        doubledUsed = doubledUsed,
        onRetry = {
          soundEngine.playClick()
          runSummary = null
          doubledUsed = false
          runKey++
        },
        onGarage = {
          soundEngine.playClick()
          onOpenGarage()
        },
        onHome = {
          soundEngine.playClick()
          onExitToMenu()
        },
        onDoubleRewards = {
          doubledUsed = true
          onDoubleRewards(summary.coinsEarned)
        },
      )
    }
  }
}

@Composable
private fun CircularControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  pressed: Boolean,
  tintColor: Color,
  sizeDp: Int,
  onPressedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) 0.90f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMedium),
      label = "pedal_scale",
    )

  Box(
    modifier =
      modifier
        .scale(scale)
        .size(sizeDp.dp)
        .shadow(elevation = if (pressed) 2.dp else 10.dp, shape = CircleShape)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors =
              if (pressed) listOf(tintColor.copy(alpha = 0.85f), MidnightBluePrimary.copy(alpha = 0.92f))
              else listOf(MidnightBluePrimary.copy(alpha = 0.62f), DarkNavyBg.copy(alpha = 0.78f))
          )
        )
        .border(
          width = if (pressed) 3.dp else 2.dp,
          color = if (pressed) Color.White else tintColor.copy(alpha = 0.85f),
          shape = CircleShape,
        )
        .pointerInput(Unit) {
          awaitPointerEventScope {
            val activeIds = mutableSetOf<PointerId>()
            while (true) {
              val event = awaitPointerEvent()
              event.changes.forEach { change ->
                if (change.pressed) activeIds.add(change.id) else activeIds.remove(change.id)
                change.consume()
              }
              onPressedChange(activeIds.isNotEmpty())
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = TextPrimary,
        modifier = Modifier.size((sizeDp * 0.38f).dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun GameOverOverlay(
  summary: RunSummary,
  doubledUsed: Boolean,
  onRetry: () -> Unit,
  onGarage: () -> Unit,
  onHome: () -> Unit,
  onDoubleRewards: () -> Unit,
) {
  var revealStarted by remember { mutableStateOf(false) }
  LaunchedEffect(summary) { revealStarted = true }

  val animDist by
    animateIntAsState(
      targetValue = if (revealStarted) summary.distanceMeters else 0,
      animationSpec = tween(750),
      label = "go_dist",
    )
  val animBest by
    animateIntAsState(
      targetValue = if (revealStarted) maxOf(summary.distanceMeters, summary.bestDistanceBefore) else 0,
      animationSpec = tween(750),
      label = "go_best",
    )
  val animCoins by
    animateIntAsState(
      targetValue =
        if (revealStarted) (if (doubledUsed) summary.coinsEarned * 2 else summary.coinsEarned) else 0,
      animationSpec = tween(750),
      label = "go_coins",
    )
  val animStunts by
    animateIntAsState(
      targetValue = if (revealStarted) summary.stuntsCompleted else 0,
      animationSpec = tween(650),
      label = "go_stunts",
    )
  val animTime by
    animateIntAsState(
      targetValue = if (revealStarted) summary.timeSurvivedSec else 0,
      animationSpec = tween(650),
      label = "go_time",
    )

  Box(
    modifier = Modifier.fillMaxSize().background(DarkNavyBg.copy(alpha = 0.68f)),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = CardNavy.copy(alpha = 0.95f),
      border =
        BorderStroke(
          2.dp,
          if (summary.isNewRecord) GoldenYellowHighlight else ElectricBlueSecondary,
        ),
      modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(0.88f),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "RACE FINISHED!",
              style = MaterialTheme.typography.headlineLarge,
              color = GoldenYellowHighlight,
            )
            Text(
              text = "${summary.reason} • ${summary.vehicleName} on ${summary.mapName}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
          if (summary.isNewRecord) {
            Surface(shape = RoundedCornerShape(50), color = SuccessGreen) {
              Text(
                text = "NEW RECORD!",
                style = MaterialTheme.typography.labelLarge,
                color = DarkNavyBg,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              )
            }
          }
        }

        // 5 Animated Counter Stat Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ResultStatBox("TOTAL DISTANCE", "${animDist}m", SkyCyan, Modifier.weight(1f))
          ResultStatBox("BEST DISTANCE", "${animBest}m", GoldenYellowHighlight, Modifier.weight(1f))
          ResultStatBox("COINS EARNED", "+$animCoins", GoldenYellowHighlight, Modifier.weight(1f))
          ResultStatBox("STUNTS", "$animStunts", SuccessGreen, Modifier.weight(1f))
          ResultStatBox("TIME SURVIVED", "${animTime}s", RacingOrangeAccent, Modifier.weight(1f))
        }

        // Reward Summary Banner
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MidnightBluePrimary,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "REWARD SUMMARY: +${summary.xpEarned} XP" +
                (if (summary.diamondsEarned > 0) " • +${summary.diamondsEarned} Diamonds" else "") +
                (if (summary.stuntBonusCoins > 0) " • +${summary.stuntBonusCoins} Stunt Coins" else ""),
              style = MaterialTheme.typography.labelMedium,
              color = TextPrimary,
            )
            Text(
              text = if (summary.leveledUp) "LEVEL UP -> LV ${summary.newLevel}!" else "LV ${summary.newLevel}",
              style = MaterialTheme.typography.labelLarge,
              color = if (summary.leveledUp) SuccessGreen else GoldenYellowHighlight,
            )
          }
        }

        // Dominant RETRY + GARAGE + HOME
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ArcadeGradientButton(
            text = "RETRY",
            icon = Icons.Filled.Refresh,
            colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
            textColor = DarkNavyBg,
            onClick = onRetry,
            modifier = Modifier.weight(1.45f).height(52.dp),
            testTag = "gameover_retry_button",
          )
          ArcadeGradientButton(
            text = "GARAGE",
            icon = Icons.Filled.Garage,
            colors = listOf(ElectricBlueSecondary, Color(0xFF1A4FD6)),
            onClick = onGarage,
            modifier = Modifier.weight(1f).height(52.dp),
            testTag = "gameover_garage_button",
          )
          ArcadeGradientButton(
            text = "HOME",
            icon = Icons.Filled.Home,
            colors = listOf(CardNavyElevated, MidnightBluePrimary),
            onClick = onHome,
            modifier = Modifier.weight(1f).height(52.dp),
            testTag = "gameover_menu_button",
          )
        }

        if (!doubledUsed && summary.coinsEarned > 0) {
          ArcadeGradientButton(
            text = "WATCH AD FOR BONUS COINS (+${summary.coinsEarned})",
            icon = Icons.Filled.PlayCircle,
            colors = listOf(SuccessGreen, EmeraldDark),
            textColor = DarkNavyBg,
            onClick = onDoubleRewards,
            modifier = Modifier.fillMaxWidth(),
            testTag = "gameover_double_reward_button",
          )
        }
      }
    }
  }
}

@Composable
private fun ResultStatBox(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MidnightBluePrimary,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
      Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
    }
  }
}
