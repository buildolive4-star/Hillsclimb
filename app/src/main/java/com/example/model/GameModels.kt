package com.example.model

enum class VehicleId {
  MINI_JEEP,
  HILL_BIKE,
  MONSTER_TRUCK,
  SPORTS_CAR,
  TRACTOR,
  SCHOOL_BUS,
  PICKUP_TRUCK,
  RALLY_CAR,
  ARMY_JEEP,
  ELECTRIC_CAR,
  SNOW_VEHICLE,
  HEAVY_TRUCK,
}

enum class VehicleCategory(val label: String) {
  ALL("ALL"),
  OFF_ROAD("OFF-ROAD"),
  SPEED("SPEED"),
  HEAVY("HEAVY"),
}

enum class VehicleVisualStyle {
  JEEP,
  BIKE,
  MONSTER,
  SPORTS,
  TRACTOR,
  BUS,
  PICKUP,
  RALLY,
  ARMY,
  ELECTRIC,
  SNOW_CAT,
  HEAVY_RIG,
}

data class VehicleSpec(
  val id: VehicleId,
  val name: String,
  val tagline: String,
  val category: VehicleCategory,
  val unlockCostCoins: Int,
  val unlockCostDiamonds: Int = 0,
  val requiredLevel: Int = 1,
  val weightKg: Float,
  val maxSpeed: Float,
  val acceleration: Float,
  val enginePower: Float,
  val suspensionStiffness: Float,
  val suspensionDamping: Float,
  val wheelRadius: Float,
  val rearWheelScale: Float = 1f,
  val wheelbase: Float,
  val chassisWidth: Float,
  val chassisHeight: Float,
  val fuelCapacity: Float,
  val stability: Float,
  val handling: Float,
  val airControlTorque: Float,
  val primaryColor: Long,
  val secondaryColor: Long,
  val style: VehicleVisualStyle,
)

enum class UpgradeType(val title: String, val shortDesc: String, val boostPerLevelLabel: String) {
  ENGINE("ENGINE", "Increases drive torque & top speed", "+9% Power & +4.5% Speed"),
  SUSPENSION("SUSPENSION", "Improves shock absorption & hill grip", "+7% Stiffness & +8% Damping"),
  TIRES("TIRES", "Boosts surface traction & climb bite", "+7.5% Grip"),
  FUEL_TANK("FUEL", "Expands fuel capacity & efficiency", "+11% Tank & -3% Burn"),
  TURBO("TURBO", "Supercharges boost thrust & recharge", "+12% Boost Thrust"),
  CHASSIS("CHASSIS", "Reinforces roll cage & impact armor", "+12% Armor"),
  BRAKES("BRAKES", "Sharpens downhill stopping & reverse", "+10% Brake Force"),
  AIR_CONTROL("AIR CONTROL", "Faster mid-air rotation & recovery", "+10% Air Torque"),
}

enum class MapId {
  INDIAN_VILLAGE,
  GREEN_HILLS,
  DESERT,
  SNOW_MOUNTAINS,
  FOREST,
  CITY,
  VOLCANO,
  JUNGLE,
  BEACH,
  SPACE,
  INDUSTRIAL_ZONE,
  MOUNTAIN_PASS,
}

enum class MapDifficulty(val label: String, val colorHex: Long) {
  EASY("EASY", 0xFF42D98A),
  MEDIUM("MEDIUM", 0xFFFFD447),
  HARD("HARD", 0xFFFF8A24),
  EXTREME("EXTREME", 0xFFF05252),
}

enum class WeatherEffect {
  NONE,
  DUST_STORM,
  SNOWFALL,
  ASH_EMBERS,
  RAIN_MIST,
  STARS,
}

data class MapSpec(
  val id: MapId,
  val name: String,
  val subtitle: String,
  val difficulty: MapDifficulty,
  val unlockCostCoins: Int,
  val unlockCostDiamonds: Int = 0,
  val gravity: Float,
  val friction: Float,
  val hillAmplitude: Float,
  val bumpiness: Float,
  val fuelSpacingMeters: Float,
  val skyTopColor: Long,
  val skyBottomColor: Long,
  val farHillColor: Long,
  val midHillColor: Long,
  val groundTopColor: Long,
  val groundBodyColor: Long,
  val weather: WeatherEffect,
  val hasBridges: Boolean,
  val physicsTags: List<String>,
)

enum class MissionPeriod(val label: String) {
  DAILY("DAILY MISSIONS"),
  WEEKLY("WEEKLY MISSIONS"),
  ACHIEVEMENT("ACHIEVEMENTS"),
}

enum class MissionMetric {
  SINGLE_RUN_DISTANCE,
  TOTAL_DISTANCE,
  TOTAL_COINS,
  TOTAL_BACKFLIPS,
  TOTAL_FRONTFLIPS,
  UNLOCKED_VEHICLES,
  FUEL_CANS_COLLECTED,
  NO_CRASH_DISTANCE,
  DESERT_DISTANCE,
  MAX_ENGINE_LEVEL,
  TOTAL_RACES,
}

data class MissionSpec(
  val id: String,
  val title: String,
  val description: String,
  val period: MissionPeriod,
  val metric: MissionMetric,
  val target: Int,
  val rewardCoins: Int,
  val rewardDiamonds: Int,
  val rewardXp: Int,
)

data class DailyRewardSpec(
  val day: Int,
  val title: String,
  val coins: Int,
  val diamonds: Int,
  val isSpecial: Boolean = false,
)

enum class GraphicsQuality(val label: String) {
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High"),
}

enum class AppLanguage(val label: String, val code: String) {
  ENGLISH("English", "en"),
  HINDI("हिन्दी (Hindi)", "hi"),
}

