package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.GameCatalog
import com.example.model.VehicleId
import com.example.ui.screens.*
import com.example.ui.theme.HillRushIndiaTheme
import com.example.ui.theme.RacingOrangeAccent
import com.example.viewmodel.GameViewModel

const val ROUTE_MAIN_MENU = "main_menu"
const val ROUTE_GAMEPLAY = "gameplay"
const val ROUTE_GARAGE = "garage"
const val ROUTE_UPGRADES = "upgrades/{vehicleId}"
const val ROUTE_MAP_SELECT = "map_select"
const val ROUTE_MISSIONS = "missions"
const val ROUTE_DAILY_REWARDS = "daily_rewards"
const val ROUTE_SHOP = "shop"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_PROFILE = "profile"
const val ROUTE_SKETCHFAB = "sketchfab?query={query}&vehicleId={vehicleId}"

class MainActivity : ComponentActivity() {

  private val gameViewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())

    setContent {
      HillRushIndiaTheme {
        HillRushApp(viewModel = gameViewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    gameViewModel.soundEngine.onResume()
  }

  override fun onPause() {
    super.onPause()
    gameViewModel.soundEngine.onPause()
  }
}

@Composable
fun HillRushApp(viewModel: GameViewModel) {
  val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
  val saveData by viewModel.saveState.collectAsStateWithLifecycle()
  val sketchfabState by viewModel.sketchfabState.collectAsStateWithLifecycle()

  if (!isLoaded) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = RacingOrangeAccent)
    }
    return
  }

  val navController = rememberNavController()
  val canClaimDaily = viewModel.canClaimDailyReward()
  val claimableMissionsCount =
    GameCatalog.missions.count { m ->
      m.id !in saveData.claimedMissions && (saveData.missionProgress[m.id] ?: 0) >= m.target
    }

  NavHost(
    navController = navController,
    startDestination = ROUTE_MAIN_MENU,
    enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 16 } },
    exitTransition = { fadeOut(tween(180)) },
    popEnterTransition = { fadeIn(tween(200)) },
    popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 16 } },
  ) {
    composable(ROUTE_MAIN_MENU) {
      MainMenuScreen(
        saveData = saveData,
        canClaimDaily = canClaimDaily,
        claimableMissionsCount = claimableMissionsCount,
        onPlay = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_GAMEPLAY)
        },
        onOpenGarage = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_GARAGE)
        },
        onOpenUpgrades = {
          viewModel.soundEngine.playClick()
          navController.navigate("upgrades/${saveData.selectedVehicleId.name}")
        },
        onOpenMaps = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_MAP_SELECT)
        },
        onOpenDailyRewards = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_DAILY_REWARDS)
        },
        onOpenMissions = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_MISSIONS)
        },
        onOpenShop = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_SHOP)
        },
        onOpenSettings = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_SETTINGS)
        },
        onOpenProfile = {
          viewModel.soundEngine.playClick()
          navController.navigate(ROUTE_PROFILE)
        },
        onOpenSketchfab = { query ->
          viewModel.soundEngine.playClick()
          val encoded = android.net.Uri.encode(query)
          navController.navigate("sketchfab?query=$encoded&vehicleId=${saveData.selectedVehicleId.name}")
        },
        onToggleSound = {
          val anyOn = saveData.settings.soundEnabled || saveData.settings.musicEnabled
          viewModel.updateSettings {
            it.copy(soundEnabled = !anyOn, musicEnabled = !anyOn)
          }
          if (!anyOn) viewModel.soundEngine.playClick()
        },
      )
    }

    composable(ROUTE_GAMEPLAY) {
      GameplayScreen(
        saveData = saveData,
        soundEngine = viewModel.soundEngine,
        consumeBoosters = viewModel::consumeBoostersForRun,
        onRunFinished = { dist, score, timeSec, coins, diamonds, stunts, stuntBonus, backflips, frontflips, fuelCans, crashed, reason ->
          viewModel.recordRunCompleted(
            mapId = saveData.selectedMapId,
            vehicleId = saveData.selectedVehicleId,
            distanceMeters = dist,
            score = score,
            timeSurvivedSec = timeSec,
            coinsEarned = coins,
            diamondsEarned = diamonds,
            stuntsCompleted = stunts,
            stuntBonusCoins = stuntBonus,
            backflips = backflips,
            frontflips = frontflips,
            fuelCansCollected = fuelCans,
            didCrash = crashed,
            reasonText = reason,
          )
        },
        onDoubleRewards = { bonus -> viewModel.doubleRunRewards(bonus) },
        onExitToMenu = { navController.popBackStack(ROUTE_MAIN_MENU, inclusive = false) },
        onOpenGarage = {
          navController.popBackStack(ROUTE_MAIN_MENU, inclusive = false)
          navController.navigate(ROUTE_GARAGE)
        },
      )
    }

    composable(ROUTE_GARAGE) {
      GarageScreen(
        saveData = saveData,
        onSelectVehicle = viewModel::selectVehicle,
        onUnlockVehicle = viewModel::unlockVehicle,
        onOpenUpgradeScreen = { vId ->
          viewModel.soundEngine.playClick()
          navController.navigate("upgrades/${vId.name}")
        },
        onBuyOrEquipSkin = viewModel::buyOrEquipSkin,
        onOpenSketchfab = { spec ->
          viewModel.soundEngine.playClick()
          val encoded = android.net.Uri.encode(spec.name)
          navController.navigate("sketchfab?query=$encoded&vehicleId=${spec.id.name}")
        },
        onTogglePrefer3d = viewModel::setPrefer3dShowroom,
        onRaceNow = { navController.navigate(ROUTE_GAMEPLAY) },
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(
      route = ROUTE_UPGRADES,
      arguments = listOf(navArgument("vehicleId") { type = NavType.StringType }),
    ) { backStackEntry ->
      val rawVeh = backStackEntry.arguments?.getString("vehicleId")
      val vehicleId =
        runCatching { VehicleId.valueOf(rawVeh.orEmpty()) }.getOrDefault(saveData.selectedVehicleId)
      UpgradeScreen(
        saveData = saveData,
        initialVehicleId = vehicleId,
        onUpgradePart = viewModel::upgradeVehiclePart,
        onRaceNow = { navController.navigate(ROUTE_GAMEPLAY) },
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(ROUTE_MAP_SELECT) {
      MapSelectionScreen(
        saveData = saveData,
        onSelectMap = viewModel::selectMap,
        onUnlockMap = viewModel::unlockMap,
        onRaceNow = { navController.navigate(ROUTE_GAMEPLAY) },
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(ROUTE_MISSIONS) {
      MissionsScreen(
        saveData = saveData,
        onClaimMission = viewModel::claimMission,
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(ROUTE_DAILY_REWARDS) {
      DailyRewardsScreen(
        saveData = saveData,
        canClaimToday = canClaimDaily,
        onClaimToday = viewModel::claimDailyReward,
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(ROUTE_SHOP) {
      ShopScreen(
        saveData = saveData,
        onExchangeDiamondsForCoins = viewModel::exchangeDiamondsForCoins,
        onExchangeCoinsForDiamonds = viewModel::exchangeCoinsForDiamonds,
        onWatchRewardCoins = viewModel::watchOptionalRewardAdForCoins,
        onBuyOrEquipSkin = viewModel::buyOrEquipSkin,
        onUnlockVehicle = viewModel::unlockVehicle,
        onBuyBooster = viewModel::buyBooster,
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(ROUTE_SETTINGS) {
      SettingsScreen(
        saveData = saveData,
        onUpdateSettings = viewModel::updateSettings,
        onResetProgress = viewModel::resetAllProgress,
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(ROUTE_PROFILE) {
      ProfileScreen(
        saveData = saveData,
        onUpdateProfile = viewModel::updatePlayerProfile,
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }

    composable(
      route = ROUTE_SKETCHFAB,
      arguments =
        listOf(
          navArgument("query") {
            type = NavType.StringType
            defaultValue = "offroad jeep 4x4"
          },
          navArgument("vehicleId") {
            type = NavType.StringType
            defaultValue = VehicleId.MINI_JEEP.name
          },
        ),
    ) { backStackEntry ->
      val query = backStackEntry.arguments?.getString("query").orEmpty().ifBlank { "offroad jeep 4x4" }
      val rawVeh = backStackEntry.arguments?.getString("vehicleId")
      val targetVeh =
        runCatching { VehicleId.valueOf(rawVeh.orEmpty()) }.getOrDefault(saveData.selectedVehicleId)
      SketchfabHangarScreen(
        saveData = saveData,
        sketchfabState = sketchfabState,
        initialQuery = query,
        targetVehicleId = targetVeh,
        onSearch = viewModel::searchSketchfabModels,
        onDownloadAndEquip = viewModel::downloadAndEquipSketchfabModel,
        onEquipDownloadedToVehicle = viewModel::equip3dModelToVehicle,
        onDeleteDownloaded = viewModel::deleteDownloaded3dModel,
        onSyncAllVehicles = viewModel::syncAllVehicleModelsFromSketchfab,
        onCancelBatchSync = viewModel::cancelBatchSync,
        onClearAllCache = viewModel::clearAll3dModelCache,
        onSaveApiKey = viewModel::saveRuntimeSketchfabKey,
        onClearError = viewModel::clearSketchfabDownloadError,
        onBack = {
          viewModel.soundEngine.playClick()
          navController.popBackStack()
        },
      )
    }
  }
}
