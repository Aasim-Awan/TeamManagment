package com.teamfund.manager.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.ActivityItem
import com.teamfund.manager.domain.model.SettlementAdjustmentKind

/**
 * One row of money activity (expense, contribution, match, settlement or charge).
 * Shared by the Home "Recent activity" feed and the All Expenses ledger.
 */
@Composable
fun ActivityRow(
    activity: ActivityItem,
    currencyCode: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (activity) {
                is ActivityItem.ExpenseActivity -> ActivityContent(
                    icon = Icons.Default.Receipt,
                    tint = MaterialTheme.colorScheme.error,
                    title = activity.expense.title,
                    subtitle = "${activity.expense.category.displayName} • Paid by ${activity.paidByPlayerName ?: "Team Fund"} • ${DateTimeUtils.formatDate(activity.expense.date)}",
                    amountMinorUnits = activity.expense.amountMinorUnits,
                    currencyCode = currencyCode
                )
                is ActivityItem.ContributionActivity -> ActivityContent(
                    icon = Icons.Default.Payments,
                    tint = MaterialTheme.colorScheme.primary,
                    title = "${activity.playerName} contributed",
                    subtitle = "${activity.contribution.purpose.displayName} • ${DateTimeUtils.formatDate(activity.contribution.date)}",
                    amountMinorUnits = activity.contribution.amountMinorUnits,
                    currencyCode = currencyCode,
                    includeSign = true
                )
                is ActivityItem.MatchActivity -> ActivityContent(
                    icon = Icons.Default.SportsCricket,
                    tint = MaterialTheme.colorScheme.secondary,
                    title = "Match vs ${activity.match.opponentName}",
                    subtitle = DateTimeUtils.formatDate(activity.match.date),
                    amountMinorUnits = null,
                    currencyCode = currencyCode
                )
                is ActivityItem.SettlementActivity -> {
                    val teamPaid = activity.settlement.adjustmentKind == SettlementAdjustmentKind.TEAM_TO_PLAYER
                    ActivityContent(
                        icon = Icons.Default.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        title = if (teamPaid) {
                            "Team paid ${activity.toPlayerName ?: "member"}"
                        } else {
                            "${activity.fromPlayerName} settled debt"
                        },
                        subtitle = listOfNotNull(
                            activity.settlement.note?.takeIf { it.isNotBlank() },
                            DateTimeUtils.formatDate(activity.settlement.date)
                        ).joinToString(" • "),
                        amountMinorUnits = activity.settlement.amountMinorUnits,
                        currencyCode = currencyCode
                    )
                }
                is ActivityItem.FineActivity -> ActivityContent(
                    icon = Icons.Default.Gavel,
                    tint = MaterialTheme.colorScheme.tertiary,
                    title = "${activity.playerName} charged",
                    subtitle = listOfNotNull(
                        activity.fine.reason.displayName,
                        activity.fine.note?.takeIf { it.isNotBlank() },
                        DateTimeUtils.formatDate(activity.fine.date)
                    ).joinToString(" • "),
                    amountMinorUnits = activity.fine.amountMinorUnits,
                    currencyCode = currencyCode
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ActivityContent(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    amountMinorUnits: Long?,
    currencyCode: String,
    includeSign: Boolean = false
) {
    Icon(icon, contentDescription = null, tint = tint)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (amountMinorUnits != null) {
        Spacer(modifier = Modifier.width(8.dp))
        MoneyText(minorUnits = amountMinorUnits, currencyCode = currencyCode, includeSign = includeSign, color = tint)
    }
}
