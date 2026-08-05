package com.teamfund.manager.feature.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.SportType
import com.teamfund.manager.domain.model.Team
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    repository: TeamFundRepository,
    userPreferencesRepository: UserPreferencesRepository,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf(SportType.CRICKET) }
    var selectedCurrency by remember { mutableStateOf("USD") }

    var sportExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val currencies = listOf("USD", "EUR", "GBP", "PKR", "INR", "AUD", "CAD")

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Create New Team",
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
            Text(text = "Team Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Team Name *") },
                placeholder = { Text("e.g. Royal Strikers") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_team_name_input"),
                singleLine = true
            )

            ExposedDropdownMenuBox(expanded = sportExpanded, onExpandedChange = { sportExpanded = !sportExpanded }) {
                OutlinedTextField(
                    value = selectedSport.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = sportExpanded, onDismissRequest = { sportExpanded = false }) {
                    SportType.entries.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport.displayName) },
                            onClick = { selectedSport = sport; sportExpanded = false }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = !currencyExpanded }) {
                OutlinedTextField(
                    value = selectedCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Base Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    currencies.forEach { curr ->
                        DropdownMenuItem(
                            text = { Text(curr) },
                            onClick = { selectedCurrency = curr; currencyExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            val newTeam = Team(
                                name = name,
                                sport = selectedSport,
                                currencyCode = selectedCurrency
                            )
                            val newId = repository.createTeam(newTeam)
                            userPreferencesRepository.setActiveTeamId(newId)
                            repository.setActiveTeam(newId)
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_team_button"),
                enabled = name.isNotBlank()
            ) {
                Text("Create Team")
            }
        }
    }
}
