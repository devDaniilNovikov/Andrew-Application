package ru.andrew.application.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import ru.andrew.application.R
import ru.andrew.application.notifications.ExactAlarmPermission

/**
 * Предупреждение о том, что напоминания не придут или будут опаздывать.
 *
 * Без него отказ в уведомлениях или отсутствие разрешения на точные будильники проходили молча:
 * мастер не знал, что напоминаний не будет. Состояние перепроверяется при каждом возврате
 * на экран, в том числе из системных настроек.
 */
@Composable
fun ReminderStatusBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    var exactAlarmsAllowed by remember { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        exactAlarmsAllowed = ExactAlarmPermission.isGranted(context)
        onPauseOrDispose { }
    }

    when {
        !notificationsEnabled -> BannerRow(
            text = stringResource(R.string.reminder_banner_notifications_off),
            actionLabel = stringResource(R.string.reminder_banner_action_enable),
            onAction = { openNotificationSettings(context) },
            modifier = modifier
        )

        !exactAlarmsAllowed -> BannerRow(
            text = stringResource(R.string.reminder_banner_exact_alarms_off),
            actionLabel = stringResource(R.string.reminder_banner_action_allow),
            onAction = {
                ExactAlarmPermission.settingsIntent(context)?.let { context.startActivitySafely(it) }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun BannerRow(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val appNotificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    if (!context.startActivitySafely(appNotificationSettings)) {
        context.startActivitySafely(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }
}

/** Некоторые прошивки не открывают отдельные экраны настроек — без падения приложения. */
private fun Context.startActivitySafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}
