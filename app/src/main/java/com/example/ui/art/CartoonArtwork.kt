package com.example.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.model.*
import com.example.physics.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawCartoonVehicle(
  spec: VehicleSpec,
  centerPx: Offset,
  pixelsPerMeter: Float,
  angleRad: Float,
  rearWheelAngleRad: Float,
  frontWheelAngleRad: Float,
  rearCompressionM: Float,
  frontCompressionM: Float,
  skin: VehicleSkin? = null,
) {
  val bodyColor = Color(skin?.takeIf { it.id != "default" }?.primaryColor ?: spec.primaryColor)
  val accentColor = Color(skin?.takeIf { it.id != "default" }?.secondaryColor ?: spec.secondaryColor)
  val outlineColor = Color(0xFF111827)

  val cw = spec.chassisWidth * pixelsPerMeter
  val ch = spec.chassisHeight * pixelsPerMeter
  val wb = spec.wheelbase * pixelsPerMeter
  val frontR = spec.wheelRadius * pixelsPerMeter
  val rearR = spec.wheelRadius * spec.rearWheelScale * pixelsPerMeter
  val strokeW = (pixelsPerMeter * 0.06f).coerceIn(2f, 4.5f)

  val angleDeg = (-angleRad * 180f / PI.toFloat())

  withTransform({
    translate(centerPx.x, centerPx.y)
    rotate(angleDeg, Offset.Zero)
  }) {
    val halfBase = wb * 0.5f
    val wheelY = ch * 0.44f - (rearCompressionM * pixelsPerMeter * 0.45f)
    val frontWheelY = ch * 0.44f - (frontCompressionM * pixelsPerMeter * 0.45f)

    // Suspension struts & coil springs
    drawLine(
      color = Color(0xFF94A3B8),
      start = Offset(-halfBase * 0.9f, 0f),
      end = Offset(-halfBase, wheelY),
      strokeWidth = strokeW * 1.4f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color(0xFF94A3B8),
      start = Offset(halfBase * 0.9f, 0f),
      end = Offset(halfBase, frontWheelY),
      strokeWidth = strokeW * 1.4f,
      cap = StrokeCap.Round,
    )
    // Orange shock coils
    drawLine(
      color = Color(0xFFFF6B1A),
      start = Offset(-halfBase * 0.94f, ch * 0.1f),
      end = Offset(-halfBase, wheelY * 0.8f),
      strokeWidth = strokeW * 1.9f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color(0xFFFF6B1A),
      start = Offset(halfBase * 0.94f, ch * 0.1f),
      end = Offset(halfBase, frontWheelY * 0.8f),
      strokeWidth = strokeW * 1.9f,
      cap = StrokeCap.Round,
    )

    // Driver helmet + scarf
    val driverX = -cw * 0.08f
    val driverY = -ch * 0.52f
    // Scarf fluttering behind
    drawLine(
      color = Color(0xFFFF6B1A),
      start = Offset(driverX - cw * 0.04f, driverY + ch * 0.16f),
      end = Offset(driverX - cw * 0.24f, driverY + ch * 0.06f),
      strokeWidth = strokeW * 1.8f,
      cap = StrokeCap.Round,
    )
    // Driver torso
    drawRoundRect(
      color = Color(0xFF1E293B),
      topLeft = Offset(driverX - cw * 0.08f, driverY + ch * 0.1f),
      size = Size(cw * 0.16f, ch * 0.28f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    // Helmet dome
    val helmR = ch * 0.26f
    drawCircle(color = Color(0xFFFFFBEB), radius = helmR + strokeW * 0.5f, center = Offset(driverX, driverY))
    drawCircle(color = Color(0xFFEF4444), radius = helmR, center = Offset(driverX, driverY))
    drawCircle(
      color = outlineColor,
      radius = helmR,
      center = Offset(driverX, driverY),
      style = Stroke(width = strokeW * 0.8f),
    )
    // Visor
    drawArc(
      color = Color(0xFF38BDF8),
      startAngle = -35f,
      sweepAngle = 70f,
      useCenter = true,
      topLeft = Offset(driverX - helmR * 0.5f, driverY - helmR * 0.55f),
      size = Size(helmR * 1.4f, helmR * 1.1f),
    )

    // Vehicle Body per Style
    when (spec.style) {
      VehicleVisualStyle.BIKE -> {
        val framePath =
          Path().apply {
            moveTo(-halfBase, wheelY * 0.45f)
            lineTo(-cw * 0.12f, -ch * 0.35f)
            lineTo(cw * 0.28f, -ch * 0.28f)
            lineTo(halfBase, wheelY * 0.45f)
            lineTo(cw * 0.10f, ch * 0.05f)
            close()
          }
        drawPath(framePath, color = bodyColor)
        drawPath(framePath, color = outlineColor, style = Stroke(width = strokeW))
        // Fuel tank accent
        drawRoundRect(
          color = accentColor,
          topLeft = Offset(-cw * 0.05f, -ch * 0.42f),
          size = Size(cw * 0.32f, ch * 0.24f),
          cornerRadius = CornerRadius(8f, 8f),
        )
      }
      VehicleVisualStyle.SPORTS, VehicleVisualStyle.ELECTRIC -> {
        val aeroPath =
          Path().apply {
            moveTo(-cw * 0.5f, ch * 0.28f)
            lineTo(-cw * 0.48f, -ch * 0.20f)
            lineTo(-cw * 0.26f, -ch * 0.48f)
            lineTo(cw * 0.12f, -ch * 0.48f)
            lineTo(cw * 0.48f, -ch * 0.05f)
            lineTo(cw * 0.50f, ch * 0.28f)
            close()
          }
        drawPath(aeroPath, color = bodyColor)
        drawPath(aeroPath, color = outlineColor, style = Stroke(width = strokeW))
        // Rear spoiler
        drawLine(
          color = accentColor,
          start = Offset(-cw * 0.44f, -ch * 0.22f),
          end = Offset(-cw * 0.54f, -ch * 0.46f),
          strokeWidth = strokeW * 1.5f,
          cap = StrokeCap.Round,
        )
        // Racing stripe
        drawRect(
          color = accentColor,
          topLeft = Offset(-cw * 0.38f, ch * 0.02f),
          size = Size(cw * 0.80f, ch * 0.11f),
        )
      }
      VehicleVisualStyle.TRACTOR -> {
        // Engine hood + rear mudguard
        drawRoundRect(
          color = bodyColor,
          topLeft = Offset(-cw * 0.45f, -ch * 0.28f),
          size = Size(cw * 0.90f, ch * 0.58f),
          cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
          color = outlineColor,
          topLeft = Offset(-cw * 0.45f, -ch * 0.28f),
          size = Size(cw * 0.90f, ch * 0.58f),
          cornerRadius = CornerRadius(10f, 10f),
          style = Stroke(width = strokeW),
        )
        // Vertical exhaust pipe
        drawRect(
          color = Color(0xFF334155),
          topLeft = Offset(cw * 0.20f, -ch * 0.68f),
          size = Size(cw * 0.07f, ch * 0.42f),
        )
        // Stripe
        drawRect(
          color = accentColor,
          topLeft = Offset(-cw * 0.15f, -ch * 0.10f),
          size = Size(cw * 0.55f, ch * 0.14f),
        )
      }
      VehicleVisualStyle.BUS -> {
        drawRoundRect(
          color = bodyColor,
          topLeft = Offset(-cw * 0.5f, -ch * 0.45f),
          size = Size(cw, ch * 0.78f),
          cornerRadius = CornerRadius(12f, 12f),
        )
        drawRoundRect(
          color = outlineColor,
          topLeft = Offset(-cw * 0.5f, -ch * 0.45f),
          size = Size(cw, ch * 0.78f),
          cornerRadius = CornerRadius(12f, 12f),
          style = Stroke(width = strokeW),
        )
        // School bus windows
        for (w in 0..3) {
          drawRoundRect(
            color = Color(0xFFBAE6FD),
            topLeft = Offset(-cw * 0.40f + w * cw * 0.21f, -ch * 0.34f),
            size = Size(cw * 0.16f, ch * 0.26f),
            cornerRadius = CornerRadius(4f, 4f),
          )
        }
        drawRect(
          color = accentColor,
          topLeft = Offset(-cw * 0.48f, ch * 0.05f),
          size = Size(cw * 0.96f, ch * 0.09f),
        )
      }
      VehicleVisualStyle.HEAVY_RIG -> {
        // Flatbed + tall cab
        drawRoundRect(
          color = accentColor,
          topLeft = Offset(-cw * 0.5f, -ch * 0.08f),
          size = Size(cw * 0.54f, ch * 0.40f),
          cornerRadius = CornerRadius(6f, 6f),
        )
        drawRoundRect(
          color = bodyColor,
          topLeft = Offset(cw * 0.02f, -ch * 0.52f),
          size = Size(cw * 0.46f, ch * 0.84f),
          cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
          color = outlineColor,
          topLeft = Offset(cw * 0.02f, -ch * 0.52f),
          size = Size(cw * 0.46f, ch * 0.84f),
          cornerRadius = CornerRadius(10f, 10f),
          style = Stroke(width = strokeW),
        )
        drawRect(
          color = Color(0xFFBAE6FD),
          topLeft = Offset(cw * 0.16f, -ch * 0.40f),
          size = Size(cw * 0.25f, ch * 0.28f),
        )
      }
      else -> {
        // JEEP, MONSTER, PICKUP, RALLY, ARMY, SNOW_CAT
        drawRoundRect(
          color = bodyColor,
          topLeft = Offset(-cw * 0.5f, -ch * 0.24f),
          size = Size(cw, ch * 0.56f),
          cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
          color = outlineColor,
          topLeft = Offset(-cw * 0.5f, -ch * 0.24f),
          size = Size(cw, ch * 0.56f),
          cornerRadius = CornerRadius(10f, 10f),
          style = Stroke(width = strokeW),
        )
        // Roll cage / cabin roof
        val cageStart = -cw * 0.34f
        val cageEnd = cw * 0.16f
        val cageTop = -ch * 0.68f
        drawLine(
          color = outlineColor,
          start = Offset(cageStart, -ch * 0.22f),
          end = Offset(cageStart + cw * 0.08f, cageTop),
          strokeWidth = strokeW * 1.4f,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = outlineColor,
          start = Offset(cageStart + cw * 0.08f, cageTop),
          end = Offset(cageEnd, cageTop),
          strokeWidth = strokeW * 1.4f,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = outlineColor,
          start = Offset(cageEnd, cageTop),
          end = Offset(cageEnd + cw * 0.10f, -ch * 0.22f),
          strokeWidth = strokeW * 1.4f,
          cap = StrokeCap.Round,
        )
        // Accent stripe
        drawRect(
          color = accentColor,
          topLeft = Offset(-cw * 0.44f, ch * 0.01f),
          size = Size(cw * 0.88f, ch * 0.12f),
        )
      }
    }

    // Headlight glow
    drawCircle(
      color = Color(0xFFFEF08A),
      radius = ch * 0.11f,
      center = Offset(cw * 0.47f, -ch * 0.04f),
    )

    // Rear & Front Tires
    drawWheel(Offset(-halfBase, wheelY), rearR, rearWheelAngleRad, accentColor, strokeW)
    drawWheel(Offset(halfBase, frontWheelY), frontR, frontWheelAngleRad, accentColor, strokeW)
  }
}

private fun DrawScope.drawWheel(
  center: Offset,
  radius: Float,
  angleRad: Float,
  hubColor: Color,
  strokeW: Float,
) {
  val rotDeg = (-angleRad * 180f / PI.toFloat())
  withTransform({
    translate(center.x, center.y)
    rotate(rotDeg, Offset.Zero)
  }) {
    // Tire rubber
    drawCircle(color = Color(0xFF1F2937), radius = radius, center = Offset.Zero)
    drawCircle(
      color = Color(0xFF0F172A),
      radius = radius,
      center = Offset.Zero,
      style = Stroke(width = strokeW * 1.1f),
    )
    // Tread notches
    for (i in 0 until 8) {
      val a = i * (PI.toFloat() / 4f)
      drawLine(
        color = Color(0xFF374151),
        start = Offset(cos(a) * radius * 0.78f, sin(a) * radius * 0.78f),
        end = Offset(cos(a) * radius * 0.98f, sin(a) * radius * 0.98f),
        strokeWidth = strokeW * 1.2f,
        cap = StrokeCap.Round,
      )
    }
    // Rim & Spokes
    drawCircle(color = Color(0xFFE2E8F0), radius = radius * 0.56f, center = Offset.Zero)
    for (i in 0 until 5) {
      val a = i * (2f * PI.toFloat() / 5f)
      drawLine(
        color = Color(0xFF475569),
        start = Offset.Zero,
        end = Offset(cos(a) * radius * 0.50f, sin(a) * radius * 0.50f),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
    }
    drawCircle(color = hubColor, radius = radius * 0.22f, center = Offset.Zero)
  }
}

fun DrawScope.drawGameWorldBackground(
  map: MapSpec,
  cameraX: Float,
  timeSec: Float,
  quality: GraphicsQuality,
) {
  val w = size.width
  val h = size.height

  // Sky Gradient
  drawRect(
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(map.skyTopColor), Color(map.skyBottomColor)),
        startY = 0f,
        endY = h,
      )
  )

  // Sun / Moon / Planet
  val sunCenter = Offset(w * 0.78f, h * 0.22f)
  if (map.id == MapId.SPACE) {
    // Ringed planet + stars
    for (i in 0 until 26) {
      val sx = ((i * 197 + 43) % 1000) / 1000f * w
      val sy = ((i * 313 + 17) % 1000) / 1000f * (h * 0.65f)
      val twinkle = 1.5f + sin(timeSec * 3f + i) * 1f
      drawCircle(Color.White.copy(alpha = 0.8f), radius = twinkle, center = Offset(sx, sy))
    }
    drawCircle(Color(0xFFA855F7), radius = h * 0.11f, center = sunCenter)
    drawOval(
      color = Color(0xFFE9D5FF).copy(alpha = 0.55f),
      topLeft = Offset(sunCenter.x - h * 0.20f, sunCenter.y - h * 0.03f),
      size = Size(h * 0.40f, h * 0.06f),
      style = Stroke(width = 4f),
    )
  } else {
    drawCircle(
      color = Color(0xFFFEF08A).copy(alpha = 0.28f),
      radius = h * 0.14f,
      center = sunCenter,
    )
    drawCircle(color = Color(0xFFFDE047), radius = h * 0.085f, center = sunCenter)
  }

  // Drifting Clouds (unless Space)
  if (map.id != MapId.SPACE && quality != GraphicsQuality.LOW) {
    for (c in 0 until 5) {
      val speed = 8f + c * 3f
      val rawX = ((c * 320f - cameraX * 3.5f - timeSec * speed) % (w + 300f) + (w + 300f)) % (w + 300f) - 150f
      val cy = h * (0.12f + (c % 3) * 0.09f)
      val cColor = Color.White.copy(alpha = if (map.id == MapId.VOLCANO) 0.25f else 0.72f)
      drawCircle(cColor, radius = 26f, center = Offset(rawX, cy))
      drawCircle(cColor, radius = 34f, center = Offset(rawX + 28f, cy - 8f))
      drawCircle(cColor, radius = 24f, center = Offset(rawX + 56f, cy))
    }
  }

  // Parallax Far Mountain Layer
  val farPath = Path()
  farPath.moveTo(0f, h)
  val farStep = 40f
  var sx = 0f
  while (sx <= w + farStep) {
    val worldSample = sx + cameraX * 6f
    val mountainH =
      h * 0.58f -
        sin(worldSample * 0.0045f) * h * 0.14f -
        cos(worldSample * 0.009f) * h * 0.06f
    farPath.lineTo(sx, mountainH)
    sx += farStep
  }
  farPath.lineTo(w, h)
  farPath.close()
  drawPath(farPath, color = Color(map.farHillColor).copy(alpha = 0.75f))

  // Parallax Mid Hill Layer
  val midPath = Path()
  midPath.moveTo(0f, h)
  sx = 0f
  while (sx <= w + farStep) {
    val worldSample = sx + cameraX * 13f
    val hillH =
      h * 0.70f -
        sin(worldSample * 0.007f + 1.2f) * h * 0.09f -
        cos(worldSample * 0.015f) * h * 0.04f
    midPath.lineTo(sx, hillH)
    sx += farStep
  }
  midPath.lineTo(w, h)
  midPath.close()
  drawPath(midPath, color = Color(map.midHillColor).copy(alpha = 0.88f))
}

fun DrawScope.drawTerrainAndProps(
  terrain: TerrainGenerator,
  map: MapSpec,
  cameraX: Float,
  cameraY: Float,
  pixelsPerMeter: Float,
  screenCenterX: Float,
  screenCenterY: Float,
) {
  fun worldToScreenX(wx: Float): Float = screenCenterX + (wx - cameraX) * pixelsPerMeter
  fun worldToScreenY(wy: Float): Float = screenCenterY - (wy - cameraY) * pixelsPerMeter

  val minVisX = cameraX - (screenCenterX / pixelsPerMeter) - 4f
  val maxVisX = cameraX + ((size.width - screenCenterX) / pixelsPerMeter) + 4f

  // Environment Props behind road
  for (prop in terrain.activeProps()) {
    if (prop.x !in minVisX..maxVisX) continue
    val px = worldToScreenX(prop.x)
    val py = worldToScreenY(prop.y)
    val s = prop.scale * pixelsPerMeter * 0.45f
    when (map.id) {
      MapId.DESERT -> {
        // Cactus
        drawRoundRect(
          color = Color(0xFF15803D),
          topLeft = Offset(px - s * 0.15f, py - s * 1.4f),
          size = Size(s * 0.30f, s * 1.4f),
          cornerRadius = CornerRadius(6f, 6f),
        )
        drawRoundRect(
          color = Color(0xFF15803D),
          topLeft = Offset(px - s * 0.55f, py - s * 1.0f),
          size = Size(s * 0.45f, s * 0.22f),
          cornerRadius = CornerRadius(4f, 4f),
        )
      }
      MapId.CITY, MapId.INDUSTRIAL_ZONE -> {
        // Urban lamp / factory tower
        drawRect(
          color = Color(0xFF334155),
          topLeft = Offset(px - s * 0.08f, py - s * 2.0f),
          size = Size(s * 0.16f, s * 2.0f),
        )
        drawCircle(
          color = Color(0xFFFACC15),
          radius = s * 0.22f,
          center = Offset(px, py - s * 2.05f),
        )
      }
      MapId.SPACE -> {
        // Alien crystal spire
        val crystal =
          Path().apply {
            moveTo(px - s * 0.25f, py)
            lineTo(px, py - s * 1.3f)
            lineTo(px + s * 0.25f, py)
            close()
          }
        drawPath(crystal, color = Color(0xFF38BDF8))
      }
      else -> {
        // Cartoon tree / palm / pine / village hut
        if (prop.kind == 2 && map.id == MapId.INDIAN_VILLAGE) {
          // Small Indian village hut
          drawRect(
            color = Color(0xFFFEF3C7),
            topLeft = Offset(px - s * 0.65f, py - s * 0.9f),
            size = Size(s * 1.3f, s * 0.9f),
          )
          val roof =
            Path().apply {
              moveTo(px - s * 0.85f, py - s * 0.88f)
              lineTo(px, py - s * 1.55f)
              lineTo(px + s * 0.85f, py - s * 0.88f)
              close()
            }
          drawPath(roof, color = Color(0xFFB45309))
        } else {
          // Tree trunk + lush canopy
          drawRect(
            color = Color(0xFF78350F),
            topLeft = Offset(px - s * 0.12f, py - s * 1.15f),
            size = Size(s * 0.24f, s * 1.15f),
          )
          val leafColor =
            when (map.id) {
              MapId.SNOW_MOUNTAINS, MapId.MOUNTAIN_PASS -> Color(0xFFE2E8F0)
              MapId.VOLCANO -> Color(0xFF44403C)
              else -> Color(0xFF15803D)
            }
          drawCircle(leafColor, radius = s * 0.58f, center = Offset(px, py - s * 1.45f))
          drawCircle(
            leafColor.copy(alpha = 0.85f),
            radius = s * 0.42f,
            center = Offset(px + s * 0.28f, py - s * 1.25f),
          )
        }
      }
    }
  }

  // Terrain Polygon + Top Grass/Road Strip
  val samples = terrain.visibleSamples(minVisX, maxVisX)
  if (samples.size < 2) return

  val bodyPath = Path()
  val topLinePath = Path()
  samples.forEachIndexed { idx, s ->
    val sx = worldToScreenX(s.x)
    val sy = worldToScreenY(s.y)
    if (idx == 0) {
      bodyPath.moveTo(sx, size.height + 40f)
      bodyPath.lineTo(sx, sy)
      topLinePath.moveTo(sx, sy)
    } else {
      bodyPath.lineTo(sx, sy)
      topLinePath.lineTo(sx, sy)
    }
  }
  val lastSx = worldToScreenX(samples.last().x)
  bodyPath.lineTo(lastSx, size.height + 40f)
  bodyPath.close()

  drawPath(
    path = bodyPath,
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(map.groundBodyColor), Color(0xFF0F172A)),
        startY = size.height * 0.45f,
        endY = size.height,
      ),
  )

  // Surface Crust
  drawPath(
    path = topLinePath,
    color = Color(map.groundTopColor),
    style = Stroke(width = (pixelsPerMeter * 0.24f).coerceIn(6f, 16f), cap = StrokeCap.Round),
  )

  // Draw Bridges clearly
  for (bridge in terrain.activeBridges()) {
    if (bridge.endX < minVisX || bridge.startX > maxVisX) continue
    val bx1 = worldToScreenX(bridge.startX)
    val bx2 = worldToScreenX(bridge.endX)
    val by1 = worldToScreenY(bridge.startY)
    val by2 = worldToScreenY(bridge.endY)
    drawLine(
      color = Color(0xFF92400E),
      start = Offset(bx1, by1 + 4f),
      end = Offset(bx2, by2 + 4f),
      strokeWidth = 10f,
    )
    drawLine(
      color = Color(0xFFF59E0B),
      start = Offset(bx1, by1 - 14f),
      end = Offset(bx2, by2 - 14f),
      strokeWidth = 3f,
    )
  }
}

