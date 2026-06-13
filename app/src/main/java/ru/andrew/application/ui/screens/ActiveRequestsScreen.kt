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
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import ru.andrew.application.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

/**
 * Экран списка активных заявок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRequestsScreen(
    navController: NavController,
    viewModel: ActiveRequestsViewModel = viewModel(factory = ActiveRequestsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedRequest by remember { mutableStateOf<Request?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Состояния для переноса даты/времени (Подэтап 5.2)
    var showDatePickerForReschedule by remember { mutableStateOf(false) }
    var showTimePickerForReschedule by remember { mutableStateOf(false) }
    var rescheduleTargetRequest by remember { mutableStateOf<Request?>(null) }
    
    // Состояния для отмены заявки (Подэтап 5.4)
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelTargetRequest by remember { mutableStateOf<Request?>(null) }

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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        when (dismissValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                scope.launch {
                                                    viewModel.completeRequest(request.id)
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Заявка успешно выполнена!",
                                                        actionLabel = "Отменить",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.restoreRequest(request.id)
                                                    }
                                                }
                                                true
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                cancelTargetRequest = request
                                                showCancelDialog = true
                                                false // false snaps the card back to Settled, so it does not get stuck in swiped state
                                            }
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            targetValue = when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32) // Мягкий зеленый для выполнения
                                                SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828) // Мягкий красный для отмены
                                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            },
                                            label = "dismissColor"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 6.dp, horizontal = 12.dp)
                                                .background(color, shape = RoundedCornerShape(16.dp))
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = when (dismissState.dismissDirection) {
                                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                                else -> Alignment.Center
                                            }
                                        ) {
                                            when (dismissState.dismissDirection) {
                                                SwipeToDismissBoxValue.StartToEnd -> Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Выполнено",
                                                    tint = Color.White
                                                )
                                                SwipeToDismissBoxValue.EndToStart -> Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Отменить",
                                                    tint = Color.White
                                                )
                                                else -> {}
                                            }
                                        }
                                    },
                                    content = {
                                        RequestCard(
                                            request = request,
                                            onClick = {
                                                selectedRequest = request
                                            }
                                        )
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
                    onEditClick = {
                        selectedRequest = null
                        navController.navigate("create?requestId=${request.id}")
                    },
                    onRescheduleClick = {
                        rescheduleTargetRequest = request
                        showDatePickerForReschedule = true
                    },
                    onCompleteClick = {
                        val targetId = request.id
                        viewModel.completeRequest(targetId)
                        selectedRequest = null // Закрываем шторку деталей
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Заявка успешно выполнена!",
                                actionLabel = "Отменить",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreRequest(targetId)
                            }
                        }
                    },
                    onCancelClick = {
                        cancelTargetRequest = request
                        showCancelDialog = true
                    }
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

            // Диалог отмены заявки с выбором причин (Подэтап 5.4)
            if (showCancelDialog && cancelTargetRequest != null) {
                val presetReasons = listOf(
                    stringResource(id = R.string.cancel_reason_client_refused),
                    stringResource(id = R.string.cancel_reason_no_contact),
                    stringResource(id = R.string.cancel_reason_no_parts),
                    stringResource(id = R.string.cancel_reason_other)
                )
                var selectedReasonIndex by remember { mutableStateOf(0) }
                var customComment by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showCancelDialog = false },
                    confirmButton = {
                        val isConfirmEnabled = selectedReasonIndex != 3 || customComment.trim().isNotEmpty()
                        Button(
                            onClick = {
                                val finalReason = if (selectedReasonIndex == 3) {
                                    customComment.trim()
                                } else {
                                    presetReasons[selectedReasonIndex]
                                }
                                val comment = if (selectedReasonIndex == 3) null else customComment.trim().takeIf { it.isNotEmpty() }
                                
                                val targetId = cancelTargetRequest!!.id
                                viewModel.cancelRequest(targetId, finalReason, comment)
                                showCancelDialog = false
                                selectedRequest = null // Закрываем шторку деталей
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Заявка отменена.",
                                        actionLabel = "Отменить",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreRequest(targetId)
                                    }
                                }
                            },
                            enabled = isConfirmEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(id = R.string.cancel_dialog_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = false }) {
                            Text(stringResource(id = R.string.dialog_cancel))
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(id = R.string.cancel_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetReasons.forEachIndexed { index, reason ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedReasonIndex = index }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedReasonIndex == index,
                                        onClick = { selectedReasonIndex = index }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = reason, style = MaterialTheme.typography.bodyLarge)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customComment,
                                onValueChange = { customComment = it },
                                label = {
                                    Text(
                                        text = if (selectedReasonIndex == 3) {
                                            "Укажите причину отмены *"
                                        } else {
                                            "Итоговый комментарий (необязательно)"
                                        }
                                    )
                                },
                                placeholder = { Text(stringResource(id = R.string.cancel_dialog_comment_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                    }
                )
            }
        }
    }
}
