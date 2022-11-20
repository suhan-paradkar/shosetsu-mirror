package app.shosetsu.android.ui.browse

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

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import app.shosetsu.android.R
import app.shosetsu.android.activity.MainActivity
import app.shosetsu.android.common.consts.BROWSE_HELP_URL
import app.shosetsu.android.common.consts.BundleKeys.BUNDLE_EXTENSION
import app.shosetsu.android.common.ext.*
import app.shosetsu.android.domain.model.local.ExtensionInstallOptionEntity
import app.shosetsu.android.view.ComposeBottomSheetDialog
import app.shosetsu.android.view.compose.ErrorAction
import app.shosetsu.android.view.compose.ErrorContent
import app.shosetsu.android.view.compose.ImageLoadingError
import app.shosetsu.android.view.compose.ShosetsuCompose
import app.shosetsu.android.view.controller.ShosetsuController
import app.shosetsu.android.view.controller.base.ExtendedFABController
import app.shosetsu.android.view.controller.base.ExtendedFABController.EFabMaintainer
import app.shosetsu.android.view.controller.base.HomeFragment
import app.shosetsu.android.view.controller.base.syncFABWithCompose
import app.shosetsu.android.view.uimodels.model.BrowseExtensionUI
import app.shosetsu.android.viewmodel.abstracted.ABrowseViewModel
import app.shosetsu.lib.Version
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * shosetsu
 * 18 / 01 / 2020
 *
 * @author github.com/doomsdayrs
 */
class BrowseController : ShosetsuController(),
	ExtendedFABController, HomeFragment, MenuProvider {
	override val viewTitleRes: Int = R.string.browse

	private var bsg: BottomSheetDialog? = null

	/***/
	val viewModel: ABrowseViewModel by viewModel()

	private var fab: EFabMaintainer? = null

	override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.toolbar_browse, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		(menu.findItem(R.id.search)?.actionView as? SearchView)?.apply {
			setOnQueryTextListener(BrowseSearchQuery(findNavController()))
			isSubmitButtonEnabled = true
		}
	}

	private fun installExtension(
		extension: BrowseExtensionUI,
		option: ExtensionInstallOptionEntity
	) {
		if (!extension.isInstalled) {
			if (viewModel.isOnline()) {
				viewModel.installExtension(extension, option)
			} else {
				displayOfflineSnackBar(R.string.controller_browse_snackbar_offline_no_install_extension)
			}
		}
	}

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
					val entities by viewModel.liveData.collectAsState()
					var isRefreshing by remember { mutableStateOf(false) }
					BrowseContent(
						entities,
						refresh = {
							isRefreshing = true
							onRefresh()
							isRefreshing = false
						},
						installExtension = ::installExtension,
						update = viewModel::updateExtension,
						openCatalogue = ::openCatalogue,
						openSettings = ::openSettings,
						cancelInstall = viewModel::cancelInstall,
						isRefreshing = isRefreshing,
						fab
					)
				}
			}
		}
	}

	private fun openSettings(entity: BrowseExtensionUI) {
		viewModel.resetSearch()
		findNavController().navigateSafely(
			R.id.action_browseController_to_configureExtension,
			bundleOf(BUNDLE_EXTENSION to entity.id),
			navOptions = navOptions {
				launchSingleTop = true
				setShosetsuTransition()
			}
		)
	}

	private fun openCatalogue(entity: BrowseExtensionUI) {
		// First check if the user is online or not
		if (viewModel.isOnline()) {
			// If the extension is installed, push to it, otherwise prompt the user to install
			if (entity.isInstalled) {
				viewModel.resetSearch()
				findNavController().navigateSafely(
					R.id.action_browseController_to_catalogController,
					bundleOf(
						BUNDLE_EXTENSION to entity.id
					),
					navOptions = navOptions {
						launchSingleTop = true
						setShosetsuTransition()
					}
				)
			} else makeSnackBar(R.string.controller_browse_snackbar_not_installed)?.setAction(
				R.string.install
			) {
				// TODO install
			}?.show()
		} else displayOfflineSnackBar(R.string.controller_browse_snackbar_offline_no_extension)

	}

	override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.help -> {
			openHelpMenu()
			true
		}
		R.id.search -> true
		R.id.browse_import -> {
			makeSnackBar(R.string.regret)?.show()
			true
		}
		else -> false
	}

	private fun openHelpMenu() {
		activity?.openInWebView(BROWSE_HELP_URL)
	}

	private fun onRefresh() {
		if (viewModel.isOnline())
			viewModel.refresh()
		else displayOfflineSnackBar(R.string.controller_browse_snackbar_offline_no_update_extension)
	}

	override fun manipulateFAB(fab: EFabMaintainer) {
		this.fab = fab
		fab.setOnClickListener {
			//bottomMenuRetriever.invoke()?.show()
			if (bsg == null)
				bsg = ComposeBottomSheetDialog(
					this.requireView().context,
					this,
					activity as MainActivity
				)
			if (bsg?.isShowing == false) {
				bsg?.apply {
					setContentView(
						ComposeView(requireView().context).apply {
							setContent {
								ShosetsuCompose {
									BrowseControllerFilterMenu(viewModel)
								}
							}
						}
					)

				}?.show()
			}
		}
		fab.setText(R.string.filter)
		fab.setIconResource(R.drawable.filter)
	}
}

