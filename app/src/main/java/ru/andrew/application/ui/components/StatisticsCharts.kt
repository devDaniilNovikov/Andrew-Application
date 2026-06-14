package ru.andrew.application.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Премиальный линейный график выручки на основе Canvas со сглаживанием Безье,
 * изумрудным градиентом, интерактивным выбором точек и анимацией.
 */
@Composable
fun RevenueLineChart(
    points: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет данных для отображения графика",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val maxVal = points.maxOfOrNull { it.second } ?: 0f
    val displayMax = if (maxVal > 0f) maxVal * 1.15f else 1000f

    // Анимация входа графика
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    // Состояние для интерактивного выбора точек
    var selectedIndex by remember { mutableStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    // Извлекаем нужные стили из MaterialTheme заранее, так как их нельзя вызывать в DrawScope Canvas
    val labelFont = MaterialTheme.typography.labelSmall.fontFamily
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val width = size.width.toFloat()
                            val leftPadding = 120f
                            val rightPadding = 40f
                            val chartWidth = width - leftPadding - rightPadding
                            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                            val relativeX = offset.x - leftPadding
                            val clickedIndex = (relativeX / stepX + 0.5f).toInt()
                            selectedIndex = if (clickedIndex in points.indices) clickedIndex else -1
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            val leftPadding = 120f
                            val rightPadding = 40f
                            val chartWidth = width - leftPadding - rightPadding
                            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                            val relativeX = offset.x - leftPadding
                            val clickedIndex = (relativeX / stepX + 0.5f).toInt()
                            selectedIndex = if (clickedIndex in points.indices) clickedIndex else -1
                        },
                        onDragEnd = {
                            selectedIndex = -1
                        },
                        onDragCancel = {
                            selectedIndex = -1
                        },
                        onDrag = { change, _ ->
                            val width = size.width.toFloat()
                            val leftPadding = 120f
                            val rightPadding = 40f
                            val chartWidth = width - leftPadding - rightPadding
                            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                            val relativeX = change.position.x - leftPadding
                            val clickedIndex = (relativeX / stepX + 0.5f).toInt()
                            selectedIndex = if (clickedIndex in points.indices) clickedIndex else -1
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            val leftPadding = 120f
            val bottomPadding = 60f
            val topPadding = 40f
            val rightPadding = 40f

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - bottomPadding - topPadding

            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

            // 1. Рисуем сетку координат и подписи по оси Y (3 горизонтальные линии)
            val gridLinesCount = 3
            val yAxisTextStyle = TextStyle(
                color = textColor,
                fontSize = 10.sp,
                fontFamily = labelFont
            )

            for (i in 0..gridLinesCount) {
                val fraction = i.toFloat() / gridLinesCount
                val y = topPadding + chartHeight * (1f - fraction)
                val gridVal = displayMax * fraction

                // Даш-линия сетки
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )

                // Текст по оси Y
                val formattedVal = String.format(Locale.getDefault(), "%,.0f ₽", gridVal)
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(formattedVal),
                    style = yAxisTextStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(10f, y - textLayoutResult.size.height / 2f)
                )
            }

            // 2. Рассчитываем координаты точек
            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
            val coords = points.mapIndexed { index, pair ->
                val x = leftPadding + index * stepX
                val yFraction = pair.second / displayMax
                val y = topPadding + chartHeight * (1f - yFraction)
                Offset(x, y)
            }

            // 3. Строим плавный Bezier Path и заполняем градиентом
            if (coords.isNotEmpty()) {
                val animatedProgress = animationProgress.value

                // Линейный путь графика
                val linePath = Path()
                linePath.moveTo(coords[0].x, coords[0].y)

                for (i in 0 until coords.size - 1) {
                    val p1 = coords[i]
                    val p2 = coords[i + 1]

                    // Контрольные точки для кубического сплайна Безье
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlY1 = p1.y
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f
                    val controlY2 = p2.y

                    val animatedP2 = Offset(
                        p1.x + (p2.x - p1.x) * animatedProgress,
                        p1.y + (p2.y - p1.y) * animatedProgress
                    )

                    linePath.cubicTo(
                        p1.x + (controlX1 - p1.x) * animatedProgress,
                        p1.y + (controlY1 - p1.y) * animatedProgress,
                        p1.x + (controlX2 - p1.x) * animatedProgress,
                        p1.y + (controlY2 - p1.y) * animatedProgress,
                        animatedP2.x,
                        animatedP2.y
                    )
                }

                // Для красивого градиента строим вспомогательный путь заливки
                val lastIndex = coords.size - 1
                val gradientPath = Path().apply {
                    moveTo(coords[0].x, topPadding + chartHeight)
                    lineTo(coords[0].x, coords[0].y)
                    
                    for (i in 0 until coords.size - 1) {
                        val p1 = coords[i]
                        val p2 = coords[i + 1]
                        val cx1 = p1.x + (p2.x - p1.x) / 2f
                        val cx2 = p1.x + (p2.x - p1.x) / 2f
                        cubicTo(cx1, p1.y, cx2, p2.y, p2.x, p2.y)
                    }
                    lineTo(coords[lastIndex].x, topPadding + chartHeight)
                    close()
                }

                // Рисуем градиент под кривой
                if (animatedProgress > 0.05f) {
                    drawPath(
                        path = gradientPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f * animatedProgress),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )
                }

                // Рисуем саму линию графика
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Рисуем точки на стыках
                coords.forEachIndexed { index, offset ->
                    if (index * stepX <= (chartWidth * animatedProgress)) {
                        drawCircle(
                            color = surfaceColor,
                            radius = 6f,
                            center = offset
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 4f,
                            center = offset,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            // 4. Подписи по оси X (под каждой точкой)
            val xAxisTextStyle = TextStyle(
                color = textColor,
                fontSize = 10.sp,
                fontFamily = labelFont,
                textAlign = TextAlign.Center
            )

            // Отображаем подписи оси X с пропуском, если точек слишком много (например, для года)
            val skipStep = when {
                points.size > 12 -> 2
                points.size > 7 -> 1
                else -> 1
            }

            points.forEachIndexed { index, pair ->
                if (index % skipStep == 0) {
                    val label = pair.first
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(label),
                        style = xAxisTextStyle
                    )
                    val x = leftPadding + index * stepX - textLayoutResult.size.width / 2f
                    val y = height - bottomPadding + 15f
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(x, y)
                    )
                }
            }

            // 5. Отрисовка выбранной точки (интерактивность)
            if (selectedIndex in coords.indices && selectedIndex != -1) {
                val selectedCoord = coords[selectedIndex]
                val selectedPoint = points[selectedIndex]

                // Рисуем вертикальную направляющую линию
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = Offset(selectedCoord.x, topPadding),
                    end = Offset(selectedCoord.x, topPadding + chartHeight),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Подсвечиваем точку увеличенным радиусом
                drawCircle(
                    color = primaryColor,
                    radius = 10f,
                    center = selectedCoord
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 5f,
                    center = selectedCoord
                )

                // Рисуем красивое всплывающее окно (tooltip) с информацией
                val tooltipValue = String.format(Locale.getDefault(), "%,.0f ₽", selectedPoint.second)
                val tooltipText = "${selectedPoint.first}: $tooltipValue"
                
                val tooltipTextStyle = TextStyle(
                    color = onPrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = labelFont
                )
                
                val tooltipLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(tooltipText),
                    style = tooltipTextStyle
                )

                val tooltipWidth = tooltipLayoutResult.size.width + 24f
                val tooltipHeight = tooltipLayoutResult.size.height + 16f
                
                // Позиционируем тултип сверху над точкой, со сдвигом влево/вправо у границ
                var tooltipX = selectedCoord.x - tooltipWidth / 2f
                if (tooltipX < leftPadding) tooltipX = leftPadding
                if (tooltipX + tooltipWidth > width - rightPadding) tooltipX = width - rightPadding - tooltipWidth

                val tooltipY = (selectedCoord.y - tooltipHeight - 15f).coerceAtLeast(10f)

                // Рисуем фон тултипа со скругленными краями
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Отрисовываем текст внутри тултипа
                drawText(
                    textLayoutResult = tooltipLayoutResult,
                    topLeft = Offset(tooltipX + 12f, tooltipY + 8f)
                )
            }
        }
    }
}

