package app.shosetsu.android.ui.settings.sub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import app.shosetsu.android.common.enums.TextAsset
import app.shosetsu.android.common.ext.viewModel
import app.shosetsu.android.view.compose.ShosetsuCompose
import app.shosetsu.android.view.controller.ShosetsuController
import app.shosetsu.android.viewmodel.abstracted.ATextAssetReaderViewModel

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
 */
class TextAssetReader : ShosetsuController() {

	private val viewModel: ATextAssetReaderViewModel by viewModel()


	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedViewState: Bundle?
	): View = ComposeView(requireContext()).apply {
		setContent {
			val content by viewModel.liveData.collectAsState()
			ShosetsuCompose {
				TextAssetReaderContent(content)
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		viewModel.setTarget(
			arguments!!.getInt(
				BUNDLE_KEY,
				TextAsset.LICENSE.bundle.getInt(BUNDLE_KEY)
			)
		)

		viewModel.targetLiveData.observe(catch = {}) {
			if (it != null)
				setViewTitle(getString(it.titleRes))
		}
	}

	companion object {
		const val BUNDLE_KEY: String = "target"
		val TextAsset.bundle: Bundle
			get() = bundleOf(BUNDLE_KEY to ordinal)
	}
}

@Composable
fun TextAssetReaderContent(text: String?) {
	if (text != null) {
		Box(
			modifier = Modifier
				.verticalScroll(
					state = rememberScrollState(),
				)
				.fillMaxSize()
		) {
			Text(text = text, modifier = Modifier.padding(16.dp))
		}
	}
}