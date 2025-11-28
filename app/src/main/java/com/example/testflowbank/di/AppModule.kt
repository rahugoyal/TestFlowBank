package com.example.testflowbank.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.testflowbank.core.logging.AppLogDao
import com.example.testflowbank.core.logging.AppLogDatabase
import com.example.testflowbank.data.auth.AuthApi
import com.example.testflowbank.data.dashboard.DashboardApi
import com.example.testflowbank.data.payments.PaymentsApi
import com.example.testflowbank.data.profile.ProfileApi
import com.example.testflowbank.data.transactions.TransactionsApi
import com.example.testflowbank.rag.RagPipeline
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // import com.example.testflowbank.rag.RagPipeline

    @Provides
    @Singleton
    fun provideRagPipeline(
        application: Application
    ): RagPipeline = RagPipeline(application)

    // 1) Moshi instance with Kotlin support
    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

    // 2) OkHttp
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()

                // Add ReqRes free API key (safe for other hosts too, they just ignore it)
                val requestWithKey = original.newBuilder()
                    .header("x-api-key", "reqres-free-v1")
                    .build()

                chain.proceed(requestWithKey)
            }
            .addInterceptor(logging)
            .build()
    }

    // 3) Retrofit instances using that Moshi

    @Provides
    @Singleton
    @Named("ReqRes")
    fun provideReqResRetrofit(
        okHttp: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://reqres.in/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()

    @Provides
    @Singleton
    @Named("DummyJson")
    fun provideDummyJsonRetrofit(
        okHttp: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()

    @Provides
    @Singleton
    @Named("JsonPlaceholder")
    fun provideJsonPlaceholderRetrofit(
        okHttp: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()

    // in AppModule.kt
    @Provides
    @Singleton
    fun providePaymentsRetrofit(
        okHttp: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://httpbin.org/") // base, overridden by @Url
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()

    // APIs
    @Provides @Singleton
    fun provideAuthApi(@Named("ReqRes") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideDashboardApi(@Named("DummyJson") retrofit: Retrofit): DashboardApi =
        retrofit.create(DashboardApi::class.java)

    @Provides @Singleton
    fun provideTransactionsApi(@Named("JsonPlaceholder") retrofit: Retrofit): TransactionsApi =
        retrofit.create(TransactionsApi::class.java)

    @Provides @Singleton
    fun providePaymentsApi(retrofit: Retrofit): PaymentsApi =
        retrofit.create(PaymentsApi::class.java)

    @Provides @Singleton
    fun provideProfileApi(@Named("DummyJson") retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    // Room
    @Provides
    @Singleton
    fun provideAppLogDatabase(
        @ApplicationContext context: Context
    ): AppLogDatabase =
        Room.databaseBuilder(
            context,
            AppLogDatabase::class.java,
            "logs.db"
        ).build()

    @Provides
    @Singleton
    fun provideAppLogDao(db: AppLogDatabase): AppLogDao = db.appLogDao()
}