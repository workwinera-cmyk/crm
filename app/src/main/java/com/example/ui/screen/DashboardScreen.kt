package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Activity
import com.example.data.model.Lead
import com.example.data.model.Reminder
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.LeadValueText
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    currentUser: User,
    leads: List<Lead>,
    reminders: List<Reminder>,
    activities: List<Activity>,
    onNavigateToLeads: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToLeadDetail: (Int) -> Unit
) {
    // Analytics calculations
    val totalLeads = leads.size
    val activeLeads = leads.filter { it.status != "WON" && it.status != "LOST" }
    val totalPipelineValue = activeLeads.sumOf { it.value }
    val wonLeads = leads.filter { it.status == "WON" }
    val wonValue = wonLeads.sumOf { it.value }
    val conversionRate = if (totalLeads > 0) {
        (wonLeads.size.toDouble() / totalLeads.toDouble() * 100).toInt()
    } else 0

    // Reminders due
    val pendingReminders = reminders.filter { !it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Welcome Header Banner
        DashboardHeader(currentUser = currentUser)

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWideScreen = maxWidth > 600.dp
            
            if (isWideScreen) {
                // Dual Pane tablet/DeX layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Pane: Core metrics and rich graphics
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MetricsSectionQuick(
                            totalPipeline = totalPipelineValue,
                            wonVal = wonValue,
                            converts = conversionRate,
                            pendingRem = pendingReminders.size,
                            onNavigateToLeads = onNavigateToLeads,
                            onNavigateToReminders = onNavigateToReminders
                        )

                        FunnelAnalyticsGraphic(leads = leads, modifier = Modifier.weight(1f))
                    }

                    // Right Pane: Activity Stream and Alerts
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardAlertsPanel(
                            activeReminders = pendingReminders,
                            onNavigateToReminders = onNavigateToReminders,
                            modifier = Modifier.weight(1f)
                        )

                        ActivityTimelineBrief(
                            activities = activities,
                            onNavigateToLeadDetail = onNavigateToLeadDetail,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            } else {
                // Smartphone single column layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        MetricsScrollableRow(
                            totalPipeline = totalPipelineValue,
                            wonVal = wonValue,
                            converts = conversionRate,
                            pendingRem = pendingReminders.size,
                            onNavigateToLeads = onNavigateToLeads,
                            onNavigateToReminders = onNavigateToReminders
                        )
                    }

                    item {
                        FunnelAnalyticsGraphic(leads = leads, modifier = Modifier.height(290.dp))
                    }

                    item {
                        DashboardAlertsPanel(
                            activeReminders = pendingReminders,
                            onNavigateToReminders = onNavigateToReminders,
                            isScrollable = false
                        )
                    }

                    item {
                        ActivityTimelineBrief(
                            activities = activities,
                            onNavigateToLeadDetail = onNavigateToLeadDetail,
                            isScrollable = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(currentUser: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back, ${currentUser.name} ✨",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Role: ${currentUser.role.name} • Session online with Local Database Sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            UserAvatar(
                name = currentUser.name,
                email = currentUser.email,
                size = 48.dp,
                hexColor = currentUser.avatarColorHex
            )
        }
    }
}

@Composable
fun MetricsSectionQuick(
    totalPipeline: Double,
    wonVal: Double,
    converts: Int,
    pendingRem: Int,
    onNavigateToLeads: () -> Unit,
    onNavigateToReminders: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricBox(
            title = "Active Pipeline",
            value = String.format("$%,.0f", totalPipeline),
            icon = Icons.Default.TrendingUp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).clickable { onNavigateToLeads() }
        )
        MetricBox(
            title = "Closed Won",
            value = String.format("$%,.0f", wonVal),
            icon = Icons.Default.CheckCircle,
            color = StageWonColor,
            modifier = Modifier.weight(1f).clickable { onNavigateToLeads() }
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricBox(
            title = "Conversion",
            value = "$converts%",
            icon = Icons.Default.Leaderboard,
            color = StageQualifiedColor,
            modifier = Modifier.weight(1f)
        )
        MetricBox(
            title = "Reminders",
            value = pendingRem.toString(),
            icon = Icons.Default.NotificationsActive,
            color = StageContactedColor,
            modifier = Modifier.weight(1f).clickable { onNavigateToReminders() }
        )
    }
}

