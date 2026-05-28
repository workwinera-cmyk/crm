package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.Reminder
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    leads: List<Lead>,
    allReminders: List<Reminder>,
    onAddReminder: (leadId: Int, leadName: String, title: String, note: String, triggerTime: Long) -> Unit,
    onCompleteReminder: (Reminder) -> Unit,
    onDeleteReminder: (Int) -> Unit
) {
    var showActiveOnly by remember { mutableStateOf(true) }

    // Scheduler Form state
    var selectedLeadIndex by remember { mutableStateOf(0) }
    var reminderTitle by remember { mutableStateOf("") }
    var reminderNote by remember { mutableStateOf("") }
    var selectedDelayOption by remember { mutableStateOf("SEC_10") } // SEC_10, MIN_1, MIN_5, HOUR_1, DAY_1

    var showSchedulerDialog by remember { mutableStateOf(false) }

    val filteredReminders = remember(allReminders, showActiveOnly) {
        if (showActiveOnly) {
            allReminders.filter { !it.isCompleted }
        } else {
            allReminders.filter { it.isCompleted }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (leads.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showSchedulerDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddAlarm, contentDescription = "Add Alarm Task")
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reminders Tab Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                TabRow(
                    selectedTabIndex = if (showActiveOnly) 0 else 1,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Tab(
                        selected = showActiveOnly,
                        onClick = { showActiveOnly = true }
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AlarmOn, "Active")
                                Text("Active Reminders", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Tab(
                        selected = !showActiveOnly,
                        onClick = { showActiveOnly = false }
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AssignmentTurnedIn, "Completed")
                                Text("Completed logs", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (filteredReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "No tasks",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (showActiveOnly) "No active sales tasks scheduled." else "No historical records in archive.",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Log tasks using the scheduler button.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReminders, key = { it.id }) { reminder ->
                        ReminderListItemRow(
                            reminder = reminder,
                            onComplete = { onCompleteReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showSchedulerDialog && leads.isNotEmpty()) {
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSchedulerDialog = false },
            title = { Text("Schedule Quick Follow-up Alarm", fontWeight = FontWeight.Black) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Prospect Lead:")
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(leads[selectedLeadIndex].name + " (" + leads[selectedLeadIndex].company + ")")
                            Icon(Icons.Default.ArrowDropDown, "dropdown")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            leads.forEachIndexed { idx, lead ->
                                DropdownMenuItem(
                                    text = { Text("${lead.name} (${lead.company})") },
                                    onClick = {
                                        selectedLeadIndex = idx
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Reminder Title") },
                        placeholder = { Text("Call to ask for budget updates") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reminderNote,
                        onValueChange = { reminderNote = it },
                        label = { Text("Private alarm brief/note") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Delay and Schedule Timer:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "SEC_10" to "10s ⏳",
                            "MIN_1" to "1m",
                            "MIN_5" to "5m",
                            "HOUR_1" to "1h"
                        ).forEach { (optVal, label) ->
                            val activeVal = selectedDelayOption == optVal
                            ElevatedFilterChip(
                                selected = activeVal,
                                onClick = { selectedDelayOption = optVal },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reminderTitle.isNotBlank()) {
                            val targetLead = leads[selectedLeadIndex]
                            val currentDelay = when (selectedDelayOption) {
                                "SEC_10" -> 10000L
                                "MIN_1" -> 60000L
                                "MIN_5" -> 300000L
                                "HOUR_1" -> 3600000L
                                else -> 86400000L // 1 day fallback
                            }
                            val triggerMs = System.currentTimeMillis() + currentDelay
                            onAddReminder(targetLead.id, targetLead.name, reminderTitle, reminderNote, triggerMs)
                            reminderTitle = ""
                            reminderNote = ""
                            showSchedulerDialog = false
                        }
                    }
                ) {
                    Text("Arm Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSchedulerDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun ReminderListItemRow(
    reminder: Reminder,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(reminder.triggerTime))
    val isPast = reminder.triggerTime <= System.currentTimeMillis() && !reminder.isCompleted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button checkbox
            IconButton(onClick = onComplete, enabled = !reminder.isCompleted) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Check",
                    tint = if (reminder.isCompleted) StageWonColor else if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                    color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lead Account: ${reminder.leadName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (reminder.note.isNotBlank()) {
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Due Date",
                        tint = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (isPast) {
                        Text(
                            text = "(Overdue follow-up alert!)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Delete action logs
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Reminder",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}
