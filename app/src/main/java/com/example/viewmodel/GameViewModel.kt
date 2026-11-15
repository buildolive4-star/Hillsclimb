package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.GameCatalog
import com.example.data.SaveRepository
import com.example.data.SketchfabRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SketchfabUiState(
  val isSearching: Boolean = false,
  val query: String = "offroad jeep 4x4",
  val results: List<SketchfabModelSummary> = emptyList(),
  val nextCursor: String? = null,
  val searchError: String? = null,
  val downloadingUid: String? = null,
  val downloadProgress: Float = 0f,
  val downloadError: String? = null,
  val hasValidApiKey: Boolean = false,
)

data class RunSummary(
  val distanceMeters: Int,
  val bestDistanceBefore: Int,
  val isNewRecord: Boolean,
  val score: Int,
  val timeSurvivedSec: Int,
  val coinsEarned: Int,
  val diamondsEarned: Int,
  val stuntsCompleted: Int,
  val stuntBonusCoins: Int,
  val xpEarned: Int,
  val leveledUp: Boolean,
  val newLevel: Int,
  val reason: String,
  val vehicleName: String,
  val mapName: String,
  val doubledAvailable: Boolean = true,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val saveRepo = SaveRepository(application)
  private val sketchfabRepo = SketchfabRepository(application)
  val soundEngine = SoundEngine(application)

  private val _saveState = MutableStateFlow(PlayerSaveData())
  val saveState: StateFlow<PlayerSaveData> = _saveState.asStateFlow()

  private val _isLoaded = MutableStateFlow(false)
  val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

  private val _sketchfabState =
    MutableStateFlow(SketchfabUiState(hasValidApiKey = sketchfabRepo.hasValidApiKey))
  val sketchfabState: StateFlow<SketchfabUiState> = _sketchfabState.asStateFlow()

  init {
    viewModelScope.launch {
      val loaded = saveRepo.loadOnce()
      val normalized = checkDailyMissionReset(loaded)
      _saveState.value = normalized
      soundEngine.updateSettings(
        music = normalized.settings.musicEnabled,
        sound = normalized.settings.soundEnabled,
        vibration = normalized.settings.vibrationEnabled,
        musicVol = normalized.settings.musicVolume,
        soundVol = normalized.settings.soundVolume,
      )
      _isLoaded.value = true
      if (normalized != loaded) {
        saveRepo.save(normalized)
      }
    }
  }

  private fun currentEpochDay(): Long = System.currentTimeMillis() / 86_400_000L

  private fun checkDailyMissionReset(data: PlayerSaveData): PlayerSaveData {
    val today = currentEpochDay()
    if (data.lastDailyResetEpochDay == today) return data
    val dailyIds =
      GameCatalog.missions.filter { it.period == MissionPeriod.DAILY }.map { it.id }.toSet()
    val updatedProgress = data.missionProgress.toMutableMap()
    dailyIds.forEach { updatedProgress[it] = 0 }
    val updatedClaimed = data.claimedMissions - dailyIds
    return data.copy(
      missionProgress = updatedProgress,
      claimedMissions = updatedClaimed,
      lastDailyResetEpochDay = today,
    )
  }

  private fun persistUpdate(transform: (PlayerSaveData) -> PlayerSaveData) {
    _saveState.update { current ->
      val next = transform(current)
      viewModelScope.launch { saveRepo.save(next) }
      next
    }
  }

  fun selectVehicle(vehicleId: VehicleId) {
    if (vehicleId !in _saveState.value.unlockedVehicles) return
    soundEngine.playClick()
    persistUpdate { it.copy(selectedVehicleId = vehicleId) }
  }

  fun unlockVehicle(vehicleId: VehicleId): Boolean {
    val spec = GameCatalog.vehicleById(vehicleId)
    val current = _saveState.value
    if (vehicleId in current.unlockedVehicles) {
      selectVehicle(vehicleId)
      return true
    }
    if (current.coins < spec.unlockCostCoins || current.diamonds < spec.unlockCostDiamonds) {
      return false
    }
    soundEngine.playUpgrade()
    persistUpdate { data ->
      val unlocked = data.unlockedVehicles + vehicleId
      val prog = data.missionProgress.toMutableMap()
      prog["perm_unlock_vehicle"] = unlocked.size
      data.copy(
        coins = data.coins - spec.unlockCostCoins,
        diamonds = data.diamonds - spec.unlockCostDiamonds,
        unlockedVehicles = unlocked,
        selectedVehicleId = vehicleId,
        missionProgress = prog,
      )
    }
    return true
  }

  fun upgradeVehiclePart(vehicleId: VehicleId, type: UpgradeType): Boolean {
    val current = _saveState.value
    val curLvl = current.upgradeLevel(vehicleId, type)
    if (curLvl >= 10) return false
    val cost = GameCatalog.upgradeCostCoins(type, curLvl)
    if (current.coins < cost) return false

    soundEngine.playUpgrade()
    val newLvl = curLvl + 1
    persistUpdate { data ->
      val vehUpgrades = data.vehicleUpgrades.toMutableMap()
      val existing = vehUpgrades[vehicleId]?.levels?.toMutableMap() ?: mutableMapOf()
      existing[type] = newLvl
      vehUpgrades[vehicleId] = VehicleUpgradeLevels(existing)

      val prog = data.missionProgress.toMutableMap()
      if (type == UpgradeType.ENGINE) {
        val maxEngine = maxOf(prog["perm_engine_lv5"] ?: 1, newLvl)
        prog["perm_engine_lv5"] = maxEngine
      }
      data.copy(
        coins = data.coins - cost,
        vehicleUpgrades = vehUpgrades,
        missionProgress = prog,
      )
    }
    return true
  }

  fun selectMap(mapId: MapId) {
    if (mapId !in _saveState.value.unlockedMaps) return
    soundEngine.playClick()
    persistUpdate { it.copy(selectedMapId = mapId) }
  }

  fun unlockMap(mapId: MapId): Boolean {
    val spec = GameCatalog.mapById(mapId)
    val current = _saveState.value
    if (mapId in current.unlockedMaps) {
      selectMap(mapId)
      return true
    }
    if (current.coins < spec.unlockCostCoins || current.diamonds < spec.unlockCostDiamonds) {
      return false
    }
    soundEngine.playUpgrade()
    persistUpdate { data ->
      data.copy(
        coins = data.coins - spec.unlockCostCoins,
        diamonds = data.diamonds - spec.unlockCostDiamonds,
        unlockedMaps = data.unlockedMaps + mapId,
        selectedMapId = mapId,
      )
    }
    return true
  }

  fun consumeBoostersForRun(): Triple<Boolean, Boolean, Boolean> {
    val cur = _saveState.value.boosterInventory
    val hasFuel = (cur[BoosterType.FUEL_BOOST] ?: 0) > 0
    val hasNitro = (cur[BoosterType.NITRO_PACK] ?: 0) > 0
    val hasMagnet = (cur[BoosterType.MAGNET_COINS] ?: 0) > 0
    if (hasFuel || hasNitro || hasMagnet) {
      persistUpdate { data ->
        val nextInv = data.boosterInventory.toMutableMap()
        if (hasFuel) nextInv[BoosterType.FUEL_BOOST] = ((nextInv[BoosterType.FUEL_BOOST] ?: 1) - 1).coerceAtLeast(0)
        if (hasNitro) nextInv[BoosterType.NITRO_PACK] = ((nextInv[BoosterType.NITRO_PACK] ?: 1) - 1).coerceAtLeast(0)
        if (hasMagnet) nextInv[BoosterType.MAGNET_COINS] = ((nextInv[BoosterType.MAGNET_COINS] ?: 1) - 1).coerceAtLeast(0)
        data.copy(boosterInventory = nextInv)
      }
    }
    return Triple(hasFuel, hasNitro, hasMagnet)
  }

  fun recordRunCompleted(
    mapId: MapId,
    vehicleId: VehicleId,
    distanceMeters: Int,
    score: Int,
    timeSurvivedSec: Int,
    coinsEarned: Int,
    diamondsEarned: Int,
    stuntsCompleted: Int,
    stuntBonusCoins: Int,
    backflips: Int,
    frontflips: Int,
    fuelCansCollected: Int,
    didCrash: Boolean,
    reasonText: String,
  ): RunSummary {
    val before = _saveState.value
    val prevBest = before.bestDistanceByMap[mapId] ?: 0
    val isNewRecord = distanceMeters > prevBest
    val xpGained = (distanceMeters / 4) + stuntsCompleted * 25 + (coinsEarned / 8)
    val oldLevel = before.playerLevel
    val newLevel = PlayerSaveData.computeLevelFromXp(before.xp + xpGained)

    persistUpdate { data ->
      val bestMap = data.bestDistanceByMap.toMutableMap()
      bestMap[mapId] = maxOf(bestMap[mapId] ?: 0, distanceMeters)

      val bestScoreMap = data.bestScoreByMap.toMutableMap()
      bestScoreMap[mapId] = maxOf(bestScoreMap[mapId] ?: 0, score)

      val prog = data.missionProgress.toMutableMap()
      prog["daily_500m"] = maxOf(prog["daily_500m"] ?: 0, distanceMeters)
      prog["daily_200_coins"] = (prog["daily_200_coins"] ?: 0) + coinsEarned
      prog["daily_backflip"] = (prog["daily_backflip"] ?: 0) + backflips
      prog["daily_5_fuel"] = (prog["daily_5_fuel"] ?: 0) + fuelCansCollected
      prog["weekly_1000m"] = maxOf(prog["weekly_1000m"] ?: 0, distanceMeters)
      prog["weekly_10_races"] = (prog["weekly_10_races"] ?: 0) + 1
      if (mapId == MapId.DESERT) {
        prog["weekly_desert_300"] = maxOf(prog["weekly_desert_300"] ?: 0, distanceMeters)
      }
      prog["perm_2km"] = data.totalDistanceMeters + distanceMeters
      prog["perm_unlock_vehicle"] = data.unlockedVehicles.size
      if (!didCrash) {
        prog["perm_no_crash"] = maxOf(prog["perm_no_crash"] ?: 0, distanceMeters)
      }
      prog["perm_1000_coins"] = data.totalCoinsEarned + coinsEarned

      data.copy(
        coins = data.coins + coinsEarned,
        diamonds = data.diamonds + diamondsEarned,
        xp = data.xp + xpGained,
        bestDistanceByMap = bestMap,
        bestScoreByMap = bestScoreMap,
        missionProgress = prog,
        totalDistanceMeters = data.totalDistanceMeters + distanceMeters,
        totalFlips = data.totalFlips + backflips + frontflips,
        totalFuelCans = data.totalFuelCans + fuelCansCollected,
        totalCoinsEarned = data.totalCoinsEarned + coinsEarned,
        totalRaces = data.totalRaces + 1,
      )
    }

    return RunSummary(
      distanceMeters = distanceMeters,
      bestDistanceBefore = prevBest,
      isNewRecord = isNewRecord,
      score = score,
      timeSurvivedSec = timeSurvivedSec,
      coinsEarned = coinsEarned,
      diamondsEarned = diamondsEarned,
      stuntsCompleted = stuntsCompleted,
      stuntBonusCoins = stuntBonusCoins,
      xpEarned = xpGained,
      leveledUp = newLevel > oldLevel,
      newLevel = newLevel,
      reason = reasonText,
      vehicleName = GameCatalog.vehicleById(vehicleId).name,
      mapName = GameCatalog.mapById(mapId).name,
    )
  }

  fun doubleRunRewards(coinsToAdd: Int) {
    if (coinsToAdd <= 0) return
    soundEngine.playCoin()
    persistUpdate { data ->
      data.copy(
        coins = data.coins + coinsToAdd,
        totalCoinsEarned = data.totalCoinsEarned + coinsToAdd,
      )
    }
  }

  fun claimMission(missionId: String): Boolean {
    val mission = GameCatalog.missions.firstOrNull { it.id == missionId } ?: return false
    val current = _saveState.value
    if (missionId in current.claimedMissions) return false
    val prog = current.missionProgress[missionId] ?: 0
    if (prog < mission.target) return false

    soundEngine.playStunt()
    persistUpdate { data ->
      data.copy(
        coins = data.coins + mission.rewardCoins,
        diamonds = data.diamonds + mission.rewardDiamonds,
        xp = data.xp + mission.rewardXp,
        claimedMissions = data.claimedMissions + missionId,
        totalCoinsEarned = data.totalCoinsEarned + mission.rewardCoins,
      )
    }
    return true
  }

  fun canClaimDailyReward(): Boolean {
    val today = currentEpochDay()
    return _saveState.value.lastDailyClaimEpochDay < today
  }

  fun claimDailyReward(): DailyRewardSpec? {
    val today = currentEpochDay()
    val current = _saveState.value
    if (current.lastDailyClaimEpochDay >= today) return null

    val dayIndex =
      if (current.lastDailyClaimEpochDay == today - 1L) {
        (current.dailyStreakDay % 7)
      } else if (current.lastDailyClaimEpochDay < 0L) {
        0
      } else {
        (current.dailyStreakDay % 7)
      }
    val reward = GameCatalog.dailyRewards[dayIndex]
    soundEngine.playStunt()
    persistUpdate { data ->
      data.copy(
        coins = data.coins + reward.coins,
        diamonds = data.diamonds + reward.diamonds,
        lastDailyClaimEpochDay = today,
        dailyStreakDay = dayIndex + 1,
        totalCoinsEarned = data.totalCoinsEarned + reward.coins,
      )
    }
    return reward
  }

  fun buyOrEquipSkin(vehicleId: VehicleId, skinId: String): Boolean {
    val skin = GameCatalog.skins.firstOrNull { it.id == skinId } ?: return false
    val current = _saveState.value
    if (skinId in current.ownedSkins) {
      soundEngine.playClick()
      persistUpdate { data ->
        data.copy(equippedSkinByVehicle = data.equippedSkinByVehicle + (vehicleId to skinId))
      }
      return true
    }
    if (current.diamonds < skin.costDiamonds) return false
    soundEngine.playUpgrade()
    persistUpdate { data ->
      data.copy(
        diamonds = data.diamonds - skin.costDiamonds,
        ownedSkins = data.ownedSkins + skinId,
        equippedSkinByVehicle = data.equippedSkinByVehicle + (vehicleId to skinId),
      )
    }
    return true
  }

  fun buyBooster(booster: BoosterType): Boolean {
    val current = _saveState.value
    if (current.coins < booster.costCoins) return false
    soundEngine.playUpgrade()
    persistUpdate { data ->
      val next = data.boosterInventory.toMutableMap()
      next[booster] = (next[booster] ?: 0) + 1
      data.copy(coins = data.coins - booster.costCoins, boosterInventory = next)
    }
    return true
  }

  fun exchangeDiamondsForCoins(diamondsCost: Int, coinsGain: Int): Boolean {
    val current = _saveState.value
    if (current.diamonds < diamondsCost) return false
    soundEngine.playCoin()
    persistUpdate { data ->
      data.copy(
        diamonds = data.diamonds - diamondsCost,
        coins = data.coins + coinsGain,
        totalCoinsEarned = data.totalCoinsEarned + coinsGain,
      )
    }
    return true
  }

  fun exchangeCoinsForDiamonds(coinsCost: Int, diamondsGain: Int): Boolean {
    val current = _saveState.value
    if (current.coins < coinsCost) return false
    soundEngine.playStunt()
    persistUpdate { data ->
      data.copy(coins = data.coins - coinsCost, diamonds = data.diamonds + diamondsGain)
    }
    return true
  }

  fun watchOptionalRewardAdForCoins() {
    soundEngine.playCoin()
    persistUpdate { data ->
      data.copy(coins = data.coins + 250, totalCoinsEarned = data.totalCoinsEarned + 250)
    }
  }

  fun updatePlayerProfile(name: String, avatarId: Int) {
    val cleaned = name.trim().take(18).ifEmpty { "Racer Veer" }
    soundEngine.playClick()
    persistUpdate { it.copy(playerName = cleaned, avatarId = avatarId.coerceIn(0, 5)) }
  }

  fun updateSettings(transform: (GameSettings) -> GameSettings) {
    persistUpdate { data ->
      val updated = transform(data.settings)
      soundEngine.updateSettings(
        music = updated.musicEnabled,
        sound = updated.soundEnabled,
        vibration = updated.vibrationEnabled,
        musicVol = updated.musicVolume,
        soundVol = updated.soundVolume,
      )
      data.copy(settings = updated)
    }
  }

  fun searchSketchfabModels(query: String, appendNextPage: Boolean = false) {
    val cleanQuery = query.ifBlank { "offroad car" }
    val cursor = if (appendNextPage) _sketchfabState.value.nextCursor else null
    if (appendNextPage && cursor == null) return

    _sketchfabState.update {
      it.copy(
        isSearching = true,
        query = cleanQuery,
        searchError = null,
        hasValidApiKey = sketchfabRepo.hasValidApiKey,
      )
    }
    viewModelScope.launch {
      val result = sketchfabRepo.searchFreeModels(query = cleanQuery, cursor = cursor)
      result.fold(
        onSuccess = { (items, next) ->
          _sketchfabState.update { cur ->
            val combined = if (appendNextPage) (cur.results + items).distinctBy { it.uid } else items
            cur.copy(isSearching = false, results = combined, nextCursor = next, searchError = null)
          }
        },
        onFailure = { err ->
          _sketchfabState.update {
            it.copy(
              isSearching = false,
              searchError = err.message ?: "Unable to connect to Sketchfab.",
            )
          }
        },
      )
    }
  }

  fun downloadAndEquipSketchfabModel(
    summary: SketchfabModelSummary,
    equipToVehicleId: VehicleId?,
  ) {
    if (_sketchfabState.value.downloadingUid != null) return
    _sketchfabState.update {
      it.copy(downloadingUid = summary.uid, downloadProgress = 0.02f, downloadError = null)
    }
    viewModelScope.launch {
      val res =
        sketchfabRepo.downloadAndExtractModel(summary) { prog ->
          _sketchfabState.update { it.copy(downloadProgress = prog) }
        }
      res.fold(
        onSuccess = { downloaded ->
          soundEngine.playUpgrade()
          persistUpdate { data ->
            val updatedModels = data.downloaded3dModels + (downloaded.uid to downloaded)
            val updatedEq =
              if (equipToVehicleId != null) data.equipped3dModelByVehicle + (equipToVehicleId to downloaded.uid)
              else data.equipped3dModelByVehicle
            data.copy(
              downloaded3dModels = updatedModels,
              equipped3dModelByVehicle = updatedEq,
              prefer3dShowroom = true,
            )
          }
          _sketchfabState.update {
            it.copy(downloadingUid = null, downloadProgress = 1f, downloadError = null)
          }
        },
        onFailure = { err ->
          _sketchfabState.update {
            it.copy(
              downloadingUid = null,
              downloadProgress = 0f,
              downloadError = err.message ?: "Model download failed.",
            )
          }
        },
      )
    }
  }

  fun equip3dModelToVehicle(vehicleId: VehicleId, uid: String?) {
    soundEngine.playClick()
    persistUpdate { data ->
      val next = data.equipped3dModelByVehicle.toMutableMap()
      if (uid == null) next.remove(vehicleId) else if (uid in data.downloaded3dModels) next[vehicleId] = uid
      data.copy(equipped3dModelByVehicle = next, prefer3dShowroom = true)
    }
  }

  fun deleteDownloaded3dModel(uid: String) {
    val existing = _saveState.value.downloaded3dModels[uid] ?: return
    sketchfabRepo.deleteLocalFiles(existing)
    soundEngine.playClick()
    persistUpdate { data ->
      val nextModels = data.downloaded3dModels - uid
      val nextEq = data.equipped3dModelByVehicle.filterValues { it != uid }
      data.copy(downloaded3dModels = nextModels, equipped3dModelByVehicle = nextEq)
    }
  }

  fun setPrefer3dShowroom(prefer3d: Boolean) {
    soundEngine.playClick()
    persistUpdate { it.copy(prefer3dShowroom = prefer3d) }
  }

  fun clearSketchfabDownloadError() {
    _sketchfabState.update { it.copy(downloadError = null) }
  }

  fun resetAllProgress() {
    soundEngine.playClick()
    _saveState.value.downloaded3dModels.values.forEach { sketchfabRepo.deleteLocalFiles(it) }
    viewModelScope.launch {
      val fresh = saveRepo.resetProgress()
      _saveState.value = fresh
      soundEngine.updateSettings(
        music = fresh.settings.musicEnabled,
        sound = fresh.settings.soundEnabled,
        vibration = fresh.settings.vibrationEnabled,
        musicVol = fresh.settings.musicVolume,
        soundVol = fresh.settings.soundVolume,
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    soundEngine.release()
  }
}
