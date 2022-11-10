package app.shosetsu.android.ui.reader.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.shosetsu.android.R
import app.shosetsu.android.common.SettingKey
import app.shosetsu.android.ui.reader.page.StringPageContent
import app.shosetsu.android.view.compose.ErrorAction
import app.shosetsu.android.view.compose.ErrorContent
import app.shosetsu.android.view.uimodels.model.reader.ReaderUIItem
import app.shosetsu.android.viewmodel.abstracted.AChapterReaderViewModel
import kotlinx.coroutines.flow.Flow

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
 */

/**
 * Shosetsu
 *
 * @since 26 / 05 / 2022
 * @author Doomsdayrs
 */
/**
 * Creates the string page
 */
@Suppress("FunctionName")
@Composable
fun ChapterReaderStringContent(
	item: ReaderUIItem.ReaderChapterUI,

	getStringContent: (item: ReaderUIItem.ReaderChapterUI) -> Flow<AChapterReaderViewModel.ChapterPassage>,
	retryChapter: (item: ReaderUIItem.ReaderChapterUI) -> Unit,
	progressFlow: () -> Flow<Double>,
	textSizeFlow: () -> Flow<Float>,
	textColorFlow: () -> Flow<Int>,
	backgroundColorFlow: () -> Flow<Int>,
	onScroll: (item: ReaderUIItem.ReaderChapterUI, perc: Double) -> Unit,
	onClick: () -> Unit,
	onDoubleClick: () -> Unit
) {
	val content by remember(item) {
		getStringContent(item)
	}.collectAsState(AChapterReaderViewModel.ChapterPassage.Loading)

	when (content) {
		is AChapterReaderViewModel.ChapterPassage.Error -> {
			val throwable = (content as? AChapterReaderViewModel.ChapterPassage.Error)?.throwable
			ErrorContent(
				throwable?.message
					?: "Unknown error",
				ErrorAction(R.string.retry) {
					retryChapter(item)
				},
				stackTrace = throwable?.stackTraceToString()
			)
		}
		is AChapterReaderViewModel.ChapterPassage.Loading -> {
			val backgroundColor by backgroundColorFlow().collectAsState(
				Color.Gray.toArgb()
			)

			Box(
				Modifier
					.background(Color(backgroundColor))
					.fillMaxSize()
			) {
				LinearProgressIndicator(
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.TopCenter)
				)
			}
		}
		is AChapterReaderViewModel.ChapterPassage.Success -> {
			val textSize by remember { textSizeFlow() }.collectAsState(SettingKey.ReaderTextSize.default)
			val textColor by remember { textColorFlow() }.collectAsState(Color.White.toArgb())
			val progress by remember { progressFlow() }.collectAsState(0.0)
			val backgroundColor by remember { backgroundColorFlow() }.collectAsState(
				Color.Gray.toArgb()
			)


			StringPageContent(
				(content as? AChapterReaderViewModel.ChapterPassage.Success)?.content ?: "",
				progress,
				textSize = textSize,
				onScroll = {
					onScroll(item, it)
				},
				textColor = textColor,
				backgroundColor = backgroundColor,
				onClick = onClick,
				onDoubleClick = onDoubleClick
				//	isTapToScroll=isTapToScroll
			)
		}

	}
}