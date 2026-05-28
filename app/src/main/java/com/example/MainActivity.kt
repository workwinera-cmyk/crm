package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.zIndex
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Lead
import com.example.data.model.User
import com.example.ui.components.AppNotificationBanner
import com.example.ui.components.UserAvatar
import com.example.ui.screen.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CrmViewModel
import com.example.ui.viewmodel.CrmViewModelFactory

enum class ScreenState {
    DASHBOARD,
    PIPELINE,
    LEAD_LIST,
    REMINDERS,
    DETAIL,
    NEW_LEAD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val vm: CrmViewModel = viewModel(factory = CrmViewModelFactory(application))

                // Observables
                val currentUser by vm.currentUser.collectAsStateWithLifecycle()
                val allUsers by vm.allUsers.collectAsStateWithLifecycle()
                val loginError by vm.loginError.collectAsStateWithLifecycle()

                val leads by vm.filteredLeads.collectAsStateWithLifecycle()
                val rawLeads by vm.allLeads.collectAsStateWithLifecycle() // Unfiltered
                val reminders by vm.allReminders.collectAsStateWithLifecycle()
                val activities by vm.allActivities.collectAsStateWithLifecycle()
                
                // Screen state machine
                var currentScreen by remember { mutableStateOf(ScreenState.DASHBOARD) }
                var selectedLeadId by remember { mutableStateOf<Int?>(null) }
                var editingLead by remember { mutableStateOf<Lead?>(null) }

