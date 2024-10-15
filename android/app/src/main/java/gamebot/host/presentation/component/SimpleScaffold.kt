package gamebot.host.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleScaffold(
    navController: NavController,
    title: String,
    hideBackButton: Boolean = false,
    scrollable: Boolean = false,
    actions: @Composable() (RowScope.() -> Unit) = {},
    snackbarHost: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable() (ColumnScope.() -> Unit)
) {

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
        TopAppBar(
//            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
            title = {
                Text(
                    title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                if (!hideBackButton) {

                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "back", // TODO
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            actions = actions
        )
    }, snackbarHost = snackbarHost,


    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .let {
                if (scrollable) {
                    it.verticalScroll(rememberScrollState())
                } else {
                    it
                }
            }
        Column(
            modifier, content = content
        )
    }

}


@Composable
fun CenterScaffold(
    navController: NavController,
    title: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    SimpleScaffold(
        navController, title, scrollable = false,
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        },
    )
}