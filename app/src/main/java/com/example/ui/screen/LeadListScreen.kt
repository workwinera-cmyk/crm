package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadListScreen(
    leads: List<Lead>,
    users: List<User>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStageFilter: String?,
    selectedAssigneeFilter: String?,
    selectedTagFilter: String?,
    onStageFilterChange: (String?) -> Unit,
    onAssigneeFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onNavigateToLeadDetail: (Int) -> Unit,
    onFABClick: () -> Unit
) {
    val context = LocalContext.current
    var sortingOption by remember { mutableStateOf("DATE_UPDATED") } // DATE_UPDATED, VALUE_DESC, VALUE_ASC, NAME
    var showSortMenu by remember { mutableStateOf(false) }

    // Pre-calculate distinct tags for filter pills
    val allTags = leads.flatMap { it.tags }.distinct()

    val sortedList = remember(leads, sortingOption) {
        when (sortingOption) {
            "VALUE_DESC" -> leads.sortedByDescending { it.value }
            "VALUE_ASC" -> leads.sortedBy { it.value }
            "NAME" -> leads.sortedBy { it.name.lowercase() }
            else -> leads.sortedByDescending { it.dateUpdated } // Default modified date
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFABClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 16.dp).testTag("add_lead_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Lead")
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
        ) {
            // Header search & filters
            CRMSearchAndControlBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                sortingOption = sortingOption,
                onSortingOptionChange = { sortingOption = it },
                showSortMenu = showSortMenu,
                onSortMenuToggle = { showSortMenu = it }
            )

            // active filters bar
            if (selectedStageFilter != null || selectedAssigneeFilter != null || selectedTagFilter != null) {
                ActiveFiltersBadgeBar(
                    stage = selectedStageFilter,
                    assignee = selectedAssigneeFilter,
                    tag = selectedTagFilter,
                    users = users,
                    onStageClear = { onStageFilterChange(null) },
                    onAssigneeClear = { onAssigneeFilterChange(null) },
                    onTagClear = { onTagFilterChange(null) },
                    onClearAll = onClearFilters
                )
            }

            // Quick Filter Chips horizontal row
            QuickStageFilterScrollRow(
                selectedStage = selectedStageFilter,
                onStageSelect = onStageFilterChange
            )

            // Results List
            if (sortedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupWork,
                            contentDescription = "No leads found",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No leads matches search parameters.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Try adjusting tags, status column, or names.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        if (selectedStageFilter != null || selectedAssigneeFilter != null || selectedTagFilter != null || searchQuery.isNotEmpty()) {
                            Button(onClick = onClearFilters) {
                                Text("Reset All Filters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(sortedList, key = { it.id }) { lead ->
                        val assignedUser = users.find { it.email == lead.assigneeEmail }
                        
                        LeadListItemCard(
                            lead = lead,
                            assignedUser = assignedUser,
                            onClick = { onNavigateToLeadDetail(lead.id) },
                            onCallClick = { triggerCallLauncher(context, lead.phone) },
                            onWhatsAppClick = { triggerWhatsAppLauncher(context, lead.phone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CRMSearchAndControlBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortingOption: String,
    onSortingOptionChange: (String) -> Unit,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name, company, email...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, "Clear query")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Box {
            IconButton(
                onClick = { onSortMenuToggle(true) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { onSortMenuToggle(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Sort by Date Updated") },
                    leadingIcon = { Icon(Icons.Default.Update, "Date") },
                    onClick = {
                        onSortingOptionChange("DATE_UPDATED")
                        onSortMenuToggle(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Deal Value (High -> Low)") },
                    leadingIcon = { Icon(Icons.Default.TrendingUp, "Val high") },
                    onClick = {
                        onSortingOptionChange("VALUE_DESC")
                        onSortMenuToggle(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Deal Value (Low -> High)") },
                    leadingIcon = { Icon(Icons.Default.TrendingDown, "Val low") },
                    onClick = {
                        onSortingOptionChange("VALUE_ASC")
                        onSortMenuToggle(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Contact Name") },
                    leadingIcon = { Icon(Icons.Default.SortByAlpha, "Name") },
                    onClick = {
                        onSortingOptionChange("NAME")
                        onSortMenuToggle(false)
                    }
                )
            }
        }
    }
}

@Composable
fun QuickStageFilterScrollRow(
    selectedStage: String?,
    onStageSelect: (String?) -> Unit
) {
    val stages = listOf("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InputChip(
            selected = selectedStage == null,
            onClick = { onStageSelect(null) },
            label = { Text("All Stages") }
        )

        stages.forEach { stage ->
            val isSelected = selectedStage == stage
            InputChip(
                selected = isSelected,
                onClick = { onStageSelect(stage) },
                label = { Text(stage) }
            )
        }
    }
}

@Composable
fun ActiveFiltersBadgeBar(
    stage: String?,
    assignee: String?,
    tag: String?,
    users: List<User>,
    onStageClear: () -> Unit,
    onAssigneeClear: () -> Unit,
    onTagClear: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filtered by:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stage?.let {
                FilterBadgeToken(text = "Stage: $it", onClear = onStageClear)
            }
            assignee?.let { email ->
                val name = users.find { it.email == email }?.name ?: email
                FilterBadgeToken(text = "Rep: ${name.substringBefore(" ")}", onClear = onAssigneeClear)
            }
            tag?.let {
                FilterBadgeToken(text = "Tag: $it", onClear = onTagClear)
            }
        }

        IconButton(onClick = onClearAll, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Clear, "Clear filters", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FilterBadgeToken(text: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
fun LeadListItemCard(
    lead: Lead,
    assignedUser: User?,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Body info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = lead.company,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusBadge(status = lead.status)
                }
                
                Text(
                    text = "${lead.name} • ${lead.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Assignee & value details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LeadValueText(value = lead.value, style = MaterialTheme.typography.bodyLarge)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        UserAvatar(
                            name = assignedUser?.name ?: "Assignee",
                            email = lead.assigneeEmail,
                            size = 20.dp,
                            hexColor = assignedUser?.avatarColorHex ?: "#FF444444"
                        )
                        Text(
                            text = assignedUser?.name?.substringBefore(" ") ?: "Assignee",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (lead.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lead.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Call / WhatsApp Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StageWonColor.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call lead",
                        tint = StageWonColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StageQualifiedColor.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "WhatsApp Chat",
                        tint = StageQualifiedColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
