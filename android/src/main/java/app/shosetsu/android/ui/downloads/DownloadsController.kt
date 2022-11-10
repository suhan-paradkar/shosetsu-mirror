package app.shosetsu.android.ui.downloads

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

import android.os.Bundle
import android.view.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import app.shosetsu.android.R
import app.shosetsu.android.common.enums.DownloadStatus.*
import app.shosetsu.android.common.ext.collectLA
import app.shosetsu.android.common.ext.displayOfflineSnackBar
import app.shosetsu.android.common.ext.viewModel
import app.shosetsu.android.view.compose.ErrorContent
import app.shosetsu.android.view.compose.SelectableBox
import app.shosetsu.android.view.compose.ShosetsuCompose
import app.shosetsu.android.view.controller.ShosetsuController
import app.shosetsu.android.view.controller.base.ExtendedFABController
import app.shosetsu.android.view.controller.base.ExtendedFABController.EFabMaintainer
import app.shosetsu.android.view.controller.base.syncFABWithCompose
import app.shosetsu.android.view.uimodels.model.DownloadUI
import app.shosetsu.android.viewmodel.abstracted.ADownloadsViewModel
import app.shosetsu.android.viewmodel.abstracted.ADownloadsViewModel.SelectedDownloadsState
import kotlinx.collections.immutable.ImmutableList

/**
 * Shosetsu
 * 9 / June / 2019
 *
 * @author github.com/doomsdayrs
 */
class DownloadsController : ShosetsuController(),
	ExtendedFABController, MenuProvider {

	override val viewTitleRes: Int = R.string.downloads
	private val viewModel: ADownloadsViewModel by viewModel()
	private var fab: EFabMaintainer? = null
	private var actionMode: ActionMode? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedViewState: Bundle?
	): View {
		activity?.addMenuProvider(this, viewLifecycleOwner)
		setViewTitle()
		return ComposeView(requireContext()).apply {
			setContent {
				ShosetsuCompose {
					val items by viewModel.liveData.collectAsState()
					val selectedDownloadState by viewModel.selectedDownloadState.collectAsState()
					val hasSelected by viewModel.hasSelectedFlow.collectAsState()

					DownloadsContent(
						items = items,
						selectedDownloadState = selectedDownloadState,
						hasSelected = hasSelected,
						pauseSelection = viewModel::pauseSelection,
						startSelection = viewModel::startSelection,
						startFailedSelection = viewModel::restartSelection,
						deleteSelected = viewModel::deleteSelected,
						toggleSelection = viewModel::toggleSelection,
						fab
					)
				}
			}
		}
	}

	private fun startSelectionAction(): Boolean {
		if (actionMode != null) return false
		hideFAB(fab!!)
		actionMode = activity?.startActionMode(SelectionActionMode())
		return true
	}

	override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.toolbar_downloads, menu)
	}

	override fun onMenuItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.set_all_pending -> {
				viewModel.setAllPending()
				return true
			}
			R.id.delete_all -> {
				viewModel.deleteAll()
				return true
			}
			else -> false
		}
	}

	private fun togglePause() {
		if (viewModel.isOnline()) viewModel.togglePause() else displayOfflineSnackBar(R.string.controller_downloads_snackbar_offline_no_download)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		viewModel.showFAB.collectLA(this, catch = {}) { showFAB ->
			if (showFAB)
				fab?.show()
			else fab?.hide()
		}
		viewModel.isDownloadPaused.collectLA(this, catch = {}) {
			fab?.setText(
				if (it)
					R.string.resume
				else R.string.pause
			)
			fab?.setIconResource(
				if (it)
					R.drawable.play_arrow
				else R.drawable.ic_pause_circle_outline_24dp
			)
		}
		viewModel.hasSelectedFlow.collectLA(this, catch = {}) {
			if (it) {
				startSelectionAction()
			} else {
				actionMode?.finish()
			}
		}
	}

	override fun onDestroy() {
		actionMode?.finish()
		super.onDestroy()
	}

	override fun manipulateFAB(fab: EFabMaintainer) {
		this.fab = fab
		fab.setOnClickListener { togglePause() }
		fab.setText(R.string.paused)
		fab.setIconResource(R.drawable.ic_pause_circle_outline_24dp)
	}

	override fun showFAB(fab: EFabMaintainer) {
		if (actionMode == null) super.showFAB(fab)
	}

	private inner class SelectionActionMode : ActionMode.Callback {
		override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
			// Hides the original action bar
			// (activity as MainActivity?)?.supportActionBar?.hide()

			mode.menuInflater.inflate(R.menu.toolbar_downloads_selected, menu)
			mode.setTitle(R.string.selection)
			return true
		}

		override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

		override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
			when (item.itemId) {
				R.id.chapter_select_all -> {
					viewModel.selectAll()
					true
				}
				R.id.chapter_select_between -> {
					viewModel.selectBetween()
					true
				}
				R.id.chapter_inverse -> {
					viewModel.invertSelection()
					true
				}
				else -> false
			}

		override fun onDestroyActionMode(mode: ActionMode) {
			actionMode = null
			showFAB(fab!!)
			viewModel.deselectAll()
		}
	}
}

