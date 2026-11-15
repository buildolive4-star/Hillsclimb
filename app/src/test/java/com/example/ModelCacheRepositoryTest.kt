package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ModelCacheRepository
import com.example.data.SketchfabRepository
import com.example.model.Downloaded3DModel
import com.example.model.VehicleId
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ModelCacheRepositoryTest {

  @Test
  fun cachesAndVerifiesDownloaded3dModelsInDataStoreWithoutReDownloading() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ModelCacheRepository(context)
    repo.clearAllCache()

    val modelDir = File(context.filesDir, "test_sf_jeep").apply { mkdirs() }
    val gltfFile = File(modelDir, "scene.gltf").apply { writeText("{}") }

    val model =
      Downloaded3DModel(
        uid = "uid_jeep_01",
        name = "Offroad 4x4 Jeep",
        authorName = "SketchfabArtist",
        licenseLabel = "CC-BY",
        thumbnailUrl = "https://example.com/jeep.jpg",
        entryFilePath = gltfFile.absolutePath,
        localDirPath = modelDir.absolutePath,
        sizeBytes = 2048L,
        downloadedAtEpochMs = 123456L,
      )

    repo.putCachedModel(model, assignToVehicle = VehicleId.MINI_JEEP)

    assertTrue("Model should be cached in DataStore & on disk", repo.isModelCached("uid_jeep_01"))
    assertEquals(model, repo.getCachedModel("uid_jeep_01"))
    assertEquals(model, repo.getCachedModelForVehicle(VehicleId.MINI_JEEP))

    val verified = repo.loadAndVerifyCache()
    assertEquals(1, verified.modelsByUid.size)
    assertEquals("uid_jeep_01", verified.vehicleToUid[VehicleId.MINI_JEEP])

    // If local extracted file is deleted, verifyAndLoadCache should prune stale entry
    gltfFile.delete()
    assertFalse("Missing disk file must not report cached", repo.isModelCached("uid_jeep_01"))
    val afterPrune = repo.loadAndVerifyCache()
    assertTrue(afterPrune.modelsByUid.isEmpty())
    assertTrue(afterPrune.vehicleToUid.isEmpty())

    modelDir.deleteRecursively()
  }

  @Test
  fun removeAndClearCacheDeletesFilesAndDataStoreEntries() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ModelCacheRepository(context)
    repo.clearAllCache()

    val modelDir = File(context.filesDir, "test_sf_monster").apply { mkdirs() }
    val glbFile = File(modelDir, "model.glb").apply { writeText("glb") }

    val model =
      Downloaded3DModel(
        uid = "uid_monster_01",
        name = "Monster Truck",
        authorName = "Artist",
        licenseLabel = "CC0",
        thumbnailUrl = "",
        entryFilePath = glbFile.absolutePath,
        localDirPath = modelDir.absolutePath,
        sizeBytes = 4096L,
        downloadedAtEpochMs = 200L,
      )

    repo.putCachedModel(model, assignToVehicle = VehicleId.MONSTER_TRUCK)
    assertTrue(modelDir.exists())

    repo.removeCachedModel("uid_monster_01")
    assertFalse("Directory should be deleted on removal", modelDir.exists())
    assertNull(repo.getCachedModel("uid_monster_01"))
    assertNull(repo.getCachedModelForVehicle(VehicleId.MONSTER_TRUCK))
  }

  @Test
  fun sketchfabPresetQueriesCoverAll12Vehicles() {
    for (vehId in VehicleId.entries) {
      val q = SketchfabRepository.VEHICLE_SEARCH_QUERIES[vehId]
      assertNotNull("Missing Sketchfab query for $vehId", q)
      assertTrue(q!!.isNotBlank())
    }
  }

  @Test
  fun savesAndReadsRuntimeSketchfabApiKeyInDataStore() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cacheRepo = ModelCacheRepository(context)
    val sketchfabRepo = SketchfabRepository(context)

    cacheRepo.saveRuntimeApiKey("  sf_test_token_9988  ")
    val loaded = cacheRepo.getRuntimeApiKey()
    assertEquals("sf_test_token_9988", loaded)

    sketchfabRepo.setRuntimeApiKey(loaded)
    assertTrue(sketchfabRepo.hasValidApiKey)
    assertEquals("sf_test_token_9988", sketchfabRepo.apiKey)

    cacheRepo.saveRuntimeApiKey("")
    assertEquals("", cacheRepo.getRuntimeApiKey())
  }
}