fun DrawScope.drawPickupsAndParticles(
  terrain: TerrainGenerator,
  particles: List<Particle>,
  cameraX: Float,
  cameraY: Float,
  pixelsPerMeter: Float,
  screenCenterX: Float,
  screenCenterY: Float,
  timeSec: Float,
) {
  fun worldToScreenX(wx: Float): Float = screenCenterX + (wx - cameraX) * pixelsPerMeter
  fun worldToScreenY(wy: Float): Float = screenCenterY - (wy - cameraY) * pixelsPerMeter

  val minVisX = cameraX - (screenCenterX / pixelsPerMeter) - 3f
  val maxVisX = cameraX + ((size.width - screenCenterX) / pixelsPerMeter) + 3f

  for (p in terrain.activePickups()) {
    if (p.collected || p.x !in minVisX..maxVisX) continue
    val bob = sin(timeSec * 4.5f + p.id) * 0.08f
    val px = worldToScreenX(p.x)
    val py = worldToScreenY(p.y + bob)

    when (p.kind) {
      PickupKind.COIN, PickupKind.BONUS_COIN -> {
        val r = (if (p.kind == PickupKind.BONUS_COIN) 0.42f else 0.31f) * pixelsPerMeter
        val spinWidth = (0.4f + 0.6f * kotlin.math.abs(cos(timeSec * 3.5f + p.id))) * r
        drawOval(
          color = Color(0xFFB45309),
          topLeft = Offset(px - spinWidth - 2f, py - r - 2f),
          size = Size((spinWidth + 2f) * 2f, (r + 2f) * 2f),
        )
        drawOval(
          color = if (p.kind == PickupKind.BONUS_COIN) Color(0xFFFF9800) else Color(0xFFFFC107),
          topLeft = Offset(px - spinWidth, py - r),
          size = Size(spinWidth * 2f, r * 2f),
        )
        drawOval(
          color = Color(0xFFFEF08A),
          topLeft = Offset(px - spinWidth * 0.55f, py - r * 0.55f),
          size = Size(spinWidth * 1.1f, r * 1.1f),
        )
      }
      PickupKind.DIAMOND -> {
        val r = 0.38f * pixelsPerMeter
        val path =
          Path().apply {
            moveTo(px, py - r)
            lineTo(px + r * 0.85f, py)
            lineTo(px, py + r)
            lineTo(px - r * 0.85f, py)
            close()
          }
        drawPath(path, color = Color(0xFF22D3EE))
        drawPath(path, color = Color.White, style = Stroke(width = 2.5f))
      }
      PickupKind.FUEL -> {
        val fw = 0.56f * pixelsPerMeter
        val fh = 0.66f * pixelsPerMeter
        // Glowing halo
        drawCircle(
          color = Color(0xFFEF4444).copy(alpha = 0.25f),
          radius = fw * 0.9f,
          center = Offset(px, py),
        )
        drawRoundRect(
          color = Color(0xFFDC2626),
          topLeft = Offset(px - fw * 0.5f, py - fh * 0.5f),
          size = Size(fw, fh),
          cornerRadius = CornerRadius(8f, 8f),
        )
        drawRoundRect(
          color = Color.White,
          topLeft = Offset(px - fw * 0.5f, py - fh * 0.5f),
          size = Size(fw, fh),
          cornerRadius = CornerRadius(8f, 8f),
          style = Stroke(width = 2.5f),
        )
        // Fuel spout & white cross/bar
        drawRect(
          color = Color(0xFFFACC15),
          topLeft = Offset(px + fw * 0.1f, py - fh * 0.65f),
          size = Size(fw * 0.28f, fh * 0.20f),
        )
        drawRect(
          color = Color.White,
          topLeft = Offset(px - fw * 0.26f, py - fh * 0.12f),
          size = Size(fw * 0.52f, fh * 0.24f),
        )
      }
    }
  }

  for (pt in particles) {
    val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
    drawCircle(
      color = Color(pt.colorHex).copy(alpha = alpha),
      radius = pt.radius * pixelsPerMeter,
      center = Offset(worldToScreenX(pt.x), worldToScreenY(pt.y)),
    )
  }
}

