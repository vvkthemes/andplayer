package com.example.andplayer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.andplayer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiniPlayer(
    state: MiniPlayerUiState,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
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
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(getDurationString(state.position), style = MaterialTheme.typography.labelSmall)
                Slider(
                    modifier = Modifier
                        .height(8.dp)
                        .padding(horizontal = 8.dp)
                        .weight(1f),
                    value = if (state.song.duration > 0) {
                        (state.position.toFloat() / state.song.duration).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    onValueChange = { fraction -> onSeek((fraction * state.song.duration).toLong()) },
                    thumb = {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(4.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            thumbTrackGapSize = 0.dp,
                            drawStopIndicator = null,
                        )
                    },
                )
                Text(state.song.durationString, style = MaterialTheme.typography.labelSmall)
            }

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

                IconButton(onClick = onRewind) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rewind),
                        contentDescription = "Rewind",
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

                IconButton(onClick = onForward) {
                    Icon(
                        painter = painterResource(R.drawable.ic_forward),
                        contentDescription = "Forward",
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
