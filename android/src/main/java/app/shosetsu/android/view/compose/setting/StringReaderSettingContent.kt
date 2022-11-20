package app.shosetsu.android.view.compose.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shosetsu.android.common.SettingKey
import app.shosetsu.android.common.ext.launchIO
import app.shosetsu.android.domain.repository.base.ISettingsRepository

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringSettingContent(
	title: String,
	description: String,
	value: String,
	modifier: Modifier = Modifier,
	onValueChanged: (newString: String) -> Unit
) {
	Column(
		modifier = modifier,
	) {
		OutlinedTextField(
			value = value,
			onValueChange = onValueChanged,
			label = { Text(title) },
			modifier = Modifier.fillMaxWidth()
		)
		Text(description)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringSettingContent(
	title: String,
	description: String,
	repo: ISettingsRepository,
	key: SettingKey<String>,
	modifier: Modifier = Modifier,
) {
	val value by repo.getStringFlow(key).collectAsState()

	Column {
		TextField(
			value = value,
			onValueChange = {
				launchIO { repo.setString(key, it) }
			},
			modifier = modifier,
			label = { Text(title) }
		)
		Text(description)
	}
}

@Preview
@Composable
fun PreviewStringSettingContent() {
	StringSettingContent("Text Input", "This is a text input", "") {

	}
}
