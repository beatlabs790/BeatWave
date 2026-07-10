package iad1tya.echo.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.decodeFromString
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FundingData(
    val target: Double,
    val raised: Double,
    val percentage: Int,
    val last_updated: String
)

object FundingRepository {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val _fundingState = MutableStateFlow<FundingData?>(null)
    val fundingState: StateFlow<FundingData?> = _fundingState

    suspend fun fetchFundingProgress() {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://raw.githubusercontent.com/EchoMusicApp/Echo-Music/funding-data/funding.json"
                val jsonString = client.get(url).bodyAsText()
                val jsonParser = Json { ignoreUnknownKeys = true }
                val data = jsonParser.decodeFromString<FundingData>(jsonString)
                _fundingState.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
