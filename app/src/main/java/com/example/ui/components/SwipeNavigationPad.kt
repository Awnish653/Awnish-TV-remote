package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.models.RemoteKey
import kotlin.math.abs

/**
 * Gesture pad for TVs whose remote protocol exposes directional key events.
 * It is intentionally described as navigation, not a real hardware mouse:
 * pointer events require a TV protocol that supports them.
 */
@Composable
fun SwipeNavigationPad(
    speed: Int,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(174.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp))
            .pointerInput(speed) {
                var accumulatedX = 0f
                var accumulatedY = 0f
                var sent = 0
                val threshold = (42f / speed.coerceIn(1, 5)).coerceAtLeast(10f)
                detectDragGestures(
                    onDragStart = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                        sent = 0
                    },
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedX += dragAmount.x
                        accumulatedY += dragAmount.y
                        while (sent < 24 && (abs(accumulatedX) >= threshold || abs(accumulatedY) >= threshold)) {
                            if (abs(accumulatedX) >= abs(accumulatedY)) {
                                onKey(if (accumulatedX > 0) RemoteKey.DPAD_RIGHT else RemoteKey.DPAD_LEFT)
                                accumulatedX += if (accumulatedX > 0) -threshold else threshold
                            } else {
                                onKey(if (accumulatedY > 0) RemoteKey.DPAD_DOWN else RemoteKey.DPAD_UP)
                                accumulatedY += if (accumulatedY > 0) -threshold else threshold
                            }
                            sent++
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onKey(RemoteKey.SELECT) })
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Swipe to navigate · Tap to select", style = MaterialTheme.typography.titleSmall)
            Text("Speed $speed× · TV cursor support varies", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
