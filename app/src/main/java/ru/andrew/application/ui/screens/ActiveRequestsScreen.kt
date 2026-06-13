package ru.andrew.application.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.andrew.application.data.entity.Request
import ru.andrew.application.ui.components.RequestCard
import ru.andrew.application.ui.components.RequestDetailsBottomSheet
import ru.andrew.application.ui.viewmodel.ActiveRequestsUiState
import ru.andrew.application.ui.viewmodel.ActiveRequestsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Экран списка активных заявок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRequestsScreen(
    viewModel: ActiveRequestsViewModel = viewModel(factory = ActiveRequestsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedRequest by remember { mutableStateOf<Request?>(null) }

    // Состояния для переноса даты/времени (Подэтап 5.2)
    var showDatePickerForReschedule by remember { mutableStateOf(false) }
    var showTimePickerForReschedule by remember { mutableStateOf(false) }
    var rescheduleTargetRequest by remember { mutableStateOf<Request?>(null) }

    val (rescheduleDatePickerState, rescheduleTimePickerState) = key(rescheduleTargetRequest?.id) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = rescheduleTargetRequest?.nextActionDateTime
                ?.toLocalDate()
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
                ?: LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        val timeState = rememberTimePickerState(
            initialHour = rescheduleTargetRequest?.nextActionDateTime?.hour ?: LocalDateTime.now().hour,
            initialMinute = rescheduleTargetRequest?.nextActionDateTime?.minute ?: LocalDateTime.now().minute
        )
        Pair(dateState, timeState)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Активные заявки",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "😞",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ошибка при загрузке данных",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Неизвестная ошибка",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    if (uiState.requests.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📋",
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Нет активных заявок",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Все задачи выполнены! Чтобы создать новую заявку, перейдите на вкладку «Создать» в нижнем меню.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = uiState.requests,
                                key = { request -> request.id }
                            ) { request ->
                                RequestCard(
                                    request = request,
                                    onClick = {
                                        selectedRequest = request
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Детальная карточка заявки в виде ModalBottomSheet
            selectedRequest?.let { request ->
                RequestDetailsBottomSheet(
                    request = request,
                    onDismissRequest = { selectedRequest = null },
                    onCallClick = {
                        if (request.phone.isNotEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${request.phone}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось открыть номеронабиратель", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Номер телефона не указан", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEditClick = { /* Будет реализовано на этапе 5.3 */ },
                    onRescheduleClick = {
                        rescheduleTargetRequest = request
                        showDatePickerForReschedule = true
                    },
                    onCompleteClick = { /* Будет реализовано на этапе 5.4 */ },
                    onCancelClick = { /* Будет реализовано на этапе 5.4 */ }
                )
            }

            // Нативный диалог выбора даты для переноса
            if (showDatePickerForReschedule) {
                DatePickerDialog(
                    onDismissRequest = { showDatePickerForReschedule = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePickerForReschedule = false
                                showTimePickerForReschedule = true
                            },
                            enabled = rescheduleDatePickerState.selectedDateMillis != null
                        ) {
                            Text("ОК")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerForReschedule = false }) {
                            Text("Отмена")
                        }
                    }
                ) {
                    DatePicker(state = rescheduleDatePickerState)
                }
            }

            // Нативный диалог выбора времени для переноса
            if (showTimePickerForReschedule) {
                AlertDialog(
                    onDismissRequest = { showTimePickerForReschedule = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showTimePickerForReschedule = false
                                val dateMillis = rescheduleDatePickerState.selectedDateMillis
                                if (dateMillis != null && rescheduleTargetRequest != null) {
                                    val localDate = Instant.ofEpochMilli(dateMillis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                    val localTime = LocalTime.of(rescheduleTimePickerState.hour, rescheduleTimePickerState.minute)
                                    val selectedDateTime = LocalDateTime.of(localDate, localTime)
                                    
                                    viewModel.rescheduleRequest(rescheduleTargetRequest!!, selectedDateTime)
                                    selectedRequest = null // Закрываем шторку деталей
                                    Toast.makeText(context, "Заявка успешно перенесена", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("ОК")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePickerForReschedule = false }) {
                            Text("Отмена")
                        }
                    },
                    title = {
                        Text(
                            text = "Выберите время действия",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TimePicker(state = rescheduleTimePickerState)
                        }
                    }
                )
            }
        }
    }
}
