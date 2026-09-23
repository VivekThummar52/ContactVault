package com.codecraft.contactvault.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.codecraft.contactvault.ui.theme.ContactVaultTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SleekScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    sectionLetter: String? = null
) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size

    val isPreview = LocalInspectionMode.current
    if (!isPreview && (totalItems <= visibleItems || totalItems == 0)) return

    var isDragging by remember { mutableStateOf(false) }
    var isScrollActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState, isDragging) {
        snapshotFlow { listState.isScrollInProgress || isDragging }
            .collectLatest { active ->
                if (active) {
                    isScrollActive = true
                } else {
                    isScrollActive = true
                    delay(1200)
                    isScrollActive = false
                }
            }
    }

    val scrollFraction by remember(listState, totalItems, visibleItems) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex.toFloat()
            val maxScrollIndex = (totalItems - visibleItems).coerceAtLeast(1).toFloat()
            (firstVisible / maxScrollIndex).coerceIn(0f, 1f)
        }
    }

    AnimatedVisibility(
        visible = isPreview || listState.isScrollInProgress || isScrollActive || isDragging,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            val density = LocalDensity.current
            val badgeSize = 36.dp
            val badgeSizePx = with(density) { badgeSize.toPx() }
            val badgeOffsetPx = with(density) { 16.dp.toPx() }

            val trackHeightPx = if (constraints.hasBoundedHeight && constraints.maxHeight > 0) {
                constraints.maxHeight.toFloat()
            } else {
                layoutInfo.viewportSize.height.toFloat()
            }

            val thumbHeightPx = trackHeightPx * 0.15f
            val maxThumbOffset = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
            val thumbTopPx = scrollFraction * maxThumbOffset
            val thumbCenterYPx = thumbTopPx + (thumbHeightPx / 2f)

            val badgeTopPx = thumbCenterYPx - (badgeSizePx / 2f)

            // Remember updated state for values used inside pointerInput to prevent gesture cancellation on recomposition
            val currentTotalItems by rememberUpdatedState(totalItems)
            val currentVisibleItems by rememberUpdatedState(visibleItems)
            val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)
            val currentThumbHeightPx by rememberUpdatedState(thumbHeightPx)

            fun scrollToY(yPx: Float) {
                val total = currentTotalItems
                val visible = currentVisibleItems
                val trackH = currentTrackHeightPx
                val thumbH = currentThumbHeightPx
                val maxOffset = (trackH - thumbH).coerceAtLeast(0f)

                if (total <= 0) return
                val maxScrollIndex = (total - visible).coerceAtLeast(1)
                val fraction = if (maxOffset > 0f) {
                    ((yPx - thumbH / 2f) / maxOffset).coerceIn(0f, 1f)
                } else 0f
                val targetIndex = (fraction * maxScrollIndex).roundToInt().coerceIn(0, total - 1)
                coroutineScope.launch {
                    listState.scrollToItem(targetIndex)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isDragging = true
                            var currentY = down.position.y
                            scrollToY(currentY)

                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!pointer.pressed) break
                                val newY = pointer.position.y
                                if (newY != currentY) {
                                    currentY = newY
                                    pointer.consume()
                                    scrollToY(currentY)
                                }
                            }
                            isDragging = false
                        }
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .align(Alignment.CenterEnd)
                )

                // Scroll Indicator Thumb
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.15f)
                        .width(6.dp)
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(0, thumbTopPx.roundToInt())
                        }
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )

                // Current Section Letter Badge (Popup)
                if (!sectionLetter.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset {
                                IntOffset(
                                    x = -badgeOffsetPx.roundToInt(),
                                    y = badgeTopPx.roundToInt()
                                )
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.size(badgeSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sectionLetter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SleekScrollBarPreview() {
    ContactVaultTheme {
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .height(400.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(50) { index ->
                    Text(
                        text = "Item $index",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
            SleekScrollBar(
                listState = listState,
                sectionLetter = "A",
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