/**
 * Премиальный пончиковый график (Donut Chart) эффективности выполнения заявок.
 * Имеет скругленные края дуг, контрастный процент по центру, легенду и анимацию входа.
 */
@Composable
fun EfficiencyDonutChart(
    completed: Int,
    cancelled: Int,
    successRate: Float,
    modifier: Modifier = Modifier,
    completedColor: Color = MaterialTheme.colorScheme.primary,
    cancelledColor: Color = MaterialTheme.colorScheme.error,
    emptyColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
) {
    val total = completed + cancelled
    val animatedSuccessProgress = animateFloatAsState(
        targetValue = if (total > 0) successRate / 100f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "success_rate_animation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левая часть: Пончик (Canvas)
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                if (total == 0) {
                    // Пустое состояние (серый круг)
                    drawCircle(
                        color = emptyColor,
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    val completedSweep = animatedSuccessProgress.value * 360f
                    val cancelledSweep = (1f - animatedSuccessProgress.value) * 360f

                    // Фоновая дуга (Отмененные)
                    drawArc(
                        color = cancelledColor,
                        startAngle = -90f + completedSweep,
                        sweepAngle = cancelledSweep,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Основная дуга (Выполненные)
                    if (completedSweep > 0f) {
                        drawArc(
                            color = completedColor,
                            startAngle = -90f,
                            sweepAngle = completedSweep,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Текст внутри пончика
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (total > 0) "${successRate.toInt()}%" else "0%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Успешно",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Правая часть: Легенда и статистика в числах
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(
                color = completedColor,
                title = "Выполнено",
                count = completed,
                percentage = if (total > 0) successRate.toInt() else 0
            )
            LegendItem(
                color = cancelledColor,
                title = "Отменено",
                count = cancelled,
                percentage = if (total > 0) (100f - successRate).toInt() else 0
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    title: String,
    count: Int,
    percentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count заяв. ($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

