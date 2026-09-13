package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.*
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.client.*

object SaavnDependencies {
    val api by lazy { JioSaavnApi() }
    val parser by lazy { JioSaavnParser() }
}

class SaavnExtension : ExtensionClient,
    QuickSearchClient by QuickSearchClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    HomeFeedClient by HomeFeedClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    LibraryFeedClient by LibraryFeedClientImpl(),
    TrackClient by TrackClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    AlbumClient by AlbumClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    ArtistClient by ArtistClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    PlaylistClient by PlaylistClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    RadioClient by RadioClientImpl(SaavnDependencies.api, SaavnDependencies.parser),
    ShareClient by ShareClientImpl() {

    private lateinit var settings: Settings

    override suspend fun getSettingItems(): List<Setting> = emptyList()

    override fun setSettings(settings: Settings) {
        this.settings = settings
    }
}
