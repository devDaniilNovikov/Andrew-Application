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
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.ui.components.ThemeIconButton
import ru.andrew.application.ui.util.cleanPhoneNumber
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import ru.andrew.application.ui.extensions.smoothScroll

/**
 * Экран списка активных заявок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRequestsScreen(
    navController: NavController,
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    deepLinkRequestId: Long = -1L,
    viewModel: ActiveRequestsViewModel = viewModel(factory = ActiveRequestsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedRequest by remember { mutableStateOf<Request?>(null) }

    LaunchedEffect(uiState.requests, deepLinkRequestId) {
        if (deepLinkRequestId != -1L && uiState.requests.isNotEmpty()) {
            val matchingRequest = uiState.requests.find { it.id == deepLinkRequestId }
            if (matchingRequest != null) {
                selectedRequest = matchingRequest
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cancelActionLabel = stringResource(R.string.btn_cancel_action)

    // Состояния для переноса даты/времени (Подэтап 5.2)
    var showDatePickerForReschedule by remember { mutableStateOf(false) }
    var showTimePickerForReschedule by remember { mutableStateOf(false) }
    var rescheduleTargetRequest by remember { mutableStateOf<Request?>(null) }
    
    // Состояния для отмены заявки (Подэтап 5.4)
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelTargetRequest by remember { mutableStateOf<Request?>(null) }

    // Состояния для выполнения заявки с вводом цены (fix-add-money)
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completeTargetRequest by remember { mutableStateOf<Request?>(null) }

    // Состояния для физического удаления заявки из списка
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetRequest by remember { mutableStateOf<Request?>(null) }

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
                        text = stringResource(R.string.active_requests_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    ThemeIconButton(
                        currentTheme = currentTheme,
                        onThemeSelected = onThemeSelected
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
                            text = stringResource(R.string.active_error_loading),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: stringResource(R.string.active_unknown_error),
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(20.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "📋",
                                            style = MaterialTheme.typography.headlineLarge,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = stringResource(id = R.string.active_empty_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.active_empty_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().smoothScroll(0.80f),
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
                                                completeTargetRequest = request
                                                showCompleteDialog = true
                                                false // snaps card back, dialog takes over
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
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32) // Зеленый для выполнения
                                            SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828) // Красный для отмены
                                            SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            null -> Color.Transparent
                                        }

                                        val alpha by animateFloatAsState(
                                            targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.Settled || dismissState.dismissDirection == null) 0f else 1f,
                                            label = "bgAlpha"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 6.dp, horizontal = 12.dp)
                                                .background(color.copy(alpha = alpha), shape = RoundedCornerShape(16.dp))
                                                .padding(horizontal = 20.dp)
                                        ) {
                                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.btn_complete),
                                                    tint = Color.White,
                                                    modifier = Modifier.align(Alignment.CenterStart)
                                                )
                                            } else if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = cancelActionLabel,
                                                    tint = Color.White,
                                                    modifier = Modifier.align(Alignment.CenterEnd)
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        RequestCard(
                                            request = request,
                                            onClick = {
                                                selectedRequest = request
                                            },
                                            onDeleteClick = {
                                                deleteTargetRequest = request
                                                showDeleteDialog = true
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
                                    data = Uri.parse("tel:${cleanPhoneNumber(request.phone)}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.active_toast_dial_error), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.active_toast_no_phone), Toast.LENGTH_SHORT).show()
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
                        completeTargetRequest = request
                        showCompleteDialog = true
                        selectedRequest = null // Закрываем шторку деталей
                    },
                    onCancelClick = {
                        cancelTargetRequest = request
                        showCancelDialog = true
                    },
                    onDeleteClick = {
                        deleteTargetRequest = request
                        showDeleteDialog = true
                        selectedRequest = null
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
                            Text(stringResource(R.string.dialog_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerForReschedule = false }) {
                            Text(stringResource(R.string.dialog_cancel))
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
                                    Toast.makeText(context, context.getString(R.string.active_toast_rescheduled), Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePickerForReschedule = false }) {
                            Text(stringResource(R.string.dialog_cancel))
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.active_reschedule_time_title),
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
                                        message = context.getString(R.string.toast_request_cancelled),
                                        actionLabel = cancelActionLabel,
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
                                        .padding(vertical = 8.dp),
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
                                            stringResource(R.string.active_cancel_reason_required)
                                        } else {
                                            stringResource(R.string.active_cancel_optional_comment)
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

            // Диалог выполнения заявки с вводом стоимости (fix-add-money)
            if (showCompleteDialog && completeTargetRequest != null) {
                CompleteRequestDialog(
                    request = completeTargetRequest!!,
                    onDismiss = { showCompleteDialog = false },
                    onConfirm = { parsedPrice, commentText ->
                        val targetId = completeTargetRequest!!.id
                        viewModel.completeRequest(targetId, parsedPrice, commentText)
                        showCompleteDialog = false
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.toast_request_completed),
                                actionLabel = cancelActionLabel,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreRequest(targetId)
                            }
                        }
                    }
                )
            }

            // Диалог физического удаления заявки из базы
            if (showDeleteDialog && deleteTargetRequest != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        deleteTargetRequest = null
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val targetId = deleteTargetRequest!!.id
                                viewModel.deleteRequest(targetId)
                                showDeleteDialog = false
                                deleteTargetRequest = null
                                selectedRequest = null
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_request_deleted),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(id = R.string.delete_dialog_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                deleteTargetRequest = null
                            }
                        ) {
                            Text(stringResource(id = R.string.dialog_cancel))
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(id = R.string.delete_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(id = R.string.delete_dialog_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
    }
}

/**
 * Диалог завершения заявки с вводом стоимости и итогового комментария.
 */
@Composable
fun CompleteRequestDialog(
    request: Request,
    onDismiss: () -> Unit,
    onConfirm: (finalPrice: Double?, finalComment: String?) -> Unit
) {
    var priceText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var priceError by remember { mutableStateOf<String?>(null) }
    val invalidPriceMsg = stringResource(id = R.string.history_invalid_price_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.complete_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Поле ввода цены
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        priceError = null
                    },
                    label = { Text(text = stringResource(id = R.string.complete_dialog_price_label)) },
                    isError = priceError != null,
                    supportingText = priceError?.let { error -> { Text(text = error) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Поле ввода комментария
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text(text = stringResource(id = R.string.complete_dialog_comment_label)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceText.trim().replace(",", ".").toDoubleOrNull()
                    if (priceText.trim().isNotEmpty() && parsedPrice == null) {
                        priceError = invalidPriceMsg
                    } else {
                        onConfirm(parsedPrice, commentText.trim().takeIf { it.isNotEmpty() })
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.complete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}
