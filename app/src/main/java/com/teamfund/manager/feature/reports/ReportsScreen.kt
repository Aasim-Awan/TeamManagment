package com.teamfund.manager.feature.reports

import com.teamfund.manager.core.designsystem.TeamFundButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var exportMenuOpen by remember { mutableStateOf(false) }
    var importMenuOpen by remember { mutableStateOf(false) }
    val showResult: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message, withDismissAction = true) } }

    val contributionImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                viewModel.importContributionsFromCsv(content, showResult)
            }
        }
    }

    val expenseImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                viewModel.importExpensesFromCsv(content, showResult)
            }
        }
    }

    fun shareCsv(content: String, filename: String) {
        val file = File(context.cacheDir, filename)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export $filename"))
    }

    Scaffold(
        topBar = {
            // Reports is a bottom-bar tab, so no back arrow. One Export and one Import menu.
            TeamFundTopBar(
                title = "Reports",
                actions = {
                    Box {
                        IconButton(onClick = { exportMenuOpen = true }, modifier = Modifier.testTag("reports_export")) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export CSV")
                        }
                        DropdownMenu(expanded = exportMenuOpen, onDismissRequest = { exportMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { MenuText("Full report", "Summary, balances and every record") },
                                onClick = { exportMenuOpen = false; shareCsv(viewModel.exportFullReport(), fileName(state.teamName, "report")) }
                            )
                            DropdownMenuItem(
                                text = { MenuText("Contributions", "Can be imported again") },
                                onClick = { exportMenuOpen = false; shareCsv(viewModel.exportContributions(), fileName(state.teamName, "contributions")) }
                            )
                            DropdownMenuItem(
                                text = { MenuText("Expenses", "Can be imported again") },
                                onClick = { exportMenuOpen = false; shareCsv(viewModel.exportExpenses(), fileName(state.teamName, "expenses")) }
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { importMenuOpen = true }, modifier = Modifier.testTag("reports_import")) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                        }
                        DropdownMenu(expanded = importMenuOpen, onDismissRequest = { importMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { MenuText("Import contributions", "From a TeamFund contributions CSV") },
                                onClick = { importMenuOpen = false; contributionImportLauncher.launch("text/*") }
                            )
                            DropdownMenuItem(
                                text = { MenuText("Import expenses", "From a TeamFund expenses CSV") },
                                onClick = { importMenuOpen = false; expenseImportLauncher.launch("text/*") }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Executive Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "TEAM FUND AUDIT SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ReportRow(
                            label = "Net Team Fund Balance",
                            minorUnits = state.financialSummary.teamFundBalanceMinorUnits,
                            currencyCode = state.currencyCode,
                            isPrimary = true
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ReportRow(
                            label = "Money in",
                            minorUnits = state.financialSummary.totalContributionsMinorUnits,
                            currencyCode = state.currencyCode
                        )
                        ReportRow(
                            label = "Money out from fund",
                            minorUnits = -state.financialSummary.totalExpensesFromFundMinorUnits,
                            currencyCode = state.currencyCode
                        )
                        ReportRow(
                            label = "Paid by players",
                            minorUnits = state.financialSummary.totalExpensesByPlayersMinorUnits,
                            currencyCode = state.currencyCode
                        )
                        ReportRow(
                            label = "To collect from players",
                            minorUnits = state.financialSummary.totalOutstandingDebtMinorUnits,
                            currencyCode = state.currencyCode
                        )

                    }
                }

                // Expense Category Breakdown
                Text(
                    text = "Expense Breakdown by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                state.categoryBreakdowns.forEach { breakdown ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = breakdown.category.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                MoneyText(
                                    minorUnits = breakdown.amountMinorUnits,
                                    currencyCode = state.currencyCode,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (breakdown.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(java.util.Locale.getDefault(), "%.1f", breakdown.percentage)}% of total team expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Member Net Ledger Breakdown
                Text(
                    text = "Member Net Balance Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                state.playerSummaries.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = p.player.name, style = MaterialTheme.typography.bodyLarge)
                        MoneyText(
                            minorUnits = p.positionMinorUnits,
                            currencyCode = state.currencyCode,
                            includeSign = true,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                p.positionMinorUnits > 0 -> MaterialTheme.colorScheme.primary
                                p.positionMinorUnits < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MenuText(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** e.g. "northside-cricket-report-2026-11-11.csv": dated, so exports do not overwrite each other. */
private fun fileName(teamName: String, kind: String): String {
    val slug = teamName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "team" }
    return "$slug-$kind-${java.time.LocalDate.now()}.csv"
}

@Composable
private fun ReportRow(
    label: String,
    minorUnits: Long,
    currencyCode: String,
    isPrimary: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isPrimary) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal
        )
        MoneyText(
            minorUnits = minorUnits,
            currencyCode = currencyCode,
            style = if (isPrimary) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
