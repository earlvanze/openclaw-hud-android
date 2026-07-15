package ai.openclaw.app.ui.chat

import ai.openclaw.app.chat.ChatMessage
import ai.openclaw.app.reporting.ContentReportCategory
import ai.openclaw.app.reporting.ContentReportClient
import ai.openclaw.app.reporting.ContentReportReceipt
import ai.openclaw.app.reporting.reportableAssistantExcerpt
import ai.openclaw.app.ui.mobileDanger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ContentReportDialog(
    message: ChatMessage,
    client: ContentReportClient,
    onDismiss: () -> Unit,
) {
    var category by remember(message.id) { mutableStateOf(ContentReportCategory.OtherOffensive) }
    var comment by remember(message.id) { mutableStateOf("") }
    var categoryMenuExpanded by remember(message.id) { mutableStateOf(false) }
    var submitting by remember(message.id) { mutableStateOf(false) }
    var errorText by remember(message.id) { mutableStateOf<String?>(null) }
    var receipt by remember(message.id) { mutableStateOf<ContentReportReceipt?>(null) }
    val scope = rememberCoroutineScope()
    val excerpt = remember(message) { message.reportableAssistantExcerpt() }

    if (receipt != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Report received") },
            text = {
                Text(
                    "ECO Systems LLC received this report. Receipt ${receipt!!.id.take(8)}.",
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = mobileDanger,
            )
        },
        title = { Text("Report offensive response") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The selected assistant text, category, optional note, app version, and a one-way message hash will be sent to ECO Systems LLC. No image, gateway, device ID, or session data is included.",
                )

                Column {
                    OutlinedButton(
                        onClick = { categoryMenuExpanded = true },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(category.displayName)
                            Icon(Icons.Outlined.ExpandMore, contentDescription = "Choose report category")
                        }
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                    ) {
                        for (option in ContentReportCategory.entries) {
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    category = option
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(ContentReportClient.MAX_COMMENT_CHARS) },
                    enabled = !submitting,
                    label = { Text("Optional note") },
                    supportingText = { Text("${comment.length}/${ContentReportClient.MAX_COMMENT_CHARS}") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = excerpt,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )

                errorText?.let { error ->
                    Text(text = error, color = mobileDanger)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting && excerpt.isNotBlank(),
                onClick = {
                    submitting = true
                    errorText = null
                    scope.launch {
                        runCatching {
                            client.submit(
                                message = message,
                                category = category,
                                userComment = comment,
                            )
                        }.onSuccess { submitted ->
                            receipt = submitted
                        }.onFailure { error ->
                            errorText = error.message ?: "The report could not be delivered. Try again."
                        }
                        submitting = false
                    }
                },
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Flag, contentDescription = null)
                    Text("Send report")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Cancel")
            }
        },
    )
}
