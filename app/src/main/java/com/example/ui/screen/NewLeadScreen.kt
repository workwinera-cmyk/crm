package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.User
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLeadScreen(
    editingLead: Lead? = null,
    users: List<User>,
    onSaveLead: (name: String, company: String, email: String, phone: String, status: String, valRange: Double, assigneeEmail: String, tags: String, notes: String, alarmDelayMs: Long?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(editingLead?.name ?: "") }
    var company by remember { mutableStateOf(editingLead?.company ?: "") }
    var email by remember { mutableStateOf(editingLead?.email ?: "") }
    var phone by remember { mutableStateOf(editingLead?.phone ?: "") }
    var status by remember { mutableStateOf(editingLead?.status ?: "NEW") }
    var dealValue by remember { mutableStateOf(editingLead?.value?.toString() ?: "15000") }
    var assigneeEmail by remember { mutableStateOf(editingLead?.assigneeEmail ?: "work.winera@gmail.com") }
    var tags by remember { mutableStateOf(editingLead?.tagsAsString ?: "") }
    var notes by remember { mutableStateOf(editingLead?.notes ?: "") }

    // Alarm simulation options
    var selectedAlarmOption by remember { mutableStateOf("NONE") } // NONE, SEC_10, MIN_1, DAY_1

    val stages = listOf("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST")
    val scrollState = rememberScrollState()

    var assigneeDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingLead == null) "Configure New Lead" else "Modify Lead Info", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Lead Profile Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name *") },
                        leadingIcon = { Icon(Icons.Default.Business, "Company") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Person Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, "Rep") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, "Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Valuation input
                    OutlinedTextField(
                        value = dealValue,
                        onValueChange = { dealValue = it },
                        label = { Text("Estimated Deal Valuation ($) *") },
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, "Valuation") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Pipeline Logistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Pipeline status Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pipeline Status Stage") },
                            leadingIcon = { Icon(Icons.Default.SwapCalls, "Stage") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "dropdown") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { statusDropdownExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        DropdownMenu(
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            stages.forEach { stageName ->
                                DropdownMenuItem(
                                    text = { Text(stageName) },
                                    onClick = {
                                        status = stageName
                                        statusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Assignee Selection Dropdown
                    val currentAssignee = users.find { it.email == assigneeEmail }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentAssignee?.name ?: assigneeEmail,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Representative") },
                            leadingIcon = {
                                Icon(Icons.Default.SupervisedUserCircle, "Team")
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Dropdown") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { assigneeDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = assigneeDropdownExpanded,
                            onDismissRequest = { assigneeDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            users.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name + " (" + user.role.name + ")") },
                                    leadingIcon = { UserAvatar(name = user.name, email = user.email, size = 24.dp, hexColor = user.avatarColorHex) },
                                    onClick = {
                                        assigneeEmail = user.email
                                        assigneeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Tags field
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma separated, e.g. Hot, Tech, Proposal)") },
                        placeholder = { Text("Enterprise, Hot Deal") },
                        leadingIcon = { Icon(Icons.Default.LocalOffer, "Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description / notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Internal Prospect Notes") },
                        leadingIcon = { Icon(Icons.Default.EditNote, "Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }
            }

            // Follow-up alarm schedule slider (Exclusive Premium Component!)
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Alarm, "Follow-up Alarm", tint = StageContactedColor)
                        Text(
                            text = "Schedule Follow-up Alarm Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Creates a local push notification alert. Excellent for testing interactive states immediately inside this stream emulator!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "NONE" to "Off",
                            "SEC_10" to "10 Secs ⏳",
                            "MIN_1" to "1 Min",
                            "DAY_1" to "1 Day"
                        ).forEach { (optCode, label) ->
                            val activeVal = selectedAlarmOption == optCode
                            ElevatedFilterChip(
                                selected = activeVal,
                                onClick = { selectedAlarmOption = optCode },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = {
                    if (company.isBlank() || name.isBlank() || phone.isBlank() || dealValue.isBlank()) {
                        errorMessage = "Please complete all fields flagged with *"
                        showError = true
                        return@Button
                    }
                    val parsedVal = dealValue.toDoubleOrNull()
                    if (parsedVal == null) {
                        errorMessage = "Please input a valid numeric sum for deal valuation"
                        showError = true
                        return@Button
                    }
                    val currentDelayMs = when (selectedAlarmOption) {
                        "SEC_10" -> 10000L
                        "MIN_1" -> 60000L
                        "DAY_1" -> 86400000L
                        else -> null
                    }
                    onSaveLead(
                        name, company, email, phone, status, parsedVal, assigneeEmail, tags, notes, currentDelayMs
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, "Save")
                Spacer(Modifier.width(8.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
