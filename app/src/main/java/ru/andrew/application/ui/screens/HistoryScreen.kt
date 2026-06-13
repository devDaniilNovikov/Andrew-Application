package ru.andrew.application.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.andrew.application.R
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.util.formatPhoneNumber
import ru.andrew.application.ui.viewmodel.HistoryFilter
import ru.andrew.application.ui.viewmodel.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val historyRequests by viewModel.historyRequests.collectAsStateWithLifecycle()
    val sortByStatus by viewModel.sortByStatus.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Center-Aligned App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.history_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Filter and Sort Controls Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Premium segmented control for sorting
            HistorySortControl(
                sortByStatus = sortByStatus,
                onSortChanged = { viewModel.toggleSortByStatus(it) }
            )

            // Horizontal chips for filtering
            HistoryFilterRow(
                currentFilter = filterMode,
                onFilterSelected = { viewModel.setFilterMode(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historyRequests.isEmpty()) {
            // Beautiful Empty State
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⏳",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(id = R.string.history_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(48.dp))

                // Theme selector always accessible in empty state
                ThemeSelectorCard(
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
        } else {
            // LazyColumn with historical cards
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyRequests, key = { it.id }) { request ->
                    HistoryRequestCard(
                        request = request,
                        formatter = dateTimeFormatter
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ThemeSelectorCard(
                        currentTheme = currentTheme,
                        onThemeSelected = onThemeSelected
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Premium segmented sort switcher (By Date / By Status) with smooth animations.
 */
@Composable
fun HistorySortControl(
    sortByStatus: Boolean,
    onSortChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option 1: By Date
            val isDateSelected = !sortByStatus
            val dateBgColor by animateColorAsState(
                targetValue = if (isDateSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                animationSpec = tween(250),
                label = "dateBg"
            )
            val dateContentColor by animateColorAsState(
                targetValue = if (isDateSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(250),
                label = "dateContent"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(dateBgColor)
                    .clickable { onSortChanged(false) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.history_sort_by_date),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isDateSelected) FontWeight.Bold else FontWeight.Medium,
                    color = dateContentColor
                )
            }

            // Option 2: By Status
            val isStatusSelected = sortByStatus
            val statusBgColor by animateColorAsState(
                targetValue = if (isStatusSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                animationSpec = tween(250),
                label = "statusBg"
            )
            val statusContentColor by animateColorAsState(
                targetValue = if (isStatusSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(250),
                label = "statusContent"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusBgColor)
                    .clickable { onSortChanged(true) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.history_sort_by_status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isStatusSelected) FontWeight.Bold else FontWeight.Medium,
                    color = statusContentColor
                )
            }
        }
    }
}

/**
 * Horizontal row of filter chips (All / Completed / Cancelled).
 */
@Composable
fun HistoryFilterRow(
    currentFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryFilter.values().forEach { filter ->
            val isSelected = currentFilter == filter
            val chipBgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                animationSpec = tween(200),
                label = "chipBg"
            )
            val chipContentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "chipContent"
            )
            val chipBorderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                animationSpec = tween(200),
                label = "chipBorder"
            )

            val textResId = when (filter) {
                HistoryFilter.ALL -> R.string.history_filter_all
                HistoryFilter.COMPLETED -> R.string.history_filter_completed
                HistoryFilter.CANCELLED -> R.string.history_filter_cancelled
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(filter) },
                color = chipBgColor,
                border = BorderStroke(1.dp, chipBorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(id = textResId),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = chipContentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Premium historical card to display completed or cancelled requests.
 */
@Composable
fun HistoryRequestCard(
    request: Request,
    formatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val isCompleted = request.status == RequestStatus.COMPLETED
    val statusColor = if (isCompleted) {
        MaterialTheme.colorScheme.primary // Green/Primary
    } else {
        MaterialTheme.colorScheme.outline // Grey/Outline
    }

    val statusText = stringResource(
        id = if (isCompleted) R.string.status_completed else R.string.status_cancelled
    )

    val formattedCreated = remember(request.createdAt) {
        request.createdAt.format(formatter)
    }

    val formattedClosed = remember(request.closedAt) {
        request.closedAt?.format(formatter) ?: ""
    }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            statusColor.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    contentColor = statusColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Client row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.clientName ?: stringResource(id = R.string.client_name_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatPhoneNumber(request.phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Dates Row (Created / Closed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.history_created_at, formattedCreated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (formattedClosed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.history_closed_at, formattedClosed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Results Section (Price / Comment / Cancel Reason)
            if (isCompleted && request.finalPrice != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.history_final_price,
                                String.format("%.2f", request.finalPrice)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            val resultText = if (isCompleted) request.finalComment else request.cancelReason
            if (!resultText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange, // Placeholder icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                id = if (isCompleted) R.string.history_final_comment else R.string.history_cancel_reason,
                                resultText
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Premium container for theme selection.
 */
@Composable
fun ThemeSelectorCard(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.theme_selection_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ThemeSelector(
                currentTheme = currentTheme,
                onThemeSelected = onThemeSelected
            )
        }
    }
}

private data class ThemeOption(
    val theme: AppTheme,
    val icon: String,
    val titleResId: Int
)

private val ThemeOptions = listOf(
    ThemeOption(AppTheme.LIGHT, "☀️", R.string.theme_light),
    ThemeOption(AppTheme.DARK, "🌙", R.string.theme_dark),
    ThemeOption(AppTheme.SYSTEM, "⚙️", R.string.theme_system)
)

@Composable
private fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val options = ThemeOptions

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = currentTheme == option.theme
                
                val baseColor = MaterialTheme.colorScheme.primary
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) baseColor else baseColor.copy(alpha = 0f),
                    animationSpec = tween(durationMillis = 300),
                    label = "bgColor"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 300),
                    label = "contentColor"
                )

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onThemeSelected(option.theme) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = option.icon,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = stringResource(id = option.titleResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
