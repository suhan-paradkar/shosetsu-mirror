package app.shosetsu.android.ui.reader.page

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import app.shosetsu.android.common.ext.launchUI
import app.shosetsu.android.view.compose.ScrollStateBar
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
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

@Composable
fun WebViewPageContent(
	html: String,
	progress: Double,
	onScroll: (perc: Double) -> Unit,
	onClick: () -> Unit,
	onDoubleClick: () -> Unit
) {
	val scrollState = rememberScrollState()
	val state = rememberWebViewStateWithHTMLData(html)
	var first by remember { mutableStateOf(true) }

	if (scrollState.isScrollInProgress)
		DisposableEffect(Unit) {
			onDispose {
				if (scrollState.value != 0)
					onScroll((scrollState.value.toDouble() / scrollState.maxValue))
				else onScroll(0.0)
			}
		}

	val backgroundColor = MaterialTheme.colorScheme.background
	ScrollStateBar(scrollState) {
		WebView(
			state,
			captureBackPresses = false,
			onCreated = { webView ->
				webView.setBackgroundColor(backgroundColor.toArgb())
				webView.settings.apply {
					@SuppressLint("SetJavaScriptEnabled")
					javaScriptEnabled = true
					blockNetworkLoads = false
					blockNetworkImage = false
					loadsImagesAutomatically = true
					allowFileAccess = true
					cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
				}

				val inter = ShosetsuScript(
					onClickMethod = onClick,
					onDClickMethod = onDoubleClick
				)

				webView.addJavascriptInterface(inter, "shosetsuScript")
				webView.isScrollContainer = false
			},
			modifier = Modifier
				.fillMaxWidth()
				.verticalScroll(scrollState),
			client = object : AccompanistWebViewClient() {
				override fun shouldOverrideUrlLoading(
					view: WebView?,
					request: WebResourceRequest?
				): Boolean = true

				override fun onPageFinished(view: WebView?, url: String?) {
					super.onPageFinished(view, url)
					view?.evaluateJavascript(
						"""
						window.addEventListener("click",(event)=>{ shosetsuScript.onClick(); });
						window.addEventListener("dblclick",(event)=>{ shosetsuScript.onDClick(); });
						""".trimIndent(), null
					)
					view?.layoutParams = ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					)
				}
			}
		)
	}

	// Avoid scrolling when the state has not fully loaded
	if (scrollState.maxValue != 0 && scrollState.maxValue != Int.MAX_VALUE && !state.isLoading) {
		if (first) {
			LaunchedEffect(progress) {
				launch {
					val result = (scrollState.maxValue * progress).toInt()
					scrollState.scrollTo(result)
					first = false
				}
			}
		}
	}
}

class ShosetsuScript(
	val onClickMethod: () -> Unit,
	val onDClickMethod: () -> Unit
) {
	@Suppress("unused")
	@JavascriptInterface
	fun onClick() {
		launchUI {
			onClickMethod()
		}
	}

	@Suppress("unused")
	@JavascriptInterface
	fun onDClick() {
		launchUI {
			onDClickMethod()
		}
	}
}