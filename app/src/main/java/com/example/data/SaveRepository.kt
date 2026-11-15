package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.saveDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "hill_rush_india_save")

class SaveRepository(private val context: Context) {

  private val saveKey = stringPreferencesKey("player_save_json_v1")

  val saveFlow: Flow<PlayerSaveData> =
    context.saveDataStore.data.map { prefs ->
      val raw = prefs[saveKey]
      if (raw.isNullOrBlank()) PlayerSaveData() else decodeSave(raw)
    }

  suspend fun loadOnce(): PlayerSaveData = saveFlow.first()

  suspend fun save(data: PlayerSaveData) {
    val json = encodeSave(data)
    context.saveDataStore.edit { prefs -> prefs[saveKey] = json }
  }

  suspend fun resetProgress(): PlayerSaveData {
    val fresh = PlayerSaveData()
    save(fresh)
    return fresh
  }

  private fun encodeSave(data: PlayerSaveData): String {
    val root = JSONObject()
    root.put("saveVersion", data.saveVersion)
    root.put("playerName", data.playerName)
    root.put("avatarId", data.avatarId)
    root.put("coins", data.coins)
    root.put("diamonds", data.diamonds)
    root.put("xp", data.xp)
    root.put("selectedVehicleId", data.selectedVehicleId.name)
    root.put("selectedMapId", data.selectedMapId.name)

    root.put("unlockedVehicles", JSONArray(data.unlockedVehicles.map { it.name }))
    root.put("unlockedMaps", JSONArray(data.unlockedMaps.map { it.name }))

    val upgradesObj = JSONObject()
    data.vehicleUpgrades.forEach { (vehId, levels) ->
      val mapObj = JSONObject()
      levels.levels.forEach { (type, lvl) -> mapObj.put(type.name, lvl) }
      upgradesObj.put(vehId.name, mapObj)
    }
    root.put("vehicleUpgrades", upgradesObj)

    val bestObj = JSONObject()
    data.bestDistanceByMap.forEach { (mapId, dist) -> bestObj.put(mapId.name, dist) }
    root.put("bestDistanceByMap", bestObj)

    val scoreObj = JSONObject()
    data.bestScoreByMap.forEach { (mapId, sc) -> scoreObj.put(mapId.name, sc) }
    root.put("bestScoreByMap", scoreObj)

    val missionProgObj = JSONObject()
    data.missionProgress.forEach { (id, prog) -> missionProgObj.put(id, prog) }
    root.put("missionProgress", missionProgObj)
    root.put("claimedMissions", JSONArray(data.claimedMissions.toList()))

    root.put("lastDailyClaimEpochDay", data.lastDailyClaimEpochDay)
    root.put("dailyStreakDay", data.dailyStreakDay)
    root.put("lastDailyResetEpochDay", data.lastDailyResetEpochDay)
    root.put("ownedSkins", JSONArray(data.ownedSkins.toList()))

    val skinObj = JSONObject()
    data.equippedSkinByVehicle.forEach { (vehId, skinId) -> skinObj.put(vehId.name, skinId) }
    root.put("equippedSkinByVehicle", skinObj)

    val boosterObj = JSONObject()
    data.boosterInventory.forEach { (b, count) -> boosterObj.put(b.name, count) }
    root.put("boosterInventory", boosterObj)

    val modelsObj = JSONObject()
    data.downloaded3dModels.forEach { (uid, m) ->
      val mObj = JSONObject()
      mObj.put("uid", m.uid)
      mObj.put("name", m.name)
      mObj.put("authorName", m.authorName)
      mObj.put("licenseLabel", m.licenseLabel)
      mObj.put("thumbnailUrl", m.thumbnailUrl)
      mObj.put("entryFilePath", m.entryFilePath)
      mObj.put("localDirPath", m.localDirPath)
      mObj.put("sizeBytes", m.sizeBytes)
      mObj.put("downloadedAtEpochMs", m.downloadedAtEpochMs)
      modelsObj.put(uid, mObj)
    }
    root.put("downloaded3dModels", modelsObj)

    val eq3dObj = JSONObject()
    data.equipped3dModelByVehicle.forEach { (vehId, uid) -> eq3dObj.put(vehId.name, uid) }
    root.put("equipped3dModelByVehicle", eq3dObj)
    root.put("prefer3dShowroom", data.prefer3dShowroom)

    root.put("adsRemoved", data.adsRemoved)
    root.put("totalDistanceMeters", data.totalDistanceMeters)
    root.put("totalFlips", data.totalFlips)
    root.put("totalFuelCans", data.totalFuelCans)
    root.put("totalCoinsEarned", data.totalCoinsEarned)
    root.put("totalRaces", data.totalRaces)

    val settingsObj = JSONObject()
    settingsObj.put("musicEnabled", data.settings.musicEnabled)
    settingsObj.put("soundEnabled", data.settings.soundEnabled)
    settingsObj.put("musicVolume", data.settings.musicVolume.toDouble())
    settingsObj.put("soundVolume", data.settings.soundVolume.toDouble())
    settingsObj.put("controlSensitivity", data.settings.controlSensitivity.toDouble())
    settingsObj.put("vibrationEnabled", data.settings.vibrationEnabled)
    settingsObj.put("graphicsQuality", data.settings.graphicsQuality.name)
    settingsObj.put("language", data.settings.language.name)
    settingsObj.put("autoBalanceAssist", data.settings.autoBalanceAssist)
    root.put("settings", settingsObj)

    return root.toString()
  }

