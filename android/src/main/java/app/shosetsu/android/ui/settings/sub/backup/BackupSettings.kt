package app.shosetsu.android.ui.settings.sub.backup

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.shosetsu.android.common.ext.context
import app.shosetsu.android.common.ext.logV
import app.shosetsu.android.common.ext.toast
import app.shosetsu.android.common.ext.viewModel
import app.shosetsu.android.ui.settings.SettingsSubController
import app.shosetsu.android.view.uimodels.settings.ButtonSettingData
import app.shosetsu.android.view.uimodels.settings.base.SettingsItemData
import app.shosetsu.android.view.uimodels.settings.dsl.onButtonClicked
import app.shosetsu.android.viewmodel.abstracted.settings.ABackupSettingsViewModel
import app.shosetsu.common.consts.BACKUP_FILE_EXTENSION
import com.github.doomsdayrs.apps.shosetsu.R

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
 * 13 / 07 / 2019
 */
class BackupSettings : SettingsSubController() {
	override val viewTitleRes: Int = R.string.settings_backup

	override val viewModel: ABackupSettingsViewModel by viewModel()

	override val adjustments: List<SettingsItemData>.() -> Unit = {
		find<ButtonSettingData>(3)?.onButtonClicked {
			// Stops novel updates while backup is taking place
			// Starts backing up data
			viewModel.startBackup()
		}
		find<ButtonSettingData>(4)?.onButtonClicked {
			AlertDialog.Builder(recyclerView.context!!).apply {
				setTitle(R.string.settings_backup_alert_select_location_title)
				setItems(R.array.settings_backup_alert_location_array) { d, i ->
					when (i) {
						0 -> {
							// Open file selector
							performFileSelection()
							d.dismiss()
						}
						1 -> {
							// Open internal list
							viewModel.loadInternalOptions().handleObserve { list ->
								d.dismiss()
								AlertDialog.Builder(recyclerView.context!!).apply {
									setTitle(R.string.settings_backup_alert_internal_title)
									setItems(list.map {
										it
											.removePrefix("shosetsu-backup-")
											.removeSuffix(".$BACKUP_FILE_EXTENSION")
										// TODO Map dates with proper localization
									}.toTypedArray()) { d, w ->
										viewModel.restore(list[w])
										d.dismiss()
									}
									setNegativeButton(android.R.string.cancel) { d, i ->
										d.cancel()
									}
								}.show()
							}
						}
					}
				}
			}.show()
		}
	}

	/*
	 * Options for restore
	 * > Internal
	 * >> Open pop up list to select file
	 * > Via external
	 * >> Open file explorer and select file
	 */


	inner class Observer(private val registry: ActivityResultRegistry) :
		DefaultLifecycleObserver {
		lateinit var launcher: ActivityResultLauncher<Array<String>>

		override fun onCreate(owner: LifecycleOwner) {
			launcher = registry.register(
				"backup_settings_rq#",
				owner,
				ActivityResultContracts.OpenDocument()
			) { uri ->
				if (MimeTypeMap.getFileExtensionFromUrl(uri.toString()) != BACKUP_FILE_EXTENSION) {
					logV("invalid type")
					context?.toast("Invalid file")
					return@register
				}

				context?.toast("Restoring now...")
				viewModel.restore(uri)
			}
		}
	}

	val observer by lazy { Observer((activity as AppCompatActivity).activityResultRegistry) }

	override fun onContextAvailable(context: Context) {
		lifecycle.addObserver(observer)
	}

	private fun performFileSelection() {
		context?.toast(
			"Please make sure this is on the main storage, " +
					"SD card storage is not functional yet"
		)
		observer.launcher.launch(arrayOf("application/octet-stream"))
	}


	companion object {
		private const val REQUEST_CODE_RESTORE_SELECTION = 2116
	}
}