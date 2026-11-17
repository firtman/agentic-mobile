package dev.master.lumio.ui.arcade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.master.lumio.R

@Composable
fun ArcadeScreen(hasRound: Boolean, hasPuzzle: Boolean, play: () -> Unit, puzzle: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.arcade_heading), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.arcade_intro), style = MaterialTheme.typography.bodyLarge)
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(painterResource(R.drawable.lucide_circle_question_mark), null, tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.trivia), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.trivia_rules, 5, 20))
                Text(stringResource(R.string.trivia_english), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = play) { Text(stringResource(if (hasRound) R.string.return_to_round else R.string.play_trivia)) }
            }
        }
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(painterResource(R.drawable.lucide_puzzle), null, tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.poster_puzzle), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.puzzle_teaser))
                Button(onClick = puzzle) { Text(stringResource(if (hasPuzzle) R.string.return_to_puzzle else R.string.start_puzzle)) }
            }
        }
    }
}
