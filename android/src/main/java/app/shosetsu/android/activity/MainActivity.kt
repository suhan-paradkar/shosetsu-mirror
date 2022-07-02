package app.shosetsu.android.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_SEARCH
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.window.layout.WindowMetricsCalculator
import app.shosetsu.android.common.consts.*
import app.shosetsu.android.common.consts.BundleKeys.BUNDLE_QUERY
import app.shosetsu.android.common.enums.NavigationStyle.LEGACY
import app.shosetsu.android.common.enums.NavigationStyle.MATERIAL
import app.shosetsu.android.common.ext.*
import app.shosetsu.android.common.utils.collapse
import app.shosetsu.android.common.utils.expand
import app.shosetsu.android.domain.repository.base.IBackupRepository
import app.shosetsu.android.ui.browse.BrowseController
import app.shosetsu.android.ui.intro.IntroductionActivity
import app.shosetsu.android.ui.library.LibraryController
import app.shosetsu.android.ui.more.ComposeMoreController
import app.shosetsu.android.ui.search.SearchController
import app.shosetsu.android.ui.updates.ComposeUpdatesController
import app.shosetsu.android.view.controller.base.CollapsedToolBarController
import app.shosetsu.android.view.controller.base.ExtendedFABController
import app.shosetsu.android.view.controller.base.LiftOnScrollToolBarController
import app.shosetsu.android.view.controller.base.TabbedController
import app.shosetsu.android.viewmodel.abstracted.AMainViewModel
import com.bluelinelabs.conductor.Conductor.attachRouter
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.github.doomsdayrs.apps.shosetsu.R
import com.github.doomsdayrs.apps.shosetsu.databinding.ActivityMainBinding
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.BaseTransientBottomBar.Duration
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.acra.ACRA
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI


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
 * Shosetsu
 * 9 / June / 2019
 *
 * @author github.com/doomsdayrs
 */
