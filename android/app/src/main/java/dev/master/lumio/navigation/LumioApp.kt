package dev.master.lumio.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import kotlinx.serialization.Serializable
import dev.master.lumio.AppContainer
import dev.master.lumio.R
import dev.master.lumio.ui.foundation.*
import dev.master.lumio.ui.explore.*
import dev.master.lumio.ui.details.*
import dev.master.lumio.ui.swipe.*
import dev.master.lumio.ui.history.*
import dev.master.lumio.ui.genie.*
import dev.master.lumio.ui.arcade.ArcadeScreen
import dev.master.lumio.ui.arcade.GameSound
import dev.master.lumio.ui.trivia.*
import dev.master.lumio.ui.puzzle.*
import dev.master.lumio.ui.settings.*
import androidx.navigation.toRoute
import androidx.lifecycle.createSavedStateHandle
import dev.master.lumio.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview

@Serializable data object Explore
@Serializable data object Swipe
@Serializable data object Genie
@Serializable data object PosterPuzzle
@Serializable data object Trivia
@Serializable data object Arcade
@Serializable data object History
@Serializable data object Settings
@Serializable data class MovieDetails(val id: String)
private data class Destination(val route: Any, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)
private val destinations = listOf(
    Destination(Explore,R.string.explore,R.drawable.lucide_compass),
    Destination(Swipe,R.string.swipe,R.drawable.lucide_layers_2),
    Destination(Genie,R.string.genie,R.drawable.lucide_sparkles),
    Destination(Arcade,R.string.arcade,R.drawable.lucide_gamepad_2),
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LumioApp(container: AppContainer, settingsModel: SettingsViewModel) {
    val preferences by settingsModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val settings = entry?.destination?.hasRoute<Settings>() == true
    // Details, settings (and later history and games) are full-screen with back navigation.
    val history = entry?.destination?.hasRoute<History>() == true
    val triviaRoute = entry?.destination?.hasRoute<Trivia>() == true
    val puzzleRoute = entry?.destination?.hasRoute<PosterPuzzle>() == true
    val fullScreen = settings || history || triviaRoute || puzzleRoute || entry?.destination?.hasRoute<MovieDetails>() == true
    val layout = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        androidx.compose.material3.adaptive.currentWindowAdaptiveInfo())
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.ui.graphics.Color.Transparent),
        navigationRailItemColors = NavigationRailItemDefaults.colors(indicatorColor = androidx.compose.ui.graphics.Color.Transparent),
    )
    NavigationSuiteScaffold(
        layoutType = if(fullScreen) NavigationSuiteType.None else layout,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteItems = {
            destinations.forEach { destination ->
                val selected = entry?.destination?.hasRoute(destination.route::class) == true
                item(selected = selected, onClick = {
                    nav.navigate(destination.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }, icon = { NavigationGlyph(destination.icon, selected) },
                    label = { Text(stringResource(destination.label), modifier = Modifier.widthIn(max = 80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    colors = itemColors)
            }
        },
    ) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(title = { Text(stringResource(if(settings) R.string.settings else if(history) R.string.history else if(triviaRoute) R.string.trivia else if(puzzleRoute) R.string.poster_puzzle else R.string.app_name), style = MaterialTheme.typography.headlineMedium) },
                    navigationIcon = { if(fullScreen) IconButton(onClick = { nav.navigateUp() }) {
                        Icon(painterResource(R.drawable.lucide_arrow_left), stringResource(R.string.back))
                    } },
                    actions = { if(!settings) IconButton(onClick = { nav.navigate(Settings) { launchSingleTop = true } }) {
                        Icon(painterResource(R.drawable.lucide_settings_2),stringResource(R.string.settings))
                    } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
            }) { padding ->
            NavHost(navController = nav, startDestination = Explore, modifier = Modifier.padding(padding)) {
                composable<Explore> {
                    val explore: ExploreViewModel = viewModel { ExploreViewModel(container.exploreRepository,createSavedStateHandle()) }
                    val state by explore.state.collectAsStateWithLifecycle()
                    ExploreScreen(state,explore::search,explore::selectGenre,explore::sort,explore::refresh,explore::loadMore,explore::retryGenres,explore::resetFilters) { movie ->
                        // Only from Explore itself, so two quick taps cannot stack two details screens.
                        if (nav.currentDestination?.hasRoute<Explore>() == true) nav.navigate(MovieDetails(movie.id))
                    }
                }
                composable<MovieDetails> { backStackEntry ->
                    val id = backStackEntry.toRoute<MovieDetails>().id
                    val details: MovieDetailsViewModel = viewModel { MovieDetailsViewModel(id,container.movieRepository,container.ratingsRepository) }
                    val state by details.state.collectAsStateWithLifecycle()
                    MovieDetailsScreen(state,details::rate,details::undo,details::retry,{ nav.navigateUp() },details::messageShown)
                }
                composable<Swipe> {
                    val swipe: SwipeViewModel = viewModel { SwipeViewModel(container.exploreRepository, container.ratingsRepository) }
                    val state by swipe.state.collectAsStateWithLifecycle()
                    SwipeScreen(state, swipe::rate, swipe::undo, swipe::retry, { movie ->
                        if (nav.currentDestination?.hasRoute<Swipe>() == true) nav.navigate(MovieDetails(movie.id))
                    }, swipe::messageShown, onHistory = {
                        if (nav.currentDestination?.hasRoute<Swipe>() == true) nav.navigate(History)
                    })
                }
                composable<History> {
                    val historyModel: HistoryViewModel = viewModel { HistoryViewModel(container.ratingsRepository) }
                    val state by historyModel.state.collectAsStateWithLifecycle()
                    HistoryScreen(state, historyModel::rate, historyModel::undo, historyModel::retry, { id ->
                        if (nav.currentDestination?.hasRoute<History>() == true) nav.navigate(MovieDetails(id))
                    }, historyModel::messageShown)
                }
                composable<Genie> {
                    val genie: GenieViewModel = viewModel { GenieViewModel(container.catalogRepository::catalog, container.ratingsRepository) }
                    val state by genie.state.collectAsStateWithLifecycle()
                    GenieScreen(state, genie::retry, { id ->
                        if (nav.currentDestination?.hasRoute<Genie>() == true) nav.navigate(MovieDetails(id))
                    }, {
                        nav.navigate(Swipe) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, {
                        if (nav.currentDestination?.hasRoute<Genie>() == true) nav.navigate(History)
                    })
                }
                composable<Arcade> { arcadeEntry ->
                    val game: TriviaViewModel = viewModel(arcadeEntry) { TriviaViewModel(container.catalogRepository::catalog) }
                    val state by game.state.collectAsStateWithLifecycle()
                    val puzzle: PuzzleViewModel = viewModel(arcadeEntry) { PuzzleViewModel(container.catalogRepository::catalog) }
                    val puzzleState by puzzle.state.collectAsStateWithLifecycle()
                    ArcadeScreen(state is TriviaState.Playing, puzzleState is PuzzleState.Playing, {
                        if (nav.currentDestination?.hasRoute<Arcade>() == true) nav.navigate(Trivia)
                    }, {
                        if (nav.currentDestination?.hasRoute<Arcade>() == true) nav.navigate(PosterPuzzle)
                    })
                }
                composable<Trivia> { triviaEntry ->
                    val arcadeEntry = remember(triviaEntry) { nav.getBackStackEntry(Arcade) }
                    val game: TriviaViewModel = viewModel(arcadeEntry) { TriviaViewModel(container.catalogRepository::catalog) }
                    val state by game.state.collectAsStateWithLifecycle()
                    GameSound(game.sounds, preferences.settings.sound)
                    TriviaScreen(state, game::start, game::retry, game::answer, game::pause, game::resume,
                        game::fiftyFifty, game::hold, game::next, game::playAgain, { nav.navigateUp() }, game::setVisible)
                }
                composable<PosterPuzzle> { puzzleEntry ->
                    val arcadeEntry = remember(puzzleEntry) { nav.getBackStackEntry(Arcade) }
                    val puzzle: PuzzleViewModel = viewModel(arcadeEntry) { PuzzleViewModel(container.catalogRepository::catalog) }
                    val state by puzzle.state.collectAsStateWithLifecycle()
                    GameSound(puzzle.sounds, preferences.settings.sound)
                    PuzzleScreen(state, puzzle::start, puzzle::retry, puzzle::choose, puzzle::toggleReference,
                        puzzle::nextPuzzle, puzzle::posterFailed, puzzle::posterLoaded, puzzle::retryPoster, { id ->
                            if (nav.currentDestination?.hasRoute<PosterPuzzle>() == true) nav.navigate(MovieDetails(id))
                        }, { nav.navigateUp() })
                }
                composable<Settings> { SettingsScreen(preferences, settingsModel::appearance, settingsModel::sound, settingsModel::retry) }
            }
        }
    }
}
@Composable private fun NavigationGlyph(@DrawableRes icon: Int, selected: Boolean) {
    Box(Modifier.size(48.dp,32.dp).background(if(selected) Coral else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp)),contentAlignment = Alignment.Center) {
        Icon(painterResource(icon),contentDescription = null, modifier = Modifier.size(24.dp),tint = if(selected) Ink else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun FoundationScreen(@StringRes title: Int, @DrawableRes icon: Int, connection: ConnectionState? = null, retry: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(32.dp))
        Icon(painterResource(icon),null,Modifier.size(32.dp),tint = MaterialTheme.colorScheme.secondary)
        Text(stringResource(title),style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.coming_soon),style = MaterialTheme.typography.bodyLarge,color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(connection != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when(connection) {
                ConnectionState.Loading -> {
                    CircularProgressIndicator(Modifier.size(24.dp),color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.loading_catalog))
                }
                ConnectionState.Error -> {
                    Text(stringResource(R.string.connection_error),color = MaterialTheme.colorScheme.error)
                    Button(onClick = retry) { Text(stringResource(R.string.retry)) }
                }
                is ConnectionState.Connected -> {
                    Text(stringResource(R.string.catalog_connected,connection.count),style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = retry, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Text(stringResource(R.string.check_connection)) }
                }
            }
        }
    }
}
// Preview functions should not be private so that Compose Tooling reflection (ComposableInvoker) can find and invoke them.
@Preview @Composable fun LightPreview() { LumioTheme(false) { Surface { FoundationScreen(R.string.explore,R.drawable.lucide_compass,ConnectionState.Connected(0)) } } }
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable fun DarkPreview() { LumioTheme(true) { Surface { FoundationScreen(R.string.explore,R.drawable.lucide_compass,ConnectionState.Error) } } }
@Preview(fontScale = 2f) @Composable fun LargeTextPreview() { LightPreview() }
@Preview(name = "German light", locale = "de") @Composable fun GermanLightPreview() { LightPreview() }
@Preview(name = "German dark", locale = "de", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable fun GermanDarkPreview() { DarkPreview() }
@Preview(name = "German large text", locale = "de", fontScale = 2f) @Composable fun GermanLargeTextPreview() { LightPreview() }
