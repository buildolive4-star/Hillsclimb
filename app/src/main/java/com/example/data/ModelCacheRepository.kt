package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.Downloaded3DModel
import com.example.model.VehicleId
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Context.modelCacheDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "hill_rush_3d_model_cache")

data class ModelCacheSnapshot(
  val modelsByUid: Map<String, Downloaded3DModel> = emptyMap(),
  val vehicleToUid: Map<VehicleId, String> = emptyMap(),
  val lastUsedMsByUid: Map<String, Long> = emptyMap(),
) {
  val totalSizeBytes: Long
    get() = modelsByUid.values.sumOf { it.sizeBytes }

  fun isModelCached(uid: String): Boolean {
    val entry = modelsByUid[uid] ?: return false
    return entry.entryFilePath.isNotBlank() && File(entry.entryFilePath).exists()
  }

  fun cachedModelForVehicle(vehicleId: VehicleId): Downloaded3DModel? {
    val uid = vehicleToUid[vehicleId] ?: return null
    val entry = modelsByUid[uid] ?: return null
    return if (entry.entryFilePath.isNotBlank() && File(entry.entryFilePath).exists()) entry else null
  }
}

/**
 * Dedicated DataStore-backed repository for caching downloaded Sketchfab 3D model
 * metadata and verifying extracted local glTF/GLB assets so models are never
 * re-downloaded across app launches.
 */
class ModelCacheRepository(private val context: Context) {

  private val cacheKey = stringPreferencesKey("cached_3d_models_v1")

  val cacheFlow: Flow<ModelCacheSnapshot> =
    context.modelCacheDataStore.data.map { prefs ->
      val raw = prefs[cacheKey]
      if (raw.isNullOrBlank()) ModelCacheSnapshot() else decodeSnapshot(raw)
    }

  suspend fun loadAndVerifyCache(): ModelCacheSnapshot =
    withContext(Dispatchers.IO) {
      val snapshot = cacheFlow.first()
      val validModels =
        snapshot.modelsByUid.filterValues { m ->
          m.entryFilePath.isNotBlank() && File(m.entryFilePath).exists()
        }
      val validMapping = snapshot.vehicleToUid.filterValues { uid -> uid in validModels }
      val validLastUsed = snapshot.lastUsedMsByUid.filterKeys { uid -> uid in validModels }

      val cleaned =
        ModelCacheSnapshot(
          modelsByUid = validModels,
          vehicleToUid = validMapping,
          lastUsedMsByUid = validLastUsed,
        )
      if (cleaned != snapshot) {
        persistSnapshot(cleaned)
      }
      cleaned
    }

  suspend fun getCachedModel(uid: String): Downloaded3DModel? =
    withContext(Dispatchers.IO) {
      val snapshot = cacheFlow.first()
      val model = snapshot.modelsByUid[uid] ?: return@withContext null
      if (File(model.entryFilePath).exists()) model else null
    }

  suspend fun getCachedModelForVehicle(vehicleId: VehicleId): Downloaded3DModel? =
    withContext(Dispatchers.IO) {
      cacheFlow.first().cachedModelForVehicle(vehicleId)
    }

  suspend fun isModelCached(uid: String): Boolean = getCachedModel(uid) != null

  suspend fun putCachedModel(
    model: Downloaded3DModel,
    assignToVehicle: VehicleId? = null,
  ): ModelCacheSnapshot =
    updateSnapshot { current ->
      val nextModels = current.modelsByUid + (model.uid to model)
      val nextMap =
        if (assignToVehicle != null) current.vehicleToUid + (assignToVehicle to model.uid)
        else current.vehicleToUid
      val nextUsed = current.lastUsedMsByUid + (model.uid to System.currentTimeMillis())
      ModelCacheSnapshot(
        modelsByUid = nextModels,
        vehicleToUid = nextMap,
        lastUsedMsByUid = nextUsed,
      )
    }

  suspend fun assignModelToVehicle(vehicleId: VehicleId, uid: String?): ModelCacheSnapshot =
    updateSnapshot { current ->
      val nextMap = current.vehicleToUid.toMutableMap()
      if (uid == null) {
        nextMap.remove(vehicleId)
      } else if (uid in current.modelsByUid) {
        nextMap[vehicleId] = uid
      }
      current.copy(vehicleToUid = nextMap)
    }

  suspend fun touchLastUsed(uid: String) {
    updateSnapshot { current ->
      if (uid !in current.modelsByUid) current
      else current.copy(lastUsedMsByUid = current.lastUsedMsByUid + (uid to System.currentTimeMillis()))
    }
  }

