package com.teamfund.manager.feature.settings

import com.teamfund.manager.core.designsystem.TeamFundTextField
import com.teamfund.manager.core.designsystem.TeamFundButton

import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.BuildConfig
import com.teamfund.manager.core.util.AppPrivacy
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.SportType
import java.io.File
import kotlinx.coroutines.launch

private val currencies = listOf(
    "USD" to "US dollar",
    "EUR" to "Euro",
    "GBP" to "British pound",
    "PKR" to "Pakistani rupee",
    "INR" to "Indian rupee",
    "AUD" to "Australian dollar",
    "CAD" to "Canadian dollar",
    "NZD" to "New Zealand dollar",
    "AED" to "UAE dirham"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCreateTeam: () -> Unit,
    onNavigateToKit: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTeamPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showSeasonsSheet by remember { mutableStateOf(false) }
    var showDuesSheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showExportChoice by remember { mutableStateOf(false) }
    var showImportChoice by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val currencyCode = state.activeTeam?.currencyCode ?: "USD"

    fun launchExternal(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to open this link.", Toast.LENGTH_LONG).show()
        }
    }

    fun contactSupport() {
        launchExternal(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "TeamFund Manager ${BuildConfig.VERSION_NAME} support")
        })
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

    // Results (import counts, dues, reset) show as a snackbar, then clear.
    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message, withDismissAction = true)
        viewModel.clearStatusMessage()
    }

    // ---- Dialogs and sheets ----

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Help") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("1. Create a team and add players.\n\n2. Schedule matches with date, start time and venue. Share fixture details from the match screen.\n\n3. Record expenses and choose who shares each cost. Record contributions when members pay into the fund.\n\n4. Use Settlements to record payments and check remaining balances. TeamFund tracks money; it does not send payments.\n\n5. Export CSV reports before removing records. CSV is not a complete backup. Your records stay on this device; there is no team sync.")
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Close") } }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy policy") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(AppPrivacy.disclosure)
                    if (BuildConfig.PRIVACY_POLICY_URL.startsWith("https://")) {
                        TextButton(onClick = {
                            launchExternal(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)))
                        }) { Text("Open public privacy policy") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("Close") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset all data?") },
            text = { Text("This permanently deletes every team, player, match and financial record on this device. Exported CSV files and Android backups are not affected. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllData { showClearConfirm = false } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportChoice) {
        ChoiceDialog(
            title = "Export CSV",
            message = "Share records of the active team as a CSV file. CSV is a report, not a full backup: matches, settlements and expense splits are not included.",
            options = listOf(
                "Contributions" to {
                    scope.launch {
                        viewModel.exportContributionsCsv()?.let { shareCsv(it, "contributions.csv") }
                        showExportChoice = false
                    }
                    Unit
                },
                "Expenses" to {
                    scope.launch {
                        viewModel.exportExpensesCsv()?.let { shareCsv(it, "expenses.csv") }
                        showExportChoice = false
                    }
                    Unit
                }
            ),
            onDismiss = { showExportChoice = false }
        )
    }

    if (showImportChoice) {
        ChoiceDialog(
            title = "Import CSV",
            message = "Import a TeamFund CSV into the active team. Player names must already exist. Rows already recorded are skipped. Imported expenses have no player split; edit them to split.",
            options = listOf(
                "Contributions" to {
                    showImportChoice = false
                    contributionImportLauncher.launch("text/*")
                },
                "Expenses" to {
                    showImportChoice = false
                    expenseImportLauncher.launch("text/*")
                }
            ),
            onDismiss = { showImportChoice = false }
        )
    }

    if (showTeamPicker) {
        RadioListDialog(
            title = "Switch team",
            options = state.allTeams.map { it.id to it.name },
            selected = state.activeTeam?.id,
            onSelect = {
                viewModel.switchActiveTeam(it)
                showTeamPicker = false
            },
            onDismiss = { showTeamPicker = false },
            footer = {
                TextButton(onClick = {
                    showTeamPicker = false
                    onNavigateToCreateTeam()
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New team")
                }
            }
        )
    }

    if (showCurrencyPicker) {
        RadioListDialog(
            title = "Team currency",
            message = "Changes the currency label only. Existing amounts are not converted.",
            options = currencies.map { (code, name) -> code to "$code · $name" },
            selected = currencyCode,
            onSelect = {
                viewModel.updateTeamCurrency(it)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    if (showSeasonsSheet) {
        SeasonsSheet(
            state = state,
            onSetActive = viewModel::setActiveSeason,
            onCreate = viewModel::createSeason,
            onDismiss = { showSeasonsSheet = false }
        )
    }

    if (showDuesSheet) {
        DuesSheet(
            state = state,
            currencyCode = currencyCode,
            onCharge = viewModel::generateDueContributions,
            onAdd = viewModel::addMonthlyDue,
            onDismiss = { showDuesSheet = false }
        )
    }

    // ---- Screen ----

    Scaffold(
        topBar = {
            TeamFundTopBar(title = "Settings", showBackButton = true, onBackClick = onBackClick)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TeamHeaderCard(
                    teamName = state.activeTeam?.name ?: "No team selected",
                    subtitle = state.activeTeam?.let { "${it.sport.displayName} · $currencyCode" }
                        ?: "Create a team to get started",
                    sport = state.activeTeam?.sport,
                    teamCount = state.allTeams.size,
                    onSwitch = { showTeamPicker = true },
                    onNewTeam = onNavigateToCreateTeam
                )

                if (state.activeTeam != null) {
                    SettingsSection(title = "Team") {
                        SettingsRow(
                            icon = Icons.Default.Payments,
                            title = "Currency",
                            subtitle = currencies.find { it.first == currencyCode }?.let { "${it.first} · ${it.second}" } ?: currencyCode,
                            onClick = { showCurrencyPicker = true },
                            testTag = "currency_selector"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.CalendarMonth,
                            title = "Seasons",
                            subtitle = state.activeSeason?.let { "Active: ${it.name}" } ?: "No active season",
                            onClick = { showSeasonsSheet = true },
                            testTag = "settings_seasons"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.EventRepeat,
                            title = "Monthly dues",
                            subtitle = when (val count = state.dueSchedules.size) {
                                0 -> "No schedules"
                                1 -> "1 schedule"
                                else -> "$count schedules"
                            },
                            onClick = { showDuesSheet = true },
                            testTag = "settings_dues"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Inventory2,
                            title = "Kit & stock",
                            subtitle = "Balls, kit and equipment",
                            onClick = onNavigateToKit,
                            testTag = "settings_kit"
                        )
                    }
                }

                SettingsSection(title = "Appearance") {
                    Column(Modifier.padding(16.dp)) {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        val modes = listOf(
                            Triple("SYSTEM", "System", Icons.Default.SettingsSuggest),
                            Triple("LIGHT", "Light", Icons.Default.LightMode),
                            Triple("DARK", "Dark", Icons.Default.DarkMode)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { index, (mode, label, icon) ->
                                SegmentedButton(
                                    selected = state.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.testTag("theme_$mode")
                                ) { Text(label) }
                            }
                        }
                    }
                }

                SettingsSection(
                    title = "Data",
                    footer = "Records are stored on this device and included in your Android backup. CSV files are reports, not full backups."
                ) {
                    SettingsRow(
                        icon = Icons.Default.Share,
                        title = "Export CSV",
                        subtitle = "Contributions or expenses",
                        onClick = { showExportChoice = true },
                        testTag = "settings_export"
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.FileUpload,
                        title = "Import CSV",
                        subtitle = "Add records from a TeamFund export",
                        onClick = { showImportChoice = true },
                        testTag = "settings_import"
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Reset all data",
                        subtitle = "Delete every team and record on this device",
                        onClick = { showClearConfirm = true },
                        destructive = true,
                        showChevron = false,
                        testTag = "settings_reset"
                    )
                }

                SettingsSection(title = "About") {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help",
                        subtitle = "How TeamFund works",
                        onClick = { showHelp = true }
                    )
                    if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Email,
                            title = "Contact support",
                            subtitle = BuildConfig.SUPPORT_EMAIL,
                            onClick = ::contactSupport
                        )
                    }
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy policy",
                        subtitle = "What TeamFund stores and shares",
                        onClick = { showPrivacy = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.IosShare,
                        title = "Share TeamFund",
                        subtitle = "Tell another team manager",
                        onClick = {
                            val message = "TeamFund Manager — manage sports team players, fixtures and financial records offline." +
                                if (BuildConfig.PUBLIC_APP_URL.startsWith("https://")) "\n${BuildConfig.PUBLIC_APP_URL}" else ""
                            launchExternal(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }, "Share TeamFund"))
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        onClick = null,
                        showChevron = false
                    )
                }

                Text(
                    text = "TeamFund Manager · S&Ainnovation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ---- Building blocks ----

@Composable
private fun TeamHeaderCard(
    teamName: String,
    subtitle: String,
    sport: SportType?,
    teamCount: Int,
    onSwitch: () -> Unit,
    onNewTeam: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sportIcon(sport),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = teamName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (teamCount > 1) {
                    OutlinedButton(
                        onClick = onSwitch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_team_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Switch team")
                    }
                }
                TeamFundButton(
                    onClick = onNewTeam,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_switch_team_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New team")
                }
            }
        }
    }
}

private fun sportIcon(sport: SportType?): ImageVector = when (sport) {
    SportType.CRICKET -> Icons.Default.SportsCricket
    SportType.FOOTBALL -> Icons.Default.SportsSoccer
    SportType.BASKETBALL -> Icons.Default.SportsBasketball
    SportType.BADMINTON -> Icons.Default.SportsTennis
    SportType.VOLLEYBALL -> Icons.Default.SportsVolleyball
    SportType.OTHER, null -> Icons.Default.Groups
}

@Composable
private fun SettingsSection(
    title: String,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(content = content)
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    destructive: Boolean = false,
    showChevron: Boolean = onClick != null,
    testTag: String? = null
) {
    val contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconBackground = if (destructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val iconTint = if (destructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = contentColor)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    message: String,
    options: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                options.forEach { (label, action) ->
                    OutlinedButton(
                        onClick = action,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) { Text(label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun <T> RadioListDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    footer: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = value == selected, onClick = { onSelect(value) }, role = Role.RadioButton)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                footer?.invoke()
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonsSheet(
    state: SettingsUiState,
    onSetActive: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newSeasonName by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Seasons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "New matches are added to the active season.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            if (state.seasons.isEmpty()) {
                Text("No seasons yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.seasons.forEach { season ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = season.isActive, onClick = { onSetActive(season.id) }, role = Role.RadioButton)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = season.isActive, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(season.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (season.isActive) {
                        Text("Active", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            TeamFundTextField(
                value = newSeasonName,
                onValueChange = { newSeasonName = it },
                label = { Text("New season name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            TeamFundButton(
                onClick = {
                    onCreate(newSeasonName)
                    newSeasonName = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newSeasonName.isNotBlank()
            ) { Text("Create and make active") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuesSheet(
    state: SettingsUiState,
    currencyCode: String,
    onCharge: (Long) -> Unit,
    onAdd: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newDueName by remember { mutableStateOf("") }
    var newDueAmount by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Monthly dues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Charge adds the due to every active player once per month.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            if (state.dueSchedules.isEmpty()) {
                Text("No schedules yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.dueSchedules.forEach { schedule ->
                val isDue = schedule.nextDueDate <= now
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${schedule.name} · ${MoneyFormatter.format(schedule.amountMinorUnits, currencyCode)}",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (isDue) "Due now" else "Next due ${DateTimeUtils.formatAbsoluteDate(schedule.nextDueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { onCharge(schedule.id) }, enabled = isDue) { Text("Charge") }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("New schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TeamFundTextField(
                value = newDueName,
                onValueChange = { newDueName = it },
                label = { Text("Name, e.g. Monthly subscription") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            TeamFundTextField(
                value = newDueAmount,
                onValueChange = { newDueAmount = it },
                label = { Text("Amount per player ($currencyCode)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(Modifier.height(12.dp))
            TeamFundButton(
                onClick = {
                    onAdd(newDueName, MoneyFormatter.toMinorUnits(newDueAmount))
                    newDueName = ""
                    newDueAmount = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newDueName.isNotBlank() && MoneyFormatter.toMinorUnits(newDueAmount) > 0L
            ) { Text("Save schedule") }
        }
    }
}
