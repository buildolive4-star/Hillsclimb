package com.example.physics

import com.example.model.MapSpec
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class PickupKind {
  COIN,
  BONUS_COIN,
  DIAMOND,
  FUEL,
}

data class TerrainPickup(
  val id: Int,
  val x: Float,
  val y: Float,
  val kind: PickupKind,
  val value: Int,
  var collected: Boolean = false,
)

data class TerrainProp(
  val x: Float,
  val y: Float,
  val kind: Int, // 0=tree/cactus/lamp, 1=rock/sign, 2=house/factory/flag
  val scale: Float,
)

data class BridgeSpan(val startX: Float, val endX: Float, val startY: Float, val endY: Float)

data class TerrainSample(val x: Float, val y: Float, val isBridge: Boolean = false)

class TerrainGenerator(private val map: MapSpec, seed: Int = 2026) {

  val stepMeters = 0.8f
  private val rng = Random(seed + map.id.ordinal * 997)

  private val samples = ArrayList<TerrainSample>(512)
  private val pickups = ArrayList<TerrainPickup>(256)
  private val props = ArrayList<TerrainProp>(128)
  private val bridges = ArrayList<BridgeSpan>(16)

  private var nextSampleIndex = -20
  private var nextPickupId = 1
  private var nextFuelX = 78f
  private var nextCoinClusterX = 16f
  private var nextBridgeX = 95f
  private var activeBridgeStart = -1f
  private var activeBridgeEnd = -1f
  private var bridgeStartY = 0f
  private var bridgeEndY = 0f

  private val phaseA = rng.nextFloat() * 100f
  private val phaseB = rng.nextFloat() * 100f
  private val phaseC = rng.nextFloat() * 100f

  init {
    ensureGeneratedUpTo(120f)
  }

  fun ensureGeneratedUpTo(targetX: Float) {
    val targetIndex = (targetX / stepMeters).toInt() + 10
    while (nextSampleIndex <= targetIndex) {
      val x = nextSampleIndex * stepMeters
      val sample = generateSampleAt(x)
      samples.add(sample)
      if (nextSampleIndex > 6 && nextSampleIndex % 11 == 0 && !sample.isBridge) {
        props.add(
          TerrainProp(
            x = x + rng.nextFloat() * 2f,
            y = sample.y,
            kind = rng.nextInt(3),
            scale = 0.85f + rng.nextFloat() * 0.45f,
          )
        )
      }
      nextSampleIndex++
    }

    // Spawn Fuel Cans reliably
    while (nextFuelX <= targetX) {
      val gy = heightAt(nextFuelX)
      pickups.add(
        TerrainPickup(
          id = nextPickupId++,
          x = nextFuelX,
          y = gy + 0.92f,
          kind = PickupKind.FUEL,
          value = 100,
        )
      )
      val distanceFactor = (1f + (nextFuelX / 2500f).coerceAtMost(0.35f))
      nextFuelX += map.fuelSpacingMeters * distanceFactor + rng.nextFloat() * 12f
    }

    // Spawn diverse Coin patterns (lines, hill arcs, high-jump bonus coins, rare diamonds)
    while (nextCoinClusterX <= targetX) {
      val pattern = rng.nextInt(5)
      when (pattern) {
        0 -> { // Smooth line along terrain
          val count = 4 + rng.nextInt(4)
          for (i in 0 until count) {
            val cx = nextCoinClusterX + i * 2.1f
            pickups.add(
              TerrainPickup(nextPickupId++, cx, heightAt(cx) + 0.85f, PickupKind.COIN, 5)
            )
          }
          nextCoinClusterX += count * 2.1f + 12f + rng.nextFloat() * 8f
        }
        1 -> { // High jump arc over crest
          val count = 5
          val baseX = nextCoinClusterX
          for (i in 0 until count) {
            val cx = baseX + i * 1.9f
            val arcLift = sin(PI * i / (count - 1)).toFloat() * 2.4f
            pickups.add(
              TerrainPickup(
                nextPickupId++,
                cx,
                heightAt(cx) + 1.0f + arcLift,
                if (i == count / 2) PickupKind.BONUS_COIN else PickupKind.COIN,
                if (i == count / 2) 25 else 5,
              )
            )
          }
          nextCoinClusterX += count * 1.9f + 16f + rng.nextFloat() * 10f
        }
        2 -> { // Difficult high cluster + rare diamond
          val baseX = nextCoinClusterX
          for (i in 0 until 3) {
            val cx = baseX + i * 2.0f
            pickups.add(
              TerrainPickup(
                nextPickupId++,
                cx,
                heightAt(cx) + 2.1f,
                if (i == 1 && rng.nextFloat() < 0.35f) PickupKind.DIAMOND else PickupKind.BONUS_COIN,
                if (i == 1) 1 else 25,
              )
            )
          }
          nextCoinClusterX += 18f + rng.nextFloat() * 10f
        }
        else -> { // Double staggered coins
          val count = 3 + rng.nextInt(3)
          for (i in 0 until count) {
            val cx = nextCoinClusterX + i * 2.4f
            val cy = heightAt(cx) + if (i % 2 == 0) 0.8f else 1.55f
            pickups.add(TerrainPickup(nextPickupId++, cx, cy, PickupKind.COIN, 10))
          }
          nextCoinClusterX += count * 2.4f + 13f + rng.nextFloat() * 8f
        }
      }
    }
  }

