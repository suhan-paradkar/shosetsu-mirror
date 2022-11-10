package app.shosetsu.android.view.compose.setting

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.shosetsu.android.common.SettingKey
import app.shosetsu.android.common.ext.launchIO
import app.shosetsu.android.domain.repository.base.ISettingsRepository
import app.shosetsu.android.view.uimodels.StableHolder
import com.chargemap.compose.numberpicker.NumberPicker

@Composable
fun NumberPickerSettingContent(
	title: String,
	description: String,
	range: StableHolder<IntRange>,
	repo: ISettingsRepository,
	key: SettingKey<Int>,
	modifier: Modifier = Modifier,
) {
	val selection by repo.getIntFlow(key).collectAsState()

	NumberPickerSettingContent(
		title, description, selection, range, modifier
	) {
		launchIO { repo.setInt(key, it) }
	}
}

@Composable
fun NumberPickerSettingContent(
	title: String,
	description: String,
	value: Int,
	range: StableHolder<IntRange>,
	modifier: Modifier = Modifier,
	onValueChanged: (newValue: Int) -> Unit
) {
	var openDialog by remember { mutableStateOf(false) }

	GenericRightSettingLayout(
		title,
		description,
		modifier,
		onClick = { openDialog = !openDialog }) {
		IconButton({
			openDialog = true
		}) {
			Text("$value", color = MaterialTheme.colorScheme.secondary)
		}
	}

	if (openDialog)
		Dialog({ openDialog = false }) {
			NumberPickerSettingDialogContent(
				title,
				value,
				range,
				onValueChanged
			)
		}

}

@Composable
fun NumberPickerSettingDialogContent(
	title: String,
	value: Int,
	range: StableHolder<IntRange>,
	onValueChanged: (newValue: Int) -> Unit,
) {
	Card(
		modifier = Modifier.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
	) {
		Column(
			modifier = Modifier
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				title,
				modifier = Modifier.padding(bottom = 8.dp),
				textAlign = TextAlign.Center
			)
			NumberPicker(
				value = value,
				onValueChange = onValueChanged,
				range = range.item,
				dividersColor = MaterialTheme.colorScheme.secondary,
			)
		}
	}
}


@Preview
@Composable
fun PreviewNumberPickerSettingDialogContent() {
	Box(modifier = Modifier.size(300.dp, 500.dp)) {
		NumberPickerSettingDialogContent(
			value = 5,
			range = remember { StableHolder(0..10) },
			title = "Test Dialog",
		) {
		}
	}
}

@Preview
@Composable
fun PreviewPickerSettingContent() {
	NumberPickerSettingContent(
		"A Number picker",
		"This is a number picker",
		2,
		range = remember { StableHolder(0..10) }
	) {

	}
}