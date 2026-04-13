package com.example.rednotebook.data.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val PREFS_NAME = "rednotebook_prefs"
    private const val KEY_API_URL = "api_url"

    private var _api: RedNotebookApi? = null
    val api: RedNotebookApi
        get() = _api ?: throw IllegalStateException("ApiClient not initialised. Call init() first.")

    private val okhttp = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /** Call once from Application.onCreate() */
    fun init(context: Context) {
        val url = getSavedUrl(context)
        if (url.isNotBlank()) {
            buildApi(url)
        }
    }

    fun buildApi(baseUrl: String) {
        _api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RedNotebookApi::class.java)
    }

    fun saveUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_URL, url).apply()
    }

    fun getSavedUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_URL, "") ?: ""
    }

    fun isConfigured() = _api != null
}