  suspend fun removeCachedModel(uid: String): ModelCacheSnapshot {
    val current = cacheFlow.first()
    current.modelsByUid[uid]?.let { deleteModelDirectory(it.localDirPath) }
    return updateSnapshot { snap ->
      ModelCacheSnapshot(
        modelsByUid = snap.modelsByUid - uid,
        vehicleToUid = snap.vehicleToUid.filterValues { it != uid },
        lastUsedMsByUid = snap.lastUsedMsByUid - uid,
      )
    }
  }

  suspend fun clearAllCache(): ModelCacheSnapshot {
    val current = cacheFlow.first()
    current.modelsByUid.values.forEach { deleteModelDirectory(it.localDirPath) }
    val empty = ModelCacheSnapshot()
    persistSnapshot(empty)
    return empty
  }

  suspend fun seedIfEmpty(
    models: Map<String, Downloaded3DModel>,
    mapping: Map<VehicleId, String>,
  ) {
    if (models.isEmpty()) return
    val current = cacheFlow.first()
    if (current.modelsByUid.isNotEmpty()) return
    val validModels = models.filterValues { it.entryFilePath.isNotBlank() && File(it.entryFilePath).exists() }
    val validMapping = mapping.filterValues { it in validModels }
    if (validModels.isNotEmpty()) {
      persistSnapshot(
        ModelCacheSnapshot(
          modelsByUid = validModels,
          vehicleToUid = validMapping,
          lastUsedMsByUid = validModels.mapValues { it.value.downloadedAtEpochMs },
        )
      )
    }
  }

  private suspend fun updateSnapshot(
    transform: (ModelCacheSnapshot) -> ModelCacheSnapshot
  ): ModelCacheSnapshot =
    withContext(Dispatchers.IO) {
      var updated = ModelCacheSnapshot()
      context.modelCacheDataStore.edit { prefs ->
        val raw = prefs[cacheKey]
        val cur = if (raw.isNullOrBlank()) ModelCacheSnapshot() else decodeSnapshot(raw)
        updated = transform(cur)
        prefs[cacheKey] = encodeSnapshot(updated)
      }
      updated
    }

  private suspend fun persistSnapshot(snapshot: ModelCacheSnapshot) {
    withContext(Dispatchers.IO) {
      val json = encodeSnapshot(snapshot)
      context.modelCacheDataStore.edit { prefs -> prefs[cacheKey] = json }
    }
  }

  private fun deleteModelDirectory(dirPath: String) {
    if (dirPath.isBlank()) return
    runCatching {
      val dir = File(dirPath)
      if (dir.exists()) dir.deleteRecursively()
    }
  }

  internal fun encodeSnapshot(snapshot: ModelCacheSnapshot): String {
    val root = JSONObject()
    root.put("version", 1)

    val modelsObj = JSONObject()
    snapshot.modelsByUid.forEach { (uid, m) ->
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
    root.put("models", modelsObj)

    val mappingObj = JSONObject()
    snapshot.vehicleToUid.forEach { (vehId, uid) -> mappingObj.put(vehId.name, uid) }
    root.put("vehicleToUid", mappingObj)

    val usedObj = JSONObject()
    snapshot.lastUsedMsByUid.forEach { (uid, ts) -> usedObj.put(uid, ts) }
    root.put("lastUsedMs", usedObj)

    return root.toString()
  }

  internal fun decodeSnapshot(raw: String): ModelCacheSnapshot {
    return try {
      val root = JSONObject(raw)
      val models = mutableMapOf<String, Downloaded3DModel>()
      root.optJSONObject("models")?.let { mRoot ->
        mRoot.keys().forEach { uidKey ->
          val mObj = mRoot.optJSONObject(uidKey) ?: return@forEach
          val entryPath = mObj.optString("entryFilePath", "")
          if (entryPath.isNotBlank()) {
            models[uidKey] =
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

      val mapping = mutableMapOf<VehicleId, String>()
      root.optJSONObject("vehicleToUid")?.let { eqObj ->
        eqObj.keys().forEach { key ->
          val vId = runCatching { VehicleId.valueOf(key) }.getOrNull() ?: return@forEach
          val uid = eqObj.optString(key, "")
          if (uid in models) mapping[vId] = uid
        }
      }

      val lastUsed = mutableMapOf<String, Long>()
      root.optJSONObject("lastUsedMs")?.let { uObj ->
        uObj.keys().forEach { uid ->
          if (uid in models) lastUsed[uid] = uObj.optLong(uid, 0L)
        }
      }

      ModelCacheSnapshot(modelsByUid = models, vehicleToUid = mapping, lastUsedMsByUid = lastUsed)
    } catch (_: Exception) {
      ModelCacheSnapshot()
    }
  }
}