@Preview
@Composable
fun PreviewBrowseContent() {
	BrowseContent(
		entities =
		List(10) {
			BrowseExtensionUI(
				it,
				"Fake a b c",
				"",
				"en",
				installOptions = null,
				isInstalled = true,
				installedVersion = Version(1, 1, 1),
				installedRepo = 1,
				isUpdateAvailable = false,
				updateVersion = Version(1, 2, 1),
				isInstalling = false
			)
		}.toImmutableList(),
		{},
		{ _, _ -> },
		{},
		{},
		{},
		{},
		false,
		fab = null
	)
}

@Composable
fun BrowseContent(
	entities: ImmutableList<BrowseExtensionUI>?,
	refresh: () -> Unit,
	installExtension: (BrowseExtensionUI, ExtensionInstallOptionEntity) -> Unit,
	update: (BrowseExtensionUI) -> Unit,
	openCatalogue: (BrowseExtensionUI) -> Unit,
	openSettings: (BrowseExtensionUI) -> Unit,
	cancelInstall: (BrowseExtensionUI) -> Unit,
	isRefreshing: Boolean,
	fab: EFabMaintainer?
) {
	SwipeRefresh(
		state = rememberSwipeRefreshState(isRefreshing),
		onRefresh = refresh,
	) {
		if (!entities.isNullOrEmpty()) {
			val state = rememberLazyListState()
			if (fab != null)
				syncFABWithCompose(state, fab)
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(
					bottom = 198.dp,
					top = 4.dp,
					start = 8.dp,
					end = 8.dp
				),
				state = state,
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				items(entities) { entity ->
					BrowseExtensionContent(
						entity,
						install = {
							installExtension(entity, it)
						},
						update = {
							update(entity)
						},
						openCatalogue = {
							openCatalogue(entity)
						},
						openSettings = {
							openSettings(entity)
						},
						cancelInstall = {
							cancelInstall(entity)
						}
					)
				}
			}
		} else if (entities != null && entities.isEmpty()) {
			ErrorContent(
				R.string.empty_browse_message,
				ErrorAction(R.string.empty_browse_refresh_action) {
					refresh()
				}
			)
		}
	}
}

@Preview
@Composable
fun PreviewBrowseExtensionContent() {
	BrowseExtensionContent(
		BrowseExtensionUI(
			1,
			"Fake a  aaaaaaaaaaaaaaaaa",
			"",
			"en",
			installOptions = listOf(
				ExtensionInstallOptionEntity(1, "Wowa", Version(1, 1, 1))
			),
			isInstalled = true,
			installedVersion = Version(1, 1, 1),
			installedRepo = 1,
			isUpdateAvailable = true,
			updateVersion = Version(1, 2, 1),
			isInstalling = false
		),
		{},
		{},
		{},
		{},
		{}
	)
}