enum class BoosterType(val title: String, val desc: String, val costCoins: Int) {
  FUEL_BOOST("Reserve Fuel Pack", "+35% starting fuel tank for 1 race", 180),
  NITRO_PACK("Full Nitro Cell", "Instant 100% faster Turbo recharge for 1 race", 220),
  MAGNET_COINS("Coin Magnet", "Attracts nearby coins automatically for 1 race", 250),
}

enum class ShopCategory(val label: String) {
  COINS("COINS"),
  DIAMONDS("DIAMONDS"),
  SKINS("VEHICLE SKINS"),
  VEHICLES("SPECIAL VEHICLES"),
  BOOSTERS("BOOSTERS"),
}

data class VehicleUpgradeLevels(val levels: Map<UpgradeType, Int> = emptyMap()) {
  fun levelOf(type: UpgradeType): Int = (levels[type] ?: 1).coerceIn(1, 10)
  fun averageLevel(): Int =
    if (levels.isEmpty()) 1 else (levels.values.sum() + (8 - levels.size)) / 8
}

data class GameSettings(
  val musicEnabled: Boolean = true,
  val soundEnabled: Boolean = true,
  val musicVolume: Float = 0.75f,
  val soundVolume: Float = 0.85f,
  val controlSensitivity: Float = 1.0f,
  val vibrationEnabled: Boolean = true,
  val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val autoBalanceAssist: Boolean = true,
)

data class VehicleSkin(
  val id: String,
  val name: String,
  val primaryColor: Long,
  val secondaryColor: Long,
  val costDiamonds: Int,
)

data class SketchfabModelSummary(
  val uid: String,
  val name: String,
  val authorName: String,
  val authorProfileUrl: String,
  val licenseLabel: String,
  val licenseSlug: String,
  val thumbnailUrl: String,
  val faceCount: Int,
  val vertexCount: Int,
  val likeCount: Int,
  val viewCount: Int,
)

data class Downloaded3DModel(
  val uid: String,
  val name: String,
  val authorName: String,
  val licenseLabel: String,
  val thumbnailUrl: String,
  val entryFilePath: String,
  val localDirPath: String,
  val sizeBytes: Long,
  val downloadedAtEpochMs: Long,
)

data class PlayerSaveData(
  val saveVersion: Int = 2,
  val playerName: String = "Racer Veer",
  val avatarId: Int = 0,
  val coins: Int = 500,
  val diamonds: Int = 15,
  val xp: Int = 0,
  val selectedVehicleId: VehicleId = VehicleId.MINI_JEEP,
  val selectedMapId: MapId = MapId.INDIAN_VILLAGE,
  val unlockedVehicles: Set<VehicleId> = setOf(VehicleId.MINI_JEEP),
  val unlockedMaps: Set<MapId> = setOf(MapId.INDIAN_VILLAGE),
  val vehicleUpgrades: Map<VehicleId, VehicleUpgradeLevels> = emptyMap(),
  val bestDistanceByMap: Map<MapId, Int> = emptyMap(),
  val bestScoreByMap: Map<MapId, Int> = emptyMap(),
  val missionProgress: Map<String, Int> = emptyMap(),
  val claimedMissions: Set<String> = emptySet(),
  val lastDailyClaimEpochDay: Long = -1L,
  val dailyStreakDay: Int = 0,
  val lastDailyResetEpochDay: Long = -1L,
  val ownedSkins: Set<String> = setOf("default"),
  val equippedSkinByVehicle: Map<VehicleId, String> = emptyMap(),
  val boosterInventory: Map<BoosterType, Int> = emptyMap(),
  val downloaded3dModels: Map<String, Downloaded3DModel> = emptyMap(),
  val equipped3dModelByVehicle: Map<VehicleId, String> = emptyMap(),
  val prefer3dShowroom: Boolean = true,
  val adsRemoved: Boolean = false,
  val totalDistanceMeters: Int = 0,
  val totalFlips: Int = 0,
  val totalFuelCans: Int = 0,
  val totalCoinsEarned: Int = 500,
  val totalRaces: Int = 0,
  val settings: GameSettings = GameSettings(),
) {
  val playerLevel: Int
    get() = computeLevelFromXp(xp)

  fun levelProgressFraction(): Float {
    val curLvl = playerLevel
    if (curLvl >= 100) return 1f
    val startXp = xpForLevel(curLvl)
    val nextXp = xpForLevel(curLvl + 1)
    return ((xp - startXp).toFloat() / (nextXp - startXp).coerceAtLeast(1)).coerceIn(0f, 1f)
  }

  fun upgradeLevel(vehicleId: VehicleId, type: UpgradeType): Int =
    vehicleUpgrades[vehicleId]?.levelOf(type) ?: 1

  fun vehicleAvgLevel(vehicleId: VehicleId): Int =
    vehicleUpgrades[vehicleId]?.averageLevel() ?: 1

  fun boosterCount(type: BoosterType): Int = boosterInventory[type] ?: 0

  fun equipped3dModelFor(vehicleId: VehicleId): Downloaded3DModel? =
    equipped3dModelByVehicle[vehicleId]?.let { downloaded3dModels[it] }

  companion object {
    fun xpForLevel(level: Int): Int {
      val clamped = level.coerceIn(1, 100)
      return if (clamped <= 1) 0 else ((clamped - 1) * (clamped - 1) * 45)
    }

    fun computeLevelFromXp(xp: Int): Int {
      var lvl = 1
      while (lvl < 100 && xp >= xpForLevel(lvl + 1)) {
        lvl++
      }
      return lvl
    }
  }
}
