package app.shosetsu.android.datasource.remote.impl.update

import android.annotation.SuppressLint
import app.shosetsu.android.datasource.remote.base.IRemoteAppUpdateDataSource
import app.shosetsu.android.domain.model.local.AppUpdateEntity
import java.io.InputStream

class UpToDownAppUpdateDataSource : IRemoteAppUpdateDataSource,
	IRemoteAppUpdateDataSource.Downloadable {
	@SuppressLint("StopShip")
	override suspend fun loadAppUpdate(): AppUpdateEntity {
		@Suppress("TodoComment")
		TODO("Add up-to-down update source")
	}

	@SuppressLint("StopShip")
	override suspend fun downloadAppUpdate(update: AppUpdateEntity): InputStream {
		@Suppress("TodoComment")
		TODO("Add up-to-down update source")
	}
}