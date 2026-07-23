package id.co.alphanusa.perisaipoc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.co.alphanusa.perisaipoc.data.local.datasource.EncryptedTokenLocalDataSource
import id.co.alphanusa.perisaipoc.data.local.datasource.PrefsSettingsLocalDataSource
import id.co.alphanusa.perisaipoc.data.local.datasource.SettingsLocalDataSource
import id.co.alphanusa.perisaipoc.data.local.datasource.TokenLocalDataSource
import id.co.alphanusa.perisaipoc.data.repository.AuthRepositoryImpl
import id.co.alphanusa.perisaipoc.data.repository.CallRepositoryImpl
import id.co.alphanusa.perisaipoc.data.repository.MapRepositoryImpl
import id.co.alphanusa.perisaipoc.data.repository.PocRepositoryImpl
import id.co.alphanusa.perisaipoc.data.repository.RealtimeRepositoryImpl
import id.co.alphanusa.perisaipoc.data.repository.SettingsRepositoryImpl
import id.co.alphanusa.perisaipoc.data.repository.UserRepositoryImpl
import id.co.alphanusa.perisaipoc.domain.repository.AuthRepository
import id.co.alphanusa.perisaipoc.domain.repository.CallRepository
import id.co.alphanusa.perisaipoc.domain.repository.MapRepository
import id.co.alphanusa.perisaipoc.domain.repository.PocRepository
import id.co.alphanusa.perisaipoc.domain.repository.RealtimeRepository
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import id.co.alphanusa.perisaipoc.domain.repository.UserRepository
import javax.inject.Singleton

/** Menghubungkan kontrak di domain dengan implementasinya di data. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTokenLocalDataSource(
        impl: EncryptedTokenLocalDataSource,
    ): TokenLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSettingsLocalDataSource(
        impl: PrefsSettingsLocalDataSource,
    ): SettingsLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindPocRepository(impl: PocRepositoryImpl): PocRepository

    @Binds
    @Singleton
    abstract fun bindMapRepository(impl: MapRepositoryImpl): MapRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(impl: CallRepositoryImpl): CallRepository

    @Binds
    @Singleton
    abstract fun bindRealtimeRepository(impl: RealtimeRepositoryImpl): RealtimeRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
