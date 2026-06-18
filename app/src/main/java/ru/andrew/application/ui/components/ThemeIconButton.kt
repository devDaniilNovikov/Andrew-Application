package ru.andrew.application.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.andrew.application.ui.theme.AppTheme
import ru.andrew.application.R

@Composable
fun ThemeIconButton(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = modifier
    ) {
        val icon = when (currentTheme) {
            AppTheme.LIGHT -> Icons.Default.LightMode
            AppTheme.DARK -> Icons.Default.DarkMode
            AppTheme.SYSTEM -> Icons.Default.Settings
        }
        val contentDescription = when (currentTheme) {
            AppTheme.LIGHT -> stringResource(R.string.theme_light)
            AppTheme.DARK -> stringResource(R.string.theme_dark)
            AppTheme.SYSTEM -> stringResource(R.string.theme_system)
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.theme_light)) },
            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null) },
            trailingIcon = if (currentTheme == AppTheme.LIGHT) {
                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            } else null,
            onClick = {
                onThemeSelected(AppTheme.LIGHT)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.theme_dark)) },
            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
            trailingIcon = if (currentTheme == AppTheme.DARK) {
                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            } else null,
            onClick = {
                onThemeSelected(AppTheme.DARK)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.theme_system)) },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            trailingIcon = if (currentTheme == AppTheme.SYSTEM) {
                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            } else null,
            onClick = {
                onThemeSelected(AppTheme.SYSTEM)
                expanded = false
            }
        )
    }
}