@Composable
fun MetricsScrollableRow(
    totalPipeline: Double,
    wonVal: Double,
    converts: Int,
    pendingRem: Int,
    onNavigateToLeads: () -> Unit,
    onNavigateToReminders: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricBox(
                title = "Pipeline Deal Value",
                value = String.format("$%,.0f", totalPipeline),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).clickable { onNavigateToLeads() }
            )
            MetricBox(
                title = "Revenue Won",
                value = String.format("$%,.0f", wonVal),
                icon = Icons.Default.MonetizationOn,
                color = StageWonColor,
                modifier = Modifier.weight(1f).clickable { onNavigateToLeads() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricBox(
                title = "Win Conversion",
                value = "$converts%",
                icon = Icons.Default.AssignmentTurnedIn,
                color = StageQualifiedColor,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "Follow-ups Due",
                value = pendingRem.toString(),
                icon = Icons.Default.Alarm,
                color = StageContactedColor,
                modifier = Modifier.weight(1f).clickable { onNavigateToReminders() }
            )
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FunnelAnalyticsGraphic(leads: List<Lead>, modifier: Modifier = Modifier) {
    val stages = listOf("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST")
    val counts = stages.associateWith { stage -> leads.count { it.status == stage } }
    val maxCount = counts.values.maxOrNull() ?: 1
    val limit = if (maxCount == 0) 1 else maxCount

    val secondaryCol = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sales Funnel distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            stages.forEach { stage ->
                val count = counts[stage] ?: 0
                val ratio = (count.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
                
                // Color mapping for bars
                val barColor = when (stage) {
                    "NEW" -> StageNewColor
                    "CONTACTED" -> StageContactedColor
                    "QUALIFIED" -> StageQualifiedColor
                    "PROPOSAL" -> StageProposalColor
                    "WON" -> StageWonColor
                    "LOST" -> StageLostColor
                    else -> secondaryCol
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stage,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$count leads",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }

                    // Progress Track Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .clip(CircleShape)
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertItemRow(reminder: Reminder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(StageContactedColor)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Lead: ${reminder.leadName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(reminder.triggerTime))
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DashboardAlertsPanel(
    activeReminders: List<Reminder>,
    onNavigateToReminders: () -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = StageContactedColor
                    )
                    Text(
                        text = "Follow-up Action Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToReminders() }
                )
            }

            if (activeReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isScrollable) Modifier.weight(1f) else Modifier.height(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "No alerts",
                            tint = StageWonColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All caught up!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                if (isScrollable) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeReminders.take(3)) { reminder ->
                            AlertItemRow(reminder)
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeReminders.take(3).forEach { reminder ->
                            AlertItemRow(reminder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityTimelineBrief(
    activities: List<Activity>,
    onNavigateToLeadDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Timeline",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Live Activity Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isScrollable) Modifier.weight(1f) else Modifier.height(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activities logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                if (isScrollable) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activities.take(10)) { activity ->
                            ActivityTimelineItem(activity = activity, onNavigateToLeadDetail = onNavigateToLeadDetail)
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        activities.take(10).forEach { activity ->
                            ActivityTimelineItem(activity = activity, onNavigateToLeadDetail = onNavigateToLeadDetail)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityTimelineItem(
    activity: Activity,
    onNavigateToLeadDetail: (Int) -> Unit
) {
    val dateStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activity.timestamp))
    
    val (icon, tint) = when (activity.type.uppercase()) {
        "CREATION" -> Icons.Default.AddHomeWork to StageNewColor
        "STATUS" -> Icons.Default.SwapHoriz to StageProposalColor
        "CALL" -> Icons.Default.Call to StageWonColor
        "WHATSAPP" -> Icons.Default.Message to StageQualifiedColor
        "EMAIL" -> Icons.Default.Email to StageQualifiedColor
        else -> Icons.Default.Description to StageContactedColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToLeadDetail(activity.leadId) },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left timeline line & circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = activity.type, tint = tint, modifier = Modifier.size(16.dp))
            }
        }

        // Contents
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "By ${activity.actorName}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
