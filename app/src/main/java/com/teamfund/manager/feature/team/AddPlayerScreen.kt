package com.teamfund.manager.feature.team

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import com.teamfund.manager.core.designsystem.TeamFundTextField
import com.teamfund.manager.core.designsystem.TeamFundButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.teamfund.manager.domain.model.Player
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.CricketRole
import com.teamfund.manager.domain.model.PlayerRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerScreen(
    viewModel: TeamViewModel,
    playerId: Long? = null,
    onBackClick: () -> Unit,
    onOpenExistingPlayer: (Long) -> Unit = {}
) {
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var duplicatePlayer by remember { mutableStateOf<Player?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf(PlayerRole.PLAYER) }
    var selectedCricketRole by rememberSaveable { mutableStateOf(CricketRole.UNSPECIFIED) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var roleExpanded by rememberSaveable { mutableStateOf(false) }
    var cricketRoleExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(playerId) {
        if (playerId != null && playerId > 0L) {
            val playerSummary = viewModel.rosterState.value.playerSummaries.find { it.player.id == playerId }
            playerSummary?.player?.let {
                name = it.name
                selectedRole = it.role
                selectedCricketRole = it.cricketRole
                phoneNumber = it.phoneNumber ?: ""
            }
        }
    }

    duplicatePlayer?.let { existing ->
        AlertDialog(
            onDismissRequest = { duplicatePlayer = null },
            title = { Text("Player already exists") },
            text = {
                Text(
                    "${existing.name} is already on this team${if (existing.isActive) "" else " (inactive)"}. " +
                        "Reactivate or open their profile instead of creating a duplicate."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    duplicatePlayer = null
                    if (!existing.isActive) {
                        viewModel.reactivatePlayer(existing.id) { onOpenExistingPlayer(existing.id) }
                    } else {
                        onOpenExistingPlayer(existing.id)
                    }
                }) { Text(if (existing.isActive) "Open" else "Reactivate") }
            },
            dismissButton = {
                TextButton(onClick = { duplicatePlayer = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = if (playerId == null || playerId == 0L) "Add Player" else "Edit Player",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Player Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TeamFundTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Player Name *") },
                placeholder = { Text("e.g. John Smith") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_name_input"),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = !roleExpanded }
            ) {
                TeamFundTextField(
                    value = selectedRole.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role in Team") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    PlayerRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.displayName) },
                            onClick = {
                                selectedRole = role
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = cricketRoleExpanded,
                onExpandedChange = { cricketRoleExpanded = !cricketRoleExpanded }
            ) {
                TeamFundTextField(
                    value = selectedCricketRole.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cricket role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cricketRoleExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = cricketRoleExpanded,
                    onDismissRequest = { cricketRoleExpanded = false }
                ) {
                    CricketRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.displayName) },
                            onClick = {
                                selectedCricketRole = role
                                cricketRoleExpanded = false
                            }
                        )
                    }
                }
            }

            TeamFundTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (Optional)") },
                placeholder = { Text("+1234567890") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            TeamFundButton(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addOrUpdatePlayer(
                            playerId = playerId,
                            name = name,
                            role = selectedRole,
                            cricketRole = selectedCricketRole,
                            phoneNumber = phoneNumber.ifBlank { null },
                            onSuccess = onBackClick,
                            onDuplicateExisting = { duplicatePlayer = it }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_player_button"),
                enabled = name.isNotBlank() && !isSaving
            ) {
                Text(text = if (playerId == null || playerId == 0L) "Add Player" else "Update Player")
            }
        }
    }
}
