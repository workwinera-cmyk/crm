package com.example.data.repository

import com.example.data.local.ActivityDao
import com.example.data.local.LeadDao
import com.example.data.local.ReminderDao
import com.example.data.local.UserDao
import com.example.data.model.Activity
import com.example.data.model.Lead
import com.example.data.model.Reminder
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CrmRepository(
    private val userDao: UserDao,
    private val leadDao: LeadDao,
    private val activityDao: ActivityDao,
    private val reminderDao: ReminderDao
) {
    val allLeads: Flow<List<Lead>> = leadDao.getAllLeadsFlow()
    val allUsers: Flow<List<User>> = userDao.getAllUsersFlow()
    val allActivities: Flow<List<Activity>> = activityDao.getAllActivitiesFlow()
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllRemindersFlow()
    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveRemindersFlow()

    fun getLeadById(id: Int): Flow<Lead?> = leadDao.getLeadByIdFlow(id)
    fun getActivitiesForLead(leadId: Int): Flow<List<Activity>> = activityDao.getActivitiesForLeadFlow(leadId)

    suspend fun insertLead(lead: Lead, actor: String): Int {
        val id = leadDao.insertLead(lead).toInt()
        val formattedValue = String.format("$%,.2f", lead.value)
        activityDao.insertActivity(
            Activity(
                leadId = id,
                title = "Lead Created",
                description = "Created lead with company '${lead.company}' valued at $formattedValue.",
                type = "CREATION",
                actorName = actor
            )
        )
        return id
    }

    suspend fun updateLead(lead: Lead, actor: String) {
        val oldLead = leadDao.getLeadById(lead.id)
        leadDao.updateLead(lead)
        
        if (oldLead != null) {
            val changes = mutableListOf<String>()
            if (oldLead.name != lead.name) changes.add("Contact name changed from '${oldLead.name}' to '${lead.name}'")
            if (oldLead.company != lead.company) changes.add("Company changed to '${lead.company}'")
            if (oldLead.value != lead.value) {
                val oldVal = String.format("$%,.2f", oldLead.value)
                val newVal = String.format("$%,.2f", lead.value)
                changes.add("Deal value changed from $oldVal to $newVal")
            }
            if (oldLead.status != lead.status) changes.add("Stage moved from ${oldLead.status} to ${lead.status}")
            if (oldLead.assigneeEmail != lead.assigneeEmail) changes.add("Reassigned to ${lead.assigneeEmail}")
            if (oldLead.tagsAsString != lead.tagsAsString) changes.add("Tags updated: [${lead.tagsAsString}]")

            if (changes.isNotEmpty()) {
                activityDao.insertActivity(
                    Activity(
                        leadId = lead.id,
                        title = "Lead Updated",
                        description = changes.joinToString("; "),
                        type = if (oldLead.status != lead.status) "STATUS" else "NOTE",
                        actorName = actor
                    )
                )
            }
        }
    }

    suspend fun updateLeadStatus(leadId: Int, newStatus: String, actor: String) {
        val oldLead = leadDao.getLeadById(leadId)
        if (oldLead != null && oldLead.status != newStatus) {
            val timestamp = System.currentTimeMillis()
            leadDao.updateLeadStatus(leadId, newStatus, timestamp)
            activityDao.insertActivity(
                Activity(
                    leadId = leadId,
                    title = "Stage Moved",
                    description = "Sales pipeline stage updated from '${oldLead.status}' to '$newStatus'.",
                    type = "STATUS",
                    actorName = actor
                )
            )
        }
    }

    suspend fun deleteLead(lead: Lead) {
        leadDao.deleteLead(lead)
        activityDao.deleteActivitiesForLead(lead.id)
    }

    suspend fun addActivityLog(leadId: Int, title: String, description: String, type: String, actor: String) {
        activityDao.insertActivity(
            Activity(
                leadId = leadId,
                title = title,
                description = description,
                type = type,
                actorName = actor
            )
        )
    }

    suspend fun addReminder(reminder: Reminder) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(id: Int) {
        reminderDao.deleteReminderById(id)
    }

    suspend fun checkPendingReminders(actor: String): List<Reminder> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val pending = reminderDao.getPendingUnnotifiedReminders(now)
        for (rem in pending) {
            reminderDao.markAsNotified(rem.id)
            // Log alarm alert in activities so it is saved in timeline
            activityDao.insertActivity(
                Activity(
                    leadId = rem.leadId,
                    title = "Alarm Triggered",
                    description = "Follow-up Reminder alert shown: ${rem.title}.",
                    type = "NOTE",
                    actorName = "System"
                )
            )
        }
        return@withContext pending
    }

    suspend fun authenticateUser(email: String): User? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext userDao.getUserByEmail(email.lowercase().trim())
    }

    suspend fun populateDefaultsIfEmpty() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val existingUsers = userDao.getAllUsers()
        if (existingUsers.isNotEmpty()) return@withContext

        // Create initial users (including user's real email)
        val defaultUsers = listOf(
            User("work.winera@gmail.com", "Winera Work", UserRole.ADMIN, "#FF2C55"),
            User("admin@apexcrm.com", "Sarah Jenkins", UserRole.ADMIN, "#415A77"),
            User("manager@apexcrm.com", "David Thorne", UserRole.MANAGER, "#E07A5F"),
            User("rep@apexcrm.com", "Marcus Vance", UserRole.SALES_REP, "#3D5A80")
        )
        userDao.insertUsers(defaultUsers)

        // Seed realistic sales pipeline leads
        val leads = listOf(
            Lead(
                name = "Tony Stark",
                company = "Stark Industries",
                email = "tony@stark.com",
                phone = "18005550199",
                status = "NEW",
                value = 150000.0,
                assigneeEmail = "work.winera@gmail.com",
                tagsAsString = "High-Value, Hot, Tech",
                notes = "Interested in integrating clean arc reactor energy solutions with CRM databases. Initial contact made via email.",
                dateCreated = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
                dateUpdated = System.currentTimeMillis() - 86400000 * 5
            ),
            Lead(
                name = "Bruce Wayne",
                company = "Wayne Enterprises",
                email = "bwayne@wayne.com",
                phone = "15551234567",
                status = "CONTACTED",
                value = 85000.0,
                assigneeEmail = "rep@apexcrm.com",
                tagsAsString = "Enterprise, Warm",
                notes = "Expressed strong interest in bulk licensing. Security compliance department is reviewing our terms. Wants a call on their secured channels.",
                dateCreated = System.currentTimeMillis() - 86400000 * 4,
                dateUpdated = System.currentTimeMillis() - 86400000 * 2,
                nextFollowUp = System.currentTimeMillis() + 8640000 * 12 // Next ~1 day
            ),
            Lead(
                name = "John Smith",
                company = "ACME Logistics",
                email = "smith@acme.com",
                phone = "4159990155",
                status = "QUALIFIED",
                value = 45000.0,
                assigneeEmail = "manager@apexcrm.com",
                tagsAsString = "Mid-Market, Warm",
                notes = "Completed technical product walkthrough. Pricing fits budget limits perfectly, currently arranging decision maker meeting with CFO next Tuesday.",
                dateCreated = System.currentTimeMillis() - 86400000 * 10,
                dateUpdated = System.currentTimeMillis() - 86400000 * 1,
                nextFollowUp = System.currentTimeMillis() + (86400000 * 2) // Next 2 days
            ),
            Lead(
                name = "Arthur Dent",
                company = "Megadodo Publications",
                email = "adent@hitchhike.org",
                phone = "442079460192",
                status = "PROPOSAL",
                value = 25000.0,
                assigneeEmail = "rep@apexcrm.com",
                tagsAsString = "International, Demo Scheduled",
                notes = "Customized Service Level Agreement proposed last Friday. Reviewing terms including emergency escape provisions. Highly responsive lead.",
                dateCreated = System.currentTimeMillis() - 86400000 * 6,
                dateUpdated = System.currentTimeMillis() - 86400000 * 2
            ),
            Lead(
                name = "Peter Parker",
                company = "Daily Bugle",
                email = "photo@bugle.com",
                phone = "2125550143",
                status = "WON",
                value = 12000.0,
                assigneeEmail = "work.winera@gmail.com",
                tagsAsString = "Small-Business",
                notes = "Successfully onboarded. Customer is using the automated push scheduler tools to dispatch photojournalist alerts. Invoice fully paid.",
                dateCreated = System.currentTimeMillis() - 86400000 * 15,
                dateUpdated = System.currentTimeMillis() - 86400000 * 3
            ),
            Lead(
                name = "Charles Foster Kane",
                company = "The New York Inquirer",
                email = "publisher@inquirer.com",
                phone = "3125550100",
                status = "LOST",
                value = 95000.0,
                assigneeEmail = "admin@apexcrm.com",
                tagsAsString = "Disengaged",
                notes = "Decided to build a proprietary internal publishing tracker database instead. Friendly departure, will monitor again next year.",
                dateCreated = System.currentTimeMillis() - 86400000 * 25,
                dateUpdated = System.currentTimeMillis() - 86400000 * 8
            )
        )

        leads.forEach { lead ->
            val insertedId = leadDao.insertLead(lead).toInt()
            
            // Seed Activity Timeline with real, historical milestones matching the dates!
            if (lead.status == "NEW") {
                activityDao.insertActivity(
                    Activity(
                        leadId = insertedId,
                        title = "Lead Created",
                        description = "Hot lead captured through email integration. Initial evaluation completed.",
                        type = "CREATION",
                        timestamp = lead.dateCreated,
                        actorName = "Sarah Jenkins"
                    )
                )
            } else {
                activityDao.insertActivity(
                    Activity(
                        leadId = insertedId,
                        title = "Lead Ingested",
                        description = "Lead added manually to sales pipeline registry.",
                        type = "CREATION",
                        timestamp = lead.dateCreated,
                        actorName = "David Thorne"
                    )
                )
                
                activityDao.insertActivity(
                    Activity(
                        leadId = insertedId,
                        title = "Initial Call",
                        description = "Followed up with ${lead.name} to confirm business profile and qualify requirements.",
                        type = "CALL",
                        timestamp = lead.dateCreated + 3600000 * 2, // 2 hours later
                        actorName = "Marcus Vance"
                    )
                )

                if (lead.status == "QUALIFIED" || lead.status == "PROPOSAL" || lead.status == "WON" || lead.status == "LOST") {
                    activityDao.insertActivity(
                        Activity(
                            leadId = insertedId,
                            title = "Technical Demo Delivered",
                            description = "Walked through multi-column interface and real-time syncing module with engineering lead.",
                            type = "NOTE",
                            timestamp = lead.dateCreated + 86400000,
                            actorName = lead.assigneeEmail.substringBefore("@")
                        )
                    )
                    
                    activityDao.insertActivity(
                        Activity(
                            leadId = insertedId,
                            title = "Qualified",
                            description = "Requirement verified, budget confirmation secure, and moving to structured proposal stage.",
                            type = "STATUS",
                            timestamp = lead.dateCreated + 86400000 * 2,
                            actorName = "David Thorne"
                        )
                    )
                }

                if (lead.status == "PROPOSAL" || lead.status == "WON") {
                    activityDao.insertActivity(
                        Activity(
                            leadId = insertedId,
                            title = "Proposal Sent",
                            description = "Emailed formal service contract draft. Value logged as $${lead.value}.",
                            type = "EMAIL",
                            timestamp = lead.dateCreated + 86400000 * 3,
                            actorName = lead.assigneeEmail.substringBefore("@")
                        )
                    )
                }

                if (lead.status == "WON") {
                    activityDao.insertActivity(
                        Activity(
                            leadId = insertedId,
                            title = "Deal Won 🎉",
                            description = "Service agreement signed via digital credentials. Initial invoice processing complete. Fantastic win!",
                            type = "STATUS",
                            timestamp = lead.dateUpdated,
                            actorName = lead.assigneeEmail.substringBefore("@")
                        )
                    )
                } else if (lead.status == "LOST") {
                    activityDao.insertActivity(
                        Activity(
                            leadId = insertedId,
                            title = "Deal Lost ❌",
                            description = "Client backed out due to preference for building custom homebrew databases internally.",
                            type = "STATUS",
                            timestamp = lead.dateUpdated,
                            actorName = "Sarah Jenkins"
                        )
                    )
                }
            }

            // If there's a follow-up date, seed a Reminder too!
            lead.nextFollowUp?.let { followUp ->
                reminderDao.insertReminder(
                    Reminder(
                        leadId = insertedId,
                        leadName = lead.name,
                        title = "Follow-up Call with ${lead.name}",
                        note = "Secure feedback on proposal/pricing from security team.",
                        triggerTime = followUp
                    )
                )
            }
        }
    }
}
