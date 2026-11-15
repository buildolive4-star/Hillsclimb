package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.model.*
import com.example.ui.art.drawCartoonVehicle
import com.example.ui.components.ArcadeGradientButton
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.*

@Composable
fun ShopScreen(
  saveData: PlayerSaveData,
  onExchangeDiamondsForCoins: (Int, Int) -> Boolean,
  onExchangeCoinsForDiamonds: (Int, Int) -> Boolean,
  onWatchRewardCoins: () -> Unit,
  onBuyOrEquipSkin: (VehicleId, String) -> Boolean,
  onUnlockVehicle: (VehicleId) -> Boolean,
  onBuyBooster: (BoosterType) -> Boolean,
  onBack: () -> Unit,
) {
  var selectedCategory by remember { mutableStateOf(ShopCategory.BOOSTERS) }
  var statusToast by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "RACING SHOP",
      coins = saveData.coins,
      diamonds = saveData.diamonds,
      onBack = onBack,
    )

    // 5 Shop Categories: COINS | DIAMONDS | VEHICLE SKINS | SPECIAL VEHICLES | BOOSTERS
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ShopCategory.entries.forEach { cat ->
        FilterChip(
          selected = cat == selectedCategory,
          onClick = {
            selectedCategory = cat
            statusToast = null
          },
          label = { Text(cat.label, style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("shop_tab_${cat.name.lowercase()}"),
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      if (statusToast != null) {
        Text(
          text = statusToast.orEmpty(),
          style = MaterialTheme.typography.labelLarge,
          color = SuccessGreen,
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (selectedCategory) {
        ShopCategory.COINS -> {
          item {
            ShopItemRow(
              icon = Icons.Filled.PlayCircle,
              iconColor = SuccessGreen,
              title = "Watch Bonus Clip",
              subtitle = "Earn +250 Coins immediately (optional reward)",
              actionLabel = "CLAIM +250",
              actionColors = listOf(SuccessGreen, EmeraldDark),
              enabled = true,
              onAction = {
                onWatchRewardCoins()
                statusToast = "+250 Coins added!"
              },
            )
          }
          val coinPacks = listOf(5 to 1200, 12 to 3200, 25 to 7500)
          items(coinPacks) { (gemsCost, coinsReward) ->
            ShopItemRow(
              icon = Icons.Filled.MonetizationOn,
              iconColor = GoldenYellowHighlight,
              title = "%,d Coins Pouch".format(coinsReward),
              subtitle = "Convert Diamonds into Garage upgrade Coins",
              actionLabel = "$gemsCost ♦",
              actionColors = listOf(GoldenYellowHighlight, RacingOrangeAccent),
              enabled = saveData.diamonds >= gemsCost,
              onAction = {
                if (onExchangeDiamondsForCoins(gemsCost, coinsReward)) {
                  statusToast = "+%,d Coins!".format(coinsReward)
                }
              },
            )
          }
        }
        ShopCategory.DIAMONDS -> {
          val gemPacks = listOf(1000 to 5, 2500 to 15, 6000 to 40)
          items(gemPacks) { (coinsCost, gemsReward) ->
            ShopItemRow(
              icon = Icons.Filled.Diamond,
              iconColor = DiamondCyan,
              title = "$gemsReward Diamonds Pack",
              subtitle = "Trade earned race Coins for rare Diamonds",
              actionLabel = "%,d COINS".format(coinsCost),
              actionColors = listOf(ElectricBlueSecondary, SkyCyan),
              enabled = saveData.coins >= coinsCost,
              onAction = {
                if (onExchangeCoinsForDiamonds(coinsCost, gemsReward)) {
                  statusToast = "+$gemsReward Diamonds!"
                }
              },
            )
          }
        }
        ShopCategory.SKINS -> {
          items(GameCatalog.skins) { skin ->
            val owned = skin.id in saveData.ownedSkins
            val eq = (saveData.equippedSkinByVehicle[saveData.selectedVehicleId] ?: "default") == skin.id
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = CardNavy,
              border = BorderStroke(1.5.dp, if (eq) GoldenYellowHighlight else Color.White.copy(alpha = 0.1f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(skin.primaryColor), Color(skin.secondaryColor))))
                  )
                  Column {
                    Text(skin.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                      text = "Applies to ${GameCatalog.vehicleById(saveData.selectedVehicleId).name}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextSecondary,
                    )
                  }
                }
                ArcadeGradientButton(
                  text =
                    when {
                      eq -> "EQUIPPED"
                      owned -> "EQUIP"
                      else -> "${skin.costDiamonds} ♦"
                    },
                  colors =
                    when {
                      eq -> listOf(SuccessGreen, EmeraldDark)
                      owned -> listOf(ElectricBlueSecondary, Color(0xFF1A4FD6))
                      else -> listOf(GoldenYellowHighlight, RacingOrangeAccent)
                    },
                  textColor = if (eq || !owned) DarkNavyBg else TextPrimary,
                  enabled = owned || saveData.diamonds >= skin.costDiamonds,
                  onClick = {
                    if (onBuyOrEquipSkin(saveData.selectedVehicleId, skin.id)) {
                      statusToast = "${skin.name} equipped!"
                    }
                  },
                )
              }
            }
          }
        }
        ShopCategory.VEHICLES -> {
          val specialVehicles = GameCatalog.vehicles.filter { it.unlockCostDiamonds > 0 || it.unlockCostCoins >= 9500 }
          items(specialVehicles) { v ->
            val unlocked = v.id in saveData.unlockedVehicles
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = CardNavy,
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Canvas(modifier = Modifier.size(width = 96.dp, height = 52.dp)) {
                    drawCartoonVehicle(
                      spec = v,
                      centerPx = Offset(size.width * 0.5f, size.height * 0.58f),
                      pixelsPerMeter = 15f,
                      angleRad = 0f,
                      rearWheelAngleRad = 0f,
                      frontWheelAngleRad = 0f,
                      rearCompressionM = 0f,
                      frontCompressionM = 0f,
                    )
                  }
                  Column {
                    Text(v.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(v.tagline, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                  }
                }
                ArcadeGradientButton(
                  text =
                    if (unlocked) "OWNED"
                    else if (v.unlockCostDiamonds > 0) "%,d + %d♦".format(v.unlockCostCoins, v.unlockCostDiamonds)
                    else "%,d COINS".format(v.unlockCostCoins),
                  colors =
                    if (unlocked) listOf(SuccessGreen, EmeraldDark)
                    else listOf(GoldenYellowHighlight, RacingOrangeAccent),
                  textColor = DarkNavyBg,
                  enabled =
                    !unlocked &&
                      saveData.coins >= v.unlockCostCoins &&
                      saveData.diamonds >= v.unlockCostDiamonds,
                  onClick = {
                    if (onUnlockVehicle(v.id)) statusToast = "${v.name} unlocked!"
                  },
                )
              }
            }
          }
        }
        ShopCategory.BOOSTERS -> {
          items(BoosterType.entries) { booster ->
            val ownedCount = saveData.boosterCount(booster)
            ShopItemRow(
              icon =
                when (booster) {
                  BoosterType.FUEL_BOOST -> Icons.Filled.LocalGasStation
                  BoosterType.NITRO_PACK -> Icons.Filled.Bolt
                  BoosterType.MAGNET_COINS -> Icons.Filled.TrackChanges
                },
              iconColor = RacingOrangeAccent,
              title = "${booster.title} (Owned: $ownedCount)",
              subtitle = booster.desc,
              actionLabel = "${booster.costCoins} COINS",
              actionColors = listOf(RacingOrangeAccent, GoldenYellowHighlight),
              enabled = saveData.coins >= booster.costCoins,
              onAction = {
                if (onBuyBooster(booster)) statusToast = "${booster.title} ready for next race!"
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ShopItemRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconColor: Color,
  title: String,
  subtitle: String,
  actionLabel: String,
  actionColors: List<Color>,
  enabled: Boolean,
  onAction: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = CardNavy,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier.size(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MidnightBluePrimary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(26.dp))
        }
        Column {
          Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
          Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
      }
      ArcadeGradientButton(
        text = actionLabel,
        colors = actionColors,
        textColor = DarkNavyBg,
        enabled = enabled,
        onClick = onAction,
      )
    }
  }
}
