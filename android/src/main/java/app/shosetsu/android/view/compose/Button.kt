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
 *
 */

package app.shosetsu.android.view.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun TextButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	elevation: ButtonElevation? = null,
	shape: Shape = MaterialTheme.shapes.small,
	border: BorderStroke? = null,
	colors: ButtonColors = ButtonDefaults.textButtonColors(),
	contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
	content: @Composable RowScope.() -> Unit,
) =
	Button(
		onClick = onClick,
		modifier = modifier,
		onLongClick = onLongClick,
		enabled = enabled,
		interactionSource = interactionSource,
		elevation = elevation,
		shape = shape,
		border = border,
		colors = colors,
		contentPadding = contentPadding,
		content = content,
	)

@Composable
fun Button(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
	shape: Shape = MaterialTheme.shapes.small,
	border: BorderStroke? = null,
	colors: ButtonColors = ButtonDefaults.buttonColors(),
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
	content: @Composable RowScope.() -> Unit
) {
	Surface(
		onClick = onClick,
		onLongClick = onLongClick,
		modifier = modifier,
		enabled = enabled,
		shape = shape,
		border = border,
		interactionSource = interactionSource,
	) {
		ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
			Row(
				Modifier
					.defaultMinSize(
						minWidth = ButtonDefaults.MinWidth,
						minHeight = ButtonDefaults.MinHeight,
					)
					.padding(contentPadding),
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically,
				content = content,
			)
		}
	}
}
