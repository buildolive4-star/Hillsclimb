package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
  saveData: PlayerSaveData,
  onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
  onResetProgress: () -> Unit,
  onBack: () -> Unit,
) {
  val settings = saveData.settings
  var showResetConfirm by remember { mutableStateOf(false) }
  var showPrivacyModal by remember { mutableStateOf(false) }
  var showTermsModal by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "SETTINGS",
      coins = saveData.coins,
      diamonds = saveData.diamonds,
      onBack = onBack,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Music Volume & Sound Effects Volume Sliders
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SettingSliderCard(
            title = "Music Volume (${(settings.musicVolume * 100).toInt()}%)",
            value = settings.musicVolume,
            valueRange = 0f..1f,
            onValueChange = { v ->
              onUpdateSettings { it.copy(musicVolume = v, musicEnabled = v > 0.02f) }
            },
            modifier = Modifier.weight(1f),
          )
          SettingSliderCard(
            title = "Sound Effects Volume (${(settings.soundVolume * 100).toInt()}%)",
            value = settings.soundVolume,
            valueRange = 0f..1f,
            onValueChange = { v ->
              onUpdateSettings { it.copy(soundVolume = v, soundEnabled = v > 0.02f) }
            },
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Control Sensitivity Slider + Vibration Switch
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SettingSliderCard(
            title = "Control Sensitivity (${String.format("%.1fx", settings.controlSensitivity)})",
            value = settings.controlSensitivity,
            valueRange = 0.6f..1.5f,
            onValueChange = { v -> onUpdateSettings { it.copy(controlSensitivity = v) } },
            modifier = Modifier.weight(1f),
          )
          SettingSwitchCard(
            title = "Vibration",
            subtitle = "Haptic feedback on crashes & fuel pickups",
            checked = settings.vibrationEnabled,
            onCheckedChange = { v -> onUpdateSettings { it.copy(vibrationEnabled = v) } },
            modifier = Modifier.weight(1f),
            testTag = "setting_vibration_switch",
          )
        }
      }

      // Graphics Quality & Language
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardNavy,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Graphics Quality", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GraphicsQuality.entries.forEach { q ->
                  FilterChip(
                    selected = settings.graphicsQuality == q,
                    onClick = { onUpdateSettings { it.copy(graphicsQuality = q) } },
                    label = { Text(q.label) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                  )
                }
              }
            }
          }
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardNavy,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Language", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.forEach { lang ->
                  FilterChip(
                    selected = settings.language == lang,
                    onClick = { onUpdateSettings { it.copy(language = lang) } },
                    label = { Text(lang.label) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Privacy Policy | Terms & Conditions | Reset Progress
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ArcadeGradientButton(
            text = "PRIVACY POLICY",
            icon = Icons.Filled.Policy,
            colors = listOf(ElectricBlueSecondary, Color(0xFF1A4FD6)),
            onClick = { showPrivacyModal = true },
            modifier = Modifier.weight(1f),
            testTag = "privacy_button",
          )
          ArcadeGradientButton(
            text = "TERMS & CONDITIONS",
            icon = Icons.Filled.Description,
            colors = listOf(CardNavyElevated, MidnightBluePrimary),
            onClick = { showTermsModal = true },
            modifier = Modifier.weight(1f),
            testTag = "terms_button",
          )
          ArcadeGradientButton(
            text = "RESET PROGRESS",
            icon = Icons.Filled.DeleteForever,
            colors = listOf(DangerRed, Color(0xFF991B1B)),
            onClick = { showResetConfirm = true },
            modifier = Modifier.weight(1f),
            testTag = "reset_progress_button",
          )
        }
      }
    }
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("Reset All Progress?") },
      text = {
        Text("This permanently clears your coins, diamonds, unlocked vehicles, upgrades, maps, and records.")
      },
      confirmButton = {
        Button(
          onClick = {
            onResetProgress()
            showResetConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
        ) {
          Text("RESET")
        }
      },
      dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("CANCEL") } },
    )
  }

  if (showPrivacyModal) {
    AlertDialog(
      onDismissRequest = { showPrivacyModal = false },
      title = { Text("Privacy Policy") },
      text = {
        Text("Hill Rush India stores all gameplay progress, garage upgrades, settings, and records locally on your device. No personal information is collected or shared.")
      },
      confirmButton = { Button(onClick = { showPrivacyModal = false }) { Text("CLOSE") } },
    )
  }

  if (showTermsModal) {
    AlertDialog(
      onDismissRequest = { showTermsModal = false },
      title = { Text("Terms & Conditions") },
      text = {
        Text("Hill Rush India is provided for entertainment. All in-game virtual coins, diamonds, boosters, and skins are virtual items stored on your device.")
      },
      confirmButton = { Button(onClick = { showTermsModal = false }) { Text("CLOSE") } },
    )
  }
}

