package com.teamfund.manager.feature.team

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.PlayerRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerScreen(
    viewModel: TeamViewModel,
    playerId: Long? = null,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(PlayerRole.PLAYER) }
    var phoneNumber by remember { mutableStateOf("") }
    var roleExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(playerId) {
        if (playerId != null && playerId > 0L) {
            val playerSummary = viewModel.rosterState.value.playerSummaries.find { it.player.id == playerId }
            playerSummary?.player?.let {
                name = it.name
                selectedRole = it.role
                phoneNumber = it.phoneNumber ?: ""
            }
        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Player Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
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
                OutlinedTextField(
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

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (Optional)") },
                placeholder = { Text("+1234567890") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addOrUpdatePlayer(
                            playerId = playerId,
                            name = name,
                            role = selectedRole,
                            phoneNumber = phoneNumber.ifBlank { null },
                            onSuccess = onBackClick
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_player_button"),
                enabled = name.isNotBlank()
            ) {
                Text(text = if (playerId == null || playerId == 0L) "Add Player" else "Update Player")
            }
        }
    }
}