                // Follow up Alarm active notifications state
                val activeBannerReminders by vm.activeDismissedNotifications.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    val user = currentUser
                    if (user == null) {
                        // Secure Login Screen
                        LoginScreen(
                            onLoginSuccess = { vm.login(it.email) },
                            allUsers = allUsers,
                            loginError = loginError,
                            onLoginClick = { vm.login(it) }
                        )
                    } else {
                        // Main CRM Workspace
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isExpanded = maxWidth > 600.dp

                            Scaffold(
                                bottomBar = {
                                    // Mobile bottom bar (only if screen is small)
                                    if (!isExpanded) {
                                        CRMBottomBar(
                                            activeScreen = currentScreen,
                                            onTabSelected = { screen ->
                                                currentScreen = screen
                                                selectedLeadId = null
                                                editingLead = null
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    // Sidebar Navigation Rail (Adaptive support for large screens / Tablets!)
                                    if (isExpanded) {
                                        CRMNavigationRail(
                                            activeScreen = currentScreen,
                                            currentUser = user,
                                            onTabSelected = { screen ->
                                                currentScreen = screen
                                                selectedLeadId = null
                                                editingLead = null
                                            },
                                            onLogout = { vm.logout() }
                                        )
                                    }

                                    // Content routing box
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        when (currentScreen) {
                                            ScreenState.DASHBOARD -> {
                                                DashboardScreen(
                                                    currentUser = user,
                                                    leads = rawLeads,
                                                    reminders = reminders,
                                                    activities = activities,
                                                    onNavigateToLeads = { currentScreen = ScreenState.LEAD_LIST },
                                                    onNavigateToReminders = { currentScreen = ScreenState.REMINDERS },
                                                    onNavigateToLeadDetail = { id ->
                                                        selectedLeadId = id
                                                        currentScreen = ScreenState.DETAIL
                                                    }
                                                )
                                            }
                                            ScreenState.PIPELINE -> {
                                                val searchTag by vm.selectedTagFilter.collectAsStateWithLifecycle()
                                                val searchAssignee by vm.selectedAssigneeFilter.collectAsStateWithLifecycle()
                                                
                                                PipelineScreen(
                                                    leads = LeadsRoleSecurityFilter(rawLeads, user),
                                                    users = allUsers,
                                                    selectedAssigneeFilter = searchAssignee,
                                                    selectedTagFilter = searchTag,
                                                    onAssigneeFilterChange = { vm.setAssigneeFilter(it) },
                                                    onTagFilterChange = { vm.setTagFilter(it) },
                                                    onMoveStage = { id, stage -> vm.moveLeadStage(id, stage) },
                                                    onNavigateToLeadDetail = { id ->
                                                        selectedLeadId = id
                                                        currentScreen = ScreenState.DETAIL
                                                    },
                                                    onClearFilters = { vm.clearFilters() }
                                                )
                                            }
                                            ScreenState.LEAD_LIST -> {
                                                val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
                                                val stageFilter by vm.selectedStageFilter.collectAsStateWithLifecycle()
                                                val assigneeFilter by vm.selectedAssigneeFilter.collectAsStateWithLifecycle()
                                                val tagFilter by vm.selectedTagFilter.collectAsStateWithLifecycle()

                                                LeadListScreen(
                                                    leads = leads,
                                                    users = allUsers,
                                                    searchQuery = searchQuery,
                                                    onSearchQueryChange = { vm.setSearchQuery(it) },
                                                    selectedStageFilter = stageFilter,
                                                    selectedAssigneeFilter = assigneeFilter,
                                                    selectedTagFilter = tagFilter,
                                                    onStageFilterChange = { vm.setStageFilter(it) },
                                                    onAssigneeFilterChange = { vm.setAssigneeFilter(it) },
                                                    onTagFilterChange = { vm.setTagFilter(it) },
                                                    onClearFilters = { vm.clearFilters() },
                                                    onNavigateToLeadDetail = { id ->
                                                        selectedLeadId = id
                                                        currentScreen = ScreenState.DETAIL
                                                    },
                                                    onFABClick = {
                                                        editingLead = null
                                                        currentScreen = ScreenState.NEW_LEAD
                                                    }
                                                )
                                            }
                                            ScreenState.REMINDERS -> {
                                                RemindersScreen(
                                                    leads = LeadsRoleSecurityFilter(rawLeads, user),
                                                    allReminders = reminders.filter { rem ->
                                                        // Enforce role metrics: reps only see reminders for their assigned leads
                                                        if (user.role == com.example.data.model.UserRole.SALES_REP) {
                                                            val parentLead = rawLeads.find { it.id == rem.leadId }
                                                            parentLead?.assigneeEmail?.equals(user.email, ignoreCase = true) == true
                                                        } else true
                                                    },
                                                    onAddReminder = { leadId, name, title, note, time ->
                                                        vm.addReminder(leadId, name, title, note, time)
                                                    },
                                                    onCompleteReminder = { vm.completeReminder(it) },
                                                    onDeleteReminder = { id -> vm.deleteReminder(id) }
                                                )
                                            }
                                            ScreenState.DETAIL -> {
                                                val targetLeadId = selectedLeadId ?: 0
                                                val leadActivities = activities.filter { it.leadId == targetLeadId }
                                                
                                                LeadDetailScreen(
                                                    leadId = targetLeadId,
                                                    leads = rawLeads,
                                                    users = allUsers,
                                                    currentUser = user,
                                                    leadActivities = leadActivities,
                                                    onBack = { currentScreen = ScreenState.DASHBOARD },
                                                    onEditLead = {
                                                        editingLead = rawLeads.find { it.id == targetLeadId }
                                                        currentScreen = ScreenState.NEW_LEAD
                                                    },
                                                    onMoveStage = { id, stage -> vm.moveLeadStage(id, stage) },
                                                    onAddActivity = { id, title, desc, type ->
                                                        vm.addManualActivityLog(id, title, desc, type)
                                                    },
                                                    onUpdateLead = { updatedLead -> vm.updateLead(updatedLead) },
                                                    onDeleteLead = { leadToDelete -> vm.deleteLead(leadToDelete) }
                                                )
                                            }
                                            ScreenState.NEW_LEAD -> {
                                                NewLeadScreen(
                                                    editingLead = editingLead,
                                                    users = allUsers,
                                                    onSaveLead = { name, company, email, phone, statusStr, valueInt, assigneeEmail, tagsStr, notesStr, delayTime ->
                                                        if (editingLead == null) {
                                                            vm.addLead(
                                                                name, company, email, phone, statusStr, valueInt, assigneeEmail, tagsStr, notesStr, delayTime
                                                            )
                                                        } else {
                                                            val updated = editingLead!!.copy(
                                                                name = name,
                                                                company = company,
                                                                email = email,
                                                                phone = phone,
                                                                status = statusStr,
                                                                value = valueInt,
                                                                assigneeEmail = assigneeEmail,
                                                                tagsAsString = tagsStr,
                                                                notes = notesStr,
                                                                dateUpdated = System.currentTimeMillis()
                                                            )
                                                            vm.updateLead(updated)
                                                        }
                                                        currentScreen = ScreenState.LEAD_LIST
                                                        editingLead = null
                                                    },
                                                    onCancel = {
                                                        currentScreen = ScreenState.LEAD_LIST
                                                        editingLead = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Interactive Push Notification Alarm banners
                    AnimatedVisibility(
                        visible = activeBannerReminders.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .zIndex(99f)
                    ) {
                        val activeItem = activeBannerReminders.firstOrNull()
                        if (activeItem != null) {
                            AppNotificationBanner(
                                reminder = activeItem,
                                onDismiss = { vm.removeNotificationFromBanner(activeItem.id) },
                                onGoToLead = { leadId ->
                                    selectedLeadId = leadId
                                    currentScreen = ScreenState.DETAIL
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Security filtering utility (enforcing role restriction on leads list)
fun LeadsRoleSecurityFilter(allLeads: List<Lead>, currentUser: User): List<Lead> {
    return if (currentUser.role == com.example.data.model.UserRole.SALES_REP) {
        allLeads.filter { it.assigneeEmail.equals(currentUser.email, ignoreCase = true) }
    } else allLeads
}

@Composable
fun CRMBottomBar(activeScreen: ScreenState, onTabSelected: (ScreenState) -> Unit) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = activeScreen == ScreenState.DASHBOARD,
            onClick = { onTabSelected(ScreenState.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = activeScreen == ScreenState.PIPELINE,
            onClick = { onTabSelected(ScreenState.PIPELINE) },
            icon = { Icon(Icons.Default.SwapCalls, "Pipeline") },
            label = { Text("Pipeline", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = activeScreen == ScreenState.LEAD_LIST,
            onClick = { onTabSelected(ScreenState.LEAD_LIST) },
            icon = { Icon(Icons.Default.Group, "Leads") },
            label = { Text("Leads", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = activeScreen == ScreenState.REMINDERS,
            onClick = { onTabSelected(ScreenState.REMINDERS) },
            icon = { Icon(Icons.Default.Alarm, "Follow-ups") },
            label = { Text("Tasks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        )
    }
}

@Composable
fun CRMNavigationRail(
    activeScreen: ScreenState,
    currentUser: User,
    onTabSelected: (ScreenState) -> Unit,
    onLogout: () -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(80.dp),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                UserAvatar(
                    name = currentUser.name,
                    email = currentUser.email,
                    size = 40.dp,
                    hexColor = currentUser.avatarColorHex
                )
                Text(
                    text = currentUser.name.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        NavigationRailItem(
            selected = activeScreen == ScreenState.DASHBOARD,
            onClick = { onTabSelected(ScreenState.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
            label = { Text("Dashboard") }
        )
        NavigationRailItem(
            selected = activeScreen == ScreenState.PIPELINE,
            onClick = { onTabSelected(ScreenState.PIPELINE) },
            icon = { Icon(Icons.Default.SwapCalls, "Pipeline") },
            label = { Text("Pipeline") }
        )
        NavigationRailItem(
            selected = activeScreen == ScreenState.LEAD_LIST,
            onClick = { onTabSelected(ScreenState.LEAD_LIST) },
            icon = { Icon(Icons.Default.Group, "Leads") },
            label = { Text("Leads") }
        )
        NavigationRailItem(
            selected = activeScreen == ScreenState.REMINDERS,
            onClick = { onTabSelected(ScreenState.REMINDERS) },
            icon = { Icon(Icons.Default.Alarm, "Follow-ups") },
            label = { Text("Tasks") }
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationRailItem(
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Default.ExitToApp, "Log Out", tint = MaterialTheme.colorScheme.error) },
            label = { Text("Exit", color = MaterialTheme.colorScheme.error) }
        )
    }
}
