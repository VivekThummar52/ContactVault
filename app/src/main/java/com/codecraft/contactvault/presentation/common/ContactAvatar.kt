package com.codecraft.contactvault.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

@Composable
fun ContactAvatar(
    displayName: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNull_OrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Photo of $displayName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val backgroundColor = getAvatarColor(displayName)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45f).sp
                )
            }
        }
    }
}

private fun String?.isNull_OrEmpty(): Boolean = this == null || this.trim().isEmpty()

private fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA),
        Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF1E88E5),
        Color(0xFF039BE5), Color(0xFF00ACC1), Color(0xFF00897B),
        Color(0xFF43A047), Color(0xFF7CB342), Color(0xFFC0CA33),
        Color(0xFFFDD835), Color(0xFFFB8C00), Color(0xFFF4511E)
    )
    val hash = kotlin.math.abs(name.hashCode())
    return colors[hash % colors.size]
}

@Preview(showBackground = true)
@Composable
fun ContactAvatarInitialsPreview() {
    ContactVaultTheme {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ContactAvatar(displayName = "Alice Smith", photoUri = null, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            ContactAvatar(displayName = "Bob Jones", photoUri = null, size = 64.dp)
        }
    }
}
