package app.shosetsu.android.common

import app.shosetsu.android.common.enums.MarkingType
import app.shosetsu.android.domain.model.local.LibrarySortFilterEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
 * 23 / 06 / 2020
 */


typealias IntKey = SettingKey<Int>
typealias BooleanKey = SettingKey<Boolean>
typealias FloatKey = SettingKey<Float>
typealias StringKey = SettingKey<String>
typealias StringSetKey = SettingKey<Set<String>>


sealed class SettingKey<T : Any>(val name: String, val default: T) {

	/**
	 * Languages to filter out of browse view
	 */
	object BrowseFilteredLanguages : StringSetKey("browse_filtered_languages", emptySet())

	/**
	 * Show only installed extensions
	 */
	object BrowseOnlyInstalled : BooleanKey("browse_only_installed", false)

	/**
	 * Selected reader theme
	 */
	object ReaderTheme : IntKey("readerTheme", -1)

	/**
	 * Is this the first time the application ran?
	 */
	object FirstTime : BooleanKey("first_time2", true)


	/**
	 * Themes that can be edited by the user
	 */
	// How things look in Reader
	object ReaderUserThemes : StringSetKey("readerThemes", emptySet())


	object ReaderTextSize : FloatKey("readerTextSize", 14f)

	object ReaderParagraphSpacing : FloatKey("readerParagraphSpacing", 1f)

	object ReaderIndentSize : IntKey("readerIndentSize", 1)

	/**
	 * 0 : align start
	 * 1 : align center
	 * 2 : justified
	 * 3 : align end
	 */
	object ReaderTextAlignment : IntKey("readerTextAlignment", 0)

	object ReaderMarginSize : FloatKey("readerMargin", 0f)

	object ReaderLineSpacing : FloatKey("readerLineSpacing", 1f)

	object ReaderFont : StringKey("readerFont", "")

	object ReaderType : IntKey("readerType", -1)

	//- How things act in Reader
	object ReaderIsTapToScroll : BooleanKey("tapToScroll", false)
	object ReaderVolumeScroll : BooleanKey("volumeToScroll_force", false)


	object ReaderIsInvertedSwipe : BooleanKey("invertedSwipe", false)
	object ReadingMarkingType : StringKey("readingMarkingType", MarkingType.ONVIEW.name)

	/**
	 * Should the application convert string returns from an extension to an Html page
	 */
	object ReaderStringToHtml : BooleanKey("convertStringToHtml", false)
	object ReaderIsFirstFocus : BooleanKey("reader_first_focus", true)
	object ReaderDoubleTapFocus : BooleanKey("reader_double_tap_focus", false)
	object ReaderDoubleTapSystem : BooleanKey("reader_double_tap_system", false)
	object ReaderEnableFullscreen : BooleanKey("reader_enable_fullscreen", true)
	object ReaderMatchFullscreenToFocus : BooleanKey("reader_match_fullscreen_focus", false)

	/**
	 * User customization for CSS in html reader
	 */
	object ReaderHtmlCss : StringKey(
		"readerHtmlCss",
		"""
			
		""".trimIndent()
	)

	/**
	 * Instead of vertically moving between chapters, do a horizontal move
	 */
	object ReaderHorizontalPageSwap : BooleanKey("readerHorizontalPaging_force", true)

	/**
	 * The reader smoothly moves between chapters
	 */
	object ReaderContinuousScroll : BooleanKey("readerContinuousScroll", false)

	/**
	 * The reader should keep the screen on
	 */
	object ReaderKeepScreenOn : BooleanKey("reader_keep_screen_on", false)

	/**
	 * Should the reader display a separator between chapters
	 */
	object ReaderShowChapterDivider : BooleanKey("reader_show_divider_page", true)

	//- Some things
	object ChaptersResumeFirstUnread : BooleanKey(
		"readerResumeFirstUnread",
		false
	)

	object ReaderPitch : FloatKey("reader_pitch", 1f)

	object ReaderSpeed : FloatKey("reader_speed", 1f)

	// Download options
	object IsDownloadPaused : BooleanKey("isDownloadPaused", false)


	/**
	 * Which chapter to delete after reading
	 *
	 * If -1, then does nothing
	 *
	 * If 0, then deletes the read chapter
	 *
	 * If 1+, deletes the chapter of READ CHAPTER - deletePreviousChapter
	 */
	object DeleteReadChapter : IntKey("deleteReadChapter_force", -1)

	object DownloadOnLowStorage : BooleanKey("downloadNotLowStorage", true)
	object DownloadOnLowBattery : BooleanKey("downloadNotLowBattery", true)
	object DownloadOnMeteredConnection : BooleanKey("downloadNotMetered", true)

	/**
	 * Set the time between any two downloads, forcing the task to occur in a reasonable time frame.
	 */
	object DownloadBufferTime : IntKey("download_time_space", 5)
	object DownloadOnlyWhenIdle : BooleanKey("downloadIdle", false)

	object NotifyExtensionDownload : BooleanKey("notifyExtensionDownloading", false)

	/** Bookmark a novel if it is not bookmarked when a chapter from it is downloaded */
	object BookmarkOnDownload : BooleanKey("bookmarkOnDownload", false)

	// Update options
	object DownloadNewNovelChapters : BooleanKey("isDownloadOnUpdate", false)
	object OnlyUpdateOngoingNovels : BooleanKey("onlyUpdateOngoing", false)
	object UpdateNovelsOnStartup : BooleanKey("updateOnStartup", true)

