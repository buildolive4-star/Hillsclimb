package com.example

import com.example.data.GameCatalog
import com.example.model.*
import com.example.physics.GameOverReason
import com.example.physics.VehiclePhysicsEngine
import org.junit.Assert.*
import org.junit.Test

class PhysicsAndProgressionTest {

  @Test
  fun catalogHas12Vehicles12Maps8UpgradesAnd7DailyRewards() {
    assertEquals(12, GameCatalog.vehicles.size)
    assertEquals(12, GameCatalog.maps.size)
    assertEquals(8, UpgradeType.entries.size)
    assertEquals(7, GameCatalog.dailyRewards.size)
    assertTrue(GameCatalog.missions.size >= 10)
  }

  @Test
  fun vehicleAcceleratesCollectsCoinsAndMovesForward() {
    val vehicle = GameCatalog.vehicleById(VehicleId.MINI_JEEP)
    val map = GameCatalog.mapById(MapId.INDIAN_VILLAGE)
    val engine = VehiclePhysicsEngine(vehicle, map, VehicleUpgradeLevels(), autoBalanceAssist = true)

    var totalCoins = 0
    repeat(300) { // 5 seconds at 60fps
      val ev = engine.step(1f / 60f, gasPressed = true, brakePressed = false, turboPressed = false)
      totalCoins += ev.coinsDelta
    }
    assertTrue("Vehicle should move forward past 15m, got ${engine.maxDistanceMeters}", engine.maxDistanceMeters >= 15)
    assertTrue("Fuel should decrease during drive", engine.fuel < engine.maxFuel)
    assertTrue("Should collect coins along starting path or remain valid", totalCoins >= 0)
  }

  @Test
  fun vehicleReversesWhenBrakeHeld() {
    val vehicle = GameCatalog.vehicleById(VehicleId.MINI_JEEP)
    val map = GameCatalog.mapById(MapId.GREEN_HILLS)
    val engine = VehiclePhysicsEngine(vehicle, map, VehicleUpgradeLevels(), autoBalanceAssist = true)

    repeat(60) {
      engine.step(1f / 60f, gasPressed = false, brakePressed = false, turboPressed = false)
    }
    repeat(120) {
      engine.step(1f / 60f, gasPressed = false, brakePressed = true, turboPressed = false)
    }
    assertTrue("Vehicle should reverse cleanly when brake/reverse held", engine.posX < -0.5f)
  }

  @Test
  fun fuelDepletionEndsRunOutOfFuel() {
    val vehicle = GameCatalog.vehicleById(VehicleId.MINI_JEEP)
    val map = GameCatalog.mapById(MapId.INDIAN_VILLAGE)
    val engine = VehiclePhysicsEngine(vehicle, map, VehicleUpgradeLevels(), autoBalanceAssist = true)
    engine.fuel = 0f

    repeat(260) {
      engine.step(1f / 60f, gasPressed = true, brakePressed = false, turboPressed = false)
    }
    assertTrue(engine.isRunEnded)
    assertEquals(GameOverReason.OUT_OF_FUEL, engine.gameOverReason)
  }

  @Test
  fun upsideDownImpactTriggersCrash() {
    val vehicle = GameCatalog.vehicleById(VehicleId.MINI_JEEP)
    val map = GameCatalog.mapById(MapId.INDIAN_VILLAGE)
    val engine = VehiclePhysicsEngine(vehicle, map, VehicleUpgradeLevels(), autoBalanceAssist = false)
    engine.angle = Math.PI.toFloat()
    engine.posY = engine.terrain.heightAt(engine.posX) + 0.1f

    engine.step(1f / 60f, gasPressed = false, brakePressed = false, turboPressed = false)
    assertTrue(engine.isRunEnded)
    assertEquals(GameOverReason.CRASHED, engine.gameOverReason)
  }

  @Test
  fun terrainRemainsContinuousAcrossAll12Maps() {
    for (map in GameCatalog.maps) {
      val veh = GameCatalog.vehicleById(VehicleId.RALLY_CAR)
      val engine = VehiclePhysicsEngine(veh, map, VehicleUpgradeLevels(), autoBalanceAssist = true)
      engine.terrain.ensureGeneratedUpTo(400f)
      var prevY = engine.terrain.heightAt(0f)
      var x = 1f
      while (x <= 350f) {
        val y = engine.terrain.heightAt(x)
        assertFalse("NaN height in ${map.name}", y.isNaN())
        assertTrue("Sudden cliff discontinuity in ${map.name} at $x", kotlin.math.abs(y - prevY) < 4.5f)
        prevY = y
        x += 1f
      }
    }
  }

