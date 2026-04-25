package com.awper.lightscore.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awper.lightscore.TeamInfo
import com.awper.lightscore.settings.SettingsStore

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToTeamPicker: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back", tint = Color.White) }
                Text("settings", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
        }
        item { SettingsToggle("auto-update on cellular", uiState.autoUpdateCellular) { viewModel.setAutoUpdateCellular(it) } }
        item { SettingsToggle("update in background", uiState.updateBackground) { viewModel.setUpdateBackground(it) } }
        item { SettingsToggle("low data mode", uiState.lowDataMode) { viewModel.setLowDataMode(it) } }
        item { SettingsToggle("keep screen awake", uiState.keepScreenAwake) { viewModel.setKeepScreenAwake(it) } }
        if (uiState.favoriteTeamIds.isNotEmpty()) {
            item { SettingsToggle("pin favorites", uiState.pinFavorites) { viewModel.setPinFavorites(it) } }
        }
        item { TeamRow("favorite teams", uiState.favoriteTeamAbbrevs, onNavigateToTeamPicker) }
        if (!uiState.autoUpdateCellular && uiState.favoriteTeamIds.isNotEmpty()) {
            item { SettingsToggle("auto-update favorites on cellular", uiState.autoUpdateFavoritesCellular) { viewModel.setAutoUpdateFavoritesCellular(it) } }
        }
        if (!uiState.updateBackground && uiState.favoriteTeamIds.isNotEmpty()) {
            item { SettingsToggle("update favorites in background", uiState.updateFavoritesBackground) { viewModel.setUpdateFavoritesBackground(it) } }
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle(!checked) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = Color.White, fontSize = 16.sp))
        SimpleSwitch(checked)
    }
}

@Composable
private fun SimpleSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp, 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color.White else Color.Transparent)
            .border(1.dp, Color.White, RoundedCornerShape(12.dp))
    )
}

@Composable
private fun TeamRow(label: String, teamIds: List<String>, onClick: () -> Unit) {
    val display = teamIds.joinToString(", ").ifEmpty { "none" }
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = Color.White, fontSize = 16.sp))
        Text(display, style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 16.sp))
    }
}

@Composable
fun TeamPickerScreen(viewModel: TeamPickerViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back", tint = Color.White) }
                Text("favorite teams", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
        }
        items(uiState.allTeams) { team ->
            val isSelected = uiState.selectedTeamIds.contains(team.id.toString())
            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTeam(team.id.toString()) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text((team.abbreviation ?: "").uppercase(), Modifier.width(48.dp), style = TextStyle(color = Color.White, fontSize = 16.sp))
                Text((team.name ?: "").lowercase(), Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 16.sp))
                SimpleSwitch(isSelected)
            }
        }
    }
}
