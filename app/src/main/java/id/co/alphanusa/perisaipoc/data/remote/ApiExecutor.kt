package id.co.alphanusa.perisaipoc.data.remote

import com.google.gson.Gson
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.common.DispatcherProvider
import id.co.alphanusa.perisaipoc.data.remote.dto.ApiErrorDto
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menjalankan panggilan API di dispatcher IO dan menerjemahkan hasilnya ke
 * [AppResult]. Terpusat di sini supaya penanganan error tidak diulang di setiap
 * repository.
 */
@Singleton
class ApiExecutor @Inject constructor(
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {

    private companion object {
        const val MESSAGE_EMPTY_BODY = "Invalid response from server"
        const val MESSAGE_NETWORK = "Network error. Please check your connection."
        const val MESSAGE_UNEXPECTED = "An unexpected error occurred"
    }

    /** Untuk endpoint yang membungkus hasil dalam [Response]. */
    suspend fun <T : Any> execute(
        fallbackMessage: String,
        request: suspend () -> Response<T>,
    ): AppResult<T> = runCatchingNetwork {
        val response = request()
        val body = response.body()
        when {
            response.isSuccessful && body != null -> AppResult.success(body)
            response.isSuccessful -> AppResult.failure(MESSAGE_EMPTY_BODY)
            else -> AppResult.failure(parseError(response, fallbackMessage))
        }
    }

    /** Untuk endpoint yang langsung mengembalikan body (tanpa [Response]). */
    suspend fun <T : Any> executeDirect(request: suspend () -> T): AppResult<T> =
        runCatchingNetwork { AppResult.success(request()) }

    private suspend fun <T : Any> runCatchingNetwork(
        block: suspend () -> AppResult<T>,
    ): AppResult<T> = withContext(dispatchers.io) {
        try {
            block()
        } catch (e: IOException) {
            AppResult.failure(MESSAGE_NETWORK, isConnectionError = true, cause = e)
        } catch (e: Exception) {
            AppResult.failure(MESSAGE_UNEXPECTED, cause = e)
        }
    }

    private fun parseError(response: Response<*>, fallbackMessage: String): String = try {
        val raw = response.errorBody()?.string()
        gson.fromJson(raw, ApiErrorDto::class.java)?.message
            ?: "$fallbackMessage: HTTP ${response.code()}"
    } catch (e: Exception) {
        "$fallbackMessage: HTTP ${response.code()}"
    }
}
