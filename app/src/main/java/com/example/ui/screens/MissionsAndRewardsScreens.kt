package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.model.*
import com.example.ui.components.ArcadeGradientButton
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.*

@Composable
fun MissionsScreen(
  saveData: PlayerSaveData,
  onClaimMission: (String) -> Boolean,
  onBack: () -> Unit,
) {
  var selectedPeriod by remember { mutableStateOf(MissionPeriod.DAILY) }
  val visibleMissions =
    remember(selectedPeriod) { GameCatalog.missions.filter { it.period == selectedPeriod } }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "MISSION DASHBOARD",
      coins = saveData.coins,
      diamonds = saveData.diamonds,
      onBack = onBack,
    )

    // 3 Requested Categories: DAILY MISSIONS | WEEKLY MISSIONS | ACHIEVEMENTS
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      MissionPeriod.entries.forEach { period ->
        val selected = period == selectedPeriod
        FilterChip(
          selected = selected,
          onClick = { selectedPeriod = period },
          label = { Text(period.label, style = MaterialTheme.typography.labelLarge) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        )
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(visibleMissions) { m ->
        val progress = (saveData.missionProgress[m.id] ?: 0).coerceAtMost(m.target)
        val claimed = m.id in saveData.claimedMissions
        val ready = !claimed && progress >= m.target
        val frac by
          animateFloatAsState(
            targetValue = (progress.toFloat() / m.target.coerceAtLeast(1)).coerceIn(0f, 1f),
            animationSpec = tween(500),
            label = "mission_prog",
          )

        val icon =
          when (m.metric) {
            MissionMetric.TOTAL_BACKFLIPS, MissionMetric.TOTAL_FRONTFLIPS -> Icons.Filled.Loop
            MissionMetric.TOTAL_COINS -> Icons.Filled.MonetizationOn
            MissionMetric.FUEL_CANS_COLLECTED -> Icons.Filled.LocalGasStation
            MissionMetric.MAX_ENGINE_LEVEL -> Icons.Filled.Build
            MissionMetric.UNLOCKED_VEHICLES -> Icons.Filled.DirectionsCar
            else -> Icons.Filled.EmojiEvents
          }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = CardNavy,
          border =
            BorderStroke(
              width = if (ready) 2.dp else 1.dp,
              color =
                when {
                  claimed -> SuccessGreen
                  ready -> GoldenYellowHighlight
                  else -> Color.White.copy(alpha = 0.10f)
                },
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier.size(46.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    if (claimed || ready) SuccessGreen.copy(alpha = 0.18f) else MidnightBluePrimary
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (claimed) Icons.Filled.CheckCircle else icon,
                contentDescription = m.title,
                tint = if (claimed) SuccessGreen else GoldenYellowHighlight,
                modifier = Modifier.size(26.dp),
              )
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(m.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Text(m.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .height(8.dp)
                      .clip(RoundedCornerShape(50))
                      .background(MidnightBluePrimary)
                ) {
                  Box(
                    modifier =
                      Modifier.fillMaxHeight()
                        .fillMaxWidth(frac)
                        .clip(RoundedCornerShape(50))
                        .background(if (ready || claimed) SuccessGreen else RacingOrangeAccent)
                  )
                }
                Text(
                  text = "$progress / ${m.target}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextPrimary,
                )
              }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "+${m.rewardCoins} Coins • +${m.rewardDiamonds}♦",
                style = MaterialTheme.typography.labelSmall,
                color = GoldenYellowHighlight,
              )
              if (claimed) {
                Surface(shape = RoundedCornerShape(10.dp), color = EmeraldDark.copy(alpha = 0.55f)) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Text("COMPLETED", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                  }
                }
              } else {
                ArcadeGradientButton(
                  text = if (ready) "CLAIM" else "ACTIVE",
                  colors =
                    if (ready) listOf(SuccessGreen, EmeraldDark)
                    else listOf(CardNavyElevated, MidnightBluePrimary),
                  textColor = if (ready) DarkNavyBg else TextSecondary,
                  enabled = ready,
                  onClick = { onClaimMission(m.id) },
                  modifier = Modifier.width(118.dp),
                  testTag = "claim_mission_${m.id}",
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DailyRewardsScreen(
  saveData: PlayerSaveData,
  canClaimToday: Boolean,
  onClaimToday: () -> DailyRewardSpec?,
  onBack: () -> Unit,
) {
  var claimedPopup by remember { mutableStateOf<DailyRewardSpec?>(null) }
  val activeDayIndex = saveData.dailyStreakDay % 7

  val glowPulse = rememberInfiniteTransition(label = "daily_glow")
  val glowScale by
    glowPulse.animateFloat(
      initialValue = 1f,
      targetValue = 1.04f,
      animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
      label = "daily_scale",
    )

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkNavyBg, MidnightBluePrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "7-DAY DAILY REWARDS",
      coins = saveData.coins,
      diamonds = saveData.diamonds,
      onBack = onBack,
    )

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text =
          if (canClaimToday) "Your Day ${activeDayIndex + 1} reward is unlocked! Tap CLAIM REWARD below."
          else "Today's gift is already claimed. Come back tomorrow to continue your streak!",
        style = MaterialTheme.typography.bodyLarge,
        color = if (canClaimToday) SuccessGreen else TextSecondary,
      )

      Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GameCatalog.dailyRewards.forEachIndexed { idx, reward ->
          val isPastClaimed = idx < saveData.dailyStreakDay && !(canClaimToday && idx == activeDayIndex)
          val isToday = canClaimToday && idx == activeDayIndex
          Surface(
            shape = RoundedCornerShape(18.dp),
            color =
              when {
                isToday -> MidnightBluePrimary
                isPastClaimed -> EmeraldDark.copy(alpha = 0.28f)
                else -> CardNavy
              },
            border =
              BorderStroke(
                width = if (isToday) 2.5.dp else 1.dp,
                color =
                  when {
                    isToday -> GoldenYellowHighlight
                    isPastClaimed -> SuccessGreen
                    reward.isSpecial -> DiamondCyan
                    else -> Color.White.copy(alpha = 0.12f)
                  },
              ),
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .then(if (isToday) Modifier.scale(glowScale) else Modifier),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().padding(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "DAY ${reward.day}",
                style = MaterialTheme.typography.labelLarge,
                color = if (isToday) GoldenYellowHighlight else TextPrimary,
              )
              Icon(
                imageVector =
                  when {
                    isPastClaimed -> Icons.Filled.CheckCircle
                    reward.isSpecial -> Icons.Filled.WorkspacePremium
                    else -> Icons.Filled.CardGiftcard
                  },
                contentDescription = reward.title,
                tint =
                  when {
                    isPastClaimed -> SuccessGreen
                    reward.isSpecial -> DiamondCyan
                    else -> GoldenYellowHighlight
                  },
                modifier = Modifier.size(36.dp),
              )
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "${reward.coins} Coins",
                  style = MaterialTheme.typography.labelMedium,
                  color = GoldenYellowHighlight,
                )
                if (reward.diamonds > 0) {
                  Text(
                    text = "+${reward.diamonds} Gems",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiamondCyan,
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color =
                  when {
                    isPastClaimed -> SuccessGreen.copy(alpha = 0.2f)
                    isToday -> GoldenYellowHighlight
                    else -> DarkNavyBg
                  },
              ) {
                Text(
                  text =
                    when {
                      isPastClaimed -> "CLAIMED"
                      isToday -> "UNLOCKED"
                      else -> "LOCKED"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    when {
                      isPastClaimed -> SuccessGreen
                      isToday -> DarkNavyBg
                      else -> TextSecondary
                    },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }
        }
      }

      ArcadeGradientButton(
        text = if (canClaimToday) "CLAIM REWARD (DAY ${activeDayIndex + 1})" else "CLAIMED TODAY",
        icon = if (canClaimToday) Icons.Filled.CardGiftcard else Icons.Filled.CheckCircle,
        colors =
          if (canClaimToday) listOf(RacingOrangeAccent, GoldenYellowHighlight)
          else listOf(CardNavyElevated, MidnightBluePrimary),
        textColor = if (canClaimToday) DarkNavyBg else TextSecondary,
        enabled = canClaimToday,
        onClick = {
          val claimed = onClaimToday()
          if (claimed != null) claimedPopup = claimed
        },
        modifier = Modifier.fillMaxWidth().height(54.dp),
        testTag = "claim_daily_reward_button",
      )
    }

    val popup = claimedPopup
    if (popup != null) {
      AlertDialog(
        onDismissRequest = { claimedPopup = null },
        title = { Text("${popup.title} Claimed!") },
        text = {
          Text(
            "You received ${popup.coins} Coins" +
              (if (popup.diamonds > 0) " and ${popup.diamonds} Diamonds!" else "!")
          )
        },
        confirmButton = { Button(onClick = { claimedPopup = null }) { Text("AWESOME!") } },
      )
    }
  }
}