@OptIn(
	ExperimentalMaterial3Api::class,
	androidx.compose.foundation.ExperimentalFoundationApi::class,
	androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class,
	androidx.compose.ui.unit.ExperimentalUnitApi::class
)
@Composable
fun BrowseExtensionContent(
	item: BrowseExtensionUI,
	install: (ExtensionInstallOptionEntity) -> Unit,
	update: () -> Unit,
	openCatalogue: () -> Unit,
	openSettings: () -> Unit,
	cancelInstall: () -> Unit
) {
	Card(
		onClick = openCatalogue,
		shape = RoundedCornerShape(16.dp)
	) {
		Column {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(end = 8.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					if (item.imageURL.isNotEmpty()) {
						SubcomposeAsyncImage(
							ImageRequest.Builder(LocalContext.current)
								.data(item.imageURL)
								.crossfade(true)
								.build(),
							contentDescription = stringResource(R.string.controller_browse_ext_icon_desc),
							modifier = Modifier.size(64.dp),
							error = {
								ImageLoadingError()
							},
							loading = {
								Box(Modifier.placeholder(true))
							}
						)
					} else {
						ImageLoadingError(Modifier.size(64.dp))
					}
					Column(
						modifier = Modifier.padding(start = 8.dp)
					) {
						Text(item.name)
						Row {
							Text(item.displayLang, fontSize = TextUnit(14f, TextUnitType.Sp))

							if (item.isInstalled && item.installedVersion != null)
								Text(
									item.installedVersion.toString(),
									modifier = Modifier.padding(start = 8.dp),
									fontSize = TextUnit(14f, TextUnitType.Sp)
								)

							if (item.isUpdateAvailable && item.updateVersion != null) {
								if (item.updateVersion != Version(-9, -9, -9))
									Text(
										stringResource(
											R.string.update_to,
											item.updateVersion.toString()
										),
										modifier = Modifier.padding(start = 8.dp),
										fontSize = TextUnit(14f, TextUnitType.Sp),
										color = MaterialTheme.colorScheme.Tertiary
									)
							}
						}
					}
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.End
				) {
					if (!item.isInstalled && !item.isInstalling && !item.installOptions.isNullOrEmpty()) {
						var isDropdownVisible by remember { mutableStateOf(false) }
						IconButton(
							onClick = {
								// We can skip to dropdown if there is only 1 install option
								if (item.installOptions.size != 1)
									isDropdownVisible = true
								else install(item.installOptions[0])
							}
						) {
							Icon(painterResource(R.drawable.download), null)
						}
						DropdownMenu(
							expanded = isDropdownVisible,
							onDismissRequest = { isDropdownVisible = false },
						) {
							item.installOptions.forEach { s ->
								DropdownMenuItem(
									onClick = {
										install(s)
										isDropdownVisible = false
									}
								) {
									Row {
										Text(
											text = AnnotatedString(s.repoName)
										)
										Text(
											text = AnnotatedString(s.version.toString()),
											modifier = Modifier.padding(start = 8.dp)
										)
									}
								}
							}
						}
					}

					if (item.isUpdateAvailable) {
						IconButton(
							onClick = update
						) {
							Icon(
								painterResource(R.drawable.download),
								stringResource(R.string.update),
								modifier = Modifier.rotate(180f),
								tint = MaterialTheme.colorScheme.Tertiary
							)
						}
					}

					if (item.isInstalled) {
						IconButton(
							onClick = openSettings
						) {
							Icon(
								painterResource(R.drawable.settings),
								stringResource(R.string.settings)
							)
						}
					}

					if (item.isInstalling) {
						IconButton(
							onClick = {},
							modifier = Modifier.combinedClickable(
								onClick = {},
								onLongClick = cancelInstall,
							)
						) {
							val image =
								AnimatedImageVector.animatedVectorResource(R.drawable.animated_refresh)

							Icon(
								rememberAnimatedVectorPainter(image, false),
								stringResource(R.string.installing)
							)
						}
					}
				}

			}

			if (item.isUpdateAvailable && item.updateVersion != null) {
				if (item.updateVersion == Version(-9, -9, -9)) {
					Box(
						modifier = Modifier
							.background(MaterialTheme.colorScheme.Tertiary)
							.fillMaxWidth()
					) {
						Text(
							stringResource(R.string.obsolete_extension),
							color = colorResource(com.google.android.material.R.color.design_default_color_on_primary),
							modifier = Modifier
								.padding(8.dp)
								.align(Alignment.Center)
						)
					}
				}
			}
		}
	}
}