	object IncludeCategoriesInUpdate : StringSetKey("includedCategoriesInUpdate", emptySet())
	object ExcludedCategoriesInUpdate : StringSetKey("excludedCategoriesInUpdate", emptySet())

	object IncludeCategoriesToDownload : StringSetKey("includedCategoriesToDownload", emptySet())
	object ExcludedCategoriesToDownload : StringSetKey("excludedCategoriesToDownload", emptySet())

	object NovelUpdateCycle : IntKey("updateCycle", 12)
	object NovelUpdateOnLowStorage : BooleanKey("updateLowStorage", true)
	object NovelUpdateOnLowBattery : BooleanKey("updateLowBattery", true)
	object NovelUpdateOnMeteredConnection : BooleanKey("updateMetered", true)
	object NovelUpdateOnlyWhenIdle : BooleanKey("updateIdle", false)

	object UpdateNotificationStyle : BooleanKey("updateNotificationStyle", false)
	object NovelUpdateShowProgress : BooleanKey("novelUpdateShowProgress", true)
	object NovelUpdateClassicFinish : BooleanKey("novelUpdateClassicFinish", false)

	object RepoUpdateOnLowStorage : BooleanKey("repoUpdateLowStorage", true)
	object RepoUpdateOnLowBattery : BooleanKey("repoUpdateLowBattery", true)
	object RepoUpdateOnMeteredConnection : BooleanKey("repoUpdateMetered", true)
	object RepoUpdateDisableOnFail : BooleanKey("update_repository_disable_on_fail", false)


	// App Update Options
	object AppUpdateOnStartup : BooleanKey("appUpdateOnStartup", true)
	object AppUpdateOnMeteredConnection : BooleanKey("appUpdateMetered", true)

	object AppUpdateOnlyWhenIdle : BooleanKey("appUpdateIdle", false)
	object AppUpdateCycle : IntKey("appUpdateCycle", 12)


	// View options
	object ChapterColumnsInPortait : IntKey("columnsInNovelsViewP", 3)
	object ChapterColumnsInLandscape : IntKey("columnsInNovelsViewH", 6)
	object NovelBadgeToast : BooleanKey("novelBadge", true)
	object SelectedNovelCardType : IntKey("novelCardType", 0)
	object NavStyle : IntKey("navigationStyle", 0)

	// Backup Options
	object RestorePrintChapters : BooleanKey("backupPrintChapters", false)

	/**
	 * If true, backup only contains chapters that have been modified, else they are ignored
	 */
	object BackupOnlyModifiedChapters : BooleanKey("backup_only_modified", true)
	object ShouldBackupChapters : BooleanKey("backupChapters", true)
	object ShouldBackupSettings : BooleanKey("backupSettings", false)
	object BackupCycle : IntKey("backupCycle", 12)

	object BackupOnLowStorage : BooleanKey("backupLowStorage", true)
	object BackupOnLowBattery : BooleanKey("backupLowBattery", true)
	object BackupOnlyWhenIdle : BooleanKey("backupIdle", false)

	object ExposeTrueChapterDelete : BooleanKey("expose_true_chapter_delete", false)

	object LogToFile : BooleanKey("log_to_file", false)

	// Download Options
	object CustomExportDirectory : StringKey("downloadDirectory", "")

	/** How many threads to download at the same time */
	object DownloadThreadPool : IntKey("downloadThreads", 1)

	/** How many extension threads allowed to work in the pool */
	object DownloadExtThreads : IntKey("downloadExtThreads", 1)

	object DownloadNotifyChapters : BooleanKey("download_notify_chapters", false)

	/** If the reader can mark a read chapter as reading when its opened / scrolled */
	object ReaderMarkReadAsReading : BooleanKey(
		"readerMarkReadAsReading",
		false
	)


	// Advanced settings
	object ACRAEnabled : BooleanKey("is_ACRA_enabled", false)

	/**
	 * Automatically bookmark a novel when scanned via a QR code
	 */
	object AutoBookmarkFromQR : BooleanKey("bookmark_from_qr2", true)

	object AppTheme : IntKey("selectedAppTheme", 0)

	/** Verify if the check sum of the extension matches or not */
	object VerifyCheckSum : BooleanKey("verifyCheckSum", false)

	object LibraryFilter :
		StringKey("libraryFilter", Json.encodeToString(LibrarySortFilterEntity()))

	object RequireDoubleBackToExit : BooleanKey("requireDoubleBackToExit", false)

	class CustomString(
		name: String,
		default: String
	) : StringKey("string_$name", default)

	class CustomInt(
		name: String,
		default: Int
	) : IntKey("int_$name", default)

	class CustomBoolean(
		name: String,
		default: Boolean
	) : BooleanKey("boolean_$name", default)

	class CustomLong(
		name: String,
		default: Long
	) : SettingKey<Long>("long_$name", default)

	class CustomFloat(
		name: String,
		default: Float
	) : FloatKey("float_$name", default)

	class CustomStringSet(
		name: String,
		default: Set<String>
	) : StringSetKey("stringSet_$name", default)

	companion object {
		private val map: Map<String, SettingKey<*>> by lazy {
			SettingKey::class.sealedSubclasses
				.map { it.objectInstance }
				.filterIsInstance<SettingKey<*>>()
				.associateBy { it.name }
		}

		fun valueOf(key: String): SettingKey<*>? = map[key]
	}
}