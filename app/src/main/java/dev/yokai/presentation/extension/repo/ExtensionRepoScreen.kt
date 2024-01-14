package dev.yokai.presentation.extension.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExtensionOff
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yokai.presentation.AppBarType
import dev.yokai.presentation.YokaiScaffold
import dev.yokai.presentation.component.EmptyScreen
import dev.yokai.presentation.extension.repo.component.ExtensionRepoItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ExtensionRepoScreen(
    title: String,
    onBackPress: () -> Unit,
    viewModel: ExtensionRepoViewModel = viewModel(),
    repoUrl: String? = null,
) {
    val context = LocalContext.current
    val repoState = viewModel.repoState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    YokaiScaffold(
        onNavigationIconClicked = onBackPress,
        title = title,
        appBarType = AppBarType.SMALL,
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 },
        ),
    ) { innerPadding ->
        if (repoState.value is ExtensionRepoState.Loading) return@YokaiScaffold

        val repos = (repoState.value as ExtensionRepoState.Success).repos

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            userScrollEnabled = true,
            verticalArrangement = Arrangement.Top,
            state = listState,
        ) {
            item {
                ExtensionRepoItem(
                    inputText = inputText,
                    inputHint = stringResource(R.string.label_add_repo),
                    onInputChange = { inputText = it },
                    onAddClick = { viewModel.addRepo(it) },
                )
            }

            if (repos.isEmpty()) {
                item {
                    EmptyScreen(
                        modifier = Modifier.fillParentMaxSize(),
                        image = Icons.Filled.ExtensionOff,
                        message = stringResource(R.string.information_empty_repos),
                    )
                }
                return@LazyColumn
            }

            repos.forEach { repo ->
                item {
                    ExtensionRepoItem(
                        repoUrl = repo,
                        // TODO: Confirmation dialog
                        onDeleteClick = { viewModel.deleteRepo(it) },
                    )
                }
            }
        }
    }

    LaunchedEffect(repoUrl) {
        repoUrl?.let { viewModel.addRepo(repoUrl) }
    }
    
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            if (event is ExtensionRepoEvent.LocalizedMessage)
                context.toast(event.stringRes)
            if (event is ExtensionRepoEvent.Success)
                inputText = ""
        }
    }
}
