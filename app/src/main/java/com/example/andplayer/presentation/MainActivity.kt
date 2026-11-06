package com.example.andplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.andplayer.theme.AndPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndPlayerTheme {
                val playerBarViewModel: PlayerBarViewModel = hiltViewModel()
                val playerBarState by playerBarViewModel.playerBarState.collectAsStateWithLifecycle()

                Scaffold(
                    bottomBar = {
                        playerBarState?.let { state ->
                            PlayerBar(
                                state = state,
                                onTogglePlayback = playerBarViewModel::onTogglePlayback,
                                onStop = playerBarViewModel::onStop,
                                onPrev = playerBarViewModel::onPrev,
                                onNext = playerBarViewModel::onNext,
                                onSeek = playerBarViewModel::onSeekTo,
                                onRewind = playerBarViewModel::onRewind,
                                onForward = playerBarViewModel::onForward,
                            )
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        ListScreen(
                            onClickTrack = playerBarViewModel::onClickTrack,
                        )
                    }
                }
            }
        }
    }
}
