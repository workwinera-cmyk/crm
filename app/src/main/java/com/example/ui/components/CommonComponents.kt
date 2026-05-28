package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.Reminder
import com.example.ui.theme.*

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "NEW" -> StageNewColor.copy(alpha = 0.15f) to StageNewColor
        "CONTACTED" -> StageContactedColor.copy(alpha = 0.15f) to StageContactedColor
        "QUALIFIED" -> StageQualifiedColor.copy(alpha = 0.15f) to StageQualifiedColor
        "PROPOSAL" -> StageProposalColor.copy(alpha = 0.15f) to StageProposalColor
        "WON" -> StageWonColor.copy(alpha = 0.15f) to StageWonColor
        "LOST" -> StageLostColor.copy(alpha = 0.15f) to StageLostColor
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) to MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    email: String,
    size: Dp = 40.dp,
    hexColor: String = "#FF1E88E5",
    modifier: Modifier = Modifier
) {
    val initials = if (name.isNotBlank()) {
        val parts = name.trim().split(" ")
        if (parts.size > 1) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
        } else {
            "${name.firstOrNull() ?: ""}".uppercase()
        }
    } else {
        "A"
    }

    val parsedColor = try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (e: Exception) {
        Color(0xFF3D5A80)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(parsedColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (size < 36.dp) 11.sp else 14.sp
            ),
            color = Color.White
        )
    }
}

@Composable
fun LeadValueText(
    value: Double,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val formattedValue = String.format("$%,.0f", value)
    Text(
        text = formattedValue,
        style = style.copy(fontWeight = fontWeight),
        color = color
    )
}

@Composable
fun AppNotificationBanner(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onGoToLead: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Notification",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Follow-up Alarm Triggered!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${reminder.title} - ${reminder.leadName}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reminder.note.isNotBlank()) {
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = { onGoToLead(reminder.leadId); onDismiss() }) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Go to Lead",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// WhatsApp & Phone triggers
fun triggerCallLauncher(context: android.content.Context, phone: String) {
    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_DIAL,
            android.net.Uri.parse("tel:$phone")
        )
        if (context !is android.app.Activity) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No app available to handle phone calls", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun triggerWhatsAppLauncher(context: android.content.Context, phone: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        // Ensure international format
        val formattedPhone = if (cleanPhone.startsWith("0")) "60$cleanPhone" else cleanPhone // default international or standard prefix
        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=Hello! This is Apex CRM sales representative."
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        )
        if (context !is android.app.Activity) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No app available to handle WhatsApp / web links", android.widget.Toast.LENGTH_SHORT).show()
    }
}