@Composable
fun DownloadsContent(
	items: ImmutableList<DownloadUI>,
	selectedDownloadState: SelectedDownloadsState,
	hasSelected: Boolean,
	pauseSelection: () -> Unit,
	startSelection: () -> Unit,
	startFailedSelection: () -> Unit,
	deleteSelected: () -> Unit,
	toggleSelection: (DownloadUI) -> Unit,
	fab: EFabMaintainer?
) {
	if (items.isNotEmpty()) {
		Box(
			modifier = Modifier.fillMaxSize()
		) {
			val state = rememberLazyListState()
			if (fab != null)
				syncFABWithCompose(state, fab)
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = 140.dp),
				state = state
			) {
				items(items, key = { it.chapterID }) {
					DownloadContent(
						it,
						onClick = {
							if (hasSelected)
								toggleSelection(it)
						},
						onLongClick = {
							toggleSelection(it)
						}
					)
				}
			}

			if (hasSelected) {
				Card(
					modifier = Modifier
						.align(BiasAlignment(0f, 0.7f))
				) {
					Row {
						IconButton(
							onClick = pauseSelection,
							enabled = selectedDownloadState.pauseVisible
						) {
							Icon(
								painterResource(R.drawable.pause),
								stringResource(R.string.pause)
							)
						}
						IconButton(
							onClick = startSelection,
							enabled = selectedDownloadState.startVisible
						) {
							Icon(
								painterResource(R.drawable.play_arrow),
								stringResource(R.string.start)
							)
						}
						IconButton(
							onClick = startFailedSelection,
							enabled = selectedDownloadState.restartVisible
						) {
							Icon(
								painterResource(R.drawable.refresh),
								stringResource(R.string.restart)
							)
						}
						IconButton(
							onClick = deleteSelected,
							enabled = selectedDownloadState.deleteVisible
						) {
							Icon(
								painterResource(R.drawable.trash),
								stringResource(R.string.delete)
							)
						}
					}
				}
			}
		}
	} else {
		ErrorContent(
			stringResource(R.string.empty_downloads_message)
		)
	}
}

@Preview
@Composable
fun PreviewDownloadContent() {
	ShosetsuCompose {
		DownloadContent(
			DownloadUI(
				0,
				0,
				"aaa",
				"Chpater",
				"Novel",
				0,
				DOWNLOADING,
				false
			),
			{},
			{}
		)
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadContent(
	item: DownloadUI,
	onClick: () -> Unit,
	onLongClick: () -> Unit,
) {
	SelectableBox(
		item.isSelected,
		modifier = Modifier
			.combinedClickable(
				onClick = onClick,
				onLongClick = onLongClick
			)
	) {
		Column(
			Modifier
				.padding(16.dp)
				.fillMaxWidth()
		) {
			Text(
				text = item.novelName,
				style = MaterialTheme.typography.bodyLarge
			)
			Text(
				text = item.chapterName,
				style = MaterialTheme.typography.bodyMedium
			)

			Row(
				Modifier
					.padding(top = 8.dp)
					.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				val status = item.status
				if (status == DOWNLOADING || status == WAITING) {
					LinearProgressIndicator(modifier = Modifier.fillMaxWidth(.7f))
				} else {
					LinearProgressIndicator(0.0f, modifier = Modifier.fillMaxWidth(.7f))
				}

				Text(
					text = stringResource(
						id = when (status) {
							PENDING -> {
								R.string.pending
							}
							DOWNLOADING -> {
								R.string.downloading
							}
							PAUSED -> {
								R.string.paused
							}
							ERROR -> {
								R.string.error
							}
							WAITING -> {
								R.string.waiting
							}
							else -> {
								R.string.completed
							}
						}
					),
					textAlign = TextAlign.End,
					style = MaterialTheme.typography.bodySmall,
					modifier = Modifier
						.padding(start = 8.dp)
						.fillMaxWidth()
				)
			}
		}
	}
}