  private fun decodeSave(raw: String): PlayerSaveData {
    return try {
      val root = JSONObject(raw)
      val version = root.optInt("saveVersion", 2)

      val unlockedVeh = mutableSetOf<VehicleId>()
      val vehArr = root.optJSONArray("unlockedVehicles")
      if (vehArr != null) {
        for (i in 0 until vehArr.length()) {
          runCatching { VehicleId.valueOf(vehArr.getString(i)) }.getOrNull()?.let { unlockedVeh.add(it) }
        }
      }
      if (unlockedVeh.isEmpty()) unlockedVeh.add(VehicleId.MINI_JEEP)

      val unlockedMaps = mutableSetOf<MapId>()
      val mapArr = root.optJSONArray("unlockedMaps")
      if (mapArr != null) {
        for (i in 0 until mapArr.length()) {
          runCatching { MapId.valueOf(mapArr.getString(i)) }.getOrNull()?.let { unlockedMaps.add(it) }
        }
      }
      if (unlockedMaps.isEmpty()) unlockedMaps.add(MapId.INDIAN_VILLAGE)

      val upgrades = mutableMapOf<VehicleId, VehicleUpgradeLevels>()
      val upObj = root.optJSONObject("vehicleUpgrades")
      upObj?.keys()?.forEach { vehKey ->
        val vehId = runCatching { VehicleId.valueOf(vehKey) }.getOrNull() ?: return@forEach
        val inner = upObj.optJSONObject(vehKey) ?: return@forEach
        val map = mutableMapOf<UpgradeType, Int>()
        inner.keys().forEach { typeKey ->
          val uType = runCatching { UpgradeType.valueOf(typeKey) }.getOrNull() ?: return@forEach
          map[uType] = inner.optInt(typeKey, 1).coerceIn(1, 10)
        }
        upgrades[vehId] = VehicleUpgradeLevels(map)
      }

      val bestDist = mutableMapOf<MapId, Int>()
      root.optJSONObject("bestDistanceByMap")?.let { obj ->
        obj.keys().forEach { k ->
          runCatching { MapId.valueOf(k) }.getOrNull()?.let { mId -> bestDist[mId] = obj.optInt(k, 0) }
        }
      }

      val bestScore = mutableMapOf<MapId, Int>()
      root.optJSONObject("bestScoreByMap")?.let { obj ->
        obj.keys().forEach { k ->
          runCatching { MapId.valueOf(k) }.getOrNull()?.let { mId -> bestScore[mId] = obj.optInt(k, 0) }
        }
      }

      val missionProg = mutableMapOf<String, Int>()
      root.optJSONObject("missionProgress")?.let { obj ->
        obj.keys().forEach { key -> missionProg[key] = obj.optInt(key, 0) }
      }

      val claimed = mutableSetOf<String>()
      val cArr = root.optJSONArray("claimedMissions")
      if (cArr != null) {
        for (i in 0 until cArr.length()) claimed.add(cArr.getString(i))
      }

      val ownedSkins = mutableSetOf("default")
      val osArr = root.optJSONArray("ownedSkins")
      if (osArr != null) {
        for (i in 0 until osArr.length()) ownedSkins.add(osArr.getString(i))
      }

      val eqSkins = mutableMapOf<VehicleId, String>()
      root.optJSONObject("equippedSkinByVehicle")?.let { eqObj ->
        eqObj.keys().forEach { key ->
          val vId = runCatching { VehicleId.valueOf(key) }.getOrNull() ?: return@forEach
          eqSkins[vId] = eqObj.optString(key, "default")
        }
      }

      val boosters = mutableMapOf<BoosterType, Int>()
      root.optJSONObject("boosterInventory")?.let { bObj ->
        bObj.keys().forEach { key ->
          val bType = runCatching { BoosterType.valueOf(key) }.getOrNull() ?: return@forEach
          boosters[bType] = bObj.optInt(key, 0)
        }
      }

      val downloaded3d = mutableMapOf<String, Downloaded3DModel>()
      root.optJSONObject("downloaded3dModels")?.let { mRoot ->
        mRoot.keys().forEach { uidKey ->
          val mObj = mRoot.optJSONObject(uidKey) ?: return@forEach
          val entryPath = mObj.optString("entryFilePath", "")
          if (entryPath.isNotBlank() && java.io.File(entryPath).exists()) {
            downloaded3d[uidKey] =
              Downloaded3DModel(
                uid = mObj.optString("uid", uidKey),
                name = mObj.optString("name", "3D Vehicle"),
                authorName = mObj.optString("authorName", "Sketchfab Creator"),
                licenseLabel = mObj.optString("licenseLabel", "CC-BY"),
                thumbnailUrl = mObj.optString("thumbnailUrl", ""),
                entryFilePath = entryPath,
                localDirPath = mObj.optString("localDirPath", ""),
                sizeBytes = mObj.optLong("sizeBytes", 0L),
                downloadedAtEpochMs = mObj.optLong("downloadedAtEpochMs", 0L),
              )
          }
        }
      }

      val eq3d = mutableMapOf<VehicleId, String>()
      root.optJSONObject("equipped3dModelByVehicle")?.let { eqObj ->
        eqObj.keys().forEach { key ->
          val vId = runCatching { VehicleId.valueOf(key) }.getOrNull() ?: return@forEach
          val uid = eqObj.optString(key, "")
          if (uid in downloaded3d) eq3d[vId] = uid
        }
      }

      val sObj = root.optJSONObject("settings")
      val settings =
        if (sObj != null) {
          GameSettings(
            musicEnabled = sObj.optBoolean("musicEnabled", true),
            soundEnabled = sObj.optBoolean("soundEnabled", true),
            musicVolume = sObj.optDouble("musicVolume", 0.75).toFloat().coerceIn(0f, 1f),
            soundVolume = sObj.optDouble("soundVolume", 0.85).toFloat().coerceIn(0f, 1f),
            controlSensitivity = sObj.optDouble("controlSensitivity", 1.0).toFloat().coerceIn(0.6f, 1.5f),
            vibrationEnabled = sObj.optBoolean("vibrationEnabled", true),
            graphicsQuality =
              runCatching { GraphicsQuality.valueOf(sObj.optString("graphicsQuality", "HIGH")) }
                .getOrDefault(GraphicsQuality.HIGH),
            language =
              runCatching { AppLanguage.valueOf(sObj.optString("language", "ENGLISH")) }
                .getOrDefault(AppLanguage.ENGLISH),
            autoBalanceAssist = sObj.optBoolean("autoBalanceAssist", true),
          )
        } else {
          GameSettings()
        }

      val selectedVeh =
        runCatching { VehicleId.valueOf(root.optString("selectedVehicleId", "MINI_JEEP")) }
          .getOrDefault(VehicleId.MINI_JEEP)
          .let { if (it in unlockedVeh) it else VehicleId.MINI_JEEP }

      val selectedMap =
        runCatching { MapId.valueOf(root.optString("selectedMapId", "INDIAN_VILLAGE")) }
          .getOrDefault(MapId.INDIAN_VILLAGE)
          .let { if (it in unlockedMaps) it else MapId.INDIAN_VILLAGE }

      PlayerSaveData(
        saveVersion = version,
        playerName = root.optString("playerName", "Racer Veer"),
        avatarId = root.optInt("avatarId", 0),
        coins = root.optInt("coins", 500),
        diamonds = root.optInt("diamonds", 15),
        xp = root.optInt("xp", 0),
        selectedVehicleId = selectedVeh,
        selectedMapId = selectedMap,
        unlockedVehicles = unlockedVeh,
        unlockedMaps = unlockedMaps,
        vehicleUpgrades = upgrades,
        bestDistanceByMap = bestDist,
        bestScoreByMap = bestScore,
        missionProgress = missionProg,
        claimedMissions = claimed,
        lastDailyClaimEpochDay = root.optLong("lastDailyClaimEpochDay", -1L),
        dailyStreakDay = root.optInt("dailyStreakDay", 0),
        lastDailyResetEpochDay = root.optLong("lastDailyResetEpochDay", -1L),
        ownedSkins = ownedSkins,
        equippedSkinByVehicle = eqSkins,
        boosterInventory = boosters,
        downloaded3dModels = downloaded3d,
        equipped3dModelByVehicle = eq3d,
        prefer3dShowroom = root.optBoolean("prefer3dShowroom", true),
        adsRemoved = root.optBoolean("adsRemoved", false),
        totalDistanceMeters = root.optInt("totalDistanceMeters", 0),
        totalFlips = root.optInt("totalFlips", 0),
        totalFuelCans = root.optInt("totalFuelCans", 0),
        totalCoinsEarned = root.optInt("totalCoinsEarned", 500),
        totalRaces = root.optInt("totalRaces", 0),
        settings = settings,
      )
    } catch (_: Exception) {
      PlayerSaveData()
    }
  }
}
