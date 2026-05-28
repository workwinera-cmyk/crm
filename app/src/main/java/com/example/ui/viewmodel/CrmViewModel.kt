package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Activity
import com.example.data.model.Lead
import com.example.data.model.Reminder
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.CrmRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CrmViewModel(
    application: Application,
    private val repository: CrmRepository
) : AndroidViewModel(application) {

    // Authentication session state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStageFilter = MutableStateFlow<String?>(null)
    val selectedStageFilter: StateFlow<String?> = _selectedStageFilter.asStateFlow()

    private val _selectedAssigneeFilter = MutableStateFlow<String?>(null)
    val selectedAssigneeFilter: StateFlow<String?> = _selectedAssigneeFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    // Observable flows from database
    val allLeads: StateFlow<List<Lead>> = repository.allLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<Activity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<Reminder>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // In-app Push Notification stream (for simulation)
    private val _notificationTriggerStream = MutableSharedFlow<Reminder>(extraBufferCapacity = 5)
    val notificationTriggerStream: SharedFlow<Reminder> = _notificationTriggerStream.asSharedFlow()

    // Currently showing push alerts in UI
    private val _activeDismissedNotifications = MutableStateFlow<List<Reminder>>(emptyList())
    val activeDismissedNotifications: StateFlow<List<Reminder>> = _activeDismissedNotifications.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Populate defaults on startup
                repository.populateDefaultsIfEmpty()
            } catch (e: Exception) {
                android.util.Log.e("CrmViewModel", "Error populating database defaults", e)
            }
            
            try {
                // Auto log in if work.winera@gmail.com is present to make testing beautiful,
                // but user can easily switch profiles from login or settings.
                val devUser = repository.authenticateUser("work.winera@gmail.com")
                if (devUser != null) {
                    _currentUser.value = devUser
                }
            } catch (e: Exception) {
                android.util.Log.e("CrmViewModel", "Error authenticating dev user", e)
            }
        }

        // Run the periodic reminders check in a separate coroutine
        viewModelScope.launch {
            // Give database a brief moment to initialize and seed
            try {
                kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                // Handle delay cancellation
            }
            while (true) {
                try {
                    val actor = _currentUser.value?.name ?: "System"
                    val triggered = repository.checkPendingReminders(actor)
                    triggered.forEach { reminder ->
                        _notificationTriggerStream.emit(reminder)
                        _activeDismissedNotifications.update { list -> list + reminder }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CrmViewModel", "Exception checking pending reminders", e)
                }
                try {
                    kotlinx.coroutines.delay(2000)
                } catch (e: Exception) {
                    // Handle delay cancellation
                    break
                }
            }
        }
    }

    // Auth actions
    fun login(email: String) {
        viewModelScope.launch {
            try {
                val user = repository.authenticateUser(email)
                if (user != null) {
                    _currentUser.value = user
                    _loginError.value = null
                } else {
                    _loginError.value = "User not found. Try 'work.winera@gmail.com' (Admin) or 'rep@apexcrm.com' (Sales Rep)."
                }
            } catch (e: Exception) {
                android.util.Log.e("CrmViewModel", "Exception during login", e)
                _loginError.value = "Login failed: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // Lead operations
    fun addLead(name: String, company: String, email: String, phone: String, status: String, value: Double, assigneeEmail: String, tags: String, notes: String, nextFollowUp: Long? = null) {
        viewModelScope.launch {
            val actorName = _currentUser.value?.name ?: "Unknown Executive"
            val lead = Lead(
                name = name,
                company = company,
                email = email,
                phone = phone,
                status = status,
                value = value,
                assigneeEmail = assigneeEmail,
                tagsAsString = tags,
                notes = notes,
                nextFollowUp = nextFollowUp
            )
            val leadId = repository.insertLead(lead, actorName)
            if (nextFollowUp != null) {
                repository.addReminder(
                    Reminder(
                        leadId = leadId,
                        leadName = name,
                        title = "Follow-up schedule with $name",
                        note = "Scheduled reminder regarding deal update",
                        triggerTime = nextFollowUp
                    )
                )
            }
        }
    }

    fun updateLead(lead: Lead) {
        viewModelScope.launch {
            val actorName = _currentUser.value?.name ?: "Unknown Executive"
            repository.updateLead(lead, actorName)
            
            // Adjust associated reminder if follow-up changed
            if (lead.nextFollowUp != null) {
                val currentReminders = allReminders.value.filter { it.leadId == lead.id && !it.isCompleted }
                if (currentReminders.isEmpty()) {
                    repository.addReminder(
                        Reminder(
                            leadId = lead.id,
                            leadName = lead.name,
                            title = "Follow-up schedule with ${lead.name}",
                            note = "Scheduled reminder regarding deal update",
                            triggerTime = lead.nextFollowUp
                        )
                    )
                } else {
                    // Update the existing reminder time
                    val updatedRem = currentReminders.first().copy(triggerTime = lead.nextFollowUp, leadName = lead.name)
                    repository.updateReminder(updatedRem)
                }
            }
        }
    }

    fun moveLeadStage(leadId: Int, newStage: String) {
        viewModelScope.launch {
            val actorName = _currentUser.value?.name ?: "Unknown Executive"
            repository.updateLeadStatus(leadId, newStage, actorName)
        }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            repository.deleteLead(lead)
        }
    }

    // Activities
    fun addManualActivityLog(leadId: Int, title: String, description: String, type: String) {
        viewModelScope.launch {
            val actorName = _currentUser.value?.name ?: "Unknown Executive"
            repository.addActivityLog(leadId, title, description, type, actorName)
        }
    }

    // Reminders
    fun addReminder(leadId: Int, leadName: String, title: String, note: String, triggerTime: Long) {
        viewModelScope.launch {
            repository.addReminder(
                Reminder(
                    leadId = leadId,
                    leadName = leadName,
                    title = title,
                    note = note,
                    triggerTime = triggerTime
                )
            )
        }
    }

    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = true))
            // Log in activity timeline
            _currentUser.value?.let { user ->
                repository.addActivityLog(
                    leadId = reminder.leadId,
                    title = "Reminder Cleared",
                    description = "Follow-up task completed: '${reminder.title}'.",
                    type = "NOTE",
                    actor = user.name
                )
            }
        }
    }

    fun deleteReminder(reminderId: Int) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
        }
    }

    fun removeNotificationFromBanner(reminderId: Int) {
        _activeDismissedNotifications.update { list ->
            list.filterNot { it.id == reminderId }
        }
    }

    // Filter Controls
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStageFilter(stage: String?) {
        _selectedStageFilter.value = stage
    }

    fun setAssigneeFilter(email: String?) {
        _selectedAssigneeFilter.value = email
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedStageFilter.value = null
        _selectedAssigneeFilter.value = null
        _selectedTagFilter.value = null
    }

    // Get filtered list of leads
    val filteredLeads: StateFlow<List<Lead>> = combine(
        allLeads,
        _searchQuery,
        _selectedStageFilter,
        _selectedAssigneeFilter,
        _selectedTagFilter,
        _currentUser
    ) { arrayOfFlows ->
        val leads = arrayOfFlows[0] as List<Lead>
        val query = arrayOfFlows[1] as String
        val stage = arrayOfFlows[2] as String?
        val assignee = arrayOfFlows[3] as String?
        val tag = arrayOfFlows[4] as String?
        val user = arrayOfFlows[5] as User?

        var result = leads

        // If simple Sales Rep is logged in, only show leads assigned to them to enforce "role-based logic"
        if (user != null && user.role == UserRole.SALES_REP) {
            result = result.filter { it.assigneeEmail.equals(user.email, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.company.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.phone.contains(q) ||
                it.notes.lowercase().contains(q)
            }
        }

        if (stage != null) {
            result = result.filter { it.status.equals(stage, ignoreCase = true) }
        }

        if (assignee != null) {
            result = result.filter { it.assigneeEmail.equals(assignee, ignoreCase = true) }
        }

        if (tag != null) {
            result = result.filter { it.tags.any { t -> t.equals(tag, ignoreCase = true) } }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Suppress("UNCHECKED_CAST")
class CrmViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrmViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = CrmRepository(
                database.userDao(),
                database.leadDao(),
                database.activityDao(),
                database.reminderDao()
            )
            return CrmViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
