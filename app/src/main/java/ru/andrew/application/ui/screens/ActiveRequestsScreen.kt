package ru.andrew.application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.andrew.application.ui.components.RequestCard
import ru.andrew.application.ui.viewmodel.ActiveRequestsUiState
import ru.andrew.application.ui.viewmodel.ActiveRequestsViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.andrew.application.ui.components.RequestDetailsBottomSheet
import ru.andrew.application.data.entity.Request

/**
 * Экран списка активных заявок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRequestsScreen(
    viewModel: ActiveRequestsViewModel = viewModel(factory = ActiveRequestsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedRequest by remember { mutableStateOf<Request?>(null) }

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
                    onCallClick = { /* Будет реализовано на этапе 5.2 */ },
                    onEditClick = { /* Будет реализовано на этапе 5.3 */ },
                    onRescheduleClick = { /* Будет реализовано на этапе 5.2 */ },
                    onCompleteClick = { /* Будет реализовано на этапе 5.4 */ },
                    onCancelClick = { /* Будет реализовано на этапе 5.4 */ }
                )
            }
        }
    }
}
