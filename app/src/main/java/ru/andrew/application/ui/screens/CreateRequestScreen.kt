package ru.andrew.application.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import ru.andrew.application.R
import ru.andrew.application.domain.ActionType
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.ui.extensions.displayNameResId
import ru.andrew.application.ui.navigation.Screen
import ru.andrew.application.ui.viewmodel.CreateRequestViewModel
import ru.andrew.application.ui.utils.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    navController: NavController,
    viewModel: CreateRequestViewModel = viewModel(factory = CreateRequestViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    var equipmentMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var actionMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Состояния для нативных пикеров Material 3
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var dialogSessionKey by rememberSaveable { mutableStateOf(0) }

    // Инициализируем пикеры текущим временем по умолчанию (предотвращает null и сокращает клики).
    // Обернуто в key(dialogSessionKey, uiState.nextActionDateTime == null), чтобы при сбросе формы (clearForm) или открытии диалога состояния пикеров сбрасывались.
    val (datePickerState, timePickerState) = key(dialogSessionKey, uiState.nextActionDateTime == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.nextActionDateTime
                ?.atZone(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
                ?: LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        val timeState = rememberTimePickerState(
            initialHour = uiState.nextActionDateTime?.hour ?: LocalDateTime.now().hour,
            initialMinute = uiState.nextActionDateTime?.minute ?: LocalDateTime.now().minute
        )
        Pair(dateState, timeState)
    }

    val showDateTimePicker = {
        dialogSessionKey++
        showDatePicker = true
    }

    // Нативный диалог выбора даты
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(stringResource(id = R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Нативный диалог выбора времени
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        val dateMillis = datePickerState.selectedDateMillis
                        if (dateMillis != null) {
                            val localDate = Instant.ofEpochMilli(dateMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            val selectedDateTime = LocalDateTime.of(localDate, localTime)
                            viewModel.updateNextActionDateTime(selectedDateTime)
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.dialog_select_time),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.clearForm()
        viewModel.events.collect { event ->
            when (event) {
                is CreateRequestViewModel.CreateRequestEvent.NavigationSuccess -> {
                    android.widget.Toast.makeText(context.applicationContext, R.string.create_success_message, android.widget.Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Active.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_create),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Название заявки *
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(stringResource(id = R.string.create_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                }
            )

            // Телефон *
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { viewModel.updatePhone(it) },
                label = { Text(stringResource(id = R.string.create_phone_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )
                }
            )

            // Имя клиента
            OutlinedTextField(
                value = uiState.clientName,
                onValueChange = { viewModel.updateClientName(it) },
                label = { Text(stringResource(id = R.string.create_client_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                }
            )

            // Адрес
            OutlinedTextField(
                value = uiState.address,
                onValueChange = { viewModel.updateAddress(it) },
                label = { Text(stringResource(id = R.string.create_address_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null
                    )
                }
            )

            // Выпадающий список: Тип оборудования
            ExposedDropdownMenuBox(
                expanded = equipmentMenuExpanded,
                onExpandedChange = { equipmentMenuExpanded = !equipmentMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = stringResource(id = uiState.equipmentType.displayNameResId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.create_equipment_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentMenuExpanded) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = equipmentMenuExpanded,
                    onDismissRequest = { equipmentMenuExpanded = false }
                ) {
                    EquipmentType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = type.displayNameResId)) },
                            onClick = {
                                viewModel.updateEquipmentType(type)
                                equipmentMenuExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Выпадающий список: Тип действия
            ExposedDropdownMenuBox(
                expanded = actionMenuExpanded,
                onExpandedChange = { actionMenuExpanded = !actionMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = stringResource(id = uiState.actionType.displayNameResId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.create_action_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionMenuExpanded) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = actionMenuExpanded,
                    onDismissRequest = { actionMenuExpanded = false }
                ) {
                    ActionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = type.displayNameResId)) },
                            onClick = {
                                viewModel.updateActionType(type)
                                actionMenuExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Выбор даты и времени действия *
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.nextActionDateTime?.format(dateTimeFormatter) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.create_date_time_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDateTimePicker() }
                )
            }

            // Комментарий
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = { viewModel.updateComment(it) },
                label = { Text(stringResource(id = R.string.create_comment_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null
                    )
                }
            )

            // Сообщение об ошибке валидации с кэшированием последнего значения для плавной анимации закрытия
            var lastNonNullError by remember { mutableStateOf<UiText?>(null) }
            if (uiState.error != null) {
                lastNonNullError = uiState.error
            }

            AnimatedVisibility(visible = uiState.error != null) {
                lastNonNullError?.let { errorText ->
                    val errorString = when (errorText) {
                        is UiText.DynamicString -> errorText.value
                        is UiText.StringResource -> stringResource(id = errorText.resId)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorString,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Кнопки управления формой
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearForm() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.btn_clear))
                }

                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.saveRequest()
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.btn_create))
                    }
                }
            }
        }
    }
}