fun DrawScope.drawWeatherParticles(
  weather: WeatherEffect,
  timeSec: Float,
  quality: GraphicsQuality,
) {
  if (weather == WeatherEffect.NONE || weather == WeatherEffect.STARS || quality == GraphicsQuality.LOW) {
    return
  }
  val count = if (quality == GraphicsQuality.HIGH) 36 else 18
  val w = size.width
  val h = size.height
  for (i in 0 until count) {
    when (weather) {
      WeatherEffect.SNOWFALL -> {
        val x = ((i * 137f + timeSec * (25f + (i % 5) * 8f)) % w)
        val y = ((i * 89f + timeSec * (55f + (i % 4) * 14f)) % h)
        drawCircle(Color.White.copy(alpha = 0.78f), radius = 3.5f + (i % 3), center = Offset(x, y))
      }
      WeatherEffect.DUST_STORM -> {
        val x = ((i * 143f - timeSec * 190f) % w + w) % w
        val y = ((i * 71f + sin(timeSec * 2f + i) * 20f) % h + h) % h
        drawLine(
          color = Color(0xFFFDE68A).copy(alpha = 0.35f),
          start = Offset(x, y),
          end = Offset(x + 26f, y + 3f),
          strokeWidth = 3f,
        )
      }
      WeatherEffect.ASH_EMBERS -> {
        val x = ((i * 129f + sin(timeSec + i) * 30f) % w + w) % w
        val y = ((i * 97f - timeSec * 65f) % h + h) % h
        drawCircle(Color(0xFFFF6B1A).copy(alpha = 0.75f), radius = 3f, center = Offset(x, y))
      }
      WeatherEffect.RAIN_MIST -> {
        val x = ((i * 111f - timeSec * 120f) % w + w) % w
        val y = ((i * 79f + timeSec * 310f) % h)
        drawLine(
          color = Color(0xFFBAE6FD).copy(alpha = 0.42f),
          start = Offset(x, y),
          end = Offset(x - 6f, y + 18f),
          strokeWidth = 2f,
        )
      }
      else -> Unit
    }
  }
}

