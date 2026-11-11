package com.teamfund.manager.feature.team

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.CompactMetricRow
import com.teamfund.manager.core.designsystem.LedgerListItem
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.SectionHeading
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.FineReason
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.model.SettleAdjustmentMode
import com.teamfund.manager.feature.settlements.SettleDebtSheet

@Composable
fun PlayerDetailScreen(
    playerId: Long,
    viewModel: TeamViewModel,
    onNavigateToAddContribution: (Long) -> Unit,
    onNavigateToEditPlayer: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var contributionToDelete by remember { mutableStateOf<Contribution?>(null) }
    var settlementToRevoke by remember { mutableStateOf<Settlement?>(null) }
    var fineToDelete by remember { mutableStateOf<Fine?>(null) }
    var confirmDeactivate by remember { mutableStateOf(false) }
    var showAdjustBalance by remember { mutableStateOf(false) }
    var showAddFine by remember { mutableStateOf(false) }
    var fineAmountText by remember { mutableStateOf("") }
    var fineReason by remember { mutableStateOf(FineReason.OTHER) }
    var fineNote by remember { mutableStateOf("") }

    if (confirmDeactivate) {
        AlertDialog(
            onDismissRequest = { confirmDeactivate = false },
            title = { Text("Deactivate player?") },
            text = { Text("Player leaves active roster. History kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeactivate = false
                    viewModel.deactivatePlayer(playerId, onBackClick)
                }) { Text("Deactivate") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeactivate = false }) { Text("Cancel") }
            }
        )
    }

    LaunchedEffect(playerId) {
        viewModel.loadPlayerDetail(playerId)
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = state.summary?.player?.name ?: "Player",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (state.isLoading || state.summary == null) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else {
            val summary = state.summary!!
            val player = summary.player
            val fmt = { minor: Long -> MoneyFormatter.format(minor, state.currencyCode) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${player.role.displayName} · ${player.cricketRole.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!player.phoneNumber.isNull_or_blank()) {
                                    Text(
                                        text = player.phoneNumber!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { onNavigateToEditPlayer(player.id) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { confirmDeactivate = true }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.PersonOff, contentDescription = "Deactivate", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SectionHeading("Balance")
                        MoneyText(
                            minorUnits = summary.positionMinorUnits,
                            currencyCode = state.currencyCode,
                            includeSign = true,
                            style = MaterialTheme.typography.headlineMedium,
                            color = when {
                                summary.positionMinorUnits > 0 -> MaterialTheme.colorScheme.primary
                                summary.positionMinorUnits < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        summary.balanceActionLabel(state.currencyCode)?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    summary.positionMinorUnits > 0 -> MaterialTheme.colorScheme.primary
                                    summary.positionMinorUnits < 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        } ?: Text(
                            text = summary.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        summary.sharesBreakdownLabel(state.currencyCode)?.let { breakdown ->
                            Text(
                                text = breakdown,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactMetricRow("Contributed", fmt(summary.totalContributionsMinorUnits))
                        CompactMetricRow("Paid for team", fmt(summary.totalPaidOnBehalfMinorUnits))
                        CompactMetricRow("Expense shares", fmt(summary.personalExpenseSharesMinorUnits))
                    }
                }

                if (!player.isActive) {
                    OutlinedButton(
                        onClick = { viewModel.reactivatePlayer(playerId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reactivate player")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Pay in", Icons.Default.Payments) { onNavigateToAddContribution(player.id) },
                        Triple("Adjust", Icons.Default.SwapHoriz) { showAdjustBalance = true },
                        Triple("Fine", Icons.Default.Receipt) { showAddFine = true }
                    ).forEach { (label, icon, action) ->
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }

                if (state.fines.isNotEmpty()) {
                    SectionHeading("Charges")
                    state.fines.forEach { fine ->
                        LedgerListItem(
                            title = fine.reason.displayName,
                            subtitle = buildString {
                                append(DateTimeUtils.formatDate(fine.date))
                                fine.note?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                            },
                            amountMinorUnits = fine.amountMinorUnits,
                            currencyCode = state.currencyCode,
                            amountColor = MaterialTheme.colorScheme.error,
                            onDelete = { fineToDelete = fine }
                        )
                    }
                }

                SectionHeading("Activity")
                if (state.contributions.isEmpty() && state.expenseShares.isEmpty() && state.settlements.isEmpty()) {
                    Text(
                        "No activity yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.contributions.forEach { contrib ->
                        LedgerListItem(
                            title = "Contribution · ${contrib.purpose.displayName}",
                            subtitle = DateTimeUtils.formatDate(contrib.date),
                            amountMinorUnits = contrib.amountMinorUnits,
                            currencyCode = state.currencyCode,
                            includeSign = true,
                            amountColor = MaterialTheme.colorScheme.primary,
                            onDelete = { contributionToDelete = contrib }
                        )
                    }
                    state.settlements.forEach { settlement ->
                        val isOutgoing = settlement.fromPlayerId == playerId
                        LedgerListItem(
                            title = if (isOutgoing) "Paid team" else "Received",
                            subtitle = buildString {
                                append(DateTimeUtils.formatDate(settlement.date))
                                settlement.note?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                            },
                            amountMinorUnits = settlement.amountMinorUnits,
                            currencyCode = state.currencyCode,
                            includeSign = true,
                            amountColor = if (isOutgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            onDelete = { settlementToRevoke = settlement }
                        )
                    }
                    state.expenseShares.forEach { share ->
                        val exp = state.expensesMap[share.expenseId]
                        LedgerListItem(
                            title = "Share · ${exp?.title ?: "Expense"}",
                            subtitle = exp?.date?.let { DateTimeUtils.formatDate(it) },
                            amountMinorUnits = -share.shareAmountMinorUnits,
                            currencyCode = state.currencyCode,
                            amountColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    contributionToDelete?.let { contrib ->
        AlertDialog(
            onDismissRequest = { contributionToDelete = null },
            title = { Text("Delete contribution?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContribution(contrib.id)
                    contributionToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { contributionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    fineToDelete?.let { fine ->
        AlertDialog(
            onDismissRequest = { fineToDelete = null },
            title = { Text("Delete charge?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFine(fine.id)
                    fineToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { fineToDelete = null }) { Text("Cancel") }
            }
        )
    }

    settlementToRevoke?.let { settlement ->
        AlertDialog(
            onDismissRequest = { settlementToRevoke = null },
            title = { Text("Revoke settlement?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSettlement(settlement.id)
                    settlementToRevoke = null
                }) { Text("Revoke", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { settlementToRevoke = null }) { Text("Cancel") }
            }
        )
    }

    val summary = state.summary
    if (showAdjustBalance && summary != null) {
        SettleDebtSheet(
            playerSummary = summary,
            currencyCode = state.currencyCode,
            expenses = state.expenses,
            onDismiss = { showAdjustBalance = false },
            isSaving = isSaving,
            onSettle = { amount, note, mode, expenseId ->
                viewModel.settleAdjustment(
                    playerId = playerId,
                    mode = mode,
                    amountMinorUnits = amount,
                    note = note,
                    expenseId = expenseId,
                    onSuccess = { showAdjustBalance = false }
                )
            }
        )
    }

    if (showAddFine) {
        AlertDialog(
            onDismissRequest = { showAddFine = false },
            title = { Text("Fine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fineAmountText,
                        onValueChange = { fineAmountText = it },
                        label = { Text("Amount (${state.currencyCode})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fineNote,
                        onValueChange = { fineNote = it },
                        label = { Text("Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minor = MoneyFormatter.toMinorUnits(fineAmountText)
                    if (minor > 0L) {
                        viewModel.addFine(playerId, minor, fineReason, fineNote.ifBlank { null }, recordPayment = true)
                        showAddFine = false
                        fineAmountText = ""
                        fineNote = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFine = false }) { Text("Cancel") }
            }
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
