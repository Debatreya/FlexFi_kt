package com.example.flexfi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.theme.*
import coil.compose.AsyncImage

enum class AvatarSize(val sizeDp: Dp, val fontSize: Int) {
    SMALL(40.dp, 14),
    MEDIUM(56.dp, 20),
    LARGE(80.dp, 28)
}

private val pastelColors = listOf(
    Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB),
    Color(0xFFF8BBD0), Color(0xFFD1C4E9), Color(0xFFFFE0B2),
    Color(0xFFB2EBF2), Color(0xFFF0F4C3), Color(0xFFE1BEE7),
    Color(0xFFFFECB3)
)

@Composable
fun FlexFiAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: AvatarSize = AvatarSize.MEDIUM,
    showOnlineDot: Boolean = false,
    isGhost: Boolean = false,
    borderColor: Color? = null
) {
    val bgColor = if (isGhost) FlexFiGreySurface
                  else pastelColors[kotlin.math.abs(name.hashCode()) % pastelColors.size]
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(modifier = modifier.size(size.sizeDp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bgColor)
                .then(
                    if (borderColor != null) Modifier.border(2.dp, borderColor, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isGhost) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = FlexFiLightText,
                    modifier = Modifier.size(size.sizeDp * 0.5f)
                )
            } else {
                Text(
                    text = initials,
                    fontSize = size.fontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlexFiDarkText
                )
            }
        }

        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .size(size.sizeDp * 0.25f)
                    .align(Alignment.BottomStart)
                    .clip(CircleShape)
                    .background(FlexFiGreen)
                    .border(2.dp, FlexFiWhite, CircleShape)
            )
        }
    }
}
