package com.messmate.bd.di

import android.content.Context
import androidx.room.Room
import com.messmate.bd.auth.GoogleAuthManager
import com.messmate.bd.core.network.AndroidNetworkMonitor
import com.messmate.bd.core.network.NetworkMonitor
import com.messmate.bd.data.local.MessDatabase
import com.messmate.bd.data.remote.GoogleAccessTokenProvider
import com.messmate.bd.data.repository.AccountRepositoryImpl
import com.messmate.bd.data.repository.MessRepositoryImpl
import com.messmate.bd.domain.calculation.CutoffPolicy
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Singleton

/**
 * Application wide singletons that are not bound to a specific layer.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * The clock is injected everywhere instead of using `LocalDateTime.now()`
     * directly – that makes the cut-off logic testable.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    @Provides
    @Singleton
    fun provideCutoffPolicy(clock: Clock): CutoffPolicy = CutoffPolicy(clock)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        AndroidNetworkMonitor(context)
}

/**
 * The local cache.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MessDatabase =
        Room.databaseBuilder(context, MessDatabase::class.java, MessDatabase.NAME)
            // The cache is a *cache*: everything can be downloaded from the sheet
            // again, so a destructive migration is acceptable (and keeps the
            // schema migrations simple while the app is young).
            .fallbackToDestructiveMigration()
            .build()
}

/**
 * Interface → implementation bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMessRepository(impl: MessRepositoryImpl): MessRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    /** The Google clients ask this provider for the bearer token. */
    @Binds
    @Singleton
    abstract fun bindAccessTokenProvider(impl: GoogleAuthManager): GoogleAccessTokenProvider
}
