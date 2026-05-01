package com.saad.moviessaad.data

import android.os.Handler
import android.os.Looper
import com.saad.moviessaad.model.WatchlistItem
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class WatchlistEntry(
    @SerialName("user_id") val userId: String,
    @SerialName("movie_id") val movieId: Int,
    val title: String,
    @SerialName("poster_path") val posterPath: String,
    val rating: Double,
    @SerialName("added_at") val addedAt: String
)

@Serializable
data class UserEntry(
    val id: String,
    val username: String,
    val email: String
)

object SupabaseService {

    interface AuthCallback {
        fun onSuccess(userId: String)
        fun onError(message: String)
    }

    interface WatchlistStatusCallback {
        fun onSuccess(isInWatchlist: Boolean)
        fun onError(message: String)
    }

    interface WatchlistCallback {
        fun onSuccess(items: List<@JvmSuppressWildcards WatchlistItem>)
        fun onError(message: String)
    }

    interface ActionCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getCurrentUserId(): String? = SupabaseClientProvider.client.auth.currentUserOrNull()?.id

    fun isLoggedIn(): Boolean = getCurrentUserId() != null

    fun login(email: String, password: String, callback: AuthCallback) {
        ioScope.launch {
            runCatching {
                SupabaseClientProvider.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            }.onSuccess { userId ->
                if (userId == null) {
                    postError(callback, "Unable to load session")
                    return@onSuccess
                }
                mainHandler.post { callback.onSuccess(userId) }
            }.onFailure { error ->
                postError(callback, error.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, username: String, callback: AuthCallback) {
        ioScope.launch {
            runCatching {
                SupabaseClientProvider.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("Unable to load user id")
                
                // Fixed: Using UserEntry data class instead of mapOf
                val userEntry = UserEntry(id = userId, username = username, email = email)
                SupabaseClientProvider.client.from("users").upsert(userEntry)
                
                userId
            }.onSuccess { userId ->
                mainHandler.post { callback.onSuccess(userId) }
            }.onFailure { error ->
                postError(callback, error.message ?: "Registration failed")
            }
        }
    }

    fun signOut(callback: ActionCallback) {
        ioScope.launch {
            runCatching {
                SupabaseClientProvider.client.auth.signOut()
            }.onSuccess {
                mainHandler.post { callback.onSuccess() }
            }.onFailure { error ->
                postActionError(callback, error.message ?: "Logout failed")
            }
        }
    }

    fun isMovieInWatchlist(userId: String, movieId: Int, callback: WatchlistStatusCallback) {
        ioScope.launch {
            runCatching {
                val response = SupabaseClientProvider.client.from("watchlist").select {
                    filter {
                        eq("user_id", userId)
                        eq("movie_id", movieId)
                    }
                }
                // Check if result is empty
                JSONArray(response.data.toString()).length() > 0
            }.onSuccess { exists ->
                mainHandler.post { callback.onSuccess(exists) }
            }.onFailure { error ->
                postStatusError(callback, error.message ?: "Watchlist lookup failed")
            }
        }
    }

    fun upsertWatchlistItem(
        userId: String,
        movieId: Int,
        title: String,
        posterPath: String,
        rating: Double,
        callback: ActionCallback
    ) {
        ioScope.launch {
            runCatching {
                // Fixed: Ensure we use the serializable data class
                val entry = WatchlistEntry(
                    userId = userId,
                    movieId = movieId,
                    title = title,
                    posterPath = posterPath,
                    rating = rating,
                    addedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
                )
                SupabaseClientProvider.client.from("watchlist").upsert(entry)
            }.onSuccess {
                mainHandler.post { callback.onSuccess() }
            }.onFailure { error ->
                postActionError(callback, error.message ?: "Unable to add watchlist item")
            }
        }
    }

    fun removeWatchlistItem(userId: String, movieId: Int, callback: ActionCallback) {
        ioScope.launch {
            runCatching {
                SupabaseClientProvider.client.from("watchlist").delete {
                    filter {
                        eq("user_id", userId)
                        eq("movie_id", movieId)
                    }
                }
            }.onSuccess {
                mainHandler.post { callback.onSuccess() }
            }.onFailure { error ->
                postActionError(callback, error.message ?: "Unable to remove watchlist item")
            }
        }
    }

    fun loadWatchlist(userId: String, callback: WatchlistCallback) {
        ioScope.launch {
            runCatching {
                val response = SupabaseClientProvider.client.from("watchlist").select {
                    filter { eq("user_id", userId) }
                    order("added_at", Order.DESCENDING)
                }
                parseWatchlist(response.data.toString())
            }.onSuccess { items ->
                mainHandler.post { callback.onSuccess(items) }
            }.onFailure { error ->
                postWatchlistError(callback, error.message ?: "Unable to load watchlist")
            }
        }
    }

    private fun parseWatchlist(rawJson: String): List<WatchlistItem> {
        val array = JSONArray(rawJson)
        val result = mutableListOf<WatchlistItem>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            result.add(
                WatchlistItem(
                    item.optInt("movie_id"),
                    item.optString("title"),
                    item.optString("poster_path"),
                    item.optDouble("rating"),
                    item.optString("added_at")
                )
            )
        }
        return result
    }

    private fun postError(callback: AuthCallback, message: String) {
        mainHandler.post { callback.onError(message) }
    }

    private fun postStatusError(callback: WatchlistStatusCallback, message: String) {
        mainHandler.post { callback.onError(message) }
    }

    private fun postWatchlistError(callback: WatchlistCallback, message: String) {
        mainHandler.post { callback.onError(message) }
    }

    private fun postActionError(callback: ActionCallback, message: String) {
        mainHandler.post { callback.onError(message) }
    }
}
