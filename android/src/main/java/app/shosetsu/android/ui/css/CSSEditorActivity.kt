package app.shosetsu.android.ui.css

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

import android.annotation.SuppressLint
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Base64
import android.view.Window
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.getSystemService
import app.shosetsu.android.R
import app.shosetsu.android.common.ext.logI
import app.shosetsu.android.common.ext.openInWebView
import app.shosetsu.android.common.ext.viewModel
import app.shosetsu.android.view.compose.ShosetsuCompose
import app.shosetsu.android.viewmodel.abstracted.ACSSEditorViewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI

/**
 * Shosetsu
 * 21 / October / 2021
 *
 * @author github.com/doomsdayrs
 */
class CSSEditorActivity : AppCompatActivity(), DIAware {
	private val viewModel: ACSSEditorViewModel by viewModel()
	override val di: DI by closestDI()

	private val clipboardManager by lazy { getSystemService<ClipboardManager>()!! }

	companion object {
		const val CSS_ID = "css-id"
		private const val HELP_WEBSITE = "https://developer.mozilla.org/en-US/docs/Learn/CSS"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requestWindowFeature(Window.FEATURE_NO_TITLE)
		if (savedInstanceState != null)
			viewModel.setCSSId(savedInstanceState.getInt(CSS_ID, -1))


		setContent {
			val cssTitle by viewModel.cssTitle.collectAsState()
			val cssContent by viewModel.cssContent.collectAsState()


			val isCSSInvalid by viewModel.isCSSValid.collectAsState()
			val cssInvalidReason by viewModel.cssInvalidReason.collectAsState()

			val canRedo by viewModel.canRedo.collectAsState()
			val canUndo by viewModel.canUndo.collectAsState()

			ShosetsuCompose {
				CSSEditorContent(
					cssTitle = cssTitle,
					cssContent = cssContent,
					isCSSInvalid = isCSSInvalid,
					cssInvalidReason = cssInvalidReason,
					onUndo = { viewModel.undo() },
					onRedo = { viewModel.redo() },
					onBack = { onBackPressedDispatcher.onBackPressed() },
					onHelp = { openInWebView(HELP_WEBSITE) },
					onExport = {
						// TODO Add exporting
					},
					onNewText = viewModel::write,
					onPaste = {
						val text = clipboardManager.primaryClip?.getItemAt(0)?.text
						if (text == null) {
							// TODO Handle no paste content
						} else {
							viewModel.appendText(text.toString())
						}
					},
					hasPaste = when {
						!clipboardManager.hasPrimaryClip() -> {
							false
						}
						!(clipboardManager.primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) -> {
							// This disables the paste menu item, since the clipboard has data but it is not plain text
							false
						}
						else -> {
							// This enables the paste menu item, since the clipboard contains plain text.
							true
						}
					},
					canRedo = canRedo,
					canUndo = canUndo
				) {
					viewModel.saveCSS()
				}
			}
		}
	}
}

@Preview
@Composable
fun PreviewCSSEditorContent() {
	ShosetsuCompose {
		CSSEditorContent(
			"TestCSS",
			"",
			onBack = {},
			onNewText = {},
			onUndo = {},
			onRedo = {},
			isCSSInvalid = false,
			cssInvalidReason = "This is not CSS",
			onPaste = {},
			onExport = {},
			onHelp = {},
			canRedo = true,
			canUndo = true,
		) {}
	}
}

