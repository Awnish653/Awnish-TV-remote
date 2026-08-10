package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TvErrorContainer
import com.example.ui.theme.TvErrorRed
import com.example.ui.theme.TvPrimaryCyan

enum class RemoteButtonStyle {
    STANDARD,
    ACCENT,
    POWER
}

@Composable
fun RemoteButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RemoteButtonStyle = RemoteButtonStyle.STANDARD,
    size: Dp = 60.dp,
    testTag: String = "remote_btn_$label"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1.0f, label = "buttonScale")

    val bgContainerColor = when (style) {
        RemoteButtonStyle.POWER -> if (isPressed) TvErrorRed else TvErrorContainer
        RemoteButtonStyle.ACCENT -> if (isPressed) TvPrimaryCyan else TvPrimaryCyan.copy(alpha = 0.2f)
        RemoteButtonStyle.STANDARD -> if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    }

    val iconTintColor = when (style) {
        RemoteButtonStyle.POWER -> if (isPressed) Color.White else TvErrorRed
        RemoteButtonStyle.ACCENT -> if (isPressed) Color.Black else TvPrimaryCyan
        RemoteButtonStyle.STANDARD -> if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(scale)
                .size(size)
                .clip(CircleShape)
                .background(bgContainerColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTintColor,
                modifier = Modifier.size(size * 0.45f)
            )
        }
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RockerButton(
    plusLabel: String,
    minusLabel: String,
    title: String,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
    modifier: Modifier = Modifier,
    plusIcon: ImageVector,
    minusIcon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(130.dp)
            ) {
                RemoteButton(
                    icon = plusIcon,
                    label = "",
                    onClick = onPlusClick,
                    size = 56.dp,
                    testTag = "rocker_plus_$title"
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                RemoteButton(
                    icon = minusIcon,
                    label = "",
                    onClick = onMinusClick,
                    size = 56.dp,
                    testTag = "rocker_minus_$title"
                )
            }
        }
    }
}
