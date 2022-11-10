package app.shosetsu.android.ui.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.shosetsu.android.R
import app.shosetsu.android.view.compose.ShosetsuCompose
import app.shosetsu.android.viewmodel.abstracted.ABrowseViewModel
import app.shosetsu.android.viewmodel.abstracted.ABrowseViewModel.FilteredLanguages
import app.shosetsu.android.viewmodel.abstracted.ABrowseViewModel.LanguageFilter
import kotlinx.collections.immutable.*

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
 * Shosetsu
 *
 * @since 30 / 07 / 2021
 * @author Doomsdayrs
 */


@Composable
fun BrowseControllerFilterMenu(viewModel: ABrowseViewModel) {
	val showOnlyInstalled by viewModel.onlyInstalledLive.collectAsState()
	val languageList: FilteredLanguages by viewModel.filteredLanguagesLive.collectAsState()

	var hideLanguageFilter by remember { mutableStateOf(false) }

	val searchTerm by viewModel.searchTermLive.collectAsState()

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(vertical = 16.dp)
			.verticalScroll(rememberScrollState())
	) {
		BrowseControllerNameFilter(searchTerm, viewModel::setSearch)

		BrowseControllerLanguagesFilter(languageList, hideLanguageFilter,
			setLanguageFilterState = { l, s ->
				viewModel.setLanguageFiltered(l, s)
			},
			setHidden = {
				hideLanguageFilter = it
			}
		)

		Divider(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))

		BrowseControllerInstalledFilter(
			state = showOnlyInstalled,
			updateState = { viewModel.showOnlyInstalled(it) })
	}
}

@Preview
@Composable
fun PreviewBrowseControllerNameFilter() {
	BrowseControllerNameFilter("") {}
}

@Composable
fun BrowseControllerNameFilter(searchTerm: String, setSearchTerm: (newTerm: String) -> Unit) {
	OutlinedTextField(
		value = searchTerm,
		onValueChange = setSearchTerm,
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
		label = {
			Text(stringResource(R.string.controller_browse_filter_name_label))
		}
	)
}

@Preview
@Composable
fun PreviewBrowseControllerLanguagesFilter() {
	BrowseControllerLanguagesFilter(
		FilteredLanguages(listOf(LanguageFilter("en")).toImmutableList(), mapOf("en" to true).toImmutableMap()),
		false,
		{ _, _ -> },
		{}
	)
}

@Composable
fun BrowseControllerLanguagesFilter(
	languageList: FilteredLanguages,
	hidden: Boolean,
	setLanguageFilterState: (language: String, newState: Boolean) -> Unit,
	setHidden: (newValue: Boolean) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 8.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clickable(onClick = { setHidden(!hidden) }),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = stringResource(R.string.languages),
				Modifier.padding(start = 16.dp, bottom = 8.dp)
			)

			IconToggleButton(
				onCheckedChange = {
					setHidden(it)
				},
				checked = hidden,
			) {
				if (hidden)
					Icon(painterResource(R.drawable.expand_more), "")
				else
					Icon(painterResource(R.drawable.expand_less), "")
			}
		}

		AnimatedVisibility(!hidden) {
			languageList.let { (languages, state) ->
				Divider(modifier = Modifier.padding(bottom = 8.dp, end = 8.dp, start = 8.dp))

				BrowseControllerLanguagesContent(
					languages = languages,
					state = state,
					onLanguageChecked = { language, newState ->
						setLanguageFilterState(language, newState)
					}
				)
			}
		}
	}
}

@Preview
@Composable
fun PreviewBrowseControllerLanguages() {
	ShosetsuCompose {
		BrowseControllerLanguagesContent(
			languages = listOf("en", "ch", "ru", "fr").map(::LanguageFilter).toImmutableList(),
			state = persistentMapOf("en" to false, "ch" to false, "ru" to true, "fr" to false),
			onLanguageChecked = { _, _ -> })
	}
}

@Composable
fun BrowseControllerLanguagesContent(
	languages: ImmutableList<LanguageFilter>,
	state: ImmutableMap<String, Boolean>,
	onLanguageChecked: (String, Boolean) -> Unit
) {
	Column(
		Modifier
			.fillMaxWidth()
			.padding(top = 8.dp, bottom = 8.dp)
	) {
		languages.forEach { language ->
			BrowseControllerLanguageItem(language, state[language.lang] ?: false, onLanguageChecked)
		}
	}
}


@Preview
@Composable
fun PreviewBrowseControllerLanguageItem() {
	BrowseControllerLanguageItem(LanguageFilter("en"), false) { _, _ -> }
}


@Composable
fun BrowseControllerLanguageItem(
	language: LanguageFilter,
	state: Boolean,
	onLanguageChecked: (String, Boolean) -> Unit
) {
	Row(
		horizontalArrangement = Arrangement.SpaceBetween,
		modifier = Modifier
			.fillMaxWidth()
			.height(56.dp)
			.clickable(onClick = { onLanguageChecked(language.lang, !state) })
			.padding(horizontal = 16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = language.displayLang,
		)
		Checkbox(
			checked = state,
			onCheckedChange = null,
			modifier = Modifier

				.padding(bottom = 8.dp, end = 4.dp)
		)
	}

}

@Preview
@Composable
fun PreviewBrowseControllerInstalledFilter() {
	BrowseControllerInstalledFilter(false) {}
}

@Composable
fun BrowseControllerInstalledFilter(state: Boolean, updateState: (Boolean) -> Unit) {
	Row(
		horizontalArrangement = Arrangement.SpaceBetween,
		modifier = Modifier
			.fillMaxWidth()
			.height(56.dp)
			.clickable(onClick = { updateState(!state) })
			.padding(horizontal = 16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = stringResource(R.string.controller_browse_filter_only_installed),
		)
		Checkbox(
			checked = state,
			onCheckedChange = null,
			modifier = Modifier
				.padding(end = 4.dp)
		)
	}
}