@Composable
fun CSSEditorContent(
	cssTitle: String,
	cssContent: String,
	isCSSInvalid: Boolean,
	cssInvalidReason: String? = null,
	onBack: () -> Unit,
	onHelp: () -> Unit,
	onNewText: (String) -> Unit,
	onUndo: () -> Unit,
	onRedo: () -> Unit,
	onPaste: () -> Unit,
	onExport: () -> Unit,
	hasPaste: Boolean = true,
	canUndo: Boolean,
	canRedo: Boolean,
	onSave: () -> Unit
) {
	val fabShape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50))

	Scaffold(
		topBar = {
			Column {
				TopAppBar {
					Row(
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth()
					) {
						Row(
							verticalAlignment = Alignment.CenterVertically
						) {
							IconButton(
								onClick = onBack
							) {
								Icon(
									Icons.Filled.ArrowBack,
									stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
								)
							}
							Text(cssTitle)
						}

						IconButton(
							onClick = onHelp
						) {
							Icon(
								painterResource(R.drawable.help_outline_24),
								stringResource(R.string.help),
							)
						}
					}
				}
			}
		},
		bottomBar = {
			Column {
				if (!isCSSInvalid && cssInvalidReason != null)
					Card(
						border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
						modifier = Modifier
							.align(Alignment.CenterHorizontally)
							.fillMaxWidth()
							.padding(16.dp)
					) {
						Column(
							modifier = Modifier.padding(8.dp)
						) {
							Text(
								"Invalid CSS"
							)
							Text(
								cssInvalidReason,
								fontSize = dimensionResource(R.dimen.sub_text_size).value.sp,
								modifier = Modifier
									.alpha(0.7f)
							)
						}
					}
				BottomAppBar(
					cutoutShape = fabShape
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
						modifier = Modifier.fillMaxWidth()
					) {
						Row(
							verticalAlignment = Alignment.CenterVertically,
						) {
							IconButton(onClick = onUndo, enabled = canUndo) {
								Icon(
									painterResource(R.drawable.ic_baseline_undo_24),
									stringResource(R.string.activity_css_undo)
								)
							}

							IconButton(
								onClick = onPaste, enabled = hasPaste,
								modifier = Modifier.padding(start = 8.dp)
							) {
								Icon(
									painterResource(androidx.appcompat.R.drawable.abc_ic_menu_paste_mtrl_am_alpha),
									stringResource(R.string.activity_css_paste)
								)
							}
						}

						Row(
							verticalAlignment = Alignment.CenterVertically,
						) {
							IconButton(
								onClick = onExport, enabled = false,
								modifier = Modifier.padding(end = 8.dp)
							) {
								Icon(
									painterResource(R.drawable.ic_baseline_save_alt_24),
									stringResource(R.string.activity_css_export)
								)
							}
							IconButton(onClick = onRedo, enabled = canRedo) {
								Icon(
									painterResource(R.drawable.ic_baseline_redo_24),
									stringResource(R.string.activity_css_redo)
								)
							}
						}
					}
				}
			}
		},
		floatingActionButton = {
			FloatingActionButton(
				onClick = onSave,
				shape = fabShape,
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = colorResource(android.R.color.white)
			) {
				Icon(
					painterResource(R.drawable.ic_baseline_save_24),
					stringResource(R.string.activity_css_save)
				)
			}
		},
		isFloatingActionButtonDocked = true,
		floatingActionButtonPosition = FabPosition.Center
	) {
		Column(Modifier.padding(it)) {
			AndroidView(
				factory = { context ->
					WebView(context).apply {
						@SuppressLint("SetJavaScriptEnabled")
						settings.javaScriptEnabled = true
						settings.apply {
							cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
						}
						loadData(
							context.getString(R.string.activity_css_example),
							"text/html",
							"UTF-8"
						)

					}
				},
				update = { webView ->
					logI("Updating style")
					val base64String =
						Base64.encodeToString(cssContent.encodeToByteArray(), Base64.DEFAULT)
					webView.evaluateJavascript(
						"""
				style = document.getElementById('example-css');
				if(!style){
					style = document.createElement('style');
					style.id = 'example-css';
					style.type = 'text/css';
					document.head.appendChild(style);
				}
				style.textContent = atob(`$base64String`);
			""".trimIndent(),
						null
					)
				},
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight(0.25f)
			)
			TextField(
				cssContent,
				onNewText,
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(bottom = 92.dp),
				shape = RectangleShape,
				isError = !isCSSInvalid
			)
		}

	}
}