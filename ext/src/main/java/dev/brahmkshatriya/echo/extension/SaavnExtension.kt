package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.*
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.settings.*
import dev.brahmkshatriya.echo.extension.client.*

object SaavnDependencies {
    val api by lazy { JioSaavnApi() }
    val parser by lazy { JioSaavnParser() }

    var settings: Settings? = null

    fun getDefaultLanguages(): List<String> {
        val value = settings?.getStringSet("default_home_languages")
        return value?.toList() ?: listOf("hindi")
    }
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

    override suspend fun getSettingItems(): List<Setting> {
        return listOf(
            SettingMultipleChoice(
                title = "Default Home Languages",
                key = "default_home_languages",
                summary = "Languages for the Default tab",
                entryTitles = listOf(
                    "Hindi", "English", "Punjabi", "Tamil", "Telugu",
                    "Marathi", "Gujarati", "Bengali", "Kannada",
                    "Bhojpuri", "Malayalam", "Urdu"
                ),
                entryValues = listOf(
                    "hindi", "english", "punjabi", "tamil", "telugu",
                    "marathi", "gujarati", "bengali", "kannada",
                    "bhojpuri", "malayalam", "urdu"
                ),
                defaultEntryIndices = setOf(0)  // Hindi
            )
        )
    }

    override fun setSettings(settings: Settings) {
        SaavnDependencies.settings = settings
    }

}
