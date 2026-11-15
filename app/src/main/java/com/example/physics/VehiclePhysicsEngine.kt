package com.example.physics

import com.example.model.MapSpec
import com.example.model.UpgradeType
import com.example.model.VehicleSpec
import com.example.model.VehicleUpgradeLevels
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float,
  val maxLife: Float,
  val radius: Float,
  val colorHex: Long,
)

data class FloatingText(
  val text: String,
  var x: Float,
  var y: Float,
  var life: Float,
  val maxLife: Float,
  val colorHex: Long,
)

enum class GameOverReason {
  NONE,
  CRASHED,
  OUT_OF_FUEL,
}

data class PhysicsEvents(
  val coinsDelta: Int = 0,
  val diamondsDelta: Int = 0,
  val fuelCollected: Boolean = false,
  val stuntTriggered: Boolean = false,
  val backflipsDelta: Int = 0,
  val frontflipsDelta: Int = 0,
  val jumped: Boolean = false,
  val landedIntensity: Float = 0f,
  val crashedNow: Boolean = false,
)

class VehiclePhysicsEngine(
  val vehicle: VehicleSpec,
  val map: MapSpec,
  upgrades: VehicleUpgradeLevels,
  private val autoBalanceAssist: Boolean,
  private val controlSensitivity: Float = 1.0f,
  private val fuelBoostActive: Boolean = false,
  private val nitroBoostActive: Boolean = false,
  private val magnetBoostActive: Boolean = false,
) {
  private val engineLvl = upgrades.levelOf(UpgradeType.ENGINE)
  private val suspLvl = upgrades.levelOf(UpgradeType.SUSPENSION)
  private val tireLvl = upgrades.levelOf(UpgradeType.TIRES)
  private val fuelLvl = upgrades.levelOf(UpgradeType.FUEL_TANK)
  private val chassisLvl = upgrades.levelOf(UpgradeType.CHASSIS)
  private val turboLvl = upgrades.levelOf(UpgradeType.TURBO)
  private val brakeLvl = upgrades.levelOf(UpgradeType.BRAKES)
  private val airLvl = upgrades.levelOf(UpgradeType.AIR_CONTROL)

  // Effective upgraded specs
  private val effMass = vehicle.weightKg
  private val effMaxSpeed = vehicle.maxSpeed * (1f + (engineLvl - 1) * 0.045f)
  private val effEnginePower = vehicle.enginePower * (1f + (engineLvl - 1) * 0.09f)
  private val effSuspK = vehicle.suspensionStiffness * (1f + (suspLvl - 1) * 0.07f)
  private val effSuspC = vehicle.suspensionDamping * (1f + (suspLvl - 1) * 0.08f)
  private val effTireGrip = 1f + (tireLvl - 1) * 0.075f
  val maxFuel =
    vehicle.fuelCapacity * (1f + (fuelLvl - 1) * 0.11f) * (if (fuelBoostActive) 1.35f else 1f)
  private val fuelBurnEfficiency = 1f - (fuelLvl - 1) * 0.032f
  private val effChassisArmor = 1f + (chassisLvl - 1) * 0.12f
  private val effTurboBoost = 1f + (turboLvl - 1) * 0.12f
  private val effBrakePower = 1f + (brakeLvl - 1) * 0.10f
  private val effAirTorque = vehicle.airControlTorque * (1f + (airLvl - 1) * 0.10f) * controlSensitivity

  val terrain = TerrainGenerator(map)

  // Rigid-body chassis state (world meters, Y-up)
  var posX: Float = 0f
  var posY: Float = 1.25f
  var velX: Float = 0f
  var velY: Float = 0f
  var angle: Float = 0f // radians, +CCW (nose up)
  var angularVel: Float = 0f

  // Wheel suspension & rotation state
  var rearComp: Float = 0f
  var frontComp: Float = 0f
  var rearWheelAngle: Float = 0f
  var frontWheelAngle: Float = 0f
  var rearGrounded: Boolean = false
  var frontGrounded: Boolean = false

  // Run progression & resources
  var fuel: Float = maxFuel
  var turboCharge: Float = 1f
  var maxDistanceMeters: Int = 0
  var coinsCollectedRun: Int = 0
  var diamondsCollectedRun: Int = 0
  var stuntBonusCoinsRun: Int = 0
  var fuelCansCollectedRun: Int = 0
  var stuntsCompletedRun: Int = 0
  var backflipsRun: Int = 0
  var frontflipsRun: Int = 0
  var elapsedTimeSec: Float = 0f

  val liveScore: Int
    get() = maxDistanceMeters * 10 + coinsCollectedRun * 5 + stuntsCompletedRun * 150

  var gameOverReason: GameOverReason = GameOverReason.NONE
  var isRunEnded: Boolean = false

  private var wasGrounded: Boolean = true
  private var airTime: Float = 0f
  private var airRotationAccum: Float = 0f
  private var takeoffX: Float = 0f
  private var outOfFuelCoastTimer: Float = 0f

  val particles = ArrayList<Particle>(120)
  val floatingTexts = ArrayList<FloatingText>(16)

  val speedKmh: Float
    get() = abs(velX) * 3.6f

  val rpmNormalized: Float
    get() = (abs(velX) / effMaxSpeed).coerceIn(0.12f, 1.3f)

  fun step(
    frameDeltaSec: Float,
    gasPressed: Boolean,
    brakePressed: Boolean,
    turboPressed: Boolean,
  ): PhysicsEvents {
    val dtClamped = frameDeltaSec.coerceIn(0.001f, 0.05f)
    terrain.ensureGeneratedUpTo(posX + 115f)
    terrain.pruneBehind(posX - 40f)

    if (isRunEnded) {
      updateParticlesAndTexts(dtClamped)
      return PhysicsEvents()
    }
    elapsedTimeSec += dtClamped

    val subSteps = 3
    val dt = dtClamped / subSteps

    var coinsDelta = 0
    var diamondsDelta = 0
    var fuelCollected = false
    var stuntTriggered = false
    var backflipsDelta = 0
    var frontflipsDelta = 0
    var jumped = false
    var maxLandImpact = 0f
    var crashedNow = false

    val hasFuel = fuel > 0.01f
    val activeGas = gasPressed && hasFuel
    val activeBrake = brakePressed && hasFuel
    val activeTurbo = turboPressed && hasFuel && turboCharge > 0.05f

    for (s in 0 until subSteps) {
      val prevRearGrounded = rearGrounded
      val prevFrontGrounded = frontGrounded
      val prevGrounded = prevRearGrounded || prevFrontGrounded

      val cosA = cos(angle)
      val sinA = sin(angle)

      // Anchor positions relative to center of mass
      val halfBase = vehicle.wheelbase * 0.5f
      val anchorLocalY = -vehicle.chassisHeight * 0.42f

      val rearX = posX - halfBase * cosA - anchorLocalY * sinA
      val rearY = posY - halfBase * sinA + anchorLocalY * cosA

      val frontX = posX + halfBase * cosA - anchorLocalY * sinA
      val frontY = posY + halfBase * sinA + anchorLocalY * cosA

      val rearRadius = vehicle.wheelRadius * vehicle.rearWheelScale
      val frontRadius = vehicle.wheelRadius
      val suspRest = 0.34f

      // Gravity
      var fx = 0f
      var fy = -map.gravity * 1.15f // slight arcade weight feel
      var torque = 0f

      // Wheel ground contacts
      val rearGroundY = terrain.heightAt(rearX)
      val frontGroundY = terrain.heightAt(frontX)

      val rearDistFromGround = (rearY - suspRest) - (rearGroundY + rearRadius)
      val frontDistFromGround = (frontY - suspRest) - (frontGroundY + frontRadius)

      rearGrounded = rearDistFromGround <= 0f
      frontGrounded = frontDistFromGround <= 0f

      val inertia = (effMass * (vehicle.chassisWidth * vehicle.chassisWidth + vehicle.chassisHeight * vehicle.chassisHeight)) / 12f

      fun applyWheelPhysics(
        isRear: Boolean,
        wx: Float,
        wy: Float,
        distFromGround: Float,
        grounded: Boolean,
        radius: Float,
      ): Float {
        if (!grounded) return 0f
        val compression = (-distFromGround).coerceIn(0f, 0.52f)
        val relX = wx - posX
        val relY = wy - posY
        val pointVx = velX - angularVel * relY
        val pointVy = velY + angularVel * relX

        val slope = terrain.slopeAngleAt(wx)
        val nx = -sin(slope)
        val ny = cos(slope)
        val tx = cos(slope)
        val ty = sin(slope)

        val velAlongNormal = pointVx * nx + pointVy * ny
        val springForce = ((effSuspK / (effMass / 120f)) * compression - (effSuspC / (effMass / 120f)) * velAlongNormal)
          .coerceAtLeast(0f)

        fx += nx * springForce
        fy += ny * springForce
        torque += (relX * (ny * springForce) - relY * (nx * springForce)) * 0.48f

        // Tangential drive & braking forces
        val isBridge = terrain.isBridgeAt(wx)
        val surfaceGrip = (map.friction + if (isBridge) 0.12f else 0f) * effTireGrip
        val velAlongTangent = pointVx * tx + pointVy * ty

        var driveAccel = 0f
        if (activeGas) {
          val speedFactor = (1f - (velAlongTangent / effMaxSpeed).coerceIn(0f, 0.92f))
          val turboMul = if (activeTurbo) 1.55f * effTurboBoost else 1f
          driveAccel = (effEnginePower * 0.72f) * speedFactor * turboMul * surfaceGrip
        } else if (activeBrake) {
          if (velAlongTangent > 0.6f) {
            driveAccel = -22f * effBrakePower * surfaceGrip
          } else {
            // Reverse
            val revFactor = (1f - (-velAlongTangent / (effMaxSpeed * 0.48f)).coerceIn(0f, 0.92f))
            driveAccel = -(effEnginePower * 0.45f) * revFactor * surfaceGrip
          }
        } else {
          // Gentle rolling drag
          driveAccel = -velAlongTangent * (0.45f + (1f - map.friction).coerceAtLeast(0f) * 0.4f)
        }

        // Extra uphill torque boost for high-power vehicles
        val tractionWeight = if (isRear) 0.62f else 0.48f
        val tanForce = driveAccel * tractionWeight
        fx += tx * tanForce
        fy += ty * tanForce

        // Wheelie pitch torque when flooring throttle on ground
        if (activeGas && isRear) {
          torque += (effEnginePower * 0.085f) * (1f - vehicle.stability * 0.45f)
        }

        // Update wheel angular rotation
        val omega = velAlongTangent / radius
        if (isRear) {
          rearWheelAngle += omega * dt
          rearComp = compression
        } else {
          frontWheelAngle += omega * dt
          frontComp = compression
        }
        return compression
      }

      if (!rearGrounded) rearComp = (rearComp - dt * 2.5f).coerceAtLeast(0f)
      if (!frontGrounded) frontComp = (frontComp - dt * 2.5f).coerceAtLeast(0f)

      applyWheelPhysics(true, rearX, rearY, rearDistFromGround, rearGrounded, rearRadius)
      applyWheelPhysics(false, frontX, frontY, frontDistFromGround, frontGrounded, frontRadius)

      // Free-spin wheel animation when airborne
      if (!rearGrounded) {
        val spin = if (activeGas) 28f else if (activeBrake) -16f else velX / rearRadius
        rearWheelAngle += spin * dt
      }
      if (!frontGrounded) {
        val spin = if (activeGas) 18f else if (activeBrake) -14f else velX / frontRadius
        frontWheelAngle += spin * dt
      }

      val nowGrounded = rearGrounded || frontGrounded

      // Air control & flip tracking
      if (!nowGrounded) {
        if (prevGrounded) {
          takeoffX = posX
          airTime = 0f
          airRotationAccum = 0f
          if (velY > 2.2f) jumped = true
        }
        airTime += dt
        val prevA = angle
        if (gasPressed) {
          angularVel += effAirTorque * 0.36f * dt
        } else if (brakePressed) {
          angularVel -= effAirTorque * 0.36f * dt
        } else if (autoBalanceAssist) {
          // Gentle stability assist toward terrain slope when no input held
          val targetAngle = terrain.slopeAngleAt(posX)
          val diff = wrapAngle(targetAngle - angle)
          angularVel += diff * 4.5f * dt
        }
        val dA = angularVel * dt
        angle += dA
        airRotationAccum += (angle - prevA)
      } else {
        // Just landed!
        if (!prevGrounded && airTime > 0.32f) {
          val impactSpeed = abs(velY)
          maxLandImpact = maxOf(maxLandImpact, (impactSpeed / 12f).coerceIn(0f, 1.5f))
          spawnDustBurst((rearX + frontX) * 0.5f, terrain.heightAt(posX), 10)

          // Evaluate completed stunts!
          val totalFlipsAbs = (abs(airRotationAccum) / (2f * PI.toFloat())).toInt()
          if (totalFlipsAbs >= 1) {
            val isBackflip = airRotationAccum > 0f
            if (isBackflip) backflipsDelta += totalFlipsAbs else frontflipsDelta += totalFlipsAbs
            stuntsCompletedRun += totalFlipsAbs
            val bonus = totalFlipsAbs * 50
            stuntBonusCoinsRun += bonus
            coinsDelta += bonus
            stuntTriggered = true
            val label =
              when {
                totalFlipsAbs >= 2 && isBackflip -> "${totalFlipsAbs}x BACKFLIP! +$bonus"
                totalFlipsAbs >= 2 && !isBackflip -> "${totalFlipsAbs}x FRONT FLIP! +$bonus"
                isBackflip -> "BACKFLIP! +$bonus"
                else -> "FRONT FLIP! +$bonus"
              }
            floatingTexts.add(FloatingText(label, posX, posY + 1.8f, 1.6f, 1.6f, 0xFFFFC107))
          } else if (airTime > 1.15f) {
            val airBonus = 30
            stuntsCompletedRun++
            stuntBonusCoinsRun += airBonus
            coinsDelta += airBonus
            stuntTriggered = true
            floatingTexts.add(FloatingText("BIG AIR! +$airBonus", posX, posY + 1.7f, 1.4f, 1.4f, 0xFF38BDF8))
          }

          val slopeDiff = abs(wrapAngle(angle - terrain.slopeAngleAt(posX)))
          if (airTime > 0.65f && slopeDiff < 0.28f) {
            val perfBonus = 25
            stuntsCompletedRun++
            stuntBonusCoinsRun += perfBonus
            coinsDelta += perfBonus
            stuntTriggered = true
            floatingTexts.add(
              FloatingText("PERFECT LANDING! +$perfBonus", posX, posY + 2.25f, 1.4f, 1.4f, 0xFF4ADE80)
            )
          } else {
            val jumpDist = posX - takeoffX
            if (jumpDist > 14f) {
              val distBonus = 20
              stuntsCompletedRun++
              stuntBonusCoinsRun += distBonus
              coinsDelta += distBonus
              stuntTriggered = true
              floatingTexts.add(FloatingText("LONG JUMP! +$distBonus", posX, posY + 2.1f, 1.3f, 1.3f, 0xFFFACC15))
            }
          }
        }
        airTime = 0f
        airRotationAccum = 0f
      }

      // Integrate velocities & positions
      velX += fx * dt
      velY += fy * dt

      // Air drag + speed ceiling
      velX *= (1f - 0.08f * dt)
      val maxAllowed = effMaxSpeed * (if (activeTurbo) 1.32f else 1.05f)
      if (velX > maxAllowed) velX -= (velX - maxAllowed) * 3.5f * dt
      if (velX < -effMaxSpeed * 0.5f) velX = -effMaxSpeed * 0.5f

      val angularDrag = if (nowGrounded) 4.2f else 1.6f
      angularVel += (torque / (inertia / effMass.coerceAtLeast(1f) * 1.4f)) * dt
      angularVel *= (1f - angularDrag * dt).coerceIn(0.8f, 1f)
      angularVel = angularVel.coerceIn(-8.5f, 8.5f)

      posX += velX * dt
      posY += velY * dt
      if (nowGrounded) angle += angularVel * dt

      // Prevent chassis bottom from tunneling deeply through ground
      val bodyBottomY = posY - vehicle.chassisHeight * 0.35f
      val centerGround = terrain.heightAt(posX)
      if (bodyBottomY < centerGround) {
        posY = centerGround + vehicle.chassisHeight * 0.35f
        if (velY < 0f) velY = -velY * 0.15f
      }

      // Prevent reversing past start wall
      if (posX < -8f) {
        posX = -8f
        if (velX < 0f) velX = 0f
      }

      // Driver Head / Roof Crash Detection
      val headLocalX = -0.15f
      val headLocalY = vehicle.chassisHeight * 0.72f
      val headWorldX = posX + headLocalX * cos(angle) - headLocalY * sin(angle)
      val headWorldY = posY + headLocalX * sin(angle) + headLocalY * cos(angle)
      val headGroundY = terrain.heightAt(headWorldX)
      val wrapped = abs(wrapAngle(angle))

      if (headWorldY <= headGroundY + 0.12f && wrapped > (1.68f + (chassisLvl - 1) * 0.03f)) {
        isRunEnded = true
        gameOverReason = GameOverReason.CRASHED
        crashedNow = true
        velX *= 0.2f
        velY = 0f
        spawnSparkBurst(headWorldX, headGroundY, 22)
        floatingTexts.add(FloatingText("CRASHED!", posX, posY + 1.5f, 2f, 2f, 0xFFEF4444))
        break
      }

      // Fuel Consumption & Turbo management
      if (hasFuel) {
        val baseIdleDrain = 0.65f
        val throttleDrain = if (activeGas) 1.85f else if (activeBrake) 0.9f else 0f
        val turboDrain = if (activeTurbo) 0.9f else 0f
        fuel = (fuel - (baseIdleDrain + throttleDrain + turboDrain) * fuelBurnEfficiency * dt).coerceAtLeast(0f)
      } else {
        // Coast down when out of fuel
        outOfFuelCoastTimer += dt
        if (abs(velX) < 0.35f || outOfFuelCoastTimer > 3.5f) {
          isRunEnded = true
          gameOverReason = GameOverReason.OUT_OF_FUEL
          floatingTexts.add(FloatingText("OUT OF FUEL!", posX, posY + 1.5f, 2f, 2f, 0xFFEF4444))
          break
        }
      }

      if (activeTurbo) {
        turboCharge = (turboCharge - (if (nitroBoostActive) 0.22f else 0.38f) * dt).coerceAtLeast(0f)
      } else {
        val rechargeRate = (0.14f + turboLvl * 0.015f) * (if (nitroBoostActive) 2.0f else 1f)
        turboCharge = (turboCharge + rechargeRate * dt).coerceAtMost(1f)
      }

      // Tire dust / turbo flame particles
      if (nowGrounded && abs(velX) > 3f && Random.nextFloat() < 0.35f) {
        particles.add(
          Particle(
            x = rearX - 0.25f,
            y = rearGroundY + 0.1f,
            vx = -velX * 0.25f + (Random.nextFloat() - 0.5f) * 1.5f,
            vy = 0.8f + Random.nextFloat() * 1.6f,
            life = 0.42f,
            maxLife = 0.42f,
            radius = 0.12f + Random.nextFloat() * 0.12f,
            colorHex = map.groundTopColor,
          )
        )
      }
      if (activeTurbo && Random.nextFloat() < 0.7f) {
        particles.add(
          Particle(
            x = posX - cosA * (vehicle.chassisWidth * 0.48f),
            y = posY - sinA * (vehicle.chassisWidth * 0.48f),
            vx = -cosA * 7f + velX * 0.2f,
            vy = -sinA * 7f + (Random.nextFloat() - 0.5f),
            life = 0.22f,
            maxLife = 0.22f,
            radius = 0.16f,
            colorHex = 0xFFFF6B1A,
          )
        )
      }
    }

    // Check Pickup Collisions once per frame (with optional Coin Magnet radius)
    val baseRadius = vehicle.chassisWidth * 0.56f + 0.55f
    val pickupRadiusSq = baseRadius * baseRadius
    val magnetRadiusSq = (baseRadius + 3.4f) * (baseRadius + 3.4f)
    for (p in terrain.activePickups()) {
      if (p.collected) continue
      val dx = p.x - posX
      val dy = p.y - posY
      val distSq = dx * dx + dy * dy
      val useMagnet = magnetBoostActive && p.kind != PickupKind.FUEL
      if (distSq <= (if (useMagnet) magnetRadiusSq else pickupRadiusSq)) {
        p.collected = true
        when (p.kind) {
          PickupKind.COIN -> {
            coinsCollectedRun += p.value
            coinsDelta += p.value
            spawnCoinSparkle(p.x, p.y, 0xFFFFC107)
          }
          PickupKind.BONUS_COIN -> {
            coinsCollectedRun += p.value
            coinsDelta += p.value
            spawnCoinSparkle(p.x, p.y, 0xFFFF9800)
            floatingTexts.add(FloatingText("+${p.value}", p.x, p.y + 0.7f, 0.9f, 0.9f, 0xFFFFC107))
          }
          PickupKind.DIAMOND -> {
            diamondsCollectedRun += p.value
            diamondsDelta += p.value
            spawnCoinSparkle(p.x, p.y, 0xFF22D3EE)
            floatingTexts.add(FloatingText("+1 GEM!", p.x, p.y + 0.8f, 1.2f, 1.2f, 0xFF22D3EE))
          }
          PickupKind.FUEL -> {
            fuel = maxFuel
            outOfFuelCoastTimer = 0f
            fuelCansCollectedRun++
            fuelCollected = true
            spawnCoinSparkle(p.x, p.y, 0xFF22C55E)
            floatingTexts.add(FloatingText("FUEL REFILLED!", p.x, p.y + 1.0f, 1.3f, 1.3f, 0xFF4ADE80))
          }
        }
      }
    }

    val currentDist = posX.toInt().coerceAtLeast(0)
    if (currentDist > maxDistanceMeters) {
      maxDistanceMeters = currentDist
    }

    updateParticlesAndTexts(dtClamped)

    return PhysicsEvents(
      coinsDelta = coinsDelta,
      diamondsDelta = diamondsDelta,
      fuelCollected = fuelCollected,
      stuntTriggered = stuntTriggered,
      backflipsDelta = backflipsDelta,
      frontflipsDelta = frontflipsDelta,
      jumped = jumped,
      landedIntensity = maxLandImpact,
      crashedNow = crashedNow,
    )
  }

  private fun updateParticlesAndTexts(dt: Float) {
    val pIter = particles.iterator()
    while (pIter.hasNext()) {
      val pt = pIter.next()
      pt.x += pt.vx * dt
      pt.y += pt.vy * dt
      pt.vy -= 4.5f * dt
      pt.life -= dt
      if (pt.life <= 0f) pIter.remove()
    }
    val tIter = floatingTexts.iterator()
    while (tIter.hasNext()) {
      val ft = tIter.next()
      ft.y += 1.4f * dt
      ft.life -= dt
      if (ft.life <= 0f) tIter.remove()
    }
  }

  private fun spawnDustBurst(cx: Float, cy: Float, count: Int) {
    repeat(count) {
      particles.add(
        Particle(
          x = cx + (Random.nextFloat() - 0.5f) * 1.4f,
          y = cy + 0.1f,
          vx = (Random.nextFloat() - 0.5f) * 4.5f,
          vy = 1.0f + Random.nextFloat() * 3.0f,
          life = 0.5f,
          maxLife = 0.5f,
          radius = 0.16f + Random.nextFloat() * 0.14f,
          colorHex = map.groundTopColor,
        )
      )
    }
  }

  private fun spawnSparkBurst(cx: Float, cy: Float, count: Int) {
    repeat(count) {
      particles.add(
        Particle(
          x = cx,
          y = cy + 0.2f,
          vx = (Random.nextFloat() - 0.5f) * 9f,
          vy = 2f + Random.nextFloat() * 6f,
          life = 0.65f,
          maxLife = 0.65f,
          radius = 0.12f,
          colorHex = if (it % 2 == 0) 0xFFFF6B1A else 0xFFFACC15,
        )
      )
    }
  }

  private fun spawnCoinSparkle(cx: Float, cy: Float, colorHex: Long) {
    repeat(6) {
      val ang = (it / 6f) * 2f * PI.toFloat()
      particles.add(
        Particle(
          x = cx,
          y = cy,
          vx = cos(ang) * 3.2f,
          vy = sin(ang) * 3.2f,
          life = 0.35f,
          maxLife = 0.35f,
          radius = 0.11f,
          colorHex = colorHex,
        )
      )
    }
  }

  private fun wrapAngle(a: Float): Float {
    var r = a % (2f * PI.toFloat())
    if (r > PI.toFloat()) r -= 2f * PI.toFloat()
    if (r < -PI.toFloat()) r += 2f * PI.toFloat()
    return r
  }
}
