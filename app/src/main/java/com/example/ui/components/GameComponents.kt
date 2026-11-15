package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UpgradeType
import com.example.ui.theme.*

@Composable
fun ArcadeGradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  colors: List<Color> = listOf(RacingOrangeAccent, SaffronDeep),
  textColor: Color = Color.White,
  enabled: Boolean = true,
  badgeCount: Int = 0,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed && enabled) 0.94f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "btn_scale",
    )

  Box(
    modifier =
      modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .scale(scale)
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .shadow(elevation = if (enabled) 8.dp else 0.dp, shape = RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .background(
          brush =
            Brush.horizontalGradient(
              colors =
                if (enabled) colors
                else listOf(Color(0xFF283552), Color(0xFF1B2740))
            )
        )
        .border(
          width = 1.5.dp,
          color = Color.White.copy(alpha = if (enabled) 0.38f else 0.10f),
          shape = RoundedCornerShape(16.dp),
        )
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = Color.White),
          enabled = enabled,
          onClick = onClick,
        )
        .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) textColor else TextSecondary,
          modifier = Modifier.size(20.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) textColor else TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (badgeCount > 0) {
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .offset(x = 6.dp, y = (-4).dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(DangerRed)
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = badgeCount.toString(),
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
  }
}

@Composable
fun GlowingStartRacingButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "play_button",
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.93f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "start_scale",
    )
  val infinite = rememberInfiniteTransition(label = "glow_pulse")
  val glowAlpha by
    infinite.animateFloat(
      initialValue = 0.45f,
      targetValue = 0.95f,
      animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "glow_alpha",
    )

  Box(
    modifier =
      modifier
        .testTag(testTag)
        .scale(scale)
        .defaultMinSize(minWidth = 180.dp, minHeight = 58.dp)
        .shadow(
          elevation = 16.dp,
          shape = RoundedCornerShape(20.dp),
          ambientColor = RacingOrangeAccent,
          spotColor = GoldenYellowHighlight,
        )
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.horizontalGradient(
            listOf(RacingOrangeAccent, Color(0xFFFFA826), GoldenYellowHighlight)
          )
        )
        .border(
          width = 2.5.dp,
          color = Color.White.copy(alpha = glowAlpha),
          shape = RoundedCornerShape(20.dp),
        )
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = Color.White),
          onClick = onClick,
        )
        .padding(horizontal = 24.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = null,
        tint = DarkNavyBg,
        modifier = Modifier.size(30.dp),
      )
      Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = DarkNavyBg,
        fontWeight = FontWeight.ExtraBold,
      )
    }
  }
}

@Composable
fun CurrencyChipsRow(coins: Int, diamonds: Int, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(50),
      color = MidnightBluePrimary.copy(alpha = 0.9f),
      border = BorderStroke(1.5.dp, GoldenYellowHighlight.copy(alpha = 0.75f)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.MonetizationOn,
          contentDescription = "Coins",
          tint = GoldenYellowHighlight,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "%,d".format(coins),
          style = MaterialTheme.typography.labelLarge,
          color = GoldenYellowHighlight,
          modifier = Modifier.testTag("coin_balance_text"),
        )
      }
    }
    Surface(
      shape = RoundedCornerShape(50),
      color = MidnightBluePrimary.copy(alpha = 0.9f),
      border = BorderStroke(1.5.dp, DiamondCyan.copy(alpha = 0.75f)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Diamond,
          contentDescription = "Diamonds",
          tint = DiamondCyan,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "%,d".format(diamonds),
          style = MaterialTheme.typography.labelLarge,
          color = DiamondCyan,
          modifier = Modifier.testTag("diamond_balance_text"),
        )
      }
    }
  }
}

@Composable
fun ScreenTopBar(
  title: String,
  coins: Int,
  diamonds: Int,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      FilledIconButton(
        onClick = onBack,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardNavy),
        modifier =
          Modifier.size(48.dp)
            .border(1.dp, ElectricBlueSecondary.copy(alpha = 0.5f), CircleShape)
            .testTag("back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = TextPrimary,
        )
      }
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
      )
    }
    CurrencyChipsRow(coins = coins, diamonds = diamonds)
  }
}

