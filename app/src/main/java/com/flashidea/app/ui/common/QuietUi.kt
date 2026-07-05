package com.flashidea.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuietBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val sheenAlpha = if (dark) 0.08f else 0.24f
    val edgeAlpha = if (dark) 0.16f else 0.18f
    val primaryGlow = if (dark) 0.13f else 0.10f
    val tertiaryGlow = if (dark) 0.10f else 0.07f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surfaceContainerLow,
                        colors.background,
                        colors.surfaceContainer,
                        colors.surfaceContainerLowest
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.primary.copy(alpha = primaryGlow),
                            colors.background.copy(alpha = 0f)
                        ),
                        center = Offset(120f, 80f),
                        radius = 860f
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.tertiary.copy(alpha = tertiaryGlow),
                            Color.Transparent
                        ),
                        center = Offset(940f, 1300f),
                        radius = 980f
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = sheenAlpha),
                            Color.Transparent,
                            colors.secondary.copy(alpha = edgeAlpha)
                        ),
                        start = Offset.Zero,
                        end = Offset(1040f, 1380f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            colors.onSurface.copy(alpha = if (dark) 0.06f else 0.035f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 460f),
                        end = Offset(980f, 940f)
                    )
                )
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            val lineColor = colors.onSurface.copy(alpha = if (dark) 0.026f else 0.032f)
            val grainColor = colors.onSurface.copy(alpha = if (dark) 0.030f else 0.024f)
            var y = -size.width * 0.1f
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y + size.width * 0.12f),
                    strokeWidth = 0.55f
                )
                y += 24f
            }
            var gx = 8f
            var xi = 0
            while (gx < size.width) {
                var gy = 10f
                var yi = 0
                while (gy < size.height) {
                    if ((xi * 17 + yi * 11) % 7 == 0) {
                        drawCircle(
                            color = grainColor,
                            radius = 0.72f,
                            center = Offset(gx + ((yi % 3) * 2.1f), gy)
                        )
                    }
                    gy += 20f
                    yi++
                }
                gx += 18f
                xi++
            }
        }
        content()
    }
}

@Composable
fun MetallicBrandMark(
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val brush = Brush.linearGradient(
        colors = if (dark) {
            listOf(
                Color(0xFFF4FBFF),
                Color(0xFF9FD6E8),
                Color(0xFF6E8797),
                Color(0xFFEAF7FC)
            )
        } else {
            listOf(
                Color(0xFF0F1720),
                Color(0xFF5C7485),
                Color(0xFFAEC2CF),
                Color(0xFF20303A)
            )
        },
        start = Offset.Zero,
        end = Offset(360f, 92f)
    )
    Box(modifier = modifier) {
        Text(
            text = "Flash Idea",
            maxLines = 1,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface.copy(alpha = if (dark) 0.28f else 0.18f),
                shadow = Shadow(
                    color = colors.onSurface.copy(alpha = if (dark) 0.42f else 0.16f),
                    offset = Offset(0f, 2.4f),
                    blurRadius = 5.5f
                )
            )
        )
        Text(
            text = "Flash Idea",
            maxLines = 1,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                brush = brush,
                shadow = Shadow(
                    color = colors.primary.copy(alpha = if (dark) 0.22f else 0.16f),
                    offset = Offset(0f, 0.8f),
                    blurRadius = 2.6f
                )
            )
        )
        Text(
            text = "Flash Idea",
            maxLines = 1,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = if (dark) 0.13f else 0.20f)
            )
        )
    }
}

@Composable
fun QuietIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Surface(
        modifier = modifier
            .size(42.dp)
            .semantics { role = Role.Button }
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f)),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun QuietPill(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun QuietRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier.padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuietDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
