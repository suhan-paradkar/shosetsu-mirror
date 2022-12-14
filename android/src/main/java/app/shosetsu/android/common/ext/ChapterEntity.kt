package app.shosetsu.android.common.ext

import app.shosetsu.android.domain.model.database.DBChapterEntity
import app.shosetsu.android.domain.model.local.ChapterEntity

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
 * shosetsu
 * 05 / 12 / 2020
 */

fun ChapterEntity.toDB(): DBChapterEntity = DBChapterEntity(
	id,
	url,
	novelID,
	extensionID,
	title,
	releaseDate,
	order,
	readingPosition,
	readingStatus,
	bookmarked,
	isSaved
)

fun List<ChapterEntity>.toDB() = map { it.toDB() }