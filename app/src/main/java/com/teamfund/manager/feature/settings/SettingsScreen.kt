package com.teamfund.manager.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.notifications.NotificationHelper
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, onNavigateToCreateTeam: () -> Unit, onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currencyExpanded by remember { mutableStateOf(false) }
    var teamSelectExpanded by remember { mutableStateOf(false) }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showExportChoice by remember { mutableStateOf(false) }
    var showImportChoice by remember { mutableStateOf(false) }

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

    val contributionImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
            if (content != null) viewModel.importContributionsFromCsv(content)
        }
    }

    val expenseImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
            if (content != null) viewModel.importExpensesFromCsv(content)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all teams, players, matches, and financial records. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData { showClearConfirm = false }
                }) {
                    Text("CLEAR ALL", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            })
    }

    if (showExportChoice) {
        AlertDialog(
            onDismissRequest = { showExportChoice = false },
            title = { Text("Export Data") },
            text = { Text("Choose what to export as CSV for the active team.") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.exportContributionsCsv()?.let { shareCsv(it, "contributions.csv") }
                            showExportChoice = false
                        }
                    }) { Text("Contributions") }
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.exportExpensesCsv()?.let { shareCsv(it, "expenses.csv") }
                            showExportChoice = false
                        }
                    }) { Text("Expenses") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportChoice = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportChoice) {
        AlertDialog(
            onDismissRequest = { showImportChoice = false },
            title = { Text("Import Data") },
            text = { Text("Choose CSV type to import into the active team. Player names must already exist on the roster.") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        showImportChoice = false
                        contributionImportLauncher.launch("text/*")
                    }) { Text("Contributions") }
                    TextButton(onClick = {
                        showImportChoice = false
                        expenseImportLauncher.launch("text/*")
                    }) { Text("Expenses") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportChoice = false }) { Text("Cancel") }
            }
        )
    }

    state.statusMessage?.let { message ->
        LaunchedEffect(message) {
            // Auto-clear after shown once via snackbar-less toast path
            kotlinx.coroutines.delay(2500)
            viewModel.clearStatusMessage()
        }
        AlertDialog(
            onDismissRequest = { viewModel.clearStatusMessage() },
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearStatusMessage() }) { Text("OK") }
            }
        )
    }

    val currencies = listOf(
        "USD ($)" to "USD",
        "EUR (€)" to "EUR",
        "GBP (£)" to "GBP",
        "PKR (Rs)" to "PKR",
        "INR (₹)" to "INR",
        "AUD ($)" to "AUD",
        "CAD ($)" to "CAD",
        "NZD ($)" to "NZD",
        "AED (AED)" to "AED"
    )

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Settings & Preferences", showBackButton = true, onBackClick = onBackClick
            )
        }) { innerPadding ->
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
                // 1. Team & Currency Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsCricket,
                                    contentDescription = "Team",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ACTIVE TEAM & REGIONAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = state.activeTeam?.name ?: "No Team Selected",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch Team Dropdown (if multiple exist)
                        if (state.allTeams.size > 1) {
                            ExposedDropdownMenuBox(
                                expanded = teamSelectExpanded,
                                onExpandedChange = { teamSelectExpanded = !teamSelectExpanded }) {
                                OutlinedTextField(
                                    value = state.activeTeam?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Active Team") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = teamSelectExpanded
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = teamSelectExpanded,
                                    onDismissRequest = { teamSelectExpanded = false }) {
                                    state.allTeams.forEach { t ->
                                        DropdownMenuItem(text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    t.name,
                                                    fontWeight = if (t.id == state.activeTeam?.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (t.id == state.activeTeam?.id) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }, onClick = {
                                            viewModel.switchActiveTeam(t.id)
                                            teamSelectExpanded = false
                                        })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Currency Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = !currencyExpanded }) {
                            OutlinedTextField(
                                value = state.activeTeam?.currencyCode ?: "USD",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Base Currency") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("currency_selector")
                            )
                            ExposedDropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }) {
                                currencies.forEach { (label, code) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        viewModel.updateTeamCurrency(code)
                                        currencyExpanded = false
                                    })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToCreateTeam,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_switch_team_button")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create / Add New Team")
                        }
                    }
                }

                // 2. Appearance & Theme Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "APPEARANCE & THEME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Theme Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOptionChip(
                                label = "System",
                                icon = Icons.Default.SettingsSuggest,
                                isSelected = state.themeMode == "SYSTEM",
                                onClick = { viewModel.setThemeMode("SYSTEM") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionChip(
                                label = "Light",
                                icon = Icons.Default.LightMode,
                                isSelected = state.themeMode == "LIGHT",
                                onClick = { viewModel.setThemeMode("LIGHT") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionChip(
                                label = "Dark",
                                icon = Icons.Default.DarkMode,
                                isSelected = state.themeMode == "DARK",
                                onClick = { viewModel.setThemeMode("DARK") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Data Migration & Management Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "DATA MIGRATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Export & Import",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Manage your data by exporting to CSV for backups or importing from other devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showExportChoice = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Data")
                            }
                            OutlinedButton(
                                onClick = { showImportChoice = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Data")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset All Data")
                        }
                    }
                }

                // 4. Notifications & Reminders Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "NOTIFICATIONS & ALERTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Local Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Match & Debt Dues",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Get local system alerts for upcoming matches and outstanding balances",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = state.notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setNotificationsEnabled(enabled)
                                    if (enabled) {
                                        NotificationHelper.createNotificationChannel(context)
                                        NotificationHelper.showLocalReminder(
                                            context = context,
                                            title = "Reminders enabled",
                                            message = "TeamFund will use local alerts for dues and matches.",
                                            notificationId = 1001
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("notification_toggle_switch")
                            )
                        }
                    }
                }

                // 5. App Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TeamFund Manager v1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "The Professional Sports Financial Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
