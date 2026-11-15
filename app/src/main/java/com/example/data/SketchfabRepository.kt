package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.model.Downloaded3DModel
import com.example.model.SketchfabModelSummary
import com.example.model.VehicleId
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

private interface SketchfabApiService {
  @GET("v3/search")
  suspend fun searchModels(
    @Query("type") type: String = "models",
    @Query("downloadable") downloadable: Boolean = true,
    @Query("archives_flavours") archivesFlavours: Boolean = false,
    @Query("max_face_count") maxFaceCount: Int = 90_000,
    @Query("count") count: Int = 18,
    @Query("q") query: String? = null,
    @Query("sort_by") sortBy: String? = null,
    @Query("cursor") cursor: String? = null,
    @Header("Authorization") authorization: String? = null,
  ): Response<ResponseBody>

  @GET("v3/models/{uid}/download")
  suspend fun getModelDownloadInfo(
    @Path("uid") uid: String,
    @Header("Authorization") authorization: String,
  ): Response<ResponseBody>

  @Streaming
  @GET
  suspend fun downloadModelArchive(@Url fileUrl: String): Response<ResponseBody>
}

class SketchfabRepository(private val context: Context) {

  companion object {
    val VEHICLE_SEARCH_QUERIES: Map<VehicleId, String> =
      mapOf(
        VehicleId.MINI_JEEP to "offroad jeep 4x4 lowpoly",
        VehicleId.HILL_BIKE to "dirt bike motocross lowpoly",
        VehicleId.MONSTER_TRUCK to "monster truck 3d",
        VehicleId.SPORTS_CAR to "sports car coupe lowpoly",
        VehicleId.TRACTOR to "farm tractor lowpoly",
        VehicleId.SCHOOL_BUS to "school bus cartoon",
        VehicleId.PICKUP_TRUCK to "pickup truck 4x4 lowpoly",
        VehicleId.RALLY_CAR to "rally car lowpoly",
        VehicleId.ARMY_JEEP to "military jeep willys",
        VehicleId.ELECTRIC_CAR to "futuristic electric car lowpoly",
        VehicleId.SNOW_VEHICLE to "snowmobile tracked vehicle",
        VehicleId.HEAVY_TRUCK to "heavy cargo truck lowpoly",
      )
  }

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()

  private val apiService: SketchfabApiService =
    Retrofit.Builder()
      .baseUrl("https://api.sketchfab.com/")
      .client(client)
      .build()
      .create(SketchfabApiService::class.java)

  @Volatile private var runtimeApiKeyOverride: String = ""

  fun setRuntimeApiKey(token: String) {
    runtimeApiKeyOverride = token.trim()
  }

  val apiKey: String
    get() {
      val override = runtimeApiKeyOverride.trim()
      if (override.isNotBlank() && override != "MY_SKETCHFAB_API_KEY") return override
      val buildCfg = BuildConfig.SKETCHFAB_API_KEY.trim()
      return if (buildCfg.isNotBlank() && buildCfg != "MY_SKETCHFAB_API_KEY") buildCfg else ""
    }

  val hasValidApiKey: Boolean
    get() = apiKey.isNotBlank()

  private val modelsRootDir: File
    get() = File(context.filesDir, "sketchfab_models").apply { mkdirs() }

  suspend fun searchFreeModels(
    query: String,
    sortBy: String? = null,
    maxFaceCount: Int = 90_000,
    cursor: String? = null,
  ): Result<Pair<List<SketchfabModelSummary>, String?>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanedQuery = query.trim().takeIf { it.isNotBlank() }
        // Omit sort_by when a query is present so Sketchfab v3/search sorts by relevance
        val effectiveSort = sortBy ?: if (cleanedQuery == null) "-likeCount" else null
        val authHeader = if (hasValidApiKey) "Token $apiKey" else null

        val resp =
          apiService.searchModels(
            query = cleanedQuery,
            sortBy = effectiveSort,
            maxFaceCount = maxFaceCount,
            cursor = cursor?.takeIf { it.isNotBlank() },
            authorization = authHeader,
          )

        val bodyStr = resp.body()?.string() ?: resp.errorBody()?.string().orEmpty()
        if (!resp.isSuccessful) {
          throw IllegalStateException(
            when (resp.code()) {
              401 -> "Sketchfab API token rejected (401). Tap KEY badge to update your token."
              429 -> "Sketchfab rate limit reached (429). Please try again in a moment."
              else -> "Sketchfab search error (${resp.code()}): ${resp.message()}"
            }
          )
        }