fun DrawScope.drawShowroomBackdrop(
  timeSec: Float,
  accentColor: Color = Color(0xFF246BFD),
) {
  val w = size.width
  val h = size.height

  // Deep Midnight Blue automotive showroom gradient
  drawRect(
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFF070B16), Color(0xFF10182B), Color(0xFF0B1020))
      )
  )

  // Overhead twin volumetric spotlight beams
  val beamLeft = Path().apply {
    moveTo(w * 0.22f, 0f)
    lineTo(w * 0.34f, 0f)
    lineTo(w * 0.62f, h * 0.78f)
    lineTo(w * 0.16f, h * 0.78f)
    close()
  }
  drawPath(
    beamLeft,
    brush =
      Brush.verticalGradient(
        colors = listOf(accentColor.copy(alpha = 0.20f), Color.Transparent)
      ),
  )

  val beamRight = Path().apply {
    moveTo(w * 0.66f, 0f)
    lineTo(w * 0.78f, 0f)
    lineTo(w * 0.84f, h * 0.78f)
    lineTo(w * 0.38f, h * 0.78f)
    close()
  }
  drawPath(
    beamRight,
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFFFF8A24).copy(alpha = 0.14f), Color.Transparent)
      ),
  )

  // Subtle floor grid & neon turntable ring
  val floorY = h * 0.74f
  drawRect(
    brush = Brush.verticalGradient(listOf(Color(0xFF131D36), Color(0xFF090E1C))),
    topLeft = Offset(0f, floorY),
    size = Size(w, h - floorY),
  )
  drawLine(
    color = accentColor.copy(alpha = 0.45f),
    start = Offset(0f, floorY),
    end = Offset(w, floorY),
    strokeWidth = 2.5f,
  )

  // Glowing turntable ellipse & vehicle ground shadow
  drawOval(
    color = accentColor.copy(alpha = 0.22f),
    topLeft = Offset(w * 0.20f, floorY - h * 0.04f),
    size = Size(w * 0.60f, h * 0.12f),
    style = Stroke(width = 3f),
  )
  drawOval(
    color = Color.Black.copy(alpha = 0.65f),
    topLeft = Offset(w * 0.28f, floorY - h * 0.02f),
    size = Size(w * 0.44f, h * 0.07f),
  )

  // Floating subtle showroom dust motes
  for (i in 0 until 14) {
    val px = ((i * 173f + timeSec * (10f + (i % 3) * 5f)) % w)
    val py = ((i * 97f + sin(timeSec * 1.4f + i) * 18f) % (h * 0.72f) + h * 0.72f) % (h * 0.72f)
    drawCircle(
      color = Color(0xFFFFD447).copy(alpha = 0.28f),
      radius = 2.2f + (i % 2),
      center = Offset(px, py),
    )
  }
}

