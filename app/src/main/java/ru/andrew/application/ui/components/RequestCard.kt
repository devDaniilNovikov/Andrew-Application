package ru.andrew.application.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import ru.andrew.application.R
import ru.andrew.application.data.entity.Request
import ru.andrew.application.domain.ActionType
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.ui.extensions.displayNameResId
import ru.andrew.application.ui.theme.urgentOrange
import ru.andrew.application.ui.util.formatPhoneNumber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Статусы срочности заявки.
 */
enum class UrgencyStatus {
    OVERDUE, // Просрочено (красный)
    TODAY,   // Сегодня (оранжевый)
    FUTURE   // Будущее (нейтральный/первичный)
}

/**
 * Премиальный компонент карточки заявки с адаптивной цветовой индикацией срочности.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCard(
    request: Request,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    val formattedDateTime = remember(request.nextActionDateTime) {
        request.nextActionDateTime?.format(dateTimeFormatter) ?: ""
    }

    // Вычисление статуса срочности
    val urgencyStatus = remember(request.nextActionDateTime) {
        val nextAction = request.nextActionDateTime
        val now = LocalDateTime.now()
        when {
            nextAction == null -> UrgencyStatus.FUTURE
            nextAction.isBefore(now) -> UrgencyStatus.OVERDUE
            nextAction.toLocalDate().isEqual(now.toLocalDate()) -> UrgencyStatus.TODAY
            else -> UrgencyStatus.FUTURE
        }
    }

    // Определение цветовой схемы в зависимости от срочности
    val (cardBorderColor, dateContainerColor, dateContentColor, dateBadgeText) = when (urgencyStatus) {
        UrgencyStatus.OVERDUE -> {
            Quadruple(
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.error,
                stringResource(R.string.urgency_overdue)
            )
        }
        UrgencyStatus.TODAY -> {
            Quadruple(
                MaterialTheme.colorScheme.urgentOrange.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.urgentOrange.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.urgentOrange,
                stringResource(R.string.urgency_today)
            )
        }
        UrgencyStatus.FUTURE -> {
            Quadruple(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primary,
                stringResource(R.string.urgency_planned)
            )
        }
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (urgencyStatus != UrgencyStatus.FUTURE) 1.5.dp else 1.dp,
            color = cardBorderColor
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Строка заголовка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Имя клиента и телефон
            if (!request.clientName.isNullOrBlank() || request.phone.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Клиент",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.clientName ?: stringResource(R.string.client_name_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (request.phone.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Телефон",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatPhoneNumber(request.phone),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Адрес
            if (!request.address.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Адрес",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Премиальная плашка даты и времени с индикацией срочности
            if (formattedDateTime.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(dateContainerColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Дата следующего действия",
                        tint = dateContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDateTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dateContentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = dateBadgeText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = dateContentColor,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Бэджи (chips) для типов оборудования и действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(id = (request.equipmentType ?: EquipmentType.OTHER).displayNameResId),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null
                )

                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(id = (request.actionType ?: ActionType.OTHER).displayNameResId),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null
                )
            }

            // Короткий комментарий, если есть
            if (!request.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Комментарий",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Вспомогательный кортеж для хранения четырех значений.
 */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
