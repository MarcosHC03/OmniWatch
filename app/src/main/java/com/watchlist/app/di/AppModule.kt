package com.watchlist.app.di

import android.content.Context
import androidx.room.Room
import com.watchlist.app.BuildConfig
import com.watchlist.app.data.local.WatchListDatabase
import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.remote.NewsApiService
import com.watchlist.app.data.remote.TmdbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ---- Room ----

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WatchListDatabase =
        Room.databaseBuilder(context, WatchListDatabase::class.java, "watchlist.db").build()

    @Provides
    fun provideMediaItemDao(db: WatchListDatabase): MediaItemDao = db.mediaItemDao()

    // ---- OkHttp ----

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            // Acá le inyectamos la API Key en la URL
            val url = original.url.newBuilder()
                .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                .build()
            
            val request = original.newBuilder()
                .url(url)
                .addHeader("accept", "application/json")
                .build()
                
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("news")
    fun provideNewsOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("apiKey", BuildConfig.NEWS_API_KEY)
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    // ---- Retrofit ----

    @Provides
    @Singleton
    fun provideTmdbApiService(@Named("tmdb") client: OkHttpClient): TmdbApiService =
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApiService::class.java)

    @Provides
    @Singleton
    fun provideNewsApiService(@Named("news") client: OkHttpClient): NewsApiService =
        Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
}
