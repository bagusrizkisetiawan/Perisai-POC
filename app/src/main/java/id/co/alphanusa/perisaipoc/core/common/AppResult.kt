package id.co.alphanusa.perisaipoc.core.common

/**
 * Pembungkus hasil operasi yang dipakai lintas lapisan (data → domain → ui).
 *
 * Dipakai agar UI tidak perlu menangani `Exception` mentah maupun tipe milik
 * Retrofit/OkHttp — cukup membedakan sukses dan gagal beserta pesannya.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    /**
     * @param message pesan siap tampil untuk pengguna.
     * @param isConnectionError true bila kegagalan berasal dari jaringan/timeout,
     *   bukan penolakan dari server (dipakai UI untuk menawarkan Reconnect).
     */
    data class Failure(
        val message: String,
        val isConnectionError: Boolean = false,
        val cause: Throwable? = null,
    ) : AppResult<Nothing>

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)

        fun failure(
            message: String,
            isConnectionError: Boolean = false,
            cause: Throwable? = null,
        ): AppResult<Nothing> = Failure(message, isConnectionError, cause)
    }
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppResult.Failure) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(this)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
