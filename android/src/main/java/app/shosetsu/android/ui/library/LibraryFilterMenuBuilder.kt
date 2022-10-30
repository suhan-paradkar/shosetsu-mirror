package app.shosetsu.android.ui.library

import android.content.Context
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.state.ToggleableState.Off
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.unit.dp
import app.shosetsu.android.R
import app.shosetsu.android.common.enums.NovelSortType
import app.shosetsu.android.common.enums.NovelSortType.*
import app.shosetsu.android.view.compose.ShosetsuCompose
import app.shosetsu.android.viewmodel.abstracted.ALibraryViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/*
 * This file is part of Shosetsu.
 *
 * Shosetsu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shosetsu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shosetsu.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * shosetsu
 * 22 / 11 / 2020
 *
 * Creates the bottom menu for Novel Controller
 */
@Stable
class LibraryFilterMenuBuilder constructor(
	private val context: Context,
	private val viewModel: ALibraryViewModel
) {
	@OptIn(ExperimentalPagerApi::class)
	fun build(): View =
		ComposeView(context).apply {
			setContent {
				ShosetsuCompose {
					val pagerState = rememberPagerState()
					val pages =
						listOf(stringResource(R.string.filter), stringResource(R.string.sort))
					val scope = rememberCoroutineScope()

					Column {
						TabRow(
							// Our selected tab is our current page
							selectedTabIndex = pagerState.currentPage,
							// Override the indicator, using the provided pagerTabIndicatorOffset modifier
							indicator = { tabPositions ->
								TabRowDefaults.Indicator(
									Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
								)
							}
						) {
							// Add tabs for all of our pages
							pages.forEachIndexed { index, title ->
								Tab(
									text = { Text(title) },
									selected = pagerState.currentPage == index,
									onClick = {
										scope.launch {
											pagerState.animateScrollToPage(index)
										}
									},
								)
							}
						}
						HorizontalPager(count = pages.size, state = pagerState) {
							when (it) {
								0 -> {
									Menu0Content()
								}
								1 -> {
									Menu1Content()
								}
							}
						}
					}
				}
			}
		}

	@Composable
	private fun Menu0Content() {
		val genres by viewModel.genresFlow.collectAsState(persistentListOf())
		val genresIsNotEmpty by derivedStateOf { genres.isNotEmpty() }
		var genresIsExpanded by remember { mutableStateOf(false) }

		val tags by viewModel.tagsFlow.collectAsState(persistentListOf())
		val tagsIsNotEmpty by derivedStateOf { tags.isNotEmpty() }
		var tagsIsExpanded by remember { mutableStateOf(false) }

		val authors by viewModel.authorsFlow.collectAsState(persistentListOf())
		val authorsIsNotEmpty by derivedStateOf { authors.isNotEmpty() }
		var authorsIsExpanded by remember { mutableStateOf(false) }

		val artists by viewModel.artistsFlow.collectAsState(persistentListOf())
		val artistsIsNotEmpty by derivedStateOf { artists.isNotEmpty() }
		var artistsIsExpanded by remember { mutableStateOf(false) }

		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState()),
		) {
			UnreadStatusFilter()

			if (genresIsNotEmpty)
				FilterContent(
					R.string.genres,
					genres,
					genresIsExpanded,
					toggleExpansion = {
						genresIsExpanded = !genresIsExpanded
					},
					getState = viewModel::getFilterGenreState,
					cycleState = viewModel::cycleFilterGenreState
				)

			if (tagsIsNotEmpty)
				FilterContent(
					R.string.tags,
					tags,
					tagsIsExpanded,
					toggleExpansion = {
						tagsIsExpanded = !tagsIsExpanded
					},
					getState = {
						viewModel.getFilterTagState(it)
					},
					cycleState = { name, state ->
						viewModel.cycleFilterTagState(name, state)
					}
				)

			if (authorsIsNotEmpty)
				FilterContent(
					R.string.authors,
					authors,
					authorsIsExpanded,
					toggleExpansion = {
						authorsIsExpanded = !authorsIsExpanded
					},
					getState = viewModel::getFilterAuthorState,
					cycleState = viewModel::cycleFilterAuthorState
				)

			if (artistsIsNotEmpty)
				FilterContent(
					R.string.artists,
					artists,
					artistsIsExpanded,
					toggleExpansion = {
						artistsIsExpanded = !artistsIsExpanded
					},
					getState = {
						viewModel.getFilterArtistState(it)
					},
					cycleState = { name, state ->
						viewModel.cycleFilterArtistState(name, state)
					}
				)
		}
	}

	@Composable
	private fun UnreadStatusFilter() {
		SimpleFilter(
			modifier = Modifier.padding(top = 8.dp),
			name = stringResource(R.string.unread_status),
			getState = {
				viewModel.getUnreadFilter()
			},
			cycleState = { state ->
				viewModel.cycleUnreadFilter(state)
			}
		)
	}

	@Composable
	private fun PinOnTopOption() {
		SimpleFilter(
			modifier = Modifier.padding(top = 8.dp),
			name = stringResource(R.string.pin_on_top),
			getState = {
				viewModel.isPinnedOnTop().map { if (it) On else Off }
			},
			cycleState = { state ->
				viewModel.setPinnedOnTop(state == On)
			}
		)
	}

	@Composable
	private fun SimpleFilter(
		modifier: Modifier = Modifier,
		name: String,
		getState: () -> Flow<ToggleableState>,
		cycleState: (ToggleableState) -> Unit
	) {
		Box(modifier) {
			val state by getState().collectAsState(Off)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.clickable {
						cycleState(state)
					}
					.padding(vertical = 8.dp, horizontal = 16.dp)
			) {

				TriStateCheckbox(state = state, null)

				Text(name, modifier = Modifier.padding(start = 8.dp))
			}
		}
	}

	@OptIn(ExperimentalMaterialApi::class)
	@Composable
	private fun ColumnScope.FilterContent(
		name: Int,
		items: ImmutableList<String>,
		isExpanded: Boolean,
		toggleExpansion: () -> Unit,
		getState: (String) -> Flow<ToggleableState>,
		cycleState: (String, ToggleableState) -> Unit
	) {
		Card(
			onClick = toggleExpansion,
			modifier = Modifier.padding(horizontal = 8.dp)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
			) {
				Icon(
					painterResource(if (isExpanded) R.drawable.expand_less else R.drawable.expand_more),
					null
				)
				Text(stringResource(name), modifier = Modifier.padding(start = 8.dp))
			}
		}

		AnimatedVisibility(isExpanded) {
			Column {
				items.forEach { item ->
					SimpleFilter(
						name = item,
						getState = {
							getState(item)
						},
						cycleState = {
							cycleState(item, it)
						}
					)
				}
			}
		}
	}

	@Composable
	private fun Menu1Item(
		name: Int,
		state: NovelSortType,
		expectedState: NovelSortType,
		reversed: Boolean
	) {
		val isExpected = state == expectedState
		Box(
			modifier = Modifier
				.clickable {
					if (isExpected)
						viewModel.setIsSortReversed(!reversed)
					else viewModel.setSortType(expectedState)
				}
				.padding(8.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
			) {
				Box(modifier = Modifier.size(32.dp)) {
					if (isExpected)
						Icon(
							painterResource(
								if (reversed) {
									R.drawable.expand_less
								} else {
									R.drawable.expand_more
								}
							),
							null,
							modifier = Modifier.align(Alignment.Center)
						)
				}
				Text(stringResource(name), modifier = Modifier.padding(start = 8.dp))
			}
		}
	}

	@Composable
	private fun Menu1Content() {
		val state by viewModel.getSortType().collectAsState(BY_TITLE)
		val reversed by viewModel.isSortReversed().collectAsState(false)

		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState()),
		) {
			PinOnTopOption()

			Menu1Item(
				R.string.controller_library_menu_tri_by_title,
				state,
				BY_TITLE,
				reversed
			)
			Menu1Item(
				R.string.controller_library_menu_tri_by_unread,
				state,
				BY_UNREAD_COUNT,
				reversed
			)
			Menu1Item(
				R.string.controller_library_menu_tri_by_id,
				state,
				BY_ID,
				reversed
			)
		}
	}

}