fun DrawScope.drawAnimatedMenuLandscape(
  timeSec: Float,
  selectedVehicle: VehicleSpec,
  skin: VehicleSkin?,
) {
  val w = size.width
  val h = size.height

  // Rich Midnight-to-Sunset Racing Sky
  drawRect(
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFF0B1020), Color(0xFF10182B), Color(0xFF1E3A8A), Color(0xFFFF8A24))
      )
  )

  // Dynamic Sun Halo + Volumetric Light Rays
  val sunPos = Offset(w * 0.64f, h * 0.30f)
  drawCircle(Color(0xFFFFD447).copy(alpha = 0.14f), radius = h * 0.34f, center = sunPos)
  drawCircle(Color(0xFFFF8A24).copy(alpha = 0.22f), radius = h * 0.21f, center = sunPos)
  drawCircle(Color(0xFFFFD447), radius = h * 0.10f, center = sunPos)

  // Drifting Clouds
  for (i in 0 until 5) {
    val cx = ((i * 290f + timeSec * (14f + i * 3f)) % (w + 260f)) - 130f
    val cy = h * (0.14f + (i % 3) * 0.08f)
    val col = Color.White.copy(alpha = 0.62f)
    drawCircle(col, radius = 24f, center = Offset(cx, cy))
    drawCircle(col, radius = 32f, center = Offset(cx + 26f, cy - 8f))
    drawCircle(col, radius = 22f, center = Offset(cx + 54f, cy))
  }

  // Moving Birds
  for (b in 0 until 4) {
    val bx = ((b * 180f + timeSec * 36f) % (w + 120f)) - 60f
    val by = h * 0.20f + b * 18f + sin(timeSec * 3f + b) * 6f
    val wing = sin(timeSec * 9f + b * 1.5f) * 6f
    drawLine(Color(0xFF0B1020), Offset(bx - 9f, by - wing), Offset(bx, by), strokeWidth = 2.4f)
    drawLine(Color(0xFF0B1020), Offset(bx, by), Offset(bx + 9f, by - wing), strokeWidth = 2.4f)
  }

  // Far Mountain Range
  val farMtn = Path().apply {
    moveTo(0f, h)
    var x = 0f
    while (x <= w + 30f) {
      val y = h * 0.60f - sin(x * 0.0055f) * h * 0.16f - cos(x * 0.011f) * h * 0.05f
      lineTo(x, y)
      x += 30f
    }
    lineTo(w, h)
    close()
  }
  drawPath(farMtn, color = Color(0xFF1B2740).copy(alpha = 0.85f))

  // Mid Emerald Hills
  val midHill = Path().apply {
    moveTo(0f, h)
    var x = 0f
    while (x <= w + 25f) {
      val y = h * 0.72f - sin(x * 0.008f + 0.8f) * h * 0.08f
      lineTo(x, y)
      x += 25f
    }
    lineTo(w, h)
    close()
  }
  drawPath(midHill, color = Color(0xFF14532D))

  // Distant tiny background vehicle cruising on mid ridge
  val bgCarX = ((timeSec * 42f) % (w + 120f)) - 60f
  val bgCarY = h * 0.72f - sin(bgCarX * 0.008f + 0.8f) * h * 0.08f - 8f
  drawRoundRect(
    color = Color(0xFFFFD447),
    topLeft = Offset(bgCarX - 11f, bgCarY - 7f),
    size = Size(22f, 9f),
    cornerRadius = CornerRadius(3f, 3f),
  )
  drawCircle(Color(0xFF0B1020), radius = 3.5f, center = Offset(bgCarX - 6f, bgCarY + 2f))
  drawCircle(Color(0xFF0B1020), radius = 3.5f, center = Offset(bgCarX + 6f, bgCarY + 2f))

  // Foreground Rolling Road
  fun roadY(px: Float): Float = h * 0.82f - sin((px + timeSec * 72f) * 0.011f) * h * 0.055f

  val fgPath = Path().apply {
    moveTo(0f, h)
    var x = 0f
    while (x <= w + 16f) {
      lineTo(x, roadY(x))
      x += 16f
    }
    lineTo(w, h)
    close()
  }
  drawPath(fgPath, color = Color(0xFF10182B))

  val topGrass = Path().apply {
    var x = 0f
    moveTo(0f, roadY(0f))
    while (x <= w + 16f) {
      lineTo(x, roadY(x))
      x += 16f
    }
  }
  drawPath(topGrass, color = Color(0xFF42D98A), style = Stroke(width = 9f))

  // Animated Trees
  for (t in 0 until 5) {
    val tx = ((t * 270f - timeSec * 72f) % (w + 180f) + (w + 180f)) % (w + 180f) - 90f
    val ty = roadY(tx)
    val sway = sin(timeSec * 2.5f + t) * 4f
    drawRect(Color(0xFF78350F), topLeft = Offset(tx - 4f, ty - 34f), size = Size(8f, 34f))
    drawCircle(Color(0xFF1FA862), radius = 20f, center = Offset(tx + sway, ty - 42f))
  }

  // Floating golden atmosphere particles
  for (p in 0 until 16) {
    val px = ((p * 149f + timeSec * 22f) % w)
    val py = ((p * 83f - timeSec * 14f) % h + h) % h
    drawCircle(
      color = Color(0xFFFFD447).copy(alpha = 0.45f),
      radius = 2.4f + (p % 2),
      center = Offset(px, py),
    )
  }

  // Large Animated Hero Vehicle centered proudly with shadow & idle suspension bounce
  val heroX = w * 0.42f
  val bounce = sin(timeSec * 4.5f) * 3.5f
  val heroRoadY = roadY(heroX)
  val slope = ((roadY(heroX + 10f) - roadY(heroX - 10f)) / 20f)
  val heroPxPerM = (h * 0.088f).coerceIn(26f, 44f)

  // Hero contact shadow
  drawOval(
    color = Color.Black.copy(alpha = 0.50f),
    topLeft = Offset(heroX - heroPxPerM * 1.4f, heroRoadY - 6f),
    size = Size(heroPxPerM * 2.8f, 14f),
  )

  drawCartoonVehicle(
    spec = selectedVehicle,
    centerPx = Offset(heroX, heroRoadY - heroPxPerM * 0.92f + bounce),
    pixelsPerMeter = heroPxPerM,
    angleRad = -slope,
    rearWheelAngleRad = timeSec * 9f,
    frontWheelAngleRad = timeSec * 9f,
    rearCompressionM = (sin(timeSec * 4.5f) * 0.06f).coerceAtLeast(0f),
    frontCompressionM = (cos(timeSec * 4.5f) * 0.06f).coerceAtLeast(0f),
    skin = skin,
  )
}
