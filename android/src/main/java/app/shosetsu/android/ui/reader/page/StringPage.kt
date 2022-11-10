package app.shosetsu.android.ui.reader.page

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import app.shosetsu.android.view.compose.ScrollStateBar
import app.shosetsu.android.view.compose.ShosetsuCompose
import kotlinx.coroutines.launch

/*
 * This file is part of shosetsu.
 *
 * shosetsu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shosetsu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shosetsu.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @since 18 / 03 / 2022
 * @author Doomsdayrs
 */

@Preview
@Composable
fun PreviewStringPageContent() {
	ShosetsuCompose {
		StringPageContent(
			content = "la\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\nla\n",
			progress = 0.0,
			textSize = 16.0f,
			onScroll = {},
			textColor = Color.Black.toArgb(),
			backgroundColor = Color.White.toArgb(),
			onClick = {},
			onDoubleClick = {}
			//isTapToScroll = false
		)
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StringPageContent(
	content: String,
	progress: Double,
	textSize: Float,
	onScroll: (perc: Double) -> Unit,
	textColor: Int,
	backgroundColor: Int,
	onClick: () -> Unit,
	onDoubleClick: () -> Unit
//isTapToScroll: Boolean
) {
	val state = rememberScrollState()
	var first by remember { mutableStateOf(true) }

	if (state.isScrollInProgress)
		DisposableEffect(Unit) {
			onDispose {
				if (state.value != 0)
					onScroll((state.value.toDouble() / state.maxValue))
				else onScroll(0.0)
			}
		}

	ScrollStateBar(state) {
		SelectionContainer(
			modifier = Modifier.combinedClickable(
				onDoubleClick = onDoubleClick,
				onClick = onClick,
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
			)

		) {
			Text(
				content,
				fontSize = textSize.sp,
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(state)
					.background(Color(backgroundColor)),
				color = Color(textColor)
			)
		}
	}

	// Avoid scrolling when the state has not fully loaded
	if (state.maxValue != 0 && state.maxValue != Int.MAX_VALUE) {
		if (first) {
			LaunchedEffect(progress) {
				launch {
					state.scrollTo((state.maxValue * progress).toInt())
					first = false
				}
			}
		}
	}
}