@Composable
fun StatProgressRow(
  label: String,
  fraction: Float,
  color: Color = RacingOrangeAccent,
  valueText: String? = null,
) {
  val animatedFraction by
    animateFloatAsState(
      targetValue = fraction.coerceIn(0.04f, 1f),
      animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
      label = "stat_anim",
    )

  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
      if (valueText != null) {
        Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
      }
    }
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(50))
          .background(MidnightBluePrimary)
          .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(animatedFraction)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.78f), color)))
      )
    }
  }
}

@Composable
fun PlayerAvatarBadge(
  avatarId: Int,
  modifier: Modifier = Modifier,
  sizeDp: Int = 40,
) {
  val (icon, bgColors) =
    when (avatarId % 6) {
      0 -> Icons.Filled.SportsMotorsports to listOf(RacingOrangeAccent, GoldenYellowHighlight)
      1 -> Icons.Filled.FlashOn to listOf(ElectricBlueSecondary, SkyCyan)
      2 -> Icons.Filled.EmojiEvents to listOf(GoldenYellowHighlight, GoldCoinDark)
      3 -> Icons.Filled.Whatshot to listOf(DangerRed, RacingOrangeAccent)
      4 -> Icons.Filled.Shield to listOf(SuccessGreen, EmeraldDark)
      else -> Icons.Filled.RocketLaunch to listOf(Color(0xFF8B5CF6), ElectricBlueSecondary)
    }
  Box(
    modifier =
      modifier
        .size(sizeDp.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(bgColors))
        .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = "Player Avatar",
      tint = DarkNavyBg,
      modifier = Modifier.size((sizeDp * 0.58f).dp),
    )
  }
}

fun upgradeIconFor(type: UpgradeType): ImageVector =
  when (type) {
    UpgradeType.ENGINE -> Icons.Filled.Speed
    UpgradeType.SUSPENSION -> Icons.Filled.UnfoldMore
    UpgradeType.TIRES -> Icons.Filled.Album
    UpgradeType.FUEL_TANK -> Icons.Filled.LocalGasStation
    UpgradeType.TURBO -> Icons.Filled.Bolt
    UpgradeType.CHASSIS -> Icons.Filled.Security
    UpgradeType.BRAKES -> Icons.Filled.PanTool
    UpgradeType.AIR_CONTROL -> Icons.Filled.Air
  }

enum class BottomNavTab(val label: String, val icon: ImageVector) {
  HOME("HOME", Icons.Filled.Home),
  GARAGE("GARAGE", Icons.Filled.Garage),
  MAPS("MAPS", Icons.Filled.Map),
  MISSIONS("MISSIONS", Icons.Filled.EmojiEvents),
  SHOP("SHOP", Icons.Filled.ShoppingBag),
}

@Composable
fun PremiumBottomNavDock(
  currentTab: BottomNavTab,
  missionBadge: Int,
  onSelectTab: (BottomNavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MidnightBluePrimary.copy(alpha = 0.94f),
    border = BorderStroke(1.5.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
    shadowElevation = 12.dp,
    modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BottomNavTab.entries.forEach { tab ->
        val selected = tab == currentTab
        Box(
          modifier =
            Modifier.defaultMinSize(minWidth = 76.dp, minHeight = 48.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (selected)
                  Brush.horizontalGradient(listOf(ElectricBlueSecondary, Color(0xFF1D4ED8)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
              )
              .clickable { onSelectTab(tab) }
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("nav_${tab.name.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.label,
              tint = if (selected) TextPrimary else TextSecondary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) TextPrimary else TextSecondary,
            )
          }
          if (tab == BottomNavTab.MISSIONS && missionBadge > 0) {
            Box(
              modifier =
                Modifier.align(Alignment.TopEnd)
                  .offset(x = 4.dp, y = (-2).dp)
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(DangerRed),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = missionBadge.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }
    }
  }
}
