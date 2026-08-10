package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.RemoteKey
import com.example.ui.theme.TvPrimaryCyan

@Composable
fun DPadControl(
    onKeyClick: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("dpad_controller")
    ) {
        // D-Pad Outer Ring Canvas / Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        // UP Button
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            onClick = { onKeyClick(RemoteKey.DPAD_UP) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .testTag("dpad_up")
        )

        // DOWN Button
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            onClick = { onKeyClick(RemoteKey.DPAD_DOWN) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .testTag("dpad_down")
        )

        // LEFT Button
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Left",
            onClick = { onKeyClick(RemoteKey.DPAD_LEFT) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .testTag("dpad_left")
        )

        // RIGHT Button
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowRight,
            contentDescription = "Right",
            onClick = { onKeyClick(RemoteKey.DPAD_RIGHT) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .testTag("dpad_right")
        )

        // CENTER OK / SELECT Button
        DPadCenterSelectButton(
            onClick = { onKeyClick(RemoteKey.SELECT) },
            modifier = Modifier.testTag("dpad_ok")
        )
    }
}

@Composable
private fun DPadDirectionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1.0f, label = "buttonScale")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isPressed) TvPrimaryCyan.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) TvPrimaryCyan else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun DPadCenterSelectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1.0f, label = "okScale")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .size(86.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(TvPrimaryCyan, TvPrimaryCyan.copy(alpha = 0.8f))
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = "OK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 20.sp
        )
    }
}
