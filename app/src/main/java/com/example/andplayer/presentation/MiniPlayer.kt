package com.example.andplayer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.andplayer.R

@Composable
internal fun MiniPlayer(
    state: MiniPlayerUiState,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 3.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ListItem(
                headlineContent = { OneLineText(state.song.title) },
                supportingContent = { OneLineText(state.song.artist) },
                trailingContent = { OneLineText(state.song.duration) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        painter = painterResource(R.drawable.ic_prev),
                        contentDescription = "Previous",
                    )
                }

                IconButton(onClick = onTogglePlayback) {
                    if (state.isPlaying)
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = "Pause",
                        )
                    else
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = "Play",
                        )
                }

                IconButton(onClick = onStop) {
                    Icon(
                        painter = painterResource(R.drawable.ic_stop),
                        contentDescription = "Stop",
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Next",
                    )
                }
            }
        }
    }
}
