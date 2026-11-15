package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.GameCatalog
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SketchfabUiState

private val SKETCHFAB_PRESETS =
  listOf(
    "4x4 Jeep" to "offroad jeep 4x4",
    "Monster Truck" to "monster truck",
    "Rally Car" to "rally car",
    "Dirt Bike" to "dirt bike motorcycle",
    "Tractor" to "farm tractor",
    "Sports Car" to "sports car lowpoly",
    "Heavy Truck" to "cargo truck",
    "Bus & Van" to "vintage bus",
  )

@Composable
fun SketchfabHangarScreen(
  saveData: PlayerSaveData,
  sketchfabState: SketchfabUiState,
  initialQuery: String,
  targetVehicleId: VehicleId,
  onSearch: (String, Boolean) -> Unit,
  onDownloadAndEquip: (SketchfabModelSummary, VehicleId?) -> Unit,
  onEquipDownloadedToVehicle: (VehicleId, String?) -> Unit,
  onDeleteDownloaded: (String) -> Unit,
  onSyncAllVehicles: () -> Unit,
  onCancelBatchSync: () -> Unit,
  onClearAllCache: () -> Unit,
  onSaveApiKey: (String) -> Unit,
  onClearError: () -> Unit,
  onBack: () -> Unit,
) {
  var showDownloadedTab by remember { mutableStateOf(false) }
  var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery.ifBlank { "offroad jeep 4x4" }) }
  var assignVehicleId by remember(targetVehicleId) { mutableStateOf(targetVehicleId) }
  var selectedSummary by remember { mutableStateOf<SketchfabModelSummary?>(null) }
  var selectedDownloadedUid by remember { mutableStateOf<String?>(null) }
  var autoRotate by remember { mutableStateOf(true) }
  var showApiKeyDialog by remember { mutableStateOf(false) }
  var apiKeyDraft by remember { mutableStateOf("") }

  LaunchedEffect(initialQuery) {
    if (sketchfabState.results.isEmpty() || initialQuery.isNotBlank()) {
      onSearch(searchQuery, false)
    }
  }

  val activeDownloaded: Downloaded3DModel? =
    remember(selectedDownloadedUid, selectedSummary, saveData.downloaded3dModels) {
      val uid = selectedDownloadedUid ?: selectedSummary?.uid
      if (uid != null) saveData.downloaded3dModels[uid] else saveData.downloaded3dModels.values.firstOrNull()
    }

  val assignVehicleSpec = remember(assignVehicleId) { GameCatalog.vehicleById(assignVehicleId) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary, DarkNavyBg)))
  ) {
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
      ScreenTopBar(
        title = "SKETCHFAB 3D HANGAR",
        coins = saveData.coins,
        diamonds = saveData.diamonds,
        onBack = onBack,
      )

      // Top Controls Strip: Mode Tabs + API Key Status + Assign Vehicle Selector
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          HangarModeChip(
            label = "DISCOVER FREE 3D",
            icon = Icons.Filled.Public,
            selected = !showDownloadedTab,
            onClick = { showDownloadedTab = false },
            testTag = "sketchfab_discover_tab",
          )
          HangarModeChip(
            label = "MY 3D RIGS (${saveData.downloaded3dModels.size})",
            icon = Icons.Filled.Inventory2,
            selected = showDownloadedTab,
            onClick = { showDownloadedTab = true },
            testTag = "sketchfab_downloaded_tab",
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val totalCacheBytes =
            remember(saveData.downloaded3dModels) {
              saveData.downloaded3dModels.values.sumOf { it.sizeBytes }
            }
          val mappedCount = saveData.equipped3dModelByVehicle.size

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardNavy,
            border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.4f)),
          ) {
            Text(
              text = "$mappedCount/12 CACHED • ${formatFileSizeMb(totalCacheBytes)}",
              style = MaterialTheme.typography.labelSmall,
              color = SkyCyan,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }

          ArcadeGradientButton(
            text =
              if (sketchfabState.isBatchSyncing)
                "STOP (${sketchfabState.batchCompletedCount}/${sketchfabState.batchTotalCount})"
              else "SYNC ALL 12",
            icon = if (sketchfabState.isBatchSyncing) Icons.Filled.Stop else Icons.Filled.CloudSync,
            colors =
              if (sketchfabState.isBatchSyncing) listOf(DangerRed, RacingOrangeAccent)
              else listOf(RacingOrangeAccent, GoldenYellowHighlight),
            textColor = if (sketchfabState.isBatchSyncing) TextPrimary else DarkNavyBg,
            onClick = {
              if (sketchfabState.isBatchSyncing) onCancelBatchSync() else onSyncAllVehicles()
            },
            modifier = Modifier.height(42.dp),
            testTag = "sketchfab_sync_all_button",
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (sketchfabState.hasValidApiKey) EmeraldDark.copy(alpha = 0.65f) else Color(0xFF451A03),
            border =
              BorderStroke(
                1.dp,
                if (sketchfabState.hasValidApiKey) SuccessGreen else RacingOrangeAccent,
              ),
            modifier =
              Modifier.defaultMinSize(minHeight = 42.dp)
                .clickable { showApiKeyDialog = true }
                .testTag("sketchfab_api_key_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = if (sketchfabState.hasValidApiKey) Icons.Filled.VerifiedUser else Icons.Filled.Key,
                contentDescription = "Configure Sketchfab API Key",
                tint = if (sketchfabState.hasValidApiKey) SuccessGreen else GoldenYellowHighlight,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (sketchfabState.hasValidApiKey) "API ACTIVE" else "KEY MISSING",
                style = MaterialTheme.typography.labelSmall,
                color = if (sketchfabState.hasValidApiKey) SuccessGreen else GoldenYellowHighlight,
              )
            }
          }
        }
      }

      if (sketchfabState.isBatchSyncing || sketchfabState.batchStatusMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardNavyElevated,
          border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = sketchfabState.batchStatusMessage.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = GoldenYellowHighlight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              if (!sketchfabState.isBatchSyncing) {
                Text(
                  text = "DISMISS",
                  style = MaterialTheme.typography.labelSmall,
                  color = SkyCyan,
                  modifier = Modifier.clickable { onClearError() },
                )
              }
            }
            if (sketchfabState.isBatchSyncing) {
              LinearProgressIndicator(
                progress = {
                  (sketchfabState.batchCompletedCount.toFloat() / sketchfabState.batchTotalCount.coerceAtLeast(1))
                    .coerceIn(0.05f, 1f)
                },
                color = SuccessGreen,
                trackColor = MidnightBluePrimary,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // LEFT COLUMN: Search + Presets + Grid of Free Sketchfab Models (or Downloaded List)
        Column(
          modifier = Modifier.weight(1.25f).fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (!showDownloadedTab) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Search free 3D cars, jeeps, bikes...") },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = SkyCyan) },
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlueSecondary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardNavy,
                    unfocusedContainerColor = CardNavy,
                  ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).testTag("sketchfab_search_input"),
              )
              ArcadeGradientButton(
                text = "SEARCH",
                icon = Icons.Filled.Search,
                colors = listOf(ElectricBlueSecondary, Color(0xFF1D4ED8)),
                onClick = { onSearch(searchQuery, false) },
                testTag = "sketchfab_search_button",
              )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(SKETCHFAB_PRESETS) { (label, query) ->
                val active = searchQuery.equals(query, ignoreCase = true)
                FilterChip(
                  selected = active,
                  onClick = {
                    searchQuery = query
                    onSearch(query, false)
                  },
                  label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = RacingOrangeAccent,
                      selectedLabelColor = DarkNavyBg,
                      containerColor = CardNavy,
                      labelColor = TextSecondary,
                    ),
                )
              }
            }

            if (sketchfabState.isSearching && sketchfabState.results.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  CircularProgressIndicator(color = RacingOrangeAccent)
                  Text("Searching Sketchfab Free 3D Models...", color = TextSecondary)
                }
              }
            } else if (sketchfabState.searchError != null && sketchfabState.results.isEmpty()) {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardNavy,
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxSize(),
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(Icons.Filled.WifiOff, null, tint = DangerRed, modifier = Modifier.size(38.dp))
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(sketchfabState.searchError, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                  Spacer(modifier = Modifier.height(10.dp))
                  ArcadeGradientButton(
                    text = "RETRY SEARCH",
                    icon = Icons.Filled.Refresh,
                    colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
                    textColor = DarkNavyBg,
                    onClick = { onSearch(searchQuery, false) },
                  )
                }
              }
            } else {
              LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
              ) {
                items(sketchfabState.results, key = { it.uid }) { model ->
                  val isDownloaded = model.uid in saveData.downloaded3dModels
                  val isFocused = selectedSummary?.uid == model.uid || selectedDownloadedUid == model.uid
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFocused) CardNavyElevated else CardNavy,
                    border =
                      BorderStroke(
                        width = if (isFocused) 2.dp else 1.dp,
                        color =
                          when {
                            isFocused -> RacingOrangeAccent
                            isDownloaded -> SuccessGreen.copy(alpha = 0.7f)
                            else -> Color.White.copy(alpha = 0.12f)
                          },
                      ),
                    modifier =
                      Modifier.fillMaxWidth()
                        .clickable {
                          selectedSummary = model
                          selectedDownloadedUid = if (isDownloaded) model.uid else null
                        }
                        .testTag("sketchfab_card_${model.uid}"),
                  ) {
                    Column {
                      Box(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                        AsyncImage(
                          model = model.thumbnailUrl,
                          contentDescription = model.name,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        )
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = DarkNavyBg.copy(alpha = 0.84f),
                          modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                        ) {
                          Text(
                            text = if (isDownloaded) "SAVED 3D" else "FREE ${model.licenseSlug.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDownloaded) SuccessGreen else GoldenYellowHighlight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          )
                        }
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = DarkNavyBg.copy(alpha = 0.80f),
                          modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        ) {
                          Text(
                            text = "%,dk faces".format((model.faceCount / 1000).coerceAtLeast(1)),
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          )
                        }
                      }
                      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                          text = model.name,
                          style = MaterialTheme.typography.labelLarge,
                          color = TextPrimary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "By ${model.authorName} • ♥ ${model.likeCount}",
                          style = MaterialTheme.typography.labelSmall,
                          color = TextSecondary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                }
              }
            }
          } else {
            // Downloaded Offline Cache Tab
            val downloadedList = saveData.downloaded3dModels.values.toList()
            if (downloadedList.isEmpty()) {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardNavy,
                modifier = Modifier.fillMaxSize(),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                ) {
                  Icon(Icons.Filled.ViewInAr, null, tint = SkyCyan, modifier = Modifier.size(44.dp))
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("NO 3D MODELS SAVED YET", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                  Text(
                    "Browse 'Discover Free 3D' and download any car, jeep, or monster truck!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                  )
                }
              }
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "DATASTORE OFFLINE 3D CACHE",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyCyan,
                  )
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DangerRed.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onClearAllCache() }.testTag("clear_3d_cache_button"),
                  ) {
                    Text(
                      text = "CLEAR ALL CACHE",
                      style = MaterialTheme.typography.labelSmall,
                      color = DangerRed,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                  }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                  items(downloadedList, key = { it.uid }) { dl ->
                    val equippedTo = saveData.equipped3dModelByVehicle.entries.filter { it.value == dl.uid }.map { it.key }
                    val selected = activeDownloaded?.uid == dl.uid
                    Surface(
                      shape = RoundedCornerShape(16.dp),
                      color = if (selected) CardNavyElevated else CardNavy,
                      border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) RacingOrangeAccent else SuccessGreen.copy(alpha = 0.5f)),
                      modifier =
                        Modifier.fillMaxWidth().clickable {
                          selectedDownloadedUid = dl.uid
                          selectedSummary = null
                        },
                    ) {
                      Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        AsyncImage(
                          model = dl.thumbnailUrl,
                          contentDescription = dl.name,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(dl.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
                          Text(
                            "By ${dl.authorName} • ${dl.licenseLabel} • ${formatFileSizeMb(dl.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                          )
                          if (equippedTo.isNotEmpty()) {
                            Text(
                              text = "EQUIPPED ON: ${equippedTo.joinToString { GameCatalog.vehicleById(it).name }}",
                              style = MaterialTheme.typography.labelSmall,
                              color = SuccessGreen,
                            )
                          }
                        }
                        IconButton(onClick = { onDeleteDownloaded(dl.uid) }) {
                          Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete 3D model", tint = DangerRed)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // RIGHT COLUMN: Live 360° SceneView 3D Viewport + Download / Equip Inspector
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = CardNavy.copy(alpha = 0.94f),
          border = BorderStroke(1.5.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
          modifier = Modifier.weight(1f).fillMaxHeight().padding(bottom = 4.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            val inspectingDownloaded =
              selectedDownloadedUid?.let { saveData.downloaded3dModels[it] }
                ?: selectedSummary?.uid?.let { saveData.downloaded3dModels[it] }
                ?: if (showDownloadedTab) activeDownloaded else null

            val titleText = inspectingDownloaded?.name ?: selectedSummary?.name ?: "SELECT A 3D MODEL"
            val subtitleText =
              when {
                inspectingDownloaded != null ->
                  "By ${inspectingDownloaded.authorName} • ${inspectingDownloaded.licenseLabel} • ${formatFileSizeMb(inspectingDownloaded.sizeBytes)}"
                selectedSummary != null ->
                  "By ${selectedSummary!!.authorName} • ${selectedSummary!!.licenseLabel} • %,d faces".format(selectedSummary!!.faceCount)
                else -> "Tap any free Sketchfab vehicle on the left to inspect & load in 3D"
              }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(titleText, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitleText, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              if (inspectingDownloaded != null) {
                IconButton(onClick = { autoRotate = !autoRotate }) {
                  Icon(
                    imageVector = if (autoRotate) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = "Toggle Turntable",
                    tint = GoldenYellowHighlight,
                  )
                }
              }
            }

            // 3D Viewport or Thumbnail Stage
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .weight(1f)
                  .padding(vertical = 8.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(DarkNavyBg),
              contentAlignment = Alignment.Center,
            ) {
              when {
                inspectingDownloaded != null -> {
                  Sketchfab3DModelViewport(
                    entryFilePath = inspectingDownloaded.entryFilePath,
                    modelTitle = inspectingDownloaded.name,
                    authorName = inspectingDownloaded.authorName,
                    licenseLabel = inspectingDownloaded.licenseLabel,
                    autoRotate = autoRotate,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
                selectedSummary != null -> {
                  AsyncImage(
                    model = selectedSummary!!.thumbnailUrl,
                    contentDescription = selectedSummary!!.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkNavyBg.copy(alpha = 0.80f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
                  ) {
                    Text(
                      text = "Download to spin & inspect in interactive 360° 3D",
                      style = MaterialTheme.typography.labelMedium,
                      color = GoldenYellowHighlight,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                  }
                }
                else -> {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(20.dp),
                  ) {
                    Icon(Icons.Filled.ViewInAr, null, tint = ElectricBlueSecondary, modifier = Modifier.size(54.dp))
                    Text("3D PBR STUDIO READY", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                      "Powered by Filament 3D Engine + Sketchfab Free CC Library",
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextSecondary,
                    )
                  }
                }
              }
            }

            // Target Vehicle Strip (choose which garage rig receives this 3D skin)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(GameCatalog.vehicles.filter { it.id in saveData.unlockedVehicles }) { veh ->
                val sel = veh.id == assignVehicleId
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (sel) ElectricBlueSecondary else MidnightBluePrimary,
                  modifier = Modifier.clickable { assignVehicleId = veh.id },
                ) {
                  Text(
                    text = veh.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel) TextPrimary else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            sketchfabState.downloadError?.let { err ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = DangerRed.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, DangerRed),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { onClearError() },
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "$err (tap to dismiss)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                  )
                  if (!sketchfabState.hasValidApiKey || err.contains("401")) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = RacingOrangeAccent,
                      modifier = Modifier.clickable { showApiKeyDialog = true }.testTag("enter_key_inline_button"),
                    ) {
                      Text(
                        text = "ENTER KEY",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkNavyBg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                      )
                    }
                  }
                }
              }
            }

            val isDownloadingThis =
              selectedSummary != null && sketchfabState.downloadingUid == selectedSummary?.uid

            if (isDownloadingThis) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                  progress = { sketchfabState.downloadProgress.coerceIn(0.05f, 1f) },
                  color = RacingOrangeAccent,
                  trackColor = MidnightBluePrimary,
                  modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                )
                Text(
                  text = "Downloading & Extracting 3D GLTF/GLB... ${(sketchfabState.downloadProgress * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = GoldenYellowHighlight,
                )
              }
            } else if (inspectingDownloaded != null) {
              val alreadyEquippedHere = saveData.equipped3dModelByVehicle[assignVehicleId] == inspectingDownloaded.uid
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (alreadyEquippedHere) {
                  ArcadeGradientButton(
                    text = "UNEQUIP FROM ${assignVehicleSpec.name.uppercase()}",
                    icon = Icons.Filled.LinkOff,
                    colors = listOf(CardNavyElevated, MidnightBluePrimary),
                    onClick = { onEquipDownloadedToVehicle(assignVehicleId, null) },
                    modifier = Modifier.weight(1f),
                    testTag = "unequip_3d_button",
                  )
                } else {
                  ArcadeGradientButton(
                    text = "EQUIP TO ${assignVehicleSpec.name.uppercase()}",
                    icon = Icons.Filled.CheckCircle,
                    colors = listOf(SuccessGreen, EmeraldDark),
                    textColor = DarkNavyBg,
                    onClick = { onEquipDownloadedToVehicle(assignVehicleId, inspectingDownloaded.uid) },
                    modifier = Modifier.weight(1f),
                    testTag = "equip_3d_button",
                  )
                }
              }
            } else if (selectedSummary != null) {
              ArcadeGradientButton(
                text = "DOWNLOAD FREE 3D & EQUIP (${assignVehicleSpec.name.uppercase()})",
                icon = Icons.Filled.CloudDownload,
                colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
                textColor = DarkNavyBg,
                enabled = sketchfabState.downloadingUid == null,
                onClick = { onDownloadAndEquip(selectedSummary!!, assignVehicleId) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "download_sketchfab_button",
              )
            }
          }
        }
      }
    }

    if (showApiKeyDialog) {
      AlertDialog(
        onDismissRequest = { showApiKeyDialog = false },
        containerColor = CardNavy,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Sketchfab API Token", style = MaterialTheme.typography.titleLarge) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "Paste your personal Sketchfab API Token (sketchfab.com/settings/password) to enable free 3D downloads right in this APK. Stored locally in DataStore.",
              style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
              value = apiKeyDraft,
              onValueChange = { apiKeyDraft = it },
              singleLine = true,
              placeholder = { Text("Paste Sketchfab API Token...") },
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = RacingOrangeAccent,
                  unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary,
                ),
              modifier = Modifier.fillMaxWidth().testTag("sketchfab_api_key_input"),
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              onSaveApiKey(apiKeyDraft)
              showApiKeyDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = RacingOrangeAccent, contentColor = DarkNavyBg),
            modifier = Modifier.testTag("save_sketchfab_key_button"),
          ) {
            Text("SAVE TOKEN")
          }
        },
        dismissButton = {
          TextButton(onClick = { showApiKeyDialog = false }) {
            Text("CANCEL", color = TextSecondary)
          }
        },
      )
    }
  }
}

@Composable
private fun HangarModeChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (selected) ElectricBlueSecondary else CardNavy,
    border = BorderStroke(1.dp, if (selected) SkyCyan else Color.White.copy(alpha = 0.14f)),
    modifier = Modifier.defaultMinSize(minHeight = 44.dp).clickable(onClick = onClick).testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(icon, null, tint = if (selected) TextPrimary else TextSecondary, modifier = Modifier.size(18.dp))
      Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) TextPrimary else TextSecondary)
    }
  }
}
