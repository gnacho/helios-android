package club.cloudless.helios.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Result of a call, with the error already turned into something showable. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Error(val message: String, val unauthorized: Boolean = false) : ApiResult<Nothing>
}

/**
 * Minimal Helios client: two GETs and a login probe. The token is sent as
 * `Authorization: Bearer`, which the server accepts next to the browser cookie
 * (see `internal/auth/tokens.go` in the Helios repo).
 */
class HeliosApi(
    private val baseUrl: String,
    private val token: String,
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun live(): ApiResult<LiveDto> = get("/api/solar/live", LiveDto.serializer())

    fun extensions(): ApiResult<ExtensionsDto> = get("/api/extensions", ExtensionsDto.serializer())

    /** Helios 1.2.11 and later. An older server answers 404 and the widget degrades. */
    fun chargerState(): ApiResult<ChargerStateDto> =
        get("/api/charger/state", ChargerStateDto.serializer())

    /** `/api/version` is public: it proves the URL points at a Helios server. */
    fun version(): ApiResult<String> {
        val req = Request.Builder().url("$baseUrl/api/version").get().build()
        return run(req) { body -> json.parseToJsonElement(body).toString() }
    }

    private fun <T> get(path: String, serializer: kotlinx.serialization.DeserializationStrategy<T>): ApiResult<T> {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return run(req) { body -> json.decodeFromString(serializer, body) }
    }

    private fun <T> run(req: Request, parse: (String) -> T): ApiResult<T> = try {
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            when {
                resp.code == 401 || resp.code == 403 ->
                    ApiResult.Error("El token no vale o no tiene permiso", unauthorized = true)
                !resp.isSuccessful ->
                    ApiResult.Error("El servidor respondió ${resp.code}")
                else -> ApiResult.Ok(parse(body))
            }
        }
    } catch (e: IOException) {
        // El tipo de excepción es la diferencia entre "no hay red", "el DNS no
        // resuelve" y "el certificado no vale": sin él, depurar es adivinar.
        ApiResult.Error("No se pudo conectar con el servidor (" + (e::class.simpleName ?: "error") + ")")
    } catch (e: Exception) {
        ApiResult.Error("Respuesta no válida del servidor (" + (e::class.simpleName ?: "error") + ")")
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        /** Accepts `helios.cloudless.club`, `https://…` or a LAN address with port. */
        fun normalizeBaseUrl(raw: String): String {
            var s = raw.trim().trimEnd('/')
            if (s.isEmpty()) return ""
            if (!s.startsWith("http://") && !s.startsWith("https://")) {
                s = if (s.startsWith("192.168.") || s.startsWith("10.") || s.startsWith("127.") || s.contains(":") && !s.contains(".")) {
                    "http://$s"
                } else {
                    "https://$s"
                }
            }
            return s
        }
    }
}