        val root = JSONObject(bodyStr)
        val nextCursor =
          root.optJSONObject("cursors")?.optString("next", "")?.takeIf { it.isNotBlank() && it != "null" }
        val resultsArr = root.optJSONArray("results")
        val items = mutableListOf<SketchfabModelSummary>()

        if (resultsArr != null) {
          for (i in 0 until resultsArr.length()) {
            val item = resultsArr.optJSONObject(i) ?: continue
            val uid = item.optString("uid", "")
            if (uid.isBlank()) continue

            val thumbUrl = pickBestThumbnailUrl(item.optJSONObject("thumbnails"))
            val userObj = item.optJSONObject("user")
            val authorName =
              userObj?.optString("displayName")?.takeIf { it.isNotBlank() }
                ?: userObj?.optString("username")?.takeIf { it.isNotBlank() }
                ?: "Sketchfab Creator"
            val authorUrl = userObj?.optString("profileUrl", "") ?: ""

            val licObj = item.optJSONObject("license")
            val licLabel = licObj?.optString("label", "Creative Commons") ?: "Creative Commons"
            val licSlug = licObj?.optString("slug", "by") ?: "by"

            items.add(
              SketchfabModelSummary(
                uid = uid,
                name = item.optString("name", "Untitled 3D Model"),
                authorName = authorName,
                authorProfileUrl = authorUrl,
                licenseLabel = licLabel,
                licenseSlug = licSlug,
                thumbnailUrl = thumbUrl,
                faceCount = item.optInt("faceCount", 0),
                vertexCount = item.optInt("vertexCount", 0),
                likeCount = item.optInt("likeCount", 0),
                viewCount = item.optInt("viewCount", 0),
              )
            )
          }
        }
        items to nextCursor
      }
    }

  suspend fun fetchBestFreeModelForQuery(
    query: String,
    avoidUids: Set<String> = emptySet(),
  ): Result<SketchfabModelSummary> =
    withContext(Dispatchers.IO) {
      searchFreeModels(query = query, sortBy = null, maxFaceCount = 55_000).mapCatching { (items, _) ->
        val filtered = items.filter { it.uid !in avoidUids }
        filtered.firstOrNull { it.faceCount in 500..45_000 }
          ?: filtered.firstOrNull()
          ?: items.firstOrNull()
          ?: throw IllegalStateException("No free downloadable 3D model found for \"$query\".")
      }
    }

  suspend fun downloadAndExtractModel(
    summary: SketchfabModelSummary,
    onProgress: (Float) -> Unit,
  ): Result<Downloaded3DModel> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!hasValidApiKey) {
          throw IllegalStateException(
            "SKETCHFAB_API_KEY missing. Tap 'KEY MISSING' above to enter your token, or add SKETCHFAB_API_KEY to GitHub/AI Studio Secrets."
          )
        }

        onProgress(0.04f)
        val infoResp = apiService.getModelDownloadInfo(uid = summary.uid, authorization = "Token $apiKey")
        val infoBody = infoResp.body()?.string() ?: infoResp.errorBody()?.string().orEmpty()
        if (!infoResp.isSuccessful) {
          throw IllegalStateException(
            when (infoResp.code()) {
              401 -> "Invalid SKETCHFAB_API_KEY (401). Tap 'KEY' badge above to paste a valid token."
              403 -> "This model is restricted or requires creator permission (403)."
              404 -> "Download package not available for this model (404)."
              429 -> "Sketchfab download quota/rate limit reached (429)."
              else -> "Sketchfab download endpoint failed (${infoResp.code()})."
            }
          )
        }

        val root = JSONObject(infoBody)
        val glbObj = root.optJSONObject("glb")
        val gltfObj = root.optJSONObject("gltf")
        val downloadUrl: String
        val expectedSize: Long
        val isSingleGlb: Boolean
        when {
          glbObj != null && glbObj.optString("url").isNotBlank() -> {
            downloadUrl = glbObj.optString("url")
            expectedSize = glbObj.optLong("size", 0L)
            isSingleGlb = true
          }
          gltfObj != null && gltfObj.optString("url").isNotBlank() -> {
            downloadUrl = gltfObj.optString("url")
            expectedSize = gltfObj.optLong("size", 0L)
            isSingleGlb = false
          }
          else -> throw IllegalStateException("No glTF or GLB download format available for ${summary.name}.")
        }

        if (expectedSize > 75L * 1024L * 1024L) {
          throw IllegalStateException(
            "Model archive is ${(expectedSize / (1024 * 1024))} MB (over 75 MB mobile limit). Pick a lighter model."
          )
        }

        val modelDir = File(modelsRootDir, summary.uid)
        if (modelDir.exists()) modelDir.deleteRecursively()
        modelDir.mkdirs()

        val tempFile = File(context.cacheDir, "sf_${summary.uid}.tmp")
        if (tempFile.exists()) tempFile.delete()

        var downloadedBytes = 0L
        val assetResp = apiService.downloadModelArchive(downloadUrl)
        if (!assetResp.isSuccessful) {
          throw IllegalStateException("CDN download failed (${assetResp.code()}).")
        }
        val body = assetResp.body() ?: throw IllegalStateException("Empty download stream.")
        body.use { responseBody ->
          val contentLen = responseBody.contentLength().takeIf { it > 0L } ?: expectedSize.coerceAtLeast(1L)
          responseBody.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
              val buf = ByteArray(16 * 1024)
              var read: Int
              while (input.read(buf).also { read = it } != -1) {
                output.write(buf, 0, read)
                downloadedBytes += read
                val frac = (downloadedBytes.toFloat() / contentLen.toFloat()).coerceIn(0f, 1f)
                onProgress(0.05f + frac * 0.78f)
              }
            }
          }
        }

        onProgress(0.86f)
        val entryFile: File =
          if (isSingleGlb && !looksLikeZip(tempFile)) {
            val destGlb = File(modelDir, "model.glb")
            tempFile.copyTo(destGlb, overwrite = true)
            tempFile.delete()
            destGlb
          } else {
            unzipWithSlipGuard(tempFile, modelDir)
            tempFile.delete()
            findEntryModelFile(modelDir)
              ?: throw IllegalStateException("Extracted archive contains no .gltf or .glb file.")
          }

        onProgress(1f)
        Downloaded3DModel(
          uid = summary.uid,
          name = summary.name,
          authorName = summary.authorName,
          licenseLabel = summary.licenseLabel,
          thumbnailUrl = summary.thumbnailUrl,
          entryFilePath = entryFile.absolutePath,
          localDirPath = modelDir.absolutePath,
          sizeBytes = computeDirSize(modelDir),
          downloadedAtEpochMs = System.currentTimeMillis(),
        )
      }
    }

  fun deleteLocalFiles(model: Downloaded3DModel) {
    runCatching {
      val dir = File(model.localDirPath)
      if (dir.exists()) dir.deleteRecursively()
    }
  }

  private fun pickBestThumbnailUrl(thumbnailsObj: JSONObject?): String {
    val images = thumbnailsObj?.optJSONArray("images") ?: return ""
    var bestUrl = ""
    var bestDiff = Int.MAX_VALUE
    for (i in 0 until images.length()) {
      val img = images.optJSONObject(i) ?: continue
      val url = img.optString("url", "")
      if (url.isBlank()) continue
      val width = img.optInt("width", 0)
      val diff = kotlin.math.abs(width - 720)
      if (diff < bestDiff) {
        bestDiff = diff
        bestUrl = url
      }
    }
    return bestUrl
  }

  private fun looksLikeZip(file: File): Boolean {
    if (!file.exists() || file.length() < 4) return false
    FileInputStream(file).use { fis ->
      val header = ByteArray(4)
      val n = fis.read(header)
      return n >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
    }
  }

  private fun unzipWithSlipGuard(zipFile: File, destDir: File) {
    val canonicalDest = destDir.canonicalPath
    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
      var entry = zis.nextEntry
      val buf = ByteArray(16 * 1024)
      while (entry != null) {
        val outFile = File(destDir, entry.name)
        val canonicalOut = outFile.canonicalPath
        if (!canonicalOut.startsWith(canonicalDest + File.separator)) {
          throw SecurityException("Blocked unsafe zip entry path: ${entry.name}")
        }
        if (entry.isDirectory) {
          outFile.mkdirs()
        } else {
          outFile.parentFile?.mkdirs()
          FileOutputStream(outFile).use { fos ->
            var len: Int
            while (zis.read(buf).also { len = it } != -1) {
              fos.write(buf, 0, len)
            }
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
  }

  private fun findEntryModelFile(root: File): File? {
    val allFiles = root.walkTopDown().filter { it.isFile }.toList()
    return allFiles.firstOrNull { it.name.equals("scene.gltf", ignoreCase = true) }
      ?: allFiles.firstOrNull { it.extension.equals("glb", ignoreCase = true) }
      ?: allFiles.firstOrNull { it.extension.equals("gltf", ignoreCase = true) }
  }

  private fun computeDirSize(dir: File): Long =
    dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