class MainActivity : AppCompatActivity(), DIAware,
	ControllerChangeHandler.ControllerChangeListener {
	companion object {
		const val INTRO_CODE: Int = 1944
	}

	override val di: DI by closestDI()

	private lateinit var binding: ActivityMainBinding

	private var registered = false

	// The main router of the application
	private lateinit var router: Router

	private var actionBarDrawerToggle: ActionBarDrawerToggle? = null

	private val viewModel: AMainViewModel by viewModel()

	private val splashResultLauncher =
		registerForActivityResult(StartActivityForResult()) {
			if (it.resultCode == Activity.RESULT_OK) {
				viewModel.toggleShowIntro()
			}
		}

	private val broadcastReceiver by lazy {
		object : BroadcastReceiver() {
			override fun onReceive(context: Context?, intent: Intent?) {
				intent?.let {
					handleIntentAction(it)
				} ?: logE("Null intent recieved")
			}
		}
	}

	override fun onDestroy() {
		if (registered)
			unregisterReceiver(broadcastReceiver)
		super.onDestroy()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		val splashScreen = installSplashScreen()
		viewModel.navigationStyle

		onBackPressedDispatcher.addCallback(this) {
			logI("Back pressed")
			val backStackSize = router.backstackSize
			logD("Back stack size: $backStackSize")
			when {
				binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
					binding.drawerLayout.closeDrawer(GravityCompat.START)

				backStackSize == 1 && router.getControllerWithTag("${R.id.nav_library}") == null ->
					setSelectedDrawerItem(R.id.nav_library)

				shouldProtectBack() -> protectedBackWait()

				backStackSize == 1 || !router.handleBack() -> this@MainActivity.finish()
			}
		}

		runBlocking {
			setTheme(viewModel.appThemeLiveData.first())
		}
		DynamicColors.applyToActivityIfAvailable(this)
		viewModel.appThemeLiveData.collectLA(this, catch = {
			makeSnackBar(
				getString(
					R.string.activity_main_error_theme,
					it.message ?: "Unknown error"
				)
			).setAction(R.string.report) { _ ->
				ACRA.errorReporter.handleSilentException(it)
			}.show()
		}) {
			setTheme(it)
			DynamicColors.applyToActivityIfAvailable(this)
		}
		this.requestPerms()
		super.onCreate(savedInstanceState)

		// Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
		if (!isTaskRoot) {
			logI("Broadcasting intent ${intent.action}")
			sendBroadcast(Intent(intent.action))
			finish()
			return
		}
		registerReceiver(broadcastReceiver, IntentFilter().apply {
			addAction(ACTION_OPEN_UPDATES)
			addAction(ACTION_OPEN_LIBRARY)
			addAction(ACTION_OPEN_CATALOGUE)
			addAction(ACTION_OPEN_SEARCH)
			addAction(ACTION_OPEN_APP_UPDATE)
			addAction(ACTION_DOWNLOAD_COMPLETE)
		})
		registered = true

		runBlocking {
			// Settings setup
			if (viewModel.showIntro())
				splashResultLauncher.launch(
					Intent(
						this@MainActivity,
						IntroductionActivity::class.java
					)
				)
		}
		binding = ActivityMainBinding.inflate(layoutInflater)
		computeWindowSizeClasses()
		setupRouter(savedInstanceState)
		setContentView(binding.root)
		handleIntentAction(intent)
		setupProcesses()
	}

	private var isTablet = false

	/**
	 * Observe configuration changed
	 */
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		computeWindowSizeClasses()
	}

	/**
	 * Compute the dimensions of the display and morph the UI to match
	 */
	private fun computeWindowSizeClasses() {
		val metrics = WindowMetricsCalculator.getOrCreate()
			.computeCurrentWindowMetrics(this)

		val metricsWidth = metrics.bounds.width() / resources.displayMetrics.density

		isTablet = metricsWidth > 600
		logD("Is tablet?: $isTablet $metricsWidth")

		binding.navRail.removeHeaderView()
		binding.coordinator.removeView(binding.efab)
		binding.coordinator.addView(binding.efab)

		if (viewModel.navigationStyle == MATERIAL) {
			binding.navRail.isVisible = isTablet
			binding.navBottom.isVisible = !isTablet

			if (isTablet) {
				binding.coordinator.removeView(binding.efab)
				binding.navRail.addHeaderView(binding.efab)
				binding.efab.shrink()
			}
		}

		setupView()
	}


	/**
	 * Get the current navigation bar view
	 *
	 * If [isTablet] true, then the nav rail will be provided, else the bottom nav
	 */
	private fun getMaterialNav(): NavigationBarView {
		return if (isTablet)
			binding.navRail
		else binding.navBottom
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)

		actionBarDrawerToggle?.syncState()
	}

	/**
	 * If true, the app is preventing the user from leaving the app accidentally
	 */
	private var inProtectingBack = false

	private fun protectedBackWait() {
		launchIO {
			inProtectingBack = true
			val snackBar =
				makeSnackBar(R.string.double_back_message, Snackbar.LENGTH_INDEFINITE).apply {
					setOnDismissed { _, _ ->
						inProtectingBack = false
					}
				}
			snackBar.show()
			delay(2000)
			snackBar.dismiss()
		}
	}

	private fun shouldProtectBack(): Boolean =
		router.backstackSize == 1 &&
				router.getControllerWithTag("${R.id.nav_library}") != null &&
				viewModel.requireDoubleBackToExit &&
				!inProtectingBack

	private fun setSelectedDrawerItem(id: Int) {
		if (!isFinishing) {
			when (viewModel.navigationStyle) {
				MATERIAL -> {
					getMaterialNav().selectedItemId = id
					getMaterialNav().menu.performIdentifierAction(id, 0)
				}
				LEGACY -> {
					binding.navDrawer.setCheckedItem(id)
					binding.navDrawer.menu.performIdentifierAction(id, 0)
				}
			}
		}
	}

	private fun setupView() {
		//Sets the toolbar
		setSupportActionBar(binding.toolbar)

		binding.toolbar.setNavigationOnClickListener {
			logV("Navigation item clicked")
			if (router.backstackSize == 1) {
				if (viewModel.navigationStyle == LEGACY) {
					binding.drawerLayout.openDrawer(GravityCompat.START)
				} else onBackPressed()
			} else onBackPressed()
		}

		when (viewModel.navigationStyle) {
			MATERIAL -> {
				getMaterialNav().isVisible = true
				binding.navDrawer.isVisible = false
				setupMaterialNavigation()
			}
			LEGACY -> {
				getMaterialNav().isVisible = false
				binding.navDrawer.isVisible = true
				setupLegacyNavigation()
			}
		}
	}

	/**
	 * Setup the navigation drawer
	 */
	private fun setupLegacyNavigation() {
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		actionBarDrawerToggle = ActionBarDrawerToggle(
			this,
			binding.drawerLayout,
			binding.toolbar,
			R.string.navigation_drawer_open,
			R.string.navigation_drawer_close
		)

		actionBarDrawerToggle?.setToolbarNavigationClickListener {
			onBackPressed()
		}

		binding.drawerLayout.addDrawerListener(actionBarDrawerToggle!!)


		// Navigation view
		//nav_view.setNavigationItemSelectedListener(NavigationSwapListener(this))
		binding.navDrawer.setNavigationItemSelectedListener {
			logI("Navigation item selected: $it")
			val id = it.itemId
			val currentRoot = router.backstack.firstOrNull()
			if (currentRoot?.tag()?.toIntOrNull() != id) handleNavigationSelected(id)
			binding.drawerLayout.closeDrawer(GravityCompat.START)
			return@setNavigationItemSelectedListener true
		}
	}


	/**
	 * Setup the bottom navigation
	 */
	private fun setupMaterialNavigation() {
		binding.drawerLayout.setDrawerLockMode(
			DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
			binding.navDrawer
		)

		getMaterialNav().setOnItemSelectedListener {
			val id = it.itemId
			val currentRoot = router.backstack.firstOrNull()
			if (currentRoot?.tag()?.toIntOrNull() != id) handleNavigationSelected(id)
			return@setOnItemSelectedListener true
		}
	}

	private fun handleNavigationSelected(id: Int) {
		when (id) {
			R.id.nav_library -> setRoot(LibraryController(), R.id.nav_library)
			R.id.nav_updates -> setRoot(ComposeUpdatesController(), R.id.nav_updates)
			R.id.nav_browse -> setRoot(BrowseController(), R.id.nav_browse)
			R.id.nav_more -> setRoot(ComposeMoreController(), R.id.nav_more)
		}
	}

	private fun setupRouter(savedInstanceState: Bundle?) {
		router = attachRouter(this, binding.controllerContainer, savedInstanceState)
		router.addChangeListener(this)
		syncActivityViewWithController(router.backstack.lastOrNull()?.controller)
	}

	private fun actionMain() {
		if (!router.hasRootController()) {
			setSelectedDrawerItem(R.id.nav_library)
		} else {
			logE("Router has a root controller")
		}
	}

	internal fun handleIntentAction(intent: Intent) {
		logD("Intent received was ${intent.action}")
		when (intent.action) {
			ACTION_OPEN_CATALOGUE -> setSelectedDrawerItem(R.id.nav_browse)
			ACTION_OPEN_UPDATES -> setSelectedDrawerItem(R.id.nav_updates)
			ACTION_OPEN_LIBRARY -> setSelectedDrawerItem(R.id.nav_library)
			ACTION_SEARCH -> {
				if (!router.hasRootController()) setSelectedDrawerItem(R.id.nav_library)

				router.shosetsuPush(
					SearchController(
						bundleOf(
							BUNDLE_QUERY to (intent.getStringExtra(SearchManager.QUERY) ?: "")
						)
					)
				)
			}
			ACTION_OPEN_SEARCH -> {
				if (!router.hasRootController()) setSelectedDrawerItem(R.id.nav_library)

				router.shosetsuPush(
					SearchController(
						bundleOf(
							BUNDLE_QUERY to (intent.getStringExtra(SearchManager.QUERY) ?: "")
						)
					)
				)
			}
			ACTION_OPEN_APP_UPDATE -> {
				handleAppUpdate()
				actionMain()
			}
			ACTION_MAIN -> actionMain()
			else -> if (!router.hasRootController()) {
				setSelectedDrawerItem(R.id.nav_library)
			} else {
				logE("Router has a root controller")
			}
		}
	}

	private fun setRoot(controller: Controller, id: Int) {
		router.setRoot(controller.withFadeTransaction().tag(id.toString()))
	}

	private fun setupProcesses() {
		viewModel.startAppUpdateCheck().collectLA(this, catch = {
			makeSnackBar(
				getString(
					R.string.activity_main_error_update_check,
					it.message ?: "Unknown error"
				)
			)
				.setAction(R.string.report) { _ ->
					ACRA.errorReporter.handleSilentException(it)
				}.show()
		}) { result ->
			if (result != null)
				AlertDialog.Builder(this).apply {
					setTitle(R.string.update_app_now_question)
					setMessage(
						"${result.version}\t${result.versionCode}\n" + result.notes.joinToString(
							"\n"
						)
					)
					setPositiveButton(R.string.update) { it, _ ->
						handleAppUpdate()
						it.dismiss()
					}
					setNegativeButton(R.string.update_not_interested) { it, _ ->
						it.dismiss()
					}
					setOnDismissListener { dialogInterface ->
						dialogInterface.dismiss()
					}
				}.let {
					launchUI { it.show() }
				}
		}
		viewModel.backupProgressState.collectLatestLA(this, catch = {
			logE("Backup failed", it)
			ACRA.errorReporter.handleException(it)
			binding.backupWarning.isVisible = false
		}) {
			when (it) {
				IBackupRepository.BackupProgress.IN_PROGRESS -> {
					binding.backupWarning.isVisible = true
				}
				IBackupRepository.BackupProgress.NOT_STARTED -> {
					binding.backupWarning.isVisible = false
				}
				IBackupRepository.BackupProgress.COMPLETE -> {
					binding.backupWarning.isVisible = false
				}
				IBackupRepository.BackupProgress.FAILURE -> {
					binding.backupWarning.isVisible = false
				}
			}
		}
	}

	private fun handleAppUpdate() {
		viewModel.handleAppUpdate().collectLA(this, catch = {
			makeSnackBar(
				getString(
					R.string.activity_main_error_handle_update,
					it.message ?: "Unknown error"
				)
			)
				.setAction(R.string.report) { _ ->
					ACRA.errorReporter.handleSilentException(it)
				}.show()
		}) {
			if (it != null)
				when (it) {
					AMainViewModel.AppUpdateAction.SelfUpdate -> {
						makeSnackBar(R.string.activity_main_app_update_download)
					}
					is AMainViewModel.AppUpdateAction.UserUpdate -> {
						openShare(it.updateURL, it.updateTitle)
					}
				}
		}
	}

	private val eFabMaintainer by lazy {
		object : ExtendedFABController.EFabMaintainer {
			override fun hide() {
				if (!isTablet)
					binding.efab.hide()
			}

			override fun show() {
				binding.efab.show()
			}

			override fun setOnClickListener(onClick: ((View) -> Unit)?) {
				binding.efab.setOnClickListener(onClick)
			}

			override fun shrink() {
				binding.efab.shrink()
			}

			override fun extend() {
				if (!isTablet)
					binding.efab.extend()
			}

			override fun setText(textRes: Int) {
				if (!isTablet)
					binding.efab.setText(textRes)
				ViewCompat.setTooltipText(binding.efab, getString(textRes))
			}

			override fun setIconResource(iconRes: Int) {
				binding.efab.setIconResource(iconRes)
			}

		}
	}

	@SuppressLint("ObjectAnimatorBinding")
	internal fun syncActivityViewWithController(to: Controller?, from: Controller? = null) {
		val showHamburger = router.backstackSize == 1 // Show hamburg means this is home

		logI("Show hamburger?: $showHamburger")

		if (showHamburger) {
			// Shows navigation
			when (viewModel.navigationStyle) {
				LEGACY -> {
					logI("Sync activity view with controller for legacy")
					actionBarDrawerToggle?.isDrawerIndicatorEnabled = true
					binding.drawerLayout.setDrawerLockMode(
						DrawerLayout.LOCK_MODE_UNLOCKED,
						binding.navDrawer
					)
				}
				MATERIAL -> {
					supportActionBar?.setDisplayHomeAsUpEnabled(false)
					if (!isTablet)
						binding.navBottom.isVisible = true
				}
			}
		} else {

			// Hides navigation
			when (viewModel.navigationStyle) {
				LEGACY -> {
					logI("Sync activity view with controller for legacy")
					actionBarDrawerToggle?.isDrawerIndicatorEnabled = false
					binding.drawerLayout.setDrawerLockMode(
						DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
						binding.navDrawer
					)
				}
				MATERIAL -> {
					supportActionBar?.setDisplayHomeAsUpEnabled(true)
					if (!isTablet)
						binding.navBottom.isVisible = false
				}
			}
		}

		if (from is ExtendedFABController) {
			binding.navRail.headerView?.isVisible = false
			from.hideFAB(eFabMaintainer)
			from.resetFAB(eFabMaintainer)
		}

		if (to is ExtendedFABController) {
			to.manipulateFAB(eFabMaintainer)
			to.showFAB(eFabMaintainer)
			binding.navRail.headerView?.isVisible = true
		}

		val tabLayout = binding.tabLayout

		if (from is TabbedController) {
			tabLayout.removeAllTabs()
			tabLayout.clearOnTabSelectedListeners()
		}

		if (to is TabbedController) {
			to.acceptTabLayout(tabLayout)
			to.configureTabs(tabLayout)
		}

		// clean up TabbedController
		if (from is TabbedController && to !is TabbedController) tabLayout.collapse()

		// setup TabbedController
		if (from !is TabbedController && to is TabbedController) tabLayout.expand()

		// Change the elevation for the app bar layout
		when (to) {
			is CollapsedToolBarController -> {
				binding.elevatedAppBarLayout.drop()
			}
			is LiftOnScrollToolBarController -> {
				binding.elevatedAppBarLayout.elevate(true)
			}
			else -> {
				binding.elevatedAppBarLayout.elevate(false)
			}
		}
	}

	@SuppressLint("ShowToast")
	fun makeSnackBar(
		@StringRes stringRes: Int,
		@Duration length: Int = Snackbar.LENGTH_SHORT
	): Snackbar =
		makeSnackBar(getString(stringRes), length)

	@SuppressLint("ShowToast")
	fun makeSnackBar(
		string: String,
		@Duration length: Int = Snackbar.LENGTH_SHORT
	): Snackbar =
		Snackbar.make(binding.coordinator, string, length).apply {
			when {
				binding.efab.isVisible -> anchorView = binding.efab
				binding.navBottom.isVisible -> anchorView = binding.navBottom
			}
		}

	override fun onChangeStarted(
		to: Controller?,
		from: Controller?,
		isPush: Boolean,
		container: ViewGroup,
		handler: ControllerChangeHandler,
	) {
		binding.elevatedAppBarLayout.setExpanded(true)
		syncActivityViewWithController(to, from)
	}

	override fun onChangeCompleted(
		to: Controller?,
		from: Controller?,
		isPush: Boolean,
		container: ViewGroup,
		handler: ControllerChangeHandler,
	) {
	}
}