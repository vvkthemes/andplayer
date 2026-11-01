package com.example.andplayer.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.andplayer.theme.AndPlayerTheme

@Composable
internal fun PlayerScreen(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun PlayerScreenPreview() {
    AndPlayerTheme {
        PlayerScreen("Android")
    }
}
