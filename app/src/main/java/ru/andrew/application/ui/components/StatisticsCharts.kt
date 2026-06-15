package ru.andrew.application.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.andrew.application.R
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
                text = stringResource(R.string.stats_no_chart_data),
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

    val yAxisTextStyle = remember(textColor, labelFont) {
        TextStyle(
            color = textColor,
            fontSize = 10.sp,
            fontFamily = labelFont
        )
    }

    val xAxisTextStyle = remember(textColor, labelFont) {
        TextStyle(
            color = textColor,
            fontSize = 10.sp,
            fontFamily = labelFont,
            textAlign = TextAlign.Center
        )
    }

    // Предварительное вычисление подписей по оси Y
    val yLabels = remember(displayMax, yAxisTextStyle) {
        val list = mutableListOf<Pair<Float, TextLayoutResult>>()
        val gridLinesCount = 3
        for (i in 0..gridLinesCount) {
            val fraction = i.toFloat() / gridLinesCount
            val gridVal = displayMax * fraction
            val formattedVal = String.format(java.util.Locale("ru", "RU"), "%,.0f ₽", gridVal)
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(formattedVal),
                style = yAxisTextStyle
            )
            list.add(fraction to textLayoutResult)
        }
        list
    }

    // Предварительное вычисление подписей по оси X
    val skipStep = remember(points.size) {
        when {
            points.size > 12 -> 2
            points.size > 7 -> 1
            else -> 1
        }
    }
    val xLabels = remember(points, xAxisTextStyle) {
        points.mapIndexedNotNull { index, pair ->
            if (index % skipStep == 0) {
                val label = pair.first
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = xAxisTextStyle
                )
                index to textLayoutResult
            } else {
                null
            }
        }
    }

    // Предварительное вычисление тултипов для точек
    val tooltipTextStyle = remember(onPrimaryColor, labelFont) {
        TextStyle(
            color = onPrimaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = labelFont
        )
    }
    
    val tooltips = remember(points, tooltipTextStyle) {
        points.map { point ->
            val tooltipValue = String.format(java.util.Locale("ru", "RU"), "%,.0f ₽", point.second)
            val tooltipText = "${point.first}: $tooltipValue"
            textMeasurer.measure(
                text = AnnotatedString(tooltipText),
                style = tooltipTextStyle
            )
        }
    }

    val leftPaddingDp = 40.dp
    val bottomPaddingDp = 24.dp
    val topPaddingDp = 16.dp
    val rightPaddingDp = 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val width = size.width.toFloat()
                            val leftPadding = leftPaddingDp.toPx()
                            val rightPadding = rightPaddingDp.toPx()
                            val chartWidth = width - leftPadding - rightPadding
                            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                            val relativeX = offset.x - leftPadding
                            val clickedIndex = (relativeX / stepX + 0.5f).toInt()
                            selectedIndex = if (clickedIndex in points.indices) clickedIndex else -1
                        }
                    )
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            val leftPadding = leftPaddingDp.toPx()
                            val rightPadding = rightPaddingDp.toPx()
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
                        onHorizontalDrag = { change, _ ->
                            val width = size.width.toFloat()
                            val leftPadding = leftPaddingDp.toPx()
                            val rightPadding = rightPaddingDp.toPx()
                            val chartWidth = width - leftPadding - rightPadding
                            val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                            val relativeX = change.position.x - leftPadding
                            val clickedIndex = (relativeX / stepX + 0.5f).toInt()
                            selectedIndex = if (clickedIndex in points.indices) clickedIndex else -1
                        }
                    )
                }
                .drawWithCache {
                    val width = size.width
                    val height = size.height

                    val leftPadding = leftPaddingDp.toPx()
                    val bottomPadding = bottomPaddingDp.toPx()
                    val topPadding = topPaddingDp.toPx()
                    val rightPadding = rightPaddingDp.toPx()

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - bottomPadding - topPadding

                    val stepX = if (chartWidth > 0f) chartWidth / (points.size - 1).coerceAtLeast(1).toFloat() else 0f
                    val coords = if (chartWidth > 0f && chartHeight > 0f) {
                        points.mapIndexed { index, pair ->
                            val x = leftPadding + index * stepX
                            val yFraction = pair.second / displayMax
                            val y = topPadding + chartHeight * (1f - yFraction)
                            Offset(x, y)
                        }
                    } else emptyList()

                    val staticLinePath = Path()
                    val staticGradientPath = Path()

                    if (coords.isNotEmpty() && chartWidth > 0f && chartHeight > 0f) {
                        staticLinePath.moveTo(coords[0].x, coords[0].y)
                        for (i in 0 until coords.size - 1) {
                            val p1 = coords[i]
                            val p2 = coords[i + 1]
                            val cx = p1.x + (p2.x - p1.x) / 2f
                            staticLinePath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }

                        staticGradientPath.moveTo(coords[0].x, topPadding + chartHeight)
                        staticGradientPath.lineTo(coords[0].x, coords[0].y)
                        for (i in 0 until coords.size - 1) {
                            val p1 = coords[i]
                            val p2 = coords[i + 1]
                            val cx = p1.x + (p2.x - p1.x) / 2f
                            staticGradientPath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                        staticGradientPath.lineTo(coords.last().x, topPadding + chartHeight)
                        staticGradientPath.close()
                    }

                    onDrawBehind {
                        if (chartWidth <= 0 || chartHeight <= 0) return@onDrawBehind

            // 1. Рисуем сетку координат и подписи по оси Y (3 горизонтальные линии)
            yLabels.forEach { (fraction, textLayoutResult) ->
                val y = topPadding + chartHeight * (1f - fraction)
                // Даш-линия сетки
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
                )
                // Текст по оси Y
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(4.dp.toPx(), y - textLayoutResult.size.height / 2f)
                )
            }

            // 2. Координаты вычислены в кэше

            // 3. Строим плавный Bezier Path и заполняем градиентом
            if (coords.isNotEmpty()) {
                val animatedProgress = animationProgress.value

                val activeLinePath: Path
                val activeGradientPath: Path

                if (animatedProgress >= 1f) {
                    activeLinePath = staticLinePath
                    activeGradientPath = staticGradientPath
                } else {
                    activeLinePath = Path().apply { moveTo(coords[0].x, coords[0].y) }
                    for (i in 0 until coords.size - 1) {
                        val p1 = coords[i]
                        val p2 = coords[i + 1]
                        val cx = p1.x + (p2.x - p1.x) / 2f
                        val animatedP2 = Offset(
                            p1.x + (p2.x - p1.x) * animatedProgress,
                            p1.y + (p2.y - p1.y) * animatedProgress
                        )
                        activeLinePath.cubicTo(
                            p1.x + (cx - p1.x) * animatedProgress,
                            p1.y + (p1.y - p1.y) * animatedProgress,
                            p1.x + (cx - p1.x) * animatedProgress,
                            p1.y + (p2.y - p1.y) * animatedProgress,
                            animatedP2.x,
                            animatedP2.y
                        )
                    }

                    activeGradientPath = Path().apply {
                        moveTo(coords[0].x, topPadding + chartHeight)
                        lineTo(coords[0].x, coords[0].y)
                        for (i in 0 until coords.size - 1) {
                            val p1 = coords[i]
                            val p2 = coords[i + 1]
                            val cx = p1.x + (p2.x - p1.x) / 2f
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                        lineTo(coords.last().x, topPadding + chartHeight)
                        close()
                    }
                }

                // Рисуем градиент под кривой
                if (animatedProgress > 0.05f) {
                    drawPath(
                        path = activeGradientPath,
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
                    path = activeLinePath,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Рисуем точки на стыках
                val outerRadius = 3.dp.toPx()
                val innerRadius = 2.dp.toPx()
                coords.forEachIndexed { index, offset ->
                    if (index * stepX <= (chartWidth * animatedProgress)) {
                        drawCircle(
                            color = surfaceColor,
                            radius = outerRadius,
                            center = offset
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = innerRadius,
                            center = offset,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }

            // 4. Подписи по оси X (под каждой точкой)
            xLabels.forEach { (index, textLayoutResult) ->
                val x = leftPadding + index * stepX - textLayoutResult.size.width / 2f
                val y = height - bottomPadding + 8.dp.toPx()
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(x, y)
                )
            }

            // 5. Отрисовка выбранной точки (интерактивность)
            if (selectedIndex in coords.indices && selectedIndex != -1) {
                val selectedCoord = coords[selectedIndex]

                // Рисуем вертикальную направляющую линию
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = Offset(selectedCoord.x, topPadding),
                    end = Offset(selectedCoord.x, topPadding + chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                )

                // Подсвечиваем точку увеличенным радиусом
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = selectedCoord
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 2.5.dp.toPx(),
                    center = selectedCoord
                )

                // Рисуем красивое всплывающее окно (tooltip) с информацией
                val tooltipLayoutResult = tooltips[selectedIndex]

                val tooltipWidth = tooltipLayoutResult.size.width + 16.dp.toPx()
                val tooltipHeight = tooltipLayoutResult.size.height + 8.dp.toPx()
                
                // Позиционируем тултип сверху над точкой, со сдвигом влево/вправо у границ
                var tooltipX = selectedCoord.x - tooltipWidth / 2f
                if (tooltipX < leftPadding) tooltipX = leftPadding
                if (tooltipX + tooltipWidth > width - rightPadding) tooltipX = width - rightPadding - tooltipWidth

                val tooltipY = (selectedCoord.y - tooltipHeight - 8.dp.toPx()).coerceAtLeast(4.dp.toPx())

                // Рисуем фон тултипа со скругленными краями
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Отрисовываем текст внутри тултипа
                drawText(
                    textLayoutResult = tooltipLayoutResult,
                    topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx())
                )
            }
        }
    }
    )
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
                    text = stringResource(R.string.stats_success),
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
                title = stringResource(R.string.stats_completed),
                count = completed,
                percentage = if (total > 0) successRate.toInt() else 0
            )
            LegendItem(
                color = cancelledColor,
                title = stringResource(R.string.stats_cancelled),
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
                text = stringResource(id = R.string.stats_legend_format, count, percentage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