@Composable
private fun SettingSliderCard(
  title: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = CardNavy,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
      Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(thumbColor = GoldenYellowHighlight, activeTrackColor = RacingOrangeAccent),
      )
    }
  }
}

@Composable
private fun SettingSwitchCard(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = CardNavy,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
      Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
    }
  }
}

@Composable
fun ProfileScreen(
  saveData: PlayerSaveData,
  onUpdateProfile: (String, Int) -> Unit,
  onBack: () -> Unit,
) {
  var nameDraft by remember(saveData.playerName) { mutableStateOf(saveData.playerName) }
  var selectedAvatar by remember(saveData.avatarId) { mutableIntStateOf(saveData.avatarId) }
  val achievementsCompleted =
    GameCatalog.missions.count {
      it.period == MissionPeriod.ACHIEVEMENT &&
        (it.id in saveData.claimedMissions || (saveData.missionProgress[it.id] ?: 0) >= it.target)
    }
  val achievementsTotal = GameCatalog.missions.count { it.period == MissionPeriod.ACHIEVEMENT }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "DRIVER PROFILE",
      coins = saveData.coins,
      diamonds = saveData.diamonds,
      onBack = onBack,
    )

    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Left: Avatar Picker + Username + Level & XP Progress
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardNavy,
        border = BorderStroke(1.5.dp, ElectricBlueSecondary.copy(alpha = 0.55f)),
        modifier = Modifier.weight(0.95f).fillMaxHeight(),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PlayerAvatarBadge(avatarId = selectedAvatar, sizeDp = 56)
            Column {
              Text(
                text = "LEVEL ${saveData.playerLevel} / 100",
                style = MaterialTheme.typography.headlineMedium,
                color = GoldenYellowHighlight,
              )
              Text(
                text = "Total XP: %,d".format(saveData.xp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }

          StatProgressRow(
            label = "NEXT LEVEL PROGRESS",
            fraction = saveData.levelProgressFraction(),
            color = RacingOrangeAccent,
            valueText = "${(saveData.levelProgressFraction() * 100).toInt()}%",
          )

          // Avatar Chooser Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            for (i in 0..5) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .border(
                      width = if (selectedAvatar == i) 2.5.dp else 0.dp,
                      color = if (selectedAvatar == i) GoldenYellowHighlight else Color.Transparent,
                      shape = CircleShape,
                    )
                    .clickable {
                      selectedAvatar = i
                      onUpdateProfile(nameDraft, i)
                    }
                    .padding(2.dp)
              ) {
                PlayerAvatarBadge(avatarId = i, sizeDp = 38)
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = nameDraft,
              onValueChange = { nameDraft = it },
              label = { Text("Username") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("player_name_input"),
            )
            ArcadeGradientButton(
              text = "SAVE",
              colors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
              textColor = DarkNavyBg,
              onClick = { onUpdateProfile(nameDraft, selectedAvatar) },
              modifier = Modifier.height(54.dp),
              testTag = "save_name_button",
            )
          }
        }
      }

      // Right: Clean Career Statistics Cards Grid
      Column(
        modifier = Modifier.weight(1.1f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ProfileStatCard("TOTAL DISTANCE", "%,d m".format(saveData.totalDistanceMeters), SkyCyan, Modifier.weight(1f))
          ProfileStatCard("TOTAL COINS", "%,d".format(saveData.totalCoinsEarned), GoldenYellowHighlight, Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ProfileStatCard("TOTAL RACES", "%,d".format(saveData.totalRaces), RacingOrangeAccent, Modifier.weight(1f))
          ProfileStatCard("TOTAL BACKFLIPS", "%,d".format(saveData.totalFlips), SuccessGreen, Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ProfileStatCard("VEHICLES UNLOCKED", "${saveData.unlockedVehicles.size} / 12", ElectricBlueSecondary, Modifier.weight(1f))
          ProfileStatCard("ACHIEVEMENTS", "$achievementsCompleted / $achievementsTotal", GoldenYellowHighlight, Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun ProfileStatCard(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = CardNavy,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    modifier = modifier.fillMaxHeight(),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(12.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
    }
  }
}
