package com.teamfund.manager.feature.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.DatePickerField
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.MatchSportDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMatchScreen(
    viewModel: MatchesViewModel,
    matchId: Long? = null,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sport = state.sport
    val isEdit = matchId != null && matchId > 0L

    var opponentName by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var selectedResult by remember { mutableStateOf(MatchResult.UPCOMING) }
    var teamScore by remember { mutableStateOf("") }
    var opponentScore by remember { mutableStateOf("") }
    var formatText by remember { mutableStateOf(MatchSportDefaults.defaultFormat(sport)) }
    var notes by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var resultExpanded by remember { mutableStateOf(false) }
    var didLoadEdit by remember { mutableStateOf(false) }
    var didApplySportDefault by remember { mutableStateOf(false) }

    LaunchedEffect(sport) {
        if (!isEdit && !didApplySportDefault) {
            formatText = MatchSportDefaults.defaultFormat(sport)
            didApplySportDefault = true
        }
    }

    LaunchedEffect(matchId, state.matches) {
        if (isEdit && !didLoadEdit) {
            state.matches.find { it.id == matchId }?.let {
                opponentName = it.opponentName
                venue = it.venue ?: ""
                selectedResult = it.result
                teamScore = it.teamScore ?: ""
                opponentScore = it.opponentScore ?: ""
                formatText = it.overs ?: MatchSportDefaults.defaultFormat(sport)
                notes = it.notes ?: ""
                selectedDateMillis = it.date
                didLoadEdit = true
            }
        }
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = MatchSportDefaults.addScreenTitle(sport, isEdit),
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Fixture Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = opponentName,
                onValueChange = { opponentName = it },
                label = { Text("Opponent Team Name *") },
                placeholder = { Text("e.g. Rival Club") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("match_opponent_input"),
                singleLine = true
            )

            DatePickerField(
                selectedDateMillis = selectedDateMillis,
                onDateSelected = { selectedDateMillis = it },
                label = "Match Date",
                testTag = "match_date_picker"
            )

            OutlinedTextField(
                value = venue,
                onValueChange = { venue = it },
                label = { Text("Ground / Venue") },
                placeholder = { Text(MatchSportDefaults.venuePlaceholder(sport)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("match_venue_input"),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = resultExpanded,
                onExpandedChange = { resultExpanded = !resultExpanded }
            ) {
                OutlinedTextField(
                    value = selectedResult.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Match Status / Result") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resultExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = resultExpanded,
                    onDismissRequest = { resultExpanded = false }
                ) {
                    MatchResult.entries.forEach { res ->
                        DropdownMenuItem(
                            text = { Text(res.displayName) },
                            onClick = {
                                selectedResult = res
                                resultExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedResult != MatchResult.UPCOMING) {
                Text(
                    text = "Scores (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = teamScore,
                        onValueChange = { teamScore = it },
                        label = { Text("Our Score") },
                        placeholder = { Text("Score") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = opponentScore,
                        onValueChange = { opponentScore = it },
                        label = { Text("Opponent Score") },
                        placeholder = { Text("Score") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            OutlinedTextField(
                value = formatText,
                onValueChange = { formatText = it },
                label = { Text(MatchSportDefaults.formatFieldLabel(sport)) },
                placeholder = { Text(MatchSportDefaults.formatPlaceholder(sport)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Match Summary") },
                placeholder = { Text("Key moments, conditions...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (opponentName.isNotBlank()) {
                        viewModel.saveMatch(
                            matchId = matchId,
                            opponentName = opponentName,
                            date = selectedDateMillis,
                            venue = venue.ifBlank { null },
                            result = selectedResult,
                            teamScore = teamScore.ifBlank { null },
                            opponentScore = opponentScore.ifBlank { null },
                            overs = formatText.ifBlank { null },
                            tossWinner = null,
                            playerOfTheMatchId = null,
                            notes = notes.ifBlank { null },
                            onSuccess = onBackClick
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_match_button"),
                enabled = opponentName.isNotBlank()
            ) {
                Text(text = if (isEdit) "Update Match" else "Save Match")
            }
        }
    }
}
