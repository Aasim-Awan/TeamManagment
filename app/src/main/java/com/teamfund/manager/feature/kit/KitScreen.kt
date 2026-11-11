package com.teamfund.manager.feature.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.TeamFundButton
import com.teamfund.manager.core.designsystem.TeamFundTextField
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.InventoryCategory

@Composable
fun KitScreen(
    viewModel: KitViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newItemName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TeamFundTopBar(title = "Kit & stock", showBackButton = true, onBackClick = onBackClick)
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingView(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeamFundTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("New item name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TeamFundButton(
                    onClick = {
                        viewModel.addItem(newItemName, InventoryCategory.OTHER, 0)
                        newItemName = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newItemName.isNotBlank()
                ) { Text("Add stock item") }

                state.items.forEach { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${item.category.displayName} • Qty ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                IconButton(onClick = { viewModel.adjustStock(item.id, -1, "Adjusted") }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                IconButton(onClick = { viewModel.adjustStock(item.id, 1, "Adjusted") }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
