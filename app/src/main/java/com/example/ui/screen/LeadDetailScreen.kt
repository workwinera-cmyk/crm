package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Activity
import com.example.data.model.Lead
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadDetailScreen(
    leadId: Int,
    leads: List<Lead>,
    users: List<User>,
    currentUser: User,
    leadActivities: List<Activity>,
    onBack: () -> Unit,
    onEditLead: () -> Unit,
    onMoveStage: (Int, String) -> Unit,
    onAddActivity: (leadId: Int, title: String, description: String, type: String) -> Unit,
    onUpdateLead: (Lead) -> Unit,
    onDeleteLead: (Lead) -> Unit
) {
    val context = LocalContext.current
    val lead = leads.find { it.id == leadId }

    if (lead == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lead not found or has been deleted.", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val assigneeUser = users.find { it.email == lead.assigneeEmail }
    val stages = listOf("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST")
    
    // Note Logging local state
    var noteContent by remember { mutableStateOf("") }
    
    // Quick Reassign Dialogs
    var showReassignDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lead.company, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditLead) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    
                    // True Role-Based Permissions: Delete is restricted to ADMIN and MANAGER
                    if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER) {
                        var showDeleteWarning by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteWarning = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Lead", tint = MaterialTheme.colorScheme.error)
                        }

                        if (showDeleteWarning) {
                            AlertDialog(
                                onDismissRequest = { showDeleteWarning = false },
                                title = { Text("Permanently delete lead?") },
                                text = { Text("This will destroy all related timeline active logs and follow-up reminds logs. This action is destructive and irreversible.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            onDeleteLead(lead)
                                            showDeleteWarning = false
                                            onBack()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Destroy Lead Record")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteWarning = false }) {
                                        Text("Dismiss")
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWide = maxWidth > 650.dp
            
            if (isWide) {
                // Wide Screen Multi-Pane Detail layout (Aesthetic Layout depth)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LeadProfileCardView(
                            lead = lead,
                            assignee = assigneeUser,
                            onDial = { triggerCallLauncher(context, lead.phone) },
                            onWhatsApp = { triggerWhatsAppLauncher(context, lead.phone) },
                            onReassignClick = { showReassignDialog = true }
                        )

                        StageManipulatorGrid(
                            currentStage = lead.status,
                            stages = stages,
                            onMoveStage = { newStage -> onMoveStage(lead.id, newStage) }
                        )

                        ActionTimberLoggingBox(
                            noteContent = noteContent,
                            onNoteContentChange = { noteContent = it },
                            onLogNote = {
                                if (noteContent.isNotBlank()) {
                                    onAddActivity(lead.id, "Note Added", noteContent, "NOTE")
                                    onUpdateLead(lead.copy(notes = noteContent, dateUpdated = System.currentTimeMillis()))
                                    noteContent = ""
                                }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActivityLogsPane(activities = leadActivities)
                    }
                }
            } else {
                // Mobile Normal scrolling column
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                    item {
                        LeadProfileCardView(
                            lead = lead,
                            assignee = assigneeUser,
                            onDial = { triggerCallLauncher(context, lead.phone) },
                            onWhatsApp = { triggerWhatsAppLauncher(context, lead.phone) },
                            onReassignClick = { showReassignDialog = true }
                        )
                    }

                    item {
                        StageManipulatorGrid(
                            currentStage = lead.status,
                            stages = stages,
                            onMoveStage = { newStage -> onMoveStage(lead.id, newStage) }
                        )
                    }

                    item {
                        ActionTimberLoggingBox(
                            noteContent = noteContent,
                            onNoteContentChange = { noteContent = it },
                            onLogNote = {
                                if (noteContent.isNotBlank()) {
                                    onAddActivity(lead.id, "Note Added", noteContent, "NOTE")
                                    onUpdateLead(lead.copy(notes = noteContent, dateUpdated = System.currentTimeMillis()))
                                    noteContent = ""
                                }
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Lead Activity Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (leadActivities.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No milestones in history.")
                                }
                            }
                        }
                    } else {
                        items(leadActivities, key = { it.id }) { item ->
                            LeadActivityTimelineDetailRow(activity = item)
                        }
                    }
                }
            }
        }
    }

    if (showReassignDialog) {
        AlertDialog(
            onDismissRequest = { showReassignDialog = false },
            title = { Text("Reassign Representative") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a sales executive to transfer responsibility for tracking this account.")
                    users.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onUpdateLead(lead.copy(assigneeEmail = user.email, dateUpdated = System.currentTimeMillis()))
                                    onAddActivity(lead.id, "Account Reassigned", "Transferred representative assignment to ${user.name}.", "STATUS")
                                    showReassignDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserAvatar(name = user.name, email = user.email, size = 32.dp, hexColor = user.avatarColorHex)
                            Column {
                                Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(user.role.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReassignDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LeadProfileCardView(
    lead: Lead,
    assignee: User?,
    onDial: () -> Unit,
    onWhatsApp: () -> Unit,
    onReassignClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First Row: Company name & value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Representing: ${lead.company}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                StatusBadge(status = lead.status)
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Contact specs
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ContactRowInfo(icon = Icons.Default.Email, label = "Email Address", value = lead.email.ifBlank { "Not provided" })
                ContactRowInfo(icon = Icons.Default.Phone, label = "Phone Number", value = lead.phone)
                ContactRowInfo(
                    icon = Icons.Default.MonetizationOn, 
                    label = "Project Valuation", 
                    value = String.format("$%,.2f", lead.value),
                    textColor = MaterialTheme.colorScheme.primary
                )
            }

            // Tags block
            if (lead.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    lead.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Assignee Badge click
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onReassignClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserAvatar(
                        name = assignee?.name ?: "Assignee",
                        email = lead.assigneeEmail,
                        size = 32.dp,
                        hexColor = assignee?.avatarColorHex ?: "#FF555555"
                    )
                    Column {
                        Text(
                            text = assignee?.name ?: "Assignee Representative",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lead Executive Specialist",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(Icons.Default.SwapHoriz, "Reassign", tint = MaterialTheme.colorScheme.primary)
            }

            // Direct Action Triggers (Dials / Whatsapp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDial,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageWonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, "Dial")
                    Spacer(Modifier.width(8.dp))
                    Text("Call Representative")
                }

                Button(
                    onClick = onWhatsApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageQualifiedColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Forum, "WhatsApp")
                    Spacer(Modifier.width(8.dp))
                    Text("Message WhatsApp")
                }
            }
        }
    }
}

@Composable
fun ContactRowInfo(icon: ImageVector, label: String, value: String, textColor: Color = Color.Unspecified) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun StageManipulatorGrid(
    currentStage: String,
    stages: List<String>,
    onMoveStage: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Rapid Stage Progression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stages.take(3).forEach { stage ->
                    val isActive = currentStage.equals(stage, ignoreCase = true)
                    ElevatedFilterChip(
                        selected = isActive,
                        onClick = { onMoveStage(stage) },
                        label = { Text(stage, fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stages.drop(3).forEach { stage ->
                    val isActive = currentStage.equals(stage, ignoreCase = true)
                    val chipColor = if (isActive) {
                        when (stage) {
                            "WON" -> FilterChipDefaults.elevatedFilterChipColors(selectedContainerColor = StageWonColor, selectedLabelColor = Color.White)
                            "LOST" -> FilterChipDefaults.elevatedFilterChipColors(selectedContainerColor = StageLostColor, selectedLabelColor = Color.White)
                            else -> FilterChipDefaults.elevatedFilterChipColors()
                        }
                    } else FilterChipDefaults.elevatedFilterChipColors()

                    ElevatedFilterChip(
                        selected = isActive,
                        onClick = { onMoveStage(stage) },
                        colors = chipColor,
                        label = { Text(stage, fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionTimberLoggingBox(
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    onLogNote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Add Internal Account Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = noteContent,
                onValueChange = onNoteContentChange,
                placeholder = { Text("Log a customer email, status change, or private briefing updates...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                maxLines = 3
            )

            Button(
                onClick = onLogNote,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddComment, "Log note")
                Spacer(Modifier.width(6.dp))
                Text("Log Notes Update", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActivityLogsPane(activities: List<Activity>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Full Activity Log history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities recorded for this lead.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(activities, key = { it.id }) { activity ->
                        LeadActivityTimelineDetailRow(activity = activity)
                    }
                }
            }
        }
    }
}

@Composable
fun LeadActivityTimelineDetailRow(activity: Activity) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(activity.timestamp))

    val (icon, tint) = when (activity.type.uppercase()) {
        "CREATION" -> Icons.Default.AddHomeWork to StageNewColor
        "STATUS" -> Icons.Default.SwapHoriz to StageProposalColor
        "CALL" -> Icons.Default.Call to StageWonColor
        "WHATSAPP" -> Icons.Default.Message to StageQualifiedColor
        "EMAIL" -> Icons.Default.Email to StageQualifiedColor
        else -> Icons.Default.Description to StageContactedColor
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = activity.type, tint = tint, modifier = Modifier.size(16.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Text(
                activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Logged by ${activity.actorName}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
