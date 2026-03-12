package com.example.flexfi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.flexfi.ui.theme.FlexFiGradients
import com.example.flexfi.ui.theme.FlexFiWhite

@Composable
fun FlexFiFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(60.dp)
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(FlexFiGradients.fab),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = FlexFiWhite,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
