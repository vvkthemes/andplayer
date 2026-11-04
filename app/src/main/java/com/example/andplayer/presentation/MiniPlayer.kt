package com.example.andplayer.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                OneLineText(state.song.title)
                OneLineText(state.song.artist)
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
        }
    }
}
