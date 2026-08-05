package com.teamfund.manager.feature.contributions

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.ContributionPurpose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContributionScreen(
    viewModel: ContributionsViewModel,
    preselectedPlayerId: Long? = null,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPlayerId by remember { mutableStateOf(preselectedPlayerId) }
    var amountText by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf(ContributionPurpose.MONTHLY) }
    var notes by remember { mutableStateOf("") }

    var playerExpanded by remember { mutableStateOf(false) }
    var purposeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.players) {
        if (selectedPlayerId == null && state.players.isNotEmpty()) {
            selectedPlayerId = state.players.first().id
        }
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Record Contribution",
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
            Text(text = "Contribution Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Player Selector
            ExposedDropdownMenuBox(expanded = playerExpanded, onExpandedChange = { playerExpanded = !playerExpanded }) {
                val selectedPlayerName = state.players.find { it.id == selectedPlayerId }?.name ?: "Select Member *"
                OutlinedTextField(
                    value = selectedPlayerName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Contributing Member *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = playerExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("contribution_player_selector")
                )
                ExposedDropdownMenu(expanded = playerExpanded, onDismissRequest = { playerExpanded = false }) {
                    state.players.forEach { player ->
                        DropdownMenuItem(
                            text = { Text(player.name) },
                            onClick = {
                                selectedPlayerId = player.id
                                playerExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (${state.currencyCode}) *") },
                placeholder = { Text("50.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contribution_amount_input"),
                singleLine = true
            )

            // Purpose Selector
            ExposedDropdownMenuBox(expanded = purposeExpanded, onExpandedChange = { purposeExpanded = !purposeExpanded }) {
                OutlinedTextField(
                    value = selectedPurpose.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Purpose") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purposeExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = purposeExpanded, onDismissRequest = { purposeExpanded = false }) {
                    ContributionPurpose.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.displayName) },
                            onClick = {
                                selectedPurpose = p
                                purposeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("e.g. July monthly fee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            val amountMinor = com.teamfund.manager.core.util.MoneyFormatter.toMinorUnits(amountText)

            Button(
                onClick = {
                    if (selectedPlayerId != null && amountMinor > 0L) {
                        viewModel.addContribution(
                            playerId = selectedPlayerId!!,
                            amountMinorUnits = amountMinor,
                            purpose = selectedPurpose,
                            notes = notes.ifBlank { null },
                            onSuccess = onBackClick
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_contribution_button"),
                enabled = selectedPlayerId != null && amountMinor > 0L
            ) {
                Text("Record Contribution")
            }
        }
    }
}
