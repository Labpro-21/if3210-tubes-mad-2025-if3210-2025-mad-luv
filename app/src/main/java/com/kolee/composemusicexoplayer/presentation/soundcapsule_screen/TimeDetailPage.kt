package com.kolee.composemusicexoplayer.presentation.soundcapsule_screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.composemusicexoplayer.data.model.DailyListeningTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun TimeDetailPage(
    dailyStats: List<DailyListeningTime>,
    onBackClick: () -> Unit
) {
    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    // Calculate total minutes from daily stats
    val totalMinutes = dailyStats.sumOf { (it.dailyDuration / 1000 / 60).toInt() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        TopAppBar(
            backgroundColor = Color.Black,
            elevation = 0.dp
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Time listened",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Month Title
            Text(
                text = currentMonth,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Main Statistics
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You listened to music for ",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalMinutes minutes",
                    fontSize = 20.sp,
                    color = Color(0xFF1DB954),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " this month.",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Average
            val dailyAverage = if (dailyStats.isNotEmpty()) {
                totalMinutes / dailyStats.size
            } else 0

            Text(
                text = "Daily average: $dailyAverage min",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color(0xFF1A1A1A)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Daily Chart",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (dailyStats.isNotEmpty()) {
                        DailyChart(
                            dailyStats = dailyStats,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyChart(
    dailyStats: List<DailyListeningTime>,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = maxHeight

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (dailyStats.isEmpty()) return@detectTapGestures

                        val padding = 40.dp.toPx()
                        val chartWidth = size.width - padding * 2
                        val stepX = if (dailyStats.size > 1) chartWidth / (dailyStats.size - 1) else 0f

                        // Find closest point
                        var closestIndex = -1
                        var minDistance = Float.MAX_VALUE

                        dailyStats.forEachIndexed { index, stat ->
                            val x = padding + index * stepX
                            val distance = kotlin.math.abs(offset.x - x)
                            if (distance < minDistance && distance < 30.dp.toPx()) {
                                minDistance = distance
                                closestIndex = index
                            }
                        }

                        if (closestIndex != -1) {
                            val minutes = (dailyStats[closestIndex].dailyDuration / 1000 / 60).toInt()
                            selectedPoint = Pair(closestIndex, minutes)
                        } else {
                            selectedPoint = null
                        }
                    }
                }
        ) {
            if (dailyStats.isEmpty()) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 40.dp.toPx() // Reduced from 60 to 40
            val bottomPadding = 50.dp.toPx() // More space for x-axis labels
            val leftPadding = 50.dp.toPx() // More space for y-axis labels
            val topPadding = 30.dp.toPx() // Less top padding
            val rightPadding = 30.dp.toPx() // Less right padding

            val chartWidth = canvasWidth - leftPadding - rightPadding
            val chartHeight = canvasHeight - topPadding - bottomPadding

            // Calculate max value for scaling
            val maxMinutes = dailyStats.maxOfOrNull { (it.dailyDuration / 1000 / 60).toInt() } ?: 1
            val maxValue = max(maxMinutes, 10) // Minimum 10 for better scaling

            // Draw chart background with rounded corners effect
            drawRoundRect(
                color = Color(0xFF2A2A2A),
                topLeft = Offset(leftPadding, topPadding),
                size = androidx.compose.ui.geometry.Size(chartWidth, chartHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )

            // Draw Y-axis labels (minutes)
            val ySteps = 4 // Reduced from 5 to 4 for less clutter
            for (i in 0..ySteps) {
                val value = (maxValue * i / ySteps)
                val y = topPadding + chartHeight - (i.toFloat() / ySteps * chartHeight)

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "${value}m",
                        leftPadding - 12.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 11.sp.toPx() // Slightly larger text
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // Draw horizontal grid lines
                if (i > 0) {
                    drawLine(
                        color = Color(0xFF404040),
                        start = Offset(leftPadding, y),
                        end = Offset(leftPadding + chartWidth, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }

            // Draw X-axis labels (days)
            val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
            dailyStats.forEachIndexed { index, stat ->
                val x = leftPadding + if (dailyStats.size > 1) index * (chartWidth / (dailyStats.size - 1)) else chartWidth / 2

                // Draw vertical grid lines
                if (index > 0) {
                    drawLine(
                        color = Color(0xFF404040),
                        start = Offset(x, topPadding),
                        end = Offset(x, topPadding + chartHeight),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.date)
                    val dayText = if (date != null) dateFormat.format(date) else "${index + 1}"

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            dayText,
                            x,
                            topPadding + chartHeight + 25.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 11.sp.toPx() // Slightly larger text
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                } catch (e: Exception) {
                    // Fallback to index if date parsing fails
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${index + 1}",
                            x,
                            topPadding + chartHeight + 25.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 11.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            // Draw main axis lines (thicker)
            drawLine(
                color = Color.Gray,
                start = Offset(leftPadding, topPadding + chartHeight),
                end = Offset(leftPadding + chartWidth, topPadding + chartHeight),
                strokeWidth = 1.5.dp.toPx()
            )

            drawLine(
                color = Color.Gray,
                start = Offset(leftPadding, topPadding),
                end = Offset(leftPadding, topPadding + chartHeight),
                strokeWidth = 1.5.dp.toPx()
            )

            // Draw axis titles (positioned better)
            drawContext.canvas.nativeCanvas.apply {
                // Y-axis title
                save()
                rotate(-90f, 15.dp.toPx(), canvasHeight / 2)
                drawText(
                    "minutes",
                    15.dp.toPx(),
                    canvasHeight / 2,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 13.sp.toPx() // Slightly larger
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
                restore()

                // X-axis title
                drawText(
                    "day",
                    canvasWidth / 2,
                    canvasHeight - 8.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 13.sp.toPx() // Slightly larger
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
            }

            // Draw chart lines and data points
            if (dailyStats.size > 1) {
                val path = Path()
                val stepX = chartWidth / (dailyStats.size - 1)

                dailyStats.forEachIndexed { index, stat ->
                    val minutes = (stat.dailyDuration / 1000 / 60).toInt()
                    val x = leftPadding + index * stepX
                    val y = topPadding + chartHeight - (minutes.toFloat() / maxValue * chartHeight)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }

                    // Draw data points with better styling
                    val isSelected = selectedPoint?.first == index

                    // Draw outer circle for selected point
                    if (isSelected) {
                        drawCircle(
                            color = Color(0xFF1DB954).copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    drawCircle(
                        color = if (isSelected) Color(0xFF4CAF50) else Color(0xFF1DB954),
                        radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                        center = Offset(x, y)
                    )

                    // Draw selection indicator and value
                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )

                        // Draw tooltip with better positioning
                        val tooltipText = "${selectedPoint!!.second} minutes"
                        val tooltipWidth = 90.dp.toPx()
                        val tooltipHeight = 35.dp.toPx()
                        val tooltipX = (x - tooltipWidth / 2).coerceIn(
                            leftPadding,
                            leftPadding + chartWidth - tooltipWidth
                        )
                        val tooltipY = (y - tooltipHeight - 15.dp.toPx()).coerceAtLeast(topPadding)

                        // Tooltip shadow
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.3f),
                            topLeft = Offset(tooltipX + 2.dp.toPx(), tooltipY + 2.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )

                        // Tooltip background
                        drawRoundRect(
                            color = Color(0xFF333333),
                            topLeft = Offset(tooltipX, tooltipY),
                            size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )

                        // Draw tooltip text
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                tooltipText,
                                tooltipX + tooltipWidth / 2,
                                tooltipY + tooltipHeight / 2 + 4.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 12.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                            )
                        }
                    }
                }

                // Draw the line with gradient effect
                drawPath(
                    path = path,
                    color = Color(0xFF1DB954),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw area under the curve
                val areaPath = Path().apply {
                    addPath(path)
                    lineTo(leftPadding + (dailyStats.size - 1) * stepX, topPadding + chartHeight)
                    lineTo(leftPadding, topPadding + chartHeight)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1DB954).copy(alpha = 0.3f),
                            Color(0xFF1DB954).copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )

            } else if (dailyStats.size == 1) {
                // Single point with better styling
                val minutes = (dailyStats[0].dailyDuration / 1000 / 60).toInt()
                val x = leftPadding + chartWidth / 2
                val y = topPadding + chartHeight - (minutes.toFloat() / maxValue * chartHeight)

                val isSelected = selectedPoint?.first == 0

                if (isSelected) {
                    drawCircle(
                        color = Color(0xFF1DB954).copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                drawCircle(
                    color = if (isSelected) Color(0xFF4CAF50) else Color(0xFF1DB954),
                    radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                    center = Offset(x, y)
                )

                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )

                    // Draw tooltip
                    val tooltipText = "${selectedPoint!!.second} minutes"
                    val tooltipWidth = 90.dp.toPx()
                    val tooltipHeight = 35.dp.toPx()
                    val tooltipX = x - tooltipWidth / 2
                    val tooltipY = y - tooltipHeight - 15.dp.toPx()

                    drawRoundRect(
                        color = Color(0xFF333333),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            tooltipText,
                            x,
                            tooltipY + tooltipHeight / 2 + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }
    }
}