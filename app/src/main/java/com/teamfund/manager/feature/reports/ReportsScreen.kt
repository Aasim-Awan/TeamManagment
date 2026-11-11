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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val contributionImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                viewModel.importContributionsFromCsv(content) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val expenseImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                viewModel.importExpensesFromCsv(content) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
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
            TeamFundTopBar(
                title = "Reports",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = {
                        val csv = viewModel.exportMasterReport()
                        shareCsv(csv, "team_fund_audit_report.csv")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Master Report")
                    }
                    IconButton(onClick = { contributionImportLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import Contributions")
                    }
                    IconButton(onClick = { expenseImportLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import Expenses", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        }
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
                            label = "Total Member Contributions",
                            minorUnits = state.financialSummary.totalContributionsMinorUnits,
                            currencyCode = state.currencyCode
                        )
                        ReportRow(
                            label = "Direct Team Expenses",
                            minorUnits = -state.financialSummary.totalExpensesFromFundMinorUnits,
                            currencyCode = state.currencyCode
                        )
                        ReportRow(
                            label = "Expenses Paid On Behalf",
                            minorUnits = state.financialSummary.totalExpensesByPlayersMinorUnits,
                            currencyCode = state.currencyCode
                        )
                        ReportRow(
                            label = "Total Member Outstanding Dues",
                            minorUnits = state.financialSummary.totalOutstandingDebtMinorUnits,
                            currencyCode = state.currencyCode
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        TeamFundButton(
                            onClick = {
                                val csv = viewModel.exportMasterReport()
                                shareCsv(csv, "team_fund_audit_report.csv")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Full Audit Report (CSV)")
                        }
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
                                text = "${String.format("%.1f", breakdown.percentage)}% of total team expenses",
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