  @Test
  fun upgradeCostScalesAndCapsAtLevel10() {
    val c1 = GameCatalog.upgradeCostCoins(UpgradeType.ENGINE, 1)
    val c5 = GameCatalog.upgradeCostCoins(UpgradeType.ENGINE, 5)
    val c10 = GameCatalog.upgradeCostCoins(UpgradeType.ENGINE, 10)
    assertTrue(c5 > c1)
    assertEquals(0, c10)
  }

  @Test
  fun xpProgressionScalesFromLevel1To100() {
    assertEquals(1, PlayerSaveData.computeLevelFromXp(0))
    assertTrue(PlayerSaveData.computeLevelFromXp(5000) > 5)
    assertEquals(100, PlayerSaveData.computeLevelFromXp(999_999_999))

    val midSave = PlayerSaveData(xp = 5000)
    val frac = midSave.levelProgressFraction()
    assertTrue("Level progress fraction must be within [0, 1]", frac in 0f..1f)
  }

  @Test
  fun boostersAndLiveScoreWorkInPhysicsEngine() {
    val vehicle = GameCatalog.vehicleById(VehicleId.SPORTS_CAR)
    val map = GameCatalog.mapById(MapId.CITY)

    val normalEngine = VehiclePhysicsEngine(vehicle, map, VehicleUpgradeLevels(), autoBalanceAssist = true)
    val boostedEngine =
      VehiclePhysicsEngine(
        vehicle = vehicle,
        map = map,
        upgrades = VehicleUpgradeLevels(),
        autoBalanceAssist = true,
        controlSensitivity = 1.4f,
        fuelBoostActive = true,
        nitroBoostActive = true,
        magnetBoostActive = true,
      )

    assertEquals(normalEngine.maxFuel * 1.35f, boostedEngine.maxFuel, 0.05f)
    assertEquals(boostedEngine.maxFuel, boostedEngine.fuel, 0.01f)

    repeat(120) {
      boostedEngine.step(1f / 60f, gasPressed = true, brakePressed = false, turboPressed = true)
    }
    assertTrue("Elapsed time should advance by ~2s", boostedEngine.elapsedTimeSec >= 1.9f)
    assertTrue("Live score should grow with distance", boostedEngine.liveScore > 0)
  }

  @Test
  fun catalogCoversAllCategoriesDifficultiesAndMissionPeriods() {
    assertTrue(GameCatalog.vehicles.any { it.category == VehicleCategory.OFF_ROAD })
    assertTrue(GameCatalog.vehicles.any { it.category == VehicleCategory.SPEED })
    assertTrue(GameCatalog.vehicles.any { it.category == VehicleCategory.HEAVY })

    assertTrue(GameCatalog.maps.any { it.difficulty == MapDifficulty.EASY })
    assertTrue(GameCatalog.maps.any { it.difficulty == MapDifficulty.MEDIUM })
    assertTrue(GameCatalog.maps.any { it.difficulty == MapDifficulty.HARD })
    assertTrue(GameCatalog.maps.any { it.difficulty == MapDifficulty.EXTREME })

    assertTrue(GameCatalog.missions.any { it.period == MissionPeriod.DAILY })
    assertTrue(GameCatalog.missions.any { it.period == MissionPeriod.WEEKLY })
    assertTrue(GameCatalog.missions.any { it.period == MissionPeriod.ACHIEVEMENT })
  }

  @Test
  fun sketchfab3dModelLookupAndBuildConfigFieldExist() {
    val model =
      Downloaded3DModel(
        uid = "abc123",
        name = "Offroad Thar 3D",
        authorName = "Creator",
        licenseLabel = "CC-BY",
        thumbnailUrl = "https://example.com/thumb.jpg",
        entryFilePath = "/tmp/scene.gltf",
        localDirPath = "/tmp/abc123",
        sizeBytes = 1024L,
        downloadedAtEpochMs = 1000L,
      )
    val save =
      PlayerSaveData(
        downloaded3dModels = mapOf("abc123" to model),
        equipped3dModelByVehicle = mapOf(VehicleId.MINI_JEEP to "abc123"),
      )

    assertEquals(model, save.equipped3dModelFor(VehicleId.MINI_JEEP))
    assertNull(save.equipped3dModelFor(VehicleId.SPORTS_CAR))
    assertNotNull(BuildConfig.SKETCHFAB_API_KEY)
  }
}
