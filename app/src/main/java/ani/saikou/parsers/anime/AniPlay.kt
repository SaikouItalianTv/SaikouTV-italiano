package ani.saikou.parsers.anime

import ani.saikou.client
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ani.saikou.media.Media
import org.jsoup.nodes.Document

class AniPlay : AnimeParser() {

    override val name = "AniPlay"
    override val saveName = "aniplay_to"
    override val hostUrl = "https://aniplay.it"
    override val isDubAvailableSeparately = true
    override val allowsPreloading = false

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val response = client.get(animeLink).parsed<ApiAnime>()
        val episodes =  if (response.seasons.isNullOrEmpty()) response.episodes.mapNotNull { it.toEpisode() } else response.seasons.map{ it.toEpisodeList(animeLink) }.flatten()
        (episodes as MutableList).sortBy { it.number.toInt() }
        return episodes
    }

    private fun ApiEpisode.toEpisode() : Episode? {
        val number = this.number.toIntOrNull() ?: return null
        return Episode(
            link = "$hostUrl/api/episode/${this.id}",
            number = number.toString(),
            title = this.title
        )
    }
    private suspend fun ApiSeason.toEpisodeList(url: String) : List<Episode> {
        return Json{ignoreUnknownKeys = true }.decodeFromString<List<ApiEpisode>>(client.get("$url/season/${this.id}").text).mapNotNull { it.toEpisode() }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val link = client.get(episodeLink).parsed<ApiEpisodeUrl>().url
        return  listOf(VideoServer("AniPlay", link))

    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = AniPlayExtractor(server)
    class AniPlayExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            val type = if (server.embed.url.contains("m3u8")) VideoType.M3U8 else VideoType.CONTAINER
            return VideoContainer(
                listOf(
                    Video(null, type, server.embed)
                )
            )

        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = query.replace(" ", "+")
        val jsonstring = client.get("$hostUrl/api/anime/advanced-search?page=0&size=36&query=$encoded").text
        val response = Json{ ignoreUnknownKeys = true }.decodeFromString<List<ApiSearchResult>>(jsonstring)
        return response.filter { (it.title.contains("(ITA)") === selectDub) }.map {
            val title = it.title
            val link = "$hostUrl/api/anime/${it.id}"
            val cover = it.posters.first().posterUrl
            ShowResponse(title, link, cover)
        }
    }
    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        val response = search(mediaObj.nameRomaji) + search(mediaObj.name!!)
        if (response.isNotEmpty()) {
            val media = response.first {
                getID(client.get(it.link).parsed<ApiAnime>()) == mediaObj.id
            }
            saveShowResponse(mediaObj.id, media, true)
        }
        return loadSavedShowResponse(mediaObj.id)
    }

    private fun getID(response: ApiAnime): Int?{
        return response.websites.find { it.websiteId == 4 }?.url?.removePrefix("https://anilist.co/anime/")?.split("/")?.first()?.toIntOrNull()
    }

    @Serializable
    data class ApiWebsite(
        @SerialName("listWebsiteId") val websiteId: Int,
        @SerialName("url") val url: String
    )

    @Serializable
    data class ApiSearchResult(
        @SerialName("id") val id: Int,
        @SerialName("title") val title: String,
        @SerialName("verticalImages") val posters: List<ApiPoster>
    )
    @Serializable
    data class ApiPoster(
        @SerialName("imageFull") val posterUrl: String
    )
    @Serializable
    data class ApiAnime(
        @SerialName("title") val title: String,
        @SerialName("episodes") val episodes: List<ApiEpisode>,
        @SerialName("seasons") val seasons: List<ApiSeason>?,
        @SerialName("listWebsites") val websites: List<ApiWebsite>,
    )
    @Serializable
    data class ApiEpisode(
        @SerialName("id") val id: Int,
        @SerialName("title") val title: String?,
        @SerialName("episodeNumber") val number: String,
    )
    @Serializable
    data class ApiSeason(
        @SerialName("id") val id: Int,
        @SerialName("name") val name: String
    )
    @Serializable
    data class ApiEpisodeUrl(
        @SerialName("videoUrl") val url: String
    )
}