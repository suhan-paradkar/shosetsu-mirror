package app.shosetsu.android.ui.reader

import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterialApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import app.shosetsu.android.common.consts.BundleKeys.BUNDLE_CHAPTER_ID
import app.shosetsu.android.common.consts.BundleKeys.BUNDLE_NOVEL_ID
import app.shosetsu.android.common.ext.*
import app.shosetsu.android.ui.reader.content.*
import app.shosetsu.android.ui.reader.page.DividierPageContent
import app.shosetsu.android.view.uimodels.model.reader.ReaderUIItem.ReaderChapterUI
import app.shosetsu.android.view.uimodels.model.reader.ReaderUIItem.ReaderDividerUI
import app.shosetsu.android.viewmodel.abstracted.AChapterReaderViewModel
import app.shosetsu.android.viewmodel.abstracted.AChapterReaderViewModel.ChapterPassage
import app.shosetsu.android.viewmodel.impl.settings.*
import app.shosetsu.lib.Novel.ChapterType
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.material.composethemeadapter.Mdc3Theme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import java.util.*


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
 * shosetsu
 * 13 / 12 / 2019
 */
class ChapterReader
	: AppCompatActivity(), DIAware {
	override val di: DI by closestDI()
	internal val viewModel: AChapterReaderViewModel by viewModel()

	private val isTTSCapable = MutableStateFlow(false)
	private val isTTSPlaying = MutableStateFlow(false)

	private val ttsInitListener: TextToSpeech.OnInitListener by lazy {
		TextToSpeech.OnInitListener {
			when (it) {
				TextToSpeech.SUCCESS -> {
					val result = tts.setLanguage(Locale.getDefault())

					if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
						logE("Language not supported for TTS")
						isTTSCapable.value = false
					} else {
						isTTSCapable.value = true
					}
				}
				else -> {
					logE("TTS Initialization failed")
					isTTSCapable.value = false
				}
			}
		}
	}

	private val tts: TextToSpeech by lazy {
		TextToSpeech(this, ttsInitListener)
	}

	override fun onTrimMemory(level: Int) {
		super.onTrimMemory(level)
		// Determine which lifecycle or system event was raised.
		when (level) {
			ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
				/*
				   Release any UI objects that currently hold memory.

				   The user interface has moved to the background.
				*/
			}

			ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
			ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
			ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
				/*
				   Release any memory that your app doesn't need to run.

				   The device is running low on memory while the app is running.
				   The event raised indicates the severity of the memory-related event.
				   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
				   begin killing background processes.
				*/
			}

			ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
			ComponentCallbacks2.TRIM_MEMORY_MODERATE,
			ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
				/*
				   Release as much memory as the process can.

				   The app is on the LRU list and the system is running low on memory.
				   The event raised indicates where the app sits within the LRU list.
				   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
				   the first to be terminated.
				*/
			}

			else -> {
				/*
				  Release any non-critical data structures.

				  The app received an unrecognized memory level value
				  from the system. Treat this as a generic low-memory message.
				*/
				viewModel.clearMemory()
			}
		}

	}

	/** On Create */
	@OptIn(ExperimentalMaterialApi::class, ExperimentalPagerApi::class)
	public override fun onCreate(savedInstanceState: Bundle?) {
		logV("")
		viewModel.apply {
			setNovelID(intent.getIntExtra(BUNDLE_NOVEL_ID, -1))
			viewModel.setCurrentChapterID(intent.getIntExtra(BUNDLE_CHAPTER_ID, -1), true)
		}
		runBlocking {
			setTheme(viewModel.appThemeLiveData.first())
		}
		viewModel.appThemeLiveData.collectLA(this, catch = {}) {
			setTheme(it)
		}
		super.onCreate(savedInstanceState)
		val insetsController = WindowInsetsControllerCompat(window, window.decorView)

		setContent {
			val items by viewModel.liveData.collectAsState()
			val isHorizontalReading by viewModel.isHorizontalReading.collectAsState()
			val isBookmarked by viewModel.isCurrentChapterBookmarked.collectAsState()
			val isRotationLocked by viewModel.liveIsScreenRotationLocked.collectAsState()
			val isFocused by viewModel.isFocused.collectAsState()
			val enableFullscreen by viewModel.enableFullscreen.collectAsState()
			val matchFullscreenToFocus by viewModel.matchFullscreenToFocus.collectAsState()
			val chapterType by viewModel.chapterType.collectAsState()
			val currentChapterID by viewModel.currentChapterID.collectAsState()
			val isTTSCapable by isTTSCapable.collectAsState()
			val isTTSPlaying by isTTSPlaying.collectAsState()
			val setting by viewModel.getSettings().collectAsState()
			val currentPage by viewModel.currentPage.collectAsState()

			val isFirstFocus by viewModel.isFirstFocusFlow.collectAsState()
			val isSwipeInverted by viewModel.isSwipeInverted.collectAsState()
			//val isTapToScroll by viewModel.tapToScroll.collectAsState(false)

			Mdc3Theme {
				ChapterReaderContent(
					isFirstFocus = isFirstFocus,
					isFocused = isFocused,
					onFirstFocus = viewModel::onFirstFocus,
					sheetContent = { state ->
						ChapterReaderBottomSheetContent(
							scaffoldState = state,
							isTTSCapable = isTTSCapable,
							isTTSPlaying = isTTSPlaying,
							isBookmarked = isBookmarked,
							isRotationLocked = isRotationLocked,
							setting = setting,
							toggleRotationLock = viewModel::toggleScreenRotationLock,
							toggleBookmark = viewModel::toggleBookmark,
							exit = {
								finish()
							},
							onPlayTTS = {
								if (chapterType == null) return@ChapterReaderBottomSheetContent
								items
									.orEmpty()
									.filterIsInstance<ReaderChapterUI>()
									.find { it.id == currentChapterID }
									?.let { item ->
										tts.setPitch(viewModel.ttsPitch.value)
										tts.setSpeechRate(viewModel.ttsSpeed.value)
										when (chapterType!!) {
											ChapterType.STRING -> {
												viewModel.getChapterStringPassage(item)
													.collectLA(
														this@ChapterReader,
														catch = {}) { content ->
														if (content is ChapterPassage.Success)
															tts.speak(
																content.content,
																TextToSpeech.QUEUE_FLUSH,
																null,
																content.hashCode().toString()
															)
													}

											}
											ChapterType.HTML -> {
												viewModel.getChapterStringPassage(item)
													.collectLA(
														this@ChapterReader,
														catch = {}) { content ->
														if (content is ChapterPassage.Success)
															tts.speak(
																content.content,
																TextToSpeech.QUEUE_FLUSH,
																null,
																content.hashCode().toString()
															)
													}
											}
											else -> {}
										}
									}
							},
							onStopTTS = {
								tts.stop()
							},
							updateSetting = viewModel::updateSetting,
							lowerSheet = {
								item { viewModel.textSizeOption() }
								//item { viewModel.tapToScrollOption() }
								//item { viewModel.volumeScrollingOption() }
								//item { viewModel.horizontalSwitchOption() }
								item { viewModel.invertChapterSwipeOption() }
								item { viewModel.readerKeepScreenOnOption() }
								item { viewModel.enableFullscreen() }
								item { viewModel.matchFullscreenToFocus() }
								item { viewModel.showReaderDivider() }
								item { viewModel.stringAsHtmlOption() }
								item { viewModel.doubleTapFocus() }
								item { viewModel.doubleTapSystem() }
								item { viewModel.readerTableHackOption() }
							},
							toggleFocus = viewModel::toggleFocus,
							onShowNavigation = viewModel::toggleSystemVisible.takeIf { enableFullscreen && !matchFullscreenToFocus },
						)
					},
					content = { paddingValues ->
						ChapterReaderPagerContent(
							paddingValues = paddingValues,
							items = items ?: persistentListOf(),
							isHorizontal = isHorizontalReading,
							isSwipeInverted = isSwipeInverted,
							currentPage = currentPage,
							onPageChanged = viewModel::setCurrentPage,
							markChapterAsCurrent = {
								viewModel.onViewed(it)
								viewModel.setCurrentChapterID(it.id)
							},
							onChapterRead = viewModel::updateChapterAsRead,
							onStopTTS = {
								tts.stop()
							},
							createPage = { page ->
								when (val item = items.orEmpty()[page]) {
									is ReaderChapterUI -> {
										when (chapterType) {
											ChapterType.STRING -> {
												ChapterReaderStringContent(
													item = item,
													getStringContent = viewModel::getChapterStringPassage,
													retryChapter = viewModel::retryChapter,
													textSizeFlow = { viewModel.liveTextSize },
													textColorFlow = { viewModel.textColor },
													containerColorFlow = { viewModel.containerColor },
													onScroll = viewModel::onScroll,
													onClick = viewModel::onReaderClicked,
													onDoubleClick = viewModel::onReaderDoubleClicked,
													progressFlow = {
														viewModel.getChapterProgress(item)
													}
												)
											}
											ChapterType.HTML -> {
												ChapterReaderHTMLContent(
													item = item,
													getHTMLContent = viewModel::getChapterHTMLPassage,
													retryChapter = viewModel::retryChapter,
													onScroll = viewModel::onScroll,
													onClick = viewModel::onReaderClicked,
													onDoubleClick = viewModel::onReaderDoubleClicked,
													progressFlow = {
														viewModel.getChapterProgress(item)
													}
												)
											}
											else -> {
											}
										}
									}
									is ReaderDividerUI -> {
										DividierPageContent(
											item.prev.title,
											item.next?.title
										)
									}
								}
							}
						)
					}
					//isTapToScroll = isTapToScroll
				)
			}
		}

		viewModel.isSystemVisible.collectLA(this, catch = {
		}) {
			if (!it) {
				insetsController.hide(Type.systemBars())
				insetsController.hide(Type.displayCutout())
				insetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
			} else {
				insetsController.show(Type.systemBars())
				insetsController.show(Type.displayCutout())
			}
		}

		viewModel.liveIsScreenRotationLocked.collectLA(this, catch = {}) {
			if (it)
				lockRotation()
			else unlockRotation()
		}

		viewModel.liveKeepScreenOn.collectLA(this, catch = {}) {
			if (it) {
				window.addFlags(FLAG_KEEP_SCREEN_ON)
			} else {
				window.clearFlags(FLAG_KEEP_SCREEN_ON)
			}
		}
	}

	/** On Destroy */
	override fun onDestroy() {
		logV("")
		tts.stop()
		tts.shutdown()
		super.onDestroy()
	}

	/**
	 * Adds the
	 */
	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return if (viewModel.isVolumeScrollEnabled.value)
			when (keyCode) {
				KeyEvent.KEYCODE_VOLUME_DOWN -> {
					viewModel.incrementProgress()
					true
				}
				KeyEvent.KEYCODE_VOLUME_UP -> {
					viewModel.depleteProgress()
					true
				}
				else -> super.onKeyDown(keyCode, event)
			}
		else super.onKeyDown(keyCode, event)
	}

	private fun lockRotation() {
		val currentOrientation = resources.configuration.orientation
		requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
		} else {
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
		}
	}

	private fun unlockRotation() {
		//window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
		requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
	}
}
