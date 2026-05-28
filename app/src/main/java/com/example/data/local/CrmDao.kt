package com.example.data.local

import androidx.room.*
import com.example.data.model.Activity
import com.example.data.model.Lead
import com.example.data.model.Reminder
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY dateUpdated DESC")
    fun getAllLeadsFlow(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    fun getLeadByIdFlow(id: Int): Flow<Lead?>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    suspend fun getLeadById(id: Int): Lead?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    @Query("UPDATE leads SET status = :newStatus, dateUpdated = :timestamp WHERE id = :leadId")
    suspend fun updateLeadStatus(leadId: Int, newStatus: String, timestamp: Long)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY timestamp DESC")
    fun getAllActivitiesFlow(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE leadId = :leadId ORDER BY timestamp DESC")
    fun getActivitiesForLeadFlow(leadId: Int): Flow<List<Activity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: Activity): Long

    @Query("DELETE FROM activities WHERE leadId = :leadId")
    suspend fun deleteActivitiesForLead(leadId: Int)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerTime ASC")
    fun getActiveRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE triggerTime <= :now AND isNotified = 0 AND isCompleted = 0")
    suspend fun getPendingUnnotifiedReminders(now: Long): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isNotified = 1 WHERE id = :id")
    suspend fun markAsNotified(id: Int)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
}
