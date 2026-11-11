package com.teamfund.manager.feature.matches

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.saveable.rememberSaveable

import com.teamfund.manager.core.designsystem.TeamFundTextField
import com.teamfund.manager.core.designsystem.TeamFundButton

import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.DatePickerField
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import androidx.compose.ui.Alignment
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.MatchPlayerStatus
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
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sport = state.sport
    val isEdit = matchId != null && matchId > 0L

    var opponentName by rememberSaveable { mutableStateOf("") }
    var venue by rememberSaveable { mutableStateOf("") }
    var selectedResult by rememberSaveable { mutableStateOf(MatchResult.UPCOMING) }
    var teamScore by rememberSaveable { mutableStateOf("") }
    var opponentScore by rememberSaveable { mutableStateOf("") }
    var formatText by rememberSaveable { mutableStateOf(MatchSportDefaults.defaultFormat(sport)) }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var resultExpanded by rememberSaveable { mutableStateOf(false) }
    var didLoadEdit by rememberSaveable { mutableStateOf(false) }
    var didApplySportDefault by rememberSaveable { mutableStateOf(false) }
    var matchFeeText by rememberSaveable { mutableStateOf("") }
    var groundFeeText by rememberSaveable { mutableStateOf("") }
    var chargeMatchFee by rememberSaveable { mutableStateOf(false) }
    var createGroundExpense by rememberSaveable { mutableStateOf(false) }
    var selectedXi by rememberSaveable { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(sport, state.isLoading) {
        if (!state.isLoading && !isEdit && !didApplySportDefault) {
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
                matchFeeText = MoneyFormatter.fromMinorUnits(it.matchFeeMinorUnits)
                groundFeeText = MoneyFormatter.fromMinorUnits(it.groundFeeMinorUnits)
                didLoadEdit = true
            }
        }
    }

    LaunchedEffect(matchId) {
        if (isEdit && matchId != null) {
            selectedXi = viewModel.fetchSelectedXi(matchId)
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
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fixture Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TeamFundTextField(
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
                onDateSelected = { date ->
                    val zone = ZoneId.systemDefault()
                    val time = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalTime()
                    selectedDateMillis = Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
                        .atTime(time).atZone(zone).toInstant().toEpochMilli()
                },
                label = "Match Date",
                testTag = "match_date_picker"
            )

            OutlinedButton(
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val current = Instant.ofEpochMilli(selectedDateMillis).atZone(zone)
                    TimePickerDialog(context, { _, hour, minute ->
                        selectedDateMillis = current.toLocalDate().atTime(hour, minute)
                            .atZone(zone).toInstant().toEpochMilli()
                    }, current.hour, current.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("match_time_picker")
            ) {
                Text("Start time: " + Instant.ofEpochMilli(selectedDateMillis)
                    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")))
            }

            TeamFundTextField(
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
                TeamFundTextField(
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
                    TeamFundTextField(
                        value = teamScore,
                        onValueChange = { teamScore = it },
                        label = { Text("Our Score") },
                        placeholder = { Text("Score") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TeamFundTextField(
                        value = opponentScore,
                        onValueChange = { opponentScore = it },
                        label = { Text("Opponent Score") },
                        placeholder = { Text("Score") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            TeamFundTextField(
                value = formatText,
                onValueChange = { formatText = it },
                label = { Text(MatchSportDefaults.formatFieldLabel(sport)) },
                placeholder = { Text(MatchSportDefaults.formatPlaceholder(sport)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TeamFundTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Match Summary") },
                placeholder = { Text("Key moments, conditions...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text(
                text = "Fees & XI",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            TeamFundTextField(
                value = matchFeeText,
                onValueChange = { matchFeeText = it },
                label = { Text("Match fee per XI player (${state.currencyCode})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Charge XI fee", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = chargeMatchFee, onCheckedChange = { chargeMatchFee = it })
            }

            TeamFundTextField(
                value = groundFeeText,
                onValueChange = { groundFeeText = it },
                label = { Text("Ground fee (${state.currencyCode})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ground expense", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = createGroundExpense, onCheckedChange = { createGroundExpense = it })
            }

            Text("Playing XI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                state.players.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedXi.contains(player.id),
                            onCheckedChange = { checked ->
                                selectedXi = if (checked) selectedXi + player.id else selectedXi - player.id
                            }
                        )
                        Text(
                            "${player.name} · ${player.cricketRole.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TeamFundButton(
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
                            seasonId = state.activeSeason?.id,
                            matchFeeMinorUnits = MoneyFormatter.toMinorUnits(matchFeeText),
                            groundFeeMinorUnits = MoneyFormatter.toMinorUnits(groundFeeText),
                            selectedXiPlayerIds = selectedXi,
                            chargeMatchFee = chargeMatchFee,
                            createGroundExpense = createGroundExpense,
                            onSuccess = onBackClick
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_match_button"),
                enabled = opponentName.isNotBlank() && !isSaving
            ) {
                Text(text = if (isEdit) "Update Match" else "Save Match")
            }
        }
    }
}
