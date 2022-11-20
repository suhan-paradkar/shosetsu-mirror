package app.shosetsu.android.view.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.shosetsu.android.R
import app.shosetsu.android.view.uimodels.StableHolder
import kotlin.math.roundToInt

@Preview
@Composable
fun PreviewSeekBar() {
	var value by remember { mutableStateOf(1) }
	ShosetsuCompose {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp)
		) {
			DiscreteSlider(
				value,
				"${value}h",
				{ it, _ ->
					value = it
				},
				StableHolder(0..10),
			)
		}
	}
}


/**
 * This creates a sudo discrete slider
 *
 * @param value Value to set [Slider] to
 * @param parsedValue [value] parsed to be displayed as a string
 * @param updateValue Called when [Slider] updates its value, is fed a rounded float
 * @param valueRange An integer range of possible values
 */
@Composable
fun DiscreteSlider(
	value: Int,
	parsedValue: String,
	updateValue: (Int, fromDialog: Boolean) -> Unit,
	valueRange: StableHolder<IntRange>,
	haveSteps: Boolean = true,
	maxHeaderSize: Dp? = null,
) {
	Row(
		verticalAlignment = Alignment.CenterVertically
	) {
		var showDialog by remember { mutableStateOf(false) }

		if (showDialog) {
			var newValue: Int? by remember { mutableStateOf(value) }

			Dialog(
				onDismissRequest = {
					showDialog = false
				}
			) {
				Card {
					Column(
						modifier = Modifier.padding(8.dp),
					) {
						var isTextError by remember { mutableStateOf(false) }

						Text(
							stringResource(R.string.input_float),
							style = MaterialTheme.typography.titleLarge,
							modifier = Modifier.padding(
								bottom = 16.dp,
								top = 8.dp,
								start = 24.dp,
								end = 24.dp
							)
						)

						Text(
							stringResource(
								R.string.input_int_range_desc,
								valueRange.item.first,
								valueRange.item.last
							),
							style = MaterialTheme.typography.body1,
							modifier = Modifier.padding(
								bottom = 16.dp,
								start = 24.dp,
								end = 24.dp
							)
						)

						TextField(
							value = if (newValue != null) "$newValue" else "",
							onValueChange = {
								val value = it.toIntOrNull()

								if (value != null) {
									if (value in valueRange.item) {
										newValue = value
										isTextError = false
										return@TextField
									}
								} else if (it.isEmpty()) {
									newValue = null
								}

								isTextError = true
							},
							singleLine = true,
							keyboardOptions = KeyboardOptions(
								keyboardType = KeyboardType.Number
							),
							modifier = Modifier
								.padding(bottom = 8.dp, start = 24.dp, end = 24.dp)
								.fillMaxWidth()
						)

						Row(
							horizontalArrangement = Arrangement.End,
							modifier = Modifier.fillMaxWidth()

						) {
							TextButton(
								onClick = {
									showDialog = false
								},
							) {
								Text(stringResource(android.R.string.cancel))
							}

							TextButton(
								onClick = {
									updateValue(newValue!!, true)
									showDialog = false
								},
								enabled = !isTextError
							) {
								Text(stringResource(R.string.apply))
							}
						}

					}
				}
			}
		}

		TextButton(onClick = {
			showDialog = true
		}) {
			Text(
				text = parsedValue,
				modifier = Modifier.let {
					if (maxHeaderSize != null)
						it.width(maxHeaderSize)
					else it
				}
			)
		}
		Slider(
			value.toFloat(),
			{
				updateValue(it.roundToInt(), false)
			},
			valueRange = valueRange.item.first.toFloat()..valueRange.item.last.toFloat(),
			steps = if (haveSteps) valueRange.item.count() - 2 else 0
		)
	}
}

/**
 * This creates a sudo discrete slider
 *
 * @param value Value to set [Slider] to
 * @param parsedValue [value] parsed to be displayed as a string
 * @param updateValue Called when [Slider] updates its value
 * @param valueRange An integer range of possible values
 */
@Composable
fun DiscreteSlider(
	value: Float,
	parsedValue: String,
	updateValue: (Float, fromDialog: Boolean) -> Unit,
	valueRange: StableHolder<IntRange>,
	haveSteps: Boolean = true,
	maxHeaderSize: Dp? = null
) {
	Row(
		verticalAlignment = Alignment.CenterVertically
	) {
		var showDialog by remember { mutableStateOf(false) }

		if (showDialog) {
			var fieldContent: String by remember { mutableStateOf("$value") }

			Dialog(
				onDismissRequest = {
					showDialog = false
				}
			) {
				Card {
					Column(
						modifier = Modifier.padding(8.dp),
					) {
						var isTextError by remember { mutableStateOf(false) }

						Text(
							stringResource(R.string.input_float),
							style = MaterialTheme.typography.titleLarge,
							modifier = Modifier.padding(
								bottom = 16.dp,
								top = 8.dp,
								start = 24.dp,
								end = 24.dp
							)
						)

						Text(
							stringResource(
								R.string.input_float_range_desc,
								valueRange.item.first,
								valueRange.item.last
							),
							style = MaterialTheme.typography.body1,
							modifier = Modifier.padding(
								bottom = 16.dp,
								start = 24.dp,
								end = 24.dp
							)
						)

						TextField(
							value = fieldContent,
							onValueChange = { newString ->
								val newFloat = newString.toFloatOrNull()

								// Set text as error if the value is invalid
								isTextError = if (newFloat != null) {
									!(valueRange.item.first <= newFloat || newFloat <= valueRange.item.last)
								} else {
									true
								}

								fieldContent = newString
							},
							singleLine = true,
							keyboardOptions = KeyboardOptions(
								keyboardType = KeyboardType.Number
							),
							modifier = Modifier
								.padding(bottom = 8.dp, start = 24.dp, end = 24.dp)
								.fillMaxWidth()
						)

						Row(
							horizontalArrangement = Arrangement.End,
							modifier = Modifier.fillMaxWidth()

						) {
							TextButton(
								onClick = {
									showDialog = false
								},
							) {
								Text(stringResource(android.R.string.cancel))
							}

							TextButton(
								onClick = {
									updateValue(
										fieldContent.toFloatOrNull() ?: value,
										true
									)
									showDialog = false
								},
								enabled = !isTextError
							) {
								Text(stringResource(R.string.apply))
							}
						}

					}
				}
			}
		}

		TextButton(onClick = {
			showDialog = true
		}) {
			Text(
				text = parsedValue,
				modifier = Modifier.let {
					if (maxHeaderSize != null)
						it.width(maxHeaderSize)
					else it
				}
			)
		}
		Slider(
			value,
			{
				updateValue(it, false)
			},
			valueRange = valueRange.item.first.toFloat()..valueRange.item.last.toFloat(),
			steps = if (haveSteps) valueRange.item.count() - 2 else 0
		)
	}
}