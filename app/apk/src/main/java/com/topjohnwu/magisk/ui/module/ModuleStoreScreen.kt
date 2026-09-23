package com.topjohnwu.magisk.ui.module

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.R as CoreR
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleStoreScreen(
    viewModel: ModuleStoreViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val rooted = Info.isRooted
    var showRootDialog by remember { mutableStateOf(false) }
    var selectedModule by remember { mutableStateOf<StoreModule?>(null) }
    val modules = viewModel.filteredModules(state)
    val selectedRepository = MODULE_REPOSITORIES.firstOrNull {
        it.id == state.selectedRepositoryId
    }
    val selectedRepositoryName = selectedRepository?.name
        ?: stringResource(CoreR.string.module_store_all_repositories)
    val repositoryModuleCount = if (state.selectedRepositoryId == ALL_MODULE_REPOSITORIES) {
        state.modules.distinctBy { it.id.lowercase() }.size
    } else {
        state.modules.count { it.repositoryId == state.selectedRepositoryId }
    }

    if (showRootDialog) {
        AlertDialog(
            onDismissRequest = { showRootDialog = false },
            title = { Text(stringResource(CoreR.string.module_store_root_required_title)) },
            text = { Text(stringResource(CoreR.string.module_store_root_required_message)) },
            confirmButton = {
                TextButton(onClick = { showRootDialog = false }) {
                    Text(stringResource(CoreR.string.ok))
                }
            },
        )
    }

    if (selectedModule != null) {
        ModuleStoreDetailScreen(
            viewModel = viewModel,
            module = selectedModule!!,
            rooted = rooted,
            onBack = { selectedModule = null },
            onInstall = { installModule(selectedModule!!, context, rooted) { showRootDialog = true } },
            onOpenSource = { openModuleSource(selectedModule!!, context) },
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(CoreR.string.module_store_title))
                        Text(
                            selectedRepositoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(CoreR.string.back),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::reload) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(CoreR.string.refresh),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                label = { Text(stringResource(CoreR.string.module_store_search)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepositorySelector(
                    selectedRepositoryId = state.selectedRepositoryId,
                    onSelect = viewModel::selectRepository,
                    modifier = Modifier.weight(1f),
                )
                SortSelector(
                    selectedSort = state.sort,
                    onSelect = viewModel::setSort,
                )
            }

            FilterChip(
                selected = state.favoritesOnly,
                onClick = { viewModel.setFavoritesOnly(!state.favoritesOnly) },
                label = {
                    Text(
                        stringResource(
                            if (state.favoritesOnly) {
                                CoreR.string.module_store_show_all
                            } else {
                                CoreR.string.module_store_favorites
                            },
                        ),
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Star, contentDescription = null)
                },
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = if (rooted) {
                                stringResource(CoreR.string.module_store_root_ready)
                            } else {
                                stringResource(CoreR.string.module_store_browse_only)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(
                            CoreR.string.module_store_repository_summary,
                            state.loadedRepositoryIds.size,
                            repositoryModuleCount,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (state.favoriteIds.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                CoreR.string.module_store_favorites_saved,
                                state.favoriteIds.size,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    if (state.repositoryErrors.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                CoreR.string.module_store_repository_unavailable,
                                state.repositoryErrors.size,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.error != null -> {
                    StoreError(
                        message = state.error!!,
                        onRetry = viewModel::reload,
                    )
                }
                modules.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(
                                if (state.favoritesOnly) {
                                    CoreR.string.module_store_no_favorites
                                } else {
                                    CoreR.string.module_store_empty
                                },
                            ),
                        )
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScrollbar(listState),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(modules, key = { "${it.repositoryId}:${it.id}" }) { module ->
                            StoreModuleCard(
                                module = module,
                                favorite = viewModel.isFavorite(module),
                                onToggleFavorite = { viewModel.toggleFavorite(module) },
                                onOpenDetails = { selectedModule = module },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositorySelector(
    selectedRepositoryId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedRepositoryName = MODULE_REPOSITORIES.firstOrNull {
        it.id == selectedRepositoryId
    }?.name ?: stringResource(CoreR.string.module_store_all_repositories)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = "${stringResource(CoreR.string.module_store_repository)}: $selectedRepositoryName",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(CoreR.string.module_store_all_repositories)) },
                onClick = {
                    expanded = false
                    onSelect(ALL_MODULE_REPOSITORIES)
                },
            )
            MODULE_REPOSITORIES.forEach { repository ->
                DropdownMenuItem(
                    text = { Text(repository.name) },
                    onClick = {
                        expanded = false
                        onSelect(repository.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun SortSelector(
    selectedSort: ModuleSort,
    onSelect: (ModuleSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selectedSort) {
        ModuleSort.POPULAR -> CoreR.string.module_store_sort_popular
        ModuleSort.RECENT -> CoreR.string.module_store_sort_recent
        ModuleSort.NAME -> CoreR.string.module_store_sort_name
    }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = stringResource(CoreR.string.module_store_sort, stringResource(label)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ModuleSort.entries.forEach { sort ->
                val sortLabel = when (sort) {
                    ModuleSort.POPULAR -> CoreR.string.module_store_sort_popular
                    ModuleSort.RECENT -> CoreR.string.module_store_sort_recent
                    ModuleSort.NAME -> CoreR.string.module_store_sort_name
                }
                DropdownMenuItem(
                    text = { Text(stringResource(sortLabel)) },
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    },
                )
            }
        }
    }
}

@Composable
private fun StoreModuleCard(
    module: StoreModule,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Card(
        onClick = onOpenDetails,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteModuleImage(
                    urls = module.iconUrls.take(6),
                    contentDescription = module.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        module.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        module.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(CoreR.string.module_store_source_label, module.repositoryName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = stringResource(
                            if (favorite) {
                                CoreR.string.module_store_remove_favorite
                            } else {
                                CoreR.string.module_store_add_favorite
                            },
                        ),
                        tint = if (favorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(CoreR.string.module_store_stars, module.stars),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(CoreR.string.module_store_details),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleStoreDetailScreen(
    viewModel: ModuleStoreViewModel,
    module: StoreModule,
    rooted: Boolean,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var details by remember(module.id, module.repositoryId) {
        mutableStateOf<StoreModuleDetails?>(null)
    }
    var detailError by remember(module.id, module.repositoryId) {
        mutableStateOf<String?>(null)
    }
    var detailRequest by remember(module.id, module.repositoryId) { mutableIntStateOf(0) }
    var selectedScreenshot by remember { mutableStateOf<String?>(null) }
    val storeState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorite = storeState.favoriteIds.contains("${module.repositoryId}::${module.id}")
    LaunchedEffect(module.id, module.repositoryId, detailRequest) {
        details = null
        detailError = null
        runCatching { viewModel.loadDetails(module) }
            .onSuccess { details = it }
            .onFailure { detailError = it.message ?: "Unable to load module details" }
    }

    selectedScreenshot?.let { screenshotUrl ->
        Dialog(
            onDismissRequest = { selectedScreenshot = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
            ) {
                RemoteModuleImage(
                    urls = listOf(screenshotUrl),
                    contentDescription = module.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                )
                IconButton(
                    onClick = { selectedScreenshot = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(CoreR.string.close),
                        tint = Color.White,
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(module.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(module) }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(
                                if (favorite) {
                                    CoreR.string.module_store_remove_favorite
                                } else {
                                    CoreR.string.module_store_add_favorite
                                },
                            ),
                            tint = if (favorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = onOpenSource) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = stringResource(CoreR.string.module_store_source),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (detailError != null) {
                    StoreError(
                        message = detailError!!,
                        onRetry = { detailRequest++ },
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        val loaded = details!!
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar(rememberLazyListState()),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RemoteModuleImage(
                            urls = loaded.iconUrls.take(12),
                            contentDescription = module.name,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(26.dp)),
                        )
                        Spacer(Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                module.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                module.id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                module.version.ifBlank { stringResource(CoreR.string.module_store_version_unknown) },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(CoreR.string.module_store_about),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    loaded.description.ifBlank {
                        stringResource(CoreR.string.module_store_description_unavailable)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (loaded.screenshotUrls.isNotEmpty()) {
                item {
                    Text(
                        stringResource(CoreR.string.module_store_screenshots),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(loaded.screenshotUrls, key = { it }) { url ->
                            Box(
                                modifier = Modifier
                                    .size(width = 250.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                            ) {
                                RemoteModuleImage(
                                    urls = listOf(url),
                                    contentDescription = module.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { selectedScreenshot = url },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(CoreR.string.module_store_source_label, module.repositoryName),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (module.author.isNotBlank()) {
                            Text(
                                stringResource(CoreR.string.module_store_author, module.author),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = onInstall,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (rooted) stringResource(CoreR.string.module_store_install)
                                else stringResource(CoreR.string.module_store_install_root),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteModuleImage(
    urls: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = urls) {
        value = withContext(Dispatchers.IO) {
            var decoded: Bitmap? = null
            for (url in urls) {
                try {
                    decoded = decodeRemoteBitmap(
                        ServiceLocator.networkService.fetchFileBytes(url)
                    )
                    if (decoded != null) break
                } catch (_: Exception) {
                    // Try the next GitHub mirror/path candidate.
                }
            }
            decoded
        }
    }
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            RoundedCornerShape(20.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private fun decodeRemoteBitmap(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )
}

private fun installModule(
    module: StoreModule,
    context: android.content.Context,
    rooted: Boolean,
    onRootRequired: () -> Unit,
) {
    if (!rooted) {
        onRootRequired()
    } else {
        val title = "${module.id.replace(Regex("[^A-Za-z0-9._-]"), "_")}.zip"
        DownloadEngine.start(
            context.applicationContext,
            StoreModuleSubject(module.zipUrl, title, module.id),
        )
    }
}

private fun openModuleSource(module: StoreModule, context: android.content.Context) {
    val url = module.sourceUrl.ifBlank { module.notesUrl.ifBlank { module.propUrl } }
    if (url.isNotBlank()) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun StoreError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(CoreR.string.module_store_load_failed),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(CoreR.string.retry)) }
    }
}
