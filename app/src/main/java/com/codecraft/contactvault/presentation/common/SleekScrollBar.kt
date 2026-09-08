package com.codecraft.contactvault.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.codecraft.contactvault.ui.theme.ContactVaultTheme
import kotlinx.coroutines.delay
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

    if (totalItems <= visibleItems || totalItems == 0) return

    var isScrollActive by remember { mutableStateOf(false) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        isScrollActive = true
        delay(1200)
        isScrollActive = false
    }

    val scrollFraction by remember(listState) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex.toFloat()
            val maxScrollIndex = (totalItems - visibleItems).coerceAtLeast(1).toFloat()
            (firstVisible / maxScrollIndex).coerceIn(0f, 1f)
        }
    }

    AnimatedVisibility(
        visible = listState.isScrollInProgress || isScrollActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp),
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
                            val availableHeight = layoutInfo.viewportSize.height * 0.8f
                            val yOffset = (scrollFraction * availableHeight).roundToInt()
                            IntOffset(0, yOffset)
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
                                val availableHeight = layoutInfo.viewportSize.height * 0.8f
                                val yOffset = (scrollFraction * availableHeight).roundToInt()
                                IntOffset(-48, yOffset - 12)
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
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
