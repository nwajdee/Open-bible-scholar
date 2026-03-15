package com.openbiblescholar.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.openbiblescholar.data.db.entity.SwordModuleEntity
import com.openbiblescholar.data.model.ModuleType
import com.openbiblescholar.services.sword.DownloadProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateUp: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val tabs = ModuleType.values().map { it.displayName() }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text("Library", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshCatalog() }) {
                            Icon(Icons.Filled.Refresh, "Refresh catalog")
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { i, name ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    ) { padding ->

        val moduleType = ModuleType.values()[selectedTab]
        val filteredModules = uiState.modules.filter {
            it.moduleType == moduleType.name
        }

        if (filteredModules.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading catalog...", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(padding)
            ) {
                item {
                    Text(
                        "${filteredModules.count { it.isDownloaded }}/${filteredModules.size} installed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(filteredModules) { module ->
                    ModuleCard(
                        module = module,
                        downloadProgress = if (downloadProgress.moduleName == module.name) downloadProgress else null,
                        onDownload = { viewModel.downloadModule(module.name) },
                        onDelete = { viewModel.deleteModule(module.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: SwordModuleEntity,
    downloadProgress: DownloadProgress?,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (module.isDownloaded) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    moduleTypeIcon(module.moduleType),
                    null,
                    tint = if (module.isDownloaded) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(module.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                if (module.isDownloaded) {
                    Icon(Icons.Filled.CheckCircle, "Installed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(module.language.uppercase(), style = MaterialTheme.typography.labelSmall) }
                    )
                    if (module.isPublicDomain) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Public Domain", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null, Modifier.size(12.dp)) }
                        )
                    }
                    Text("${module.downloadSizeMb.toInt()} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }

            // Download progress
            if (downloadProgress != null && !downloadProgress.isComplete) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = downloadProgress.percent / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${downloadProgress.percent}% downloaded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }

            // Action button
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (module.isDownloaded) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                } else {
                    Button(onClick = onDownload) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

private fun moduleTypeIcon(type: String) = when (type) {
    "BIBLE" -> Icons.Outlined.MenuBook
    "COMMENTARY" -> Icons.Outlined.Comment
    "DICTIONARY" -> Icons.Outlined.Search
    "LEXICON" -> Icons.Outlined.Translate
    "CHURCH_FATHERS" -> Icons.Outlined.HistoryEdu
    "DEVOTIONAL" -> Icons.Outlined.Favorite
    "MAPS" -> Icons.Outlined.Map
    else -> Icons.Outlined.LibraryBooks
}

private fun ModuleType.displayName() = when (this) {
    ModuleType.BIBLE -> "Bibles"
    ModuleType.COMMENTARY -> "Commentaries"
    ModuleType.DICTIONARY -> "Dictionaries"
    ModuleType.LEXICON -> "Lexicons"
    ModuleType.CHURCH_FATHERS -> "Church Fathers"
    ModuleType.DEVOTIONAL -> "Devotionals"
    ModuleType.GENERAL_BOOK -> "Books"
    ModuleType.MAPS -> "Maps"
}
