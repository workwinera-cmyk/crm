package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.User
import com.example.ui.components.LeadValueText
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    leads: List<Lead>,
    users: List<User>,
    selectedAssigneeFilter: String?,
    selectedTagFilter: String?,
    onAssigneeFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onMoveStage: (Int, String) -> Unit,
    onNavigateToLeadDetail: (Int) -> Unit,
    onClearFilters: () -> Unit
) {
    val stages = listOf("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST")
    val horizontalScrollState = rememberScrollState()

    // Extract all unique tags for filter
    val allTags = leads.flatMap { it.tags }.distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Stats & Filter Bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sales Pipeline Kanban",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (selectedAssigneeFilter != null || selectedTagFilter != null) {
                        TextButton(onClick = onClearFilters) {
                            Icon(Icons.Default.ClearAll, "Clear")
                            Spacer(Modifier.width(4.dp))
                            Text("Clear Filters", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Assignee Filter dropdown
                    var assigneeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { assigneeExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAssigneeFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedAssigneeFilter != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Person, "Team", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = users.find { it.email == selectedAssigneeFilter }?.name?.substringBefore(" ") ?: "Team",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, "down")
                        }
                        DropdownMenu(
                            expanded = assigneeExpanded,
                            onDismissRequest = { assigneeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Team Members") },
                                onClick = { onAssigneeFilterChange(null); assigneeExpanded = false }
                            )
                            users.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name) },
                                    leadingIcon = { UserAvatar(name = user.name, email = user.email, size = 20.dp, hexColor = user.avatarColorHex) },
                                    onClick = { onAssigneeFilterChange(user.email); assigneeExpanded = false }
                                )
                            }
                        }
                    }

                    // Tag Filter dropdown
                    var tagExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { tagExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTagFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedTagFilter != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.LocalOffer, "Tag", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = selectedTagFilter ?: "Tag",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, "down")
                        }
                        DropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Tags") },
                                onClick = { onTagFilterChange(null); tagExpanded = false }
                            )
                            allTags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    leadingIcon = { Icon(Icons.Default.Label, "Tag", modifier = Modifier.size(16.dp)) },
                                    onClick = { onTagFilterChange(tag); tagExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Kanban Board Columns Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            stages.forEach { stage ->
                val stageLeads = leads.filter { it.status.equals(stage, ignoreCase = true) }
                val stageValueSum = stageLeads.sumOf { it.value }

                KanbanColumn(
                    stageName = stage,
                    stageLeads = stageLeads,
                    stageValuation = stageValueSum,
                    users = users,
                    onMoveStage = onMoveStage,
                    onNavigateToLeadDetail = onNavigateToLeadDetail,
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun KanbanColumn(
    stageName: String,
    stageLeads: List<Lead>,
    stageValuation: Double,
    users: List<User>,
    onMoveStage: (Int, String) -> Unit,
    onNavigateToLeadDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnColor = when (stageName) {
        "NEW" -> StageNewColor
        "CONTACTED" -> StageContactedColor
        "QUALIFIED" -> StageQualifiedColor
        "PROPOSAL" -> StageProposalColor
        "WON" -> StageWonColor
        "LOST" -> StageLostColor
        else -> MaterialTheme.colorScheme.primary
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(8.dp)
        ) {
            // Column Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = columnColor.copy(alpha = 0.12f),
                    contentColor = columnColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(columnColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stageLeads.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = String.format("$%,.0f total val", stageValuation),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Cards list
            if (stageLeads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No leads in this stage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stageLeads, key = { it.id }) { lead ->
                        KanbanLeadCard(
                            lead = lead,
                            users = users,
                            onMoveStage = onMoveStage,
                            onNavigateToLeadDetail = onNavigateToLeadDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanLeadCard(
    lead: Lead,
    users: List<User>,
    onMoveStage: (Int, String) -> Unit,
    onNavigateToLeadDetail: (Int) -> Unit
) {
    val assignedUser = users.find { it.email == lead.assigneeEmail }
    val stages = listOf("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST")
    val currentIdx = stages.indexOf(lead.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToLeadDetail(lead.id) }
            .testTag("task_item_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First Row: Company & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.company,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = lead.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LeadValueText(value = lead.value, style = MaterialTheme.typography.bodyMedium)
            }

            // Tags row
            if (lead.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lead.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bottom Actions row: Assignee Avatar + Stage Shifter!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserAvatar(
                        name = assignedUser?.name ?: "Rep",
                        email = lead.assigneeEmail,
                        size = 22.dp,
                        hexColor = assignedUser?.avatarColorHex ?: "#FF3D5A80"
                    )
                    Text(
                        text = assignedUser?.name?.substringBefore(" ") ?: "Assignee",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Interactive Quick Slide Stage controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Left Shift
                    IconButton(
                        onClick = {
                            if (currentIdx > 0) {
                                onMoveStage(lead.id, stages[currentIdx - 1])
                            }
                        },
                        enabled = currentIdx > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Move Left",
                            tint = if (currentIdx > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Direct Stage Selector Trigger
                    var triggerSelector by remember { mutableStateOf(false) }
                    Box {
                        Icon(
                            imageVector = Icons.Default.SwapCalls,
                            contentDescription = "Direct Shift",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { triggerSelector = true }
                                .padding(4.dp)
                        )
                        DropdownMenu(
                            expanded = triggerSelector,
                            onDismissRequest = { triggerSelector = false }
                        ) {
                            stages.forEach { stage ->
                                DropdownMenuItem(
                                    text = { Text(stage, fontWeight = if (lead.status == stage) FontWeight.Black else FontWeight.Normal) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FiberManualRecord,
                                            contentDescription = "Status dot",
                                            tint = when (stage) {
                                                "NEW" -> StageNewColor
                                                "CONTACTED" -> StageContactedColor
                                                "QUALIFIED" -> StageQualifiedColor
                                                "PROPOSAL" -> StageProposalColor
                                                "WON" -> StageWonColor
                                                "LOST" -> StageLostColor
                                                else -> MaterialTheme.colorScheme.outline
                                            },
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    onClick = {
                                        onMoveStage(lead.id, stage)
                                        triggerSelector = false
                                    }
                                )
                            }
                        }
                    }

                    // Right Shift
                    IconButton(
                        onClick = {
                            if (currentIdx < stages.lastIndex) {
                                onMoveStage(lead.id, stages[currentIdx + 1])
                            }
                        },
                        enabled = currentIdx < stages.lastIndex,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Move Right",
                            tint = if (currentIdx < stages.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