  private fun generateSampleAt(x: Float): TerrainSample {
    if (x <= 12f) {
      return TerrainSample(x, 0f, isBridge = false)
    }

    // Bridge spans on maps that feature bridges
    if (map.hasBridges && x >= nextBridgeX && activeBridgeEnd < 0f) {
      val len = 14f + rng.nextFloat() * 8f
      activeBridgeStart = x
      activeBridgeEnd = x + len
      bridgeStartY = computeRawHillHeight(x)
      bridgeEndY = computeRawHillHeight(activeBridgeEnd)
      bridges.add(BridgeSpan(x, activeBridgeEnd, bridgeStartY, bridgeEndY))
    }

    if (activeBridgeEnd > 0f) {
      if (x <= activeBridgeEnd) {
        val span = (activeBridgeEnd - activeBridgeStart).coerceAtLeast(1f)
        val t = ((x - activeBridgeStart) / span).coerceIn(0f, 1f)
        val baseLine = bridgeStartY + (bridgeEndY - bridgeStartY) * t
        val sag = -0.25f * sin(PI * t).toFloat()
        return TerrainSample(x, baseLine + sag, isBridge = true)
      } else {
        nextBridgeX = x + 135f + rng.nextFloat() * 75f
        activeBridgeStart = -1f
        activeBridgeEnd = -1f
      }
    }

    val y = computeRawHillHeight(x)
    return TerrainSample(x, y, isBridge = false)
  }

  private fun computeRawHillHeight(x: Float): Float {
    val rampIn = ((x - 12f) / 35f).coerceIn(0f, 1f)
    val difficultyScale = (1f + (x / 1800f).coerceAtMost(0.95f)) * rampIn
    val amp = map.hillAmplitude * difficultyScale

    val majorHills = sin(x * 0.048f + phaseA) * amp
    val mediumHills = sin(x * 0.135f + phaseB) * (amp * 0.46f)
    val steepSlope = cos(x * 0.022f + phaseC) * (amp * 0.72f)
    val bumps =
      sin(x * 0.42f + phaseA) * (map.bumpiness * difficultyScale) +
        sin(x * 0.88f + phaseB) * (map.bumpiness * 0.35f * difficultyScale)

    // Periodic exhilarating ramp wave
    val rampWave = sin(x * 0.075f) * cos(x * 0.031f) * (amp * 0.42f)

    return majorHills + mediumHills + steepSlope + bumps + rampWave
  }

  fun pruneBehind(minKeepX: Float) {
    while (samples.size > 40 && samples[1].x < minKeepX) {
      samples.removeAt(0)
    }
    pickups.removeAll { it.x < minKeepX || it.collected }
    props.removeAll { it.x < minKeepX }
    bridges.removeAll { it.endX < minKeepX }
  }

  fun heightAt(x: Float): Float {
    if (samples.isEmpty()) return 0f
    val firstX = samples.first().x
    if (x <= firstX) return samples.first().y
    val idxFloat = (x - firstX) / stepMeters
    val idx = idxFloat.toInt().coerceIn(0, samples.lastIndex - 1)
    val s0 = samples[idx]
    val s1 = samples[idx + 1]
    val t = ((x - s0.x) / (s1.x - s0.x).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
    return s0.y + (s1.y - s0.y) * t
  }

  fun isBridgeAt(x: Float): Boolean {
    if (samples.isEmpty()) return false
    val idx = ((x - samples.first().x) / stepMeters).toInt().coerceIn(0, samples.lastIndex)
    return samples[idx].isBridge
  }

  fun slopeAngleAt(x: Float): Float {
    val dx = 0.45f
    val h1 = heightAt(x - dx)
    val h2 = heightAt(x + dx)
    return atan2(h2 - h1, dx * 2f)
  }

  fun visibleSamples(minX: Float, maxX: Float): List<TerrainSample> =
    samples.filter { it.x in (minX - 2f)..(maxX + 2f) }

  fun activePickups(): List<TerrainPickup> = pickups

  fun activeProps(): List<TerrainProp> = props

  fun activeBridges(): List<BridgeSpan> = bridges
}
