package com.summarizer.app.core.di

import android.content.Context
import com.summarizer.app.data.database.AppDatabase
import com.summarizer.app.data.network.GeminiApiService
import com.summarizer.app.data.network.YoutubeTranscriptScraper
import com.summarizer.app.data.preferences.PreferenceManager
import com.summarizer.app.data.repository.SummaryRepositoryImpl
import com.summarizer.app.domain.repository.SummaryRepository
import com.summarizer.app.domain.usecase.DeleteSummaryUseCase
import com.summarizer.app.domain.usecase.GetHistoryUseCase
import com.summarizer.app.domain.usecase.GetSettingsUseCase
import com.summarizer.app.domain.usecase.GetSummaryUseCase
import com.summarizer.app.domain.usecase.SaveSettingsUseCase
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Thread-safe Service Locator orchestrating dependency injection for the entire application.
 * Replaces complex Hilt/Dagger setups, reducing APK footprint and enabling instant builds.
 */
object ServiceLocator {

    private var database: AppDatabase? = null
    private var preferenceManager: PreferenceManager? = null
    private var okHttpClient: OkHttpClient? = null
    private var geminiApiService: GeminiApiService? = null
    private var transcriptScraper: YoutubeTranscriptScraper? = null
    private var repository: SummaryRepository? = null

    /**
     * Singleton instance of [OkHttpClient] with custom timeouts and an in-memory CookieJar
     * to preserve browser session cookies between HTML pages and subtitle downloads.
     */
    private fun getOkHttpClient(): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .cookieJar(object : CookieJar {
                    // Thread-safe in-memory cache mapped by hostname
                    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

                    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                        cookieStore[url.host] = cookies
                    }

                    override fun loadForRequest(url: HttpUrl): List<Cookie> {
                        return cookieStore[url.host] ?: emptyList()
                    }
                })
                .build()
            okHttpClient = client
            client
        }
    }

    /**
     * Singleton instance of the Retrofit [GeminiApiService].
     */
    private fun getGeminiApiService(): GeminiApiService {
        return geminiApiService ?: synchronized(this) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service = retrofit.create(GeminiApiService::class.java)
            geminiApiService = service
            service
        }
    }

    /**
     * Singleton instance of Room [AppDatabase].
     */
    private fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context)
            database = db
            db
        }
    }

    /**
     * Singleton instance of Android [PreferenceManager] with hardware-backed encryption.
     */
    private fun getPreferenceManager(context: Context): PreferenceManager {
        return preferenceManager ?: synchronized(this) {
            val pm = PreferenceManager(context)
            preferenceManager = pm
            pm
        }
    }

    /**
     * Singleton instance of our client-side [YoutubeTranscriptScraper].
     */
    private fun getTranscriptScraper(): YoutubeTranscriptScraper {
        return transcriptScraper ?: synchronized(this) {
            val scraper = YoutubeTranscriptScraper(getOkHttpClient())
            transcriptScraper = scraper
            scraper
        }
    }

    /**
     * Singleton instance of the repository layer [SummaryRepository].
     */
    fun getRepository(context: Context): SummaryRepository {
        return repository ?: synchronized(this) {
            val repo = SummaryRepositoryImpl(
                summaryDao = getDatabase(context).summaryDao(),
                transcriptScraper = getTranscriptScraper(),
                geminiApiService = getGeminiApiService(),
                preferenceManager = getPreferenceManager(context)
            )
            repository = repo
            repo
        }
    }

    // Factory methods to build and expose individual UseCases to ViewModels

    fun provideGetSummaryUseCase(context: Context): GetSummaryUseCase {
        return GetSummaryUseCase(getRepository(context))
    }

    fun provideGetHistoryUseCase(context: Context): GetHistoryUseCase {
        return GetHistoryUseCase(getRepository(context))
    }

    fun provideDeleteSummaryUseCase(context: Context): DeleteSummaryUseCase {
        return DeleteSummaryUseCase(getRepository(context))
    }

    fun provideGetSettingsUseCase(context: Context): GetSettingsUseCase {
        return GetSettingsUseCase(getRepository(context))
    }

    fun provideSaveSettingsUseCase(context: Context): SaveSettingsUseCase {
        return SaveSettingsUseCase(getRepository(context))
    }
}
